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

### 3.2 Aldonu la metadatenon en via fork — UNU dosiero

**Antaŭ ĉio**: via fork devas esti **PUBLIKA** (la oficiala ŝablono postulas
ĝin; la MR-reviziantoj devas vidi la branĉon): via fork → Settings → General →
Visibility → Project visibility: **Public**.

**Grave**: la CI de fdroiddata validigas ĉiun taskon kontraŭ la kanona formo
(`fdroid rewritemeta` devas esti no-op) — tial la yml enhavas NENIAJN
komentojn, kaj NENIU teksto (summary/priskribo/ŝanĝoprotokolo/bildoj) rajtas
en fdroiddata — la oficiala MR-ŝablono diras „Don't add summary/description/
changelog/images or anything that should be provided in upstream repo":
ĉiuj tekstoj venas aŭtomate el nia Play-listing (Triple-T-strukturo en
`androidApp/src/main/play/`). La MR do ŝanĝu **nur unu dosieron**:

1. **`metadata/dk.nordfalk.esperanto.radio.yml`** —
   https://github.com/nordfalk/EsperantoRadio/raw/master/fdroid/metadata/dk.nordfalk.esperanto.radio.yml
   (kopiu SEN ŝanĝoj — `commit` jam estas la plena haketaĵo, ĉar la ŝablono
   malpermesas etikedon/branon en la commit-kampo)

En via fork (https://gitlab.com/nordfalk/fdroiddata): branĉo
`dk.nordfalk.esperanto.radio` (NE `master`) → anstataŭigu la dosierenhavon;
Commit message ekz. `New app: EsperantoRadio`.

**Fina lini-salo devas resti**: algluo per la retredaktilo ofte perdas la
lastan `\n` — `fdroid rewritemeta` tiam fiaskas per „\ No newline at end of
file" (2026-10-31). Sekurega vojo: elŝutu la dosieron el la raw-ligilo kaj
uzu **Replace**/alŝuton en la redaktilo anstataŭ kopii-alk. **Se lokaj dosierujoj
(`metadata/dk.nordfalk.esperanto.radio/…`) restis de pli fruaj provoj, forigu
ilin** — la ŝanĝoj-tabulo de la MR devas montri NUR la yml-on.

#### Kial la yml aspektas kiel ĝi aspektas (sen komentoj en la dosiero)

- `scanignore: build.gradle.kts` — la radika konstrudosiero deklaras
  `https://jcenter.bintray.com` (necesa por la MALNOVA apo, A1); la
  F-Droid-skanilo rifuzas nekonatajn Maven-deponejojn. La nova apo neniam
  efektive elŝutas de jcenter (ĉiuj ĝiaj dependejoj estas en
  Maven Central/Google), do la skanilo sekure ignorigas tiun dosieron.
- `UpdateCheckName: Ignore` — la aŭtomata nom-serĉanto ne trovas nian
  manifeston (KMP-aranĝo: `src/androidMain/`) kaj prenus la nomon de la
  malnova apo („Esperanto-radio Muzaiko"); sen „Ignore" la ĝisdatig-boto
  ŝanĝus `AutoName` kaj la CI-diferkontrolo ruĝiĝus.
- `gradle: - yes` — gustuma indiko (`yes` = sen gustumoj → `assembleRelease`);
  la valoro devas esti la ĉeno „yes" (en YAML-1.1 la necitita formo estas
  legata kiel buleo de iuj iloj — la kanona formo de `rewritemeta` tamen
  precize tiel skribas ĝin kaj la CI-skemilo legas ĝin kiel ĉenon).
- Neniu `Summary` en la yml kaj neniu `summary.txt` en fdroiddata —
  `tools/make-summary-translatable.py` mem migurus la Summary el la yml kaj
  fiaskigus la CI; resumo, priskribo, ŝanĝoprotokolo (`default.txt`) kaj
  bildoj venas el nia deponejo (Triple-T: `androidApp/src/main/play/
  listings/en-US/`).

### 3.3 Kontrolu la CI-on de via fork

1. En via fork iru al **Build → Pipelines**
   (aŭ "CI/CD → Pipelines" en la malnova fasado).
2. Atendu la pipeline (typical 10–40 minutoj — ĝi vere provas konstrui la apon).
3. **Verda (passed)** → daŭrigu ĉe 3.4. La ses taskoj (schema validation,
   fdroid lint, fdroid rewritemeta, tools check scripts, checkupdates,
   fdroid build) estis ĉiuj reproduktitaj VERDAJ loke per la samaj iloj
   (fdroidserver + check-jsonschema + la tools/-skriptoj de fdroiddata,
   2026-10-31) — se tamen io ruĝiĝas, aldonu la taskan eligilon al la
   sekva laborsesio kun mi.
   **Ruĝa (failed)** → malfermu la malsukcesan taskon, legu la eraron kaj
   aldonu la eligilon al la sekva laborsesio kun mi — la plej oftaj kaŭzoj
   estas YAML-tajperaro aŭ retaj eraretoj; ni riparos la metadatenon kaj vi
   refaros la paŝon 3.2.

### 3.4 Kreu la merge request-on

1. Via fork devas esti **publika** kaj la branĉo ne protektita.
2. Flava rubando en via fork → **Create merge request** (aŭ
   https://gitlab.com/fdroid/fdroiddata/-/merge_requests → New merge request
   → via fork + `dk.nordfalk.esperanto.radio`).
3. **Titolo (la ŝablona formo)**: `New app: EsperantoRadio`
4. Priskribo: **la PLENA 28-peca ŝablono** (uzita por !55129, 2026-10-31 —
   GitLab tiam montras „27 of 28"; ne kondensu la liston — la plej bonaj
   alsendoj gardas ĉiun punkton aparte). Ŝovmetu 2–4 ekranfotojn en la
   unuan sekcion (el `androidApp/src/main/play/listings/en-US/graphics/
   phoneScreenshots/`). Forigu la `Closes rfp#…`-linion se la ŝablono
   enhavas ĝin.

   ```markdown
## Abstract for new app inclusion

### Please introduce the functions and usage of your app in summary, and also attach some screenshots.

EsperantoRadio unifies all scattered Esperanto radio stations and podcasts (27 sources: 24-hour web radio Muzaiko, Vatican Radio in Esperanto since 1977, Varsovia Vento, Polish Radio, Kernpunkto and many more) into a single app: channel overview, live streams, podcasts, downloads, favourites, search and an alarm clock. Written in Kotlin with Compose Multiplatform (Android, Desktop, Web targets), GPL-3.0-or-later; a maintained fork of the Danish "DR Radio" app, rewritten from scratch for the Esperanto audience.

### Please elaborate why you wrote this app. Please highlight the unique functions it provides over other similar apps in F-Droid.

Esperanto radio content is spread over many small, half-abandoned feeds; this is the only app that aggregates them all in one place, with per-source knowledge of stream quirks built in. There is no other Esperanto-focused radio/podcast app in F-Droid, so it provides unique value; it is actively developed with an established user base on Google Play (Quality point 6 exception: proven users).

## Checklist

### Policy

* [x] The app complies with the [inclusion policy](https://f-droid.org/docs/Inclusion_Policy). _Pay special attention to Quality point 6._
* [x] The original app author has been notified (and does not oppose the inclusion). If you are not the author, please paste the link of the reply from the author. — I am the author.
* [x] The upstream app source code repo contains the app metadata in a [Fastlane](https://gitlab.com/snippets/1895688) or [Triple-T](https://gitlab.com/snippets/1901490) folder structure. The summary and description must be included and images, icon, and changelog should also be provided for better user experience. The `en-US` locale must be included. — Triple-T at `androidApp/src/main/play/`: en-US title, summary, description, screenshots, icon and changelog (`release-notes/en-US/default.txt`).

### Docs

* [x] Please read [the guide](https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md) first if this is your first contribution.
* [x] Please make sure your metadata follows the best practice in [our templates](https://gitlab.com/fdroid/fdroiddata/tree/master/templates).
* [x] Please read the [Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/) and make sure your metadata is valid.
* [x] Please read the [Quick Start Guide](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/).

### Merge Request Setup

* [x] The title of this merge request should follow "New app: app name" format.
* [x] Please make sure your fdroiddata fork is public and your branch is not protected. See <https://docs.gitlab.com/user/project/repository/branches/protected/>.
* [x] Please read [our Git guide](https://gitlab.com/fdroid/wiki/-/wikis/Tips-for-fdroiddata-contributors/Git-Usage) if you don't know how to rebase your branch. Don't rebase your branch if there is no conflict.
* [x] All related [fdroiddata](https://gitlab.com/fdroid/fdroiddata/issues) and [RFP issues](https://gitlab.com/fdroid/rfp/issues) have been referenced in this merge request — none exist for this app.
* [x] Please only submit one app in one MR.

### Metadata

* [x] Metadata must be put in `metadata/<applicationId>.yml`.
* [x] Metadata must be a valid YAML file.
* [x] Metadata must use LF as line ending.
* [x] Don't add summary/description/changelog/images or anything that should be provided in upstream repo. Please check the Changes tab to make sure there is no other unrelated files added in the MR. — the Changes tab shows only the yml.
* [x] Releases are tagged and auto update is enabled unless there is a special reason. — `v3.1.1`; `AutoUpdateMode: Version` + `UpdateCheckMode: Tags`.
* [x] There is an issue tracker and contact info of the author so that we can report bugs and contact the author.
* [x] An AuthorName must be added. It doesn't need to be the real name.
* [x] External repos are added as git submodules instead of srclibs. You can update git submodules without opening an MR in this repo and the submodule is covered by our scanner. — none used.
* [ ] Enable [Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds/). We'll use your signature for improved security/reliability, also allowing users to switch between different channels. Do note that if you don't enable reproducible build then the apk will be signed with our key so you can't enable it later. If you can't enable this, please add the reasons here. — Not currently bit-identical; we accept the F-Droid signing key for this app and understand it cannot be switched later without a reinstall.
* [x] Setup abi split if the APK is large and the splitted ones can be much smaller. — APK ≈ 23 MB; not large, no split needed.
* [x] Only the latest versions should be kept in the metadata before it's merged. If you update the metadata, please replace the old versions with the new ones.
* [x] Don't add any disabled versions in the metadata.
* [x] The `commit` field should be the full hash. Please don't use tag or branch in commit.

### Pipeline

* [x] All pipelines should pass.
* [x] All warnings and errors in the Reports tab should be fixed or explained.
* [x] F-Droid CI runners are under GitLab's FOSS program, so there's no need for you to pay for any CI time. If Gitlab starts asking for phone numbers or credit cards don't submit anything, just leave a note in the MR so we know we need to trigger the CI.

/label ~"New App"
   ```

5. **Create merge request** → atendas la revizion (tigoj–semajnoj); respondu
   rapide al komentoj.

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
2. **Estontaj versioj**: nenion plian vi devas fari ĉe F-Droid.
   Noto pri 3.1.1: F-Droid konstruas versionCode **253** (la etikedo), dum
   la Play-eldono havas **254** — 253 estis konsumita en Play de interna
   testo (3.1.1-beta29okt, PR #144). Tio estas normala kaj senproblema:
   la F-Droid-versionumejo estas sendependa, kaj la etikedo kongruas kun
   la metadateno (253). Ekde la sekva versio (v3.1.2+) la kodoj re kongruas. Nia eldona
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

Konataj jam solvitaj fiaskoj (2026-10-31, la tuta.taga maratono — ĉiuj
kaŭzoj kaj kialoj staras ĉe la koncernaj kampoj supre):
- „Categories 'Audio' is not valid" → nur nomoj el `schemas/metadata.json`
- „AutoUpdateMode … does not match" → nuda `Version`
- lokalizitaj kampoj en la yml → malpermesataj; tekstoj el la Triple-T
- rewritemeta-dosiero → forigi ĉiujn `#`-komentojn; fina lini-salo ĉe EOF
- „Unused file at …/.gitkeep" → forigu la `.gitkeep`-ojn (neniam kreu
  malplenajn dosierujojn per ili)
- „Found unknown maven repo 'https://jcenter.bintray.com'" → `scanignore:
  build.gradle.kts` (la malnova apo bezonas jcenter; la nova neniam ĝin uzas)
- `AutoName` ŝanĝiĝas per si mem („Esperanto-radio Muzaiko") → `UpdateCheckName:
  Ignore` (la KMP-manifesto ne troveblas de la nom-serĉanto)
- „SigningConfig with name 'release' not found" → `findByName` (fdroidserver
  forigas la signingConfigs-blokon antaŭ konstrui — `remove_signing_keys`)

Pri etikedoj: ekstera kontribuinto NE povas aldoni etikedojn (la
`/label ~"New App"`-komando el la ŝablono funkcias nur por projekt-membroj);
la prizorgantoj aldonas `New App` kaj `review-requested` dum la triaĝo —
neniu ago bezonata (konfirmita ĉe la najbaraj MR-oj, 2026-10-31).

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
