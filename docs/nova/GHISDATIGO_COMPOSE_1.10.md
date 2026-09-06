# Plano: Ĝisdatigo al Compose Multiplatform 1.10 + Kotlin 2.1.20

> **Ne kuru ĉi tion aŭtomate.** Tiu ĉi dokumento priskribas la paŝojn kaj riskojn.
> La ĝisdatigo devas esti farata zorgeme, paŝo post paŝo, kun kontrolo je ĉiu paŝo.

## Kial ĝisdatigi

| Avantaĝo | Detalo |
|---|---|
| Unuigita `@Preview` en `commonMain` | Ne plu bezonas `expect`/`actual` aŭ apartan `androidMain`-dosieron |
| Compose Hot Reload (stabila) | Ŝanĝoj en la kodo aperi tuj sen rekompili |
| Navigation 3 | Nova naviga biblioteko por ne-Android-celoj |
| Plibonigita IDE-subteno | Pli bona aŭtomata kompletigo, pli rapidaj antaŭrigardoj |

## Versioj

| Komponanto | Nuna versio | Celo | Risko |
|---|---|---|---|
| Kotlin | 2.1.0 | 2.1.20 (minimume) | Malalta — patch-versio |
| Compose Multiplatform | 1.7.3 | 1.10.x | Meza — multaj ŝanĝoj |
| AGP | 8.7.3 | 8.7.3 (aŭ 9.0 se necesa) | Alta se 9.0 postulata |
| Ktor | 3.1.3 | 3.1.3 (neniu ŝanĝo) | Malalta |
| ksoup | 0.2.2 | ??? (kontroli kongruecon) | **Alta** — 0.2.6+ postulas Kotlin 2.3+ |
| Coil | 3.0.4 | 3.0.4 (aŭ pli nova) | Malalta |
| Media3 | 1.5.1 | 1.5.1 (neniu ŝanĝo) | Neniu |
| mp3spi | 1.9.5.4 | 1.9.5.4 (neniu ŝanĝo) | Neniu |
| multiplatform-settings | 1.2.0 | 1.2.0 (neniu ŝanĝo) | Neniu |

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
