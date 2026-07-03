package com.example.spelltracker

import android.content.Context
import android.os.Build
import android.os.LocaleList as PlatformLocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Per-app locale storage.
 *
 * **Зачем свой SharedPreferences, а не только [AppCompatDelegate].**
 * `AppCompatDelegate.setApplicationLocales` / `getApplicationLocales` внутри
 * опираются на статический `AppCompatDelegate.sContext`, который
 * инициализируется только через `AppCompatActivity.attachBaseContext`. На
 * `ComponentActivity` (Compose-only проект, у нас именно так) `sContext`
 * остаётся `null`, и оба вызова тихо деградируют:
 *   - `setApplicationLocales` не персистит (нет доступа к `LocaleManager`),
 *   - `getApplicationLocales` возвращает пустой список.
 *
 * Симптомы на устройстве пользователя:
 *   1. После тапа «Русский» / «English» в Settings язык не меняется.
 *   2. Активный radio в Settings не подсвечен (`currentLocaleTag == ""`).
 *
 * Свой SharedPreferences (`spell_tracker_locale`) решает оба: пишем и
 * читаем всегда из `applicationContext`, минуя AppCompatDelegate.
 *
 * На API 33+ дополнительно зовём `LocaleManager.setApplicationLocales`
 * (через AppCompatDelegate), чтобы per-app язык отображался в системных
 * Settings → Apps → Spell Tracker → Language. Если AppCompatDelegate
 * не сработает (sContext == null) — наш SharedPreferences всё равно
 * обеспечит корректную работу внутри приложения.
 */
object LocaleStorage {

    private const val PREFS_NAME = "spell_tracker_locale"
    private const val KEY_TAG    = "current_tag"

    private val DEFAULT_TAG = "ru"

    /** Текущий тег ("ru" / "en") или null, если пользователь ещё не выбирал. */
    fun getTag(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TAG, null)

    /** Записать тег и (на API 33+) попробовать синхронизировать с системой. */
    fun setTag(context: Context, tag: String) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TAG, tag)
            .apply()
        // На API 33+ дублируем в LocaleManager, чтобы системные Settings
        // → Apps → Language тоже показали наш выбор. Если AppCompatDelegate
        // не инициализирован (sContext == null) — вызов no-op, и это ОК:
        // наш SharedPreferences остаётся единственным источником истины
        // внутри приложения.
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(tag),
                )
            } catch (_: Throwable) {
                // no-op: SharedPreferences уже сохранён, UI работает.
            }
        }
    }

    /** Дефолтный тег для первой установки. */
    fun defaultTag(): String = DEFAULT_TAG

    /** Locale, который соответствует тегу или дефолту. Для attachBaseContext. */
    fun localeFor(context: Context): Locale {
        val tag = getTag(context) ?: DEFAULT_TAG
        return Locale.forLanguageTag(tag)
    }

    /**
     * Применить тег к ресурсам Context — используется в `attachBaseContext`
     * ДО загрузки ресурсов Activity. Не зависит от AppCompatDelegate.
     */
    fun applyTo(base: Context): Context {
        val tag = getTag(base) ?: DEFAULT_TAG
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        // На API 24+ дополнительно прокидываем LocaleList — некоторые
        // ресурсы (qualified resources, plurals) смотрят именно в список,
        // а не на один locale.
        if (Build.VERSION.SDK_INT >= 24) {
            config.setLocales(PlatformLocaleList.forLanguageTags(tag))
        }
        return base.createConfigurationContext(config)
    }
}