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
// BETA-celo: apo-beta — ŝanĝoj al la reta apo inter eldonoj iras tien
// (REGULO de la posedanto, 2026-11-25); la kutima rsync de publika/ publikigas
// la betaon aŭtomate, ĉar apo-beta/ estas parto de publika/.
val kopiuAlApoBetaElsutejo by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Kopias la produktaĵan wasmJs-faskon al server/data/publika/apo-beta/ (beta — defaŭlta)"
    dependsOn("wasmJsBrowserDistribution")
    from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    into(rootProject.file("server/data/publika/apo-beta"))
    doLast {
        // Enmetu la version en index.html (Sentry-release) — la deponeja fonto
        // de vero: gradle/libs.versions.toml (same kiel ApoVersio en :shared)
        val versio = libs.versions.apoversio.get()
        val index = destinationDirectory.get().asFile.resolve("index.html")
        index.writeText(index.readText().replace("__APOVERSIIO__", versio))
    }
}

// PRODUKTA celo: apo/ — NUR kiam la posedanto eksplicite petas
// («publikigu la novan version de la reta apo al apo/», REGULO 2026-11-25).
val kopiuAlApoElsutejo by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Kopias la produktaĵan wasmJs-faskon al server/data/publika/apo/ (produktada — nur laŭ eksplicita peto!)"
    dependsOn("wasmJsBrowserDistribution")
    from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    into(rootProject.file("server/data/publika/apo"))
    doLast {
        val versio = libs.versions.apoversio.get()
        val index = destinationDirectory.get().asFile.resolve("index.html")
        index.writeText(index.readText().replace("__APOVERSIIO__", versio))
    }
}
