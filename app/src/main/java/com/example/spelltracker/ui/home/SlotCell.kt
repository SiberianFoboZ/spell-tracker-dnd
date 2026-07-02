package com.example.spelltracker.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.spelltracker.ui.theme.AppColors

/**
 * Один визуальный блок ячейки (Этап 21 — дизайн ячеек).
 *
 * Вынесен из [HomeScreen], где раньше жил приватный `SpellSlotBlock`.
 *
 * Размер определяется **modifier'ом снаружи**, а не параметром:
 *   - фиксированный: `Modifier.size(48.dp)` — одиночные блоки (арканум)
 *   - flex по ширине: `Modifier.weight(1f).aspectRatio(1f).sizeIn(max = 80.dp)`
 *     — обычные ячейки в [SlotCells] (1..5 в ряд, 6..10 в 2 ряда).
 *     Так ячейки заполняют доступную ширину равномерно с одинаковым
 *     aspectRatio (квадрат) и cap'ом по максимуму (для случая 1 ячейки
 *     в широком контейнере — не дать ей расползтись на всю ширину).
 *
 * Анимации (сохранены из старого `SpellSlotBlock`):
 *   - `animateColorAsState` — плавная смена цвета фона/рамки (300 мс)
 *   - `animateDpAsState` — плавное появление/снятие тени (300 мс)
 *   - `Animatable` + `LaunchedEffect(used)` — короткая «вспышка» scale
 *     1.0 → 0.92 → 1.0 при переходе `used = false → true` (потрачено).
 *     При обратном переходе (long rest) — без scale-анимации, только
 *     плавная смена цвета/тени («затухание»).
 *
 * @param used true, если ячейка потрачена (серый блок)
 * @param modifier обязательно содержит размер — `Modifier.size(...)` или
 *              `Modifier.weight(1f).aspectRatio(1f).sizeIn(max = ...)`
 */
@Composable
fun SlotCell(
    used: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(used) {
        if (used) {
            scale.animateTo(0.92f, animationSpec = tween(140))
            scale.animateTo(1f, animationSpec = tween(180))
        } else {
            scale.snapTo(1f)
        }
    }

    val bg by animateColorAsState(
        targetValue = if (used) AppColors.SlotUsed else AppColors.Gold,
        animationSpec = tween(300),
        label = "slotBg",
    )
    val edge by animateColorAsState(
        targetValue = if (used) AppColors.SlotUsedEdge else AppColors.GoldDeep,
        animationSpec = tween(300),
        label = "slotEdge",
    )
    val elevation by animateDpAsState(
        targetValue = if (used) 0.dp else 3.dp,
        animationSpec = tween(300),
        label = "slotElev",
    )

    Box(
        modifier = modifier
            .scale(scale.value)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(8.dp),
                ambientColor = if (used) Color.Transparent else AppColors.Gold.copy(alpha = 0.4f),
                spotColor    = if (used) Color.Transparent else AppColors.Gold.copy(alpha = 0.4f),
            )
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, edge, RoundedCornerShape(8.dp)),
    )
}