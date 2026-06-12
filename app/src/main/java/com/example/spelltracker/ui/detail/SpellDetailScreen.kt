package com.example.spelltracker.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.data.Spell
import com.example.spelltracker.ui.theme.AppColors

/**
 * Экран детальной карточки заклинания. Идентификатор приходит из
 * SavedStateHandle через NavType.LongType.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellDetailScreen(
    viewModel: SpellDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // первый запуск — берём id из SavedStateHandle
    LaunchedEffect(Unit) {
        // viewModel-у не передали id напрямую, поэтому возьмём его
        // из SavedStateHandle, который Compose-Nav кладёт в extras.
        // Здесь мы используем простой путь: id пробрасывается через
        // ViewModel-фабрику ниже. Но мы храним ссылку и берём через
        // application, чтобы не тащить абстракции сюда.
        // (см. AppNavigation — там фабрика передаёт id через extras)
        // для упрощения оставим прямой вызов load() из эффекта —
        // SavedStateHandle будет источником id (см. AppNavigation.kt).
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("", color = AppColors.TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = AppColors.TextWhite)
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
                        Text("Заклинание не найдено", color = AppColors.TextGrey, fontSize = 14.sp)
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
                        if (spell.description.isNotBlank()) {
                            TextBlock("Описание", spell.description)
                        }
                        if (spell.higherLevel.isNotBlank()) {
                            TextBlock("На высших уровнях", spell.higherLevel)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.togglePrepared() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isPrepared) AppColors.Purple else AppColors.Gold,
                                contentColor = if (state.isPrepared) AppColors.TextWhite else AppColors.BgDark,
                            ),
                        ) {
                            Icon(
                                imageVector = if (state.isPrepared) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (state.isPrepared) "В списке подготовленных" else "Отметить как подготовленное",
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
            if (spell.school.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = spell.school,
                    color = AppColors.Gold,
                    fontSize = 13.sp,
                )
            }
        }
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
            if (spell.castingTime.isNotBlank()) MetaRow("Время",   spell.castingTime)
            if (spell.range.isNotBlank())      MetaRow("Дистанция", spell.range)
            if (spell.components.isNotBlank()) MetaRow("Компоненты", spell.components)
            if (spell.duration.isNotBlank())   MetaRow("Длительность", spell.duration)
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
private fun TextBlock(title: String, body: String) {
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
                body,
                color = AppColors.TextWhite,
                fontSize = 13.sp,
            )
        }
    }
}

private fun spellLevelLabel(level: Int): String = when (level) {
    0 -> "Заговор"
    else -> "Уровень ${romanLevel(level)}"
}

private fun romanLevel(n: Int): String = when (n) {
    1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"
    5 -> "V"; 6 -> "VI"; 7 -> "VII"; 8 -> "VIII"; 9 -> "IX"
    else -> n.toString()
}
