# 7. CRI Esperanto (esperanto.cri.cn) — situacio kaj solvo-propono

> Esplorita: 2026-09-25. Ĉi tiu dokumento priskribas kion ni trovis pri la
> sono-Enhavo de Ĉina Radio Internacia (CRI) en Esperanto, kial ĝi ne povas
> esti aldonita kiel ordinara RSS-kanalo, kaj du eblaj solvojn — inkluzive
> respondon al la demando: ĉu la Androida apo povas ludi la elsendojn sen
> iu ajn ekstra servilo?

## 1. La situacio

**esperanto.cri.cn** estas la Esperanto-eldono de la ŝtata ĉina radio CRI.
Ĝi enhavas ĉiutagajn Esperantajn novaĵojn (sekcio `aktualajo`) kaj
sonprogramojn (ekz. `LuciaStudio`, `eklubo`). LaEnhavo estas regule
ĝisdatigata (ĉiutage), do ĝi estas valora fonto por la apo.

Sed ĝi ne estas podkasto:

| Demando | Respondo |
|---|---|
| Ĉu ekzistas RSS/podkasta fluo? | **Ne.** `/rss`, `/feed` redirektas al la hejmpaĝo; `podcast.cri.cn` ne plu ekzistas; neniom da `.mp3` troveblas en la API-datumoj. |
| Ĉu ekzistas rekta radio-fluo? | **Ne** (por Esperanto). |
| Kio estas la sonformato? | **HLS-video** (`.m3u8`): H.264-video + AAC-sono (`mp4a.40.2`), en pluraj daŭrkokurtaj variantoj (600k–2000k). |
| Kiel la retejo mem ludas ĝin? | Per video.js en Nuxt-unupaĝa apo — la sono estas fakte la sontrako de video. |
| Ĉu la fluoj estas rekte atingeblaj? | **Jes**: sen aŭtentigo, sen geoblokado (testite el Eŭropo, 2026-09), sed nur HLS, ne aparta sono. |

## 2. La (nedokumentita) API

La tuta retejo estas Nuxt-unupaĝa apo, kiu ŝargas ĉion per **unu API-voko**:

```
POST https://esperanto.cri.cn/api/getData
Content-Type: application/json
Korpo: {"id": "<plena URL de paĝo aŭ artikolo>"}
```

Ekzemploj de `id`:

- `https://esperanto.cri.cn/` — hejmpaĝo (kanalo-identigilo: `CHAL1723113653432123`)
- `https://esperanto.cri.cn/aktualajo/page.shtml` — sekcia paĝo (elsendolisto)
- `https://esperanto.cri.cn/2026/09/25/ARTI1790268580629320` — unuopa artikolo

La respondo estas JSON:

```
result
├── title, brief, published (epoko-ms), lang, photo
├── modules[]                      — paĝa fasono
│   └── cardgroups[]
│       └── cards[]                — artikolaj kartoj
│           └── card{ title, link, date, isPlay, brief, photo, published }
└── (ĉe artikoloj)
    ├── video{ url, hdUrl, cdUrl, duration }   — HLS-fluo (VIDE-artikoloj)
    └── content                              — HTML kun enmetita <video …m3u8>
```

- `isPlay: "1"` markas kartojn kun sono/video.
- `published` estas epoko-milisekundoj (ekz. `1790328555000`).
- HLS-gastigantoj: `vcrires.cri.cn` (programoj, `result.video.url`) kaj
  `38vodres.cgtn.com` (sonartikoloj, enmetitaj en `result.content`).

**Ne ĉiuj `isPlay=1`-artikoloj estas Esperantaj** — ekz. `LuciaStudio` enhavas
ankaŭ norvegajn/finnajn/svedajn videojn. Kanalo devas do aŭ limiĝi al
`aktualajo`/`eklubo`, aŭ filtri laŭ lingvo/`lang`-kampo.

## 3. Ĉu la Androida apo povas ludi sen ekstra servilo?

**Ludado: jes. Elsendolisto kaj elŝutoj: ne (sen plia laboro).**

- ExoPlayer (Media3) subtenas HLS denaske — la apo jam ludas la Muzaiko-
  rektan fluon (`https://fluo.muzaiko.info/hls/muzaiko/live.m3u8`) per la
  sama mekanismo. La CRI-m3u8-oj (po 600k–2000k, AAC-sono) do ludeblus sur
  Android sen ia transkodado.
- Sed por alveni al la elsendolisto la apo bezonas RSS, kaj CRI ne havas
  RSS. Sen servilo la apo devus mem parsi la CRI-JSON-API-on — t.e. nova
  **parsregulo 6.8** (POST-voko al `/api/getData` + JSON-mapaado al
  `Elsendo`), kiu nur funkcias por tiu ĉi kanalo.
- Desktop (mp3spi) kaj Web (HTMLAudioElement) **ne povas ludi HLS**. Sen
  servilo la kanalo do estus Android-ekskluziva.
- La elŝut-funkcio (Ktor→`FileOutputStream`) povas elŝuti nur ordinaran
  dosieron, ne HLS-ludliston. Elŝuto de HLS postulus ExoPlayer
  `DownloadService` aŭ apartan HLS-elŝutilon.

**Konkludo:** Android-nura solvo sen servilo estas *ebla* (nova parsregulo +
ExoPlayer-HLS), sed ĝi lasas la kanalon nefunkciema sur 3 el 4 platformoj
kaj sen elŝutoj. La transkoda servo solvas ĉiujn problemojn per unu
kanal-enskribo en la JSONC-agordo — sen ia nova kodo en la apo.

## 4. Solvo-propono: CRI-peranto (transkoda servo)

La plano de `server/` (vidu [06_servilo_arkivo.md](./06_servilo_arkivo.md))
antaŭvidas peranton kiu "normigas la rompiĝemajn fontojn servilflanke".
Regulo 6.3 (peranto-parsilo por archive.org kaj Google Drive) estas la rekta
antaŭulo. CRI fariĝus la tria peranto-kazo:

```
CRI /api/getData  →  Servo: RSS-generatoro  →  GET /cri.rss  →  la apo (elsendojRssUrl)
                          ↓ (laza peto + kaŝmemoro)
                    ffmpeg: HLS → MP3 (64k)  →  GET /cri/audio/<id>.mp3
```

1. **`GET /cri.rss`** — la servo demandas la CRI-API-on (1× hore sufiĉas),
   kolektas `isPlay=1`-artikolojn el la sekcioj, eltiras la m3u8-URL-on
   (el `result.video.url` aŭ per regex el `result.content`) kaj generas
   RSS-2.0 kun `<enclosure>` al sia propra MP3-fino. Esperantaj titoloj,
   `brief` kiel priskribo, `published` kiel `pubDate`, `photo` kiel bildo.
2. **`GET /cri/audio/<id>.mp3`** — transkodas per
   `ffmpeg -i <m3u8> -vn -c:a libmp3lame -b:a 64k` ĉe la unua peto kaj
   kaŝmemorigas la rezulton (artikoloj ne ŝanĝiĝas post publikigo).
   Ĉar la elsendoj estas mallongaj (novaĵoj: 1–3 min; programoj: ~12 min),
   la CPU-kosto estas malgranda; antaŭgenerado per cron estas sufiĉa.
3. **En la apo** — nur unu nova enskribo en
   `esperantoradio_kanaloj_v9.json`:

```jsonc
{
    "kodo": "cri",
    "nomo": "CRI — Ĉina Radio Internacia",
    "elsendojRssUrl": "https://<servo>/cri.rss",
    "emblemoUrl": "https://esperanto.cri.cn/…/emblemo.png",
    "hejmpaĝoButono": "https://esperanto.cri.cn/"
}
```

Neniuj ŝanĝoj en la kodo de la apo: la ĝenerala parsregulo 6.1, la ludiloj,
la elŝutoj, la ludvico kaj la eksterreta ludado tuj funkcias, ĉar por la
apo la kanalo aspektas kiel ordinara MP3-podkasto.

**Kial MP3 kaj ne la pli efika senpera AAC-kopio (`-c:a copy` al `.m4a`)?**
La Desktop-ludilo uzas mp3spi, kiu subtenas nur MP3; Web (HTMLAudioElement)
kaj Android ambaŭ bone ludas MP3-on. 64k MP3 sufiĉas por parolo.

## 5. Demonstra programo: `CriTranskodaDemo`

La pruvitan koncepton oni povas reproducigi per:

```
./gradlew :desktopApp:criTranskodaDemo        # bezonas ffmpeg en $PATH
```

La programo (`desktopApp/src/desktopMain/kotlin/dk/nordfalk/esperanto/desktop/CriTranskodaDemo.kt`)
demontras la tutan servan logikon en unu rulado:

1. Demandas `/api/getData` por la sekcioj `aktualajo`, `LuciaStudio`, `eklubo`.
2. Kolektas la `isPlay=1`-artikolojn, ordigas laŭ `published` kaj prenas
   la **20 plej novajn**.
3. Po artikolo eltiras la m3u8-URL-on kaj transkodas per `ffmpeg` al MP3 64k.
4. Generas `cri_demo.rss` — validan RSS-2.0-fluon, kiun la apo povus rekte
   konsumi per regulo 6.1 (`<item>` kun `title`, `description`, `pubDate`,
   `guid`, `enclosure`, `itunes:duration`, `itunes:image`).
5. Generas `cri_demo.rss` — validan RSS-2.0-fluon, kiun la apo povus rekte
   konsumi per regulo 6.1 (`<item>` kun `title`, `description`, `pubDate`,
   `guid`, `enclosure`, `itunes:duration`, `itunes:image`).
6. Presas resumon de la datumoj.

La transkodado mem elektas la HLS-varianton kun la plej malalta
BANDWIDTH (600 kbps) el la mastra ludlisto — la sono (AAC) estas identa en
ĉiuj variantoj, do elŝuti 2-Mbps-videon estus malŝparo. Parta dosiero
neniam eniras la kaŝmemoron (foriĝas ĉe tempolimo).

Eligo iras al `desktopApp/build/cri-demo/`:
`cri_demo.rss` + `sonoj/<artikol-id>.mp3`. Unu erara fonto ne haltigas la
programon (regulo 4: toleremo al putrantaj fontoj).

**Rezulto de reala rulaĵo (2026-09-25):** 24 unikaj `isPlay=1`-artikoloj
en la tri sekcioj; 20 plej novaj transkoditaj al 42 MB MP3 entute
(49 s–704 s da sono po elsendo); la 20-a (12-minuta programo) bezonis la
plej multan tempon — la elŝut-rapideco de la CRI-CDN al Eŭropo, ne la
rekodado, estas la botelkolo. Tial reala servilo antaŭgeneru per cron.
La generita RSS estas kovrita de ora testo
(`CriPerantoRssTesto`, fiksaĵo `cri_peranto_feed.xml`) kiu certigas ke la
ĝenerala parsregulo 6.1 komprenas ĝin.

## 6. Riskoj kaj avertoj

- `/api/getData` estas **nedokumentita** kaj povas ŝanĝiĝi sen averto. La
  servo devus fiaski milde (malplena RSS, protokolita eraro) — regulo 4.
- CRI estas ŝtata amaskomunikilo; la fluoj povus enkonduki geolimon ekster
  Eŭropo aŭ malaperi. La kaŝmemoro de la servo servas kiel protekto.
- Lingvo-filtrado necesas (ne-Esperantaj programoj en `LuciaStudio`).
- Estu ĝentila al CRI: unu ĝisdatigo hore, kaŝmemoro, reala User-Agent.
- GPL (regulo 6) validas ankaŭ por la servo, se ĝi publike disdonas la
  datumojn — nia peranto estas GPL kaj restas GPL.

## 7. Decidoj kiuj restas malfermaj

1. Ĉu la kanalo montru nur `aktualajo` (ĉiutagaj novaĵoj) aŭ ankaŭ la
   programojn de `LuciaStudio`/`eklubo`?
2. Kie ruli la servon — sur la sama servilo kiel la estonta podkasta
   arkivo (`server/`), aŭ kiel simpla cron-skripto + statikaj dosieroj?
3. Ĉu enkonduki parsregulon 6.8 (CRI-API-parsilo en la apo) kiel
   Android-ekskluzivan retroiron se la servo iam mortos?
