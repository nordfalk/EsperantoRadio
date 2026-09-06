---
name: seancorezumo
description: Kreas HTML-resumon de cxiuj uzantomesagxoj kaj asistantaj respondoj el Vibe-seancoprotokoloj por cxiuj dosierujoj de la sama git-repo. Sxargxu cxi tiun skill kiam la uzanto petas resumon de siaj mesagxoj al Mistral Vibe.
user-invocable: true
allowed-tools: bash read_file
---

# Skill: Seancoresumo

Kreas HTML-dosieron kiu montras cxiujn uzantomesagxojn kaj la finajn asistantajn
respondojn el la Vibe-seancoprotokoloj por cxiuj dosierujoj de la sama git-repo
(kongrue laux git remote URL, ne nur laux preciza dosierujo).

## Kiam uzi

Kiam la uzanto petas ion kiel:
- "montru miajn mesagxojn al Vibe"
- "resumo de miaj seancoj"
- "kion mi faris dum la pasinta semajno"
- "kreu HTML-resumon de miaj mesagxoj"

## Procezo

1. Rulu la Python-skripton `krei_html.py` kiu trovigxas apud tiu cxi SKILL.md:
   ```bash
   python3 "$(dirname "$0")/krei_html.py" "$(pwd)" "$(pwd)/seancorezumo.html"
   ```
   (Anstatauxigu `$0` per la vojo al tiu dosiero.)

   Pli precize, rulu:
   ```bash
   python3 .vibe/skills/seancorezumo/krei_html.py "$(pwd)" "$(pwd)/seancorezumo.html"
   ```

2. La skripto:
   - Determinas la git-remote-URL de la nuna repo (`git remote get-url origin`)
   - Trovas cxiujn seanc-dosierujojn en `~/.vibe/logs/session/`
   - Inkluzivas seancojn el cxiuj dosierujoj kiuj havas la saman git-remote-URL
     (ekz. `EsperantoRadio` kaj `EsperantoRadio2` ambaux inkluzivigxas)
   - Legas `messages.jsonl` el cxiu seanco
   - Por cxiu uzantomesagxo (`role=user`, `injected=false`) trovas la finan
     asistantan respondon (lasta `role=assistant` kun `content` kaj sen `tool_calls`)
   - Fortondas mesagxojn pli longajn ol 1000 signojn
   - Ordigas seancojn kronologie
   - Generas HTML-grupigitan laux seanco, kun:
     - tempohoro por cxiu seanco
     - la dosierujo (cwd) en la session-header, emfazita flave kiam gxi
       sxtangxas inter seancoj
     - liston de cxiuj dosierujoj en la resumo
     - bluajn uzantomesagxojn kaj purpurajn asistantajn respondojn

3. Raportu al la uzanto:
   - Kiom da mesagxoj trovigxas
   - Kiom da seancoj kaj kiom da dosierujoj
   - La dosierindikon de la generita HTML
   - Sugestu malfermi gxin per `xdg-open`

## Notoj

- La skripto bezonas Python 3 (jam havebla en la medio)
- Ne necesas retkonekto
- La protokoloj trovigxas en `~/.vibe/logs/session/` (aux `$VIBE_HOME/logs/session/`)
- Se `VIBE_HOME` estas agordita, uzu gxin anstataux `~/.vibe`
