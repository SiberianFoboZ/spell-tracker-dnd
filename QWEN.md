# Spell Tracker — QWEN Context

> Инструкции для Qwen Code по работе с этим проектом.
> Проект — Android-приложение на Kotlin + Jetpack Compose для отслеживания
> ячеек заклинаний D&D 5e по правилам PHB. Все строки UI — на русском.

---

## 1. Что это за проект

**Spell Tracker** — локальное Android-приложение, которое помогает магу
D&D 5e следить за ячейками заклинаний, пакт-магией Колдуна и арканумами
на одном экране. Без аккаунтов, без аналитики, без интернета (после
установки APK работает полностью офлайн).

- **applicationId / namespace**: `com.example.spelltracker`
- **Текущая версия**: v2.8.0 (`versionCode = 19`)
- **Min SDK**: 24 (Android 7.0). **Target/Compile SDK**: 36 (Android 16).
- **Язык UI**: русский. Все строки — в `app/src/main/res/values/strings.xml`,
  плюс хардкод русских строк внутри `*Screen.kt` (см. раздел Conventions).
- **Хранилище состояния**: SharedPreferences (слоты, уровни, prepared,
  мульти-персонажи) + Room БД `spells.db` (справочник заклинаний).
- **Build-time pipeline**: Gradle-таска `generateSpellsDb` нормализует
  1000 per-spell JSON из `spells_data/` в ОДИН `spells_normalized.json`,
  который уходит в APK как asset. На устройстве JSON читается ОДИН раз
  при первом запуске и заливается в Room.
- **Объём справочника**: 487 заклинаний (после удаления 3rd party / homebrew
  из `spells_data/` и whitelist-фильтра по `class-subclass.txt`).
  Включая 44 кантипа (level 0).
- **Игнор-лист классов** (16 классов — homebrew/UA, выкидываются при сборке):
  Шаман, Магус, Хранитель Рун, Савант, Неупокоенная душа, Мистик,
  Кровавый Охотник, Звездочет, Егерь, Воевода, Альтернативный плут,
  Альтернативный монах, Альтернативный воин, Альтернативный варвар,
  Альтернативный следопыт, Алхимик.
- **Версионирование**: «Этап N» в коммит-сообщениях + SemVer-теги
  (`v2.6.0`).

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
│   ├── build.gradle.kts                    # AGP/KSP/Room/Compose + GenerateSpellsDbTask
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   ├── class_*.json                # legacy per-class файлы (НЕ читаются кодом)
│       │   └── spells_normalized.json      # 2.7 MB, build-time артефакт (487 спеллов)
│       ├── java/com/example/spelltracker/
│       │   ├── MainActivity.kt
│       │   ├── data/                       # слои данных
│       │   │   ├── Character.kt            # CharacterData + JSON сериализация
│       │   │   ├── Classes.kt              # 11 классов (9 PHB + 2 mystic)
│       │   │   ├── ClassFilter.kt          # matches(spell, SpellFilterState)
│       │   │   ├── ComponentFlag.kt        # V / S / M / RC для multi-select
│       │   │   ├── CustomSlot.kt           # кастомные ячейки (Breath, Lay on Hands, ...)
│       │   │   ├── DieType.kt              # d6/d8/d10/d12/★ для CustomSlot
│       │   │   ├── HpState.kt              # HpState + HitDiceState + HitDie (Этап HP)
│       │   │   ├── Spell.kt                # @Entity (24 поля)
│       │   │   ├── SpellDao.kt
│       │   │   ├── SpellDatabase.kt        # version=5, fallbackToDestructive
│       │   │   ├── SpellFilterState.kt     # data class со всеми осями фильтра
│       │   │   ├── SpellMenuConfig.kt      # 33 источника / 8 школ / ComponentFlag
│       │   │   ├── SpellParser.kt          # читает единый normalized.json
│       │   │   ├── SpellRepository.kt      # Room + ensureInitialized + orphan cleanup
│       │   │   ├── SpellStorage.kt         # SharedPreferences + multi-character + HP
│       │   │   └── TriState.kt             # YES / NO / ANY для ritual / concentration
│       │   ├── util/
│       │   │   └── Xoroshiro128Plus.kt     # PRNG для бросков кубиков (Этап HP)
│       │   └── ui/
│       │       ├── characters/             # CharactersScreen + CharactersViewModel
│       │       ├── common/                 # swipeableNavigation (карусельный свайп)
│       │       ├── customslot/             # EditCustomSlotScreen + ViewModel
│       │       ├── detail/                 # SpellDetailScreen + SpellHtml (HTML→AnnotatedString)
│       │       ├── home/                   # HomeScreen + HomeViewModel
│       │       ├── hp/                     # HpScreen + HpDialogs + HpViewModel (Этап HP)
│       │       ├── nav/AppNavigation.kt
│       │       ├── settings/               # SettingsScreen + язык
│       │       ├── spells/                 # SpellsScreen + SpellsViewModel
│       │       └── theme/                  # Color, Theme, Type
│       └── res/values/                     # strings.xml, themes.xml
├── build.gradle.kts                        # пустой (только комментарий)
├── settings.gradle.kts                     # pluginManagement + версии
├── gradle.properties                       # daemon JVM args + KSP-флаг
├── gradle/libs.versions.toml               # все версии
├── gradle/wrapper/                         # gradle wrapper (9.3.1)
├── spells_data/                            # 1000 per-spell JSON (НЕ в app/, читается таской)
├── class-subclass.txt                       # reference маппинг подкласс→parent class
├── menu_json.txt                            # reference конфиг фильтр-меню
├── docs/                                   # документация
├── .github/workflows/
│   ├── release.yml                         # tag v* → APK + Release
│   └── screenshots.yml                     # ручной запуск эмулятора
├── README.md
├── LICENSE                                 # MIT
└── QWEN.md                                 # ← этот файл
```

> **Ключевое отличие от старого pipeline**: CSV и per-class JSON больше
> НЕ читаются кодом. Они остались в `assets/class_*.json` как legacy
> артефакты, но фактический источник данных — `spells_normalized.json`.

---

## 4. Архитектура

### 4.1 Pipeline сборки данных (build-time)

```
spells_data/*.json (1000 файлов, по одному на заклинание)
        │
        ▼  GenerateSpellsDbTask (app/build.gradle.kts)
        │
   • читает каждый JSON, парсит поля
   • денормализация:
     - school "вызов" → enum-key "CONJURATION"
     - classes[].url "/classes/druid" → English id "druid"
     - subclasses[]: parallel CSV имён + CSV parent class English id
   • regex-производные:
     - "расход" в components.m → materialConsumed=true
     - <span class="saving_throw">X</span> в HTML → savingThrows "X"
   • фильтрация: 16 ignored-классов выкидываются (целиком или частично)
   • дедуп по id через LinkedHashMap (на случай будущих пересечений)
        │
        ▼
app/build/generated/assets/spells_normalized.json
   (2.7 MB, 487 заклинания, single JSON-array)
        │
        ▼  AGP Variant API: addGeneratedSourceDirectory
APK assets/spells_normalized.json
```

### 4.2 Pipeline загрузки (runtime, при первом запуске)

```
APK assets/spells_normalized.json
        │  SpellParser.loadFromAssets() — ОДИН раз
        ▼
List<Spell> (487 объектов в памяти)
        │
        ▼  SpellRepository.ensureInitialized()
        │   if (dao.count() == 0 || dao.count() != 487) {
        │       dao.clearAll(); dao.insertAll(spells)
        │   }
        │   pruneOrphanedPrepared()  // удаляет сирот из SharedPreferences
        ▼
Room БД `spells.db` (487 записей)
        │
        ▼  SpellsViewModel получает данные через repo.getAll()
        │  Фильтрация — в памяти через ClassFilter.matches()
        ▼
SpellsScreen (LazyColumn)
```

> **Важно**: данные заклинаний НЕ хранятся в памяти как постоянная
> структура. Каждый запуск приложения читает Room. Чтение всего
> JSON в `List<Spell>` в память как замена Room **не предлагать** —
> проверено, приводит к memory pressure (см. memory `room-over-in-memory`).

### 4.3 MVVM + StateFlow

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

- **State** — только в `SpellStorage` (single source of truth).
  Любое изменение через `storage.setX(...)` пишет в `prefs.edit().apply()`
  И эмитит в `MutableStateFlow.update { ... }`.
- **Composable** — без побочных эффектов. Побочки — в `LaunchedEffect` /
  `ViewModel` (`SharedFlow<HomeEvent>`).
- **Navigation** — один `NavHost` (`ui/nav/AppNavigation.kt`),
  3 экрана: `home` → `spells` → `spell/{id}`.

### 4.4 Главный экран: 3 секции

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
- `fighter_mystic` / `rogue_mystic` (1/3 кастер) — отдельные записи
  в `Classes.kt`, считаются как `(lvl + 2) / 3`.

**Арканумы** (Этап 17, v2.4.0): по одной ячейке VI–IX уровня.
Открытие: warlockLevel 11/13/15/17 → 1/2/3/4 арканума. Восстановление —
**только длинный отдых** (по PHB).

**Пакт-магия** (Этап 18, v2.4.1): отдельная секция «МАГИЯ ДОГОВОРА»
над обычными ячейками. Восстанавливается на **коротком** отдыхе.
`HomeState.pactSlot: SlotInfo?` — `null`, если warlockLevel == 0
или `WARLOCK_SLOTS[warlockLevel][0] == 0`.

**Отдых (Этап 15, расширен Этапом HP):**
- `shortRest()` → сбрасывает только `usedPactSlots`. Арканумы
  остаются (если есть потраченные арканумы — эмитится
  `HomeEvent.ArcanumShortRestBlocked` → Snackbar-предупреждение).
- `longRest()` → сбрасывает `usedSlots[1..9]` + `usedPactSlots` +
  `usedArcanums[6..9]` + кастомные слоты (`used=0`) + **HP: current =
  max, temp = 0, hitDice.spent = 0** (см. Этап HP ниже). Class levels
  и prepared — **сохраняются**.
- В `SpellStorage.resetAllUsed()` (debug-only) — `prefs.edit().clear()`.

### 4.8 Этап HP: трекер ХП и Hit Dice (v2.8.0)

**Проблема**: Spell Tracker исторически был заточен под кастеров
(ячейки заклинаний, пакт-магия, арканумы), но игроку нужны и
базовые ресурсы — текущие/максимальные хиты, временные хиты (по PHB
поглощают урон первыми и не складываются), и Hit Dice для короткого
отдыха. Кастерские классы — частный случай; трекер должен работать
и для немаг. персонажей тоже.

**Решение — отдельный экран «Хиты»**:
1. **Новые модели в data-слое**:
   - `HpState(maxHp, currentHp, tempHp)` — обычные + temp HP.
   - `HitDiceState(total, spent, die: HitDie, conMod)` — Hit Dice пул.
   - `HitDie.{D6, D8, D10, D12}` — размеры кубика; `HitDiceState.healingPerDie()`
     оставлено как `max(1, die + conMod)`, но логика PHB-формулы теперь
     в `SpellStorage.spendHitDice(count, rolls)`.
2. **Хранение**: `CharacterData.hp: HpAndHitDice` — атомарный блоб,
   сериализуется в JSON-поле `hp` под ключом `char_data_${id}`. Для
   существующих персонажей (без поля) — безопасный default через
   `optJSONObject("hp")?.let { hpStateFromJson(it) } ?: HpState()`.
3. **StateFlow**: новый `SpellStorage.hpAndHitDice: StateFlow<HpAndHitDice>`
   — атомарный snapshot, мутации `setMaxHp/setCurrentHp/adjustCurrentHp/
   setTempHp/adjustTempHp/updateHitDice/adjustHitDiceTotal/adjustHitDiceConMod/
   spendHitDice`. Каждая мутация вызывает `persistCurrentCharacter()`.
4. **Формула хилинга** (PHB-faithful): `heal = sum(rolls) + conMod * count`,
   минимум `count` HP (PHB: «regain at least 1 HP per die»). Heal клампится
   в `0..maxHp - currentHp`. Каждый кубик бросается отдельно.
5. **Long rest**: `current = max`, `temp = 0`, **`hitDice.spent = 0`**
   (все кубики доступны — отход от строгого PHB `ceil(total/2)`, по
   выбору пользователя).
6. **PRNG**: `util/Xoroshiro128Plus.kt` — public domain реализация
   xoroshiro128+ (период 2^128−1), unbiased rejection sampling.
   Singleton `Xoroshiro128Plus.instance` через `lazy`. Причина
   замены — стандартный `java.util.Random` (LCG, 48-bit) даёт
   субъективно «статичные» средние при коротких сериях.
7. **UI — `HpScreen.kt`**:
   - Карточка «Здоровье» (текущее/max HP в подложке `BgDark.copy(α=0.6f)`)
   - Карточка «Кость здоровья» (тип кубика, available, conMod)
   - Bottom-bar `RestButtonsBar` (точный клон из HomeScreen) —
     короткий отдых открывает диалог «Потратить кубик», длинный —
     сбрасывает HP и Hit Dice
   - **Кнопка «Потратить» удалена из карточки Hit Dice** — точка входа
     одна (short rest в bottom-bar), карточка только информационная
   - Стиль: радиальный градиент `ScreenGradient`, скругления 12.dp
     (как строки ячеек), системный Snackbar (без custom override)
8. **Диалог `HitDiceSpendDialog`**:
   - Поле количества кубиков 1..available
   - Кнопка «Бросить» (иконка `Casino`) — бросает сразу `count`
     кубиков через `Xoroshiro128Plus`, результат — список «3, 5, 7»
   - Превью: «Бросок: d8», список, «+ CON × 3 = +6»,
     «Итого восстановлено: 21 HP»
   - Кнопка «Применить» (вместо старой «Потратить»)
9. **Навигация — карусельный свайп**:
   - Экран Хитов открывается **свайпом влево** с HomeScreen
     (раньше это был экран «Персонажи»).
   - Карусель: `Home ↔ HP ↔ Characters ↔ Home` (зациклена).
     Settings вынесен из карусели — открывается только шестерёнкой
     в Home TopAppBar.
   - Логика свайпа — `Modifier.swipeableNavigation(...)` в
     `ui/common/HorizontalSwipeHandler.kt`. Совместимо с
     вертикальной прокруткой (Compose отменяет горизонтальный
     жест при вертикальном драге).
10. **Локализация**: новые ключи в `values/strings.xml` (RU) и
    `values-en/strings.xml` (EN): `hp_title`, `hp_section_hp`,
    `hp_section_hit_dice`, `hp_current_label`, `hp_max_label`,
    `hp_temp_label`, `hp_temp_description`, `hp_value_format`,
    `hp_temp_value_format`, `hp_empty_hint`, `hp_edit_dialog_title`,
    `hp_edit_field_*`, `hp_edit_apply`, `hp_edit_decrease_*/increase_*`,
    `hit_dice_label_*`, `hit_dice_value_*_format`, `hit_dice_con_mod_format`,
    `hit_dice_spend_title/body/button`, `hit_dice_no_available/no_max_hp/
    not_set`, `hp_snackbar_long_rest_done`, `hp_snackbar_hit_dice_spent`,
    `hp_long_rest_action`, `hit_dice_roll_button/_content_description`,
    `hit_dice_rolls_format`, `hit_dice_rolls_label`,
    `hit_dice_con_bonus_format`, `hit_dice_total_heal_format`,
    `hit_dice_apply`.

### 4.5 Фильтр подклассов (новое — Этап N+1)

**Проблема**: подклассов в датасете 110+, в BottomSheet-чипах это слишком
громоздко. Нужен parent class для группировки.

**Решение:**
1. **Build-time**: `Spell.subclassParents: String` (CSV English id,
   параллельно `subclasses`). Например,
   `subclasses="Домен Войны,Клятва Мести,..."`,
   `subclassParents="cleric,paladin,..."`.
2. **Runtime**: `SpellsViewModel.subclassToParents: Map<String, Set<String>>`
   — ленивый, считается один раз из allSpellsSnapshot.
3. **UI**: `displayedSubclasses` реактивный getter:
   - `classIds.isEmpty()` → пустое множество (секция **скрыта**)
   - иначе → только подклассы, parent которых ∈ classIds
4. **BottomSheet**: секция «Подкласс» рендерится только когда
   `displayedSubclasses.isNotEmpty()`.

### 4.6 Multi-select компонентов (новое — Этап N+1)

Заменены 4 TriState-строки на ОДИН multi-select row:
- `ComponentFlag.{V, S, M, RC}` — кликабельные чипы
- AND-семантика: спелл проходит, если для каждого выбранного
  компонента спелл его имеет (PHB-нотация «В, С» = оба нужны)
- Расовый фильтр удалён (не имел осмысленного UX)

### 4.7 Реактивность: как избежать «дрейфа» в compose-эффектах

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
@Database(entities = [Spell::class], version = 5, exportSchema = false)
abstract class SpellDatabase : RoomDatabase() { ... }
```

- **24 поля**: `id, name, nameEng, source, sourceGroup, level, school,
  ritual, concentration, timecast, distance, duration, componentV/S/M,
  materialConsumed, materialDesc, descriptionHtml, upperLevel, url,
  classes, subclasses, subclassParents, races, savingThrows`.
- **Объём**: 487 записей (после destructive rebuild при mismatch count).
- **Миграция**: `fallbackToDestructiveMigration()` — данные приходят
  из assets, потеря пользовательских записей невозможна (это справочник).
- **Авто-реконструкция**: `SpellRepository.ensureInitialized()`
  сравнивает `dao.count()` с `loadFromAssets().size`. Если не совпало
  (например, APK обновился с 802 на 487 спеллами), `clearAll()` + `insertAll()`
  перезаливает таблицу.
- **Cleanup orphan'ов**: после миграции `pruneOrphanedPrepared()`
  удаляет из `SpellStorage.prepared` все id, которых больше нет в БД
  (актуально после v3→v5, где id сменился с hashCode на numeric).

### Источник данных

```
spells_data/*.json (1000 файлов)
        ↓  GenerateSpellsDbTask
spells_normalized.json (в APK как asset)
        ↓  SpellParser.loadFromAssets()
List<Spell>
        ↓  dao.insertAll()
Room БД
```

**Дополнительные reference-файлы в корне проекта:**
- `class-subclass.txt` — JSON со списком классов и их подклассов
  (id=1..14). Используется как reference для маппинга подкласс→parent,
  но **сам не парсится в build-time** — маппинг строится из raw JSON
  spell + classNameToId.
- `menu_json.txt` — референс конфиг фильтр-меню (источники, школы и т.п.).
  Не используется runtime напрямую; значения hardcoded в
  `SpellMenuConfig.kt`.

### Parser (`SpellParser.kt`)

- Один файл `spells_normalized.json` — JSON-массив объектов.
- Поля 1:1 совпадают с `Spell` (см. §5).
- Нет regex, нет per-class маппинга, нет ничего — всё уже нормализовано
  build-time.

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
- **Циклы в Composable**: только `for (x in collection) { ... }`.
  `collection.forEach { ... }` лямбда **НЕ** @Composable — внутри неё
  нельзя вызывать Composable-функции. (Все Composable-циклы в этом
  проекте — `for`.)

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
# Только build-time нормализация (без полной сборки)
./gradlew generateSpellsDb
# → app/build/generated/assets/spells_normalized.json

# Сборка debug APK (запустит generateSpellsDb как часть pipeline)
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk (переименован в spell-tracker-v2.6.0.apk)

# Сборка release APK (подписан debug-ключом, если нет keystore.properties)
./gradlew assembleRelease
# → app/build/outputs/apk/release/spell-tracker-v2.6.0.apk

# Установка на устройство
adb install -r app/build/outputs/apk/release/spell-tracker-v2.6.0.apk

# Тесты
./gradlew test                  # unit (только ExampleUnitTest на данный момент)
./gradlew connectedAndroidTest  # instrumented
```

**Windows (PowerShell):**
```powershell
.\gradlew.bat assembleDebug
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

Генерация: `keytool -genkeypair ...` вручную. Шаблон параметров —
`keystore.properties.example`.

**Пароли `storePassword` и `keyPassword` одинаковые** — PKCS12 (по умолчанию в
современной JDK) не поддерживает разные пароли для store/key.

**Не коммитить `keystore/`, `keystore.properties`** (в `.gitignore`). Для совместной
разработки передавайте keystore.properties (и при необходимости сам keystore)
**отдельно** от кода по защищённому каналу.

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
4. Поставить тег: `git tag v2.6.0 && git push origin v2.6.0`.
5. CI собирает APK и публикует GitHub Release автоматически
   (`softprops/action-gh-release@v2` с `generate_release_notes: true`).

Стиль существующих коммитов:
- `feat(subclass): parent class в normalized + скрывать секцию без выбранного класса`
- `fix: пересобирать БД при mismatch count (802 → 487 при обновлении APK)`
- `fix: чистить orphan id в prepared при старте (после v3→v4)`
- `chore: удалить легаси assets/spells.csv + assets/databases/populate.sql`
- `refactor: объединить spells_data/ + losses/ в единую папку spells_data/`
- `feat(spellbook): schema v4 + расширенные фильтры (Этап N)`

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

### Добавить новое заклинание
1. Создать `spells_data/<Название>.json` по образцу существующих.
2. Убедиться что класс — не в игнор-листе, иначе спелл будет выкинут.
3. `./gradlew generateSpellsDb` — нормализация в `spells_normalized.json`.
4. `./gradlew assembleDebug` → тестирование.

### Добавить новый класс D&D 5e (например, Монах)
1. `data/Classes.kt` → новый `Info(...)` с `factor`, `roundUp`, `id`.
2. Дополнить `classNameToId` map в `app/build.gradle.kts` (Rus→English).
3. Удалить класс из `ignoredClasses` set (если он там был).
4. Запустить `generateSpellsDb` — все спеллы класса автоматически появятся.
5. `assembleDebug` → тест в эмуляторе.

### Изменить фильтр-меню (добавить новую ось)
1. Добавить поле в `SpellFilterState` + `setX`/`toggleX` в VM.
2. Добавить правило в `ClassFilter.matches()`.
3. Добавить секцию в `FiltersBottomSheet` (внутри `SpellsScreen.kt`).
4. Увеличить `activeFilterCount` если ось считается.

### Изменить палитру
Все цвета — в `ui/theme/Color.kt` (`AppColors`). Material 3 mapping — в
`ui/theme/Theme.kt` (`DarkColors`). Тёмная тема форсирована —
`isSystemInDarkTheme()` игнорируется, осветлённая схема не предусмотрена.

### Поменять иконку приложения
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

### Обновить build-time данные после изменения spells_data/
Запустить `./gradlew generateSpellsDb` вручную (приложит пересобранный
`spells_normalized.json` в `build/generated/assets/`). Или просто
`./gradlew assembleDebug` — таска вызывается через `mergeAssets`.

---

## 11. Чего НЕ делать

- **Не** предлагать убрать Room-базу `spells.db` в пользу прямого
  чтения `assets/spells_normalized.json` в `List<Spell>` в памяти.
  Проверено: memory pressure, медленная работа. Архитектура
  `assets → insertAll → Room → filter в памяти` зафиксирована.
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
- **Не** использовать `collection.forEach { ... }` для рендера
  Composable — лямбда не @Composable. Только `for (x in collection)`.
- **Не** удалять `assets/class_*.json` без подтверждения (хотя они не
  читаются runtime — могут пригодиться как reference).

### 11.1 Остаточный русский текст в data-слое (намеренно)

После Этапа 25 весь UI чист (`grep "text\s*=\s*\"[А-Я]` → 0 совпадений),
но в data-слое остаются русские литералы **по делу**:

| Файл | Что | Почему не локализуем |
|------|-----|----------------------|
| `data/Classes.kt` | `assetFile: String = "бард.json"` | Legacy filename, помечен `@Suppress("unused")`, runtime не читается. Удалить в Этапе N+2 когда полностью избавимся от legacy-структуры |
| `data/SpellMenuConfig.SAVING_THROWS.key` | `"Сила"`, `"Ловкости"` (род. падеж) | Это **ключи** для матчинга сохранённых данных в БД (где они лежат в родительном падеже как в HTML). Локализованный лейбл — в `labelRes` |
| `data/TriState.kt` парсер | `"да"`, `"нет"` в `fromString` | Legacy-парсинг сохранённых значений; новый код использует enum напрямую |
| `data/SpellStorage.kt` дефолты | `"Без имени"`, `"Персонаж 1"` | Fallback для пустого ввода. Пользователь сразу переименовывает; внутренние дефолты не показываются в UI как «переведённые». Тоже кандидат на `Context.getString()` если появится надобность |
| Комментарии + `@Deprecated("...")` | Пояснения в коде | Код читается разработчиком (вероятно, на русском); пользователь их не видит |

Принцип: локализуем то, что **видит пользователь**. Внутренние
идентификаторы, ключи для матчинга данных, fallback'и для edge-case'ов
оставляем как есть, чтобы не размывать границу между «данные» и
«представление».

## 13. Интернационализация (i18n) и миграции

### 13.1 Поддерживаемые локали

- **Русский** (`values/strings.xml`) — дефолт.
- **Английский** (`values-en/strings.xml`) — добавляется в Этапе 25.

Добавление нового языка = создать `res/values-XX/strings.xml` с тем же
набором ключей + добавить `<locale android:name="XX"/>` в
`res/xml/locale_config.xml`.

### 13.2 Локаль в runtime

Два механизма, выбираются платформой автоматически:

| API | Механизм | Где настраивается |
|-----|----------|-------------------|
| 33+ (Android 13+) | Системные настройки (Settings → Apps → язык) | `android:localeConfig="@xml/locale_config"` в `AndroidManifest.xml` |
| 24..32 | `AppCompatDelegate.setApplicationLocales(...)` | `MainActivity.onCreate` (восстанавливает + переключатель в UI) |

Per-app локаль персистится через AppCompat Storage Service (API 24..32) —
пользователю не нужно выбирать язык заново после перезапуска.

### 13.3 Переключатель в UI

Иконка-глобус (`Icons.Filled.Public`) в TopAppBar главного экрана →
`DropdownMenu` с пунктами «Русский» / «English». Тап по пункту вызывает
`AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"|"en"))`,
после чего Activity пересоздаётся системой и Compose подхватывает
новую локаль через `stringResource(...)`.

На API 33+ в системных настройках появляется тот же список языков
(благодаря `locale_config.xml`).

### 13.4 Локализация data-слоя

`Classes`, `SpellMenuConfig`, `CustomSlot` хранят стабильные ключи
(English id, Russian именительный/родительный падеж) и ссылки на
`@StringRes Int`. UI резолвит через `stringResource(...)`:

```kotlin
// data/Classes.kt
data class Info(
    val id: String,           // "warlock" — стабильный ключ
    @Suppress("unused") val assetFile: String,
    val factor: Double,
    val roundUp: Boolean,
    val isThirdCaster: Boolean = false,
    // name вынесено: см. ClassNames.kt
)

// ClassNames.kt
@StringRes
fun Classes.Info.nameRes(): Int = ClassNames.resFor(id)

// В UI:
Text(stringResource(info.nameRes()))
```

Аналогично:
- `SpellMenuConfig.Source/SourceGroup/School/SavingThrow` → `labelRes: Int`
- `ComponentFlag.labelRes: Int` (В/С/М/Cons)
- `RestType.displayNameRes: Int` (Короткий/Длинный / Short/Long)
- `TriState` через `TRI_ANY_LABEL: Int` и т.д.

Хранение ключей + ресурсов разделено, чтобы data-слой не зависел от R.

### 13.5 Миграции Room (Этап 25)

`SpellDatabase.get(context)` теперь регистрирует массив миграций:

```kotlin
private val MIGRATIONS: Array<Migration> = arrayOf(
    // MIGRATION_5_6, // ← раскомментировать при первом изменении схемы
)
.addMigrations(*MIGRATIONS)
.fallbackToDestructiveMigration()   // safety-net
.build()
```

Workflow при изменении схемы:
1. Изменить `Spell` (добавить поле, переименовать)
2. Поднять `version` в `@Database(...)`
3. Добавить `MIGRATION_N_M` (написать SQL в `migrate(db)`)
4. Положить его в `MIGRATIONS`
5. Можно убрать `.fallbackToDestructiveMigration()`, если уверены,
   что все пути апгрейда покрыты

`fallbackToDestructiveMigration()` оставлен как **safety-net**:
если на устройстве лежит БД версии, для которой миграции не
написаны (например, debug-сборка с экспериментальной схемой), Room
дропнет таблицу и перечитает данные из `spells_normalized.json`.
Пользовательские `prepared` id при этом чистятся отдельным проходом
через `SpellRepository.pruneOrphanedPrepared()` (логика из v2.6.0).

## 12. Файлы-якоря (быстрая навигация)

| Что ищешь | Где |
|-----------|-----|
| Локализованные строки (RU) | `res/values/strings.xml` |
| Локализованные строки (EN) | `res/values-en/strings.xml` |
| Per-app locale config (API 33+) | `res/xml/locale_config.xml` |
| Карта class_id → @StringRes | `ClassNames.kt` |
| Переключатель языка (UI) | `ui/home/HomeScreen.kt` → `LanguageSwitcherAction` |
| Восстановление локали при старте | `MainActivity.kt` → `onCreate` |
| Миграции Room | `data/SpellDatabase.kt` → `MIGRATIONS` |
| Build-time нормализация спеллов | `app/build.gradle.kts` → `GenerateSpellsDbTask` |
| Состояние фильтров (snapshot) | `data/ClassFilter.kt` → `SpellFilterState` |
| Enum компонентов | `data/SpellMenuConfig.kt` → `ComponentFlag` |
| TriState YES/NO/ANY | `data/TriState.kt` |
| Room-entity заклинания | `data/Spell.kt` (24 поля) |
| Room-DAO запросы | `data/SpellDao.kt` |
| Парсер JSON → Spell | `data/SpellParser.kt` |
| Repository + orphan cleanup | `data/SpellRepository.kt` |
| SharedPreferences + персонажи | `data/SpellStorage.kt` |
| Цвета и тема | `ui/theme/Color.kt`, `ui/theme/Theme.kt` |
| Главный экран (3 секции) | `ui/home/HomeScreen.kt` + `HomeViewModel.kt` |
| Список заклинаний + 13 секций фильтра | `ui/spells/SpellsScreen.kt` + `SpellsViewModel.kt` |
| Детали заклинания (HTML render) | `ui/detail/SpellDetailScreen.kt` + `SpellDetailViewModel.kt` |
| HTML → AnnotatedString парсер | `ui/detail/SpellHtml.kt` |
| Навигация | `ui/nav/AppNavigation.kt` |
| Снимок состояния главного экрана | `ui/home/HomeViewModel.kt` → `HomeState` |
| Single source of truth (state) | `data/SpellStorage.kt` |
| HP и Hit Dice (модели) | `data/HpState.kt` → `HpState`, `HitDiceState`, `HitDie` |
| Экран Хиты | `ui/hp/HpScreen.kt` + `HpViewModel.kt` |
| Диалоги HP/Hit Dice | `ui/hp/HpDialogs.kt` |
| Карусельный свайп (3 экрана) | `ui/common/HorizontalSwipeHandler.kt` → `Modifier.swipeableNavigation` |
| PRNG для бросков кубиков | `util/Xoroshiro128Plus.kt` |
| Мульти-персонажи | `data/Character.kt` + `ui/characters/CharactersScreen.kt` |
| Кастомные ячейки | `data/CustomSlot.kt` + `ui/customslot/EditCustomSlotScreen.kt` |
| Настройки (язык) | `ui/settings/SettingsScreen.kt` |
| CI / Release | `.github/workflows/release.yml` |
| Версии | `gradle/libs.versions.toml` |
| AGP / KSP gotchas | комментарии в `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` |
| Build-time таска нормализации | `app/build.gradle.kts` → `GenerateSpellsDbTask` |
| Исходные данные (1000 JSON) | `spells_data/` |
| Маппинг подклассов (reference) | `class-subclass.txt` |
| Референс фильтр-меню | `menu_json.txt` |
| Output pipeline (1 файл в APK) | `app/build/generated/assets/spells_normalized.json` |