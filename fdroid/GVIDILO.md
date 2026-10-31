# Gvidilo: Aldoni EsperantoRadio al F-Droid (por la posedanto)

> Skribita 2026-10-29 (C50). Ĝisdatigita stato: ĉio teknika pretas; mankas nur
> via GitLab-konto kaj ~30 minutoj da klakado. La mallonga versio staras en
> [`docs/nova/07_eldonado.md`](../docs/nova/07_eldonado.md), sekcio 2; ĉi tie
> estas la detala trairado.

## 0. Kiel F-Droid funkcias (por orientiĝi)

F-Droid estas VENDUMILA ŜANĜO rilate al Google Play:

- Vi **ne alŝutas APK-on**. Vi aldonas nur malgrandan "recepto-dosieron"
  (metadateno) al ilia deponejo `fdroiddata`.
- F-Droid mem elŝutas nian fontkodon ĉe la git-**etikedo** (`v3.1.1`),
  konstruas la APK-on en ilia konstruo-servilo kaj **subskribas ĝin per sia
  propra ŝlosilo**.
- Konsekvenco: la F-Droid-APK havas alian subskribon ol la Play-APK. Uzanto ne
  povas "transiri" inter la du vendejoj sen malinstalo/reinstalo. Nia apo
  tamen ne ligas datumojn al la subskribo, do tio ne damaĝas ion.
- Nia flanko jam preparis ĉion: la konstruo funkcias ankaŭ **sen** niaj
  subskribaj sekretoj (tial F-Droid povas konstrui), kaj la metadatenca
  "recepto" estas preta en `fdroid/metadata/dk.nordfalk.esperanto.radio.yml`.

## 1. Antaŭkontrola listo — kio jam pretas

| Kio | Stato |
|---|---|
| Metadateno (recepto) por 3.1.1/253, etikedo `v3.1.1` | ✅ en ĉi tiu deponejo (`fdroid/metadata/…`); kontraŭvalidigita kontraŭ la oficiala fdroiddata-skemo (`schemas/metadata.json`) |
| Git-etikedo `v3.1.1` sur GitHub | ✅ (unu fonto de vero kun versionCode 253) |
| Nesubskribita konstruo ebla (F-Droid kondiĉo) | ✅ pruvita 2026-10-29 |
| Permesilo GPL-3.0-or-later | ✅ `LICENSE` en la radiko |
| Neniuj malliberaj dependejoj / binaroj en la deponejo | ✅ |
| Sentry-erar-raportado **opt-in** en F-Droid-konstruoj | ✅ (PR #148 — sen tio ili atribuus la kontraŭecon "Tracking") |
| **GitLab-konto** | ❌ vi devas krei ĝin (paso 2) |

**Antaŭ ol daŭrigi**: la metadateno devas esti sur `master` (PR #148). Se la
PR ne estas kunfandita, uzu en la paŝoj anstataŭe la branĉan ligilon:
`https://github.com/nordfalk/EsperantoRadio/raw/1029-fdroid-aptoide/fdroid/metadata/dk.nordfalk.esperanto.radio.yml`

## 2. Kreu GitLab-konton (5 min)

1. Iru al https://gitlab.com/users/sign_up
2. Registriĝu (GitHub-ensaluto ankaŭ funkcias — butono "GitHub").
3. Nenio plu necesas pri agordado; la konton vi bezonos nur por la fork + MR.

## 3. Vojo A — tute per la retumilo (rekomendita; sen komandlinio)

### 3.1 Forku fdroiddata

1. Salutinte, iru al https://gitlab.com/fdroid/fdroiddata
2. Dekstre supre klaku **Fork**.
3. Lasu la agordon kiel estas (lasi la kampojn kiel ili estas: "Project name" = fdroiddata, videbleco = Private) → klaku **Fork project**.
4. Vi nun havas `https://gitlab.com/VIA_UZANTNOMO/fdroiddata`.

### 3.2 Aldonu la metadatenon en via fork

1. En via fork malfermu la dosierujon **`metadata`**
   (`https://gitlab.com/VIA_UZANTNOMO/fdroiddata/-/tree/master/metadata`).
2. Klaku la **`+`** apud la dosieruja vojo (…/metadata/) → **New file**.
3. Dosiernomo: `dk.nordfalk.esperanto.radio.yml`
   (la tuta vojo do iĝas `metadata/dk.nordfalk.esperanto.radio.yml`).
4. Enigu la enhavon: malfermu en dua langeto
   https://github.com/nordfalk/EsperantoRadio/raw/master/fdroid/metadata/dk.nordfalk.esperanto.radio.yml
   kaj kopiu la tutan tekston en la GitLab-redaktilon.
   (La esperantaj komentaj linioj komenciĝantaj per `#` estas permesataj —
   la F-Droid-iloj forigas ilin aŭtomate; ili helpas eventualajn reviziantojn.)
5. Malsupre ĉe "Target Branch" **ŝanĝu** la nomon al nova branĉo:
   `dk.nordfalk.esperanto.radio` (NE `master` — MR-oj el `master` estas
   malpermesitaj).
6. Commit message: `New App: dk.nordfalk.esperanto.radio` → **Commit changes**.

### 3.3 Kontrolu la CI-on de via fork

1. En via fork iru al **Build → Pipelines**
   (aŭ "CI/CD → Pipelines" en la malnova fasado).
2. Atendu la pipeline (typical 10–40 minutoj — ĝi vere provas konstrui la apon).
3. **Verda (passed)** → daŭrigu ĉe 3.4. La unua paŝo validigas la metadatenon
   kontraŭ `schemas/metadata.json` (kategorioj nur el ties listo, neniu
   lokalizita kampo en la yml, `AutoUpdateMode` laŭ la nova sintakso) — nia
   dosiero estas jam kontraŭvalidigita loke kontraŭ la sama skemo.
   **Ruĝa (failed)** → malfermu la malsukcesan taskon, legu la eraron kaj
   aldonu la eligilon al la sekva laborsesio kun mi.
   **Ruĝa (failed)** → malfermu la malsukcesan taskon, legu la eraron kaj
   aldonu la eligilon al la sekva laborsesio kun mi — la plej oftaj kaŭzoj
   estas YAML-tajperaro aŭ retaj eraretoj; ni riparos la metadatenon kaj vi
   refaros la paŝon 3.2.

### 3.4 Kreu la merge request-on

1. Reen en via fork: supre aperas flavrubando "You have X new commits" →
   klaku **Create merge request**. (Aŭ: malfermu
   https://gitlab.com/fdroid/fdroiddata/-/merge_requests → "New merge request"
   → elektu vian fork kaj la branĉon `dk.nordfalk.esperanto.radio`.)
2. Fonto: `VIA_UZANTNOMO/fdroiddata:dk.nordfalk.esperanto.radio` → Celo: `fdroid/fdroiddata`.
3. Titolo: `New App: dk.nordfalk.esperanto.radio`
4. Priskribo (kopiu ĉi tion — ĝi plenumas ilian ŝablonon):

   ```
   The app complies with the inclusion criteria (https://f-droid.org/docs/Inclusion_Policy/):
   GPL-3.0-or-later, source code and issue tracker on GitHub, no binaries in the repo,
   no proprietary dependencies.

   The original app author has been notified — I am the author of the app.

   No RFP issue exists for this app (checked https://gitlab.com/fdroid/rfp).

   Notes for reviewers:
   - The recipe builds version 3.1.1 (versionCode 253) from tag v3.1.1.
   - It is a Kotlin Multiplatform project: the Gradle build lives in the repo
     root, `subdir: androidApp` runs `assembleRelease` from the app module
     (verified: resolves to :androidApp:assembleRelease).
   - Gradle 9.8.1 comes from gradle/wrapper/gradle-wrapper.properties via
     gradlew-fdroid, so the buildserver Gradle version does not matter.
   - The release build signs only when our keystore exists; on the F-Droid
     buildserver it is absent, so the APK is unsigned and F-Droid signs it.
   - Error reporting (Sentry) is opt-in in builds without our signature
     (BuildConfig.SEN_NIA_SUBSKRIBO); there is a one-time consent dialog on
     first launch and a toggle in Settings → Privacy.
   - Summary/Description (Esperanto) are in the yml as the default;
     the en-US texts are read automatically from the Play listing in
     the repo (Triple-T structure: androidApp/src/main/play/listings/en-US).
   ```

5. Marku **"Squash commits when merge is accepted"** (defaŭlta) → **Create merge request**.

## 4. Vojo B — per komandlinio (se vi preferas)

```bash
git clone https://gitlab.com/VIA_UZANTNOMO/fdroiddata.git
cd fdroiddata
git checkout -b dk.nordfalk.esperanto.radio
curl -o metadata/dk.nordfalk.esperanto.radio.yml \
  https://github.com/nordfalk/EsperantoRadio/raw/master/fdroid/metadata/dk.nordfalk.esperanto.radio.yml
git add metadata/dk.nordfalk.esperanto.radio.yml
git commit -m "New App: dk.nordfalk.esperanto.radio"
git push -u origin dk.nordfalk.esperanto.radio
# poste: kontrolu la pipeline en GitLab, tiam malfermu la MR per la sama
# ŝablono kiel en 3.4
```

(Opcia plia kontrolo hejme: `pip install git+https://gitlab.com/fdroid/fdroidserver.git`,
tiam `fdroid readmeta` kaj `fdroid lint dk.nordfalk.esperanto.radio` en la
fdroiddata-klono. La vera konstruo tamen okazas en ilia CI — tio sufiĉas.)

## 5. Kio okazas post la MR — kaj kion respondi

- La revizio daŭras **kelkajn tagojn ĝis semajnojn**. Ekzemple la MR !24907 (julio 2025) estis reviziita kaj mergita ene de semajno; ne paniku se estas silenta.
- GitLab sendos retmesaĝon pri ĉiu komento — **respondu rapide**; MR-oj kun
  respondeda aŭtoro estas traktataj unue.
- Probablaj demandoj kaj pretaj respondoj:

| Demando | Respondo |
|---|---|
| "Why is `subdir` not the repo root?" | The Gradle build needs the root (KMP: `settings.gradle.kts`, `gradle/wrapper`); running from `androidApp/` resolves `assembleRelease` to `:androidApp:assembleRelease` and gradlew-fdroid finds the root wrapper properties one level up. Verified locally and in fdroidserver's `common.py`. |
| "Sentry = Tracking anti-feature?" | Reporting is opt-in in F-Droid builds: default off, one-time consent dialog on first launch, toggle in Settings → Privacy. See `BuildConfig.SEN_NIA_SUBSKRIBO` in `androidApp/build.gradle.kts`. |
| "Description is not English" | Esperanto is the default text (the app's audience language); the en-US texts come from the Play listing in the repo (androidApp/src/main/play/listings/en-US). |
| "Gradle 9/AGP 9 on the buildserver?" | gradlew-fdroid downloads Gradle 9.8.1 from our `gradle/wrapper` (SHA-256-verified via the gradle-transparency-log), independent of the buildserver's Gradle. |
| "Why unsigned APK?" | The release signing config only applies when our keystore exists; F-Droid builds have no access to it — intended, F-Droid signs with its key. |

- Se ili petas ŝanĝojn en la metadateno: bonvolu **ne korekti nur en via
  fork** — diru al mi kaj mi ĝisdatigos la dosieron en nia deponejo (unu
  fonto de vero) kaj vi refaros la fork-branĉon el la nova versio.

## 6. Post la akcepto

1. **Kiam ĝi aperas**: post la mergo, la konstruado okazas en la venonta
   konstruo-ciklo; praktike 1–3 tagojn. Kontrol-ligilo:
   https://f-droid.org/en/packages/dk.nordfalk.esperanto.radio/
   (paĝo "not found" = ankoraŭ ne).
2. **Estontaj versioj**: nenion plian vi devas fari ĉe F-Droid. Nia eldona
   procezo jam kreas la etikedon `vX.Y.Z` kaj puŝas ĝin; `AutoUpdateMode:
   Version` + `UpdateCheckMode: Tags` igas la fdroiddata-robotojn malfermi
   ĝisdatigo-MR-on mem. Reciprokaĵo: **ĉiu** eldonota versio bezonas etikedon
   sur GitHub kun kongrua versionCode — nia `eldonado`-procezo jam tion faras.
   Se post ~1 semajno la nova versio ne aperas en F-Droid, kontrolu ilian
   issue-trackero aŭ demandu min kontroli.
3. **Emblemo**: kiam la paĝo ekzistas, ni aldonu al la README la oficialan
   "Get it on F-Droid"-emblemon (https://f-droid.org/docs/Badges/) kun la
   ligilo https://f-droid.org/packages/dk.nordfalk.esperanto.radio/.
4. **Ekranfotoj/plurlingva priskribo en F-Droid**: eblas aldoni poste per
   aparta MR (fdroiddata havas `metadata/<id>/<lingvo>/…`-strukturon);
   ni jam havas ekranfotojn en `androidApp/src/main/play/listings/`. Malalta
   prioritato — la apo funkcias ankaŭ sen ili.

## 7. Se io fiaskas

- **CI ruĝa en via fork**: kuru al la paŝo 3.3 — la eraroj estas preskaŭ ĉiam
  metadatenaj; korekto venas de ni (unu fonto de vero: nia deponejo).
- **La revizio haltas sen respondo**: skribu ĝentilan rimarkon en la MR-fadeno
  post 2 semajnoj; la fdroiddata-teamo estas malgranda sed respondema.
- **F-Droid-uzantoj raportas konstruo-malsukceson ĉe ĝisdatigo**: tiam plej
  verŝajne io en nia deponejo ŝanĝiĝis (ekz. nova dependaĵo el privata
  deponejo, kromprogramo neatingebla por ili) — tion mi kontrolu antaŭ ĉiu
  eldono (novaj kromprogramoj devas veni el Maven Central / Google Maven).

## Utilaj ligiloj

- Inclusion Policy: https://f-droid.org/docs/Inclusion_Policy/
- Quick Start Guide: https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- Build Metadata Reference: https://f-droid.org/docs/Build_Metadata_Reference/
- Anti-Features: https://f-droid.org/en/docs/Anti-Features/
- CONTRIBUTING de fdroiddata: https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md
- RFP-trackero (nedeviga alternativo): https://gitlab.com/fdroid/rfp
- FAQ "kiam aperas mia apo?": https://gitlab.com/fdroid/wiki/-/wikis/FAQ
