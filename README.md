# 🎲 Spell Tracker

[![GitHub release](https://img.shields.io/github/v/release/SiberianFoboZ/spell-tracker-dnd)](https://github.com/SiberianFofoZ/spell-tracker-dnd/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84)](https://developer.android.com)
[![CI: Release](https://img.shields.io/badge/CI-GitHub_Actions-2088FF)](.github/workflows/)

Android-приложение для отслеживания ячеек заклинаний (spell slots) персонажа
**Dungeons & Dragons 5e** с поддержкой мультикласса по правилам **Player's Handbook**.

> Считает слоты по PHB-формуле, считает Pact Magic отдельно для Warlock,
> ничего не теряет при перезапуске.

## 📸 Скриншоты

| Главный экран | Список заклинаний | Меню фильтров |
|:-:|:-:|:-:|
| ![Главный экран](docs/screenshots/main.png) | ![Список](docs/screenshots/spells.png) | ![Drawer](docs/screenshots/drawer.png) |

> Скриншоты-имитации сгенерированы Python'ом для превью. Реальные снимки
> эмулятора можно получить вручную через `.github/workflows/screenshots.yml`
> (см. раздел «Сборка»).

## ✨ Возможности

- **9 классов** с поддержкой multiclass: Bard, Wizard, Druid, Cleric,
  Warlock, Paladin, **Ranger**, Sorcerer, **Artificer**
- **PHB-формула** эффективного уровня заклинателя:
  - *Full caster* (Bard, Cleric, Druid, Sorcerer, Wizard): `lvl`
  - *Half caster, round down* (Paladin, Ranger): `lvl / 2`
  - *Half caster, round up* (Artificer): `(lvl + 1) / 2`
  - *Warlock (Pact Magic)*: отдельный счётчик, не суммируется
- **Каталог заклинаний** импортируется из `assets/*.json` / `*.csv` для
  каждого класса
- **Фильтры** в боковом меню: школа, уровень, концентрация, ритуал,
  только подготовленные
- **Сброс ячеек** — отметка «длинного отдыха» (long rest) одним тапом
- **Edge-to-edge UI** на Material 3, поддержка Android 7.0+ (API 24+)
- **Локализация** интерфейса на русском

## 📥 Установка

1. Откройте раздел [Releases](https://github.com/SiberianFoboZ/spell-tracker-dnd/releases)
2. Скачайте последний `spell-tracker-vX.Y.apk`
3. Установите APK на устройство (Android 7.0+)

> APK подписан **debug-ключом** (для личного использования). Для публикации
> в Google Play замените signing config в `app/build.gradle.kts` на release-keystore.

## 🔧 Сборка из исходников

```bash
git clone https://github.com/SiberianFoboZ/spell-tracker-dnd.git
cd spell-tracker-dnd
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/spell-tracker-v1.0.apk
```

**Требования к окружению:**
- JDK 21 (Gradle 9.3.1 + AGP 9.1.1 требуют именно 21)
- Android SDK с `compileSdk = 36` (build-tools 36.0.0)

На Windows JDK 21 указывается в `C:\Users\<user>\.gradle\gradle.properties`
(вне репозитория), потому что `foojay.io` не справляется со скачиванием
для этой версии. На Linux/macOS — поставьте JDK 21 любым удобным способом
(`brew install openjdk@21`, `sdkman`, системный пакет) — проектный
`gradle.properties` портативный.

## 🧪 Использование

1. Запустите приложение
2. Введите уровни ваших классов в поля (например, Bard 5, Warlock 3)
3. Эффективный уровень заклинателя и ячейки пересчитаются автоматически
4. Тапните ячейку, чтобы отметить её использованной
5. Кнопка **«Сбросить ячейки»** — это long rest: восстанавливает всё
6. Боковое меню → **«К заклинаниям»** — полный список с фильтрами

### Пример multiclass-расчёта

| Класс | Уровень | Вклад в caster level |
|-------|---------|---------------------|
| Bard | 5 | +5 (full) |
| Paladin | 6 | +3 (half, round down) |
| Artificer | 1 | +1 (half, round up) |
| Warlock | 3 | +0 (pact magic — отдельно) |
| **Эфф. ур-нь** |   | **9** |

При 9 уровне ячейки по таблице PHB: 4 / 3 / 3 / 3 / 1 / 0 / 0 / 0 / 0
плюс 2 слота 2-го уровня от Warlock (Pact Magic).

## 🏗️ Архитектура

- **Язык**: Java 11 (compileSdk), Java 21 (среда сборки)
- **Build**: Gradle 9.3.1 + Android Gradle Plugin 9.1.1
- **БД**: Room 2.6.1 (заклинания, состояние ячеек)
- **UI**: Material 3, RecyclerView, ViewPager2, Fragment
- **Паттерн**: Multi-Activity + Repository
- **Импорт данных**: CSV/JSON в `app/src/main/assets/`

## 🚀 CI/CD

`.github/workflows/release.yml` — на каждый push тега `v*` (например,
`v1.0.0`):

1. Поднимает JDK 21 (Temurin) + Android SDK 36
2. Собирает release APK
3. Переименовывает в `spell-tracker-v{versionName}.apk`
4. Создаёт GitHub Release с прикреплённым APK

`.github/workflows/screenshots.yml` — ручной запуск (`workflow_dispatch`):

1. Запускает эмулятор API 30
2. Устанавливает debug APK
3. Снимает 3 скриншота через `adb shell screencap`
4. Загружает как Actions-артефакт `screenshots/`

Запуск:
```bash
gh workflow run screenshots.yml
# или: GitHub UI → Actions → Screenshots → Run workflow
```

## 🤝 Contributing

PR'ы приветствуются. Для добавления нового класса:

1. Положите `assets/класс.json` (формат как у `bard.json`)
2. Добавьте запись в `Classes.java`:
   ```java
   new Info("id", "Отображение", "класс.json", 1.0, false)
   ```
   - `1.0` = full caster
   - `0.5` + `false` = half caster, round down (Paladin, Ranger)
   - `0.5` + `true` = half caster, round up (Artificer)
3. Добавьте строку в `activity_main.xml` и строку в `strings.xml`
4. Соберите и проверьте, что ячейки считаются

## 📄 Лицензия

[MIT](LICENSE)
