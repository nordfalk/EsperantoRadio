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

### 1. Ĉefekrano (Kanalaro)

```
┌──────────────────────────────────┐
│ ☰  EsperantoRadio     🔍 ⚙️       │  Trinkesto (ruĝa akcento)
├──────────────────────────────────┤
│ [Mozaik-kaheloj de kanaloj]       │  Tablo de emblemoj; plejŝatataj unue
│  ┌──┐ ┌──┐ ┌──┐                  │  (stango de 2-3 kolumnoj, adapteblas)
│  │Mu│ │VV│ │LM│  ...              │
│  └──┘ └──┘ └──┘                  │
├──────────────────────────────────┤
│  ▶ Nuna elsendo: Muzaiko - ...    │  Mini-ludilbreto (malvolvigita)
└──────────────────────────────────┘
```

- Langetoj aŭ glataj rulumo inter kanalaro kaj "lastaŭskultitaj"/"plejŝatataj".
- Ĉiu kahelo montras emblemon + nomon + nombron da novaj elsendoj (ruĝa punkto).
- Longa premo sur kahelo → kunteksta menuo (plejŝati/kunhavigi).

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
│  • VVE185 1a parto  [▶] [⬇]      │  (PinnedSectionList-stilo)
│  • VVE185 2a parto  [▶] [⬇]      │
│  • VVE185 3a parto  [▶] [⬇]      │
│ Hieraŭ                            │
│  • VVE184 ...                     │
│ ...                               │
└──────────────────────────────────┘
```

- Elsendoj grupigitaj laŭ dato (hodiaŭ/hieraŭ/dato).
-Ĉiu ero: titolo, daŭro, ludbutono, elŝutbutono, plejŝat-stelo.
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
│        [⟵][⟳][⟶]                │  (antaŭa/restarta/sekva)
│                                   │
│ Priskribo (purigita HTML)         │
│                                   │
│ [⬇ Elŝuti] [★ Plejŝati] [↗ Kunh.]│
└──────────────────────────────────┘
```

### 4. Ludilbreto (mini, malsupre)

Konstanta malsupra breto:
```
┌──────────────────────────────────┐
│ [emblemo] Tiu ĉi elsendo    ▶  ↕ │  titolo + ludbutono + volvigi
│           ━━━━●━━━━━━ 12:34      │  (kaŝita por rekta fluo)
└──────────────────────────────────┘
```
Klako sur la breto → volvigas al elsendodetalo.

### 5. Tirkesto (navigacio)

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

### 7. Agordoj

- Lingvo (Esperanto / Dana / aŭtomata)
- Elŝuthejjo
- Nur per WiFi
- Son-efikoj (afspillerlyde)
- Devigi portreton
- Malpeza/malhela temo (aŭ sistemo)

## Navigado (Compose)

```kotlin
@Composable
fun EsperantoRadioNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "kanalaro") {
        composable("kanalaro") { KanalaroEkrano(onKanal = { navController.navigate("kanal/$it") }) }
        composable("kanal/{slug}") { KanalEkrano(...) }
        composable("elsendo/{id}") { ElsendoEkrano(...) }
        composable("sercxi") { SercxiEkrano() }
        composable("plejsatataj") { PlejsatatajEkrano() }
        composable("elsutitaj") { ElsutitajEkrano() }
        composable("lastauxskultitaj") { LastAuxskultitajEkrano() }
        composable("alarmoj") { AlarmoEkrano() }
        composable("agordoj") { AgordojEkrano() }
        composable("pri") { PriEkrano() }
    }
}
```

Uzu **`ModalNavigationDrawer`** (Compose) por la tirkesto, **`BottomSheet`**
aŭ malsupra daŭra breto por la mini-ludilo.

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
