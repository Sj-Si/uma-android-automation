package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult

import com.steve1316.uma_android_automation.components.BaseComponentInterface
import com.steve1316.uma_android_automation.components.ButtonAutoSelect
import com.steve1316.uma_android_automation.components.ButtonChampionsMeeting
import com.steve1316.uma_android_automation.components.ButtonChampionsMeetingChangeRegistration
import com.steve1316.uma_android_automation.components.ButtonChampionsMeetingEntry
import com.steve1316.uma_android_automation.components.ButtonChampionsMeetingRegistrationsOpenEntry
import com.steve1316.uma_android_automation.components.ButtonClaim
import com.steve1316.uma_android_automation.components.ButtonCollectAll
import com.steve1316.uma_android_automation.components.ButtonConfirm
import com.steve1316.uma_android_automation.components.ButtonNext
import com.steve1316.uma_android_automation.components.ButtonPlacing
import com.steve1316.uma_android_automation.components.ButtonRace
import com.steve1316.uma_android_automation.components.ButtonRaceEvents
import com.steve1316.uma_android_automation.components.ButtonRaceExclamation
import com.steve1316.uma_android_automation.components.ButtonRaceExclamationPink
import com.steve1316.uma_android_automation.components.ButtonReplayWithImage
import com.steve1316.uma_android_automation.components.ButtonSkip
import com.steve1316.uma_android_automation.components.ButtonSpecialMissions
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.MenuBar
import com.steve1316.uma_android_automation.components.PageChampionsMeetingEntryInfo
import com.steve1316.uma_android_automation.components.PageChampionsMeetingFinals
import com.steve1316.uma_android_automation.components.PageChampionsMeetingHome
import com.steve1316.uma_android_automation.components.PageChampionsMeetingPostRace
import com.steve1316.uma_android_automation.components.PageChampionsMeetingRaces
import com.steve1316.uma_android_automation.components.PageInterface


class ChampionsMeeting(
    game: Game,
    menuBar: MenuBar,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, menuBar, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]ChampionsMeeting"

    override fun handleDialogs(dialog: DialogInterface?): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "auto_select" -> result.dialog.ok(game.imageUtils)
            "confirm_entry" -> result.dialog.ok(game.imageUtils)
            "confirm_registration" -> result.dialog.ok(game.imageUtils)
            "special_missions" -> {
                if (ButtonCollectAll.checkDisabled(game.imageUtils) == true) {
                    result.dialog.close(game.imageUtils)
                } else {
                    result.dialog.ok(game.imageUtils)
                }
            }
            "rewards_collected" -> result.dialog.close(game.imageUtils)
            "runner_history" -> result.dialog.close(game.imageUtils)
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        return listOf<PageInterface>(
            PageChampionsMeetingHome,
            PageChampionsMeetingEntryInfo,
            PageChampionsMeetingFinals,
            PageChampionsMeetingRaces,
            PageChampionsMeetingPostRace,
        ).find { it.check(game.imageUtils, bitmap) }
    }

    fun handleRaceLoop(timeoutMs: Int = 60000 * 2): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                ButtonPlacing.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    // Small delay in case we qualified for next round.
                    // If we did, then it will pop up on screen and we have to
                    // click to dismiss it.
                    game.wait(0.5, skipWaitingForLoading = true)
                    waitForButton(ButtonNext, bShouldTapWhileWaiting = true)
                    return true
                }
                ButtonSkip.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                ButtonRace.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                ButtonRaceExclamation.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                ButtonNext.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                else -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }
        MessageLog.w(TAG, "[$name] handleRaceLoop timed out.")
        return false
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val currentPage: PageInterface? = super.progress(bitmap)

        // We do this after super call to avoid taking unnecessary screenshots.
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        when (currentPage) {
            PageChampionsMeetingHome -> {
                if (ButtonSpecialMissions.click(game.imageUtils)) {
                    game.wait(0.5)
                    handleDialogsUntilNoneRemain()
                }

                // If we didnt qualify for this round, then we have nothing to do.
                if (ButtonChampionsMeetingRegistrationsOpenEntry.checkDisabled(game.imageUtils) == true) {
                    MessageLog.i(TAG, "[$name] Registration is locked. Completed.")
                    bIsComplete = true
                    return PageChampionsMeetingHome
                }

                // If the entry button is disabled for any reason then we have nothing to do.
                if (ButtonChampionsMeetingEntry.checkDisabled(game.imageUtils) == true) {
                    MessageLog.i(TAG, "[$name] Entry is locked. Completed.")
                    bIsComplete = true
                    return PageChampionsMeetingHome
                }

                // Otherwise, just continue to the next screen.
                PageChampionsMeetingHome.next(game.imageUtils)
            }
            PageChampionsMeetingEntryInfo -> {
                if (ButtonConfirm.checkDisabled(game.imageUtils) == true) {
                    ButtonAutoSelect.click(game.imageUtils)
                    game.wait(0.5, skipWaitingForLoading = true)
                    handleDialogs()
                }
                ButtonConfirm.click(game.imageUtils)
                game.wait(0.5, skipWaitingForLoading = true)
                handleDialogs()
                waitForPage(PageChampionsMeetingRaces)
            }
            PageChampionsMeetingFinals -> {
                // Final round registrations open.
                if (ButtonChampionsMeetingChangeRegistration.check(game.imageUtils, sourceBitmap = bitmap)) {
                    bIsComplete = true
                    return PageChampionsMeetingFinals
                }

                // Final round matching.
                if (ButtonRaceExclamationPink.checkDisabled(game.imageUtils, sourceBitmap = bitmap) == true) {
                    bIsComplete = true
                    return PageChampionsMeetingFinals
                }

                // Race available. Handle final race and reward collection.
                if (ButtonRaceExclamationPink.click(game.imageUtils, sourceBitmap = bitmap)) {
                    waitForButton(ButtonNext, bShouldTapWhileWaiting = true)
                    if (!handleRaceLoop()) {
                        throw IllegalStateException("[$name] Failed to complete race loop. Stopping...")
                    }
                    waitForButton(ButtonClaim, bShouldClickButton = true)
                    // Finish claiming rewards.
                    waitForButton(ButtonNext, bShouldClickButton = true)
                } else {
                    if (!ButtonClaim.click(game.imageUtils, tries = 50)) {
                        MessageLog.w(TAG, "[$name] Failed to click the Claim button.")
                        return PageChampionsMeetingRaces
                    }

                    if (waitForButton(ButtonNext, bShouldClickButton = true) == null) {
                        MessageLog.w(TAG, "[$name] Failed to wait for round rewards Next button.")
                        return null
                    }

                    waitForPage(PageChampionsMeetingHome)
                }
            }
            PageChampionsMeetingRaces -> {
                if (ButtonRaceExclamationPink.click(game.imageUtils)) {
                    waitForButton(ButtonNext, bShouldTapWhileWaiting = true)
                    if (!handleRaceLoop()) {
                        throw IllegalStateException("[$name] Failed to complete race loop. Stopping...")
                    }
                    waitForPage(PageChampionsMeetingRaces)
                } else {
                    // Many tries since it has animated sparkles.
                    if (!ButtonClaim.click(game.imageUtils, tries = 50)) {
                        MessageLog.w(TAG, "[$name] Failed to click the Claim button.")
                        return PageChampionsMeetingRaces
                    }

                    if (waitForButton(ButtonNext, bShouldClickButton = true) == null) {
                        MessageLog.w(TAG, "[$name] Failed to wait for round rewards Next button.")
                        return null
                    }

                    waitForPage(PageChampionsMeetingHome)
                }
            }
            PageChampionsMeetingPostRace -> {
                if (ButtonReplayWithImage.check(game.imageUtils, sourceBitmap = bitmap)) {
                    bIsComplete = true
                    MessageLog.i(TAG, "[$name] Champions Meeting is already complete. Exiting...")
                    return PageChampionsMeetingPostRace
                }

                // Many tries since it has animated sparkles.
                if (ButtonClaim.click(game.imageUtils, tries = 50)) {
                    if (waitForButton(ButtonNext, bShouldClickButton = true) != null) {
                        bIsComplete = true
                        return PageChampionsMeetingPostRace
                    }
                    throw IllegalStateException("[$name] Failed to find the NEXT button after claiming rewards. Aborting...")
                }
            }
            else -> handleDialogs()
        }

        return checkPage()
    }

    override fun goToStart(): Boolean {
        super.goToStart()

        if (PageChampionsMeetingHome.check(game.imageUtils)) {
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

        if (waitForButton(ButtonChampionsMeeting, bShouldClickButton = false) == null) {
            MessageLog.e(TAG, "[$name] Failed to find Champions Meeting button. Cannot proceed.")
            return false
        }

        if (ButtonChampionsMeeting.checkDisabled(game.imageUtils) == true) {
            MessageLog.i(TAG, "[$name] Champions Meeting is locked. Cannot proceed.")
            return false
        }

        if (!ButtonChampionsMeeting.click(game.imageUtils)) {
            MessageLog.w(TAG, "[$name] Failed to click Champions Meeting button.")
            return false
        }

        // Tap while waiting for page since on every day it will show a splash
        // screen that needs to be tapped to dismiss.
        return waitForPage(PageChampionsMeetingHome, bShouldTapWhileWaiting = true) != null
    }

    override fun start(timeoutMs: Int): Boolean {
        return super.start(60000 * 20)
    }
}
