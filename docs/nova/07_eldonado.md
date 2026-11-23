# Eldonado de EsperantoRadio

> Gvidilo por eldoni la Android-apon ĉe Google Play, F-Droid kaj Aptoide.

## Superrigardo

| Vendejo | Aŭtomateco | Postuloj                           | Stato |
|---|---|------------------------------------|---|
| **Google Play** | Tute aŭtomata per Gradle Play Publisher (GPP) | Servila konto JSON, $25-konto      | Aktiva (eldonado de la loka maŝino) |
| **F-Droid** | Duon-aŭtomata (F-Droid konstruas mem el la git-etikedo) | GPL-permesilo, GitLab-konto | Metadateno + nesubskribita konstruo pretaj (2026-10-29) — restas: MR al fdroiddata |
| **Aptoide** | Duon-aŭtomata (retkonzolo, API havebla) | Aptoide Connect-konto | Restas: posedanto kreas konton kaj alŝutas la APK-on |

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
   (Konvencio de la posedanto, 2026-11-19: «faru beta-eldonon» signifas internan test-eldonon al `internal`.)

### Uzataj versionCode

La kanona historio de uzitaj versionCode (Play rifuzas reuzon!) staras en
[`.vibe/skills/eldonado/SKILL.md`](https://github.com/nordfalk/EsperantoRadio/blob/master/.vibe/skills/eldonado/SKILL.md)
(paŝo 2 de la eldona laborfluo) — la dosiero estas en la deponejo, do rekte videbla sur GitHub.
Ĉiu eldono, inkluzive de internaj testoj el PR-branĉoj (ekz. 253, 256, 257),
aldonas sian linion tie; nur la versionCode restas konsumita ĉe Play.

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

> **Noto pri versioj**: la GPP-versio estas fiksita en `androidApp/build.gradle.kts`
> (ekde la AGP-9-ĝisdatigo ni uzas la 4.x-linion). GPP 4.0 subtenas nur AGP 9
> (malnova AGP/Gradle ne subtenata — restu je 3.x se ne ĝisdatigita). Tasko-nomoj
> ne ŝanĝiĝis. Nova en 4.x: la ap-titolo en listing-metadatumoj havas limon de
> 30 signoj (estis 50), kaj `retainInProgressRollout` povas eviti haltigon de
> interrompita disvastigo dum promocio. Kiam vi reeldonas la unuan fojon post la
> GPP-4-ĝisdatigo, kontrolu la `publishReleaseApk`-eligilon atente (nova ĉefa versio).

### Sekurecaj notoj

- **NENIAM versiigu `play-service-account.json`** — ĝi estas en `.gitignore`
- La subskriba keystore restas ĉe `/home/j/android/A_signaturer/jacobnordfalk.keystore`
  (aŭ per env-variablej `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, `KEYSTORE_KEY_PASSWORD`)
- En CI (GitHub Actions), stoku la JSON-ŝlosilon kaj keystore kiel sekretaj variabloj

---

## 2. F-Droid

F-Droid funkcias alie ol Google Play — **vi ne alŝutas APK-on**. F-Droid mem
konstruas la apon el via fontkodo (el la git-etikedo) kaj subskribas ĝin per
sia propra ŝlosilo.

### Kio estas pretigita (2026-10-29, C50)

- **Metadateno**: `fdroid/metadata/dk.nordfalk.esperanto.radio.yml` — en la
  KANONA formo de `fdroid rewritemeta` (neniu komento — alie la CI-tasko
  „fdroid rewritemeta" ruĝiĝas), kun `Builds`-ero por 3.1.1/253 (etikedo
  `v3.1.1`), `AutoUpdateMode: Version` + `UpdateCheckMode: Tags`,
  `UpdateCheckName: Ignore` kaj `scanignore: build.gradle.kts`; `commit` estas
  la plena haketaĵo (la ŝablono malpermesas etikedojn). NENIU teksto rajtas en
  fdroiddata — resumo, priskripo, ŝanĝoprotokolo (`default.txt`) kaj bildoj
  venas el nia Play-listing (Triple-T). ĈIŬ SES CI-taskoj de
  la forko estis reproduktitaj verdaj loke per la samaj iloj (2026-10-31):
  `check-jsonschema` (skemo), `fdroid rewritemeta`/`lint`, la kvin
  `tools/`-kontrolskriptoj, `fdroid checkupdates --auto` („up to date") kaj
  la fonta skanilo („Ignoring unknown maven repo" per scanignore).
  Submetebla kiel tri dosieroj sub `metadata/`.
- **Nesubskribita konstruo**: la release-buildtype en `androidApp/build.gradle.kts`
  subskribas nur kiam la keystore-dosiero ekzistas. Sen `secrets.properties` kaj
  sen keystore `./gradlew :androidApp:assembleRelease` produktas
  `androidApp-release-unsigned.apk` (kontrolita 2026-10-29 en pura worktree);
  F-Droid subskribas ĝin mem per sia ŝlosilo.
  **ATENTU**: fdroidserver antaŭ ĉiu konstruo FORIGAS la `signingConfigs`-blokon
  el ĉiu `build.gradle(.kts)` (`common.remove_signing_keys`) — tial la keystore-
  kontrolo devas uzi `signingConfigs.findByName("release")` (nenul-sekura), NE
  `getByName` (ĵetus „SigningConfig with name 'release' not found", 2026-10-31).
  Reprodukte loke: rulu `remove_signing_keys` sur kopio kaj konstruu — la
  konstruo devas daŭri nesubskribita.
- **Permesilo**: GPL-3.0-or-later (`LICENSE` en la radiko) — kongrua.
- **Neniuj binaroj en la deponejo** krom la norma
  `gradle/wrapper/gradle-wrapper.jar`, kiun la F-Droid-skanilo konas.
- **Gradle-versio**: F-Droid rulas `gradlew-fdroid` (propra reiaĵo de la
  gradlew-rajto, kiu elŝutas la version de `gradle/wrapper/gradle-wrapper.properties`
  kun SHA-256-kontrolo). Ĝi serĉas la dosieron ankaŭ en la gepatra dosierujo —
  do `subdir: androidApp` trovas la radikan `gradle/wrapper/`. La tasko
  `assembleRelease` lanĉita el `androidApp/` solviĝas kiel `:androidApp:assembleRelease`
  (kontrolite 2026-10-29). Gradle 9.8.1 do ne dependas de la versio sur la
  buildservilo.
- **JDK**: la Android-konstruo funkcias sur JDK 17 (same kiel loke).

### Kion FARAS F-Droid kun nia metadateno

```yaml
Builds:
  - versionName: '3.1.1'
    versionCode: 253
    commit: v3.1.1
    subdir: androidApp
    gradle:
      - yes
```

Tio signifas: elprenu `v3.1.1`, rulu `gradlew-fdroid assembleRelease` en
`androidApp/`, trovu la APK-on, subskribu per la F-Droid-ŝlosilo.
(ATENTU: la `gradle:`-kampo signifas GUSTUMOJN, ne taskojn — `yes` = sen
gustumo, do `assembleRelease`. Pliaj taskoj iras en `preassemble:`.)

### Paŝoj por eldoni

1. **Kreu GitLab-konton** ĉe https://gitlab.com (se vi ne jam havas).
2. **Forku https://gitlab.com/fdroid/fdroiddata**.
3. **Kopiu la metadatumojn** al fork-branĉo nomata laŭ la apo:
   ```bash
   git clone https://gitlab.com/VIA_UZANTNOMO/fdroiddata.git
   cd fdroiddata
   git checkout -b dk.nordfalk.esperanto.radio
   cp /vojo/al/EsperantoRadio/fdroid/metadata/dk.nordfalk.esperanto.radio.yml \
      metadata/dk.nordfalk.esperanto.radio.yml
   git add metadata/dk.nordfalk.esperanto.radio.yml
   git commit -m "New App: dk.nordfalk.esperanto.radio"
   git push origin dk.nordfalk.esperanto.radio
   ```
4. **Kreu merge request** al `fdroid/fdroiddata`. La GitLab-CI de fdroiddata
   aŭtomate provas konstrui — se ĝi malsukcesas, la eligilo montras kial.
5. **Atendu la revizion** (kelkaj tagoj ĝis semajnoj); respondu al demandoj
   de la kontrolistoj en la MR-fadeno. Detala paŝon-post-paŝa trairado (ankaŭ
   por la fork/MR en GitLab kaj pretaj respondoj al kontrolistoj):
   [`fdroid/GVIDILO.md`](../fdroid/GVIDILO.md).

### Estontaj ĝisdatigoj

Kreu novan git-etikedon (`v3.1.1`, `v3.2.0`, ktp.) kaj puŝu ĝin. La
`AutoUpdateMode: Version` + `UpdateCheckMode: Tags` igas F-Droid-on
aŭtomate detekti la etikedon kaj pretigi ĝisdatigo-konstruon.

### Sentry kaj la "Tracking"-kontraŭeco — SOLVITA (PR #148, ĝisdatigita 2026-11-22)

La apo sendis erarojn kaj protokolojn al sentry.io defaŭlte, sen konsento —
laŭ la F-Droid-lingvaĵo (https://f-droid.org/en/docs/Anti-Features/) tio
estus la kontraŭeco **Tracking**. Ekde PR #148 (2026-10-29) la defaŭlto
dependas de la subskriba atestilo de la APK. Ekde 2026-11-22 tio estas
kontrolata RUTEMPE ĉe starto (`estasSubskribitaPerNiaAtestilo()` en
`EsperantoRadioAplikaĵo.kt`, per `PackageManager` + SHA-256), NE plu per
`BuildConfig.SEN_NIA_SUBSKRIBO` — konstruo-dependa valoro en la APK
malebligus bit-identan reprodukteblan konstruon (F-Droid RB, MR !55129):

- **APK kun fremda subskribo** (tiel F-Droid subskribas): **opt-in** —
  defaŭlte malŝaltita, unufoja konsento-demando ĉe la unua starto. La apo ne
  ricevas la kontraŭecon "Tracking".
- **APK kun nia subskribo** (Google Play, Aptoide, GitHub-releases):
  **defaŭlte ŝaltita**, sen demando — la uzanto povas malŝalti en Agordoj →
  Privateco ("Sendu erar-raportojn"). Ĉe malŝalto iras unu LASTA protokolo al
  Sentry ("malŝaltita de la uzanto"), ĉe (re)ŝalto unu protokolo ("(re)ŝaltita").

La konduto do ne ŝanĝiĝis por la uzanto; ŝanĝiĝis nur KIAM ĝi decidiĝas
(rultempe, ne dum la konstruo). La eks-kontrolo baziĝis sur
`BuildConfig.SEN_NIA_SUBSKRIBO` (vera kiam la keystore mankis dum la
konstruo) — tio diferencigus la APK-bitokojn inter konstru-medioj kaj estas
nun forigita.
Ambaŭ reĝimoj: ŝanĝoj de la protokolo-fluo ekvalidas tuj, kraŝ-raportoj
(nekapteblaj esceptoj) post restartigo.
Sekvo por la Sentry-esplorado (C48/C49/G44): la telemetrio el F-Droid-uzantoj
malpliiĝas (nur konsentintoj raportas); Play-uzantoj plu raportas kaj iliaj
malŝaltoj/reŝaltoj aperiĝos kiel logGrava-eventoj.

### Reprodukteblaj konstruoj (RB) — FARBAS (MR !55129, 2026-11-22)

Norma F-Droid-konstruo estas subskribita per la F-Droid-ŝlosilo. La
F-Droid-revizianto demandis en MR !55129 („Why no RB?“) — ekde 2026-11-22 la
konstruo estas pretigita bit-identa:

- `dependenciesInfo { includeInApk = false; includeInBundle = false }` en
  `androidApp/build.gradle.kts` (vidu https://gitlab.com/fdroid/fdroiddata/-/issues/3330)
- la telemetria defaŭlto ne plu bakeblas en la APK: rultempa kontrolo de la
  subskriba atestilo (vidu la Sentry-sekcion supre)
- Kontrolite 2026-11-22: du sendependaj konstruoj de la sama fontstato
  produktas bit-identan APK-on (`9e6acc63…`; clean + assembleRelease
  nesubskribita, kaj clean + `--rerun-tasks --no-build-cache`)
- `AllowedAPKSigningKeys: aab92d2bb6401743128fbe641ff86a0875ca95cb2cc5e15b1d4a7cd5e534eb16`
  (SHA-256 de nia subskriba atestilo, 2026-10-29) — en la metadateno kiam ni
  ankaŭ petos la `Binaries`-vojon

POR FARI por vera RB-konfirmo de F-Droid:

1. Venonta eldono (nova git-etikedo, ekz. `v3.1.2`) devas havi versionCode, kiu
   kongruas inter Play kaj la etikedo (unu numero — la 253/254-dissplitiĝo de
   3.1.1 ne ripetebla).
2. Subskribita APK de la etikedo publikigita en GitHub-releases (F-Droid
   konstruas el la etikedo kaj komparas malpostsubskribe).
3. Ĝisdatigita yml (new versionName/versionCode/commit + `AllowedAPKSigningKeys`)
   + respondo en la MR-fadeno.

### Propra F-Droid-repo (opcia)

Por tujaj ĝisdatigoj sendepende de la oficiala revizio ekzistas propra
fdroid-repo (uzantoj aldonas ĝin per deponeja URL):

```bash
sudo apt install fdroidserver
mkdir ~/fdroid-repo && cd ~/fdroid-repo
fdroid init
fdroid update --create-metadata
cp androidApp/build/outputs/apk/release/androidApp-release.apk ~/fdroid-repo/
fdroid update
```

Dokumentaro: https://f-droid.org/docs/Publishing_Nightly_Builds/

### Ligiloj

- Quick Start: https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- Build Metadata Reference: https://f-droid.org/docs/Build_Metadata_Reference/
- Anti-Features (Tracking): https://f-droid.org/en/docs/Anti-Features/
- FAQ por ap-evoluigistoj: https://f-droid.org/gd/docs/FAQ_-_App_Developers/
- fdroiddata-deponejo: https://gitlab.com/fdroid/fdroiddata
- gradlew-fdroid: https://gitlab.com/fdroid/gradlew-fdroid
- Priskribo-stilo: https://f-droid.org/docs/All_about_descriptions/

---

## 3. Aptoide

Aptoide estas memstara vendejo (ne necese FOSS-ema — kontroli iliajn kondiĉojn).
La praktika vojo (2026-10-29) iras tra **Aptoide Connect**:
https://docs.connect.aptoide.com/

### Paŝoj por eldoni

1. **Kreu konton** ĉe https://connect.aptoide.com/ kaj ensalutu al la konzolo.
2. **Aldonu la apon** (pako `dk.nordfalk.esperanto.radio`). Ĉar la apo jam
   ekzistas en Google Play, Aptoide Connect prenas la metadatumojn de tie —
   titolo, priskribo kaj ekranfotoj neniam redakteblas duoble.
3. **Pruvu la posedon** de la pako-nomo (propra paŝo de Aptoide Connect;
   sekvu la instrukciojn en la konzolo — ili postulas, ke la posedanto
   respondu al la identigilo ligita al la Play-listing).
4. **Alŝutu la APK-on** (`androidApp-release.apk`, subskribita per nia ŝlosilo —
   sama APK kiel en Play; uzu per ilia retkonzolo). Aptoide disdonas la
   **samsubskribajn** ĝisdatigojn, do la elŝutejo montras la saman version.
5. **Revizio**: Aptoide kontrolas la politikon; post akcepto la apo publikiĝas.

### Notoj

- Aptoide Connect mencias **kotizon/abonon** por iuj funkcioj — kontrolu la
  nunajn kondiĉojn en la konzolo antaŭ ol subskribi ion.
- API por aŭtomata alŝuto ekzistas (vidu https://docs.connect.aptoide.com/),
  sed la unua eldono per la retkonzolo estas pli simpla; aŭtomatigebla poste.
- La `eldonado.yml`-CI alŝutas la APK-on kiel GitHub-artefakton — ĝi estas
  elŝutebla por permana alŝuto al Aptoide.
- La servo estas **nedeviga**: neniuj partoj de la apo dependas de Aptoide
  (neniuj Aptoide-SDK-oj estas integritaj).

### Ligiloj

- Aptoide Connect: https://connect.aptoide.com/
- Dokumentaro: https://docs.connect.aptoide.com/

---

## 4. Aŭtomata CI/CD (GitHub Actions)

> **ATENTU (2026-10-13):** la CI-eldonado nuntempe **malsukcesas** — la GitHub-sekretoj
> (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `PLAY_SERVICE_ACCOUNT_JSON`) ne estas
> konfiguritaj (vidu `ŜANĜOJ.md`, 2026-10-12). Eldonu per la **loka maŝino**:
> `./gradlew :androidApp:publishReleaseApk` kaj poste promocii permane en Play Console.

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
   - Kreu GitLab-konton, fork `fdroiddata`
   - Kopiu `fdroid/metadata/dk.nordfalk.esperanto.radio.yml` al `metadata/`
   - Kreu MR kaj sekvu la fdroiddata-CI-konstruon

3. **Aptoide**:
   - Kreu konton ĉe Aptoide Connect
   - Aldonu la apon, pruvu la posedon de la pako-nomo
   - Alŝutu `androidApp-release.apk` per la retkonzolo

4. **CI/CD**:
   - Agordu GitHub-sekretajn variablojn (keystore + Play JSON)
   - Kreu git-etikedon por aŭtomate eldoni
