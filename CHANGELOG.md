# Ŝanĝoprotokolo (CHANGELOG)

Ĉiuj rimarkindaj, uzantvideblaj ŝanĝoj de EsperantoRadio.

Unu linio po ŝanĝo: konciza priskribo + ligilo al la PR. Sen teknikaj detaloj —
tiuj staras en [`SXANGXOJ.md`](SXANGXOJ.md) kaj en la PR-priskriboj.
La versioj sekvas `apoversio` en [`gradle/libs.versions.toml`](gradle/libs.versions.toml);
la formato estas [Keep a Changelog](https://keepachangelog.com/1.1.0/) kun
kategorioj **Aldonita**, **Ŝanĝita**, **Riparita**, **Forigita**, **Sekureco**.

La protokolo komenciĝas ĉe 3.0.0. La historio de la malnova apo (2.x, ĝis
2.0.12f) estas en la git-historio; ĝian finan eldonaĵon markas la etikedo
[`fresxa_versio`](https://github.com/nordfalk/EsperantoRadio/releases/tag/fresxa_versio).

## [Neeldonita]

### Aldonita

- Vekhorloĝo: se ĉiuj elsendoj de la kanalo estas jam aŭskultitaj, la alarmo nun reludas la plej novan sane ludeblan anstataŭ eble eraran elsendon (https://github.com/nordfalk/EsperantoRadio/pull/95)

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
[3.0.2]: https://github.com/nordfalk/EsperantoRadio/compare/v3.0.1...v3.0.2
[3.0.1]: https://github.com/nordfalk/EsperantoRadio/compare/294834b...master
[3.0.0]: https://github.com/nordfalk/EsperantoRadio/compare/fresxa_versio...294834b
