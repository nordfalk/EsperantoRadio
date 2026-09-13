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

sentry {
    // Generas JVM-fontpakaĵon kaj alŝutas fontkodon al Sentry.
    // Ebligas fontkuntekston (source context) en stack traces.
    includeSourceContext = true

    org = "esperantoradio"
    projectName = "kmp"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}
