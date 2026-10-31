---
name: eldonado
description: Ŝargu ĉi tiun skill-on kiam la uzanto petas eldoni novan version al Google Play, promocii eldonon, ĝisdatigi eldon-notojn, baskuli versionCode, aŭ kiam ajn temas pri la eldona procezo (signado, GPP-taskoj, Play Console, F-Droid, Aptoide).
---

# Eldonado de EsperantoRadio

> La plena gvidilo ankaŭ vivas en `docs/nova/07_eldonado.md`; la oficiala GPP-dokumentaro:
> https://github.com/Triple-T/gradle-play-publisher

## Resuma laborfluo (nova versio X.Y.Z)

0. **Eldonoj ĉiam tra PR** (2026-10-25, decido de la posedanto): kreu branĉon
   `MMdd-eldono-XY` de master, montru la ŝanĝojn al la posedanto ANTAŬ ol
   komiti, kaj kunfandu nur post aparta eksplicita aprobo („kunfandu“ — regulo
   L6). Neniam komitu eldonon rekte sur master. `gh pr create` bezonas
   `--head <branĉo>` (la loka fetch-refspec estas limigita al master). La
   paŝoj 4 (etikedo) kaj 6 (publish) okazas nur POST la squash-kunfando: la
   etikedo devas stari sur la SQUASH-komito sur master, ne sur la branĉa komito.
   **Internaj eldonoj NENIAM restu en la PR** (2026-10-29, decido de la
   posedanto): se vi faras internan teston-eldonon el PR-branĉo, NE komitu la
   eldonan libraron (apoversio-beta-nomo, versionCode-baskulo, internal.txt)
   al la PR — faru ilin en la laborarbo SEN commito (aŭ forigu/revertu la
   komiton tuj post la publikigo). Nur la versionCode restas konsumita ĉe
   Play (ne re-uzebla); la venonta vera eldono prenas la sekvan liberan
   versionCode. La interna testo estas esplorilo por la PR, ne parto de ĝia
   historio.
1. **`apoversio`** en `gradle/libs.versions.toml` ricevas la novan nombron (unu fonto de
   vero por `versionName`/Sentry — `ApoVersio.kt` generiĝas el ĝi).
2. **Baskulu `versionCode` KAJ `versionName`** en `androidApp/build.gradle.kts`
   (`defaultConfig.versionCode` + `versionName` — la versionName devas esti
   LAŬVORTA citita ĉeno, ekz. `versionName = "3.1.1"`, kaj kongrui kun
   `apoversio` en `gradle/libs.versions.toml`: la F-Droid-ĝisdatigilo parsas
   versionName per regex, kiu bezonas cititan signoĉenon —
   `libs.versions.apoversio.get()` estas por ĝi nelegebla (2026-10-31, C50)).
   GPP ne pliigas versionCode aŭtomate kaj la Play-API rifuzas re-uzi jam
   eldonitan versionCode ("Version code is too low or has already been used").
   Historio: 246=3.0.2, 247=3.0.3, 248=3.0.3-rekonstruo, 249=kvoto-protekto,
   250=3.0.4-19okt, 251=3.0.4-beta24okt (2026-10-24), 252=3.1.0 (2026-10-25),
   253=3.1.1-beta29okt (INTERNA TESTO, 2026-10-29, el la nekunfandita PR #144 —
   bruligis la versionumon; NE ĝisdatigu versionCode de ekzistanta etikedo!),
   254=3.1.1 (2026-10-31, la vera eldono; F-Droid konstruas la etikedon v3.1.1
   kiel 253 — ilia versionumejo estas sendependa de Play).
3. **Alinomu `## [Neeldonita]` al `## [X.Y.Z] - JJJJ-MM-TT`** en CHANGELOG.md kaj malfermu
   novan malplenan `## [Neeldonita]`-sekcion; aldonu kompar-ligon malsupre
   (`[X.Y.Z]: https://github.com/nordfalk/EsperantoRadio/compare/vANTAŬA...vX.Y.Z`).
   **Interna-testaj versioj ne atingas produktadon** (2026-10-25, 3.1.0): se
   versio iris nur al la trako `internal` (ekz. 3.0.4-beta24okt), ĝia
   CHANGELOG-sekcio kunfandiĝas en la sekvan veran version — produktaciaj
   uzantoj neniam vidis ĝin, do la vera versio portas la tutan delton.
   Forigu tiam ankaŭ la orfajn kompar-ligojn kaj uzu en la nova ligo la lastan
   VERAN git-etikedon (interna-testaj komitoj ne ricevas etikedon).
4. **Kreu git-etikedon** `vX.Y.Z` sur la eldona komito kaj puŝu ĝin
   (`git tag vX.Y.Z && git push origin vX.Y.Z`). F-Droid konstruas el la etikedo —
   do la eldonita APK devus kongrui kun etikedita komito (ne eldonu el nekompromita
   laborarbo kiam eviteble; se tamen necesis, komitu la ĝustan fonton tuj poste —
   kiel farite por 249 per PR #109).
5. **Eldon-notoj** (intence en Esperanto, maks. 500 signoj):
   `androidApp/src/main/play/release-notes/en-US/internal.txt`.
   **GPP elektas la dosieron laŭ la TRAKO-nomo** — `internal.txt` (aŭ `default.txt`
   kiel retroiro) — NENIAM laŭ versionName. Versio-nomitaj dosieroj (`3.0.3.txt` ktp.)
   estas ignoritaj de GPP (tiel ĉiuj eldonoj 3.0.2…249 eldoniĝis kun la malnovaj
   3.0.1-notoj; la 249-notoj korektiĝis permane). La historiaj `X.Y.Z.txt`-dosieroj
   estas arkivigitaj en `androidApp/eldon-notoj-arkivo/`. NENIAM interne de
   `src/main/play/`: GPP 4.x validigas ĈIUJN dosierojn tie kaj rifuzas la taskon
   per „Unknown Play resource file“ (2026-10-24, la unua GPP-4-eldono).
   **La notoj resumu la delton ekde la lasta PRODUKTADA versio** — ne ekde la
   lasta interna testo, kiun nur testantoj vidis (2026-10-25, 3.1.0).
   **La signolimo 500 validas laŭ UTF-16-unuoj**, ne laŭ karpunktoj:
   `len(s.encode('utf-16-le'))//2` en Python (emooj kiel 🗣️ kalkuliĝas po 2).
   **Ĝisdatigu ankaŭ `apaj_novaĵoj`** en la `android`-sekcio de la
   kanalkonfiguro — la versio-anonco, kiu montriĝas kiel dialogo al ĉiuj
   uzantoj kiam ĝia haketaĵo ŝanĝiĝas (nova enhavo = nova dialogo). La kanalkonfiguro
   vivas nur en `shared/src/commonMain/resources/esperantoradio_konfiguro.json`
   (unu kopio por ĉiuj platformoj; JSONC-detalon vidu en la
   `kanalkonfiguro`-skill-o).
6. **Eldonu al Google Play de la LOKA maŝino** (2026-10-13: nur la loka maŝino rajtas —
   la CI-`eldonado.yml` per GitHub-sekretoj ne uzeblas nuntempe):
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
   ./gradlew :androidApp:publishReleaseApk   # APK, ne AAB — la apo NE estas en Play App Signing
   ```
   Tio eldonas al la trako `internal` (la `play`-bloko en `androidApp/build.gradle.kts`).
   La signado legas `secrets.properties` en la radiko — NENIAM tuŝu/legu/komitu ĝin.
7. **Promocio al produktado**: permane en Play Console (aŭ per `promoteReleaseArtifact`,
   vidu suben). Play disvastigas la ĝisdatigon laŭgrade dum horoj–tagoj.
   La konzolo-URL de la apo (releases-superrigardo, 2026-10-31):
   https://play.google.com/console/u/0/developers/8294421063218887510/app/4973090730523647228/releases/overview

## GPP-taskoj (el `./gradlew :androidApp:tasks`)

| Tasko | Kion ĝi faras |
|---|---|
| `publishReleaseApk` | APK + eldon-notoj (`internal.txt`) — **kion ni uzas** |
| `publishReleaseListing` | **aparte** alŝutas metadatumojn (priskribo, ekranfotoj el `play/listings/`) — `publishReleaseApk` NE inkluzivas ilin! |
| `publishReleaseApps` | APK + metadatumoj kune |
| `promoteReleaseArtifact` | promocias eldonon inter trakoj (`play { fromTrack/promoteTrack }`) |
| `bootstrapReleaseListing` | elŝutas la NUNAN konzolan staton loken — diagnozilo |

- **FOOTGUN**: `defaultToAppBundles.set(true)` en la `play`-bloko + la agrega tasko
  `publish`/`publishApps` → GPP provus AAB → 403 (ne enskribita en Play App Signing).
  Ĉiam uzu la eksplicitan `publishReleaseApk` aŭ ŝanĝu `defaultToAppBundles` al `false`.

## Jam konataj limoj kaj problemoj

- **fdroidserver forigas la signingConfigs-blokon antaŭ konstrui**
  (`common.remove_signing_keys`, 2026-10-31): la keystore-kontrolo en
  `androidApp/build.gradle.kts` devas uzi `findByName` (nenul-sekura) —
  `getByName` ĵetus per „SigningConfig with name 'release' not found" sur la
  F-Droid-buildservilo.
- **Release subskribas nur kiam la keystore-dosiero ekzistas** (2026-10-29, C50):
  `assembleRelease` sen keystore produktas `androidApp-release-unsigned.apk` —
  necesa por F-Droid (ĝi konstruas kaj subskribas mem); la loka subskribita
  konstruo ne ŝanĝiĝas. F-Droid-faktoj: `gradlew-fdroid` legas la radikan
  `gradle/wrapper/gradle-wrapper.properties` ankaŭ el `subdir: androidApp`;
  la `gradle:`-kampo en la metadateno signifas GUSTUMOJN (`yes` = `assembleRelease`),
  ne taskojn. Detaloj: `docs/nova/07_eldonado.md`, sekcio 2.
- **Subskriba eraro „Keystore was tampered with, or password was incorrect“**
  (2026-10-25, 3.1.0): la subskribo legas `KEYSTORE_*` el `secrets.properties`
  (la build-defaŭltoj estas malplena pasvorto + la vojo `/home/j/android/A_signaturer/`
  — agentaj sesioj ne havas la mediovariablojn). Se la eraro aperas, la pasvorto
  en `secrets.properties` estas malĝusta aŭ malaktuala — la POSEDANTO devas
  korekti ĝin; agentoj ne rajtas legi la dosieron (regulo A12).
- **Eldonitan versionCode oni NE povas re-alŝuti**: reprovo de `publishReleaseApk`
  por jam alŝutita versionCode fiaskas per „Version code is too low or has
  already been used“ — sed tio mem estas KONFIRMO ke la unua alŝuto sukcesis
  (2026-10-25, 252). Ne uzu `--rerun` por kontroli eldonon; kaptu la eligilon de
  la unua rulo (`Updating [completed] release (...:[N]) in track 'internal'`)
  — reprovo montras nur `UP-TO-DATE` sen la konfirman linion.
- **Notoj de jam eldonita versionCode NE korekteblas per GPP** (la API rifuzas
  versionCode-reuzon) — korektu ilin permane en Play Console (kiel farite por 249).
- **AAB ne eblas** ĝis la apo enskribiĝos en Play App Signing (403 "must be enrolled").
- **Reproduktebleco**: versio-identaj rekonstruoj (247/248/249 ĉiuj = 3.0.3) malfaciligas
  la diagnozon — `dist` (= versionCode) en Sentry estas la nura distingilo por eraroj.
- **Keystore**: `jacobnordfalk.keystore` (2010) estas la kronjuvelo — sen Play App
  Signing, ĝia perdo signifas ke la apo neniam plu ĝisdateblas. Kontrolu rezervkopiojn.
- **F-Droid** konstruas mem el la git-etikedo (duon-aŭtomata; vidu `docs/nova/07`).
- **Aptoide** bezonas konton + API-ŝlosilon (vidu `docs/nova/07`).

## GPP-versio kaj kongrueco

- La GPP-versio estas fiksita en `androidApp/build.gradle.kts`. Ekde la
  AGP-9-ĝisdatigo ni uzas la 4.x-linion (2026-10-17): GPP 4.0 subtenas nur
  AGP 9 — kun malnova AGP ĝi ne funkcius (restu je 3.x).
- Tasko-nomoj ne ŝanĝiĝis inter 3.x kaj 4.x (`publishReleaseApk` ktp. restas).
- GPP 4.1.0 ŝanĝoj: ap-titolo en listing-metadatumoj maks. **30 signoj** (estis
  50); nova agordo `retainInProgressRollout` (evitas haltigon de rulata
  disvastigo dum promocio).
- **GPP 4.x estas nun praktike testita** (2026-10-24, 251): la tasko-nomoj kaj la
  atendata eligilo (`Updating [completed] release (...:[N]) in track 'internal'`)
  funkcias; du provoj lernitaj: la versio-nomitaj dosieroj rompas la validigilon
  (vidu supre) kaj la versionCode-historio ĉi tie devas resti ĝisdata — 250 estis
  jam okupita de 3.0.4-19okt kiam ĝi ankoraŭ mankis en la listo.

## Kontrolu post la eldono

- La tasko-eligilo devas montri `Updating [completed] release (...:[N]) in track 'internal'`.
- Post la promocio: observu Sentry (per la `sentry-esplorado`-skill) — uzantoj laŭ
  `dist`, eraro-kvanto malkreskos pro la specimenado/agordo de la nova versio.
