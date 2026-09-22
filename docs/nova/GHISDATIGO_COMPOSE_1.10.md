# Ghisdatigo al Compose Multiplatform 1.10 + Kotlin 2.2.20

> **Efektivigita: 2026-09-06** (PR #32/#35). La plan-dokumento estas forigita;
> nur la rezulto kaj lernitaj lecionoj restas por referenco.

## Kio ŝanĝiĝis

- **Kotlin**: 2.1.0 → 2.2.20 (ne 2.1.20 kiel planite — web/native-celoj postulas 2.2.20)
- **Compose MP**: 1.7.3 → 1.10.0
- **kotlinOptions → compilerOptions**: Kotlin 2.2.20 forigis la malnovan DSL (nun eraro)
- **Kaŝnomoj → rektaj Maven-referencoj**: ĉiuj `compose.xxx` kaŝnomoj anstataŭigitaj
  per versikatalogaj eniroj, krom `compose.desktop.currentOs`
- **Material3**: aparta versio `1.10.0-alpha05` (ne la sama kiel `compose-multiplatform`)
- **@Preview**: movita de `androidMain` al `commonMain`
- **ui-tooling-preview**: movita de `androidMain` al `commonMain`
- **@OptIn(ExperimentalComposeLibrary::class)**: forigita (ne plu necesas)

## Lernitaj lecionoj

1. **Kotlin 2.2.20, ne 2.1.20**: La oficiala dokumentaro diras "Kotlin 2.2 is required
   for native and web platforms". Ĉar ni havas wasmJs-celon, 2.2.20 estas necesa.
   La Hot Reload-minimumo (2.1.20) ne sufiĉas.

2. **ksoup 0.2.2 estas kongrua kun Kotlin 2.2.20**: Kontraŭe al timo, ĝi funkcias senprobleme.
   Versio 0.2.6+ postulas Kotlin 2.3+ — restu ĉe 0.2.2.

3. **`compose.desktop.currentOs` ne povas esti anstataŭigita**: La deprecation-mesaĝo
   diras "can be safely removed", sed la `desktop` artifiko sole ne enhavas la
   platform-specifan Skia-runtimon (`skiko-awt-runtime-linux-x64`). Ni konservis
   `currentOs` kun `@Suppress("DEPRECATION")`. Kiam JetBrains disponigas veran
   anstataŭaĵon, ni migrigos.

4. **Material3 havas apartan version**: `org.jetbrains.compose.material3:material3` estas
   `1.10.0-alpha05`, ne `1.10.0`. Aldonita `compose-material3` al la versikatalogo.

5. **AGP 8.7.3 sufiĉas**: Ne necesis ĝisdatigi al AGP 9.0.

6. **Coil 3.0.4 funkcias**: Neniu ĝisdatigo necesa.
