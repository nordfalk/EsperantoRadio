---
name: seancorezumo
description: Kreas HTML-resumon de ĉiuj uzantomesaĝoj kaj asistantaj respondoj el Vibe-seancoprotokoloj por ĉiuj dosierujoj de la sama git-repo. Ŝargu ĉi tiun skill kiam la uzanto petas resumon de siaj mesaĝoj al Mistral Vibe.
user-invocable: true
allowed-tools: bash read_file
---

# Skill: Seancoresumo

Kreas HTML-dosieron kiu montras ĉiujn uzantomesaĝojn kaj la finajn asistantajn
respondojn el la Vibe-seancoprotokoloj por ĉiuj dosierujoj de la sama git-repo
(kongrue laŭ git remote URL, ne nur laŭ preciza dosierujo).

## Kiam uzi

Kiam la uzanto petas ion kiel:
- "montru miajn mesaĝojn al Vibe"
- "resumo de miaj seancoj"
- "kion mi faris dum la pasinta semajno"
- "kreu HTML-resumon de miaj mesaĝoj"

## Procezo

1. Rulu la Python-skripton `krei_html.py` kiu troviĝas apud tiu ĉi SKILL.md:
   ```bash
   python3 "$(dirname "$0")/krei_html.py" "$(pwd)" "$(pwd)/seancorezumo.html"
   ```
   (Anstataŭigu `$0` per la vojo al tiu dosiero.)

   Pli precize, rulu:
   ```bash
   python3 .vibe/skills/seancorezumo/krei_html.py "$(pwd)" "$(pwd)/seancorezumo.html"
   ```

2. La skripto:
   - Determinas la git-remote-URL de la nuna repo (`git remote get-url origin`)
   - Trovas ĉiujn seanc-dosierujojn en `~/.vibe/logs/session/`
   - Inkluzivas seancojn el ĉiuj dosierujoj kiuj havas la saman git-remote-URL
     (ekz. `EsperantoRadio` kaj `EsperantoRadio2` ambaŭ inkluziviĝas)
   - Legas `messages.jsonl` el ĉiu seanco
   - Por ĉiu uzantomesaĝo (`role=user`, `injected=false`) trovas la finan
     asistantan respondon (lasta `role=assistant` kun `content` kaj sen `tool_calls`)
   - Fortondas mesaĝojn pli longajn ol 1000 signojn
   - Ordigas seancojn kronologie
   - Generas HTML-grupigitan laŭ seanco, kun:
     - tempohoro por ĉiu seanco
     - la dosierujo (cwd) en la session-header, emfazita flave kiam ĝi
       ŝtanĝas inter seancoj
     - liston de ĉiuj dosierujoj en la resumo
     - bluajn uzantomesaĝojn kaj purpurajn asistantajn respondojn

3. Raportu al la uzanto:
   - Kiom da mesaĝoj troviĝas
   - Kiom da seancoj kaj kiom da dosierujoj
   - La dosierindikon de la generita HTML
   - Sugestu malfermi ĝin per `xdg-open`

## Notoj

- La skripto bezonas Python 3 (jam havebla en la medio)
- Ne necesas retkonekto
- La protokoloj troviĝas en `~/.vibe/logs/session/` (aŭ `$VIBE_HOME/logs/session/`)
- Se `VIBE_HOME` estas agordita, uzu ĝin anstataŭ `~/.vibe`
