plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(libs.compose.ui)
            }
        }
    }
}

// Sync, ne Copy: forigas arkaikajn (malnov-haŝajn) .wasm-dosierojn el la elŝutejo —
// alie ĉiu konstruo postlasas 8–17 MB da rubo (2026-11-24, C56).
val kopiuAlPublikaElsutejo by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Kopias la produktaĵan wasmJs-faskon al server/data/publika/apo/ (C56)"
    dependsOn("wasmJsBrowserDistribution")
    from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    into(rootProject.file("server/data/publika/apo"))
}
