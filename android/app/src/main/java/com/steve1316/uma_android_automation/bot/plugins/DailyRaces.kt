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
import com.steve1316.uma_android_automation.bot.DialogHandlerResult

import com.steve1316.uma_android_automation.components.BaseComponentInterface
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageDailyRacesRaceSelection
import com.steve1316.uma_android_automation.components.PageDailyRacesDifficultySelection
import com.steve1316.uma_android_automation.components.PageExtraRacesRunnerSelection
import com.steve1316.uma_android_automation.components.PageExtraRacesPreRacePrep
import com.steve1316.uma_android_automation.components.PageExtraRacesRacePrep
import com.steve1316.uma_android_automation.components.PageDailyRacesResultsPlacing
import com.steve1316.uma_android_automation.components.PageDailyRacesResultsRewards
import com.steve1316.uma_android_automation.components.ButtonDailyRacesMoonlightSho
import com.steve1316.uma_android_automation.components.ButtonDailyRacesJupiterCup
import com.steve1316.uma_android_automation.components.ButtonDailyRacesMultiRaceOff
import com.steve1316.uma_android_automation.components.ButtonDailyRacesMultiRaceOn
import com.steve1316.uma_android_automation.components.ButtonRaceManual
import com.steve1316.uma_android_automation.components.ButtonViewResults
import com.steve1316.uma_android_automation.components.ButtonRaceAgain
import com.steve1316.uma_android_automation.components.ButtonSkip
import com.steve1316.uma_android_automation.components.ButtonNext
import com.steve1316.uma_android_automation.components.ButtonRaceExclamation
import com.steve1316.uma_android_automation.components.ButtonDailyRaces
import com.steve1316.uma_android_automation.components.IconExtraRacePill
import com.steve1316.uma_android_automation.components.MenuBar

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

class DailyRaces(
    game: Game,
    menuBar: MenuBar,
    maxRuntimeMinutes: Int = 10,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, menuBar, maxRuntimeMinutes, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]DailyRaces"

    private val dailyRaceNameString: String = SettingsHelper.getStringSetting("plugins", "dailyRaceName")
    private val dailyRaceName: DailyRaceName = DailyRaceName.fromName(dailyRaceNameString)!!

    private val dailyRaceButton: ComponentInterface = when (dailyRaceName) {
        DailyRaceName.MOONLIGHT_SHO -> ButtonDailyRacesMoonlightSho
        DailyRaceName.JUPITER_CUP -> ButtonDailyRacesJupiterCup
    }

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

        // If there are no available races, then we've completed them all.
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
            "items_selected" -> result.dialog.ok(game.imageUtils)
            "multi_race" -> result.dialog.ok(game.imageUtils)
            "purchase_daily_race_ticket" -> {
                result.dialog.close(game.imageUtils)
                bIsComplete = true
            }
            "race_details" -> {
                // If the multi race button is disabled, we just need to proceed.
                // This happens if the player hasn't completed this race before.
                // Multi-race is disabled until they win the race for the first time.
                if (ButtonDailyRacesMultiRaceOff.checkDisabled(game.imageUtils) == true ||
                    ButtonDailyRacesMultiRaceOn.checkDisabled(game.imageUtils) == true
                ) {
                    result.dialog.ok(game.imageUtils)
                    // We need an extra delay here since this dialog is slow to close.
                    // Without this, the bot may try to click the dialog button again
                    // as the dialog is closing which will cause the bot to accidentally
                    // click the Race menu bar button instead.
                    game.wait(0.5)
                    return DialogHandlerResult.Handled(result.dialog)
                }
                
                ButtonDailyRacesMultiRaceOff.click(game.imageUtils)
                result.dialog.ok(game.imageUtils)
                // We need an extra delay here since this dialog is slow to close.
                // Without this, the bot may try to click the dialog button again
                // as the dialog is closing which will cause the bot to accidentally
                // click the Race menu bar button instead.
                game.wait(0.5)
            }
            "race_results" -> result.dialog.ok(game.imageUtils)
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        return listOf<PageInterface>(
            PageDailyRacesRaceSelection,
            PageDailyRacesDifficultySelection,
            PageExtraRacesRunnerSelection,
            PageExtraRacesPreRacePrep,
            PageExtraRacesRacePrep,
            PageDailyRacesResultsPlacing,
            PageDailyRacesResultsRewards,
        ).find { it.check(game.imageUtils, bitmap) }
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val currentPage: PageInterface? = super.progress(bitmap)

        // We do this after super call to avoid taking unnecessary screenshots.
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        when (currentPage) {
            PageDailyRacesRaceSelection -> {
                dailyRaceButton.click(game.imageUtils)
            }
            PageDailyRacesDifficultySelection -> {
                selectRace()
            }
            PageExtraRacesRunnerSelection -> {
                PageExtraRacesRunnerSelection.next(game.imageUtils)
                // Add a delay for the dialog to pop up.
                game.wait(0.5, skipWaitingForLoading = true)
            }
            PageExtraRacesPreRacePrep -> {
                PageExtraRacesPreRacePrep.next(game.imageUtils)
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
            PageDailyRacesResultsPlacing -> {
                PageDailyRacesResultsPlacing.next(game.imageUtils)
            }
            PageDailyRacesResultsRewards -> {
                // We don't want to click the RaceAgain button in this instance.
                // This is because we will only be here if multi-race is disabled
                // which means the player hasn't beaten this race yet.
                // So if they DO beat the race, we want to go to the next
                // difficulty up or enable multi-race if possible.
                // Thus we want to go back to the daily races menu.
                PageDailyRacesResultsRewards.next(game.imageUtils)
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

        if (PageDailyRacesRaceSelection.check(game.imageUtils)) {
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

        if (waitForButton(ButtonDailyRaces, bShouldClickButton = false) == null) {
            MessageLog.e(TAG, "[$name] Failed to find Daily Races button. Cannot proceed.")
            return false
        }

        if (ButtonDailyRaces.checkDisabled(game.imageUtils) == true) {
            MessageLog.i(TAG, "[$name] Daily Races are locked. Cannot proceed.")
            return false
        }

        if (!ButtonDailyRaces.click(game.imageUtils)) {
            MessageLog.e(TAG, "[$name] Failed to click Daily Races button.")
            return false
        }

        return waitForPage(PageDailyRacesRaceSelection) != null
    }
}
