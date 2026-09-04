// Radika konstrudusiero — transira fazo: malnova Groovy-apo + nova KMP-apo (estonte)
//
// La malnovaj moduloj (malnova/app, malnova/parse, malnova/data) uzas Groovy-build.gradle
// kaj la malnovan AGP-classpath. La novaj moduloj uzos la plugins()-blokon kaj la versikatalogon.

plugins {
    // Malnova apo
    id("com.android.application") version "8.7.3" apply false

    // Nova KMP-apo (malkomentu kiam la moduloj estos kreitaj)
    // id("org.jetbrains.kotlin.multiplatform") version "2.1.0" apply false
    // id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    // id("org.jetbrains.compose") version "1.7.0" apply false
    // id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        // jcenter() — malaktuala, sed ankoraŭ bezonata de iuj malnovaj dependencoj
        @Suppress("DEPRECATION")
        jcenter()
    }
}
