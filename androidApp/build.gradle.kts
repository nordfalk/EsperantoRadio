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

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "/home/j/android/A_signaturer/jacobnordfalk.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEYSTORE_ALIAS") ?: "jacobnordfalk"
            keyPassword = System.getenv("KEYSTORE_KEY_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
        }
    }

    defaultConfig {
        applicationId = "dk.nordfalk.esperanto.radio"
        minSdk = 26
        targetSdk = 36
        versionCode = 245
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
    // Servila konto-JSON — metu ĉe androidApp/play-service-account.json (NE versiigu)
    // aŭ agordu per env-variablej: PLAY_SERVICE_ACCOUNT_JSON_PATH
    serviceAccountCredentials.set(
        file(System.getenv("PLAY_SERVICE_ACCOUNT_JSON_PATH") ?: "play-service-account.json")
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
