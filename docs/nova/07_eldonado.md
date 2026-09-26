# Eldonado de EsperantoRadio

> Gvidilo por eldoni la Android-apon ĉe Google Play, F-Droid kaj Aptoide.

## Superrigardo

| Vendejo | Aŭtomateco | Postuloj | Stato |
|---|---|---|---|
| **Google Play** | Tute aŭtomata per Gradle Play Publisher (GPP) | Servila konto JSON, $25-konto | Agordita — bezonas servilan konton |
| **F-Droid** | Duon-aŭtomata (F-Droid konstruas mem el fonto) | GPL-licenco, GitLab-konto | Metadato pretigita — bezonas MR al fdroiddata |
| **Aptoide** | Duon-aŭtomata (API havebla) | Aptoide Connect-konto, API-ŝlosilo | Bezonas konton kaj API-ŝlosilon |

---

## 1. Google Play (per Gradle Play Publisher)

### Kio estas agordita

La kromprogramo `com.github.triplet.play` (GPP) estas aldonita al `androidApp/build.gradle.kts`.
Agordita track: `internal` (interna testado — ŝanĝu al `alpha`, `beta` aŭ `production` laŭbezone).

### Paŝoj por eldoni

1. **Kreu servilan konton en Google Play Console**:
   - Iru al https://play.google.com/console
   - Menuo → **Setup** → **API access** → **Service accounts** → **Create service account**
   - Elŝutu la JSON-ŝlosilon
   - Sekvu la ligojn al Google Cloud Console por krei la konton
   - Reen en Play Console, donu al la servila konto la rolon "Release Manager"

2. **Metu la JSON-ŝlosilon**:
   ```bash
   # Opcio A: metu la dosieron ĉe androidApp/play-service-account.json
   cp /loko/de/servila-konto.json androidApp/play-service-account.json

   # Opcio B: uzu env-variablejon
   export PLAY_SERVICE_ACCOUNT_JSON_PATH=/loko/de/servila-konto.json
   ```

3. **Konstruu kaj subskribu APK**:
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
   ./gradlew :androidApp:assembleRelease
   ```
   La APK aperos ĉe `androidApp/build/outputs/apk/release/`.

4. **Eldonu al Google Play**:
   ```bash
   KEYSTORE_PASSWORD=xxxxxx ./gradlew :androidApp:publishReleaseApk
   ```
   Tio alŝutas la APK-on al la track agordita en `build.gradle.kts` (defaŭlte `internal`).

> **Kial APK, ne AAB?** AAB-alŝuto postulas enskribiĝon en **Play App Signing**
> (eraro: "For uploading an AppBundle you must be enrolled in Play Signing").
> La apo ankoraŭ ne estas enskribita, do ni eldonas APK-on. Kiam la enskribiĝo
> estos farita (Setup → App signing → Enroll, eksportu la ekzistan ŝlosilon per
> la PEPK-ilo), ŝanĝu al `publishReleaseBundle` — ambaŭ taskoj ekzistas flankantde.

### Utilaj GPP-taskoj

| Tasko | Priskribo |
|---|---|
| `:androidApp:publishReleaseApk` | Konstruas APK + alŝutas al Play Store (release-varianto) — **nia nuna vojo** |
| `:androidApp:publishReleaseBundle` | Konstruas AAB + alŝutas — postulas Play App Signing-enskribiĝon |
| `:androidApp:promoteArtifact` | Plipromocias eldonon de unu track al alia |
| `:androidApp:publishListing` | Alŝutas ap-priskribon, screenshot-ojn, ktp. |

### Ligiloj

- GPP-dokumentaro: https://github.com/Triple-T/gradle-play-publisher
- GPP-versioj: https://plugins.gradle.org/plugin/com.github.triplet.play
- Google Play Console: https://play.google.com/console
- Quick Start: https://github.com/Triple-T/gradle-play-publisher#usage

> **Noto pri versioj**: GPP 4.x postulas Gradle 9.1.0+. Ni uzas 3.12.2 ĉar
> nia Gradle-versio estas 8.11.1. Kiam la projekto ĝisdatigos al Gradle 9+,
> ŝanĝu al 4.1.1 en `androidApp/build.gradle.kts`.

### Sekurecaj notoj

- **NENIAM versiigu `play-service-account.json`** — ĝi estas en `.gitignore`
- La subskriba keystore restas ĉe `/home/j/android/A_signaturer/jacobnordfalk.keystore`
  (aŭ per env-variablej `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, `KEYSTORE_KEY_PASSWORD`)
- En CI (GitHub Actions), stoku la JSON-ŝlosilon kaj keystore kiel sekretaj variabloj

---

## 2. F-Droid

F-Droid funkcias alie ol Google Play — **vi ne alŝutas APK-on**. F-Droid mem
konstruas la apon el via fontkodo kaj subskribas ĝin.

### Kio estas pretigita

- Metadatena ŝablono: `fdroid/metadata/dk.nordfalk.esperanto.radio.yml`
- GPL-3.0-or-later licenco (kongrua kun F-Droid-postuloj)

### Paŝoj por eldoni

1. **Kreu GitLab-konton** ĉe https://gitlab.com (se vi ne jam havas)

2. **Forku la fdroiddata-deponejon**:
   - Iru al https://gitlab.com/fdroid/fdroiddata
   - Klaku "Fork"

3. **Kopiu la metadatenon**:
   ```bash
   git clone https://gitlab.com/VIA_UZANTNOMO/fdroiddata.git
   cp fdroid/metadata/dk.nordfalk.esperanto.radio.yml \
      fdroiddata/metadata/dk.nordfalk.esperanto.radio.yml
   ```

4. **Kreu git-etikedon** en ĉi tiu deponejo:
   ```bash
   git tag v3.0.0
   git push origin v3.0.0
   ```
   La etikedo devas kongrui kun `versionName` kaj `versionCode` en `build.gradle.kts`.

5. **Kreu merge request** al `fdroid/fdroiddata`:
   - La F-Droid-teamo revizios vian MR
   - Procezo povas daŭri tagojn ĝis semajnojn
   - Post aprobo, F-Droid aŭtomate konstruas kaj eldonas la apon

6. **Estontaj ĝisdatigoj**: kreu novan git-etikedon (`v3.0.1`, `v3.1.0`, ktp.)
   kaj puŝu ĝin. F-Droid aŭtomate detektos kaj rekonstruos.

### Reprodukteblaj konstruoj (rekomendita)

Por ke F-Droid povu kontroli ke ĝia konstruo kongruas kun via, la konstruo devas esti reproduktebla:

- `isMinifyEnabled = false` (jam agordita)
- Neniuj dependecoj kiuj varias inter konstruoj (ekz. tempor-markoj en dosiernomoj)
- Konservu versionCode/versionName stabilaj inter etikedo kaj build.gradle

Por reproduktebleco, oni povas specifi `AllowedAPKSigningKeys` en la metadateno
kun la SHA-256 de via subskriba atestilo.

### Propra F-Droid-nightly-repo (opcia)

Por evoluantoj kiuj volas tujajn ĝisdatigojn sendepende de la oficiala F-Droid-revizio:

```bash
# Instalu fdroidserver
sudo apt install fdroidserver

# Kreu novan deponejon
mkdir ~/fdroid-repo && cd ~/fdroid-repo
fdroid init
fdroid update --create-metadata

# Aldonu vian APK-on
cp androidApp/build/outputs/apk/release/androidApp-release.apk ~/fdroid-repo/
fdroid update
```

Dokumentaro: https://f-droid.org/docs/Publishing_Nightly_Builds/

### Ligiloj

- Quick Start: https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- Build Metadata Reference: https://f-droid.org/docs/Build_Metadata_Reference/
- Reprodukteblaj konstruoj: https://f-droid.org/docs/Reproducible_Builds/
- fdroiddata-deponejo: https://gitlab.com/fdroid/fdroiddata
- F-Droid-stilo: https://f-droid.org/docs/All_about_descriptions/

---

## 3. Aptoide

Aptoide havas API por aŭtomata alŝuto de APK/AAB.

### Paŝoj por eldoni

1. **Kreu konton ĉe Aptoide Connect**:
   - Iru al https://connect.aptoide.com/
   - Registriĝu kiel evoluanto

2. **Akiru API-ŝlosilon**:
   - En la Aptoide Connect-konzolo, iru al **Settings** → **API Keys**
   - Kreu novan API-ŝlosilon

3. **Alŝutu per API** (curl-ekzemplo):

   ```bash
   # Pliigu versionCode kaj versionName en androidApp/build.gradle.kts unue!
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
   ./gradlew :androidApp:assembleRelease

   # Alŝutu al Aptoide
   curl -X POST \
     "https://api.aptoide.com/v1/api/uploadApp/v3" \
     -H "Authorization: Bearer VIA_API_SXLOSILON" \
     -F "apk=@androidApp/build/outputs/apk/release/androidApp-release.apk" \
     -F "package_name=dk.nordfalk.esperanto.radio" \
     -F "version_code=244" \
     -F "version_name=3.0.0"
   ```

   Noto: La preciza API-endpoint kaj parametroj povas varii — vidu la oficialan dokumentaron.

4. **Permana alternative**: Uzu la Aptoide Connect-retinterfacon por alŝuti la APK-on.

### Ligiloj

- Aptoide Connect: https://connect.aptoide.com/
- Uploader API-dokumentaro: https://docs.connect.aptoide.com/reference/android-app-version-submission-api
- API-plena dokumentaro: https://docs.connect.aptoide.com/

---

## 4. Aŭtomata CI/CD (GitHub Actions)

La workflow-dosiero `.github/workflows/eldonado.yml` aŭtomate konstruas kaj eldonas:

- **Ĉe `git tag v*`**: konstruas subskribitan APK-on, eldonas al Google Play (internal track)
- La APK estas alŝutita kiel GitHub-artefakto (por permana alŝuto al Aptoide)
- F-Droid ne estas en la CI — ĝi konstruas mem el la git-etikedo

### Postulataj GitHub-sekretaj variabloj

| Nomo | Priskribo |
|---|---|
| `KEYSTORE_BASE64` | Base64-kodita keystore-dosiero |
| `KEYSTORE_PASSWORD` | Keystore-pasvorto |
| `KEYSTORE_ALIAS` | Ŝlosila alinomo |
| `KEYSTORE_KEY_PASSWORD` | Ŝlosila pasvorto |
| `PLAY_SERVICE_ACCOUNT_JSON` | JSON-enhavo de la Play Console-servila konto |

Agordu ilin ĉe: GitHub → Deponejo → Settings → Secrets and variables → Actions

### Kiel uzi

1. Plibonigu `versionCode` kaj `versionName` en `gradle/libs.versions.toml` (linio `apoversio`)
   kaj `androidApp/build.gradle.kts` (linio `versionCode`)
2. Kreu git-etikedon:
   ```bash
   git tag v3.0.1
   git push origin v3.0.1
   ```
3. GitHub Actions aŭtomate konstruas kaj eldonas al Google Play

---

## Resumo de unuavice necesaj paŝoj

1. **Google Play**:
   - Kreu servilan konton en Play Console
   - Metu JSON ĉe `androidApp/play-service-account.json`
   - `KEYSTORE_PASSWORD=xxxxxx ./gradlew :androidApp:publishReleaseApk`

2. **F-Droid**:
   - Forku `fdroiddata` ĉe GitLab
   - Kopiu `fdroid/metadata/dk.nordfalk.esperanto.radio.yml`
   - Kreu MR

3. **Aptoide**:
   - Kreu konton ĉe Aptoide Connect
   - Akiru API-ŝlosilon
   - Alŝutu APK per API aŭ retinterfaco

4. **CI/CD**:
   - Agordu GitHub-sekretajn variablojn (keystore + Play JSON)
   - Kreu git-etikedon por aŭtomate eldoni
