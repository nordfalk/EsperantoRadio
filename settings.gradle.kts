pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Malnova apo — moduloj movitaj al malnova/ por fari lokon por la nova Compose Multiplatform-apo
include(":app")
include(":parse")
include(":data")

project(":app").projectDir = file("malnova/app")
project(":parse").projectDir = file("malnova/parse")
project(":data").projectDir = file("malnova/data")

// Novaj moduloj (malkomentu kiam ili estos kreitaj):
// include(":shared")
// include(":androidApp")
// include(":iosApp")
// include(":desktopApp")
// include(":webApp")
// include(":server")
