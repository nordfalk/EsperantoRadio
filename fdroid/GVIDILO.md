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
Commit message ekz. `New app: EsperantoRadio`. **Se lokaj dosierujoj
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

1. Via fork devas esti **publika** (vidu 3.2) kaj la branĉo ne protektita.
2. Flava rubando supre en via fork → **Create merge request** (aŭ
   https://gitlab.com/fdroid/fdroiddata/-/merge_requests → New merge request
   → via fork + `dk.nordfalk.esperanto.radio`).
3. **Titolo (laŭ la ŝablona formo)**: `New app: EsperantoRadio`
4. Priskribo: kopiu la PLENIGITAN ŜABLONON sube (ĝi jam respondas ĉiun
   kontrolkeston) kaj **alkroĉu 2–3 ekranfotojn** (alkroĉu per ŝovmetado en la
   priskribo-kampon; la bildoj kuŝas en
   `androidApp/src/main/play/listings/en-US/graphics/phoneScreenshots/`):

   ```markdown
   ## Abstract for new app inclusion

   EsperantoRadio unifies all scattered Esperanto radio stations and podcasts
   (27 sources: 24-hour web radio Muzaiko, Vatican Radio in Esperanto since
   1977, Varsovia Vento, Polish Radio, Kernpunkto and many more) into a single
   app: channel overview, live streams, podcasts, downloads, favourites,
   search and an alarm clock. Written in Kotlin with Compose Multiplatform
   (Android, Desktop, Web targets), GPL-3.0-or-later; a maintained fork of the
   Danish "DR Radio" app, rewritten from scratch for the Esperanto audience.

   Why: Esperanto radio content is spread over many small, half-abandoned
   feeds; this is the only app that aggregates them all in one place. There is
   no other Esperanto-focused radio/podcast app in F-Droid, so it provides
   unique value (Quality point 5/6: actively developed, established user base
   on Google Play).

   ### Policy
   * [x] The app complies with the inclusion policy (GPL-3.0-or-later, source
     on GitHub, no binaries, no proprietary dependencies, unique app id).
   * [x] I am the author of the app.
   * [x] The upstream repo contains the metadata in the Triple-T structure
     (`androidApp/src/main/play/`): en-US summary + description + images +
     icon + changelog (`release-notes/en-US/default.txt`).

   ### Docs
   * [x] Read the guide; metadata follows the templates and the Build
     Metadata Reference; validated locally with fdroidserver 2.4.2
     (rewritemeta is a no-op, check-jsonschema passes).

   ### Merge Request Setup
   * [x] Title follows "New app: app name"; fork is public; branch is not
     protected; no rebase needed (no conflict); no related fdroiddata/RFP
     issues exist; only one app in this MR.

   ### Metadata
   * [x] `metadata/dk.nordfalk.esperanto.radio.yml`, valid YAML, LF endings.
   * [x] No summary/description/changelog/images in fdroiddata — all texts
     come from the upstream Triple-T structure; the Changes tab shows only
     the yml.
   * [x] Releases are tagged (`v3.1.1`) and auto-update is enabled
     (`AutoUpdateMode: Version` + `UpdateCheckMode: Tags`).
   * [x] IssueTracker and AuthorEmail/AuthorName are set.
   * [x] No srclibs/external repos.
   * [ ] Reproducible Builds: **not enabled** — the build is not currently
     bit-identical; we accept the F-Droid signing key for this app (note: we
     understand this cannot be changed later without a reinstall for users).
   * [x] No disabled versions; only the latest build is kept.
   * [x] `commit` is the full hash (10e3f13513ec00231e30148f4ee42cec6cb00c2a).

   ### Pipeline
   * [x] All pipelines pass (including fdroid build — the APK builds unsigned
     from the tag and is signed by F-Droid).
   * [x] Reports tab: no unexplained errors.
   ```

   Rimarkoj pri la fakoj, se la reviziantoj demandas:
   - la `scanignore: build.gradle.kts` kial — la radika konstrudosiero
     deklaras la mortintan `jcenter.bintray.com` (necesa por la hereda
     GPL-apo en la sama deponejo; la konstruata modulo neniam elŝutas de ĝi);
   - la `UpdateCheckName: Ignore` kial — la aŭtonomo-parsilo ne trovas la
     KMP-manifeston (`src/androidMain/`) kaj prenus la nomon de la hereda apo;
   - Sentry (erar-raportado) estas **opt-in** en konstruoj sen nia subskribo
     (ĉi tiuj, kiujn F-Droid konstruas): unufoja konsento-demando je la unua
     starto + ŝaltilo en Agordoj → Privateco — do neniu Tracking-kontraŭeco.

5. Forigu la `Closes rfp#…`-linion kaj la `<!--…-->`-komentojn; tenu la
   `/label ~"New App"`-linion (ĝi mem metas la etikedon).
6. **Create merge request** → atentu la revizion (tigoj–semajnoj; respondu
   rapide al komentoj).

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
