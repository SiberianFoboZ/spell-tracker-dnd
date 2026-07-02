package com.example.spelltracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
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
 * Динамический рендер ячеек слота (Этап 21 — новый дизайн ячеек,
 * Этап 24 v3 — равномерное распределение по ширине).
 *
 * Правила (по спецификации пользователя):
 *   - `total in 1..5`   → **1 ряд** ячеек, распределённых равномерно по
 *                         доступной ширине (каждая ячейка занимает
 *                         равную долю через `weight(1f)` + `aspectRatio(1f)`,
 *                         с cap'ом 80.dp чтобы 1 ячейка не расползлась).
 *   - `total in 6..10`  → **2 ряда** ячеек, тот же формат: каждый ряд
 *                         распределён по ширине, 5 ячеек в ряду.
 *   - `total in 11..20` → **числовой диапазон** `remaining / total`,
 *                         ячейки не рисуются. Инвертированный счётчик
 *                         (remaining, а не used) — пользователю
 *                         привычнее видеть остаток, а не потраченное.
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
 * Арканумы (всегда 1 ячейка) идут напрямую через [SlotCell] с
 * `Modifier.size(48.dp)`.
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
    // Этап 24 v3: убрали фиксированные 48.dp / 38.dp — теперь ячейки
    // сами занимают равную долю ширины (weight + aspectRatio),
    // с cap'ом 80.dp на размер, чтобы 1 ячейка не растягивалась
    // на всю ширину родителя.
    //
    // NB: Modifier.weight() — член RowScope/ColumnScope, поэтому
    // сам модификатор нельзя вынести в val снаружи when'а —
    // приходится инлайнить в каждой Row/Column-ветке.

    when {
        total <= 0 -> Unit
        total <= 5 -> {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                repeat(total) { i ->
                    val isUsed = i >= (total - used)
                    SlotCell(
                        used = isUsed,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .sizeIn(maxWidth = 80.dp, maxHeight = 80.dp),
                    )
                }
            }
        }
        total <= 10 -> {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                for (rowIdx in 0 until 2) {
                    val start = rowIdx * 5
                    val end = (start + 5).coerceAtMost(total)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        for (i in start until end) {
                            val isUsed = i >= (total - used)
                            SlotCell(
                                used = isUsed,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .sizeIn(maxWidth = 80.dp, maxHeight = 80.dp),
                            )
                        }
                    }
                }
            }
        }
        else -> {
            // 11..20: числовой диапазон. Инвертированный счётчик
            // (remaining / total) — пользователю привычнее видеть
            // остаток, а не потраченное.
            val remaining = (total - used).coerceAtLeast(0)
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.CardBg)
                    .border(1.dp, AppColors.Outline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$remaining / $total",
                    color = AppColors.Gold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "осталось $remaining",
                    color = AppColors.TextGrey,
                    fontSize = 12.sp,
                )
            }
        }
    }
}