pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    // Версии плагинов — здесь, чтобы AGP подтянул их в classpath
    // и корректно скоординировал с Kotlin/Compose.
    plugins {
        id("com.android.application") version "9.1.1"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
        // KSP — обязателен для Room на Kotlin-коде.
        // annotationProcessor (Java API) не обрабатывает Kotlin-аннотации,
        // из-за чего SpellDatabase_Impl не генерируется и приложение
        // падает с "SpellDatabase_Impl does not exist".
        id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Spell tracker"
include(":app")
