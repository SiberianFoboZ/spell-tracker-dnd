package com.example.spelltracker.ui.common

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Утилита: горизонтальный свайп для навигации между экранами (Этап HP+).
 *
 * Линейка экранов Spell Tracker (для свайпов):
 *   Home ↔ HP ↔ Characters ↔ Settings ↔ Home
 *
 * Свайпы реализованы через [detectHorizontalDragGestures] — он
 * сосуществует с вертикальной прокруткой [LazyColumn] / `verticalScroll`,
 * потому что Compose автоматически отменяет горизонтальный жест,
 * когда пользователь ведёт палец преимущественно по вертикали.
 *
 * Использование — оборачиваем корень экрана:
 *
 * ```
 * Box(
 *     modifier = Modifier
 *         .swipeableNavigation(
 *             onSwipeLeft  = onOpenNext,
 *             onSwipeRight = onOpenPrev,
 *         ),
 * ) { /* содержимое экрана */ }
 * ```
 *
 * @param onSwipeLeft колбэк при свайпе влево (например, открыть следующий экран).
 * @param onSwipeRight колбэк при свайпе вправо (открыть предыдущий экран).
 * @param thresholdDp порог срабатывания в dp. По умолчанию 80dp —
 *                     достаточно, чтобы случайный «тычок» при скролле
 *                     не считался свайпом.
 */
@Composable
fun Modifier.swipeableNavigation(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    thresholdDp: androidx.compose.ui.unit.Dp = 80.dp,
): Modifier {
    val density = LocalDensity.current
    val thresholdPx = remember(density, thresholdDp) {
        with(density) { thresholdDp.toPx() }
    }
    return this.pointerInput(Unit) {
        var accumulated = 0f
        detectHorizontalDragGestures(
            onDragStart = { accumulated = 0f },
            onDragEnd = {
                when {
                    accumulated < -thresholdPx -> onSwipeLeft()
                    accumulated > thresholdPx  -> onSwipeRight()
                }
                accumulated = 0f
            },
            onDragCancel = { accumulated = 0f },
        ) { _, dragAmount -> accumulated += dragAmount }
    }
}