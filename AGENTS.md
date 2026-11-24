# AGENTS.md — Gvidlinioj por agentoj en ĉi tiu deponejo

> **Nuna jaro: 2026 (novembro).** Ĉiuj referencoj al "aktuala" datoj rilatas
> al 2026 se ne alie specifite.

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

## Stato de la projekto

La nova KMP-apo (Android + Desktop + Web/wasmJs) estas eldonita kaj aktiva en
Google Play; la kanona listo de versioj kaj uzitaj versionCode staras en la
skill-o **`eldonado`**. **F-Droid**: la metadatena MR !55129 estas submetita kaj
atendas revizion (paŝoj kaj stato: `fdroid/GVIDILO.md`, FAROTA C50). **Aptoide**:
ankoraŭ ne komencita (docs/nova/07 § 3). Ĉiuj fazoj de la plano estas faritaj krom:
iOS-ludado (bezonas AVPlayer + Xcode-projekton), eldono de Desktop + Web/wasmJs,
hejmekrana widget, Chromecast, parolsintezo kaj la plena HTTP-API de la
arkiv-servilo (fazo 1 — statikaj elŝuteblaj dosieroj — jam funkcias; vidu la
skill-on `servilo-arkivo` kaj `docs/nova/06_servilo_arkivo.md`). Web estas
limigita de CORS (nur la anchor.fm-podkastoj ŝargeblas sen servilo). La historia
fazo-tabelo kaj la detala listo "kio funkcias / kio ne funkcias" staras en
[`NOTOJ.md`](NOTOJ.md) → "Projekta stato kaj fazoj".

## Granda plano

Rekrei la apot en **Compose Multiplatform** (Android + iOS + Desktop + Web/Wasm), plus
konstrui memstaran **servilon** kiu funkcias kiel arkivo de Esperanto-podkastoj.

- **Malnova apo** (kodo en `malnova/app/`, `malnova/parse/`, `malnova/data/`): priskribita en `docs/malnova/`.
- **Nova apo** (en la radiko): priskribita en `docs/nova/` (komenco: [`docs/nova/INDEKSO.md`](docs/nova/INDEKSO.md)). La nova Compose Multiplatform-aposeraĵo (`androidApp/`, `desktopApp/`, `webApp/`, `shared/`) vivas en la radiko, apud `malnova/`, laŭ la oficiala KMP-ŝablono (https://kotlinlang.org/docs/multiplatform/compose-multiplatform-create-first-app.html). `iosApp/` (Xcode-projekto) ankoraŭ ne estas kreita — la iOS-kodo jam ekzistas en `shared/src/iosMain/`, sed la Xcode-projekto restas estonta. **`server/` jam ekzistas** (fazo 1: Kotlin-arkiv-konstruilo, publika elŝutejo ĉe `https://javabog.dk/esperanto-radio/` — vidu la skill-on `servilo-arkivo` kaj `docs/nova/06_servilo_arkivo.md`).

## Dosierujo-strukturo

La kompleta arbo estas en [`NOTOJ.md`](NOTOJ.md) → "Deponeja koda mapo". Esencoj:
`malnova/` (la historia apo — ne tuŝebla), `androidApp/` + `desktopApp/` +
`webApp/` + `shared/` (la nova KMP-apo), `docs/malnova/` + `docs/nova/`
(dokumentaro), `CHANGELOG.md` + `ŜANĜOJ.md` + `NOTOJ.md` + `FAROTA.md` +
`AGENTS.md` (la protokolaro), `.vibe/skills/` (proceduraj skill-oj:
`eldonado`, `sentry-esplorado`, `konstruado-kaj-testado`, `kanalkonfiguro`,
`servilo-arkivo`, `enkonduk-detekto`, `seancorezumo`), `server/` (arkiv-servilo),
`fdroid/` (F-Droid-metadatenoj), `iloj/` (agordaj skriptoj).

## Plej gravaj reguloj por agentoj

> La reguloj portas stabilajn nomojn (**A1–A12**); same la git-laborfluaj reguloj
> (**L1–L8**) kaj la sep parsreguloj (**P6.1–P6.7**, vidu `docs/malnova/03`). La FAROTA-eroj
> uzas la literojn C/G/K/X. Referencu regulojn ĉiam per tiuj nomoj, ne per nura numero.

- **A1 — Ne tuŝu la malnovan kodon** krom se eksplicite petite. Ĝi estas historia.
  La celo estas rekreado, ne riparado.
- **A2 — La parsado estas la kerno.** Antaŭ ol ŝanĝi ion pri datumoj, legu
  `docs/nova/04_parsado_kaj_arkivo.md` kaj `docs/malnova/03_parsado_kaj_fontoj.md`.
  La sep parsreguloj kaj la salto-listo devas esti konservitaj.
- **A3 — Testu la daten tavolon kontraŭ golden fixtures**, sen reto. La dosierujo
  `RssArkivServer-filcache/` enhavas realajn kaŝenitajn fluojn (NE en la repo — .gitignore-ita).
  Uzu ilin kiel determinismajn test-enirojn kopiantaj la bezonatajn fiksaĵojn al
  `shared/src/commonTest/resources/feeds/` (kernpunkto, peranto, varsoviavento, vinilkosmo, lamalfamuloj). Vidu `docs/nova/04_parsado_kaj_arkivo.md`.
- **A4 — Unu fonto-eraro ne devas panei la apon.** Se unu kanal-fluo mortas, la aliaj
  devas daŭre funkcii. Toleremeco al putrantaj fontoj estas deziro.
- **A5 — Konservu la kanalkonfiguron** (`esperantoradio_konfiguro.json`). Ĝi estas
  daten-movita konfiguro, ne malmola kodo. Per-kanalaj apartaĵoj devas esti
  en agordo, ne en logiko.
- **A6 — GPL-permesilo.** Ĉiu derivaĵo devas resti GPL.
- **A7 — Ĉiam pripensu ĉu indas fari teston.** Por ĉiu nova funkcio, modelo, aŭ
  regulo, demandu: ĉu tio estas testebla? Se jes, skribu teston. Tamen ne
  faru trivialajn testojn; preferu UI-testojn kiuj testas plurajn tavolojn.
  Ne nur skribu kodon — ankaŭ validigu ĝin. Se estas UI-ŝanĝo, antaŭ ol fari
  commit, kontrolu ke navigado funkcias, kaj ke la enhavo estas videbla.
- **A8 — Neniam engluti escepton silente.** Ĉiu `catch` bloko devas protokoli la
  eraron per `loge(tag, msg, e)` aŭ `logw(tag, msg, e)` (kun stacktrace).
  Eĉ se la eraro estas atendata aŭ negrava, protokolu ĝin per `logd`.
- **A9 — Uzu `squash`-merge por ĉiuj PR-oj.** Kiam vi kunfandas PR-on al master,
  uzu `gh pr merge --squash`. Tiel master ricevas precize 1 commit po PR.
  La commit-mesaĝo estu la titolo de la PR + ligo al la PR.
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
   esti en la git-deponejo aŭ legataj de iu agento. Ilia ekzisto estas ŝirmata
   de `.gitignore`. Se vi trovas ilin en la deponejo, tuj forigu ilin per `git rm`
   kaj informu la deponejan posedanton.

## Git-laborfluo

- **L1 —** Kreu branĉon de `master` (aŭ de la plej nova feature-branĉo se temas pri
  plia komito en ekzistanta PR).
  **Branĉnomo**: `MMdd-nomo` — ekz. `1009-hejmo-luditaj-kio-novas`.
  La `MMdd`-prefikso estas la nuna dato (ekz. 1009 = 9-a de oktobro).
  Uzu la saman nomon loke kaj sur GitHub.
- **L2 —** Faru ŝanĝojn, skribu testojn, kompilu kaj rulu testojn.
- **L3 —** Commit, push, kreu PR kun `gh pr create`.
- **L4 —** Kiam la PR estas aprobita, kunfandu per `gh pr merge --squash --delete-branch`.
  Tio kreas 1 commit sur master kun la PR-titolo kaj ligilo al la PR.
- **L5 —** Antaŭ commit kontrolu `git diff`: la posedanto povas labori paralele kaj
  liaj ŝanĝoj povas miksiĝi kun viaj (2026-11-02, #166/#167). Rilatajn ŝanĝojn aldonu
  (se en ordo), sed ne komutu senrilatajn; korektu eraretojn sed ne reskribu lian
  varianton — avertu se li enkondukis eraron. Komparu la STAGING-diffon kun la
  commit-mesaĝo (la dosieroj povas ŝanĝiĝi intertempe). Commitigu nur viajn proprajn
  dosierojn: `git add <dosiero>`, neniam blinda `git add -A`.
- **L6 — Ne commitu antaŭ ol la uzanto aprobis la ŝanĝojn.** La rajtigo por
  unu commit/push (ekz. "faru PR") validas nur por tiu unu fojo — ĝi ne
  ĝeneraligas al sekvaj ŝanĝoj sur la sama branĉo.
  **KUNFANDO (`gh pr merge`) BEZONAS APARTAN, EKPLICITAN aprobon** (ekz.
  "kunfandu"). Rajtigo por commiti/puŝi/krei PR-on NE inkluzivas la
  kunfandon — eĉ ne por tekstaj/agentaj PR-oj; "ne atendu CI" rajtigas nur la
  ŝparon de la atendo. (2026-10-19: PR #120 estis kunfandita post kiam la uzanto
  aprobis nur "commitos, puŝos kaj kreos la PR-on" — tio estis eraro.)
- **L7 — La kapa branĉo de PR ne nepre havas la nomon de via loka branĉo** — kontrolu
  `gh pr view N --json headRefName` antaŭ ol puŝi. Puŝi alinomitan lokan branĉon
  (ekz. `pr-85`) kreus NOVAN foran branĉon anstataŭ ĝisdatigi la PR-on.
- **L8 — Por ĝisdatigi malfermitan PR-on de master, uzu merge, ne rebazo** — rebazo
  reskribas la historion kaj postulas force-push (kiu estas blokita de hoko);
  `git merge origin/master` en la PR-branĉon ebligas normalan antaŭen-iran puŝon.

## Ŝanĝoprotokolo (CHANGELOG.md)

[`CHANGELOG.md`](CHANGELOG.md) estas konciza, uzantvida resumo de ĉiuj
rimarkindaj ŝanĝoj. La formato estas [Keep a Changelog](https://keepachangelog.com/1.1.0/)
kun esperantaj kategorioj (**Aldonita**, **Ŝanĝita**, **Riparita**, **Forigita**,
**Sekureco**). Ĉiu eniro estas **unu linio**: konciza priskribo sen teknikaj
detaloj + ligilo al la PR (regulo A10). Rilato al [`ŜANĜOJ.md`](ŜANĜOJ.md):
CHANGELOG = "kio ŝanĝiĝis por la uzanto", ŜANĜOJ = nur la **iom gravaj ŝanĝoj**
kaj detaloj **memorindaj por la estonteco** — suplemento al la git-historio kaj
la PR-priskriboj (problem-analizo, kialoj, kontrolrezultoj); ne ĉiu PR bezonas
enskribon. La samo ne devas aperi duoble — CHANGELOG donas la koncizan fakton,
ŜANĜOJ la kialojn.

**Laborfluo (kiu tenas ĝin ĝisdatigata):**

1. Ĉiu PR, kiu ŝanĝas uzantvideblan konduton, aldonas sian eniron al
   `## [Neeldonita]` en la sama PR (regulo A10).
2. La CI (`.github/workflows/changelog-kontrolo.yaml`) rifuzas PR-ojn, kiuj
   ŝanĝas la ĉefan fontkodon (`*Main`-dosierujoj) sen ŝanĝo de `CHANGELOG.md`.
   Preterpaso: PR-etikedo `preterlasu-changelog` (puraj internaj ŝanĝoj).
3. Eldono de nova versio: sekvu la skill-on **`eldonado`** (`.vibe/skills/eldonado/`)
   — versionCode-baskulo + CHANGELOG-sekcio + git-etikedo + eldon-notoj +
   `publishReleaseApk` de la LOKA maŝino (la CI-eldonado per GitHub-sekretoj
   ne uzeblas) + permana promocio al produktado.

## Aktivaj reguloj kaj rimarkoj

> La procedura scio vivas en la skill-oj de `.vibe/skills/` — ŝargu la koncernan
> antaŭ ol labori pri la temo: **`eldonado`** (versioj/Play/F-Droid/Aptoide),
> **`konstruado-kaj-testado`** (komandoj, JDK, platform-limigoj, testoj, emulilo,
> Compose-testaj kaptindaĵoj), **`sentry-esplorado`** (legi/analizi Sentry-n),
> **`kanalkonfiguro`** (JSONC, regex-minacoj, novaj kanaloj), **`servilo-arkivo`**
> (servilo, STT, rsync), **`enkonduk-detekto`** (transsaltuKomenceSekundojn),
> **`seancorezumo`** (HTML-resumo de seancoj). Historiaj lecionoj kaj
> arkitekturaj resumoj staras en [`NOTOJ.md`](NOTOJ.md); la okazhistorio en
> [`ŜANĜOJ.md`](ŜANĜOJ.md).

### Git- kaj PR-laboro

- **Kreu PR-branĉojn el `origin/master`, ne el la loka master (2026-11-24, PR #201→#202):**
  la loka master povas antaŭeniĝi je nekunfandita laboro — branĉo el ĝi portus tiun
  laboron en la PR (17 dosieroj anstataŭ la celataj 6). Do: `git fetch origin master`
  kaj `git switch -c MMdd-nomo origin/master`. Se la branĉo jam puŝiĝis malpure:
  eligu la proprajn komitojn per
  `git log --no-merges --reverse --oneline origin/master..<malnova>` kaj
  cherry-picku ilin
  ĈIUJN UNUOPE sur la nova branĉo (kunfandajn komitojn — ekz. de
  `git merge origin/master` — ne cherry-picku: sen `-m` git haltas ĉe ili, kaj
  ilian enhavon la nova branĉo ricevos per propra `git merge origin/master`);
  kontrolu la rezultan diffon kontraŭ `origin/master`, forigu la foran branĉon
  per `git push origin --delete <nomo>` (tio fermas la PR-on; la numero
  ŝoviĝas — ĝustigu la ligilojn per aparta commit) kaj kreu novan PR-on;
  `--force` estas blokita de hoko. La malnova loka branĉo konserviĝu ĝis la
  nova PR estas kontrolita. (La loka master mem restu netuŝita.)
- **Loka `remote.origin.fetch` estas limigita al master** (2026-10-14): `git fetch origin`
  (ankaŭ kun `--prune`) NE alportas PR-branĉojn. Por revizii PR-on, alportu la
  kap-branĉon eksplicite:
  `git fetch origin <kap-branĉo> && git switch -c <loka-nomo> FETCH_HEAD`.
  Flankefiko: `gh pr create` tiam ĉiam eraras per "you must first push the current
  branch" — aldonu `--head <branĉnomo>`.
- **Eltrovu la venontan PR-numeron tuj antaŭ la unua puŝo kaj uzu ĝin en CHANGELOG**
  per aparta commit (sen amendo): la numeroj de PR kaj issue estas komunaj, do
  prenu la maksimuman el ambaŭ kaj aldonu 1, ekz.
  `gh api "repos/nordfalk/EsperantoRadio/issues?state=all&per_page=100" -q '.[].number' | sort -n | tail -1`;
  skribu la ligilon en CHANGELOG, commit, push, poste `gh pr create --head <branĉo>`.
  Se la numero intertempe ŝoviĝis, korektu per aparta commit — ne per amendo.
- **Neniam enmetu kodo-pecojn (backtick-ojn) en komandojn kun duoblaj citiloj** —
  precipe `gh pr create --body "..."`: bash plenumas ilin kiel komand-anstataŭigon
  (2026-10-08, PR #80: akcidentaj git-tag/push/gradle-plenumoj; ripetiĝis 2026-11-24,
  PR #207: la PR-korpo mangliĝis). Ankaŭ REST-vokoj (`gh api -f body="..."`) havas la
  saman minacon. Liveru la korpon ĉiam el dosiero: `--body-file` aŭ `-f body="$(cat dosiero)"`.
- **`gh pr edit` ne funkcias en ĉi tiu deponejo** (GraphQL projectCards-eraro) —
  uzu la REST API: `gh api -X PATCH repos/nordfalk/EsperantoRadio/pulls/N -f body="$(cat dosiero)"`.
- **Afiŝitan PR-recenzon oni ne povas redakti per REST** (2026-10-20, PR #126):
  `PATCH …/pulls/N/reviews/{id}` redonas 404 kaj `DELETE …` 422 ("non-pending") —
  korektu per GraphQL `updatePullRequestReview` (mutacio kun `pullRequestReviewId`
  = `node_id` de la recenzo). **Sed unu recenzo-KOMENTON oni ja povas korekti
  per REST**: `gh api -X PATCH repos/…/pulls/comments/{id} -f body="…"` funkcias
  (2026-11-24, PR #207: fuŝita respondo korektita tiel) — `{id}` estas la
  komentar-ID, ne la recenz-ID. Kontrolu la AFIŜITAN tekston post la afiŝo.
- **Recenzo-komentoj de la posedanto povas esti nudpunktaj** (2026-10-29, PR #143):
  «forigu» aŭ nura citita linio signifas forig-peton. Agu, respondu en la fadeno
  (`gh api -X POST …/pulls/N/comments/{id}/replies -f body="…"`), kaj se la
  forigo malplenigas CHANGELOG-diferon de PR kiu tuŝas `*Main`, aldonu la
  etikedon `preterlasu-changelog` (`gh api -X POST …/issues/N/labels -f "labels[]=preterlasu-changelog"`).
- **CodeRabbit recenzas ĉiun kod-PR-on (2026-11-24, PR #204)**: kontrolu ĝiajn
  enliniajn komentojn kelkajn minutojn post ĉiu puŝo
  (`gh api repos/nordfalk/EsperantoRadio/pulls/N/comments`), traktu la validajn
  trovaĵojn kaj respondu en la fadeno — la vojo bezonas la PR-numeron
  (`pulls/N/comments/{id}/replies`; sen N ĝi redonas 404). Lasu la posedanton
  trarigardi antaŭ ol komiti. La bot-lego povas vidi malnovan revizion kaj
  kontesti jam riparitan aferon — kontrolu la faktan kodon, respondu kun la
  koncerna commit-SHA kaj ne amendu pro ĝi.
- **PR-revizio je peto de la posedanto**: troreviziu la tutan diff-on kaj aktive
  pripensu kio mankas, misas aŭ povus esti problema — ne nur konfirmu. Ĉu estas
  bona kodo? Ĉu estas verŝajnaj problemoj? Kontrolu A7 (testo), A8 (neniu silenta
  catch), A10 (CHANGELOG), A11 (FAROTA/lecionoj), ĉu nur la propraj dosieroj estas
  commititaj, kaj ĉu titolo kaj priskribo de la PR kongruas kun la kodo kaj
  intenco. Se io ne kongruas, avertu.
- **Dependabot-versi-PR-oj povas esti interdependaj kaj konfliktaj** (2026-10-13,
  #99–#102): la Kotlin-bumpoj tuŝas apudajn liniojn de `build.gradle.kts` — solvu
  per merge de master en la PR-branĉon, ĉiuj kromprogramoj je la sama versio.
  Antaŭ ol kunfandi versi-bumpon kontrolu kaj la Kotlin-metadatumojn de transiraj
  bibliotekoj (la kompililo havas maksimumon) kaj la compileSdk/AGP-postulojn
  (`checkDebugAarMetadata`). Ĝenerale: `git fetch origin master` antaŭ la fina
  puŝo — Dependabot povas esti enmetinta novajn komitojn al master dum la
  revizio (2026-10-14, #109); konfliktoj en CHANGELOG/ŜANĜOJ/AGENTS estas kutimaj
  — solvu ilin konservante ambaŭ flankojn.
- **`mergeStateStatus: UNSTABLE`** = mergeable, sed la status-kontroloj ankoraŭ
  kuras — gravas nur la fina konkludo de la kontroloj.
- **`mergeStateStatus: DIRTY` = la CI-fluoj NE startas** (2026-11-24, PR #210):
  `pull_request`-fluoji ruliĝas el la merga referenco de la PR; ĉe konflikto kun
  master la merga komito ne ekzistas, do `testoj-kaj-konstruo` kaj
  `changelog-kontrolo` silente ne ruliĝas. Mergeu `origin/master` en la PR-branĉon
  (L8) kaj puŝu — nur tiam la kontroloj rekomencas.
- **«faru beta-eldonon» = interna testo (2026-11-19, decido de la posedanto):** la
  posedanto ĉiam celas la trakon `internal` de Google Play (li testas mem kaj
  poste promocias al produktado); la trako `beta` uziĝu nur se li eksplicite diras
  «beta-trakto». La uzitaj versionCode estas listigitaj en la `eldonado`-skill-o.
- **Konstruu post kunfando de kod-ŝanĝoj**: la CI nur kontrolas CHANGELOG-ŝanĝojn —
  post ĉiu merge de kod-ŝanĝoj rulu `:shared:desktopTest` + `:androidApp:assembleDebug`.
  Por PR-oj nur kun tekstaj/agentaj dosieroj (AGENTS.md, `.vibe/skills/`, `docs/`,
  NOTOJ ktp.) tio ne necesas — nek loka konstruo nek atendo de la `testoj`-CI;
  same por kod-dosieroj kies ŝanĝoj tuŝas NUR komentojn aŭ dokumentadon
  (2026-10-17/25, reguloj de la uzanto). La kunfando mem restas sub L6.
- **Post mergo de master, reviziu la novajn invariantojn** en la kunfanditaj
  dosieroj — nova metodo povas malobservi novaĵon de master (2026-10-11:
  `markiLudita` skribis sen la intertempe postulita `mutex.withLock`).

### Kodo, lingvo kaj stilo

- **Esperanto-ortografio: nur veraj supersignoj** (renomo 2026-10-24): ĉiuj identigiloj,
  klasoj kaj dosier-nomoj uzas `ŝ ĝ ĉ ĵ ŭ` (`ŝargiElsendojnPorKanal`, `ElŝutStato.kt`,
  `SerĉoEkrano`, `ApajNovaĵojDeponejo`) — la x/h-sistemo estas forigita el la nova kodo.
  **ESCEPTOJ (persistigitaj valoroj — ne renomu)**: agord-ŝlosiloj `"auxtomata_daurigo"`,
  `"kasxitaj_avertoj"`, `"apaj_novajxoj_hesh"` (valoroj de `SettingsKeys`, ne la konstantaj
  nomoj), la kaŝmemora dosierujo `"elsendoj_kasho"`, kaj la `@JsonNames`-kromnomoj en
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
- **Terminaro kaj identigiloj (2026-10-24, kontraŭ Komputeko):** teknikajn terminojn
  en nova kodo kontrolu unue kontraŭ https://komputeko.net/data.json. Renomante
  publikan identigilon, nepre grep-u ĉiujn `*Main`-fontojn kaj testojn kaj rekompilu
  ĉiujn tuŝitajn celojn — inkluzive
  `:androidApp:compileDebugAndroidTestKotlin` (androidTest kompilas aparte kaj povas
  referenci malnovajn kampojn). Post amasa anstataŭigo (find/replace) skanu la nove
  verkitajn liniojn por fuŝitaj vortoj — ekz. `nenieu` anstataŭ `neniu` (2026-10-24,
  PR #137). Kanonaj terminoj staras en la sekcio "Terminaro" sube.
- **Lingva kontrolo por fuŝitaj vortoj (2026-11-21):** fojfoje (ekz. ĉe PR-revizio)
  rulu `python3 iloj/lingvokontrolo.py`, kiu skanas la prozon de la PR-diff per
  REVO-vortaro kaj markas nekonatajn vortojn (ekz. `temro` → `tempolimo`); trovitajn
  fuŝojn korektu, legitimajn novajn terminojn aldonu al `iloj/terminoj.txt`.
  **Ne blindigu la kontrolilon:** se la vorto estas fuŝita, korektu ĝin en la teksto
  — ne aldonu ĝin al la permeslisto. Regenero: `python3 iloj/lingvokontrolo.py
  --generu-permesliston`. Detaloj: `iloj/README.md`. (Intence NENIU CI-kontrolo.)
- **Kiam `edit` fiaskas per "string not found" sen evidenta kialo**, kontrolu la
  ĝustan literumadon per `grep`/`od` antaŭ ol reprovi — la okulo ofte "korektas"
  similan nomon (`estasEneDe` ≠ `estaEneDe`). Spacetoj kaj supersignoj glitas preter
  la okulo (2026-10-17: `"Plej ŝatataj"` ≠ `"Plejŝatataj"` — spaceto! — igis
  instrumentitan teston atendi 5 s senfine). Komparu laŭbite (`od -c`,
  `python3`-asserto), ne per la okulo; en python-ĉenoj uzu `\u015d`-eskapoliterojn,
  ne `\xc5\x9d`. Legu ankaŭ la erarmesaĝon mem — ĝi montras la plej proksiman
  kongruon kaj malkaŝas ĵus enŝtelintajn tiperojn.
- **Post awk/sed-amasa prilaboro de longa dokumento (FAROTA.md), ĉiam kontrolu per
  `grep -c '^#### '` kaj listu la ID-ojn** — ŝajne sukcesa skripto povas silente
  forvori najbarajn sekciojn (2026-11-20: C5/C26/C27 perdiĝis; Statuso-linioj
  forglutiĝis de `getline`).
- **Ne faru CPU-intensan laboron (parsado, dosier-I/O) sur la ĉefa fadeno.**

### Arĥitekturo kaj konduto de la apo

- **Arkitekturaj bazlinioj**: neniu DI-framintervalo (permana injektado en
  konstruktiloj); neniu datumbazo (la kliento kaŝenas servil-respondojn kiel
  dosierojn).
- **`LaunchedEffect(Unit) { vm.ŝargi() }` lanĉas la ŝargadon je ĈIU ekran-eniro**
  (2026-10-20): navigado rekomponas la ekranon, do reveno al ekrano reŝargus ĉion.
  Ĉiu ŝargado, kiun ekrano ekigas, devas iri tra `ŜargaKontrolilo` (dedupigo +
  TTL 5 min; `ui/ŜargaKontrolilo.kt`) — kaj `leguKaŝitajnElsendojn` ne re-parsu
  kiam la en-memora kaŝo jam havas la kanalon.
- **La diska kaŝo havas `<slug>.rss` (kruda, rezervo) KAJ `<slug>.json`** (parsitaj
  `Elsendo`, @Serializable; ~10× pli rapide ol re-parsado, 2026-11-21). Legado de la
  kaŝo okazas paralele sur `Dispatchers.Default` kaj liveras rezultojn progresive
  (`leguĈiujnKaŝitajnElsendojn(poKanalRicevita=…)`), por ke Hejmo montru enhavon tuj.
- **La apo kunfandas la arkivan kaj la originalan fluon po kanalo (2026-11-20):**
  `arkivoBazoUrl` → `<bazo><slug>.xml` + la originala `elsendojRssUrl`, paralele,
  kunfandataj per `kunfanduElsendojn` (laŭ ID: la arkivo venkas; la originalo
  aldonas novajn elsendojn tuj). Se ambaŭ fiaskas: kaŝo (unue enmemora, poste
  diska). `deArkivo=true` konservas la finan ID-on el la guid. Tempolimoj: arkivo
  10 s, originalo 15 s; grandec-limoj 8 MB/1 MB. `normigiDaton` akceptas la
  monaton usklec-sensive (RFC-822).
- **La persistigita navig-stato NE transvivas ĝisdatigon de la apo** (2026-10-27):
  la pak-anstataŭigo forviŝas la task-rekordon — la Vojo-diskriminiloj el #134
  ŝirmis okazintaĵon, kiu ne povas okazi, kaj estas forigitaj (ne reenkonduku).
  La `@JsonNames`-kromnomoj en `Modeloj.kt` tamen RESTAS NECESAJ: ili ŝirmas
  `Settings`/dosierojn, kiuj ja transvivas ĝisdatigojn.
- **nav3-vojoj estas VALOR-bazitaj ŝlosiloj — ŝlosil-egalo signifas komunan
  vizito-ŝtaton (2026-11-24):** nav3 konservas la staton de eniro per
  `contentKey = "$key:${key::class}"` (la TUTA voja valoro). Solvo:
  `Vojo.ElsendoDetalo(id, kanaloSlug, vizito)` / `KanaloDetalo(slug, vizito)` —
  `vizito` estas hazarda kodo unika po malfermo. NE reenkonduku paĝ-korektojn en
  `ElsendoEkranoKunSvipo` — ili rompas la rotacian pozicion. La cel-identeco
  komparas nur la identigilojn per `estasSamaCelo` (App.kt); uzu `puŝuVojon`,
  ne `backStack.add`. Mankanta kanalo/elsendo montras `NetroveblasEkrano`
  (elsendo: post la ŝarg-kaskado `ElsendoDeponejoImpl.trovuElsendon`).
- **ICY/StreamTitle ne atingas la MediaController-on** (2026-11-02, #166): la
  MediaSession ne plusendas `onMetadata` — la servo skribas la titolon en
  `IcyTitoloRegistro`, kiun `ExoPlayerLudiloRegilo.updateState()` legas
  (ĉiusekunda enketado). Desktop kaj wasmJs ne eligas ICY.
- **wasmJs: material3 devas resti parigita kun CMP (2026-11-22):**
  `compose-material3 = "1.13.0-alpha01"` kun CMP 1.12.2 kompiliĝas sed mortas en
  la retumilo per `LinkError` (skiko-malkongruo) — restu je la parigo el la
  CMP-eldon-notoj (nun `1.12.0-alpha03`). Ne altgradigu ilin sendepende.
- **CancellationException de animacio ne mortigu kolektanton en commonMain**
  (2026-11-24, #205): `animateScrollToItem` ĵetas `CancellationException` kiam
  uzanta gesto interrompas ĝin — en `LaunchedEffect(Unit)`-kolektanto tio finus
  la kolekton porĉiame. Kaptu kaj voku `coroutineContext.ensureActive()`, por ke
  nur la propra nuligo disvastiĝu; poste protokolu per `logd`. ATENTU:
  `kotlin.coroutines.currentCoroutineContext` NE kompileblas en commonMain —
  uzu `kotlin.coroutines.coroutineContext` + `kotlinx.coroutines.ensureActive`.

### Servilo kaj publikigo

- **Kanona datum-arbo kaj ligiloj (2026-11-24):** la servilaj datumoj vivas en
  `/home/j/Code/EsperantoRadio/server/data/` (koneje: 2,7 GB `privata/` + `publika/`).
  ĈIU kontrolilo ligas ĝin per SUB-LIGILOJ, ne per tuta-direktoria ligilo:
  `server/data/privata` → kanona `privata`; `server/data/publika/` estas REALA
  dosierujo, en kiu nur `index.html` kaj `icon.png` estas realaj (git-versiaj) kaj
  `kanaloj/`, `bildoj/`, `apo/`, `esperantoradio_konfiguro.json` estas ligiloj al
  la kanonaj. La kopieblan recepton por nova kontrolilo vidu en la skill-o
  `servilo-arkivo`. Portempaj dosieroj iras al `tmp/` ĉe la radiko de la laborkopio
  (gitignorita) — NENIAM en la kanonan arbon.
- **Ne publikigu al la servilo sen eksplicita peto (2026-11-20):** `rsync …
  javabog.dk:javabog.dk/esperanto-radio/` (aŭ ĉiu alia ekster-retea publikigo)
  okazas NUR post kiam la uzanto tion eksplicite petas („kopiu al la servilo“).
  Loka konstruado kaj loka rsync-simulado estas en ordo; la reala rsync ĉiam
  atendas la aprobon (detaloj: skill-o `servilo-arkivo`). La publikiga rsync
  devas uzi `-L` (la `publika/`-subvojoj estas simbolaj ligiloj) kaj
  `--chmod=D755,F644`.
- **GitHub ne vidigas `.html`-dosierojn en la deponejo** (2026-10-29): ili montriĝas
  kiel fontokodo. Solvo: **GitHub Pages** el `master:/docs` (aktivigita; rekonstruas
  je ĉiu puŝo al master). Ligiloj al `.html`-dosieroj en README/dokumentoj iru al la
  Pages-URL (`https://nordfalk.github.io/EsperantoRadio/…`); la generita
  seancorezumo iras al `docs/historio.html` (vidu la skill-on `seancorezumo`).

### Testoj kaj medio

- Kaptindaĵoj de Compose-testoj (apartaj radikoj de dialogoj,
  `ModalNavigationDrawer` en la semantika arbo, Scaffold-padding,
  LazyRow-svingo, subtitolo de MiniLudilbreto), la emulilo kaj la medio
  (`/tmp` blokita, force-push-hoko, `connectedDebugAndroidTest` malinstalas
  la APK-on, ŝarĝo-dependaj fiaskoj) staras en la skill-o
  **`konstruado-kaj-testado`** — ŝargu ĝin antaŭ ol verki aŭ ruli testojn.
- **Se fona konstruo fiaskas sed vi vidis nur filtritan eligon** (`tail`/`grep` en
  `process.start`): la procez-reteno forĵetis la detalojn — rerulu kun plena
  (aŭ `grep -E "FAILED|BUILD|e: "`) eligokapto antaŭ ol komenci diagnozon.

## Terminaro — kanonaj terminoj

Elektitaj kanonaj terminoj fiksitaj de la lingvaĵa korekto (2026-10-24, PR #137).
Ĝeneralaj komputikaj terminoj estas en https://komputeko.net/data.json; ĉi tie
estas la projekto-specifaj decidoj kaj la terminoj por prozo (por identigiloj
sufiĉas grep-i kaj kopii el la kodo).

| Angla (fonto) | Esperanta | Ekzemplo / rimarko |
|---|---|---|
| byte | **bitoko** | `MAKSIMUMA_FLUA_GRANDECO_BITOKOJ` ("bajto" akceptebla, sed bitoko preferata) |
| disk cache | **diska kaŝmemoro** | "legas diskan kaŝmemoron" (kaŝmemoro = la tekniko, diska kaŝmemoro = la loko) |
| ringtone | **vokosono** | rezerva vokosono, `haltuAlarmVokosonon` |
| dial | **ciferplato** | "Elekti per ciferplato" (tempo-elektilo) |
| slider | **ŝovilo** | progresoŝovilo, `testTag("ŝovilo")` |
| optimization | **optimumigo** | "Bateri-optimumigo estas ŝaltita" |
| server error | **servila eraro** | `estasServilaEraro` |
| kill (process) | **ĉesigi** | "ĉesigi la aplikon en la fono" |
| domain name | **domajna nomo** | "solvi domajnajn nomojn" |
| buffer (verbo) | **bufri** | "ekbufras ludado" |
| buffer (substantivo) | **bufro** | "bufro = … bitokoj" |
| ongoing | **daŭra** | daŭra sciigo |
| pending | **atendanta** | `AtendantaElsendoNavigacio` |
| orphan | **orfo** | "Forigas orfan partan dosieron" |
| metadata | **metadatumoj** | `METADATUMOJ_LIMTEMPO_MS` |
| livestream | **tuja elsendfluo** | `rektaElsendaSonoUrl` |
| media notification | **ludada sciigo** | (ne "mediasciigo") |
| media button | **ludbutono** | (ne "mediabutono") |
| audio | **aŭda** | "Retumila aŭda eraro" |
| bundled | **paktita** | `leguPakititanKanalkonfiguron` |
| developer mode | **programista reĝimo** | agordo "Programista reĝimo" |
| spectrogram | **spektrogramo** | derivaj bildoj el metadatumoj |
| skip list | **salto-listo** | salto-listo de neeltireblaj gastigantoj |
| use case | **ukazo** | "Ukazoj el la malnova apo" |
| download location | **elŝutejo** | elekti lokon de elŝutitaj elsendoj |

Kunmetaĵoj skribiĝas kiel unu vorto: **sciigpermeso**, **sciigagordoj**, **sciiglimoj**
(ne "sciig-permeso" ktp.). Esceptoj restas la persistitaj valoroj kaj API-nomoj
(vidu la noton pri supersignoj en "Kodo, lingvo kaj stilo").

## Protokolado kaj Sentry — konvencio

La apo sendas protokolojn kaj erarojn al **Sentry.io** (organizo `esperantoradio`,
projekto `KMP`). **Nur eldon-konstruoj sur realaj aparatoj raportas** —
debug-konstruoj, Desktop kaj emuliloj ne inicialigas Sentry-n (2026-10-13, PR #109)
— **kaj nur kiam la erar-raportado estas ŝaltita** (agordo `erar_raportado`;
defaŭlto RUTEMPA laŭ la INSTAL-FONTO de la pako, ne la subskribo
(2026-11-24, PR: riparo de la CodeRabbit-trovo en #200 — per RB F-Droid
distribuas APK-on kun NIA atestilo, do la subskribo de PR #148 ne plu
distingas la kanalojn): instalaĵo el KONATA ne-F-Droid-kanalo
(`com.android.vending`, `com.android.packageinstaller`) = defaŭlte ŝaltita
(opt-out), malŝaltebla en Agordoj → Privateco; ĉiu alia instala fonto
(F-Droid `org.fdroid.*` kaj ĈIU NEKONATA) = opt-in kun unufoja
konsento-demando ĉe la unua starto. Por legi/esplori Sentry-n (MCP-iloj, datumaro-kapabloj,
kvoto-taksado) uzu la skill-on **`sentry-esplorado`**.

Niveloj (`ProtokoloUtil.kt`). Ĉiuj Sentry-vojoj estas protektataj per la
tutmonda flago `raportadoAktiva` (konsento, PR #148). Ĝenerala regulo (FAROTA
G46): **ĉio krom `logd` iras al Sentry** (post konsento); `logi` estas
malŝaltebla per la agordo "Ŝaltu detalan protokoladon".
Avertoj neniam iĝas error-eventoj kaj ne konsumas la erarojn-kvoton.

- `logd` — NUR loka logcat. Altfrekvencaj datum-tavolaj detaloj (parsado,
  diska kaŝmemoro, serĉo — po kanalo po ŝargo) uzu ĉi tion.
- `logi` — loka + `Sentry.logger.info`. Defaŭlte ŜALTITA (agordo "Ŝaltu detalan
  protokoladon", Agordoj → Privateco). Por uzantvidaj eventoj: navigado, klakoj,
  ŝargadoj.
- `logGrava` — al Sentry (post konsento). NUR la gravaj eventoj: malfermo de la
  apo, ekludo de elsendo (kun kanalo), vekiĝo de alarmo. Uzu malmulte (kvoto).
- `logw` — avertoj. Ambaŭ formoj sendas `Sentry.logger.warn` al Sentry Logs —
  NENIAM error-evento, do ili ne konsumas la erarojn-kvoton; `logw(tag, msg, e)`
  sendas la escepto-resumon kaj la staktracon. Atendataj fiaskoj (reto, ludado,
  elŝuto, serĉo) uzu `logw(tag, msg, e)`.
- `loge` — veraj eraroj. **`loge(tag, msg, e)` vokas `captureException` kaj
  konsumas la MONATAN ERAROJ-KVOTON** — neniam uzu por atendataj fiaskoj;
  `loge(tag, msg)` sen escepto estas nur protokol-evento (aparta kvoto).
- `Sentry.captureMessage` rekte en la kodo — NE uzu (po-ŝanĝaj eventoj
  plenigas la kvoton).
- Ĉiu `catch` devas protokoli (regulo A8): `logw(tag, msg, e)` aŭ `loge(tag, msg, e)`
  kun stacktrace — neniam engluti escepton silente.
- En produktado eraroj estas 50 %-specimenataj (`options.sampleRate`, PR #109).
  Versio-bazita forĵeto (beforeSend) bezonas ĝisdateblan centran liston — ne
  malmolu versio-listojn en la apo (la provo kaj retiro staras en ŜANĜOJ,
  „Forĵeto de eraroj de malnovaj versioj“).

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
Ekzistas ankaŭ diagnoza ekrano (`DiagnozoEkrano`/`DiagnozoRegilo`, modelo `Diagnozo.kt`)
kaj la memstara fonto-aŭtomata daŭrigo. La Muzaiko-temo estas implementita en `Temo.kt`.

## Konstru-komandoj

Ĉiuj konstruaj/testaj komandoj, JDK-postuloj kaj platform-limigoj staras en la
skill-o **`konstruado-kaj-testado`** (`.vibe/skills/`). Bazaj:
`export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` (JDK 17 devas — JDK 21
mankas `jlink`); `./gradlew :shared:desktopTest` (ĉiuj testoj);
`./gradlew :androidApp:assembleDebug` (APK).

### Kaptindaĵoj en komuna kodo (commonMain)

Tiuj limigoj mordas jam dum la skribado de kodo — detaloj kaj erarmesaĝoj
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
esperantoradio_konfiguro.json (paktita risurco)
        ↓ KanalAgordoLeganto (striptigas // komentojn, traktas JSONC)
   List<Kanal>
        ↓ KanalDeponejoImpl (StateFlow)
        ↓
   KanalaroEkrano (Compose UI — LazyColumn de kanaloj)
        ↓ Ktor-kliento (HttpClient CIO) elŝutas RSS-fluon
   RssParsilo.parsRss(fluoTeksto, kanal)
        ↓ (reguloj P6.1–P6.7)
   List<Elsendo>
        ↓ KanalEkrano — elsendlisto (dat-grupigo, diska kaŝmemoro)
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
