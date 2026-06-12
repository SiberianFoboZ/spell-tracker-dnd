package com.example.spelltracker.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Палитра приложения в стиле «магический фиолетовый + золото».
 *
 * Тёмный фон, фиолетовые акценты, золото для важных значений (текущий
 * caster level, кнопки действий). Кремовый и серые — для иерархии текста.
 */
object AppColors {
    // Фоны
    val BgDark        = Color(0xFF160828)
    val BgMid         = Color(0xFF5A2A8A)
    val BgPurpleDeep  = Color(0xFF3A1A5C)
    val PurpleDeep    = BgPurpleDeep  // алиас для использования вне background
    val CardBg        = Color(0xFF2A1545)
    val CardBgLighter = Color(0xFF3A2060)

    // Акценты
    val Purple        = Color(0xFF7B2FB4)
    val PurpleLight   = Color(0xFFAA64DC)
    val Gold          = Color(0xFFF4C430)
    val GoldDeep      = Color(0xFFB8901A)
    val Cream         = Color(0xFFF4E4C1)

    // Текст
    val TextWhite     = Color(0xFFF2EAFB)
    val TextGrey      = Color(0xFFB7A8C8)
    val TextGreyDark  = Color(0xFF7E6E92)

    // Состояния
    val Error         = Color(0xFFE05A7A)
    val Outline       = Color(0xFF5A3E80)

    /**
     * Радиальный градиент для фона экрана HomeScreen.
     * Тёмный центр → более светлый край с уклоном в фиолетовый.
     */
    val ScreenGradient: Brush = Brush.radialGradient(
        colors = listOf(BgMid, BgDark, BgPurpleDeep),
        radius = 1400f,
    )
}
