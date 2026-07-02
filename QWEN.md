# Spell Tracker — QWEN Context

> Инструкции для Qwen Code по работе с этим проектом.
> Проект — Android-приложение на Kotlin + Jetpack Compose для отслеживания
> ячеек заклинаний D&D 5e по правилам PHB. Все строки UI — на русском.

---

## 1. Что это за проект

**Spell Tracker** — локальное Android-приложение, которое помогает магу
D&D 5e следить за ячейками заклинаний, пакт-магией Колдуна и арканумами
на одном экране. Без аккаунтов, без аналитики, без интернета.

- **applicationId / namespace**: `com.example.spelltracker`
- **Текущая версия**: v2.4.2 (`versionCode = 10`)
- **Min SDK**: 24 (Android 7.0). **Target/Compile SDK**: 36 (Android 16).
- **Язык UI**: русский. Все строки — в `app/src/main/res/values/strings.xml`,
  плюс хардкод русских строк внутри `*Screen.kt` (см. раздел Conventions).
- **Хранилище состояния**: SharedPreferences (слоты, уровни, prepared) +
  Room БД (только справочник заклинаний).
- **Метод версионирования**: «Этапы» (Этап 15, 16, 17, 18) внутри
  коммитов + SemVer-теги (`v2.4.2`).

Релиз-флоу:
- Push тега `v*` → `release.yml` собирает APK и публикует GitHub Release.
- Ручной запуск `screenshots.yml` — снять скриншоты в эмуляторе.

---

## 2. Стек и версии

| Слой | Технология / версия |
|------|---------------------|
| Язык | Kotlin **2.0.21** |
| UI | Jetpack Compose (BOM **2024.10.01**) + Material 3 |
| Сборка | AGP **9.1.1** + Gradle + **KSP 2.0.21-1.0.27** |
| Состояние | Lifecycle **2.8.7** + `StateFlow` / `SharedFlow` |
| Навигация | Navigation Compose **2.8.4** |
| Локальная БД | Room **2.6.1** (только справочник заклинаний) |
| Java | **JDK 17** (CI использует JDK 21 на Linux; локально — JDK 17+) |

Все версии — в `gradle/libs.versions.toml`. **Никаких сторонних зависимостей
сверх AndroidX / Compose / Room.**

### AGP 9.x gotchas (важно — иначе сборка падает)

Эти правила зафиксированы комментариями в `build.gradle.kts` /
`settings.gradle.kts` / `gradle.properties` — не «упрощать»:

- **Kotlin-плагин не объявляем в `app/build.gradle.kts` явно** —
  AGP 9.x авто-применяет `org.jetbrains.kotlin.android`, если он есть
  в classpath. Явное объявление → «extension already registered».
- **KSP обязателен для Room на Kotlin-коде**. `annotationProcessor`
  не обрабатывает Kotlin-аннотации → `SpellDatabase_Impl` не генерируется
  → `IllegalStateException: SpellDatabase_Impl does not exist`.
- **`android.disallowKotlinSourceSets=false`** в `gradle.properties` —
  иначе KSP не сможет подкинуть сгенерированные kotlin-исходники.
- **`lint.checkReleaseBuilds = false`** — `lint-vital` под Windows иногда
  валится на file-lock; на сам APK не влияет.

---

## 3. Структура проекта

```
Spelltracker/
├── app/
│   ├── build.gradle.kts                    # AGP/KSP/Room/Compose
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml         # одна Activity
│       │   ├── assets/
│       │   │   ├── spells.csv              # ~500 заклинаний D&D 5e
│       │   │   ├── class_*.json            # 9 per-class JSON-ов
│       │   │   └── databases/populate.sql  # legacy (НЕ используется кодом)
│       │   ├── java/com/example/spelltracker/
│       │   │   ├── MainActivity.kt         # single-Activity host
│       │   │   ├── data/                   # слои данных (см. §4)
│       │   │   │   ├── Classes.kt          # метаданные 9 классов
│       │   │   │   ├── ClassFilter.kt
│       │   │   │   ├── Spell.kt            # @Entity
│       │   │   │   ├── SpellDao.kt
│       │   │   │   ├── SpellDatabase.kt    # version=3, fallbackToDestructive
│       │   │   │   ├── SpellParser.kt      # CSV + JSON парсер
│       │   │   │   ├── SpellRepository.kt
│       │   │   │   └── SpellStorage.kt     # SharedPreferences + StateFlow
│       │   │   └── ui/
│       │   │       ├── detail/             # SpellDetailScreen + ViewModel
│       │   │       ├── home/               # HomeScreen + HomeViewModel
│       │   │       ├── nav/AppNavigation.kt
│       │   │       ├── spells/             # SpellsScreen + ViewModel
│       │   │       └── theme/              # Color, Theme, Type
│       │   └── res/values/                 # strings.xml, themes.xml
│       ├── test/java/.../ExampleUnitTest.kt
│       └── androidTest/
├── build.gradle.kts                        # пустой (только комментарий)
├── settings.gradle.kts                     # pluginManagement + версии
├── gradle.properties                       # daemon JVM args + KSP-флаг
├── gradle/libs.versions.toml               # все версии
├── gradle/wrapper/                         # gradle wrapper (9.3.1)
├── tools/
│   ├── generate_sql.ps1                    # legacy: CSV → populate.sql
│   ├── gen_icons.py                        # генерация иконок
│   └── gen_screenshots.py                  # локальная генерация скриншотов
├── docs/screenshots/                       # main.png, spells.png, drawer.png
├── .github/workflows/
│   ├── release.yml                         # tag v* → APK + Release
│   └── screenshots.yml                     # ручной запуск эмулятора
├── README.md
├── LICENSE                                 # MIT
└── QWEN.md                                 # ← этот файл
```

---

## 4. Архитектура

### MVVM + StateFlow + single source of truth

```
SpellStorage (SharedPreferences + StateFlow)
    │  classLevels, usedSlots, usedPactSlots, usedArcanums, prepared
    ▼
HomeViewModel / SpellsViewModel / SpellDetailViewModel
    │  комбинируют потоки → immutable HomeState / SpellsState / SpellDetailState
    ▼
*Screen.kt (Compose)
    │  collectAsState(), побочки в LaunchedEffect / viewModelScope
    ▼
```

- **State** — только в `SpellStorage` (single source of truth). Любое
  изменение идёт через `storage.setX(...)`, оно пишет в `prefs.edit()`
  И эмитит в `MutableStateFlow.update { ... }`.
- **Composable** — без побочных эффектов. Побочки (Snackbar, навигация)
  — в `LaunchedEffect` / `ViewModel` (`SharedFlow<HomeEvent>`).
- **Navigation** — один `NavHost` (`ui/nav/AppNavigation.kt`), 3 экрана:
  `home` → `spells` → `spell/{id}`.

### Главный экран: 3 секции (Этап 16–18)

```
┌─ Header + Effective Caster Level (большая золотая цифра)
├─ Классы: 3×3 сетка карточек с LevelInput
├─ МАГИЯ ДОГОВОРА        ← только если warlockLevel > 0
├─ ЯЧЕЙКИ ЗАКЛИНАНИЙ     ← regularSlots (все классы, кроме пакт)
├─ АРКАНУМЫ              ← только если warlockLevel ≥ 11
└─ [Длинный отдых] [Короткий отдых]   ← bottom bar
```

**Логика PHB-мультикласса** живёт в `SpellStorage.computeCasterLevel()`:
- `factor = 1.0` (бард, волшебник, друид, жрец, чародей) → `+lvl`
- `factor = 0.5` (паладин, следопыт, изобретатель) → `+lvl/2`
  (для `roundUp = true` → `(lvl+1)/2`, это только Изобретатель)
- `factor = 0.0` (колдун) — **исключён** из формулы caster level.
  У него собственная пакт-магия (`WARLOCK_SLOTS` в `SpellStorage`).
- `Warlock` с `factor = 0.0` всё равно в сетке классов (нужен для
  пакт-магии и арканумов).

**Арканумы** (Этап 17, v2.4.0): по одной ячейке VI–IX уровня.
Открытие: warlockLevel 11/13/15/17 → 1/2/3/4 арканума. Восстановление —
**только длинный отдых** (по PHB). Хранение: 4 булевых флага
(`arcanum_6..arcanum_9`) в `SpellStorage._usedArcanums`.

**Пакт-магия** (Этап 18, v2.4.1): отдельная секция «МАГИЯ ДОГОВОРА»
над обычными ячейками. Восстанавливается на **коротком** отдыхе.
`HomeState.pactSlot: SlotInfo?` — `null`, если warlockLevel == 0
или `WARLOCK_SLOTS[warlockLevel][0] == 0`.

**Отдых (Этап 15):**
- `shortRest()` → сбрасывает только `usedPactSlots`. Арканумы
  остаются (если есть потраченные арканумы — эмитится
  `HomeEvent.ArcanumShortRestBlocked` → Snackbar-предупреждение).
- `longRest()` → сбрасывает `usedSlots[1..9]` + `usedPactSlots` +
  `usedArcanums[6..9]`. Class levels и prepared — **сохраняются**.
- В `SpellStorage.resetAllUsed()` (debug-only) — `prefs.edit().clear()`.

### Реактивность: как избежать «дрейфа» в compose-эффектах

- `HomeViewModel.init { combine(storage.classLevels, storage.usedSlots,
  storage.usedPactSlots, storage.usedArcanums) { ... } }` — пересобирает
  snapshot при ЛЮБОМ изменении всех 4 потоков. Никакого ручного
  invalidate.
- В `HomeScreen` Snackbar получает события через
  `viewModel.events.collectLatest { ... }` — старый Snackbar
  отменяется, виден самый свежий.

---

## 5. База данных (Room)

```kotlin
@Database(entities = [Spell::class], version = 3, exportSchema = false)
abstract class SpellDatabase : RoomDatabase() { ... }
```

- `fallbackToDestructiveMigration()` — безопасно: данные приходят
  из `assets/spells.csv` + `class_*.json`. Потеря пользовательских
  записей невозможна (это справочник, не пользовательские данные).
- Импорт: `SpellRepository.ensureInitialized()` → `dao.count() == 0` →
  `SpellParser.loadFromAssets()` → `dao.insertAll()`. **Запускается
  один раз** на новом устройстве.
- `version = 3` форсирует реимпорт при апгрейде с v2.0.0 (там
  `version = 2` и могли быть не залиты заклинания 6–9 уровней).
- `Spell.id` = `name.hashCode().toLong() and 0x7FFFFFFF` — стабильный
  между запусками, чтобы `prepared` работал между сессиями.

### Parser (`SpellParser.kt`)

- CSV: разделитель `;`, кавычки `"..."`, `""` = экранирование,
  многострочные записи склеиваются по балансу кавычек.
- `\f` (form feed, `'\u000C'`) внутри записи = перенос строки
  в описании.
- Per-class JSON-ы (`class_*.json`) — маппинг имя→классы. Если
  заклинание **не нашлось** ни в одном JSON, ему пишется **пустая**
  строка классов (а не «все классы»!). Иначе 258 заклинаний с
  расхождениями в переводах «протекали» бы в любой фильтр по классу.
  См. развёрнутый комментарий в `SpellParser.loadFromAssets` —
  не упрощать это поле.

---

## 6. Conventions (обязательные к соблюдению)

### Стиль кода
- **Язык комментариев и строк UI**: русский.
- **Имена в коде** (классы, функции, переменные): английский.
- **Идиомы Kotlin**: `data class`, `sealed interface` для событий,
  extension-функции не злоупотреблять.
- `companion object` для констант-таблиц (`SLOT_TABLE`, `WARLOCK_SLOTS`,
  `ARCANUM_LEVELS` в `SpellStorage`).

### Строки UI
- Идеальный путь: `strings.xml`. Реальность: ~80% строк **захардкожены
  в `*Screen.kt` русским текстом**. Это унаследованный долг v2.0.0.
  **Новые строки**: добавлять И в `strings.xml`, И в код (для
  consistency с существующим). Не «исправлять» хардкод в рамках
  unrelated-задач.

### Иммутабельные state-классы
- `HomeState`, `SpellsState`, `SpellDetailState` — `data class` с
  `val`-полями. Никаких мутаций «на месте». `copy(...)` для
  обновления.

### События vs состояние
- Состояние UI, которое нужно «прочитать несколько раз» (например,
  «выбран ли уровень N») — `StateFlow<SpellsState>`.
- Одноразовые побочки (показать Snackbar, навигация) — `SharedFlow`
  с `replay=0`, `extraBufferCapacity=4`. Не смешивать в одном флоу.

### Поведение SharedPreferences
- `prefs.edit().apply()` — fire-and-forget запись на диск, in-memory
  StateFlow обновляется сразу. Не использовать `commit()` (блокирует UI).

### Поведение Compose
- `clickable(enabled = ...)` для блокировки тапа, когда действие
  невозможно (например, `isAllSpent` → клик по строке ячеек).
- Цветовая иерархия: `Gold` (важные значения, активные элементы) →
  `PurpleLight` (заголовки секций) → `TextWhite` (основной текст) →
  `TextGrey` (вторичный) → `TextGreyDark` (отключённое/неактивное).
- Анимации: `animateColorAsState` / `animateDpAsState` /
  `animateIntAsState` для плавных переходов; `Animatable` + scale
  для коротких «вспышек» при тапе.

### Naming
- VM-методы — событийные: `setClassLevel(id, lvl)`, `shortRest()`,
  `onPactRowClick(slot)`, `togglePrepared()`. Не `update*` / `process*`.
- StateFlow: `_state` (private) + `state` (public read-only).
- SharedFlow событий: `_events` (private) + `events` (public read-only).

---

## 7. Сборка и запуск

### Требования
- **JDK 17+** (CI ставит JDK 21; локально JDK 17 достаточно).
- **Android SDK** с `platforms/android-36` и `build-tools 36.0.0+`.
- Интернет для первой загрузки зависимостей Gradle.

### Команды

```bash
# Сборка debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Сборка release APK (подписан debug-ключом!)
./gradlew assembleRelease
# → app/build/outputs/apk/release/spell-tracker-v2.4.2.apk
# (имя переименовывается через afterEvaluate в app/build.gradle.kts)

# Установка на устройство
adb install -r app/build/outputs/apk/release/spell-tracker-v2.4.2.apk

# Тесты
./gradlew test                  # unit (только ExampleUnitTest на данный момент)
./gradlew connectedAndroidTest  # instrumented
```

**Windows (PowerShell):**
```powershell
.\gradlew.bat assembleRelease
```

### CI-обходной путь
CI передаёт `-Dorg.gradle.java.home=$JAVA_HOME` явно — иначе Gradle
подхватит путь из локального `gradle.properties` и упадёт (это
Windows-путь, на Linux-раннере его нет).

### Подпись (Этап 23)
`release` подписывается **личным keystore** (`keystore/spell-tracker-release.jks`,
RSA 2048, срок 10000 дней). Креды — в `keystore.properties` (корень).
`app/build.gradle.kts` читает их через `Properties.load(FileInputStream(...))`:
- если `keystore.properties` есть → `signingConfigs.release` + `signingConfig = signingConfigs.getByName("release")`;
- если нет → fallback на debug-keystore (обратная совместимость).

Генерация: `tools/generate_keystore.ps1` (Windows, PowerShell) или вручную
`keytool -genkeypair ...`. Шаблон параметров — `keystore.properties.example`.

**Пароли `storePassword` и `keyPassword` одинаковые** — PKCS12 (по умолчанию в
современной JDK) не поддерживает разные пароли для store/key.

**Не коммитить `keystore/`, `keystore.properties`** (в `.gitignore`). Для совместной
разработки передавайте keystore.properties (и при необходимости сам keystore)
**отдельно** от кода по защищённому каналу. `keystore.properties.example` —
единственное, что коммитится, как шаблон формата.

Если у пользователя стоит APK, подписанный старым (debug) ключом, обновление
поверх не получится — нужно сначала удалить старую версию.

### Учёт локального Windows
`gradle/gradle-daemon-jvm.properties` и пользовательский
`~/.gradle/gradle.properties` (на Windows) задают пути к локальному
JDK 21. Сам проектный `gradle.properties` остаётся портативным.

---

## 8. Релиз-процедура

1. Обновить `versionCode` и `versionName` в `app/build.gradle.kts`.
2. Обновить `README.md` (история релизов + блок «Что нового»).
3. `git add ... && git commit -m "..."` в стиле существующих коммитов
   (см. `git log --oneline`).
4. Поставить тег: `git tag v2.4.2 && git push origin v2.4.2`.
5. CI собирает APK и публикует GitHub Release автоматически
   (`softprops/action-gh-release@v2` с `generate_release_notes: true`).

Стиль существующих коммитов:
- `docs: rewrite README for GitHub first page (v2.4.2)`
- `Hide 'АРКАНУМЫ' block when warlockLevel == 0 (v2.4.2 patch)`
- `Arcanums section for Warlock (Этап 17, v2.4.0)`
- `Click-on-row slot UX + unify Warlock into main list v2.3.0`

Не нумеровать этапы произвольно — «Этап N» указан в коммит-сообщениях
как счётчик фич, не SemVer.

---

## 9. Тестирование

- **Сейчас**: только `ExampleUnitTest` (заглушка). Реальных unit-тестов
  на `SLOT_TABLE`, `WARLOCK_SLOTS`, `computeCasterLevel()` — нет.
- **README** упоминает `./gradlew test` как «тесты на PHB-таблицы»,
  но фактически они не написаны. Это TODO.
- **При добавлении тестов**: проверять `computeCasterLevel()` для
  известных мультиклассов (Wizard 5 / Cleric 5 = caster level 10),
  граничные значения `WARLOCK_SLOTS`, реакцию `applySlotTable()` на
  уменьшение total.

---

## 10. Типичные задачи и подводные камни

### Добавить новый класс D&D 5e
1. `data/Classes.kt` → новый `Info(...)` с `factor`, `roundUp`.
2. Дополнить `SLOT_TABLE` (в `SpellStorage`) — там уже полная таблица
   до 20 caster level, новых уровней не нужно.
3. Добавить `class_<id>.json` в `app/src/main/assets/`.
4. При особой логике отдыха — добавить ветку в `SpellStorage.shortRest()`.
5. Запустить `assembleDebug`, пройтись по сценарию мультикласса в эмуляторе.

### Изменить палитру
Все цвета — в `ui/theme/Color.kt` (`AppColors`). Material 3 mapping — в
`ui/theme/Theme.kt` (`DarkColors`). Тёмная тема форсирована —
`isSystemInDarkTheme()` игнорируется, осветлённая схема не предусмотрена.

### Поменять иконку приложения
- `tools/gen_icons.py` — генерация иконок из исходника.
- `app/src/main/res/mipmap-*` — пять density-папок + `mipmap-anydpi-v26`
  (адаптивная иконка).

### Не работает KSP / не генерируется SpellDatabase_Impl
Проверить, что в `app/build.gradle.kts` есть `id("com.google.devtools.ksp")`
и в `dependencies` стоит `ksp(libs.androidx.room.compiler)`, а НЕ
`annotationProcessor(...)`. Удалить `app/build/` и пересобрать.

### `Build was configured to prefer settings repositories...`
`repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` в
`settings.gradle.kts` запрещает `repositories { ... }` внутри модулей.
Все репозитории — только в `dependencyResolutionManagement` в settings.

### Локальный путь к JDK ломается на CI
См. §7. CI передаёт `-Dorg.gradle.java.home=$JAVA_HOME` явно.

### Добавить зависимость
Только в `gradle/libs.versions.toml` → затем `implementation(libs.xxx)`
в `app/build.gradle.kts`. **Не** указывать версии в `app/build.gradle.kts`
напрямую.

---

## 11. Чего НЕ делать

- **Не** удалять `usedArcanums` из `SpellStorage.combine(...)` в
  `HomeViewModel.init` — иначе снимок не пересоберётся при трате
  арканума.
- **Не** менять `SpellStorage.SLOT_TABLE` без синхронизации с PHB.
  Это закодированная таблица из Player's Handbook, любое расхождение =
  неправильное заклинание в полевых условиях.
- **Не** использовать `commit()` в `prefs.edit()` — UI замёрзнет
  на записи.
- **Не** ставить `replay > 0` в `MutableSharedFlow<HomeEvent>` —
  события (Snackbar) получат подписавшиеся ПОЗЖЕ, и пользователь
  увидит «Ячейки восстановлены» при следующем открытии экрана.
- **Не** подменять `assetFile` в `Classes.Info` (он помечен `@Suppress("unused")`,
  но это просто маркер «файл больше не загружается per-class»).
- **Не** добавлять сторонние библиотеки (Dagger, Hilt, Retrofit, Coil и
  т.п.) — проект сознательно держится только на AndroidX / Compose / Room.

---

## 12. Файлы-якоря (быстрая навигация)

| Что ищешь | Где |
|-----------|-----|
| Таблица ячеек по caster level | `data/SpellStorage.kt` → `SLOT_TABLE` |
| Таблица пакт-магии Колдуна | `data/SpellStorage.kt` → `WARLOCK_SLOTS` |
| Список арканумов (VI–IX) | `data/SpellStorage.kt` → `ARCANUM_LEVELS` |
| Логика мультикласса PHB | `data/SpellStorage.kt` → `computeCasterLevel()` |
| Цвета и тема | `ui/theme/Color.kt`, `ui/theme/Theme.kt` |
| Главный экран (3 секции) | `ui/home/HomeScreen.kt` + `HomeViewModel.kt` |
| Список заклинаний | `ui/spells/SpellsScreen.kt` + `SpellsViewModel.kt` |
| Детали заклинания | `ui/detail/SpellDetailScreen.kt` + `SpellDetailViewModel.kt` |
| Навигация | `ui/nav/AppNavigation.kt` |
| Снимок состояния главного экрана | `ui/home/HomeViewModel.kt` → `HomeState` |
| Single source of truth | `data/SpellStorage.kt` |
| Парсер CSV | `data/SpellParser.kt` |
| CI / Release | `.github/workflows/release.yml` |
| Версии | `gradle/libs.versions.toml` |
| AGP / KSP gotchas | комментарии в `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` |
