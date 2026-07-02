package com.example.spelltracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
 * Динамический рендер ячеек слота (Этап 21 — новый дизайн ячеек).
 *
 * Правила (по спецификации пользователя):
 *   - `total in 1..5`   → **1 ряд** обычных ячеек (`sizeDp = 48.dp`)
 *   - `total in 6..10`  → **2 ряда** уменьшенных ячеек (`sizeDp = 38.dp`,
 *                         ≈80% от 48.dp). 1-й ряд всегда полный (5 шт.),
 *                         2-й ряд — остаток (1..5 шт.).
 *   - `total in 11..20` → **числовой диапазон** «used / total», ячейки
 *                         не рисуются. Вместо них — заметная плашка,
 *                         чтобы ряд не «схлопывался» в пустоту.
 *
 * Цвет ячеек (логика «доступна / потрачена»):
 *   - `i <  total - used` → gold (доступна)
 *   - `i >= total - used` → серая (потрачена)
 *
 * Используется в [HomeScreen] в трёх местах с одинаковой семантикой:
 *   - обычные ступени заклинаний ([SpellSlotRow])
 *   - пакт-магия Колдуна   ([PactMagicRow])
 *   - пользовательские ячейки ([CustomSlotRow])
 *
 * Арканумы (всегда 1 ячейка) идут напрямую через [SlotCell].
 *
 * @param used     сколько ячеек потрачено (0..total)
 * @param total    сколько ячеек всего (1..20)
 * @param modifier модификатор снаружи (обычно `Modifier.weight(1f)`,
 *                 чтобы занять оставшуюся ширину ряда в [HomeScreen])
 */
@Composable
fun SlotCells(
    used: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = 6.dp
    val largeSize = 48.dp
    val smallSize = 38.dp  // ≈80% от largeSize (новый дизайн)

    when {
        total <= 0 -> Unit  // защита: storage clamp даёт ≥1, но если что —
                            // ничего не рисуем, а не валимся
        total <= 5 -> {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                repeat(total) { i ->
                    val isUsed = i >= (total - used)
                    SlotCell(used = isUsed, sizeDp = largeSize)
                }
            }
        }
        total <= 10 -> {
            // 2 ряда: первый всегда полный (5 шт.), второй — остаток
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                for (rowIdx in 0 until 2) {
                    val start = rowIdx * 5
                    val end = (start + 5).coerceAtMost(total)
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        for (i in start until end) {
                            val isUsed = i >= (total - used)
                            SlotCell(used = isUsed, sizeDp = smallSize)
                        }
                    }
                }
            }
        }
        else -> {
            // 11..20: числовой диапазон. Инвертированный счётчик
            // (remaining / total, а не used / total) — пользователю
            // привычнее видеть остаток, а не потраченное (отнимается,
            // а не прибавляется: «12 / 15» → «11 / 15» по клику).
            //
            // Без рамки/фона/паддинга — поле не должно быть больше
            // соседних рядов с ячейками, простой inline-текст.
            val remaining = (total - used).coerceAtLeast(0)
            Text(
                text = "$remaining / $total",
                color = AppColors.Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = modifier,
            )
        }
    }
}