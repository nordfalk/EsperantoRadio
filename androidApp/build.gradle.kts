plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    // Gradle Play Publisher — aŭtomata eldonado al Google Play
    // Dokumentaro: https://github.com/Triple-T/gradle-play-publisher
    id("com.github.triplet.play") version "3.12.2"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.material3)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.session)
            implementation(libs.androidx.work.runtime)
        }
    }
}

android {
    namespace = "dk.nordfalk.esperanto.android"
    compileSdk = 36

    // Ŝargu sekretajn agordojn el secrets.properties en la radiko (NENIAM commit!)
    val keystoreProperties = java.util.Properties().apply {
        file("../secrets.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("KEYSTORE_PATH") ?: System.getenv("KEYSTORE_PATH") ?: "/home/j/android/A_signaturer/jacobnordfalk.keystore")
            storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = keystoreProperties.getProperty("KEYSTORE_ALIAS") ?: System.getenv("KEYSTORE_ALIAS") ?: "jacobnordfalk"
            keyPassword = keystoreProperties.getProperty("KEYSTORE_KEY_PASSWORD") 
                ?: keystoreProperties.getProperty("KEYSTORE_PASSWORD") 
                ?: System.getenv("KEYSTORE_KEY_PASSWORD") 
                ?: System.getenv("KEYSTORE_PASSWORD") 
                ?: ""
        }
    }

    defaultConfig {
        applicationId = "dk.nordfalk.esperanto.radio"
        minSdk = 26
        targetSdk = 36
        versionCode = 246
        versionName = libs.versions.apoversio.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".alfa"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Gradle Play Publisher — agordo por Google Play-eldonado
// Vidu docs/nova/07_eldonado.md por plena gvidilo
play {
    // Servila konto-JSON — metebla en secrets.properties kiel PLAY_SERVICE_ACCOUNT_JSON_PATH
    // aŭ agordu per env-variablej: PLAY_SERVICE_ACCOUNT_JSON_PATH
    serviceAccountCredentials.set(
        file(keystoreProperties.getProperty("PLAY_SERVICE_ACCOUNT_JSON_PATH") 
            ?: System.getenv("PLAY_SERVICE_ACCOUNT_JSON_PATH") 
            ?: "play-service-account.json")
    )
    // Kiu track eldoni: "internal", "alpha", "beta", "production"
    track.set("internal")
    // Aŭtomate pliigi versionCode estas malaktiva — ni mastrumas versionCode permane
    // Nur eldoni AAB (App Bundle), ne APK
    defaultToAppBundles.set(true)
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1") // GrantPermissionRule
}
