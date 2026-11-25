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

/** La profund-indego ({d}) de la historia eniro, kie la retumilo NUN estas. */
private fun historiaProfundoNun(): Int {
    js("return (history.state && history.state.d != null ? history.state.d : 0);")
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

    actual fun startu(onReen: () -> Unit, onReenProponata: () -> Boolean) {
        if (startigita) return
        startigita = true
        memoritaProfundo = 1
        historyAnstataŭiguStaton(1)
        popstateAŭsktilo { d ->
            when (val traktado = popstateTraktado(memoritaProfundo, d)) {
                PopstateTraktado.Nenio -> Unit

                PopstateTraktado.Ununivela -> {
                    // UNU reen-paŝo: la internaj traktiloj rajtas konsumi ĝin unue
                    // (NavigationBackHandler-paralelo — ekz. kolapso de la ludilbreto).
                    // Se konsumita, la retumilo jam forigis la historian eniron —
                    // rearmu ĝin (pushState), por ke la stako kaj la historio
                    // restu kongruaj kaj la SEKVA reen-butono popu la ekranon.
                    if (onReenProponata()) {
                        historyPuŝuStaton(memoritaProfundo)
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
                // La nav-stako profundiĝis (puŝo) — nova historia eniro
                historyPuŝuStaton(novaProfundo)
            } else {
                // Nur kiam la retumilo NE mem iris reen (enpropra reen-sago ktp.):
                // history.back(); nul = la historio jam kongruas (popstate direktis)
                historiaReeniro(memoritaProfundo, novaProfundo, historiaProfundoNun())?.let(::historyIruReen)
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
