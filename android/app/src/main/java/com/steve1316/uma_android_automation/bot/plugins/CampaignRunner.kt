package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap
import org.opencv.core.Point

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.SettingsHelper
import com.steve1316.automation_library.data.SharedData

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
import com.steve1316.uma_android_automation.components.ButtonSkip
import com.steve1316.uma_android_automation.components.ButtonNext
import com.steve1316.uma_android_automation.components.MenuBar
import com.steve1316.uma_android_automation.components.ButtonScenarioDetails
import com.steve1316.uma_android_automation.components.IconScenarioSelectUraFinale
import com.steve1316.uma_android_automation.components.IconScenarioSelectUnityCup
import com.steve1316.uma_android_automation.components.ButtonHomeCareer
import com.steve1316.uma_android_automation.components.PageCampaignRunnerSelection
import com.steve1316.uma_android_automation.components.PageScenarioSelect

import com.steve1316.uma_android_automation.types.Scenario

class CampaignRunner(
    game: Game,
    menuBar: MenuBar,
    maxRuntimeMinutes: Int = 60 * 5,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, menuBar, maxRuntimeMinutes, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]CampaignRunner"

    private val scenarioString: String = SettingsHelper.getStringSetting("plugins", "campaignRunnerScenario")
    // Key error indicates programmer error.
    private val scenario: Scenario = Scenario.fromName(scenarioString)!!

    private val scenarioIcon: ComponentInterface = when(scenario) {
        Scenario.URA_FINALE -> IconScenarioSelectUraFinale
        Scenario.UNITY_CUP -> IconScenarioSelectUnityCup
        else -> throw IllegalStateException("Invalid scenario selected: $scenario")
    }

    override fun handleDialogs(dialog: DialogInterface?, args: Map<String, Any>): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog, args)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "continue_career" -> result.dialog.ok(game.imageUtils)
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        return listOf<PageInterface>(
            PageScenarioSelect,
            PageCampaignRunnerSelection,
        ).find { it.check(game.imageUtils, bitmap) }
    }

    private fun selectScenario(timeoutMs: Int = 10000): Boolean {
        // Calculate swipe positions. Start from center screen and end on left
        // side of the screen.
        val x0: Float = (SharedData.displayWidth / 2).toFloat()
        val y0: Float = (SharedData.displayHeight / 2).toFloat()
        val x1: Float = x0 - (SharedData.displayWidth / 4).toFloat()
        val y1: Float = y0

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (scenarioIcon.check(game.imageUtils, tries = 30)) {
                return true
            }

            // Swipe from right to left.
            game.gestureUtils.swipe(x0, y0, x1, y1)
        }

        MessageLog.w(TAG, "[$name] Timed out while attempting to select the scenario: $scenario.")
        return false
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val currentPage: PageInterface? = super.progress(bitmap)

        // We do this after super call to avoid taking unnecessary screenshots.
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        when (currentPage) {
            PageScenarioSelect -> {
                if (selectScenario()) {
                    PageScenarioSelect.next(game.imageUtils)
                }
            }
            PageCampaignRunnerSelection -> {
                PageCampaignRunnerSelection.next(game.imageUtils)
                bIsComplete = true
            }
            else -> {
                // Try and catch any other intermediate overlays or buttons
                // that may pop up. If none are caught, then just click the screen
                // to progress to next screens.
                if (
                    !ButtonSkip.click(game.imageUtils)
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

        if (!goToHome()) {
            MessageLog.e(TAG, "[$name] Failed to go to MenuBar Home tab. Cannot continue.")
            return false
        }

        if (waitForButton(ButtonHomeCareer, bShouldClickButton = true) == null) {
            MessageLog.e(TAG, "[$name] Failed to find Home Career button. Cannot proceed...")
            return false
        }

        return waitForPage(PageScenarioSelect) != null
    }
}
