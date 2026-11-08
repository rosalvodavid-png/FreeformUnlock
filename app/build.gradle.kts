plugins {
    id("com.android.application")
}

android {
    namespace = "com.freeform.unlock"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.freeform.unlock"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            // Sem assinatura de release: use o APK debug para instalar.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources.excludes += setOf("META-INF/*.kotlin_module", "kotlin/**")
    }
}

dependencies {
    // API clássica do Xposed: só compila, não vai dentro do APK.
    compileOnly("de.robv.android.xposed:api:82")
}
