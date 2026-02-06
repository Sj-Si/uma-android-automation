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

import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageHome
import com.steve1316.uma_android_automation.components.PageTeamTrialsHome
import com.steve1316.uma_android_automation.components.PageTeamTrialsSelectOpponent
import com.steve1316.uma_android_automation.components.PageTeamTrialsPreRace
import com.steve1316.uma_android_automation.components.PageTeamTrialsRaceQuickModeOff
import com.steve1316.uma_android_automation.components.PageTeamTrialsRaceQuickModeOn
import com.steve1316.uma_android_automation.components.PageTeamTrialsRaceFinished
import com.steve1316.uma_android_automation.components.PageTeamTrialsPreRaceResults
import com.steve1316.uma_android_automation.components.PageTeamTrialsRaceResults
import com.steve1316.uma_android_automation.components.ButtonTeamTrialsQuickModeOff
import com.steve1316.uma_android_automation.components.ButtonRaceAgain
import com.steve1316.uma_android_automation.components.ButtonSkip
import com.steve1316.uma_android_automation.components.ButtonNext
import com.steve1316.uma_android_automation.components.ButtonMenuBarRace
import com.steve1316.uma_android_automation.components.ButtonTeamTrialsTallying
import com.steve1316.uma_android_automation.components.ButtonTeamTrials
import com.steve1316.uma_android_automation.components.IconTeamTrialsOpponentSelectionLaurelRight
import com.steve1316.uma_android_automation.components.IconPleasingParfait
import com.steve1316.uma_android_automation.components.LabelTeamTrialsExtraRewardsOpponent

class TeamTrials(
    game: Game,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]TeamTrials"

    private val bShouldHandleDailySale: Boolean = SettingsHelper.getStringArraySetting("dailyTasks", "saleItems").isNotEmpty()
    private val bShouldUseParfaitOnExtraRewards: Boolean = SettingsHelper.getBooleanSetting("dailyTasks", "enableTeamTrialsUseParfaitOnExtraRewards")

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

    override fun handleDialogs(dialog: DialogInterface?): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "daily_sale" -> {
                // TODO: Handle daily sales.
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
            "items_selected" -> {
                if (bIsExtraRewards && bShouldUseParfaitOnExtraRewards) {
                    IconPleasingParfait.click(game.imageUtils)
                }
                result.dialog.ok(game.imageUtils)
                // Reset this flag every time we handle this dialog.
                bIsExtraRewards = false
            }
            "confirm_restore_rp" -> {
                result.dialog.close(game.imageUtils)
                game.wait(0.5, skipWaitingForLoading = true)
                bIsComplete = true
            }
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
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
                if (handleDialogs() !is DialogHandlerResult.Handled &&
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

    override fun goToStart(): Boolean {
        var dialogResult: DialogHandlerResult = handleDialogs()
        while (dialogResult is DialogHandlerResult.Handled) {
            dialogResult = handleDialogs()
        }

        if (dialogResult is DialogHandlerResult.Unhandled) {
            MessageLog.e(TAG, "Unhandled dialog prevented plugin execution: ${dialogResult.dialog.name}")
            return false
        }

        if (PageTeamTrialsHome.check(game.imageUtils)) {
            return true
        }

        if (!PageHome.check(game.imageUtils)) {
            MessageLog.w(TAG, "Not at home menu. Cannot proceed.")
            return false
        }

        if (!waitForButton(ButtonMenuBarRace)) {
            MessageLog.w(TAG, "Failed to find Race button on menu bar.")
            return false
        }

        game.wait(0.5)
        game.waitForLoading()

        if (ButtonTeamTrialsTallying.check(game.imageUtils)) {
            MessageLog.i(TAG, "Team Trials are tallying. Cannot proceed.")
            return false
        }
        
        if (!waitForButton(ButtonTeamTrials)) {
            MessageLog.w(TAG, "Failed to find Team Trials button.")
            return false
        }

        return waitForPage(PageTeamTrialsHome)
    }
}
