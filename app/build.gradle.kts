import java.io.FileInputStream
import java.util.Properties

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
