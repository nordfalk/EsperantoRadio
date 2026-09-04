package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.alKanal
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.repository.KanalDeponejo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * KanalDeponejo-implentaĵo. Legas la kanalkonfiguron el bundled resource.
 *
 * Estonte: ankaŭ elŝutas defore de https://javabog.dk/privat/esperantoradio_kanaloj_v9.json
 * kaj kaŝenas, kun rezervo al la bundled versio.
 */
class KanalDeponejoImpl(
    private val leganto: KanalAgordoLeganto,
    private val bundledTeksto: () -> String,
) : KanalDeponejo {

    private val _kanaloj = MutableStateFlow<List<Kanal>>(emptyList())
    override fun observiKanalojn(): StateFlow<List<Kanal>> = _kanaloj.asStateFlow()

    override suspend fun getKanalojn(fortoRefresigi: Boolean): List<Kanal> {
        if (_kanaloj.value.isNotEmpty() && !fortoRefresigi) {
            return _kanaloj.value
        }
        val teksto = bundledTeksto()
        val agordo = leganto.legu(teksto)
        val kanaloj = agordo.kanaloj.map { it.alKanal() }
        _kanaloj.value = kanaloj
        return kanaloj
    }

    override suspend fun getKanal(slug: String): Kanal? {
        return _kanaloj.value.find { it.slug == slug }
    }
}
