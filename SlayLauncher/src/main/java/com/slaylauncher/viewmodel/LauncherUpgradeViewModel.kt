/*
 * SlayLauncher
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.slaylauncher.viewmodel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slaylauncher.BuildConfig
import com.slaylauncher.R
import com.slaylauncher.path.GLOBAL_CLIENT
import com.slaylauncher.ui.components.MarqueeText
import com.slaylauncher.ui.components.SimpleListDialog
import com.slaylauncher.ui.screens.content.elements.DisabledAlpha
import com.slaylauncher.ui.upgrade.UpgradeDialog
import com.slaylauncher.ui.upgrade.UpgradeFilesDialog
import com.slaylauncher.upgrade.GithubReleaseApi
import com.slaylauncher.upgrade.RemoteData
import com.slaylauncher.upgrade.TooFrequentOperationException
import com.slaylauncher.upgrade.UpgradePrefs
import com.slaylauncher.utils.logging.Logger
import com.slaylauncher.utils.network.safeBodyAsJson
import com.slaylauncher.utils.network.withRetry
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val TAG = "LauncherUpgradeVM"

sealed interface LauncherUpgradeOperation {
    data object None : LauncherUpgradeOperation
    /** 已检查到启动器存在新版本，展示更新信息 */
    data class Upgrade(val data: RemoteData) : LauncherUpgradeOperation
    /** 选择要安装的安装包文件 */
    data class SelectApk(val data: RemoteData) : LauncherUpgradeOperation
    /** 打开网盘分享 */
    data class OpenCloudDrive(val cloudDrive: RemoteData.CloudDrive) : LauncherUpgradeOperation
}

/**
 * Обновления берутся напрямую из GitHub Releases этого репозитория.
 */
private const val LATEST_API_URL =
    "https://api.github.com/repos/egor1231421/SlayLauncher/releases/latest"

/** Извлекает числовой номер версии из строки вида v2.8.2-slay или 2.8.2-debug. */
private fun versionNumberFrom(value: String?): Int {
    if (value.isNullOrBlank()) return 0
    val match = Regex("(\\d+)\\.(\\d+)\\.(\\d+)").find(value) ?: return 0
    val (major, minor, patch) = match.destructured
    return major.toIntOrNull()?.times(10000)?.plus(
        (minor.toIntOrNull() ?: 0) * 100 + (patch.toIntOrNull() ?: 0)
    ) ?: 0
}

private fun archFromAssetName(name: String): RemoteData.RemoteFile.Arch {
    return when {
        name.contains("arm64", ignoreCase = true) -> RemoteData.RemoteFile.Arch.ARM64
        name.contains("x86_64", ignoreCase = true) -> RemoteData.RemoteFile.Arch.X86_64
        name.contains("x86", ignoreCase = true) -> RemoteData.RemoteFile.Arch.X86
        name.contains("arm", ignoreCase = true) -> RemoteData.RemoteFile.Arch.ARM
        else -> RemoteData.RemoteFile.Arch.ALL
    }
}

/**
 * 用于记录启动器更新 ViewModel
 */
class LauncherUpgradeViewModel: ViewModel() {
    var operation by mutableStateOf<LauncherUpgradeOperation>(LauncherUpgradeOperation.None)

    private val checkMutex = Mutex()

    /**
     * 检查是否在限频时间内
     * @param time 限频时间（毫秒）
     * @param lastCheckTime 上次检查的时间戳
     */
    private fun isWithinRateLimit(
        time: Long,
        lastCheckTime: Long
    ): Boolean {
        val currentTime = System.currentTimeMillis()
        if (lastCheckTime > currentTime) {
            //用户调整到了未来的时间，无法正常判断
            //直接允许进行检查
            return false
        }
        return currentTime - lastCheckTime < time
    }

    /**
     * 更新最后一次检查的时间
     */
    private fun updateLastCheckTime() {
        UpgradePrefs.lastUpgradeCheck = System.currentTimeMillis()
    }

    /**
     * 在启动时，快速完成所有的检查
     */
    fun checkOnAppStart(
        onIsLatest: suspend () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (
                isWithinRateLimit(
                    time = TimeUnit.HOURS.toMillis(1L),
                    lastCheckTime = UpgradePrefs.lastUpgradeCheck
                )
            ) {
                Logger.info(TAG, "App start check: Within rate limit, skipping")
                return@launch
            }

            val data = fetchRemoteData()
            if (data != null) {
                checkForUpgrade(
                    data = data,
                    lastIgnored = UpgradePrefs.lastIgnoredVersion,
                    ignoreDismissedVersions = true, //启动时检查忽略用户已忽略的版本
                    onUpgrade = { data ->
                        operation = LauncherUpgradeOperation.Upgrade(data)
                    },
                    onIsLatest = onIsLatest
                )
            }
            updateLastCheckTime()
        }
    }

    /**
     * 用户在设置内手动点击检查更新
     * @param onInProgress 准备检查更新
     * @param onIsLatest 当前启动器是最新版
     */
    suspend fun checkManually(
        onInProgress: suspend () -> Unit = {},
        onIsLatest: suspend () -> Unit = {}
    ): Boolean {
        return checkMutex.withLock {
            if (
                isWithinRateLimit(
                    time = TimeUnit.SECONDS.toMillis(5L),
                    lastCheckTime = UpgradePrefs.lastUpgradeCheck
                )
            ) throw TooFrequentOperationException()

            onInProgress()

            val data = fetchRemoteData()
            if (data != null) {
                checkForUpgrade(
                    data = data,
                    lastIgnored = UpgradePrefs.lastIgnoredVersion,
                    ignoreDismissedVersions = false,
                    onUpgrade = { data ->
                        operation = LauncherUpgradeOperation.Upgrade(data)
                    },
                    onIsLatest = onIsLatest
                )
            }
            updateLastCheckTime()
            data != null
        }
    }

    /**
     * Получает информацию о последнем релизе напрямую из GitHub Releases.
     */
    private suspend fun fetchRemoteData(): RemoteData? {
        return withContext(Dispatchers.IO) {
            runCatching {
                withRetry(logTag = "LauncherUpgrade", maxRetries = 2) {
                    val release = GLOBAL_CLIENT.get(LATEST_API_URL)
                        .safeBodyAsJson<GithubReleaseApi>()

                    val code = versionNumberFrom(release.tagName)
                        .takeIf { it > 0 }
                        ?: versionNumberFrom(release.name)
                    if (code == 0) {
                        throw IllegalStateException("Cannot parse version from release: ${release.tagName}")
                    }

                    val markdown = release.body.orEmpty().ifBlank { release.name.orEmpty() }
                    val files = release.assets.map { asset ->
                        RemoteData.RemoteFile(
                            fileName = asset.name,
                            uri = asset.browserDownloadUrl,
                            arch = archFromAssetName(asset.name),
                            size = asset.size
                        )
                    }

                    RemoteData(
                        code = code,
                        version = release.tagName,
                        createdAt = release.publishedAt,
                        defaultCloudDrive = null,
                        cloudDrives = emptyList(),
                        files = files,
                        defaultBody = RemoteData.RemoteBody("en", markdown),
                        bodies = listOf(
                            RemoteData.RemoteBody("en", markdown),
                            RemoteData.RemoteBody("ru", markdown)
                        )
                    )
                }
            }.getOrElse { e ->
                Logger.warning(TAG, "Failed to check for launcher upgrade!", e)
                null
            }
        }
    }

    /**
     * 检查启动器是否需要更新
     * @param lastIgnored 上次弹出更新弹窗时，用户所忽略的版本号
     * @param ignoreDismissedVersions 是否忽略用户已忽略的版本
     * @param onUpgrade 发现需要更新时调用
     * @param onIsLatest 当前已是最新版本时
     */
    private suspend fun checkForUpgrade(
        data: RemoteData,
        lastIgnored: Int?,
        ignoreDismissedVersions: Boolean,
        onUpgrade: suspend (RemoteData) -> Unit,
        onIsLatest: suspend () -> Unit = {}
    ) {
        val currentVersionCode = versionNumberFrom(BuildConfig.VERSION_NAME)
        if (currentVersionCode < data.code) {
            //启动器为旧版本
            when {
                ignoreDismissedVersions && lastIgnored == data.code -> {
                    //忽略这次更新
                    Logger.info(TAG, "Launcher update detected: $currentVersionCode -> ${data.code}, but ignored by user")
                }
                else -> {
                    //弹出更新弹窗
                    Logger.info(TAG, "Launcher update detected: $currentVersionCode -> ${data.code}, dialog shown to user")
                    onUpgrade(data)
                }
            }
        } else {
            Logger.info(TAG, "Launcher is running the latest version: $currentVersionCode")
            onIsLatest()
        }
    }
}

@Composable
fun LauncherUpgradeOperation(
    operation: LauncherUpgradeOperation,
    onChanged: (LauncherUpgradeOperation) -> Unit,
    onIgnoredClick: (code: Int) -> Unit,
    onLinkClick: (String) -> Unit
) {
    when (operation) {
        is LauncherUpgradeOperation.None -> {}
        is LauncherUpgradeOperation.Upgrade -> {
            UpgradeDialog(
                data = operation.data,
                onDismissRequest = {
                    onChanged(LauncherUpgradeOperation.None)
                },
                onFilesClick = {
                    onChanged(LauncherUpgradeOperation.SelectApk(operation.data))
                },
                onIgnored = {
                    onIgnoredClick(operation.data.code)
                },
                onLinkClick = onLinkClick,
                onCloudDriveClick = { cloudDrive ->
                    onChanged(LauncherUpgradeOperation.OpenCloudDrive(cloudDrive))
                }
            )
        }
        is LauncherUpgradeOperation.SelectApk -> {
            UpgradeFilesDialog(
                data = operation.data,
                onDismissRequest = {
                    onChanged(LauncherUpgradeOperation.None)
                },
                onFileSelected = { file ->
                    onLinkClick(file.uri)
                    onChanged(LauncherUpgradeOperation.None)
                }
            )
        }
        is LauncherUpgradeOperation.OpenCloudDrive -> {
            val current by remember(operation) {
                mutableStateOf<RemoteData.CloudDrive.Link?>(null)
            }
            SimpleListDialog(
                title = stringResource(R.string.upgrade_cloud_drive),
                items = operation.cloudDrive.links,
                onItemSelected = { link ->
                    onLinkClick(link.link)
                },
                onDismissRequest = {
                    onChanged(LauncherUpgradeOperation.None)
                },
                current = current,
                itemLayout = { item, isCurrent, onClick ->
                    CloudDriveLayout(
                        link = item,
                        selected = isCurrent,
                        onClick = onClick
                    )
                },
                showConfirm = true,
                confirmText = {
                    MarqueeText(text = stringResource(R.string.generic_confirm))
                }
            )
        }
    }
}


@Composable
private fun CloudDriveLayout(
    link: RemoteData.CloudDrive.Link,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled
        )
        Column(
            modifier = Modifier.alpha(if (enabled) 1.0f else DisabledAlpha),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            //网盘名称
            MarqueeText(
                modifier = Modifier.fillMaxWidth(),
                text = link.name,
                style = MaterialTheme.typography.labelMedium
            )
            //网盘链接
            MarqueeText(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.7f),
                text = link.link,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}