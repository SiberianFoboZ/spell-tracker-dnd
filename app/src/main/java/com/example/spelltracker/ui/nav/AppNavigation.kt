package com.example.spelltracker.ui.nav

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.spelltracker.LocaleStorage
import com.example.spelltracker.ui.characters.CharactersScreen
import com.example.spelltracker.ui.characters.CharactersViewModel
import com.example.spelltracker.ui.customslot.EditCustomSlotScreen
import com.example.spelltracker.ui.customslot.EditCustomSlotViewModel
import com.example.spelltracker.ui.detail.SpellDetailScreen
import com.example.spelltracker.ui.detail.SpellDetailViewModel
import com.example.spelltracker.ui.home.HomeScreen
import com.example.spelltracker.ui.home.HomeViewModel
import com.example.spelltracker.ui.settings.SettingsScreen
import com.example.spelltracker.ui.spells.SpellsScreen
import com.example.spelltracker.ui.spells.SpellsViewModel

/**
 * Единая точка входа в навигацию. Экраны:
 *
 *   home                   — главный экран с классами и ячейками
 *   spells                 — список заклинаний с фильтром
 *   spell/{id}             — детальная карточка одного заклинания
 *   customslot/{id}        — экран редактирования пользовательской
 *                            ячейки (Этап 20, long press 1.5с)
 *   characters             — экран мульти-персонажей (Этап 22,
 *                            свайп влево из HomeScreen)
 *   settings               — экран настроек (Этап 26, шестерёнка
 *                            в TopAppBar Home)
 */
@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as Application

    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        // Этап 22 v2: плавные горизонтальные переходы между экранами
        // (300 мс, easing — стандартный FastOutSlowIn). Семантика:
        //   push → новый экран въезжает справа, старый уезжает влево
        //   pop  → возврат слева, текущий уезжает вправо
        // Применяется ко всем экранам сразу, в т.ч. к свайпу Home → Characters.
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 300),
            ) + fadeIn(animationSpec = tween(durationMillis = 300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(durationMillis = 300),
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(durationMillis = 300),
            ) + fadeIn(animationSpec = tween(durationMillis = 300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 300),
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        },
    ) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = vm,
                onOpenSpells = { nav.navigate(Routes.SPELLS) },
                onEditCustomSlot = { id -> nav.navigate(Routes.customSlotEdit(id)) },
                onOpenCharacters = { nav.navigate(Routes.CHARACTERS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }

        // Этап 22: экран мульти-персонажей. Свайп влево из HomeScreen.
        composable(Routes.CHARACTERS) {
            val factory = CharactersViewModel.Factory(application)
            val vm: CharactersViewModel = viewModel(factory = factory)
            CharactersScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
            )
        }

        // Этап 26: экран настроек. Шестерёнка в Home TopAppBar.
        composable(Routes.SETTINGS) {
            SettingsScreen(
                currentTag = currentLocaleTag(context),
                onLanguageSelected = { tag ->
                    LocaleStorage.setTag(context, tag)
                    // Без recreate() Compose-дерево остаётся на старой локали —
                    // переключение визуально не работает до следующего запуска.
                    context.findActivity()?.recreate()
                },
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.SPELLS) {
            val vm: SpellsViewModel = viewModel()
            SpellsScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onOpenSpell = { id -> nav.navigate(Routes.spell(id)) },
            )
        }

        composable(
            route = Routes.SPELL_DETAIL_PATTERN,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStack ->
            val spellId = backStack.arguments?.getLong("id") ?: 0L
            val factory = SpellDetailViewModel.Factory(application, spellId)
            val vm: SpellDetailViewModel = viewModel(factory = factory)
            SpellDetailScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
            )
        }

        // Этап 20: маршрут редактирования пользовательской ячейки.
        // Открывается из HomeScreen через long press 3с.
        composable(
            route = Routes.CUSTOM_SLOT_EDIT_PATTERN,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStack ->
            val slotId = backStack.arguments?.getLong("id") ?: 0L
            val factory = EditCustomSlotViewModel.Factory(application, slotId)
            val vm: EditCustomSlotViewModel = viewModel(factory = factory)
            EditCustomSlotScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
            )
        }
    }
}

/** Константы маршрутов в одном месте, чтобы не было опечаток. */
object Routes {
    const val HOME                     = "home"
    const val SPELLS                   = "spells"
    const val SPELL_DETAIL_PATTERN     = "spell/{id}"
    const val CUSTOM_SLOT_EDIT_PATTERN = "customslot/{id}"
    // Этап 22: экран мульти-персонажей
    const val CHARACTERS               = "characters"
    // Этап 26: экран настроек
    const val SETTINGS                 = "settings"

    fun spell(id: Long): String = "spell/$id"

    /** Этап 20: маршрут на экран редактирования пользовательской ячейки. */
    fun customSlotEdit(id: Long): String = "customslot/$id"
}

/**
 * Текущий per-app locale tag ("ru" / "en" / null если пользователь не выбирал).
 *
 * Читаем из [LocaleStorage] — собственный SharedPreferences, потому что
 * `AppCompatDelegate.getApplicationLocales` на `ComponentActivity` тихо
 * возвращает пустой список (`sContext == null`).
 *
 * Используется в SettingsScreen для подсветки активного radio-пункта.
 */
private fun currentLocaleTag(context: Context): String =
    LocaleStorage.getTag(context) ?: ""

/**
 * Достаём Activity из произвольного [Context] (LocalContext.current
 * может быть ContextWrapper). Нужно для [Activity.recreate] после
 * смены локали в SettingsScreen.
 */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
