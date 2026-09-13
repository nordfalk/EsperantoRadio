package dk.nordfalk.esperanto.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DosierNomojTest {

    @Test
    fun sanigasKolonkajSuprenStreko() {
        val id = "polaretradio:2026-09-10:https://pola-retradio.org/?p=44538"
        val rezulto = sanigiDosiernomon(id)
        assertNotEquals(id, rezulto, "Devus sanigi la ID-on")
        assertEquals(false, rezulto.contains(":"), "Ne devas enhavi ':'")
        assertEquals(false, rezulto.contains("/"), "Ne devas enhavi '/'")
        assertEquals(false, rezulto.contains("?"), "Ne devas enhavi '?'")
    }

    @Test
    fun sanigasChiujnNevalidajnSignojn() {
        val id = "a:b/c\\d?e*f\"g<h>i|j"
        val rezulto = sanigiDosiernomon(id)
        assertEquals("a-b-c-d-e-f-g-h-i-j", rezulto)
    }

    @Test
    fun lasasValidajnSignojnNetushitajn() {
        val id = "k1_2024-03-01_epizodo-5"
        assertEquals(id, sanigiDosiernomon(id), "Validaj signoj devas resti netuŝitaj")
    }

    @Test
    fun malplenaIdRestasMalplena() {
        assertEquals("", sanigiDosiernomon(""))
    }
}
