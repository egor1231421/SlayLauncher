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

package com.slaylauncher.ui.screens.main

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import com.slaylauncher.BuildKeys
import com.slaylauncher.R
import com.slaylauncher.coroutine.TaskSystem
import com.slaylauncher.game.version.installed.Version
import com.slaylauncher.game.version.installed.VersionsManager
import com.slaylauncher.path.PathManager
import com.slaylauncher.setting.LauncherSettings
import com.slaylauncher.ui.AndroidStringText
import com.slaylauncher.ui.androidText
import com.slaylauncher.ui.components.BackgroundCard
import com.slaylauncher.ui.components.CardTitleLayout
import com.slaylauncher.ui.components.TextRailItem
import com.slaylauncher.ui.guide.sendStartGuideOnce
import com.slaylauncher.ui.screens.BackStackNavKey
import com.slaylauncher.ui.screens.NavStateHost
import com.slaylauncher.ui.screens.NestedNavDisplay
import com.slaylauncher.ui.screens.NestedNavKey
import com.slaylauncher.ui.screens.NormalNavKey
import com.slaylauncher.ui.screens.TitledNavKey
import com.slaylauncher.ui.screens.content.AccountManageScreen
import com.slaylauncher.ui.screens.content.DownloadScreen
import com.slaylauncher.ui.screens.content.FileSelectorScreen
import com.slaylauncher.ui.screens.content.LauncherScreen
import com.slaylauncher.ui.screens.content.LicenseScreen
import com.slaylauncher.ui.screens.content.LogViewScreen
import com.slaylauncher.ui.screens.content.MultiplayerScreen
import com.slaylauncher.ui.screens.content.SettingsScreen
import com.slaylauncher.ui.screens.content.VersionExportScreen
import com.slaylauncher.ui.screens.content.VersionSettingsScreen
import com.slaylauncher.ui.screens.content.VersionsManageScreen
import com.slaylauncher.ui.screens.content.WebViewScreen
import com.slaylauncher.ui.screens.content.addoninfo.AddonInfoScreen
import com.slaylauncher.ui.screens.content.navigateToDownload
import com.slaylauncher.ui.screens.navigateTo
import com.slaylauncher.ui.screens.onBack
import com.slaylauncher.ui.screens.rememberTransitionSpec
import com.slaylauncher.ui.theme.backgroundColor
import com.slaylauncher.ui.theme.cardColor
import com.slaylauncher.ui.theme.festivals.FestivalTitleText
import com.slaylauncher.ui.theme.onBackgroundColor
import com.slaylauncher.ui.theme.onCardColor
import com.slaylauncher.ui.theme.SlayPalette
import com.slaylauncher.ui.theme.slayPixelSurface
import com.slaylauncher.utils.animation.getAnimateTween
import com.slaylauncher.utils.festival.LocalFestivals
import com.slaylauncher.utils.file.formatFileSize
import com.slaylauncher.viewmodel.ErrorViewModel
import com.slaylauncher.viewmodel.EventViewModel
import com.slaylauncher.viewmodel.LocalBackgroundViewModel
import com.slaylauncher.viewmodel.ModifyVersionViewModel
import com.slaylauncher.viewmodel.ModpackImportViewModel
import com.slaylauncher.viewmodel.ScreenBackStackViewModel
import com.slaylauncher.viewmodel.sendKeepScreen
import com.slaylauncher.viewmodel.sendToast

@Composable
fun MainScreen(
    screenBackStackModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    //监控当前是否有任务正在进行
    var taskRunning by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        TaskSystem.tasksFlow.collect { tasks ->
            val noTasks = tasks.isEmpty()
            eventViewModel.sendKeepScreen(!noTasks)
            taskRunning = noTasks
        }
    }

    val isTaskMenuExpanded = LauncherSettings.launcherTaskMenuExpanded.state

    fun changeTasksExpandedState() {
        LauncherSettings.launcherTaskMenuExpanded.save(!isTaskMenuExpanded)
    }

    /** 回到主页面通用函数 */
    val toMainScreen: () -> Unit = {
        screenBackStackModel.mainScreen.clearWith(NormalNavKey.LauncherMain)
    }

    val mainScreenKey = screenBackStackModel.mainScreen.currentKey
    val inLauncherScreen = mainScreenKey == null || mainScreenKey is NormalNavKey.LauncherMain

    val isBackgroundValid = LocalBackgroundViewModel.current?.isValid == true
    val launcherBackgroundOpacity = LauncherSettings.launcherBackgroundOpacity.state.toFloat() / 100f

    val backgroundColor = if (isBackgroundValid) {
        backgroundColor().copy(alpha = launcherBackgroundOpacity)
    } else backgroundColor()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor,
        contentColor = onBackgroundColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            TopBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .slayPixelSurface(
                        fill = SlayPalette.surface,
                        cut = 0.dp,
                        outline = Color(0x12FFFFFF),
                        bottomBand = SlayPalette.bandBottomSoft
                    ),
                mainScreenKey = mainScreenKey,
                inLauncherScreen = inLauncherScreen,
                taskRunning = taskRunning,
                isTasksExpanded = isTaskMenuExpanded,
                contentColor = onBackgroundColor(),
                onScreenBack = {
                    screenBackStackModel.mainScreen.backStack.removeFirstOrNull()
                },
                toMainScreen = toMainScreen,
                toSettingsScreen = {
                    screenBackStackModel.mainScreen.removeAndNavigateTo(
                        removes = screenBackStackModel.clearBeforeNavKeys,
                        screenKey = screenBackStackModel.settingsScreen
                    )
                },
                toDownloadScreen = {
                    screenBackStackModel.navigateToDownload()
                },
                toMultiplayerScreen = {
                    screenBackStackModel.mainScreen.removeAndNavigateTo(
                        removes = screenBackStackModel.clearBeforeNavKeys,
                        screenKey = NormalNavKey.Multiplayer
                    )
                },
                openFileManager = {
                    eventViewModel.sendEvent(
                        EventViewModel.Event.OpenFileManager(
                            rootPath = PathManager.DIR_FILES_EXTERNAL.absolutePath
                        )
                    )
                },
                changeExpandedState = {
                    changeTasksExpandedState()
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                NavStateHost(rootKey = screenBackStackModel.mainScreen) {
                    NavigationUI(
                        modifier = Modifier.fillMaxSize(),
                        screenBackStackModel = screenBackStackModel,
                        toMainScreen = toMainScreen,
                        eventViewModel = eventViewModel,
                        modpackImportViewModel = modpackImportViewModel,
                        modifyVersionViewModel = modifyVersionViewModel,
                        submitError = submitError
                    )
                }

                TaskMenu(
                    isExpanded = isTaskMenuExpanded,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                        .align(Alignment.CenterStart)
                        .padding(all = 6.dp)
                ) {
                    changeTasksExpandedState()
                }
            }

            val activeTab = when {
                inLauncherScreen -> 0
                mainScreenKey is NormalNavKey.VersionsManager ||
                    mainScreenKey is NestedNavKey.VersionSettings -> 1
                mainScreenKey is NestedNavKey.Download -> 2
                mainScreenKey is NormalNavKey.Multiplayer -> 3
                else -> 4
            }
            SlayBottomBar(
                activeTab = activeTab,
                onSelect = { index ->
                    when (index) {
                        0 -> toMainScreen()
                        1 -> screenBackStackModel.mainScreen.navigateTo(
                            screenKey = NormalNavKey.VersionsManager
                        )
                        2 -> screenBackStackModel.navigateToDownload()
                        3 -> screenBackStackModel.mainScreen.removeAndNavigateTo(
                            removes = screenBackStackModel.clearBeforeNavKeys,
                            screenKey = NormalNavKey.Multiplayer
                        )
                        else -> screenBackStackModel.mainScreen.removeAndNavigateTo(
                            removes = screenBackStackModel.clearBeforeNavKeys,
                            screenKey = screenBackStackModel.settingsScreen
                        )
                    }
                }
            )
        }
    }
}

/** Millida-style five-slot dock: Play / Builds / Catalog / Servers / More. */
@Composable
private fun SlayBottomBar(
    activeTab: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        R.string.slay_tab_play to R.drawable.ic_play_arrow_filled,
        R.string.slay_tab_builds to R.drawable.ic_assignment_filled,
        R.string.slay_tab_catalog to R.drawable.ic_download_2_filled,
        R.string.slay_tab_servers to R.drawable.ic_group_filled,
        R.string.slay_tab_more to R.drawable.ic_settings_filled
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .slayPixelSurface(
                fill = Color(0x8C04140A),
                cut = 10.dp,
                topOnly = true,
                outline = SlayPalette.dockOutline,
                topBand = Color(0x33FFFFFF)
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, (labelRes, iconRes) ->
                val selected = activeTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .padding(horizontal = 2.dp)
                        .slayPixelSurface(
                            fill = if (selected) SlayPalette.accent else Color.Transparent,
                            cut = 5.dp,
                            outline = if (selected) Color(0x29000000) else Color.Transparent,
                            topBand = if (selected) SlayPalette.bandTop else Color.Transparent,
                            bottomBand = if (selected) SlayPalette.bandBottom else Color.Transparent,
                            dots = selected
                        )
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = stringResource(labelRes),
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) SlayPalette.onAccent else SlayPalette.fgMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) SlayPalette.onAccent else SlayPalette.fgMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <E: TitledNavKey> TopBar(
    mainScreenKey: E?,
    inLauncherScreen: Boolean,
    taskRunning: Boolean,
    isTasksExpanded: Boolean,
    modifier: Modifier = Modifier,
    contentColor: Color,
    onScreenBack: () -> Unit,
    toMainScreen: () -> Unit,
    toSettingsScreen: () -> Unit,
    toDownloadScreen: () -> Unit,
    toMultiplayerScreen: () -> Unit,
    openFileManager: () -> Unit,
    changeExpandedState: () -> Unit,
) {
    val festivals = LocalFestivals.current

    val inMultiplayerScreen = mainScreenKey is NormalNavKey.Multiplayer
    val inDownloadScreen = mainScreenKey is NestedNavKey.Download
    val inSettingsScreen = mainScreenKey is NestedNavKey.Settings

    CompositionLocalProvider(
        LocalContentColor provides contentColor
    ) {
        ConstraintLayout(modifier = modifier) {
            val (backCenter, title, endButtons) = createRefs()

            val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

            Row(
                modifier = Modifier
                    .constrainAs(backCenter) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .fillMaxHeight()
            ) {
                Icon(
                    modifier = Modifier
                        .size(26.dp)
                        .padding(start = 8.dp),
                    painter = painterResource(R.drawable.slay_logo),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
                AnimatedVisibility(
                    visible = !inLauncherScreen
                ) {
                    Row(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.width(12.dp))

                        IconButton(
                            modifier = Modifier.fillMaxHeight(),
                            onClick = {
                                if (!inLauncherScreen) {
                                    //不在主屏幕时才允许返回
                                    backDispatcher?.onBackPressed() ?: run {
                                        onScreenBack()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.generic_back)
                            )
                        }

                        IconButton(
                            modifier = Modifier.fillMaxHeight(),
                            onClick = {
                                if (!inLauncherScreen) {
                                    //不在主屏幕时才允许回到主页面
                                    toMainScreen()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_home_filled),
                                contentDescription = stringResource(R.string.generic_main_menu)
                            )
                        }
                    }
                }
            }
            val parentRes = mainScreenKey?.title
            val childRes = (mainScreenKey as? BackStackNavKey<*>)?.currentKey?.title

            Crossfade(
                modifier = Modifier.constrainAs(title) {
                    centerVerticallyTo(parent)
                    start.linkTo(backCenter.end, margin = 16.dp)
                },
                targetState = parentRes to childRes
            ) { (parent, child) ->
                val style = MaterialTheme.typography.titleMedium
                val softWarp = false
                val maxLines = 1

                if (parent == null) {
                    if (festivals.isEmpty()) {
                        Text(
                            text = BuildKeys.LAUNCHER_IDENTIFIER,
                            style = style,
                            softWrap = softWarp,
                            maxLines = maxLines
                        )
                    } else {
                        FestivalTitleText(
                            festivals = festivals,
                            style = style,
                            maxLines = maxLines
                        )
                    }
                } else {
                    val titleText = if (child != null) {
                        androidText(parent, androidText(" - "), child)
                    } else {
                        parent
                    }

                    AndroidStringText(
                        text = titleText,
                        style = style,
                        softWrap = softWarp,
                        maxLines = maxLines
                    )
                }
            }

            Row(
                modifier = Modifier
                    .constrainAs(endButtons) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end, margin = 12.dp)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = !(isTasksExpanded || taskRunning),
                    enter = slideInVertically(
                        initialOffsetY = { -50 }
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { -50 }
                    ) + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .clip(shape = MaterialTheme.shapes.large)
                            .clickable { changeExpandedState() }
                            .padding(all = 8.dp)
                            .width(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(modifier = Modifier.weight(1f))
                        Icon(
                            modifier = Modifier.size(22.dp),
                            painter = painterResource(R.drawable.ic_assignment_filled),
                            contentDescription = stringResource(R.string.main_task_menu)
                        )
                    }
                }

                IconButton(
                    onClick = openFileManager
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder_filled),
                        contentDescription = null
                    )
                }

                TopBarRailItem(
                    selected = inMultiplayerScreen,
                    painter = painterResource(R.drawable.ic_group_filled),
                    text = stringResource(R.string.terracotta),
                    onClick = {
                        if (!inMultiplayerScreen) toMultiplayerScreen()
                    },
                )

                TopBarRailItem(
                    selected = inDownloadScreen,
                    painter = painterResource(R.drawable.ic_download_2_filled),
                    text = stringResource(R.string.generic_download),
                    onClick = {
                        if (!inDownloadScreen) toDownloadScreen()
                    },
                )

                TopBarRailItem(
                    selected = inSettingsScreen,
                    painter = painterResource(R.drawable.ic_settings_filled),
                    text = stringResource(R.string.generic_setting),
                    onClick = {
                        if (!inSettingsScreen) toSettingsScreen()
                    },
                )
            }
        }
    }
}

@Composable
private fun TopBarRailItem(
    selected: Boolean,
    painter: Painter,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    textStyle: TextStyle = MaterialTheme.typography.labelMedium
) {
    TextRailItem(
        modifier = modifier,
        onClick = onClick,
        text = {
            AnimatedVisibility(visible = selected) {
                Row {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = text,
                        style = textStyle
                    )
                }
            }
        },
        icon = {
            Icon(
                painter = painter,
                contentDescription = text
            )
        },
        selected = selected,
        selectedPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        unSelectedPadding = PaddingValues(all = 8.dp),
    )
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    screenBackStackModel: ScreenBackStackViewModel,
    toMainScreen: () -> Unit,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val backStack = screenBackStackModel.mainScreen.backStack
    val currentKey = backStack.lastOrNull()

    LaunchedEffect(currentKey) {
        screenBackStackModel.mainScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        /** 导航至版本详细信息屏幕 */
        val navigateToVersions: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.navigateTo(
                screenKey = NestedNavKey.VersionSettings(version),
                useClassEquality = true
            )
        }
        /** 导航至整合包导出屏幕 */
        val navigateToExport: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.removeAndNavigateTo(
                remove = NestedNavKey.VersionSettings::class,
                screenKey = NestedNavKey.VersionExport(version),
                useClassEquality = true
            )
        }

        NestedNavDisplay(
            ownerKey = screenBackStackModel.mainScreen,
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.LauncherMain> {
                    LauncherScreen(
                        backStackViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        onLaunchGame = { version ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.Game(version)
                            )
                        },
                        onQuickPlayServer = { version, address ->
                            val target = (version ?: VersionsManager.currentVersion.value)?.takeIf { it.isValid() }
                            if (target != null) {
                                eventViewModel.sendEvent(
                                    EventViewModel.Event.Launch.PlayServer(target, address)
                                )
                            } else {
                                eventViewModel.sendToast(androidText(R.string.game_launch_no_version))
                                screenBackStackModel.mainScreen.navigateTo(NormalNavKey.VersionsManager)
                            }
                        },
                        onOpenLink = {
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(it))
                        },
                        startGuideOnce = { keys ->
                            eventViewModel.sendStartGuideOnce(keys)
                        }
                    )
                }
                entry<NestedNavKey.Settings> { key ->
                    SettingsScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        openLicenseScreen = { raw ->
                            backStack.navigateTo(NormalNavKey.License(raw))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.License> { key ->
                    LicenseScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel
                    )
                }
                entry<NormalNavKey.AccountManager> { key ->
                    AccountManageScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        openLink = { url ->
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.WebScreen> { key ->
                    WebViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.VersionsManager> {
                    VersionsManageScreen(
                        backScreenViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        navigateToExport = navigateToExport,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.FileSelector> { key ->
                    FileSelectorScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel
                    ) {
                        backStack.removeLastOrNull()
                    }
                }
                entry<NestedNavKey.VersionSettings> { key ->
                    VersionSettingsScreen(
                        key = key,
                        modifyViewModel = modifyVersionViewModel,
                        backScreenViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        onExportModpack = {
                            navigateToExport(key.version)
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.VersionExport> { key ->
                    VersionExportScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        backToMainScreen = toMainScreen
                    )
                }
                entry<NestedNavKey.Download> { key ->
                    DownloadScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        modpackImportViewModel = modpackImportViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.AddonInfo> { key ->
                    AddonInfoScreen(
                        key = key,
                        mainScreenKey = screenBackStackModel.mainScreen.currentKey,
                        addonInfoScreenKey = key.currentKey,
                        eventViewModel = eventViewModel,
                        submitError = submitError,
                    )
                }
                entry<NormalNavKey.Multiplayer> {
                    MultiplayerScreen(
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.LogView> { key ->
                    LogViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun TaskMenu(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    changeExpandedState: () -> Unit = {}
) {
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    val show = isExpanded && tasks.isNotEmpty()

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedVisibility(
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeOut(),
        visible = show
    ) {
        BackgroundCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 6.dp),
            influencedByBackground = false,
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor(),
                contentColor = onBackgroundColor()
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                CardTitleLayout(blur = 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        IconButton(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterStart),
                            onClick = changeExpandedState
                        ) {
                            Icon(
                                modifier = Modifier.size(28.dp),
                                painter = painterResource(R.drawable.ic_arrow_left_rounded),
                                contentDescription = stringResource(R.string.generic_collapse)
                            )
                        }

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.main_task_menu)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val taskProgress by task.progress.collectAsStateWithLifecycle()
                        val taskMessage by task.message.collectAsStateWithLifecycle()
                        val rateBytesPerSec by task.rateBytesPerSec.collectAsStateWithLifecycle()

                        TaskItem(
                            taskProgress = taskProgress,
                            taskMessage = taskMessage,
                            rateBytesPerSec = rateBytesPerSec,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            //取消任务
                            TaskSystem.cancelTask(task.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItem(
    taskProgress: Float,
    taskMessage: AndroidStringText?,
    rateBytesPerSec: Long?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
    onCancelClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                onClick = onCancelClick
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.generic_cancel)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                taskMessage?.let { message ->
                    AndroidStringText(
                        text = message,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (taskProgress < 0) { //负数则代表不确定
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { taskProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    taskProgress.takeIf { it >= 0f }?.let { progress ->
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    rateBytesPerSec?.let { bytes ->
                        val text = remember(bytes) { "${formatFileSize(bytes)}/s" }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}