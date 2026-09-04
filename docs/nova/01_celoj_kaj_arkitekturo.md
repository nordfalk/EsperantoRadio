# 1. Celoj kaj arkitekturo

## Celoj

1. **Konservi ĉiun funkcion** de la malnova apo (kanaloj, livestreno, podkastoj,
   elŝutoj, plej ŝatataj, lastaŭskultitaj, serĉo, vekhorloĝo).
2. **Konservi la pars-scio** — la sep parsregoloj, skip-listo, kanalkonfiguro
   (vidu `04_parsado_kaj_arkivo.md`).
3. **Plursistema** — Android + iOS + Desktop (JVM) + Web (Wasm). Funkcioj
   malfacilaj sur iu platformo estas preterlasitaj tie ("graceful degradation").
4. **MVP unue** — kerno (kanaloj, ludado, serĉo, plejŝatataj) antaŭ pezaj
   platform-funkcioj (vekhoro, Chromecast, hejmeekrana widget, TTS).
4. **Toleremeco al putrantaj fontoj** — unu fonto-eraro ne paneas la apot.
5. **Testebleco** — la datentavolo rulas en pura JVM/KMP-testo kontraŭ golden
   fixtures, sen reto kaj sen Android.
6. **Dateno-movita konfiguro** — per-kanalaj apartaĵoj en agordo, ne en logiko.
7. **Moderna, Muzaiko-inspirita dizajno** (vidu `05_dizajno_kaj_ui.md`).

## Principoj

- **Pura arkitekturo** — dependaĵoj montras internen (UI → domajno ← dateno).
- **Komuna kode plej multe** — platform-specifa kodo nur por ludado, sciigoj,
  elŝutoj, vekhorloĝo, ktp.
- **Reaktiva stato** — Kotlin Flow/StateFlow, unu-direkta datumfluo.
- **Grasa kliento, maldika servilo (unue)** — la apo povas funkcii sen la servilo,
  sed la servilo estas "sekureca valvo" por rompiĝemaj fontoj.

## Tavoligita arkitekturo

```
Prezento (Compose UI, ViewModel, Stato, Navigado)
    ↑ dependas de
Domajno (Uzkazoj, Domajnmodeloj, Logiko, Entitoj)
    ↑ dependas de
Datumoj (Deponejoj, Datumfontoj, Reto, Kaŝmemoro, Stokado)
    ↑ dependas de
Platformo (Android/iOS/Desktop-specifaj implementaĵoj)
```

### Respondeco de ĉiu tavolo

| Tavolo | Respondeco |
|---|---|
| **Prezento** | Compose-ekranoj, ViewModel-oj, UI-stato, navigado, temo |
| **Domajno** | Purkotlinaj modeloj, deponej-interfacoj, uzkazoj, komerca logiko |
| **Datumoj** | Reto (Ktor), parsado, kaŝmemoro, lokala stokado, deponej-implementaĵoj |
| **Platformo** | Sonludado, sciigoj, elŝutoj, vekhorloĝo, malfona servo |

## Modulo-strukturo (Kotlin Multiplatform)

```
EsperantoRadioMultiplatform/
├── shared/                          # Komuna KMP-modulo
│   ├── src/
│   │   ├── commonMain/kotlin/dk/nordfalk/esperanto/
│   │   │   ├── app/                 # eniro, komponado de dependencaĵoj (permana)
│   │   │   ├── common/              # komunaj utilaĵoj
│   │   │   ├── data/                # kaŝmemoro, dto, mapilo, reto, deponejo, fonto
│   │   │   │   ├── network/         # Ktor-kliento, RSS-parsilo
│   │   │   │   ├── parser/          # la sep parsregoloj (04_parsado_kaj_arkivo.md)
│   │   │   │   ├── repository/      # deponej-implementaĵoj
│   │   │   │   └── cache/           # kanal-kaŝmemoro
│   │   │   ├── domain/              # model, repository (interfacoj), usecase
│   │   │   └── ui/                  # komuna Compose (komponantoj, temo, navigado)
│   │   ├── commonTest/              # golden-testoj (kontraŭ fiksaĵoj)
│   │   ├── androidMain/             # platform/, di/
│   │   ├── iosMain/                 # platform/
│   │   ├── desktopMain/             # platform/ (JVM)
│   │   └── wasmJsMain/             # platform/ (Web)
│   ├── src/commonTest/resources/feeds/  # frostigitaj golden-fiksaĵoj
│   └── build.gradle.kts
├── androidApp/                      # MainActivity, res/, AndroidManifest
├── iosApp/                          # App.swift, ContentView.swift, Info.plist
├── server/                          # podkasta arkiv-servilo (06_servilo_arkivo.md)
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

### Kial tiu strukturo

- La **parsado** vivas en `shared/data/parser/` — komuna, pura, testebla.
- La **UI-temo kaj komunaj komponantoj** vivas en `shared/ui/` — reuzeblaj
  trans Android/iOS/Desktop.
- Platform-specifaj aferoj (ExoPlayer, AVPlayer, sciigoj) vivas en
  `androidMain`/`iosMain`/`desktopMain` kiel `expect`/`actual`.
- La **servilo** estas aparta modulo — povas esti Ktor-server-aplikaĵo aŭ
  Kotlin-script (vidu `06_servilo_arkivo.md`).

## Kion konservi el la malnova apo, kion reenrigardi

| Konservi (kono) | Reenrigardi (implementaĵo) |
|---|---|
| La kanallisto kaj agordo (`esperantoradio_kanaloj_v9.json`) | RssArkivServer Java-jar |
| La sep parsregoloj | Fragment-bazita UI |
| La skip-listo de neeltireblaj gastigantoj | Singletona `App`-stato |
| radio.txt-formato kaj antaŭeco | Volley |
| archive.org/Google Drive-skraptrikoj | ExoMedia |
| Kernpunkto-https-korekto | AndroidQuery |
| Varsovia-Vento-plurparto | Malmolaj per-kanalaj apartaĵoj en kodo |
| Golden fixtures (`RssArkivServer-filcache/`) | |

## Plursistema cel-matrico

| Funkcio | Android | iOS | Desktop | Web |
|---|:--:|:--:|:--:|:--:|
| Kanalaro, elsendlistoj, detalo | ✅ | ✅ | ✅ | ✅ |
| Livestream + podkast-ludado | ✅ | ✅ | ✅ | ✅ |
| Serĉo, plejŝatataj, lastaŭskultitaj | ✅ | ✅ | ✅ | ✅ |
| Live "nun ludas"-metadateno | ✅ | ✅ | ✅ | ✅ |
| Malfona ludado + mediasciigo | ✅ | ✅ | ➖ | ❌ |
| Mediabutonoj / kapaŭskultilo / alvok | ✅ | ✅ | ➖ | ➖ |
| Elŝutoj (eksterrete) | ✅ | ✅ | ✅ | ❌ |
| Hejmekrana/ŝlosoekrana widget | ✅ | ➖ | ❌ | ❌ |
| Vekhorloĝo | ✅ | ❌ | ➖ | ❌ |
| Chromecast | ✅ | ➖ | ❌ | ❌ |
| Talesyntezo (kanalŝanĝo) | ✅ | ✅ | ✅ | ➖ |

✅ = en celo · ➖ = ebla sed malalta prioritato · ❌ = ne sensenca/ne ebla

## Faza koureplano

### Fazo 0 — Fundamento
- Konstrui CMP-projekton (`shared` + 4 celoj), versikatalogo, ĉiuj celoj kompilas "Saluton".
- Porti datenmodelon al Kotlin `@Serializable`. Agordi Ktor, kotlinx.serialization, Coil.
- Enmeti `esperantoradio_kanaloj_v9.json` kiel bundled asset + defora elŝuto.

### Fazoj 1–3 — MVP (kerno)
- **Fazo 1 — Datumoj & foliumado** (nur legado): `KanalDeponejo` (JSON) + kanalaro
  (Compose, emblemoj per Coil). `ElsendoDeponejo` + RSS-parsilo (komuna, ksoup)
  por vivantaj kanaloj + radio.txt-rezervo. Kanalvido (`LazyColumn` + gluaj
  dat-kapoj) kaj elsendodetalo (sen ludado ankoraŭ).
- **Fazo 2 — Ludado**: `LudiloRegilo` (expect/actual): Android (Media3) + Desktop
  (VLCJ) unue, poste iOS (AVPlayer), poste Web (`HTMLAudioElement`). Mini-ludilbreto,
  serĉbreto, ludi/paŭzi/antaŭa/sekva. Livestream (Muzaiko) + podkastoj. Live
  "nun ludas"-metadatena pridemandado.
- **Fazo 3 — Personigo**: plejŝatataj (+ novaj-elsendoj-signo), lastaŭskultitaj
  (+ daŭriga pozicio), serĉo, agordoj. Persisto per dosierkaŝmemoro + multiplatform-settings.

### Fazoj 4–6 — Plena eldono
- **Fazo 4 — Malfono & mediaintegriĝo (Android/iOS)**: Android MediaSessionService
  + mediasciigo + mediabutonoj + kapaŭskultil/alvok-traktado (sonfokuso).
  iOS: malfona sono + Now Playing + remote commands.
- **Fazo 5 — Elŝutoj**: elŝut-abstraktado (Android DownloadManager/Ktor, iOS
  URLSession, Desktop Ktor→dosiero). Web preterlasita. Eksterreta listo, stato,
  ludado-el-dosiero, nur-WiFi-agordo.
- **Fazo 6 — pezaj platform-funkcioj (prokrastitaj, Android unue)**: vekhorloĝo
  (AlarmManager), hejmekrana widget, Chromecast (moderna Cast SDK), talesyntezo.

### Daŭra
- `server` (arkiv-servilo) ĝisdatigita por dividi modelon/parsilon kun la nova projekto.

## Riskoj kaj malfermitaj punktoj

- **Rompiĝemaj fluoj** (Google Drive/archive.org-skrapado) estas la plej granda
  prizorga risko — konsideru servil-normigon por la plej malbonaj fontoj.
- **Muzaiko livestream** eble ankoraŭ estas malfunkcia; kontrolu HLS-URL frue
  (influas Desktop/Web-ludil-elekton pro HLS).
- **HLS sur Desktop/Web**: bezonas VLCJ/hls.js — testu frue.
- **iOS malfona sono** bezonas ĝustan AVAudioSession-kategorion + capability.
- **Cleartext HTTP**: migru fontojn al HTTPS kie eble; alikaze per-platformaj esceptoj.
- **Mortaj kanaloj**: purigu kanal-JSON kiel parto de Fazoj 1 (forigu
  `FORPRENITAJ_KANALOJ` / `_malbona`-fluoj).
- **Web-limigoj**: neniu elŝuto, neniu malfono, neniu widget/vekhoro — UI devas
  elegante kaŝi tiujn.
