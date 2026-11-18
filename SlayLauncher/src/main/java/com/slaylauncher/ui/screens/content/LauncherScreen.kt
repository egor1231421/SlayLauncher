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

package com.slaylauncher.ui.screens.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.cardgrid.state.rememberCardGridState
import com.slaylauncher.R
import com.slaylauncher.game.account.AccountsManager
import com.slaylauncher.game.account.getAccountTypeName
import com.slaylauncher.game.version.installed.Version
import com.slaylauncher.game.version.installed.VersionsManager
import com.slaylauncher.ui.base.BaseScreen
import com.slaylauncher.ui.components.BackgroundCard
import com.slaylauncher.ui.components.MarqueeText
import com.slaylauncher.ui.guide.GuideKeys
import com.slaylauncher.ui.screens.NestedNavKey
import com.slaylauncher.ui.screens.NormalNavKey
import com.slaylauncher.ui.screens.content.elements.CommonVersionInfoLayout
import com.slaylauncher.ui.screens.content.elements.PlayerFace
import com.slaylauncher.ui.screens.content.elements.VersionIconImage
import com.slaylauncher.ui.screens.content.home.HomeGrid
import com.slaylauncher.ui.screens.content.home.server.LocalServerCardQuickPlay
import com.slaylauncher.ui.screens.content.home.version.LocalHomeCardLauncher
import com.slaylauncher.ui.screens.content.home.version.LocalHomeCardVersionSettings
import com.slaylauncher.ui.theme.SlayPalette
import com.slaylauncher.ui.theme.slayPixelSurface
import com.slaylauncher.viewmodel.ScreenBackStackViewModel

@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    onLaunchGame: (Version?) -> Unit,
    onQuickPlayServer: (Version?, String) -> Unit,
    onOpenLink: (String) -> Unit,
    startGuideOnce: (GuideKeys.Keys) -> Unit,
) {
    LaunchedEffect(Unit) {
        startGuideOnce(GuideKeys.Main)
    }

    BaseScreen(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        val versions by VersionsManager.versions.collectAsStateWithLifecycle()
        val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
        val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()
        val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

        val toAccount: () -> Unit = {
            backStackViewModel.mainScreen.navigateTo(
                screenKey = NormalNavKey.AccountManager(FirstLoginMenu.NONE)
            )
        }
        val toVersions: () -> Unit = {
            backStackViewModel.mainScreen.removeAndNavigateTo(
                remove = NestedNavKey.VersionSettings::class,
                screenKey = NormalNavKey.VersionsManager
            )
        }
        val toVersionSettings: (Version) -> Unit = { version -> navigateToVersions(version) }

        CompositionLocalProvider(
            LocalUriHandler provides object : UriHandler {
                override fun openUri(uri: String) = onOpenLink(uri)
            }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val wide = maxWidth >= 600.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (wide) {
                        VersionRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(226.dp),
                            versions = versions,
                            currentVersion = currentVersion,
                            isRefreshing = isRefreshing,
                            onSelect = { VersionsManager.saveVersion(it) },
                            onLaunch = onLaunchGame,
                            onSettings = toVersionSettings,
                            onInstall = toVersions,
                            onAccount = toAccount,
                            account = account
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        LobbyHero(
                            modifier = Modifier.fillMaxWidth(),
                            version = currentVersion,
                            versions = versions,
                            onLaunch = { onLaunchGame(null) },
                            onSelect = { VersionsManager.saveVersion(it) },
                            onSettings = {
                                currentVersion?.let(toVersionSettings) ?: toVersions()
                            },
                            onManage = toVersions
                        )

                        BuildShelf(
                            modifier = Modifier.fillMaxWidth(),
                            versions = versions,
                            currentVersion = currentVersion,
                            onSelect = { VersionsManager.saveVersion(it) },
                            onLaunch = onLaunchGame,
                            onInstall = toVersions
                        )

                        QuickTiles(
                            modifier = Modifier.fillMaxWidth(),
                            onBuilds = toVersions,
                            onCatalog = { backStackViewModel.navigateToDownload() },
                            onServers = {
                                backStackViewModel.mainScreen.removeAndNavigateTo(
                                    removes = backStackViewModel.clearBeforeNavKeys,
                                    screenKey = NormalNavKey.Multiplayer
                                )
                            },
                            onSettings = {
                                backStackViewModel.mainScreen.removeAndNavigateTo(
                                    removes = backStackViewModel.clearBeforeNavKeys,
                                    screenKey = backStackViewModel.settingsScreen
                                )
                            }
                        )

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            CompositionLocalProvider(
                                LocalHomeCardLauncher provides onLaunchGame,
                                LocalHomeCardVersionSettings provides toVersionSettings,
                                LocalServerCardQuickPlay provides onQuickPlayServer
                            ) {
                                HomeGrid(
                                    state = rememberCardGridState(),
                                    isVisible = isVisible,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Horizon-style left rail: account and all installed versions. */
@Composable
private fun VersionRail(
    modifier: Modifier = Modifier,
    versions: List<Version>,
    currentVersion: Version?,
    isRefreshing: Boolean,
    onSelect: (Version) -> Unit,
    onLaunch: (Version?) -> Unit,
    onSettings: (Version) -> Unit,
    onInstall: () -> Unit,
    onAccount: () -> Unit,
    account: com.slaylauncher.game.account.Account?,
) {
    Column(
        modifier = modifier
            .slayPixelSurface(
                fill = SlayPalette.surface,
                cut = 8.dp,
                outline = Color(0x18FFFFFF),
                bottomBand = SlayPalette.bandBottomSoft
            )
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(22.dp),
                painter = painterResource(R.drawable.slay_logo),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.slay_lobby_versions),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = SlayPalette.fg
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = versions.size.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = SlayPalette.accent
            )
        }

        Spacer(Modifier.height(8.dp))

        BackgroundCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAccount),
            influencedByBackground = false,
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (account != null) {
                    PlayerFace(account = account, avatarSize = 32.dp)
                } else {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(R.drawable.ic_person_outlined),
                        contentDescription = null,
                        tint = SlayPalette.fgMuted
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    MarqueeText(
                        text = account?.username ?: stringResource(R.string.account_add_new_account),
                        style = MaterialTheme.typography.labelLarge
                    )
                    account?.let {
                        Text(
                            text = getAccountTypeName(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = SlayPalette.fgMuted
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(8.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(versions, key = { it.getVersionPath() }) { version ->
                VersionRailItem(
                    version = version,
                    selected = currentVersion?.getVersionPath() == version.getVersionPath(),
                    onSelect = { onSelect(version) },
                    onLaunch = { onLaunch(version) },
                    onSettings = { onSettings(version) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .slayPixelSurface(
                    fill = SlayPalette.accent,
                    cut = 6.dp,
                    topBand = SlayPalette.bandTop,
                    bottomBand = SlayPalette.bandBottom
                )
                .clickable(onClick = onInstall),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.slay_lobby_install),
                style = MaterialTheme.typography.labelLarge,
                color = SlayPalette.onAccent
            )
        }
    }
}

@Composable
private fun VersionRailItem(
    version: Version,
    selected: Boolean,
    onSelect: () -> Unit,
    onLaunch: () -> Unit,
    onSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .slayPixelSurface(
                fill = if (selected) SlayPalette.accentSoft else Color(0x0BFFFFFF),
                cut = 6.dp,
                outline = if (selected) SlayPalette.accent else Color(0x10FFFFFF)
            )
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VersionIconImage(modifier = Modifier.size(30.dp), version = version)
            Spacer(Modifier.width(7.dp))
            Column(modifier = Modifier.weight(1f)) {
                MarqueeText(
                    text = version.getVersionName(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                )
                MarqueeText(
                    text = version.getVersionSummary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SlayPalette.fgMuted
                )
            }
            IconButton(modifier = Modifier.size(28.dp), onClick = onLaunch) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                    contentDescription = stringResource(R.string.main_launch_game),
                    tint = SlayPalette.accent
                )
            }
            IconButton(modifier = Modifier.size(28.dp), onClick = onSettings) {
                Icon(
                    modifier = Modifier.size(15.dp),
                    painter = painterResource(R.drawable.ic_settings_filled),
                    contentDescription = stringResource(R.string.versions_manage_settings),
                    tint = SlayPalette.fgMuted
                )
            }
        }
    }
}

/** Millida/Horizon-style hero with the active build and the main PLAY action. */
@Composable
private fun LobbyHero(
    modifier: Modifier = Modifier,
    version: Version?,
    versions: List<Version>,
    onLaunch: () -> Unit,
    onSelect: (Version) -> Unit,
    onSettings: () -> Unit,
    onManage: () -> Unit,
) {
    var showVersions by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .slayPixelSurface(
                fill = Color(0xFF11240E),
                cut = 10.dp,
                outline = Color(0x30000000),
                topBand = Color(0x33FFFFFF),
                bottomBand = Color(0x66000000)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.size(30.dp),
                    painter = painterResource(R.drawable.slay_logo),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SLAYLAUNCHER",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.02).sp
                        ),
                        color = SlayPalette.fg
                    )
                    Text(
                        text = "MINECRAFT JAVA  •  ANDROID",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlayPalette.accent
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (version != null) {
                    VersionIconImage(modifier = Modifier.size(44.dp), version = version)
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .slayPixelSurface(fill = SlayPalette.surface2, cut = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_rocket_launch_filled),
                            contentDescription = null,
                            tint = SlayPalette.fgMuted
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.slay_lobby_active_build),
                        style = MaterialTheme.typography.labelSmall,
                        color = SlayPalette.accent
                    )
                    MarqueeText(
                        text = version?.getVersionName()
                            ?: stringResource(R.string.versions_manage_no_versions),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    version?.let {
                        MarqueeText(
                            text = it.getVersionSummary(),
                            style = MaterialTheme.typography.labelSmall,
                            color = SlayPalette.fgMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .height(46.dp)
                        .weight(1.1f)
                        .slayPixelSurface(
                            fill = SlayPalette.accent,
                            cut = 7.dp,
                            topBand = SlayPalette.bandTop,
                            bottomBand = SlayPalette.bandBottom,
                            dots = true
                        )
                        .clickable(onClick = onLaunch),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_arrow_filled),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = SlayPalette.onAccent
                        )
                        Text(
                            text = stringResource(R.string.slay_tab_play),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = SlayPalette.onAccent
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .height(46.dp)
                        .weight(1f)
                        .slayPixelSurface(
                            fill = SlayPalette.surface2,
                            cut = 7.dp,
                            outline = Color(0x24FFFFFF),
                            bottomBand = SlayPalette.bandBottomSoft
                        )
                        .clickable { showVersions = true }
                        .onGloballyPositioned { anchor = it },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MarqueeText(
                            modifier = Modifier.weight(1f),
                            text = version?.getVersionName()
                                ?: stringResource(R.string.slay_lobby_select_version),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = SlayPalette.accent
                        )
                    }
                }

                VersionListMenu(
                    show = showVersions,
                    versions = versions,
                    menuAnchor = anchor,
                    version = version,
                    onLaunchGame = { onSelect(it) },
                    onDismissRequest = { showVersions = false }
                )

                Spacer(Modifier.width(6.dp))

                HeroIconButton(
                    icon = R.drawable.ic_settings_filled,
                    text = R.string.versions_manage_settings,
                    onClick = onSettings
                )
                Spacer(Modifier.width(4.dp))
                HeroIconButton(
                    icon = R.drawable.ic_folder_filled,
                    text = R.string.versions_manage_install_new,
                    onClick = onManage
                )
            }
        }
    }
}

@Composable
private fun HeroIconButton(icon: Int, text: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .slayPixelSurface(
                fill = SlayPalette.surface2,
                cut = 7.dp,
                outline = Color(0x24FFFFFF),
                bottomBand = SlayPalette.bandBottomSoft
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(text),
            modifier = Modifier.size(20.dp),
            tint = SlayPalette.fg
        )
    }
}

@Composable
private fun VersionListMenu(
    show: Boolean,
    versions: List<Version>,
    menuAnchor: LayoutCoordinates?,
    version: Version?,
    onLaunchGame: (Version) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val bounds = menuAnchor?.boundsInParent()
    DropdownMenu(
        expanded = show && menuAnchor != null && versions.isNotEmpty(),
        onDismissRequest = onDismissRequest,
        modifier = Modifier.width(260.dp),
        offset = DpOffset(
            x = with(LocalDensity.current) { (bounds?.left ?: 0f).toDp() },
            y = with(LocalDensity.current) { (-(bounds?.height ?: 0f)).toDp() } - 4.dp
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        versions.forEach { item ->
            DropdownMenuItem(
                text = {
                    CommonVersionInfoLayout(version = item, iconSize = 26.dp)
                },
                onClick = {
                    if (version != item) onLaunchGame(item)
                    onDismissRequest()
                }
            )
        }
    }
}

/** Millida-style horizontal build shelf. */
@Composable
private fun BuildShelf(
    modifier: Modifier = Modifier,
    versions: List<Version>,
    currentVersion: Version?,
    onSelect: (Version) -> Unit,
    onLaunch: (Version?) -> Unit,
    onInstall: () -> Unit,
) {
    Column(modifier = modifier.padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.slay_lobby_builds),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = SlayPalette.fg
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.slay_lobby_versions),
                style = MaterialTheme.typography.labelSmall,
                color = SlayPalette.fgMuted
            )
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(versions, key = { "shelf_" + it.getVersionPath() }) { version ->
                Box(
                    modifier = Modifier
                        .size(width = 150.dp, height = 76.dp)
                        .slayPixelSurface(
                            fill = if (currentVersion?.getVersionPath() == version.getVersionPath()) {
                                SlayPalette.accentSoft
                            } else {
                                SlayPalette.surface
                            },
                            cut = 6.dp,
                            outline = if (currentVersion?.getVersionPath() == version.getVersionPath()) {
                                SlayPalette.accent
                            } else {
                                Color(0x1AFFFFFF)
                            },
                            bottomBand = SlayPalette.bandBottomSoft
                        )
                        .clickable { onSelect(version) }
                ) {
                    Column(modifier = Modifier.padding(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VersionIconImage(modifier = Modifier.size(24.dp), version = version)
                            Spacer(Modifier.width(5.dp))
                            MarqueeText(
                                modifier = Modifier.weight(1f),
                                text = version.getVersionName(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MarqueeText(
                                modifier = Modifier.weight(1f),
                                text = version.getVersionSummary(),
                                style = MaterialTheme.typography.labelSmall,
                                color = SlayPalette.fgMuted
                            )
                            IconButton(
                                modifier = Modifier.size(24.dp),
                                onClick = { onLaunch(version) }
                            ) {
                                Icon(
                                    modifier = Modifier.size(14.dp),
                                    painter = painterResource(R.drawable.ic_play_arrow_filled),
                                    contentDescription = stringResource(R.string.main_launch_game),
                                    tint = SlayPalette.accent
                                )
                            }
                        }
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .size(width = 92.dp, height = 76.dp)
                        .slayPixelSurface(
                            fill = Color(0x0BFFFFFF),
                            cut = 6.dp,
                            outline = Color(0x1AFFFFFF)
                        )
                        .clickable(onClick = onInstall),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            tint = SlayPalette.accent
                        )
                        Text(
                            text = stringResource(R.string.slay_lobby_install),
                            style = MaterialTheme.typography.labelSmall,
                            color = SlayPalette.fgMuted
                        )
                    }
                }
            }
        }
    }
}

/** Millida hub tiles for fast navigation. */
@Composable
private fun QuickTiles(
    modifier: Modifier = Modifier,
    onBuilds: () -> Unit,
    onCatalog: () -> Unit,
    onServers: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = modifier.padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        QuickTile(Modifier.weight(1f), R.drawable.ic_assignment_filled, R.string.slay_tab_builds, onBuilds)
        QuickTile(Modifier.weight(1f), R.drawable.ic_download_2_filled, R.string.slay_tab_catalog, onCatalog)
        QuickTile(Modifier.weight(1f), R.drawable.ic_group_filled, R.string.slay_tab_servers, onServers)
        QuickTile(Modifier.weight(1f), R.drawable.ic_settings_filled, R.string.generic_setting, onSettings)
    }
}

@Composable
private fun QuickTile(
    modifier: Modifier = Modifier,
    icon: Int,
    label: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(58.dp)
            .slayPixelSurface(
                fill = SlayPalette.surface,
                cut = 6.dp,
                outline = Color(0x1AFFFFFF),
                bottomBand = SlayPalette.bandBottomSoft
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                modifier = Modifier.size(17.dp),
                painter = painterResource(icon),
                contentDescription = null,
                tint = SlayPalette.accent
            )
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
