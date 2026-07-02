package com.example.spelltracker.ui.customslot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.data.CustomSlot
import com.example.spelltracker.data.DieType
import com.example.spelltracker.data.RestType
import com.example.spelltracker.ui.theme.AppColors

/**
 * Форма параметров пользовательской ячейки (Этап 20).
 *
 * Используется **двумя** сценариями:
 *   - [com.example.spelltracker.ui.home.AddCustomSlotSheet] (создание)
 *   - [com.example.spelltracker.ui.customslot.EditCustomSlotScreen] (правка)
 *
 * Поля формы:
 *   - **Название** (`title`) — обязательно, не пустое
 *   - **Количество** (`total`) — степпер 1..20
 *   - **Кубик** (`die`) — пять чипов: d4, d6, d8, d10, d12
 *   - **Восстановление** (`restType`) — два чипа: Короткий / Длинный
 *
 * Форма **без собственного state-а** — все значения контролируются
 * вызывающим (slot + onChange). Это позволяет одному источнику правды
 * (вызывающий) жить наверху (в BottomSheet или ViewModel экрана
 * редактирования), а здесь — только рендер.
 *
 * @param slot текущее значение полей (для инициализации)
 * @param onChange вызывается при ЛЮБОМ изменении — вызывающий создаёт
 *                 `slot.copy(...)` и передаёт обратно
 * @param modifier опциональный модификатор снаружи
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomSlotForm(
    slot: CustomSlot,
    onChange: (CustomSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ─── Название ───────────────────────────────────────────
        OutlinedTextField(
            value = slot.title,
            onValueChange = { onChange(slot.copy(title = it)) },
            label = { Text("Название") },
            placeholder = { Text("Дыхание дракона") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppColors.TextWhite,
                unfocusedTextColor = AppColors.TextWhite,
                focusedBorderColor = AppColors.PurpleLight,
                unfocusedBorderColor = AppColors.Outline,
                cursorColor = AppColors.Gold,
            ),
        )

        // ─── Количество (степпер) ──────────────────────────────
        Column {
            Text(
                "Количество",
                color = AppColors.TextGrey,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepperButton(
                    icon = Icons.Filled.Remove,
                    description = "Уменьшить",
                    enabled = slot.total > 1,
                    onClick = { onChange(slot.copy(total = (slot.total - 1).coerceAtLeast(1))) },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.CardBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = slot.total.toString(),
                        color = AppColors.Gold,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                StepperButton(
                    icon = Icons.Filled.Add,
                    description = "Увеличить",
                    enabled = slot.total < 20,
                    onClick = { onChange(slot.copy(total = (slot.total + 1).coerceAtMost(20))) },
                )
            }
        }

        // ─── Кубик (d4..d12) ───────────────────────────────────
        Column {
            Text(
                "Кубик",
                color = AppColors.TextGrey,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DieType.entries.forEach { type ->
                    FilterChipMini(
                        label = type.label,
                        selected = slot.die == type,
                        onClick = { onChange(slot.copy(die = type)) },
                    )
                }
            }
        }

        // ─── Восстановление (Короткий / Длинный) ───────────────
        Column {
            Text(
                "Восстановление",
                color = AppColors.TextGrey,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestType.entries.forEach { type ->
                    FilterChipMini(
                        label = type.displayName,
                        selected = slot.restType == type,
                        onClick = { onChange(slot.copy(restType = type)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = AppColors.CardBg
    val border = AppColors.Outline
    val tint = if (enabled) AppColors.Gold else AppColors.TextGreyDark
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
