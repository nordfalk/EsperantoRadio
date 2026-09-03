# 1. Strukturo kaj konstruo

## Moduloj (Gradle)

```
EsperantoRadio/
├── app/      # Android-apo (dk.dr.radio.v3 / dk.nordfalk.esperanto.radio)
├── parse/    # RSS-parsado + RssArkivServer (java-library + Kotlin)
└── data/     # Datummodeloj (java-library)
```

`settings.gradle`:
```groovy
include ':app'
include ':parse'
include ':data'
```

`app` dependas de `parse` kaj `data`; `parse` dependas de `data`.

## Radika `build.gradle`

- AGP `8.7.3`, deponejoj `google()` + `jcenter()`
- Neniu versikatalogo; ĉiuj versioj malmolaj en `app/build.gradle`

## `app/build.gradle`

| Eco | Valoro |
|---|---|
| namespace | `dk.dr.radio.v3` |
| applicationId | `dk.nordfalk.esperanto.radio` |
| compileSdk | 34 |
| minSdk | 26 |
| targetSdk | 30 |
| versionCode | 230 |
| versionName | `2.3 Ordigado` |
| Java | 11 |
| debug-sufikso | `.beta` |
| minify | malŝaltita |

### Ĉefaj dependencoj

| Dependenco | Versio | Kialo |
|---|---|---|
| Volley | 1.2.1 | Reta komunikado |
| Sentry | 1.7.27 | Erarospurado |
| ExoMedia | 5.0.0 | Sonludado |
| Picasso | 2.5.2 | Bildoj |
| Android-Query | 0.25.9 | Legacy-UI-utilaĵoj |
| Material / AppCompat | 1.0.0 | UI |
| Play Cast Framework | 9.4.0 | Chromecast |
| Joda-Time Android | 2.9.9 | Datoj |
| Apache HTTP Legacy | — | `useLibrary` |

### Testoj

- Robolectric 4.3, JUnit 4.12, BouncyCastle 1.57

## `parse/build.gradle`

- `java-library` + Kotlin 1.8.21, Java 11
- Rometools Rome 1.18.0 (+ modules) — RSS/Atom
- OkHttp 4.10.0 (+ logging-interceptor)
- `org.json:json:20080701` (malnova versio, devias kongrui kun Android)
- kXML2 2.3.0 — XmlPullParser
- Brotli dec 0.1.2, Jsoup 1.14.3
- Tasko `rssarkivserverJar` — konstruas ruleblan JAR kun
  `Main-Class: rssarkivserver.RssArkivServer`

## `data/build.gradle`

- `java-library`, Java 11
- `org.json:json:20080701`

## `gradle.properties`

```
android.useAndroidX=true
android.enableJetifier=true
android.enableR8.fullMode=false
```

## AndroidManifest.xml

### Permesoj

| Permeso | Kialo |
|---|---|
| `INTERNET` | Reta aliro |
| `ACCESS_NETWORK_STATE` | Kontroli konekteblecon |
| `WAKE_LOCK` | Teni aparaton veka dum ludado |
| `SCHEDULE_EXACT_ALARM` | Vekhorloĝo |
| `RECEIVE_BOOT_COMPLETED` | Reaktivigi alarmojn post restarto |
| `VIBRATE` | Vekhorloĝo-vibrigo |
| `WRITE_EXTERNAL_STORAGE` | Elŝuti podkastojn |
| `FOREGROUND_SERVICE` | Malfona servo (Android 9+) |
| `READ_PHONE_STATE` (maxSdk 22) | Paŭzigi ludon dum alvoko |
| `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` | Chromecast |

### `<application>`

- `android:name="dk.dr.radio.diverse.ApplicationSingleton"`
- `android:usesCleartextTraffic="true"` (permesas HTTP — multaj fontoj estas HTTP)
- Temo `@style/Theme.Dr`

### Aktivaĵoj

| Aktivaĵo | Rolo |
|---|---|
| `Hovedaktivitet` | Ĉefa lanĉilo (`MAIN`/`LAUNCHER`), `singleTop` |
| `Indstillinger_akt` | Agordoj |
| `AlarmClock_akt` | Vekhorloĝo-listo |
| `SetAlarm_akt` | Agordi unuopan alarmon |
| `GenstartProgrammet` | Debug "restartigi programon" |

### Servoj

| Servo | Rolo |
|---|---|
| `HoldAppIHukommelsenService` | Malfona servo dum ludado |

### Riceviloj

| Ricevilo | Intent-filtero | Rolo |
|---|---|---|
| `HentedeUdsendelser$DownloadServiceReciever` | `DOWNLOAD_COMPLETE`, `DOWNLOAD_NOTIFICATION_CLICKED` | Elŝutoj |
| `AfspillerStartStopReciever` | — | Start/halt la ludilon |
| `HovedtelefonFjernetReciever` | `ACTION_HEADSET_PLUG` (dinamika) | Haltas ĉe kapaŭskultil-elmeto |
| `FjernbetjeningReciever` | `MEDIA_BUTTON` (priority 1) | Forkontrolilo/bluetooth |
| `AfspillerIkonOgNotifikation` | `APPWIDGET_UPDATE` | Vivanta piktogramo |
| `AlarmReceiver` | `dk.dr.radio.ALARM_ALERT` | Alarm-ago |
| `AlarmInitReceiver` | `BOOT_COMPLETED` | Reaktivigas alarmojn post ekrektigo |
