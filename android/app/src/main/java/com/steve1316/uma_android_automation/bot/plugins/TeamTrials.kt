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
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
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
import com.steve1316.uma_android_automation.components.ButtonTeamTrialsTallying
import com.steve1316.uma_android_automation.components.ButtonTeamTrials
import com.steve1316.uma_android_automation.components.IconTeamTrialsOpponentSelectionTeamRank
import com.steve1316.uma_android_automation.components.IconPleasingParfait
import com.steve1316.uma_android_automation.components.LabelTeamTrialsExtraRewardsOpponent
import com.steve1316.uma_android_automation.components.MenuBar

class TeamTrials(
    game: Game,
    menuBar: MenuBar,
    maxRuntimeMinutes: Int = 15,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, menuBar, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]TeamTrials"

    private val bShouldUseParfaitOnExtraRewards: Boolean = SettingsHelper.getBooleanSetting("dailyTasks", "enableTeamTrialsUseParfaitOnExtraRewards")

    private var bIsExtraRewards: Boolean = false

    fun handleSelectOpponent(bitmap: Bitmap? = null): Boolean {
        MessageLog.d(TAG, "[$name] Selecting opponent...")
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        // Always select the extra rewards option if it is available.
        if (LabelTeamTrialsExtraRewardsOpponent.click(game.imageUtils, tries = 10)) {
            MessageLog.d(TAG, "[$name] Selected opponent with extra rewards.")
            return true
        }

        // Otherwise always select the hardest opponent.
        val locs: ArrayList<Point> = IconTeamTrialsOpponentSelectionTeamRank.findAll(
            game.imageUtils,
            sourceBitmap = bitmap,
        )

        if (locs.isEmpty()) {
            MessageLog.w(TAG, "[$name] Failed to find any opponents.")
            return false
        }

        MessageLog.d(TAG, "[$name] Selecting opponent at ${locs.first()}.")

        game.tap(
            locs.first().x,
            locs.first().y,
            IconTeamTrialsOpponentSelectionTeamRank.template.path,
        )
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
                    result.dialog.close(game.imageUtils)
                } else {
                    result.dialog.ok(game.imageUtils)
                    game.wait(0.5)
                    dailySale.start()
                }
            }
            "items_selected" -> {
                if (bIsExtraRewards && bShouldUseParfaitOnExtraRewards) {
                    IconPleasingParfait.click(game.imageUtils)
                }
                result.dialog.ok(game.imageUtils)
                // Reset this flag every time we handle this dialog.
                bIsExtraRewards = false
                game.wait(0.5)
            }
            "confirm_restore_rp" -> {
                result.dialog.close(game.imageUtils)
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
        val currentPage: PageInterface? = super.progress(bitmap)

        // We do this after super call to avoid taking unnecessary screenshots.
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        when (currentPage) {
            PageTeamTrialsHome -> {
                PageTeamTrialsHome.next(game.imageUtils, bitmap)
                // Waiting for this page here should prevent us from tapping
                // randomly on the screen which could cause us to accidentally
                // select an opponent before properly handling the opponent screen.
                waitForPage(PageTeamTrialsSelectOpponent)
            }
            PageTeamTrialsSelectOpponent -> {
                // Add a small delay here since when we get extra rewards, it will
                // temporarily make the screen inactive which would cause us to fail
                // to click an opponent.
                game.wait(1.0, skipWaitingForLoading = true)
                if (!handleSelectOpponent(bitmap)) {
                    MessageLog.d(TAG, "[$name] PageTeamTrialsSelectOpponent: Failed to select opponent.")
                    return null
                }
                waitForPage(PageTeamTrialsPreRace)
            }
            PageTeamTrialsPreRace -> {
                bIsExtraRewards = LabelTeamTrialsExtraRewardsOpponent.check(game.imageUtils, tries = 5)
                // This Next button will open a dialog.
                PageTeamTrialsPreRace.next(game.imageUtils, bitmap)
            }
            PageTeamTrialsRaceQuickModeOff -> {
                ButtonTeamTrialsQuickModeOff.click(game.imageUtils, sourceBitmap = bitmap)
                waitForPage(PageTeamTrialsRaceQuickModeOn)
            }
            PageTeamTrialsRaceQuickModeOn -> {
                PageTeamTrialsRaceQuickModeOn.next(game.imageUtils, bitmap)
                waitForButton(ButtonSkip, bShouldClickButton = true)
            }
            // TODO: Maybe remove this handler and allow it to fall through to the ButtonNext.click case?
            PageTeamTrialsRaceFinished -> {
                waitForButton(
                    ButtonNext,
                    bShouldClickButton = true,
                    bShouldWaitForButtonToGoAway = true,
                )
                waitForPage(
                    listOf(PageTeamTrialsPreRaceResults, PageTeamTrialsRaceResults),
                    bShouldTapWhileWaiting = true,
                )
            }
            PageTeamTrialsPreRaceResults -> {
                PageTeamTrialsPreRaceResults.next(game.imageUtils, bitmap)
            }
            PageTeamTrialsRaceResults -> {
                if (bIsComplete) {
                    MessageLog.d(TAG, "[$name] PageTeamTrialsRaceResults: Out of RP. Returning to PageTeamTrialsHome.")
                    PageTeamTrialsRaceResults.next(game.imageUtils, bitmap)
                    waitForPage(PageTeamTrialsHome)
                } else {
                    MessageLog.d(TAG, "[$name] PageTeamTrialsRaceResults: Racing again...")
                    // Clicking RaceAgain can either pop up the Out of RP dialog,
                    // or bring us to the Select Opponent page.
                    // We don't want to wait for the Select Opponents page since
                    // if the dialog pops up, then we'll be stuck waiting for a
                    // page that won't exist.
                    ButtonRaceAgain.click(game.imageUtils)
                }
            }
            else -> {
                // Try and catch any other intermediate overlays or buttons
                // that may pop up. If none are caught, then just click the screen
                // to progress to next screens.
                if (
                    !ButtonSkip.click(game.imageUtils) &&
                    !ButtonNext.click(game.imageUtils)
                ) {
                    MessageLog.d(TAG, "[$name] Unknown page. Tapping...")
                    game.tap(350.0, 750.0, "ok", taps = 1)
                }
            }
        }

        return null
    }

    override fun goToHome(): Boolean {
        if (PageTeamTrialsRaceResults.check(game.imageUtils)) {
            PageTeamTrialsRaceResults.next(game.imageUtils)
        }

        return super.goToHome()
    }

    override fun goToStart(): Boolean {
        super.goToStart()

        if (PageTeamTrialsHome.check(game.imageUtils)) {
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

        val button: BaseComponentInterface? = waitForButton(
            listOf(ButtonTeamTrials, ButtonTeamTrialsTallying),
            bShouldClickButton = false,
        )

        when (button) {
            is ButtonTeamTrialsTallying -> {
                MessageLog.i(TAG, "[$name] Team Trials are tallying. Cannot proceed.")
                return false
            }
            is ButtonTeamTrials -> {
                if (button.checkDisabled(game.imageUtils) == true) {
                    MessageLog.i(TAG, "[$name] Team Trials are locked. Cannot proceed.")
                    return false
                }
                button.click(game.imageUtils)
            }
            else -> {
                MessageLog.e(TAG, "[$name] Failed to find Team Trials button.")
                return false
            }
        }

        return waitForPage(PageTeamTrialsHome) != null
    }
}
