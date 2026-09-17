package com.wakeiq

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Swipe-to-delete (HomeScreen.kt SwipeToDeleteAlarmCard) has no built-in accessible equivalent:
// TalkBack's explore-by-touch gestures conflict with a raw swipe gesture, so a screen-reader or
// switch-access user cannot perform it. This test asserts the accessible fallback - a custom
// accessibility action exposed on the alarm card's semantics node - is present. TalkBack surfaces
// custom accessibility actions as a menu when the user long-presses a node's actions control, so
// this is what makes delete reachable without a swipe.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AlarmListAccessibilityTest {

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
    fun alarmCardExposesDeleteAsCustomAccessibilityAction() {
        // Create one alarm so the list has a card to inspect.
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.new_alarm))
            .performClick()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.edit_alarm_title_new),
        ).assertIsDisplayed()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.save))
            .performClick()

        val deleteLabel = composeRule.activity.getString(R.string.delete_alarm)

        val cardHasDeleteAction = composeRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .any { node ->
                node.config.getOrNull(SemanticsProperties.CustomActions)
                    ?.any { it.label == deleteLabel } == true
            }

        assertTrue(
            "Expected an alarm card with a '$deleteLabel' custom accessibility action",
            cardHasDeleteAction,
        )
    }
}
