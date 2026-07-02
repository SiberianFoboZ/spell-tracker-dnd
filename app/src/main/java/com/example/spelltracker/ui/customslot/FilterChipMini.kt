package com.example.spelltracker.ui.customslot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.ui.theme.AppColors

/**
 * Компактный фильтр-чип в стиле приложения (Этап 9, использовался
 * в [com.example.spelltracker.ui.spells.SpellsScreen]; здесь
 * переиспользуется для выбора кубика и типа восстановления в
 * [CustomSlotForm] / [com.example.spelltracker.ui.home.AddCustomSlotSheet]
 * и [com.example.spelltracker.ui.customslot.EditCustomSlotScreen]).
 *
 * Визуально идентичен оригиналу из SpellsScreen:
 *   - выбран:  фон Gold,    текст BgDark, рамка Gold    — золотая подсветка
 *   - не выбран: фон CardBg, текст TextWhite, рамка Outline — нейтральный
 *
 * Сделан публичным внутри модуля (`internal`), чтобы не светить
 * внутренний UI-кит за пределы `:app`.
 */
@Composable
internal fun FilterChipMini(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) AppColors.Gold else AppColors.CardBg
    val fg = if (selected) AppColors.BgDark else AppColors.TextWhite
    val border = if (selected) AppColors.Gold else AppColors.Outline
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
