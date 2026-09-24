plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sentry.kmp.gradle)
}

// Generas ApoVersio.kt kun la versia konstanto el libs.versions.toml.
// La fonto de vero por versionName (Android), packageVersion (Desktop) kaj Sentry release.
val generuApoVersio by tasks.registering {
    val versio = libs.versions.apoversio.get()
    outputs.dir(layout.buildDirectory.dir("generated/sources/apoVersio/commonMain/kotlin"))
    outputs.upToDateWhen { false }
    doLast {
        val dosiero = layout.buildDirectory.file("generated/sources/apoVersio/commonMain/kotlin/dk/nordfalk/esperanto/ApoVersio.kt").get().asFile
        dosiero.parentFile.mkdirs()
        dosiero.writeText("""
            |package dk.nordfalk.esperanto
            |
            |object ApoVersio {
            |    // NE ŜANĜŬ ĉi tie, anstataŭ redaktu en gradle/libs.versions.toml
            |    const val VERSION = "$versio"
            |}
        """.trimMargin())
    }
}

// Enigas la kanalkonfiguron (JSONC) kiel Kotlin-ĉenon por platformoj kie la resurco ne legeblas
// sinkrone (wasmJs: nur nesinkrona fetch; iOS: ankoraŭ neniu resurco-mekanismo).
// La fonto restas la sama dosiero — neniu duobligita konfiguro.
val generuEnigitanKanalkonfiguron by tasks.registering {
    val fonto = layout.projectDirectory.file("src/commonMain/resources/esperantoradio_kanaloj_v9.json")
    val celDosierujo = layout.buildDirectory.dir("generated/sources/kanalkonfiguro/kotlin")
    inputs.file(fonto)
    outputs.dir(celDosierujo)
    doLast {
        val teksto = fonto.asFile.readText()
        val eskapita = buildString {
            for (c in teksto) when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '$' -> append("\\$")
                '\n' -> append("\\n")
                '\r' -> {}
                else -> append(c)
            }
        }
        val dosiero = celDosierujo.get().file("dk/nordfalk/esperanto/data/config/EnigitaKanalkonfiguro.kt").asFile
        dosiero.parentFile.mkdirs()
        dosiero.writeText(
            "package dk.nordfalk.esperanto.data.config\n\n" +
            "// GENERITA de la Gradle-tasko generuEnigitanKanalkonfiguron el esperantoradio_kanaloj_v9.json — NE REDAKTU\n" +
            "internal val ENIGITA_KANALKONFIGURO: String = \"" + eskapita + "\"\n"
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ksoup)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.coil)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.multiplatform.settings)
            implementation(libs.navigation3.ui)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.ui.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.session)
        }

        val desktopMain by getting {
            dependencies {
                @Suppress("DEPRECATION")
                implementation(compose.desktop.currentOs)
                implementation(libs.multiplatform.settings.jvm)
                implementation(libs.mp3spi)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(libs.compose.ui.test)
            }
        }
    }
}

kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generuApoVersio)
// iosMain ekzistas nur kiam la iOS-celoj estas agorditaj — do `matching` anstataŭ `getByName`
kotlin.sourceSets.matching { it.name == "wasmJsMain" || it.name == "iosMain" }.configureEach {
    kotlin.srcDir(generuEnigitanKanalkonfiguron)
}

android {
    namespace = "dk.nordfalk.esperanto.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
}

// Temporary workaround: navigation3-runtime:1.1.1 requires savedstate:1.4.0
// but savedstate-ktx:1.4.0 doesn't exist (only 1.3.1). Force to 1.3.1.
configurations.all {
    resolutionStrategy.force("androidx.savedstate:savedstate-ktx:1.3.1")
}
