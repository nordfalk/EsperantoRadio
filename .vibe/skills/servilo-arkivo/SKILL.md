---
name: servilo-arkivo
description: Ŝargu ĉi tiun skill-on kiam temas pri la arkiv-servilo de EsperantoRadio — la Kotlin-modulo server/ (arkiv-konstruilo), la publika elŝutejo ĉe javabog.dk, la dispozicio publika/privata, STT-transskriboj kaj AI-rezultoj, aŭ la fluon ruli kaj publikigi (rsync).
---

# Servilo-arkivo (server/)

Nia servilo estas **simplaj elŝuteblaj dosieroj** (ne HTTP-API ankoraŭ): la enhavo de
`server/data/publika/` estas servata de Apache kiel statikaj dosieroj.

## Kanona datum-loko (2026-11-24)

**`/home/j/Code/EsperantoRadio/server/data/`** estas la ĉefloko de la servilaj datumoj
(la 2,7 GB kun: `privata/audio/`, `modelo/`, `transkriboj/`, `rezumoj/` …).
### Fina decido pri la ligiloj (2026-11-24) — reproduktenda en ĈIU kontrolilo

Laborkopioj (ekz. `mobile-app4/`) divididas la kanonan arbon per **SUB-LIGILOJ, ne
per tuta-direktoria ligilo** (konsideritaj alternativoj: tuta ligilo — poluigebla
de akcidentaj skriboj kaj tuteunuebliga por git; nur subligiloj en data/* —
sen versiado de la prezentpaĝo). La fina strukturo:

```
server/data/                      ← REALA
├── privata → <kanona>/privata    ← LIGILO (la hereda fluoj/ NE ligiĝas)
└── publika/                      ← REALA
    ├── index.html, icon.png      ← REALAJ, GIT-VERSIAJ (aŭtoritato)
    ├── kanaloj/    → <kanona>/publika/kanaloj    ┐
    ├── bildoj/     → <kanona>/publika/bildoj     │ LIGILOJ
    ├── apo/        → <kanona>/publika/apo        │ (de PR #210 la
    └── esperantoradio_konfiguro.json → <kanonan> ┘  apo-nomo malnova-speca)
```

**Kial**: `publika/` devas esti REALA, por ke git povu versii la prezentpaĝon;
`privata` povas esti unu ligilo (nenio versienda); akcidenta skribo en
`server/data/` restas LOKALA; la prezentpaĝo restas PR-reviebla. LA PUBLIKIGA
RSYNC DEVAS UZI `-L`.

**Recepto por nova kontrolilo** (post `git clone` la du realaj dosieroj jam
ekzistas el git; nur la ligiloj mankas):

```bash
cd <repo>
ln -s /home/j/Code/EsperantoRadio/server/data/privata  server/data/privata
ln -s /home/j/Code/EsperantoRadio/server/data/publika/kanaloj  server/data/publika/kanaloj
ln -s /home/j/Code/EsperantoRadio/server/data/publika/bildoj   server/data/publika/bildoj
ln -s /home/j/Code/EsperantoRadio/server/data/publika/apo     server/data/publika/apo
ln -s /home/j/Code/EsperantoRadio/server/data/publika/esperantoradio_konfiguro.json  server/data/publika/esperantoradio_konfiguro.json
```

(Ekzistanta kontrolilo SEN la versiita paro: antaŭe `cp` ankaŭ `index.html` kaj
`icon.png` el la kanona publika.)
**Portempaj dosieroj** (logaroj, prov-skriptoj, mezuradoj) apartenas al
`tmp/` ĉe la radiko de la propra laborkopio (gitignorita) — NENIAM al la
kanona arbo, kiu estas dividebla kun aliaj taskoj kaj agentoj.
La prezentpaĝo (`publika/index.html`, `icon.png`) estas GIT-VERSIAJ realaj dosieroj —
ŝanĝu per PR; post mergo SKRIBU ankaŭ la kanonan kopion (aŭ rulu la publikigan rsync -L,
kiu publikigas la laborkopian version).

## Publika baz-URL

```
https://javabog.dk/esperanto-radio/        (= server/data/publika/ lokale)
├── index.html + icon.png                (prezentpaĝo — en laborkopio GIT-VERSIAJ realaj dosieroj)
├── esperantoradio_konfiguro.json        (purigita, strikta JSON; kopiiĝas je ĉiu rulo;
│                                          la emblemoUrl-oj jam resskribitaj al bildoj/)
├── kanaloj/<slug>.xml                   (29 riĉigitaj RSS-fluoj, RSS 2.0 + iTunes)
├── bildoj/<slug>.<png|jpg|gif|webp>     (emblemo-spegulo — C56, por samdevena Coil-ŝargo)
└── apo/                                 (produkta wasmJs-fasko de la retversio — C56:
                                           .js/.wasm/index.html, ~17 MB; NENIAM privatajn
                                           dosierojn ĉi tien)
```

**La retversio (C56, 2026-11-24) gastigas samdevene** sub `apo/`: sur la retumilo
`uzuOriginajnFluojn=false` (AppStato laŭ `platformo`) — nur la arkivaj fluoj kaj la
spegulitaj bildoj estas ŝargataj (la originaloj estas CORS-blokitaj); la sono ludas
rekte el la fonto per HTML-`<audio>` (ne CORS-bezona). Konstruo:
`./gradlew :webApp:kopiuAlPublikaElsutejo` (kopias el
`webApp/build/dist/wasmJs/productionExecutable/` al `publika/apo/`), poste la kutima rsync.

La **apo konsumas tiujn fluojn kunfandante kun la originalaj**: `arkivoBazoUrl` en
`esperantoradio_konfiguro.json` → `ElsendoDeponejoImpl` elŝutas paralele
`<arkivoBazoUrl><slug>.xml` kaj la originalan `elsendojRssUrl` kaj kunfandas la rezultojn
per `kunfanduElsendojn` (laŭ ID: la arkivo venkas; la originalo aldonas la novajn
elsendojn, do ili aperas tuj eĉ se la arkivo estas malfreŝa). Se ambaŭ fiaskas, la apo
uzas la kaŝon (enmemora, poste diska). La arkivaj fluoj portas la finan ID-on kiel
`guid` (`<slug>:…`) — tial `deArkivo=true` ĉe la parsado konservas ĝin (stabilaj ID-oj
inter fontoj). Vidu `docs/nova/04_parsado_kaj_arkivo.md` → „Arkivo-unue“.

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

**Formato de la rezumo-dosiero** (plata teksto):
- nedeviga unua linio `Titolo: …` — anstataŭas la titolon de la elsendo en la publika fluo;
- poste **unua linio = la temo** (resumo de la sekvaj punktoj), poste **unu punkto po linio**;
- la konstruilo aldonas la AI-averton kaj konvertas ĉion al HTML: la tem-linio iĝas
  `<p><strong>…</strong></p>` (titolo-stila), la punktoj `<ul><li>`, la averto `<em>` —
  tiel la rezulto estas legebla en la detala ekrano same kiel la priskriboj de aliaj
  elsendoj. Se mankas `Titolo:` kaj la originala titolo enhavas daton (ripetiĝanta
  titolo, ekz. „Esperanto Programo 18.11.2026“), la titolo estas derivita el la unua
  linio de la rezumo — vidu `analizuRezumon`/`derivuTitolon`/`rezumoAlHtml` en
  `shared/.../data/parser/RezumoHtml.kt` (kun testoj en `RezumoHtmlTest`).

## Publikigi (rsync)

**REGULO (2026-11-20): neniam lanĉu la rsync al la servilo sen eksplicita peto de la uzanto** („kopiu al la servilo“ k.s.). Loka konstruado kaj kontrolo de `server/data/publika/` estas en ordo; la publikigo ĉiam atendas aprobon.

**REGULO (posedanto, 2026-11-25): `publika/apo/` (la produkta reta apo) ne ŝanĝiĝas sen eksplicita peto.**
Ŝanĝoj al la reta apo inter eldonoj iras al **`publika/apo-beta/`**
(https://javabog.dk/esperanto-radio/apo-beta/) — la defaŭlta Gradle-tasko estas
`:webApp:kopiuAlApoBetaElsutejo` (ĝi kreas la dosierujon mem kaj enmetas la Sentry-version);
la kutima rsync de `publika/` publikigas la betaon AŬTOMATE (ĝi estas ja parto de `publika/`).
`publika/apo/` tuŝiĝas NUR per `:webApp:kopiuAlApoElsutejo` kiam la posedanto diras
ekz. «publikigu la novan version de la reta apo al apo/». Sur la servilo `apo-beta/`
ekzistas ekde 2026-11-25 (lok-tenila index.html).

```bash
# unu rsync — publika/ estas la tuta radiko de la retejo (index.html, icon.png, kanaloj/, konfiguro)
# nur post eksplicita peto de la uzanto!
rsync -avLz --chmod=D755,F644 server/data/publika/ javabog.dk:javabog.dk/esperanto-radio/
# ATENTU: --chmod=644 (sen D755) detruas la plenumilon de dosierujoj — tiam Apache redonas 403 por ĉio
# kontrolo:
curl -s https://javabog.dk/esperanto-radio/kanaloj/ | head -3
```

**Prezentpaĝo**: `server/data/publika/index.html` (+ `icon.png`) estas servata kiel
`https://javabog.dk/esperanto-radio/` (la prezentpaĝo `index.html` estas git-versia reala
dosiero en la laborkopio — vidu malsupre);
ne forgesu `--chmod=D755,F644` (alie Apache redonas 403).

## Regula rulado

cron/systemd: `./gradlew :server:arkivKonstruilo` + `rsync` (ekz. ĉiun horon).
Unu kanalo fiaskanta ne haltigas la aliajn (regulo A4); eraroj en `privata/stato.json`.

## Kaptilo: Apache kaj .wasm (C56, 2026-11-24)

- Apache 2.4.29 je javabog.dk NE konas la typ-on `application/wasm` (`/etc/mime.types`
  mankas la linion; **sen ĝi la retumilo rifuzas lanĉi la faskon** en `apo/`:
  "Incorrect response MIME type. Expected 'application/wasm'"), kaj
  `AllowOverride None` en la `DocumentRoot` (`/etc/apache2/sites-enabled/10_javabog*.conf`
  → `<Directory /diverse/javabog/html>`) mortigas ĉiun `.htaccess` (probita: `AddType`
  ne efikas). Sudo necesas por la permana sistem-fikso:
  `echo "application/wasm wasm" | sudo tee -a /etc/mime.types && sudo systemctl reload apache2`
  (elektita de la posedanto 2026-11-24). Kontrolu per
  `curl -sI https://javabog.dk/esperanto-radio/apo/<hash>.wasm | grep -i content-type`.
  Rezervvojo se mime.types ne korekteblas: mod_php 7.2 ruliĝas en tiu dosierujo —
  `<hash>.wasm.php`-ĉirkaŭpakaĵo kun `header('Content-Type: application/wasm')` + ŝarg-intercepto.
- La fasko-konstruo kopias per `:webApp:kopiuAlPublikaElsutejo` (ne permane) —
  la elirejo estas `build/dist/wasmJs/productionExecutable/` (NE `browserProduction`;
  2026-11-24: erara podo igis la Copy-taskon trankvile krei malplenan publikan dosierujon).

## Pliaj detaloj

- Plano/fazo 2 (Ktor HTTP-API, JSON): `docs/nova/06_servilo_arkivo.md`
- STT-esploro kun rezultoj: PR #157, `docs/nova/08_subtekstado.md`
- Malnova Java-servilo (referenco): `docs/malnova/05_arkiva_servilo.md`
