package com.steve1316.uma_android_automation.bot

import android.graphics.Bitmap

import org.opencv.core.Point

import com.steve1316.automation_library.data.SharedData
import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.SettingsHelper

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.campaigns.Campaign
import com.steve1316.uma_android_automation.bot.Task
import com.steve1316.uma_android_automation.bot.TaskResult
import com.steve1316.uma_android_automation.bot.TaskResultCode

import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.PluginFactory
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult

import com.steve1316.uma_android_automation.components.*

class Plugins(game: Game) : Task(game) {
    // The ordered list of plugins from the settings.
    val pluginsSetting: List<String> = SettingsHelper.getStringArraySetting("plugins", "enabledPlugins")
        .map { it.replace("\\s+".toRegex(), "") }

    fun pluginDialogHandler(dialog: DialogInterface? = null, args: Map<String, Any> = mapOf()): DialogHandlerResult {
        val (bWasDialogHandled, dialog) = handleDialogs(dialog, args)

        if (dialog == null) {
            return DialogHandlerResult.NoDialogDetected
        }

        if (bWasDialogHandled) {
            return DialogHandlerResult.Handled(dialog)
        }

        when (dialog.name) {
            "date_changed" -> {
                dialog.close(game.imageUtils)
                handleTitleMenu()
            }
            "notices" -> dialog.close(game.imageUtils)
            "open_soon" -> {
                dialog.close(game.imageUtils)
            }
            "story_unlocked" -> dialog.close(game.imageUtils)
            else -> return DialogHandlerResult.Unhandled(dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(dialog)
    }

    private fun selectLegendRace(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        // Always select the hardest race.
        val locs: ArrayList<Point> = IconExtraRacePill.findAll(game.imageUtils, sourceBitmap = bitmap)
        if (locs.isEmpty()) {
            return false
        }

        game.tap(locs.first().x, locs.first().y, IconExtraRacePill.template.path)

        game.waitForLoading()

        return true
    }

    /** Handles navigating to the home screen from the title menu.
     *
     * @param timeoutMs The max time this operation can run before failing.
     *
     * @return Whether the bot is currently at the home screen.
     */
    private fun handleTitleMenu(timeoutMs: Int = 60000): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                pluginDialogHandler() is DialogHandlerResult.Handled -> {}
                // If we see the ButtonHomeSpecialMissions button, then we're at the home page.
                // Since we're at home page, we're done.
                ButtonHomeSpecialMissions.check(game.imageUtils, sourceBitmap = bitmap) -> return true
                ButtonTitleScreen.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                ButtonSkip.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                else -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }

        MessageLog.e(TAG, "[PLUGINS] Timed out going to home screen from title.")
        return false
    }

	override fun start(maxRuntimeMinutes: Int = 5 * 90): TaskResult {
		MessageLog.i(TAG, "[PLUGINS] Starting process for handling plugins...")

        val startTime: Long = System.currentTimeMillis()

        var overallResult: TaskResult = TaskResult.Error(
            TaskResultCode.TASK_RESULT_TIMED_OUT,
            "The task timed out after $maxRuntimeMinutes minutes.",
        )
        val results: MutableList<TaskResult> = mutableListOf()
        for (pluginName in pluginsSetting) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                break
            }

            val plugin: Plugin? = PluginFactory.create(pluginName, game, dialogHandler = ::pluginDialogHandler)
            if (plugin == null) {
                continue
            }

            val pluginResult: Boolean = plugin.start()
            val result: TaskResult = if (pluginResult) {
                MessageLog.i(TAG, "[PLUGINS] [${plugin.name}] Completed successfully.")
                TaskResult.Success(
                    TaskResultCode.TASK_RESULT_COMPLETE,
                    "[PLUGINS] [${plugin.name}] Completed successfully.",
                )
            } else {
                MessageLog.e(TAG, "[PLUGINS] [${plugin.name}] Failed.")
                TaskResult.Error(
                    TaskResultCode.TASK_RESULT_UNHANDLED_EXCEPTION,
                    "[PLUGINS] [${plugin.name}] Failed.",
                )
            }

            results.add(result)
        }

        val numSuccess: Int = results.count { it is TaskResult.Success }
        val numFailed: int = results.size - numSuccess

        var logMessage: String = "Plugins tasks completed: $numSuccess success / $numFailed failed"
        var discordMessage: String = "Plugins tasks completed: $numSuccess success / $numFailed failed"
        for (result in results) {
            val msgBase: String = "${result.javaClass.simpleName} (${result.code}): ${result.message}"
            val diffChar: String = if (result is TaskResult.Success) "+" else "-"
            logMessage += "\n\t$msgBase"
            discordMessage += "\n\t$diffChar $msgBase"
        }
        game.notificationMessage = logMessage

        if (result is TaskResult.Success) {
            MessageLog.i(TAG, logMessage)
        } else {
            MessageLog.e(TAG, logMessage)
        }

        if (DiscordUtils.enableDiscordNotifications) {
            DiscordUtils.queue.add("```diff\n[${MessageLog.getSystemTimeString()}] $discordMessage\n```")
            // Wait to make sure Discord webhook message queue gets fully processed.
            game.wait(1.0, skipWaitingForLoading = true)
        }

        return result
	}
}
