#!/bin/sh
# Diras, en kiu klono Vibe-seanco kuras, kiam ĝi finas sian vicon aŭ demandas
# ion, sed nur se la vico komenciĝis antaŭ pli ol 5 minutoj — alie vi verŝajne
# ankoraŭ rigardas. Klonoj ne listigitaj ĉi tie silentas.
#
#   ludu.sh komenco   ĉe ĉiu ilvoko: notas la komencon de la vico
#   ludu.sh demando   kiam la agento demandas vin
#   ludu.sh fino      kiam la vico finiĝas

# kiam la aviz-sono de `.vibe/sonoj/ludu.sh` ne aŭdiĝas post pasivigo de la komputilo, sufiĉas restartigi la uzant-nivelan sonservon:
# systemctl --user restart pipewire wireplumber pipewire-pulse

# Subagentoj: legu stdin nur se ĝi ne estas terminalo — mane vokite la skripto
# alie blokus je grep, kiu atendas EOF de la klavaro.
if [ ! -t 0 ]; then
  grep -q '"parent_session_id": *"' && exit 0  # subagentoj
fi

cd "$(dirname "$0")"

case "$(git rev-parse --show-toplevel)" in
  "/home/j/Code/EsperantoRadio")  sono=unu.wav ;;
  "/home/j/Code/EsperantoRadio2") sono=du.wav ;;
  "/home/j/Code/EsperantoRadio3") sono=tri.wav ;;
  "/home/j/Code/mobile-app4")     sono=kvar.wav ;;
  *) exit 0 ;;
esac

komenco="$(git rev-parse --git-path vibe-vico-komenco)"

if [ "$1" = komenco ]; then
  [ -f "$komenco" ] || touch "$komenco"
  exit 0
fi

if [ -f "$komenco" ] && [ -z "$(find "$komenco" -mmin -5)" ]; then
  if command -v afplay >/dev/null; then
    afplay "$sono" >/dev/null 2>&1 &
  else
    paplay "$sono" >/dev/null 2>&1 &
  fi
fi

if [ "$1" = fino ]; then
  rm -f "$komenco"
fi
