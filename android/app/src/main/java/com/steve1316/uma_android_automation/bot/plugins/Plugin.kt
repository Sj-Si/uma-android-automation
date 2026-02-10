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
import com.steve1316.uma_android_automation.components.ButtonNext
import com.steve1316.uma_android_automation.components.ButtonClose

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

    /** Attempts to progress to this plugin's next state.
     *
     * This base implementation helps to simplify the code in overriding classes.
     * Thus it should be called at the start of all overriding classes like so:
     * 
     * override fun progress(bitmap: Bitmap?): PageInterface? {
     *      val currentPage: PageInterface? = super.progress(bitmap)
     *      if (currentPage == null) {
     *          return null
     *      }
     *      ...
     * }
     *
     * @return The current page after progressing the state.
     */
    open fun progress(bitmap: Bitmap? = null): PageInterface? {
        val dialogResult: DialogHandlerResult = handleDialogs()
        when (dialogResult) {
            // If it is handled, just return to continue with the loop.
            // Don't want to checkPage here since it would take unnecessary
            // processing time.
            is DialogHandlerResult.Handled -> return null
            // If unhandled, then we can't progress any further.
            // If we did, we'd get stuck in an infinite loop looking for a dialog.
            is DialogHandlerResult.Unhandled -> throw IllegalStateException("Unhandled dialog: ${dialogResult.dialog.name}")
            // If no dialog detected, we can just continue with this function.
            is DialogHandlerResult.NoDialogDetected -> {}
        }

        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        return checkPage(bitmap)
    }

    /** Navigates to the home page of the plugin.
     *
     * This base implementation helps to simplify the code in overriding classes.
     * Thus it should be called at the start of all overriding classes like so:
     * 
     * override fun goToStart(): Boolean {
     *      super.goToStart()
     *      ...
     * }
     *
     * @return Whether the plugin is currently at its home page.
     */
    open fun goToStart(): Boolean {
        var dialogResult: DialogHandlerResult = handleDialogs()
        // Keep handling dialogs until there are none left.
        while (dialogResult is DialogHandlerResult.Handled) {
            dialogResult = handleDialogs()
        }

        // If unhandled, then we can't progress any further.
        if (dialogResult is DialogHandlerResult.Unhandled) {
            throw IllegalStateException("Unhandled dialog: ${dialogResult.dialog.name}")
        }

        return false
    }

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

    /** Wait for any of the specified pages to become available on screen.
     *
     * @param pages A list of PageInterface objects to wait for.
     * @param timeoutMs The max time (in milliseconds) to spend looking for a page.
     * @param bShouldTapWhileWaiting Whether the bot should tap on the screen
     * on each iteration where no page is found. This can be helpful for progressing
     * past intermediate screens that either require a tap to progress or can be
     * sped up by tapping.
     *
     * @return If any of [pages] is found, then that PageInterface object is returned.
     * Otherwise, NULL is returned.
     */
    fun waitForPage(
        pages: List<PageInterface>,
        timeoutMs: Int = 10000,
        bShouldTapWhileWaiting: Boolean = false,
    ): PageInterface? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            // Handle dialogs first to prevent overlap of dialog buttons
            // with page components.
            val dialogResult: DialogHandlerResult = handleDialogs()
            when (dialogResult) {
                // If we handled a dialog, just continue to next iteration of loop.
                is DialogHandlerResult.Handled -> continue
                // If unhandled, then we can't progress any further.
                // If we did, we'd get stuck in an infinite loop looking for a dialog.
                is DialogHandlerResult.Unhandled -> throw IllegalStateException("Unhandled dialog: ${dialogResult.dialog.name}")
                // If no dialog detected, we can just continue with this function.
                is DialogHandlerResult.NoDialogDetected -> {}
            }

            // Now check if any of our pages exist.
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            val page: PageInterface? = pages.firstOrNull { it.check(game.imageUtils, bitmap) }
            if (page != null) {
                return page
            }

            // Finally, handle edge cases.
            when {
                // Handle any overlay screen buttons.
                // i.e. Rewards, Tutorials, etc.
                ButtonNext.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                ButtonClose.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                // Otherwise, tap the screen to progress past any intermediate screens.
                bShouldTapWhileWaiting -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }
        return null
    }

    /** Wait for a single page to become available on screen.
     *
     * This is an overloaded version of [waitForPage] that makes it easier
     * to wait for just a single page.
     *
     * @param page A PageInterface object to wait for.
     * @param timeoutMs The max time (in milliseconds) to spend looking for a page.
     * @param bShouldTapWhileWaiting Whether the bot should tap on the screen
     * on each iteration where no page is found. This can be helpful for progressing
     * past intermediate screens that either require a tap to progress or can be
     * sped up by tapping.
     *
     * @return If the [page] is found, then that PageInterface object is returned.
     * Otherwise, NULL is returned.
     */
    fun waitForPage(
        page: PageInterface,
        timeoutMs: Int = 10000,
        bShouldTapWhileWaiting: Boolean = false,
    ): PageInterface? {
        return waitForPage(
            pages = listOf<PageInterface>(page),
            timeoutMs = timeoutMs,
            bShouldTapWhileWaiting = bShouldTapWhileWaiting,
        )
    }

    /** Wait for any of the specified buttons to become available.
     *
     * @param buttons A list of buttons to wait for.
     * @param timeoutMs The max time (in milliseconds) to spend looking for a button.
     * @param bShouldTapWhileWaiting Whether the bot should tap on the screen
     * on each iteration where no button is found. This can be helpful for progressing
     * past intermediate screens that either require a tap to progress or can be
     * sped up by tapping.
     * @param bShouldClickButton Whether the bot should click the button when
     * one is found.
     *
     * @return If any of [buttons] is found, then that BaseComponentInterface object
     * is returned. Otherwise, NULL is returned.
     */
    fun waitForButton(
        buttons: List<BaseComponentInterface>,
        timeoutMs: Int = 3000,
        bShouldTapWhileWaiting: Boolean = false,
        bShouldClickButton: Boolean = false,
    ): BaseComponentInterface? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            // Handle dialogs first to prevent overlap of dialog buttons
            // with the buttons we're waiting for.
            val dialogResult: DialogHandlerResult = handleDialogs()
            when (dialogResult) {
                // If we handled a dialog, just continue to next iteration of loop.
                is DialogHandlerResult.Handled -> continue
                // If unhandled, then we can't progress any further.
                // If we did, we'd get stuck in an infinite loop looking for a dialog.
                is DialogHandlerResult.Unhandled -> throw IllegalStateException("Unhandled dialog: ${dialogResult.dialog.name}")
                // If no dialog detected, we can just continue with this function.
                is DialogHandlerResult.NoDialogDetected -> {}
            }

            // Now check for the buttons in question.
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            if (bShouldClickButton) {
                val button: BaseComponentInterface? = buttons.firstOrNull { it.click(game.imageUtils, sourceBitmap = bitmap) }
                if (button != null) {
                    return button
                }
            } else {
                val button: BaseComponentInterface? = buttons.firstOrNull { it.check(game.imageUtils, sourceBitmap = bitmap) }
                if (button != null) {
                    return button
                }
            }

            // Finally, handle any edge cases.
            when {
                // Handle any overlay screen buttons.
                // i.e. Rewards, Tutorials, etc.
                ButtonNext.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                ButtonClose.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                bShouldTapWhileWaiting -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }

        return null
    }

    /** Wait for the specified button to become available.
     *
     * This is an overloaded version of [waitForButton] that makes it easier
     * to wait for just a single button.
     *
     * @param button A button to wait for.
     * @param timeoutMs The max time (in milliseconds) to spend looking for a button.
     * @param bShouldTapWhileWaiting Whether the bot should tap on the screen
     * on each iteration where no button is found. This can be helpful for progressing
     * past intermediate screens that either require a tap to progress or can be
     * sped up by tapping.
     * @param bShouldClickButton Whether the bot should click the button when
     * one is found.
     *
     * @return If the [button] is found, then that BaseComponentInterface object
     * is returned. Otherwise, NULL is returned.
     */
    fun waitForButton(
        button: BaseComponentInterface,
        timeoutMs: Int = 3000,
        bShouldTapWhileWaiting: Boolean = false,
        bShouldClickButton: Boolean = false,
    ): BaseComponentInterface? {
        return waitForButton(
            buttons = listOf<BaseComponentInterface>(button),
            timeoutMs = timeoutMs,
            bShouldTapWhileWaiting = bShouldTapWhileWaiting,
            bShouldClickButton = bShouldClickButton,
        )
    }

    open fun checkPage(bitmap: Bitmap? = null): PageInterface? {
        return null
    }

    open fun goToHome(): Boolean {
        waitForButton(ButtonMenuBarHome, bShouldClickButton = true)
        ButtonMenuBarHome.click(game.imageUtils)
        val res: Boolean = waitForPage(PageHome) != null
        if (res) {
            // Small delay to ensure that home elements are interactive
            // by the time we return from this function.
            game.wait(0.5)
        }
        return res
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
