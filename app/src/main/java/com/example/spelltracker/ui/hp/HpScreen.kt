package com.example.spelltracker.ui.hp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.R
import com.example.spelltracker.data.HitDie
import com.example.spelltracker.ui.common.swipeableNavigation
import com.example.spelltracker.ui.theme.AppColors
import kotlinx.coroutines.flow.collectLatest

/**
 * Экран HP / Hit Dice (Этап HP).
 *
 * Назначение:
 *   - Отслеживание текущих/максимальных/временных HP (PHB правила).
 *   - Управление пулом Hit Dice: total/spent/die/conMod.
 *   - Кнопки длинного/короткого отдыха в TopAppBar (shortRest просто
 *     пока не вызывается из этого экрана — нет смысла, кубики тратятся
 *     через явный диалог «Потратить»; long rest восстанавливает всё).
 *
 * Визуальная иерархия:
 *   1. Карточка «Здоровье» — крупная золотая цифра текущего HP,
 *      под ней «N / max», ниже отдельной строкой temp HP (если > 0).
 *   2. Карточка «Кость здоровья» — total/available, тип кубика,
 *      conMod, кнопка «Потратить».
 *
 * Навигация: открывается свайпом влево с HomeScreen, назад через
 * стрелку в TopAppBar (см. [com.example.spelltracker.ui.nav.Routes.HP]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HpScreen(
    viewModel: HpViewModel,
    onBack: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showHpEditDialog by remember { mutableStateOf(false) }
    var showHitDiceEditDialog by remember { mutableStateOf(false) }
    var showHitDiceSpendDialog by remember { mutableStateOf(false) }

    // Локализованные тексты для диалогов и снекбаров — резолвим здесь,
    // внутри Composable. stringResource нельзя вызывать внутри LaunchedEffect.
    val emptyHint         = stringResource(R.string.hp_empty_hint)
    val noMaxHp           = stringResource(R.string.hit_dice_no_max_hp)
    val longRestDone      = stringResource(R.string.hp_long_rest_done_toast)
    val hitDiceSnackbar   = stringResource(R.string.hp_snackbar_hit_dice_spent)

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            val msg = when (event) {
                HpEvent.LongRest -> longRestDone
                is HpEvent.HitDiceSpent -> {
                    // Этап HP v2: Snackbar показывает реальные броски,
                    // не один обобщённый множитель.
                    val rollsStr = event.rolls.joinToString(", ")
                    // Один общий формат с 5 плейсхолдерами:
                    // %1$d healed, %2$d dieSize, %3$s rolls,
                    // %4$+d conTotal, %5$d count.
                    String.format(
                        hitDiceSnackbar,
                        event.healed,
                        event.dieSize,
                        rollsStr,
                        event.conTotal,
                        event.count,
                    )
                }
                HpEvent.HitDiceBlockedNoMaxHp -> noMaxHp
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.hp_title),
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
                actions = {
                    // Длинный отдых теперь живёт в bottom-bar `RestButtonsBar`,
                    // как на HomeScreen (единая стилистика и расположение).
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    // Этап HP: дизайн в едином стиле HomeScreen —
                    // TopAppBar на тёмном BgDark без контрастного
                    // «фиолетового прямоугольника» сверху.
                    containerColor = AppColors.BgDark,
                    titleContentColor = AppColors.TextWhite,
                    navigationIconContentColor = AppColors.TextWhite,
                    actionIconContentColor = AppColors.Gold,
                ),
            )
        },
        snackbarHost = {
            // Системный Snackbar, как на HomeScreen — без ручного
            // override цветов.
            SnackbarHost(snackbarHostState)
        },
        containerColor = AppColors.BgDark,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Радиальный градиент в стиле HomeScreen — единый фон
                // для всех экранов Spell Tracker.
                .background(AppColors.ScreenGradient)
                // Этап HP+: карусельный свайп. ↔ Home (справа),
                // ↔ Characters (слева).
                .swipeableNavigation(
                    onSwipeLeft  = onSwipeLeft,
                    onSwipeRight = onSwipeRight,
                ),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                        HpHealthSection(
                            state = state,
                            onEditClick = { showHpEditDialog = true },
                            emptyHint = emptyHint,
                        )
                    }
                item {
                    HitDiceSection(
                        state = state,
                        onEditClick = { showHitDiceEditDialog = true },
                    )
                }
            }
            // Этап HP: единый bottom-bar с двумя кнопками отдыха — как
            // на HomeScreen. Short rest открывает диалог «Потратить
            // кубик», long rest сбрасывает HP и Hit Dice.
            RestButtonsBar(
                shortEnabled = state.hitDice.available > 0 && state.hp.maxHp > 0,
                onShortRest = { showHitDiceSpendDialog = true },
                onLongRest = { viewModel.longRest() },
            )
        }
    }

    if (showHpEditDialog) {
        HpEditDialog(
            hp = state.hp,
            onDismiss = { showHpEditDialog = false },
            onApply = { newMax, newCurrent, newTemp ->
                viewModel.setMaxHp(newMax)
                viewModel.setCurrentHp(newCurrent)
                viewModel.setTempHp(newTemp)
                showHpEditDialog = false
            },
        )
    }
    if (showHitDiceEditDialog) {
        HitDiceEditDialog(
            hd = state.hitDice,
            onDismiss = { showHitDiceEditDialog = false },
            onApply = { newHd ->
                viewModel.updateHitDice(newHd)
                showHitDiceEditDialog = false
            },
        )
    }
    if (showHitDiceSpendDialog) {
        HitDiceSpendDialog(
            hd = state.hitDice,
            maxHealable = (state.hp.maxHp - state.hp.currentHp).coerceAtLeast(0),
            onDismiss = { showHitDiceSpendDialog = false },
            onApply = { count, rolls ->
                viewModel.spendHitDice(count, rolls)
                showHitDiceSpendDialog = false
            },
        )
    }
}

// =============================================================
// Секция «Здоровье»
// =============================================================

@Composable
private fun HpHealthSection(
    state: HpState,
    onEditClick: () -> Unit,
    emptyHint: String,
) {
    Column {
        SectionTitle(R.string.hp_section_hp)
        Spacer(Modifier.height(10.dp))
        if (state.hp.maxHp == 0) {
            EmptyHintCard(text = emptyHint, onClick = onEditClick)
        } else {
            HpCard(state = state, onEditClick = onEditClick)
        }
    }
}

/**
 * Главная карточка «Здоровье». Клик по любой части → [onEditClick]
 * (открывает модалку редактирования). Большая золотая цифра current
 * визуально доминирует — пользователь видит HP при первом взгляде.
 */
@Composable
private fun HpCard(
    state: HpState,
    onEditClick: () -> Unit,
) {
    val hp = state.hp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Этап HP: единый стиль с HomeScreen — RoundedCornerShape(12.dp)
            // (как строки ячеек заклинаний), не 14.dp.
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBg)
            .border(1.dp, AppColors.Outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onEditClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Большое current HP — в подложке BgDark.copy(alpha=0.6f)
            // со скруглением 8.dp, как у `LevelInput` на HomeScreen.
            // Размер 36.sp — единый ритм с caster level в HomeScreen.
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .width(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.BgDark.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = hp.currentHp.toString(),
                    color = if (hp.currentHp == 0) AppColors.Error else AppColors.Gold,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.hp_max_label),
                    color = AppColors.TextGrey,
                    fontSize = 11.sp,
                )
                Text(
                    text = hp.maxHp.toString(),
                    color = AppColors.TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // Счётчик «current / max» — для быстрого чтения.
        Text(
            text = stringResource(R.string.hp_value_format, hp.currentHp, hp.maxHp),
            color = AppColors.TextGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        // Temp HP — отдельной строкой, если > 0.
        if (hp.tempHp > 0) {
            Spacer(Modifier.height(10.dp))
            TempHpRow(tempHp = hp.tempHp)
        }
    }
}

/**
 * Строка «Временные хиты» внутри карточки. Зелёно-золотая плашка,
 * чтобы отличать от обычных HP. Описание «поглощают урон первыми»
 * помогает игроку не путать temp с обычным исцелением.
 */
@Composable
private fun TempHpRow(tempHp: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Этап HP: единый стиль — RoundedCornerShape(8.dp), как у
            // LevelInput'а и подложек на HomeScreen.
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.CardBgLighter)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.hp_temp_label),
                color = AppColors.TextGrey,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.hp_temp_description),
                color = AppColors.TextGreyDark,
                fontSize = 10.sp,
            )
        }
        Text(
            text = stringResource(R.string.hp_temp_value_format, tempHp),
            color = AppColors.Gold,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// =============================================================
// Hit Dice
// =============================================================

@Composable
private fun HitDiceSection(
    state: HpState,
    onEditClick: () -> Unit,
) {
    Column {
        SectionTitle(R.string.hp_section_hit_dice)
        Spacer(Modifier.height(10.dp))
        HitDiceCard(
            hd = state.hitDice,
            onEditClick = onEditClick,
        )
    }
}

/**
 * Карточка «Кость здоровья». Содержит:
 *   - Заголовок: тип кубика (например, «1d8»)
 *   - Доступно / всего (например, «5 из 8»)
 *   - conMod (например, «+2»)
 *   - Кнопка «Потратить» — disabled, если available = 0 или maxHp = 0
 *   - Тап по карточке (вне кнопки) — открывает диалог редактирования
 */
@Composable
private fun HitDiceCard(
    hd: com.example.spelltracker.data.HitDiceState,
    onEditClick: () -> Unit,
) {
    val totalText = stringResource(
        R.string.hit_dice_value_total_format,
        hd.total,
        hd.die.name.lowercase(),
    )
    val availableText = stringResource(
        R.string.hit_dice_value_available_format,
        hd.available,
        hd.total,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Этап HP: единый стиль HomeScreen — RoundedCornerShape(12.dp).
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBg)
            .border(1.dp, AppColors.Outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onEditClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.hit_dice_label_die),
                    color = AppColors.TextGrey,
                    fontSize = 11.sp,
                )
                Text(
                    text = totalText,
                    color = AppColors.Gold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = availableText,
                    color = if (hd.available > 0) AppColors.TextWhite else AppColors.TextGreyDark,
                    fontSize = 12.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.hit_dice_label_con_mod),
                    color = AppColors.TextGrey,
                    fontSize = 11.sp,
                )
                Text(
                    text = stringResource(R.string.hit_dice_con_mod_format, hd.conMod),
                    color = AppColors.TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        // Кнопка «Потратить» удалена — она дублировала «Короткий
        // отдых» в bottom-bar. Теперь карточка показывает только
        // информацию (тип кубика, available, conMod), а трата
        // выполняется через единую точку входа — кнопку
        // «Короткий отдых» в нижней панели.
    }
}

/**
 * Нижний бар с двумя кнопками отдыха (Этап HP).
 *
 * Точная копия `RestButtonsBar` с HomeScreen — единая стилистика
 * приложения. Short rest — OutlinedButton (рамка золотом), Long rest —
 * FilledButton (залитая золотом). Фон бара `BgPurpleDeep`,
 * `windowInsetsPadding(WindowInsets.navigationBars)` чтобы кнопки
 * не уезжали за системный nav-bar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestButtonsBar(
    shortEnabled: Boolean,
    onShortRest: () -> Unit,
    onLongRest: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.BgPurpleDeep)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Короткий отдых — Outlined (рамка золотом).
        // Открывает диалог «Потратить кубик» (см. caller).
        OutlinedButton(
            onClick = onShortRest,
            enabled = shortEnabled,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, AppColors.GoldDeep),
        ) {
            Icon(
                Icons.Filled.LocalCafe,
                contentDescription = null,
                tint = if (shortEnabled) AppColors.Gold else AppColors.TextGreyDark,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.rest_short_title),
                color = if (shortEnabled) AppColors.Gold else AppColors.TextGreyDark,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontSize = 14.sp,
            )
        }
        // Длинный отдых — Filled (залитая золотом).
        Button(
            onClick = onLongRest,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Gold,
                contentColor = AppColors.BgDark,
            ),
        ) {
            Icon(
                Icons.Filled.Bed,
                contentDescription = null,
                tint = AppColors.BgDark,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.rest_long_title),
                color = AppColors.BgDark,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontSize = 14.sp,
            )
        }
    }
}

// =============================================================
// Пустая карточка-подсказка для случая maxHp = 0
// =============================================================

@Composable
private fun EmptyHintCard(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Этап HP: единый стиль — RoundedCornerShape(12.dp).
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBg)
            .border(1.dp, AppColors.Outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = AppColors.TextGrey,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

// =============================================================
// Общие helpers
// =============================================================

@Composable
private fun SectionTitle(@androidx.annotation.StringRes textRes: Int) {
    Text(
        text = stringResource(textRes).uppercase(),
        color = AppColors.PurpleLight,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}