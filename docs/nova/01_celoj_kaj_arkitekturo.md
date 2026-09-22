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

La nova KMP-aposieraĵo vivas **en la radiko**, apud `malnova/`. Unu Gradle-build
(`settings.gradle.kts`, Kotlin DSL) estros kaj la novajn modulojn kaj la
malnovajn (el `malnova/`).

```
EsperantoRadio/
├── shared/                          # Komuna KMP-modulo
│   ├── src/
│   │   ├── commonMain/kotlin/dk/nordfalk/esperanto/
│   │   │   ├── App.kt               # radika Compose-funkcio (EsperantoRadioApp, navigation3)
│   │   │   ├── AppStato.kt          # proceznivela unuopulo: deponejoj + ViewModel-oj
│   │   │   ├── Protokolo.kt         # protokol-funkcioj (logi/logd/logw/loge) — expect/actual
│   │   │   ├── navigation/Vojoj.kt  # NavKey-oj (Hejmo, Kanalaro, KanaloDetalo, ElsendoDetalo, …)
│   │   │   ├── data/
│   │   │   │   ├── config/          # JSONC-leganto + PlatformResource + KreuSettings
│   │   │   │   ├── parser/          # RssParsilo (la sep parsregoloj — 04_parsado_kaj_arkivo.md)
│   │   │   │   └── repository/     # deponej-implementaĵoj (Ktor, diskkaŝmemoro, persisto)
│   │   │   ├── domain/
│   │   │   │   ├── model/           # Kanalo, Elsendo, Sonfonto, LudantoStato, Alarmo, ElshutStato
│   │   │   │   ├── player/          # LudiloRegilo, LudvicoLogiko, LudvicoRegilo
│   │   │   │   └── repository/     # deponej-interfacoj (Deponejoj, PersonigoDeponejoj)
│   │   │   └── ui/                  # Compose-ekranoj, Temo, HtmlVido, MiniLudilbreto, ktp
│   │   ├── commonTest/              # golden-testoj (kontraŭ fiksaĵoj) + desktopTest
│   │   ├── androidMain/             # ExoPlayer, AlarmManager, WorkManager, Receiviloj
│   │   ├── iosMain/                 # NoOp-ludilo (Xcode-projekto ankoraŭ ne kreita)
│   │   ├── desktopMain/             # mp3spi + SourceDataLine-ludilo
│   │   └── wasmJsMain/             # HTMLAudioElement-ludilo
│   ├── src/commonTest/resources/feeds/  # frostigitaj golden-fiksaĵoj
│   └── build.gradle.kts
├── androidApp/                      # Android-aplikaĵo (MainActivity, res/, AndroidManifest)
├── desktopApp/                      # Desktop-JVM-aplikaĵo
├── webApp/                          # Web-aplikaĵo (nur Wasm — la JS-celo havis Skia-eraron)
├── malnova/                         # Malnova Android-apo (heredaĵo, ne tuŝebla)
│   ├── app/
│   ├── parse/
│   └── data/
├── settings.gradle.kts              # Unuigita Kotlin-DSL-build (iosApp/server komentitaj)
├── build.gradle.kts
├── gradle/libs.versions.toml
└── gradle.properties
```

> `iosApp/` (Xcode-projekto) kaj `server/` ankoraŭ ne estas kreitaj. La iOS-kodo
> ekzistas en `shared/src/iosMain/`, sed mankas Xcode-projekto. Vidu
> `06_servilo_arkivo.md` por la servila plano.

### Kial tiu strukturo

- La **parsado** vivas en `shared/data/parser/` — komuna, pura, testebla.
- La **UI-temo kaj komunaj komponantoj** vivas en `shared/ui/` — reuzeblaj
  trans Android/iOS/Desktop.
- Platform-specifaj aferoj (ExoPlayer, sciigoj, mp3spi) vivas en
  `androidMain`/`desktopMain`/`wasmJsMain` kiel `expect`/`actual`.
- Neniu DI-framintervalo — dependencaĵoj transdonitaj permane en konstruktiloj.
- Neniu datumbazo — la kliento kaŝenas servil-respondojn kiel dosierojn.

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

## Plursistema cel-matrico (stato 22a de septembro 2026)

| Funkcio                        | Android | iOS | Desktop | Web |
|--------------------------------|:--:|:--:|:--:|:--:|
| Kanalaro, elsendlistoj, detalo | ✅ | ✅ | ✅ | ✅ |
| MP3-ludado                     | ✅ ExoPlayer | no-op | ✅ mp3spi | ✅ HTMLAudioElement |
| HLS (Muzaiko livestream)       | ✅ ExoPlayer | no-op | ❌ | retumilo |
| Serĉo, plejŝatataj             | ✅ | ✅ | ✅ | ✅ |
| Ludvico + daŭra ludado         | ✅ | ✅ | ✅ | ✅ |
| Malfona ludado + mediasciigo   | ✅ | no-op | ❌ | ❌ |
| Sciigoj pri novaj elsendoj     | ✅ WorkManager | ❌ | ❌ | ❌ |
| Elŝutoj (eksterrete)           | ✅ | no-op | ✅ | ❌ |
| Vekhorloĝo                     | ✅ | ❌ | ❌ | ❌ |
| Sentry.io erarmonitorado       | ✅ | ✅ | ✅ | ✅ |
| Hejmekrana widget              | ❌ | ❌ | ❌ | ❌ |
| Chromecast                     | ❌ | ❌ | ❌ | ❌ |
| Parolsintezo                   | ❌ | ❌ | ❌ | ❌ |

✅ = funkcias · ❌ = ne implementita · no-op = kodo ekzistas sed ne ludas

## Faza stato

La plej ĝisdatigitan fazo-tabelon vidu en `AGENTS.md` (sekcio "Stato de la projekto").
Resumo: fazoj 0–5 estas kompletaj; fazo 6 (pezaj platform-funkcioj) estas parte farita
(vekhoro + sciigoj; widget/Chromecast/TTS ankoraŭ venas).
