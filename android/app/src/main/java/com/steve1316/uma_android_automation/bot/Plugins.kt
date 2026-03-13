package com.steve1316.uma_android_automation.bot

import android.graphics.Bitmap

import org.opencv.core.Point

import com.steve1316.automation_library.data.SharedData
import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.SettingsHelper
import com.steve1316.automation_library.utils.DiscordUtils

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.Campaign
import com.steve1316.uma_android_automation.bot.Task
import com.steve1316.uma_android_automation.bot.TaskResult
import com.steve1316.uma_android_automation.bot.TaskResultCode

import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.PluginFactory
import com.steve1316.uma_android_automation.bot.DialogHandlerResult

import com.steve1316.uma_android_automation.components.*

class Plugins(game: Game) : Task(game) {
    // The ordered list of plugins from the settings.
    val pluginsSetting: List<String> = SettingsHelper.getStringArraySetting("plugins", "enabledPlugins")
        .map { it.replace("\\s+".toRegex(), "") }

    override fun handleDialogs(dialog: DialogInterface?, args: Map<String, Any>): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog, args)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "date_changed" -> {
                result.dialog.close(game.imageUtils)
                handleTitleMenu()
            }
            "notices" -> result.dialog.close(game.imageUtils)
            "open_soon" -> {
                result.dialog.close(game.imageUtils)
            }
            "story_unlocked" -> result.dialog.close(game.imageUtils)
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
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
                handleDialogs() is DialogHandlerResult.Handled -> {}
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

    override fun process(): TaskResult? {
        return null
    }

	override fun start(maxRuntimeMinutes: Int): TaskResult {
		MessageLog.i(TAG, "[PLUGINS] Starting process for handling plugins...")

        val maxRuntimeMinutes: Int = maxRuntimeMinutes.coerceAtLeast(60 * 5)

        val startTime: Long = System.currentTimeMillis()
        val timeoutMs: Long = (maxRuntimeMinutes * (60 * 1000)).toLong()

        var overallResult: TaskResult = TaskResult.Success(
            TaskResultCode.TASK_RESULT_COMPLETE,
            "[PLUGINS] Task completed successfully.",
        )
        val results: MutableList<TaskResult> = mutableListOf()
        for (pluginName in pluginsSetting) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                overallResult = TaskResult.Error(
                    TaskResultCode.TASK_RESULT_TIMED_OUT,
                    "[PLUGINS] Timed out after $maxRuntimeMinutes minutes.",
                )
                break
            }

            val plugin: Plugin? = PluginFactory.create(pluginName, game, dialogHandler = ::handleDialogs)
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
                overallResult = TaskResult.Success(
                    TaskResultCode.TASK_RESULT_COMPLETE,
                    "[PLUGINS] Task completed with errors.",
                )
                TaskResult.Error(
                    TaskResultCode.TASK_RESULT_UNHANDLED_EXCEPTION,
                    "[PLUGINS] [${plugin.name}] Failed.",
                )
            }

            results.add(result)
        }

        val numSuccess: Int = results.count { it is TaskResult.Success }
        val numFailed: Int = results.size - numSuccess

        var logMessage: String = "Plugins tasks completed: $numSuccess success / $numFailed failed"
        var discordMessage: String = "Plugins tasks completed: $numSuccess success / $numFailed failed"
        for (result in results) {
            val msgBase: String = "${result.javaClass.simpleName} (${result.code}): ${result.message}"
            val diffChar: String = if (result is TaskResult.Success) "+" else "-"
            logMessage += "\n\t$msgBase"
            discordMessage += "\n\t$diffChar $msgBase"
        }
        game.notificationMessage = logMessage

        if (overallResult is TaskResult.Success) {
            MessageLog.i(TAG, logMessage)
        } else {
            MessageLog.e(TAG, logMessage)
        }

        if (DiscordUtils.enableDiscordNotifications) {
            DiscordUtils.queue.add("```diff\n[${MessageLog.getSystemTimeString()}] $discordMessage\n```")
            // Wait to make sure Discord webhook message queue gets fully processed.
            game.wait(1.0, skipWaitingForLoading = true)
        }

        return overallResult
	}
}
