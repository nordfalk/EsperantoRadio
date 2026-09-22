# Esperanto
EsperantoRadio estas radio-apo kun Muzaiko kaj aliaj kanaloj en Esperanto


## La arkivo de elsendoj

Por kontrui mian arkivon de Esperanto-elsendoj, mi uzas

./gradlew clean
./gradlew :parse:rssarkivserverJar
java -jar malnova/parse/build/libs/rssarkivserver.jar


## Farota (notoj)
Eldoni ĉe F-droid, Apptiode kaj aliaj vendejoj.
Vidu https://github.com/nordfalk/EsperantoRadio/pull/66


Zorgu ke la alarmo estas ekzakta ĝis 10 minutoj (https://developer.android.com/develop/background-work/services/alarms) ?


Mankas eksponenta repro-logiko. La malnova `Afspiller` havis eksponentan backoff (gxis 10 provoj).
Se reto perdigxas dum ludado, la uzanto devas mane reprovi (gxis 10 provoj) kaj poste raporti ke ne eblas, kaj daŭrigi per alia
Aldone, la malnova apo montris tre klare kio okazas (ekzemple 'Konaktas'). Tion la nova ankaŭ faru.

Rigardi la malnova apo eo_strings.xml ĉu estas iuj uzkazoj kiujn ni ne kovras?


Vekhorloĝo: podkastoj ne povas aŭtomate ludi ĉe alarmo (nur rekta radio — alarmo lanĉas la kanal-livestreamon, ne specifan elsendon)


- **Muzaiko livestream** eble ankoraŭ estas malfunkcia; kontrolu HLS-URL frue
  (influas Desktop/Web-ludil-elekton pro HLS).
- **HLS sur Desktop/Web**: bezonas VLCJ/hls.js — testu frue.


- Malsupren tiro refreŝigas. Ĉe frontpaĝo kaj ĉe kanaloj. Mankas indiko ke la apo estas refreŝiĝanta.


### 5. Tirkesto (navigacio)
La malnova plano havis `ModalNavigationDrawer`-tirkeston

```
┌────────────────────┐
│  [Muzaiko-M]        │  Emblemo
│  EsperantoRadio     │
│  ─────────────      │
│  ♪ Senest aŭskultitaj│
│  ★ Plejŝatataj (3)  │  nombro da novaj
│  ⬇ Elŝutitaj (5)    │
│  ⏰ Vekhorloĝo      │  sekva alarmtempo
│  ─────────────      │
│  ℹ Pri / Kontakt    │
│  ⚙ Agordoj          │
│  ─────────────      │
│  Elektu kanalon     │
└────────────────────┘
```


# 6. La podkasta arkiv-servilo



Transskribo de elsendoj kun tempokodoj, montrataj dum ludado (por lerni Esperanton)

# Dansk
App'en er open source under GPL licensen og kan findes på https://github.com/nordfalk/EsperantoRadio 

EsperantoRadio bygger på arbejde på DR Radio lavet af Lund&Bendsen for DR. 
Det oprindelige arbejde ligger på https://github.com/androidlundogbendsen/dr-radio-android.
GPL licensen som DR Radio blev publiceret under kan læses her https://github.com/androidlundogbendsen/dr-radio-android/blob/master/DRRadiov2/LICENSE.txt

