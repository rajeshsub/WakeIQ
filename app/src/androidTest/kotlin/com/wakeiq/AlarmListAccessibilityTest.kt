package com.wakeiq

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun alarmCardExposesDeleteAsCustomAccessibilityAction() {
        // HomeViewModel.seedDefaultAlarmIfNeeded() seeds two alarms ("Weekdays", "Weekends") on
        // a fresh install/first launch - the exact state of a CI emulator - before this test
        // creates its own, and AlarmDao orders by (hour, minute), not insertion order, with no
        // tiebreaker - EditAlarmViewModel's new-alarm default (7:00) ties with the seeded
        // "Weekends" alarm, so this test's card cannot be reliably picked out by position or
        // count alone. Giving it a distinctive label removes the ambiguity entirely: the card
        // is identified by that label's text, not by where it happens to sort.
        val uniqueLabel = "AlarmListAccessibilityTest ${System.nanoTime()}"
        val isThisTestsCard = hasAnyDescendant(hasText(uniqueLabel))

        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.new_alarm))
            .performClick()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.edit_alarm_title_new),
        ).assertIsDisplayed()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.label_hint))
            .performScrollTo()
            .performTextInput(uniqueLabel)
        // EditAlarmScreen is a scrollable column; the Save button is not guaranteed to be in
        // the initial viewport, and clicking a node whose layout coordinates are outside the
        // visible/laid-out bounds is a known way for a Compose test click to silently not
        // register on the real target. Scroll to it explicitly before clicking rather than
        // assuming performClick() alone is enough regardless of position.
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.save))
            .performScrollTo()
            .performClick()

        // EditAlarmViewModel.save() runs a full chain before navigating back to Home: two
        // DataStore reads, the Room insert, and a real AlarmScheduler.schedule() call
        // (AlarmManager) - only then does savedOrDeleted flip and EditAlarmScreen's
        // LaunchedEffect pop the screen. Checkpointed separately from the card assertion below
        // so a failure here (still on the edit screen) is distinguishable from the card itself
        // never appearing on an already-navigated Home screen.
        // Timeout generous by design, not just for the save chain itself: logcat from a real
        // CI run (GitHub-hosted emulator, software rendering, no GPU) showed a 13s window with
        // zero process output mid-test - the emulator host, not this app, stalling completely -
        // on top of single frames taking over 2s to render. 15s barely covered the chain and
        // nothing extra; 30s leaves headroom for that class of host-level freeze too.
        val homeTitle = composeRule.activity.getString(R.string.home_title)
        composeRule.waitUntil("navigation back to the Home screen after save", 30_000) {
            composeRule.onAllNodes(hasText(homeTitle)).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.waitUntil("the newly saved alarm's card appears", 15_000) {
            composeRule.onAllNodes(isThisTestsCard).fetchSemanticsNodes().size == 1
        }

        val deleteLabel = composeRule.activity.getString(R.string.delete_alarm)

        // Invoking the label directly (rather than just checking it is present in the node's
        // semantics config) proves both that the action is exposed - TalkBack surfaces custom
        // accessibility actions by label - and that it actually deletes the alarm.
        composeRule
            .onAllNodes(isThisTestsCard)[0]
            .performCustomAccessibilityActionWithLabel(deleteLabel)

        // Deletion is gated behind the undo snackbar's SnackbarDuration.Short window
        // (HomeScreen.kt's deleteWithUndo suspends on showSnackbar before calling
        // viewModel.delete), so the card does not disappear immediately after triggering
        // the action - it disappears once that window elapses. 30s for the same
        // host-level-freeze headroom as the wait above, not just the snackbar duration.
        composeRule.waitUntil("the deleted alarm's card disappears", 30_000) {
            composeRule.onAllNodes(isThisTestsCard).fetchSemanticsNodes().isEmpty()
        }
    }
}
