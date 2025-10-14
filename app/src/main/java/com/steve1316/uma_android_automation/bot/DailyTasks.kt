package com.steve1316.uma_android_automation.bot.campaigns

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.components.*
import com.steve1316.uma_android_automation.dialog.DialogInterface
import com.steve1316.uma_android_automation.utils.AppEvent
import com.steve1316.uma_android_automation.utils.EventBus
import com.steve1316.uma_android_automation.utils.GameUtils
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.utils.MessageLog
import kotlinx.coroutines.CoroutineScope
import org.opencv.core.Point

import kotlin.system.measureTimeMillis

class DailyTasks(val game: Game, val coroutineScope: CoroutineScope) {
    private val TAG: String = "DailyTasks"

    private var teamTrialsComplete: Boolean = true
    private var dailyRacesComplete: Boolean = true
    private var legendRacesComplete: Boolean = true
    private var dailyMissionsCollected: Boolean = false

    init {
        EventBus.subscribe<AppEvent.DialogEvent>(coroutineScope) { event ->
            handleDialogs(event.dialog)
        }
    }

    fun handleDialogs(dialog: DialogInterface?) {
        MessageLog.d(TAG, "handleDialogs:: ${dialog?.name}")
        when (dialog?.name) {
            "confirm_restore_rp" -> {
                MessageLog.d(TAG, "Dialog Event: TeamTrials: Confirm Restore RP")
                teamTrialsComplete = true
                // Escape from TeamTrials menus.
                ButtonNextWithImage.click(imageUtils=game.imageUtils)
                GameUtils.wait(0.1, imageUtils=game.imageUtils)
                ButtonMenuBarHome.click(imageUtils=game.imageUtils)
            }
            "purchase_daily_race_ticket" -> {
                MessageLog.d(TAG, "Dialog Event: DailyRaces: Purchase Daily Race Tickets")
                dailyRacesComplete = true
            }
            "items_selected" -> {
                MessageLog.d(TAG, "Dialog Event: General: Items Selected")
                dialog.ok(imageUtils=game.imageUtils, tries=5)
                return
            }
            "race_details" -> {
                MessageLog.d(TAG, "Dialog Event: General: Race Details")
                dialog.ok(imageUtils=game.imageUtils, tries=5)
                return
            }
            "special_missions" -> {
                MessageLog.d(TAG, "Dialog Event: General: Special Missions")
                dialog.ok(imageUtils=game.imageUtils, tries=5)
                legendRacesComplete = true
                return
            }
        }

        dialog?.close(imageUtils=game.imageUtils)
    }

    fun start() {
        //GameUtils.wait(2.0, imageUtils=game.imageUtils) // delay to allow bot toast to disappear
        while (
            teamTrialsComplete == false ||
            dailyRacesComplete == false ||
            legendRacesComplete == false ||
            dailyMissionsCollected == false
        ) {
            // TODO: Move this check outside of DailyTasks and into main game or something.
            if (ScreenTitle.check(imageUtils=game.imageUtils)) {
                handleTitleScreenOperations()
            } else if (ScreenRace.check(imageUtils=game.imageUtils)) {
                // Catch-all for any races that we may be in.
                game.handleStandaloneRaceNonCampaign(strategyName="default")
            } else {
                if (!teamTrialsComplete) {
                    handleTeamTrials()
                } else if (!dailyRacesComplete) {
                    handleDailyRaces()
                } else if (!legendRacesComplete) {
                    handleLegendRaces()
                } else if (!dailyMissionsCollected) {
                    dailyMissionsCollected = game.collectAllRewards()
                    MessageLog.d(TAG, "dailyMissionsCollected = $dailyMissionsCollected")
                }
            }
        }
        MessageLog.d(TAG, "All daily tasks complete. Stopping...")
        game.notificationMessage = "Daily Tasks Completed"
    }

    fun handleTitleScreenOperations() {
        // Navigate from title screen to home screen.
        ScreenTitle.click(game.imageUtils, tries=3)
        while (!ButtonMenuBarHome.check(imageUtils=game.imageUtils)) {
            ButtonSkip.click(imageUtils=game.imageUtils)
        }
    }

    // ==================
    // TEAM TRIALS
    // ==================

    fun checkTeamTrialsRaceResult(): Boolean {
        return (
            LabelTeamTrialsRaceResultWin.check(imageUtils=game.imageUtils) ||
            LabelTeamTrialsRaceResultLose.check(imageUtils=game.imageUtils) ||
            LabelTeamTrialsRaceResultDraw.check(imageUtils=game.imageUtils)
        )
    }

    fun handleTeamTrials() {
        if (teamTrialsComplete) {
            // If we are out of RP, dialog handler will catch
            // this dialog and set the completed flag for us.
            MessageLog.i(TAG, "handleTeamTrials:: teamTrialsComplete=true")
            return
        } else if (ButtonMenuBarRaceUnselected.click(imageUtils=game.imageUtils)) {
            // Now at the Race menu.
            MessageLog.d(TAG, "handleTeamTrials:: ButtonMenuBarRaceUnselected.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonTeamTrials.click(imageUtils=game.imageUtils)) {
            // Race -> TeamTrials
            MessageLog.d(TAG, "handleTeamTrials:: ButtonTeamTrials.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonTeamRace.click(imageUtils=game.imageUtils)) {
            // Race -> TeamTrials -> TeamRace
            MessageLog.d(TAG, "handleTeamTrials:: ButtonTeamRace.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonSeeResults.click(imageUtils=game.imageUtils)) {
            MessageLog.d(TAG, "handleTeamTrials:: ButtonSeeResults.click(imageUtils=game.imageUtils)")
            return
        } else if (LabelTeamTrialsSelectOpponent.check(imageUtils=game.imageUtils)) {
            // At the opponent selection screen. Click just below the
            // "Select Opponent" label (top opponent)
            // TODO: Add some logic to click opponent with prize every win.
            MessageLog.d(TAG, "handleTeamTrials:: LabelTeamTrialsSelectOpponent.check(imageUtils=game.imageUtils)")
            val point = LabelTeamTrialsSelectOpponent.find(imageUtils=game.imageUtils).first
            if (point == null) {
                return
            }
            ComponentUtils.tap(
                point.x,
                point.y + game.imageUtils.relHeight(100),
                "team_trials_select_opponent",
                taps=1,
                ignoreWaiting=true,
            )
            return
        } else if (ButtonNext.click(imageUtils=game.imageUtils)) {
            // Many cases where the generic NEXT button appears.
            // In all cases for team trials it is our only option so we
            // just always want to click it.
            MessageLog.d(TAG, "handleTeamTrials:: ButtonNext.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonRaceAgain.click(imageUtils=game.imageUtils)) {
            // The race again button must come before the NextWithImage button.
            // Otherwise, we might click NextWithImage and exit from the TeamTrials
            // menu before we're actually done there. This should auto recover
            // but would just waste time.
            MessageLog.d(TAG, "handleTeamTrials:: ButtonRaceAgain.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonNextWithImage.click(imageUtils=game.imageUtils)) {
            // This should only appear in two cases:
            // 1) Extra rewards at end of team trials match. In this case,
            // the RaceAgain button will not be on the screen at the same time.
            // 2) After a match, both this and the RaceAgain button will be on screen.
            MessageLog.d(TAG, "handleTeamTrials:: ButtonNextWithImage.click(imageUtils=game.imageUtils)")
            return
        } else {
            // In all other cases, we want to just click on screen to speed up
            // any transition screens.
            MessageLog.d(TAG, "handleTeamTrials:: fallthrough case")
            ComponentUtils.tap(350.0, 450.0, "buttons/ok", taps=1, ignoreWaiting=true)
            return
        }
    }

    // ==================
    // DAILY RACES
    // ==================

    fun handleDailyRaces() {
        if (dailyRacesComplete) {
            // If we are out of Daily Race tickets, dialog handler will catch
            // this dialog and set the completed flag for us.
            MessageLog.i(TAG, "handleDailyRaces:: dailyRacesComplete=true")
            return
        } else if (ButtonDailyRacesDisabled.check(imageUtils=game.imageUtils)) {
            MessageLog.d(TAG, "handleDailyRaces:: ButtonDailyRacesDisabled.check(imageUtils=game.imageUtils)")
            dailyRacesComplete = true
            return
        } else if (ButtonMenuBarRaceUnselected.click(imageUtils=game.imageUtils)) {
            // Now at the Race menu.
            MessageLog.d(TAG, "handleDailyRaces:: ButtonMenuBarRaceUnselected.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonDailyRaces.click(imageUtils=game.imageUtils)) {
            // Race -> DailyRaces
            MessageLog.d(TAG, "handleDailyRaces:: ButtonDailyRaces.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonDailyRacesMoonlightSho.click(imageUtils=game.imageUtils)) {
            // TODO: Handle user specified daily race
            // Race -> DailyRaces -> MoonlightSho
            MessageLog.d(TAG, "handleDailyRaces:: ButtonDailyRacesMoonlightSho.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonRaceHardActive.click(imageUtils=game.imageUtils)) {
            // TODO: Handle user specified daily race difficulty
            // Race -> DailyRaces -> MoonlightSho -> Race (Hard)
            MessageLog.d(TAG, "handleDailyRaces:: ButtonRaceHardActive.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonConfirm.click(imageUtils=game.imageUtils)) {
            // Runner Selection
            MessageLog.d(TAG, "handleDailyRaces:: ButtonConfirm.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonNext.click(imageUtils=game.imageUtils)) {
            // Many cases where the generic NEXT button appears.
            // In all cases for daily races it is our only option so we
            // just always want to click it.
            MessageLog.d(TAG, "handleDailyRaces:: ButtonNext.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonRaceAgain.click(imageUtils=game.imageUtils)) {
            // The race again button must come before the NextWithImage button.
            // This way we always attempt to race again before exiting the menu.
            MessageLog.d(TAG, "handleDailyRaces:: ButtonRaceAgain.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonNextWithImage.click(imageUtils=game.imageUtils)) {
            // This will only appear after a race has completed and rewards received.
            MessageLog.d(TAG, "handleDailyRaces:: ButtonNextWithImage.click(imageUtils=game.imageUtils)")
            return
        } else {
            // In all other cases, we want to just click on screen to speed up
            // any transition screens.
            MessageLog.d(TAG, "handleDailyRaces:: fallthrough case")
            ComponentUtils.tap(350.0, 450.0, "buttons/ok", taps=1, ignoreWaiting=true)
            return
        }
    }

    // ==================
    // LEGEND RACES
    // ==================

    fun handleLegendRaces() {
        if (legendRacesComplete) {
            MessageLog.i(TAG, "handleLegendRaces:: legendRacesComplete=true")
            return
        } else if (ButtonRaceHardInactive.check(imageUtils=game.imageUtils)) {
            // If we are out of races, we want to collect the special missions rewards.
            // When this dialog opens, the dialog handler will process it and
            // set the legendRacesComplete flag.
            MessageLog.d(TAG, "handleLegendRaces:: ButtonRaceHardInactive.check(imageUtils=game.imageUtils)")
            ButtonLegendRaceHomeSpecialMissions.click(game.imageUtils, tries=5)
            MessageLog.d(TAG, "handleLegendRaces:: ButtonLegendRaceHomeSpecialMissions.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonLegendRaceDisabled.check(imageUtils=game.imageUtils)) {
            MessageLog.d(TAG, "handleLegendRaces:: ButtonLegendRaceDisabled.check(imageUtils=game.imageUtils)")
            legendRacesComplete = true
            return
        } else if (ButtonMenuBarRaceUnselected.click(imageUtils=game.imageUtils)) {
            // Now at the Race menu.
            MessageLog.d(TAG, "handleLegendRaces:: ButtonMenuBarRaceUnselected.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonRaceEvents.click(imageUtils=game.imageUtils)) {
            // Race -> RaceEvents
            MessageLog.d(TAG, "handleLegendRaces:: ButtonRaceEvents.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonLegendRace.click(imageUtils=game.imageUtils)) {
            // TODO: Handle user specified daily race
            // Race -> RaceEvents -> LegendRace
            MessageLog.d(TAG, "handleLegendRaces:: ButtonLegendRace.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonRaceHardActive.click(imageUtils=game.imageUtils)) {
            // TODO: Handle user specified daily race difficulty
            // Race -> RaceEvents -> LegendRace -> Race (Hard)
            MessageLog.d(TAG, "handleLegendRaces:: ButtonRaceHardActive.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonConfirm.click(imageUtils=game.imageUtils)) {
            // Runner Selection
            MessageLog.d(TAG, "handleLegendRaces:: ButtonConfirm.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonNext.click(imageUtils=game.imageUtils)) {
            // Many cases where the generic NEXT button appears.
            // In all cases for daily races it is our only option so we
            // just always want to click it.
            MessageLog.d(TAG, "handleLegendRaces:: ButtonNext.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonRaceAgain.click(imageUtils=game.imageUtils)) {
            // The race again button must come before the NextWithImage button.
            // This way we always attempt to race again before exiting the menu.
            MessageLog.d(TAG, "handleLegendRaces:: ButtonRaceAgain.click(imageUtils=game.imageUtils)")
            return
        } else if (ButtonNextWithImage.click(imageUtils=game.imageUtils)) {
            // This will only appear after a race has completed and rewards received.
            MessageLog.d(TAG, "handleLegendRaces:: ButtonNextWithImage.click(imageUtils=game.imageUtils)")
            return
        } else {
            // In all other cases, we want to just click on screen to speed up
            // any transition screens.
            MessageLog.d(TAG, "handleLegendRaces:: fallthrough case")
            ComponentUtils.tap(350.0, 450.0, "buttons/ok", taps=1, ignoreWaiting=true)
            return
        }
    }
}