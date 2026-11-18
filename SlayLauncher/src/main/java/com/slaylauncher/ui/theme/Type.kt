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

package com.slaylauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Compact, heavy, slightly tightened type — closer to modern game launchers. */
val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.02).sp),
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.02).sp),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.01).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.01).sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Bold),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontWeight = FontWeight.Medium),
        bodyMedium = base.bodyMedium.copy(fontWeight = FontWeight.Medium),
        bodySmall = base.bodySmall.copy(fontWeight = FontWeight.Medium)
    )
}
