package com.steve1316.uma_android_automation.bot.campaigns

import android.graphics.Bitmap

import org.opencv.core.Point

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Campaign
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.utils.ScrollList

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

enum class ShoeType {
    SPRINT,
    MILE,
    MEDIUM,
    LONG,
    DIRT;

    companion object {
        private val nameMap = entries.associateBy { it.name }
        private val ordinalMap = entries.associateBy { it.ordinal }

        fun fromName(value: String): ShoeType? = nameMap[value.uppercase()]
        fun fromOrdinal(ordinal: Int): ShoeType? = ordinalMap[ordinal]
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
    private var bHasCollectedEventMissions: Boolean = false
    private var bHasCollectedPresents: Boolean = false
    private var bHasHandledDailySale: Boolean = false

    // TODO: Read from settingshelper.
    private val bShouldHandleDailySale: Boolean = true
    private val bShouldBuyStarPiece: Boolean = true
    private val bShouldBuyAlarmClock: Boolean = true
    private val bShouldBuyPleasingParfait: Boolean = true

    private val dailyRaceName: DailyRaceName = DailyRaceName.MOONLIGHT_SHO
    private val clubDonationShoeTypeString: String = "medium"
    private val clubDonationShoeType: ShoeType = ShoeType.fromName(clubDonationShoeTypeString) ?: ShoeType.MEDIUM

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
            "confirm_exchange" -> dialog.ok(game.imageUtils)
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
                    game.wait(0.5)
                    handleDailySale()
                } else {
                    dialog.close(game.imageUtils)
                }
            }
            "date_changed" -> {
                dialog.close(game.imageUtils)
                handleTitleMenu()
            }
            "donation_complete" -> {
                dialog.close(game.imageUtils)
                game.wait(0.5, skipWaitingForLoading = true)

                // Return to the home screen.
                ButtonMenuBarHome.click(game.imageUtils)
                game.waitForLoading()

                bHasCompletedClubActivity = true
            }
            "end_sale_confirmation" -> dialog.ok(game.imageUtils)
            // We want to handle this dialog elsewhere. So we don't do anything
            // and just let the dialog get returned to the calling function
            // for them to handle it.
            "event_exclusive_missions" -> {}
            "exchange_complete" -> dialog.close(game.imageUtils)
            "item_request" -> {
                val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
                val shoeButtons: Map<ShoeType, ComponentInterface> = mapOf(
                    ShoeType.SPRINT to ButtonShoesSprint,
                    ShoeType.MILE to ButtonShoesMile,
                    ShoeType.MEDIUM to ButtonShoesMedium,
                    ShoeType.LONG to ButtonShoesLong,
                    ShoeType.DIRT to ButtonShoesDirt,
                )

                if (shoeButtons.values.all { it.check(game.imageUtils, sourceBitmap = bitmap) }) {
                    val button: ComponentInterface = shoeButtons[clubDonationShoeType]!!
                    button.click(game.imageUtils, sourceBitmap = bitmap)
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
                // TODO: Add option for selecting parfait when we have bonus rewards.
                dialog.ok(game.imageUtils)
            }
            "notices" -> dialog.close(game.imageUtils)
            "open_soon" -> {
                dialog.close(game.imageUtils)
                bHasHandledDailySale = true
            }
            "presents" -> {
                if (bHasCollectedPresents || !ButtonCollect.check(game.imageUtils, tries = 5)) {
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
            "story_unlocked" -> dialog.close(game.imageUtils)
            else -> {
                return Pair(false, dialog)
            }
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return Pair(true, dialog)
    }

    private fun goToDailySale(): Boolean {
        if (!goToHomeMenu()) {
            return false
        }

        val maxTimeMs = 10000 // 10 seconds
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                bHasHandledDailySale -> {
                    goToHomeMenu()
                    return true
                }
                ButtonHomeShopDaily.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_SALE] Entering shop...")
                }
                ButtonShopDailySales.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_SALE] Entering daily sale...")
                }
            }
        }

        MessageLog.e(TAG, "[DAILY_SALE] goToDailySale timed out.")
        return false
    }

    fun handleDailySale(bShouldReturnToHome: Boolean = false): Boolean {
        if (!ButtonShopEndSale.check(game.imageUtils, tries = 5)) {
            if (!goToDailySale()) {
                MessageLog.e(TAG, "[DAILY_SALE] Failed to go to daily sale.")
                return false
            }
        }

        val buttonBitmap: Bitmap? = ButtonShopExchange.template.getBitmap(game.imageUtils)
        if (buttonBitmap == null) {
            MessageLog.e(TAG, "Failed to load bitmap for ButtonShopExchange.")
            return false
        }

        val bitmap: Bitmap = game.imageUtils.getSourceBitmap()

        val scrollList: ScrollList? = ScrollList.create(
            game,
            entryHeight = (SharedData.displayHeight * 0.0979).toInt(),
            bitmap = bitmap,
        )

        if (scrollList == null) {
            MessageLog.e(TAG, "[DAILY_SALE] Failed to detect sale list.")
            return false
        }

        var prevNames: Set<String> = setOf()

        var numHandled: Int = 0

        scrollList.process(ButtonShopExchange) { _, _, loc, bitmap ->
            when {
                LabelShopStarPiece.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    if (bShouldBuyStarPiece) {
                        game.tap(loc.x, loc.y, ButtonShopExchange.template.path)
                    }
                }
                LabelShopAlarmClock.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    if (bShouldBuyAlarmClock) {
                        game.tap(loc.x, loc.y, ButtonShopExchange.template.path)
                    }
                }
                LabelShopPleasingParfait.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    if (bShouldBuyPleasingParfait) {
                        game.tap(loc.x, loc.y, ButtonShopExchange.template.path)
                    }
                }
            }
            game.wait(0.5, skipWaitingForLoading = true)
            if (handleDialogs().first) {
                numHandled++
            }
            // Need to close the second dialog opened when confirming the exchange.
            handleDialogs()

            // Return true if we bought everything to stop the scroll list loop.
            numHandled >= 4
        }

        bHasHandledDailySale = true

        ButtonShopEndSale.click(game.imageUtils)
        handleDialogs()

        // If specified, we return to the home menu.
        if (bShouldReturnToHome) {
            goToHomeMenu()
            return true
        }

        // Otherwise, return to previous location after handling daily sale.
        return ButtonBack.click(game.imageUtils, sourceBitmap = bitmap)
    }

    private fun goToHomeMenu(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        val res: Boolean = ButtonMenuBarHome.click(game.imageUtils, sourceBitmap = bitmap)
        game.waitForLoading()
        return res
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

        if (ButtonTeamTrialsTallying.check(game.imageUtils)) {
            MessageLog.i(TAG, "[TEAM_TRIALS] Tallying in progress. Cannot race.")
            return true
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
                    goToHomeMenu()
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

        MessageLog.e(TAG, "[TEAM_TRIALS] handleTeamTrials timed out.")
        return false
    }

    private fun selectDailyRace(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val dailyRaceSelectionButton: ComponentInterface = when (dailyRaceName) {
            DailyRaceName.MOONLIGHT_SHO -> ButtonDailyRacesMoonlightShoRaceSelection
            DailyRaceName.JUPITER_CUP -> ButtonDailyRacesJupiterCupRaceSelection
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
        }

        val maxTimeMs = 180000 // 3 min (180 seconds)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                bHasCompletedDailyRaces -> {
                    goToHomeMenu()
                    return true
                }
                handleDialogs().first -> {}
                ButtonDailyRacesDoneForToday.check(game.imageUtils) -> {
                    MessageLog.i(TAG, "[DAILY_RACES] Daily races are done for today. Cannot race.")
                    bHasCompletedDailyRaces = true
                }
                ButtonDailyRacesLocked.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[DAILY_RACES] Daily Races are locked. Cannot race.")
                    bHasCompletedDailyRaces = true
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

        MessageLog.e(TAG, "[DAILY_RACES] handleDailyRaces timed out.")
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
                bHasCompletedLegendRaces -> {
                    game.waitForLoading()
                    goToHomeMenu()
                    return true
                }
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

        MessageLog.e(TAG, "[LEGEND_RACES] handleLegendRaces timed out.")
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
                bHasCompletedChampionsMeeting -> {
                    goToHomeMenu()
                    return true
                }
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

        MessageLog.e(TAG, "[CHAMPIONS_MEETING] handleChampionsMeeting timed out.")
        return false
    }

    private fun handleClub(): Boolean {
        if (!goToHomeMenu()) {
            MessageLog.e(TAG, "handleClub: Failed to goToHomeMenu.")
            return false
        }

        val maxTimeMs = 30000 // 30 seconds
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                bHasCompletedClubActivity -> {
                    goToHomeMenu()
                    return true
                }
                handleDialogs().first -> {}
                ButtonClub.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.e(TAG, "[CLUB] Navigating to club.")
                }
                ButtonClubLocked.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.d(TAG, "[CLUB] Club is locked. Cannot perform tasks.")
                    bHasCompletedClubActivity = true
                }
                ButtonClubItemRequest.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.e(TAG, "[CLUB] Processing existing item request.")
                }
                ButtonClubViewRequests.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.e(TAG, "[CLUB] Handling club member requests.")
                }
                // If there are no item requests, then we have nothing left to do.
                ButtonClubEmoji.check(game.imageUtils, sourceBitmap = bitmap) && 
                !ButtonClubViewRequests.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.e(TAG, "[CLUB] No available item requests.")
                    bHasCompletedClubActivity = true
                }
            }
        }

        MessageLog.e(TAG, "[CLUB] handleClub timed out.")
        return false
    }

    private fun handleEventMissions(): Boolean {
        // If there isn't an event going on, then this button won't exist.
        // Thus we don't treat this as a failure.
        if (!ButtonEventMissions.click(game.imageUtils, tries = 5)) {
            MessageLog.w(TAG, "[EVENT_MISSIONS] Event Missions button not found.")
            bHasCollectedEventMissions = true
            return true
        }

        game.wait(0.5)
        val dialog: DialogInterface? = handleDialogs().second
        if (dialog == null || dialog.name != "event_exclusive_missions") {
            MessageLog.e(TAG, "[EVENT_MISSIONS] Event Exclusive Missions dialog wasn't detected.")
            return false
        }

        if (!ButtonEventExclusiveMissionsStoryEvent.click(game.imageUtils)) {
            MessageLog.e(TAG, "[EVENT_MISSIONS] Failed to click Story Event button.")
            return false
        }
        game.wait(1.0)
        game.waitForLoading()

        val tabs: List<ComponentInterface> = listOf(
            ButtonSpecialMissionsTabDaily,
            ButtonSpecialMissionsTabTitles,
            ButtonEventMissionsTabLimitedTime,
        )

        for (tab in tabs) {
            tab.click(game.imageUtils)
            game.wait(0.5, skipWaitingForLoading = true)
            val bm = game.imageUtils.getSourceBitmap()
            game.imageUtils.saveBitmap(bm, "BLAH")
            if (!ButtonCollectAll.click(game.imageUtils, tries = 5)) {
                MessageLog.e(TAG, "[EVENT_MISSIONS] Failed to click collect for tab ${tab::class.simpleName}")
            }
            game.wait(0.5, skipWaitingForLoading = true)
            handleDialogs()
        }

        bHasCollectedEventMissions = true
        return true
    }

    private fun handleSpecialMissions(): Boolean {
        if (!goToHomeMenu()) {
            return false
        }

        game.wait(0.5)

        ButtonHomeSpecialMissions.click(game.imageUtils)
        MessageLog.d(TAG, "[SPECIAL_MISSIONS] Collecting special missions rewards...")
        game.wait(0.5)

        val tabs: List<ComponentInterface> = listOf(
            ButtonSpecialMissionsTabDaily,
            ButtonSpecialMissionsTabMain,
            ButtonSpecialMissionsTabTitles,
            ButtonSpecialMissionsTabSpecial,
        )

        for (tab in tabs) {
            tab.click(game.imageUtils)
            game.wait(0.5, skipWaitingForLoading = true)
            if (!ButtonCollectAll.click(game.imageUtils, tries = 5)) {
                MessageLog.e(TAG, "[SPECIAL_MISSIONS] Failed to click collect for tab ${tab::class.simpleName}")
            }
            game.wait(0.5, skipWaitingForLoading = true)
            handleDialogs()
        }

        // Now handle event missions.
        if (!handleEventMissions()) {
            MessageLog.e(TAG, "[SPECIAL_MISSIONS] Failed to handle event missions.")
            return false
        }

        bHasCollectedSpecialMissions = true
        return true
    }

    private fun handlePresents(): Boolean {
        if (!goToHomeMenu()) {
            return false
        }

        val maxTimeMs = 10000 // 10 seconds
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                bHasCollectedPresents -> {
                    goToHomeMenu()
                    return true
                }
                handleDialogs().first -> {
                    // Need to close the extra dialog that pops up.
                    handleDialogs()
                    // Now close the original dialog.
                    handleDialogs()
                }
                ButtonHomePresents.click(game.imageUtils, sourceBitmap = bitmap) -> {
                    MessageLog.e(TAG, "[PRESENTS] Opening presents dialog...")
                    game.wait(0.5, skipWaitingForLoading = true)
                }
            }
        }

        MessageLog.e(TAG, "[PRESENTS] handlePresents timed out.")
        return false
    }

    private fun handleTitleMenu(): Boolean {
        val maxTimeMs = 60000 // 60 seconds
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                handleDialogs().first -> {}
                ButtonMenuBarHome.check(game.imageUtils, sourceBitmap = bitmap) -> return true
                ButtonTitleScreen.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                ButtonSkip.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                else -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }

        MessageLog.e(TAG, "[DAILY_TASKS] handleTitleMenu timed out.")
        return false
    }
	
	/**
	 * Handles the Unity Cup race event.
	 */
	override fun start() {
		MessageLog.i(TAG, "[DAILY_TASKS] Starting process for handling the Daily Tasks.")

        val maxTimeMs = 600000 // 10 min (600 seconds)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            when {
                handleDialogs().first -> {}
                !bHasCompletedTeamTrials -> handleTeamTrials()
                !bHasCompletedDailyRaces -> handleDailyRaces()
                !bHasCompletedLegendRaces -> handleLegendRaces()
                !bHasCompletedChampionsMeeting -> handleChampionsMeeting()
                !bHasCompletedClubActivity -> handleClub()
                !bHasCollectedSpecialMissions -> handleSpecialMissions()
                !bHasCollectedEventMissions -> handleSpecialMissions()
                !bHasCollectedPresents -> handlePresents()
                !bHasHandledDailySale -> handleDailySale(bShouldReturnToHome = true)
                else -> return
            }
        }
	}
}
