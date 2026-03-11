package com.steve1316.uma_android_automation.bot

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.DiscordUtils

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.DialogHandler
import com.steve1316.uma_android_automation.bot.DialogHandlerResult
import com.steve1316.uma_android_automation.bot.Game

import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

enum class TaskResultCode {
    TASK_RESULT_COMPLETE,
    TASK_RESULT_BREAKPOINT_REACHED,
    TASK_RESULT_MANUALLY_STOPPED,
    TASK_RESULT_UNHANDLED_EXCEPTION,
    TASK_RESULT_CONNECTION_ERROR,
    TASK_RESULT_TIMED_OUT,
}

sealed interface TaskResult {
    val code: TaskResultCode
    val message: String

    data class Success(
        override val code: TaskResultCode = TaskResultCode.TASK_RESULT_COMPLETE,
        override val message: String = "Task completed successfully.",
    ) : TaskResult
    data class Error(
        override val code: TaskResultCode = TaskResultCode.TASK_RESULT_UNHANDLED_EXCEPTION,
        override val message: String = "Task completed with errors.",
    ) : TaskResult
}

abstract class Task(game: Game) : DialogHandler(game) {
    val TAG: String = "[${MainActivity.loggerTag}]${this::class.simpleName}"

    /** Processes a single iteration of the main loop.
     *
     * @return If the main loop should stop, then a TaskResult should be returned.
     * Otherwise, return NULL for the main loop to continue iterating.
     */
    abstract fun process(): TaskResult?

    /** Runs all tests for this task.
     *
     * @return Whether any tests ran.
     */
    open fun startTests(): Boolean {
        return false
    }

    /** Attempts to handle dialogs.
     *
     * This allows us to handle successive dialogs in a single function call.
     * This can be useful in mainloops so that we don't need to process the entire
     * mainloop for each successive dialog on the screen.
     *
     * @param timeoutMs The max time (in milliseconds) that this operation can run.
     *
     * @return Whether a dialog was successfully handled.
     * If no dialog is detected at all, then False is returned.
     * If an unhandled dialog was detected, throws an InterruptedException.
     */
    fun tryHandleAllDialogs(timeoutMs: Int = 15000): Boolean {
        var bWasDialogHandled: Boolean = false
        var dialogResult: DialogHandlerResult = DialogHandlerResult.NoDialogDetected
        var startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            dialogResult = handleDialogs()

            if (dialogResult !is DialogHandlerResult.Handled) {
                break
            }
            bWasDialogHandled = true
        }

        if (dialogResult is DialogHandlerResult.Unhandled) {
            throw IllegalStateException("Unhandled dialog: ${dialogResult.dialog.name}")
        }

        return bWasDialogHandled
    }

    /** Handles cleanup when the task main loop breaks and the task is ending.
     *
     * @param result The [TaskResult] of the main loop.
     */
    private fun handleTaskEnd(result: TaskResult) {
        val logMessage: String = "${result.javaClass.simpleName} (${result.code}): ${result.message}"
        game.notificationMessage = logMessage
        val discordMessage: String = "${this::class.simpleName}:: ${result.javaClass.simpleName} (${result.code}): ${result.message}"
        var diffChar: String = ""
        when (result) {
            is TaskResult.Success -> {
                MessageLog.i(TAG, logMessage)
                diffChar = "+"
            }
            is TaskResult.Error -> {
                MessageLog.e(TAG, logMessage)
                diffChar = "-"
            }
        }

        if (DiscordUtils.enableDiscordNotifications) {
            DiscordUtils.queue.add("```diff\n$diffChar ${MessageLog.getSystemTimeString()} $discordMessage.\n```")
            // Wait to make sure Discord webhook message queue gets fully processed.
            game.wait(1.0, skipWaitingForLoading = true)
        }
    }

    /** Runs the task's main loop.
     *
     * @param maxRuntimeMinutes The max allowed time (in minutes) that the main loop
     * can run before it times out.
     *
     * @return The [TaskResult] result of this task's main loop.
     */
    open fun start(maxRuntimeMinutes: Int = 90): TaskResult {
        var result: TaskResult = TaskResult.Error(
            TaskResultCode.TASK_RESULT_TIMED_OUT,
            "The task timed out after $maxRuntimeMinutes minutes.",
        )

        val timeoutMs: Long = 2000L//(maxRuntimeMinutes * (60 * 1000)).toLong()
        val startTime: Long = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val tmpResult: TaskResult? = process()
                // If we receive any result, then we need to stop the task.
                if (tmpResult != null) {
                    result = tmpResult
                    break
                }
            } catch (e: InterruptedException) {
                result = TaskResult.Error(
                    TaskResultCode.TASK_RESULT_UNHANDLED_EXCEPTION,
                    e.message ?: "Unhandled exception. Stopping bot...",
                )
                break
            }
        }

        handleTaskEnd(result)
        return result
    }
}
