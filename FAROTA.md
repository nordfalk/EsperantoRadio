# FAROTA.md — Malfermitaj trovoj por posta riparado

> **Kiel uzi** (ĝisdatigita 2026-11-20): ĉi tiu dosiero enhavas NUR la MALFERMAJN punktojn, ordigitaj laŭ graveco (KRITIKA → GRAVA → KONSILO). Kiam punkto estas solvita, FORIGU ĝin tute — ĝia priskribo kaj la historio de la riparo estas arkivitaj en `git log` de ĉi tiu dosiero, en la PR/CHANGELOG kaj en [`ŜANĜOJ.md`](ŜANĜOJ.md); la daŭre valida scio el finitaj punktoj staras en [`NOTOJ.md`](NOTOJ.md).
> **Rilato al la aliaj dosieroj** (ekde 2026-10-11): [`NOTOJ.md`](NOTOJ.md) = konstanta scio laŭ temo; [`ŜANĜOJ.md`](ŜANĜOJ.md) = historio de la riparoj (problemo→kaŭzo→testo); `CHANGELOG.md` = uzantvida resumo.

## KONSILO

#### C49 — Kontrolu ĉu `RespondoTroGrandaException` trafas realan fluon (limo 1 MB, G45)
- **Statuso**: [ ] (neniu okazinta en 30 tagoj, kontrolite 2026-11-13→11-20 — plu observu kun malalta prioritato)
- **Fonto**: G45-riparo (2026-10-20): `KorpaLegilo.kt` limas la RSS-korpo-legadon al
  `MAKSIMUMA_FLUA_GRANDECO_BAJTOJ = 1 MB` (la plej granda konata legitima fluo ~310 KB).
- **Priskribo**: Se iam reala kanal-fluo transpasos 1 MB (ekz. podkasto kun centoj da
  longaj priskriboj), la kanalo silente ne ŝargiĝos — la escepto estas nur `logw`-averto,
  ne eraro. Tio estas intence (memor-protekto), sed necesas observado.
- **Sugesto**: periode (ekz. ĉiusemajne, per la skill-o `sentry-esplorado`) serĉi en
  Sentry la escepto-nomon `RespondoTroGrandaException` aŭ la protokolon "transpasas la
  limon": se ĝi aperas por reala fluo (ne por misdirektita MP3), levu
  `MAKSIMUMA_FLUA_GRANDECO_BAJTOJ` aŭ serĉu la kaŭzon en la koncerna kanalo.

#### C50 — Eldonaj kanaloj ekster Google Play: F-Droid, Aptoide
- **Statuso**: [ ]
- **Fonto**: README.md "Farota (notoj)" (2026-10-24, movita ĉi tien; prepara laboro en PR #66)
- **Priskribo**: la apo eldoniĝas nur en Google Play; F-Droid kaj Aptoide estis planitaj
  sed neniam realigitaj.
- **Progreso (2026-10-29)**: la teknika preparo estas farita — la paŝoj de la
  posedanto staras en `docs/nova/07_eldonado.md` (sekcioj 2 kaj 3):
  - nesubskribita release-konstruo ebligita (release subskribas nur kiam la
    keystore ekzistas; pruvita en pura worktree — F-Droid kondiĉo);
  - `fdroid/metadata/dk.nordfalk.esperanto.radio.yml` estas pretigita laŭ la
    nuna fdroiddata-formato kaj **kontraŭvalidigita kontraŭ la oficiala
    `schemas/metadata.json`** (kategorioj `Radio`/`Podcast`; `AutoUpdateMode:
    Version` — la malnova `v%v`-ŝablono ne plu rajtas; lokalizitaj kampoj
    malpermesataj en la yml — la en-US-tekstojn F-Droid legas aŭtomate el
    nia Play-listing `androidApp/src/main/play/` (Triple-T-strukturo));
    3.1.1/253, etikedo `v3.1.1`, aŭtomata ĝisdatigo per etikedoj
    (la unua fork-CI-provo de la posedanto malkaŝis la skemo-erarojn, 2026-10-31);
  - ĉiuj ses forko-CI-taskoj reproduktitaj VERDAJ loke per la samaj iloj
    (fdroidserver + check-jsonschema + tools/-skriptoj, 2026-10-31): la yml
    estas nun en la kanona rewritemeta-formo (neniu komento), NENIU teksto
    en fdroiddata (resumo/priskribo/ŝanĝoprotokolo venas el nia Play-listing,
    Triple-T + `release-notes/en-US/default.txt`), `commit` = plena haketaĵo,
    `UpdateCheckName: Ignore` (la nom-boto prenus la nomon de la malnova
    apo — KMP-manifesto ne troveblas), `scanignore: build.gradle.kts`
    (la skanilo rifuzis la jcenter-deklaron necesan por la malnova apo; post la
    jcenter-forigo en tiu ĉi PR la scanignore povas foriĝi ĉe la sekva
    yml-ĝisdatigo);
    `fdroid checkupdates --auto` finas per „up to date" kaj nenia diff;
  - fdroid-internaĵoj esploritaj kaj dokumentitaj: `gradlew-fdroid` trovas la
    radikan `gradle/wrapper` ankaŭ el `subdir: androidApp`; la `gradle:`-kampo
    signifas gustumojn (`yes` = `assembleRelease`), ne taskojn;
  - Sentry-raportado iĝis **dumoda** (PR #148): konstruoj sen nia subskribo
    (tiel F-Droid konstruas) = opt-in kun unufoja konsento-demando; konstruoj
    kun nia subskribo (Play/Aptoide) = defaŭlte ŝaltita, malŝaltebla en
    Agordoj → Privateco (kun fina protokolo ĉe malŝalto kaj protokolo ĉe
    reŝalto) — la F-Droid-kontraŭeco `Tracking` ne aplikeblas al la
    F-Droid-konstruo.
- **Stato (2026-10-31)**:
  - **fdroiddata-MR !55129 estas SUBMETITA kaj atendas revizion**:
    https://gitlab.com/fdroid/fdroiddata/-/merge_requests/55129 —
    „New app: EsperantoRadio", 27/28 ŝablonaj kestoj (nur Reproducible
    Builds malmarkita kun kialo), 4 ekranfotoj, ŝanĝoj-tabulo = nur la
    yml, ĉiuj pipelinoj verdaj INKLUZIVE fdroid build (la APK eĉ
    konstruiĝis kaj F-Droid-subskribiĝis sur ilia servilo). La
    prizorgantoj etikedos (New App/review-requested — ekstera kontribuinto
    ne povas mem) kaj revizios, tigoj–semajnoj; **respondu rapide al
    komentoj** (tio decidas la rapidecon).
  - **Sentry-telemetrio** (C48/C49/G44): el F-Droid-uzantoj nur konsentintoj
    plu raportos; la malpliiĝo estas atendata, ne incidento.
- **Stato (2026-11-22)**: la revizianto demandis en la MR-fadeno
  „Why no RB? Provide reason else put Binaries and AllowedAPKSigningKeys in
  yaml and release signed apk in github releases". Ni elektis **plenajn RB**:
  - `BuildConfig.SEN_NIA_SUBSKRIBO` forigita — la telemetria defaŭlto decidiĝas
    RUTEMPE per la subskriba atestilo de la APK
    (`estasSubskribitaPerNiaAtestilo()`, SHA-256-komparo);
  - `dependenciesInfo { includeInApk = false; includeInBundle = false }`;
  - kontrolite: du sendependaj konstruoj de la sama fontstato -> bit-identa
    nesubskribita APK (haketo `9e6acc63…`);
  - RESTAS: venonta eldono kun kongrua versionCode (ekde v3.1.2 re unu
    numero), subskribita APK en GitHub-releases, ĝisdatigita yml
    (+ `AllowedAPKSigningKeys`/`Binaries`), respondo en la MR-fadeno.
    Detaloj: docs/nova/07_eldonado.md -> „Reprodukteblaj konstruoj (RB)".
- **Restas poste**: (1) post mergo de !55129 — 1–3 tagoj ĝis la paĝo
  https://f-droid.org/en/packages/dk.nordfalk.esperanto.radio/ ekvivos, tiam
  „Get it on F-Droid"-emblemo al la README (GVIDILO § 6); (2) Aptoide
  Connect-konto + posedo-pruvo + alŝuto de la subskribita APK; (3) PR #144
  (la beta-branĉo) revizii/fermi. Vidu ankaŭ
  https://github.com/nordfalk/EsperantoRadio/pull/66

#### C51 — Funkcioj de la malnova apo ankoraŭ ne portitaj
- **Statuso**: [ ]
- **Fonto**: README.md "Farota (notoj)" (2026-10-24, movitaj ĉi tien; la referencaj
  ŝlosiloj el `malnova/app/src/main/res/values/eo_strings.xml`). Komplemento al C44.
- **Priskribo**:
  (a) konigi/kunhavigi elsendon (neniu partigo-intento en la nova apo —
  `PlatformLigilo` malfermas nur ligilojn/retpoŝtojn);
  (e) nombro da novaj elsendoj ĉe Plej ŝatataj, kaj nombroj (elŝutoj) + sekva
  alarmtempo en la naviga tirkesto — la malnova tirkesto-skizo montris ilin;
  (f) "Nun estas ludata" por la rekta elsendo — `rektaElsendaPriskriboUrl`
  (muzaiko.info/data.json) estas legata sed ne montrata;
  (g) Pri / Kontakto-ekrano;
  (h) averto ke la vekhorloĝo ne povas garantii kontraŭ teknikaj problemoj.

#### C52 — Ideoj kaj plibonigoj (konsideroj, ne deciditaj)
- **Statuso**: [ ]
- **Fonto**: README.md "Farota (notoj)" (2026-10-24, movitaj ĉi tien)
- **Priskribo**:
  (a) animacioj pli ĉieaj — nun nur la mini-ludilbreto havas; mankas ekz. ĉe
  ŝargado-indikoj, listo-ŝanĝoj (la nunaj ekran-transiroj venis en PR #116);
  (c) transskribo de elsendoj kun tempokodoj, montrataj dum ludado — por lerni
  Esperanton (ideo; la STT-flanko jam funkcias sur la arkiv-servilo — vidu la
  skill-on `servilo-arkivo`; restas la montrado en la apo).
- **Sugesto**: (c) povus esti la unua praktika uzanto de la arkiv-servilo.

#### C55 — Eldoni la komputilan (desktop) version
- **Statuso**: [ ]
- **Fonto**: ideo de la posedanto (2026-10-24); AGENTS.md "Stato" jam listigas desktop-eldonom kiel estontan
- **Priskribo**: `desktopApp` ekzistas kaj konstruiĝas (Gradle 9/AGP 9, PR #117), sed
  estas nenie distribuita. Aparta tasko disde C50 (F-Droid/Aptoide = Android).
- **Sugesto**: decidi eldonformon (Linukso: AppImage/deb/flatpak; Vindozo: porta
  .zip/.exe; makinto?) kaj kanalon (GitHub-Releases estas la plej simpla komenco);
  unue traktu la konatajn limojn (C16: `saltiAl` estas ŝajnigo sur desktop-mp3spi;
  HLS ne funkcias — docs/nova/02) kaj provu sur sen-JDK-aparatoj (jlink/`jpackage`).

#### C56 — Esploro: reta (WASM) versio kun datumoj de la servilo, ludado rekte el la fonto
- **Statuso**: [x] SOLVITA 2026-11-24 (PR #210): beta-retversio gastigata samdevene
  je https://javabog.dk/esperanto-radio/apo/ — nur-arkiva datumvojo (bildoj kaj fluoj
  el la servilo), sono rekte el la fonto. Restas: finfajnigo de la reta UI por la
  eldono (vidu la CHANGELOG-on de PR #210).
- **Fonto**: ideo de la posedanto (2026-10-24); modelo: https://esperanto-radio.com/
- **Priskribo**: esplori ĉu la reta versio povus funkcii tiel, ke la fluo- kaj
  metadatumoj venas de la arkiv-servilo (fazo 1 funkcias; JSON-API = fazo 2 —
  docs/nova/06), sed la ludado mem
  estas reta HTML-komponento kiu ludas rekte de la fonto. Ŝlosila fakto: CORS blokas
  `fetch`/XHR (pro tio plej multaj RSS-fluoj kaj ankaŭ la Coil-bildoj ne ŝargeblas en
  la nuna reta versio), sed **ne** `<audio src>` — la rekt-ludado do funkcias sen
  prokurilo, kaj `WasmJsLudiloRegilo` jam uzas HTML-`<audio>`-elementon. Do:
  datumvojoj (RSS, bildoj) devas veni samdevene (el la servilo aŭ per `<img>`-etikedoj
  anstataŭ Coil-fetch); la sono restas rekt-flua el la fonto (nur anchor.fm-MP3 nun
  funkcias, ĉar ĝi jam estas CORS-permesa).
- **Sugesto**: prototipo post kiam la arkiv-servilo havas JSON-API-on (docs/nova/06):
  samdevena listo de kanaloj/elsendoj → la ekzistanta ŝarga-logiko, kun la bild-URL-oj
  servataj de la servilo aŭ rekte `<img>`-ebligataj. Kvanto-specimenado: mezuru ĉu la
  servila datumvojo povas esti subflue (sen CORS-kapoj) por ĉiuj 26 kanaloj.

#### C58 — "White label"-ligilo por kanal-eldonantoj
- **Statuso**: [ ]
- **Fonto**: ideo de la posedanto (2026-10-24)
- **Priskribo**: ebleco por kanal-eldonanto kreii specialan ligilon al la apo, kiu
  malfermas ĝin tiel, ke ĝuste tiu kanalo staras ĉe la frontpaĝo ("white label" —
  ekz. ManchaPod alvokas siajn aŭskultantojn al "sia" EsperantoRadio).
- **Sugesto**: Android App Links / intenco-ŝablono (ekz. `…/kanalo/<kodo>`) → nav3
  deep-link (hodiaŭ la apo ne havas deep links — C34) → enscenigu `Vojo.KanaloDetalo`
  kiel komenca ekrano (eble kun `Vojo.Hejmo` sube en la stako, por ke reen-gesto
  kondutu nature); por la retversio (https://javabog.dk/esperanto-radio/apo/, ekde la solvo de C56) sufiĉas URL-parametro. Aldonu ankaŭ al la
  Play-Stor-listo ligilojn (`assetlinks.json`-kontrolo) kaj memoru: la kanalo devas
  ekzisti en la agordo (A5).

---

> **Pri la finitaj trovindikoj**: ekde 2026-11-20 ili ne plu listiĝas ĉi tie — vidu
> `git log` de FAROTA.md (la etikedoj K…, G…, C… restas spureblaj), la PR-ojn,
> `CHANGELOG.md`, [`ŜANĜOJ.md`](ŜANĜOJ.md) kaj [`NOTOJ.md`](NOTOJ.md).

#### C60 — Regula (ĉiuhora/ĉiutaga) rulado de la servilaj taskoj — neniu cron ekzistas
- **Statuso**: [ ]
- **Fonto**: esplorado 2026-11-24 (C56): `crontab` je ambaŭ maŝinoj kaj `systemctl list-timers` estis kontrolitaj — NENIU temp-planita rulado ekzistas; ĉio ĝis nun rulis permane. La establon faros la posedanto mem.
- **Priskribo**: nenio aŭtomate refreŝigas la arkivon. La perioda rulado devas kovri almenaŭ:
  - **`:server:arkivKonstruilo`** — RSS-fluoj kaj la publikigota kanalkonfiguro; de PR #210 ĝi ankaŭ elŝutas novajn emblemojn/bildetojn al `publika/bildoj/`. Ekz. ĉiuhore aŭ ĉiutage.
  - **AI-tekstoj** — la STT-pipelino (`server/data/privata/skriptoj/stt_chiujn.py` → `privata/transkriptoj/`) kaj la LLM-prilaboro al pretaj rezultoj (`privata/rezumoj/`). Kompute peza: ekz. ĉiunokte aŭ semajne, kun la limigoj `N_POR_KANALO` kaj `MAKS_DAURO_MIN`.
  - **`rsync`-publikigo** — decidu, ĉu ĝi iĝu parto de la aŭtomato aŭ restu permana. Aktuala regulo: „neniam publikigu al la servilo sen eksplicita peto de la uzanto" — aŭtomata publikigo do bezonas apartan, eksplicitan aprobon de la posedanto (kun `--chmod=D755,F644` kaj `-L` por la ligiloj; vidu la skill-on `servilo-arkivo`).
- **Sugesto**: unu `crontab`-linio sur la konstanta maŝino — tiu kun la kanona datum-arbo (`/home/j/Code/EsperantoRadio/server/data`) — ekz. `5 * * * *` (ĉiuhora) aŭ `17 4 * * *` (ĉiutage), kun protokolo al `tmp/`. Post-vidigo: `stato.json` (`lastaRulo`) kaj https://javabog.dk/esperanto-radio/kanaloj/ estas freŝaj; unu mortinta kanal-fluo ne haltigu la aliajn (A4).

---

> **Pri la finitaj trovindikoj**: ekde 2026-11-20 ili ne plu listiĝas ĉi tie — vidu
> `git log` de FAROTA.md (la etikedoj K…, G…, C… restas spureblaj), la PR-ojn,
> `CHANGELOG.md`, [`ŜANĜOJ.md`](ŜANĜOJ.md) kaj [`NOTOJ.md`](NOTOJ.md).

#### C62 — Apache: X-Content-Type-Options: nosniff por la elŝutejo
- **Statuso**: [x] SOLVITA 2026-11-24: la posedanto aldonis `Header always set
  X-Content-Type-Options "nosniff"` en la `<Directory>`-bloko de la DocumentRoot
  kaj reloadis apache2; kontrolita per curl (200 kaj 404 respondas kun la kapo).
- **Fonto**: CodeRabbit-sekureca recenzo 2026-11-24 (PR #210, CWE-79). La apo-verifikado
  rifuzas ne-bildan embleman enhavon (Emblemoj.formatoSeLegebla, ImageIO-malkodado), sed
  defenda kapo mankas: la servataj bildoj ne havas `X-Content-Type-Options: nosniff`.
- **Priskribo**: `AllowOverride None` en la DocumentRoot (probita, vidu la skill-on
  `servilo-arkivo`) mortigas iun ajn `.htaccess` — do la kapo devas esti en la
  Apache-agordo mem (kiel la mime.types-fikso de .wasm):
  `Header always set X-Content-Type-Options "nosniff"` en la koncerna
  `<Directory>`/vhost, poste `sudo systemctl reload apache2`.
- **Sugesto**: permane farenda de la posedanto sur javabog.dk; kontrolindoj per
  `curl -sI https://javabog.dk/esperanto-radio/bildoj/muzaiko.png | grep -i nosniff`.
