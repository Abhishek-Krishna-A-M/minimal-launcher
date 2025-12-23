pluginManagement {
    repositories {
        google()            // Crucial for Android plugins
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()            // Crucial for library dependencies
        mavenCentral()
    }
}

rootProject.name = "MinimalLauncher"
include(":app")
