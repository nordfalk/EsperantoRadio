---
name: esperanto-lingvo
description: Ŝargu ĉi tiun skill-on kiam vi skribas aŭ ŝanĝas Esperanto-tekston, identigilojn, komentojn, protokol-mesaĝojn, dokumentaron aŭ commit-mesaĝojn en ĉi tiu deponejo — por uzi la kanonajn terminojn kaj skribojn kaj eviti eraretojn kun supersignaj literoj (ŝ ĝ ĉ ĵ ŭ).
---

# Esperanto-lingvouzo en la kodo (kanonaj skriboj)

> Depo 2026-10-24 la kodo uzas NUR verajn supersignojn por ĉiuj identigiloj,
> klasoj kaj dosiernomoj (`ŝargi`, `ElŝutStato`, `SerĉoEkrano`, `ŜargaKontrolilo`).
> La historia x/h-sistemo restas NUR en persistigitaj valoroj (vidu malsupre)
> kaj en la malnova apo (`malnova/`).

## Ordonoj (laŭ graveco)

1. **Grep-u antaŭ ol skribi.** Se termino verŝajne jam ekzistas, trovu ĝin per
   `grep` kaj kopiu la ĝustan skribon. Kopiado preskaŭ ĉiam ĝustas;
   retajpado el memoro eraras (2026-10-20: `ambaix`/`ambaux` anstataŭ `ambaŭ`).
2. **Nova kodo nur veraj supersignoj** — la x-sistemo kaj la h-sistemo estas
   malpermesataj por novaj vortoj, identigiloj, klasoj kaj dosier-nomoj.
3. **Neniam miksu sistemojn** ene de unu vorto, kaj neniam skribu la x-on
   antaŭ la bazlitero (`ambaix`, `xsargxi` estas eraroj).
4. **Persistigitaj valoroj restas x-sistemaj** (kunmetaĵo de Android/Settings):
   ŝlosil-valoroj `"auxtomata_daurigo"`, `"kasxitaj_avertoj"`,
   `"apaj_novajxoj_hesh"` en `SettingsKeys`, la disk-kaŝmemora dosierujo
   `"elsendoj_kasho"`, kaj la `@SerialName`-ŝlosiloj
   (`retposhto`, `defaltSxatata`, `dauro`, `dauroMs`, `retpaghoUrl`) en
   `Modeloj.kt` — ili protektas ekzistantajn uzant-datumojn (naviga stato,
   lastajn ludatajn, elŝutojn). NE renomu ilin.
5. **Se `edit` fiaskas pro "string not found"**: serĉu per nuda ASCII-fragmento
   (`grep amba`, `grep ŝarg`) kaj legu la realan skribon el la eligo.
6. **Post-skribo-kontrolo**: post krei novan identigilon, `grep`-u ĝin.

## Vortaro de la domajno (kanonaj skriboj — post la renomo de 2026-10-24)

| Koncepto | Skribo | Ekzemploj |
|---|---|---|
| ŝargi (elreto) | `ŝargi…` | `ŝargiElsendojnPorKanal`, `ŜargaKontrolilo` |
| elŝuti | `elŝut…` | `ElŝutDeponejo`, `ElŝutitajEkrano`, `elŝutStato`, `ElŝutStato` |
| serĉi | `serĉ…` | `serĉiElsendojn`, `SerĉoEkrano`, `serĉoDeponejo` |
| ĝisdatigi | `ĝisdatigu…` | `ĜisdatiguSciigSkedon.kt` |
| kaŝi/kaŝmemoro | `kaŝ…` | `leguKaŝon`, `skribuKaŝon`, `DosierKaŝo.kt`, `kaŝmemoro` |
| elsendo / kanalo | `elsendo(j)`, `kanalo(j)` | `Elsendo`, `elsendoKontoj`, `KanalaroEkrano` |
| deponejo | `…Deponejo` | `ElsendoDeponejo`, `agordojDeponejo` |
| ludvico / ludilo | `ludvico…`, `Ludvico…`, `ludilo…` | `LudvicoRegilo`, `LudiloRegilo` |
| fluo (RSS) | `fluo(j)` | `podkastaRssUrl` |
| sciigo | `sciigo(j)` | `SciigoKontroloj`, `sciigPermeso` |
| novaĵoj | `novaĵ…` | `ApajNovaĵojDeponejo` |
| freŝa | `freŝ…` | `plejFreŝa`, `FreŝProcezaFallbackTest` — **sed `Refresh`/`refresh` en Material3-restas** (`PullToRefreshBox`, `isRefreshing`, `onRefresh`) |
| mesaĝo | `mesaĝ…` | `Eraro(val mesaĝo: …)` |
| poŝto / paĝo | `poŝt…`, `paĝ…` | `retpoŝto`, `retpaĝoUrl` |
| vekhorloĝo | `Vekhorloĝo`, `vekhorloĝo` | „Vekhorloĝo" |
| daŭro / daŭrigo | `daŭro`, `daŭri…` | `daŭroMs`, `aŭtomataDaŭrigo` |
| ĉiu / ĉe | `ĉiu…`, `ĉe` | `ĉiujElsendoj` — **sed `cx`/`cy` kiel koordinato-variabloj estas ne-Esperantaj** |
| antaŭ / hodiaŭ / morgaŭ | `antaŭ`, `hodiaŭ`, `morgaŭ` | `datoAntaŭ`, `unufojeHodiaŭ` |
| paŭzi | `paŭz…` | `paŭzigi`, `Paŭzita` |
| ŝalti | `ŝalt…` | `ŝaltita` |
| ŝanĝi | `ŝanĝ…` | `ŝanĝo`, `elfalditaŜanĝo` |
| ŝati | `ŝat…` | `defaŭltŜatata`, `plejŝatataj` |
| ŝlosilo | `ŝlosil…` | `IntentŜlosilo.kt` |
| -iĝ- | `…iĝ…` | `konserviĝas`, `haltiĝas`, `resumiĝo` |
| forĵeti | `forĵet…` | `forĵetasElsendojn` |

## Legi la historian x/h-sistemon (malnova apo, malnovaj protokoloj, persistaj valoroj)

- x-sistemo: **bazlitero + x** — `sx` = ŝ, `cx` = ĉ, `gx` = ĝ, `jx` = ĵ, `ux` = ŭ.
- h-sistemo (historie miksitaj): `gh` = ĝ (`ghisdatigu`), `sh` = ŝ (`poshto`,
  `malshalita`), `kash` = kaŝ (`leguKashon` en protokoloj antaŭ 2026-10-24).
- La h-sistemo estas tute malpermesata por nova kodo (`shargi`, `ghisdatigu`
  ŝtatan): ĝi kolizias kun veraj h-vortoj kaj restas ambigua.
