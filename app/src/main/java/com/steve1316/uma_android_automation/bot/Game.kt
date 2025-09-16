package com.steve1316.uma_android_automation.bot

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.campaigns.AoHaru
import com.steve1316.uma_android_automation.utils.BotService
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.utils.MediaProjectionService
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.MyAccessibilityService
import com.steve1316.uma_android_automation.utils.SettingsPrinter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.opencv.core.Point
import java.text.DecimalFormat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.intArrayOf

/**
 * Main driver for bot activity and navigation.
 */
class Game(val myContext: Context) {
	private val tag: String = "[${MainActivity.loggerTag}]Game"
	var notificationMessage: String = ""
	private val decimalFormat = DecimalFormat("#.##")
	val imageUtils: ImageUtils = ImageUtils(myContext, this)
	val gestureUtils: MyAccessibilityService = MyAccessibilityService.getInstance()
	private val textDetection: TextDetection = TextDetection(this, imageUtils)

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	// SharedPreferences
	private var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(myContext)
	private val campaign: String = sharedPreferences.getString("campaign", "")!!
	private val strategy: String = sharedPreferences.getString("strategy", "")!!
	private val strategyImageName: String = when (strategy) {
		"Front Runner" -> "strategy_front"
		"Pace Chaser" -> "strategy_pace"
		"Late Surger" -> "strategy_late"
		"End Closer" -> "strategy_end"
		else -> "default"
	}

	private val debugMode: Boolean = sharedPreferences.getBoolean("debugMode", false)

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	// Training
	private val trainings: List<String> = listOf("Speed", "Stamina", "Power", "Guts", "Wit")
	private val trainingMap: MutableMap<String, Training> = mutableMapOf()
	private var currentStatsMap: MutableMap<String, Int> = mutableMapOf(
		"Speed" to 0,
		"Stamina" to 0,
		"Power" to 0,
		"Guts" to 0,
		"Wit" to 0
	)

	// Track current conditions (good and bad)
	private var currentConditions: MutableList<String> = mutableListOf()
	private var currentFans: Int = 0
	private var currentDistance: String = "Medium" // Default to Medium distance
	private val blacklist: List<String> = sharedPreferences.getStringSet("trainingBlacklist", setOf())!!.toList()
	private var statPrioritization: List<String> = sharedPreferences.getString("statPrioritization", "Speed|Stamina|Power|Guts|Wit")!!.split("|")
	private val enablePrioritizeEnergyOptions: Boolean = sharedPreferences.getBoolean("enablePrioritizeEnergyOptions", false)
	private val maximumFailureChance: Int = sharedPreferences.getInt("maximumFailureChance", 15)
	private val disableTrainingOnMaxedStat: Boolean = sharedPreferences.getBoolean("disableTrainingOnMaxedStat", true)
	private val focusOnSparkStatTarget: Boolean = sharedPreferences.getBoolean("focusOnSparkStatTarget", false)
	private val statTargetsByDistance: MutableMap<String, IntArray> = mutableMapOf(
		"Sprint" to intArrayOf(0, 0, 0, 0, 0),
		"Mile" to intArrayOf(0, 0, 0, 0, 0),
		"Medium" to intArrayOf(0, 0, 0, 0, 0),
		"Long" to intArrayOf(0, 0, 0, 0, 0)
	)
	private var preferredDistance: String = ""
	private var firstTrainingCheck = true
	private val currentStatCap = 1200
	private val historicalTrainingCounts: MutableMap<String, Int> = mutableMapOf()

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	// Racing
	private val enableFarmingFans = sharedPreferences.getBoolean("enableFarmingFans", false)
	private val daysToRunExtraRaces: Int = sharedPreferences.getInt("daysToRunExtraRaces", 4)
	private val disableRaceRetries: Boolean = sharedPreferences.getBoolean("disableRaceRetries", false)
	val enableForceRacing = sharedPreferences.getBoolean("enableForceRacing", false)
	private var raceRetries = 3
	private var raceRepeatWarningCheck = false
	var encounteredRacingPopup = false
	var skipRacing = false
	var strategySelected = false

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	// Stops
	val enableSkillPointCheck: Boolean = sharedPreferences.getBoolean("enableSkillPointCheck", false)
	val skillPointsRequired: Int = sharedPreferences.getInt("skillPointCheck", 750)
	private val enablePopupCheck: Boolean = sharedPreferences.getBoolean("enablePopupCheck", false)
	private val enableStopOnMandatoryRace: Boolean = sharedPreferences.getBoolean("enableStopOnMandatoryRace", false)
	var detectedMandatoryRaceCheck = false

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	// Misc
	private var currentDate: Date = Date(1, "Early", 1, 1)
	private var inheritancesDone = 0
	private val startTime: Long = System.currentTimeMillis()

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////

	data class Training(
		val name: String,
		val statGains: IntArray,
		val failureChance: Int,
		val relationshipBars: ArrayList<ImageUtils.BarFillResult>
	) {
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Training

			if (failureChance != other.failureChance) return false
			if (name != other.name) return false
			if (!statGains.contentEquals(other.statGains)) return false
			if (relationshipBars != other.relationshipBars) return false

			return true
		}

		override fun hashCode(): Int {
			var result = failureChance
			result = 31 * result + name.hashCode()
			result = 31 * result + statGains.contentHashCode()
			result = 31 * result + relationshipBars.hashCode()
			return result
		}
	}

	data class Date(
		val year: Int,
		val phase: String,
		val month: Int,
		val turnNumber: Int
	)

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////

	/**
	 * Sets up stat targets for different race distances by reading values from SharedPreferences. These targets are used to determine training priorities based on the expected race distance.
	 */
	private fun setStatTargetsByDistances() {
		val sprintSpeedTarget = sharedPreferences.getInt("trainingSprintStatTarget_speedStatTarget", 900)
		val sprintStaminaTarget = sharedPreferences.getInt("trainingSprintStatTarget_staminaStatTarget", 300)
		val sprintPowerTarget = sharedPreferences.getInt("trainingSprintStatTarget_powerStatTarget", 600)
		val sprintGutsTarget = sharedPreferences.getInt("trainingSprintStatTarget_gutsStatTarget", 300)
		val sprintWitTarget = sharedPreferences.getInt("trainingSprintStatTarget_witStatTarget", 300)

		val mileSpeedTarget = sharedPreferences.getInt("trainingMileStatTarget_speedStatTarget", 900)
		val mileStaminaTarget = sharedPreferences.getInt("trainingMileStatTarget_staminaStatTarget", 300)
		val milePowerTarget = sharedPreferences.getInt("trainingMileStatTarget_powerStatTarget", 600)
		val mileGutsTarget = sharedPreferences.getInt("trainingMileStatTarget_gutsStatTarget", 300)
		val mileWitTarget = sharedPreferences.getInt("trainingMileStatTarget_witStatTarget", 300)

		val mediumSpeedTarget = sharedPreferences.getInt("trainingMediumStatTarget_speedStatTarget", 800)
		val mediumStaminaTarget = sharedPreferences.getInt("trainingMediumStatTarget_staminaStatTarget", 450)
		val mediumPowerTarget = sharedPreferences.getInt("trainingMediumStatTarget_powerStatTarget", 550)
		val mediumGutsTarget = sharedPreferences.getInt("trainingMediumStatTarget_gutsStatTarget", 300)
		val mediumWitTarget = sharedPreferences.getInt("trainingMediumStatTarget_witStatTarget", 300)

		val longSpeedTarget = sharedPreferences.getInt("trainingLongStatTarget_speedStatTarget", 700)
		val longStaminaTarget = sharedPreferences.getInt("trainingLongStatTarget_staminaStatTarget", 600)
		val longPowerTarget = sharedPreferences.getInt("trainingLongStatTarget_powerStatTarget", 450)
		val longGutsTarget = sharedPreferences.getInt("trainingLongStatTarget_gutsStatTarget", 300)
		val longWitTarget = sharedPreferences.getInt("trainingLongStatTarget_witStatTarget", 300)

		// Set the stat targets for each distance type.
		// Order: Speed, Stamina, Power, Guts, Wit
		// If no custom targets are set, use optimal defaults from the guide
		statTargetsByDistance["Sprint"] = if (sprintSpeedTarget == 0)
			intArrayOf(1200, 400, 900, 400, 600) else
			intArrayOf(sprintSpeedTarget, sprintStaminaTarget, sprintPowerTarget, sprintGutsTarget, sprintWitTarget)

		statTargetsByDistance["Mile"] = if (mileSpeedTarget == 0)
			intArrayOf(1100, 500, 800, 500, 600) else
			intArrayOf(mileSpeedTarget, mileStaminaTarget, milePowerTarget, mileGutsTarget, mileWitTarget)

		statTargetsByDistance["Medium"] = if (mediumSpeedTarget == 0)
			intArrayOf(1000, 700, 700, 500, 600) else
			intArrayOf(mediumSpeedTarget, mediumStaminaTarget, mediumPowerTarget, mediumGutsTarget, mediumWitTarget)

		statTargetsByDistance["Long"] = if (longSpeedTarget == 0)
			intArrayOf(800, 1000, 600, 600, 600) else
			intArrayOf(longSpeedTarget, longStaminaTarget, longPowerTarget, longGutsTarget, longWitTarget)
	}

	/**
	 * Returns a formatted string of the elapsed time since the bot started as HH:MM:SS format.
	 *
	 * Source is from https://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format/9027379
	 *
	 * @return String of HH:MM:SS format of the elapsed time.
	 */
	@SuppressLint("DefaultLocale")
	private fun printTime(): String {
		val elapsedMillis: Long = System.currentTimeMillis() - startTime

		return String.format(
			"%02d:%02d:%02d",
			TimeUnit.MILLISECONDS.toHours(elapsedMillis),
			TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(elapsedMillis)),
			TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedMillis))
		)
	}

	/**
	 * Print the specified message to debug console and then saves the message to the log.
	 *
	 * @param message Message to be saved.
	 * @param tag Distinguishes between messages for where they came from. Defaults to Game's TAG.
	 * @param isError Flag to determine whether to display log message in console as debug or error.
	 * @param isOption Flag to determine whether to append a newline right after the time in the string.
	 */
	fun printToLog(message: String, tag: String = this.tag, isError: Boolean = false, isOption: Boolean = false) {
		if (!isError) {
			Log.d(tag, message)
		} else {
			Log.e(tag, message)
		}

		// Remove the newline prefix if needed and place it where it should be.
		if (message.startsWith("\n")) {
			val newMessage = message.removePrefix("\n")
			if (isOption) {
				MessageLog.addMessage("\n" + printTime() + "\n" + newMessage)
			} else {
				MessageLog.addMessage("\n" + printTime() + " " + newMessage)
			}
		} else {
			if (isOption) {
				MessageLog.addMessage(printTime() + "\n" + message)
			} else {
				MessageLog.addMessage(printTime() + " " + message)
			}
		}
	}

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
	 * Find and tap the specified image.
	 *
	 * @param imageName Name of the button image file in the /assets/images/ folder.
	 * @param tries Number of tries to find the specified button. Defaults to 3.
	 * @param region Specify the region consisting of (x, y, width, height) of the source screenshot to template match. Defaults to (0, 0, 0, 0) which is equivalent to searching the full image.
	 * @param taps Specify the number of taps on the specified image. Defaults to 1.
	 * @param suppressError Whether or not to suppress saving error messages to the log in failing to find the button. Defaults to false.
	 * @return True if the button was found and clicked. False otherwise.
	 */
	fun findAndTapImage(imageName: String, tries: Int = 3, region: IntArray = intArrayOf(0, 0, 0, 0), taps: Int = 1, suppressError: Boolean = false): Boolean {
		if (debugMode) {
			printToLog("[DEBUG] Now attempting to find and click the \"$imageName\" button.")
		}

		val tempLocation: Point? = imageUtils.findImage(imageName, tries = tries, region = region, suppressError = suppressError).first

		return if (tempLocation != null) {
			Log.d(tag, "Found and going to tap: $imageName")
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
	fun tap(x: Double, y: Double, imageName: String, taps: Int = 1, ignoreWaiting: Boolean = false) {
		// Perform the tap.
		gestureUtils.tap(x, y, imageName, taps = taps)

		if (!ignoreWaiting) {
			// Now check if the game is waiting for a server response from the tap and wait if necessary.
			wait(0.20)
			waitForLoading()
		}
	}

	/**
	 * Handles the test to perform template matching to determine what the best scale will be for the device.
	 */
	fun startTemplateMatchingTest() {
		printToLog("\n[TEST] Now beginning basic template match test on the Home screen.")
		printToLog("[TEST] Template match confidence setting will be overridden for the test.\n")
		val results = imageUtils.startTemplateMatchingTest()
		printToLog("\n[TEST] Basic template match test complete.")

		// Print all scale/confidence combinations that worked for each template.
		for ((templateName, scaleConfidenceResults) in results) {
			if (scaleConfidenceResults.isNotEmpty()) {
				printToLog("[TEST] All working scale/confidence combinations for $templateName:")
				for (result in scaleConfidenceResults) {
					printToLog("[TEST]	Scale: ${result.scale}, Confidence: ${result.confidence}")
				}
			} else {
				printToLog("[WARNING] No working scale/confidence combinations found for $templateName")
			}
		}

		// Then print the median scales and confidences.
		val medianScales = mutableListOf<Double>()
		val medianConfidences = mutableListOf<Double>()
		for ((templateName, scaleConfidenceResults) in results) {
			if (scaleConfidenceResults.isNotEmpty()) {
				val sortedScales = scaleConfidenceResults.map { it.scale }.sorted()
				val sortedConfidences = scaleConfidenceResults.map { it.confidence }.sorted()
				val medianScale = sortedScales[sortedScales.size / 2]
				val medianConfidence = sortedConfidences[sortedConfidences.size / 2]
				medianScales.add(medianScale)
				medianConfidences.add(medianConfidence)
				printToLog("[TEST] Median scale for $templateName: $medianScale")
				printToLog("[TEST] Median confidence for $templateName: $medianConfidence")
			}
		}

		if (medianScales.isNotEmpty()) {
			printToLog("\n[TEST] The following are the recommended scales to set (pick one as a whole number value): $medianScales.")
			printToLog("[TEST] The following are the recommended confidences to set (pick one as a whole number value): $medianConfidences.")
		} else {
			printToLog("\n[ERROR] No median scale/confidence can be found.", isError = true)
		}
	}

	/**
	 * Handles the test to perform OCR on the training failure chance for the current training on display.
	 */
	fun startSingleTrainingFailureOCRTest() {
		printToLog("\n[TEST] Now beginning Single Training Failure OCR test on the Training screen for the current training on display.")
		printToLog("[TEST] Note that this test is dependent on having the correct scale.")
		val failureChance: Int = imageUtils.findTrainingFailureChance()
		if (failureChance == -1) {
			printToLog("[ERROR] Training Failure Chance detection failed.", isError = true)
		} else {
			printToLog("[TEST] Training Failure Chance: $failureChance")
		}
	}

	/**
	 * Handles the test to perform OCR on training failure chances for all 5 of the trainings on display.
	 */
	fun startComprehensiveTrainingFailureOCRTest() {
		printToLog("\n[TEST] Now beginning Comprehensive Training Failure OCR test on the Training screen for all 5 trainings on display.")
		printToLog("[TEST] Note that this test is dependent on having the correct scale.")
		analyzeTrainings(test = true)
		printTrainingMap()
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Helper functions to be shared amongst the various Campaigns.

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Functions to check what screen the bot is at.

	/**
	 * Checks if the bot is at the Main screen or the screen with available options to undertake.
	 * This will also make sure that the Main screen does not contain the option to select a race.
	 *
	 * @return True if the bot is at the Main screen. Otherwise false.
	 */
	fun checkMainScreen(): Boolean {
		printToLog("[INFO] Checking if the bot is sitting at the Main screen.")
		return if (imageUtils.findImage("tazuna", tries = 1, region = imageUtils.regionTopHalf).first != null &&
			imageUtils.findImage("race_select_mandatory", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true).first == null) {
			printToLog("\n[INFO] Current bot location is at Main screen.")

			// Perform updates here if necessary.
			updateDate()
			if (preferredDistance == "") updatePreferredDistance()
			true
		} else if (!enablePopupCheck && imageUtils.findImage("cancel", tries = 1, region = imageUtils.regionBottomHalf).first != null &&
			imageUtils.findImage("race_confirm", tries = 1, region = imageUtils.regionBottomHalf).first != null) {
			// This popup is most likely the insufficient fans popup. Force an extra race to catch up on the required fans.
			printToLog("[INFO] There is a possible insufficient fans or maiden race popup.")
			encounteredRacingPopup = true
			skipRacing = false
			true
		} else {
			false
		}
	}

	/**
	 * Checks if the bot is at the Training Event screen with an active event with options to select on screen.
	 *
	 * @return True if the bot is at the Training Event screen. Otherwise false.
	 */
	fun checkTrainingEventScreen(): Boolean {
		printToLog("[INFO] Checking if the bot is sitting on the Training Event screen.")
		return if (imageUtils.findImage("training_event_active", tries = 1, region = imageUtils.regionMiddle).first != null) {
			printToLog("\n[INFO] Current bot location is at Training Event screen.")
			true
		} else {
			false
		}
	}

	/**
	 * Checks if the bot is at the preparation screen with a mandatory race needing to be completed.
	 *
	 * @return True if the bot is at the Main screen with a mandatory race. Otherwise false.
	 */
	fun checkMandatoryRacePrepScreen(): Boolean {
		printToLog("[INFO] Checking if the bot is sitting on the Race Preparation screen.")
		return if (imageUtils.findImage("race_select_mandatory", tries = 1, region = imageUtils.regionBottomHalf).first != null) {
			printToLog("\n[INFO] Current bot location is at the preparation screen with a mandatory race ready to be completed.")
			true
		} else if (imageUtils.findImage("race_select_mandatory_goal", tries = 1, region = imageUtils.regionMiddle).first != null) {
			// Most likely the user started the bot here so a delay will need to be placed to allow the start banner of the Service to disappear.
			wait(2.0)
			printToLog("\n[INFO] Current bot location is at the Race Selection screen with a mandatory race needing to be selected.")
			// Walk back to the preparation screen.
			findAndTapImage("back", tries = 1, region = imageUtils.regionBottomHalf)
			wait(1.0)
			true
		} else {
			false
		}
	}

	/**
	 * Checks if the bot is at the Racing screen waiting to be skipped or done manually.
	 *
	 * @return True if the bot is at the Racing screen. Otherwise, false.
	 */
	fun checkRacingScreen(): Boolean {
		printToLog("[INFO] Checking if the bot is sitting on the Racing screen.")
		return if (imageUtils.findImage("race_change_strategy", tries = 1, region = imageUtils.regionBottomHalf).first != null) {
			printToLog("\n[INFO] Current bot location is at the Racing screen waiting to be skipped or done manually.")
			true
		} else {
			false
		}
	}

	/**
	 * Checks if the day number is odd to be eligible to run an extra race, excluding Summer where extra racing is not allowed.
	 *
	 * @return True if the day number is odd. Otherwise false.
	 */
	fun checkExtraRaceAvailability(): Boolean {
		val dayNumber = imageUtils.determineDayForExtraRace()
		printToLog("\n[INFO] Current remaining number of days before the next mandatory race: $dayNumber.")

		// If the setting to force racing extra races is enabled, always return true.
		if (enableForceRacing) return true

		return enableFarmingFans && dayNumber % daysToRunExtraRaces == 0 && !raceRepeatWarningCheck &&
				imageUtils.findImage("race_select_extra_locked_uma_finals", tries = 1, region = imageUtils.regionBottomHalf).first == null &&
				imageUtils.findImage("race_select_extra_locked", tries = 1, region = imageUtils.regionBottomHalf).first == null &&
				imageUtils.findImage("recover_energy_summer", tries = 1, region = imageUtils.regionBottomHalf).first == null
	}

	/**
	 * Checks if the bot is at the Ending screen detailing the overall results of the run.
	 *
	 * @return True if the bot is at the Ending screen. Otherwise false.
	 */
	fun checkEndScreen(): Boolean {
		return if (imageUtils.findImage("complete_career", tries = 1, region = imageUtils.regionBottomHalf).first != null) {
			printToLog("\n[END] Bot has reached the End screen.")
			true
		} else {
			false
		}
	}

	/**
	 * Checks if the bot has a injury.
	 *
	 * @return True if the bot has a injury. Otherwise false.
	 */
	fun checkInjury(): Boolean {
		val recoverInjuryLocation = imageUtils.findImage("recover_injury", tries = 1, region = imageUtils.regionBottomHalf).first
		return if (recoverInjuryLocation != null && imageUtils.checkColorAtCoordinates(
				recoverInjuryLocation.x.toInt(),
				recoverInjuryLocation.y.toInt() + 15,
				intArrayOf(151, 105, 243),
				10
			)) {
			if (findAndTapImage("recover_injury", tries = 1, region = imageUtils.regionBottomHalf)) {
				wait(0.3)
				if (imageUtils.confirmLocation("recover_injury", tries = 1, region = imageUtils.regionMiddle)) {
					printToLog("\n[INFO] Injury detected and attempted to heal.")
					true
				} else {
					false
				}
			} else {
				printToLog("\n[WARNING] Injury detected but attempt to rest failed.")
				false
			}
		} else {
			printToLog("\n[INFO] No injury detected.")
			false
		}
	}

	/**
	 * Checks if the bot is at a "Now Loading..." screen or if the game is awaiting for a server response. This may cause significant delays in normal bot processes.
	 *
	 * @return True if the game is still loading or is awaiting for a server response. Otherwise, false.
	 */
	fun checkLoading(): Boolean {
		printToLog("[INFO] Now checking if the game is still loading...")
		return if (imageUtils.findImage("connecting", tries = 1, region = imageUtils.regionTopHalf, suppressError = true).first != null) {
			printToLog("[INFO] Detected that the game is awaiting a response from the server from the \"Connecting\" text at the top of the screen. Waiting...")
			true
		} else if (imageUtils.findImage("now_loading", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true).first != null) {
			printToLog("[INFO] Detected that the game is still loading from the \"Now Loading\" text at the bottom of the screen. Waiting...")
			true
		} else {
			false
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Functions to execute Training by determining failure percentages, overall stat gains and stat weights.

	/**
	 * The entry point for handling Training.
	 */
	fun handleTraining() {
		printToLog("\n[TRAINING] Starting Training process...")

		// Enter the Training screen.
		if (findAndTapImage("training_option", region = imageUtils.regionBottomHalf)) {
			// Acquire the percentages and stat gains for each training.
			wait(0.5)
			analyzeTrainings()

			if (trainingMap.isEmpty()) {
				printToLog("[TRAINING] Backing out of Training and returning on the Main screen.")
				findAndTapImage("back", region = imageUtils.regionBottomHalf)
				wait(1.0)

				if (checkMainScreen()) {
					printToLog("[TRAINING] Will recover energy due to either failure chance was high enough to do so or no failure chances were detected via OCR.")
					recoverEnergy()
				} else {
					printToLog("[ERROR] Could not head back to the Main screen in order to recover energy.")
				}
			} else {
				// Now select the training option with the highest weight.
				executeTraining()

				firstTrainingCheck = false
			}

			raceRepeatWarningCheck = false
			printToLog("\n[TRAINING] Training process completed.")
		} else {
			printToLog("[ERROR] Cannot start the Training process. Moving on...", isError = true)
		}
	}

	/**
	 * Calculates the deficit multiplier based on how far a stat is from its target.
	 * Based on the comprehensive guide:
	 * - Deficit > 300: 3.0x (Critical priority)
	 * - Deficit 200-300: 2.5x (High priority)
	 * - Deficit 100-200: 2.0x (Moderate priority)
	 * - Deficit 50-100: 1.5x (Low priority)
	 * - Deficit < 50: 1.1x (Maintenance)
	 * - Surplus: 0.5x (Diminishing returns)
	 */
	private fun getDeficitMultiplier(deficit: Int): Double {
		return when {
			deficit > 300 -> 3.0
			deficit in 200..300 -> 2.5
			deficit in 100..199 -> 2.0
			deficit in 50..99 -> 1.5
			deficit in 1..49 -> 1.1
			deficit <= 0 -> 0.5  // Surplus - diminishing returns
			else -> 1.0
		}
	}

	/**
	 * Determines if we should prioritize recovery based on multiple factors.
	 * Returns true if recovery (rest or Wit training) should be prioritized.
	 */
	private fun shouldPrioritizeRecovery(avgFailureRate: Double, estimatedEnergy: Double): Boolean {
		// Check multiple conditions for recovery need
		val needsRecovery = when {
			// Critical energy - must recover
			estimatedEnergy < 15 -> {
				printToLog("[RECOVERY] Critical energy level (~${estimatedEnergy.toInt()}%) - MUST recover")
				true
			}
			// Very high failure rates across the board
			avgFailureRate > 40 -> {
				printToLog("[RECOVERY] Very high average failure rate (${avgFailureRate.toInt()}%) - should recover")
				true
			}
			// Low energy and no high-value trainings available
			estimatedEnergy < 30 && trainingMap.values.none {
				it.failureChance <= 15 && (it.relationshipBars.size >= 2 || it.statGains.sum() > 50)
			} -> {
				printToLog("[RECOVERY] Low energy with no high-value safe options - should recover")
				true
			}
			// Consecutive failures detected (if we could track this)
			historicalTrainingCounts.getOrDefault("Rest", 0) == 0 && estimatedEnergy < 25 -> {
				printToLog("[RECOVERY] Haven't rested recently and energy is low - consider recovery")
				true
			}
			else -> false
		}

		return needsRecovery
	}

	/**
	 * Calculates the risk-reward ratio for a training option.
	 * Higher values mean better risk-reward ratio.
	 */
	private fun calculateRiskRewardRatio(training: Training): Double {
		// Calculate total value of the training
		val statValue = training.statGains.sum()
		val relationshipValue = training.relationshipBars.size * 30
		val friendshipBonusValue = training.relationshipBars.count { it.fillPercent >= 80 } * 50
		val totalValue = statValue + relationshipValue + friendshipBonusValue

		// Calculate risk factor (higher failure = higher risk)
		val riskFactor = when {
			training.failureChance <= 5 -> 1.0   // Minimal risk
			training.failureChance <= 10 -> 1.2  // Low risk
			training.failureChance <= 15 -> 1.5  // Moderate risk
			training.failureChance <= 20 -> 2.0  // High risk
			training.failureChance <= 25 -> 3.0  // Very high risk
			else -> 5.0  // Extreme risk
		}

		// Risk-reward ratio: value divided by risk
		val ratio = totalValue / riskFactor

		// Apply phase-based adjustments
		val phaseMultiplier = when {
			// Early game: prioritize relationships even with some risk
			currentDate.year == 1 && training.relationshipBars.isNotEmpty() -> 1.3
			// Late game: prioritize safe stat gains
			currentDate.year == 3 && training.failureChance <= 10 -> 1.2
			else -> 1.0
		}

		return ratio * phaseMultiplier
	}

	/**
	 * Evaluates if Wit training is worth doing over resting.
	 * Considers multiple turns of Wit vs one rest for better decision making.
	 */
	private fun evaluateWitVsRest(witTraining: Training?, estimatedEnergy: Double): Boolean {
		if (witTraining == null) return false

		// Check if summer is approaching (Early June = 2 turns before Late June summer)
		val isSummerApproaching = currentDate.month == 6 && currentDate.phase == "Early"
		if (isSummerApproaching && estimatedEnergy < 70) {
			printToLog("[WIT VS REST] Summer training approaching in 2 turns. Energy low (~${estimatedEnergy.toInt()}%), prioritizing Rest for Lv5 training benefits")
			return false  // Always rest before summer if energy is not high
		}

		// Check if we've already reached Wit stat target
		val currentWit = currentStatsMap.getOrDefault("Wit", 0)
		val witTarget = statTargetsByDistance[preferredDistance]?.getOrNull(4) ?: 600
		val witDeficit = witTarget - currentWit

		// Get the actual Wit stat gain from the training
		val witStatGain = witTraining.statGains.getOrNull(4) ?: 0

		// If we're at or above target, only do Wit if it provides exceptional value
		if (witDeficit <= 0) {
			// We've exceeded the target
			val hasGreatFriendships = witTraining.relationshipBars.count { it.fillPercent >= 80 } >= 2
			val hasLowFailure = witTraining.failureChance <= 10
			val hasExceptionalValue = witTraining.statGains.sum() >= 60 ||
					witTraining.relationshipBars.size >= 3

			if (!hasExceptionalValue && !hasGreatFriendships) {
				printToLog("[WIT VS REST] Wit stat already at target ($currentWit/$witTarget). Rest is better unless exceptional value.")
				return false
			}
			printToLog("[WIT VS REST] Wit at target but training has exceptional value - considering it")
		} else if (witDeficit <= 50) {
			// We're very close to target
			if (witStatGain >= witDeficit * 2) {
				// This would overshoot significantly
				printToLog("[WIT VS REST] Wit training would overshoot target significantly ($currentWit + $witStatGain vs $witTarget)")
				// Only do it if it has other great benefits
				if (witTraining.relationshipBars.count { it.fillPercent >= 80 } < 2 &&
					witTraining.failureChance > 15) {
					return false
				}
			}
		}

		// Calculate the value of Wit training
		val witStatValue = witTraining.statGains.sum()
		val witRelationshipValue = witTraining.relationshipBars.size * 20
		val witFriendshipValue = witTraining.relationshipBars.count { it.fillPercent >= 80 } * 40
		val witEnergyRecovery = 5  // Wit recovers ~5 energy

		// Reduce value if we're already at/near Wit target
		val witTargetMultiplier = when {
			witDeficit <= 0 -> 0.3  // Already exceeded target
			witDeficit <= 50 -> 0.5  // Very close to target
			witDeficit <= 100 -> 0.7  // Close to target
			else -> 1.0  // Still need Wit
		}

		// Total value per Wit training (adjusted for target proximity)
		val witValuePerTurn = (witStatValue * witTargetMultiplier + witRelationshipValue + witFriendshipValue).toInt()

		// Calculate how many Wit trainings would equal one rest
		// Rest recovers ~40 energy, Wit recovers ~5, so 8 Wit trainings = 1 rest in energy
		// But we need to consider failure risk over multiple turns
		val witSuccessRate = (100 - witTraining.failureChance) / 100.0

		// Expected value over multiple turns (considering failure chance)
		val expectedWitValue = witValuePerTurn * witSuccessRate

		// Rest value: Full energy recovery allows for better trainings later
		// Estimate the value of future trainings with full energy
		val restValue = when {
			isSummerApproaching && estimatedEnergy < 70 -> 200  // Pre-summer rest is extremely valuable
			estimatedEnergy < 20 -> 150  // Critical energy - rest is very valuable
			estimatedEnergy < 30 -> 100  // Low energy - rest is valuable
			estimatedEnergy < 40 -> 50   // Moderate energy - rest has some value
			else -> 30  // Good energy - rest is less valuable
		}

		// Decision factors
		val shouldDoWit = when {
			// Never do Wit if failure is too high
			witTraining.failureChance > 40 -> false

			// Always rest if energy is critical and Wit has no friends
			estimatedEnergy < 15 && witTraining.relationshipBars.isEmpty() -> false

			// Do Wit if it has multiple high-value friendships
			witTraining.relationshipBars.count { it.fillPercent >= 80 } >= 2 && witTraining.failureChance <= 25 -> true

			// Do Wit if good stats and low failure
			witStatValue >= 50 && witTraining.failureChance <= 15 -> true

			// Do Wit if it has good overall value and acceptable risk
			expectedWitValue > 40 && witTraining.failureChance <= 20 -> true

			// Rest if Wit has minimal value
			witValuePerTurn < 20 -> false

			// Compare expected values
			else -> expectedWitValue * 3 > restValue  // 3 Wit turns vs 1 rest
		}

		if (shouldDoWit) {
			printToLog("[WIT VS REST] Wit training is worth doing: ${witValuePerTurn} value/turn, ${witTraining.failureChance}% failure")
			printToLog("[WIT VS REST] Current Wit: $currentWit/$witTarget (deficit: $witDeficit)")
		} else {
			printToLog("[WIT VS REST] Rest is better: Wit only provides ${witValuePerTurn} value at ${witTraining.failureChance}% failure")
			if (isSummerApproaching && estimatedEnergy < 70) {
				printToLog("[WIT VS REST] Pre-summer preparation: Resting to maximize Lv5 training benefits")
			} else if (witDeficit <= 0) {
				printToLog("[WIT VS REST] Wit target already reached: $currentWit/$witTarget")
			}
		}

		return shouldDoWit
	}

	/**
	 * Analyze all 5 Trainings for their details including stat gains, relationship bars, etc.
	 * Now checks ALL trainings first to make better decisions, and considers Wit training for energy recovery.
	 *
	 * @param test Flag that forces the failure chance through even if it is not in the acceptable range for testing purposes.
	 */
	private fun analyzeTrainings(test: Boolean = false) {
		printToLog("\n[TRAINING] Now starting process to analyze all 5 Trainings.")

		// Acquire the position of the speed stat text.
		val (speedStatTextLocation, _) = if (campaign == "Ao Haru") {
			imageUtils.findImage("aoharu_stat_speed", tries = 1, region = imageUtils.regionBottomHalf)
		} else {
			imageUtils.findImage("stat_speed", tries = 1, region = imageUtils.regionBottomHalf)
		}

		if (speedStatTextLocation != null) {
			// Start by selecting Speed training
			if (!imageUtils.confirmLocation("speed_training", tries = 1, region = imageUtils.regionTopHalf, suppressError = true)) {
				findAndTapImage("training_speed", region = imageUtils.regionBottomHalf)
				wait(0.5)
			}

			val initialFailureChance: Int = imageUtils.findTrainingFailureChance()
			if (initialFailureChance == -1) {
				printToLog("[WARNING] Skipping training due to not being able to confirm whether or not the bot is at the Training screen.")
				return
			}

			// Early exit if initial failure is very high - likely all trainings will be high
			if (!test && initialFailureChance > 50) {
				printToLog("[TRAINING] Initial failure chance is very high (${initialFailureChance}%). All trainings likely have high failure.")
				printToLog("[TRAINING] Going to rest instead of checking all trainings.")
				trainingMap.clear()  // Clear map to trigger rest
				return
			}

			// Analyze all trainings once to make informed decisions
			printToLog("[TRAINING] Checking all trainings to find the best option...")

			// Check all trainings to build a complete picture
			if (test || true) {  // Always check to make informed decisions
				var highFailureCount = 0  // Track consecutive high failures

				// Iterate through every training that is not blacklisted.
				trainings.forEachIndexed { index, training ->
					if (blacklist.getOrElse(index) { "" } == training) {
						printToLog("[TRAINING] Skipping $training training due to being blacklisted.")
						return@forEachIndexed
					}

					// Select the Training to make it active except Speed Training since that is already selected at the start.
					val newX: Double = when (training) {
						"Stamina" -> {
							280.0
						}
						"Power" -> {
							402.0
						}
						"Guts" -> {
							591.0
						}
						"Wit" -> {
							779.0
						}
						else -> {
							0.0
						}
					}

					if (newX != 0.0) {
						if (imageUtils.isTablet) {
							if (training == "Stamina") {
								tap(
									speedStatTextLocation.x + imageUtils.relWidth((newX * 1.05).toInt()),
									speedStatTextLocation.y + imageUtils.relHeight((319 * 1.50).toInt()),
									"training_option_circular",
									ignoreWaiting = true
								)
							} else {
								tap(
									speedStatTextLocation.x + imageUtils.relWidth((newX * 1.36).toInt()),
									speedStatTextLocation.y + imageUtils.relHeight((319 * 1.50).toInt()),
									"training_option_circular",
									ignoreWaiting = true
								)
							}
						} else {
							tap(
								speedStatTextLocation.x + imageUtils.relWidth(newX.toInt()),
								speedStatTextLocation.y + imageUtils.relHeight(319),
								"training_option_circular",
								ignoreWaiting = true
							)
						}
					}

					// Update the object in the training map.
					// Use CountDownLatch to run the 3 operations in parallel to cut down on processing time.
					val latch = CountDownLatch(3)

					// Variables to store results from parallel threads.
					var statGains: IntArray = intArrayOf()
					var failureChance: Int = -1
					var relationshipBars: ArrayList<ImageUtils.BarFillResult> = arrayListOf()

					// Get the Points and source Bitmap beforehand before starting the threads to make them safe for parallel processing.
					val (skillPointsLocation, sourceBitmap) = imageUtils.findImage("skill_points", tries = 1, region = imageUtils.regionMiddle)
					val (trainingSelectionLocation, _) = imageUtils.findImage("training_failure_chance", tries = 1, region = imageUtils.regionBottomHalf)

					// Thread 1: Determine stat gains.
					Thread {
						try {
							statGains = imageUtils.determineStatGainFromTraining(training, sourceBitmap, skillPointsLocation!!)
						} catch (e: Exception) {
							printToLog("[ERROR] Error in determineStatGainFromTraining: ${e.stackTraceToString()}", isError = true)
							statGains = intArrayOf(0, 0, 0, 0, 0)
						} finally {
							latch.countDown()
						}
					}.start()

					// Thread 2: Find failure chance.
					Thread {
						try {
							failureChance = imageUtils.findTrainingFailureChance(sourceBitmap, trainingSelectionLocation!!)
						} catch (e: Exception) {
							printToLog("[ERROR] Error in findTrainingFailureChance: ${e.stackTraceToString()}", isError = true)
							failureChance = -1
						} finally {
							latch.countDown()
						}
					}.start()

					// Thread 3: Analyze relationship bars.
					Thread {
						try {
							relationshipBars = imageUtils.analyzeRelationshipBars(sourceBitmap)
						} catch (e: Exception) {
							printToLog("[ERROR] Error in analyzeRelationshipBars: ${e.stackTraceToString()}", isError = true)
							relationshipBars = arrayListOf()
						} finally {
							latch.countDown()
						}
					}.start()

					// Wait for all threads to complete.
					try {
						latch.await(10, TimeUnit.SECONDS)
					} catch (_: InterruptedException) {
						printToLog("[ERROR] Parallel training analysis timed out", isError = true)
					}

					val newTraining = Training(
						name = training,
						statGains = statGains,
						failureChance = failureChance,
						relationshipBars = relationshipBars
					)
					trainingMap.put(training, newTraining)

					// Track high failures and potentially skip remaining checks
					if (failureChance > 40) {
						highFailureCount++
						if (highFailureCount >= 3 && training != "Wit") {
							printToLog("[TRAINING] Found 3+ consecutive trainings with >40% failure. Skipping remaining checks.")
							// Still need to check Wit training for recovery option
							if (index < trainings.size - 1 && trainings[trainings.size - 1] == "Wit") {
								// Jump to Wit training directly
								printToLog("[TRAINING] Checking Wit training for recovery option...")
								// Will check Wit in the next iteration
							} else {
								return@forEachIndexed
							}
						}
					} else {
						highFailureCount = 0  // Reset counter if we find acceptable training
					}
				}

				// After analyzing all trainings, decide what to do
				printToLog("[TRAINING] Process to analyze all 5 Trainings complete.")

				// Count friendship trainings available
				val friendshipTrainingCount = trainingMap.values.count { training ->
					training.relationshipBars.any { bar -> bar.fillPercent >= 80 }
				}

				// Calculate average failure rate to understand energy state
				val avgFailureRate = trainingMap.values
					.filter { it.failureChance >= 0 }
					.map { it.failureChance }
					.average()

				// Estimate current energy based on failure rates
				// Formula: Energy ≈ 50 - (FailureRate * 2)
				val estimatedEnergy = (50 - avgFailureRate * 2).coerceIn(0.0, 100.0)
				printToLog("[TRAINING] Estimated energy: ${estimatedEnergy.toInt()}% (avg failure: ${avgFailureRate.toInt()}%)")

				// Calculate the value of the best friendship training
				val bestFriendshipValue = trainingMap.values
					.filter { training -> training.relationshipBars.any { it.fillPercent >= 80 } }
					.maxOfOrNull { training ->
						val friendCount = training.relationshipBars.count { it.fillPercent >= 80 }
						val statValue = training.statGains.sum()
						friendCount * 50 + statValue  // Rough value calculation
					} ?: 0

				// Check if summer training is approaching (Late June is month 6, phase "Late")
				val isSummerApproaching = when {
					// 2 turns before summer: Early June (month 6, phase "Early")
					currentDate.month == 6 && currentDate.phase == "Early" -> {
						printToLog("[TRAINING] Summer training is 2 turns away (currently Early June)")
						true
					}
					// 1 turn before summer: Mid June (between Early and Late)
					// Since we only have Early/Late phases, this would be handled as part of Early June
					else -> false
				}

				// Pre-summer rest strategy logging
				if (isSummerApproaching && estimatedEnergy < 60) {
					printToLog("[TRAINING] PRE-SUMMER: Energy is low (~${estimatedEnergy.toInt()}%) with summer approaching in 2 turns")
					printToLog("[TRAINING] Prioritizing rest to maximize summer training benefits (equivalent to Lv5 training)")
					printToLog("[TRAINING] Setting ultra-conservative 5% failure threshold for pre-summer preparation")
				}

				// Check if we're currently IN summer training (not just approaching)
				val isCurrentlySummer = imageUtils.findImage("recover_energy_summer", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true).first != null

				// Enhanced risk management with multiple factors
				val adjustedFailureThreshold = when {
					// DURING SUMMER - Be more aggressive to maximize Lv5 training
					isCurrentlySummer && estimatedEnergy >= 40 -> {
						printToLog("[TRAINING] SUMMER LV5: Accepting higher risk (25%) to maximize training benefits")
						25
					}
					isCurrentlySummer && estimatedEnergy >= 30 -> {
						printToLog("[TRAINING] SUMMER LV5: Moderate energy, accepting 20% risk for summer training")
						20
					}

					// PRE-SUMMER PREPARATION - Rest if energy is low
					isSummerApproaching && estimatedEnergy < 60 -> {
						printToLog("[TRAINING] PRE-SUMMER REST: Preparing for summer training - max 5% failure risk")
						5
					}

					// CRITICAL SITUATIONS - Be very conservative
					estimatedEnergy < 20 -> {
						printToLog("[TRAINING] CRITICAL: Very low energy (~${estimatedEnergy.toInt()}%) - max 5% failure risk")
						5
					}

					// HIGH VALUE OPPORTUNITIES - Accept more risk
					friendshipTrainingCount >= 3 && bestFriendshipValue > 200 -> {
						printToLog("[TRAINING] HIGH VALUE: $friendshipTrainingCount friendship trainings (value: $bestFriendshipValue) - accepting up to 25% failure risk")
						25
					}
					friendshipTrainingCount >= 2 && bestFriendshipValue > 150 -> {
						printToLog("[TRAINING] GOOD VALUE: $friendshipTrainingCount friendship trainings (value: $bestFriendshipValue) - accepting up to 20% failure risk")
						20
					}
					friendshipTrainingCount == 1 && bestFriendshipValue > 100 -> {
						printToLog("[TRAINING] MODERATE VALUE: 1 friendship training (value: $bestFriendshipValue) - accepting up to 15% failure risk")
						15
					}

					// GAME PHASE ADJUSTMENTS
					currentDate.year == 3 && currentDate.month >= 11 -> {
						// Very late game: need to preserve energy for final push
						printToLog("[TRAINING] ENDGAME: Year 3, Month 11+ - conservative 8% failure risk")
						8
					}
					currentDate.year == 3 -> {
						// Late game: slightly conservative
						printToLog("[TRAINING] Late game (Year 3) - standard 12% failure risk")
						12
					}
					currentDate.year == 1 && estimatedEnergy > 40 -> {
						// Early game with good energy: can take more risks for relationships
						printToLog("[TRAINING] Early game with good energy - accepting up to 18% failure risk")
						18
					}

					// ENERGY-BASED ADJUSTMENTS
					estimatedEnergy > 60 -> {
						// High energy: can afford some risk
						val threshold = minOf(20, maximumFailureChance + 5)
						printToLog("[TRAINING] High energy (~${estimatedEnergy.toInt()}%) - accepting up to $threshold% failure risk")
						threshold
					}
					estimatedEnergy > 40 -> {
						// Moderate energy: standard threshold
						printToLog("[TRAINING] Moderate energy (~${estimatedEnergy.toInt()}%) - standard ${maximumFailureChance}% failure risk")
						maximumFailureChance
					}
					estimatedEnergy > 25 -> {
						// Low energy: be more conservative
						val threshold = maxOf(10, maximumFailureChance - 5)
						printToLog("[TRAINING] Low energy (~${estimatedEnergy.toInt()}%) - reduced $threshold% failure risk")
						threshold
					}
					else -> {
						// Very low energy: minimal risk
						printToLog("[TRAINING] Very low energy (~${estimatedEnergy.toInt()}%) - minimal 8% failure risk")
						8
					}
				}

				// Check for special conditions that might affect risk tolerance
				val riskAdjustment = when {
					// If we're close to stat caps, be more conservative
					currentStatsMap.values.any { it >= currentStatCap - 50 } -> {
						printToLog("[TRAINING] Near stat cap - reducing risk tolerance by 3%")
						-3
					}
					// If we have high stat deficits, accept more risk
					else -> {
						val targets = statTargetsByDistance[preferredDistance]
						if (targets != null) {
							val maxDeficit = trainings.withIndex().maxOfOrNull { (index, stat) ->
								targets[index] - currentStatsMap.getOrDefault(stat, 0)
							} ?: 0
							if (maxDeficit > 300) {
								printToLog("[TRAINING] High stat deficit ($maxDeficit) - increasing risk tolerance by 3%")
								3
							} else {
								0
							}
						} else {
							0
						}
					}
				}

				val finalThreshold = (adjustedFailureThreshold + riskAdjustment).coerceIn(5, 30)
				printToLog("[TRAINING] Final failure threshold: $finalThreshold%")

				// Check if we should prioritize recovery
				val needsRecovery = shouldPrioritizeRecovery(avgFailureRate, estimatedEnergy)

				// Check if we have any acceptable training options with adjusted threshold
				val acceptableTrainings = trainingMap.values.filter { it.failureChance <= finalThreshold && it.failureChance >= 0 }
				val witTraining = trainingMap["Wit"]

				// Calculate risk-reward ratios for acceptable trainings
				val trainingsWithRatios = acceptableTrainings.map { training ->
					training to calculateRiskRewardRatio(training)
				}.sortedByDescending { it.second }

				// Log the best risk-reward options
				if (trainingsWithRatios.isNotEmpty()) {
					printToLog("[TRAINING] Best risk-reward ratios:")
					trainingsWithRatios.take(3).forEach { (training, ratio) ->
						printToLog("  - ${training.name}: ${ratio.toInt()} (${training.failureChance}% risk, ${training.statGains.sum()} stats, ${training.relationshipBars.size} friends)")
					}
				}

				// Use the new evaluation function for Wit vs Rest decision
				val witIsBetterThanRest = evaluateWitVsRest(witTraining, estimatedEnergy)

				// Decision logic based on recovery needs and available options
				if (needsRecovery) {
					if (witIsBetterThanRest) {
						printToLog("[RECOVERY] Need recovery but Wit training provides good value - will do Wit")
						// Keep only Wit in the map
						trainingMap.clear()
						trainingMap["Wit"] = witTraining!!
					} else {
						printToLog("[RECOVERY] Need recovery and no good Wit option - will rest")
						trainingMap.clear()
					}
				} else if (acceptableTrainings.isNotEmpty()) {
					// Filter training map to only acceptable trainings with good risk-reward
					val minRiskReward = if (estimatedEnergy > 40) 30.0 else 40.0
					val goodTrainings = trainingsWithRatios
						.filter { it.second >= minRiskReward }
						.map { it.first }

					if (goodTrainings.isNotEmpty()) {
						printToLog("[TRAINING] Found ${goodTrainings.size} training(s) with good risk-reward ratios (>= $minRiskReward)")
						// Keep only good trainings in the map
						val goodTrainingNames = goodTrainings.map { it.name }.toSet()
						trainingMap.entries.removeIf { it.key !in goodTrainingNames }
					} else {
						printToLog("[TRAINING] Found ${acceptableTrainings.size} acceptable training(s) but risk-reward ratios are low")
					}
				} else if (witIsBetterThanRest) {
					// Wit training is better than resting - keep it in the map for scoring
					printToLog("[TRAINING] No trainings within normal range, but Wit training is better than resting:")
					printToLog("[TRAINING] - Wit has ${witTraining!!.relationshipBars.size} friends, ${witTraining.statGains.sum()} total stats, ${witTraining.failureChance}% failure")
					// Keep only Wit
					trainingMap.clear()
					trainingMap["Wit"] = witTraining
				} else if (witTraining != null && witTraining.failureChance <= 40) {
					// Wit is available but not amazing - still consider it
					printToLog("[TRAINING] No trainings within acceptable range. Wit training available at ${witTraining.failureChance}% failure.")
					printToLog("[TRAINING] Wit has ${witTraining.relationshipBars.size} friends and ${witTraining.statGains.sum()} total stats.")
					if (witTraining.statGains.sum() > 30 || witTraining.relationshipBars.isNotEmpty()) {
						printToLog("[TRAINING] Will consider Wit training as it provides some value.")
					} else {
						printToLog("[TRAINING] Wit training doesn't provide enough value. Will rest instead.")
						trainingMap.clear()
					}
				} else {
					// No good options - need to recover energy
					printToLog("[TRAINING] No viable training options. All failure chances too high. Proceeding to recover energy.")
					trainingMap.clear()
				}
			} else {
				// This shouldn't happen anymore since we always check all trainings
				printToLog("[TRAINING] Unable to analyze trainings properly.")
				trainingMap.clear()
			}
		}
	}

	/**
	 * Recommends the best training option based on current game state and strategic priorities.
	 *
	 * This function implements a sophisticated training recommendation system that adapts to different
	 * phases of the game. It uses different scoring algorithms depending on the current game year:
	 *
	 * **Early Game (Pre-Debut/Year 1):**
	 * - Focuses on relationship building using `scoreFriendshipTraining()`
	 * - Prioritizes training options that build friendship bars, especially blue bars
	 * - Ignores stat gains in favor of relationship development
	 *
	 * **Mid/Late Game (Year 2+):**
	 * - Uses comprehensive scoring via `scoreStatTrainingEnhanced()`
	 * - Combines stat efficiency (60-70%), relationship building (10%), and context bonuses (30%)
	 * - Adapts weighting based on whether relationship bars are present
	 *
	 * The scoring system considers multiple factors:
	 * - **Stat Efficiency:** How well training helps achieve target stats for the preferred race distance
	 * - **Relationship Building:** Value of friendship bar progress with diminishing returns
	 * - **Context Bonuses:** Phase-specific bonuses and stat gain thresholds
	 * - **Blacklist Compliance:** Excludes blacklisted training options
	 * - **Stat Cap Respect:** Avoids training that would exceed stat caps when enabled
	 *
	 * @return The name of the recommended training option, or empty string if no suitable option found.
	 */
	private fun recommendTraining(): String {
		/**
		 * Scores the currently selected training option during Junior Year based on friendship bar progress.
		 *
		 * This algorithm prefers training options with the least relationship progress (especially blue bars).
		 * It ignores stat gains unless all else is equal.
		 *
		 * @param training The training option to evaluate.
		 *
		 * @return A score representing relationship-building value.
		 */
		fun scoreFriendshipTraining(training: Training): Double {
			// Ignore the blacklist in favor of making sure we build up the relationship bars as fast as possible.
			printToLog("\n[TRAINING] Starting process to score ${training.name} Training with a focus on building relationship bars.")

			val barResults = training.relationshipBars
			if (barResults.isEmpty()) return Double.NEGATIVE_INFINITY

			var score = 0.0
			for (bar in barResults) {
				val contribution = when (bar.dominantColor) {
					"orange" -> 0.0
					"green" -> 1.0
					"blue" -> 2.5
					else -> 0.0
				}
				score += contribution
			}

			printToLog("[TRAINING] ${training.name} Training has a score of ${decimalFormat.format(score)} with a focus on building relationship bars.")
			return score
		}

		/**
		 * Calculates the efficiency score for stat gains based on target achievement and priority weights.
		 *
		 * This function evaluates how well a training option helps achieve stat targets by considering:
		 * - The gap between current stats and target stats
		 * - Priority weights that vary by game year (higher priority in later years)
		 * - Efficiency bonuses for closing gaps vs diminishing returns for overage
		 * - Spark stat target focus when enabled (Speed, Stamina, Power to 600+)
		 * - Enhanced priority weighting for top 3 stats to prevent target completion from overriding large gains
		 *
		 * @param training The training option to evaluate.
		 * @param target Array of target stat values for the preferred race distance.
		 *
		 * @return A normalized score (0-100) representing stat efficiency.
		 */
		fun calculateStatEfficiencyScore(training: Training, target: IntArray): Double {
			var score = 100.0

			for ((index, stat) in trainings.withIndex()) {
				val currentStat = currentStatsMap.getOrDefault(stat, 0)
				val targetStat = target.getOrElse(index) { 0 }
				val statGain = training.statGains.getOrElse(index) { 0 }
				val deficit = targetStat - currentStat

				if (statGain > 0) {
					// Priority weight based on the current state of the game.
					val priorityIndex = statPrioritization.indexOf(stat)
					val priorityWeight = if (priorityIndex != -1) {
						// Enhanced priority weighting for top 3 stats
						val top3Bonus = when (priorityIndex) {
							0 -> 2.0
							1 -> 1.5
							2 -> 1.1
							else -> 1.0
						}

						val baseWeight = when {
							currentDate.year == 1 || currentDate.phase == "Pre-Debut" -> 1.0 + (0.1 * (statPrioritization.size - priorityIndex)) / statPrioritization.size
							currentDate.year == 2 -> 1.0 + (0.3 * (statPrioritization.size - priorityIndex)) / statPrioritization.size
							currentDate.year == 3 -> 1.0 + (0.5 * (statPrioritization.size - priorityIndex)) / statPrioritization.size
							else -> 1.0
						}

						// Apply deficit multiplier from the guide
						val deficitMultiplier = getDeficitMultiplier(deficit)
						baseWeight * top3Bonus * deficitMultiplier  // Include deficit multiplier
					} else {
						// Apply deficit multiplier even for non-prioritized stats
						val deficitMultiplier = getDeficitMultiplier(deficit)
						deficitMultiplier * 0.5
					}

					// Apply deficit-based multiplier according to the training guide
					val deficitMultiplier = when {
						deficit > 300 -> 3.0  // Critical priority
						deficit > 200 -> 2.5  // High priority
						deficit > 100 -> 2.0  // Moderate priority
						deficit > 50 -> 1.5   // Low priority
						deficit > 0 -> 1.1    // Maintenance
						else -> 0.5          // Surplus (diminishing returns)
					}

					Log.d(tag, "[DEBUG] Priority Weight: $priorityWeight, Deficit: $deficit, Deficit Multiplier: $deficitMultiplier")

					// Calculate efficiency based on remaining gap between the current stat and the target.
					var efficiency = if (deficit > 0) {
						// Stat is below target, apply deficit multiplier
						Log.d(tag, "[DEBUG] Giving bonus for remaining efficiency.")
						val gapRatio = deficit.toDouble() / targetStat
						val targetBonus = when {
							gapRatio > 0.1 -> 1.5
							gapRatio > 0.05 -> 1.25
							else -> 1.1
						}
						targetBonus + (statGain.toDouble() / deficit).coerceAtMost(1.0)
					} else {
						// Stat is above target, give a diminishing bonus based on how much over.
						Log.d(tag, "[DEBUG] Stat is above target so giving diminishing bonus.")
						val overageRatio = (statGain.toDouble() / (-deficit + statGain))
						1.0 + overageRatio * 0.5 // Reduced bonus for over-target training
					}

					Log.d(tag, "[DEBUG] Efficiency: $efficiency")

					// Apply Spark stat target focus when enabled.
					if (focusOnSparkStatTarget) {
						val sparkTarget = 600
						val sparkRemaining = sparkTarget - currentStat

						// Check if this is a Spark stat (Speed, Stamina, Power) and it's below 600.
						if ((stat == "Speed" || stat == "Stamina" || stat == "Power") && sparkRemaining > 0) {
							// Boost efficiency for Spark stats that are below 600.
							val sparkEfficiency = 2.0 + (statGain.toDouble() / sparkRemaining).coerceAtMost(1.0)
							// Use the higher of the two efficiencies (original target vs spark target).
							efficiency = maxOf(efficiency, sparkEfficiency)
						}
					}

					// Apply deficit multiplier to the scoring
					score += statGain * 2
					score += (statGain * 2) * (efficiency * priorityWeight * deficitMultiplier)
					Log.d(tag, "[DEBUG] Score: $score")
				}
			}

			return score.coerceAtMost(1000.0)
		}

		/**
		 * Calculates relationship building score with diminishing returns.
		 *
		 * Evaluates the value of relationship bars based on their color and fill level:
		 * - Blue bars: 2.5 points (highest priority)
		 * - Green bars: 1.0 points (medium priority)
		 * - Orange bars: 0.0 points (no value)
		 *
		 * Applies diminishing returns as bars fill up and early game bonuses for relationship building.
		 *
		 * @param training The training option to evaluate.
		 *
		 * @return A normalized score (0-100) representing relationship building value.
		 */
		fun calculateRelationshipScore(training: Training): Double {
			if (training.relationshipBars.isEmpty()) return 0.0

			var score = 0.0
			var maxScore = 0.0

			for (bar in training.relationshipBars) {
				// Relationship bar values from the guide
				val baseValue = when (bar.dominantColor) {
					"blue" -> 2.5    // Blue bars: Always highest priority
					"green" -> 1.0   // Green bars: Secondary priority
					"orange" -> 0.3  // Orange bars: Minimal value
					else -> 0.0
				}

				if (baseValue > 0) {
					// Apply diminishing returns for relationship building
					val fillLevel = bar.fillPercent / 100.0
					val diminishingFactor = 1.0 - (fillLevel * 0.5) // Less valuable as bars fill up

					// Year-based focus from the guide
					// Year 1: 55% relationship focus, Year 2: 50/50, Year 3: 30% relationships
					val yearMultiplier = when {
						currentDate.year == 1 || currentDate.phase == "Pre-Debut" -> 1.55  // 55% focus
						currentDate.year == 2 -> 1.0   // 50/50 balanced
						currentDate.year == 3 -> 0.6   // 30% relationships, 70% stats
						else -> 1.0
					}

					val contribution = baseValue * diminishingFactor * yearMultiplier
					score += contribution
					maxScore += 2.5 * 1.55  // Max possible value
				}
			}

			return if (maxScore > 0) (score / maxScore * 100.0) else 0.0
		}

		/**
		 * Calculates context-aware bonuses and penalties based on game phase and training properties.
		 *
		 * Applies various bonuses including:
		 * - Phase-specific bonuses (relationship focus in early game, stat efficiency in later years)
		 * - Stat gain thresholds that provide additional bonuses
		 * - Mood effects on training gains
		 * - Energy recovery bonus for Wit training when energy is low
		 *
		 * @param training The training option to evaluate.
		 *
		 * @return A context score between 0-200 representing situational bonuses.
		 */
		fun calculateContextScore(training: Training): Double {
			// Start with neutral score.
			var score = 100.0

			// Apply mood multiplier based on current mood
			val currentMood: String = when {
				imageUtils.findImage("mood_great", tries = 1, region = imageUtils.regionTopHalf, suppressError = true).first != null -> "Great"
				imageUtils.findImage("mood_good", tries = 1, region = imageUtils.regionTopHalf, suppressError = true).first != null -> "Good"
				imageUtils.findImage("mood_normal", tries = 1, region = imageUtils.regionTopHalf, suppressError = true).first != null -> "Normal"
				else -> "Bad/Awful"
			}

			// More accurate mood multipliers from the guide
			val moodMultiplier = when (currentMood) {
				"Great" -> 1.2    // +20% to all stat gains (絶好調)
				"Good" -> 1.1     // +10% to all stat gains (好調)
				"Normal" -> 1.0   // No modifier (普通)
				"Bad" -> 0.9      // -10% to all stat gains (不調)
				"Awful" -> 0.8    // -20% to all stat gains (絶不調)
				else -> 0.9       // Assume Bad if unknown
			}

			// Apply mood bonus to score - more impactful for stat-heavy trainings
			if (training.statGains.sum() > 30) {
				score *= moodMultiplier
				printToLog("[TRAINING] Mood multiplier ${moodMultiplier}x applied for ${training.name} (${currentMood} mood)")
			}

			// Energy recovery bonus for Wit training
			if (training.name == "Wit") {
				// Check if other trainings have high failure rates (indicating low energy)
				val otherTrainings = trainingMap.values.filter { it.name != "Wit" }
				val avgFailureRate = if (otherTrainings.isNotEmpty()) {
					otherTrainings.map { it.failureChance }.average()
				} else 0.0

				// Dynamic bonus based on how much energy recovery is needed
				// Wit training recovers ~5-10 energy, which reduces failure by 2.5-5%
				val energyRecoveryValue = when {
					avgFailureRate > 20 -> {
						// Critical energy state - Wit recovery is very valuable
						val bonus = avgFailureRate * 2.0  // Dynamic scaling
						score += bonus
						printToLog("[TRAINING] Wit gets +${bonus.toInt()} for energy recovery (critical: ${avgFailureRate.toInt()}% avg failure)")
						bonus
					}
					avgFailureRate > 15 -> {
						// Low energy - Wit recovery is valuable
						val bonus = avgFailureRate * 1.5
						score += bonus
						printToLog("[TRAINING] Wit gets +${bonus.toInt()} for energy recovery (low: ${avgFailureRate.toInt()}% avg failure)")
						bonus
					}
					avgFailureRate > 10 -> {
						// Moderate energy - Wit recovery has some value
						val bonus = avgFailureRate * 1.0
						score += bonus
						printToLog("[TRAINING] Wit gets +${bonus.toInt()} for energy recovery (moderate: ${avgFailureRate.toInt()}% avg failure)")
						bonus
					}
					else -> 0.0
				}

				// Calculate value of Wit training vs resting
				// Resting recovers ~40 energy but wastes a turn
				// Wit with good stats/friends can be more efficient
				val witStatValue = training.statGains.sum()
				val witFriendValue = training.relationshipBars.size * 10  // Each friend bar worth ~10 points
				val witTotalValue = witStatValue + witFriendValue + energyRecoveryValue

				// Compare to resting value (40 energy = 20% failure reduction)
				val restingValue = avgFailureRate * 2  // Rough value of full energy recovery

				if (witTotalValue > restingValue && training.failureChance <= 30) {
					val preferenceBonus = (witTotalValue - restingValue) * 0.5  // Half the difference as bonus
					score += preferenceBonus
					printToLog("[TRAINING] Wit preferred over rest: +${preferenceBonus.toInt()} (${witStatValue} stats, ${training.relationshipBars.size} friends)")
				}
			}

			// Dynamic bonuses based on game phase
			when {
				currentDate.year == 1 || currentDate.phase == "Pre-Debut" -> {
					// Year 1: Focus on relationship building (55% weight according to guide)
					if (training.relationshipBars.isNotEmpty()) {
						score += training.relationshipBars.size * 15.0  // Dynamic based on friend count
					}
					// Small bonus for decent stat gains
					val statBonus = minOf(training.statGains.sum() * 0.5, 20.0)
					if (statBonus > 0) {
						score += statBonus
						printToLog("[TRAINING] Year 1 stat bonus: +${statBonus.toInt()}")
					}
				}
				currentDate.year == 2 -> {
					// Year 2: Balanced approach (50/50 according to guide)
					// Stat gains become more important
					val statBonus = minOf(training.statGains.sum() * 0.8, 40.0)
					score += statBonus
					if (statBonus > 20) {
						printToLog("[TRAINING] Year 2 stat bonus: +${statBonus.toInt()}")
					}
				}
				currentDate.year == 3 -> {
					// Year 3: Stat maximization (70% weight according to guide)
					// High value on large stat gains
					val statBonus = minOf(training.statGains.sum() * 1.2, 60.0)
					score += statBonus
					// Additional bonus for very high stat trainings
					if (training.statGains.sum() > 40) {
						val extraBonus = (training.statGains.sum() - 40) * 0.5
						score += extraBonus
						printToLog("[TRAINING] Year 3 high stat bonus: +${(statBonus + extraBonus).toInt()}")
					}
				}
			}

			// Bonuses for skill hints - each hint is valuable but not overwhelming
			val skillHintLocations = imageUtils.findAll(
				"stat_skill_hint",
				region = intArrayOf(
					MediaProjectionService.displayWidth - (MediaProjectionService.displayWidth / 3),
					0,
					(MediaProjectionService.displayWidth / 3),
					MediaProjectionService.displayHeight - (MediaProjectionService.displayHeight / 3)
				)
			)
			if (skillHintLocations.isNotEmpty()) {
				val hintBonus = skillHintLocations.size * 25.0  // 25 points per hint
				score += hintBonus
				printToLog("[TRAINING] Skill hint bonus: +${hintBonus.toInt()} for ${skillHintLocations.size} hints")
			}

			// Rainbow training (multiple friendship training) bonus
			// This is valuable but should be proportional to the actual benefit
			val highFriendshipBars = training.relationshipBars.count { bar ->
				bar.dominantColor == "blue" && bar.fillPercent >= 80
			}
			if (highFriendshipBars >= 2) {
				// Rainbow training typically gives 50-100% more stats
				val rainbowBonus = training.statGains.sum() * 0.5 * highFriendshipBars
				score += rainbowBonus
				printToLog("[TRAINING] Rainbow training bonus: +${rainbowBonus.toInt()} for ${highFriendshipBars} high friendship bars")
			}

			// Summer training special handling
			val isSummer = imageUtils.findImage("recover_energy_summer", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true).first != null
			if (isSummer) {
				// During summer, we have Lv5 training facilities - maximize usage!
				// All trainings are significantly more valuable during summer

				// Base summer bonus for ANY training (Lv5 facilities)
				val baseSummerBonus = 30.0
				score += baseSummerBonus
				printToLog("[TRAINING] Summer Lv5 facility bonus: +${baseSummerBonus.toInt()}")

				// Extra bonus for high-value summer trainings
				if (training.relationshipBars.size >= 2) {
					// Multiple friends during summer are extremely valuable
					val summerFriendBonus = training.relationshipBars.size * 20.0
					score += summerFriendBonus
					printToLog("[TRAINING] Summer friendship bonus: +${summerFriendBonus.toInt()} for ${training.relationshipBars.size} friends")
				}

				// Bonus for high stat gains during summer
				if (training.statGains.sum() > 30) {
					val summerStatBonus = (training.statGains.sum() - 30) * 0.5
					score += summerStatBonus
					printToLog("[TRAINING] Summer high-stat bonus: +${summerStatBonus.toInt()}")
				}

				// Reduce penalty for slightly higher failure during summer
				// We want to be more aggressive about training during summer
				if (training.failureChance <= 25) {
					val summerRiskBonus = 15.0
					score += summerRiskBonus
					printToLog("[TRAINING] Summer acceptable risk bonus: +${summerRiskBonus.toInt()}")
				}
			}

			return score.coerceIn(0.0, 500.0)  // Reasonable max with dynamic bonuses
		}

		/**
		 * Performs comprehensive scoring of training options using multiple weighted factors.
		 *
		 * This scoring system combines three main components:
		 * - Stat efficiency (60-70% weight): How well the training helps achieve stat targets
		 * - Relationship building (10% weight): Value of friendship bar progress
		 * - Context bonuses (30% weight): Phase-specific bonuses, etc.
		 *
		 * The weighting changes based on whether relationship bars are present:
		 * - With relationship bars: 60% stat, 10% relationship, 30% context
		 * - Without relationship bars: 70% stat, 0% relationship, 30% context
		 *
		 * @param training The training option to evaluate.
		 *
		 * @return A normalized score (1-1000) representing overall training value.
		 */
		fun scoreStatTraining(training: Training): Double {
			if (training.name in blacklist) return 0.0

			// Don't score for stats that are maxed or would be maxed.
			if ((disableTrainingOnMaxedStat && currentStatsMap[training.name]!! >= currentStatCap) ||
				(currentStatsMap.getOrDefault(training.name, 0) + training.statGains[trainings.indexOf(training.name)] >= currentStatCap)) {
				return 0.0
			}

			printToLog("\n[TRAINING] Starting scoring for ${training.name} Training.")

			val target = statTargetsByDistance[preferredDistance] ?: intArrayOf(600, 600, 600, 300, 300)

			var totalScore = 0.0
			var maxPossibleScore = 0.0

			// 1. Stat Efficiency scoring
			val statScore = calculateStatEfficiencyScore(training, target)

			// 2. Friendship scoring
			val relationshipScore = calculateRelationshipScore(training)

			// 3. Context-aware scoring
			val contextScore = calculateContextScore(training)

			if (training.relationshipBars.isNotEmpty()) {
				totalScore += statScore * 0.6
				maxPossibleScore += 100.0 * 0.6

				totalScore += relationshipScore * 0.1
				maxPossibleScore += 100.0 * 0.1

				totalScore += contextScore * 0.3
				maxPossibleScore += 100.0 * 0.3
			} else {
				totalScore += statScore * 0.7
				maxPossibleScore += 100.0 * 0.7

				totalScore += contextScore * 0.3
				maxPossibleScore += 100.0 * 0.3
			}

			printToLog(
				"[TRAINING] Scores | Current Stat: ${currentStatsMap[training.name]}, Target Stat: ${target[trainings.indexOf(training.name)]}, " +
						"Stat Efficiency: ${decimalFormat.format(statScore)}, Relationship: ${decimalFormat.format(relationshipScore)}, " +
						"Context: ${decimalFormat.format(contextScore)}"
			)

			// Normalize the score.
			val normalizedScore = (totalScore / maxPossibleScore * 100.0).coerceIn(1.0, 1000.0)

			printToLog("[TRAINING] Enhanced final score for ${training.name} Training: ${decimalFormat.format(normalizedScore)}/1000.0")

			return normalizedScore
		}

		// Filter trainings by acceptable failure chance first
		val acceptableTrainings = trainingMap.values.filter {
			it.failureChance >= 0 && it.failureChance <= maximumFailureChance && it.name !in blacklist
		}

		if (acceptableTrainings.isEmpty()) {
			printToLog("[TRAINING] No trainings within acceptable failure threshold (${maximumFailureChance}%)")
			return ""
		}

		// Decide which scoring function to use based on the current phase or year.
		// Junior Year will focus on building relationship bars.
		val best = if (currentDate.phase == "Pre-Debut" || currentDate.year == 1) {
			acceptableTrainings.maxByOrNull { scoreFriendshipTraining(it) }
		} else acceptableTrainings.maxByOrNull { scoreStatTraining(it) }

		return if (best != null) {
			printToLog("[TRAINING] Selected ${best.name} training with ${best.failureChance}% failure chance")
			historicalTrainingCounts.put(best.name, historicalTrainingCounts.getOrDefault(best.name, 0) + 1)
			best.name
		} else {
			""
		}
	}

	/**
	 * Execute the training with the highest stat weight.
	 */
	private fun executeTraining() {
		printToLog("\n********************")
		printToLog("[TRAINING] Now starting process to execute training...")
		val trainingSelected = recommendTraining()

		if (trainingSelected != "") {
			printTrainingMap()
			printToLog("[TRAINING] Executing the $trainingSelected Training.")
			findAndTapImage("training_${trainingSelected.lowercase()}", region = imageUtils.regionBottomHalf, taps = 3)
			printToLog("[TRAINING] Process to execute training completed.")
		} else {
			printToLog("[TRAINING] Conditions have not been met so training will not be done.")
		}

		printToLog("********************\n")

		// Now reset the Training map.
		trainingMap.clear()
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Functions to handle Training Events with the help of the TextDetection class.

	/**
	 * Start text detection to determine what Training Event it is and the event rewards for each option.
	 * It will then select the best option according to the user's preferences. By default, it will choose the first option.
	 */
	fun handleTrainingEvent() {
		printToLog("\n[TRAINING-EVENT] Starting Training Event process...")

		val (eventRewards, confidence) = textDetection.start()

		val regex = Regex("[a-zA-Z]+")
		var optionSelected = 0

		// Double check if the bot is at the Main screen or not.
		if (checkMainScreen()) {
			return
		}

		if (eventRewards.isNotEmpty() && eventRewards[0] != "") {
			// Initialize the List.
			val selectionWeight = List(eventRewards.size) { 0 }.toMutableList()

			// Sum up the stat gains with additional weight applied to stats that are prioritized.
			eventRewards.forEach { reward ->
				val formattedReward: List<String> = reward.split("\n")

				formattedReward.forEach { line ->
					val formattedLine: String = regex
						.replace(line, "")
						.replace("(", "")
						.replace(")", "")
						.trim()
						.lowercase()

					printToLog("[TRAINING-EVENT] Original line is \"$line\".")
					printToLog("[TRAINING-EVENT] Formatted line is \"$formattedLine\".")

					var priorityStatCheck = false
					if (line.lowercase().contains("energy")) {
						val finalEnergyValue = try {
							val energyValue = if (formattedLine.contains("/")) {
								val splits = formattedLine.split("/")
								var sum = 0
								for (split in splits) {
									sum += try {
										split.trim().toInt()
									} catch (_: NumberFormatException) {
										printToLog("[WARNING] Could not convert $formattedLine to a number for energy with a forward slash.")
										20
									}
								}
								sum
							} else {
								formattedLine.toInt()
							}

							if (enablePrioritizeEnergyOptions) {
								energyValue * 100
							} else {
								energyValue * 3
							}
						} catch (_: NumberFormatException) {
							printToLog("[WARNING] Could not convert $formattedLine to a number for energy.")
							20
						}
						printToLog("[TRAINING-EVENT] Adding weight for option #${optionSelected + 1} of $finalEnergyValue for energy.")
						selectionWeight[optionSelected] += finalEnergyValue
					} else if (line.lowercase().contains("mood")) {
						val moodWeight = if (formattedLine.contains("-")) -50 else 50
						printToLog("[TRAINING-EVENT Adding weight for option#${optionSelected + 1} of $moodWeight for ${if (moodWeight > 0) "positive" else "negative"} mood gain.")
						selectionWeight[optionSelected] += moodWeight
					} else if (line.lowercase().contains("bond")) {
						printToLog("[TRAINING-EVENT] Adding weight for option #${optionSelected + 1} of 20 for bond.")
						selectionWeight[optionSelected] += 20
					} else if (line.lowercase().contains("event chain ended")) {
						printToLog("[TRAINING-EVENT] Adding weight for option #${optionSelected + 1} of -50 for event chain ending.")
						selectionWeight[optionSelected] += -50
					} else if (line.lowercase().contains("(random)")) {
						printToLog("[TRAINING-EVENT] Adding weight for option #${optionSelected + 1} of -10 for random reward.")
						selectionWeight[optionSelected] += -10
					} else if (line.lowercase().contains("randomly")) {
						printToLog("[TRAINING-EVENT] Adding weight for option #${optionSelected + 1} of 50 for random options.")
						selectionWeight[optionSelected] += 50
					} else if (line.lowercase().contains("hint")) {
						printToLog("[TRAINING-EVENT] Adding weight for option #${optionSelected + 1} of 25 for skill hint(s).")
						selectionWeight[optionSelected] += 25
					} else if (line.lowercase().contains("skill")) {
						val finalSkillPoints = if (formattedLine.contains("/")) {
							val splits = formattedLine.split("/")
							var sum = 0
							for (split in splits) {
								sum += try {
									split.trim().toInt()
								} catch (_: NumberFormatException) {
									printToLog("[WARNING] Could not convert $formattedLine to a number for skill points with a forward slash.")
									10
								}
							}
							sum
						} else {
							formattedLine.toInt()
						}
						printToLog("[TRAINING-EVENT] Adding weight for option #${optionSelected + 1} of $finalSkillPoints for skill points.")
						selectionWeight[optionSelected] += finalSkillPoints
					} else {
						// Apply inflated weights to the prioritized stats based on their order.
						statPrioritization.forEachIndexed { index, stat ->
							if (line.contains(stat)) {
								// Calculate weight bonus based on position (higher priority = higher bonus).
								val priorityBonus = when (index) {
									0 -> 50
									1 -> 40
									2 -> 30
									3 -> 20
									else -> 10
								}

								val finalStatValue = try {
									priorityStatCheck = true
									if (formattedLine.contains("/")) {
										val splits = formattedLine.split("/")
										var sum = 0
										for (split in splits) {
											sum += try {
												split.trim().toInt()
											} catch (_: NumberFormatException) {
												printToLog("[WARNING] Could not convert $formattedLine to a number for a priority stat with a forward slash.")
												10
											}
										}
										sum + priorityBonus
									} else {
										formattedLine.toInt() + priorityBonus
									}
								} catch (_: NumberFormatException) {
									printToLog("[WARNING] Could not convert $formattedLine to a number for a priority stat.")
									priorityStatCheck = false
									10
								}
								printToLog("[TRAINING-EVENT] Adding weight for option #${optionSelected + 1} of $finalStatValue for prioritized stat.")
								selectionWeight[optionSelected] += finalStatValue
							}
						}

						// Apply normal weights to the rest of the stats.
						if (!priorityStatCheck) {
							val finalStatValue = try {
								if (formattedLine.contains("/")) {
									val splits = formattedLine.split("/")
									var sum = 0
									for (split in splits) {
										sum += try {
											split.trim().toInt()
										} catch (_: NumberFormatException) {
											printToLog("[WARNING] Could not convert $formattedLine to a number for non-prioritized stat with a forward slash.")
											10
										}
									}
									sum
								} else {
									formattedLine.toInt()
								}
							} catch (_: NumberFormatException) {
								printToLog("[WARNING] Could not convert $formattedLine to a number for non-prioritized stat.")
								10
							}
							printToLog("[TRAINING-EVENT] Adding weight for option #${optionSelected + 1} of $finalStatValue for non-prioritized stat.")
							selectionWeight[optionSelected] += finalStatValue
						}
					}

					printToLog("[TRAINING-EVENT] Final weight for option #${optionSelected + 1} is: ${selectionWeight[optionSelected]}.")
				}

				optionSelected++
			}

			// Select the best option that aligns with the stat prioritization made in the Training options.
			var max: Int? = selectionWeight.maxOrNull()
			if (max == null) {
				max = 0
				optionSelected = 0
			} else {
				optionSelected = selectionWeight.indexOf(max)
			}

			// Print the selection weights.
			printToLog("[TRAINING-EVENT] Selection weights for each option:")
			selectionWeight.forEachIndexed { index, weight ->
				printToLog("Option ${index + 1}: $weight")
			}

			// Format the string to display each option's rewards.
			var eventRewardsString = ""
			var optionNumber = 1
			eventRewards.forEach { reward ->
				eventRewardsString += "Option $optionNumber: \"$reward\"\n"
				optionNumber += 1
			}

			val minimumConfidence = sharedPreferences.getInt("confidence", 80).toDouble() / 100.0
			val resultString = if (confidence >= minimumConfidence) {
				"[TRAINING-EVENT] For this Training Event consisting of:\n$eventRewardsString\nThe bot will select Option ${optionSelected + 1}: \"${eventRewards[optionSelected]}\" with a " +
						"selection weight of $max."
			} else {
				"[TRAINING-EVENT] Since the confidence was less than the set minimum, first option will be selected."
			}

			printToLog(resultString)
		} else {
			printToLog("[TRAINING-EVENT] First option will be selected since OCR failed to detect anything.")
			optionSelected = 0
		}

		val trainingOptionLocations: ArrayList<Point> = imageUtils.findAll("training_event_active")
		val selectedLocation: Point? = if (trainingOptionLocations.isNotEmpty()) {
			// Account for the situation where it could go out of bounds if the detected event options is incorrect and gives too many results.
			try {
				trainingOptionLocations[optionSelected]
			} catch (_: IndexOutOfBoundsException) {
				// Default to the first option.
				trainingOptionLocations[0]
			}
		} else {
			imageUtils.findImage("training_event_active", tries = 5, region = imageUtils.regionMiddle).first
		}

		if (selectedLocation != null) {
			tap(selectedLocation.x + imageUtils.relWidth(100), selectedLocation.y, "training_event_active")
		}

		printToLog("[TRAINING-EVENT] Process to handle detected Training Event completed.")
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Functions to handle Race Events.

	/**
	 * The entry point for handling mandatory or extra races.
	 *
	 * @return True if the mandatory/extra race was completed successfully. Otherwise false.
	 */
	fun handleRaceEvents(): Boolean {
		printToLog("\n[RACE] Starting Racing process...")
		if (encounteredRacingPopup) {
			// Dismiss the insufficient fans popup here and head to the Race Selection screen.
			findAndTapImage("race_confirm", tries = 1, region = imageUtils.regionBottomHalf)
			encounteredRacingPopup = false
			wait(1.0)
		}

		// If there are no races available, cancel the racing process.
		if (imageUtils.findImage("race_none_available", tries = 1, region = imageUtils.regionMiddle, suppressError = true).first != null) {
			printToLog("[RACE] There are no races to compete in. Canceling the racing process and doing something else.")
			return false
		}

		skipRacing = false

		// First, check if there is a mandatory or a extra race available. If so, head into the Race Selection screen.
		// Note: If there is a mandatory race, the bot would be on the Home screen.
		// Otherwise, it would have found itself at the Race Selection screen already (by way of the insufficient fans popup).
		if (findAndTapImage("race_select_mandatory", tries = 1, region = imageUtils.regionBottomHalf)) {
			printToLog("\n[RACE] Starting process for handling a mandatory race.")

			if (enableStopOnMandatoryRace) {
				detectedMandatoryRaceCheck = true
				return false
			} else if (enableForceRacing) {
				findAndTapImage("ok", tries = 1, region = imageUtils.regionMiddle)
				wait(1.0)
			}

			// There is a mandatory race. Now confirm the selection and the resultant popup and then wait for the game to load.
			wait(2.0)
			printToLog("[RACE] Confirming the mandatory race selection.")
			findAndTapImage("race_confirm", tries = 3, region = imageUtils.regionBottomHalf)
			wait(1.0)
			printToLog("[RACE] Confirming any popup from the mandatory race selection.")
			findAndTapImage("race_confirm", tries = 3, region = imageUtils.regionBottomHalf)
			wait(2.0)

			waitForLoading()

			// Select the preferred race strategy if it is not already selected.
			printToLog("[DEBUG] SECOND")
			if (strategySelected == false) {
				if (strategyImageName != "default") {
					findAndTapImage("race_change_strategy", tries = 10, region = imageUtils.regionBottomHalf)
					findAndTapImage(strategyImageName + "_select", tries = 10, region = imageUtils.regionBottomHalf)
					findAndTapImage("confirm", tries = 10, region = imageUtils.regionBottomHalf)
					wait(1.0)
				}
				strategySelected = true;
			}

			// Skip the race if possible, otherwise run it manually.
			val resultCheck: Boolean = if (imageUtils.findImage("race_skip_locked", tries = 5, region = imageUtils.regionBottomHalf).first == null) {
				skipRace()
			} else {
				manualRace()
			}

			finishRace(resultCheck)

			printToLog("[RACE] Racing process for Mandatory Race is completed.")
			return true
		} else if (currentDate.phase != "Pre-Debut" && findAndTapImage("race_select_extra", tries = 1, region = imageUtils.regionBottomHalf)) {
			printToLog("\n[RACE] Starting process for handling a extra race.")

			// If there is a popup warning about repeating races 3+ times, stop the process and do something else other than racing.
			if (imageUtils.findImage("race_repeat_warning").first != null) {
				if (!enableForceRacing) {
					raceRepeatWarningCheck = true
					printToLog("\n[RACE] Closing popup warning of doing more than 3+ races and setting flag to prevent racing for now. Canceling the racing process and doing something else.")
					findAndTapImage("cancel", region = imageUtils.regionBottomHalf)
					return false
				} else {
					findAndTapImage("ok", tries = 1, region = imageUtils.regionMiddle)
					wait(1.0)
				}
			}

			// There is a extra race.
			// Swipe up the list to get to the top and then select the first option.
			val statusLocation = imageUtils.findImage("race_status").first
			if (statusLocation == null) {
				printToLog("[ERROR] Unable to determine existence of list of extra races. Canceling the racing process and doing something else.", isError = true)
				return false
			}
			gestureUtils.swipe(statusLocation.x.toFloat(), statusLocation.y.toFloat() + 300, statusLocation.x.toFloat(), statusLocation.y.toFloat() + 888)
			wait(1.0)

			// Now determine the best extra race with the following parameters: highest fans and double star prediction.
			// First find the fans of only the extra races on the screen that match the double star prediction. Read only 3 extra races.
			var count = 0
			val maxCount = imageUtils.findAll("race_selection_fans", region = imageUtils.regionBottomHalf).size
			if (maxCount == 0) {
				printToLog("[WARNING] Was unable to find any extra races to select. Canceling the racing process and doing something else.", isError = true)
				return false
			} else {
				printToLog("[RACE] There are $maxCount extra race options currently on screen.")
			}
			val listOfFans = mutableListOf<Int>()
			val extraRaceLocation = mutableListOf<Point>()
			val doublePredictionLocations = imageUtils.findAll("race_extra_double_prediction")
			if (doublePredictionLocations.size == 1) {
				printToLog("[RACE] There is only one race with double predictions so selecting that one.")
				tap(
					doublePredictionLocations[0].x,
					doublePredictionLocations[0].y,
					"race_extra_double_prediction",
					ignoreWaiting = true
				)
			} else {
				val (sourceBitmap, templateBitmap) = imageUtils.getBitmaps("race_extra_double_prediction")
				val listOfRaces: ArrayList<ImageUtils.RaceDetails> = arrayListOf()
				while (count < maxCount) {
					// Save the location of the selected extra race.
					val selectedExtraRace = imageUtils.findImage("race_extra_selection", region = imageUtils.regionBottomHalf).first
					if (selectedExtraRace == null) {
						printToLog("[ERROR] Unable to find the location of the selected extra race. Canceling the racing process and doing something else.", isError = true)
						break
					}
					extraRaceLocation.add(selectedExtraRace)

					// Determine its fan gain and save it.
					val raceDetails: ImageUtils.RaceDetails = imageUtils.determineExtraRaceFans(extraRaceLocation[count], sourceBitmap, templateBitmap!!, forceRacing = enableForceRacing)
					listOfRaces.add(raceDetails)
					if (count == 0 && raceDetails.fans == -1) {
						// If the fans were unable to be fetched or the race does not have double predictions for the first attempt, skip racing altogether.
						listOfFans.add(raceDetails.fans)
						break
					}
					listOfFans.add(raceDetails.fans)

					// Select the next extra race.
					if (count + 1 < maxCount) {
						if (imageUtils.isTablet) {
							tap(
								imageUtils.relX(extraRaceLocation[count].x, (-100 * 1.36).toInt()).toDouble(),
								imageUtils.relY(extraRaceLocation[count].y, (150 * 1.50).toInt()).toDouble(),
								"race_extra_selection",
								ignoreWaiting = true
							)
						} else {
							tap(
								imageUtils.relX(extraRaceLocation[count].x, -100).toDouble(),
								imageUtils.relY(extraRaceLocation[count].y, 150).toDouble(),
								"race_extra_selection",
								ignoreWaiting = true
							)
						}
					}

					wait(0.5)

					count++
				}

				val fansList = listOfRaces.joinToString(", ") { it.fans.toString() }
				printToLog("[RACE] Number of fans detected for each extra race are: $fansList")

				// Next determine the maximum fans and select the extra race.
				val maxFans: Int? = listOfFans.maxOrNull()
				if (maxFans != null) {
					if (maxFans == -1) {
						printToLog("[WARNING] Max fans was returned as -1. Canceling the racing process and doing something else.")
						return false
					}

					// Get the index of the maximum fans or the one with the double predictions if available when force racing is enabled.
					val index = if (!enableForceRacing) {
						listOfFans.indexOf(maxFans)
					} else {
						// When force racing is enabled, prioritize races with double predictions.
						val doublePredictionIndex = listOfRaces.indexOfFirst { it.hasDoublePredictions }
						if (doublePredictionIndex != -1) {
							printToLog("[RACE] Force racing enabled - selecting race with double predictions.")
							doublePredictionIndex
						} else {
							// Fall back to the race with maximum fans if no double predictions found
							printToLog("[RACE] Force racing enabled but no double predictions found - falling back to race with maximum fans.")
							listOfFans.indexOf(maxFans)
						}
					}

					printToLog("[RACE] Selecting the extra race at option #${index + 1}.")

					// Select the extra race that matches the double star prediction and the most fan gain.
					tap(
						extraRaceLocation[index].x - imageUtils.relWidth((100 * 1.36).toInt()),
						extraRaceLocation[index].y - imageUtils.relHeight(70),
						"race_extra_selection",
						ignoreWaiting = true
					)
				} else if (extraRaceLocation.isNotEmpty()) {
					// If no maximum is determined, select the very first extra race.
					printToLog("[RACE] Selecting the first extra race on the list by default.")
					tap(
						extraRaceLocation[0].x - imageUtils.relWidth((100 * 1.36).toInt()),
						extraRaceLocation[0].y - imageUtils.relHeight(70),
						"race_extra_selection",
						ignoreWaiting = true
					)
				} else {
					printToLog("[WARNING] No extra races detected and thus no fan maximums were calculated. Canceling the racing process and doing something else.")
					return false
				}
			}

			// Confirm the selection and the resultant popup and then wait for the game to load.
			findAndTapImage("race_confirm", tries = 30, region = imageUtils.regionBottomHalf)
			findAndTapImage("race_confirm", tries = 10, region = imageUtils.regionBottomHalf)
			wait(2.0)

			// Select the preferred race strategy if it is not already selected.
			printToLog("[DEBUG] THIRD")
			if (strategySelected == false) {
				if (strategyImageName != "default") {
					findAndTapImage("race_change_strategy", tries = 10, region = imageUtils.regionBottomHalf)
					findAndTapImage(strategyImageName + "_select", tries = 10, region = imageUtils.regionBottomHalf)
					findAndTapImage("confirm", tries = 10, region = imageUtils.regionBottomHalf)
					wait(1.0)
				}
				strategySelected = true;
			}

			// Skip the race if possible, otherwise run it manually.
			val resultCheck: Boolean = if (imageUtils.findImage("race_skip_locked", tries = 5, region = imageUtils.regionBottomHalf).first == null) {
				skipRace()
			} else {
				manualRace()
			}

			finishRace(resultCheck, isExtra = true)

			printToLog("[RACE] Racing process for Extra Race is completed.")
			return true
		}

		return false
	}

	/**
	 * The entry point for handling standalone races if the user started the bot on the Racing screen.
	 */
	fun handleStandaloneRace() {
		printToLog("\n[RACE] Starting Standalone Racing process...")

		// Select the preferred race strategy if it is not already selected.
		printToLog("[DEBUG] FIRST")
		if (strategySelected == false) {
			if (strategyImageName != "default") {
				findAndTapImage("race_change_strategy", tries = 10, region = imageUtils.regionBottomHalf)
				findAndTapImage(strategyImageName + "_select", tries = 10, region = imageUtils.regionBottomHalf)
				findAndTapImage("confirm", tries = 10, region = imageUtils.regionBottomHalf)
				wait(1.0)
			}
			strategySelected = true;
		}

		// Skip the race if possible, otherwise run it manually.
		val resultCheck: Boolean = if (imageUtils.findImage("race_skip_locked", tries = 5, region = imageUtils.regionBottomHalf).first == null) {
			skipRace()
		} else {
			manualRace()
		}

		finishRace(resultCheck)

		printToLog("[RACE] Racing process for Standalone Race is completed.")
	}

	/**
	 * Skips the current race to get to the results screen.
	 *
	 * @return True if the bot completed the race with retry attempts remaining. Otherwise false.
	 */
	private fun skipRace(): Boolean {
		while (raceRetries >= 0) {
			printToLog("[RACE] Skipping race...")

			// Press the skip button and then wait for your result of the race to show.
			if (findAndTapImage("race_skip", tries = 30, region = imageUtils.regionBottomHalf)) {
				printToLog("[RACE] Race was able to be skipped.")
			}
			wait(2.0)

			// Now tap on the screen to get past the Race Result screen.
			tap(350.0, 450.0, "ok", taps = 3)

			// Check if the race needed to be retried.
			if (imageUtils.findImage("race_retry", tries = 5, region = imageUtils.regionBottomHalf, suppressError = true).first != null) {
				if (disableRaceRetries) {
					printToLog("\n[END] Stopping the bot due to failing a mandatory race.")
					notificationMessage = "Stopping the bot due to failing a mandatory race."
					throw IllegalStateException()
				}
				findAndTapImage("race_retry", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true)
				printToLog("[RACE] The skipped race failed and needs to be run again. Attempting to retry...")
				wait(3.0)
				raceRetries--
			} else {
				return true
			}
		}

		return false
	}

	/**
	 * Manually runs the current race to get to the results screen.
	 *
	 * @return True if the bot completed the race with retry attempts remaining. Otherwise false.
	 */
	private fun manualRace(): Boolean {
		while (raceRetries >= 0) {
			printToLog("[RACE] Skipping manual race...")

			// Press the manual button.
			if (findAndTapImage("race_manual", tries = 30, region = imageUtils.regionBottomHalf)) {
				printToLog("[RACE] Started the manual race.")
			}
			wait(2.0)

			// Confirm the Race Playback popup if it appears.
			if (findAndTapImage("ok", tries = 1, region = imageUtils.regionMiddle, suppressError = true)) {
				printToLog("[RACE] Confirmed the Race Playback popup.")
				wait(5.0)
			}

			waitForLoading()

			// Now press the confirm button to get past the list of participants.
			if (findAndTapImage("race_confirm", tries = 30, region = imageUtils.regionBottomHalf)) {
				printToLog("[RACE] Dismissed the list of participants.")
			}
			waitForLoading()
			wait(1.0)
			waitForLoading()
			wait(1.0)

			// Skip the part where it reveals the name of the race.
			if (findAndTapImage("race_skip_manual", tries = 30, region = imageUtils.regionBottomHalf)) {
				printToLog("[RACE] Skipped the name reveal of the race.")
			}
			// Skip the walkthrough of the starting gate.
			if (findAndTapImage("race_skip_manual", tries = 30, region = imageUtils.regionBottomHalf)) {
				printToLog("[RACE] Skipped the walkthrough of the starting gate.")
			}
			wait(3.0)
			// Skip the start of the race.
			if (findAndTapImage("race_skip_manual", tries = 30, region = imageUtils.regionBottomHalf)) {
				printToLog("[RACE] Skipped the start of the race.")
			}
			// Skip the lead up to the finish line.
			if (findAndTapImage("race_skip_manual", tries = 30, region = imageUtils.regionBottomHalf)) {
				printToLog("[RACE] Skipped the lead up to the finish line.")
			}
			wait(2.0)
			// Skip the result screen.
			if (findAndTapImage("race_skip_manual", tries = 30, region = imageUtils.regionBottomHalf)) {
				printToLog("[RACE] Skipped the results screen.")
			}
			wait(2.0)

			waitForLoading()
			wait(1.0)

			// Check if the race needed to be retried.
			if (imageUtils.findImage("race_retry", tries = 5, region = imageUtils.regionBottomHalf, suppressError = true).first != null) {
				if (disableRaceRetries) {
					printToLog("\n[END] Stopping the bot due to failing a mandatory race.")
					notificationMessage = "Stopping the bot due to failing a mandatory race."
					throw IllegalStateException()
				}
				findAndTapImage("race_retry", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true)
				printToLog("[RACE] Manual race failed and needs to be run again. Attempting to retry...")
				wait(5.0)
				raceRetries--
			} else {
				// Check if a Trophy was acquired.
				if (findAndTapImage("race_accept_trophy", tries = 5, region = imageUtils.regionBottomHalf)) {
					printToLog("[RACE] Closing popup to claim trophy...")
				}

				return true
			}
		}

		return false
	}

	/**
	 * Finishes up and confirms the results of the race and its success.
	 *
	 * @param resultCheck Flag to see if the race was completed successfully. Throws an IllegalStateException if it did not.
	 * @param isExtra Flag to determine the following actions to finish up this mandatory or extra race.
	 */
	private fun finishRace(resultCheck: Boolean, isExtra: Boolean = false) {
		printToLog("\n[RACE] Now performing cleanup and finishing the race.")
		if (!resultCheck) {
			notificationMessage = "Bot has run out of retry attempts for racing. Stopping the bot now..."
			throw IllegalStateException()
		}

		// Bot will be at the screen where it shows the final positions of all participants.
		// Press the confirm button and wait to see the triangle of fans.
		printToLog("[RACE] Now attempting to confirm the final positions of all participants and number of gained fans")
		if (findAndTapImage("next", tries = 30, region = imageUtils.regionBottomHalf)) {
			wait(0.5)

			// Now tap on the screen to get to the next screen.
			tap(350.0, 750.0, "ok", taps = 3)

			// Now press the end button to finish the race.
			findAndTapImage("race_end", tries = 30, region = imageUtils.regionBottomHalf)

			if (!isExtra) {
				printToLog("[RACE] Seeing if a Training Goal popup will appear.")
				// Wait until the popup showing the completion of a Training Goal appears and confirm it.
				// There will be dialog before it so the delay should be longer.
				wait(5.0)
				if (findAndTapImage("next", tries = 10, region = imageUtils.regionBottomHalf)) {
					wait(2.0)

					// Now confirm the completion of a Training Goal popup.
					printToLog("[RACE] There was a Training Goal popup. Confirming it now.")
					findAndTapImage("next", tries = 10, region = imageUtils.regionBottomHalf)
				}
			} else if (findAndTapImage("next", tries = 10, region = imageUtils.regionBottomHalf)) {
				// Same as above but without the longer delay.
				wait(2.0)
				findAndTapImage("race_end", tries = 10, region = imageUtils.regionBottomHalf)
			}
		} else {
			printToLog("[ERROR] Cannot start the cleanup process for finishing the race. Moving on...", isError = true)
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// Helper Functions

	fun updatePreferredDistance() {
		printToLog("\n[STATS] Updating preferred distance.")
		if (findAndTapImage("main_status", tries = 1, region = imageUtils.regionMiddle)) {
			preferredDistance = imageUtils.determinePreferredDistance()
			findAndTapImage("race_accept_trophy", tries = 1, region = imageUtils.regionBottomHalf)
			printToLog("[STATS] Preferred distance set to $preferredDistance.")
		}
	}

	/**
	 * Updates the current stat value mapping by reading the character's current stats from the Main screen.
	 */
	fun updateStatValueMapping() {
		printToLog("\n[STATS] Updating stat value mapping.")
		currentStatsMap = imageUtils.determineStatValues(currentStatsMap)
		// Print the updated stat value mapping here.
		currentStatsMap.forEach { it ->
			printToLog("[STATS] ${it.key}: ${it.value}")
		}
		printToLog("[STATS] Stat value mapping updated.\n")
	}

	/**
	 * Updates the stored date in memory by keeping track of the current year, phase, month and current turn number.
	 */
	fun updateDate() {
		printToLog("\n[DATE] Updating the current turn number.")
		val dateString = imageUtils.determineDayNumber()
		currentDate = textDetection.determineDateFromString(dateString)
		printToLog("\n[DATE] It is currently $currentDate.")
	}

	/**
	 * Handles the Inheritance event if detected on the screen.
	 *
	 * @return True if the Inheritance event happened and was accepted. Otherwise false.
	 */
	fun handleInheritanceEvent(): Boolean {
		return if (inheritancesDone < 2) {
			if (findAndTapImage("inheritance", tries = 1, region = imageUtils.regionBottomHalf)) {
				inheritancesDone++
				true
			} else {
				false
			}
		} else {
			false
		}
	}

	/**
	 * Attempt to recover energy.
	 * During summer, be more selective about resting to maximize Lv5 training benefits.
	 *
	 * @return True if the bot successfully recovered energy. Otherwise false.
	 */
	private fun recoverEnergy(): Boolean {
		printToLog("\n[ENERGY] Now starting attempt to recover energy.")

		// Check if it's summer
		val isSummer = imageUtils.findImage("recover_energy_summer", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true).first != null

		if (isSummer) {
			// During summer, only rest if we really need it
			// Summer rest gives +40 energy and +1 mood level
			printToLog("[ENERGY] Summer training active. Rest provides +40 energy and +1 mood.")

			// Try to estimate current energy based on failure rates if available
			val avgFailureRate = trainingMap.values
				.filter { it.failureChance >= 0 }
				.map { it.failureChance }
				.average()

			if (!avgFailureRate.isNaN()) {
				val estimatedEnergy = (50 - avgFailureRate * 2).coerceIn(0.0, 100.0)
				printToLog("[ENERGY] Estimated energy: ${estimatedEnergy.toInt()}% based on training failure rates")

				// Only rest during summer if energy is quite low
				if (estimatedEnergy >= 40) {
					printToLog("[ENERGY] Summer: Energy is sufficient (~${estimatedEnergy.toInt()}%). Skipping rest to maximize Lv5 training.")
					return false
				}
			}

			// If we do need to rest during summer
			if (findAndTapImage("recover_energy_summer", tries = 1, imageUtils.regionBottomHalf)) {
				findAndTapImage("ok")
				printToLog("[ENERGY] Summer: Low energy detected. Using summer rest for recovery.")
				raceRepeatWarningCheck = false
				return true
			}
		}

		// Normal energy recovery (non-summer)
		return when {
			findAndTapImage("recover_energy", tries = 1, imageUtils.regionBottomHalf) -> {
				findAndTapImage("ok")
				printToLog("[ENERGY] Successfully recovered energy.")
				raceRepeatWarningCheck = false
				true
			}
			else -> {
				printToLog("[ENERGY] Failed to recover energy. Moving on...")
				false
			}
		}
	}

	/**
	 * Attempt to recover mood to always maintain at least Above Normal mood.
	 * Never recovers mood on turn 1 to avoid wasting the random chance opportunity.
	 *
	 * @return True if the bot successfully recovered mood. Otherwise false.
	 */
	fun recoverMood(): Boolean {
		printToLog("\n[MOOD] Detecting current mood.")

		// Detect what Mood the bot is at.
		val currentMood: String = when {
			imageUtils.findImage("mood_normal", tries = 1, region = imageUtils.regionTopHalf, suppressError = true).first != null -> {
				"Normal"
			}
			imageUtils.findImage("mood_good", tries = 1, region = imageUtils.regionTopHalf, suppressError = true).first != null -> {
				"Good"
			}
			imageUtils.findImage("mood_great", tries = 1, region = imageUtils.regionTopHalf, suppressError = true).first != null -> {
				"Great"
			}
			else -> {
				"Bad/Awful"
			}
		}

		printToLog("[MOOD] Detected mood to be $currentMood.")

		// Never recover mood on turn 1 (random chance opportunity)
		if (currentDate.turnNumber == 1) {
			printToLog("[MOOD] Turn 1 detected. Never recovering mood on turn 1 to utilize random chance opportunity.")
			return false
		}

		// Check if it's summer
		val isSummer = imageUtils.findImage("recover_energy_summer", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true).first != null

		// During summer, only recover mood if it's Bad/Awful (not Normal) since we want to maximize training
		if (isSummer) {
			if (currentMood == "Bad/Awful") {
				printToLog("[MOOD] Summer: Current mood is Bad/Awful. Using summer rest for mood recovery.")
				findAndTapImage("recover_energy_summer", tries = 1, region = imageUtils.regionBottomHalf)
				findAndTapImage("ok", region = imageUtils.regionMiddle, suppressError = true)
				raceRepeatWarningCheck = false
				return true
			} else {
				printToLog("[MOOD] Summer: Current mood is $currentMood. Skipping rest to maximize Lv5 training opportunities.")
				return false
			}
		}

		// Normal (non-summer) mood recovery logic
		return if (firstTrainingCheck && currentMood == "Normal") {
			printToLog("[MOOD] Current mood is Normal. Not recovering mood due to firstTrainingCheck flag being active. Will need to complete a training first before being allowed to recover mood.")
			false
		} else if (currentMood == "Bad/Awful" || currentMood == "Normal") {
			printToLog("[MOOD] Current mood is not good. Recovering mood now.")
			if (!findAndTapImage("recover_mood", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true)) {
				// Fallback to summer rest if available (shouldn't happen since we check isSummer above)
				findAndTapImage("recover_energy_summer", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true)
			}

			// Do the date if it is unlocked.
			if (findAndTapImage("recover_mood_date", tries = 1, region = imageUtils.regionMiddle, suppressError = true)) {
				wait(1.0)
			}

			findAndTapImage("ok", region = imageUtils.regionMiddle, suppressError = true)
			raceRepeatWarningCheck = false
			true
		} else {
			printToLog("[MOOD] Current mood is good enough. Moving on...")
			false
		}
	}

	/**
	 * Prints the training map object for informational purposes.
	 */
	private fun printTrainingMap() {
		printToLog("\n[INFO] Stat Gains by Training:")
		trainingMap.forEach { name, training ->
			printToLog("[TRAINING] $name Training stat gains: ${training.statGains.contentToString()}, failure chance: ${training.failureChance}%.")
		}
	}

	/**
	 * Perform misc checks to potentially fix instances where the bot is stuck.
	 *
	 * @return True if the checks passed. Otherwise false if the bot encountered a warning popup and needs to exit.
	 */
	fun performMiscChecks(): Boolean {
		printToLog("\n[INFO] Beginning check for misc cases...")

		if (enablePopupCheck && imageUtils.findImage("cancel", tries = 1, region = imageUtils.regionBottomHalf).first != null &&
			imageUtils.findImage("recover_mood_date", tries = 1, region = imageUtils.regionMiddle).first == null) {
			printToLog("\n[END] Bot may have encountered a warning popup. Exiting now...")
			notificationMessage = "Bot may have encountered a warning popup"
			return false
		} else if (findAndTapImage("next", tries = 1, region = imageUtils.regionBottomHalf)) {
			// Now confirm the completion of a Training Goal popup.
			wait(2.0)
			findAndTapImage("next", tries = 1, region = imageUtils.regionBottomHalf)
			wait(1.0)
		} else if (imageUtils.findImage("crane_game", tries = 1, region = imageUtils.regionBottomHalf).first != null) {
			findAndTapImage("crane_game", tries = 1, region = imageUtils.regionBottomHalf)
			wait(5.0)
		} else if (
			imageUtils.findImage("ordinary_cuties", tries = 1, region = imageUtils.regionBottomHalf).first != null &&
			imageUtils.findImage("crane_ok", tries = 1, region = imageUtils.regionBottomHalf).first != null) {
			findAndTapImage("crane_ok", tries=1, region = imageUtils.regionBottomHalf)
			wait(1.0)
		}  else if (findAndTapImage("race_retry", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true)) {
			printToLog("[INFO] There is a race retry popup.")
			wait(5.0)
		} else if (findAndTapImage("race_accept_trophy", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true)) {
			printToLog("[INFO] There is a possible popup to accept a trophy.")
			finishRace(true, isExtra = true)
		} else if (findAndTapImage("race_end", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true)) {
			printToLog("[INFO] Ended a leftover race.")
		} else if (imageUtils.findImage("connection_error", tries = 1, region = imageUtils.regionMiddle, suppressError = true).first != null) {
			printToLog("\n[END] Bot will stop due to detecting a connection error.")
			notificationMessage = "Bot will stop due to detecting a connection error."
			return false
		} else if (imageUtils.findImage("race_not_enough_fans", tries = 1, region = imageUtils.regionMiddle, suppressError = true).first != null) {
			printToLog("[INFO] There was a popup about insufficient fans.")
			encounteredRacingPopup = true
			findAndTapImage("cancel", region = imageUtils.regionBottomHalf)
		} else if (findAndTapImage("back", tries = 1, region = imageUtils.regionBottomHalf, suppressError = true)) {
			wait(1.0)
		} else if (!BotService.isRunning) {
			throw InterruptedException()
		} else {
			printToLog("[INFO] Did not detect any popups or the Crane Game on the screen. Moving on...")
		}

		return true
	}

	/**
	 * Bot will begin automation here.
	 *
	 * @return True if all automation goals have been met. False otherwise.
	 */
	fun start(): Boolean {
		// Print current app settings at the start of the run.
		SettingsPrinter.printCurrentSettings(myContext) { message ->
			printToLog(message)
		}

		// Update the stat targets by distances.
		setStatTargetsByDistances()

		// If debug mode is off, then it is necessary to wait a few seconds for the Toast message to disappear from the screen to prevent it obstructing anything beneath it.
		if (!debugMode) {
			wait(5.0)
		}

		// Print device and version information.
		printToLog("[INFO] Device Information: ${MediaProjectionService.displayWidth}x${MediaProjectionService.displayHeight}, DPI ${MediaProjectionService.displayDPI}")
		if (MediaProjectionService.displayWidth != 1080) printToLog("[WARNING] ⚠️ Bot performance will be severely degraded since display width is not 1080p unless an appropriate scale is set for your device.")
		if (debugMode) printToLog("[WARNING] ⚠️ Debug Mode is enabled. All bot operations will be significantly slower as a result.")
		if (sharedPreferences.getInt("customScale", 100).toDouble() / 100.0 != 1.0) printToLog("[INFO] Manual scale has been set to ${sharedPreferences.getInt("customScale", 100).toDouble() / 100.0}")
		printToLog("[WARNING] ⚠️ Note that certain Android notification styles (like banners) are big enough that they cover the area that contains the Mood which will interfere with mood recovery logic in the beginning.")
		val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
			myContext.packageManager.getPackageInfo(myContext.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
		} else {
			myContext.packageManager.getPackageInfo(myContext.packageName, 0)
		}
		val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
			packageInfo.longVersionCode
		} else {
			@Suppress("DEPRECATION")
			packageInfo.versionCode.toLong()
		}
		printToLog("[INFO] Bot version: ${packageInfo.versionName} ($versionCode)\n\n")

		val startTime: Long = System.currentTimeMillis()

		// Start debug tests here if enabled.
		if (sharedPreferences.getBoolean("debugMode_startTemplateMatchingTest", false)) {
			startTemplateMatchingTest()
		} else if (sharedPreferences.getBoolean("debugMode_startSingleTrainingFailureOCRTest", false)) {
			startSingleTrainingFailureOCRTest()
		} else if (sharedPreferences.getBoolean("debugMode_startComprehensiveTrainingFailureOCRTest", false)) {
			startComprehensiveTrainingFailureOCRTest()
		}
		// Otherwise, proceed with regular bot operations.
		else if (campaign == "Ao Haru") {
			val aoHaruCampaign = AoHaru(this)
			aoHaruCampaign.start()
		} else {
			val uraFinaleCampaign = Campaign(this)
			uraFinaleCampaign.start()
		}

		val endTime: Long = System.currentTimeMillis()
		Log.d(tag, "Total Runtime: ${endTime - startTime}ms")

		return true
	}
}