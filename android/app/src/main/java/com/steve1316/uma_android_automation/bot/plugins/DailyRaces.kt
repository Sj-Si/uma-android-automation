package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap
import org.opencv.core.Point

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.SettingsHelper

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult
import com.steve1316.uma_android_automation.utils.ScrollList

import com.steve1316.uma_android_automation.components.BaseComponentInterface
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageHome
import com.steve1316.uma_android_automation.components.PageDailyRacesRaceSelection
import com.steve1316.uma_android_automation.components.PageDailyRacesDifficultySelection
import com.steve1316.uma_android_automation.components.PageExtraRacesRunnerSelection
import com.steve1316.uma_android_automation.components.PageDailyRacesPreRacePrep
import com.steve1316.uma_android_automation.components.PageDailyRacesRacePrep
import com.steve1316.uma_android_automation.components.PageDailyRacesResultsPlacing
import com.steve1316.uma_android_automation.components.PageDailyRacesResultsRewards
import com.steve1316.uma_android_automation.components.ButtonDailyRacesMoonlightSho
import com.steve1316.uma_android_automation.components.ButtonDailyRacesJupiterCup
import com.steve1316.uma_android_automation.components.ButtonDailyRacesMultiRaceOff
import com.steve1316.uma_android_automation.components.ButtonRaceManual
import com.steve1316.uma_android_automation.components.ButtonViewResults
import com.steve1316.uma_android_automation.components.ButtonRaceAgain
import com.steve1316.uma_android_automation.components.ButtonSkip
import com.steve1316.uma_android_automation.components.ButtonNext
import com.steve1316.uma_android_automation.components.ButtonRaceExclamation
import com.steve1316.uma_android_automation.components.ButtonMenuBarRace
import com.steve1316.uma_android_automation.components.ButtonDailyRaces
import com.steve1316.uma_android_automation.components.IconExtraRacePill

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
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]DailyRaces"

    private val bShouldHandleDailySale: Boolean = SettingsHelper.getStringArraySetting("dailyTasks", "saleItems").isNotEmpty()
    private val dailyRaceNameString: String = SettingsHelper.getStringSetting("dailyTasks", "dailyRaceName")
    private val dailyRaceName: DailyRaceName = DailyRaceName.fromName(dailyRaceNameString)!!

    private val dailyRaceButton: ComponentInterface = when (dailyRaceName) {
        DailyRaceName.MOONLIGHT_SHO -> ButtonDailyRacesMoonlightSho
        DailyRaceName.JUPITER_CUP -> ButtonDailyRacesJupiterCup
    }

    private fun selectRace(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val pillBitmap: Bitmap = IconExtraRacePill.template.getBitmap(game.imageUtils)!!

        // Always select the hardest available race.
        val locs: ArrayList<Point> = IconExtraRacePill.findAll(game.imageUtils, sourceBitmap = bitmap)
        if (locs.isEmpty()) {
            return false
        }

        // Check color at the top left of the pill bitmap region. If it is greyed
        // out, then the race isn't available.
        val enabledLocs: List<Point> = locs
            .mapNotNull {
                val x: Int = (it.x - (pillBitmap.width / 2)).toInt()
                val y: Int = (it.y - (pillBitmap.height / 2)).toInt()
                val bIsEnabled: Boolean = !game.imageUtils.checkColorAtCoordinates(x, y, intArrayOf(162, 159, 164))
                if (bIsEnabled) it else null
            }
            .sortedBy { it.y }

        // If there are no available races, then we've completed them all.
        if (enabledLocs.isEmpty()) {
            bIsComplete = true
            return true
        }

        val loc: Point = enabledLocs.first()
        game.tap(loc.x, loc.y, IconExtraRacePill.template.path)
        game.waitForLoading()

        return true
    }

    override fun handleDialogs(dialog: DialogInterface?): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "daily_sale" -> {
                if (bShouldHandleDailySale) {
                    result.dialog.ok(game.imageUtils)
                    game.wait(0.5)
                    game.waitForLoading()
                    val dailySale = DailySale(game, commonDialogHandler)
                    dailySale.start()
                } else {
                    result.dialog.close(game.imageUtils)
                }
            }
            "items_selected" -> result.dialog.ok(game.imageUtils)
            "multi_race" -> result.dialog.ok(game.imageUtils)
            "purchase_daily_race_ticket" -> {
                result.dialog.close(game.imageUtils)
                bIsComplete = true
            }
            "race_details" -> {
                // Always try to enable multi-race.
                if (ButtonDailyRacesMultiRaceOff.checkDisabled(game.imageUtils)) {
                    result.dialog.ok(game.imageUtils)
                }
                
                if (!ButtonDailyRacesMultiRaceOff.click(game.imageUtils, tries = 5)) {
                    return DialogHandlerResult.Unhandled(result.dialog)
                }
                game.wait(0.5, skipWaitingForLoading = true)
                result.dialog.ok(game.imageUtils)
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
            PageDailyRacesPreRacePrep,
            PageDailyRacesRacePrep,
            PageDailyRacesResultsPlacing,
            PageDailyRacesResultsRewards,
        ).find { it.check(game.imageUtils, bitmap) }
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val currentPage: PageInterface? = super.progress(bitmap)
        if (currentPage == null) {
            return null
        }

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
            }
            PageDailyRacesPreRacePrep -> {
                PageDailyRacesPreRacePrep.next(game.imageUtils)
            }
            PageDailyRacesRacePrep -> {
                if (ButtonViewResults.checkDisabled(game.imageUtils)) {
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
                if (!ButtonSkip.click(game.imageUtils) &&
                    !ButtonNext.click(game.imageUtils) &&
                    !ButtonRaceExclamation.click(game.imageUtils)
                ) {
                    game.tap(350.0, 750.0, "ok", taps = 1)
                }
            }
        }

        return checkPage()
    }

    override fun goToStart(): Boolean {
        super.goToStart()

        if (PageDailyRacesRaceSelection.check(game.imageUtils)) {
            return true
        }

        if (!PageHome.check(game.imageUtils)) {
            MessageLog.w(TAG, "Not at home menu. Cannot proceed.")
            return false
        }

        if (waitForButton(ButtonMenuBarRace, bShouldClickButton = true) == null) {
            MessageLog.w(TAG, "Failed to find Race button on menu bar.")
            return false
        }

        if (ButtonDailyRaces.checkDisabled(game.imageUtils)) {
            MessageLog.i(TAG, "Daily Races are locked. Cannot proceed.")
            return false
        }

        if (!ButtonDailyRaces.click(game.imageUtils)) {
            MessageLog.e(TAG, "Failed to click Daily Races button.")
            return false
        }

        return waitForPage(PageDailyRacesRaceSelection) != null
    }
}
