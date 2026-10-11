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

    @Test // FAROTA C3: RSS-id-oj povas esti plenaj URL-oj — >255 bajtoj malsukcesigas elŝuton
    fun limigasLongonKajKonservasUnikecon() {
        val longa = "x".repeat(500)
        val rezulto = sanigiDosiernomon(longa)
        assertEquals(true, rezulto.length <= 120, "Nomo devas esti maks 120 signoj: ${rezulto.length}")

        // Malsamaj longaj ID-oj devas resti DISTINGEBLAJ (malsamaj haketaĵoj)
        val alia = "y".repeat(500)
        assertNotEquals(sanigiDosiernomon(longa), sanigiDosiernomon(alia), "Haketaĵoj devas esti malsamaj")
    }

    @Test // FAROTA C3: Windows-rezervitaj nomoj kaj finaj punktoj
    fun sanigasWindowsRezervitajnNomojn() {
        assertEquals("_CON", sanigiDosiernomon("CON"))
        assertEquals("_prn", sanigiDosiernomon("prn"))
        assertEquals("abc", sanigiDosiernomon("abc."), "Fina punkto devas esti forigita (Windows)")
        assertEquals("abc", sanigiDosiernomon("abc "), "Fina spaco devas esti forigita")
    }

    @Test // FAROTA C3: nul-bajtoj
    fun forigasNulbajtojn() {
        assertEquals("ab", sanigiDosiernomon("a\u0000b"))
    }
}
