package com.example.spelltracker.ui.characters

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.R
import com.example.spelltracker.data.Character
import com.example.spelltracker.ui.common.swipeableNavigation
import com.example.spelltracker.ui.theme.AppColors

/**
 * Экран «Персонажи» (Этап 22 — мульти-персонажи).
 *
 * Достигается свайпом влево из [com.example.spelltracker.ui.home.HomeScreen].
 * Содержит:
 *   - список всех персонажей (с подсветкой активного)
 *   - тап по строке → переключение на этого персонажа и возврат назад
 *   - FAB «+» внизу → диалог создания нового персонажа
 *   - иконка активного персонажа (галочка справа)
 *
 * Пустое состояние невозможно — минимум один персонаж создаётся при
 * первой миграции (см. [com.example.spelltracker.data.SpellStorage]).
 *
 * Намеренно НЕ показываем здесь:
 *   - редактирование заклинаний / ячеек — это удел HomeScreen
 *   - удаление / переименование — пока только создание и переключение.
 *     При необходимости добавим long-press → контекстное меню.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(
    viewModel: CharactersViewModel,
    onBack: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
) {
    val characters by viewModel.characters.collectAsState()
    val activeId by viewModel.activeCharacterId.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    // Этап 22 v2: модалка редактирования (long-press по строке).
    // Текст имени хранится в [inProgressNames] — переживает закрытие
    // модалки, чтобы при повторном открытии не терять несохранённый ввод.
    var editingCharacter by remember { mutableStateOf<Character?>(null) }
    var inProgressNames by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    // Диалог подтверждения удаления (из модалки).
    var deleteTarget by remember { mutableStateOf<Character?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.characters_title),
                        color = AppColors.TextWhite,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
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
        floatingActionButton = {
            // FAB «+» — создать нового персонажа
            IconButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AppColors.Gold),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.characters_create_fab_content_description),
                    tint = AppColors.BgDark,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
    ) { padding ->
        if (characters.isEmpty()) {
            // На практике не должно случаться (минимум 1 после миграции),
            // но покажем понятное сообщение, если что-то пошло не так.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .swipeableNavigation(
                        onSwipeLeft  = onSwipeLeft,
                        onSwipeRight = onSwipeRight,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.characters_empty),
                    color = AppColors.TextGrey,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .swipeableNavigation(
                        onSwipeLeft  = onSwipeLeft,
                        onSwipeRight = onSwipeRight,
                    ),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(characters, key = { it.id }) { character ->
                    CharacterRow(
                        character = character,
                        isActive = character.id == activeId,
                        onClick = {
                            viewModel.setActive(character.id)
                            onBack()
                        },
                        // Long-press → открыть модалку редактирования.
                        // Текст подтянется из inProgressNames (если есть)
                        // или из текущего имени персонажа.
                        onLongPress = { editingCharacter = character },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCharacterDialog(
            onConfirm = { name ->
                viewModel.addCharacter(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    // Этап 22 v2: модалка редактирования персонажа.
    // - Текст имени поднимается в parent (inProgressNames) — переживает
    //   закрытие модалки.
    // - При открытии фокус сразу на поле + всплывает клавиатура.
    // - «Удалить» открывает диалог подтверждения (deleteTarget).
    // - «Сохранить» коммитит имя + закрывает модалку.
    // - Свайп вниз / крестик / back → просто закрыть (текст сохраняется).
    editingCharacter?.let { target ->
        val canDelete = characters.size > 1
        val currentName = inProgressNames[target.id] ?: target.name
        EditCharacterSheet(
            initialName = currentName,
            canDelete = canDelete,
            onNameChange = { newName ->
                inProgressNames = inProgressNames + (target.id to newName)
            },
            onSave = {
                val toSave = inProgressNames[target.id] ?: target.name
                viewModel.renameCharacter(target.id, toSave)
                inProgressNames = inProgressNames - target.id
                editingCharacter = null
            },
            onDelete = {
                // Не закрываем модалку сразу — сначала подтверждение.
                deleteTarget = target
            },
            onDismiss = {
                // Просто закрываем модалку; inProgressNames сохраняет текст
                // для следующего открытия.
                editingCharacter = null
            },
        )
    }

    deleteTarget?.let { target ->
        DeleteCharacterDialog(
            characterName = target.name,
            onConfirm = {
                viewModel.deleteCharacter(target.id)
                // Чистим недосохранённый текст + закрываем всё.
                inProgressNames = inProgressNames - target.id
                deleteTarget = null
                editingCharacter = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

/**
 * Отображаемое имя персонажа. Если в storage лежит пустая строка
 * (миграция Этапа 22, либо специально очищенное имя) — подставляем
 * локализованный fallback `R.string.characters_default_name`.
 *
 * Сама строка из storage не модифицируется: подстановка происходит
 * только в момент отрисовки, чтобы при смене языка имя сразу
 * обновилось без миграции данных.
 */
@Composable
private fun characterDisplayName(raw: String): String =
    raw.ifBlank { stringResource(R.string.characters_default_name) }

/**
 * Одна строка персонажа в списке. Активный — с золотой обводкой и
 * галочкой справа; неактивный — нейтральный серый бордюр.
 *
 * Жесты (Этап 22 v2):
 *   - тап → переключение на этого персонажа + возврат на HomeScreen
 *   - долгое нажатие → открыть модалку редактирования (имя + удалить)
 *
 * Дропдаун с тремя точками убран — в него было неудобно попадать
 * (точечные иконки мелкие, на мобиле промахи). Long-press проще
 * и привычнее (тот же паттерн, что у пользовательских ячеек на
 * главном экране, см. LONG_PRESS_TIMEOUT_MS в HomeScreen).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterRow(
    character: Character,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val borderColor = if (isActive) AppColors.Gold else AppColors.Outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBg)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Иконка «персонаж»
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.PurpleDeep),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = AppColors.Gold,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        // Имя (растягивается)
        Text(
            text = characterDisplayName(character.name),
            color = if (isActive) AppColors.Gold else AppColors.TextWhite,
            fontSize = 16.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        // Галочка для активного
        if (isActive) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.characters_active_content_description),
                tint = AppColors.Gold,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Диалог создания нового персонажа. Просто TextField + кнопки.
 */
@Composable
private fun CreateCharacterDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.characters_create_dialog_title),
                color = AppColors.TextWhite,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.characters_name_field_placeholder), color = AppColors.TextGreyDark) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.TextWhite,
                    unfocusedTextColor = AppColors.TextWhite,
                    focusedBorderColor = AppColors.PurpleLight,
                    unfocusedBorderColor = AppColors.Outline,
                    cursorColor = AppColors.Gold,
                ),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Gold,
                    contentColor = AppColors.BgDark,
                    disabledContainerColor = AppColors.CardBg,
                    disabledContentColor = AppColors.TextGreyDark,
                ),
            ) { Text(stringResource(R.string.common_create), fontWeight = FontWeight.Bold) }
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
 * Этап 22 v2: модалка редактирования персонажа (ModalBottomSheet).
 *
 * Заменяет старый [RenameCharacterDialog] (AlertDialog). В модалке:
 *   - поле имени с авто-фокусом и всплывающей клавиатурой при открытии
 *   - «Сохранить» — коммитит имя через [onSave]
 *   - «Удалить» — открывает диалог подтверждения через [onDelete]
 *     (НЕ закрывает модалку сразу; если подтверждения нет — ничего)
 *   - крестик / свайп вниз / back — закрывает через [onDismiss]
 *
 * Текст имени контролируется **снаружи** (parent держит
 * `inProgressNames`), чтобы при свайпе/закрытии без сохранения
 * ввод не терялся. Здесь же только прокидываем [initialName]
 * и [onNameChange].
 *
 * Если [canDelete] == false (это последний персонаж), кнопка
 * «Удалить» отображается как disabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCharacterSheet(
    initialName: String,
    canDelete: Boolean,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

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
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // ─── Заголовок + крестик ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.characters_edit_sheet_title),
                    color = AppColors.TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = AppColors.TextWhite,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // ─── Поле имени (авто-фокус + клавиатура) ────────
            OutlinedTextField(
                value = initialName,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.characters_name_label)) },
                placeholder = { Text(stringResource(R.string.characters_name_field_placeholder), color = AppColors.TextGreyDark) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.TextWhite,
                    unfocusedTextColor = AppColors.TextWhite,
                    focusedBorderColor = AppColors.PurpleLight,
                    unfocusedBorderColor = AppColors.Outline,
                    cursorColor = AppColors.Gold,
                ),
            )
            // Сразу при открытии: фокус на поле + всплыть клавиатуру.
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }

            Spacer(Modifier.height(16.dp))

            // ─── Сохранить ────────────────────────────────────
            Button(
                onClick = onSave,
                enabled = initialName.isNotBlank(),
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

            Spacer(Modifier.height(12.dp))

            // ─── Удалить (outlined, красная рамка) ────────────
            OutlinedButton(
                onClick = onDelete,
                enabled = canDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (canDelete) AppColors.Error else AppColors.TextGreyDark,
                ),
                border = BorderStroke(1.dp, if (canDelete) AppColors.Error else AppColors.Outline),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (canDelete) R.string.common_delete
                        else R.string.characters_delete_disabled,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Диалог подтверждения удаления персонажа. Удаление безвозвратно —
 * сносится blob `char_data_v22_${id}`, восстановить нельзя.
 */
@Composable
private fun DeleteCharacterDialog(
    characterName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.characters_delete_dialog_title),
                color = AppColors.TextWhite,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                stringResource(R.string.characters_delete_dialog_body, characterDisplayName(characterName)),
                color = AppColors.TextGrey,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.characters_delete_confirm),
                    color = AppColors.Error,
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