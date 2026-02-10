package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult

import com.steve1316.uma_android_automation.components.BaseComponentInterface
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageHome
import com.steve1316.uma_android_automation.components.ButtonMenuBarRace
import com.steve1316.uma_android_automation.components.ButtonRaceEvents
import com.steve1316.uma_android_automation.components.ButtonChampionsMeeting

class ChampionsMeeting(
    game: Game,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]ChampionsMeeting"

    override fun handleDialogs(dialog: DialogInterface?): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        return listOf<PageInterface>(
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
            else -> handleDialogs()
        }

        return checkPage()
    }

    override fun goToStart(): Boolean {
        super.goToStart()

        if (!PageHome.check(game.imageUtils)) {
            MessageLog.w(TAG, "Not at home menu. Cannot proceed.")
            return false
        }

        if (waitForButton(ButtonMenuBarRace, bShouldClickButton = true) == null) {
            MessageLog.w(TAG, "Failed to find Race button on menu bar.")
            return false
        }
        
        if (waitForButton(ButtonRaceEvents, bShouldClickButton = true) == null) {
            MessageLog.w(TAG, "Failed to find Race Events button.")
            return false
        }

        if (ButtonChampionsMeeting.checkDisabled(game.imageUtils)) {
            MessageLog.i(TAG, "Champions Meeting is locked. Cannot proceed.")
            return false
        }

        if (!ButtonChampionsMeeting.click(game.imageUtils)) {
            MessageLog.w(TAG, "Failed to click Champions Meeting button.")
            return false
        }

        MessageLog.e(TAG, "NOT IMPLEMENTED")
        return false
    }
}
