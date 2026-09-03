# docs/nova — Plano por la nova EsperantoRadio

> La nova aplikaĵo estos konstruita en **Compose Multiplatform** (Android +
> iOS, opcie Desktop), kune kun memstara **servilo** kiu funkcias kiel arkivo
> de Esperanto-podkastoj. Ĉi tiu dosierujo priskribas la celan arkitekturon,
> teknikan stakon, domajnan modelon, parsadon, UI-dizajnon kaj la servilon.

## Legu-ordo

| # | Dokumento | Enhavo |
|---|---|---|
| 1 | [01_celoj_kaj_arkitekturo.md](./01_celoj_kaj_arkitekturo.md) | Celoj, principoj, tavoligita arkitekturo, modulo-strukturo |
| 2 | [02_teknika_stako.md](./02_teknika_stako.md) | Kotlin Multiplatform, Compose, Ktor, Koin, Media3/AVPlayer, ktp |
| 3 | [03_domajno_kaj_datumoj.md](./03_domajno_kaj_datumoj.md) | Domajnmodeloj, deponej-interfacoj, uzkazoj, datentavolo |
| 4 | [04_parsado_kaj_arkivo.md](./04_parsado_kaj_arkivo.md) | Reprodukto de la sep parsregoloj, golden-testoj, parser-kontrakto |
| 5 | [05_dizajno_kaj_ui.md](./05_dizajno_kaj_ui.md) | Muzaiko-temo, koloroj, tiparo, ekranoj, navigado |
| 6 | [06_servilo_arkivo.md](./06_servilo_arkivo.md) | La podkasta arkiv-servilo: API, inkrementa konstruo, servado |

## La granda ideo en unu frazo

Konservi la **scion pri la fontoj** (la plej valora parto) en komuna KMP-modulo
testebla kontraŭ golden fixtures, kaj konstrui ĉirkaŭ ĝi modernan plursistem-an
UI-on kaj servilon kiu normigas la rompiĝemajn fontojn servilflanke.

## Rilato al la malnova apo

| Malnova | Nova |
|---|---|
| `dk.dr.radio.*` (dana/esperanto-miksaĵo) | `dk.nordfalk.esperanto.*` (Esperanto) |
| Android-nur (Java + Kotlin) | Kotlin Multiplatform (Compose) |
| Singletona `App`-stato | Koin-DI + StateFlow |
| Fragmentoj + Volley | Compose + Ktor |
| ExoMedia | Media3 ExoPlayer (Android) / AVPlayer (iOS) |
| RssArkivServer (Java-jar) | KMP-servilo (Ktor-server aŭ Kotlin-script) |
| Malmolaj per-kanalaj apartaĵoj | Dateno-movita agordo (JSON/konfiguro) |
