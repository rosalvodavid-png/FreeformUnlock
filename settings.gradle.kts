pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Classic Xposed API (de.robv.android.xposed:api:82)
        maven { url = uri("https://api.xposed.info/") }
    }
}

rootProject.name = "FreeformUnlock"
include(":app")
