# docs/malnova — Superrigordo de la malnova EsperantoRadio-apo

> Ĉi tiu dosierujo priskribas la **malnovan** Android-apon — kiel ĝi estas
> konstruita, kiel datumoj fluas, kiel la parsado funkcias, kiel la UI aspektas,
> kaj kiel la arkiva servilo funkcias. Ĝi estas la referenco por la rekreado.
>
> **Noto:** La malnovaj moduloj (`app/`, `parse/`, `data/`) moviĝis al la
> dosierujo `malnova/`. La nova Compose Multiplatform-apo vivos en la radiko
> (unuigita Kotlin-DSL-build). La Gradle-modulnomoj restas `:app`/`:parse`/`:data`.

## Legu-ordo

| # | Dokumento | Enhavo |
|---|---|---|
| 1 | [01_strukturo_kaj_konstruo.md](./01_strukturo_kaj_konstruo.md) | Moduloj, Gradle, dependencoj, manifest |
| 2 | [02_datumfluo.md](./02_datumfluo.md) | Pakoj, enirejoj, datumfluo reto→UI, ŝlosilaj klasoj |
| 3 | [03_parsado_kaj_fontoj.md](./03_parsado_kaj_fontoj.md) | **Plej grava.** Kanal-konfiguro, radio.txt, la sep parsregoloj, skip-listo |
| 4 | [04_ui_kaj_funkcioj.md](./04_ui_kaj_funkcioj.md) | Fragmentoj, navigado, ludilo, elŝutoj, plej ŝatataj, vekhorloĝo |
| 5 | [05_arkiva_servilo.md](./05_arkiva_servilo.md) | RssArkivServer — la memstara CLI-servilo kiu konstruas la arkivon |

## La esenco en unu frazo

La plej valora parto de ĉi tiu projekto ne estas la UI aŭ ludilo — tiuj estas
ordinaraj. La valora parto estas la **scio pri la fontoj**: kiuj kanaloj ekzistas,
kiel iliaj fluoj aspektas, kaj kiel eltiri rektan MP3-URL el ĉiu. Tiu scio vivas
en `03_parsado_kaj_fontoj.md` kaj devas esti konservita en la nova apo.

## Suplemento

La plej teknikaj detaloj (konkretaj XML-elfragmentoj, atendataj test-valoroj)
troviĝas en `03_parsado_kaj_fontoj.md` kaj en `../nova/04_parsado_kaj_arkivo.md`.
La dana dokumentaro estis forigita; ĉiu grava enhavo estas konservita en
tiu ĉi Esperanto-versio.
