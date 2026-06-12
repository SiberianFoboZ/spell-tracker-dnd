package com.example.spelltracker.ui.home

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.data.Classes
import com.example.spelltracker.ui.theme.AppColors

/**
 * Главный экран. Содержит:
 *   - Заголовок и подпись
 *   - Панель эффективного caster level (большая золотая цифра)
 *   - 3×3 сетку карточек классов с полем ввода уровня
 *   - Секцию пакт-магии (показывается, если warlock > 0)
 *   - Список ячеек заклинаний 1..N с кнопками «−»/«+»
 *   - Нижние кнопки «Сброс» / «К заклинаниям»
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSpells: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ScreenGradient)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 8.dp, bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Header() }
                item { EffectiveLevelPanel(state.casterLevel) }
                item { ClassesGrid(viewModel) }

                if (state.warlockLevel > 0) {
                    item {
                        PactMagicRow(
                            state = state,
                            onUse = viewModel::usePactSlot,
                            onRestore = viewModel::restorePactSlot,
                        )
                    }
                }

                item { SlotsSection(state, viewModel) }
            }

            BottomBar(
                onReset = { showResetDialog = true },
                onOpenSpells = onOpenSpells,
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сбросить все ячейки?") },
            text = { Text("Все использованные ячейки заклинаний и пакт-магии обнулятся. Уровни классов сохранятся.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAllUsed()
                    showResetDialog = false
                }) { Text("Сбросить", color = AppColors.Gold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена", color = AppColors.TextGrey)
                }
            },
            containerColor = AppColors.CardBg,
            titleContentColor = AppColors.TextWhite,
            textContentColor = AppColors.TextGrey,
        )
    }
}

@Composable
private fun Header() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Spell Tracker",
            color = AppColors.TextWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Мультикласс по правилам PHB",
            color = AppColors.TextGrey,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun EffectiveLevelPanel(casterLevel: Int) {
    val animated by animateIntAsState(
        targetValue = casterLevel,
        animationSpec = tween(durationMillis = 350),
        label = "casterLevel",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(AppColors.Purple, AppColors.PurpleDeep)))
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "УРОВЕНЬ ЗАКЛИНАТЕЛЯ",
                color = AppColors.Cream,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = animated.toString(),
                color = AppColors.Gold,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun ClassesGrid(viewModel: HomeViewModel) {
    Column {
        SectionTitle("Классы")
        Spacer(Modifier.height(10.dp))
        val rows = viewModel.classes().chunked(3)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { info ->
                    ClassCard(
                        info = info,
                        level = viewModel.state.value.classLevels[info.id] ?: 0,
                        onLevelChange = { viewModel.setClassLevel(info.id, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ClassCard(
    info: Classes.Info,
    level: Int,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasCasting = info.factor > 0.0
    val isMulticlass = level > 0 && hasCasting
    val bg = if (isMulticlass) AppColors.CardBgLighter else AppColors.CardBg
    Box(
        modifier = modifier
            .heightIn(min = 92.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                width = if (isMulticlass) 1.5.dp else 1.dp,
                color = if (isMulticlass) AppColors.PurpleLight else AppColors.Outline,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = info.name,
                color = AppColors.TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (hasCasting) "×${formatFactor(info)}" else "пзак",
                color = if (hasCasting) AppColors.Gold else AppColors.TextGreyDark,
                fontSize = 10.sp,
            )
            BasicTextField(
                value = level.toString(),
                onValueChange = { raw ->
                    val v = raw.filter { it.isDigit() }.take(2).toIntOrNull() ?: 0
                    onLevelChange(v.coerceIn(0, 20))
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = if (level > 0) AppColors.Gold else AppColors.TextGrey,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(AppColors.Gold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().height(32.dp),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(
                                color = AppColors.BgDark.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) { inner() }
                }
            )
        }
    }
}

private fun formatFactor(info: Classes.Info): String = when {
    info.factor >= 1.0 -> "1.0"
    info.roundUp -> "0.5↑"
    else -> "0.5"
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = AppColors.PurpleLight,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun PactMagicRow(
    state: HomeState,
    onUse: () -> Unit,
    onRestore: () -> Unit,
) {
    Column {
        SectionTitle("Пакт-магия")
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.BgDark)
                .border(1.dp, AppColors.GoldDeep, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Колдун: ${state.warlockLevel}",
                        color = AppColors.TextGrey, fontSize = 12.sp)
                    Text("${state.pactUsed} / ${state.pactSlots} ячеек ${state.pactSlotLevel}-го ур.",
                        color = AppColors.Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                StepButton(icon = Icons.Filled.Remove, enabled = state.pactUsed > 0, onClick = onRestore)
                Spacer(Modifier.width(8.dp))
                StepButton(icon = Icons.Filled.Add, enabled = state.pactUsed < state.pactSlots, onClick = onUse)
            }
        }
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) AppColors.GoldDeep else AppColors.Outline),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) AppColors.BgDark else AppColors.TextGreyDark,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SlotsSection(state: HomeState, viewModel: HomeViewModel) {
    Column {
        SectionTitle("Ячейки заклинаний")
        Spacer(Modifier.height(10.dp))
        if (state.slots.isEmpty()) {
            Text(
                "Нет доступных ячеек — укажите уровень хотя бы одного заклинателя.",
                color = AppColors.TextGrey,
                fontSize = 12.sp,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.slots.forEach { slot ->
                    SlotRow(
                        slot = slot,
                        onUse = { viewModel.useSlot(slot.level) },
                        onRestore = { viewModel.restoreSlot(slot.level) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SlotRow(slot: SlotInfo, onUse: () -> Unit, onRestore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBg)
            .border(1.dp, AppColors.Outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.PurpleDeep),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = romanLevel(slot.level),
                color = AppColors.Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${slot.used} / ${slot.total}",
            color = AppColors.TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        StepButton(icon = Icons.Filled.Remove, enabled = slot.used > 0, onClick = onRestore)
        Spacer(Modifier.width(8.dp))
        StepButton(icon = Icons.Filled.Add, enabled = slot.used < slot.total, onClick = onUse)
    }
}

private fun romanLevel(n: Int): String = when (n) {
    1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"
    5 -> "V"; 6 -> "VI"; 7 -> "VII"; 8 -> "VIII"; 9 -> "IX"
    else -> n.toString()
}

@Composable
private fun BottomBar(onReset: () -> Unit, onOpenSpells: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.BgPurpleDeep)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null,
                tint = AppColors.TextWhite, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Сброс", color = AppColors.TextWhite, maxLines = 1)
        }
        Button(
            onClick = onOpenSpells,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Gold,
                contentColor = AppColors.BgDark,
            ),
        ) {
            Icon(Icons.Filled.AutoStories, contentDescription = null,
                tint = AppColors.BgDark, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Заклинания", color = AppColors.BgDark,
                fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
