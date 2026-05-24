package com.age.android.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun `stored names map to theme modes`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredName("SYSTEM"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStoredName("DARK"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStoredName("LIGHT"))
    }

    @Test
    fun `unknown stored names fall back to system`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredName(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredName(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredName("legacy"))
    }

    @Test
    fun `stored names map to theme accents`() {
        assertEquals(ThemeAccent.LIQUID_DEFAULT, ThemeAccent.fromStoredName("LIQUID_DEFAULT"))
        assertEquals(ThemeAccent.LIQUID_AURORA, ThemeAccent.fromStoredName("LIQUID_AURORA"))
        assertEquals(ThemeAccent.SOLID_RED, ThemeAccent.fromStoredName("SOLID_RED"))
    }

    @Test
    fun `unknown stored accent names fall back to liquid default`() {
        assertEquals(ThemeAccent.LIQUID_DEFAULT, ThemeAccent.fromStoredName(null))
        assertEquals(ThemeAccent.LIQUID_DEFAULT, ThemeAccent.fromStoredName(""))
        assertEquals(ThemeAccent.LIQUID_DEFAULT, ThemeAccent.fromStoredName("legacy"))
    }
}
