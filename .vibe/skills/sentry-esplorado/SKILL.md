---
name: sentry-esplorado
description: Ŝargu ĉi tiun skill-on kiam vi devas legi aŭ analizi Sentry-datumojn per la mcp_sentry-konekto — serĉi erarojn/problemojn/protokolojn, mezuri uzantojn po versio aŭ build, kontroli kvotojn, aŭ diagnozi raportitajn problemojn de la produktada apo.
---

# Sentry-esplorado (esperantoradio / projekto KMP)

Organizo `esperantoradio`, regiono `de.sentry.io`, projekto `KMP`.
Agordo en la apo: `SentryAgordo.kt` — medio "produktado" por release-konstruoj,
`release = "esperantoradio@<apoversio>"`, `dist` = versionCode (aŭtomate de la
Android-SDK). **Nur release-konstruoj sur realaj aparatoj raportas** —
debug-konstruoj, Desktop kaj emuliloj ne inicialigas Sentry-n.

## Konekto (MCP) kaj ĝiaj malsanoj

La oficiala servilo `https://mcp.sentry.dev/mcp` estas konektita kiel `mcp_sentry`
(OAuth; agordo en `~/.vibe/config.toml`). Se la konekto mankas aŭ fiaskas:

1. **Iloj tute malaperis el la nomspaco** → rulu `vibe mcp add sentry --url
   https://mcp.sentry.dev/mcp` (lanĉas retumilan ensaluton; atendu "OAuth login
   completed") kaj **restartigu Vibe** (`vibe -c`) — la iloj ŝargiĝas nur je
   seanco-starto.
2. **Iloj restas videblaj sed ĉiu voko fiaskas** per `effect_recovery_failed:
   RustInvalidStateRejection(command_type='reconfigure', state='running')`
   (harness-replay-tavolo, nekaptrebla per try/catch) → same nur restarto de Vibe.
   (Ambaŭ variantoj: 2026-10-10/13.)
3. **Iloj mankas en la seanco, sed la agordo ekzistas** (2026-10-17) → laborema
   pretervojo sen restarto: parolu rekte al la servilo per JSON-RPC super
   `POST https://mcp.sentry.dev/mcp` (streamable HTTP; kapo `Mcp-Session-Id` de la
   respondo de `initialize`, poste `notifications/initialized`, poste `tools/call`).
   La OAuth-ĵetonoj estas en la OS-kringo: servo `ai.mistral.vibe`, uzanto
   `mcp-oauth:sentry:tokens` (atributoj), klient-id en `mcp-oauth:sentry:client_info`.
   Se la ĵetono eksvalidiĝis, refreŝigu ĝin per `grant_type=refresh_token` kontraŭ
   **`https://mcp.sentry.dev/oauth/token`** (laŭ `/.well-known/oauth-authorization-server`
   de `mcp.sentry.dev`) — NE kontraŭ `sentry.io/oauth/token` (tie `invalid_client`)
   nek `de.sentry.io` (404) — kaj konservu la novan ĵetonon reen en la keringo.

## La plej uzataj iloj

- `find_organizations` / `find_projects` — trovu la slug-ojn.
- `search_issues` — grupigitaj problemoj (query: `is:unresolved`, `level:error`,
  `lastSeen:-7d`; sort: `date`/`freq`/`user`/`recommended`).
- `get_sentry_resource` — detaloj pri unu problemo (stacktrace, eventoj);
  `resourceType='issue'`, `resourceId='KMP-X'`.
- `search_logs` — protokol-agregaĵoj. Ekzemple:
  - Plej oftaj mesaĝoj: `fields: ['count()', 'message']`, `sort: '-count()'`.
  - Uzantoj po eldono: `fields: ['release', 'count_unique(user.id)']` — proksimumo
    de aktivaj uzantoj.
  - Tempo-serio: petu naturlingve «... as a time series» kun
    `fields: ['timestamp', 'count_unique(user.id)']` → tagaj/horaj siteloj.
- `search_errors` — erar-eventoj, nombroj, tendencoj (po tago/horo per time series).
- `find_dropped_events` — ĉu protokoloj/eraroj estis forĵetitaj pro kvoto
  (rezulto `droppedEvents: []` = neniuj perdoj).
- `update_issue` — solvigi/atribui problemon.

## Datumaro-kapabloj (grava por demandoj)

- **logs** (protokoloj): `user.id`, `device.model`, `release`, `message` — sed
  **neniu geo-kampon** kaj **nenian `dist`-kampon** (build-numero — grupigo per
  `dist` ĵetas API-eraron; protokoloj ne distingeblas per build, nur per release).
- **errors** (erar-eventoj): havas `geo.country_code` kaj **`dist` (= versionCode)** —
  sed `device.model` estas malplena (la KMP-SendSDK ne sendas aparat-kuntekston).
  Aparato × lando NE estas rekombebla per la API.
- Negaciaj filtriloj (`!device.model:...`) ne funkcias en logs — prenu ĉiujn grupojn
  (`limit: 100`) kaj filtru loke.
- `find_projects` bezonas `organizationSlug` (sen ĝi: skemo-validiga eraro).

## Interpreto-avertoj pri uzant-nombroj

- `user.id` estas **anonima po instalaĵo** — reinstalo = nova uzanto-id.
- Emulilaj testreinstaloj rekoneblas: `device.model:sdk_gphone*` (Google-emulilaj
  bildoj); ekde la 249-gardo ili tute ne raportas.
- `count_unique(device.model)` nombras unikajn **modelajn ĉenojn**, ne aparatojn.
- Landoj haveblas nur el erar-eventoj (do nur por uzantoj kun almenaŭ unu eraro) —
  la nombro subtaksas.
- **`dist` distingas versio-identajn rekonstruojn** (247/248/249 = ĉiuj "3.0.3"):
  eraroj jes (per `dist:`), protokoloj ne. Indirekte 249-uzantoj rekoneblas per la
  "detala protokolado"-defaŭlto (ekde 249 ŝaltita): detalaj mesaĝoj
  ("parsado kompleta", "[Nav] …") povas veni nur de 249 aŭ de manaj ŝaltintoj.

## Kvotoj kaj specimenoj (kiel ilin legi)

- La **erar-kvoto** (5 000/monato) konsumiĝas nur de `captureException`
  (`loge(tag, msg, e)`) — `Sentry.logger.*` (protokoloj) estas aparta kvoto.
- Ekde 249: eraroj estas **50 %-specimenataj** en produktado (`options.sampleRate`)
  — krudaj nombroj de 249 estas duone intence reduktitaj; atendataj fiaskoj
  (reto/ludado/elŝuto/serĉo) estas `logw` kaj tute ne aperas kiel erar-eventoj.
- Malnovaj versioj (≤ 248) ankoraŭ generas la malnovajn erarojn — komparu laŭ `dist`.
