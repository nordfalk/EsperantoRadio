# 5. Dizajno kaj UI (Muzaiko-temo)

> La dizajno estas moderna, deklara (Compose), kaj inspirita de **Muzaiko** —
> la Esperanto-radiostacio kies nomo mem estas vortludo el *mozaiko* + *muziko*.
> La emblemo de Muzaiko estas mozaiko-stiligita "M" el ruĝaj geometriaj
> fragmentoj. Tiu motivo gvidas la tutan vizualan identecon.

## Vizuala koncepto: "Mozaiko de sonoj"

Same kiel Muzaiko kunigas malsamajn Esperanto-voĉojn kaj muzikojn en unu
fluon, la UI kunigas diversajn kanalojn en unu mozaiko. Ĉiu kanal estas
*peco* de la mozaiko — emblemoj komponiĝas kiel kaheloj.

### Kial tio taŭgas

- **Esperanto** = "lingvo monduma" — multaj pecoj, unu tuto.
- **Muzaiko** = mozaiko + muziko. La nomo mem pravigas la metaforon.
- **La emblemo** (ruĝa mozaik-M) rekte donas la koloro-paletron kaj formo-idiomon.
- **Podkastoj** kolektiĝas el dise — la apo estas la mozaiko kiu kunigas ilin.

## Kolorpaletron (de la Muzaiko-emblemo)

La Muzaiko-SVG uzas tri ruĝajn nuancojn. Tio iĝas la marka koloro.

| Nomo | Heks | Uzo |
|---|---|---|
| `muzaiko_ruĝo` | `#D0002F` | ĉefmarko, aktivaj elementoj, ludbutono |
| `muzaiko_ruĝo_malhela` | `#9F001E` | emfazo, premata stato |
| `muzaiko_ruĝo_profunda` | `#4F000E` | malhela temo, fono de etaĵoj |
| `muzaiko_kremo` | `#FAF6F2` | hela fono (varma, ne pura blanko) |
| `muzaiko_karbono` | `#1A1413` | teksto, malhela fono |
| `muzaiko_arĝento` | `#E8E2DD` | dividiloj, kartoj, malaktivaj |

### Hel/malhela temo

| Rolo | Hela temo | Malhela temo |
|---|---|---|
| Fono | `muzaiko_kremo` `#FAF6F2` | `muzaiko_karbono` `#1A1413` |
| Surfaco (karto) | `#FFFFFF` | `#241D1B` |
| Teksto | `muzaiko_karbono` | `muzaiko_kremo` |
| Marko/Akcento | `muzaiko_ruĝo` `#D0002F` | `muzaiko_ruĝo` `#D0002F` (sama) |
| Malaktiva | `muzaiko_arĝento` | `#3A3030` |

La ruĝo restas konstanta trans temoj — ĝi estas la marko. La fono varmiĝas
(kremo) neakra neŭtrala blanko, por ne konkuri kun la ruĝo.

## Tiparo

- **Ĉefa:** interreto-sana senkondiĉa tiparo kun bona Esperanto-subteno
  (ĉapelitaj literoj ĉ, ĝ, ĥ, ĵ, ŝ, ŭ). Opcioj:
  - **Noto Sans** (libera, bonega Esperanto-kovrado) — defaŭlta
  - **Roboto Flex** (Android-natura, bonega Esperanto-kovrado) — opcia
- **Emfazo/Titoloj:** **Noto Serif** aŭ **Source Serif** — por kanalnomoj kaj
  elsendotitoloj, donas "eldonan" senton taŭgan por podkastoj.
- **Monospace:** por statusteksto / teknikaj detaloj (nur debug).

Ĉiuj tiparoj devas korekte montri ĉapelitajn literojn. Tio estas ne-negocebla.

## Formo-idiomo: geometriaj fragmentoj

- **Kanalemblemoj** estas montrataj en **kvadrataj kaheloj** (mozaikpecoj) kun
  milda angul-rondo (8-12dp). Plej ŝatataj kanaloj povas montri malgrandan
  ruĝan angul-pecon (mozaik-akcento).
- **Ludbutono** estas cirkla, ruĝa, kun mozaik-stiligita "play"-simbolo
  (aŭ la Muzaiko-M kiel load/peziga animacio).
- **Dividiloj** estas subtilaj maldikaj linioj (`muzaiko_arĝento`), ne ombroj.
- **Kartoj** uzas malaltan plian altecon, ne fortan ombro — plato, ne flosaĵo.

## Ekranoj

### 1. Hejmo (ĉefekrano)

```
┌──────────────────────────────────┐
│  Kio novas                       │  Horizontala LazyRow de kartoj
│  [karto] [karto] [karto] →       │  (emblemo, titolo, subtitolo)
├──────────────────────────────────┤
│  Lastatempe ludata                │  Horizontala LazyRow
│  [karto] [karto] →               │  (kun ludprocento)
├──────────────────────────────────┤
│  Kio popularas                    │  Horizontala LazyRow
│  [karto] [karto] →               │
├──────────────────────────────────┤
│  Ĉiuj kanaloj                     │  Vertikala LazyColumn
│  • Kernpunkto                     │
│  • Varsovia Vento                 │
│  ...                             │
├──────────────────────────────────┤
│  [Hejmo] [Kanaloj] [★] [🔍]     │  Malsupra naviga breto (4 langetoj)
└──────────────────────────────────┘
```

- 4 langetoj en la malsupra naviga breto: Hejmo, Kanaloj, Plej ŝatataj, Serĉi.
- Ĉiu karto en la horizontalaj sekcioj montras emblemon, titolon kaj subtitolon.
- Kartoj en "Lastatempe ludata" montras ludprocenton.
- Tripunkta menuo kaj ludbutono sur kartoj.

### 2. Kanalvido

```
┌──────────────────────────────────┐
│ ← Varsovia Vento                 │
├──────────────────────────────────┤
│ [Kanalemblemo, granda]            │
│ Varsovia Vento                    │
│ [▶ Ludu rektan] (se rekta)        │
├──────────────────────────────────┤
│ Hodiaŭ                            │  Dato-grupigitaj elsendoj
│  • VVE185 1a parto  [▶] [⬇]      │
│  • VVE185 2a parto  [▶] [⬇]      │
│ Hieraŭ                            │
│  • VVE184 ...                     │
│ ...                               │
└──────────────────────────────────┘
```

- Elsendoj grupigitaj laŭ dato (hodiaŭ/hieraŭ/dato).
- Ĉiu ero: titolo, daŭro, ludbutono, elŝutbutono, plejŝat-koro.
- Horizontala svipo inter kanaloj (maldekstren/dekstren ŝanĝas kanalon).
- Malsupren tiro refreŝigas.

### 3. Elsendodetalo

Plenekrana malvolo de la mini-ludilbreto:
```
┌──────────────────────────────────┐
│ [Bildo de elsendo, larĝa]        │
│ Titolo de elsendo                 │
│ Kanalnomo · Dato · Daŭro          │
│                                   │
│        [▶]  ━━━━●━━━━━━  12:34   │  Ludregiloj + serĉbreto
│                                   │
│ Priskribo (riĉa HTML-vido)        │
│                                   │
│ [⬇ Elŝuti] [♥ Plejŝati] [↗ Kunh.]│
│ [Queueludvico]                   │
│                                   │
│ Daŭrigi de 12:34 [▶]             │  (se antaŭa pozicio ekzistas)
└──────────────────────────────────┘
```

- Horizontala svipo inter elsendoj de sama kanalo.
- "Daŭrigi de X:XX" butono se antaŭa ludpozicio ekzistas.
- Ludvico-butono (Material 3 ikono) por eksplicite aldoni al ludvico.
- Riĉa HTML-vido de priskribo (`HtmlVido`/`HtmlTeksto`).
- Evolua reĝimo: elektiloj por priskribo-fonto kaj vidmaniero.

### 4. Ludilbreto (mini, malsupre)

Konstanta malsupra breto:
```
┌──────────────────────────────────┐
│ [emblemo] Tiu ĉi elsendo    ▶  ↕ │  titolo + ludbutono + volvigi
│           ━━━━●━━━━━━ 12:34      │  (kaŝita por rekta fluo)
│           [halti] [ludvico]      │
└──────────────────────────────────┘
```
Klako sur la breto → volvigas al elsendodetalo.
Ludvico-butono montras la nunan ludvicon.

### 5. Malsupra naviga breto

```
┌──────────────────────────────────┐
│  [Hejmo]  [Kanaloj]  [♥]  [🔍]  │  4 langetoj
└──────────────────────────────────┘
```

- **Hejmo**: "Kio novas", "Lastatempe ludata", "Kio popularas", "Ĉiuj kanaloj"
- **Kanaloj**: plena kanalaro (`LazyColumn` de `ListItem`-oj)
- **Plej ŝatataj**: nur ŝatataj kanaloj
- **Serĉi**: serĉo trans ĉiuj kanaloj

La malnova plano havis `ModalNavigationDrawer`-tirkeston — tio estis anstataŭigita
per la malsupra naviga breto (pli moderna, pli bona por unumana uzo).
Aldonaj ekranoj (Alarmo, Agordoj, Elŝutitaj, Ludvico) estas atingeblaj per
navigado ekde la ĉefekranoj.

### 6. Serĉo

```
┌──────────────────────────────────┐
│ ← 🔍 [Trovu elsendon...]          │
├──────────────────────────────────┤
│  Rezultoj                         │
│  • Kernpunkto - KP204 Pigmentoj    │
│  • Varsovia Vento - VVE185 ...    │
│  ...                              │
└──────────────────────────────────┘
```
Serĉo trans ĉiuj kanaloj (uzante `ElsendoDeponejo.sercxiElsendojn`).

### 7. Agordoj (aktuala stato)

La agordoj estis simpligitaj ekde la originala plano. Nuna listo:

- **Aŭtomate daŭrigu kun alia elsendo** (ŝaltilo)
- **Ricevi sciigojn** (ŝaltilo, nur Android — kontrolas WorkManager-skedadon)
- **Evolua reĝimo** (ŝaltilo — montras elektilojn por priskribo-fonto kaj vidmaniero en ElsendoEkrano)
- **Temo** (hela/malhela/sistemo)

> La originala plano havis lingvoelekton, elŝuthejmon, nur-WiFi, son-efikojn,
> devigi portreton — tiuj estis forigitaj (#55, #37).

## Navigado (navigation3)

La apo uzas `androidx.navigation3` — **ne** la tradician `NavHost`/`composable`-DSL.

```kotlin
// App.kt
@Composable
fun EsperantoRadioApp() {
    val backStack = rememberNavBackStack(Vojo.Hejmo)
    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<Vojo.Hejmo> { HejmoEkrano(...) }
            entry<Vojo.Kanalaro> { KanalaroEkrano(...) }
            entry<Vojo.KanaloDetalo> { KanalEkrano(...) }
            entry<Vojo.ElsendoDetalo> { ElsendoEkrano(...) }
            entry<Vojo.Sercxi> { SercxiEkrano(...) }
            entry<Vojo.Plejsxatataj> { PlejstatatajEkrano(...) }
            entry<Vojo.Elshutitaj> { ElshutitajEkrano(...) }
            entry<Vojo.Alarmoj> { AlarmoEkrano(...) }
            entry<Vojo.Agordoj> { AgordojEkrano(...) }
            entry<Vojo.Ludvico> { LudvicoEkrano(...) }
        },
    )
}
```

`Vojo` estas `sealed`/`NavKey`-hierarkio en `navigation/Vojoj.kt`.
Stato persistebla per `SavedStateConfiguration` kun `polymorphic` serializers.

## Movado / animacio

- **Kanalŝanĝo:** kruc-fades inter kanalekranoj.
- **Ludilbreto volvigo:** springo-bazita `BottomSheet` (ne abrupta).
- **Ŝarĝado:** la Muzaiko-M kiel rotacia/peziganta indikilo (mozaikpecoj kunfandiĝas).
- **Transiroj:** `AnimatedVisibility` / `Crossfade` — subtila, ne ŝika.

## Alirebleco

- Ĉiuj interagaj elementoj havas `contentDescription`.
- Minimuma tuŝcelo 48dp.
- Subteni TalkBack/VoiceOver.
- Dinamika tipogrando (sistemo-skalado).
- Alta kontrasto en malhela temo.

## Lokalizado

- **Defaŭlta lingvo: Esperanto** (eo). La malnova apo defaŭltis al dana; la nova
  defaŭltas al Esperanto — la uzantaro estas Esperant-parolanta.
- **Dana** (da) kiel duaranga (heredaĵo).
- Uzu Compose `LocalContext`-rimedojn aŭ `kotlinx`-lokalizo (KMP-kongrua).
-Ĉiuj ĉapelitaj literoj devas korekte montri en ĉiuj tiparoj.
