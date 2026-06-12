package com.example.spelltracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Тёмная цветовая схема Material 3 на базе [AppColors].
 * Приложение всегда работает в тёмной теме — осветлённая палитра
 * не предусмотрена (это магический гримуар, а не офисное приложение).
 */
private val DarkColors = darkColorScheme(
    primary           = AppColors.Purple,
    onPrimary         = AppColors.TextWhite,
    primaryContainer  = AppColors.PurpleLight,
    onPrimaryContainer= AppColors.BgDark,

    secondary         = AppColors.Gold,
    onSecondary       = AppColors.BgDark,
    secondaryContainer= AppColors.GoldDeep,
    onSecondaryContainer= AppColors.TextWhite,

    tertiary          = AppColors.PurpleLight,
    onTertiary        = AppColors.BgDark,

    background        = AppColors.BgDark,
    onBackground      = AppColors.TextWhite,

    surface           = AppColors.CardBg,
    onSurface         = AppColors.TextWhite,
    surfaceVariant    = AppColors.CardBgLighter,
    onSurfaceVariant  = AppColors.TextGrey,

    error             = AppColors.Error,
    onError           = AppColors.TextWhite,

    outline           = AppColors.Outline,
    outlineVariant    = AppColors.BgPurpleDeep,
)

/**
 * Корневой Composable темы. Применяется в `MainActivity.setContent`.
 * Форсирует тёмную тему независимо от системной.
 */
@Composable
fun SpellTrackerTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = AppTypography,
        content     = content,
    )
}
