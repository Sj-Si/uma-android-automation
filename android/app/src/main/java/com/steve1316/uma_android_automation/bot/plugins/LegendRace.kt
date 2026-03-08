package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap
import org.opencv.core.Point

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.SettingsHelper

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.PluginFactory
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult

import com.steve1316.uma_android_automation.components.BaseComponentInterface
import com.steve1316.uma_android_automation.components.ButtonCollectAll
import com.steve1316.uma_android_automation.components.ButtonEventMissionsTabLimitedTime
import com.steve1316.uma_android_automation.components.ButtonLegendRace
import com.steve1316.uma_android_automation.components.ButtonNext
import com.steve1316.uma_android_automation.components.ButtonRaceEvents
import com.steve1316.uma_android_automation.components.ButtonRaceExclamation
import com.steve1316.uma_android_automation.components.ButtonRaceManual
import com.steve1316.uma_android_automation.components.ButtonSkip
import com.steve1316.uma_android_automation.components.ButtonSpecialMissions
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabDaily
import com.steve1316.uma_android_automation.components.ButtonViewResults
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.IconExtraRacePill
import com.steve1316.uma_android_automation.components.IconPleasingParfait
import com.steve1316.uma_android_automation.components.MenuBar
import com.steve1316.uma_android_automation.components.PageExtraRacesPreRacePrep
import com.steve1316.uma_android_automation.components.PageExtraRacesRacePrep
import com.steve1316.uma_android_automation.components.PageExtraRacesRunnerSelection
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageLegendRaceHome

class LegendRace(
    game: Game,
    menuBar: MenuBar,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, menuBar, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]LegendRace"

    private val bShouldUseParfait: Boolean = SettingsHelper.getBooleanSetting("dailyTasks", "enableLegendRaceUseParfait")

    private val specialMissionsTabs: List<ComponentInterface> = listOf(
        ButtonSpecialMissionsTabDaily,
        ButtonEventMissionsTabLimitedTime,
    )

    private var bHasHandledSpecialMissions: Boolean = false

    private fun selectRace(bitmap: Bitmap? = null): Boolean {
        MessageLog.d(TAG, "[$name] Selecting race...")
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val pillBitmap: Bitmap = IconExtraRacePill.template.getBitmap(game.imageUtils)!!

        // Always select the hardest available race.
        val locs: List<Point> = IconExtraRacePill.findAll(
            game.imageUtils,
            sourceBitmap = bitmap,
            ignoreDisabled = true,
        ).toList().sortedBy { it.y }

        if (locs.isEmpty()) {
            bIsComplete = true
            MessageLog.w(TAG, "[$name] No more races available today. Finishing up.")
            return true
        }

        val loc: Point = locs.first()
        MessageLog.d(TAG, "[$name] Selecting race at $loc.")
        game.tap(loc.x, loc.y, IconExtraRacePill.template.path)
        game.waitForLoading()

        return true
    }

    override fun handleDialogs(dialog: DialogInterface?, args: Map<String, Any>): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog, args)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "daily_sale" -> {
                val dailySale: Plugin? = PluginFactory.create("DailySale", game, menuBar, commonDialogHandler)
                if (dailySale == null) {
                    MessageLog.d(TAG, "[$name] DailySale plugin returned NULL.")
                    result.dialog.close(game.imageUtils)
                } else {
                    MessageLog.d(TAG, "[$name] Handling Daily Sale...")
                    result.dialog.ok(game.imageUtils)
                    game.wait(0.5)
                    dailySale.start()
                }
            }
            "items_selected" -> {
                if (bShouldUseParfait) {
                    MessageLog.d(TAG, "[$name] Using Pleasing Parfait...")
                    IconPleasingParfait.click(game.imageUtils)
                }
                result.dialog.ok(game.imageUtils)
            }
            "race_details" -> result.dialog.ok(game.imageUtils)
            "retry_race" -> {
                MessageLog.d(TAG, "[$name] Resuming existing race...")
                result.dialog.ok(game.imageUtils)
            }
            "special_missions" -> {
                handleSpecialMissionsTabs()
                result.dialog.close(game.imageUtils)
            }
            "rewards_collected" -> result.dialog.close(game.imageUtils)
            "trophy_won" -> result.dialog.close(game.imageUtils)
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        return listOf<PageInterface>(
            PageLegendRaceHome,
            PageExtraRacesRunnerSelection,
            PageExtraRacesPreRacePrep,
            PageExtraRacesRacePrep,
        ).find { it.check(game.imageUtils, bitmap) }
    }

    private fun handleTab(tab: ComponentInterface) {
        game.wait(0.25, skipWaitingForLoading = true)
        // Not an error since we check against all possible tabs to make this
        // function as general as possible.
        if (!tab.click(game.imageUtils)) {
            return
        }
        MessageLog.d(TAG, "[$name] Handling tab: ${tab::class.simpleName}")
        game.wait(0.25, skipWaitingForLoading = true)
        if (ButtonCollectAll.checkDisabled(game.imageUtils) == true) {
            MessageLog.d(TAG, "[$name] Tab has no rewards to collect.")
            return
        }
        ButtonCollectAll.click(game.imageUtils)
        MessageLog.d(TAG, "[$name] Collected rewards for tab.")
        game.wait(0.5)
        handleDialogs()
    }

    private fun handleSpecialMissionsTabs() {
        MessageLog.d(TAG, "[$name] Handling Special Missions tabs...")

        // Click CollectAll on the current tab before proceeding.
        // Otherwise we'll fail to detect the active tab and won't ever collect it.
        if (ButtonCollectAll.checkDisabled(game.imageUtils) == false) {
            ButtonCollectAll.click(game.imageUtils)
            MessageLog.d(TAG, "[$name] Collected rewards for initial tab.")
            game.wait(0.5)
            handleDialogs()
        }

        specialMissionsTabs.forEach { handleTab(it) }
        bHasHandledSpecialMissions = true
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val currentPage: PageInterface? = super.progress(bitmap)

        // We do this after super call to avoid taking unnecessary screenshots.
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        when (currentPage) {
            PageLegendRaceHome -> {
                if (ButtonSpecialMissions.click(game.imageUtils, tries = 5)) {
                    MessageLog.d(TAG, "[$name] PageLegendRaceHome: Handling special missions...")
                    game.wait(0.5)
                    handleDialogsUntilNoneRemain()
                    game.wait(0.5)
                }

                selectRace()
            }
            PageExtraRacesRunnerSelection -> {
                PageExtraRacesRunnerSelection.next(game.imageUtils)
                // Add a delay for the dialog to pop up.
                game.wait(0.5, skipWaitingForLoading = true)
            }
            PageExtraRacesPreRacePrep -> {
                PageExtraRacesPreRacePrep.next(game.imageUtils)
                // Add a delay for the dialog to pop up.
                game.wait(0.5, skipWaitingForLoading = true)
            }
            PageExtraRacesRacePrep -> {
                if (ButtonViewResults.checkDisabled(game.imageUtils) == true) {
                    MessageLog.d(TAG, "[$name] PageExtraRacesRacePrep: ViewResults is disabled. Manually racing...")
                    ButtonRaceManual.click(game.imageUtils)
                } else {
                    MessageLog.d(TAG, "[$name] PageExtraRacesRacePrep: Skipping race...")
                    ButtonViewResults.click(game.imageUtils)
                }
            }
            else -> {
                // Catch-all for various intermediate screens.
                if (!ButtonSkip.click(game.imageUtils, sourceBitmap = bitmap) &&
                    !ButtonNext.click(game.imageUtils, sourceBitmap = bitmap) &&
                    !ButtonRaceExclamation.click(game.imageUtils, sourceBitmap = bitmap)
                ) {
                    MessageLog.d(TAG, "[$name] Unknown page. Tapping...")
                    game.tap(350.0, 750.0, "ok", taps = 1)
                }
            }
        }

        return null
    }

    override fun goToStart(): Boolean {
        super.goToStart()

        if (PageLegendRaceHome.check(game.imageUtils)) {
            return true
        }

        if (!goToHome()) {
            MessageLog.e(TAG, "[$name] Failed to go to MenuBar Home tab. Cannot continue.")
            return false
        }

        if (!menuBar.goToRace()) {
            MessageLog.w(TAG, "[$name] Failed to go to menu bar's Race tab.")
            return false
        }
        
        if (waitForButton(ButtonRaceEvents, bShouldClickButton = true) == null) {
            MessageLog.w(TAG, "[$name] Failed to find Race Events button.")
            return false
        }

        if (waitForButton(ButtonLegendRace, bShouldClickButton = false) == null) {
            MessageLog.e(TAG, "[$name] Failed to find Legend Race button. Cannot proceed.")
            return false
        }

        if (ButtonLegendRace.checkDisabled(game.imageUtils) == true) {
            MessageLog.i(TAG, "[$name] Legend Race is locked. Cannot proceed.")
            return false
        }

        if (!ButtonLegendRace.click(game.imageUtils)) {
            MessageLog.w(TAG, "[$name] Failed to click Legend Race button.")
            return false
        }

        // If the game closes or user backs out of a legend race after selecting a runner,
        // then clicking the LegendRace button will ask if we want to resume the race
        // in a RetryRace dialog. Need to handle this case.
        game.wait(0.5, skipWaitingForLoading = true)
        val dialogResult: DialogHandlerResult = handleDialogs()
        if (
            dialogResult is DialogHandlerResult.Handled &&
            dialogResult.dialog.name == "retry_race"
        ) {
            // Allow game to load before progressing.
            game.wait(0.5)
            // If we handled the RetryRace dialog, then the game will go straight
            // to the race prep screen. So we need to return now instead of waiting
            // for PageLegendRaceHome.
            return true
        }

        return waitForPage(PageLegendRaceHome) != null
    }
}
