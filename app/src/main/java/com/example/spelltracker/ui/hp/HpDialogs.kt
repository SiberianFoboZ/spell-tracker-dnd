package com.example.spelltracker.ui.hp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.R
import com.example.spelltracker.data.HitDie
import com.example.spelltracker.data.HitDiceState
import com.example.spelltracker.data.HpState
import com.example.spelltracker.ui.theme.AppColors
import com.example.spelltracker.util.Xoroshiro128Plus

/**
 * Диалог редактирования HP (Этап HP).
 *
 * Логика:
 *   - Три поля: max / current / temp HP.
 *   - У каждого поля — пара кнопок ±1 и ±5 для быстрой коррекции.
 *   - Поле max задаёт потолок; current автоматически клампится в 0..max
 *     через [SpellStorage.setCurrentHp] (см. data-слой).
 *   - temp HP ведёт себя по правилам PHB (см. [SpellStorage.setTempHp]).
 *
 * Кнопка «Применить» атомарно записывает все три значения через
 * [SpellStorage]; «Отмена» — no-op.
 */
@Composable
fun HpEditDialog(
    hp: HpState,
    onDismiss: () -> Unit,
    onApply: (maxHp: Int, currentHp: Int, tempHp: Int) -> Unit,
) {
    var maxHp by remember { mutableStateOf(hp.maxHp.toString()) }
    var currentHp by remember { mutableStateOf(hp.currentHp.toString()) }
    var tempHp by remember { mutableStateOf(hp.tempHp.toString()) }

    val maxInt = maxHp.toIntOrNull()?.coerceIn(0, 9999) ?: 0
    val currentInt = currentHp.toIntOrNull()?.coerceIn(0, maxInt) ?: 0
    val tempInt = tempHp.toIntOrNull()?.coerceIn(0, 9999) ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.hp_edit_dialog_title),
                color = AppColors.TextWhite,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HpStepperRow(
                    label = stringResource(R.string.hp_edit_field_max),
                    value = maxInt,
                    onValueChange = {
                        maxHp = it.coerceIn(0, 9999).toString()
                    },
                )
                HpStepperRow(
                    label = stringResource(R.string.hp_edit_field_current),
                    value = currentInt,
                    // current клампится в 0..max здесь же, чтобы при
                    // увеличении max не приходилось перенабирать current.
                    onValueChange = {
                        currentHp = it.coerceIn(0, maxInt).toString()
                    },
                )
                HpStepperRow(
                    label = stringResource(R.string.hp_edit_field_temp),
                    value = tempInt,
                    onValueChange = {
                        tempHp = it.coerceIn(0, 9999).toString()
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(maxInt, currentInt, tempInt) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Gold,
                    contentColor = AppColors.BgDark,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    stringResource(R.string.hp_edit_apply),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.common_cancel),
                    color = AppColors.TextGrey,
                )
            }
        },
        containerColor = AppColors.CardBg,
        titleContentColor = AppColors.TextWhite,
        textContentColor = AppColors.TextGrey,
    )
}

/**
 * Одна строка диалога: подпись + поле ввода + пара кнопок ±1/±5.
 *
 * Использует BasicTextField + локальный state, синхронизированный
 * через remember+onValueChange. Пользователь может либо набивать
 * руками, либо жать ±-кнопки.
 */
@Composable
private fun HpStepperRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Column {
        Text(
            text = label,
            color = AppColors.TextGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // ±1 кнопки — узкие (компактные).
            StepperButton(
                symbol = "-1",
                onClick = { onValueChange(value - 1) },
                small = true,
            )
            // Поле ввода — растягивается между кнопками.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.BgDark)
                    .border(1.dp, AppColors.Outline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicTextField(
                    value = value.toString(),
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() }.take(4)
                        onValueChange(filtered.toIntOrNull() ?: 0)
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = AppColors.Gold,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(AppColors.Gold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            StepperButton(
                symbol = "+1",
                onClick = { onValueChange(value + 1) },
                small = true,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StepperButton(
                symbol = "-5",
                onClick = { onValueChange(value - 5) },
                small = true,
            )
            Spacer(Modifier.weight(1f))
            StepperButton(
                symbol = "+5",
                onClick = { onValueChange(value + 5) },
                small = true,
            )
        }
    }
}

/**
 * Узкая кнопка-степпер «±N». Золотая рамка, в стиле остальных
 * outlined-кнопок проекта (см. OutlinedButton в HomeScreen RestButtonsBar).
 */
@Composable
private fun StepperButton(
    symbol: String,
    onClick: () -> Unit,
    small: Boolean,
) {
    Box(
        modifier = Modifier
            .size(if (small) 40.dp else 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.CardBgLighter)
            .border(1.dp, AppColors.GoldDeep, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            color = AppColors.Gold,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Диалог редактирования Hit Dice (Этап HP).
 *
 * Поля:
 *   - Тип кубика: 4 кнопки D6 / D8 / D10 / D12
 *   - Total: ±кнопки + поле ввода (как у HP)
 *   - conMod: ±кнопки + поле ввода (в диапазоне -10..+10)
 *
 * spent не редактируется напрямую — он растёт через [HpViewModel.spendHitDice]
 * и падает на longRest. Если пользователь хочет «обнулить» spent вручную
 * (например, после долгого отдыха за пределами приложения), он может
 * через UI временно поставить total = spent (тогда available = 0),
 * или просто дождаться longRest.
 */
@Composable
fun HitDiceEditDialog(
    hd: HitDiceState,
    onDismiss: () -> Unit,
    onApply: (HitDiceState) -> Unit,
) {
    var die by remember { mutableStateOf(hd.die) }
    var total by remember { mutableStateOf(hd.total) }
    var conMod by remember { mutableStateOf(hd.conMod) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.hp_edit_dialog_title),
                color = AppColors.TextWhite,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Тип кубика — 4 кликабельные кнопки.
                Column {
                    Text(
                        text = stringResource(R.string.hit_dice_label_die),
                        color = AppColors.TextGrey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HitDie.entries.forEach { d ->
                            DieChip(
                                label = d.name.lowercase(),
                                selected = d == die,
                                onClick = { die = d },
                            )
                        }
                    }
                }
                // Total — ±кнопки + поле.
                IntStepperRow(
                    label = stringResource(R.string.hit_dice_label_total),
                    value = total,
                    onValueChange = { total = it.coerceIn(0, 200) },
                    range = 0..200,
                )
                // conMod — ±кнопки + поле (диапазон -10..+10).
                IntStepperRow(
                    label = stringResource(R.string.hit_dice_label_con_mod),
                    value = conMod,
                    onValueChange = { conMod = it.coerceIn(-10, 10) },
                    range = -10..10,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // spent не редактируется напрямую; клампим до total.
                    val safeSpent = hd.spent.coerceAtMost(total)
                    onApply(HitDiceState(total = total, spent = safeSpent, die = die, conMod = conMod))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Gold,
                    contentColor = AppColors.BgDark,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(stringResource(R.string.hp_edit_apply), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = AppColors.TextGrey)
            }
        },
        containerColor = AppColors.CardBg,
        titleContentColor = AppColors.TextWhite,
        textContentColor = AppColors.TextGrey,
    )
}

/**
 * Chip для выбора типа кубика. Активный — золотой, иначе серый.
 */
@Composable
private fun DieChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AppColors.Gold else AppColors.CardBgLighter)
            .border(
                1.dp,
                if (selected) AppColors.Gold else AppColors.Outline,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) AppColors.BgDark else AppColors.TextWhite,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/**
 * Универсальная строка «поле ввода + ±1/±5 кнопки» (Этап HP).
 *
 * Переиспользуется в обоих диалогах — для HP-полей и для Hit Dice
 * (total, conMod). Принимает [range] для клампа на месте.
 */
@Composable
private fun IntStepperRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
) {
    Column {
        Text(
            text = label,
            color = AppColors.TextGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StepperButton(symbol = "-1", onClick = { onValueChange(value - 1) }, small = true)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.BgDark)
                    .border(1.dp, AppColors.Outline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicTextField(
                    value = value.toString(),
                    onValueChange = { raw ->
                        // Поддерживаем отрицательные значения для conMod.
                        val cleaned = if (raw.startsWith("-")) {
                            "-" + raw.drop(1).filter { it.isDigit() }
                        } else {
                            raw.filter { it.isDigit() }
                        }.take(5)
                        onValueChange(cleaned.toIntOrNull() ?: 0)
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = AppColors.Gold,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(AppColors.Gold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            StepperButton(symbol = "+1", onClick = { onValueChange(value + 1) }, small = true)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StepperButton(symbol = "-5", onClick = { onValueChange(value - 5) }, small = true)
            Spacer(Modifier.weight(1f))
            StepperButton(symbol = "+5", onClick = { onValueChange(value + 5) }, small = true)
        }
        // Молчаливый параметр — чтобы компилятор не ругался на unused.
        @Suppress("UNUSED_EXPRESSION") range
    }
}

/**
 * Диалог «Потратить Hit Dice» (Этап HP, v2).
 *
 * Новая формула (PHB-faithful):
 *   heal = sum(rolls) + conMod * count
 *
 * Каждый кубик бросается отдельно через [Xoroshiro128Plus], результаты
 * складываются. Логика в [SpellStorage.spendHitDice].
 *
 * UX:
 *   - Поле «Сколько кубиков» (1..available)
 *   - Большая кнопка-кубик «Бросить» → бросает [count] кубиков сразу,
 *     результат отображается списком «3, 5, 7»
 *   - Превью: «Бросок: d8», список чисел, «+ CON × 3 = +6»,
 *     «Итого восстановлено: X HP»
 *   - Кнопка «Применить» — финальный шаг (а не «Потратить», который
 *     был в карточке и дублировал bottom-bar)
 *
 * Если [hd.available] = 0 или [maxHealable] = 0 — диалог всё равно
 * открывается, но «Применить» disabled.
 */
@Composable
fun HitDiceSpendDialog(
    hd: HitDiceState,
    maxHealable: Int,
    onDismiss: () -> Unit,
    onApply: (count: Int, rolls: List<Int>) -> Unit,
) {
    var countText by remember { mutableStateOf("1") }
    var rolls by remember { mutableStateOf<List<Int>>(emptyList()) }
    val maxCount = hd.available.coerceAtLeast(1)
    val count = countText.toIntOrNull()?.coerceIn(1, maxCount) ?: 1
    // Автосинхронизация списка бросков с [count] — если пользователь
    // уже бросил 5, а потом уменьшил счётчик до 3, лишние отбрасываем.
    // Если увеличил — дополняем нулями (чтобы длина всегда совпадала).
    val effectiveRolls = remember(count, rolls) {
        when {
            rolls.size == count -> rolls
            rolls.size > count -> rolls.take(count)
            else -> rolls + List(count - rolls.size) { 0 }
        }
    }
    val rolledSum = effectiveRolls.sum()
    val conTotal = hd.conMod * count
    val totalHeal = (rolledSum + conTotal).coerceAtLeast(count) // PHB: минимум 1 HP за каждый кубик
    val previewShown = rolls.isNotEmpty()
    val rollsListStr = effectiveRolls.joinToString(", ")
    val canApply = hd.available > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.hit_dice_spend_title),
                color = AppColors.TextWhite,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(
                        R.string.hit_dice_spend_body,
                        hd.die.maxValue + hd.conMod,
                        hd.die.maxValue,
                        hd.conMod,
                    ),
                    color = AppColors.TextGrey,
                    fontSize = 13.sp,
                )
                // Ряд «Тип кубика + Бросить».
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.BgDark)
                            .border(1.dp, AppColors.Outline, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "d${hd.die.maxValue}",
                            color = AppColors.Gold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    OutlinedButton(
                        // Бросает сразу [count] кубиков через Xoroshiro128+.
                        // PHB-логика: «каждый кубик отдельно».
                        onClick = {
                            rolls = List(count) {
                                Xoroshiro128Plus.instance.nextInt(1, hd.die.maxValue + 1)
                            }
                        },
                        enabled = hd.available > 0,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AppColors.GoldDeep),
                    ) {
                        Icon(
                            Icons.Filled.Casino,
                            contentDescription = stringResource(
                                R.string.hit_dice_roll_button_content_description,
                            ),
                            tint = AppColors.Gold,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.hit_dice_roll_button),
                            color = AppColors.Gold,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                // Поле количества кубиков.
                IntStepperRow(
                    label = stringResource(R.string.hit_dice_label_die),
                    value = count,
                    onValueChange = { countText = it.coerceIn(1, maxCount).toString() },
                    range = 1..maxCount,
                )
                // Превью — только после первого броска.
                if (previewShown) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.BgDark)
                            .border(1.dp, AppColors.Outline, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.hit_dice_rolls_label, hd.die.maxValue,
                            ),
                            color = AppColors.PurpleLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = rollsListStr,
                            color = AppColors.Gold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(
                                R.string.hit_dice_con_bonus_format, count, conTotal,
                            ),
                            color = AppColors.TextGrey,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = stringResource(
                                R.string.hit_dice_total_heal_format, totalHeal,
                            ),
                            color = AppColors.TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        if (maxHealable > 0 && totalHeal > maxHealable) {
                            Text(
                                text = "фактически: $maxHealable HP (потолок)",
                                color = AppColors.TextGreyDark,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (previewShown) onApply(count, effectiveRolls)
                },
                enabled = canApply && previewShown,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Gold,
                    contentColor = AppColors.BgDark,
                    disabledContainerColor = AppColors.Outline,
                    disabledContentColor = AppColors.TextGreyDark,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    stringResource(R.string.hit_dice_apply),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = AppColors.TextGrey)
            }
        },
        containerColor = AppColors.CardBg,
        titleContentColor = AppColors.TextWhite,
        textContentColor = AppColors.TextGrey,
    )
}