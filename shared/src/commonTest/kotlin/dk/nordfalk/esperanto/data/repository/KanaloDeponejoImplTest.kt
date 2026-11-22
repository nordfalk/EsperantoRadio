package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Testoj por [KanaloDeponejoImpl] — elŝuto de la kanalkonfiguro el la reto,
 * konservado en la DISKA kaŝmemoro kaj uzado anstataŭ la pakita version (kiel la
 * malnova apo: `App.initData` legas la konservitan, `hentEvtNyeGrunddata` elŝutas
 * la novan kaj konservas nur post sukcesa parsado).
 *
 * En commonTest (ĉiuj platformoj): la elŝuto kaj la persistigo estas injektataj
 * per lambdoj — neniu reala dosiero nek reto tuŝiĝas.
 */
class KanaloDeponejoImplTest {

    /** Eta memor-bazita Settings por testoj (kiel en ApajNovaĵojTest). */
    private class MapSettings : Settings {
        private val map = mutableMapOf<String, Any>()
        override val keys: Set<String> get() = map.keys.toSet()
        override val size: Int get() = map.size
        override fun clear() = map.clear()
        override fun remove(key: String) { map.remove(key) }
        override fun hasKey(key: String): Boolean = map.containsKey(key)
        override fun putInt(key: String, value: Int) { map[key] = value }
        override fun getInt(key: String, defaultValue: Int): Int = map[key] as? Int ?: defaultValue
        override fun getIntOrNull(key: String): Int? = map[key] as? Int
        override fun putLong(key: String, value: Long) { map[key] = value }
        override fun getLong(key: String, defaultValue: Long): Long = map[key] as? Long ?: defaultValue
        override fun getLongOrNull(key: String): Long? = map[key] as? Long
        override fun putString(key: String, value: String) { map[key] = value }
        override fun getString(key: String, defaultValue: String): String = map[key] as? String ?: defaultValue
        override fun getStringOrNull(key: String): String? = map[key] as? String
        override fun putFloat(key: String, value: Float) { map[key] = value }
        override fun getFloat(key: String, defaultValue: Float): Float = map[key] as? Float ?: defaultValue
        override fun getFloatOrNull(key: String): Float? = map[key] as? Float
        override fun putDouble(key: String, value: Double) { map[key] = value }
        override fun getDouble(key: String, defaultValue: Double): Double = map[key] as? Double ?: defaultValue
        override fun getDoubleOrNull(key: String): Double? = map[key] as? Double
        override fun putBoolean(key: String, value: Boolean) { map[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = map[key] as? Boolean ?: defaultValue
        override fun getBooleanOrNull(key: String): Boolean? = map[key] as? Boolean
    }

    /** Enmemora anstataŭaĵo de la diska kaŝmemoro. `n` = nombro da forigoj. */
    private class MemorKaŝo {
        var teksto: String? = null
        var n = 0
        var skriboj = 0
        var skribEraro: Exception? = null
        val legu: () -> String? = { teksto }
        val skribu: (String) -> Unit = {
            skribEraro?.let { e -> throw e }
            teksto = it; skriboj++
        }
        val forigu: () -> Unit = { teksto = null; n++ }
    }

    /** La "paktita" konfiguro (en la testo: konstanta teksto anstataŭ resurco). */
    private val paktitaA = """{ "kanaloj": [ { "kodo": "aaa", "nomo": "Kanalo A" } ] }"""
    /** La "reta" konfiguro, kiun la servilo redonas (kun alia kampo por kontroli getAgordo). */
    private val retaB = """{ "arkivoBazoUrl": "https://ekzemplo.dk/arkivo/", "kanaloj": [ { "kodo": "bbb", "nomo": "Kanalo B" } ] }"""

    private var alFreŝajAgordoj = 0

    private fun deponejo(
        kaŝo: MemorKaŝo = MemorKaŝo(),
        settings: Settings? = null,
        elŝuto: (suspend (String) -> String)? = null,
    ) = KanaloDeponejoImpl(
        leganto = KanalAgordoLeganto(),
        paktitaTeksto = { paktitaA },
        elŝutuDefore = elŝuto,
        settings = settings,
        leguKonservitan = kaŝo.legu,
        skribuKonservitan = kaŝo.skribu,
        foriguKonservitan = kaŝo.forigu,
        alFreŝaAgordo = { alFreŝajAgordoj++ },
    )

    @Test
    fun senKaŝo_uzasPakitan() = runTest {
        val d = deponejo()
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
        assertEquals("aaa", d.getKanalo("aaa")?.slug)
        assertNull(d.getKanalo("bbb"))
    }

    @Test
    fun kaŝitaKonfiguro_venkas_super_pakita() = runTest {
        val kaŝo = MemorKaŝo().apply { teksto = retaB }
        val d = deponejo(kaŝo = kaŝo)
        assertEquals(listOf("bbb"), d.getKanalojn(devige = false).map { it.slug })
        // getAgordo() liveras la kaŝitan agordon kun ĉiuj kampoj (ekz. arkivoBazoUrl)
        assertEquals("https://ekzemplo.dk/arkivo/", d.getAgordo().arkivoBazoUrl)
        assertEquals(0, kaŝo.n)
    }

    @Test
    fun rompitaKaŝo_forgesiĝas_kaj_rezervo_al_pakita() = runTest {
        val kaŝo = MemorKaŝo().apply { teksto = "{ ne estas json" }
        val d = deponejo(kaŝo = kaŝo)
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
        assertEquals(1, kaŝo.n, "La putrita kaŝo estu forigita por ne reprovi ĝin je ĉiu starto")
        assertNull(kaŝo.teksto)
    }

    @Test
    fun retaNovaKonfiguro_konserviĝas_uziĝas_kaj_callbackVokiĝas() = runTest {
        val kaŝo = MemorKaŝo()
        alFreŝajAgordoj = 0
        val d = deponejo(kaŝo = kaŝo, elŝuto = { url ->
            assertEquals(KanaloDeponejoImpl.KONFIGURO_URL_DE_RETO, url)
            retaB
        })

        d.refreŝiguElRete(devige = true)

        assertEquals(retaB, kaŝo.teksto)
        assertEquals(1, kaŝo.skriboj)
        assertEquals(listOf("bbb"), d.getKanalojn(devige = false).map { it.slug })
        assertEquals(listOf("bbb"), d.observiKanalojn().value.map { it.slug })
        assertEquals("https://ekzemplo.dk/arkivo/", d.getAgordo().arkivoBazoUrl)
        assertEquals(1, alFreŝajAgordoj)
    }

    @Test
    fun retaEraro_ne_ŝanĝas_la_lokan() = runTest {
        val kaŝo = MemorKaŝo().apply { teksto = paktitaA }
        alFreŝajAgordoj = 0
        val d = deponejo(kaŝo = kaŝo, elŝuto = { throw IllegalStateException("nenia reto") })

        d.refreŝiguElRete(devige = true) // ne devas ĵeti

        assertEquals(paktitaA, kaŝo.teksto)
        assertEquals(0, kaŝo.skriboj)
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
        assertEquals(0, alFreŝajAgordoj)
    }

    @Test
    fun jsMotorEraro_kaptiĝas_kiel_Throwable() = runTest {
        // La retumila Ktor-motoro (Js) ĵetas kotlin.Error (ne Exception) ĉe CORS-blokado
        val d = deponejo(elŝuto = { throw Error("Fail to fetch") })
        d.refreŝiguElRete(devige = true) // ne devas ĵeti
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
    }

    @Test
    fun retaRompitaJson_ne_konserviĝas() = runTest {
        val kaŝo = MemorKaŝo()
        // Fiaskpaĝo kun HTTP 200 (kaptiva portalo) — ne parseblas kiel JSON
        val d = deponejo(kaŝo = kaŝo, elŝuto = { "<html><body>404</body></html>" })

        d.refreŝiguElRete(devige = true)

        assertNull(kaŝo.teksto)
        assertEquals(0, kaŝo.skriboj)
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
    }

    @Test
    fun retaMalplenaKanalolisto_ne_akceptiĝas() = runTest {
        val kaŝo = MemorKaŝo()
        val d = deponejo(kaŝo = kaŝo, elŝuto = { """{ "kanaloj": [] }""" })

        d.refreŝiguElRete(devige = true)

        assertNull(kaŝo.teksto)
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
    }

    @Test
    fun samaTeksto_ne_rekonserviĝas_kaj_ne_callback() = runTest {
        val kaŝo = MemorKaŝo().apply { teksto = retaB }
        alFreŝajAgordoj = 0
        val d = deponejo(kaŝo = kaŝo, elŝuto = { retaB })

        d.refreŝiguElRete(devige = true)

        assertEquals(0, kaŝo.skriboj, "Ne re-skribu la saman tekston")
        assertEquals(0, alFreŝajAgordoj, "Neniu freŝa agordo — neniu callback")
        assertEquals(listOf("bbb"), d.getKanalojn(devige = false).map { it.slug })
    }

    @Test
    fun persistFiasko_ne_blokas_la_vivan_ĝisdatigon() = runTest {
        val kaŝo = MemorKaŝo().apply { skribEraro = IllegalStateException("plena disko") }
        alFreŝajAgordoj = 0
        val d = deponejo(kaŝo = kaŝo, elŝuto = { retaB })

        d.refreŝiguElRete(devige = true)

        assertEquals(listOf("bbb"), d.observiKanalojn().value.map { it.slug })
        assertEquals(1, alFreŝajAgordoj)
    }

    @Test
    fun ttl_evitas_tro_oftajn_elŝutojn() = runTest {
        val kaŝo = MemorKaŝo()
        val settings = MapSettings()
        var petoj = 0
        val d = deponejo(kaŝo = kaŝo, settings = settings, elŝuto = { petoj++; retaB })

        d.refreŝiguElRete(devige = true)   // unua: elŝutas
        assertEquals(1, petoj)
        d.refreŝiguElRete(devige = false)  // ene de la TTL: ne elŝutas
        assertEquals(1, petoj)
        d.refreŝiguElRete(devige = true)   // devigita (malsupren-tiro): elŝutas denove
        assertEquals(2, petoj)
    }

    @Test
    fun senElŝutanto_refreŝigo_nenion_faras() = runTest {
        val d = deponejo(elŝuto = null)
        d.refreŝiguElRete(devige = true)
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
    }
}
