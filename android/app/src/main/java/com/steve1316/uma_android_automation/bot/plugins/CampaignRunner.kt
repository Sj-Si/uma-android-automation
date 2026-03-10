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

import com.steve1316.uma_android_automation.bot.Campaign
import com.steve1316.uma_android_automation.bot.CampaignResult
import com.steve1316.uma_android_automation.bot.campaigns.UnityCup

import com.steve1316.uma_android_automation.utils.ScrollList
import com.steve1316.uma_android_automation.utils.ScrollListEntry

import com.steve1316.uma_android_automation.types.Scenario

import com.steve1316.uma_android_automation.components.BaseComponentInterface
import com.steve1316.uma_android_automation.components.ButtonAutoFill
import com.steve1316.uma_android_automation.components.ButtonAutoSelect
import com.steve1316.uma_android_automation.components.ButtonBorrowSupportCard
import com.steve1316.uma_android_automation.components.ButtonClose
import com.steve1316.uma_android_automation.components.ButtonCompleteCareer
import com.steve1316.uma_android_automation.components.ButtonHomeCareer
import com.steve1316.uma_android_automation.components.ButtonHomeCareerResume
import com.steve1316.uma_android_automation.components.ButtonLegacySelectRemove
import com.steve1316.uma_android_automation.components.ButtonNext
import com.steve1316.uma_android_automation.components.ButtonScenarioDetails
import com.steve1316.uma_android_automation.components.ButtonSelectLegacy
import com.steve1316.uma_android_automation.components.ButtonSkip
import com.steve1316.uma_android_automation.components.ButtonSkip0
import com.steve1316.uma_android_automation.components.ButtonSkip1
import com.steve1316.uma_android_automation.components.ButtonSkip2
import com.steve1316.uma_android_automation.components.ButtonSupportFormationGreenPlus
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.IconDialogScrollListBottomRight
import com.steve1316.uma_android_automation.components.IconDialogScrollListTopLeft
import com.steve1316.uma_android_automation.components.IconDuplicateSupportPill
import com.steve1316.uma_android_automation.components.IconFollowingPill
import com.steve1316.uma_android_automation.components.IconScenarioSelectUnityCup
import com.steve1316.uma_android_automation.components.IconScenarioSelectUraFinale
import com.steve1316.uma_android_automation.components.IconSelectedPill
import com.steve1316.uma_android_automation.components.IconTraineePill
import com.steve1316.uma_android_automation.components.LabelRemove
import com.steve1316.uma_android_automation.components.MenuBar
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageLegacySelect
import com.steve1316.uma_android_automation.components.PageScenarioSelect
import com.steve1316.uma_android_automation.components.PageSupportFormation
import com.steve1316.uma_android_automation.components.PageTraineeSelect

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

    val campaign: Campaign = when (scenario) {
        Scenario.URA_FINALE -> Campaign(game)
        Scenario.UNITY_CUP -> UnityCup(game)
        else -> throw InterruptedException("Invalid scenario selected: $scenario")
    }

    override fun handleDialogs(dialog: DialogInterface?, args: Map<String, Any>): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog, args)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "auto_select" -> result.dialog.ok(game.imageUtils)
            "borrow_card" -> {
                val scrollList: ScrollList? = ScrollList.create(
                    game,
                    listTopLeftComponent = IconDialogScrollListTopLeft,
                    listBottomRightComponent = IconDialogScrollListBottomRight,
                )
                if (scrollList == null) {
                    MessageLog.e(TAG, "[$name] Failed to detect Borrow Card scroll list.")
                    result.dialog.close(game.imageUtils)
                    return DialogHandlerResult.Handled(result.dialog)
                }

                scrollList.process(onEntry = ::onBorrowSupportCardListEntry)

                // If we successfully selected a card, then the dialog will close
                // on its own. We need to check to see if this dialog is still open.
                val dialogResult: DialogHandlerResult = handleDialogs(
                    args = mapOf<String, Any>("dialogNameToDefer" to "borrow_card"),
                )
                // If it is deferred, that means the dialog is still open.
                if (dialogResult is DialogHandlerResult.Deferred) {
                    dialogResult.dialog.close(game.imageUtils)
                }
            }
            "career_complete" -> result.dialog.close(game.imageUtils)
            "complete_career" -> result.dialog.ok(game.imageUtils)
            "confirm_auto_select" -> result.dialog.ok(game.imageUtils)
            "continue_career" -> {
                result.dialog.ok(game.imageUtils)
                // It takes a while to load campaign sometimes.
                game.wait(5.0)
                startCampaign()
            }
            "final_confirmation" -> {
                result.dialog.ok(game.imageUtils)
                // It takes a while to load campaign sometimes.
                game.wait(5.0)
                // Skip to get to the QuickModeSettings dialog.
                waitForButton(ButtonSkip, bShouldClickButton = true)
                waitForButton(listOf<BaseComponentInterface>(ButtonSkip0, ButtonSkip1, ButtonSkip2))
                handleSkipButton()
                handleDialogs()
                startCampaign()
            }
            "quick_mode_settings" -> {
                result.dialog.ok(game.imageUtils)
            }
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        return listOf<PageInterface>(
            PageScenarioSelect,
            PageTraineeSelect,
            PageLegacySelect,
            PageSupportFormation,
        ).find { it.check(game.imageUtils, bitmap) }
    }

    private fun selectScenario(timeoutMs: Int = 10000): Boolean {
        // Calculate arrow position.
        // (50px / 1080px) = ~0.0463 for a 240dpi screen. Offset from edge slightly.
        val x: Double = (SharedData.displayWidth - (SharedData.displayWidth * 0.0463)).toDouble()
        // (880px / 1920px) = ~0.458 for a 240dpi screen. Just less than half way down.
        val y: Double = (SharedData.displayHeight * 0.458).toDouble()

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            // Many tries since there are visual effects that may block the icon.
            if (scenarioIcon.check(game.imageUtils, tries = 30)) {
                MessageLog.d(TAG, "[$name] Detected scenario icon: ${scenarioIcon::class.simpleName}")
                return true
            }

            // Tap on the right arrow.
            game.tap(x, y)
            // Small delay to allow the screen to settle.
            game.wait(1.0, skipWaitingForLoading = true)
        }

        MessageLog.w(TAG, "[$name] Timed out while attempting to select the scenario: $scenario.")
        return false
    }

    private fun selectLegacy(): Boolean {
        if (ButtonSelectLegacy.check(game.imageUtils)) {
            ButtonAutoSelect.click(game.imageUtils)
            return handleDialogs() is DialogHandlerResult.Handled
        }

        return true
    }

    private fun onBorrowSupportCardListEntry(scrollList: ScrollList, entry: ScrollListEntry): Boolean {
        MessageLog.d(TAG, "[$name] Handling Borrow Card entry #${entry.index}.")

        when {
            // Always skip the "Remove" entry.
            LabelRemove.check(game.imageUtils, sourceBitmap = entry.bitmap) -> return false
            // Skip invalid entries.
            IconTraineePill.check(game.imageUtils, sourceBitmap = entry.bitmap) -> return false
            IconDuplicateSupportPill.check(game.imageUtils, sourceBitmap = entry.bitmap) -> return false
            // We've already selected this card. Exit early since we're done.
            IconSelectedPill.check(game.imageUtils, sourceBitmap = entry.bitmap) -> return true
        }

        // Select first valid entry in list.
        val x: Double = (entry.bbox.x + (entry.bbox.w / 2).toInt()).toDouble()
        val y: Double = (entry.bbox.y + (entry.bbox.h / 2).toInt()).toDouble()
        game.tap(x, y)
        game.wait(1.0)
        return true
    }

    private fun selectSupportCards(): Boolean {
        val locs: ArrayList<Point> = ButtonSupportFormationGreenPlus.findAll(game.imageUtils)
        when {
            // Already selected support cards. Nothing to do.
            locs.size <= 0 -> return true
            // If we only have one missing card and it is the Borrow Card,
            // then proceed with the Borrow Card dialog handling.
            locs.size == 1 && ButtonBorrowSupportCard.click(game.imageUtils) -> {
                return handleDialogs() is DialogHandlerResult.Handled
            }
            // Otherwise if more than one card is missing, we should just auto fill.
            ButtonAutoFill.click(game.imageUtils) -> {
                return handleDialogs() is DialogHandlerResult.Handled
            }
            // All other cases, just fail.
            else -> throw IllegalStateException("[$name] selectSupportCards: Unhandled case: locs.size=${locs.size}")
        }
    }

    private fun startCampaign(): Boolean {
        MessageLog.i(TAG, "[$name] Starting campaign...")

        val campaignResult: CampaignResult = campaign.start()

        when (campaignResult) {
            is CampaignResult.CareerComplete -> {
                // This should bring us back to a screen with a MenuBar.
                if (!handlePostCampaign()) {
                    return false
                }
                // Since we have a menu bar, this function will take us back to the
                // start point for this plugin.
                return goToStart()
            }
            is CampaignResult.ManuallyStopped -> throw IllegalStateException("[$name] Bot was manually stopped.")
            else -> throw IllegalStateException("[$name] Bot failed to complete campaign for unknown reason.")
        }
        return false
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val currentPage: PageInterface? = super.progress(bitmap)

        // We do this after super call to avoid taking unnecessary screenshots.
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        when (currentPage) {
            PageScenarioSelect -> {
                MessageLog.e("REMOVEME", "[$name] At PageScenarioSelect")
                if (selectScenario()) {
                    PageScenarioSelect.next(game.imageUtils)
                    MessageLog.d(TAG, "[$name] Scenario selected. Proceeding to runner selection...")
                    // Need to wait for this page so that we don't accidentally
                    // try selecting the scenario again.
                    waitForPage(PageTraineeSelect)
                }
            }
            PageTraineeSelect -> {
                MessageLog.e("REMOVEME", "[$name] At PageTraineeSelect")
                PageTraineeSelect.next(game.imageUtils)
                waitForPage(PageLegacySelect)
            }
            PageLegacySelect -> {
                if (!selectLegacy()) {
                    // [selectLegacy] may have opened another screen, so we don't know'
                    // where we are anymore. Just return NULL.
                    return null
                }

                when (PageLegacySelect.nextButton!!.checkDisabled(game.imageUtils)) {
                    // We should have handled everything to make this button enabled.
                    // If it isn't then something went wrong.
                    true -> throw IllegalStateException("[$name] PageLegacySelect.nextButton is disabled.")
                    false -> {
                        PageLegacySelect.next(game.imageUtils)
                        waitForPage(PageSupportFormation)
                    }
                    // We've already confirmed that we're on this page, so if the button
                    // is missing then we've got bigger problems.
                    null -> throw IllegalStateException("[$name] PageLegacySelect.nextButton not found.")
                }
            }
            PageSupportFormation -> {
                if (!selectSupportCards()) {
                    MessageLog.w(TAG, "[$name] PageSupportFormation: Failed to select support cards.")
                    return null
                }

                when (PageSupportFormation.nextButton?.checkDisabled(game.imageUtils)) {
                    // We should have handled everything to make this button enabled.
                    // If it isn't then something went wrong.
                    true -> throw IllegalStateException("[$name] PageSupportFormation.nextButton is disabled.")
                    false -> {
                        // This will open the Final Confirmation dialog.
                        PageSupportFormation.next(game.imageUtils)
                        game.wait(game.dialogWaitDelay)
                    }
                    // We've already confirmed that we're on this page, so if the button
                    // is missing then we've got bigger problems.
                    null -> throw IllegalStateException("[$name] PageSupportFormation.nextButton not found.")
                }
            }
        }

        return null
    }

    private fun handlePostCampaign(timeoutMs: Int = 5000): Boolean {
        ButtonCompleteCareer.click(game.imageUtils, tries = 5)

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            when {
                // Menu bar is back on screen. We're done here.
                menuBar.check() -> return true
                handleDialogs() is DialogHandlerResult.Handled -> {}
                ButtonNext.click(game.imageUtils) -> {}
                ButtonClose.click(game.imageUtils) -> {}
            }
        }
        MessageLog.w(TAG, "[$name] Timed out while handling post-campaign screens.")
        return false
    }

    private fun handleSkipButton(timeoutMs: Int = 3000): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (ButtonSkip2.check(game.imageUtils)) {
                return true
            }
            ButtonSkip1.click(game.imageUtils)
            ButtonSkip0.click(game.imageUtils, taps = 2)
        }
        return false
    }

    private fun checkCampaignInProgress(): Boolean {
        val campaignResult: CampaignResult = campaign.start(
            maxRuntimeMinutes = 5,
            bShouldStopAtMainScreen = true,
        )

        return when (campaignResult) {
            is CampaignResult.CareerComplete -> true
            is CampaignResult.StopAtMainScreenOverride -> true
            is CampaignResult.ManuallyStopped -> throw IllegalStateException("[$name] Bot was manually stopped.")
            is CampaignResult.TimedOut -> false
            is CampaignResult.Unknown -> false
        }
    }

    override fun goToStart(): Boolean {
        super.goToStart()

        if (!goToHome() && checkPage() == null) {
            MessageLog.i(TAG, "[$name] Failed to go to MenuBar Home tab. Checking if campaign is already running...")
            // If we couldn't detect the home menu bar and we couldn't detect
            // which page we're on, then we need to check if we're already mid-campaign.
            if (!checkCampaignInProgress()) {
                MessageLog.e(TAG, "[$name] Campaign not running. Could not determine bot state. Cannot proceed.")
                return false
            }
        }

        val careerButtons: List<BaseComponentInterface> = listOf(
            ButtonHomeCareer,
            ButtonHomeCareerResume,
        )
        if (waitForButton(careerButtons, bShouldClickButton = true) == null) {
            MessageLog.e(TAG, "[$name] Failed to find Home Career button. Cannot proceed.")
            return false
        }

        // Clicking the career button triggers a connection to the server.
        // The connection is delayed after we click the button, so we add a small
        // delay here to account for that.
        game.wait(1.0)

        return waitForPage(PageScenarioSelect) != null
    }
}
