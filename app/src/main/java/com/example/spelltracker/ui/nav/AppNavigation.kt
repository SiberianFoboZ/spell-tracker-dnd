package com.example.spelltracker.ui.nav

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.spelltracker.ui.detail.SpellDetailScreen
import com.example.spelltracker.ui.detail.SpellDetailViewModel
import com.example.spelltracker.ui.home.HomeScreen
import com.example.spelltracker.ui.home.HomeViewModel
import com.example.spelltracker.ui.spells.SpellsScreen
import com.example.spelltracker.ui.spells.SpellsViewModel

/**
 * Единая точка входа в навигацию. Три экрана:
 *
 *   home                  — главный экран с классами и ячейками
 *   spells                — список заклинаний с фильтром
 *   spell/{id}            — детальная карточка одного заклинания
 */
@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as Application

    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = vm,
                onOpenSpells = { nav.navigate(Routes.SPELLS) }
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
    }
}

/** Константы маршрутов в одном месте, чтобы не было опечаток. */
object Routes {
    const val HOME                  = "home"
    const val SPELLS                = "spells"
    const val SPELL_DETAIL_PATTERN  = "spell/{id}"

    fun spell(id: Long): String = "spell/$id"
}
