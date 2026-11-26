package dk.nordfalk.esperanto.navigation

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

// — JS-interop (Kotlin/Wasm, la js()-funkcio): la History-API kaj la DOM.
// Ĉiu historia eniro portas staton `{d: <profundo>}`, per kiu la popstate-traktilo
// distingas retumilan reen-iron de niaj propraj sinkronigoj.

private fun historyAnstataŭiguStaton(profundo: Int) {
    js("history.replaceState({d: profundo}, '');")
}

private fun historyPuŝuStaton(profundo: Int) {
    js("history.pushState({d: profundo}, '');")
}

private fun historyIruReen(niveloj: Int) {
    js("history.go(-niveloj);")
}

private fun popstateAŭsktilo(a: (Int) -> Unit) {
    js(
        """
        window.addEventListener('popstate', function(e) {
            a(e.state && e.state.d != null ? e.state.d : 0);
        });
        """,
    )
}

private fun agorduPaĝotitolon(titolo: String) {
    js("document.title = titolo;")
}

/** Pala-fadigas kaj forigas la ŝarĝekranon (index.html) kiam la UI pretas. */
private fun kaŝuŜarĝEkranon() {
    js(
        """
        var e = document.getElementById('ŝarĝekrano');
        if (e) {
            e.style.opacity = '0';
            setTimeout(function() { e.remove(); }, 400);
        }
        """,
    )
}

internal actual object RetumilaNavigado {
    private var startigita = false

    /** La nav-staka profundo, kiun ni laste reportis al la retumila historio. */
    private var memoritaProfundo = 1

    /**
     * Nia SINKRONA modelo de la historia pozicio, kie la retumilo staras AŬ
     * STAROS post jam planitaj trairoj. `history.state` ĝisdatiĝas NUR
     * nesinkrone post `history.go()` — dum rapida sinsekvo da agoj ĝi do
     * mensogas, kaj du `go()`-saltoj laŭ ĝi povas eliri el la paĝo (CodeRabbit,
     * PR #216). Ĉiu popstate re-sinkronigas ĝin kun la realo.
     */
    private var planitaProfundo = 1

    actual fun startu(onReen: () -> Unit, onReenProponata: () -> Boolean) {
        if (startigita) return
        startigita = true
        memoritaProfundo = 1
        planitaProfundo = 1
        historyAnstataŭiguStaton(1)
        popstateAŭsktilo { d ->
            // La popstate montras la veran pozicion — unue sinkronigu la modelon,
            // poste traktu laŭ la MEMORITA (antaŭa) profundo.
            val traktado = popstateTraktado(memoritaProfundo, d)
            planitaProfundo = d.coerceAtLeast(1)
            when (traktado) {
                PopstateTraktado.Nenio -> Unit

                PopstateTraktado.Ununivela -> {
                    // UNU reen-paŝo: la internaj traktiloj rajtas konsumi ĝin unue
                    // (NavigationBackHandler-paralelo — ekz. kolapso de la ludilbreto).
                    // Se konsumita, la retumilo jam forigis la historian eniron —
                    // rearmu ĝin (pushState), por ke la stako kaj la historio
                    // restu kongruaj kaj la SEKVA reen-butono popu la ekranon.
                    if (onReenProponata()) {
                        historyPuŝuStaton(memoritaProfundo)
                        planitaProfundo = memoritaProfundo
                    } else {
                        onReen()
                        memoritaProfundo = d.coerceAtLeast(1)
                    }
                }

                is PopstateTraktado.Plurnivela -> {
                    // Plurnivela salto (historia menuo): konsumo sen pop-efiko,
                    // poste popu ĉiujn nivelojn ĝis la elektita profundo.
                    onReenProponata()
                    repeat(traktado.nombro) { onReen() }
                    memoritaProfundo = d.coerceAtLeast(1)
                }
            }
        }
        kaŝuŜarĝEkranon()
    }

    actual fun stakŜanĝiĝis(novaProfundo: Int, titolo: String?) {
        if (startigita) {
            if (novaProfundo > memoritaProfundo) {
                // La nav-stako profundiĝis (puŝo) — UNU historia eniro po nivelo:
                // du rapidaj puŝoj antaŭ la efekto rulas povas salti plurajn nivelojn
                // (ekz. 1→3). Post antaŭen-trairo la eniroj super la nuna pozicio
                // jam kovras parton de la niveloj — REUZU ilin (pushState kun la
                // sama {d} kreiĝus duoble kaj skuus la sekvan reeniron).
                for (profundo in historiajPuŝoj(memoritaProfundo, planitaProfundo, novaProfundo)) {
                    historyPuŝuStaton(profundo)
                }
                planitaProfundo = maxOf(planitaProfundo, novaProfundo)
            } else {
                // Nur kiam la retumilo NE mem iris reen (enpropra reen-sago ktp.):
                // history.back(); nul = la historio jam kongruas (popstate direktis).
                // La paŝo eliras de la PLANITA pozicio — ne de history.state, kiu
                // post history.go() ankoraŭ montras la malnovan pozicion.
                historiaReeniro(memoritaProfundo, novaProfundo, planitaProfundo)?.let { paŝo ->
                    historyIruReen(paŝo)
                    planitaProfundo = planitaProfundo - paŝo
                }
            }
            // La profundo ĝisdatiĝas SINHRONE: popstate de nia propra history.back()
            // alvenos poste kun la nova profundo kaj ne aktivigos reenigon;
            // popstate de la RETUMILA reen-butono postulas la reenigon
            // (vidu popstateTraktadon kaj historiaReeniron).
            memoritaProfundo = novaProfundo
        }
        if (titolo != null) agorduPaĝotitolon(titolo)
    }
}
