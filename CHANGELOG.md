# Ŝanĝoprotokolo (CHANGELOG)

Ĉiuj rimarkindaj, uzantvideblaj ŝanĝoj de EsperantoRadio.

Unu linio po ŝanĝo: konciza priskribo + ligilo al la PR. Sen teknikaj detaloj —
tiuj staras en [`ŜANĜOJ.md`](ŜANĜOJ.md) kaj en la PR-priskriboj.
La versioj sekvas `apoversio` en [`gradle/libs.versions.toml`](gradle/libs.versions.toml);
la formato estas [Keep a Changelog](https://keepachangelog.com/1.1.0/) kun
kategorioj **Aldonita**, **Ŝanĝita**, **Riparita**, **Forigita**, **Sekureco**.

La protokolo komenciĝas ĉe 3.0.0. La historio de la malnova apo (2.x, ĝis
2.0.12f) estas en la git-historio; ĝian finan eldonaĵon markas la etikedo
[`fresxa_versio`](https://github.com/nordfalk/EsperantoRadio/releases/tag/fresxa_versio).

## [Neeldonita]

## [3.0.4-beta24okt] - 2026-10-24

### Aldonita

- Vekhorloĝo: la tempo elekteblas per ciferblido en dialogo (kun eblo klavare enigi) kaj la kanalo per kanal-elektilo (https://github.com/nordfalk/EsperantoRadio/pull/114)
- Hejmo: klako sur la sekcio-titolo „Kanaloj“ kondukas al la kanala vido (https://github.com/nordfalk/EsperantoRadio/pull/131)

### Ŝanĝita

- Sciigoj pri novaj elsendoj ne plu amasiĝas: maksimume 7 samtempe kaj maksimume 3 po kanalo — kiam la limoj atingiĝas, la plej malnovaj sciigoj malaperas (https://github.com/nordfalk/EsperantoRadio/pull/130)
- Dekstra tro-tiro sur la horizontalaj vicoj de la ĉefekrano (kiam la vico ne povas ruli pli reen) malfermas la navigan tirkeston — ĝi sekvas la fingron kaj solviĝas kiel la normala tirkesto-gesto (https://github.com/nordfalk/EsperantoRadio/pull/131)
- La mini-ludilbreto restas videbla ankaŭ kiam nenio ludas: ĝi montras la laste vidatan kanalon/elsendon (sekvante svipojn) aŭ la laste ludatan enhavon kun ludi-butono kaj ne ŝanĝas la enhavon je nura navigado — reen-iro al la ĉefekrano ne plu ŝanĝas la montratan elsendon; klako sur la breto ludas la enhavon kiam oni jam estas sur ĝia ekrano — kanalo kun rekta fluo ludas la fluon, podkasta kanalo ludas sian plej novan elsendon (https://github.com/nordfalk/EsperantoRadio/pull/131)
- La „Apaj novaĵoj"-dialogo ne plu aperas en evoluaj medioj (debug-konstruoj, emuliloj, komputilaj kaj retumilaj ruladoj) — antaŭe ĝi malfermiĝis ĉe ĉiu freŝa instalaĵo kaj ĝenis dum programado (https://github.com/nordfalk/EsperantoRadio/pull/125)
- Elsendoj ricevas stabilajn identigilojn (sen kreskantaj prefiksoj); pro la ŝanĝo ludpozicioj, elŝutoj kaj plej ŝatataj povas unufoje restartiĝi por iuj kanaloj (https://github.com/nordfalk/EsperantoRadio/pull/122)
- La malsupra naviga breto estas anstataŭigita per naviga tirkesto kiel en la malnova apo: ĝi malfermiĝas per la hamburgero en la supra breto (kiu morfas al reen-sago kaj reen per animacio, kaj kies tirkesto glitas enen kaj elen) aŭ per tiro de la maldekstra rando, kaj entenas ĉiujn ekranojn — Hejmo, Kanaloj, Plej ŝatataj, Serĉi, Elŝutoj, Vekhorloĝo kaj Agordoj; la supraj bretoj kun titoloj kaj agoj restas, la mallarĝa tirkesto malfermiĝas sub la supra breto kaj havas neniun propran titolon (https://github.com/nordfalk/EsperantoRadio/pull/120)
- Pli fluida navigado: langet-ŝanĝoj transiras per fado kaj detalaj ekranoj glitas horizontale — ankaŭ sur komputilo kaj retumilo, kie antaŭe ekranŝanĝo estis tute senmovigita (https://github.com/nordfalk/EsperantoRadio/pull/116)
- Unueca navigado sur flankaj ekranoj: Elŝutoj, Ludvico kaj Diagnozo estas nun tutekranoj sen suba naviga breto, same kiel Agordoj kaj Alarmoj (https://github.com/nordfalk/EsperantoRadio/pull/116)
- Ripeta klako (ekz. sur la mini-ludilbreto) ne plu povas malfermi du identajn ekranojn super unu la alia (https://github.com/nordfalk/EsperantoRadio/pull/116)
- Vekhorloĝo: la alarmoj ne plu montras fiksigitajn etikedojn (ekz. "Muzaiko semajnfine") kiuj ne kongruis kun la enhavo — la priskriba teksto nun generiĝas el la agordo de la alarmo (kanalo + ripetotagoj) (https://github.com/nordfalk/EsperantoRadio/pull/114)
- Debug-konstruoj, komputilaj ruladoj (desktop) kaj emuliloj ne plu sendas ion ajn al la erar-monitorado — nur veraj uzantoj de la eldonitaj versioj (https://github.com/nordfalk/EsperantoRadio/pull/109)
- La protokola kvoto montriĝis ne-problema: "detala protokolado" estas nun defaŭlte ŝaltita, do ankaŭ la ordinaraj protokoloj (navigado, klakoj) denove sendiĝas; malŝaltebla en Agordoj → Privateco (https://github.com/nordfalk/EsperantoRadio/pull/109)
- Erar-monitorado: eraroj estas duone specimenataj kaj atendantaj fiaskoj (reto, ludado, elŝuto) ne plu kalkuliĝas kiel eraroj — la preskaŭ elĉerpita monata eraroj-kvoto estas tiele protektata (https://github.com/nordfalk/EsperantoRadio/pull/109)

### Riparita

- Kaŝita averto-karto (ekz. "Bateri-optimumado estas ŝaltita") ne plu reaperas post ĉiu relanĉo de la apo (https://github.com/nordfalk/EsperantoRadio/pull/132)
- La kanal- kaj elsendolistoj ne plu reŝargiĝas je ĉiu ekran-vizito — ŝargadoj dedupliĝas, havas 5-minutan aĝ-limon, kaj la ŝarĝindikilo aperas nur kiam efektive ŝargiĝas (https://github.com/nordfalk/EsperantoRadio/pull/129)
- La apo ne plu kraŝas pro manko de memoro kiam kanal-fluo redonas tro grandan aŭ misdirektitan respondon (ekz. son-dosieron anstataŭ RSS-fluo) — la legado estas limigata al 1 MB (https://github.com/nordfalk/EsperantoRadio/pull/128)
- Malofte la apo tute kraŝis, kiam Android restartigis la fonan ludado-servon sen aktiva ludado (ekz. post kiam la sistemo liberigis memoron) (https://github.com/nordfalk/EsperantoRadio/pull/124)
- La alarmo-sciigo (daŭra dum la alarmo sonas) estas nun nuligata kiam la ludado haltiĝas aŭ finiĝas — antaŭe ĝi restis en la ombrumo por ĉiam
- La rezerva alarm-sono (kiam la kanalo ne disponeblas) estas nun haltigebla per "Halti" aŭ per tuŝo de la sciigo kaj mem ĉesas post 5 minutoj — antaŭe ĝi sonis senfine

## [3.0.3] - 2026-10-12

### Aldonita

- Vekhorloĝo: se ĉiuj elsendoj de la kanalo estas jam aŭskultitaj, la alarmo nun reludas la plej novan sane ludeblan anstataŭ eble eraran elsendon (https://github.com/nordfalk/EsperantoRadio/pull/95)
- La averto pri detektitaj problemoj sur la ĉefekrano havas "x"-butonon por kaŝi ĝin; ĝi kondukas al la agordoj, kie la averto ĉiam restas videbla ĉe "Diagnozo". Kiam nova problemo aperas, la averto reaperas sur la ĉefekrano (https://github.com/nordfalk/EsperantoRadio/pull/105)
- Novaĵoj: kiel en la malnova apo, grava sciigo pri la apo (ekz. versio-anonco aŭ ke kanalo ne funkcias) montriĝas kiel dialogo unufoje kiam ĝi ŝanĝiĝas (https://github.com/nordfalk/EsperantoRadio/pull/106)
- Nova agordo "Ŝaltu detalan protokoladon" (Agordoj → Privateco): defaŭlte nur eraroj kaj gravaj eventoj (malfermo de la apo, ekludo de elsendo, vekiĝo) sendiĝas al la erar-monitorado; ŝaltu por sendi ankaŭ la ceterajn protokolajn informojn (https://github.com/nordfalk/EsperantoRadio/pull/107)

### Ŝanĝita

- Novaj ekranfotoj kaj priskribo por Google Play: 8 ekranfotoj el la nova apo (anstataŭ 3 malnovaj) kaj tute nova priskribo kun listo de ĉiuj 27 kanaloj (https://github.com/nordfalk/EsperantoRadio/pull/108)
- Vekhorloĝo: la permesoj `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` kaj `USE_FULL_SCREEN_INTENT` estas forigitaj (laŭ postulo de Google Play). Sur Android 12+ alarmoj povas malfrui ĝis 10 minutoj kaj la ekrano ne vekiĝas aŭtomate — la sono ĉiam ludas. Sur Android 11 kaj sube ĉio funkcias same kiel antaŭe.

### Riparita

- La vekhorloĝo nun ludas tuj je la ĝusta tempo ankaŭ kiam la telefono dormas kun malŝaltita ekrano — antaŭe ĝi atentis ĝis vi mem vekis la telefonon (https://github.com/nordfalk/EsperantoRadio/pull/94)
- Ne plu perdiĝas jam elŝutita kanal-enhavo kiam la servilo respondas per eraro-paĝo aŭ kaptiva portalo (https://github.com/nordfalk/EsperantoRadio/pull/89)
- La sciigoj pri novaj elsendoj nun funkcias ankaŭ kiam la apo estas fermita (la fonprocezo antaŭe havis neniun kuntekston) (https://github.com/nordfalk/EsperantoRadio/pull/89)
- Sur Android la ludado ne plu povas rekomenciĝi per si mem post kiam oni premis "Halti" (https://github.com/nordfalk/EsperantoRadio/pull/89)
- Klako sur ero en la ludvico nun malfermas la elsendon (antaŭe la klako faris nenion) (https://github.com/nordfalk/EsperantoRadio/pull/89)
- Alarmo-sugestoj montras ĝustajn supersignojn (ĥ, Ŝ, Ŭ ktp.) (https://github.com/nordfalk/EsperantoRadio/pull/89)
- Kanaloj kun plej-malnovaj-unue fluoj (ekz. Internacia Retradio) montras la plej novajn elsendojn supre kaj aŭtoludas en la ĝusta direkto (https://github.com/nordfalk/EsperantoRadio/pull/89)
- Triligitaj elŝutoj ne plu estas konservataj kiel "pretaj"; partaj dosieroj estas forigataj post eraro, paŭzo kaj restarto (https://github.com/nordfalk/EsperantoRadio/pull/89)
- La fona aŭtoludo ne plu povas erare marki la sekvan elsendon "fininta" kiam la servo kaj la ludilo ambaŭ raportas la saman finon (https://github.com/nordfalk/EsperantoRadio/pull/89)
- Priskriboj de Varsovia Vento estas sen reklamaj alineoj kaj enkonstruitaj son-ludiloj (kun la vera enhavo nedifektita) (https://github.com/nordfalk/EsperantoRadio/pull/91)
- Vinilkosmon montras la verajn titolojn de la elsendoj kaj priskribojn sen la "has posted"-alineo (https://github.com/nordfalk/EsperantoRadio/pull/91)
- Elsendoj kun sono nur en la priskribo (aŭ ĉe video-gastigantoj kiel YouTube) ne plu rompas la kanalan liston (https://github.com/nordfalk/EsperantoRadio/pull/91)
- Sur la reto (web) la progreso-linio moviĝas dum ludado kaj la pozicio ĝuste konserviĝas (https://github.com/nordfalk/EsperantoRadio/pull/92)
- Sur la komputilo (desktop) daŭrigi post paŭzo ne rekomencigas la pozicion de nul (https://github.com/nordfalk/EsperantoRadio/pull/92)
- Vekhorloĝo sonas ankaŭ kiam la sistemo blokas la lanĉon de la aplikaĵo (plenekrana sciigo + sistema son-retrofalo) (https://github.com/nordfalk/EsperantoRadio/pull/92)
- La serĉo ne plu retropasas malnovajn rezultojn kaj toleras erarojn; la serĉteksto kaj la alarmo-redaktilo plu restas post ekranturno
- Alarmo-minuto kaj -horo malfermas ciferecan klavaron

### Sekureco

- HTTP (klara teksto) estas nun permesata nur por la heredaĵaj gastigantoj kiuj bezonas ĝin — ne por la tuta trafiko (https://github.com/nordfalk/EsperantoRadio/pull/89)
- Fluo- kaj bildligiloj el la radi-fluoj estas kontrolataj: nur http/https estas ludebla (ne ekz. file://) (https://github.com/nordfalk/EsperantoRadio/pull/89)
- Falsitaj sciigo-intencoj de aliaj aplikoj estas rifuzataj (privata po-instala kodo) (https://github.com/nordfalk/EsperantoRadio/pull/89)
- Ligiloj en la elsendo-priskriboj malfermiĝas ekstere, ne ene de la priskribo-vido; danĝeraj skemoj (intent://, data:, javascript:) estas forigitaj (https://github.com/nordfalk/EsperantoRadio/pull/89)

### Aldonita

- 7 novaj podkastaj kanaloj: Fremdulo 3, Usone Persone, Ne Parolu pri Esperanto, Radikala tenero, Esperanto Stories, Le Monde diplomatique en Esperanto kaj Rakonta Tempo (https://github.com/nordfalk/EsperantoRadio/pull/83)
- 2 pliaj kanaloj: Internacia Retradio (IRo) kaj Esp. Magazino Tuluzo (https://github.com/nordfalk/EsperantoRadio/pull/86)
- La ludilbreto kolapsas per reen-butono aŭ klako ekstere, kaj sekvas la navigadon kiam nenio ludas — montras la nunan kanalon/elsendon kun ludi-butono (https://github.com/nordfalk/EsperantoRadio/pull/87)
- Mallongaj priskriboj de ĉiuj kanaloj videblaj en la kanalaro kaj en la kanala paĝo (https://github.com/nordfalk/EsperantoRadio/pull/88)
- Luditaj elsendoj malaperas de "Kio novas" kaj aperas en "Lastatempe ludata" nur kiam la ludado vere ekis; la nune ludata karto restas en "Kio novas" (https://github.com/nordfalk/EsperantoRadio/pull/85)
- "Kio novas" montras maksimume 5 elsendojn po kanalo por lasi spacon al aliaj (https://github.com/nordfalk/EsperantoRadio/pull/85)
- Varsovia Vento kaj Kernpunkto estas defaŭlte ŝatataj (sciigoj pri novaj elsendoj) (https://github.com/nordfalk/EsperantoRadio/pull/85)

## [3.0.2] - 2026-10-08

### Aldonita

- Elfaldebla ludilbreto, ŝovebla serĉbreto, reen-/antaŭen-butonoj kaj laŭteco-regilo, kiel en la malnova apo (https://github.com/nordfalk/EsperantoRadio/pull/77)
- Nova diagnoza ekrano (Agordoj → Sistemo → Diagnozo) kontrolas la aparaton por oftaj problemoj (bateri-optimumado, sciig-permeso, ekzaktaj alarmoj, DNS) kaj gvidas al la ĝustaj sistem-agordoj (https://github.com/nordfalk/EsperantoRadio/pull/79)
- Avertosigno pri detektitaj problemoj aperas sur la ĉefekrano kaj kondukas rekte al la diagnozo (https://github.com/nordfalk/EsperantoRadio/pull/79)

### Ŝanĝita

- Elsendoj ne rezignas tuj kiam arkiva servilo eraras (HTTP 5xx): la ludvico reprovas anstataŭ salti al la sekva (https://github.com/nordfalk/EsperantoRadio/pull/76)
- Frontpaĝo: la kanala vico montras unue la aktivajn kanalojn (kun elsendoj dum la pasinta jaro), poste dividilon "Arkivo" kaj la kanalojn kies lasta elsendo estas pli aĝa ol unu jaro; la flava markilo ĉiam montras la aĝon de la elsendo — sur kanaloj tiun de la plej nova elsendo, neniam la ludprogreson (https://github.com/nordfalk/EsperantoRadio/pull/78)
- Aŭtomata daŭrigo al la sekva elsendo funkcias ankaŭ kiam la apo estas en la fono (la servo mem lanĉas la sekvan elsendon) (https://github.com/nordfalk/EsperantoRadio/pull/79)

## [3.0.1] - 2026-09-26

### Aldonita

- La versio de la apo nun estas videbla en la agordoj (https://github.com/nordfalk/EsperantoRadio/pull/74)
- Eksponenta reprovo: pasemaj ludo-eraroj reproviĝas aŭtomate (ĝis 10 fojojn) (https://github.com/nordfalk/EsperantoRadio/pull/68)
- Malsupren-tiro por refreŝigi sur Hejmo, Kanaloj kaj kanalvido (https://github.com/nordfalk/EsperantoRadio/pull/69)
- Vekhorloĝo ludas la plej freŝan neaŭskultitan podkaston de la kanalo, ne nur la rektsendon (https://github.com/nordfalk/EsperantoRadio/pull/68)
- Funkcianta retumila versio de la apo (wasmJs) (https://github.com/nordfalk/EsperantoRadio/pull/71)
- Averto kun "Permesi"-butono kiam mankas la permeso por ekzaktaj alarmoj (https://github.com/nordfalk/EsperantoRadio/pull/70)

### Riparita

- "Malapero de aktoro Benda (3/3)" kaj 6 pluaj Esperanta Retradio-elsendoj ne ludeblis: la apo divenis la dosiernomon ĉe archive.org anstataŭ demandi (https://github.com/nordfalk/EsperantoRadio/pull/75)
- La mini-ludilbreto malaperis post rekreo de la aktiveco, kaj nova ludado silente ne funkciis (https://github.com/nordfalk/EsperantoRadio/pull/68)
- La "Ludi"-butono en sciigoj pri novaj elsendoj neniam funkciis (kraŝis) (https://github.com/nordfalk/EsperantoRadio/pull/68)
- La Muzaiko-livestream ne ludis sur Android (https://github.com/nordfalk/EsperantoRadio/pull/68)
- Sen la permeso por ekzaktaj alarmoj, alarmoj tute ne ekigis — nun maksimume 10 minutojn malfrue (https://github.com/nordfalk/EsperantoRadio/pull/70)
- La alarmo-redaktilo perdis la etikedon de la alarmo ĉe konservado (https://github.com/nordfalk/EsperantoRadio/pull/70)
- Unufoja alarmo restis ŝaltita post ekigo kaj re-ekigis la sekvan tagon (https://github.com/nordfalk/EsperantoRadio/pull/70)
- Unu mortinta kanal-fluo blokis la ŝargadon de ĉiuj aliaj kanaloj (https://github.com/nordfalk/EsperantoRadio/pull/71)

## [3.0.0] - 2026-09-21

Kompleta reverko: la nova plursistema Compose-apo anstataŭigas la malnovan
DR-Radio-derivitan Android-aplon (2.x).

### Aldonita

- Nova plursistema apo: Android kaj Desktop el komuna KMP-kodo, pretas por Web kaj iOS
- Kanalaro kun 26 radiaj/podkastaj fontoj, kun emblemoj
- Kanalvido kaj elsendodetalo kun horizontala svipo kaj riĉa HTML-vidigo
- Ludado de rektsendoj kaj podkastoj, kun "Daŭrigi de X:XX"
- Ludvico kun aŭtomata sekva-ludado kaj "Lastatempe ludata"
- Elŝutoj kun eksterreta ludado
- Vekhorloĝo kun ripetantaj alarmoj kaj sugestoj
- Sciigoj pri novaj elsendoj de ŝatataj kanaloj
- Serĉo, plejŝatataj kanaloj kaj agordoj
- Erarmonitorado per Sentry.io

### Forigita

- La malnova apo (2.x) — anstataŭigita de la KMP-apo; la kodo restas en `malnova/` por referenco

<!--
Kompar-ligoj por Keep a Changelog. kiam vi eldonas version, aldonu:
[X.Y.Z]: https://github.com/nordfalk/EsperantoRadio/compare/vANTAŬA...vX.Y.Z
Post kiam la etikedo v3.0.0 estos kreita sur la eldona komito 294834b,
anstataŭigu la haketaĵon per la etikedo:
-->
[3.0.4-beta24okt]: https://github.com/nordfalk/EsperantoRadio/compare/v3.0.3...v3.0.4-beta24okt
[3.0.3]: https://github.com/nordfalk/EsperantoRadio/compare/v3.0.2...v3.0.3
[3.0.2]: https://github.com/nordfalk/EsperantoRadio/compare/v3.0.1...v3.0.2
[3.0.1]: https://github.com/nordfalk/EsperantoRadio/compare/294834b...master
[3.0.0]: https://github.com/nordfalk/EsperantoRadio/compare/fresxa_versio...294834b
