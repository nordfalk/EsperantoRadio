plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sentry.jvm.gradle)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(libs.kotlinx.serialization.json)
                @Suppress("DEPRECATION")
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "dk.nordfalk.esperanto.desktop.MainKt"
        nativeDistributions {
            packageName = "EsperantoRadio"
            packageVersion = libs.versions.apoversio.get()
        }
    }
}

// Ilo por kompari kanalkonfiguron kun radio.txt
// Rulu per: ./gradlew :desktopApp:radioTxtKomparilo
val radioTxtKomparilo by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Komparas kanalkonfiguron kun esperanto-radio.com/radio.txt"
    classpath = kotlin.targets.getByName("desktop").compilations.getByName("main").output.allOutputs
    classpath += configurations.getByName("desktopRuntimeClasspath")
    mainClass.set("dk.nordfalk.esperanto.desktop.RadioTxtKompariloKt")
}

// Demonstra programo por la estonta CRI-peranto (transkoda servo).
// Elŝutas kaj transkodas la 20 plej novajn CRI-Esperanto-elsendojn al MP3 kaj
// generas RSS-fluon. Vidu docs/nova/07_cri_esperanto_kanalo.md.
// Rulu per: ./gradlew :desktopApp:criTranskodaDemo   (bezonas ffmpeg en $PATH)
val criTranskodaDemo by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Demonstras CRI-HLS→MP3-transkodadon kaj RSS-generadon (vidu docs/nova/07)"
    classpath = kotlin.targets.getByName("desktop").compilations.getByName("main").output.allOutputs
    classpath += configurations.getByName("desktopRuntimeClasspath")
    mainClass.set("dk.nordfalk.esperanto.desktop.CriTranskodaDemoKt")
}

sentry {
    // Generas JVM-fontpakaĵon kaj alŝutas fontkodon al Sentry.
    // Ebligas fontkuntekston (source context) en stack traces.
    includeSourceContext = true

    org = "esperantoradio"
    projectName = "kmp"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}
