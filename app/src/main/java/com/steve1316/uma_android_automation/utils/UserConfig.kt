package com.steve1316.uma_android_automation.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

val DEFAULT_STAT_TARGETS_SPRINT = intArrayOf(1200, 400, 900, 400, 600)
val DEFAULT_STAT_TARGETS_MILE = intArrayOf(1100, 500, 800, 500, 600)
val DEFAULT_STAT_TARGETS_MEDIUM = intArrayOf(1000, 700, 700, 500, 600)
val DEFAULT_STAT_TARGETS_LONG = intArrayOf(800, 1000, 600, 600, 600)

data class TrainingStatTargets(
    private val preferences: SharedPreferences,
    private val prefix: String,
    private val defaults: IntArray,
) {
    var speed: Int
    var stamina: Int
    var power: Int
    var guts: Int
    var wit: Int

    init {
        speed = preferences.getInt("${prefix}_speedStatTarget", defaults[0])
        stamina = preferences.getInt("${prefix}_staminaStatTarget", defaults[1])
        power = preferences.getInt("${prefix}_powerStatTarget", defaults[2])
        guts = preferences.getInt("${prefix}_gutsStatTarget", defaults[3])
        wit = preferences.getInt("${prefix}_witStatTarget", defaults[4])
    }

    fun get(propertyName: String): Int {
        return when(propertyName) {
            "speed" -> speed
            "stamina" -> stamina
            "power" -> power
            "guts" -> guts
            "wit" -> wit
            else -> throw IllegalArgumentException("Invalid propertyName: ${propertyName}")
        }
    }

    fun asIntArray(): IntArray {
        return intArrayOf(speed, stamina, power, guts, wit)
    }
}

data class TrainingOptions(private val preferences: SharedPreferences) {
    var trainingBlacklist: List<String>
    var statPriority: List<String>
    var bEnableTrainMaxedStat: Boolean
    var maxFailChance: Int
    var bFocusSparks: Boolean
    
    // Training stat targets by distance
    var statTargetSprint: TrainingStatTargets
    var statTargetMile: TrainingStatTargets
    var statTargetMedium: TrainingStatTargets
    var statTargetLong: TrainingStatTargets

    // Mapping of string to targets for easier access.
    private var distanceToStatTargetMap: MutableMap<String, TrainingStatTargets>

    init {
        trainingBlacklist = preferences.getStringSet("trainingBlacklist", setOf())!!.toList()
        statPriority = preferences.getString("Speed|Stamina|Power|Guts|Wit", "")!!.split("|")
        bEnableTrainMaxedStat = preferences.getBoolean("bEnableTrainMaxedStat", false)
        maxFailChance = preferences.getInt("maxFailChance", 15)
        bFocusSparks = preferences.getBoolean("bFocusSparks", false)

        statTargetSprint = TrainingStatTargets(preferences, "trainingSprintStatTarget", DEFAULT_STAT_TARGETS_SPRINT)
        statTargetMile = TrainingStatTargets(preferences, "trainingMileStatTarget", DEFAULT_STAT_TARGETS_MILE)
        statTargetMedium = TrainingStatTargets(preferences, "trainingMediumStatTarget", DEFAULT_STAT_TARGETS_MEDIUM)
        statTargetLong = TrainingStatTargets(preferences, "trainingLongStatTarget", DEFAULT_STAT_TARGETS_LONG)

        distanceToStatTargetMap = mutableMapOf(
            "Sprint" to statTargetSprint,
            "Mile" to statTargetMile,
            "Medium" to statTargetMedium,
            "Long" to statTargetLong,
        )
    }

    fun getTrainingStatTargets(distance: String): TrainingStatTargets {
        return distanceToStatTargetMap.getOrDefault(distance, statTargetSprint)
    }
}

data class TrainingEventOptions(private val preferences: SharedPreferences) {
    var bSelectAllCharacters: Boolean
    var selectedCharacter: String
    var bSelectAllSupportCards: Boolean
    var selectedSupportCards: List<String>

    init {
        bSelectAllCharacters = preferences.getBoolean("bSelectAllCharacters", true)
        selectedCharacter = preferences.getString("selectedCharacter", "")!!
        bSelectAllSupportCards = preferences.getBoolean("bSelectAllSupportCards", true)
        selectedSupportCards = preferences.getString("selectedSupportCards", "")!!.split("|")
    }
}

data class OcrOptions(private val preferences: SharedPreferences) {
    var ocrThreshold: Double
    var bEnableOcrAutomaticRetry: Boolean
    var ocrConfidencePercent: Int // the raw setting value
    var ocrConfidence: Double

    init {
        ocrThreshold = preferences.getInt("ocrThreshold", 230).toDouble()
        bEnableOcrAutomaticRetry = preferences.getBoolean("bEnableOcrAutomaticRetry", true)
        ocrConfidencePercent = preferences.getInt("ocrConfidence", 80)
        ocrConfidence = ocrConfidencePercent.toDouble() / 100.0
    }
}

// Main Settings page
data class Config(
    private val preferences: SharedPreferences
) {
    var campaign: String
    var strategy: String
    // Sub-categories
    var training: TrainingOptions
    var events: TrainingEventOptions
    var ocr: OcrOptions
    // RACING
    var bEnableFarmingFans: Boolean
    var daysToRunExtraRaces: Int
    var bEnableRaceRetries: Boolean
    var bEnableStopOnMandatoryRace: Boolean
    var bEnableForceRacing: Boolean
    // MISC
    var bEnableSkillPointCheck: Boolean
    var skillPointCheckThreshold: Int
    var bEnablePopupCheck: Boolean
    var bEnablePrioritizeEnergy: Boolean
    var bEnableSkipCraneGame: Boolean
    // DEBUG
    var bEnableDebugMode: Boolean
    var debugOcrConfidencePercent: Int
    var debugOcrConfidence: Double
    var debugOcrScale: Double
    var bRunTemplateMatchingTest: Boolean
    var bRunSingleTrainingFailureOcrTest: Boolean
    var bRunComprehensiveTrainingFailureOcrTest: Boolean
    var bHideComparisonResults: Boolean

    var strategyImageName: String

    init {
        // Options
        campaign = preferences.getString("campaign", "")!!
        strategy = preferences.getString("strategy", "")!!

        strategyImageName = when (strategy) {
            "Front Runner" -> "strategy_front"
            "Pace Chaser" -> "strategy_pace"
            "Late Surger" -> "strategy_late"
            "End Closer" -> "strategy_end"
            else -> "default"
        }
        
        // sub-menus
        training = TrainingOptions(preferences)
        events = TrainingEventOptions(preferences)
        ocr = OcrOptions(preferences)

        // Racing Options
        bEnableFarmingFans = preferences.getBoolean("bEnableFarmingFans", false)
        daysToRunExtraRaces = preferences.getInt("daysToRunExtraRaces", 4)
        bEnableRaceRetries = preferences.getBoolean("bEnableRaceRetries", true)
        bEnableStopOnMandatoryRace = preferences.getBoolean("bEnableStopOnMandatoryRace", false)
        bEnableForceRacing = preferences.getBoolean("bEnableForceRacing", false)
        
        // Misc Options
        bEnableSkillPointCheck = preferences.getBoolean("bEnableSkillPointCheck", false)
        skillPointCheckThreshold = preferences.getInt("skillPointCheckThreshold", 750)
        bEnablePopupCheck = preferences.getBoolean("bEnablePopupCheck", false)
        bEnablePrioritizeEnergy = preferences.getBoolean("bEnablePrioritizeEnergy", true)
        bEnableSkipCraneGame = preferences.getBoolean("bEnableSkipCraneGame", false)
        
        // Debug Options
        bEnableDebugMode = preferences.getBoolean("bEnableDebugMode", false)
        debugOcrConfidencePercent = preferences.getInt("debugOcrConfidence", 80)
        debugOcrConfidence = debugOcrConfidencePercent.toDouble() / 100.0
        debugOcrScale = preferences.getInt("debugOcrScale", 100).toDouble() / 100.0
        bRunTemplateMatchingTest = preferences.getBoolean("bRunTemplateMatchingTest", false)
        bRunSingleTrainingFailureOcrTest = preferences.getBoolean("bRunSingleTrainingFailureOcrTest", false)
        bRunComprehensiveTrainingFailureOcrTest = preferences.getBoolean("bRunComprehensiveTrainingFailureOcrTest", false)
        bHideComparisonResults = preferences.getBoolean("bHideComparisonResults", true)


        // When debug mode is enabled, we need to override some settings.
        if (bEnableDebugMode) {
            ocr.ocrConfidence = debugOcrConfidence
        }
    }
}

/** Utility class for storing and printing SharedPreferences.
*
* This stores the config in a global object so that we don't need to
* use a context when attempting to retrieve config values.
*/
object UserConfig {
    private lateinit var sharedPreferences: SharedPreferences
    lateinit var config: Config
    lateinit var ctx: Context
    private var bHasInitialized: Boolean = false

    fun initialize(context: Context) {
        ctx = context
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        config = Config(sharedPreferences)
        bHasInitialized = true
    }

    fun reloadPreferences() {
        if (!bHasInitialized) {
            // Quietly fail.
            return
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        config = Config(sharedPreferences)
    }

    fun getConfigString(): String {
        fun String.center(length: Int, padChar: Char = '='): String {
            if (this.length >= length) {
                return this
            }

            val tot = length - this.length
            val left = tot / 2
            val right = tot - left
            return padStart(this.length + left, padChar).padEnd(length, padChar)
        }

        // Construct strings to display
        val campaignString: String = if (config.campaign != "") {
            "🎯 ${config.campaign}"
        } else {
            "⚠️ Please select one in the Select Campaign option"
        }

        val strategyString: String = if (config.strategy != "") {
            "🎯 ${config.strategy}"
        } else {
            "⚠️ Please select one in the Select Race Strategy option"
        }

        val selectedCharacterString: String = if (config.events.bSelectAllCharacters) {
            "👥 All Characters Selected"
        } else if (config.events.selectedCharacter == "") {
            "⚠️ Please select one in the Training Event Settings"
        } else {
            "👤 ${config.events.selectedCharacter}"
        }

        val selectedSupportCardsString: String = if (config.events.bSelectAllSupportCards) {
            "🃏 All Support Cards Selected"
        } else if (config.events.selectedSupportCards.isEmpty() || config.events.selectedSupportCards[0] == "") {
            "⚠️ None Selected"
        } else {
            "�� ${config.events.selectedSupportCards.joinToString(", ")}"
        }

        val trainingBlacklistString: String = if (config.training.trainingBlacklist.isEmpty()) {
            "✅ No Trainings blacklisted"
        } else {
            val defaultTrainingOrder = listOf("Speed", "Stamina", "Power", "Guts", "Wit")
            val sortedBlacklist = config.training.trainingBlacklist.sortedBy { defaultTrainingOrder.indexOf(it) }
            "🚫 ${sortedBlacklist.joinToString(", ")}"
        }

        val statPriorityString: String = if (config.training.statPriority.isEmpty() || config.training.statPriority[0] == "") {
            "Speed|Stamina|Power|Guts|Wit"
        } else {
            config.training.statPriority.joinToString("|")
        }

        val focusSparksString = if (config.training.bFocusSparks) "✅" else "❌"

        val configString = buildString {
            appendLine("Campaign Selected:                  $campaignString")
            appendLine("Race Strategy Selected:             $strategyString")
            appendLine()
            appendLine(" Training Event Options ".center(80))
            appendLine("Character Selected:                 $selectedCharacterString")
            appendLine("Support(s) Selected:                $selectedSupportCardsString")
            appendLine()
            appendLine(" Training Options ".center(80))
            appendLine("Training Blacklist:                 ${trainingBlacklistString}")
            appendLine("Stat Priority:                      ${statPriorityString}")
            appendLine("Maximum Failure Chance Allowed:     ${config.training.maxFailChance}")
            appendLine("Enable Training on Maxed Stat:      ${if (config.training.bEnableTrainMaxedStat) "✅" else "❌"}")
            appendLine("Focus on Sparks for Stat Targets:   ${focusSparksString}")
            appendLine()
            appendLine(" Training Stat Targets by Distance ".center(80))
            listOf("Sprint", "Mile", "Medium", "Long").forEach { distance ->
                val stats = UserConfig.config.training.getTrainingStatTargets(distance)
                val statsString = stats.asIntArray().joinToString("/")
                appendLine(String.format("%-35s ${statsString}", "${distance}:"))
            }
            appendLine()
            appendLine(" Tesseract OCR Optimization ".center(80))
            appendLine("OCR Threshold:                      ${config.ocr.ocrThreshold}")
            appendLine("Enable OCR Automatic retry:         ${if (config.ocr.bEnableOcrAutomaticRetry) "✅" else "❌"}")
            appendLine("Minimum OCR Confidence:             ${config.ocr.ocrConfidencePercent}%")
            appendLine()
            appendLine(" Racing Options ".center(80))
            appendLine("Prioritize Farming Fans:            ${if (config.bEnableFarmingFans) "✅" else "❌"}")
            appendLine("Modulo Days to Farm Fans:           ${if (config.bEnableFarmingFans) "📅 ${config.daysToRunExtraRaces} days" else "❌"}")
            appendLine("Enable Race Retries:                ${if (config.bEnableRaceRetries) "✅" else "❌"}")
            appendLine("Stop on Mandatory Race:             ${if (config.bEnableStopOnMandatoryRace) "✅" else "❌"}")
            appendLine("Force Racing Every Day:             ${if (config.bEnableForceRacing) "✅" else "❌"}")
            appendLine()
            appendLine(" Misc Options ".center(80))
            appendLine("Skill Point Check:                  ${if (config.bEnableSkillPointCheck) "✅ Stop on ${config.skillPointCheckThreshold} Skill Points or more" else "❌"}")
            appendLine("Popup Check:                        ${if (config.bEnablePopupCheck) "✅" else "❌"}")
            appendLine("Prioritize Energy Options:          ${if (config.bEnablePrioritizeEnergy) "✅" else "❌"}")
            appendLine("Skip Crane Game:                    ${if (config.bEnableSkipCraneGame) "✅" else "❌"}")
            appendLine()
            appendLine(" Debug Options ".center(80))
            appendLine("Debug Mode:                         ${if (config.bEnableDebugMode) "✅" else "❌"}")
            appendLine("Min Template Match Confidence:      ${config.debugOcrConfidence}")
            appendLine("Template Scale:                     ${config.debugOcrScale}")
            appendLine("Template Matching Test:             ${if (config.bRunTemplateMatchingTest) "✅" else "❌"}")
            appendLine("Single Training Test:               ${if (config.bRunSingleTrainingFailureOcrTest) "✅" else "❌"}")
            appendLine("Comprehensive Training Test:        ${if (config.bRunComprehensiveTrainingFailureOcrTest) "✅" else "❌"}")
            appendLine("Hide String Comparison Results:     ${if (config.bHideComparisonResults) "✅" else "❌"}")
        }

        return configString
    }

    /** Prints all SharedPreferences settings for debugging purposes. */
    fun printConfigToLog() {
        val configString = getConfigString()

        MessageLog.i("SETTINGS", "Current Bot Configuration:")
        MessageLog.i(message="=".repeat(80))
        configString.split("\n").forEach { line ->
            if (line.isNotEmpty()) {
                MessageLog.i(message=line)
            }
        }
        MessageLog.i(message="=".repeat(80))
    }
}
