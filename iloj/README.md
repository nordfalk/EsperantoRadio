# Iloj — lingva kontrolo (trovi fuŝitajn Esperanto-vortojn)

`lingvokontrolo.py` skanas la **prozon** (komentoj kaj tekstoĉenoj en `.kt`,
dokumentoj sen kodblokoj, tekstajn valorojn en `.json`) de la ŝanĝitaj dosieroj

 kaj markas vortojn, kiujn la Esperanto-vortaro ne konas — tio kaptas fuŝitajn
vortojn kiel `temro` (→ `tempolimo`) kaj x-sistemajn formojn.

## Vortaro

La vortaro estas REVO-bazita (pok49/ispell-eo, `oo/eo.dic` + `oo/eo.aff`),
ŝargata per `libhunspell` (ctypes). Ĝi ne estas en la deponejo — elŝutu ĝin:

```bash
mkdir -p iloj/vortaro && cd iloj/vortaro
curl -sL https://raw.githubusercontent.com/pok49/ispell-eo/master/oo/eo.dic -o eo.dic.raw
curl -sL https://raw.githubusercontent.com/pok49/ispell-eo/master/oo/eo.aff -o eo.aff.raw
{ echo "SET UTF-8"; cat eo.aff.raw; } > eo.aff
{ wc -l < eo.dic.raw; cat eo.dic.raw; } > eo.dic
rm eo.dic.raw eo.aff.raw
```

Sistemo: `libhunspell-1.7.so.0` (Debian/Ubuntu: `sudo apt-get install libhunspell-1.7-0`).

## Uzado

```bash
python3 iloj/lingvokontrolo.py                  # ŝanĝitaj dosieroj de git diff $BAZO...HEAD (defaŭlte origin/master)
python3 iloj/lingvokontrolo.py dosiero.kt ...   # specifitaj dosieroj
python3 iloj/lingvokontrolo.py --generu-permesliston   # regeneras iloj/terminoj.txt el la tuta repo
```

Elirkodo 1 + listo `dosiero:linio: vorto` se troviĝas nekonata vorto.
Legitimajn novajn terminojn (teknikaĵoj, slugs, nomoj) aldonu al `iloj/terminoj.txt` —
tio estas la „ratchet“-modelon: nur NOVajn nekonatajn vortojn la kontrolo markas.

## Uzado fare de la agento (ne de CI)

La posedanto intence ne volas CI-kontrolon por tio — la agento mem fojfoje rulas
`python3 iloj/lingvokontrolo.py` (ekz. ĉe PR-revizio, vidu AGENTS.md) kaj korektas
trovitajn fuŝitajn vortojn aŭ aldonas legitajn novajn terminojn al `iloj/terminoj.txt`.
