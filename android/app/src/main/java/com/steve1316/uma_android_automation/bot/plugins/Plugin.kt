/**
 *
 * To add a plugin, simply import its file, then add an entry for it
 * in the PluginFactory object.
 *
 * All plugins must exist in the App's BotStateContext settings for them
 * to be available at runtime.
 */
package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.SettingsHelper

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game

import com.steve1316.uma_android_automation.components.BaseComponentInterface
import com.steve1316.uma_android_automation.components.ButtonClose
import com.steve1316.uma_android_automation.components.ButtonNext
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.DialogUtils
import com.steve1316.uma_android_automation.components.IconTaskClearToast
import com.steve1316.uma_android_automation.components.MenuBar
import com.steve1316.uma_android_automation.components.PageHome
import com.steve1316.uma_android_automation.components.PageInterface

import com.steve1316.uma_android_automation.bot.plugins.CampaignRunner
import com.steve1316.uma_android_automation.bot.plugins.ChampionsMeeting
import com.steve1316.uma_android_automation.bot.plugins.ClubActivity
import com.steve1316.uma_android_automation.bot.plugins.DailyRaces
import com.steve1316.uma_android_automation.bot.plugins.DailySale
import com.steve1316.uma_android_automation.bot.plugins.LegendRace
import com.steve1316.uma_android_automation.bot.plugins.Presents
import com.steve1316.uma_android_automation.bot.plugins.SpecialMissions
import com.steve1316.uma_android_automation.bot.plugins.TeamTrials

sealed class DialogHandlerResult {
    data class Handled(val dialog: DialogInterface) : DialogHandlerResult()
    data class Unhandled(val dialog: DialogInterface) : DialogHandlerResult()
    data class Deferred(val dialog: DialogInterface) : DialogHandlerResult()
    data object NoDialogDetected : DialogHandlerResult()
    data object TaskClearToastDetected: DialogHandlerResult()
    data class Error(val message: String) : DialogHandlerResult()
}

typealias DialogHandlerCallback = (DialogInterface?) -> DialogHandlerResult

/** A class abstraction for all plugins.
 *
 * Should be instantiated within a try/catch since we use "require" in init.
 */
abstract class Plugin(
    protected val game: Game,
    protected val menuBar: MenuBar,
    protected val maxRuntimeMinutes: Int = 30,
    protected val commonDialogHandler: DialogHandlerCallback? = null,
) {
    abstract val TAG: String

    val name: String = this::class.simpleName!!

    protected open var bIsComplete: Boolean = false

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
    protected open fun progress(bitmap: Bitmap? = null): PageInterface? {
        var dialogResult: DialogHandlerResult = handleDialogs()
        when (dialogResult) {
            // If it is handled, just return to continue with the loop.
            // Don't want to checkPage here since it would take unnecessary
            // processing time.
            is DialogHandlerResult.Handled -> return null
            // If unhandled, then we can't progress any further.
            // If we did, we'd get stuck in an infinite loop looking for a dialog.
            is DialogHandlerResult.Unhandled -> throw IllegalStateException("Unhandled dialog: ${dialogResult.dialog.name}")
            // If the dialog handling was deferred, then just continue with function.
            is DialogHandlerResult.Deferred -> {}
            // If no dialog detected, we can just continue with this function.
            is DialogHandlerResult.NoDialogDetected -> {}
            // If a toast blocked the dialog and we are here, then we timed out while waiting
            // for the toast to go away, just continue with the function and hope for the best.
            // Rather do this than throw an error and completely stop operation.
            is DialogHandlerResult.TaskClearToastDetected -> {}
            is DialogHandlerResult.Error -> throw IllegalStateException("Dialog handler produced an error: ${dialogResult.message}")
        }

        val page: PageInterface? = checkPage()
        if (page != null) {
            MessageLog.d(TAG, "[$name] at ${page::class.simpleName}")
        }
        return page
    }

    /** Wait for any toasts at the top of the page to disappear.
     *
     * These toasts can cause dialog handling to fail for tall dialogs.
     *
     * @param timeoutMs The max time to wait for dialogs to disappear.
     *
     * @return A bitmap screenshot taken as soon as the toast disappears.
     * This helps us process the screen with no toasts in case another toast
     * appears immediately after the first one goes away.
     * If no toast is ever detected, or if we timed out waiting for toasts
     * to disappear, then NULL is returned instead.
     */
    private fun waitForToasts(timeoutMs: Int = 10000): Bitmap? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (!IconTaskClearToast.check(game.imageUtils, tries=5)) {
                return game.imageUtils.getSourceBitmap()
            }
            // Small delay when we detected a toast to allow it to go away.
            // This way we aren't just burning CPU cycles.
            game.wait(0.5, skipWaitingForLoading = true)
        }

        return null
    }

    /** Detects and handles any dialog popups.
     *
     * @param dialog An optional dialog to evaluate. This allows chaining
     * dialog handler calls for improved performance so that we don't need to
     * perform dialog detection and OCR multiple times.
     * @param args Optional arguments mapping for dialog handling.
     *
     * @return A DialogHandlerResult object based on the result of this function.
     *  - Handled: If the dialog was fully handled.
     *  - Unhandled: If a dialog was detected but not handled by this function.
     *  - NoDialogDetected: If no dialog was detected.
     *  - TaskClearToastDetected: If a toast is detected at the top of the screen,
     *      which could blocked the dialog's title bar.
     *  - Error: If an error occurred during dialog detection/handling.
     */
    protected open fun handleDialogs(dialog: DialogInterface? = null, args: Map<String, Any> = mapOf()): DialogHandlerResult {
        if (commonDialogHandler != null) {
            val commonResult: DialogHandlerResult = commonDialogHandler(dialog)
            if (commonResult is DialogHandlerResult.Handled) {
                MessageLog.d(TAG, "[$name][DIALOG] Common dialog handler handled a dialog.")
                return commonResult
            }
        } else {
            val bShouldWait = args["bShouldWait"] as? Boolean ?: false
            val bShouldWaitForLoading = args["bShouldWaitForLoading"] as? Boolean ?: false
            if (bShouldWait || bShouldWaitForLoading) {
                MessageLog.d(TAG, "[$name][DIALOG] Waiting before handling dialog due to passed args: dialogWaitDelay=${game.dialogWaitDelay}, bShouldWait=$bShouldWait, bShouldWaitForLoading=$bShouldWaitForLoading")
                game.wait(game.dialogWaitDelay, skipWaitingForLoading = !bShouldWaitForLoading)
            }
        }

        var dialog: DialogInterface? = dialog ?: DialogUtils.getDialog(game.imageUtils)
        if (dialog == null) {
            // Toasts will block the top of tall dialogs. This may be why we failed
            // to detect any dialogs. Wait for any to disappear before trying again.
            // If this returns a bitmap, then we want to use it for dialog detection
            // in case another toast tries to pop up afterward.
            val noToastBitmap: Bitmap? = waitForToasts()
            // Now that toasts are gone, check again for a dialog.
            dialog = DialogUtils.getDialog(game.imageUtils, bitmap = noToastBitmap)

            // If the dialog is still null, then just return this result.
            if (dialog == null) {
                return DialogHandlerResult.NoDialogDetected
            }
        }

        MessageLog.d(TAG, "[$name][DIALOG] ${dialog.name}")

        val dialogNameToDefer: String? = args["dialogNameToDefer"] as? String ?: null
        var dialogNamesToDefer: List<String> = args["dialogNamesToDefer"] as? List<String> ?: listOf<String>()
        var bShouldDefer = args["bShouldDefer"] as? Boolean ?: false
        if (dialogNamesToDefer.contains(dialog.name) || dialogNameToDefer == dialog.name) {
            bShouldDefer = true
        }

        if (bShouldDefer) {
            MessageLog.d(TAG, "[$name][DIALOG] Dialog handling deferred to calling function.")
            return DialogHandlerResult.Deferred(dialog)
        }

        // Return the dialog as unhandled so that subclasses can handle it.
        return DialogHandlerResult.Unhandled(dialog)
    }

    /** Handles dialogs until there are none left on the screen.
     *
     * @param timeoutMs The max runtime of this operation before timing out.
     *
     * @return Whether a dialog was detected and handled.
     * @throws IllegalStateException If a dialog was detected and was not handled.
     */
    protected fun handleDialogsUntilNoneRemain(timeoutMs: Int = 10000): Boolean {
        var bWasDialogHandled: Boolean = false
        var dialogResult: DialogHandlerResult = DialogHandlerResult.NoDialogDetected
        // Keep handling dialogs until there are none left.
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            dialogResult = handleDialogs()

            // We want to check for new dialogs if we just handled one.
            // Otherwise, we can break from the loop since there's nothing
            // left to handle.
            if (dialogResult !is DialogHandlerResult.Handled) {
                break
            }
            bWasDialogHandled = true
        }

        // If unhandled, then we can't progress any further.
        if (dialogResult is DialogHandlerResult.Unhandled) {
            throw IllegalStateException("[$name] Unhandled dialog: ${dialogResult.dialog.name}")
        }

        return bWasDialogHandled
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
    protected fun waitForPage(
        pages: List<PageInterface>,
        timeoutMs: Int = 10000,
        bShouldTapWhileWaiting: Boolean = false,
    ): PageInterface? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            // Handle dialogs first to prevent overlap of dialog buttons
            // with page components.
            handleDialogsUntilNoneRemain()

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
     * to wait for just a single page by only passing that page instead
     * of a list of a single page.
     *
     * @param page A PageInterface object to wait for.
     * @param timeoutMs See [waitForPage]
     * @param bShouldTapWhileWaiting See [waitForPage]
     *
     * @return If the [page] is found, then that PageInterface object is returned.
     * Otherwise, NULL is returned.
     */
    protected fun waitForPage(
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
     * @param delayClickMs The time to wait before clicking the button after it is
     * detected. This helps avoid cases where we detect a button before it is
     * actually clickable. That would cause us to click the button and think that
     * we handled it when in fact the button wasn't actually registered as
     * clicked by the game.
     * If [bShouldWaitForButtonToGoAway] is TRUE, then this value is ignored
     * since the button will be clicked until it disappears.
     * @param bShouldWaitForButtonToGoAway Whether to wait for the clicked button
     * to no longer be detected on the screen. This allows us to verify that we
     * actually clicked the button since clicking most buttons in the game
     * causes them to disappear from the screen in some way.
     * @param bShouldHandleDialogs Whether to handle any dialogs that pop up while
     * waiting for the button. This helps to prevent false positive detections
     * of buttons in dialogs that are the same as the button we're waiting for.
     *
     * @return If any of [buttons] is found, then that BaseComponentInterface object
     * is returned. Otherwise, NULL is returned.
     * However, if [bShouldClickButton] and [bShouldWaitForButtonToGoAway] are
     * set to TRUE, and we time out while waiting for the button to go away, then
     * we return NULL since this operation failed.
     */
    protected fun waitForButton(
        buttons: List<BaseComponentInterface>,
        timeoutMs: Int = 10000,
        bShouldTapWhileWaiting: Boolean = false,
        bShouldClickButton: Boolean = false,
        delayClickMs: Int = 250,
        bShouldWaitForButtonToGoAway: Boolean = true,
        bShouldHandleDialogs: Boolean = true,
    ): BaseComponentInterface? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (bShouldHandleDialogs) {
                handleDialogsUntilNoneRemain()
            }

            // Now check for the buttons in question.
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            val button: BaseComponentInterface? = buttons.firstOrNull { it.check(game.imageUtils, sourceBitmap = bitmap) }

            if (button != null) {
                if (bShouldClickButton) {
                    if (bShouldWaitForButtonToGoAway) {
                        // We need to wait for the button to fully disappear from
                        // the screen.
                        while (System.currentTimeMillis() - startTime < timeoutMs) {
                            if (!button.click(game.imageUtils)) {
                                return button
                            }
                            // Small delay to allow the click animation to go away.
                            // Otherwise it would cause us to immediately fail
                            // to find the button.
                            // This also helps prevent us from clicking the button
                            // location after it has already gone away.
                            game.wait(0.25, skipWaitingForLoading = true)
                        }
                        MessageLog.w(TAG, "waitForButton: Timed out while waiting for button to go away: ${button::class.simpleName}")
                        return null
                    } else {
                        if (delayClickMs > 0) {
                            game.wait(delayClickMs / 1000.0, skipWaitingForLoading = true)
                        }
                        button.click(game.imageUtils, sourceBitmap = bitmap)
                    }
                }
                return button
            }

            // If no button is found, then we need to handle any edge cases that
            // many be preventing the button from being detected.
            when {
                // Handle any overlay screen buttons. (i.e. Rewards, Tutorials, etc.)
                ButtonNext.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                ButtonClose.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                // Otherwise we tap to dismiss any other overlay screens.
                bShouldTapWhileWaiting -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }

        return null
    }

    /** Wait for the specified button to become available.
     *
     * This is an overloaded version of [waitForButton] that makes it easier
     * to wait for just a single button by only passing that component instead
     * of a list of a single item.
     *
     * @param button A button to wait for.
     * @param timeoutMs See [waitForButton]
     * @param bShouldTapWhileWaiting See [waitForButton]
     * @param bShouldClickButton See [waitForButton]
     * @param delayClickMs See [waitForButton]
     * @param bShouldWaitForButtonToGoAway See [waitForButton]
     * @param bShouldHandleDialogs See [waitForButton]
     *
     * @return If the [button] is found, then that BaseComponentInterface object
     * is returned. Otherwise, NULL is returned.
     */
    protected fun waitForButton(
        button: BaseComponentInterface,
        timeoutMs: Int = 10000,
        bShouldTapWhileWaiting: Boolean = false,
        bShouldClickButton: Boolean = false,
        delayClickMs: Int = 250,
        bShouldWaitForButtonToGoAway: Boolean = true,
        bShouldHandleDialogs: Boolean = true,
    ): BaseComponentInterface? {
        return waitForButton(
            buttons = listOf<BaseComponentInterface>(button),
            timeoutMs = timeoutMs,
            bShouldTapWhileWaiting = bShouldTapWhileWaiting,
            bShouldClickButton = bShouldClickButton,
            delayClickMs = delayClickMs,
            bShouldWaitForButtonToGoAway = bShouldWaitForButtonToGoAway,
            bShouldHandleDialogs = bShouldHandleDialogs,
        )
    }

    protected open fun checkPage(bitmap: Bitmap? = null): PageInterface? {
        return null
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
     * @return Whether the bot is at its start screen.
     */
    protected open fun goToStart(): Boolean {
        handleDialogsUntilNoneRemain()
        return true
    }

    /** Returns to the game's home screen.
     *
     * This function only needs to be overridden if there are screens
     * in the plugin that do not contain the menu bar. These would require special
     * navigation in order for the bot to return to the home screen since it
     * can't just click the Home tab on the menu bar.
     *
     * @return Whether the bot is back at the home screen.
     */
    protected open fun goToHome(): Boolean {
        handleDialogsUntilNoneRemain()
        return menuBar.goToHome()
    }

    open fun start(): Boolean {
        MessageLog.i(TAG, "[$name] Starting...")

        if (!goToStart()) {
            MessageLog.e(TAG, "[$name] Failed to go to plugin's start screen.")
            // Attempt to return to home. Whether this fails here doesn't matter
            // since the plugin is already in a failure state.
            goToHome()
            return false
        }

        // Now run the plugin until it is complete or times out.
        val timeoutMs: Int = maxRuntimeMinutes * 60000
        val startTime = System.currentTimeMillis()
        while (!bIsComplete && System.currentTimeMillis() - startTime < timeoutMs) {
            progress()
        }

        // Always return to the home screen after bot completion.
        if (!goToHome()) {
            MessageLog.w(TAG, "[$name] Failed to return to home screen after completion.")
            return false
        }

        if (bIsComplete) {
            MessageLog.i(TAG, "[$name] Completed successfully.")
        } else {
            MessageLog.w(TAG, "[$name] Timed out.")
        }

        return bIsComplete
    }
}

class PluginFactory {
    companion object {
        private val TAG: String = "[${MainActivity.loggerTag}]PluginFactory"

        fun create(
            pluginName: String,
            game: Game,
            menuBar: MenuBar? = null,
            dialogHandler: DialogHandlerCallback?,
        ): Plugin? {
            val pluginsSetting: List<String> = SettingsHelper.getStringArraySetting("plugins", "enabledPlugins")
                .map { it.replace("\\s+".toRegex(), "") }
            if (!pluginsSetting.contains(pluginName)) {
                MessageLog.d(TAG, "[$pluginName] Plugin is not enabled.")
                return null
            }

            // All plugins must start at the home screen with the menu bar visible.
            // This can only be overridden if [menuBar] is passed to this function.
            // If we can't detect a menu bar then we are forced to abort this instantiation.
            val menuBar: MenuBar? = menuBar ?: MenuBar.create(game, maxAttempts = 3)
            if (menuBar == null) {
                MessageLog.w(TAG, "[$pluginName] Failed to detect menu bar. Cannot create plugin.")
                return null
            }

            return when (pluginName) {
                "CampaignRunner" -> CampaignRunner(game, menuBar, commonDialogHandler = dialogHandler)
                "ChampionsMeeting" -> ChampionsMeeting(game, menuBar, commonDialogHandler = dialogHandler)
                "ClubActivity" -> ClubActivity(game, menuBar, commonDialogHandler = dialogHandler)
                "DailyRaces" -> DailyRaces(game, menuBar, commonDialogHandler = dialogHandler)
                "DailySale" -> {
                    // If no items are marked for purchase, then we return NULL
                    // since the plugin is effectively disabled.
                    val bShouldHandleDailySale: Boolean = SettingsHelper
                        .getStringArraySetting("plugins", "saleItems")
                        .isNotEmpty()
                    if (bShouldHandleDailySale) {
                        DailySale(game, menuBar, commonDialogHandler = dialogHandler)
                    } else {
                        null
                    }
                }
                "LegendRace" -> LegendRace(game, menuBar, commonDialogHandler = dialogHandler)
                "Presents" -> Presents(game, menuBar, commonDialogHandler = dialogHandler)
                "SpecialMissions" -> SpecialMissions(game, menuBar, commonDialogHandler = dialogHandler)
                "TeamTrials" -> TeamTrials(game, menuBar, commonDialogHandler = dialogHandler)
                else -> throw IllegalArgumentException("Unknown plugin name: $pluginName")
            }
        }
    }
}
