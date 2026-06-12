# Spell Tracker — статус разработки

## ✅ Что уже сделано

### Этап 1–5. Ядро приложения
- Импорт заклинаний из CSV → Room (с кэшем JSON-файлов классов)
- Боковое меню (DrawerLayout) с фильтрами: подготовленные, все, по уровню, заговоры
- Реестр классов `Classes.ALL` с привязкой к per-class JSON
- Расчёт эффективного уровня заклинателя по PHB multiclass:
  - full casters ×1.0 (бард, волшебник, друид, жрец, чародей)
  - half casters ×0.5 (паладин, следопыт, изобретатель)
  - warlock — pact magic, отдельный пул
- Слоты заклинаний 1–9 уровня по таблице (хардкод `SLOT_TABLE[20][9]`)
- Карточка pact magic (показывается только если warlock > 0)
- NPE-fix: убран `android:id` на `<include>` для `item_pact_magic.xml`
- Удалена ручная правка total в слотах, добавлены только «Исп.» / «Восст.»
- Поиск по заклинаниям (case-insensitive, по подстроке) в `SpellsActivity`
- Кнопка «назад» в `SpellsActivity` через `onSupportNavigateUp() → finish()`
- Release APK подписан debug-ключом: `app/build/outputs/apk/release/app-release.apk` (~5 МБ)

### Этап 6. Edge-to-edge + пустые поля
- `WindowCompat.setDecorFitsSystemWindows(window, false)` во всех 3 Activity
- `MainActivity`: insets-listener на `R.id.bottom_buttons_row` → paddingBottom = nav bar
- `SpellsActivity`: insets-listener на `R.id.fragment_container`
- `SpellDetailActivity`: insets-listener на `R.id.scroll_view` (с `clipToPadding="false"`)
- Поля уровней классов по умолчанию пустые
  - Убран флаг `syncing`
  - В `onCreate`/`onResume`: `et.setText("")`
  - `TextWatcher.afterTextChanged`: `if (text.isEmpty()) return;` — пустое поле не перезаписывает сохранённое значение
- `BUILD SUCCESSFUL` (debug ~15s, release ~11s)

---

---

## ✅ Этап 7. Изобретатель + Следопыт + плейсхолдер «0» — **ЗАВЕРШЁН**

### Что сделано
- ✅ `Classes.java` — добавлено поле `roundUp` в `Info`, добавлены классы
  `ranger` (factor=0.5, roundUp=false) и `artificer` (factor=0.5, roundUp=true)
- ✅ `SpellStorage.computeCasterLevel()` — учёт `roundUp`:
  ranger/paladin `lvl/2`, artificer `(lvl+1)/2`
- ✅ `activity_main.xml` — добавлена 5-я строка с полями «Следопыт» и «Изобретатель»
- ✅ `strings.xml` — `class_ranger`, `class_artificer`
- ✅ `MainActivity.java`:
  - `et.setText("0")` в `onCreate` и `onResume` — плейсхолдер «нет уровня»
  - `OnFocusChangeListener`: при фокусе очищает «0», при потере фокуса
    восстанавливает «0» в пустом поле
  - `TextWatcher`: «0» и пустая строка не перезаписывают сохранённое значение
  - цифровая клавиатура уже включена через `android:inputType="number"`
- ✅ Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (6.7 МБ) — `BUILD SUCCESSFUL in 20s`
- ✅ Release APK: `app/build/outputs/apk/release/app-release.apk` (5.3 МБ) — `BUILD SUCCESSFUL in 17s`

### Сопутствующие правки окружения
- В `gradle.properties` добавлено:
  - `org.gradle.java.home=.../eclipse_adoptium-21-amd64-windows.2` — путь к распакованному JDK 21 (Gradle 9.3.1 требует JDK 21 для демона; auto-download из foojay.io ломался на перемещении файла)
  - `org.gradle.java.installations.paths=.../eclipse_adoptium-21-amd64-windows.2,C:/Program Files/Java/jdk-17` — toolchain auto-detection
  - `org.gradle.java.installations.auto-download=false` — отключает повторные попытки скачивания
- Удалены `.utf8` бэкапы файлов из исходников (мешали сборке как «файлы не .xml»)

### Проверка формулы (PHB multiclass)
- Ranger 1 / Artificer 1 → 0 + (1+1)/2 = **1** (раньше ranger 1 давал бы 0)
- Paladin 10 → **5** (без изменений)
- Ranger 10 / Artificer 1 → 10/2 + (1+1)/2 = 5 + 1 = **6** (вместо 5 по старой логике)

### Хотфикс: «0» в поле теперь = уровень 0
**Проблема:** поля показывают «0» (плейсхолдер), но `TextWatcher`
пропускал «0» и не перезаписывал им сохранённое ранее ненулевое значение.
В итоге эффективный уровень заклинателя не сбрасывался к 0 при всех «0».

**Решение:** в `MainActivity.afterTextChanged` убрана ветка `"0".equals(text)`
из условия раннего выхода — теперь «0» сохраняется как реальное значение.
Пустое поле по-прежнему игнорируется (это промежуточное состояние при
фокусе, до ввода числа; на blur фокус-листенер восстанавливает «0», и
повторный TextWatcher записывает 0 в storage).

**Поведение после фикса:**
- Открытие экрана: все поля «0» → `setText("0")` → TextWatcher → storage
  сбрасывается к 0 → эффективный уровень = 0 ✓
- Тап на поле с «0»: фокус-листенер стирает в `""` → TextWatcher return
  (storage не трогается)
- Ввод числа «5»: TextWatcher → storage = 5 → пересчёт
- Очистка и blur: фокус-листенер восстанавливает «0» → TextWatcher → storage = 0
- Закрытие/перезапуск без ввода: storage сбрасывается к 0 (плата за то,
  что видимое «0» = реальный 0)

---

## ⏭️ Следующий шаг
- Протестировать APK на устройстве/эмуляторе (формула выше)
- По желанию: отображать сохранённый уровень в поле (сейчас всегда «0» при открытии экрана)

---

## ✅ Этап 8. Кастомная иконка + переименование APK — **ЗАВЕРШЁН**

### Что сделано

#### 1. Иконка приложения
**Дизайн:** тёмно-фиолетовый радиальный градиент (#5A2A8A → #160828) +
большая золотая 4-конечная звезда (#F4C430) сверху + открытая книга
заклинаний (кремовые страницы #F4E4C1 с тёмно-фиолетовым контуром и
линиями-«текстом»). Две малые искры по бокам добавляют «магической»
атмосферы.

- ✅ `drawable/ic_launcher_background.xml` — радиальный градиент (для API 26+)
- ✅ `drawable/ic_launcher_foreground.xml` — книга + звезда (для API 26+)
- ✅ `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png` + `ic_launcher_round.png` —
  PNG-фолбэки для API 24-25 (сгенерированы Python-скриптом
  `tools/gen_icons.py` через Pillow, 10 файлов 48×48 … 192×192)
- ✅ Старые `.webp` с Android-роботом удалены из всех density-папок
- ✅ Round-вариант (`mipmap-*/ic_launcher_round.png`) — с круглой маской

#### 2. Переименование APK
**В AGP 9.x `applicationVariants` удалён**, поэтому переименование
выполняется через `doLast`-хук в `afterEvaluate {}` блоке
`app/build.gradle.kts`:

```kotlin
afterEvaluate {
    val vName = android.defaultConfig.versionName ?: "0.0"
    listOf("debug", "release").forEach { variant ->
        val taskName = "assemble${variant.replaceFirstChar { it.uppercase() }}"
        tasks.findByName(taskName)?.doLast {
            val dir = file("build/outputs/apk/$variant")
            val original = file("$dir/app-$variant.apk")
            val renamed = file("$dir/spell-tracker-v$vName.apk")
            if (original.exists()) {
                if (renamed.exists()) renamed.delete()
                original.renameTo(renamed)
            }
        }
    }
}
```

Версия берётся из `defaultConfig.versionName` (сейчас `1.0`).

#### 3. Сборка
- ✅ `gradlew.bat assembleDebug` → `app/build/outputs/apk/debug/spell-tracker-v1.0.apk`
- ✅ `gradlew.bat assembleRelease` → `app/build/outputs/apk/release/spell-tracker-v1.0.apk`
- Обе сборки `BUILD SUCCESSFUL in 5-6s`

### Артефакты
```
app/build/outputs/apk/debug/spell-tracker-v1.0.apk
app/build/outputs/apk/release/spell-tracker-v1.0.apk
```

### Вспомогательные файлы
- `tools/gen_icons.py` — генератор PNG-фолбэков (можно перезапустить
  для регенерации иконок при изменении дизайна)

---

## ⏭️ Следующий шаг
- Установить `spell-tracker-v1.0.apk` на устройство и проверить,
  что иконка отображается в лаунчере корректно
- При обновлении дизайна иконки — править vector drawables в `drawable/`
  и/или `tools/gen_icons.py` (для PNG-фолбэков)

---

## ✅ Этап 9. Git + GitHub Actions — **ЗАВЕРШЁН**

### Что сделано

#### 1. Подключение git
- Локальный bare-артефакт (файлы `HEAD`, `config`, `description`, `hooks`,
  `info`, `objects`, `refs`, `FETCH_HEAD` лежали в корне проекта) — удалён
- `local.properties`, `.idea/`, `build/`, `app/build/`, `*.log`, `*.jks` —
  добавлены в `.gitignore` (уже были базовые правила Android Studio,
  расширили под наш проект)
- Remote настроен: `github → git@github.com:SiberianFoboZ/spell-tracker-dnd.git`

#### 2. SSH-доступ к GitHub (PuTTY + Pageant)
Проблема: OpenSSH-овский `ssh-agent` не видел ключ, потому что ключ
загружен в **Pageant** (PuTTY-agent), а не в OpenSSH-agent.
Решение: прописали `core.sshCommand` в `git config --global`:

```
C:/Program Files/PuTTY/plink.exe -i C:/Users/vk241/.ssh/github.ppk
```

Теперь Git ходит через `plink.exe` → `pageant` → GitHub.
Проверено: `git ls-remote github HEAD` → отдаёт коммит.

#### 3. GitHub Actions workflow
Создан `.github/workflows/release.yml`:

- **Триггер**: push тега вида `v*` (например, `v1.0.0`) или ручной запуск
  через UI (`workflow_dispatch`)
- **Среда**: `ubuntu-latest` + Temurin JDK 21 + Android SDK API 36
- **Сборка**: `./gradlew assembleRelease` с override
  `-Dorg.gradle.java.home=$JAVA_HOME` (иначе Gradle пытается открыть
  Windows-путь из `gradle.properties` и падает)
- **Публикация**: `softprops/action-gh-release@v2` создаёт релиз с
  тегом и прикрепляет `app/build/outputs/apk/release/spell-tracker-v*.apk`
- `generate_release_notes: true` — GitHub сам собирает changelog
  из PR/коммитов между тегами

### Текущее состояние репозитория
```
main
├─ cc02316  Add GitHub Actions release workflow
├─ 254812f  Expand .gitignore: cover .idea/, build/, logs, keystores
├─ cf86f74  spell tracker. first mvp version
└─ (LICENSE, README.md из GitHub)
```

Push: `cc02316..cc02316` — `main` синхронизирован с GitHub.

---

## ⏭️ Следующий шаг
- **Протестировать CI** — создать тег `v1.0.0` и убедиться, что workflow
  собирает APK и публикует релиз:
  ```bash
  cd C:\Users\vk241\AndroidStudioProjects\Spelltracker
  git tag v1.0.0
  git push github v1.0.0
  ```
  Открыть <https://github.com/SiberianFoboZ/spell-tracker-dnd/actions>
  и проверить, что:
  1. workflow запустился;
  2. шаг «Build release APK» прошёл;
  3. в разделе Releases появился релиз `v1.0.0` с прикреплённым APK.
- При баге в CI — прислать логи failed-шага, починим.
- Для будущих релизов (`v1.0.1`, `v1.1.0` и т.д.) — бампить
  `versionName` в `app/build.gradle.kts` перед тегированием.
