package dk.nordfalk.esperanto.data.config

/**
 * La resurco ne legeblas sinkrone sur ĉi tiu platformo, do la kanalkonfiguro estas enigita
 * en la kodon dum la konstruo (Gradle-tasko `generuEnigitanKanalkonfiguron` en shared/build.gradle.kts).
 */
actual fun leguPakititanKanalkonfiguron(): String = ENIGITA_KANALKONFIGURO
