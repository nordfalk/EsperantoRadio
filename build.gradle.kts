// Radika konstrudusiero — transira fazo: malnova Groovy-apo + nova KMP-apo (estonte)
//
// La malnovaj moduloj (malnova/app, malnova/parse, malnova/data) uzas Groovy-build.gradle
// kaj la malnovan AGP-classpath. La novaj moduloj uzos la plugins()-blokon kaj la versikatalogon.

plugins {
    // Malnova apo
    id("com.android.application") version "9.4.1" apply false

    // Nova KMP-apo
    id("org.jetbrains.kotlin.multiplatform") version "2.4.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.compose") version "1.12.2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
    id("com.android.library") version "9.4.1" apply false
    id("io.sentry.kotlin.multiplatform.gradle") version "0.28.0" apply false
    id("io.sentry.jvm.gradle") version "6.24.0" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        // jcenter — malaktuala, sed ankoraŭ bezonata de iuj malnovaj dependencoj
        // (la jcenter()-metodo foriĝis en Gradle 9; uzu la rektan URL-on)
        maven { url = uri("https://jcenter.bintray.com") }
    }
}
