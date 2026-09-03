# 4. UI kaj funkcioj

## Strukturo

Tradicia Android: **Navigacia tirkesto** (Venstremenu) por ĉefa navigado,
**Malsupra ludilbreto** (Afspiller_frag), **Sciigoj** por malfona funkciado.

### Fragment-hierarĥio

```
Hovedaktivitet
├── Venstremenu_frag (daŭra)
└── Indhold_frag (dinamika)
    ├── Kanaler_frag
    │   └── Kanal_frag (po kanal per ViewPager)
    │       ├── Kanal_elem0_aktuel_udsendelse
    │       ├── Kanal_elem1_udsendelse
    │       └── Kanal_elem2_tidligere_senere
    ├── Favoritprogrammer_frag
    ├── Hentede_udsendelser_frag
    ├── Senest_lyttede_frag
    ├── Soeg_efter_program_frag
    ├── Udsendelse_frag
    ├── Kanalvalg_frag
    ├── Kontakt_info_om_frag
    └── Indstillinger_akt (aparta aktivaĵo)
└── Afspiller_frag (daŭra, malsupre)
```

## Ĉefaj komponantoj

### `Hovedaktivitet` (ĉefaktivaĵo)

Fragment-administrado, reenbutona traktado, `visFragment(Class, Bundle)` por
rekta navigado, `onOptionsItemSelected` por serĉo.

### `Venstremenu_frag` (navigacia tirkesto)

Tirkesto kun menueroj: Senest lyttede (supre), Favoritprogrammer (kun nombrilo
de novaj elsendoj per `App.backend.favoritter.getAntalNyeUdsendelser()`),
Hentede udsendelser (kun nombro), Vækkeur (kun sekva alarmtempo), apartigilo,
Kontakt/info/om, Indstillinger, Elektu kanalon. `VenstremenuAdapter extends
Basisadapter` konstruas `MenuElement`-ojn. `vælgMenu()` forigas la stakon kaj
anstataŭigas `R.id.indhold_frag`.

### `Kanaler_frag` (kanallisto)

ViewPager kun langetoj po kanal, **plej ŝatataj unue**, propra
`PagerSlidingTabStrip`, ŝovnavigado. `KanalAdapter extends
FragmentStatePagerAdapter implements PagerSlidingTabStrip.IconTabProvider` —
`getItem` vokas `Fragmentfabrikering.kanal(k)`, `getPageIconUrl` liveras
malpligrandigitan emblemo-URL.

### `Afspiller_frag` (ludilbreto)

Implements `Runnable, OnClickListener, SeekBar.OnSeekBarChangeListener`. Du
stattoj: **Malvolvigita** (mini-breto) / **Volvigita** (plenekrana). Komponantoj:
`startStopKnap`, `progressbar`, `kanallogo`, `direktetekst`, `metainformation`,
`udvidSkjulKnap`, `seekBar`, `starttid`, `slutttid`, `lydstyrke`.
`opdaterSeekBar`-Runnable afiŝas ĉiun 1000 ms; por livestream malŝaltas la
seekBar; por podkastoj fiksas max=longecoMs, progres pozicio, formatas pasintan
tempon. Klak-traktado: startStop baskulas ludi/paŭzi; kanallogo/direktetekst/
metainformation baskulas volvigita/malvolvigita; antaŭa/sekva butonoj. Serĉo:
`onProgressChanged` → `afspiller.seekTo(progress)`.

### `Kanal_frag` (unuopa kanalvido)

3-elementa strukturo: nuna elsendo (emblemo, rekta indikilo, titolo, ludbutono),
nuna/sekva programlisto, antaŭaj elsendoj kun datoj.

### `Fragmentfabrikering` (fragment-fabriko)

Centra fabriko — `kanal(Kanal)` → `Kanal_frag` kun `P_KANALSLUG`-bundle;
`udsendelse(Udsendelse)` → `Udsendelse_frag` kun `P_UDSUG`;
`favoritprogrammer()`, `hentedeUdsendelser()`, ktp.

## Propraj UI-komponantoj

| Komponanto | Rolo |
|---|---|
| `PagerSlidingTabStrip` | `HorizontalScrollView` kun `IconTabProvider`-interfaco |
| `Basisadapter` | baza ListView-adaptilo |
| `AnimationAdapter` | envolvaĵo por animacio |

## Aranĝoj (layout)

Ĉefaj: `hoved_akt.xml`, `kanaler_frag.xml`, `kanal_frag.xml`, `afspiller_frag.xml`,
`venstremenu_frag.xml`. Listeroj: `listeelem_2linjer.xml`, `udsendelse_elem*.xml`,
`kanal_elem*.xml`.

## Uzantinterago

- Ludi/paŭzi baskulo, serĉo (kun `seekBarBetjenesAktivt`-flago por paŭzigi ĝisdatigojn
  dum trenado), kuntekstaj menuoj (programo: elŝuti/forigi-elŝuton/plejŝati/kunhavigi/detaloj;
  alarmo: ebligi/malebligi/redakti/forigi), longa premo (programeroj → kunteksta menuo,
  kanallangetoj → rulumo, ludilbreto → volvigi).

## Temo kaj stilo

- **Propraj tiparoj:** `App.skrift_gibson`, `skrift_gibson_fed`, `skrift_georgia`
  (nune kromnomitaj al DEFAULT/DEFAULT_BOLD/SERIF). Aplikataj per AndroidQuery.
- **Koloroj:** `App.DRFarver` interna klaso: `grå40`, `blå`, `grå60`.
- **Propra stilo:** `DRTextAppearance` — textColor `text_primary`, 14sp, sans-serif.

## Sciigoj

- **Malfona servo** (`HoldAppIHukommelsenService`): `startForeground` kun sciigo
  (apikono, kanal/programnomo, ludi/paŭzi/halti, opcia progresbreto), `START_STICKY`.
- **Elŝuto:** tosto ĉe kompleto/malsukceso.
- **Alarmo:** `AlarmReceiver` → `AlarmAlert` plenekrana aktivaĵo kun sonludado,
  ripetludo, forigo, laŭteco, vibrigo.

## Alirebleco

Propraj tuŝdelegitoj (pligrandigita tuŝ-zono per `TouchDelegate`), enhavpriskriboj
por bildoj, tekstograndaĵoj. Limigoj: baza TalkBack, limigita legilkontensto,
neniu Switch Access, defaŭlta alta kontrasto.

## Lokalizado

- **Lingvoj:** Dana (da, defaŭlta), Esperanto (eo, plena). Dosieroj: `strings.xml`,
  `eo_strings.xml`, `eo_deskclock_strings.xml`.
- **Dinamika ŝanĝo:** `App.prefs.edit().putString("sprog","eo").commit()`,
  `App.sprogKonfig.locale = new Locale("eo")`, restart aktivaĵo, aplikata per
  `getResources().updateConfiguration(App.sprogKonfig, null)`.

## Responda dezajno

Defaŭlta portreto (povas devigi per `tving_lodret_visning`-agordo →
`SCREEN_ORIENTATION_PORTRAIT`). Tablo/pejzaĝaj alternativaj aranĝoj. Dimensio-
rimedoj: `dimens.xml`, `values-large`, `values-w820dp`. Bildo-skalado en
`Backend.ŝarĝiKanalEmblemojn`: malkodi bitmapon, malpligrandigi per potencoj de 2
dum alteco > 300, kaŝeni en `App.backend.kanallogo_eo`-mapo laŭ slug.

## Funkciaj areoj (resumo)

| Areo | Klasoj | Kion ĝi faras |
|---|---|---|
| Radiostreaming | `Afspiller`, `EmaPlayerWrapper`, `Kanal`, `Lydkilde` | MP3-fluado, dinamika kanalŝanĝo, bufro/konekto, laŭteco, antaŭa/sekva, sonfokuso, wifi-ŝloso |
| Podkast-administrado | `Backend`, `Grunddataparser`, `RomePodcastParser`, `EoRssParsado` | RSS/Atom-eltiro, elsendmontro, serĉo, radio.txt-integriĝo |
| Elŝutoj | `HentedeUdsendelser`, `HentetStatus` | Elŝuti por eksterrete; vico, progres, SD/kon interna, dosierorganizado |
| Vekhorloĝo | `Alarm`, `Alarms`, `AlarmReceiver`, `AlarmClock_akt`, `SetAlarm_akt` | Vektempoj kun radio kiel sonfonto, ripetaj alarmoj, elekti kanalon, ripetludo, vibrigo |
| Plej ŝatataj | `Favoritter` | Marki kanalojn, nombri novajn elsendojn, facila aliro |
| Lastaŭskultitaj | `SenestLyttede` | Spuri lastajn elsendojn/kanalojn, daŭriga pozicio, aŭskulttempo, aŭtomata daŭrigo |
