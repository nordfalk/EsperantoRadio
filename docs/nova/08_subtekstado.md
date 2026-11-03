# 8. Subtekstado (transskriboj de podkastoj)

> Priesploro ligita al [issue #142](https://github.com/nordfalk/EsperantoRadio/issues/142)
> (2026-10-31). Ĝis nun la apo havas **nenian** subtekstan subtenon: nek kampo en
> `Elsendo`, nek legado de `podcast:transcript` en la parsilo. Neniu el la 34
> nunaj kanaloj eldonas `podcast:transcript` (kontrolita 2026-10-31).

## Eksperimento — konkretaj rezultoj

Sur loka maŝino (16 kernoj, CPU-nur, plene senrete krom la unufoja model-elŝuto)
oni transskribis 4-minutajn specimenojn (de 0:30) el kvar kanaloj:

| Kanalo / epizodo | Fluo |
|---|---|
| Kernpunkto KP241 "Hungario" | kern.punkto.info |
| Varsovia Vento 199 | podkasto.net |
| Fremdulo 3/09 "Usona Radikala Fremdulo" (la epizodo el la issue) | anchor.fm |
| La Malfamuloj | anchor.fm |

Krome la **plenan** epizodon Fremdulo 3/09 (48:14) — vidu
`ekzemploj/subtekstado/conformer/fremdulo309-PLENA.srt`.

### Tekniko 1: NVIDIA Esperanto Conformer-Transducer Large (INT8 ONNX) — TAŬGA

- Modelo: `OpenVoiceOS/nvidia-eo-conformer-transducer-large-onnx` (141,7 MB,
  sha256-kontrolita elŝuto), licenco CC-BY-4.0, fajntunita sur Mozilla Common
  Voice Esperanto 11.0. Senpaga ilo ĉirkaŭ ĝi: `BitsAndDopamine/Esperanto-SRT`
  (CC0) — la fonton de la modelo kaj la chunk-transskribon uzis la eksperimento.
- Rapido: 4-minuta specimeno en ~12 s (≈20x reala tempo); plena 48-minuta
  epizodo en 252 s (≈11,5x reala tempo), 495 segmentoj kun vort-tempostampoj.
- Kvalito: ĝusta esperanta teksto **kun supersignoj**, minuskla kaj sen punktigo
  (konata limigo; vort-erara ofteco ~5 % laŭ la aŭtoro, dependas de sonkvalito).
  Ekz. el Fremdulo 3/09: „saluton kaj bonvenon al fremdulo tri hodiaŭ mi havas
  la honoron prezenti al vi apartan specialan epizodon…“
- Rezultoj: `ekzemploj/subtekstado/conformer/*.srt`

### Tekniko 2: Whisper-small (faster-whisper, aŭtodetekto) — NE TAŬGA

- Whisper **ne konas Esperanton**: `eo` ne estas valida lingvokodo en Whisper;
  aŭtodetekto identigis la lingvon kiel latina (`la`) resp. hispana (`es`).
- Eligo estas fonetika rubaĵo, ekz. „Mi shategas ambau tyun podkastań“,
  „Saluton, dankon ge mi reitas e stichetia“ — kaj krome pli malrapida
  (~60 s por 4 min).
- Rezultoj: `ekzemploj/subtekstado/whisper-small/*.srt`

### Tekniko 3: eligoformatoj kaj integriĝo

- SRT → VTT-konverto: `ekzemploj/subtekstado/vtt/*.vtt`
- PodcastIndex JSON-transskribo (`application/json`):
  `ekzemploj/subtekstado/fremdulo-309-4min.json`
- RSS-ekzemplo kiel la arkiv-servilo aldonus `podcast:transcript`
  (`application/srt` + `text/vtt`, `language="eo"`) al ero 3/09:
  `ekzemploj/subtekstado/fremdulo-3-09-rss-frammento.xml`

## Konkludo kaj rekomendo

1. Por Esperanto la sola praktika malferma STT-opcio estas la **dediĉita
   NVIDIA Esperanto-modelo** (INT8 ONNX); Whisper estas senutila sen `eo`.
2. Ne sur la aparato (142 MB-modelo, CPU-intensa) — sed **servilflanke** en la
   planita arkiv-servilo (`06_servilo_arkivo.md`): post elŝuto de ĉiu epizodo
   ruli la modelon, generi SRT/VTT kaj servi per `podcast:transcript` en la
   arkivaj fluoj. Tiel også homaj transskriboj de podkastistoj estos subtenataj
   sen kroma kodo.
3. Klienta paŝo poste: nova kampo `transskriboUrl` en `Elsendo`, legado de
   `podcast:transcript` en `RssParsilo`, montrado en la ekrano `ElsendoDetalo`.
4. Malferma punkto: punktigo kaj kapitaligo de la minuskla eligo — necesas
   postprilaboro per reguloj aŭ malgranda LLM (ankoraŭ ne esplorita profunde).
