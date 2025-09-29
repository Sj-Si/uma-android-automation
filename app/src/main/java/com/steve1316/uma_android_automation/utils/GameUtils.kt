package com.steve1316.uma_android_automation.utils

import com.steve1316.uma_android_automation.utils.ScreenRegion
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

object GameUtils {
    private var TAG: String = "GameUtils"

    // In-game Date info.
    data class Date(
		val year: Int,
		val phase: String,
		val month: Int,
		val turnNumber: Int
	)

    /**
    * Wait the specified seconds to account for ping or loading.
    * It also checks for interruption every 100ms to allow faster interruption and checks if the game is still in the middle of loading.
    *
    * @param seconds Number of seconds to pause execution.
    * @param skipWaitingForLoading If true, then it will skip the loading check. Defaults to false.
    */
    fun wait(seconds: Double, skipWaitingForLoading: Boolean = false) {
        val totalMillis = (seconds * 1000).toLong()
        // Check for interruption every 100ms.
        val checkInterval = 100L

        var remainingMillis = totalMillis
        while (remainingMillis > 0) {
            if (!BotService.isRunning) {
                throw InterruptedException()
            }

            val sleepTime = minOf(checkInterval, remainingMillis)
            runBlocking {
                delay(sleepTime)
            }
            remainingMillis -= sleepTime
        }

        if (!skipWaitingForLoading) {
            // Check if the game is still loading as well.
            waitForLoading()
        }
    }

    /**
    * Wait for the game to finish loading.
    */
    fun waitForLoading() {
        while (checkLoading()) {
            // Avoid an infinite loop by setting the flag to true.
            wait(0.5, skipWaitingForLoading = true)
        }
    }

    /**
    * Checks if the bot is at a "Now Loading..." screen or if the game is awaiting for a server response. This may cause significant delays in normal bot processes.
    *
    * @return True if the game is still loading or is awaiting for a server response. Otherwise, false.
    */
    fun checkLoading(tag: String = TAG): Boolean {
        MessageLog.i(tag, "Now checking if the game is still loading...")
        return if (ImageUtils.findImage("connecting", tries = 1, region = ScreenRegion.TOP_HALF, suppressError = true).first != null) {
            MessageLog.i(tag, "Detected that the game is awaiting a response from the server from the \"Connecting\" text at the top of the screen. Waiting...")
            true
        } else if (ImageUtils.findImage("now_loading", tries = 1, region = ScreenRegion.BOTTOM_HALF, suppressError = true).first != null) {
            MessageLog.i(tag, "Detected that the game is still loading from the \"Now Loading\" text at the bottom of the screen. Waiting...")
            true
        } else {
            false
        }
    }
}