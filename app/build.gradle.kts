// Konfigurasi build aplikasi
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Konfigurasi dibaca dari build.env di folder utama proyek
val env: Map<String, String> = rootProject.file("build.env").takeIf { it.exists() }
    ?.readLines()
    ?.filter { it.contains("=") && !it.trimStart().startsWith("#") }
    ?.associate { it.substringBefore("=").trim() to it.substringAfter("=").trim() }
    ?: emptyMap()

fun cfg(key: String, default: String): String = env[key] ?: default

android {
    namespace = "id.dstech.ujianmankeerom"
    compileSdk = 34

    defaultConfig {
        applicationId = cfg("APP_ID", "id.dstech.ujianmankeerom")
        minSdk = cfg("MIN_SDK", "26").toInt()
        targetSdk = cfg("TARGET_SDK", "34").toInt()
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["appName"] = cfg("APP_NAME", "Ujian MAN")
        buildConfigField("String", "APP_NAME", "\"" + cfg("APP_NAME", "Ujian MAN") + "\"")
        buildConfigField("String", "PASSWORD", "\"" + cfg("PASSWORD", "MAN123") + "\"")
        buildConfigField("String", "HOME_URL", "\"" + cfg("HOME_URL", "https://mankeerom.yzz.me/ujian") + "\"")
        buildConfigField("String", "LOGIN_TITLE", "\"" + cfg("LOGIN_TITLE", "Ujian Online MAN Keerom") + "\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
}
