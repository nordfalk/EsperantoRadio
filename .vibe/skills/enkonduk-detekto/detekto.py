#!/usr/bin/env python3
"""Enkonduk-detekto (FAROTA C59b) - ĉu kanalo havas ripetiĝantan enkondukon?

Legas la kanalkonfiguron, elŝutas la plej freŝajn N elsendojn de ĉiu
kanalo (120 s, 8 kHz mono PCM), komparas iliajn komencojn per spekto-
grama kosino-simileco kun ŝovo-serĉo kaj rekomendas valoron por
`transsaltuKomenceSekundojn` po kanalo.

Rulu:  python3 .vibe/skills/enkonduk-detekto/detekto.py --n 5
"""
import argparse
import itertools
import json
import os
import re
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor

import numpy as np

AGORDO = "shared/src/commonMain/resources/esperantoradio_konfiguro.json"
# Persista kaŝo (RssArkivServer-filcache estas .gitignore-ita): la PCM-dosieroj
# postvivas restartojn kaj estas reuzataj de venontaj ruloj — ne re-elŝutu ilin.
LABOREJO = "RssArkivServer-filcache/enkonduk-detekto"
SR = 8000
NA = 512      # FFT-grandeco
PASO = 256    # 32 ms
MAKS_SEK = 120  # kiom da sekundoj elŝuti po elsendo


# ---------------- lego kaj elŝuto ----------------

def legu_agordon(vojo):
    s = open(vojo, encoding="utf-8").read()
    s = re.sub(r"^\s*//.*$", "", s, flags=re.M)   # // -komentoj
    s = re.sub(r",(\s*[}\]])", r"\1", s)           # vostaj komoj
    # plurliniaj \-daŭrigoj: kunigu
    s = re.sub(r"\\\s*\n", "", s)
    return json.loads(s)["kanaloj"]


def sonaj_url_oj(el):
    """Elŝuteblaj sond URL-oj de RSS-ero: enclosure aŭ <audio>-mp3 (P6.2)."""
    u = re.search(r'<enclosure[^>]*url="([^"]+)"', el)
    if u:
        return u.group(1)
    m = re.findall(r'(https://[^"\s\\]+\.mp3)', el)
    return m[0] if m else None


def elŝutu(kanalo, indekso, url):
    """Elŝutu la unuajn MAKS_SEK s kiel PCM; redonas la dosier-vojon aŭ None."""
    celo = f"{LABOREJO}/pcm/{kanalo}_{indekso}.raw"
    if os.path.exists(celo) and os.path.getsize(celo) > 100_000:
        return celo
    try:
        r = subprocess.run(
            ["ffmpeg", "-loglevel", "error", "-y", "-i", url,
             "-t", str(MAKS_SEK), "-ac", "1", "-ar", str(SR),
             "-f", "s16le", celo],
            capture_output=True, timeout=120)
    except subprocess.TimeoutExpired:
        return None
    if r.returncode != 0 or not os.path.exists(celo) or os.path.getsize(celo) < 100_000:
        return None
    return celo


def preparu_kanalon(kanalo, n):
    """Elŝutu la n plej freŝajn elsendojn de kanalo; redonas liston de PCM-vojooj."""
    kodo = kanalo["kodo"]
    url = kanalo.get("elsendojRssUrl")
    if not url:
        return kodo, None, "neniu RSS-fluo (rekta kanalo)"
    try:
        r = subprocess.run(["curl", "-s", "--max-time", "25", url,
                           "-o", f"{LABOREJO}/fluoj/{kodo}.xml"],
                          capture_output=True, timeout=30)
    except subprocess.TimeoutExpired:
        return kodo, None, "fluo-erarejo (tempolimo)"
    if not os.path.exists(f"{LABOREJO}/fluoj/{kodo}.xml"):
        return kodo, None, "fluo ne elŝutebla"
    enhavo = open(f"{LABOREJO}/fluoj/{kodo}.xml", encoding="utf-8", errors="replace").read()
    eroj = re.findall(r"<item>(.*?)</item>", enhavo, re.S)[:n]
    vojoj, eraro = [], None
    for i, el in enumerate(eroj):
        su = sonaj_url_oj(el)
        if not su:
            continue
        p = elŝutu(kodo, i, su)
        if p:
            vojoj.append(p)
        elif not eraro:
            eraro = f"elsendo {i} ne elŝutebla"
    if len(vojoj) < 2:
        return kodo, None, eraro or f"nur {len(vojoj)} elsendoj haveblaj"
    return kodo, vojoj, None


# ---------------- analizo ----------------

def legu_pcm(vojo):
    x = np.fromfile(vojo, dtype=np.int16).astype(np.float64) / 32768.0
    return x


def trajtoj(x, maksimume_sek=35):
    x = x[: maksimume_sek * SR]
    n_f = (len(x) - NA) // PASO
    if n_f < 30:
        return None
    fenestro = np.hanning(NA)
    el = np.empty((n_f, NA // 2))
    for i in range(n_f):
        kadro = x[i * PASO: i * PASO + NA] * fenestro
        el[i] = np.abs(np.fft.rfft(kadro))[: NA // 2]
    return np.log1p(el)


def kadra_simileco(A, B):
    na = np.linalg.norm(A, axis=1)
    nb = np.linalg.norm(B, axis=1)
    valida = (na > 0.5) & (nb > 0.5)   # silent-filtrilo
    if valida.sum() < 30:
        return None
    a, b = A[valida], B[valida]
    s = (a * b).sum(1) / (np.linalg.norm(a, axis=1) * np.linalg.norm(b, axis=1))
    return s.mean()


def glata_kurbo(A, B, lag):
    """1-sekunda glata simileco-kurbo ĉe la donita ŝovo."""
    if lag >= 0:
        a, b = A, B[lag:]
    else:
        a, b = A[-lag:], B
    n = min(len(a), len(b))
    a, b = a[:n], b[:n]
    na = np.linalg.norm(a, axis=1)
    nb = np.linalg.norm(b, axis=1)
    valida = (na > 0.5) & (nb > 0.5)
    s = np.zeros(n)
    s[valida] = (a[valida] * b[valida]).sum(1) / (na[valida] * nb[valida])
    f1 = int(SR / PASO)
    el = [np.nanmean(s[t:t + f1]) for t in range(0, n - f1, f1)]
    return el


def analizu_paron(A, B):
    """(maks_simileco, ŝovo, komuna_fino_s) — la lasta daŭra >=0.85-segmento en la unuaj 30 s."""
    ŝovoj = [int(d * 0.5 * SR / PASO) for d in range(-30, 31)]
    plej = (0.0, 0)
    for lag in ŝovoj:
        if lag >= 0:
            a, b = A, B[lag:]
        else:
            a, b = A[-lag:], B
        n = min(len(a), len(b))
        m = kadra_simileco(a[:n], b[:n])
        if m is not None and m > plej[0]:
            plej = (m, lag)
    if plej[0] < 0.5:
        return plej[0], plej[1], 0
    kurbo = glata_kurbo(A, B, plej[1])[:30]
    # trovu ĉiujn daŭrajn (>=3 s) segmentojn >= 0.85; prenu la FINON de la LASTA
    segmentoj = []
    komenco = None
    for t, v in enumerate(kurbo + [0.0]):
        if v >= 0.85 and komenco is None:
            komenco = t
        elif v < 0.85 and komenco is not None:
            if t - komenco >= 3:
                segmentoj.append((komenco, t))
            komenco = None
    if not segmentoj:
        return plej[0], plej[1], 0
    return plej[0], plej[1], segmentoj[-1][1]


def analizu_kanalon(vojoj, detale=False):
    E = [trajtoj(legu_pcm(v)) for v in vojoj]
    if any(e is None for e in E):
        return None, "tro mallongaj sondatumoj", []
    paroj = []
    for i, j in itertools.combinations(range(len(E)), 2):
        m, lag, kongrua = analizu_paron(E[i], E[j])
        paroj.append((i, j, m, lag, kongrua))
    return paroj


# ---------------- verdikto ----------------

def verdikto(paroj):
    """(kvalifikas, rekomendo, noto) el la par-rezultoj (p[4] = komuna_fino_s)."""
    kongruaj = [p for p in paroj if p[4] > 0]
    if not kongruaj:
        return False, 0.0, "neniu paro kun da\u016dra >=0.85-segmento"
    prop = len(kongruaj) / len(paroj)
    if prop < 0.67:
        return False, 0.0, f"nur {len(kongruaj)}/{len(paroj)} paroj kongruas"
    finoj = sorted(p[4] for p in kongruaj)
    if finoj[-1] - finoj[0] > 3:
        return False, 0.0, f"kongruaj finoj tro disaj: {finoj}"
    rekomendo = float(finoj[0])  # minimumo = konservativa
    return True, round(rekomendo, 1), f"komunaj finoj: {finoj}"


# ---------------- ĉefo ----------------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--n", type=int, default=5, help="kiom da plej fre\u015daj elsendoj")
    ap.add_argument("--kanaloj", default="", help="koma-disigita listo de kodoj (malplena = \u0109iuj)")
    ap.add_argument("--detale", action="store_true")
    args = ap.parse_args()

    os.makedirs(f"{LABOREJO}/pcm", exist_ok=True)
    os.makedirs(f"{LABOREJO}/fluoj", exist_ok=True)

    kanaloj = legu_agordon(AGORDO)
    celoj = [k for k in kanaloj if k.get("elsendojRssUrl")]
    if args.kanaloj:
        volataj = set(args.kanaloj.split(","))
        celoj = [k for k in celoj if k["kodo"] in volataj]
    print(f"Kanaloj por analizo: {len(celoj)} (po {args.n} elsendoj)\n")

    # 1) el\u015dutoj (paralele, 4 fadenoj)
    preparitaj = []
    with ThreadPoolExecutor(max_workers=4) as ekz:
        estontecoj = [ekz.submit(preparu_kanalon, k, args.n) for k in celoj]
        for f in estontecoj:
            kodo, vojoj, eraro = f.result()
            if vojoj:
                preparitaj.append((kodo, vojoj))
                print(f"  {kodo}: {len(vojoj)} elsendoj el\u015ditaj")
            else:
                print(f"  {kodo}: PRETERLASITA ({eraro})")
    print()

    # 2) analizo
    superrigordo = []
    for kodo, vojoj in preparitaj:
        paroj = analizu_kanalon(vojoj, detale=args.detale)
        if paroj is None:
            superrigordo.append((kodo, None, "analizo fiaskis", ""))
            continue
        kvalas, rekomendo, noto = verdikto(paroj)
        if args.detale:
            for i, j, m, lag, kongrua in paroj:
                print(f"  {kodo} {i}<->{j}: simileco={m:.3f} \u015dovo={lag*PASO/SR:+.1f}s komuna_fino={kongrua}s")
        superrigordo.append((kodo, rekomendo if kvalas else 0.0,
                             "KOMUNA ENKONDUKO" if kvalas else "ne", noto))

    # 3) superrigordo
    print("\n===== SUPERRIGONDO =====")
    print(f"{'kodo':24} {'verdikto':18} {'valoro':7} noto")
    for kodo, valoro, verd, noto in superrigordo:
        print(f"{kodo:24} {verd:18} {valoro!s:7} {noto}")
    print("\nEnmetu la valorojn per: \u011disdatigu esperantoradio_konfiguro.json")
    print("(nur la linioj kun KOMUNA ENKONDUKO; 0 = lasu la kampon for/malplena)")


if __name__ == "__main__":
    main()
