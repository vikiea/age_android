package io.github.vikiea.age.ui.glass

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.vikiea.age.ui.theme.AgeAndroidTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppTopBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun iconOnlyActionExposesExpectedSemantics() {
        var clicked = false
        composeRule.setContent {
            AgeAndroidTheme {
                AppTopBar(
                    actions = {
                        TopBarActionButton(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            onClick = { clicked = true },
                            modifier = Modifier.testTag("settingsAction")
                        )
                    }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Settings").assertExists()
        composeRule.onNodeWithTag("settingsAction")
            .assertHasClickAction()
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .performClick()
        assertEquals(true, clicked)
    }

}
