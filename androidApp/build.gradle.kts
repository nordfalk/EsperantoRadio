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

// Branĉnomo por BuildConfig.GIT_BRANCHO (montriĝas en Agordoj) — malplena
// ĉe master kaj ĉe detached HEAD (F-Droid konstruas el etikedo; se .git
// entute mankas, också malplena)
val gitBrancho = try {
    providers.exec {
        commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
    }.standardOutput.asText.get().trim()
} catch (e: Exception) {
    "HEAD"
}
// Sanigu citilojn kaj deklivojn, por ke ili ne rompu la generitan BuildConfig-ĉenon
val branĉoPorAgordoj = if (gitBrancho == "master" || gitBrancho == "HEAD") "" else gitBrancho.replace("\\", "").replace("\"", "")

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
    // keystore ekzistas, vidu malsupre). NUR por la SUBSKRIBO mem — NENIU alia
    // parto de la APK rajtas dependi de ĝi, ĉar la konstruo devas esti
    // bit-identa (reproduktebla konstruo, F-Droid RB, MR !55129, 2026-11-22).
    // La eks-BuildConfig.SEN_NIA_SUBSKRIBO (telemetria defaŭlto) translokiĝis al
    // RUTEMPA kontrolo de la subskriba atestilo (EsperantoRadioAplikaĵo.kt).
    // findByName (ne getByName!): fdroidserver FORIGAS la signingConfigs-blokon
    // el build.gradle(.kts) antaŭ ol konstrui (remove_signing_keys) — getByName
    // tiam ĵetus "SigningConfig with name 'release' not found"; findByName
    // redonas null kaj la konstruo sekure daŭras nesubskribita (2026-10-31).
    val senNiaSubskribo = !(
        signingConfigs.findByName("release")?.storeFile?.exists() == true
        )

    defaultConfig {
        applicationId = "dk.nordfalk.esperanto.radio"
        minSdk = 26
        targetSdk = 36
        // Uzitaj versionCode (kanona listo: .vibe/skills/eldonado/SKILL.md paŝo 2):
        // 246=3.0.2 … 252=3.1.0, 253=3.1.1-beta29okt (interna), 254=3.1.1,
        // 255=3.2.0-beta06nov (interna), 256/257=3.1.2-beta/alfa19nov (interna),
        // 258=3.2.0. versionCode NE reuzeblas (Play-API rifuzas).
        versionCode = 258
        // LAŬVORTE (ne el libs.versions.toml): la F-Droid-ĝisdatigilo parsas
        // versionName per regex, kiu bezonas cititan signoĉenon —
        // `libs.versions.apoversio.get()` estas por ĝi nelegebla.
        // Ĝisdatigu KUN la apoversio en gradle/libs.versions.toml (eldonado-skill-o).
        versionName = "3.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GIT_BRANCHO", "\"$branĉoPorAgordoj\"")
    }

    buildFeatures {
        // BuildConfig.GIT_BRANCHO (supre) bezonas BuildConfig-klason
        buildConfig = true
    }

    // Reproduktebla konstruo (F-Droid RB, MR !55129): la dependaĵoj-bloko en
    // la APK malkasas la subskriban atestilon kaj rompas bit-identecon —
    // vidu https://gitlab.com/fdroid/fdroiddata/-/issues/3330
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        release {
            // R8-kodminigo (FAROTA C6): la apo mem liveras la bezonatajn konsumajn
            // regulojn (proguard-rules.pro — precipe kotlinx.serialization);
            // Media3, Sentry, WorkManager kaj Compose liveras siajn proprajn.
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    // Nur eldoni APK, ne AAB (App Bundle): la apo NE estas enskribita en Play App
    // Signing, do AAB ne funkcius — la CI kaj la eldona gvidilo uzas publishReleaseApk
    // (vidu .github/workflows/eldonado.yml kaj docs/nova/07_eldonado.md; FAROTA C24)
    defaultToAppBundles.set(false)
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
    // C23: ne hokita versio — la CMP-artefakto kun la sama versio kiel la cetero
    // de Compose (la paketo ene restas androidx.compose.ui.test)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1") // GrantPermissionRule
}
