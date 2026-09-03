# 2. Datumfluo

## Pakoj en `app/src/main/java/dk/dr/radio/`

```
dk.dr.radio/
├── afspilning/   # Sonludado
├── akt/          # Aktivaĵoj + fragmentoj (UI)
├── backend/      # Logiko inter reto kaj datumoj
├── data/         # Apo-nivelaj datumoj
├── diverse/      # Utilaĵoj + malnova API
├── net/          # Reto (Volley)
└── vaekning/     # Vekhorloĝo
```

### Respondecoj laŭ pako

| Pako | Respondeco |
|---|---|
| `afspilning` | Tuta sonludado. `Afspiller` estas la centra ludilo (wrapas ExoMedia); administras staton (STOPPET/FORBINDER/SPILLER), sonfokusan, alvok-interrompon, wifi-ŝloson, sciigon kaj vivantan piktogramon. |
| `akt` | Ĉiuj aktivaĵoj kaj fragmentoj. `Hovedaktivitet` = lanĉilo. Fragmentoj por kanallisto, elsendo, ludilo, serĉo, elŝutitaj, plej ŝatataj, laste aŭskultitaj, agordoj. |
| `backend` | `Backend` provizas grund-datumojn URL, legas `radio.txt`, lanĉas RSS-parsadon, elŝutas kanalemblemojn. `Netkald` faras asinhronajn HTTP-vokojn per Volley. `NetsvarBehander` = callback-interfaco. |
| `data` | `Programdata` estas la centra objekto (enhavas `udsendelseFraSlug`, `senestLyttede`, `hentedeUdsendelser`). Ankaŭ `Favoritter`, `HentedeUdsendelser`, `SenestLyttede`, `Datoformater`. |
| `diverse` | `App` = senmova stato-tenilo. `ApplicationSingleton` = Application-subklaso. `Log` = protokolado + erarraportado. `Talesyntese` = TTS. |
| `net` | `Netvaerksstatus` spuradas konekteblecon. `volley/` enhavas proprajn Volley-adaptilojn. |
| `vaekning` | Tuta vekhorloĝo — bazita sur Android DeskClock-kodo (Apache-licencita). Alarmoj, agordoj, ripeto, vek-ŝloso, riceviloj. |

## Ĉefaj enirejoj

```
ApplicationSingleton.onCreate()
  ├── new App()  →  App.instans
  ├── App.init(ctx)      — Volley, FilCache, Sentry, JodaTime, Afspiller, Fjernbetjening, Netvaerksstatus
  └── App.initData(ctx)  — Grunddata (JSON el prefs aŭ raw/), Backend, Programdata, komencan kanalon
```

- **`ApplicationSingleton`** (`diverse/ApplicationSingleton.java`) — `android:name` en manifest.
  Kreas `App.instans` kaj vokas `init()` + `initData()`.
- **`App`** (`diverse/App.java`) — Senmova centrala stato-tenilo. Ne estas `Application`,
  sed tenas ĉiujn senmovajn kampojn: `data`, `grunddata`, `afspiller`, `backend`,
  `netkald`, `volleyRequestQueue`, `netværk`, `fjernbetjening`, `prefs`, `res`.
- **`Hovedaktivitet`** (`akt/Hovedaktivitet.java`) — Lanĉa aktivaĵo. Uzas navigacian
  tirkeston (`Venstremenu_frag`), gastigas fragmentojn en `R.id.indhold_frag`,
  kaj enhavas malsupran ludil-breton (`Afspiller_frag`).

## Datumfluo: reto → UI

```
       esperantoradio_kanaloj_v9.json  +  radio.txt
        ↓ Backend.initGrunddata()  +  Backend.leguRadioTxt()
   Grunddata.kanaler (List<Kanal>)   kaj   kanalFraSlug
        ↓ por ĉiu kanal: Backend.hentUdsendelserPåKanal(kanal)
   Netkald.kald()  →  Netsvar  (Volley / FilCache)
        ↓
   RomePodcastParser.parsRss()   (3 branĉoj, vidu 03_parsado_kaj_fontoj.md)
        ↓
   Kanal.udsendelser (ArrayList<Udsendelse>)  +  App.data.udsendelseFraSlug
        ↓
   Fragmentoj (observantoj: Runnable)  →  Kanal_frag, Kanaler_frag, Udsendelse_frag
        ↓
   Afspiller_frag  →  App.afspiller.setLydkilde()  →  EmaPlayerWrapper (ExoMedia)  →  SONO
```

### Paŝo post paŝo

1. **Startigo** — `ApplicationSingleton.onCreate()` → `App.init()` → `App.initData()`.
   `initData` legas grunddata-JSON (unue el SharedPreferences, aŭ el
   `R.raw.esperantoradio_kanaloj_v9`), vokas `Backend.initGrunddata()`, kiu vokas
   `Grunddataparser.initGrunddata2()`.

2. **Grunddata-parsado** — `Grunddataparser` (`parse/.../Grunddataparser.java`)
   legas la JSON-array `kanaloj`, kreas `Kanal`-objektojn kun `kodo` (slug),
   `nomo`, `elsendojRssUrl`, `emblemoUrl`, `rektaElsendaSonoUrl` ktp.

3. **radio.txt** — `Backend.initGrunddata()` ankaŭ legas `radio.txt` (el
   `esperanto-radio.com/radio.txt` aŭ `R.raw.radio` kiel rezervo).
   `leguRadioTxt()` parsgas la tekstdosieron (kanalnomo/dato/URL/priskribo-blokoj)
   kaj kreas `Udsendelse`-objektojn por kanaloj sen RSS aŭ kiel plenigo.

4. **Kanala RSS** — Kiam uzanto malfermas kanalon, `Backend.hentUdsendelserPåKanal(kanal)`
   faras asinhronan vokon per `Netkald.kald()` al `kanal.eo_elsendojRssUrl`.

5. **RSS-parsado** — La respondo estas parsata per `RomePodcastParser.parsRss()`
   (Kotlin, parse-modulo). Tri branĉoj: `parsVarsoviaVento` (pluraj `<audio>` po
   ero), `parsePeranto` (iframe → archive.org/Google Drive), `parseAndre`
   (ĝenerala). La malnova `EoRssParsado` (Java, XmlPullParser) ankaŭ ekzistas
   sed estas anstataŭigita.

6. **Stokado** — La rezulto (`ArrayList<Udsendelse>`) estas metata en
   `kanal.udsendelser` kaj `App.data.udsendelseFraSlug`.

7. **UI** — Fragmentoj (ekz. `Kanal_frag`) registras sin kiel `grunddata.observatører`
   (listo de `Runnable`). Kiam datumoj ŝanĝiĝas, `App.opdaterObservatører()`
   rulas ĉiujn observant-Runnable-ojn sur la ĉeffadeno.

8. **Ludado** — `Afspiller_frag` vokas `App.afspiller.setLydkilde(udsendelse)` →
   `Afspiller.startAfspilning()` → `EmaPlayerWrapper.setDataSource(stream)` →
   `prepare()` → `start()`.

## Ŝlosilaj klasoj

| Klaso | Pako | Rolo |
|---|---|---|
| `App` | `diverse` | Senmova stato-tenilo (ĉiuj singltonaj kampoj) |
| `ApplicationSingleton` | `diverse` | `Application`-subklaso, enirejo |
| `Backend` | `backend` | Grunddata-ŝargo, radio.txt, RSS-lanĉo, emblemoj |
| `Grunddataparser` | `parse/backend` | JSON-parsado de kanalkonfiguro |
| `RomePodcastParser` | `parse/backend` | Nova ĉefparsilo de RSS/Atom (3 branĉoj) |
| `EoRssParsado` | `parse/backend` | Malnova RSS-parsilo (anstataŭigita) |
| `FilCache` | `parse/net` | Dosierkaŝo kun HTTP GET + IfModifiedSince |
| `Programdata` | `data` | Centrala apo-datumo (udsendelseFraSlug, senestLyttede...) |
| `Kanal` | `data` | Kanal-modelo (etendas `Lydkilde`) |
| `Udsendelse` | `data` | Elsend-modelo |
| `Afspiller` | `afspilning` | Ludilo (wrapas ExoMedia), stato, sonfokuso |
| `Hovedaktivitet` | `akt` | Ĉefaktivaĵo, fragment-administrado |
| `Fragmentfabrikering` | `akt` | Centra fragment-fabriko |

## Konektebleco kaj eraroj

`Afspiller` administras sonfokusan per `AudioManager.OnAudioFocusChangeListener`:

- `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` → malpligrandigi laŭtecon
- `AUDIOFOCUS_LOSS_TRANSIENT` → paŭzigi
- `AUDIOFOCUS_LOSS` → halti
- `AUDIOFOCUS_GAIN` → daŭrigi

`WifiLock` (`WifiManager.WifiLock`) tenas wifi aktiva dum fluado. La ludilo havas
propran laŭteco-regilon kun minimuma devigo.

## Rimedoj (`res/raw/`)

- `esperantoradio_kanaloj_v9.json` — la kanalkonfiguro (vidu `03_parsado_kaj_fontoj.md`)
- `radio.txt` — rezerva kopio de la komunuma elsendolisto
- `afspiller_*.ogg` — son-efikoj (start/stop/forbinder/fejl/spiller), ludataj
  de `Afspiller.Afspillerlyd` nur se `prefs.getBoolean("afspillerlyde", false)`
