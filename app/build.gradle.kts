import java.io.FileInputStream
import java.util.Properties
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
    // org.jetbrains.kotlin.android не указываем явно: AGP 9.x авто-применяет
    // его, если плагин есть в classpath (объявлен в pluginManagement).
    id("org.jetbrains.kotlin.plugin.compose")
    // KSP: обрабатывает Kotlin-аннотации (Room @Database/@Dao/@Entity и т.п.)
    // и генерирует SpellDatabase_Impl во время компиляции.
    id("com.google.devtools.ksp")
}

// Этап 23: чтение release-keystore из keystore.properties (файл в
// .gitignore, лежит в корне). Если файла нет — release падает на
// debug-keystore (старое поведение до Этапа 23), чтобы ничего не
// сломать у тех, кто не генерировал keystore.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = "com.example.spelltracker"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.spelltracker"
        minSdk = 24
        targetSdk = 36
        versionCode = 15
        versionName = "2.5.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables { useSupportLibrary = true }
    }

    // Этап 23: конфиг подписи release. Объявлен ДО buildTypes,
    // чтобы buildTypes.release мог на него сослаться. Создаётся
    // только если задан keystore.properties.
    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                // storeFile в keystore.properties указан относительно
                // КОРНЯ проекта (там же лежит keystore.properties) —
                // rootProject.file(...) резолвит путь от корня, а не от
                // app/, как сделал бы голый file(...).
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            // Этап 23: подписываем release APK личным keystore вместо
            // debug, чтобы при установке на устройство не было
            // предупреждения «не из безопасных источников» и
            // «подписано отладочным ключом». Если keystore.properties
            // не найден — fallback на debug-keystore для совместимости.
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // lint-vital в release иногда валится на file-lock под Windows,
    // когда в кеше остаются хэндлы от прошлой попытки. На сам APK
    // не влияет — отключаем.
    lint {
        checkReleaseBuilds = false
    }
    // Имя итогового APK: spell-tracker-v{versionName}.apk
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

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // JVM-таргет для Kotlin: 17 (заменяет устаревший kotlinOptions).
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    // KSP-процессор для Room (заменяет annotationProcessor — тот не работает на Kotlin)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// ─── Этап N: build-time preprocessing справочника заклинаний ───
//
// Задача собирает 843 (или сколько есть) JSON-файлов из `spells_data/`
// в корне проекта в ОДИН `spells_normalized.json`, который:
//   1. клаётся в `app/build/generated/assets/` (build dir, .gitignore),
//   2. подцепляется к Android `assets` source set — попадает в APK,
//   3. парсится на устройстве ОДИН раз при первом запуске приложения.
//
// Плюсы:
//   • runtime: 1× JSON-parse вместо 843×;
//   • денормализация: enum-keys, regex-флаг "расходуемый компонент",
//     parent-class из subclasses[], saving throws из HTML —
//     делаются один раз на сборочной машине, не на телефоне;
//   • удаление «нежелательных» классов (homebrew/UA) — без копий в APK.
//
// Выход и вход объявлены как AbstractTask properties — Gradle сам
// отслеживает аптайм между сборками (если ничего не менялось — skip).

abstract class GenerateSpellsDbTask : DefaultTask() {

    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    /**
     * Куда положить сгенерированный артефакт. Ожидаем
     * `DirectoryProperty`, потому что AGP Variant API
     * `addGeneratedSourceDirectory` ищет директорию-источник,
     * а не файл. Внутри создаём `spells_normalized.json`.
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun execute() {
        val src = sourceDir.get().asFile
        val dir = outputDir.get().asFile
        dir.mkdirs()
        val out = dir.resolve("spells_normalized.json")

        // Классы, которые вычёркиваем (homebrew/UA, не нужны для трекера).
        // Имена — как они приходят в JSON `classes[].name`. Любой спелл,
        // у которого после вычёркивания НЕ осталось ни classes[], ни
        // parent-классов в subclasses[], — дропается целиком.
        val ignoredClasses = setOf(
            "Шаман", "Магус", "Хранитель Рун", "Савант", "Неупокоенная душа",
            "Мистик", "Кровавый Охотник", "Звездочет", "Егерь", "Воевода",
            "Альтернативный плут", "Альтернативный монах",
            "Альтернативный воин", "Альтернативный варвар",
            "Альтернативный следопыт", "Алхимик",
        )

        // Маппинг русского имени класса → English id из Classes.kt.
        // Совпадает с тем, что лежит в `SpellStorage.computeCasterLevel()`.
        val classNameToId = mapOf(
            "Бард" to "bard",
            "Волшебник" to "wizard",
            "Друид" to "druid",
            "Жрец" to "cleric",
            "Колдун" to "warlock",
            "Паладин" to "paladin",
            "Следопыт" to "ranger",
            "Чародей" to "sorcerer",
            "Изобретатель" to "artificer",
            // Официальные 1/3-кастеры — архетипы Воина и Плута
            "Мистический Рыцарь" to "fighter_mystic",
            "Мистический Ловкач" to "rogue_mystic",
        )

        // Школа магии: в JSON приходит свободным RU ("вызов"),
        // menu_json.txt — enum-key ("CONJURATION"). Маппим для фильтра.
        val schoolToKey = mapOf(
            "вызов" to "CONJURATION",
            "воплощение" to "EVOCATION",
            "иллюзия" to "ILLUSION",
            "некромантия" to "NECROMANCY",
            "ограждение" to "ABJURATION",
            "очарование" to "ENCHANTMENT",
            "преобразование" to "TRANSMUTATION",
            "прорицание" to "DIVINATION",
        )

        // Для классов, не нашедшихся в `classNameToId`, English id
        // берём из URL: "/classes/druid" → "druid". Это покрывает
        // домашние/нестандартные имена автоматически.
        val idFromUrl = """/classes/(\w+)""".toRegex()

        // Спасбросок лежит в HTML-описании как
        // <span class="saving_throw">Мудрости</span>.
        val stRegex = Regex("""<span class="saving_throw">([^<]+)</span>""")

        // Маркер «расходуемый» в тексте материального компонента.
        val consumedRegex = Regex("расход", RegexOption.IGNORE_CASE)

        // Словарь для дедупликации: при повторе id последняя запись побеждает.
        // В текущем одно-папочном pipeline'е дубли быть не должны, но
        // на случай грядущих правок держим ту же логику, что и раньше.
        val byId = LinkedHashMap<Long, Map<String, Any?>>()

        val slurper = JsonSlurper()
        var total = 0
        var dropped = 0
        var stripped = 0
        val errors = mutableListOf<String>()

        val files = src.listFiles { f -> f.isFile && f.extension == "json" } ?: emptyArray()
        for (file in files.sortedBy { it.name }) {
            total++
            try {
                @Suppress("UNCHECKED_CAST")
                val raw = slurper.parse(file) as Map<String, Any?>
                val result = normalize(
                    raw, ignoredClasses, classNameToId, schoolToKey,
                    idFromUrl, stRegex, consumedRegex,
                )
                if (result != null) {
                    val id = (raw["id"] as? Number)?.toLong() ?: continue
                    if (byId.containsKey(id)) {
                        // Дубль по id — перезаписываем (последний побеждает)
                        stripped++
                    }
                    byId[id] = result.first
                    if (result.second) stripped++
                } else {
                    dropped++
                }
            } catch (e: Exception) {
                errors.add("${file.name}: ${e.message}")
            }
        }

        val out0 = byId.values.toList()

        out.writeText(JsonOutput.toJson(out0))

        logger.lifecycle("Spells: total=$total, kept=${out0.size}, dropped=$dropped (целиком из ignored-классов), stripped=$stripped (часть классов отфильтрована или дубль по id)")
        if (errors.isNotEmpty()) {
            logger.warn("Ошибок разбора: ${errors.size}")
            errors.take(20).forEach { logger.warn(" - $it") }
        }
    }

    /**
     * Возвращает null, если после фильтрации у спелла не осталось ни
     * одного «нашего» класса/подкласса → спелл дропается целиком.
     * Возвращает (нормализованный_спелл, был_ли_кто-то_отфильтрован).
     */
    private fun normalize(
        raw: Map<String, Any?>,
        ignoredClasses: Set<String>,
        classNameToId: Map<String, String>,
        schoolToKey: Map<String, String>,
        idFromUrlRegex: Regex,
        stRegex: Regex,
        consumedRegex: Regex,
    ): Pair<Map<String, Any?>, Boolean>? {
        @Suppress("UNCHECKED_CAST")
        val nameObj = raw["name"] as? Map<String, Any?> ?: return null
        val nameRu = nameObj["rus"] as? String ?: return null
        val nameEng = nameObj["eng"] as? String

        val id = (raw["id"] as? Number)?.toLong() ?: return null
        val level = (raw["level"] as? Number)?.toInt() ?: return null

        @Suppress("UNCHECKED_CAST")
        val components = raw["components"] as? Map<String, Any?>
        val compV = components?.get("v") as? Boolean ?: false
        val compS = components?.get("s") as? Boolean ?: false
        val compM = components?.get("m") as? String
        val hasMaterial = compM != null && compM.isNotBlank()
        val materialConsumed = hasMaterial && consumedRegex.containsMatchIn(compM)

        var strippedSomething = false
        @Suppress("UNCHECKED_CAST")
        val rawClasses = raw["classes"] as? List<Map<String, Any?>> ?: emptyList()
        val classesEng = rawClasses.mapNotNull { cls ->
            val name = cls["name"] as? String ?: return@mapNotNull null
            if (name in ignoredClasses) {
                strippedSomething = true
                null
            } else {
                classNameToId[name]
                    ?: cls["url"]?.toString()?.let { url ->
                        idFromUrlRegex.find(url)?.groupValues?.getOrNull(1)
                    }
            }
        }

        @Suppress("UNCHECKED_CAST")
        val rawSubclasses = raw["subclasses"] as? List<Map<String, Any?>> ?: emptyList()
        // Собираем подклассы параллельно с parent class English id (для
        // runtime-фильтрации «покажи подклассы выбранного класса»).
        // Оба списка синхронизированы по индексу — на i-м месте имя
        // подкласса и его parent class id.
        val subclassNames = mutableListOf<String>()
        val subclassParents = mutableListOf<String>()
        for (sub in rawSubclasses) {
            val name = sub["name"] as? String ?: continue
            val parentRus = sub["class"] as? String ?: continue
            if (parentRus in ignoredClasses) {
                strippedSomething = true
                continue
            }
            val parentEng = classNameToId[parentRus]
            if (parentEng == null) {
                // Неизвестный parent class (например, класс «Монах»
                // которого нет в Classes.kt). Тихо пропускаем — не
                // считаем это stripped'ом, но и не сохраняем, потому
                // что для фильтра всё равно бесполезен.
                continue
            }
            subclassNames += name
            subclassParents += parentEng
        }

        @Suppress("UNCHECKED_CAST")
        val rawRaces = raw["races"] as? List<Map<String, Any?>> ?: emptyList()
        val racesNames = rawRaces.mapNotNull { race -> race["name"] as? String }

        // Дропаем спелл, если после фильтрации ничего не осталось — он был
        // исключительно для ignored-классов.
        if (classesEng.isEmpty() && subclassNames.isEmpty()) return null

        val ritual = raw["ritual"] as? Boolean ?: false
        val concentration = raw["concentration"] as? Boolean ?: false

        val time = raw["time"] as? String ?: ""
        val range = raw["range"] as? String ?: ""
        val duration = raw["duration"] as? String ?: ""

        @Suppress("UNCHECKED_CAST")
        val source = raw["source"] as? Map<String, Any?>
        val sourceName = source?.get("shortName") as? String ?: ""
        @Suppress("UNCHECKED_CAST")
        val sourceGroup = source?.get("group") as? Map<String, Any?>
        val sourceGroupName = sourceGroup?.get("shortName") as? String ?: ""

        val school = raw["school"] as? String ?: ""
        val schoolKey = schoolToKey[school.lowercase()] ?: school.uppercase()

        val description = raw["description"] as? String ?: ""
        val upper = raw["upper"] as? String ?: ""

        val savingThrows = stRegex.findAll(description)
            .map { it.groupValues[1].trim() }
            .toList()

        return Pair(
            linkedMapOf(
                "id" to id,
                "name" to nameRu,
                "nameEng" to (nameEng ?: nameRu),
                "source" to sourceName,
                "sourceGroup" to sourceGroupName,
                "level" to level,
                "school" to schoolKey,
                "ritual" to ritual,
                "concentration" to concentration,
                "timecast" to time,
                "distance" to range,
                "duration" to duration,
                "componentV" to compV,
                "componentS" to compS,
                "componentM" to hasMaterial,
                "materialConsumed" to materialConsumed,
                "materialDesc" to (compM ?: ""),
                "descriptionHtml" to description,
                "upperLevel" to upper,
                "url" to (raw["url"] as? String ?: ""),
                "classes" to classesEng.joinToString(","),
                "subclasses" to subclassNames.joinToString(","),
                "subclassParents" to subclassParents.joinToString(","),
                "races" to racesNames.joinToString(","),
                "savingThrows" to savingThrows.joinToString(","),
            ),
            strippedSomething,
        )
    }
}

val generateSpellsDb by tasks.registering(GenerateSpellsDbTask::class) {
    group = "build"
    description = "Препроцессит spells_data/*.json в один spells_normalized.json для APK (около 1000 исходников → 950 спеллов)"

    sourceDir.set(rootProject.file("spells_data"))
    outputDir.set(layout.buildDirectory.dir("generated/assets"))
}

// Сгенерированный JSON кладём в `build/generated/assets/` (вне git)
// и подцепляем к Android assets source set через Variant API
// (старый SourceSet API запрещает Provider-инстансы в AGP 9.x).
// Variant API сам разруливает Gradle-зависимость generateSpellsDb →
// mergeAssets, вручную dependsOn писать не нужно.
androidComponents {
    onVariants(selector().all()) { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            tasks.named("generateSpellsDb"),
            { (it as GenerateSpellsDbTask).outputDir },
        )
    }
}
