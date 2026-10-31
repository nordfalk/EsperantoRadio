import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    // Gradle Play Publisher — aŭtomata eldonado al Google Play
    // Dokumentaro: https://github.com/Triple-T/gradle-play-publisher
    id("com.github.triplet.play") version "4.1.1"
}

// Ŝargu sekretajn agordojn el secrets.properties en la radiko (NENIAM commitu aŭ legu)
val keystoreProperties = Properties().apply {
    file("../secrets.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
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
    compileSdk = 37

    // La agorda JSONC por androidMain/assets venas el shared/commonMain/resources
    sourceSets {
        getByName("main") {
            assets.srcDir(rootProject.layout.projectDirectory.dir("shared/src/commonMain/resources"))
        }
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

    // Ĉu ĈI TIU konstruo okazas SEN nia subskriba keystore — tiel F-Droid kaj
    // senkolora CI konstruas (la release-konstruo subskribiĝas nur kiam la
    // keystore ekzistas, vidu malsupre). Uzata por la defaŭlto de la
    // erar-raportado: sen nia subskribo → opt-in (konsento-demando, F-Droid);
    // kun nia subskribo (Google Play, Aptoide) → defaŭlte ŝaltita (opt-out).
    // C50/PR #148. En debug-konstruoj la valoro estas senefika (ne raportas).
    val senNiaSubskribo = !(
        signingConfigs.getByName("release").storeFile?.exists() == true
        )

    defaultConfig {
        applicationId = "dk.nordfalk.esperanto.radio"
        minSdk = 26
        targetSdk = 36
        // 253 estis konsumita de la interna testo "3.1.1-beta29okt" (PR #144, nekunfandita);
        // la vera 3.1.1 eldoniĝas kiel 254. La etikedo v3.1.1 (253) restas por F-Droid —
        // ilia versionumejo estas sendependa, la fdroid-yml (253) kongruas kun ĝi.
        versionCode = 254
        // LAŬVORTE (ne el libs.versions.toml): la F-Droid-ĝisdatigilo parsas
        // versionName per regex, kiu bezonas cititan signoĉenon —
        // `libs.versions.apoversio.get()` estas por ĝi nelegebla.
        // Ĝisdatigu KUN la apoversio en gradle/libs.versions.toml (eldonado-skill-o).
        versionName = "3.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "SEN_NIA_SUBSKRIBO", senNiaSubskribo.toString())
    }

    buildFeatures {
        // BuildConfig.SEN_NIA_SUBSKRIBO (supre) bezonas BuildConfig-klason
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Subskribu nur kiam la keystore-dosiero reale ekzistas. Tio ebligas
            // nesubskribitan release-konstruon en medioj SEN la sekretoj (F-Droid,
            // senkolora CI) — ili subskribas la APK-on mem per sia propra ŝlosilo.
            // Loke la keystore ekzistas, do la kutima subskribita konstruo ne ŝanĝiĝas.
            if (!senNiaSubskribo) {
                signingConfig = signingConfigs.getByName("release")
            }
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
