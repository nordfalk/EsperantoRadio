plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("server")

    sourceSets {
        val serverMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val serverTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// Rulas la arkiv-konstruilon (riĉigitaj RSS-dosieroj sur disko).
// Ekz.: ./gradlew :server:arkivKonstruilo --args="radiovatikana"
//       ./gradlew :server:arkivKonstruilo            (ĉiuj kanaloj)
val arkivKonstruilo by tasks.registering(JavaExec::class) {
    group = "application"
    description = "Konstruas riĉigitajn RSS-dosierojn en server/data/feeds/"
    classpath = kotlin.targets.getByName("server").compilations.getByName("main").output.allOutputs
    classpath += configurations.getByName("serverRuntimeClasspath")
    mainClass.set("dk.nordfalk.esperanto.server.ArkivKonstruilo")
    workingDir = rootProject.projectDir
}
