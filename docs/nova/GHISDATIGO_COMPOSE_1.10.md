# Plano: Ĝisdatigo al Compose Multiplatform 1.10 + Kotlin 2.2.20

> **Efektivigita: 2026-09-06.** Ĉi tiu dokumento priskribis la planon.
> La efektiva ĝisdatigo estas farita en branĉo `ghisdatigo-compose-1.10`.
> Vidu la fino de la dosiero por la rezulto kaj lernitaj lecionoj.

## Kial ĝisdatigi

| Avantaĝo | Detalo |
|---|---|
| Unuigita `@Preview` en `commonMain` | Ne plu bezonas `expect`/`actual` aŭ apartan `androidMain`-dosieron |
| Compose Hot Reload (stabila) | Ŝanĝoj en la kodo aperi tuj sen rekompili |
| Navigation 3 | Nova naviga biblioteko por ne-Android-celoj |
| Plibonigita IDE-subteno | Pli bona aŭtomata kompletigo, pli rapidaj antaŭrigardoj |

## Versioj

| Komponanto | Nuna versio | Celo | Aktuala versio | Risko |
|---|---|---|---|---|
| Kotlin | 2.1.0 | 2.1.20 (minimume) | 2.2.20 | **Pli alta ol planita** — web/native postulas 2.2.20 |
| Compose Multiplatform | 1.7.3 | 1.10.x | 1.10.0 | Meza — multaj ŝanĝoj |
| AGP | 8.7.3 | 8.7.3 (aŭ 9.0 se necesa) | 8.7.3 (ne necesis 9.0) | Malalta |
| Ktor | 3.1.3 | 3.1.3 (neniu ŝanĝo) | 3.1.3 | Neniu |
| ksoup | 0.2.2 | ??? (kontroli kongruecon) | 0.2.2 (kongrua!) | Malalta — funkcias kun 2.2.20 |
| Coil | 3.0.4 | 3.0.4 (aŭ pli nova) | 3.0.4 (ne necesis ĝisdatigi) | Neniu |
| Material3 | (parto de Compose) | — | 1.10.0-alpha05 (aparta versio!) | Meza |
| Media3 | 1.5.1 | 1.5.1 (neniu ŝanĝo) | 1.5.1 | Neniu |
| mp3spi | 1.9.5.4 | 1.9.5.4 (neniu ŝanĝo) | 1.9.5.4 | Neniu |
| multiplatform-settings | 1.2.0 | 1.2.0 (neniu ŝanĝo) | 1.2.0 | Neniu |

## La plej grandaj riskoj

### 1. ksoup (PLEJ GRAVA)

- **Nuna versio**: 0.2.2 (kongrua kun Kotlin 2.1.0)
- **Problemo**: Versio 0.2.6+ postulas Kotlin 2.3.0 — tro nova
- **Kontroli**: Ĉu 0.2.2 kompilas kun Kotlin 2.1.20? Ĝi estas patch-versio, do verŝajne jes.
- **Se ne**: Serĉi interversion (0.2.3, 0.2.4, 0.2.5) aŭ fork-i kaj fiksi mem
- **Agordo**: `gradle/libs.versions.toml` → `ksoup = "0.2.2"`

### 2. AGP (Android Gradle Plugin)

- **Nuna versio**: 8.7.3
- **Compose MP 1.10 subtenas AGP 9.0** — sed eble ankaŭ 8.x
- **Kontroli**: La Compose MP 1.10-kompatibleco-tabelo
- **Se AGP 9.0 necesas**: Granda ŝanĝo — povas rompi Android-konstruon
- **Agordo**: `gradle/libs.versions.toml` → `agp = "8.7.3"`

### 3. Dependec-kaŝnomoj malrekomenditaj

- **Nuna**: `compose.runtime`, `compose.foundation`, `compose.material3` ktp. (kaŝnomoj)
- **Compose MP 1.10 malrekomendas ilin** — devas anstataŭigi per rektaj bibliotekaj referencoj
- **Ekzemplo**:
  ```kotlin
  // Antaŭe
  implementation(compose.runtime)
  // Poste
  implementation("org.jetbrains.compose.runtime:runtime:1.10.0")
  ```
- **Riskto**: Malalta — mekanika ŝanĝo, sed multaj dosieroj

## Paŝoj (laŭvice, kun kontrolo je ĉiu paŝo)

### Paŝo 1: Kotlin 2.1.20 (sen Compose MP-ŝanĝo)

1. `gradle/libs.versions.toml`: `kotlin = "2.1.20"`
2. `./gradlew :shared:compileKotlinDesktop` — kontrolu ĉu kompilas
3. `./gradlew :shared:compileDebugKotlinAndroid` — kontrolu Android
4. `./gradlew :shared:desktopTest` — kontrolu testojn
5. Se ksoup rompiĝas → provo versiojn 0.2.3/0.2.4/0.2.5

### Paŝo 2: Compose Multiplatform 1.10

1. `gradle/libs.versions.toml`: `compose-multiplatform = "1.10.0"`
2. `./gradlew :shared:compileKotlinDesktop` — kontrolu erarojn
3. Anstataŭigu kaŝnomojn per rektaj referencoj se necesa
4. `./gradlew :shared:compileKotlinWasmJs` — kontrolu wasmJs
5. `./gradlew :shared:compileDebugKotlinAndroid` — kontrolu Android
6. `./gradlew :shared:desktopTest` — kontrolu testojn

### Paŝo 3: AGP (se necesa)

1. Se Compose MP 1.10 postulas AGP 9.0:
   - `gradle/libs.versions.toml`: `agp = "9.0.0"`
   - `./gradlew :androidApp:assembleDebug` — kontrolu ĉu APK konstruas
2. Se AGP 8.7.3 funkcias kun Compose MP 1.10 → neniu ŝanĝo

### Paŝo 4: Movo @Preview al commonMain

1. Forigu `shared/src/androidMain/kotlin/.../ui/Previews.kt`
2. Kreu `shared/src/commonMain/kotlin/.../ui/Previews.kt` kun `import androidx.compose.ui.tooling.preview.Preview`
3. Forigu `ui-tooling-preview` dependencon de `androidMain`
4. Aldonu `ui-tooling-preview` al `commonMain` se necesa
5. `./gradlew :shared:compileKotlinDesktop` — kontrolu

### Paŝo 5: Forigo de la malnova Preview-mekanismo

1. Forigu iujn ajn `expect`/`actual` `Preview`-difinojn se ili ekzistas
2. Forigu `desktopTest`-bazitajn `EkranfotoTesto`-n se `@Preview` sufiĉas (aŭ tenu ilin por CI)

### Paŝo 6: Coil-ĝisdatigo (se necesa)

1. Kontrolu ĉu Coil 3.0.4 funkcias kun Compose MP 1.10
2. Se ne: `gradle/libs.versions.toml`: `coil = "3.1.0"` (aŭ pli nova)
3. `./gradlew :shared:compileKotlinDesktop` — kontrolu

## Kion NE fari

- **Ne faru ĉiujn paŝojn samtempe** — faru unu paŝon, kontrolu, tiam la sekvan
- **Ne tuŝu la malnovan kodon** (`malnova/`) — ĝi ne dependas de Compose MP
- **Ne forgesu kontroli wasmJs** — ĝi ofte havas nekongruojn
- **Ne forgesu la ekranfotajn testojn** — ili estas la sola maniero kontroli la UI sen Android-aparato

## Kion kontroli post ĉiuj paŝoj

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Kompilo
./gradlew :shared:compileKotlinDesktop
./gradlew :shared:compileKotlinWasmJs
./gradlew :shared:compileDebugKotlinAndroid

# Testoj
./gradlew :shared:desktopTest

# APK
./gradlew :androidApp:assembleDebug

# Ekranfotoj (kontroli ke la UI aspektas gxuste)
./gradlew :shared:desktopTest --tests "*EkranfotoTesto*"
```

## Rezulto (2026-09-06)

Ĉiuj 6 paŝoj efektivigitaj en branĉo `ghisdatigo-compose-1.10`.

### Kio ŝanĝiĝis

- **Kotlin**: 2.1.0 -> 2.2.20 (ne 2.1.20 kiel planite — web/native-celoj postulas 2.2.20)
- **Compose MP**: 1.7.3 -> 1.10.0
- **kotlinOptions -> compilerOptions**: Kotlin 2.2.20 forigis la malnovan DSL (nun estas eraro)
- **Kaŝnomoj -> rektaj Maven-referencoj**: ĉiuj `compose.xxx` kaŝnomoj anstataŭigitaj
  per versikatalogaj eniroj, krom `compose.desktop.currentOs`
- **Material3**: aparta versio `1.10.0-alpha05` (ne la sama kiel `compose-multiplatform`)
- **@Preview**: movita de `androidMain` al `commonMain`
- **ui-tooling-preview**: movita de `androidMain` al `commonMain`
- **@OptIn(ExperimentalComposeLibrary::class)**: forigita (ne plu necesas)

### Lernitaj lecionoj

1. **Kotlin 2.2.20, ne 2.1.20**: La originala plano celis Kotlin 2.1.20, sed la oficiala
   dokumentaro diras "Kotlin 2.2 is required for native and web platforms". Ĉar ni havas
   wasmJs-celon, 2.2.20 estas necesa. La Hot Reload-minimumo (2.1.20) ne sufiĉas.

2. **ksoup 0.2.2 estas kongrua kun Kotlin 2.2.20**: Kontraŭe al timo, ĝi funkcias senprobleme.

3. **`compose.desktop.currentOs` ne povas esti anstataŭigita**: La deprecation-mesaĝo diras
   "can be safely removed", sed la `desktop` artifiko sole ne enhavas la platform-specifan
   Skia-runtimon (`skiko-awt-runtime-linux-x64`). Ni konservis `currentOs` kun
   `@Suppress("DEPRECATION")`. Kiam JetBrains disponigas veran anstataŭaĵon, ni migrigos.

4. **Material3 havas apartan version**: `org.jetbrains.compose.material3:material3` estas
   `1.10.0-alpha05`, ne `1.10.0`. Aldonita `compose-material3` al la versikatalogo.

5. **AGP 8.7.3 sufiĉas**: Ne necesis ĝisdatigi al AGP 9.0.

6. **Coil 3.0.4 funkcias**: Neniu ĝisdatigo necesa.

### Kontrolrezultoj

- compileKotlinDesktop: PASAS
- compileDebugKotlinAndroid: PASAS
- compileKotlinWasmJs: PASAS
- desktopTest: 76 testoj, 0 fiaskoj, 0 eraroj
- androidApp:assembleDebug: PASAS (APK konstruiĝas)
