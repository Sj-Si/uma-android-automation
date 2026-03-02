package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult
import com.steve1316.uma_android_automation.utils.ScrollList

import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageSpecialMissions
import com.steve1316.uma_android_automation.components.PageEventMissions
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabDaily
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabMain
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabTitles
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabSpecial
import com.steve1316.uma_android_automation.components.ButtonEventMissionsTabLimitedTime
import com.steve1316.uma_android_automation.components.ButtonEventExclusiveMissionsStoryEvent
import com.steve1316.uma_android_automation.components.ButtonEventExclusiveMissionsRacingCarnival
import com.steve1316.uma_android_automation.components.ButtonCollectAll
import com.steve1316.uma_android_automation.components.ButtonEventMissions
import com.steve1316.uma_android_automation.components.ButtonBack
import com.steve1316.uma_android_automation.components.ButtonHomeSpecialMissions
import com.steve1316.uma_android_automation.components.MenuBar

class SpecialMissions(
    game: Game,
    menuBar: MenuBar,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, menuBar, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]SpecialMissions"

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

    private val racingCarnivalMissionsTabs: List<ComponentInterface> = listOf(
        ButtonSpecialMissionsTabDaily,
        ButtonEventMissionsTabLimitedTime,
    )

    private var bHasHandledSpecialMissions: Boolean = false
    private var bHasHandledEventMissions: Boolean = false
    private var bHasHandledRacingCarnivalMissions: Boolean = false

    override fun handleDialogs(dialog: DialogInterface?): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "event_exclusive_missions" -> {
                // TODO: Need to add logic for other entries in this dialog.
                // Only event mission is currently in the dialog. Need to wait
                // for legend races to become available again.
                if (!bHasHandledEventMissions) {
                    // Event missions may not exist for this mode. If not, then
                    // just mark it as complete.
                    if (!ButtonEventExclusiveMissionsStoryEvent.click(game.imageUtils)) {
                        bHasHandledEventMissions = true
                    }
                } else if (!bHasHandledRacingCarnivalMissions) {
                    if (!ButtonEventExclusiveMissionsRacingCarnival.click(game.imageUtils)) {
                        bHasHandledRacingCarnivalMissions = true
                    }
                } else {
                    result.dialog.close(game.imageUtils)
                }
            }
            "event_missions" -> {
                if (!bHasHandledRacingCarnivalMissions) {
                    // Need to set this ahead of time since this is a dialog and it
                    // spawns more dialogs. Otherwise we get stuck in an infinite loop.
                    bHasHandledRacingCarnivalMissions = true
                    handleRacingCarnivalMissionsTabs()
                    result.dialog.close(game.imageUtils)
                    // Need a delay otherwise we'll end up handling this same
                    // dialog again since it is still on screen.
                    game.wait(0.5)
                    handleDialogs()
                }
            }
            "rewards_collected" -> result.dialog.close(game.imageUtils)
            "special_missions" -> result.dialog.ok(game.imageUtils)
            "story_unlocked" -> result.dialog.close(game.imageUtils)
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        return listOf<PageInterface>(
            PageSpecialMissions,
            PageEventMissions,
        ).find { it.check(game.imageUtils, bitmap) }
    }

    private fun handleTab(tab: ComponentInterface) {
        game.wait(0.25, skipWaitingForLoading = true)
        tab.click(game.imageUtils)
        game.wait(0.25, skipWaitingForLoading = true)
        if (ButtonCollectAll.checkDisabled(game.imageUtils) == true) {
            return
        }
        ButtonCollectAll.click(game.imageUtils)
        game.wait(0.5)
        handleDialogs()
    }

    private fun handleSpecialMissionsTabs() {
        specialMissionsTabs.forEach { handleTab(it) }
        bHasHandledSpecialMissions = true
    }

    private fun handleEventMissionsTabs() {
        eventMissionsTabs.forEach { handleTab(it) }
        bHasHandledEventMissions = true
    }

    private fun handleRacingCarnivalMissionsTabs() {
        racingCarnivalMissionsTabs.forEach { handleTab(it) }
        bHasHandledRacingCarnivalMissions = true
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val currentPage: PageInterface? = super.progress(bitmap)

        // We do this after super call to avoid taking unnecessary screenshots.
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        when (currentPage) {
            PageSpecialMissions -> {
                if (!bHasHandledSpecialMissions) {
                    handleSpecialMissionsTabs()
                } else {
                    if (!ButtonEventMissions.click(game.imageUtils)) {
                        bHasHandledEventMissions = true
                        bHasHandledRacingCarnivalMissions = true
                    }
                }
            }
            PageEventMissions -> {
                handleEventMissionsTabs()
                if (bHasHandledEventMissions) {
                    ButtonBack.click(game.imageUtils)
                    game.wait(0.5)
                    handleDialogs()
                }
            }
            else -> {}
        }

        bIsComplete = bHasHandledSpecialMissions &&
            bHasHandledEventMissions &&
            bHasHandledRacingCarnivalMissions

        if (bIsComplete) {
            ButtonBack.click(game.imageUtils)
        }

        return null
    }

     override fun goToStart(): Boolean {
        super.goToStart()

        if (PageSpecialMissions.check(game.imageUtils)) {
            return true
        }

        if (!goToHome()) {
            MessageLog.e(TAG, "[$name] Failed to go to MenuBar Home tab. Cannot continue.")
            return false
        }

        if (waitForButton(ButtonHomeSpecialMissions, bShouldClickButton = true) == null) {
            MessageLog.w(TAG, "Failed to find Special Missions button.")
            return false
        }

        return waitForPage(PageSpecialMissions) != null
    }
}
