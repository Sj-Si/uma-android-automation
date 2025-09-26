package com.steve1316.uma_android_automation.utils

val DEFAULT_STAT_TARGETS_SPRINT = intArrayOf(1200, 400, 900, 400, 600)
val DEFAULT_STAT_TARGETS_MILE = intArrayOf(1100, 500, 800, 500, 600)
val DEFAULT_STAT_TARGETS_MEDIUM = intArrayOf(1000, 700, 700, 500, 600)
val DEFAULT_STAT_TARGETS_LONG = intArrayOf(800, 1000, 600, 600, 600)

object Settings {
    private lateinit var sharedPreferences: SharedPreferences

    data class TrainingStatTargets(
        private val preferences: SharedPreferences,
        private val prefix: String,
        private val defaults: IntArray,
    ) {
        lateinit val speed: Int
        lateinit val stamina: Int
        lateinit val power: Int
        lateinit val guts: Int
        lateinit val wit: Int

        init {
            speed = preferences.getInt("${prefix}_speedStatTarget", defaults[0])
            stamina = preferences.getInt("${prefix}_staminaStatTarget", defaults[1])
            power = preferences.getInt("${prefix}_powerStatTarget", defaults[2])
            guts = preferences.getInt("${prefix}_gutsStatTarget", defaults[3])
            wit = preferences.getInt("${prefix}_witStatTarget", defaults[4])
        }
    }

    data class PreferencesTraining(private val preferences: SharedPreferences) {
        lateinit var trainingBlacklist: List<String>
        lateinit var statPriority: List<String>
        lateinit var bEnableTrainMaxedStat: Boolean
        lateinit var maxFailChance: Int
        lateinit var bFocusOnSparkStatTarget: Boolean
        
        // Training stat targets by distance
        lateinit var statTargetSprint: TrainingStatTargets
        lateinit var statTargetMile: TrainingStatTargets
        lateinit var statTargetMedium: TrainingStatTargets
        lateinit var statTargetLong: TrainingStatTargets

        init {
            trainingBlacklist = preferences.getStringSet("trainingBlacklist", setOf())!!.toList()
            statPriority = preferences.getString("Speed|Stamina|Power|Guts|Wit")!!.split("|")
            bEnableTrainMaxedStat = preferences.getBoolean("bEnableTrainMaxedStat", false)
            maxFailChance = preferences.getInt("maxFailChance", 15)
            bFocusOnSparkStatTarget = preferences.getBoolean("bFocusOnSparkStatTarget", false)

            statTargetSprint = TrainingStatTargets(preferences, DEFAULT_STAT_TARGETS_SPRINT)
            statTargetMile = TrainingStatTargets(preferences, DEFAULT_STAT_TARGETS_MILE)
            statTargetMedium = TrainingStatTargets(preferences, DEFAULT_STAT_TARGETS_MEDIUM)
            statTargetLong = TrainingStatTargets(preferences, DEFAULT_STAT_TARGETS_LONG)
        }
    }

    data class PreferencesTrainingEvent(private val preferences: SharedPreferences) {
        lateinit var bSelectAllCharacters: Boolean
        lateinit var selectedCharacter: String
        lateinit var bSelectAllSupportCards: Boolean
        lateinit var selectedSupportCards: List<String>

        init {
            bSelectAllCharacters = preferences.getBoolean("bSelectAllCharacters", true)
            selectedCharacter = preferences.getString("selectedCharacter", "")!!
            bSelectAllSupportCards = preferences.getBoolean("bSelectAllSupportCards", true)
            selectedSupportCards = preferences.getString("selectedSupportCards", "")!!.split("|")
        }
    }

    data class PreferencesOCR(private val preferences: SharedPreferences) {
        lateinit var threshold: Double
        lateinit var bEnableAutomaticRetry: Boolean
        lateinit var confidence: Double

        init {
            threshold = preferences.getInt("threshold", 230).toDouble()
            bEnableAutomaticRetry = preferences.getBoolean("bEnableAutomaticRetry", true)
            confidence = preferences.getInt("confidence", 80).toDouble() / 100.0
        }
    }


    // Main Settings page
    data class PreferencesMain(private val preferences: SharedPreferences) {
        lateinit var campaign: String,
        lateinit var strategy: String,
        // Sub-categories
        lateinit var trainingOptions: PreferencesTraining,
        lateinit var trainingEventOptions: PreferencesTrainingEvent,
        lateinit var ocrOptions: PreferencesOCR,
        // RACING
        lateinit var bEnableFarmingFans: Boolean,
        lateinit var daysToRunExtraRaces: Int,
        lateinit var bDisableRaceRetries: Boolean,
        lateinit var bEnableStopOnMandatoryRace: Boolean,
        lateinit var bEnableForceRacing: Boolean,
        // MISC
        lateinit var bEnableSkillPointCheck: Boolean,
        lateinit var skillPointCheckThreshold: Int,
        lateinit var bEnablePopupCheck: Boolean,
        lateinit var bEnablePrioritizeEnergy: Boolean,
        lateinit var bEnableSkipCraneGame: Boolean,
        // DEBUG
        lateinit var bEnableDebugMode: Boolean,
        lateinit var ocrConfidence: Int,
        lateinit var customScale: Int,
        lateinit var bRunTemplateMatchingTest: Boolean,
        lateinit var bRunSingleTrainingFailureOcrTest: Boolean,
        lateinit var bRunComprehensiveTrainingFailureOcrTest: Boolean,
        lateinit var bHideComparisonResults: Boolean,

        var strategyImageName: String = strategy
            set(value) {
                field = when (strategy) {
                    "Front Runner" -> "strategy_front"
                    "Pace Chaser" -> "strategy_pace"
                    "Late Surger" -> "strategy_late"
                    "End Closer" -> "strategy_end"
                    else -> "default"
                }
            }

        init {
            // Options
            campaign = preferences.getString("campaign", "")!!
            strategy = preferences.getString("strategy", "")!!
            
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
        pref = PreferenceManager.getDefaultSharedPreferences(ctx)
        config = PreferencesMain(pref)
    }
}