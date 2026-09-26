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


Uzkazoj el la malnova apo (`malnova/app/src/main/res/values/eo_strings.xml`) kiujn la nova ne kovras:
- Konigi (kunhavigi) elsendon
- Agordo por tuj ludi kiam la apo malfermiĝas
- Elekti lokon de elŝutitaj elsendoj / ekstera memoro, kaj averto pri libera spaco (`Placering_af_hentede_udsendelser`, `Det_lykkedes_ikke_at_hente_...`)
- Konfirmo antaŭ forigo de elŝuto
- "Ĉu ĉesi ludi? / Daŭrigi fone" ĉe fermo de la apo
- Nombro da novaj elsendoj ĉe plej ŝatataj kanaloj
- "Nun estas ludata" por la rekta elsendo — `rektaElsendaPriskriboUrl` (muzaiko.info/data.json) estas legata sed ne montrata
- Pri / Kontakto-ekrano
- Averto ke vekhorloĝo ne povas garantii kontraŭ teknikaj problemoj


- **HLS sur Desktop/Web**: bezonas VLCJ/hls.js. (La Muzaiko-livestream mem funkcias — kontrolita 2026-09-23; sur Android necesis `media3-exoplayer-hls`.)
- Eksponenta reprovo ne atendas je reta reveno (la malnova `venterPåAtKommeOnline`) — ĝi rezignas post ~3 minutoj.
- Web: plej multaj RSS-fluoj estas blokitaj de CORS; bildoj ne aperas. Bezonas servilon/prokurilon.


legi kanalkonfiguro de la reto ?

post aŭtomata ludado de 5 elsendoj la ludado iru al la rektaj elsendoj?

Faru PRojn kiun la ŝanĝoj (se eblas, faru apartajn PRojn, tamen vi povas kunmeti plurajn aferojn en unu PR se tiel estas pli simple)

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

