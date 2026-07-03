package com.example.spelltracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.R
import com.example.spelltracker.ui.theme.AppColors

/**
 * Экран настроек (Этап 26).
 *
 * Текущее содержимое — единственная секция «Язык интерфейса» с
 * двумя radio-пунктами (Русский / English). Тап по пункту вызывает
 * [onLanguageSelected], который должен:
 *   1. применить новую локаль (`AppCompatDelegate.setApplicationLocales`)
 *   2. пересоздать Activity (`activity.recreate()`), чтобы Compose
 *      подхватил обновлённые строки. Без recreate() локаль записывается,
 *      но UI остаётся на старом языке до следующего запуска приложения.
 *
 * Поскольку настройки пока всего одна, делаем минималистичный список
 * без BottomSheet'ов и без сложных секций.
 *
 * Контейнер фона — сплошной [AppColors.BgDark], как у остальных
 * «вторичных» экранов (Spells, Characters, EditCustomSlot). Это
 * визуально отделяет Settings от Home (с градиентом) и сигнализирует
 * «вспомогательный экран».
 *
 * Навигация: из Home через шестерёнку в TopAppBar → [Routes.SETTINGS].
 *
 * @param currentTag текущий BCP-47 тег языка ("ru" / "en"). Если
 *                   значение не распознано, ни один radio не отмечен.
 * @param onLanguageSelected колбэк выбора языка (см. выше про recreate).
 * @param onBack             колбэк «Назад» для TopAppBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTag: String,
    onLanguageSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
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
                    titleContentColor = AppColors.TextWhite,
                    navigationIconContentColor = AppColors.TextWhite,
                ),
            )
        },
        containerColor = AppColors.BgDark,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppColors.BgDark),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    LanguageSection(
                        currentTag = currentTag,
                        onSelect = onLanguageSelected,
                    )
                }
            }
        }
    }
}

/**
 * Секция «Язык интерфейса» — заголовок + карточка с двумя radio.
 * Иконка слева от каждого пункта = «галочка» (когда выбран) или
 * пустота. Семантика — текущая локаль.
 */
@Composable
private fun LanguageSection(currentTag: String, onSelect: (String) -> Unit) {
    Column {
        // Заголовок секции в стиле Material 3 (мелкий серый текст).
        Text(
            text = stringResource(R.string.lang_menu_title),
            color = AppColors.TextGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        // Карточка-контейнер для двух radio-пунктов.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.CardBg)
                .padding(vertical = 4.dp),
        ) {
            LanguageRow(
                label = stringResource(R.string.lang_ru),
                tag = "ru",
                selected = currentTag == "ru",
                onClick = { onSelect("ru") },
            )
            // Тонкий разделитель между пунктами.
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.Outline.copy(alpha = 0.3f)),
            )
            LanguageRow(
                label = stringResource(R.string.lang_en),
                tag = "en",
                selected = currentTag == "en",
                onClick = { onSelect("en") },
            )
        }
    }
}

/**
 * Одна строка выбора языка: иконка-globe + название + radio.
 * Тап по любой части строки вызывает [onClick] (radio-кнопка внутри
 * строки тоже кликабельна отдельно — это даёт чуть больший hit-target).
 */
@Composable
private fun LanguageRow(
    label: String,
    tag: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Language,
            contentDescription = null,
            tint = if (selected) AppColors.Gold else AppColors.TextGrey,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = label,
            color = if (selected) AppColors.Gold else AppColors.TextWhite,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        // Дополнительная визуальная индикация (radio-кружок) — для тех,
        // кто привык к radio-button-паттерну из системных настроек.
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = AppColors.Gold,
                unselectedColor = AppColors.Outline,
            ),
        )
    }
}