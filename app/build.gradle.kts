plugins {
    alias(libs.plugins.android.application)
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
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Тестовая подпись отладочным ключом, чтобы APK можно было
            // ставить на реальное устройство без своего keystore.
            // Для публикации в Google Play нужно заменить на свой signing config.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    // Имя итогового APK: spell-tracker-v{versionName}.apk
    // (например, spell-tracker-v1.0.apk). В AGP 9.x applicationVariants
    // удалён, поэтому переименовываем через doLast-хук на assemble*-задачах.
    // При повторных сборках старый файл того же имени удаляется, чтобы
    // не накапливать дубликаты.
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.material)
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}