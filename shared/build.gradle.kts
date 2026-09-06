plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
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

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.ui.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
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
