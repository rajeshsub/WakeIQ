package com.wakeiq

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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

    // HomeScreen.kt tags each alarm card "alarm_card_<id>" (AlarmCard's Modifier.testTag). Every
    // card carries this prefix regardless of which id the DB assigns, so matching the prefix
    // finds "the" card without assuming a specific id or relying on node counts/ordering, which
    // are not stable across the rest of the clickable tree (icons, toggles, etc. on this screen).
    private val isAlarmCard = SemanticsMatcher("has test tag starting with alarm_card_") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("alarm_card_") == true
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun alarmCardExposesDeleteAsCustomAccessibilityAction() {
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.new_alarm))
            .performClick()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.edit_alarm_title_new),
        ).assertIsDisplayed()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.save))
            .performClick()

        composeRule.onAllNodes(isAlarmCard).assertCountEquals(1)

        val deleteLabel = composeRule.activity.getString(R.string.delete_alarm)

        // Invoking the label directly (rather than just checking it is present in the node's
        // semantics config) proves both that the action is exposed - TalkBack surfaces custom
        // accessibility actions by label - and that it actually deletes the alarm.
        composeRule
            .onAllNodes(isAlarmCard)[0]
            .performCustomAccessibilityActionWithLabel(deleteLabel)

        composeRule.onAllNodes(isAlarmCard).assertCountEquals(0)
    }
}
