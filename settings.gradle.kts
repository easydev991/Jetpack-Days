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
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // SDK RuStore In-App Review лежит в закрытом nexus RuStore; content-фильтр
        // ограничивает репозиторий группой ru.rustore.sdk (change add-rustore-in-app-review)
        maven {
            url = uri("https://nexus-external.rustore.ru/repository/maven-rustore-exposed")
            content {
                includeGroup("ru.rustore.sdk")
            }
        }
    }
}

rootProject.name = "Jetpack-Days"
include(":app")
include(":screenshot-tests")
