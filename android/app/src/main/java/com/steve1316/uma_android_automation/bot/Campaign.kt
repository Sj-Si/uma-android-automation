package com.steve1316.uma_android_automation.bot

import android.util.Log
import android.graphics.Bitmap
import org.opencv.core.Point
import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import com.steve1316.automation_library.utils.BotService
import com.steve1316.automation_library.utils.DiscordUtils
import com.steve1316.automation_library.utils.ImageUtils.ScaleConfidenceResult
import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.SettingsHelper

import com.steve1316.uma_android_automation.bot.DialogHandlerResult
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.Racing
import com.steve1316.uma_android_automation.bot.SkillPlan
import com.steve1316.uma_android_automation.bot.Task
import com.steve1316.uma_android_automation.bot.TaskResult
import com.steve1316.uma_android_automation.bot.TaskResultCode
import com.steve1316.uma_android_automation.bot.Training
import com.steve1316.uma_android_automation.bot.TrainingEvent

import com.steve1316.uma_android_automation.types.Aptitude
import com.steve1316.uma_android_automation.types.BoundingBox
import com.steve1316.uma_android_automation.types.DateMonth
import com.steve1316.uma_android_automation.types.DatePhase
import com.steve1316.uma_android_automation.types.DateYear
import com.steve1316.uma_android_automation.types.FanCountClass
import com.steve1316.uma_android_automation.types.GameDate
import com.steve1316.uma_android_automation.types.Mood
import com.steve1316.uma_android_automation.types.RunningStyle
import com.steve1316.uma_android_automation.types.Trainee

import com.steve1316.uma_android_automation.components.*

/** Exception for breaking from the main loop when conditions are met.
 *
 * @param message A helpful message describing what breakpoint we hit.
 */
class CampaignBreakpointException(message: String) : Exception(message)

/**
 * Base campaign class that contains all shared logic for campaign automation.
 * Campaign-specific logic should be implemented in subclasses by overriding the appropriate methods.
 */
abstract class Campaign(game: Game) : Task(game) {
    // Required class instances used throughout campaign.
    protected val racing: Racing = Racing(game, this)
    protected val skillPlan: SkillPlan = SkillPlan(game, this)
    val trainee: Trainee = Trainee()
    val training: Training = Training(game, this)
    protected val trainingEvent: TrainingEvent = TrainingEvent(game, this)
    var date: GameDate = GameDate(day = 1)

    // Required settings.
    protected val enableCraneGameAttempt: Boolean = SettingsHelper.getBooleanSetting("general", "enableCraneGameAttempt")
    val enableFarmingFans: Boolean = SettingsHelper.getBooleanSetting("racing", "enableFarmingFans")
    protected val enableSkillPointCheck: Boolean = SettingsHelper.getBooleanSetting("skills", "enableSkillPointCheck")
    protected val enableStopAtDate: Boolean = SettingsHelper.getBooleanSetting("general", "enableStopAtDate")
    protected val enableStopBeforeFinals: Boolean = SettingsHelper.getBooleanSetting("general", "enableStopBeforeFinals")
    protected val mustRestBeforeSummer: Boolean = SettingsHelper.getBooleanSetting("training", "mustRestBeforeSummer")
    protected val skillPointsRequired: Int = SettingsHelper.getIntSetting("skills", "skillPointCheck")
    protected val stopAtDate: String = SettingsHelper.getStringSetting("general", "stopAtDate")

    // Misc flags and variables for control logic.
    var isFinals: Boolean = false
    protected var recreationDateCompleted: Boolean = false
    protected var scenarioCheckPerformed: Boolean = false
    protected var stopAtDateInitialTurnNumber: Int = -1
    protected var stopBeforeFinalsInitialTurnNumber: Int = -1
    // Should always check fan count at bot start unless in pre-debut.
    protected var bNeedToCheckFans: Boolean = true
    // Flag used to prevent us from attempting to check fans multiple times in a day.
    // This helps us avoid infinite loops.
    protected var bHasTriedCheckingFansToday: Boolean = false
    // Flag for whether we have handled the skill point check conditions
    // during this run.
    // This is necessary since the user may have enabled the skill point check
    // skill spending plan. If their plan ends up not purchasing many skills,
    // then it is possible that we could get stuck in a loop of hitting the
    // skill point threshold and attempting to buy skills every single turn.
    // To resolve this, we only allow the skill point check to be handled
    // once per run.
    protected var bHasHandledSkillPointCheck: Boolean = false
    // Flag for only checking for maiden races once per day.
    var bHasCheckedForMaidenRaceToday: Boolean = false
    // Flag to skip redundant date checks when no game-advancing action was taken.
    // Reset to false when training, resting, racing, or other game-advancing actions complete.
    protected var bHasCheckedDateThisTurn: Boolean = false

    // =============================================================================
    // CAMPAIGN-SPECIFIC FUNCTIONS
    // =============================================================================

    /**
     * Campaign-specific checks for special screens or conditions.
     * 
     * @return True if the conditions are met, false otherwise.
     */
    abstract fun checkCampaignSpecificConditions(): Boolean

    /**
     * Campaign-specific training event handling.
     */
    open fun handleTrainingEvent() {
        trainingEvent.handleTrainingEvent()
    }

    /**
     * Campaign-specific race event handling.
     * 
     * @param isScheduledRace True if the race is scheduled, false otherwise.
     * @return True if the race was handled successfully, false otherwise.
     */
    open fun handleRaceEvents(isScheduledRace: Boolean = false): Boolean {
        val bDidRace: Boolean = racing.handleRaceEvents(isScheduledRace)
        bNeedToCheckFans = bDidRace

        return bDidRace
    }

    // =============================================================================
    // TESTS
    // =============================================================================

    open override fun startTests(): Boolean {
        val fnMap: Map<String, () -> Unit> = mapOf(
            "debugMode_startTemplateMatchingTest" to ::startTemplateMatchingTest,
            "debugMode_startSingleTrainingOCRTest" to training::startSingleTrainingOCRTest,
            "debugMode_startComprehensiveTrainingOCRTest" to training::startComprehensiveTrainingOCRTest,
            "debugMode_startDateOCRTest" to ::startDateOCRTest,
            "debugMode_startRaceListDetectionTest" to racing::startRaceListDetectionTest,
            "debugMode_startAptitudesDetectionTest" to ::startAptitudesDetectionTest,
            "debugMode_startTraineeNameOCRTest" to ::startTraineeNameOCRTest,
            "debugMode_startMainScreenOCRTest" to ::startMainScreenOCRTest,
            "debugMode_startTrainingScreenOCRTest" to ::startTrainingScreenOCRTest,
        )

        var bDidAnyTestsRun: Boolean = false
        for ((settingName, fn) in fnMap) {
            if (SettingsHelper.getBooleanSetting("debug", settingName)) {
                fn()
                bDidAnyTestsRun = true
            }
        }

        return bDidAnyTestsRun
    }

    /**
     * Handles the test to perform template matching to determine what the best scale will be for the device.
     */
    open fun startTemplateMatchingTest() {
        MessageLog.i(TAG, "\n[TEST] Now beginning basic template match test on the Home screen.")
        MessageLog.i(TAG, "[TEST] Template match confidence setting will be overridden for the test.\n")
        var results = mutableMapOf<String, MutableList<ScaleConfidenceResult>>(
            LabelEnergy.template.path to mutableListOf(),
            IconTazuna.template.path to mutableListOf(),
            LabelStatTableHeaderSkillPoints.template.path to mutableListOf()
        )
        results = game.imageUtils.startTemplateMatchingTest(results)
        MessageLog.i(TAG, "\n[TEST] Basic template match test complete.")

        // Print all scale/confidence combinations that worked for each template.
        for ((templateName, scaleConfidenceResults) in results) {
            if (scaleConfidenceResults.isNotEmpty()) {
                MessageLog.i(TAG, "[TEST] All working scale/confidence combinations for $templateName:")
                for (result in scaleConfidenceResults) {
                    MessageLog.i(TAG, "[TEST]\tScale: ${result.scale}, Confidence: ${result.confidence}")
                }
            } else {
                MessageLog.w(TAG, "No working scale/confidence combinations found for $templateName")
            }
        }

        // Then print the median scales and confidences.
        val medianScales = mutableListOf<Double>()
        val medianConfidences = mutableListOf<Double>()
        for ((templateName, scaleConfidenceResults) in results) {
            if (scaleConfidenceResults.isNotEmpty()) {
                val sortedScales = scaleConfidenceResults.map { it.scale }.sorted()
                val sortedConfidences = scaleConfidenceResults.map { it.confidence }.sorted()
                val medianScale = sortedScales[sortedScales.size / 2]
                val medianConfidence = sortedConfidences[sortedConfidences.size / 2]
                medianScales.add(medianScale)
                medianConfidences.add(medianConfidence)
                MessageLog.i(TAG, "[TEST] Median scale for $templateName: $medianScale")
                MessageLog.i(TAG, "[TEST] Median confidence for $templateName: $medianConfidence")
            }
        }

        if (medianScales.isNotEmpty()) {
            MessageLog.i(TAG, "\n[TEST] The following are the recommended scales to set: $medianScales.")
            MessageLog.i(TAG, "[TEST] The following are the recommended confidences to set: $medianConfidences.")
        } else {
            MessageLog.e(TAG, "\nNo median scale/confidence can be found.")
        }
    }

    /**
     * Handles the test to perform OCR on the current date and elapsed turn number.
     */
    open fun startDateOCRTest() {
        MessageLog.i(TAG, "\n[TEST] Now beginning the Date OCR test on the Main screen.")
        MessageLog.i(TAG, "[TEST] Note that this test is dependent on having the correct scale.")
        game.wait(5.0)
        updateDate()
    }

    /**
     * Test function to verify aptitude detection on the Main screen.
     *
     * Opens the aptitudes dialog and processes it to test OCR accuracy.
     * Note: This test is dependent on having the correct scale.
     */
    open fun startAptitudesDetectionTest() {
        MessageLog.i(TAG, "\n[TEST] Now beginning the Aptitudes Detection test on the Main screen.")
        MessageLog.i(TAG, "[TEST] Note that this test is dependent on having the correct scale.")
        openAptitudesDialog()
        handleDialogs()
    }

    /**
     * Test function to verify trainee name OCR on the Aptitude dialog.
     *
     * Opens the aptitudes dialog and processes it to test name OCR accuracy.
     */
    open fun startTraineeNameOCRTest() {
        MessageLog.i(TAG, "\n[TEST] Now beginning the Trainee Name OCR test on the Main screen.")
        MessageLog.i(TAG, "[TEST] Note that this test is dependent on having the correct scale.")
        openAptitudesDialog()
        handleDialogs()
    }

    /**
     * Test function to verify OCR detection on the Training screen.
     */
    open fun startTrainingScreenOCRTest() {
        MessageLog.i(TAG, "---- startTrainingScreenOCRTest START ----")

        var numPass = 0
        var numFail = 0

        // Simple components to test.
        val componentsToTest: List<ComponentInterface> = listOf(
            LabelEnergy,
            LabelStatTableHeaderSkillPoints,
            LabelTrainingFailureChance,
            ButtonTrainingSpeed,
            ButtonTrainingStamina,
            ButtonTrainingPower,
            ButtonTrainingGuts,
            ButtonTrainingWit,
            ButtonBack,
            ButtonLog,
            ButtonBurger,
        )
        for (componentToTest in componentsToTest) {
            if (componentToTest.check(game.imageUtils)) {
                MessageLog.i(TAG, "[PASS] ${componentToTest.template.path}")
                numPass++
            } else {
                MessageLog.e(TAG, "[FAIL] ${componentToTest.template.path}")
                numFail++
            }
        }

        when {
            IconTrainingHeaderSpeed.check(game.imageUtils) -> {
                MessageLog.i(TAG, "[PASS] ${IconTrainingHeaderSpeed.template.path}")
                numPass++
            }
            IconTrainingHeaderStamina.check(game.imageUtils) -> {
                MessageLog.i(TAG, "[PASS] ${IconTrainingHeaderStamina.template.path}")
                numPass++
            }
            IconTrainingHeaderPower.check(game.imageUtils) -> {
                MessageLog.i(TAG, "[PASS] ${IconTrainingHeaderPower.template.path}")
                numPass++
            }
            IconTrainingHeaderGuts.check(game.imageUtils) -> {
                MessageLog.i(TAG, "[PASS] ${IconTrainingHeaderGuts.template.path}")
                numPass++
            }
            IconTrainingHeaderWit.check(game.imageUtils) -> {
                MessageLog.i(TAG, "[PASS] ${IconTrainingHeaderWit.template.path}")
                numPass++
            }
            else -> {
                MessageLog.e(TAG, "[FAIL] Could not detect any training header icons.")
                numFail++
            }
        }

        MessageLog.i(TAG, "---- startTrainingScreenOCRTest END: PASS=$numPass, FAIL=$numFail ----")
    }

    /**
     * Test function to verify OCR detection on the Main screen.
     */
    open fun startMainScreenOCRTest() {
        MessageLog.i(TAG, "---- startMainScreenOCRTest START ----")

        var numPass = 0
        var numFail = 0

        // Simple components to test.
        val componentsToTest: List<ComponentInterface> = listOf(
            ButtonHomeFansInfo,
            IconTazuna,
            LabelEnergy,
            LabelStatTableHeaderSkillPoints,
            ButtonHomeFullStats,
            ButtonRest,
            ButtonTraining,
            ButtonSkills,
            ButtonInfirmary,
            ButtonRecreation,
            ButtonLog,
            ButtonBurger,
        )
        for (componentToTest in componentsToTest) {
            if (componentToTest.check(game.imageUtils)) {
                MessageLog.i(TAG, "[PASS] ${componentToTest.template.path}")
                numPass++
            } else {
                MessageLog.e(TAG, "[FAIL] ${componentToTest.template.path}")
                numFail++
            }
        }

        // More complex components to test that have multiple different states.

        when (ButtonRaces.checkDisabled(game.imageUtils)) {
            true -> {
                MessageLog.i(TAG, "[PASS] ${ButtonRaces.template.path} (locked)")
                numPass++
            }
            false -> {
                MessageLog.i(TAG, "[PASS] ${ButtonRaces.template.path}")
                numPass++
            }
            null -> {
                MessageLog.e(TAG, "[FAIL] ${ButtonRaces.template.path}")
                numFail++
            }
        }

        MessageLog.i(TAG, "Testing mood components...")
        when {
            IconMoodGreat.check(game.imageUtils) -> {
                MessageLog.i(TAG, "[PASS] ${IconMoodGreat.template.path}")
                numPass++
            }
            IconMoodGood.check(game.imageUtils) -> {
                MessageLog.i(TAG, "[PASS] ${IconMoodGood.template.path}")
                numPass++
            }
            IconMoodNormal.check(game.imageUtils) -> {
                MessageLog.i(TAG, "[PASS] ${IconMoodNormal.template.path}")
                numPass++
            }
            IconMoodBad.check(game.imageUtils) -> {
                MessageLog.i(TAG, "[PASS] ${IconMoodBad.template.path}")
                numPass++
            }
            IconMoodAwful.check(game.imageUtils) -> {
                MessageLog.i(TAG, "[PASS] ${IconMoodAwful.template.path}")
                numPass++
            }
            else -> {
                MessageLog.e(TAG, "[FAIL] Could not detect any mood icons.")
                numFail++
            }
        }

        MessageLog.i(TAG, "---- startMainScreenOCRTest END: PASS=$numPass, FAIL=$numFail ----")
    }

    // =============================================================================
    // SCREEN CHECK FUNCTIONS
    // =============================================================================

    /**
     * Checks if the bot is at the Main screen or the screen with available options to undertake.
     * This will also make sure that the Main screen does not contain the option to select a race.
     *
     * @return True if the bot is at the Main screen. Otherwise false.
     */
    open fun checkMainScreen(): Boolean {
        return ButtonHomeFullStats.check(game.imageUtils) && IconTazuna.check(game.imageUtils)
    }

    /**
     * Checks if the bot is at the Training Event screen with an active event with options to select on screen.
     *
     * @return True if the bot is at the Training Event screen. Otherwise false.
     */
    open fun checkTrainingEventScreen(): Boolean {
        MessageLog.i(TAG, "\nChecking if the bot is sitting on the Training Event screen.")
        return if (IconTrainingEventHorseshoe.check(game.imageUtils)) {
            MessageLog.i(TAG, "Bot is at the Training Event screen.")
            true
        } else {
            MessageLog.i(TAG, "Bot is not at the Training Event screen.")
            false
        }
    }

    /**
     * Checks if the bot is at the preparation screen with a mandatory race needing to be completed.
     *
     * @return True if the bot is at the Main screen with a mandatory race. Otherwise false.
     */
    open fun checkMandatoryRacePrepScreen(): Boolean {
        MessageLog.i(TAG, "\nChecking if the bot is sitting on the Race Preparation screen for a mandatory race.")
        val sourceBitmap = game.imageUtils.getSourceBitmap()
        return if (IconRaceDayRibbon.check(game.imageUtils, sourceBitmap = sourceBitmap)) {
            MessageLog.i(TAG, "Bot is at the preparation screen with a mandatory race ready to be completed.")
            if (game.scenario == "Unity Cup") game.wait(1.0)
            true
        } else if (IconGoalRibbon.check(game.imageUtils, sourceBitmap = sourceBitmap)) {
            // Most likely the user started the bot here so a delay will need to be placed to allow the start banner of the Service to disappear.
            game.wait(2.0)
            MessageLog.i(TAG, "Bot is at the Race Selection screen with a mandatory race needing to be selected.")
            // Walk back to the preparation screen.
            ButtonBack.click(game.imageUtils, sourceBitmap = sourceBitmap)
            game.wait(1.0)
            true
        } else if (game.scenario == "Unity Cup" && ButtonUnityCupRace.check(game.imageUtils, sourceBitmap = sourceBitmap)) {
            MessageLog.i(TAG, "Bot is awaiting opponent selection for a Unity Cup race.")
            true
        } else {
            MessageLog.i(TAG, "Bot is not at the Race Preparation screen for a mandatory race.")
            false
        }
    }

    /**
     * Checks if the bot is at the Racing screen waiting to be skipped or done manually.
     *
     * @return True if the bot is at the Racing screen. Otherwise, false.
     */
    open fun checkRacingScreen(): Boolean {
        MessageLog.i(TAG, "\nChecking if the bot is sitting on the Racing screen.")
        return if (ButtonChangeRunningStyle.check(game.imageUtils)) {
            MessageLog.i(TAG, "Bot is at the Racing screen waiting to be skipped or done manually.")
            true
        } else {
            MessageLog.i(TAG, "Bot is not at the Racing screen.")
            false
        }
    }

    /**
     * Checks if the bot is at the Ending screen detailing the overall results of the run.
     *
     * @return True if the bot is at the Ending screen. Otherwise false.
     */
    open fun checkEndScreen(): Boolean {
        MessageLog.i(TAG, "\nChecking if the bot is sitting on the End screen.")
        return if (ButtonCompleteCareer.check(game.imageUtils)) {
            true
        } else {
            MessageLog.i(TAG, "Bot is not at the End screen and can keep going.")
            false
        }
    }

    /**
     * Checks if the bot should stop before the finals on turn 72.
     *
     * @return True if the bot should stop. Otherwise false.
     */
    open fun checkFinalsStop(): Boolean {
        if (!enableStopBeforeFinals) {
            Log.d(TAG, "\n[FINALS] Flag is false so skipping Finals check.")
            return false
        } else if (date.day > 72) {
            // If already past turn 72, skip the check to prevent re-checking.
            Log.d(TAG, "\n[FINALS] Turn is greater than 72 so skipping Finals check.")
            return false
        }

        MessageLog.i(TAG, "\n[FINALS] Checking if bot should stop before the finals.")

        // Check if turn is 72, but only stop if we progressed to turn 72 during this run.
        if (date.day == 72 && stopBeforeFinalsInitialTurnNumber != -1) {
            MessageLog.i(TAG, "[FINALS] Detected turn 72. Stopping bot before the finals.")
            game.notificationMessage = "Stopping bot before the finals on turn 72."
            return true
        }

        // Track initial turn number on first check to avoid stopping if bot starts on turn 72.
        if (stopBeforeFinalsInitialTurnNumber == -1) {
            stopBeforeFinalsInitialTurnNumber = date.day
        }

        return false
    }

    /**
     * Checks if the bot should stop at the user specified date.
     *
     * @return True if the bot should stop. Otherwise false.
     */
    open fun checkStopAtDate(): Boolean {
        if (!enableStopAtDate) {
            Log.d(TAG, "\n[DATE] Flag is false so skipping Stop at Date check.")
            return false
        }
        
        MessageLog.i(TAG, "\n[DATE] Checking if bot should stop at the specified date: $stopAtDate with current date ${date}.")
        
        val parts = stopAtDate.split(" ")
        if (parts.size != 3) {
            MessageLog.e(TAG, "[DATE] Invalid Stop at Date format. Expected 'YEAR MONTH PHASE'")
            return false
        }
        
        val targetYear = try { DateYear.valueOf(parts[0].uppercase()) } catch (e: IllegalArgumentException) { null }
        val targetMonth = try { DateMonth.valueOf(parts[1].uppercase()) } catch (e: IllegalArgumentException) { null }
        val targetPhase = try { DatePhase.valueOf(parts[2].uppercase()) } catch (e: IllegalArgumentException) { null }
        
        if (targetYear == null || targetMonth == null || targetPhase == null) {
            MessageLog.e(TAG, "[DATE] Invalid Stop at Date components.")
            return false
        }
        
        val targetDay = GameDate.toDay(targetYear, targetMonth, targetPhase)
        
        // Track initial turn number on first check to avoid stopping immediately if bot starts after the target date
        if (stopAtDateInitialTurnNumber == -1) {
            stopAtDateInitialTurnNumber = date.day
        }
        
        if (date.day >= targetDay && stopAtDateInitialTurnNumber <= targetDay) {
            MessageLog.i(TAG, "[DATE] Reached target date: $stopAtDate (Turn $targetDay). Stopping bot.")
            game.notificationMessage = "Stopping bot at the specified date: $stopAtDate (Turn $targetDay)"
            return true
        }
        
        return false
    }

    /**
     * Checks if the bot has a injury.
     *
     * @return True if the bot has a injury. Otherwise false.
     */
    open fun checkInjury(sourceBitmap: Bitmap? = null): Boolean {
        MessageLog.i(TAG, "\n[INJURY] Checking if there is an injury that needs healing on ${date}.")
        val sourceBitmap = sourceBitmap ?: game.imageUtils.getSourceBitmap()

        return when (ButtonInfirmary.checkDisabled(game.imageUtils, sourceBitmap)) {
            true -> {
                MessageLog.i(TAG, "[INJURY] No injury detected.")
                false
            }
            false -> {
                MessageLog.i(TAG, "[INJURY] Injury detected. Attempting to heal...")
                if (ButtonInfirmary.click(game.imageUtils, sourceBitmap = sourceBitmap)) {
                    game.wait(game.dialogWaitDelay)
                    ButtonOk.click(game.imageUtils, region = game.imageUtils.regionMiddle)
                    game.wait(game.dialogWaitDelay)

                    if (IconInfirmaryEventHeader.check(game.imageUtils)) {
                        MessageLog.i(TAG, "[INJURY] Injury detected and attempted to heal.")
                        true
                    } else {
                        MessageLog.w(TAG, "[INJURY] Injury detected but failed to detect infirmary event.")
                        false
                    }
                } else {
                    MessageLog.w(TAG, "[INJURY] Injury detected but failed to click Infirmary button.")
                    false
                }
            }
            null -> {
                MessageLog.w(TAG, "[INJURY] Failed to detect the Infirmary button.")
                false
            }
        }
    }

    // =============================================================================
    // HELPER FUNCTIONS
    // =============================================================================

    /** Returns whether we are currently in the finale season. */
    open fun checkFinals(): Boolean {
        return date.bIsFinaleSeason ?: false
    }

    /**
     * Updates the date GameDate object by detecting the date on screen.
     *
     * @param isOnMainScreen If true, it can go straight to checking the Main screen for the date. Defaults to true.
     * @return Whether the date changed.
     */
    open fun updateDate(isOnMainScreen: Boolean = true): Boolean {
        MessageLog.i(TAG, "[DATE] Attempting to update the current date.")
        val prevDay: Int = date.day
        if (!date.update(game.imageUtils, isOnMainScreen = isOnMainScreen)) {
            MessageLog.e(TAG, "[DATE] date.update() failed to update date.")
            return false
        }

        if (date.day == prevDay) {
            Log.d(TAG, "[DATE] Date did not change.")
            return false
        } else {
            MessageLog.i(TAG, "[DATE] New date: ${date}")
            return true
        }
    }

    /**
     * Handles the Inheritance event if detected on the screen.
     *
     * @return True if the Inheritance event happened and was accepted. Otherwise false.
     */
    open fun handleInheritanceEvent(): Boolean {
        // Stop checking after Senior Year Early Apr.
        return if (date.day <= 56) {
            if (ButtonInheritance.click(game.imageUtils)) { 
                MessageLog.i(TAG, "\nClaimed an inheritance on ${date}.")
                trainee.bHasUpdatedAptitudes = false
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    /**
     * Attempt to recover energy.
     *
     * @return True if the bot successfully recovered energy. Otherwise false.
     */
    open fun recoverEnergy(sourceBitmap: Bitmap? = null): Boolean {
        MessageLog.i(TAG, "\n[ENERGY] Now starting attempt to recover energy on ${date}.")
        val sourceBitmap: Bitmap = sourceBitmap ?: game.imageUtils.getSourceBitmap()
        
        // First, try to handle recreation date which also recovers energy if a date is available.
        // Skip recreation date if it's already completed (will only be used for mood recovery).
        if (
            !recreationDateCompleted &&
            IconRecreationDate.check(game.imageUtils, sourceBitmap = sourceBitmap) &&
            handleRecreationDate(recoverMoodIfCompleted = false)) {
            MessageLog.i(TAG, "[ENERGY] Successfully recovered energy via recreation date.")
            return true
        }
        
        // Otherwise, fall back to the regular energy recovery logic.
        return when {
            ButtonRest.click(game.imageUtils, sourceBitmap = sourceBitmap) -> {
                ButtonOk.click(game.imageUtils, region = game.imageUtils.regionMiddle)
                // Another OK tap for the possibility of a scheduled race warning popup.
                game.wait(game.dialogWaitDelay)
                ButtonOk.click(game.imageUtils, region = game.imageUtils.regionMiddle)
                game.waitForLoading()
                MessageLog.i(TAG, "[ENERGY] Successfully recovered energy.")
                true
            }
            ButtonRestAndRecreation.click(game.imageUtils, sourceBitmap = sourceBitmap) -> {
                ButtonOk.click(game.imageUtils, region = game.imageUtils.regionMiddle)
                // Another OK tap for the possibility of a scheduled race warning popup.
                game.wait(game.dialogWaitDelay)
                ButtonOk.click(game.imageUtils, region = game.imageUtils.regionMiddle)
                game.waitForLoading()
                MessageLog.i(TAG, "[ENERGY] Successfully recovered energy for the Summer.")
                true
            }
            else -> {
                MessageLog.i(TAG, "[ENERGY] Failed to recover energy. Moving on...")
                false
            }
        }
    }

    /**
     * Attempt to recover mood to always maintain at least Above Normal mood.
     *
     * @return True if the bot successfully recovered mood. Otherwise false.
     */
    open fun recoverMood(sourceBitmap: Bitmap? = null): Boolean {
        MessageLog.i(TAG, "\n[MOOD] Detecting current mood on ${date}.")

        val sourceBitmap = sourceBitmap ?: game.imageUtils.getSourceBitmap()

        // Make sure the trainee's mood is up to date.
        trainee.updateMood(game.imageUtils, sourceBitmap)

        MessageLog.i(TAG, "[MOOD] Detected mood to be ${trainee.mood}.")

        // Only recover mood if its below Good mood and its not Summer.
        return if (training.firstTrainingCheck && trainee.mood == Mood.NORMAL && !ButtonRestAndRecreation.check(game.imageUtils, sourceBitmap = sourceBitmap)) {
            MessageLog.i(TAG, "[MOOD] Current mood is Normal. Not recovering mood due to firstTrainingCheck flag being active. Will need to complete a training first before being allowed to recover mood.")
            false
        } else if ((trainee.mood < Mood.GOOD) && ButtonRestAndRecreation.check(game.imageUtils, sourceBitmap = sourceBitmap)) {
            MessageLog.i(TAG, "[MOOD] Current mood is not good (${trainee.mood}). Recovering mood now.")

            // Check if a date is available.
            if (!recreationDateCompleted && IconRecreationDate.check(game.imageUtils, sourceBitmap = sourceBitmap)) {
                handleRecreationDate(recoverMoodIfCompleted = true)
            } else {
                // Otherwise, recover mood as normal.
                // Note that if a date was already completed, the Recreation popup will still show so it will require an additional step to recover mood.
                recreationDateCompleted = true
                if (!ButtonRecreation.click(game.imageUtils, sourceBitmap = sourceBitmap)) {
                    ButtonRestAndRecreation.click(game.imageUtils, sourceBitmap = sourceBitmap)
                }

                // Tap OK for the possibility of a scheduled race warning popup.
                game.wait(game.dialogWaitDelay)
                if (ButtonOk.click(game.imageUtils, region = game.imageUtils.regionMiddle)) {
                    game.waitForLoading()
                }

                // The Recreation popup is now open so an additional step is required to recover mood.
                if (LabelRecreationUmamusume.click(game.imageUtils)) {
                    MessageLog.i(TAG, "[MOOD] Recreation date is already completed. Recovering mood with the Umamusume now...")
                    game.waitForLoading()
                } else {
                    // Otherwise, dismiss the popup that says to confirm recreation if the user has not set it to skip the confirmation in their in-game settings.
                    ButtonOk.click(game.imageUtils, region = game.imageUtils.regionMiddle)
                    game.waitForLoading()
                }
            }
            true
        } else {
            MessageLog.i(TAG, "[MOOD] Current mood is good enough or its the Summer event. Moving on...")
            false
        }
    }

    /**
     * Handles the Recreation date event if detected on the screen.
     *
     * @param recoverMoodIfCompleted If true, recover mood if the date was already completed. Otherwise, close the recreation popup.
     * @return True if the Recreation date event was successfully completed. False otherwise.
     */
    open fun handleRecreationDate(recoverMoodIfCompleted: Boolean = false): Boolean {
        return if (ButtonRecreation.click(game.imageUtils)) {
            // Tap OK for the possibility of a scheduled race warning popup.
            game.wait(game.dialogWaitDelay)
            ButtonOk.click(game.imageUtils, region = game.imageUtils.regionMiddle)

            MessageLog.i(TAG, "\n[RECREATION_DATE] Recreation has a possible date available.")
            game.wait(1.0)
            // Check if the date is already done.
            if (LabelRecreationDateComplete.check(game.imageUtils)) {
                MessageLog.i(TAG, "[RECREATION_DATE] Recreation date is already completed.")
                recreationDateCompleted = true
                if (recoverMoodIfCompleted) {
                    MessageLog.i(TAG, "[RECREATION_DATE] Mood requires recovery. Recovering mood with the Umamusume now...")
                    LabelRecreationUmamusume.click(game.imageUtils)
                    game.waitForLoading()
                    true
                } else {
                    MessageLog.i(TAG, "[RECREATION_DATE] Mood does not require recovery. Moving on...")
                    ButtonCancel.click(game.imageUtils)
                    true
                }
            } else if (LabelEventProgress.click(game.imageUtils)) {
                game.waitForLoading()
                MessageLog.i(TAG, "[RECREATION_DATE] Recreation date can be done.")
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    /**
     * Handles the Crane Game event by attempting to complete it with three long-press attempts.
     *
     * @return True if the crane game was successfully completed. False otherwise.
     */
    open fun handleCraneGame(): Boolean {
        MessageLog.i(TAG, "\n[CRANE GAME] Starting crane game attempt...")

        // Find the crane game button location.
        val buttonLocation = ButtonCraneGame.find(game.imageUtils)
        val buttonPoint = buttonLocation.first
        if (buttonPoint == null) {
            MessageLog.w(TAG, "\n[CRANE_GAME] Could not find the crane game button. Aborting.")
            return false
        }

        val imageName = ButtonCraneGame.template.path
        val pressDurations = listOf(1.90, 1.00, 0.65)

        // Perform three attempts with different press durations.
        for (attempt in 1..3) {
            val pressDuration = pressDurations[attempt - 1]
            MessageLog.i(TAG, "[CRANE_GAME] Attempt $attempt: Long pressing for ${pressDuration}s...")

            // Perform long press on the button.
            game.gestureUtils.tap(buttonPoint.x, buttonPoint.y, imageName, longPress = true, pressDuration = pressDuration)

            if (attempt < 3) {
                // After attempts 1 and 2, wait for the button to reappear.
                MessageLog.i(TAG, "[CRANE_GAME] Waiting for the crane game button to reappear after attempt $attempt...")
                var buttonReappeared = false
                val maxWaitTime = 30.0
                val checkInterval = 1.0
                var elapsedTime = 0.0

                while (elapsedTime < maxWaitTime) {
                    if (ButtonCraneGame.check(game.imageUtils)) {
                        buttonReappeared = true
                        break
                    }
                    game.wait(checkInterval, skipWaitingForLoading = true)
                    elapsedTime += checkInterval
                }

                if (!buttonReappeared) {
                    MessageLog.w(TAG, "[CRANE_GAME] The crane game button did not reappear within ${maxWaitTime} seconds after attempt $attempt.")
                }

                game.wait(1.0)
            } else {
                MessageLog.i(TAG, "[CRANE_GAME] Final attempt completed.")
                return true
            }
        }

        return false
    }

    /**
     * Handles the skill list screen to purchase skills.
     *
     * This function initiates the skill purchasing process using the specified
     * skill plan. If no plan name is provided, the default skill plan is used.
     *
     * @param skillPlanName The optional name of the skill plan to use.
     * @return True if the skill purchasing process was successful, false otherwise.
     */
    open fun handleSkillListScreen(skillPlanName: String? = null): Boolean {
        MessageLog.i(TAG, "Beginning process to purchase skills...")
        return skillPlan.start(skillPlanName)
    }

    /**
     * Opens the Umamusume Details dialog to update trainee aptitudes.
     *
     * This function only opens the dialog - the actual aptitude update is performed
     * by [handleDialogs] when it processes the "umamusume_details" dialog.
     */
    open fun openAptitudesDialog() {
        MessageLog.d(TAG, "Opening aptitudes dialog...")
        ButtonHomeFullStats.click(game.imageUtils)
        game.wait(game.dialogWaitDelay, skipWaitingForLoading = true)
    }

    /**
     * Opens the Umamusume Class dialog to update trainee fan count.
     *
     * This function only opens the dialog - the actual fan count update is performed
     * by [handleDialogs] when it processes the "umamusume_class" dialog.
     */
    abstract fun openFansDialog()

    /**
     * Detects the trainee's current fan count class from the main screen.
     *
     * This reads the fan count class label directly from the screen using OCR
     * without opening any dialogs.
     *
     * @param bitmap Optional pre-captured bitmap to analyze.
     * @return The detected [FanCountClass], or NULL if detection failed.
     */
    open fun getFanCountClass(bitmap: Bitmap? = null): FanCountClass? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        val templateBitmap: Bitmap? = ButtonHomeFansInfo.template.getBitmap(game.imageUtils)
        if (templateBitmap == null) {
            MessageLog.e(TAG, "getFanCountClass: Could not get template bitmap for ButtonHomeFansInfo: ${ButtonHomeFansInfo.template.path}.")
            return null
        }
        val point: Point? = ButtonHomeFansInfo.findImageWithBitmap(game.imageUtils, sourceBitmap = bitmap)
        if (point == null) {
            MessageLog.w(TAG, "getFanCountClass: Could not find ButtonHomeFansInfo.")
            return null
        }

        val bbox = BoundingBox(
            x = game.imageUtils.relX(0.0, (point.x - (templateBitmap.width / 2)).toInt() - 180),
            // Add a small buffer to vertical component.
            y = game.imageUtils.relY(0.0, (point.y - 16).toInt()),
            w = game.imageUtils.relWidth(180),
            // 32px minimum for google ML kit.
            h = game.imageUtils.relHeight(32),
        )

        val text: String = game.imageUtils.performOCROnRegion(
            bitmap,
            bbox.x,
            bbox.y,
            bbox.w,
            bbox.h,
            useThreshold = false,
            useGrayscale = true,
            scale = 1.0,
            ocrEngine = "tesseract",
            debugName = "getFanCountClass",
        )
        val fanCountClass: FanCountClass? = FanCountClass.fromName(text.replace(" ", "_"))
        if (fanCountClass == null) {
            MessageLog.w(TAG, "getFanCountClass:: Failed to match text to a FanCountClass: $text")
        }
        return fanCountClass
    }

    /**
     * Handles the fallback logic when racing fails.
     * This includes checking for mandatory race detection and falling back to training.
     *
     * @return True if the bot should break out of the main loop, false otherwise.
     */
    open fun handleRaceEventFallback(): Boolean {
        if (racing.detectedMandatoryRaceCheck) {
            MessageLog.i(TAG, "\n[END] Stopping bot due to detection of Mandatory Race.")
            game.notificationMessage = "Stopping bot due to detection of Mandatory Race."
            if (DiscordUtils.enableDiscordNotifications) {
                DiscordUtils.queue.add("```diff\n- ${MessageLog.getSystemTimeString()} Stopping bot due to detection of Mandatory Race.\n```")
            }
            return true
        }
        ButtonBack.click(game.imageUtils)
        ButtonCancel.click(game.imageUtils)
        ButtonClose.click(game.imageUtils)
        game.wait(1.0)
        training.handleTraining()
        return false
    }

    /**
     * Perform misc checks to potentially fix instances where the bot is stuck.
     *
     * @return True if the checks passed. Otherwise false if the bot encountered a warning popup and needs to exit.
     */
    open fun performMiscChecks(): Boolean {
        MessageLog.i(TAG, "\n[MISC] Beginning check for misc cases...")

        val sourceBitmap = game.imageUtils.getSourceBitmap()

        if (game.enablePopupCheck && ButtonCancel.check(game.imageUtils, sourceBitmap = sourceBitmap)) {
            MessageLog.i(TAG, "\n[END] Bot may have encountered a warning popup. Exiting now...")
            game.notificationMessage = "Bot may have encountered a warning popup"
            if (DiscordUtils.enableDiscordNotifications) {
                DiscordUtils.queue.add("```diff\n- ${MessageLog.getSystemTimeString()} Bot may have encountered a warning popup. Exiting now...\n```")
            }
            return false
        } else if (ButtonNext.click(game.imageUtils, sourceBitmap = sourceBitmap)) {
            // Now confirm the completion of a Training Goal popup.
            MessageLog.i(TAG, "[MISC] Popup detected that needs to be dismissed with the \"Next\" button.")
            game.wait(2.0)
            ButtonNext.click(game.imageUtils)
            game.wait(1.0)
        } else if (ButtonCraneGame.check(game.imageUtils, sourceBitmap = sourceBitmap)) {
            if (enableCraneGameAttempt) {
                handleCraneGame()
            } else {
                // Stop when the bot has reached the Crane Game Event.
                MessageLog.i(TAG, "\n[END] Bot will stop due to the detection of the Crane Game Event.")
                game.notificationMessage = "Bot will stop due to the detection of the Crane Game Event."
                if (DiscordUtils.enableDiscordNotifications) {
                    DiscordUtils.queue.add("```diff\n- ${MessageLog.getSystemTimeString()} Bot will stop due to the detection of the Crane Game Event.\n```")
                }
                return false
            }
        } else if (
            LabelOrdinaryCuties.check(game.imageUtils, sourceBitmap = sourceBitmap) &&
            ButtonCraneGameOk.check(game.imageUtils, sourceBitmap = sourceBitmap)
        ) {
            ButtonCraneGameOk.click(game.imageUtils, sourceBitmap = sourceBitmap)
            game.waitForLoading()
            MessageLog.i(TAG, "[CRANE GAME] Event exited.")
        } else if (ButtonNextRaceEnd.click(game.imageUtils, sourceBitmap = sourceBitmap)) {
            MessageLog.i(TAG, "[MISC] Ended a leftover race.")
            // Clicking this button triggers connection to server.
            game.waitForLoading()
        } else if (IconRaceNotEnoughFans.check(game.imageUtils, sourceBitmap = sourceBitmap)) {
            MessageLog.i(TAG, "[MISC] There was a popup about insufficient fans.")
            racing.encounteredRacingPopup = true
            ButtonCancel.click(game.imageUtils, sourceBitmap = sourceBitmap)
        } else if (ButtonBack.click(game.imageUtils, sourceBitmap = sourceBitmap)) {
            MessageLog.i(TAG, "[MISC] Navigating back a screen since all the other misc checks have been completed.")
            game.wait(1.0)
        } else if (ButtonSkip.click(game.imageUtils, sourceBitmap = sourceBitmap)) {
            MessageLog.d(TAG, "[MISC] Clicked skip button.")
        } else if (!BotService.isRunning) {
            MessageLog.i(TAG, "\n[END] BotService is not running. Exiting now...")
            throw InterruptedException()
        } else {
            MessageLog.i(TAG, "[MISC] Did not detect any popups or the Crane Game on the screen. Moving on...")
        }

        return true
    }

    /**
     * Handles all main screen logic including daily updates, racing decisions, and training.
     *
     * This is the primary decision-making function that determines what action the bot
     * should take when at the main screen. It handles date changes, aptitude/fan updates,
     * race detection, mood recovery, and training.
     *
     * @return True if the main screen was detected and handled, false otherwise.
     */
    open fun handleMainScreen(): Boolean {
        if (!checkMainScreen()) {
            return false
        }

        // Perform first-time setup of loading the user's race agenda if needed.
        racing.loadUserRaceAgenda()

        val sourceBitmap = game.imageUtils.getSourceBitmap()

        // Operations to be done every time the date changes.
        // Skip if we've already checked the date this turn and no game-advancing action was taken.
        if (!bHasCheckedDateThisTurn) {
            if (updateDate() || !trainee.bHasUpdatedStats) {

                // Reset flags on date change.
                racing.encounteredRacingPopup = false
                racing.raceRepeatWarningCheck = false
                bHasTriedCheckingFansToday = false
                bHasCheckedForMaidenRaceToday = false

                // Update the fan count class every time we're at the main screen.
                val fanCountClass: FanCountClass? = getFanCountClass(sourceBitmap)
                if (fanCountClass != null) {
                    trainee.fanCountClass = fanCountClass
                }

                // Update trainee information using parallel processing with shared screenshot.
                val skillPointsLocation = LabelStatTableHeaderSkillPoints.findImageWithBitmap(game.imageUtils, sourceBitmap = sourceBitmap)

                if (!BotService.isRunning) {
                    return false
                }
                
                // Use CountDownLatch to run the operations in parallel.
                // 1 racingRequirements (skipped during summer) + 5 stats + 1 skill points + 1 mood = 8 (or 7) threads.
                val latch = if (date.isSummer()) CountDownLatch(7) else CountDownLatch(8)

                MessageLog.disableOutput = true
                
                // Threads 1-5: Update stats (one thread per stat, created inside updateStats).
                // Pass the external latch so updateStats can count down for each stat thread.
                trainee.updateStats(game.imageUtils, sourceBitmap, skillPointsLocation, latch)
                
                // Thread 6: Update skill points.
                Thread {
                    try {
                        trainee.updateSkillPoints(game.imageUtils, sourceBitmap, skillPointsLocation)
                    } catch (e: Exception) {
                        MessageLog.e(TAG, "Error in updateSkillPoints thread: ${e.stackTraceToString()}")
                    } finally {
                        latch.countDown()
                    }
                }.apply { isDaemon = true }.start()
                
                // Thread 7: Update mood.
                Thread {
                    try {
                        trainee.updateMood(game.imageUtils, sourceBitmap)
                    } catch (e: Exception) {
                        MessageLog.e(TAG, "Error in updateMood thread: ${e.stackTraceToString()}")
                    } finally {
                        latch.countDown()
                    }
                }.apply { isDaemon = true }.start()

                // Thread 8: Update racing requirements.
                if (!date.isSummer()) {
                    Thread {
                        try {
                            racing.checkRacingRequirements(sourceBitmap)
                        } catch (e: Exception) {
                            MessageLog.e(TAG, "Error in checkRacingRequirements thread: ${e.stackTraceToString()}")
                        } finally {
                            latch.countDown()
                        }
                    }.apply { isDaemon = true }.start()
                }
                
                // Wait for all threads to complete.
                try {
                    latch.await(10, TimeUnit.SECONDS)
                } catch (_: InterruptedException) {
                    MessageLog.e(TAG, "Date change operations threads timed out.")
                } finally {
                    MessageLog.disableOutput = false
                }
                MessageLog.i(TAG, "[TRAINEE] Skills Updated: ${trainee.getStatsString()}")
                MessageLog.i(TAG, "[TRAINEE] Mood Updated: ${trainee.mood}")
                if (trainee.bHasUpdatedAptitudes) {
                    trainee.logInfo()
                    trainee.logDetailedPlayerInfo()
                }

                // Now check if we need to handle skills before finals.
                if (date.day == 72 && skillPlan.skillPlans["preFinals"]?.bIsEnabled ?: false) {
                    ButtonSkills.click(game.imageUtils)
                    game.wait(1.0)
                    if (!handleSkillListScreen()) {
                        MessageLog.w(TAG, "handleMainScreen:: handleSkillList() for Pre-Finals failed.")
                    }
                }
            }

            // Mark that we've checked the date this turn.
            bHasCheckedDateThisTurn = true
        }

        // If we haven't already handled the skill point check this run and
        // if the required skill points has been reached,
        // stop the bot or run the skill plan if it is enabled.
        if (
            !bHasHandledSkillPointCheck &&
            enableSkillPointCheck &&
            trainee.skillPoints >= skillPointsRequired
        ) {
            if (skillPlan.skillPlans["skillPointCheck"]?.bIsEnabled ?: false) {
                ButtonSkills.click(game.imageUtils)
                game.wait(1.0)
                if (!handleSkillListScreen("skillPointCheck")) {
                    throw InterruptedException("handleMainScreen:: handleSkillList() for Skill Point Check failed. Stopping bot...")
                }
                bHasHandledSkillPointCheck = true
            } else {
                throw CampaignBreakpointException("Bot reached skill point check threshold. Stopping bot...")
            }
        }

        // Since we're at the main screen, we don't need to worry about this
        // flag anymore since we will update our aptitudes here if needed.
        trainee.bTemporaryRunningStyleAptitudesUpdated = false

        if (!trainee.bHasUpdatedAptitudes) {
            openAptitudesDialog()
            tryHandleAllDialogs()
        }

        val bIsScheduledRaceDay = LabelScheduledRace.check(game.imageUtils, sourceBitmap = sourceBitmap)
        val bIsMandatoryRaceDay = IconRaceDayRibbon.check(game.imageUtils, sourceBitmap = sourceBitmap)
        var needToRace = bIsMandatoryRaceDay || bIsScheduledRaceDay

        // We don't need to bother checking fans on a mandatory race day.
        if (
            !date.bIsFinaleSeason &&
            !bIsMandatoryRaceDay &&
            !bIsScheduledRaceDay &&
            bNeedToCheckFans &&
            !bHasTriedCheckingFansToday
        ) {
            openFansDialog()
            tryHandleAllDialogs()
        }

        // Check if bot should stop before the finals.
        if (checkFinalsStop()) {
            throw InterruptedException(game.notificationMessage)
        }

        // Check if bot should stop at the user specified date.
        if (checkStopAtDate()) {
            throw InterruptedException(game.notificationMessage)
        }

        if (!needToRace && !racing.encounteredRacingPopup) {
            if (racing.enableForceRacing) {
                // If force racing is enabled, skip all other activities and go straight to racing.
                MessageLog.i(TAG, "Force racing enabled - skipping all other activities and going straight to racing.")
                needToRace = true
            } else if (
                !bHasCheckedForMaidenRaceToday &&
                !date.bIsPreDebut &&
                !trainee.bHasCompletedMaidenRace
            ) {
                // Need to check for maiden race.
                MessageLog.i(TAG, "[INFO] Bot has not yet completed maiden race. Checking for valid maiden race...")
                needToRace = true
            } else if (
                mustRestBeforeSummer &&
                (   date.year == DateYear.CLASSIC ||
                    date.year == DateYear.SENIOR
                ) &&
                date.month == DateMonth.JUNE &&
                date.phase == DatePhase.LATE
            ) {
                // Check if we need to rest before Summer Training (June Early/Late in Classic/Senior Year).
                MessageLog.i(TAG, "Forcing rest during ${date} in preparation for Summer Training.")
                recoverEnergy(sourceBitmap)
                bHasCheckedDateThisTurn = false
            } else {
                val isRacingRequirementActive = racing.hasFanRequirement || racing.hasTrophyRequirement
                val isFinals = checkFinals()

                if (isRacingRequirementActive) {
                    MessageLog.i(TAG, "[INFO] Racing requirement is active. Bypassing health and mood checks.")
                    needToRace = true
                } else {
                    val hasInjury = if (isFinals) {
                        MessageLog.i(TAG, "[INFO] Skipping injury check due to it being the Finals.")
                        false
                    } else {
                        checkInjury(sourceBitmap)
                    }

                    if (hasInjury) {
                        ButtonOk.click(game.imageUtils, sourceBitmap = sourceBitmap, region = game.imageUtils.regionMiddle)
                        game.wait(3.0)
                        bHasCheckedDateThisTurn = false
                    } else {
                        val didRecoverMood = if (isFinals) {
                            MessageLog.i(TAG, "[INFO] Skipping mood recovery due to it being the Finals.")
                            false
                        } else {
                            recoverMood(sourceBitmap)
                        }

                        if (didRecoverMood) {
                            bHasCheckedDateThisTurn = false
                        } else {
                            val eligibleForExtraRace = racing.checkEligibilityToStartExtraRacingProcess()
                            
                            if (eligibleForExtraRace) {
                                MessageLog.i(TAG, "[INFO] Bot has no injuries, mood is sufficient and extra races can be run today. Setting the needToRace flag to true.")
                                needToRace = true
                            } else if (date.day >= 16) {
                                // If not eligible for an extra race but we passed the day threshold, we train.
                                MessageLog.i(TAG, "[INFO] Training due to it not being an extra race day.")
                                training.handleTraining()
                                bHasCheckedDateThisTurn = false
                            } else {
                                // Fallback to training for early game.
                                MessageLog.i(TAG, "[INFO] Training due to it not being an extra race day.")
                                training.handleTraining()
                                bHasCheckedDateThisTurn = false
                            }
                        }
                    }
                }
            }
        }

        if (racing.encounteredRacingPopup || needToRace) {
            MessageLog.i(TAG, "[INFO] All checks are cleared for racing.")
            if (!handleRaceEvents(bIsScheduledRaceDay) && handleRaceEventFallback()) {
                throw CampaignBreakpointException("Mandatory race detected. Stopping bot...")
            }
            bHasCheckedDateThisTurn = false
        }
        return true
    }

    open override fun handleDialogs(dialog: DialogInterface?, args: Map<String, Any>): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog, args)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "consecutive_race_warning" -> {
                val overrideIgnoreConsecutiveRaceWarning = args["overrideIgnoreConsecutiveRaceWarning"] as? Boolean ?: false
                racing.raceRepeatWarningCheck = true
                if (overrideIgnoreConsecutiveRaceWarning || racing.enableForceRacing || racing.ignoreConsecutiveRaceWarning) {
                    MessageLog.i(TAG, "[RACE] Consecutive race warning! Racing anyway...")
                    result.dialog.ok(game.imageUtils)
                    // This dialog requires a little extra delay since it loads the
                    // race list instead of just closing the dialog.
                    game.wait(1.0, skipWaitingForLoading = true)
                } else {
                    MessageLog.i(TAG, "[RACE] Consecutive race warning! Aborting racing...")
                    racing.clearRacingRequirementFlags()
                    result.dialog.close(game.imageUtils)
                }
            }
            "goal_not_reached" -> {
                // We are handling the logic for when to race on our own.
                // Thus we just close this warning.
                racing.encounteredRacingPopup = true
                result.dialog.close(game.imageUtils)
            }
            "insufficient_fans" -> {
                // We are handling the logic for when to race on our own.
                // Thus we just close this warning.
                racing.encounteredRacingPopup = true
                result.dialog.close(game.imageUtils)
            }
            "scheduled_race_available" -> {
                MessageLog.i(TAG, "[INFO] There is a scheduled race today. Racing it now...")
                result.dialog.ok(game.imageUtils)
                if (!racing.handleRaceEvents(isScheduledRace = true) && handleRaceEventFallback()) {
                    throw IllegalStateException("Stopping the bot due to failing to handle a scheduled race.")
                }
            }
            "strategy" -> {
                if (!trainee.bHasUpdatedAptitudes) {
                    trainee.bTemporaryRunningStyleAptitudesUpdated = racing.updateRaceScreenRunningStyleAptitudes()
                }

                if (date.day == 1) {
                    MessageLog.i(TAG, "[DIALOG] strategy:: Unknown date. Using Original strategy.")
                }

                var runningStyle: RunningStyle? = null
                val runningStyleString: String = when {
                    // Special case for when the bot has not been able to check the date
                    // i.e. when the bot starts at the race screen.
                    date.day == 1 -> racing.userSelectedOriginalStrategy
                    date.year == DateYear.JUNIOR -> racing.juniorYearRaceStrategy
                    else -> racing.userSelectedOriginalStrategy
                }
                when (runningStyleString.uppercase()) {
                    // Do not select a strategy. Use what is already selected.
                    "DEFAULT" -> {
                        MessageLog.i(TAG, "[DIALOG] strategy:: Using the default running style.")
                        result.dialog.ok(game.imageUtils)
                        // Confirming this dialog triggers connection to server.
                        game.waitForLoading()
                        // If date is unknown we want to set style next time we're at
                        // race prep screen. See note at bottom of this handler.
                        trainee.bHasSetRunningStyle = date.day != 1
                        racing.bHasSetTemporaryRunningStyle = true
                        return DialogHandlerResult.Handled(result.dialog)
                    }
                    // Auto-select the optimal running style based on trainee aptitudes.
                    "AUTO" -> {
                        MessageLog.i(TAG, "[DIALOG] strategy:: Auto-selecting the trainee's optimal running style.")
                        runningStyle = trainee.runningStyle
                    }
                    else -> {
                        MessageLog.i(TAG, "[DIALOG] strategy:: Using user-specified running style: $runningStyleString")
                        runningStyle = RunningStyle.fromShortName(runningStyleString)
                    }
                }

                when (runningStyle) {
                    RunningStyle.FRONT_RUNNER -> ButtonRaceStrategyFront.click(game.imageUtils)
                    RunningStyle.PACE_CHASER -> ButtonRaceStrategyPace.click(game.imageUtils)
                    RunningStyle.LATE_SURGER -> ButtonRaceStrategyLate.click(game.imageUtils)
                    RunningStyle.END_CLOSER -> ButtonRaceStrategyEnd.click(game.imageUtils)
                    null -> {
                        // This indicates programmer error.
                        MessageLog.e(TAG, "[DIALOG] strategy:: Invalid running style: $runningStyle")
                        result.dialog.close(game.imageUtils)
                        trainee.bHasSetRunningStyle = false
                        return DialogHandlerResult.Handled(result.dialog)
                    }
                }

                // We only want to set this flag if the date has been checked.
                // Otherwise, if the day is still 1, that means we probably started
                // the bot at the racing screen. In this case, we still want to set
                // the running style the next time we get back to the race
                // selection screen after verifying the date.
                if (date.day != 1) {
                    trainee.bHasSetRunningStyle = true
                }
                racing.bHasSetTemporaryRunningStyle = true
                result.dialog.ok(game.imageUtils)
            }
            "try_again" -> {
                // All branches need a slight delay to allow the dialog to close
                // since the runRaceWithRetries loop handles dialogs at the start
                // of each iteration. Can cause problem where we handle one branch
                // then immediately handle dialogs again and handle a second branch
                // for the same dialog instance.
                if (racing.disableRaceRetries) {
                    if (racing.enableFreeRaceRetry && IconOneFreePerDayTooltip.check(game.imageUtils)) {
                        MessageLog.i(TAG, "[RACE] Failed mandatory race. Using daily free race retry...")
                        racing.raceRetries--
                        result.dialog.ok(game.imageUtils)
                        game.wait(0.5)
                        return DialogHandlerResult.Handled(result.dialog)
                    }
                    if (racing.enableCompleteCareerOnFailure) {
                        MessageLog.i(TAG, "[RACE] Failed a mandatory race and no retries remaining. Completing career...")
                        // Manually set retries to -1 to break the race retry loop.
                        racing.raceRetries = -1
                        result.dialog.close(game.imageUtils)
                        game.wait(0.5)
                        return DialogHandlerResult.Handled(result.dialog)
                    }
                    MessageLog.i(TAG, "\n[END] Stopping the bot due to failing a mandatory race.")
                    MessageLog.i(TAG, "********************")
                    game.notificationMessage = "Stopping the bot due to failing a mandatory race."
                    if (DiscordUtils.enableDiscordNotifications) {
                        DiscordUtils.queue.add("```diff\n- ${MessageLog.getSystemTimeString()} Stopping the bot due to failing a mandatory race.\n```")
                    }
                    throw IllegalStateException()
                }
                racing.raceRetries--
                result.dialog.ok(game.imageUtils)
            }
            "umamusume_class" -> {
                val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
                val templateBitmap: Bitmap? = game.imageUtils.getBitmaps(LabelUmamusumeClassFans.template.path).second
                if (templateBitmap == null) {
                    MessageLog.e(TAG, "[DIALOG] umamusume_class: Could not get template bitmap for LabelUmamusumeClassFans: ${LabelUmamusumeClassFans.template.path}.")
                    result.dialog.close(game.imageUtils)
                    return DialogHandlerResult.Handled(result.dialog)
                }
                val point: Point? = LabelUmamusumeClassFans.find(game.imageUtils).first
                if (point == null) {
                    MessageLog.w(TAG, "[DIALOG] umamusume_class: Could not find LabelUmamusumeClassFans.")
                    result.dialog.close(game.imageUtils)
                    return DialogHandlerResult.Handled(result.dialog)
                }

                // Add a small 8px buffer to vertical component.
                val bbox = BoundingBox(
                    x = game.imageUtils.relX(0.0, (point.x + (templateBitmap.width / 2)).toInt()),
                    y = game.imageUtils.relY(0.0, (point.y - (templateBitmap.height / 2) - 4).toInt()),
                    w = game.imageUtils.relWidth(300),
                    h = game.imageUtils.relHeight(templateBitmap.height + 4),
                )

                val croppedBitmap = game.imageUtils.createSafeBitmap(
                    bitmap,
                    bbox.x,
                    bbox.y,
                    bbox.w,
                    bbox.h,
                    "dialog::umamusume_class: Cropped bitmap.",
                )
                if (croppedBitmap == null) {
                    MessageLog.e(TAG, "[DIALOG] umamusume_class: Failed to crop bitmap.")
                    result.dialog.close(game.imageUtils)
                    return DialogHandlerResult.Handled(result.dialog)
                }
                val fans = game.imageUtils.getUmamusumeClassDialogFanCount(croppedBitmap)
                if (fans != null) {
                    trainee.fans = fans
                    bNeedToCheckFans = false
                    MessageLog.d(TAG, "[DIALOG] umamusume_class: Updated fan count: ${trainee.fans}")
                } else {
                    MessageLog.w(TAG, "[DIALOG] umamusume_class: getUmamusumeClassDialogFanCount returned NULL.")
                }
                
                result.dialog.close(game.imageUtils)

                // Print the trainee info with the updated fan count.
                trainee.logInfo()
                trainee.logDetailedPlayerInfo()
            }
            "umamusume_details" -> {
                val prevTrackSurface = trainee.trackSurface
                val prevTrackDistance = trainee.trackDistance
                val prevRunningStyle = trainee.runningStyle
                trainee.updateAptitudes(game.imageUtils)
                trainee.updateStats(game.imageUtils, isAptitudeDialog = true)
                trainee.bTemporaryRunningStyleAptitudesUpdated = false

                // Read the trainee's name once per run while the dialog is still open.
                if (trainee.name.isEmpty()) {
                    trainee.readName(game.imageUtils)
                }

                if (trainee.runningStyle != prevRunningStyle) {
                    // Reset this flag since our preferred running style has changed.
                    trainee.bHasSetRunningStyle = false
                }
                result.dialog.close(game.imageUtils)
            }
            else -> {
                Log.w(TAG, "[DIALOG] Unknown dialog \"${result.dialog.name}\" detected so it will not be handled.")
                return DialogHandlerResult.Unhandled(result.dialog)
            }
        }

        game.wait(0.5)
        return DialogHandlerResult.Handled(result.dialog)
    }

    open override fun process(): TaskResult? {
        try {
            // We always check for dialogs first.
            if (tryHandleAllDialogs()) {
                return null
            }

            if (handleMainScreen()) {
                return null
            }

            if (checkTrainingEventScreen()) {
                // If the bot is at the Training Event screen, that means there are selectable options for rewards.
                handleTrainingEvent()
            } else if (checkMandatoryRacePrepScreen()) {
                // If the bot is at the Main screen with the button to select a race visible,
                // that means the bot needs to handle a mandatory race.
                if (!handleRaceEvents() && racing.detectedMandatoryRaceCheck) {
                    return TaskResult.Success(
                        TaskResultCode.TASK_RESULT_BREAKPOINT_REACHED,
                        "Mandatory race detected. Stopping bot...",
                    )
                }
            } else if (checkRacingScreen()) {
                // If the bot is already at the Racing screen, then complete this standalone race.
                racing.handleStandaloneRace()
            } else if (checkEndScreen()) {
                // Stop when the bot has reached the screen where it details the overall result of the run.
                if (skillPlan.skillPlans["careerComplete"]?.bIsEnabled ?: false) {
                    game.wait(0.5)
                    ButtonCareerEndSkills.click(game.imageUtils)
                    game.wait(1.0)
                    if (!handleSkillListScreen()) {
                        MessageLog.w(TAG, "Career End Screen: handleSkillList() failed.")
                    }
                }
                return TaskResult.Success(
                    TaskResultCode.TASK_RESULT_COMPLETE,
                    "Bot has reached end of run. Stopping bot...",
                )
            } else if (checkCampaignSpecificConditions()) {
                MessageLog.i(TAG, "Campaign-specific checks complete.")
            } else if (handleInheritanceEvent()) {
                // If the bot is at the Inheritance screen, then accept the inheritance.
            } else if (performMiscChecks()) {
                MessageLog.d(TAG, "Misc checks complete.")
            } else {
                MessageLog.v(TAG, "Did not detect the bot being at the following screens: Main, Training Event, Inheritance, Mandatory Race Preparation, Racing and Career End.")
                // Tap to progress any intermediate screens.
                game.tap(350.0, 450.0, taps = 1)
            }
        } catch (e: CampaignBreakpointException) {
            return TaskResult.Success(
                TaskResultCode.TASK_RESULT_BREAKPOINT_REACHED,
                e.message ?: "Campaign breakpoint reached. Stopping bot...",
            )
        }

        return null
    }
}
