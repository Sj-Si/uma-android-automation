package com.steve1316.uma_android_automation.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Utility class for printing SharedPreferences settings in a consistent format.
 * Can be used by both HomeFragment and Game.kt to display current bot configuration.
 */
object SettingsPrinter {
	
	/**
	 * Print all current SharedPreferences settings for debugging purposes.
	 * 
	 * @param context The application context
	 * @param MessageLog.log Function to handle logging
	 */
	fun printCurrentSettings(context: Context, MessageLog.log: ((String) -> Unit)? = null): String {
		val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
		
		// Main Settings
		val campaign: String = sharedPreferences.getString("campaign", "")!!
        val strategy: String = sharedPreferences.getString("strategy", "")!!
		val enableFarmingFans = sharedPreferences.getBoolean("enableFarmingFans", false)
		val daysToRunExtraRaces: Int = sharedPreferences.getInt("daysToRunExtraRaces", 4)
		val enableSkillPointCheck: Boolean = sharedPreferences.getBoolean("enableSkillPointCheck", false)
		val skillPointCheck: Int = sharedPreferences.getInt("skillPointCheck", 750)
		val enablePopupCheck: Boolean = sharedPreferences.getBoolean("enablePopupCheck", false)
		val disableRaceRetries: Boolean = sharedPreferences.getBoolean("disableRaceRetries", false)
		val enableStopOnMandatoryRace: Boolean = sharedPreferences.getBoolean("enableStopOnMandatoryRace", false)
		val enableForceRacing: Boolean = sharedPreferences.getBoolean("enableForceRacing", false)
		val enablePrioritizeEnergyOptions: Boolean = sharedPreferences.getBoolean("enablePrioritizeEnergyOptions", false)
        val enableSkipCraneGame: Boolean = sharedPreferences.getBoolean("enableSkipCraneGame", false)
		
		// Training Settings
		val trainingBlacklist: Set<String> = sharedPreferences.getStringSet("trainingBlacklist", setOf<String>()) as Set<String>
		var statPrioritization: List<String> = sharedPreferences.getString("statPrioritization", "Speed|Stamina|Power|Wit|Guts")!!.split("|")
		val maximumFailureChance: Int = sharedPreferences.getInt("maximumFailureChance", 15)
		val disableTrainingOnMaxedStat: Boolean = sharedPreferences.getBoolean("disableTrainingOnMaxedStat", true)
		val focusOnSparkStatTarget: Boolean = sharedPreferences.getBoolean("focusOnSparkStatTarget", false)
		
		// Training Stat Targets
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
		
		// Training Event Settings
		val character = sharedPreferences.getString("character", "Please select one in the Training Event Settings")!!
		val selectAllCharacters = sharedPreferences.getBoolean("selectAllCharacters", true)
		val supportList = sharedPreferences.getString("supportList", "")?.split("|")!!
		val selectAllSupportCards = sharedPreferences.getBoolean("selectAllSupportCards", true)
		
		// OCR Optimization Settings
		val threshold: Int = sharedPreferences.getInt("threshold", 230)
		val enableAutomaticRetry: Boolean = sharedPreferences.getBoolean("enableAutomaticRetry", true)
		val ocrConfidence: Int = sharedPreferences.getInt("ocrConfidence", 80)
		
		// Debug Options
		val debugMode: Boolean = sharedPreferences.getBoolean("debugMode", false)
		val confidence: Int = sharedPreferences.getInt("confidence", 80)
		val customScale: Int = sharedPreferences.getInt("customScale", 100)
		val debugModeStartTemplateMatchingTest: Boolean = sharedPreferences.getBoolean("debugMode_startTemplateMatchingTest", false)
		val debugModeStartSingleTrainingFailureOCRTest: Boolean = sharedPreferences.getBoolean("debugMode_startSingleTrainingFailureOCRTest", false)
		val debugModeStartComprehensiveTrainingFailureOCRTest: Boolean = sharedPreferences.getBoolean("debugMode_startComprehensiveTrainingFailureOCRTest", false)
		val hideComparisonResults: Boolean = sharedPreferences.getBoolean("hideComparisonResults", true)

		if (statPrioritization.isEmpty() || statPrioritization[0] == "") {
			statPrioritization = listOf("Speed", "Stamina", "Power", "Wit", "Guts")
		}

		////////////////////////////////////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		// Construct display strings.
		val campaignString: String = if (campaign != "") {
			"🎯 $campaign"
		} else {
			"⚠️ Please select one in the Select Campaign option"
		}

        val strategyString: String = if (strategy != "") {
            "🎯 $strategy"
        } else {
            "⚠️ Please select one in the Select Race Strategy option"
        }
		
		val characterString: String = if (selectAllCharacters) {
			"👥 All Characters Selected"
		} else if (character == "" || character.contains("Please select")) {
			"⚠️ Please select one in the Training Event Settings"
		} else {
			"👤 $character"
		}
		
		val supportCardListString: String = if (selectAllSupportCards) {
			"🃏 All Support Cards Selected"
		} else if (supportList.isEmpty() || supportList[0] == "") {
			"⚠️ None Selected"
		} else {
			"�� ${supportList.joinToString(", ")}"
		}
		
		val trainingBlacklistString: String = if (trainingBlacklist.isEmpty()) {
			"✅ No Trainings blacklisted"
		} else {
			val defaultTrainingOrder = listOf("Speed", "Stamina", "Power", "Guts", "Wit")
			val sortedBlacklist = trainingBlacklist.sortedBy { defaultTrainingOrder.indexOf(it) }
			"🚫 ${sortedBlacklist.joinToString(", ")}"
		}
		
		val statPrioritizationString: String = if (statPrioritization.isEmpty() || statPrioritization[0] == "") {
			"�� Using Default Stat Prioritization: Speed, Stamina, Power, Guts, Wit"
		} else {
			"📊 Stat Prioritization: ${statPrioritization.joinToString(", ")}"
		}
		
		val focusOnSparkString: String = if (focusOnSparkStatTarget) {
			"✨ Focus on Sparks for Stat Targets: ✅"
		} else {
			"✨ Focus on Sparks for Stat Targets: ❌"
		}
		
		val sprintTargetsString = "Sprint: \n\t\tSpeed: $sprintSpeedTarget\t\tStamina: $sprintStaminaTarget\t\tPower: $sprintPowerTarget\n\t\tGuts: $sprintGutsTarget\t\t\tWit: $sprintWitTarget"
		val mileTargetsString = "Mile: \n\t\tSpeed: $mileSpeedTarget\t\tStamina: $mileStaminaTarget\t\tPower: $milePowerTarget\n\t\tGuts: $mileGutsTarget\t\t\tWit: $mileWitTarget"
		val mediumTargetsString = "Medium: \n\t\tSpeed: $mediumSpeedTarget\t\tStamina: $mediumStaminaTarget\t\tPower: $mediumPowerTarget\n\t\tGuts: $mediumGutsTarget\t\t\tWit: $mediumWitTarget"
		val longTargetsString = "Long: \n\t\tSpeed: $longSpeedTarget\t\tStamina: $longStaminaTarget\t\tPower: $longPowerTarget\n\t\tGuts: $longGutsTarget\t\t\tWit: $longWitTarget"

		////////////////////////////////////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		// Build the settings string.
		val settingsString = buildString {
			appendLine("Campaign Selected: $campaignString")
			appendLine()
            appendLine("Race Strategy Selected: $strategyString")
            appendLine()
			appendLine("---------- Training Event Options ----------")
			appendLine("Character Selected: $characterString")
			appendLine("Support(s) Selected: $supportCardListString")
			appendLine()
			appendLine("---------- Training Options ----------")
			appendLine("Training Blacklist: $trainingBlacklistString")
			appendLine(statPrioritizationString)
			appendLine("Maximum Failure Chance Allowed: $maximumFailureChance%")
			appendLine("Disable Training on Maxed Stat: ${if (disableTrainingOnMaxedStat) "✅" else "❌"}")
			appendLine(focusOnSparkString)
			appendLine()
			appendLine("---------- Training Stat Targets by Distance ----------")
			appendLine(sprintTargetsString)
			appendLine(mileTargetsString)
			appendLine(mediumTargetsString)
			appendLine(longTargetsString)
			appendLine()
			appendLine("---------- Tesseract OCR Optimization ----------")
			appendLine("OCR Threshold: $threshold")
			appendLine("Enable Automatic OCR retry: ${if (enableAutomaticRetry) "✅" else "❌"}")
			appendLine("Minimum OCR Confidence: $ocrConfidence")
			appendLine()
			appendLine("---------- Racing Options ----------")
			appendLine("Prioritize Farming Fans: ${if (enableFarmingFans) "✅" else "❌"}")
			appendLine("Modulo Days to Farm Fans: ${if (enableFarmingFans) "📅 $daysToRunExtraRaces days" else "❌"}")
			appendLine("Disable Race Retries: ${if (disableRaceRetries) "✅" else "❌"}")
			appendLine("Stop on Mandatory Race: ${if (enableStopOnMandatoryRace) "✅" else "❌"}")
			appendLine("Force Racing Every Day: ${if (enableForceRacing) "✅" else "❌"}")
			appendLine()
			appendLine("---------- Misc Options ----------")
			appendLine("Skill Point Check: ${if (enableSkillPointCheck) "✅ Stop on $skillPointCheck Skill Points or more" else "❌"}")
			appendLine("Popup Check: ${if (enablePopupCheck) "✅" else "❌"}")
			appendLine("Prioritize Energy Options: ${if (enablePrioritizeEnergyOptions) "✅" else "❌"}")
            appendLine("Skip Crane Game: ${if (enableSkipCraneGame) "✅" else "❌"}")
			appendLine()
			appendLine("---------- Debug Options ----------")
			appendLine("Debug Mode: ${if (debugMode) "✅" else "❌"}")
			appendLine("Minimum Template Match Confidence: $confidence")
			appendLine("Custom Scale: ${customScale.toDouble() / 100.0}")
			appendLine("Start Template Matching Test: ${if (debugModeStartTemplateMatchingTest) "✅" else "❌"}")
			appendLine("Start Single Training Failure OCR Test: ${if (debugModeStartSingleTrainingFailureOCRTest) "✅" else "❌"}")
			appendLine("Start Comprehensive Training Failure OCR Test: ${if (debugModeStartComprehensiveTrainingFailureOCRTest) "✅" else "❌"}")
			appendLine("Hide String Comparison Results: ${if (hideComparisonResults) "✅" else "❌"}")
		}

		////////////////////////////////////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		// Use the provided MessageLog.log function if available. Otherwise return the string.
		if (MessageLog.log != null) {
			MessageLog.log("\n[SETTINGS] Current Bot Configuration:")
			MessageLog.log("=====================================")
			settingsString.split("\n").forEach { line ->
				if (line.isNotEmpty()) {
					MessageLog.log(line)
				}
			}
			MessageLog.log("=====================================\n")
		}

		return settingsString
	}
	
	/**
	 * Get the formatted settings string for display in UI components.
	 * 
	 * @param context The application context
	 * @return Formatted string containing all current settings
	 */
	fun getSettingsString(context: Context): String {
		return printCurrentSettings(context)
	}
} 