package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.SettingsHelper

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game

import com.steve1316.uma_android_automation.components.BaseComponentInterface
import com.steve1316.uma_android_automation.components.DialogUtils
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageHome
import com.steve1316.uma_android_automation.components.ButtonMenuBarHome

sealed class DialogHandlerResult {
    data class Handled(val dialog: DialogInterface) : DialogHandlerResult()
    data class Unhandled(val dialog: DialogInterface) : DialogHandlerResult()
    data object NoDialogDetected : DialogHandlerResult()
}

typealias DialogHandlerCallback = (DialogInterface?) -> DialogHandlerResult

abstract class Plugin(
    protected val game: Game,
    protected val commonDialogHandler: DialogHandlerCallback? = null,
) {
    abstract val TAG: String
    // Should be set from settings.
    val pluginsSetting: List<String> = SettingsHelper.getStringArraySetting("dailyTasks", "plugins")
        .map { it.replace("\\s+".toRegex(), "").lowercase() }
    open val bIsEnabled: Boolean = pluginsSetting.contains(this::class.simpleName?.lowercase())

    protected var bIsComplete: Boolean = false

    abstract fun progress(bitmap: Bitmap? = null): PageInterface?
    abstract fun goToStart(): Boolean

    /** Detects and handles any dialog popups.
     *
     * @param dialog An optional dialog to evaluate. This allows chaining
     * dialog handler calls for improved performance so that we don't need to
     * perform dialog detection and OCR multiple times.
     *
     * @return A DialogHandlerResult object based on the result of this function.
     *  - Handled: If the dialog was fully handled.
     *  - Unhandled: If a dialog was detected but not handled by this function.
     *  - NoDialogDetected: If no dialog was detected.
     *  - Error: If an error occurred during dialog detection/handling.
     */
    open fun handleDialogs(dialog: DialogInterface? = null): DialogHandlerResult {
        if (commonDialogHandler != null) {
            return commonDialogHandler(dialog)
        }
        val dialog: DialogInterface? = dialog ?: DialogUtils.getDialog(game.imageUtils)
        if (dialog == null) {
            return DialogHandlerResult.NoDialogDetected
        }
        return DialogHandlerResult.Unhandled(dialog)
    }

    fun waitForPages(
        pages: List<PageInterface>,
        timeoutMs: Int = 10000,
        bShouldTapWhileWaiting: Boolean = false,
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                // If we detect any of the screens, then we're done here.
                pages.any { it.check(game.imageUtils, bitmap) } -> return true
                // If we failed to handle a dialog then we're stuck on this page.
                handleDialogs() == DialogHandlerResult.Unhandled -> return false
                // Handle any overlay screen buttons.
                // i.e. Rewards, Tutorials, etc.
                ButtonNext.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                ButtonClose.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                // Otherwise, tap the screen to progress past any intermediate screens.
                bShouldTapWhileWaiting -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }
        return false
    }

    fun waitForPage(
        page: PageInterface,
        timeoutMs: Int = 10000,
        bShouldTapWhileWaiting: Boolean = false,
    ): Boolean {
        return waitForPages(listOf<PageInterface>(page))
    }

    fun waitForButton(
        button: BaseComponentInterface,
        timeoutMs: Int = 3000,
        bShouldTapWhileWaiting: Boolean = false,
        bShouldClickButton: Boolean = true,
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            when {
                bShouldClickButton && button.click(game.imageUtils) -> return true
                !bShouldClickButton && button.check(game.imageUtils) -> return true
                // If we failed to handle a dialog then we're stuck on this page.
                handleDialogs() == DialogHandlerResult.Unhandled -> return false
                // Handle any overlay screen buttons.
                // i.e. Rewards, Tutorials, etc.
                ButtonNext.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                ButtonClose.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                bShouldTapWhileWaiting -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }
        return false
    }

    open fun checkPage(bitmap: Bitmap? = null): PageInterface? {
        return null
    }

    open fun goToHome(): Boolean {
        ButtonMenuBarHome.click(game.imageUtils)
        return waitForPage(PageHome)
    }

    open fun start(timeoutMs: Int = 60000 * 5): Boolean {
        if (!bIsEnabled) {
            MessageLog.d(TAG, "Plugin is disabled.")
            return false
        }

        MessageLog.i(TAG, "Starting plugin...")

        if (!goToStart()) {
            MessageLog.e(TAG, "Failed to go to start screen for plugin.")
            goToHome()
            return false
        }

        val startTime = System.currentTimeMillis()
        while (!bIsComplete && System.currentTimeMillis() - startTime < timeoutMs) {
            progress()
        }

        if (!goToHome()) {
            MessageLog.w(TAG, "Failed to return to home screen.")
            return false
        }

        if (bIsComplete) {
            MessageLog.i(TAG, "Plugin completed successfully.")
        } else {
            MessageLog.i(TAG, "Plugin timed out.")
        }
        return bIsComplete
    }
}
