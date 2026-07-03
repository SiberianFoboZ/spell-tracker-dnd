package com.example.spelltracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.spelltracker.ui.nav.AppNavigation
import com.example.spelltracker.ui.theme.SpellTrackerTheme

/**
 * Единственная Activity в приложении. Внутри — Compose-Nav.
 *
 * База — [ComponentActivity], **не** [androidx.appcompat.app.AppCompatActivity]:
 * `AppCompatActivity` требует `Theme.AppCompat*` (или наследника),
 * а у нас `Theme.Material.NoActionBar` (Compose-only тема).
 *
 * Локаль читается/пишется через [LocaleStorage] — собственный
 * SharedPreferences-источник истины. `AppCompatDelegate` на
 * `ComponentActivity` тихо деградирует (sContext == null), поэтому
 * полагаться только на него нельзя. Подробнее — в KDoc [LocaleStorage].
 *
 * Применение локали — через [Context.createConfigurationContext]
 * в [attachBaseContext] (ДО загрузки ресурсов): на API 33+ это
 * работает стабильно и не требует Activity recreate.
 */
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Применяем сохранённую локаль (или дефолт "ru") к ресурсам ДО
        // того, как Activity их загрузит. На этом этапе recreate() не нужен
        // и не вызывается — лупа не будет.
        super.attachBaseContext(LocaleStorage.applyTo(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Первый запуск: персистим дефолт, чтобы в дальнейшем
        // currentLocaleTag() в AppNavigation возвращал конкретный тег
        // и радио в Settings подсвечивалось.
        if (LocaleStorage.getTag(this) == null) {
            LocaleStorage.setTag(this, LocaleStorage.defaultTag())
        }
        enableEdgeToEdge()
        setContent {
            SpellTrackerTheme {
                AppNavigation()
            }
        }
    }
}