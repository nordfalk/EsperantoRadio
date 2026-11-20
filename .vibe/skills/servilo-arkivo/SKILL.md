---
name: servilo-arkivo
description: Ŝargu ĉi tiun skill-on kiam temas pri la arkiv-servilo de EsperantoRadio — la Kotlin-modulo server/ (arkiv-konstruilo), la publika elŝutejo ĉe javabog.dk, la dispozicio publika/privata, STT-transskriboj kaj AI-rezultoj, aŭ la fluon ruli kaj publikigi (rsync).
---

# Servilo-arkivo (server/)

Nia servilo estas **simplaj elŝuteblaj dosieroj** (ne HTTP-API ankoraŭ): la enhavo de
`server/data/publika/` estas servata de Apache kiel statikaj dosieroj.

## Publika baz-URL

```
https://javabog.dk/esperanto-radio/publika/
├── esperantoradio_konfiguro.json      (kopiita aŭtomate je ĉiu rulo)
└── kanaloj/<slug>.xml                 (28 riĉigitaj RSS-fluoj, RSS 2.0 + iTunes)
```

## Dispozicio

```
server/data/
├── publika/     ← PUBLIKU: kanaloj/*.xml + esperantoradio_konfiguro.json
└── privata/     ← NE publikigu: transkriptoj/, rezumoj/, audio/, modelo/, esr/,
                   kaŝo/, fluoj/, skriptoj/, stato.json, logoj
```

- **Transskriboj kaj rezumoj estas PRIVATAJ** (ne pretaj / krudaj). La publikaj fluoj
  enhavas nur la pretan AI-rezulton en `<description>` (4–9 linioj + fina frazo
  „Rezumo farita de la sondosiero per AI. Eraroj povas okazi“), neniam
  `podcast:transcript`-ligilon.
- `server/data/` estas gitignored tute.

## Ruli la konstruilon (Kotlin, parsado per la sama RssParsilo kiel la apo)

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew :server:arkivKonstruilo                      # ĉiuj kanaloj (rete)
./gradlew :server:arkivKonstruilo --args="radiovatikana"   # nur unu kanalo
./gradlew :server:arkivKonstruilo --args="--kaŝo"     # el kaŝo, sen reto
```

## STT (provizore Python — modelo de PR #157)

```bash
N_POR_KANALO=10 MAKS_DAURO_MIN=30 WORKERS=4 \
  python3 server/data/privata/skriptoj/stt_chiujn.py   # → privata/transkriptoj/
```

Modelo: `server/data/privata/modelo/` (NVIDIA Esperanto-Conformer INT8 ONNX,
`OpenVoiceOS/nvidia-eo-conformer-transducer-large-onnx`, CC-BY-4.0), ilo:
`server/data/privata/esr/` (BitsAndDopamine/Esperanto-SRT, CC0).
Dosiernom-regulo (sama en Kotlin kaj Python): sanitarigita id (maks 60 signoj) + `_` + md5(id)[:8].

## Rezultoj (LLM-prilaboro)

Pretaj rezultoj iras en `server/data/privata/rezumoj/<slug>/<dosiernomo>.txt`;
la konstruilo preferas ilin antaŭ la ekstrakta fallback. Se mankas ambaŭ, la ero
restas sen priskribo (kaj tio notiĝas en la konzolo kaj `privata/stato.json`).

## Publikigi (rsync)

```bash
rsync -avz server/data/publika/ javabog.dk:javabog.dk/esperanto-radio/publika/
rsync -avz --chmod=644 server/index.html server/icon.png javabog.dk:javabog.dk/esperanto-radio/
# kontrolo:
curl -s https://javabog.dk/esperanto-radio/publika/ | head
```

**Prezentpaĝo**: `server/index.html` (+ `server/icon.png`) estas servata kiel
`https://javabog.dk/esperanto-radio/` — ne forgesu, ke la dosieroj havu permesojn 644
(Apache alie redonas 403).

## Regula rulado

cron/systemd: `./gradlew :server:arkivKonstruilo` + `rsync` (ekz. ĉiun horon).
Unu kanalo fiaskanta ne haltigas la aliajn (regulo A4); eraroj en `privata/stato.json`.

## Pliaj detaloj

- Plano/fazo 2 (Ktor HTTP-API, JSON): `docs/nova/06_servilo_arkivo.md`
- STT-esploro kun rezultoj: PR #157, `docs/nova/08_subtekstado.md`
- Malnova Java-servilo (referenco): `docs/malnova/05_arkiva_servilo.md`
