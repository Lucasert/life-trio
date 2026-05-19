package com.rememberforever

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RememberForeverSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsMainModules() {
        composeRule.onNodeWithText("Remember Forever").assertIsDisplayed()
        composeRule.onNodeWithText("备忘").assertIsDisplayed()
        composeRule.onNodeWithText("记账").assertIsDisplayed()
        composeRule.onNodeWithText("计划").assertIsDisplayed()
    }
}
