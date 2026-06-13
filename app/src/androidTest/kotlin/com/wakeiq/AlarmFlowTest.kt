package com.wakeiq

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AlarmFlowTest {

    @get:Rule(order = 0)
    val permissionsRule = GrantSystemPermissionsRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun homeScreenDisplaysCorrectly() {
        composeRule.onNodeWithText("Alarms").assertIsDisplayed()
    }

    @Test
    fun fabOpensEditScreen() {
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.add_alarm))
            .performClick()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.edit_alarm_title_new),
        ).assertIsDisplayed()
    }
}
