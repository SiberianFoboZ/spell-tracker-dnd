package com.example.spelltracker.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.spelltracker.ui.theme.AppColors

/**
 * Один визуальный блок ячейки (Этап 21 — новый дизайн ячеек).
 *
 * Вынесен из [HomeScreen], где раньше жил приватный `SpellSlotBlock`,
 * и параметризован по размеру: один и тот же блок используется
 *   - в обычном ряду (`sizeDp = 48.dp`) — для `total in 1..5`
 *   - в уменьшенном ряду (`sizeDp = 38.dp`, ≈80% от 48.dp) — для
 *     `total in 6..10`
 *   - как одиночный блок арканума (`sizeDp = 48.dp`, всегда `total = 1`)
 *
 * Анимации (сохранены из старого `SpellSlotBlock`):
 *   - `animateColorAsState` — плавная смена цвета фона/рамки (300 мс)
 *   - `animateDpAsState` — плавное появление/снятие тени (300 мс)
 *   - `Animatable` + `LaunchedEffect(used)` — короткая «вспышка» scale
 *     1.0 → 0.92 → 1.0 при переходе `used = false → true` (потрачено).
 *     При обратном переходе (long rest) — без scale-анимации, только
 *     плавная смена цвета/тени («затухание»).
 *
 * @param used   true, если ячейка потрачена (серый блок)
 * @param sizeDp сторона квадрата. По умолчанию 48.dp.
 * @param modifier опциональный модификатор снаружи
 */
@Composable
fun SlotCell(
    used: Boolean,
    sizeDp: Dp = 48.dp,
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
            .size(sizeDp)
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