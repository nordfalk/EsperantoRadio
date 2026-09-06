package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * KanaloDeponejo-implentaĵo. Legas la kanalkonfiguron el bundled resource.
 *
 * Estonte: ankaŭ elŝutas defore de https://javabog.dk/privat/esperantoradio_kanaloj_v9.json
 * kaj kaŝenas, kun rezervo al la bundled versio.
 */
class KanaloDeponejoImpl(
    private val leganto: KanalAgordoLeganto,
    private val bundledTeksto: () -> String,
) : KanaloDeponejo {

    private val _kanaloj = MutableStateFlow<List<Kanalo>>(emptyList())
    override fun observiKanalojn(): StateFlow<List<Kanalo>> = _kanaloj.asStateFlow()

    override suspend fun getKanalojn(fortoRefresigi: Boolean): List<Kanalo> {
        if (_kanaloj.value.isNotEmpty() && !fortoRefresigi) {
            logd("KanaloDeponejo", "Kanaloj jam ŝargitaj (${_kanaloj.value.size}) — uzas kaŝon")
            return _kanaloj.value
        }
        logi("KanaloDeponejo", "Legas kanalkonfiguron el bundled resource")
        val teksto = bundledTeksto()
        val agordo = leganto.legu(teksto)
        val kanaloj = agordo.kanaloj.map { it.alKanalo() }
        _kanaloj.value = kanaloj
        logi("KanaloDeponejo", "Ŝargis ${kanaloj.size} kanalojn")
        return kanaloj
    }

    override suspend fun getKanalo(slug: String): Kanalo? {
        return _kanaloj.value.find { it.slug == slug }
    }
}
