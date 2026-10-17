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
(dokumentaro), `CHANGELOG.md` + `SXANGXOJ.md` + `NOTOJ.md` + `FAROTA.md` +
`AGENTS.md` (la protokolaro), `.vibe/skills/` (proceduraj skill-oj:
`eldonado`, `sentry-esplorado`, `konstruado-kaj-testado`, `kanalkonfiguro`).

## Plej gravaj reguloj por agentoj

1. **Ne tuŝu la malnovan kodon** krom se eksplice petite. Ĝi estas historika.
   La celo estas rekreado, ne riparado.
2. **La parsado estas la kerno.** Antaŭ ol ŝanĝi ion pri datumoj, legu
   `docs/nova/04_parsado_kaj_arkivo.md` kaj `docs/malnova/03_parsado_kaj_fontoj.md`.
   La sep parsreguloj kaj la skip-listo devas esti konservitaj.
3. **Testu la daten tavolon kontraŭ golden fixtures**, sen reto. La dosierujo
   `RssArkivServer-filcache/` enhavas realajn kaŝenitajn fluojn (NE en la repo — .gitignore-ita).
   Uzu ilin kiel determinismajn test-enirojn kopiantaj la bezonatajn fiksaĵojn al
   `shared/src/commonTest/resources/feeds/` (4 jam tie: kernpunkto, peranto, varsoviavento, vinilkosmo). Vidu `docs/nova/04_parsado_kaj_arkivo.md`.
   La testoj jam kopiis 3 fiksaĵojn al `shared/src/commonTest/resources/feeds/`.
4. **Unu fonto-eraro ne devas panei la apot.** Se unu kanal-fluo mortas, la aliaj
   devas daŭre funkcii. Toleremeco al putrantaj fontoj estas deziro.
5. **Konservu la kanalkonfiguron** (`esperantoradio_kanaloj_v9.json`). Ĝi estas
   daten-movita konfiguro, ne malmola kodo. Per-kanalaj apartaĵoj devas esti
   en agordo, ne en logiko.
6. **GPL-licenco.** Ĉiu derivaĵo devas resti GPL.
7. **Ĉiam pripensu ĉu indas fari teston.** Por ĉiu nova funkcio, modelo, aŭ
   regulo, demandu: ĉu tio estas testebla? Se jes, skribu teston. Tamen ne
   faru trivialajn testojn; preferu UI-testojn kiuj testas plurajn tavolojn.
   Ne nur skribu kodon — ankaŭ validigu ĝin. Se estas UI-ŝanĝo, antaŭ ol fari 
   commit, kontrolu ke navigado funkcias, kaj ke la enhavo estas videbla.
8. **Neniam engluti escepton silente.** Ĉiu `catch` bloko devas protokoli la
   eraron per `loge(tag, msg, e)` aŭ `logw(tag, msg, e)` (kun stacktrace).
   Eĉ se la eraro estas atendata aŭ negrava, protokolu ĝin per `logd`.
9. **Uzu `squash`-merge por ĉiuj PR-oj.** Kiam vi kunfandas PR-on al master,
   uzigu `gh pr merge --squash`. Tiel master ricevas precize 1 commit po PR.
   La commit-mesaĝo estu la titolo de la PR + ligo al la PR
10. **Ĝisdatigu `CHANGELOG.md` en ĉiu PR kun uzantvidebla ŝanĝo.** Aldonu
   **unu linion** al la `## [Neeldonita]`-sekcio **ene de la sama PR**: konciza
   priskribo sen teknikaj detaloj + ligilo al la PR, ekz.:
   `- Vekhorloĝo ludas ankaŭ podkastojn (https://github.com/nordfalk/EsperantoRadio/pull/123)`.
   Pura interno (testoj, refaktorado, dokumentaro, CI) ne bezonas eniron; se la
   CI-kontrolo erare postulas ĝin, aldonu la etikedon `preterlasu-changelog` al
   la PR. Detalojn vidu en la sekcio "Ŝanĝoprotokolo (CHANGELOG.md)".
11. **Aŭtomate aldonu viajn lecionojn.** Post ĉiu laboro — precipe post
    eraro, incidento aŭ neatendita rezulto — skribu koncizan lecionon (kun
    dato): aktivaj reguloj en "Aktivaj reguloj kaj rimarkoj" (sube), procedura
    scio en la koncernan skill-on (`.vibe/skills/`), historiaj detaloj en
    NOTOJ.md aŭ SXANGXOJ.md. Celo: ne ripeti la saman eraron.
12. **Neniam legu, modifu aŭ commit `secrets.properties` aŭ `*.keystore` aŭ `*.jks`.**
    Ĉi tiuj dosieroj enhavas sekretajn subskribajn informojn kaj neniam devas
    esti en la git-deponejo aŭ legataj de iu agento. Ilia ekzisto estas ŝirmita
    de `.gitignore`. Se vi trovas ili en la deponejo, tuj forigu ilin kun `git rm`
    kaj informu la deponejan posedanton.

## Git-laborfluo

1. Kreu branĉon de `master` (aŭ de la plej nova feature-branĉo se temas pri
   plia kommito en ekzistanta PR).
   **Branĉnomo**: `MMdd-nomo` — ekz. `1009-hejmo-luditaj-kio-novas`.
   La `MMdd`-prefikso estas la nuna dato (ekz. 1009 = 9-a de oktobro).
   Uzu la saman nomon loke kaj sur GitHub.
2. Faru ŝanĝojn, skribu testojn, kompilu kaj rulu testojn.
3. Commit, push, kreu PR kun `gh pr create`.
4. Kiam la PR estas aprobita, kunfandu per `gh pr merge --squash --delete-branch`.
   Tio kreas 1 commit sur master kun la PR-titolo kaj ligilo al la PR.
5. Antaŭ commit kontrolu ĉu uzanto ŝanĝis aferojn rilate al via laboro, se jes
   kaj ŝajnas esti en ordo, aldonu ankaŭ tion al la commit
6. **Ne commitu antaŭ ol la uzanto aprobis la ŝanĝojn.** La rajtigo por
   unu commit/push (ekz. "faru PR") validas nur por tiu unu fojo — ĝi ne
   ĝeneraligas al sekvaj ŝanĝoj sur la sama branĉo.
7. **La kapa branĉo de PR ne nepre havas la nomon de via loka branĉo** — kontrolu
   `gh pr view N --json headRefName` antaŭ ol puŝi. Puŝi alinomitan lokan branĉon
   (ekz. `pr-85`) kreus NOVAN foran branĉon anstataŭ ĝisdatigi la PR-on.
   (2026-10-11: PR #85 naskas el `hejmo-luditaj-kio-novas`, ne `pr-85`.)
8. **Por ĝisdatigi malfermitan PR-on de master, uzu merge, ne rebazo** — rebazo
   reskribas la historion kaj postulas force-push; `git merge origin/master` en la
   PR-branĉon ebligas normalan antaŭen-iran puŝon. (2026-10-11: tiel PR #85
   ricevis master-ĝisdatigojn.)

## Ŝanĝoprotokolo (CHANGELOG.md)

[`CHANGELOG.md`](CHANGELOG.md) estas konciza, uzantvida resumo de ĉiuj
rimarkindaj ŝanĝoj. La formato estas [Keep a Changelog](https://keepachangelog.com/1.1.0/)
kun esperantaj kategorioj (**Aldonita**, **Ŝanĝita**, **Riparita**, **Forigita**,
**Sekureco**). Ĉiu eniro estas **unu linio**: konciza priskribo sen teknikaj
detaloj + ligilo al la PR (regulo 10). Rilato al [`SXANGXOJ.md`](SXANGXOJ.md):
CHANGELOG = "kio ŝanĝiĝis por la uzanto", SXANGXOJ = detala teknika
labortaglibro (problem-analizo, kialoj, kontrolrezultoj). La samo ne devas
aperi duoble — CHANGELOG donas la koncizan fakton, SXANGXOJ la kialojn.

**Laborfluo (kiu tenas ĝin ĝisdatigata):**

1. Ĉiu PR, kiu ŝanĝas uzantvideblan konduton, aldonas sian eniron al
   `## [Neeldonita]` en la sama PR (regulo 10).
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
> **`kanalkonfiguro`** (JSONC, regex-minacoj, novaj kanaloj). Historiaj lecionoj kaj
> arkitekturaj resumoj staras en [`NOTOJ.md`](NOTOJ.md); la okazhistorio en
> [`SXANGXOJ.md`](SXANGXOJ.md).

- **Konstruu post kunfando de kod-ŝanĝoj.** La CI nur kontrolas CHANGELOG-ŝanĝojn —
  post ĉiu merge rulu `:shared:desktopTest` + `:androidApp:assembleDebug`. Ne faru
  tion por PR-oj kiuj nur tuŝas agentajn dosierojn (AGENTS.md, `.vibe/skills/`,
  `docs/`) — nenio kompiliĝas en ili (2026-10-17, regulo de la uzanto).
- **Neniam enmetu kodo-pecojn (backtick-ojn) en komandojn kun duoblaj citiloj** —
  precipe `gh pr create --body "..."`: bash plenumas ilin kiel komand-anstataŭigon
  (2026-10-08, PR #80: akcidentaj git-tag/push/gradle-plenumoj). Uzu `--body-file`.
- **Post mergo de master, reviziu la novajn invariantojn** en la kunfanditaj
  dosieroj — nova metodo povas malobservi novaĵon de master (2026-10-11: `markiLudita`
  skribis sen la intertempe postulita `mutex.withLock`).
- **Kiam `edit` fiaskas per "string not found" sen evidenta kialo**, kontrolu la
  ĝustan literumadon de identigiloj per `grep`/`od` antaŭ ol reprovi — la okulo
  ofte "korektas" similan nomon (`estasEneDe` ≠ `estaEneDe`).
- **`gh pr edit` ne funkcias en ĉi tiu deponejo** (GraphQL projectCards-eraro) —
  uzu la REST API: `gh api -X PATCH repos/nordfalk/EsperantoRadio/pulls/N -f body="$(cat dosiero)"`.
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
- **Dependabot povas enmeti pliajn komitojn al master dum revizio de PR**
  (2026-10-14, PR #109): dum la revizio envenis #99 kaj #101 — kontrolu
  `git fetch origin master` antaŭ la fina puŝo kaj re-merge se necese
  (`git merge --no-ff origin/master` en la PR-branĉo; konfliktoj en la
  dokumentaj dosieroj (CHANGELOG/SXANGXOJ/AGENTS) estas kutimaj — solvu ilin
  konservante ambaŭ flankojn).
- **`mergeStateStatus: UNSTABLE`** (2026-10-14, PR #109): signifas "mergeable,
  sed la status-kontroloj ankoraŭ kuras" — gravas nur la fina konkludo de la
  kontroloj (aŭ atendu `CLEAN` antaŭ ol merĝi).

## Protokolado kaj Sentry — konvencio

La apo sendas protokolojn kaj erarojn al **Sentry.io** (organizo `esperantoradio`,
projekto `KMP`). **Nur release-konstruoj sur realaj aparatoj raportas** —
debug-konstruoj, Desktop kaj emuliloj ne inicialigas Sentry-n (2026-10-13, PR #109).
Por legi/esplori Sentry-n (MCP-iloj, datumaro-kapabloj, kvoto-taksado) uzu la
skill-on **`sentry-esplorado`**.

Niveloj (`ProtokoloUtil.kt`):

- `logd` — NUR loka logcat. Altfrekvencaj datum-tavolaj detaloj (parsado,
  diskkaŝmemoro, serĉo — po kanalo po ŝargo) uzu ĉi tion.
- `logi` — loka + `Sentry.logger.info`. Defaŭlte ŜALTITA (agordo "Ŝaltu detalan
  protokoladon", Agordoj → Privateco — la protokola kvoto montriĝis ne-problema).
  Por uzantvidaj eventoj: navigado, klakoj, ŝargadoj.
- `logGrava` — ĉiam al Sentry. NUR la gravaj eventoj: malfermo de la apo,
  ekludo de elsendo (kun kanalo), vekiĝo de alarmo. Uzu malmulte (kvoto).
- `logw` — avertoj. Atendataj fiaskoj (reto, ludado, elŝuto, serĉo) uzu
  `logw(tag, msg, e)` — ĝi restas LOKA kaj ne konsumas la Sentry-kvoton.
- `loge` — veraj eraroj. **`loge(tag, msg, e)` vokas `captureException` kaj
  konsumas la MONATAN ERAROJ-KVOTON** — neniam uzu por atendataj fiaskoj;
  `loge(tag, msg)` sen escepto estas nur protokol-evento (aparta kvoto).
- `Sentry.captureMessage` rekte en la kodo — NE uzu (po-ŝanĝaj eventoj
  plenigas la kvoton; la „Aŭtoludo“-mesaĝoj estis tial forigitaj).
- Ĉiu `catch` devas protokoli (regulo 8): `logw(tag, msg, e)` aŭ `loge(tag, msg, e)`
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
ludata, Kio popularas) + "Ĉiuj kanaloj". Malsupra naviga breto kun 4 langetoj
(Hejmo, Kanaloj, Plej ŝatataj, Serĉi). MiniLudilbreto kun ludi/paŭzi/halti +
ludvico-butono. La Muzaiko-temo estas implementita en `Temo.kt`.

## Konstru-komandoj

Ĉiuj konstruaj/testaj komandoj, JDK-postuloj kaj platform-limigoj staras en la
skill-o **`konstruado-kaj-testado`** (`.vibe/skills/`). Bazaj:
`export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` (JDK 17 devas — JDK 21
mankas `jlink`); `./gradlew :shared:desktopTest` (ĉiuj testoj);
`./gradlew :androidApp:assembleDebug` (APK).

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
        ↓ (reguloj 6.1–6.7)
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
