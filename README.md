# Esperanto
EsperantoRadio estas radio-apo kun Muzaiko kaj aliaj kanaloj en Esperanto


## La arkivo de elsendoj

Por kontrui mian arkivon de Esperanto-elsendoj, mi uzas

./gradlew clean
./gradlew :parse:rssarkivserverJar
java -jar malnova/parse/build/libs/rssarkivserver.jar


## Farota (notoj)
Eldoni ĉe F-droid? aŭ Apptiode aŭ aliaj vendejoj?

Ĉu eblas aŭtomate eldoni ĉe Google Play en iu maniero? Ĉu eble estas iu skill aŭ ilo por tio? 
Kio pri f-droid? aŭ Apptiode aŭ aliaj vendejoj?


Zorgu ke la alarmo estas ekzakta ĝis 10 minutoj (https://developer.android.com/develop/background-work/services/alarms)


> **Neniu eksponenta repro-logiko** estis implementita. La malnova `Afspiller` havis
> eksponentan backoff (gxis 10 provoj). Se reto perdigxas dum ludado, la uzanto devas
> mane reprovi. Tio estas malfermita punkto.


# Dansk
App'en er open source under GPL licensen og kan findes på https://github.com/nordfalk/EsperantoRadio 

EsperantoRadio bygger på arbejde på DR Radio lavet af Lund&Bendsen for DR. 
Det oprindelige arbejde ligger på https://github.com/androidlundogbendsen/dr-radio-android.
GPL licensen som DR Radio blev publiceret under kan læses her https://github.com/androidlundogbendsen/dr-radio-android/blob/master/DRRadiov2/LICENSE.txt

