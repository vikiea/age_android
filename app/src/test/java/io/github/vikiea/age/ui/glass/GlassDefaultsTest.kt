package io.github.vikiea.age.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassDefaultsTest {
    @Test
    fun glassContainerAlphaIsFrostedAndMostlyOpaqueWhenGlassEffectIsEnabled() {
        assertTrue(
            GlassDefaults.glassContainerAlpha(
                glassEffectEnabled = true,
                dark = false,
                emphasis = GlassEmphasis.Normal
            ) in 0.84f..0.95f
        )
        assertTrue(
            GlassDefaults.glassContainerAlpha(
                glassEffectEnabled = true,
                dark = true,
                emphasis = GlassEmphasis.Strong
            ) < 1f
        )
    }

    @Test
    fun glassContainerAlphaIsOpaqueWhenGlassEffectIsDisabled() {
        GlassEmphasis.entries.forEach { emphasis ->
            assertEquals(
                1f,
                GlassDefaults.glassContainerAlpha(
                    glassEffectEnabled = false,
                    dark = false,
                    emphasis = emphasis
                )
            )
            assertEquals(
                1f,
                GlassDefaults.glassContainerAlpha(
                    glassEffectEnabled = false,
                    dark = true,
                    emphasis = emphasis
                )
            )
        }
    }
}
