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

// TODO: Add param for a common dialog handler for dialogs that can
// appear at any time (i.e. date_changed)
open class Routine(protected val game: Game) {
    protected open val TAG: String = "[${MainActivity.loggerTag}]Routine"

    protected var bIsComplete: Boolean = false

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
    open fun handleDialogs(dialog: DialogInterface? = null): Pair<Boolean, DialogInterface?> {
        val dialog: DialogInterface? = dialog ?: DialogUtils.getDialog(game.imageUtils)
        return Pair(false, dialog)
    }

    open fun waitForPages(
        pages: List<PageInterface>,
        timeoutMs: Int = 10000,
        bShouldTapWhileWaiting: Boolean = true,
    ): Boolean {
        val startTime = System.currentTimeMillis()
        var result: Boolean = false
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            if (pages.any { it.check(game.imageUtils, bitmap) }) {
                result = true
                break
            }
            if (bShouldTapWhileWaiting) {
                game.tap(350.0, 750.0, "ok", taps = 3)
            }
        }
        checkPage()
        return result
    }

    open fun waitForButton(
        button: ComponentInterface,
        timeoutMs: Int = 3000,
        bShouldTapWhileWaiting: Boolean = false,
        bShouldClickButton: Boolean = true,
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (bShouldClickButton) {
                if (button.click(game.imageUtils)) {
                    return true
                }
            } else {
                if (button.check(game.imageUtils)) {
                    return true
                }
            }
            if (bShouldTapWhileWaiting) {
                game.tap(350.0, 750.0, "ok", taps = 3)
            }
        }
        return false
    }

    open fun checkPage(bitmap: Bitmap? = null): PageInterface? {
        return null
    }

    open fun progress(bitmap: Bitmap? = null): PageInterface? {
        return null
    }

    open fun start(timeoutMs: Int = 60000 * 5): Boolean {
        val startTime = System.currentTimeMillis()
        while (!bIsComplete && System.currentTimeMillis() - startTime < timeoutMs) {
            progress()
        }
        return bIsComplete
    }
}

class DailySaleRoutine(game: Game) : Routine(game) {
    override val TAG: String = "[${MainActivity.loggerTag}]DailySaleRoutine"

    // TODO: Load from settings.
    private val bShouldBuyStarPieces: Boolean = true
    private val bShouldBuyAlarmClock: Boolean = true
    private val bShouldBuyPleasingParfait: Boolean = true

    override fun handleDialogs(dialog: DialogInterface?): Pair<Boolean, DialogInterface?> {
        val dialog: DialogInterface? = dialog ?: DialogUtils.getDialog(game.imageUtils)
        if (dialog == null) {
            return Pair(false, null)
        }

        when (dialog.name) {
            "confirm_exchange" -> dialog.ok(game.imageUtils)
            "end_sale_confirmation" -> dialog.ok(game.imageUtils)
            "exchange_complete" -> dialog.close(game.imageUtils)
            "return_to_shops" -> dialog.close(game.imageUtils)
            else -> return Pair(false, dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return Pair(true, dialog)
    }

    override fun start(timeoutMs: Int): Boolean {
        if (!PageDailySale.check(game.imageUtils)) {
            MessageLog.e(TAG, "[DAILY_SALE] Failed to start routine. Not at Daily Sale page.")
            return false
        }

        val scrollList: ScrollList? = ScrollList.create(
            game,
            entryHeight = (SharedData.displayHeight * 0.0979).toInt(),
        )

        if (scrollList == null) {
            MessageLog.e(TAG, "[DAILY_SALE] Failed to detect sale list.")
            return false
        }

        var prevNames: Set<String> = setOf()

        var numToHandle: Int = listOf<Boolean>(
            bShouldBuyStarPieces,
            bShouldBuyStarPieces,
            bShouldBuyAlarmClock,
            bShouldBuyPleasingParfait,
        ).count { it }

        var numHandled: Int = 0

        var bSaleExpired: Boolean = false
        
        val entryComponents: List<ComponentInterface> = listOf(
            ButtonShopExchange,
            ButtonShopExchangeDisabled,
        )
        scrollList.process(entryComponents) { _, _, component, loc, bitmap ->
            var bShouldBuyItem: Boolean = false
            when {
                LabelShopStarPiece.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    bShouldBuyItem = bShouldBuyStarPieces
                }
                LabelShopAlarmClock.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    bShouldBuyItem = bShouldBuyAlarmClock
                }
                LabelShopPleasingParfait.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    bShouldBuyItem = bShouldBuyPleasingParfait
                }
            }

            if (bShouldBuyItem) {
                if (component != ButtonShopExchangeDisabled) {
                    game.tap(loc.x, loc.y, ButtonShopExchange.template.path, taps = 3)

                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < 5000) {
                        val (_, dialog) = handleDialogs()
                        if (dialog != null) {
                            if (dialog.name == "return_to_shops") {
                                bSaleExpired = true
                                break
                            } else if (dialog.name == "exchange_complete") {
                                break
                            }
                        }
                    }
                }

                numHandled++
            }

            // Return true if we bought everything to stop the scroll list loop.
            numHandled >= numToHandle
        }

        if (!bSaleExpired) {
            ButtonShopEndSale.click(game.imageUtils, tries = 10)
            handleDialogs()
        }
        ButtonBack.click(game.imageUtils)
        return true
    }
}

class TeamTrialsRoutine(game: Game) : Routine(game) {
    override val TAG: String = "[${MainActivity.loggerTag}]TeamTrialsRoutine"

    // TODO: Load from settings.
    private val bShouldHandleDailySale: Boolean = false
    private val bShouldUseParfaitOnExtraRewards: Boolean = true

    private var bIsExtraRewards: Boolean = false

    fun handleSelectOpponent(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        // Always select the extra rewards option if it is available.
        if (LabelTeamTrialsExtraRewardsOpponent.click(
            game.imageUtils,
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
        return true
    }

    override fun handleDialogs(dialog: DialogInterface?): Pair<Boolean, DialogInterface?> {
        val dialog: DialogInterface? = dialog ?: DialogUtils.getDialog(game.imageUtils)
        if (dialog == null) {
            return Pair(false, null)
        }

        when (dialog.name) {
            "daily_sale" -> {
                // TODO: Handle daily sales.
                if (bShouldHandleDailySale) {
                    dialog.ok(game.imageUtils)
                    game.wait(0.5)
                    game.waitForLoading()
                    val dailySaleRoutine = DailySaleRoutine(game)
                    dailySaleRoutine.start()
                } else {
                    dialog.close(game.imageUtils)
                }
            }
            "items_selected" -> {
                // TODO: Add option for selecting parfait when we have bonus rewards.
                if (bIsExtraRewards && bShouldUseParfaitOnExtraRewards) {
                    IconPleasingParfait.click(game.imageUtils)
                }
                dialog.ok(game.imageUtils)
                // Reset this flag every time we handle this dialog.
                bIsExtraRewards = false
            }
            "confirm_restore_rp" -> {
                dialog.close(game.imageUtils)
                game.wait(0.5, skipWaitingForLoading = true)
                bIsComplete = true
            }
            else -> return Pair(false, dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return Pair(true, dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        return listOf<PageInterface>(
            PageTeamTrialsHome,
            PageTeamTrialsSelectOpponent,
            PageTeamTrialsPreRace,
            PageTeamTrialsRaceQuickModeOff,
            PageTeamTrialsRaceQuickModeOn,
            PageTeamTrialsRaceFinished,
            PageTeamTrialsPreRaceResults,
            PageTeamTrialsRaceResults,
        ).find { it.check(game.imageUtils, bitmap) }
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val currentPage: PageInterface? = checkPage(bitmap)
        when (currentPage) {
            PageTeamTrialsHome -> {
                PageTeamTrialsHome.next(game.imageUtils, bitmap)
            }
            PageTeamTrialsSelectOpponent -> {
                if (!handleSelectOpponent()) {
                    MessageLog.e(TAG, "progress: Failed to select opponent.")
                    return checkPage()
                }
            }
            PageTeamTrialsPreRace -> {
                bIsExtraRewards = LabelTeamTrialsExtraRewardsOpponent.check(game.imageUtils, tries=10)
                PageTeamTrialsPreRace.next(game.imageUtils, bitmap)
            }
            PageTeamTrialsRaceQuickModeOff -> {
                ButtonTeamTrialsQuickModeOff.click(game.imageUtils, sourceBitmap = bitmap)
            }
            PageTeamTrialsRaceQuickModeOn -> {
                PageTeamTrialsRaceQuickModeOn.next(game.imageUtils, bitmap)
            }
            PageTeamTrialsRaceFinished -> {
                PageTeamTrialsRaceFinished.next(game.imageUtils, bitmap)
            }
            PageTeamTrialsPreRaceResults -> {
                PageTeamTrialsPreRaceResults.next(game.imageUtils, bitmap)
            }
            PageTeamTrialsRaceResults -> {
                if (bIsComplete) {
                    PageTeamTrialsRaceResults.next(game.imageUtils)
                } else {
                    ButtonRaceAgain.click(game.imageUtils)
                }
            }
            else -> {
                if (!handleDialogs().first &&
                    !ButtonSkip.click(game.imageUtils) &&
                    !ButtonNext.click(game.imageUtils)
                ) {
                    game.tap(350.0, 750.0, "ok", taps = 3)
                }
            }
        }

        if (bIsComplete) {
            PageTeamTrialsRaceResults.next(game.imageUtils)
        }

        return checkPage()
    }
}

class DailyRacesRoutine(game: Game) : Routine(game) {
    override val TAG: String = "[${MainActivity.loggerTag}]DailyRacesRoutine"

    // TODO: Load from settings.
    private val bShouldHandleDailySale: Boolean = false
    private val dailyRaceName: DailyRaceName = DailyRaceName.MOONLIGHT_SHO

    private val dailyRaceButton: ComponentInterface = when (dailyRaceName) {
        DailyRaceName.MOONLIGHT_SHO -> ButtonDailyRacesMoonlightSho
        DailyRaceName.JUPITER_CUP -> ButtonDailyRacesJupiterCup
    }

    private val dailyRaceSelectionButton: ComponentInterface = when (dailyRaceName) {
        DailyRaceName.MOONLIGHT_SHO -> ButtonDailyRacesMoonlightShoRaceSelection
        DailyRaceName.JUPITER_CUP -> ButtonDailyRacesJupiterCupRaceSelection
    }

    private fun selectDailyRace(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        // Always select the hardest available race.
        val locs: ArrayList<Point> = dailyRaceSelectionButton.findAll(game.imageUtils, sourceBitmap = bitmap)
        if (locs.isEmpty()) {
            return false
        }

        game.tap(locs.first().x, locs.first().y, dailyRaceSelectionButton.template.path)

        // Check if we're out of ticket purchases. This also means we've
        // done all our daily races.
        if (LabelYouHaveReachedTheDailyTicketPurchaseLimit.check(game.imageUtils, tries = 10)) {
            bIsComplete = true
            return true
        }

        game.waitForLoading()
        return true
    }

    override fun handleDialogs(dialog: DialogInterface?): Pair<Boolean, DialogInterface?> {
        val dialog: DialogInterface? = dialog ?: DialogUtils.getDialog(game.imageUtils)
        if (dialog == null) {
            return Pair(false, null)
        }

        when (dialog.name) {
            "daily_sale" -> {
                if (bShouldHandleDailySale) {
                    dialog.ok(game.imageUtils)
                    game.wait(0.5)
                    game.waitForLoading()
                    val dailySaleRoutine = DailySaleRoutine(game)
                    dailySaleRoutine.start()
                } else {
                    dialog.close(game.imageUtils)
                }
            }
            "items_selected" -> dialog.ok(game.imageUtils)
            "multi_race" -> dialog.ok(game.imageUtils)
            "purchase_daily_race_ticket" -> {
                dialog.close(game.imageUtils)
                bIsComplete = true
            }
            "race_details" -> {
                // Always try to enable multi-race.
                ButtonDailyRacesMultiRaceOff.click(game.imageUtils)
                dialog.ok(game.imageUtils)
            }
            "race_results" -> dialog.ok(game.imageUtils)
            else -> return Pair(false, dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return Pair(true, dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        return listOf<PageInterface>(
            PageDailyRacesRaceSelection,
            PageDailyRacesDifficultySelection,
            PageDailyRacesRunnerSelection,
            PageDailyRacesPreRacePrep,
            PageDailyRacesRacePrep,
            PageDailyRacesResultsPlacing,
            PageDailyRacesResultsRewards,
        ).find { it.check(game.imageUtils, bitmap) }
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val currentPage: PageInterface? = checkPage(bitmap)
        when (currentPage) {
            PageDailyRacesRaceSelection -> {
                dailyRaceButton.click(game.imageUtils)
            }
            PageDailyRacesDifficultySelection -> {
                selectDailyRace()
            }
            PageDailyRacesRunnerSelection -> {
                PageDailyRacesRunnerSelection.next(game.imageUtils)
            }
            PageDailyRacesPreRacePrep -> {
                PageDailyRacesPreRacePrep.next(game.imageUtils)
            }
            PageDailyRacesRacePrep -> {
                if (ButtonViewResultsLocked.check(game.imageUtils)) {
                    ButtonRaceManual.click(game.imageUtils)
                } else {
                    ButtonViewResults.click(game.imageUtils)
                }
            }
            PageDailyRacesResultsPlacing -> {
                PageDailyRacesResultsPlacing.next(game.imageUtils)
            }
            PageDailyRacesResultsRewards -> {
                if (!bIsComplete) {
                    ButtonRaceAgain.click(game.imageUtils)
                } else {
                    PageDailyRacesResultsRewards.next(game.imageUtils)
                }
            }
            else -> {
                // Catch-all for various intermediate screens.
                if (!handleDialogs().first &&
                    !ButtonSkip.click(game.imageUtils) &&
                    !ButtonNext.click(game.imageUtils) &&
                    !ButtonRaceExclamation.click(game.imageUtils)
                ) {
                    game.tap(350.0, 750.0, "ok", taps = 3)
                }
            }
        }

        return checkPage()
    }
}

class ClubActivityRoutine(game: Game) : Routine(game) {
    override val TAG: String = "[${MainActivity.loggerTag}]ClubActivityRoutine"

    // TODO: Load from settings.
    private val clubDonationShoeTypeString: String = "medium"
    private val clubDonationShoeType: ShoeType = ShoeType.fromName(clubDonationShoeTypeString) ?: ShoeType.MEDIUM

    private var bHasSelectedShoes: Boolean = false
    private var bHasRequestedItems: Boolean = false
    private var bHasDonatedItems: Boolean = false

    private val shoeButtons: Map<ShoeType, ComponentInterface> = mapOf(
        ShoeType.SPRINT to ButtonShoesSprint,
        ShoeType.MILE to ButtonShoesMile,
        ShoeType.MEDIUM to ButtonShoesMedium,
        ShoeType.LONG to ButtonShoesLong,
        ShoeType.DIRT to ButtonShoesDirt,
    )

    override fun handleDialogs(dialog: DialogInterface?): Pair<Boolean, DialogInterface?> {
        val dialog: DialogInterface? = dialog ?: DialogUtils.getDialog(game.imageUtils)
        if (dialog == null) {
            return Pair(false, null)
        }

        when (dialog.name) {
            "confirm_donations" -> dialog.ok(game.imageUtils)
            "donation_complete" -> {
                dialog.close(game.imageUtils)
                bHasDonatedItems = true
            }
            "item_request" -> {
                val bitmap: Bitmap = game.imageUtils.getSourceBitmap()

                if (shoeButtons.values.all { it.check(game.imageUtils, sourceBitmap = bitmap) }) {
                    val button: ComponentInterface = shoeButtons[clubDonationShoeType]!!
                    button.click(game.imageUtils, sourceBitmap = bitmap)
                    bHasSelectedShoes = true
                }

                if (bHasSelectedShoes && ButtonConfirm.check(game.imageUtils)) {
                    bHasRequestedItems = true
                }
                dialog.ok(game.imageUtils)
            }
            "item_request_error" -> {
                if (ButtonHome.check(game.imageUtils)) {
                    dialog.close(game.imageUtils)
                    game.waitForLoading()
                    // We want to return to the club menu if we get this error.
                    waitForButton(ButtonClub)
                    return Pair(true, dialog)
                }
                dialog.close(game.imageUtils)
            }
            else -> return Pair(false, dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return Pair(true, dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        return if (PageClubHome.check(game.imageUtils, bitmap)) PageClubHome else null
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val currentPage: PageInterface? = checkPage(bitmap)
        when (currentPage) {
            PageClubHome -> {
                if (!bHasRequestedItems) {
                    ButtonClubItemRequest.click(game.imageUtils)
                } else if (!bHasDonatedItems) {
                    ButtonClubViewRequests.click(game.imageUtils)
                } else if (bHasRequestedItems && bHasDonatedItems) {
                    bIsComplete = true
                }
            }
            else -> handleDialogs()
        }

        return checkPage()
    }
}

class SpecialMissionsRoutine(game: Game) : Routine(game) {
    override val TAG: String = "[${MainActivity.loggerTag}]SpecialMissionsRoutine"

    private val specialMissionsTabs: List<ComponentInterface> = listOf(
        ButtonSpecialMissionsTabDaily,
        ButtonSpecialMissionsTabMain,
        ButtonSpecialMissionsTabTitles,
        ButtonSpecialMissionsTabSpecial,
    )

    private val eventMissionsTabs: List<ComponentInterface> = listOf(
        ButtonSpecialMissionsTabDaily,
        ButtonSpecialMissionsTabTitles,
        ButtonEventMissionsTabLimitedTime,
    )

    private var bHasHandledSpecialMissions: Boolean = false
    private var bHasHandledEventMissions: Boolean = false

    override fun handleDialogs(dialog: DialogInterface?): Pair<Boolean, DialogInterface?> {
        val dialog: DialogInterface? = dialog ?: DialogUtils.getDialog(game.imageUtils)
        if (dialog == null) {
            return Pair(false, null)
        }

        when (dialog.name) {
            "event_exclusive_missions" -> {
                ButtonEventExclusiveMissionsStoryEvent.click(game.imageUtils)
            }
            "rewards_collected" -> dialog.close(game.imageUtils)
            "special_missions" -> dialog.ok(game.imageUtils)
            "story_unlocked" -> dialog.close(game.imageUtils)
            else -> return Pair(false, dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return Pair(true, dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        return listOf<PageInterface>(
            PageSpecialMissions,
            PageEventMissions,
        ).find { it.check(game.imageUtils, bitmap) }
    }

    fun handleSpecialMissionsTabs() {
        for (tab in specialMissionsTabs) {
            tab.click(game.imageUtils)
            game.wait(0.1, skipWaitingForLoading = true)
            ButtonCollectAll.click(game.imageUtils)
            game.wait(0.5)
            handleDialogs()
        }

        bHasHandledSpecialMissions = true
    }

    fun handleEventMissionsTabs() {
        for (tab in eventMissionsTabs) {
            tab.click(game.imageUtils)
            game.wait(0.1, skipWaitingForLoading = true)
            ButtonCollectAll.click(game.imageUtils)
            game.wait(0.5)
            handleDialogs()
        }

        bHasHandledEventMissions = true
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val currentPage: PageInterface? = checkPage(bitmap)
        when (currentPage) {
            PageSpecialMissions -> {
                if (!bHasHandledSpecialMissions) {
                    handleSpecialMissionsTabs()
                } else {
                    if (!ButtonEventMissions.click(game.imageUtils)) {
                        bHasHandledEventMissions = true
                    }
                }
            }
            PageEventMissions -> {
                handleEventMissionsTabs()
                if (bHasHandledEventMissions) {
                    ButtonBack.click(game.imageUtils)
                }
            }
            else -> handleDialogs()
        }

        bIsComplete = bHasHandledSpecialMissions && bHasHandledEventMissions
        if (bIsComplete) {
            ButtonBack.click(game.imageUtils)
        }

        return checkPage()
    }
}

class PresentsRoutine(game: Game) : Routine(game) {
    override val TAG: String = "[${MainActivity.loggerTag}]PresentsRoutine"

    override fun handleDialogs(dialog: DialogInterface?): Pair<Boolean, DialogInterface?> {
        val dialog: DialogInterface? = dialog ?: DialogUtils.getDialog(game.imageUtils)
        if (dialog == null) {
            return Pair(false, null)
        }

        when (dialog.name) {
            "presents" -> dialog.ok(game.imageUtils)
            "rewards_collected" -> {
                dialog.close(game.imageUtils)
                bIsComplete = true
            }
            else -> return Pair(false, dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return Pair(true, dialog)
    }

    override fun start(timeoutMs: Int): Boolean {
        val startTime = System.currentTimeMillis()
        while (!bIsComplete && System.currentTimeMillis() - startTime < timeoutMs) {
            handleDialogs()
        }
        return bIsComplete
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
            "date_changed" -> {
                dialog.close(game.imageUtils)
                handleTitleMenu()
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

        val entryComponents: List<ComponentInterface> = listOf(
            ButtonShopExchange,
            ButtonShopExchangeDisabled,
        )
        scrollList.process(entryComponents) { _, _, component, loc, bitmap ->
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
