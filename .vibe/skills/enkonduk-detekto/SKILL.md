---
name: enkonduk-detekto
description: Ŝargu ĉi tiun skill-on kiam vi devas esplori/elkalkuli la valoron de `transsaltuKomenceSekundojn` por kanalo(j) — elŝuti la plej freŝajn elsendojn, kompari iliajn komencojn spektrograme kaj rekomendi valoron (FAROTA C59b). Ankaŭ por prilabori la rezultojn en la kanalkonfiguro.
---

# Enkonduk-detekto — ĉu kanalo havas ripetiĝantan enkondukon? (C59b)

## La tasko

La agord-kampo `transsaltuKomenceSekundojn` (FAROTA C59a) transsaltas la
komenco de ĉiu elsendo de kanalo. Tiu ĉi skill-o trovas la valoron empirie:
se la komenco de pluraj elsendoj sonas same, oni transsaltas ĝin ĝis la punkto
kie ili ekmalas.

## Kiel ruli

```bash
# ĉiuj kanaloj, 5 plej freŝaj elsendoj (la kutima kazo):
python3 .vibe/skills/enkonduk-detekto/detekto.py --n 5

# nur kelkaj kanaloj, 3 elsendoj, pli da diagnosego:
python3 .vibe/skills/enkonduk-detekto/detekto.py --kanaloj kernpunkto,humilopinie --n 3 --detale
```

La skripto legas `shared/src/commonMain/resources/esperantoradio_konfiguro.json`,
elŝutas la unuajn 120 s de ĉiu elsendo (8 kHz mono PCM per `ffmpeg`),
kaj presas superrigardon kun rekomendata valoro po kanalo. La PCM-filmetoj
landas en `/tmp/intro-detekto/` (forigeblas libere).

## La metodo (kaj kial ĝuste tiel)

1. **Spektrograma kosino-simileco, NE ondforma korelacio.** Kruda
   specimena korelacio inter du malsamaj MP3-eldonoj fiaskas eĉ por identa
   sono — ĝi estas faz-osentema (kontrol-eksperimento 2026-10-29: fenestro
   kontraŭ si mem ŝovita je 12.5 ms donis −0.16!). Uzu log-magnitudan
   spektrogramon (FFT 512, paso 256 ≈ 32 ms) — faz-nerimarkema.
2. **Ŝovo-serĉo ±15 s** (paŝoj 0.5 s) — la enkondukoj ne komenciĝas je
   identa sekundo (malsamaj antaŭ-anoncoj/silentoj).
3. **Silent-filtrilo**: kadroj kun normo < 0.5 estas preterlasataj
   (silento kontraŭ silento korelacias sensence).
4. **Kvalifikado**: paro kongruas se ekzistas **daŭra ≥0.85-segmento (≥ 3 s)
   en la unuaj 30 s** — ĝi NE bezonas komenciĝi je 0 (la komunaĵo de kernpunkto
   ekas nur post ~8 s da varianta antaŭanonco; 2026-10-29: la unua versio postulis
   kontinuecon ekde 0 kaj malĝuste Rifuzis kernpunkt-on). Kanalo kvalifikas se
   **almenaŭ du trionoj** de la paroj kongruas KAJ la finoj de iliaj komunaj
   segmentoj estas proksimaj (disperso ≤ 3 s). Nervalata parolo restas ĉe
   ~0.4–0.7 — la sojlo 0.85 estas konservativa.
5. **Rekomendo = la fino de la lasta daŭra komuna segmento, minimumo inter
   la paroj** (konservativa suba limo: prefere tro-malmulte transsalti ol
   perdi enhavon). rondigu al 0.5 s.

## Kaptindaĵoj

- **Fluoj sen enclosure** (varsoviavento, P6.2): la sono estas en
  `<audio><source>`-etikedoj ene de la priskribo-HTML — la skripto mem
  eltiras la unuan `.mp3`-URL-on.
- **op3.dev- kaj anchor.fm-tralasoj**: ffmpeg sekvas la alidirektojn —
  funkcias rekte.
- **Mortaj fluoj** (G44): tempo-limo 25 s per fluo, 120 s per elŝuto;
  la skripto protokolas kaj daŭras.
- **Malnovaj eroj en la fluo**: kelkaj fluoj havas ne-sonajn erojn
  (reklamaj pendantaĵoj) — komparu nur la plej freŝajn N.
- La valoro efikas nur por uzantoj, kiuj **ĵus aŭskultis la kanalon**
  (monata kondiĉo, C59a) — novaj aŭskultantoj ĉiam aŭdas la plenan
  enkondukon; tial tro-malgranda valoro estas sendanĝera.

## Post la rulo

1. Kontrolu la superrigordon; enmetu la rekomendatajn valorojn en
   `shared/src/commonMain/resources/esperantoradio_konfiguro.json`
   (`"transsaltuKomenceSekundojn": <valoro>` en la koncerna kanal-objekto).
2. `./gradlew :shared:desktopTest` (la fum-testo validigas la agordon).
3. Ĝisdatigu la C59-eron en `FAROTA.md` kun la rezultoj kaj dato.
