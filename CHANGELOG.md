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

## [Neeldonita] — iĝos 3.0.1

### Aldonita

- Eksponenta reprovo: pasemaj ludo-eraroj reproviĝas aŭtomate (ĝis 10 fojojn) (https://github.com/nordfalk/EsperantoRadio/pull/68)
- Malsupren-tiro por refreŝigi sur Hejmo, Kanaloj kaj kanalvido (https://github.com/nordfalk/EsperantoRadio/pull/69)
- Vekhorloĝo ludas la plej freŝan neaŭskultitan podkaston de la kanalo, ne nur la rektsendon (https://github.com/nordfalk/EsperantoRadio/pull/68)
- Funkcianta retumila versio de la apo (wasmJs) (https://github.com/nordfalk/EsperantoRadio/pull/71)
- Averto kun "Permesi"-butono kiam mankas la permeso por ekzaktaj alarmoj (https://github.com/nordfalk/EsperantoRadio/pull/70)

### Riparita

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
[Neeldonita]: https://github.com/nordfalk/EsperantoRadio/compare/294834b...master
[3.0.0]: https://github.com/nordfalk/EsperantoRadio/compare/fresxa_versio...294834b
