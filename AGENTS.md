# AGENTS.md — Gvidlinioj por agentoj en ĉi tiu deponejo

> **Nuna jaro: 2026 (oktobro).** Ĉiuj referencoj al "aktuala" datas
> rilatas al 2026 se ne alie specifite.

> Lingvo: Preferu Esperanton por ĉiu dokumentado, komentoj, identigiloj kaj
> komuniko kun la uzanto. La nova kodo uzas Esperanton anstataŭ la dana de la
> malnova kodo. La malnova kodo (`dk.dr.radio.*`) uzas dano/esperanto-miksaĵon
> — ne renomigu ĝin, sed en nova kodo ĉiam preferu Esperanton.

## Kio estas ĉi tiu projekto

**EsperantoRadio** estas Android-apo (kaj estonte plursistema apo) kiu kunigas 26
disajn, duonmortajn Esperanto-radiajn/podkastajn fontojn en unu unuecan sperton:
kanal superrigardo, livestreno, podkastoj, elŝutoj, plej ŝatataj, serĉo, vekhorloĝo.

Ĝi estas fork de la dana **DR Radio**-apo (GPL), prizorgata de Jacob Nordfalk.
La plej valora parto ne estas la ludilo aŭ UI, sed la **scio pri la fontoj** — kiuj
kanaloj ekzistas, kiel iliaj fluoj aspektas, kaj kiel eltiri rektan MP3-URL el ĉiu.

## Stato de la projekto (ĝisdatigita 2026-10-14)

La nova KMP-apo (Android + Desktop + Web/wasmJs) estas eldonita kaj aktiva en
Google Play (3.0.3/249, 2026-10-13). Ĉiuj fazoj de la plano estas faritaj krom:
iOS-ludado (bezonas AVPlayer + Xcode-projekton), eldono de Desktop + Web/wasmJs, hejmekrana widget, Chromecast,
parolsintezo kaj la podkasta arkiv-servilo (`docs/nova/06_servilo_arkivo.md`).
Web estas limigita de CORS (nur la anchor.fm-podkastoj ŝargeblas sen servilo).
La historia fazo-tabelo kaj la detala listo "kio funkcias / kio ne funkcias"
staras en [`NOTOJ.md`](NOTOJ.md) → "Projekta stato kaj fazoj".

## Granda plano

Rekrei la apot en **Compose Multiplatform** (Android + iOS + Desktop + Web/Wasm), plus
konstrui memstaran **servilon** kiu funkcias kiel arkivo de Esperanto-podkastoj.

- **Malnova apo** (kodo en `malnova/app/`, `malnova/parse/`, `malnova/data/`): priskribita en `docs/malnova/`.
- **Nova apo** (en la radiko): priskribita en `docs/nova/` (komenco: [`docs/nova/INDEKSO.md`](docs/nova/INDEKSO.md)). La nova Compose Multiplatform-aposieraĵo (`androidApp/`, `desktopApp/`, `webApp/`, `shared/`) vivas en la radiko, apud `malnova/`, laŭ la oficiala KMP-ŝablono (https://kotlinlang.org/docs/multiplatform/compose-multiplatform-create-first-app.html). `iosApp/` (Xcode-projekto) kaj `server/` ankoraŭ ne estas kreitaj — la iOS-kodo jam ekzistas en `shared/src/iosMain/`, sed la Xcode-projekto kaj la podkasta arkiv-servilo restas estontaj.

## Dosierujo-structuro

La kompleta arbo estas en [`NOTOJ.md`](NOTOJ.md) → "Deponeja koda mapo". Esencoj:
`malnova/` (la historia apo — ne tuŝebla), `androidApp/` + `desktopApp/` +
`webApp/` + `shared/` (la nova KMP-apo), `docs/malnova/` + `docs/nova/`
(dokumentaro), `CHANGELOG.md` + `ŜANĜOJ.md` + `NOTOJ.md` + `FAROTA.md` +
`AGENTS.md` (la protokolaro), `.vibe/skills/` (proceduraj skill-oj:
`eldonado`, `sentry-esplorado`, `konstruado-kaj-testado`, `kanalkonfiguro`).

## Plej gravaj reguloj por agentoj

> La reguloj portas stabilajn nomojn (**A1–A12**); same la git-laborfluaj reguloj
> (**L1–L8**) kaj la sep parsreguloj (**P6.1–P6.7**, vidu `docs/malnova/03`). La FAROTA-eroj
> uzas la literojn C/G/K/X. Referencu regulojn ĉiam per tiuj nomoj, ne per nura numero.

- **A1 — Ne tuŝu la malnovan kodon** krom se eksplice petite. Ĝi estas historika.
  La celo estas rekreado, ne riparado.
- **A2 — La parsado estas la kerno.** Antaŭ ol ŝanĝi ion pri datumoj, legu
  `docs/nova/04_parsado_kaj_arkivo.md` kaj `docs/malnova/03_parsado_kaj_fontoj.md`.
  La sep parsreguloj kaj la skip-listo devas esti konservitaj.
- **A3 — Testu la daten tavolon kontraŭ golden fixtures**, sen reto. La dosierujo
  `RssArkivServer-filcache/` enhavas realajn kaŝenitajn fluojn (NE en la repo — .gitignore-ita).
  Uzu ilin kiel determinismajn test-enirojn kopiantaj la bezonatajn fiksaĵojn al
  `shared/src/commonTest/resources/feeds/` (5 jam tie: kernpunkto, peranto, varsoviavento, vinilkosmo, lamalfamuloj). Vidu `docs/nova/04_parsado_kaj_arkivo.md`.
  La testoj jam kopiis 3 fiksaĵojn al `shared/src/commonTest/resources/feeds/`.
- **A4 — Unu fonto-eraro ne devas panei la apon.** Se unu kanal-fluo mortas, la aliaj
  devas daŭre funkcii. Toleremeco al putrantaj fontoj estas deziro.
- **A5 — Konservu la kanalkonfiguron** (`esperantoradio_kanaloj_v9.json`). Ĝi estas
  daten-movita konfiguro, ne malmola kodo. Per-kanalaj apartaĵoj devas esti
  en agordo, ne en logiko.
- **A6 — GPL-licenco.** Ĉiu derivaĵo devas resti GPL.
- **A7 — Ĉiam pripensu ĉu indas fari teston.** Por ĉiu nova funkcio, modelo, aŭ
  regulo, demandu: ĉu tio estas testebla? Se jes, skribu teston. Tamen ne
  faru trivialajn testojn; preferu UI-testojn kiuj testas plurajn tavolojn.
  Ne nur skribu kodon — ankaŭ validigu ĝin. Se estas UI-ŝanĝo, antaŭ ol fari 
  commit, kontrolu ke navigado funkcias, kaj ke la enhavo estas videbla.
- **A8 — Neniam engluti escepton silente.** Ĉiu `catch` bloko devas protokoli la
  eraron per `loge(tag, msg, e)` aŭ `logw(tag, msg, e)` (kun stacktrace).
  Eĉ se la eraro estas atendata aŭ negrava, protokolu ĝin per `logd`.
- **A9 — Uzu `squash`-merge por ĉiuj PR-oj.** Kiam vi kunfandas PR-on al master,
  uzigu `gh pr merge --squash`. Tiel master ricevas precize 1 commit po PR.
  La commit-mesaĝo estu la titolo de la PR + ligo al la PR
- **A10 — Ĝisdatigu `CHANGELOG.md` en ĉiu PR kun uzantvidebla ŝanĝo.** Aldonu
  **unu linion** al la `## [Neeldonita]`-sekcio **ene de la sama PR**: konciza
  priskribo sen teknikaj detaloj + ligilo al la PR, ekz.:
  `- Vekhorloĝo ludas ankaŭ podkastojn (https://github.com/nordfalk/EsperantoRadio/pull/123)`.
  Pura interno (testoj, refaktorado, dokumentaro, CI) ne bezonas eniron; ankaŭ
  bagatelaj uzantvidaj ŝanĝoj (ekz. minora vortumo de buton-teksto aŭ klaraigo)
  ne indas eniron (2026-10-20, decido de la posedanto). Se la CI-kontrolo
  erare postulas ĝin, aldonu la etikedon `preterlasu-changelog` al
  la PR. Detalojn vidu en la sekcio "Ŝanĝoprotokolo (CHANGELOG.md)".
- **A11 — Aŭtomate aldonu viajn lecionojn.** Post ĉiu laboro — precipe post
   eraro, incidento aŭ neatendita rezulto — skribu koncizan lecionon (kun
   dato): aktivaj reguloj en "Aktivaj reguloj kaj rimarkoj" (sube), procedura
   scio en la koncernan skill-on (`.vibe/skills/`), historiaj detaloj en
   NOTOJ.md aŭ ŜANĜOJ.md. Celo: ne ripeti la saman eraron.
- **A12 — Neniam legu, modifu aŭ commit `secrets.properties` aŭ `*.keystore` aŭ `*.jks`.**
   Ĉi tiuj dosieroj enhavas sekretajn subskribajn informojn kaj neniam devas
   esti en la git-deponejo aŭ legataj de iu agento. Ilia ekzisto estas ŝirmita
   de `.gitignore`. Se vi trovas ili en la deponejo, tuj forigu ilin kun `git rm`
   kaj informu la deponejan posedanton.

## Git-laborfluo

- **L1 —** Kreu branĉon de `master` (aŭ de la plej nova feature-branĉo se temas pri
  plia kommito en ekzistanta PR).
  **Branĉnomo**: `MMdd-nomo` — ekz. `1009-hejmo-luditaj-kio-novas`.
  La `MMdd`-prefikso estas la nuna dato (ekz. 1009 = 9-a de oktobro).
  Uzu la saman nomon loke kaj sur GitHub.
- **L2 —** Faru ŝanĝojn, skribu testojn, kompilu kaj rulu testojn.
- **L3 —** Commit, push, kreu PR kun `gh pr create`.
- **L4 —** Kiam la PR estas aprobita, kunfandu per `gh pr merge --squash --delete-branch`.
  Tio kreas 1 commit sur master kun la PR-titolo kaj ligilo al la PR.
- **L5 —** Antaŭ commit kontrolu ĉu uzanto ŝanĝis aferojn rilate al via laboro, se jes
  kaj ŝajnas esti en ordo, aldonu ankaŭ tion al la commit
- **L6 — Ne commitu antaŭ ol la uzanto aprobis la ŝanĝojn.** La rajtigo por
  unu commit/push (ekz. "faru PR") validas nur por tiu unu fojo — ĝi ne
  ĝeneraligas al sekvaj ŝanĝoj sur la sama branĉo.
  **KUNFANDO (`gh pr merge`) BEZONAS APARTAN, EKPLICITAN aprobon** (ekz.
  "kunfandu"). Rajtigo por commiti/puŝi/krei PR-on NE inkluzivas la
  kunfandon. (2026-10-19: PR #120 estis kunfandita post kiam la uzanto
  aprobis nur "commitos, puŝos kaj kreos la PR-on" — tio estis eraro;
  lasu la PR-on malfermita kaj atendu "kunfandu".)
- **L7 — La kapa branĉo de PR ne nepre havas la nomon de via loka branĉo** — kontrolu
  `gh pr view N --json headRefName` antaŭ ol puŝi. Puŝi alinomitan lokan branĉon
  (ekz. `pr-85`) kreus NOVAN foran branĉon anstataŭ ĝisdatigi la PR-on.
  (2026-10-11: PR #85 naskas el `hejmo-luditaj-kio-novas`, ne `pr-85`.)
- **L8 — Por ĝisdatigi malfermitan PR-on de master, uzu merge, ne rebazo** — rebazo
  reskribas la historion kaj postulas force-push; `git merge origin/master` en la
  PR-branĉon ebligas normalan antaŭen-iran puŝon. (2026-10-11: tiel PR #85
  ricevis master-ĝisdatigojn.)

## Ŝanĝoprotokolo (CHANGELOG.md)

[`CHANGELOG.md`](CHANGELOG.md) estas konciza, uzantvida resumo de ĉiuj
rimarkindaj ŝanĝoj. La formato estas [Keep a Changelog](https://keepachangelog.com/1.1.0/)
kun esperantaj kategorioj (**Aldonita**, **Ŝanĝita**, **Riparita**, **Forigita**,
**Sekureco**). Ĉiu eniro estas **unu linio**: konciza priskribo sen teknikaj
detaloj + ligilo al la PR (regulo A10). Rilato al [`ŜANĜOJ.md`](ŜANĜOJ.md):
CHANGELOG = "kio ŝanĝiĝis por la uzanto", ŜANĜOJ = detala teknika
labortaglibro (problem-analizo, kialoj, kontrolrezultoj). La samo ne devas
aperi duoble — CHANGELOG donas la koncizan fakton, ŜANĜOJ la kialojn.

**Laborfluo (kiu tenas ĝin ĝisdatigata):**

1. Ĉiu PR, kiu ŝanĝas uzantvideblan konduton, aldonas sian eniron al
   `## [Neeldonita]` en la sama PR (regulo A10).
2. La CI (`.github/workflows/changelog-kontrolo.yaml`) rifuzas PR-ojn, kiuj
   ŝanĝas la ĉefan fontkodon (`*Main`-dosierujoj) sen ŝanĝo de `CHANGELOG.md`.
   Preterpaso: PR-etikedo `preterlasu-changelog` (puraj internaj ŝanĝoj).
3. Eldono de nova versio: sekvu la skill-on **`eldonado`** (`.vibe/skills/eldonado/`)
   — `apoversio` + versionCode-baskulo + CHANGELOG-sekcio + git-etikedo +
   `internal.txt`-notoj + `publishReleaseApk` de la LOKA maŝino (la CI-eldonado
   per GitHub-sekretoj ne uzeblas) + permana promocio al produktado. Kritikaj
   faktoj: versionCode ne re-uzeblas (Play-API-rifuzo); GPP legas la notojn laŭ
   la TRAKO (`internal.txt`), neniam laŭ versionName; APK, ne AAB (la apo ne
   estas en Play App Signing); la signado legas `secrets.properties` (ne tuŝu).

## Aktivaj reguloj kaj rimarkoj

> La procedura scio vivas en la skill-oj de `.vibe/skills/` — ŝargu la koncernan:
> **`eldonado`** (versio-eldonado al Play), **`sentry-esplorado`** (legi/analizi
> Sentry-n), **`konstruado-kaj-testado`** (komandoj, JDK, platform-limigoj, emulilo),
> **`kanalkonfiguro`** (JSONC, regex-minacoj, novaj kanaloj)
> (kanonaj terminoj kaj supersigno-skriboj). Historiaj lecionoj kaj
> arkitekturaj resumoj staras en [`NOTOJ.md`](NOTOJ.md); la okazhistorio en
> [`ŜANĜOJ.md`](ŜANĜOJ.md).

- **Konstruu post kunfando de kod-ŝanĝoj.** La CI nur kontrolas CHANGELOG-ŝanĝojn —
  post ĉiu merge rulu `:shared:desktopTest` + `:androidApp:assembleDebug`. Por PR-oj
  nur kun tekstaj/agentaj dosieroj (AGENTS.md, `.vibe/skills/`, `docs/`, NOTOJ ktp.)
  tio ne necesas — nek loka konstruo nek atendo de la `testoj`-CI; merĝu tuj
  (2026-10-17, regulo de la uzanto).
- **Neniam enmetu kodo-pecojn (backtick-ojn) en komandojn kun duoblaj citiloj** —
  precipe `gh pr create --body "..."`: bash plenumas ilin kiel komand-anstataŭigon
  (2026-10-08, PR #80: akcidentaj git-tag/push/gradle-plenumoj). Uzu `--body-file`.
- **Post mergo de master, reviziu la novajn invariantojn** en la kunfanditaj
  dosieroj — nova metodo povas malobservi novaĵon de master (2026-10-11: `markiLudita`
  skribis sen la intertempe postulita `mutex.withLock`).
- **Kiam `edit` fiaskas per "string not found" sen evidenta kialo**, kontrolu la
  ĝustan literumadon de identigiloj per `grep`/`od` antaŭ ol reprovi — la okulo
  ofte "korektas" similan nomon (`estasEneDe` ≠ `estaEneDe`). Tio validas ankaŭ
  por ĉenoj: supersignaj literoj kaj SPACETOj glitas preter la okulo
  (2026-10-17: `"Plej ŝatataj"` ≠ `"Plejŝatataj"` — spaceto! — igis instrumentitan
  teston atendi 5 s senfine). Komparu laŭbite (`od -c`, `python3`-asserto), ne
  per la okulo; en python-ĉenoj uzu `\u015d`-eskapoliterojn, ne `\xc5\x9d`.
- **Esperanto-ortografio: nur veraj supersignoj** (renomo 2026-10-24): ĉiuj identigiloj,
  klasoj kaj dosier-nomoj uzas `ŝ ĝ ĉ ĵ ŭ` (`ŝargiElsendojnPorKanal`, `ElŝutStato.kt`,
  `SerĉoEkrano`, `ApajNovaĵojDeponejo`) — la x/h-sistemo estas forigita el la nova kodo.
  **ESCEPTOJ (persistigitaj valoroj — ne renomu)**: agord-ŝlosiloj `"auxtomata_daurigo"`,
  `"kasxitaj_avertoj"`, `"apaj_novajxoj_hesh"` (valoroj de `SettingsKeys`, ne la konstantaj
  nomoj), la kaŝmemora dosierujo `"elsendoj_kasho"`, kaj la `@SerialName`-ŝlosiloj en
  `Modeloj.kt` (`retposhto`, `defaltSxatata`, `dauro`, `dauroMs`, `retpaghoUrl`) — ili
  tenas kongruon kun ekzistantaj uzant-datumoj. Antaŭ ol skribi, **grep-u kaj kopiu**;
  bibliotekaj nomoj restas anglaj (`PullToRefreshBox`, `isRefreshing`), kaj `fluxo`,
  `cx`/`cy`-koordinatoj, `AUX`-regexo kaj kanal-slugs ne estas x-sistemo.
  **Legi historian materialon** (malnova apo `malnova/`, protokoloj antaŭ 2026-10-24,
  persistaj valoroj): x-sistemo = bazlitero+x (`sx`=ŝ, `cx`=ĉ, `gx`=ĝ, `jx`=ĵ, `ux`=ŭ);
  h-sistemo mikse: `gh`=ĝ, `sh`=ŝ (`poshto`=poŝto, `malshalita`=malŝalita), `kash`=kaŝ;
  la historia `hesh` estas la angla „hash" — Esperante **haketo** (ago) / **haketaĵo**
  (rezulto; la konstanto nun: `APAJ_NOVAĴOJ_HAKETAĴO`). Nova kodo neniam enkonduku
  x/h-sistemajn vortojn.
- **`LaunchedEffect(Unit) { vm.ŝargi() }` lanĉas la ŝargadon je ĈIU ekran-eniro** (2026-10-20):
  navigado rekomponas la ekranon, do reveno al Hejmo/Kanalaro reŝargis ĉion — ĝis 7 plenaj
  ŝarĝoj en 90 sekundoj ĉe unu uzanto (Sentry: 24 ŝarĝoj vs 6 apomfermoj). Ĉiu ŝargado,
  kiun ekrano ekigas, devas iri tra `ŜargaKontrolilo` (dedupigo + TTL 5 min;
  `ui/ŜargaKontrolilo.kt`) — kaj `leguKaŝitajnElsendojn` ne re-parsu kiam la en-memora
  kaŝo jam havas la kanalon.
- **Komponantoj de `ModalNavigationDrawer` restas en la semantika arbo ankaŭ
  kiam la tirkesto estas fermita** (2026-10-17) — `onNodeWithText("Agordoj")`
  trovas 2 nodojn (ekrana titolo + tirkest-ero). Asertu laŭ enhava teksto
  (ekz. agordo-ŝaltilo), ne laŭ ekrana titolo; por la tirkest-butono uzu
  unikan `contentDescription` ("Naviga menuo" — ne "Menuo", kiu ekzistas sur
  ĉiu karto).
- **`gh pr edit` ne funkcias en ĉi tiu deponejo** (GraphQL projectCards-eraro) —
  uzu la REST API: `gh api -X PATCH repos/nordfalk/EsperantoRadio/pulls/N -f body="$(cat dosiero)"`.
- **Afiŝitan PR-recenzon oni ne povas redakti per REST** (2026-10-20, PR #126):
  `PATCH …/pulls/N/reviews/{id}` redonas 404 kaj `DELETE …` 422 ("non-pending") —
  korektu per GraphQL `updatePullRequestReview` (mutacio kun `pullRequestReviewId`
  = `node_id` de la recenzo). Aldone: kontrolu la AFIŜITAN tekston post la
  afiŝo (en la unua recenzo de #126 glitis ne-esperanta artefakto en la teksto).
- **Commitigu nur viajn proprajn dosierojn** (`git add <dosiero>`, ne blinda
  `git add -A`) kiam la laborarbo povas enhavi nekomititajn ŝanĝojn de la uzanto.
- **Arkitekturaj bazlinioj**: neniu DI-framintervalo (permana injektado en
  konstruktiloj); neniu datumbazo (la kliento kaŝenas servil-respondojn kiel
  dosierojn).
- **Loka `remote.origin.fetch` estas limigita al master** (2026-10-14, revizio de
  PR #109): `git fetch origin` (ankaŭ kun `--prune`) NE alportas PR-branĉojn —
  la refspec estas `+refs/heads/master:refs/remotes/origin/master`. Por revizii
  PR-on, alportu la kap-branĉon eksplicite:
  `git fetch origin <kap-branĉo> && git switch -c <loka-nomo> FETCH_HEAD`.
  Flankefiko (2026-10-19, PR #122): `gh pr create` tiam ĉiam eraras per
  "you must first push the current branch" — aldonu `--head <branĉnomo>`.
- **Dependabot povas enmeti pliajn komitojn al master dum revizio de PR**
  (2026-10-14, PR #109): dum la revizio envenis #99 kaj #101 — kontrolu
  `git fetch origin master` antaŭ la fina puŝo kaj re-merge se necese
  (`git merge --no-ff origin/master` en la PR-branĉo; konfliktoj en la
  dokumentaj dosieroj (CHANGELOG/ŜANĜOJ/AGENTS) estas kutimaj — solvu ilin
  konservante ambaŭ flankojn).
- **`mergeStateStatus: UNSTABLE`** (2026-10-14, PR #109): signifas "mergeable,
  sed la status-kontroloj ankoraŭ kuras" — gravas nur la fina konkludo de la
  kontroloj (aŭ atendu `CLEAN` antaŭ ol merĝi).
- **Dialogoj en Compose-testoj (EkranfotoTesto)** (2026-10-17): `AlertDialog`/`Dialog`
  kreas APARTAN komponan radikon — `onRoot()` tiam fiaskas per "Expected exactly 1
  node but found 2". Kaptu la lastan radikon per
  `onAllNodes(SemanticsMatcher("estasRadiko") { it.isRoot })` kaj
  `.get(indekso)` sur `SemanticsNodeInteractionCollection`. ATENTU: `isRoot` estas
  ECO (`val`), ne funkcio — `it.isRoot()` donas la konfuzan eraron "Cannot infer
  type for T/R … DeepRecursiveFunction.invoke". Krome `onNodeWithText` serĉas en
  ĈIUJ radikoj: buton-teksto kiu ekzistas kaj en la dialogo kaj en la ekrano
  malantaŭ ĝi (ekz. "Nuligi") ne estas unika — fermu la dialogon per teksto kiu
  aperas nur en ĝi (ekz. "Bone").
- **Scaffold-enhavo DEVAS ricevi la `padding`-on el la content-lambda**
  (2026-10-17, PR #114): forgeso ne fiaskas en testoj (la nodo restas
  "displayed") — la TopAppBar simple kovras la unuajn liniojn de la enhavo
  (en AlarmoRedaktilo la "Tempo"-etikedo malaperis malantaŭ la supra breto).
  Sub-komponaĵoj de la enhavo bezonas `PaddingValues`-parametron; ĉiuj aliaj
  ekranoj jam aplikas la padding-on en ĉiuj siaj branĉoj.
- **Dependabot-versi-PR-oj povas esti interdependaj kaj konfliktaj** (2026-10-13,
  revizio de #99/#101/#102): la du Kotlin-bumpoj tuŝas apudajn liniojn de
  `build.gradle.kts` kaj konfliktas inter si — solvu per merge de master en la
  PR-branĉon, ĉiuj kvar kromprogramoj je la sama versio. Coil 3.6.3 (#102) estis
  blokita dufoje: (1) ĝia transira `kotlin-stdlib 2.4.10` havas metadatenon 2.4.0,
  nelegeblan de kompililo 2.2.20 (maks 2.3.0); (2) eĉ sub Kotlin 2.4.21 ĝi postulas
  `compileSdk 37` + AGP-nivelon kiu subenas ĝin (`checkDebugAarMetadata`; nuna AGP
  8.9.1 maks rekomendatas 36). Kontrolu kaj Kotlin-metadatenajn kaj
  compileSdk/AGP-postulojn antaŭ ol kunfandi versi-bumpon.
- **Plena ĝisdatigo de la ilaro (Gradle 9 + AGP 9) eblas kaj estis provita**
  (2026-10-17, PR #117; ĉiuj celoj verdaj: desktopTest, androidApp assembleDebug,
  wasmJs-kompilo, :app:assembleDebug; versiojn vidu en `gradle/libs.versions.toml`).
  Necesaj ŝanĝoj: `jcenter()` foriĝis en Gradle 9 (→ `maven { url = ... }`);
  AGP 9 forigis la agord-flagojn `android.defaults.buildfeatures.buildconfig`,
  `android.enableJetifier`, `android.enableR8.fullMode` el `gradle.properties`;
  la malnova apo bezonas `buildFeatures { buildConfig true }` kaj
  `proguard-android-optimize.txt` (ne `-android.txt`) kaj restas kun
  `android.nonTransitiveRClass=false` (ĝi estos forigita en AGP 10);
  KMP+`androidTarget()` bezonas `android.newDsl=false` kaj
  `android.builtInKotlin=false` (pretervojoj ĝis migrado al
  `com.android.kotlin.multiplatform.library` — vidu https://kotl.in/gradle/agp-new-kmp);
  GPP devas esti la 4.x-linio (3.x ne subtenas AGP 9); platformon la novan
  compileSdk AGP mem elŝutas se la permesiloj akceptitaj. La material3-versio
  devas kongrui kun CMP (parigo el la CMP-release-notoj, ne la sama versinombro).
  La nuna nav3-versio bone kompilas ankaŭ sur wasmJs (la klib-ABI-timo ne
  konfirmiĝis), kaj la `savedstate-ktx`-devigo ne plu necesas (1.4.0 ekzistas).

## Protokolado kaj Sentry — konvencio

La apo sendas protokolojn kaj erarojn al **Sentry.io** (organizo `esperantoradio`,
projekto `KMP`). **Nur release-konstruoj sur realaj aparatoj raportas** —
debug-konstruoj, Desktop kaj emuliloj ne inicialigas Sentry-n (2026-10-13, PR #109).
Por legi/esplori Sentry-n (MCP-iloj, datumaro-kapabloj, kvoto-taksado) uzu la
skill-on **`sentry-esplorado`**.

Niveloj (`ProtokoloUtil.kt`). Ĝenerala regulo (FAROTA G46): **ĉio krom `logd`
iras al Sentry**; `logi` estas malŝaltebla per la agordo "Ŝaltu detalan
protokoladon". Avertoj neniam iĝas error-eventoj kaj ne konsumas la erarojn-kvoton.

- `logd` — NUR loka logcat. Altfrekvencaj datum-tavolaj detaloj (parsado,
  diskkaŝmemoro, serĉo — po kanalo po ŝargo) uzu ĉi tion.
- `logi` — loka + `Sentry.logger.info`. Defaŭlte ŜALTITA (agordo "Ŝaltu detalan
  protokoladon", Agordoj → Privateco — la protokola kvoto montriĝis ne-problema).
  Por uzantvidaj eventoj: navigado, klakoj, ŝargadoj.
- `logGrava` — ĉiam al Sentry. NUR la gravaj eventoj: malfermo de la apo,
  ekludo de elsendo (kun kanalo), vekiĝo de alarmo. Uzu malmulte (kvoto).
- `logw` — avertoj. Ambaŭ formoj (kun kaj sen escepto) sendas `Sentry.logger.warn`
  al Sentry Logs — NENIAM error-evento, do ili ne konsumas la erarojn-kvoton;
  `logw(tag, msg, e)` sendas du liniojn: la escepto-resumon kaj la staktracon.
  Atendataj fiaskoj (reto, ludado, elŝuto, serĉo) uzu `logw(tag, msg, e)`.
  (Ĝis 2026-10-19 la 3-arg formo sendis NENION al Sentry — FAROTA G46.)
- `loge` — veraj eraroj. **`loge(tag, msg, e)` vokas `captureException` kaj
  konsumas la MONATAN ERAROJ-KVOTON** — neniam uzu por atendataj fiaskoj;
  `loge(tag, msg)` sen escepto estas nur protokol-evento (aparta kvoto).
- `Sentry.captureMessage` rekte en la kodo — NE uzu (po-ŝanĝaj eventoj
  plenigas la kvoton; la „Aŭtoludo“-mesaĝoj estis tial forigitaj).
- Ĉiu `catch` devas protokoli (regulo A8): `logw(tag, msg, e)` aŭ `loge(tag, msg, e)`
  kun stacktrace — neniam engluti escepton silente.
- En produktado eraroj estas 50 %-specimenataj (`options.sampleRate`, PR #109).
  Versio-bazita forĵeto (beforeSend) bezonas ĝisdateblan centran liston
  (FAROTA C45) — ne malmolu versio-listojn en la apo.

## UI-dizajno (celo)

La celo estas moderna podkasta apo-inspirita UI, bazita sur la Figma-dizajno
"Muzaiko — Antonia" (https://www.figma.com/design/uyQhrRKTLfJgQgXgAxchM4/).
Por la plenan dizajnospecifon (koloroj, tiparo, ekranoj, navigado), vidu
[`docs/nova/05_dizajno_kaj_ui.md`](docs/nova/05_dizajno_kaj_ui.md).

Nuna stato: la hejmekrano havas horizontalajn sekciojn (Kio novas, Lastatempe
ludata, Kio popularas) + "Ĉiuj kanaloj". Navigado per `ModalNavigationDrawer`-
tirkesto (`ui/NavigaTirkesto.kt`, malfermiĝas per la hamburgero en la supra
breto aŭ per tiro de la maldekstra rando); la supraj bretoj kun titoloj kaj
agoj restas, la detalaj ekranoj havas reen-sagon. MiniLudilbreto kun ludi/paŭzi/halti + ludvico-butono.
La Muzaiko-temo estas implementita en `Temo.kt`.

## Konstru-komandoj

Ĉiuj konstruaj/testaj komandoj, JDK-postuloj kaj platform-limigoj staras en la
skill-o **`konstruado-kaj-testado`** (`.vibe/skills/`). Bazaj:
`export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` (JDK 17 devas — JDK 21
mankas `jlink`); `./gradlew :shared:desktopTest` (ĉiuj testoj);
`./gradlew :androidApp:assembleDebug` (APK).

### Kaptindaĵoj en komuna kodo (commonMain)

Tiuj kvin limigoj mordas jam dum la skribado de kodo — detaloj kaj erarmesaĝoj
staras en la skill-o `konstruado-kaj-testado`:

- `Dispatchers.IO` ne ekzistas sur wasmJs — uzu `Dispatchers.Default` en komuna kodo.
- `@Volatile` bezonas `import kotlin.concurrent.Volatile` (wasmJs: "Unresolved reference").
- `Dispatchers.Main` sur Desktop bezonas `kotlinx-coroutines-swing`.
- Retezu per `HttpMotoro.kt` (expect/actual), neniam `HttpClient(CIO)` rekte —
  CIO ne funkcias sur wasmJs; la Js-motoro ĵetas `kotlin.Error` (ne Exception).
- Ne altgradigu .klib-bibliotekojn (navigation3 k.a.) sendepende de Kotlin —
  .klib de pli nova Kotlin ne legeblas sur wasmJs ("Unresolved reference").

## Datumfluo (nova apo)

```
esperantoradio_kanaloj_v9.json (bundled resource)
        ↓ KanalAgordoLeganto (striptigas // komentojn, traktas JSONC)
   List<Kanal>
        ↓ KanalDeponejoImpl (StateFlow)
        ↓
   KanalaroEkrano (Compose UI — LazyColumn de kanaloj)
        ↓ Ktor-kliento (HttpClient CIO) elŝutas RSS-fluon
   RssParsilo.parsRss(fluoTeksto, kanal)
        ↓ (reguloj P6.1–P6.7)
   List<Elsendo>
        ↓ KanalEkrano — elsendlisto (dat-grupigo, diskkaŝmemoro)
```

## Kie trovi kion

| Vi volas... | Legu |
|---|---|
| Kompreni la malnovan strukturon | `docs/malnova/01_strukturo_kaj_konstruo.md` |
| Kompreni la datumfluon | `docs/malnova/02_datumfluo.md` |
| Kompreni la parsadon (PLEJ GRAVA) | `docs/malnova/03_parsado_kaj_fontoj.md` |
| Kompreni la UI | `docs/malnova/04_ui_kaj_funkcioj.md` |
| Kompreni la arkivan servilon | `docs/malnova/05_arkiva_servilo.md` |
| Vidi la planon por la nova apo | `docs/nova/INDEKSO.md` |
| Vidi la novan arkitekturon | `docs/nova/01_celoj_kaj_arkitekturo.md` |
| Vidi la teknikan stakon | `docs/nova/02_teknika_stako.md` |
| Vidi la domajnmodelojn | `docs/nova/03_domajno_kaj_datumoj.md` |
| Vidi la parsad-specifaĵon | `docs/nova/04_parsado_kaj_arkivo.md` |
| Vidi la dizajnon (Muzaiko-temo) | `docs/nova/05_dizajno_kaj_ui.md` |
| Vidi la servilan planon | `docs/nova/06_servilo_arkivo.md` |
| Konstrui/testi/debugi la apon | la skill-o `konstruado-kaj-testado` (`.vibe/skills/`) |
| Eldoni version al Google Play | la skill-o `eldonado` (`.vibe/skills/`) |
| Legi/esplori Sentry-n | la skill-o `sentry-esplorado` (`.vibe/skills/`) |
| Redakti la kanalkonfiguron / JSONC | la skill-o `kanalkonfiguro` (`.vibe/skills/`) |
| Arkitekturaj notoj kaj historiaj lecionoj | `NOTOJ.md` |

## La nova kodo — strukturo

La nova kodo vivas en `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/`.
La kompleta dosierujo-arbo kaj dosiero-mapo staras en [`NOTOJ.md`](NOTOJ.md) →
"Deponeja koda mapo". Arkitektura konteksto:
[`docs/nova/01_celoj_kaj_arkitekturo.md`](docs/nova/01_celoj_kaj_arkitekturo.md).

## Stilo

- Dokumentado: Esperanto.
- Kodo (nova): `dk.nordfalk.esperanto.*`, identigiloj en Esperanto. La dana de la
  malnova kodo (ekz. `HentedeUdsendelser`, `Afspiller`, `Udsendelse`) estas anstataŭata
  per Esperanto en nova kodo (ekz. `ElsutitajElsendoj`, `Ludilo`, `Elsendo`). La malnova
  kodo uzas dano/esperanto-miksaĵon — ne renomigu ĝin.
- Mallonga, teknike akra stilo. Sen plenigaj vortoj.
- Ne menciu la nombron de testoj en la dokumentado — ĝi konstante ŝanĝiĝas.
