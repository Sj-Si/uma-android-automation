package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.utils.ScrollList

import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageHome
import com.steve1316.uma_android_automation.components.ButtonHomePresents
import com.steve1316.uma_android_automation.components.LabelNone

class Presents(
    game: Game,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]Presents"

    override fun handleDialogs(dialog: DialogInterface?): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "presents" -> {
                if (LabelNone.check(game.imageUtils)) {
                    bIsComplete = true
                    result.dialog.close(game.imageUtils)
                } else {
                    result.dialog.ok(game.imageUtils)
                }
            }
            "rewards_collected" -> result.dialog.close(game.imageUtils)
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        return null
    }

    override fun goToStart(): Boolean {
        super.goToStart()

        if (!PageHome.check(game.imageUtils)) {
            MessageLog.w(TAG, "Not at home menu. Cannot proceed.")
            return false
        }

        if (waitForButton(ButtonHomePresents) == null) {
            MessageLog.w(TAG, "Failed to find Presents button at home screen.")
            return false
        }

        return true
    }

    override fun start(timeoutMs: Int): Boolean {
        if (!goToStart()) {
            MessageLog.e(TAG, "Failed to go to start screen for plugin.")
            return false
        }

        val startTime = System.currentTimeMillis()
        while (!bIsComplete && System.currentTimeMillis() - startTime < timeoutMs) {
            ButtonHomePresents.click(game.imageUtils)
            handleDialogs()
        }
        return bIsComplete
    }
}
