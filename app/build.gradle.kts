plugins {
    id("com.android.application")
    // org.jetbrains.kotlin.android не указываем явно: AGP 9.x авто-применяет
    // его, если плагин есть в classpath (объявлен в pluginManagement).
    id("org.jetbrains.kotlin.plugin.compose")
    // KSP: обрабатывает Kotlin-аннотации (Room @Database/@Dao/@Entity и т.п.)
    // и генерирует SpellDatabase_Impl во время компиляции.
    id("com.google.devtools.ksp")
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
        versionCode = 3
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            // Тестовая подпись отладочным ключом, чтобы APK можно было
            // ставить на реальное устройство без своего keystore.
            signingConfig = signingConfigs.getByName("debug")
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
