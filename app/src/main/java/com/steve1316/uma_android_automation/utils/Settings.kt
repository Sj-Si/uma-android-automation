package com.steve1316.uma_android_automation.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

val DEFAULT_STAT_TARGETS_SPRINT = intArrayOf(1200, 400, 900, 400, 600)
val DEFAULT_STAT_TARGETS_MILE = intArrayOf(1100, 500, 800, 500, 600)
val DEFAULT_STAT_TARGETS_MEDIUM = intArrayOf(1000, 700, 700, 500, 600)
val DEFAULT_STAT_TARGETS_LONG = intArrayOf(800, 1000, 600, 600, 600)

object Settings {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var config: PreferencesMain

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
    }

    data class PreferencesTraining(private val preferences: SharedPreferences) {
        var trainingBlacklist: List<String>
        var statPriority: List<String>
        var bEnableTrainMaxedStat: Boolean
        var maxFailChance: Int
        var bFocusOnSparkStatTarget: Boolean
        
        // Training stat targets by distance
        var statTargetSprint: TrainingStatTargets
        var statTargetMile: TrainingStatTargets
        var statTargetMedium: TrainingStatTargets
        var statTargetLong: TrainingStatTargets

        init {
            trainingBlacklist = preferences.getStringSet("trainingBlacklist", setOf())!!.toList()
            statPriority = preferences.getString("Speed|Stamina|Power|Guts|Wit", "")!!.split("|")
            bEnableTrainMaxedStat = preferences.getBoolean("bEnableTrainMaxedStat", false)
            maxFailChance = preferences.getInt("maxFailChance", 15)
            bFocusOnSparkStatTarget = preferences.getBoolean("bFocusOnSparkStatTarget", false)

            statTargetSprint = TrainingStatTargets(preferences, "trainingSprintStatTarget", DEFAULT_STAT_TARGETS_SPRINT)
            statTargetMile = TrainingStatTargets(preferences, "trainingMileStatTarget", DEFAULT_STAT_TARGETS_MILE)
            statTargetMedium = TrainingStatTargets(preferences, "trainingMediumStatTarget", DEFAULT_STAT_TARGETS_MEDIUM)
            statTargetLong = TrainingStatTargets(preferences, "trainingLongStatTarget", DEFAULT_STAT_TARGETS_LONG)
        }
    }

    data class PreferencesTrainingEvent(private val preferences: SharedPreferences) {
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

    data class PreferencesOCR(private val preferences: SharedPreferences) {
        var threshold: Double
        var bEnableAutomaticRetry: Boolean
        var confidence: Double

        init {
            threshold = preferences.getInt("threshold", 230).toDouble()
            bEnableAutomaticRetry = preferences.getBoolean("bEnableAutomaticRetry", true)
            confidence = preferences.getInt("confidence", 80).toDouble() / 100.0
        }
    }


    // Main Settings page
    data class PreferencesMain(private val preferences: SharedPreferences) {
        var campaign: String
        var strategy: String
        // Sub-categories
        var trainingOptions: PreferencesTraining
        var trainingEventOptions: PreferencesTrainingEvent
        var ocrOptions: PreferencesOCR
        // RACING
        var bEnableFarmingFans: Boolean
        var daysToRunExtraRaces: Int
        var bDisableRaceRetries: Boolean
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
        var ocrConfidence: Int
        var customScale: Int
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
            trainingOptions = PreferencesTraining(preferences)
            trainingEventOptions = PreferencesTrainingEvent(preferences)
            ocrOptions = PreferencesOCR(preferences)

            // Racing Options
            bEnableFarmingFans = preferences.getBoolean("bEnableFarmingFans", false)
            daysToRunExtraRaces = preferences.getInt("daysToRunExtraRaces", 4)
            bDisableRaceRetries = preferences.getBoolean("bDisableRaceRetries", false)
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
            ocrConfidence = preferences.getInt("ocrConfidence", 0)
            customScale = preferences.getInt("customScale", 0)
            bRunTemplateMatchingTest = preferences.getBoolean("bRunTemplateMatchingTest", false)
            bRunSingleTrainingFailureOcrTest = preferences.getBoolean("bRunSingleTrainingFailureOcrTest", false)
            bRunComprehensiveTrainingFailureOcrTest = preferences.getBoolean("bRunComprehensiveTrainingFailureOcrTest", false)
            bHideComparisonResults = preferences.getBoolean("bHideComparisonResults", false)
        }
    }

    fun initialize(ctx: Context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        config = PreferencesMain(sharedPreferences)
    }
}