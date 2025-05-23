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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "IMG.LY Artifactory"
            url = uri("https://artifactory.img.ly/artifactory/maven")
            mavenContent {
                includeGroup("ly.img")
            }
        }
    }
}

rootProject.name = "PostcardCollage"
include(":app")
 