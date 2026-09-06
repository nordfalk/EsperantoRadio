#!/usr/bin/env python3
"""
Kreas HTML-resumon de cxiuj uzantomesagxoj kaj asistantaj respondoj
el Vibe-seancoprotokoloj por cxiuj dosierujoj de la sama git-repo.

Uzado:
    python3 krei_html.py <repo_dosierujo> [eligxdosiero]

Se eligxdosiero mankas, defauxlto estas <repo_dosierujo>/seancorezumo.html
"""

import json
import html
import os
import subprocess
import sys
from datetime import datetime


def get_text(content):
    """Ekstraktu platan tekston el content kampo (str au listo)."""
    if isinstance(content, list):
        return " ".join(
            str(x.get("text", "")) if isinstance(x, dict) else str(x)
            for x in content
        )
    return str(content) if content else ""


def get_git_remote(cwd):
    """Provu akiri la origin-remote-URL de git-dosierujo. Returnu None se fiaskas."""
    try:
        r = subprocess.run(
            ["git", "-C", cwd, "remote", "get-url", "origin"],
            capture_output=True, text=True, timeout=5,
        )
        if r.returncode == 0 and r.stdout.strip():
            return r.stdout.strip()
    except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
        pass
    return None


def find_sessions(vibe_home, repo_cwd):
    """
    Trovu cxiujn seancojn kies working_directory apartenas al la sama git-repo
    kiel repo_cwd (kongrue laux git remote URL).
    """
    session_dir = os.path.join(vibe_home, "logs", "session")
    if not os.path.isdir(session_dir):
        return []

    target_remote = get_git_remote(repo_cwd)

    all_sessions = []
    seen_cwds = {}
    for entry in sorted(os.listdir(session_dir)):
        full = os.path.join(session_dir, entry)
        if not os.path.isdir(full) or not entry.startswith("session_"):
            continue
        meta_path = os.path.join(full, "meta.json")
        msg_path = os.path.join(full, "messages.jsonl")
        if not os.path.isfile(msg_path):
            continue
        cwd = ""
        start_time = ""
        try:
            with open(meta_path) as mf:
                m = json.load(mf)
            cwd = m.get("environment", {}).get("working_directory", "")
            start_time = m.get("start_time", "")
        except (json.JSONDecodeError, FileNotFoundError, KeyError):
            pass
        if not cwd:
            continue
        all_sessions.append((entry, full, msg_path, cwd, start_time))
        if cwd not in seen_cwds:
            seen_cwds[cwd] = None

    # Determinu git-remote-URL por cxiuj unikaj dosierujoj
    if target_remote:
        for cwd in seen_cwds:
            if not os.path.isdir(cwd):
                rp = os.path.realpath(cwd)
                if os.path.isdir(rp):
                    seen_cwds[cwd] = get_git_remote(rp)
                else:
                    seen_cwds[cwd] = None
            else:
                seen_cwds[cwd] = get_git_remote(cwd)

    repo_basename = os.path.basename(os.path.normpath(repo_cwd))
    sessions = []
    for entry, full, msg_path, cwd, start_time in all_sessions:
        remote = seen_cwds.get(cwd)
        if target_remote and remote and remote == target_remote:
            sessions.append((entry, full, msg_path, cwd, start_time))
        elif not target_remote and (
            cwd == repo_cwd or os.path.realpath(cwd) == os.path.realpath(repo_cwd)
        ):
            sessions.append((entry, full, msg_path, cwd, start_time))
        elif not remote and os.path.basename(os.path.normpath(cwd)) == repo_basename:
            # Dosierujo forigita/sen git — inkluzivu se samnoma
            sessions.append((entry, full, msg_path, cwd, start_time))

    return sessions


def extract_pairs(msg_path):
    """
    Legu messages.jsonl kaj kolektu (user_text, assistant_final_text) paroj.
    La fina asistanta respondo estas la lasta assistant-mesagxo kun content
    kaj sen tool_calls antaux la venonta uzantomesagxo.
    """
    messages = []
    with open(msg_path) as f:
        for line in f:
            try:
                m = json.loads(line)
            except json.JSONDecodeError:
                continue
            messages.append(m)

    pairs = []
    i = 0
    while i < len(messages):
        m = messages[i]
        if m.get("role") == "user" and not m.get("injected", False):
            user_text = get_text(m.get("content", "")).strip()
            if not user_text:
                i += 1
                continue
            j = i + 1
            last_asst_text = ""
            while j < len(messages):
                nm = messages[j]
                if nm.get("role") == "user" and not nm.get("injected", False):
                    break
                if nm.get("role") == "assistant":
                    content = get_text(nm.get("content", "")).strip()
                    tc = nm.get("tool_calls")
                    if content and not tc:
                        last_asst_text = content
                    elif content and tc and not last_asst_text:
                        last_asst_text = content
                j += 1
            pairs.append((user_text, last_asst_text))
            i = j
        else:
            i += 1
    return pairs


def build_html(all_data, max_len, repo_cwd, target_remote):
    """Konstruu la HTML-eligxon."""
    out = []
    out.append("""<!DOCTYPE html>
<html lang="eo">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Seancorezumo — """ + html.escape(os.path.basename(repo_cwd)) + """</title>
<style>
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 900px; margin: 0 auto; padding: 20px; background: #f8f9fa; color: #222; }
h1 { font-size: 1.5em; border-bottom: 2px solid #6750A4; padding-bottom: 8px; }
.session { margin-bottom: 28px; border: 1px solid #ddd; border-radius: 8px; overflow: hidden; background: white; }
.session-header { background: #6750A4; color: white; padding: 8px 14px; font-size: 0.85em; font-weight: 600; display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.session-cwd { font-size: 0.85em; background: rgba(255,255,255,0.25); padding: 2px 8px; border-radius: 4px; font-weight: 400; }
.session-cwd.changed { background: #ffeb3b; color: #333; font-weight: 600; }
.exchange { padding: 12px 14px; border-bottom: 1px solid #eee; }
.exchange:last-child { border-bottom: none; }
.msg-time { font-size: 0.75em; color: #888; margin-bottom: 4px; }
.user-msg { background: #e8f0fe; border-left: 3px solid #1967d2; padding: 8px 12px; border-radius: 4px; white-space: pre-wrap; word-wrap: break-word; line-height: 1.5; }
.asst-label { font-size: 0.75em; color: #6750A4; font-weight: 600; margin-top: 10px; margin-bottom: 4px; }
.asst-msg { background: #f3e8ff; border-left: 3px solid #6750A4; padding: 8px 12px; border-radius: 4px; white-space: pre-wrap; word-wrap: break-word; line-height: 1.5; }
.truncated { color: #d32f2f; font-size: 0.8em; font-style: italic; margin-top: 4px; }
.no-response { color: #999; font-size: 0.85em; font-style: italic; }
.summary { background: #e8e0f0; padding: 10px 14px; border-radius: 8px; margin-bottom: 20px; font-size: 0.9em; }
</style>
</head>
<body>
<h1>Seancorezumo — """ + html.escape(os.path.basename(repo_cwd)) + """</h1>""")

    total_msgs = sum(len(pairs) for _, _, pairs, _ in all_data)
    dosierujoj = sorted({cwd for _, _, _, cwd in all_data})
    summary = f"Entute {total_msgs} mesagxoj el {len(all_data)} seancoj en {len(dosierujoj)} dosierujo(j). "
    if target_remote:
        summary += f"Repo: {html.escape(target_remote)}. "
    summary += f"Mesagxoj pli longaj ol {max_len} signoj estas fortonditaj."
    out.append(f'<div class="summary">{summary}</div>')

    if len(dosierujoj) > 1:
        out.append('<div class="summary"><b>Dosierujoj:</b><br>')
        for d in dosierujoj:
            out.append(f"{html.escape(d)}<br>")
        out.append("</div>")

    prev_cwd = None
    for sid, dato, pairs, cwd in all_data:
        if not pairs:
            continue
        cwd_changed = prev_cwd is not None and cwd != prev_cwd
        cwd_class = "session-cwd changed" if cwd_changed else "session-cwd"
        out.append('<div class="session">')
        out.append(
            f'<div class="session-header">'
            f'<span>{html.escape(sid)} — {html.escape(dato)}</span>'
            f'<span class="{cwd_class}">{html.escape(cwd)}</span>'
            f'</div>'
        )
        for user_text, asst_text in pairs:
            user_trunc = False
            u = user_text
            if len(u) > max_len:
                u = u[:max_len]
                user_trunc = True

            asst_trunc = False
            a = asst_text
            if len(a) > max_len:
                a = a[:max_len]
                asst_trunc = True

            out.append('<div class="exchange">')
            out.append(f'<div class="user-msg">{html.escape(u)}')
            if user_trunc:
                out.append(
                    f'<div class="truncated">[fortondita cxe {max_len} signoj]</div>'
                )
            out.append("</div>")
            out.append('<div class="asst-label">Respondo:</div>')
            if a:
                out.append(f'<div class="asst-msg">{html.escape(a)}')
                if asst_trunc:
                    out.append(
                        f'<div class="truncated">[fortondita cxe {max_len} signoj]</div>'
                    )
                out.append("</div>")
            else:
                out.append(
                    '<div class="no-response">[neniu fina teksta respondo registrita]</div>'
                )
            out.append("</div>")
        out.append("</div>")
        prev_cwd = cwd

    out.append("</body></html>")
    return "\n".join(out)


def main():
    if len(sys.argv) < 2:
        print("Uzado: python3 krei_html.py <repo_dosierujo> [eligxdosiero]", file=sys.stderr)
        sys.exit(1)

    repo_cwd = os.path.realpath(sys.argv[1])
    output_path = sys.argv[2] if len(sys.argv) > 2 else os.path.join(repo_cwd, "seancorezumo.html")
    max_len = 1000

    vibe_home = os.environ.get("VIBE_HOME", os.path.expanduser("~/.vibe"))

    sessions = find_sessions(vibe_home, repo_cwd)
    if not sessions:
        print(f"Neniu seanco trovita por {repo_cwd}", file=sys.stderr)
        sys.exit(1)

    target_remote = get_git_remote(repo_cwd)

    all_data = []
    for entry, full, msg_path, cwd, start_time in sessions:
        try:
            dt = datetime.fromisoformat(start_time.replace("Z", ""))
            dato = dt.strftime("%Y-%m-%d %H:%M")
        except (ValueError, TypeError):
            dato = entry

        pairs = extract_pairs(msg_path)
        if pairs:
            all_data.append((entry, dato, pairs, cwd))

    if not all_data:
        print("Neniu uzantomesagxo trovita.", file=sys.stderr)
        sys.exit(1)

    # Ordigu laux starttempo por intertempa ordo
    all_data.sort(key=lambda x: x[1])

    html_content = build_html(all_data, max_len, repo_cwd, target_remote)
    with open(output_path, "w") as f:
        f.write(html_content)

    total = sum(len(p) for _, _, p, _ in all_data)
    dosierujoj = sorted({cwd for _, _, _, cwd in all_data})
    print(f"Skribis {output_path} ({total} mesagxoj el {len(all_data)} seancoj en {len(dosierujoj)} dosierujoj)")


if __name__ == "__main__":
    main()
