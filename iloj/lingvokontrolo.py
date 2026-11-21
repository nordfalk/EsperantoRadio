#!/usr/bin/env python3
"""Lingva kontrolo: trovas fuŝitajn Esperanto-vortojn en ŝanĝitaj dosieroj.

Vortaro: REVO-bazita hunspell-vortaro (pok49/ispell-eo, oo/eo.dic + oo/eo.aff),
ŝargata per libhunspell (ctypes). Krome projekta permeslisto: iloj/terminoj.txt.

Uzado:
  python3 iloj/lingvokontrolo.py                 # kontrolas git diff $BAZO...HEAD (defaŭlte origin/master)
  python3 iloj/lingvokontrolo.py dosiero...      # kontrolas specifitajn dosierojn
  python3 iloj/lingvokontrolo.py -- tuta repo    # kontrolas ĉiujn .kt/.md/.json/.yaml (ekskl. server/data, .git)

Elirsto: "dosiero:linio: vorto" por ĉiu nekonata vorto; elirkodo 1 se estas trovoj.
Novajn legitajn terminojn (teknikaĵoj, slugs, nomoj) aldonu al iloj/terminoj.txt.
"""
import ctypes
import os
import re
import subprocess
import sys

RADIKO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AF = os.path.join(RADIKO, "iloj", "vortaro", "eo.aff")
DIC = os.path.join(RADIKO, "iloj", "vortaro", "eo.dic")
TERMINOJ = os.path.join(RADIKO, "iloj", "terminoj.txt")

# Vorto = literoj inkluzive supersignoj; poste kamelCase/dividite per _ kaj ciferoj
VORTO = re.compile(r"[A-Za-zĈĉĜĝĤĥĴĵŜŝŬŭ]+")
KAMEL = re.compile(r"[A-ZĈĜĤĴŜŬ]?[a-zĉĝĥĵŝŭ]+|[A-ZĈĜĤĴŜŬ]+(?![a-zĉĝĥĵŝŭ])")

# Nur PROZO estas kontrolata (ne kruda kodo):
# - .kt: komentoj (// kaj /* */) kaj tekstoĉenoj ("...")
# - .md/.yaml: la tuta teksto sen kodblokoj (```) kaj enlinia kodo (`...`) kaj URL-oj
# - .json: nur la valoroj de tekstaj kampoj
TEKSTO = re.compile(r'"((?:[^"\\]|\\.)*)"')
KOMENTO_LINIO = re.compile(r"//(.*)$")
URL = re.compile(r"https?://\S+|www\.\S+")


HTML_ETIKEDO = re.compile(r"<[^>]*>|&[a-z]+;|&#\d+;")


def purigu(t):
    t = URL.sub(" ", t)
    t = HTML_ETIKEDO.sub(" ", t)
    return t


def eltiru_prozon(dosiero, teksto):
    if dosiero.endswith(".kt"):
        pecoj = []
        for linio in teksto.splitlines():
            m = KOMENTO_LINIO.search(linio)
            if m:
                pecoj.append(m.group(1))
            pecoj.extend(TEKSTO.findall(linio))
        return purigu("\n".join(pecoj))
    if dosiero.endswith((".md", ".yaml", ".yml")):
        t = re.sub(r"```.*?```", " ", teksto, flags=re.S)  # kodblokoj
        t = re.sub(r"`[^`]*`", " ", t)                       # enlinia kodo
        return purigu(t)
    if dosiero.endswith(".json"):
        return purigu("\n".join(TEKSTO.findall(teksto)))
    return ""  # aliaj dosiertipoj (.py, ktp.): ne skani — kruda kodo, ne prozo


def ŝargu_vortaron():
    lib = ctypes.CDLL("libhunspell-1.7.so.0")
    lib.Hunspell_create.restype = ctypes.c_void_p
    lib.Hunspell_create.argtypes = [ctypes.c_char_p, ctypes.c_char_p]
    lib.Hunspell_spell.argtypes = [ctypes.c_void_p, ctypes.c_char_p]
    lib.Hunspell_spell.restype = ctypes.c_int
    h = lib.Hunspell_create(AF.encode(), DIC.encode())
    if not h:
        sys.exit(f"ERARO: ne povis ŝargi la vortaron el {AF} / {DIC} (ĉu iloj/vortaro/ ekzistas? vidu iloj/README.md)")
    return lib, h


def legu_permesliston():
    vortoj = set()
    if os.path.isfile(TERMINOJ):
        for linio in open(TERMINOJ, encoding="utf-8"):
            linio = linio.strip()
            if linio and not linio.startswith("#"):
                vortoj.add(linio.lower())
    return vortoj


def kontrolu_tekston(lib, h, permesitaj, teksto, dosiero, rezultoj):
    prozo = eltiru_prozon(dosiero, teksto)
    for nr, linio in enumerate(prozo.splitlines(), 1):
        for v in VORTO.findall(linio):
            for parto in KAMEL.findall(v):
                w = parto.lower()
                if len(w) < 3 or w in permesitaj or len(w) > 30:
                    continue
                if lib.Hunspell_spell(h, w.encode("utf-8")) == 0:
                    rezultoj.append((dosiero, nr, w))


def ĉefaj_dosieroj(argj):
    if argj == ["--"]:
        for rad, _, dosieroj in os.walk(RADIKO):
            if any(p in rad for p in ("/.git", "/server/data", "/build", "/.gradle", "/iloj/vortaro")):
                continue
            for d in dosieroj:
                if d.endswith((".kt", ".md", ".json", ".yaml", ".yml", ".xml")):
                    yield os.path.join(rad, d)
        return
    if argj:
        for d in argj:
            yield d if os.path.isabs(d) else os.path.join(RADIKO, d)
        return
    bazo = os.environ.get("BAZO", "origin/master")
    out = subprocess.run(["git", "diff", "--name-only", f"{bazo}...HEAD"],
                         capture_output=True, text=True, cwd=RADIKO).stdout
    for d in out.splitlines():
        if d.endswith((".kt", ".md", ".json", ".yaml", ".yml", ".xml")) and not d.startswith("server/data/"):
            yield os.path.join(RADIKO, d)


def main():
    argj = [a for a in sys.argv[1:] if a != "--generu-permesliston"]
    generi = "--generu-permesliston" in sys.argv
    if generi and not argj:
        argj = ["--"]
    lib, h = ŝargu_vortaron()
    permesitaj = legu_permesliston() if not generi else set()
    rezultoj = []
    for dosiero in ĉefaj_dosieroj(argj):
        try:
            teksto = open(dosiero, encoding="utf-8").read()
        except (OSError, UnicodeDecodeError) as e:
            print(f"ERARO {dosiero}: {e}", file=sys.stderr)
            continue
        kontrolu_tekston(lib, h, permesitaj, teksto, dosiero, rezultoj)
    if generi:
        # skribas ĉiujn nekonatajn vortojn de la tuta repo kiel novan permesliston
        vortoj = sorted({w for _, _, w in rezultoj})
        with open(TERMINOJ, "w", encoding="utf-8") as f:
            f.write("# Aŭtomate generita permeslisto (--generu-permesliston). Ne forigu la kontrolitan vortaron supre.\n")
            f.write("# Aldonu permane uzatajn novajn terminojn suben.\n\n")
            f.write("\n".join(vortoj) + "\n")
        print(f"Permeslisto generita: {len(vortoj)} vortoj -> iloj/terminoj.txt")
        return
    if rezultoj:
        for dosiero, nr, w in rezultoj:
            print(f"{os.path.relpath(dosiero, RADIKO)}:{nr}: {w}")
        print(f"\n{len(rezultoj)} nekonata(j) vorto(j). Se legitaj: aldonu al iloj/terminoj.txt.")
        sys.exit(1)
    print("Lingva kontrolo: neniuj fuŝitaj vortoj trovitaj.")


if __name__ == "__main__":
    main()
