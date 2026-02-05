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
import com.steve1316.uma_android_automation.components.PageHome
import com.steve1316.uma_android_automation.components.PageSpecialMissions
import com.steve1316.uma_android_automation.components.PageEventMissions
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabDaily
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabMain
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabTitles
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabSpecial
import com.steve1316.uma_android_automation.components.ButtonEventMissionsTabLimitedTime
import com.steve1316.uma_android_automation.components.ButtonEventExclusiveMissionsStoryEvent
import com.steve1316.uma_android_automation.components.ButtonCollectAll
import com.steve1316.uma_android_automation.components.ButtonEventMissions
import com.steve1316.uma_android_automation.components.ButtonBack
import com.steve1316.uma_android_automation.components.ButtonHomeSpecialMissions

class SpecialMissions(
    game: Game,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, commonDialogHandler) {
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

    private var bHasHandledSpecialMissions: Boolean = false
    private var bHasHandledEventMissions: Boolean = false

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
                ButtonEventExclusiveMissionsStoryEvent.click(game.imageUtils)
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

    fun handleSpecialMissionsTabs() {
        for (tab in specialMissionsTabs) {
            tab.click(game.imageUtils)
            game.wait(0.1, skipWaitingForLoading = true)
            ButtonCollectAll.click(game.imageUtils)
            game.wait(0.5)
            handleDialogs()
        }

        bHasHandledSpecialMissions = true
    }

    fun handleEventMissionsTabs() {
        for (tab in eventMissionsTabs) {
            tab.click(game.imageUtils)
            game.wait(0.1, skipWaitingForLoading = true)
            ButtonCollectAll.click(game.imageUtils)
            game.wait(0.5)
            handleDialogs()
        }

        bHasHandledEventMissions = true
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val currentPage: PageInterface? = checkPage(bitmap)
        when (currentPage) {
            PageSpecialMissions -> {
                if (!bHasHandledSpecialMissions) {
                    handleSpecialMissionsTabs()
                } else {
                    if (!ButtonEventMissions.click(game.imageUtils)) {
                        bHasHandledEventMissions = true
                    }
                }
            }
            PageEventMissions -> {
                handleEventMissionsTabs()
                if (bHasHandledEventMissions) {
                    ButtonBack.click(game.imageUtils)
                }
            }
            else -> handleDialogs()
        }

        bIsComplete = bHasHandledSpecialMissions && bHasHandledEventMissions
        if (bIsComplete) {
            ButtonBack.click(game.imageUtils)
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

        if (PageSpecialMissions.check(game.imageUtils)) {
            return true
        }

        if (!PageHome.check(game.imageUtils)) {
            MessageLog.w(TAG, "Not at home menu. Cannot proceed.")
            return false
        }

        if (!waitForButton(ButtonHomeSpecialMissions)) {
            MessageLog.w(TAG, "Failed to find Special Missions button.")
            return false
        }

        game.wait(0.5)
        game.waitForLoading()

        return waitForPage(PageSpecialMissions)
    }
}
