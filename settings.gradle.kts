pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// NE FORIGU: sen eksplicita nomo Gradle prenas la DOSIERUJAN nomon kiel
// rootProject.name. Tiu nomo fluas en la kompilproduktajn nomojn de
// Kotlin-inline-funkcioj (…$<nomigita_vojo>_shared_release…), el tie en la
// R8-mapa enhavo kaj ĝian `pg_map_id`, kiun R8 enigas en la dekson kiel
// `r8-map-id-<haketaĵo>` — do la APK estas NEREPRODUKTEBLA inter dosierujoj
// (ni: EsperantoRadio, F-Droid: dk.nordfalk.esperanto.radio). Eksperimente
// pruvita 2026-11-24: kun fiksa nomo la konstruo en alia vojo estas
// bit-identa al la eldona APK (same classes.dex, same pg_map_id).
// Detaloj: fdroid/GVIDILO.md, ŜANĜOJ.md 2026-11-24.
rootProject.name = "EsperantoRadio"

// Malnova apo — moduloj movitaj al malnova/ por fari lokon por la nova Compose Multiplatform-apo
include(":app")
include(":parse")
include(":data")

project(":app").projectDir = file("malnova/app")
project(":parse").projectDir = file("malnova/parse")
project(":data").projectDir = file("malnova/data")

// Novaj moduloj
include(":shared")
include(":androidApp")
include(":desktopApp")
include(":webApp")
// include(":iosApp")  — bezonas macOS/Xcode por konstrui; malkomentu sur Mac
include(":server")
