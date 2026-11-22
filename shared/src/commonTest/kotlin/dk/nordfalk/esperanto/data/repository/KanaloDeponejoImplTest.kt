package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import dk.nordfalk.esperanto.data.config.FORA_KONFIGURO_URL
import dk.nordfalk.esperanto.data.config.KanalAgordo
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testoj por la fora kanalkonfiguro (kiel la «grunddata»-fluo de la malnova apo):
 * elŝuto de la servilo, konservado en la diska kaŝmemoro, uzado de la kaŝita
 * versio ĉe relanĉo, rezervo al la pakita versio kaj toleremo al ĉiaj fiaskoj (A4).
 *
 * La persistigo kaj la elŝuto estas injektataj per lambdoj — neniu reala
 * dosiero aŭ reto tuŝiĝas.
 */
class KanaloDeponejoImplTest {

    /** Eta memor-bazita Settings por testoj (sama stilo kiel AgordoMigradoTest). */
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

    /** Enmemora persistigo (anstataŭ la diska kaŝmemoro). */
    private class MemorPersisto {
        var teksto: String? = null
        var forigitaj = 0
        var skribEraro: Throwable? = null
        val legu: () -> String? = { teksto }
        val forigu: () -> Unit = { teksto = null; forigitaj++ }
        val skribu: (String) -> Unit = { if (skribEraro != null) throw skribEraro!! else teksto = it }
    }

    private fun konfiguro(vararg kodoj: String): String =
        """{ "kanaloj": [ ${kodoj.joinToString(",") { """{ "kodo": "$it", "nomo": "$it" }""" }} ] }"""

    private fun deponejo(
        persisto: MemorPersisto = MemorPersisto(),
        settings: Settings? = MapSettings(),
        pakita: String = konfiguro("muzaiko"),
        elŝutTeksto: (suspend (String) -> String)? = null,
        alFreŝaAgordo: (KanalAgordo) -> Unit = {},
    ) = KanaloDeponejoImpl(
        leganto = KanalAgordoLeganto(),
        paktitaTeksto = { pakita },
        settings = settings,
        elŝutTeksto = elŝutTeksto,
        leguKonservitan = persisto.legu,
        skribuKonservitan = persisto.skribu,
        foriguKonservitan = persisto.forigu,
        alFreŝaAgordo = alFreŝaAgordo,
    )

    @Test
    fun senForaKaŝo_legasPaktitanKonfiguron() = runTest {
        val d = deponejo(pakita = konfiguro("muzaiko", "peranto"))
        assertEquals(listOf("muzaiko", "peranto"), d.getKanalojn().map { it.slug })
    }

    @Test
    fun foraKaŝo_venkasSuperPaktita() = runTest {
        val persisto = MemorPersisto()
        persisto.teksto = konfiguro("fornova")
        val d = deponejo(persisto = persisto, pakita = konfiguro("muzaiko"))
        assertEquals(listOf("fornova"), d.getKanalojn().map { it.slug })
    }

    @Test
    fun fuŝitaForaKaŝo_rezervoAlPaktitaKajForigo() = runTest {
        val persisto = MemorPersisto()
        persisto.teksto = "{ rompita json"
        val d = deponejo(persisto = persisto, pakita = konfiguro("muzaiko"))
        assertEquals(listOf("muzaiko"), d.getKanalojn().map { it.slug })
        assertEquals(1, persisto.forigitaj)
        assertNull(persisto.teksto)
    }

    @Test
    fun fortoRefreski_elŝutasKonservasKajVokasCallback_kajNovaDeponejoLegasElKaŝo() = runTest {
        val persisto = MemorPersisto()
        val settings = MapSettings()
        var freŝaAgordo: KanalAgordo? = null
        val d1 = deponejo(persisto = persisto, settings = settings, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { url ->
                assertEquals(FORA_KONFIGURO_URL, url)
                konfiguro("fornova")
            },
            alFreŝaAgordo = { freŝaAgordo = it })
        // "relanĉo": elŝuto kaj ĝisdatigo
        assertEquals(listOf("fornova"), d1.getKanalojn(fortoRefresigi = true).map { it.slug })
        // konservita en la kaŝo, tempo registrita, callback vokita
        assertEquals(konfiguro("fornova"), persisto.teksto)
        assertTrue(settings.getLong(SettingsKeys.FORA_KANALKONFIGURO_TEMPO, 0L) > 0L)
        assertEquals(listOf("fornova"), freŝaAgordo?.kanaloj?.map { it.kodo })
        // "nova relanĉo": nova deponejo super la sama kaŝo legas ĝin SEN reto
        val d2 = deponejo(persisto = persisto, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { error("ne rajtas peti la reton") })
        assertEquals(listOf("fornova"), d2.getKanalojn().map { it.slug })
    }

    @Test
    fun samaElŝutitaTeksto_nenioFariĝas() = runTest {
        val persisto = MemorPersisto()
        persisto.teksto = konfiguro("muzaiko")
        var callbackVokita = false
        val d = deponejo(persisto = persisto,
            elŝutTeksto = { konfiguro("muzaiko") },
            alFreŝaAgordo = { callbackVokita = true })
        assertEquals(listOf("muzaiko"), d.getKanalojn(fortoRefresigi = true).map { it.slug })
        assertFalse(callbackVokita)
    }

    @Test
    fun fiaskoDeElŝuto_konservasLaMalnovanKajNeKraŝas() = runTest {
        val persisto = MemorPersisto()
        val d = deponejo(persisto = persisto, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { error("reto mankas") })
        assertEquals(listOf("muzaiko"), d.getKanalojn(fortoRefresigi = true).map { it.slug })
        assertNull(persisto.teksto)
    }

    @Test
    fun kotlinErrorDeJsMotoro_kaptiĝasKielWasmJsCORS() = runTest {
        val persisto = MemorPersisto()
        val d = deponejo(persisto = persisto, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { throw Error("Fail to fetch") })  // kotlin.Error, ne Exception — kiel la Js-motoro
        assertEquals(listOf("muzaiko"), d.getKanalojn(fortoRefresigi = true).map { it.slug })
        assertNull(persisto.teksto)
    }

    @Test
    fun neParseblaElŝutitaTeksto_neKonserviĝas() = runTest {
        val persisto = MemorPersisto()
        val d = deponejo(persisto = persisto, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { "{ ankoraŭ rompita" })
        assertEquals(listOf("muzaiko"), d.getKanalojn(fortoRefresigi = true).map { it.slug })
        assertNull(persisto.teksto)
    }

    @Test
    fun malplenaKanallisto_neKonserviĝas() = runTest {
        val persisto = MemorPersisto()
        val d = deponejo(persisto = persisto, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { konfiguro() })  // neniu kanalo
        assertEquals(listOf("muzaiko"), d.getKanalojn(fortoRefresigi = true).map { it.slug })
        assertNull(persisto.teksto)
    }

    @Test
    fun malplenaElŝutitaTeksto_neKonserviĝas() = runTest {
        val persisto = MemorPersisto()
        val d = deponejo(persisto = persisto, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { "   " })
        assertEquals(listOf("muzaiko"), d.getKanalojn(fortoRefresigi = true).map { it.slug })
        assertNull(persisto.teksto)
    }

    @Test
    fun persistFiasko_neBlokasLaVivanĜisdatigon() = runTest {
        val persisto = MemorPersisto()
        persisto.skribEraro = Exception("plena disko")
        var callbackVokita = false
        val d = deponejo(persisto = persisto, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { konfiguro("fornova") },
            alFreŝaAgordo = { callbackVokita = true })
        // la viva ĝisdatigo okazas eĉ se la persistigo fiaskis
        assertEquals(listOf("fornova"), d.getKanalojn(fortoRefresigi = true).map { it.slug })
        assertTrue(callbackVokita)
        assertNull(persisto.teksto)
    }

    @Test
    fun senFonaScope_neElŝutasEnFono() = runTest {
        val persisto = MemorPersisto()
        val d = deponejo(persisto = persisto, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { error("ne rajtas okazi — neniu fona amplekso") })
        // sen fonaScope kaj sen fortoRefresigi: neniu reta voko, nur loka ŝargo
        assertEquals(listOf("muzaiko"), d.getKanalojn().map { it.slug })
    }

    @Test
    fun getAgordon_donasLaEfektivanAgordon() = runTest {
        val persisto = MemorPersisto()
        persisto.teksto = """{ "arkivoBazoUrl": "https://ekzemplo.test/", "kanaloj": [ { "kodo": "x", "nomo": "X" } ] }"""
        val d = deponejo(persisto = persisto, pakita = konfiguro("muzaiko"))
        assertEquals("https://ekzemplo.test/", d.getAgordon().arkivoBazoUrl)
        assertEquals(listOf("x"), d.getKanalojn().map { it.slug })
        assertEquals("x", d.observiKanalojn().value.single().slug)
        assertEquals("https://ekzemplo.test/", d.observiAgordon().value?.arkivoBazoUrl)
    }
}
