package com.example.spelltracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.data.CustomSlot
import com.example.spelltracker.data.DieType
import com.example.spelltracker.data.RestType
import com.example.spelltracker.ui.customslot.CustomSlotForm
import com.example.spelltracker.ui.theme.AppColors

/**
 * Bottom sheet для **создания** пользовательской ячейки (Этап 20).
 *
 * Вызывается из `HomeScreen` при тапе на «+» в заголовке секции
 * «Пользовательские ячейки». Внутри — форма [CustomSlotForm]
 * (та же, что и в экране редактирования).
 *
 * Жизненный цикл:
 *  1. HomeScreen показывает sheet при `showAddCustomSlotSheet = true`
 *  2. Пользователь заполняет поля → локальный [slot] обновляется
 *  3. Тап «Сохранить» → [onSave] с финальным [CustomSlot] (id задан
 *     вызывающим — обычно `System.currentTimeMillis()`)
 *  4. Sheet сам закрывается по [onDismiss] после сохранения
 *
 * Sheet **не валидирует** `title.isNotBlank()` сам — просто блокирует
 * кнопку «Сохранить» через [enabled]. Это индикативнее, чем показ
 * ошибки под полем, и пользователь сразу видит причину.
 *
 * @param onDismiss вызывается при тапе «Закрыть», свайпе вниз или
 *                  нажатии back. Sheet сам гасит своё отображение
 *                  на стороне вызывающего.
 * @param onSave    вызывается при тапе «Сохранить» с полностью
 *                  заполненным [CustomSlot]. id берётся на стороне
 *                  вызывающего, чтобы не зависеть от системного
 *                  времени внутри UI-слоя.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomSlotSheet(
    onDismiss: () -> Unit,
    onSave: (CustomSlot) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Локальный черновик. `id = 0L` — заглушка, при сохранении
    // HomeScreen заменит на System.currentTimeMillis(). Хранить
    // id в форме необязательно, но data class CustomSlot требует
    // его как `val` — поэтому 0L.
    var slot by remember {
        mutableStateOf(
            CustomSlot(
                id = 0L,
                title = "",
                total = 3,
                used = 0,
                die = DieType.D6,
                restType = RestType.LONG,
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = AppColors.BgDark,
        contentColor = AppColors.TextWhite,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        scrimColor = Color.Black.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // ─── Заголовок + крестик ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Новая ячейка",
                    color = AppColors.TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Закрыть",
                        tint = AppColors.TextWhite,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // ─── Форма ───────────────────────────────────
            CustomSlotForm(
                slot = slot,
                onChange = { slot = it },
            )

            Spacer(Modifier.height(20.dp))

            // ─── Кнопка «Сохранить» ─────────────────────
            Button(
                onClick = { onSave(slot) },
                enabled = slot.title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Gold,
                    contentColor = AppColors.BgDark,
                    disabledContainerColor = AppColors.CardBg,
                    disabledContentColor = AppColors.TextGreyDark,
                ),
            ) {
                Text(
                    "Сохранить",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
