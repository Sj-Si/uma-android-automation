package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult

import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.ButtonHomePresents
import com.steve1316.uma_android_automation.components.ButtonCollectAll
import com.steve1316.uma_android_automation.components.MenuBar

class Presents(
    game: Game,
    menuBar: MenuBar,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, menuBar, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]Presents"

    override fun handleDialogs(dialog: DialogInterface?): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "presents" -> {
                if (ButtonCollectAll.checkDisabled(game.imageUtils) == true) {
                    MessageLog.d(TAG, "[$name] CollectAll button is disabled. No presents to collect.")
                    bIsComplete = true
                    result.dialog.close(game.imageUtils)
                } else {
                    MessageLog.d(TAG, "[$name] Collecting presents...")
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

        if (waitForButton(ButtonHomePresents) == null) {
            MessageLog.w(TAG, "[$name] Failed to find Presents button at home screen.")
            return false
        }

        return true
    }

    override fun start(timeoutMs: Int): Boolean {
        MessageLog.i(TAG, "[$name] Starting...")

        if (!goToHome()) {
            MessageLog.e(TAG, "[$name] Failed to go to MenuBar Home tab. Cannot continue.")
            return false
        }

        if (!goToStart()) {
            MessageLog.e(TAG, "[$name] Failed to go to plugin's start screen.")
            // Attempt to return to home. Whether this fails here doesn't matter
            // since the plugin is already in a failure state.
            goToHome()
            return false
        }

        val startTime = System.currentTimeMillis()
        while (!bIsComplete && System.currentTimeMillis() - startTime < timeoutMs) {
            ButtonHomePresents.click(game.imageUtils)
            handleDialogs()
        }

        // Don't need to go to home since Presents just opens a dialog at the home
        // screen. Handling the dialog closes it which just leaves us at home screen.
        return bIsComplete
    }
}
