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
import com.steve1316.uma_android_automation.components.DialogUtils
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageHome
import com.steve1316.uma_android_automation.components.ButtonNext
import com.steve1316.uma_android_automation.components.ButtonClose
import com.steve1316.uma_android_automation.components.MenuBar

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
    data object NoDialogDetected : DialogHandlerResult()
}

typealias DialogHandlerCallback = (DialogInterface?) -> DialogHandlerResult

/** A class abstraction for all plugins.
 *
 * Should be instantiated within a try/catch since we use "require" in init.
 */
abstract class Plugin(
    protected val game: Game,
    protected val menuBar: MenuBar,
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
    protected open fun handleDialogs(dialog: DialogInterface? = null): DialogHandlerResult {
        if (commonDialogHandler != null) {
            return commonDialogHandler(dialog)
        }
        val dialog: DialogInterface? = dialog ?: DialogUtils.getDialog(game.imageUtils)
        if (dialog == null) {
            return DialogHandlerResult.NoDialogDetected
        }
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
        var dialogResult: DialogHandlerResult = handleDialogs()
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
     *
     * @return If any of [buttons] is found, then that BaseComponentInterface object
     * is returned. Otherwise, NULL is returned.
     */
    protected fun waitForButton(
        buttons: List<BaseComponentInterface>,
        timeoutMs: Int = 3000,
        bShouldTapWhileWaiting: Boolean = false,
        bShouldClickButton: Boolean = false,
    ): BaseComponentInterface? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            // Handle dialogs first to prevent overlap of dialog buttons
            // with the buttons we're waiting for.
            handleDialogsUntilNoneRemain()

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
    protected fun waitForButton(
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

    open fun start(timeoutMs: Int = 60000 * 5): Boolean {
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

        // Now run the plugin until it is complete or times out.
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
            val pluginsSetting: List<String> = SettingsHelper.getStringArraySetting("dailyTasks", "plugins")
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
                "ChampionsMeeting" -> ChampionsMeeting(game, menuBar, dialogHandler)
                "ClubActivity" -> ClubActivity(game, menuBar, dialogHandler)
                "DailyRaces" -> DailyRaces(game, menuBar, dialogHandler)
                "DailySale" -> {
                    // If no items are marked for purchase, then we return NULL
                    // since the plugin is effectively disabled.
                    val bShouldHandleDailySale: Boolean = SettingsHelper
                        .getStringArraySetting("dailyTasks", "saleItems")
                        .isNotEmpty()
                    if (bShouldHandleDailySale) {
                        DailySale(game, menuBar, dialogHandler)
                    } else {
                        null
                    }
                }
                "LegendRace" -> LegendRace(game, menuBar, dialogHandler)
                "Presents" -> Presents(game, menuBar, dialogHandler)
                "SpecialMissions" -> SpecialMissions(game, menuBar, dialogHandler)
                "TeamTrials" -> TeamTrials(game, menuBar, dialogHandler)
                else -> throw IllegalArgumentException("Unknown plugin name: $pluginName")
            }
        }
    }
}
