package io.github.vikiea.age.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vikiea.age.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun topLevelNavigationOpensKeys() {
        composeRule.onNodeWithText("Encrypt").assertExists()
        composeRule.onNodeWithText("Keys").performClick()
        composeRule.onNodeWithText("Key management").assertExists()
    }
}
