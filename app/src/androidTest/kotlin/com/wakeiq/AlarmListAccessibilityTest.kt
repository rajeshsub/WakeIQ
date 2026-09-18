package com.wakeiq

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
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
    // card carries this prefix regardless of which id the DB assigns.
    private val isAlarmCard = SemanticsMatcher("has test tag starting with alarm_card_") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("alarm_card_") == true
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun alarmCardExposesDeleteAsCustomAccessibilityAction() {
        // HomeViewModel.seedDefaultAlarmIfNeeded() seeds two alarms ("Weekdays", "Weekends") on
        // a fresh install/first launch - the exact state of a CI emulator - before this test
        // creates its own. Disambiguating the new alarm's card by a typed label was tried and
        // reverted: performTextInput/performTextReplacement always focus the field and trigger
        // a real IME show/hide cycle (confirmed via javap - no public API bypasses this), and
        // that IME animation genuinely hangs on this headless, -no-window CI emulator (logcat:
        // "FrameTracker: force finish cuj, time out: J<IME_INSETS_ANIMATION>", twice, followed
        // by 20+s of dead silence). Deleting the seeded alarms first - using the very
        // accessibility-delete action this test exists to verify - sidesteps both the ordering
        // collision (AlarmDao orders by (hour, minute) with no tiebreaker; a new alarm's default
        // 7:00 ties with the seeded "Weekends" alarm) and the IME issue entirely: no text input
        // anywhere in this test.
        // seedDefaultAlarmIfNeeded() runs in HomeViewModel.init on a background coroutine, so
        // the card count at the very start of this test can be 0 (seeding still in flight) even
        // though 2 cards are about to appear. Wait for the count to stop changing (two
        // consecutive reads, 500ms apart, agreeing) before trusting it, rather than racing the
        // seed with a single read that could be stale.
        val deleteLabel = composeRule.activity.getString(R.string.delete_alarm)
        composeRule.waitUntil("the alarm list settles after any seeding", 30_000) {
            val first = composeRule.onAllNodes(isAlarmCard).fetchSemanticsNodes().size
            Thread.sleep(500)
            val second = composeRule.onAllNodes(isAlarmCard).fetchSemanticsNodes().size
            first == second
        }
        var remainingCards = composeRule.onAllNodes(isAlarmCard).fetchSemanticsNodes().size
        while (remainingCards > 0) {
            composeRule.onAllNodes(isAlarmCard)[0].performCustomAccessibilityActionWithLabel(deleteLabel)
            val countBeforeThisDelete = remainingCards
            composeRule.waitUntil("a seeded alarm card is removed", 30_000) {
                composeRule.onAllNodes(isAlarmCard).fetchSemanticsNodes().size < countBeforeThisDelete
            }
            remainingCards = composeRule.onAllNodes(isAlarmCard).fetchSemanticsNodes().size
        }

        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.new_alarm))
            .performClick()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.edit_alarm_title_new),
        ).assertIsDisplayed()
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
        // Timeout generous by design, not just for the save chain itself: logcat from real CI
        // runs (GitHub-hosted emulator, software rendering, no GPU) repeatedly showed multi-
        // second-to-tens-of-seconds windows of zero process output mid-test.
        val homeTitle = composeRule.activity.getString(R.string.home_title)
        composeRule.waitUntil("navigation back to the Home screen after save", 30_000) {
            composeRule.onAllNodes(hasText(homeTitle)).fetchSemanticsNodes().isNotEmpty()
        }

        // No other alarm exists at this point (seeded ones were deleted above), so exactly one
        // card - this test's own - is unambiguous without needing a label or position guess.
        composeRule.waitUntil("the newly saved alarm's card appears", 30_000) {
            composeRule.onAllNodes(isAlarmCard).fetchSemanticsNodes().size == 1
        }

        // Invoking the label directly (rather than just checking it is present in the node's
        // semantics config) proves both that the action is exposed - TalkBack surfaces custom
        // accessibility actions by label - and that it actually deletes the alarm.
        composeRule
            .onAllNodes(isAlarmCard)[0]
            .performCustomAccessibilityActionWithLabel(deleteLabel)

        // Deletion is gated behind the undo snackbar's SnackbarDuration.Short window
        // (HomeScreen.kt's deleteWithUndo suspends on showSnackbar before calling
        // viewModel.delete), so the card does not disappear immediately after triggering
        // the action - it disappears once that window elapses.
        composeRule.waitUntil("the deleted alarm's card disappears", 30_000) {
            composeRule.onAllNodes(isAlarmCard).fetchSemanticsNodes().isEmpty()
        }
    }
}
