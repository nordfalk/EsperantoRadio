# docs/nova — Plano kaj arkitekturo de la nova EsperantoRadio

> La nova aplikaĵo estas konstruita en **Compose Multiplatform** (Android +
> Desktop + Web/Wasm; iOS ankoraŭ ne havas Xcode-projekton). Kune kun estonta
> memstara **servilo** kiu funkcios kiel arkivo de Esperanto-podkastoj.
> Ĉi tiu dosierujo priskribas la celan arkitekturon, teknikan stakon, domajnan
> modelon, parsadon, UI-dizajnon kaj la servilon.
>
> Por la plej ĝisdatigitan staton de la projekto, vidu `AGENTS.md` en la radiko.

## Legu-ordo

| # | Dokumento | Enhavo |
|---|---|---|
| 1 | [01_celoj_kaj_arkitekturo.md](./01_celoj_kaj_arkitekturo.md) | Celoj, principoj, tavoligita arkitekturo, modulo-strukturo, faza stato |
| 2 | [02_teknika_stako.md](./02_teknika_stako.md) | Kotlin Multiplatform, Compose, Ktor, Media3/mp3spi, versioj |
| 3 | [03_domajno_kaj_datumoj.md](./03_domajno_kaj_datumoj.md) | Domajnmodeloj, deponej-interfacoj, datentavolo, ludila abstraktado |
| 4 | [04_parsado_kaj_arkivo.md](./04_parsado_kaj_arkivo.md) | La parsregoloj (6.1–6.7 + 6.8/CRI), golden-testoj, parser-kontrakto |
| 5 | [05_dizajno_kaj_ui.md](./05_dizajno_kaj_ui.md) | Muzaiko-temo, koloroj, tiparo, ekranoj, navigado (navigation3) |
| 6 | [06_servilo_arkivo.md](./06_servilo_arkivo.md) | La podkasta arkiv-servilo (plano — ankoraŭ ne implementita) |
| 7 | [07_eldonado.md](./07_eldonado.md) | Eldonado al Google Play, F-Droid, Aptoide + CI/CD (GitHub Actions) |
| 8 | [08_cri_esperanto_kanalo.md](./08_cri_esperanto_kanalo.md) | CRI Esperanto: situacio, HLS-sonformatoj, solvo-propono (peranto), Android-kanalo (regulo 6.8) kaj demonstra transkodilo |
| — | [GHISDATIGO_COMPOSE_1.10.md](./GHISDATIGO_COMPOSE_1.10.md) | Lernitaj lecionoj de la ĝisdatigo al Compose 1.10 / Kotlin 2.2.20 |

## La granda ideo en unu frazo

Konservi la **scion pri la fontoj** (la plej valora parto) en komuna KMP-modulo
testebla kontraŭ golden fixtures, kaj konstrui ĉirkaŭ ĝi modernan plursistem-an
UI-on kaj servilon kiu normigas la rompiĝemajn fontojn servilflanke.

## Rilato al la malnova apo

| Malnova | Nova |
|---|---|
| `dk.dr.radio.*` (dana/esperanto-miksaĵo) | `dk.nordfalk.esperanto.*` (Esperanto) |
| Android-nur (Java + Kotlin) | Kotlin Multiplatform (Compose) |
| Singletona `App`-stato | Permana injektado + StateFlow |
| Fragmentoj + Volley | Compose + Ktor |
| ExoMedia | Media3 ExoPlayer (Android) / AVPlayer (iOS) |
| RssArkivServer (Java-jar) | KMP-servilo (Ktor-server aŭ Kotlin-script) |
| Malmolaj per-kanalaj apartaĵoj | Dateno-movita agordo (JSON/konfiguro) |
