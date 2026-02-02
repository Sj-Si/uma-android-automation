package com.steve1316.uma_android_automation.bot.campaigns

import android.graphics.Bitmap

import org.opencv.core.Point

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Campaign
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.automation_library.data.SharedData
import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.utils.types.BoundingBox

import com.steve1316.uma_android_automation.components.*

enum class DailyRaceName {
    MOONLIGHT_SHO,
    JUPITER_CUP;

    companion object {
        private val nameMap = entries.associateBy { it.name }
        private val ordinalMap = entries.associateBy { it.ordinal }

        fun fromName(value: String): DailyRaceName? = nameMap[value.uppercase()]
        fun fromOrdinal(ordinal: Int): DailyRaceName? = ordinalMap[ordinal]
    }
}

class DailyTasks(game: Game) : Campaign(game) {
    override val TAG: String = "[${MainActivity.loggerTag}]DailyTasks"

    private var bHasCompletedTeamTrials: Boolean = false
    private var bHasCompletedDailyRaces: Boolean = false
    private var bHasCompletedLegendRaces: Boolean = false
    private var bHasCompletedChampionsMeeting: Boolean = false
    private var bHasCompletedClubActivity: Boolean = false
    private var bHasCollectedSpecialMissions: Boolean = false
    private var bHasCollectedPresents: Boolean = false


    // TODO: Read from settingshelper.
    private val bShouldHandleDailySale: Boolean = true
    private val bShouldBuyStarPiece: Boolean = true
    private val bShouldBuyAlarmClock: Boolean = true
    private val bShouldBuyPleasingParfait: Boolean = true

    private val dailyRaceName: DailyRaceName = DailyRaceName.MOONLIGHT_SHO
    private val clubDonationShoeType: String = "medium"

    /**
     * Detects and handles any dialog popups.
     *
     * To prevent the bot moving too fast, we add a 500ms delay to the
     * exit of this function whenever we close the dialog.
     * This gives the dialog time to close since there is a very short
     * animation that plays when a dialog closes.
     *
     * @param dialog An optional dialog to evaluate. This allows chaining
     * dialog handler calls for improved performance.
     *
     * @return A pair of a boolean and a nullable DialogInterface.
     * The boolean is true when a dialog has been handled by this function.
     * The DialogInterface is the detected dialog, or NULL if no dialogs were found.
     */
    override fun handleDialogs(dialog: DialogInterface?): Pair<Boolean, DialogInterface?> {
        val (bDialogHandled, dialog) = super.handleDialogs(dialog)
        if (bDialogHandled) {
            return Pair(bDialogHandled, dialog)
        }
        if (dialog == null) {
            return Pair(false, null)
        }

        when (dialog.name) {
            "confirm_donations" -> dialog.ok(game.imageUtils)
            "confirm_restore_rp" -> {
                dialog.close(game.imageUtils)
                game.wait(0.5, skipWaitingForLoading = true)
                // Return to the home screen.
                ButtonTeamTrialsRaceResultsNext.click(game.imageUtils)
                game.waitForLoading()
                ButtonMenuBarHome.click(game.imageUtils)
                game.waitForLoading()

                bHasCompletedTeamTrials = true
            }
            "daily_sale" -> {
                // TODO: Handle daily sales.
                if (bShouldHandleDailySale) {
                    dialog.ok(game.imageUtils)
                    handleDailySale()
                } else {
                    dialog.close(game.imageUtils)
                }
            }
            "donation_complete" -> {
                dialog.close(game.imageUtils)
                game.wait(0.5, skipWaitingForLoading = true)

                // Return to the home screen.
                ButtonMenuBarHome.click(game.imageUtils)
                game.waitForLoading()

                bHasCompletedClubActivity = true
            }
            "item_request" -> {
                val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
                val shoes: List<ComponentInterface> = listOf(
                    ButtonShoesSprint,
                    ButtonShoesMile,
                    ButtonShoesMedium,
                    ButtonShoesLong,
                    ButtonShoesDirt,
                )

                if (shoes.all { it.check(game.imageUtils, sourceBitmap = bitmap) }) {
                    when (clubDonationShoeType) {
                        "sprint" -> ButtonShoesSprint.click(game.imageUtils, sourceBitmap = bitmap)
                        "mile" -> ButtonShoesMile.click(game.imageUtils, sourceBitmap = bitmap)
                        "medium" -> ButtonShoesMedium.click(game.imageUtils, sourceBitmap = bitmap)
                        "long" -> ButtonShoesLong.click(game.imageUtils, sourceBitmap = bitmap)
                        "dirt" -> ButtonShoesDirt.click(game.imageUtils, sourceBitmap = bitmap)
                        else -> {
                            MessageLog.e(TAG, "[CLUB] Invalid donation shoe type: $clubDonationShoeType")
                            throw InterruptedException("[CLUB] Invalid donation shoe type: $clubDonationShoeType")
                        }
                    }
                }
                dialog.ok(game.imageUtils)
            }
            "item_request_error" -> {
                if (ButtonHome.check(game.imageUtils)) {
                    dialog.close(game.imageUtils)
                    game.waitForLoading()
                    return Pair(true, dialog)
                }

                dialog.close(game.imageUtils)
                game.wait(0.5)
            }
            "items_selected" -> {
                // TODO: Add option for selecting parfait.
                dialog.ok(game.imageUtils)
            }
            "presents" -> {
                if (!ButtonCollect.check(game.imageUtils, tries = 5)) {
                    dialog.close(game.imageUtils)
                    game.wait(0.5, skipWaitingForLoading = true)
                    bHasCollectedPresents = true
                } else {
                    dialog.ok(game.imageUtils)
                    game.wait(0.5)
                }
            }
            "purchase_daily_race_ticket" -> {
                dialog.close(game.imageUtils)

                // Return to the home screen.
                ButtonMenuBarHome.click(game.imageUtils)
                game.waitForLoading()

                bHasCompletedDailyRaces = true
            }
            "race_details" -> dialog.ok(game.imageUtils)
            "race_results" -> dialog.ok(game.imageUtils)
            "rewards_collected" -> dialog.close(game.imageUtils)
            "special_missions" -> dialog.ok(game.imageUtils)
            else -> {
                return Pair(false, dialog)
            }
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return Pair(true, dialog)
    }

    private fun handleDailySale(): Boolean {
        val buttonBitmap: Bitmap? = ButtonShopExchange.template.getBitmap(game.imageUtils)
        if (buttonBitmap == null) {
            MessageLog.e(TAG, "Failed to load bitmap for ButtonShopExchange.")
            return false
        }

        val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
        val locs: ArrayList<Point> = ButtonShopExchange.findAll(
            game.imageUtils,
            sourceBitmap = bitmap,
        )

        if (locs.isEmpty()) {
            return false
        }

        for (loc in locs) {
            // Expand Y by half button size in both directions.
            val bbox = BoundingBox(
                x = 0,
                y = (loc.y - buttonBitmap.height).toInt(),
                w = SharedData.displayWidth,
                h = buttonBitmap.height * 2,
            )
            when {
                LabelShopStarPiece.check(game.imageUtils, sourceBitmap = bitmap, region = bbox.toIntArray()) -> {
                    if (bShouldBuyStarPiece) {
                        game.tap(loc.x, loc.y, ButtonShopExchange.template.path)
                    }
                }
                LabelShopAlarmClock.check(game.imageUtils, sourceBitmap = bitmap, region = bbox.toIntArray()) -> {
                    if (bShouldBuyAlarmClock) {
                        game.tap(loc.x, loc.y, ButtonShopExchange.template.path)
                    }
                }
                LabelShopPleasingParfait.check(game.imageUtils, sourceBitmap = bitmap, region = bbox.toIntArray()) -> {
                    if (bShouldBuyPleasingParfait) {
                        game.tap(loc.x, loc.y, ButtonShopExchange.template.path)
                    }
                }
                else -> continue
            }
        }

        // Always return to previous location after handling daily sale.
        return ButtonBack.click(game.imageUtils, sourceBitmap = bitmap)
    }

    private fun goToHomeMenu(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        return ButtonMenuBarHome.click(game.imageUtils, sourceBitmap = bitmap)
    }

    private fun goToRaceMenu(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        return ButtonMenuBarRace.click(game.imageUtils, sourceBitmap = bitmap)
    }

    private fun goToRaceEventsMenu(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        if (!goToRaceMenu(bitmap)) {
            return false
        }
        return ButtonRaceEvents.click(game.imageUtils, sourceBitmap = bitmap)
    }

    private fun checkMenuBar(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        return ButtonMenuBarHome.check(game.imageUtils, sourceBitmap = bitmap)
    }

    private fun checkRaceMenu(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        return LabelRaceMenuBanner.check(game.imageUtils, sourceBitmap = bitmap)
    }

    private fun checkTeamTrialsMenu(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        return ButtonTeamRace.check(game.imageUtils, sourceBitmap = bitmap)
    }

    private fun checkScreen(bitmap: Bitmap? = null) {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()


    }

    private fun selectTeamTrialsOpponent(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        // Always select the extra rewards option if it is available.
        if (LabelTeamTrialsExtraRewardWithEveryWin.click(
            game.imageUtils,
            sourceBitmap = bitmap,
            tries = 10,
        )) {
            return true
        }
        // Otherwise always select the hardest opponent.
        val locs: ArrayList<Point> = IconTeamTrialsOpponentSelectionLaurelRight.findAll(
            game.imageUtils,
            sourceBitmap = bitmap,
        )

        if (locs.isEmpty()) {
            return false
        }

        game.tap(
            locs.first().x,
            locs.first().y,
            IconTeamTrialsOpponentSelectionLaurelRight.template.path,
        )

        game.waitForLoading()

        return true
    }

    private fun handleTeamTrials(): Boolean {
        if (!goToRaceMenu()) {
            return false
        }

        // We use this as a means of exiting the loop if it runs too long.
        val maxTimeMs = 180000 // 3 min (180 seconds)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                bHasCompletedTeamTrials -> {
                    ButtonTeamTrialsRaceResultsNext.click(game.imageUtils)
                    game.waitForLoading()
                    ButtonMenuBarHome.click(game.imageUtils)
                    game.waitForLoading()
                    return true
                }
                handleDialogs().first -> {}
                ButtonTeamTrials.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[TEAM_TRIALS] Navigated to Team Trials menu.")
                }
                ButtonTeamRace.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[TEAM_TRIALS] Navigated to Team Trials Team Race menu.")
                }
                ButtonTeamTrialsQuickModeOff.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[TEAM_TRIALS] Enabled Team Trials Quick Mode.")
                }
                ButtonTeamTrialsSeeAllRaceResults.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[TEAM_TRIALS] Clicked \"See All Race Results\" button.")
                    game.wait(0.5, skipWaitingForLoading = true)
                    ButtonSkip.click(game.imageUtils)
                }
                LabelTeamTrialsSelectOpponent.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    if (!selectTeamTrialsOpponent(bitmap)) {
                        MessageLog.w(TAG, "[TEAM_TRIALS] Failed to select opponent.")
                        continue
                    }
                    MessageLog.d(TAG, "[TEAM_TRIALS] Selected opponent.")
                }
                ButtonSkip.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[TEAM_TRIALS] Clicked Skip button.")
                }
                ButtonNext.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[TEAM_TRIALS] Clicked Next button.")
                }
                ButtonRaceAgain.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[TEAM_TRIALS] Clicked Race Again button.")
                }
                ButtonNextWithImage.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[TEAM_TRIALS] Clicked Next race button.")
                }
                else -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }

        return false
    }

    private fun selectDailyRace(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val dailyRaceSelectionButton: ComponentInterface = when (dailyRaceName) {
            DailyRaceName.MOONLIGHT_SHO -> ButtonDailyRacesMoonlightShoRaceSelection
            DailyRaceName.JUPITER_CUP -> ButtonDailyRacesJupiterCupRaceSelection
            else -> ButtonDailyRacesMoonlightShoRaceSelection
        }

        // Always select the hardest race.
        val locs: ArrayList<Point> = dailyRaceSelectionButton.findAll(game.imageUtils, sourceBitmap = bitmap)
        if (locs.isEmpty()) {
            return false
        }

        game.tap(locs.first().x, locs.first().y, dailyRaceSelectionButton.template.path)

        // Check if we're out of ticket purchases. This also means we've
        // done all our daily races.
        if (LabelYouHaveReachedTheDailyTicketPurchaseLimit.check(game.imageUtils, tries = 10)) {
            bHasCompletedDailyRaces = true
            return true
        }

        game.waitForLoading()
        return true
    }

    private fun handleDailyRaces(): Boolean {
        if (!goToRaceMenu()) {
            return false
        }

        val dailyRaceButton: ComponentInterface = when (dailyRaceName) {
            DailyRaceName.MOONLIGHT_SHO -> ButtonDailyRacesMoonlightSho
            DailyRaceName.JUPITER_CUP -> ButtonDailyRacesJupiterCup
            else -> ButtonDailyRacesMoonlightSho
        }

        val maxTimeMs = 180000 // 3 min (180 seconds)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                bHasCompletedDailyRaces -> {
                    ButtonMenuBarHome.click(game.imageUtils)
                    game.waitForLoading()
                    return true
                }
                handleDialogs().first -> {}
                ButtonDailyRacesLocked.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_RACES] Daily Races are locked.")
                    bHasCompletedDailyRaces = true
                    return true
                }
                ButtonDailyRaces.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_RACES] Navigated to Daily Races menu.")
                }
                dailyRaceButton.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_RACES] Selected daily race type: $dailyRaceName.")
                }
                selectDailyRace(bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_RACES] Selected daily race: $dailyRaceName.")
                }
                ButtonConfirm.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_RACES] Confirmed runner selection.")
                }
                ButtonNext.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_RACES] Clicked Next button.")
                }
                ButtonRaceAgain.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_RACES] Clicked Race Again button.")
                }
                ButtonNextWithImage.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_RACES] Clicked Next race button.")
                }
                else -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }
        return false
    }

    private fun selectLegendRace(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        // Always select the hardest race.
        val locs: ArrayList<Point> = IconExtraRacePill.findAll(game.imageUtils, sourceBitmap = bitmap)
        if (locs.isEmpty()) {
            return false
        }

        game.tap(locs.first().x, locs.first().y, IconExtraRacePill.template.path)

        game.waitForLoading()

        return true
    }

    private fun handleLegendRaces(): Boolean {
        if (!goToRaceEventsMenu()) {
            return false
        }

        var numCompleted: Int = 0

        val maxTimeMs = 180000 // 3 min (180 seconds)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                bHasCompletedLegendRaces -> return true
                numCompleted >= 3 -> bHasCompletedLegendRaces = true
                handleDialogs().first -> {}
                ButtonLegendRaceLocked.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[LEGEND_RACES] Legend Races are locked.")
                    bHasCompletedLegendRaces = true
                    return true
                }
                ButtonLegendRace.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[LEGEND_RACES] Navigated to Legend Race menu.")
                }
                selectLegendRace(bitmap) -> {
                    MessageLog.d(TAG, "[LEGEND_RACES] Selected race.")
                }
                ButtonConfirm.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[LEGEND_RACES] Confirmed runner selection.")
                }
                ButtonNext.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[LEGEND_RACES] Clicked Next button.")
                }
                ButtonRaceAgain.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[LEGEND_RACES] Clicked Race Again button.")
                }
                ButtonNextWithImage.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[LEGEND_RACES] Clicked Next race button.")
                    numCompleted++
                }
                ButtonLegendRaceSpecialMissions.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[LEGEND_RACES] Clicked Special Missions button.")
                }
                else -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }
        return false
    }

    private fun handleChampionsMeeting(): Boolean {
        if (!goToRaceEventsMenu()) {
            return false
        }

        val maxTimeMs = 600000 // 10 min (600 seconds)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                bHasCompletedChampionsMeeting -> return true
                handleDialogs().first -> {}
                ButtonChampionsMeetingLocked.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[CHAMPIONS_MEETING] Champions Meeting is locked.")
                    bHasCompletedChampionsMeeting = true
                    return true
                }
                /*
                ButtonChampionsMeeting.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[CHAMPIONS_MEETING] Navigated to Champions Meeting menu.")
                }
                */
                else -> {
                    MessageLog.e(TAG, "[CHAMPIONS_MEETING] NOT IMPLEMENTED.")
                    bHasCompletedChampionsMeeting = true
                    return true
                }
            }
        }
        return false
    }

    private fun handleClub(): Boolean {
        if (!goToHomeMenu()) {
            return false
        }

        val maxTimeMs = 180000 // 3 min (180 seconds)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                bHasCompletedClubActivity -> {
                    ButtonMenuBarHome.click(game.imageUtils)
                    game.waitForLoading()
                    return true
                }
                handleDialogs().first -> {}
                ButtonClub.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.e(TAG, "[CLUB] Navigating to club.")
                }
                ButtonClubItemRequest.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.e(TAG, "[CLUB] Processing existing item request.")
                }
                ButtonClubViewRequests.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.e(TAG, "[CLUB] Handling club member requests.")
                }
                // If there are no item requests, then we have nothing left to do.
                ButtonClubItemRequest.check(game.imageUtils, sourceBitmap = bitmap) &&
                !ButtonClubViewRequests.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.e(TAG, "[CLUB] No available item requests.")
                    bHasCompletedClubActivity = true
                }
            }
        }

        return false
    }

    private fun handleSpecialMissions(): Boolean {
        if (!goToHomeMenu()) {
            return false
        }

        game.wait(0.5)

        ButtonHomeSpecialMissions.click(game.imageUtils)
        MessageLog.d(TAG, "[DAILY_TASKS] Collecting special missions rewards...")
        game.wait(0.5)

        val tabs: List<ComponentInterface> = listOf(
            ButtonSpecialMissionsTabDaily,
            ButtonSpecialMissionsTabMain,
            ButtonSpecialMissionsTabTitles,
            ButtonSpecialMissionsTabSpecial,
        )

        for (tab in tabs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            tab.click(game.imageUtils, sourceBitmap = bitmap)
            game.wait(0.5, skipWaitingForLoading = true)
            if (!ButtonCollectAll.click(game.imageUtils, sourceBitmap = bitmap)) {
                MessageLog.e(TAG, "Failed to click collect for tab ${tab::class.simpleName}")
            }
            game.wait(0.5, skipWaitingForLoading = true)
            handleDialogs()
        }

        bHasCollectedSpecialMissions = true

        return true
    }

    private fun handlePresents(): Boolean {
        if (!goToHomeMenu()) {
            return false
        }

        val maxTimeMs = 180000 // 3 min (180 seconds)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                bHasCollectedPresents -> {
                    goToHomeMenu()
                    return true
                }
                handleDialogs().first -> {}
                ButtonHomePresents.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.e(TAG, "[PRESENTS] Opening presents dialog...")
                }
            }
        }

        return false
    }
	
	/**
	 * Handles the Unity Cup race event.
	 */
	override fun start() {
		MessageLog.i(TAG, "[DAILY_TASKS] Starting process for handling the Daily Tasks.")

        // We use this as a means of exiting the loop if it runs too long.
        val maxTimeMs = 600000 // 10 min (600 seconds)
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                handleDialogs().first -> {}
                !bHasCompletedTeamTrials -> handleTeamTrials()
                !bHasCompletedDailyRaces -> handleDailyRaces()
                !bHasCompletedLegendRaces -> handleLegendRaces()
                !bHasCompletedChampionsMeeting -> handleChampionsMeeting()
                !bHasCompletedClubActivity -> handleClub()
                !bHasCollectedSpecialMissions -> handleSpecialMissions()
                !bHasCollectedPresents -> handlePresents()
                ButtonMenuBarHome.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_TASKS] Navigated to home.")
                }
                
                ButtonHomePresents.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_TASKS] Collecting presents...")
                }
                // Tap on the screen to skip past any intermediate screens.
                else -> game.tap(350.0, 750.0, "ok", taps = 3)
            }
        }

        MessageLog.e(TAG, "[DAILY_TASKS] Routine timed out.")
	}
}
