package com.example.spelltracker.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.R
import com.example.spelltracker.data.Spell
import com.example.spelltracker.data.SpellMenuConfig
import com.example.spelltracker.nameRes
import com.example.spelltracker.ui.theme.AppColors

/**
 * Экран детальной карточки заклинания.
 *
 * Отображает:
 *   • Заголовок: имя (русское + латинский транслит, если есть),
 *     уровень заклинания, школа, источник.
 *   • Мету: время накладывания, дистанция, компоненты (+материал),
 *     длительность, спасброски, классы, подклассы, расы.
 *   • Описание: HTML→[AnnotatedString] через [parseSpellHtml].
 *   • «На высших уровнях»: то же.
 *   • Кнопка «Подготовлено» / «Отметить подготовленным».
 *
 * Эволюция схемы: до v4 поля назывались `castingTime`, `range`,
 * `components`, `description`, `higherLevel`. После v4 — `timecast`,
 * `distance`, `*` (V/S/M + material*), `descriptionHtml`, `upperLevel`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellDetailScreen(
    viewModel: SpellDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = state.spell?.name.orEmpty(),
                        color = AppColors.TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppColors.BgDark)
        ) {
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Gold)
                    }
                }
                state.spell == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.detail_not_found),
                            color = AppColors.TextGrey,
                            fontSize = 14.sp,
                        )
                    }
                }
                else -> {
                    val spell = state.spell!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        HeaderCard(spell)
                        MetaCard(spell)
                        if (spell.descriptionHtml.isNotBlank()) {
                            HtmlBlock(stringResource(R.string.detail_section_description), parseSpellHtml(spell.descriptionHtml))
                        }
                        if (spell.upperLevel.isNotBlank()) {
                            HtmlBlock(stringResource(R.string.detail_section_upper_level), parseSpellHtml(spell.upperLevel))
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.togglePrepared() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isPrepared) AppColors.Purple else AppColors.Gold,
                                contentColor = if (state.isPrepared) AppColors.TextWhite else AppColors.BgDark,
                            ),
                        ) {
                            Icon(
                                imageVector = if (state.isPrepared) Icons.Filled.Bookmark
                                else Icons.Filled.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (state.isPrepared) stringResource(R.string.detail_button_unmark_prepared)
                                else stringResource(R.string.detail_button_mark_prepared),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(spell: Spell) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(AppColors.Purple, AppColors.PurpleDeep)))
            .padding(16.dp),
    ) {
        Column {
            Text(
                text = spellLevelLabel(spell.level),
                color = AppColors.Cream,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = spell.name,
                color = AppColors.TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            if (spell.nameEng.isNotBlank() && spell.nameEng != spell.name) {
                Text(
                    text = spell.nameEng,
                    color = AppColors.Cream,
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (spell.school.isNotBlank()) {
                    Text(
                        text = schoolLabel(spell.school),
                        color = AppColors.Gold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (spell.ritual) {
                    Spacer(Modifier.width(8.dp))
                    PillTag(stringResource(R.string.detail_pill_ritual))
                }
                if (spell.concentration) {
                    Spacer(Modifier.width(6.dp))
                    PillTag(stringResource(R.string.detail_pill_concentration))
                }
                if (spell.source.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    PillTag(spell.source)
                }
            }
        }
    }
}

@Composable
private fun PillTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.PurpleDeep)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = AppColors.Cream,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MetaCard(spell: Spell) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.CardBg)
            .border(1.dp, AppColors.Outline, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (spell.timecast.isNotBlank()) MetaRow(stringResource(R.string.detail_meta_casting_time), spell.timecast)
            if (spell.distance.isNotBlank()) MetaRow(stringResource(R.string.detail_meta_range), spell.distance)
            if (spell.duration.isNotBlank()) MetaRow(stringResource(R.string.detail_meta_duration), spell.duration)
            MetaRow(stringResource(R.string.detail_meta_components), componentsLabel(spell))

            val classesLine = classesLine(spell)
            if (classesLine.isNotBlank()) MetaRow(stringResource(R.string.detail_meta_classes), classesLine)

            val subclassesLine = spell.subclasses.split(',')
                .map(String::trim).filter(String::isNotEmpty).joinToString(", ")
            if (subclassesLine.isNotBlank()) MetaRow(stringResource(R.string.detail_meta_subclasses), subclassesLine)

            val racesLine = spell.races.split(',')
                .map(String::trim).filter(String::isNotEmpty).joinToString(", ")
            if (racesLine.isNotBlank()) MetaRow(stringResource(R.string.detail_meta_races), racesLine)

            if (spell.materialDesc.isNotBlank()) {
                MetaRow(
                    stringResource(R.string.detail_meta_material),
                    spell.materialDesc + if (spell.materialConsumed) stringResource(R.string.detail_material_consumed_suffix) else "",
                )
            }

            if (spell.savingThrows.isNotBlank()) MetaRow(stringResource(R.string.detail_meta_saving_throws), spellsSaveLabel(spell.savingThrows))
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            color = AppColors.PurpleLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(110.dp),
        )
        Text(
            text = value,
            color = AppColors.TextWhite,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun HtmlBlock(title: String, body: AnnotatedString) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.CardBg)
            .border(1.dp, AppColors.Outline, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Column {
            Text(
                title,
                color = AppColors.PurpleLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                color = AppColors.TextWhite,
                fontSize = 13.sp,
            )
        }
    }
}

// ─── helpers ─────────────────────────────────────────────────────────────

@Composable
private fun componentsLabel(spell: Spell): String {
    val parts = mutableListOf<String>()
    if (spell.componentV) parts += stringResource(R.string.component_V_label)
    if (spell.componentS) parts += stringResource(R.string.component_S_label)
    if (spell.componentM) parts += stringResource(R.string.component_M_label)
    return parts.joinToString(", ")
}

@Composable
private fun classesLine(spell: Spell): String =
    spell.classes.split(',')
        .mapNotNull { id ->
            val info = com.example.spelltracker.data.Classes.BY_ID[id] ?: return@mapNotNull null
            stringResource(info.nameRes())
        }
        .joinToString(", ")

@Composable
private fun schoolLabel(key: String): String =
    SpellMenuConfig.SCHOOLS.firstOrNull { it.key == key }
        ?.let { stringResource(it.labelRes) }
        ?: key

@Composable
private fun spellsSaveLabel(csv: String): String =
    csv.split(',').map(String::trim).filter(String::isNotEmpty)
        .joinToString(", ")

@Composable
private fun spellLevelLabel(level: Int): String = when (level) {
    0 -> stringResource(R.string.detail_label_cantrip)
    else -> stringResource(R.string.detail_label_level_with_roman, romanLevel(level))
}

private fun romanLevel(n: Int): String = when (n) {
    1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"
    5 -> "V"; 6 -> "VI"; 7 -> "VII"; 8 -> "VIII"; 9 -> "IX"
    else -> n.toString()
}
