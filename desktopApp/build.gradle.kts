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

sentry {
    // Generas JVM-fontpakaĵon kaj alŝutas fontkodon al Sentry.
    // Ebligas fontkuntekston (source context) en stack traces.
    includeSourceContext = true

    org = "esperantoradio"
    projectName = "kmp"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}
