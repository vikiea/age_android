package com.age.android.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassDefaultsTest {
    @Test
    fun glassContainerAlphaIsTranslucentWhenGlassEffectIsEnabled() {
        assertTrue(
            GlassDefaults.glassContainerAlpha(
                glassEffectEnabled = true,
                dark = false,
                emphasis = GlassEmphasis.Normal
            ) <= 0.70f
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
