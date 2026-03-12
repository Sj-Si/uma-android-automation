package com.steve1316.uma_android_automation.bot

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Task
import com.steve1316.uma_android_automation.bot.Campaign
import com.steve1316.uma_android_automation.bot.campaigns.UnityCup
import com.steve1316.uma_android_automation.bot.Plugins
import com.steve1316.uma_android_automation.bot.campaigns.UraFinale
import com.steve1316.uma_android_automation.utils.CustomImageUtils
import com.steve1316.automation_library.utils.BotService
import com.steve1316.automation_library.utils.DiscordUtils
import com.steve1316.automation_library.data.SharedData
import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.MyAccessibilityService
import com.steve1316.automation_library.utils.SettingsHelper

import com.steve1316.uma_android_automation.bot.SkillDatabase

import com.steve1316.uma_android_automation.components.*

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.opencv.core.Point
import java.text.DecimalFormat
import kotlin.intArrayOf

import com.steve1316.uma_android_automation.bot.plugins.TeamTrials
import com.steve1316.uma_android_automation.bot.plugins.DailySale
import com.steve1316.uma_android_automation.bot.plugins.ClubActivity
import com.steve1316.uma_android_automation.bot.plugins.SpecialMissions
import com.steve1316.uma_android_automation.components.*

/**
 * Main driver for bot activity and navigation.
 */
class Game(val myContext: Context) {
	private val TAG: String = "[${MainActivity.loggerTag}]Game"
	var notificationMessage: String = ""

	val imageUtils: CustomImageUtils = CustomImageUtils(myContext, this)
	val gestureUtils: MyAccessibilityService = MyAccessibilityService.getInstance()
    val skillDatabase: SkillDatabase = SkillDatabase(this)

	val decimalFormat = DecimalFormat("#.##")

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	// SQLite Settings
	val scenario: String = SettingsHelper.getStringSetting("general", "scenario")
	val debugMode: Boolean = SettingsHelper.getBooleanSetting("debug", "enableDebugMode")
    
	val enablePopupCheck: Boolean = SettingsHelper.getBooleanSetting("general", "enablePopupCheck")
    val waitDelay: Double = SettingsHelper.getDoubleSetting("general", "waitDelay")
    val dialogWaitDelay: Double = SettingsHelper.getDoubleSetting("general", "dialogWaitDelay")

	// Initialize Discord settings from SQLite.
	init {
		DiscordUtils.enableDiscordNotifications = SettingsHelper.getBooleanSetting("discord", "enableDiscordNotifications", false)
		if (DiscordUtils.enableDiscordNotifications) {
			try {
				DiscordUtils.discordToken = SettingsHelper.getStringSetting("discord", "discordToken")
				DiscordUtils.discordUserID = SettingsHelper.getStringSetting("discord", "discordUserID").toString()
			} catch (e: Exception) {
				Log.w(TAG, "Failed to read Discord settings: ${e.message}")
				DiscordUtils.enableDiscordNotifications = false
			}
		}
	}

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
    val task: Task = when (scenario) {
        "URA Finale" -> UraFinale(this)
        "Unity Cup" -> UnityCup(this)
        "Plugins" -> Plugins(this)
        else -> throw InterruptedException("Invalid scenario: $scenario")
    }

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////

    // Tracks the number of connection error retries. After hitting max, bot stops.
    internal val maxConnectionErrorRetryAttempts: Int = 3
    internal var connectionErrorRetryAttempts: Int = 0
    internal var lastConnectionErrorRetryTimeMs: Long = 0
    internal val connectionErrorRetryCooldownTimeMs: Long = 10000 // 10 seconds

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	// Helper functions for bot interaction.

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
	 * Wait for the game to finish loading. Note that this function is responsible for dictating how fast the bot will run so adjusting this should be done with caution.
	 */
	fun waitForLoading() {
        var loadingCounter = 0
		while (checkLoading(suppressLogging = loadingCounter % 10 != 0)) {
			// Avoid an infinite loop by setting the flag to true.
			wait(waitDelay, skipWaitingForLoading = true)
            loadingCounter++
            if (loadingCounter >= 20) {
                loadingCounter = 0
            }
		}
	}

	/**
	 * Find and tap the specified image.
	 *
	 * @param imageName Name of the button image file in the /assets/images/ folder.
     * @param sourceBitmap The source bitmap to find the image on. This is optional and defaults to null which will fetch its own source bitmap.
	 * @param tries Number of tries to find the specified button. Defaults to 3.
	 * @param region Specify the region consisting of (x, y, width, height) of the source screenshot to template match. Defaults to (0, 0, 0, 0) which is equivalent to searching the full image.
	 * @param taps Specify the number of taps on the specified image. Defaults to 1.
	 * @param suppressError Whether or not to suppress saving error messages to the log in failing to find the button. Defaults to false.
	 * @return True if the button was found and clicked. False otherwise.
	 */
	fun findAndTapImage(imageName: String, sourceBitmap: Bitmap? = null, tries: Int = 3, region: IntArray = intArrayOf(0, 0, 0, 0), taps: Int = 1, suppressError: Boolean = false): Boolean {
		if (debugMode) {
			MessageLog.d(TAG, "Now attempting to find and click the \"$imageName\" button.")
		}

		val tempLocation: Point? = if (sourceBitmap == null) {
            imageUtils.findImage(imageName, tries = tries, region = region, suppressError = suppressError).first
        } else {
            imageUtils.findImageWithBitmap(imageName, sourceBitmap, region = region, suppressError = suppressError)
        }

		return if (tempLocation != null) {
			Log.d(TAG, "Found and going to tap: $imageName")
			tap(tempLocation.x, tempLocation.y, imageName, taps = taps)
			true
		} else {
			false
		}
	}

	/**
	 * Performs a tap on the screen at the coordinates and then will wait until the game processes the server request and gets a response back.
	 *
	 * @param x The x-coordinate.
	 * @param y The y-coordinate.
	 * @param imageName The template image name to use for tap location randomization.
	 * @param taps The number of taps.
	 * @param ignoreWaiting Flag to ignore checking if the game is busy loading.
	 */
	fun tap(x: Double, y: Double, imageName: String? = null, taps: Int = 1, ignoreWaiting: Boolean = false) {
		// Perform the tap.
		gestureUtils.tap(x, y, imageName, taps = taps)

		if (!ignoreWaiting) {
			// Now check if the game is waiting for a server response from the tap and wait if necessary.
			wait(0.20)
			waitForLoading()
		}
	}

    /**
     * Checks if the bot is at a "Now Loading..." screen or if the game is awaiting for a server response. This may cause significant delays in normal bot processes.
     *
     * @param suppressLogging Whether or not to suppress logging for this function. Defaults to false.
     * @return True if the game is still loading or is awaiting for a server response. Otherwise, false.
     */
    fun checkLoading(suppressLogging: Boolean = false): Boolean {
        if (!suppressLogging) MessageLog.i(TAG, "[LOADING] Now checking if the game is still loading...")
        val sourceBitmap = imageUtils.getSourceBitmap()
        return if (LabelConnecting.check(imageUtils, sourceBitmap = sourceBitmap)) {
            if (!suppressLogging) MessageLog.i(TAG, "[LOADING] Detected that the game is awaiting a response from the server from the \"Connecting\" text at the top of the screen. Waiting...")
            true
        } else if (LabelNowLoading.check(imageUtils, sourceBitmap = sourceBitmap)) {
            if (!suppressLogging) MessageLog.i(TAG, "[LOADING] Detected that the game is still loading from the \"Now Loading\" text at the bottom of the screen. Waiting...")
            true
        } else {
            false
        }
    }

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Bot will begin automation here.
	 *
	 * @return True if all automation goals have been met. False otherwise.
	 */
	fun start(): Boolean {
        MessageLog.i(TAG, "Started at ${MessageLog.getSystemTimeString()}.")
		val startTime: Long = System.currentTimeMillis()

		// Print current app settings at the start of the run.
		try {
			val formattedSettingsString = SettingsHelper.getStringSetting("misc", "formattedSettingsString")
			MessageLog.i(TAG, "\n[SETTINGS] Current Bot Configuration:")
			MessageLog.i(TAG, "=====================================")
			formattedSettingsString.split("\n").forEach { line ->
				if (line.isNotEmpty()) {
					MessageLog.i(TAG, line)
				}
			}
			MessageLog.i(TAG, "=====================================\n")
		} catch (e: Exception) {
			MessageLog.w(TAG, "Failed to load formatted settings from SQLite: ${e.message}")
			MessageLog.i(TAG, "Using fallback settings display...")
			// Fallback to basic settings display if formatted string is not available.
			MessageLog.i(TAG, "Scenario: $scenario")
			MessageLog.i(TAG, "Debug Mode: $debugMode")
		}

		// Print device and version information.
		MessageLog.i(TAG, "Device Information: ${SharedData.displayWidth}x${SharedData.displayHeight}, DPI ${SharedData.displayDPI}")
		if (SharedData.displayWidth != 1080) MessageLog.w(TAG, "⚠️ Bot performance will be severely degraded since display width is not 1080p unless an appropriate scale is set for your device.")
		if (debugMode) MessageLog.w(TAG, "⚠️ Debug Mode is enabled. All bot operations will be significantly slower as a result.")
		if (SettingsHelper.getStringSetting("debug", "templateMatchCustomScale").toDouble() != 1.0) MessageLog.i(TAG, "Manual scale has been set to ${SettingsHelper.getStringSetting("debug", "templateMatchCustomScale").toDouble()}")
		MessageLog.w(TAG, "⚠️ Note that certain Android notification styles (like banners) are big enough that they cover the area that contains the Mood which will interfere with mood recovery logic in the beginning.")
		val packageInfo = myContext.packageManager.getPackageInfo(myContext.packageName, 0)
		MessageLog.i(TAG, "Bot version: ${packageInfo.versionName} (${packageInfo.versionCode})\n\n")

		// Start debug tests here if enabled. Otherwise, proceed with regular bot operations.
        if (!task.startTests()) {
            // Send Discord notification that the run has started.
			if (DiscordUtils.enableDiscordNotifications) {
				val enableRemoteLogViewer = SettingsHelper.getBooleanSetting("debug", "enableRemoteLogViewer", false)
				var logViewerString = ""
				if (enableRemoteLogViewer) {
                    // Notify the user that the Remote Log Viewer is enabled and is viewable at the indicated address.
					val port = SettingsHelper.getIntSetting("debug", "remoteLogViewerPort", 9000)
					val ipAddress = com.steve1316.uma_android_automation.utils.LogStreamServer.getDeviceIpAddress(myContext)
					val finalIpAddress = if (ipAddress == "10.0.2.15") "localhost" else ipAddress
					logViewerString = "Remote Log Viewer is enabled at http://$finalIpAddress:$port"
				}
				DiscordUtils.queue.add("```diff\n+ ${MessageLog.getSystemTimeString()} Bot run started! Scenario: $scenario```$logViewerString")
			}
			wait(5.0)
            task.start()
		}

		MessageLog.i(TAG, "Total runtime of ${MessageLog.formatElapsedTime(startTime, System.currentTimeMillis())} and stopped at ${MessageLog.getSystemTimeString()}.")

        // Wait to make sure Discord webhook message queue gets fully processed before terminating Bot Thread.
        if (DiscordUtils.enableDiscordNotifications) {
            wait(1.0, skipWaitingForLoading = true)
        }

		return true
	}
}