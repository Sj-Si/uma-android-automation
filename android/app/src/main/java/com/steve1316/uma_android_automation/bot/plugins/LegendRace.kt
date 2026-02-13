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

import com.steve1316.uma_android_automation.components.BaseComponentInterface
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageLegendRaceHome
import com.steve1316.uma_android_automation.components.PageExtraRacesRunnerSelection
import com.steve1316.uma_android_automation.components.ButtonSkip
import com.steve1316.uma_android_automation.components.ButtonNext
import com.steve1316.uma_android_automation.components.ButtonRaceEvents
import com.steve1316.uma_android_automation.components.ButtonLegendRace
import com.steve1316.uma_android_automation.components.IconExtraRacePill
import com.steve1316.uma_android_automation.components.IconPleasingParfait
import com.steve1316.uma_android_automation.components.MenuBar

class LegendRace(
    game: Game,
    menuBar: MenuBar,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, menuBar, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]LegendRace"

    private val bShouldUseParfait: Boolean = SettingsHelper.getBooleanSetting("dailyTasks", "enableLegendRaceUseParfait")

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
                val bIsEnabled: Boolean = game.imageUtils.checkColorAtCoordinates(x, y, intArrayOf(162, 159, 164))
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
        game.wait(0.5)

        return true
    }

    override fun handleDialogs(dialog: DialogInterface?): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "items_selected" -> {
                if (bShouldUseParfait) {
                    IconPleasingParfait.click(game.imageUtils)
                }
                result.dialog.ok(game.imageUtils)
            }
            "race_details" -> result.dialog.ok(game.imageUtils)
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
        ).find { it.check(game.imageUtils, bitmap) }
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val currentPage: PageInterface? = super.progress(bitmap)

        // We do this after super call to avoid taking unnecessary screenshots.
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        when (currentPage) {
            PageLegendRaceHome -> {
                selectRace()
            }
            PageExtraRacesRunnerSelection -> {
                PageExtraRacesRunnerSelection.next(game.imageUtils)
            }
            else -> {
                if (!ButtonSkip.click(game.imageUtils) &&
                    !ButtonNext.click(game.imageUtils)
                ) {
                    game.tap(350.0, 750.0, "ok", taps = 1)
                }
            }
        }

        return checkPage()
    }

    override fun goToStart(): Boolean {
        super.goToStart()

        if (PageLegendRaceHome.check(game.imageUtils)) {
            return true
        }

        if (!menuBar.goToRace()) {
            MessageLog.w(TAG, "Failed to go to menu bar's Race tab.")
            return false
        }
        
        if (waitForButton(ButtonRaceEvents, bShouldClickButton = true) == null) {
            MessageLog.w(TAG, "Failed to find Race Events button.")
            return false
        }

        if (waitForButton(ButtonLegendRace, bShouldClickButton = false) == null) {
            MessageLog.e(TAG, "Failed to find Legend Race button. Cannot proceed.")
            return false
        }

        if (ButtonLegendRace.checkDisabled(game.imageUtils)) {
            MessageLog.i(TAG, "Legend Race is locked. Cannot proceed.")
            return false
        }

        if (!ButtonLegendRace.click(game.imageUtils)) {
            MessageLog.w(TAG, "Failed to click Legend Race button.")
            return false
        }

        return waitForPage(PageLegendRaceHome) != null
    }
}
