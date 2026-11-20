# Konsumaj reguloj de EsperantoRadio (FAROTA C6 — ŝaltite kun isMinifyEnabled).
# Media3 (session/exoplayer), Sentry, WorkManager, Compose kaj Coil liveras siajn
# proprajn konsumajn regulojn en siaj AAR-oj — ĉi tie restas nur nia propra kodo.

# --- kotlinx.serialization -------------------------------------------------
# niaj @Serializable-modeloj (Elsendo, Kanalo, Sonfonto, LudataElsendo, Vojo,
# la JSONC-DTO-oj k.a.) kaj iliaj generitaj serializiloj devas resti neŝanĝitaj:
# la serializiloj estas atingataj per refleksio (KSerializer serializer(...)),
# kaj la persistitaj datumoj (ludatoj, agordoj, naviga stato) devas resti
# malkodigeblaj trans versioj.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,Signature,Exceptions
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class dk.nordfalk.esperanto.**$$serializer { *; }
-keepclassmembers class dk.nordfalk.esperanto.** {
    *** Companion;
}
-keepclasseswithmembers class dk.nordfalk.esperanto.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
