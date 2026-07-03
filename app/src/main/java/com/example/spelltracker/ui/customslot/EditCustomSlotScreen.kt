package com.example.spelltracker.ui.customslot

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.R
import com.example.spelltracker.ui.theme.AppColors

/**
 * Экран редактирования пользовательской ячейки (Этап 20).
 *
 * Достигается через long press 3с на строке ячейки в `HomeScreen`,
 * оттуда — `nav.navigate("customslot/$id")`. На экране — та же форма,
 * что и в `AddCustomSlotSheet` (см. [CustomSlotForm]), плюс:
 *   - кнопка «Сохранить» (записывает изменения и возвращает назад)
 *   - кнопка «Удалить» (открывает диалог подтверждения; после удаления
 *     тоже возвращает назад)
 *
 * Если ячейка с переданным id не найдена (например, удалили в
 * параллельной сессии при deep-link) — показываем «Ячейка не найдена»
 * с кнопкой «Назад», форма не рисуется.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomSlotScreen(
    viewModel: EditCustomSlotViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.custom_slot_edit_screen_title),
                        color = AppColors.TextWhite,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.common_back),
                            tint = AppColors.TextWhite,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColors.BgPurpleDeep,
                ),
            )
        },
        containerColor = AppColors.BgDark,
    ) { padding ->
        val slot = state.slot
        if (slot == null) {
            // Ячейка не найдена
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (state.isFound) stringResource(R.string.common_loading)
                    else stringResource(R.string.custom_slot_not_found),
                    color = AppColors.TextGrey,
                    fontSize = 14.sp,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CustomSlotForm(
                    slot = slot,
                    onChange = { viewModel.update { _ -> it } },
                )

                Spacer(Modifier.height(8.dp))

                // ─── Сохранить ────────────────────────────
                Button(
                    onClick = {
                        viewModel.save()
                        onBack()
                    },
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
                        stringResource(R.string.common_save),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }

                // ─── Удалить ──────────────────────────────
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.Error,
                    ),
                    border = BorderStroke(1.dp, AppColors.Error),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(18.dp),
                    )
                    Text(
                        stringResource(R.string.common_delete),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ─── Диалог подтверждения удаления ───────────────────────
    if (showDeleteConfirm) {
        val title = state.slot?.title.orEmpty()
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.custom_slot_delete_dialog_title)) },
            text = {
                Text(
                    stringResource(R.string.custom_slot_delete_dialog_body, title)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete()
                    showDeleteConfirm = false
                    onBack()
                }) {
                    Text(
                        stringResource(R.string.custom_slot_delete_confirm),
                        color = AppColors.Error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel), color = AppColors.TextGrey)
                }
            },
            containerColor   = AppColors.CardBg,
            titleContentColor = AppColors.TextWhite,
            textContentColor  = AppColors.TextGrey,
        )
    }
}
