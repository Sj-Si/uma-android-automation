package com.steve1316.uma_android_automation.bot

import android.graphics.Bitmap
import android.util.Log
import org.opencv.core.Point
import kotlin.enums.enumEntries
import kotlin.math.abs
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.BotService

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.automation_library.utils.SettingsHelper
import com.steve1316.uma_android_automation.utils.CustomImageUtils
import com.steve1316.uma_android_automation.types.StatName
import com.steve1316.uma_android_automation.types.Aptitude
import com.steve1316.uma_android_automation.types.RunningStyle
import com.steve1316.uma_android_automation.types.TrackSurface
import com.steve1316.uma_android_automation.types.TrackDistance
import com.steve1316.uma_android_automation.types.Mood
import com.steve1316.uma_android_automation.types.FanCountClass
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.IconMoodGreat
import com.steve1316.uma_android_automation.components.IconMoodGood
import com.steve1316.uma_android_automation.components.IconMoodNormal
import com.steve1316.uma_android_automation.components.IconMoodBad
import com.steve1316.uma_android_automation.components.IconMoodAwful
import com.steve1316.uma_android_automation.components.LabelStatDistance
import com.steve1316.uma_android_automation.components.LabelStatTrackSurface
import com.steve1316.uma_android_automation.components.LabelStatStyle

/** Defines a trainee (uma).
 *
 * This class tracks a trainee's state across the runtime of the bot.
 * This includes things such as their current stats, aptitudes, etc.
 *
 * @property stats The trainee's current stats.
 * @property trackSurfaceAptitudes Mapping of TrackSurface to the trainee's aptitudes.
 * @property trackDistanceAptitudes Mapping of TrackDistance to the trainee's aptitudes.
 * @property runningStyleAptitudes Mapping of RunningStyle to the trainee's aptitudes.
 * @property skillPoints The trainee's current number of skill points.
 * @property fans The trainee's current fan count.
 * @property mood The trainee's current mood.
 * @property bHasUpdatedAptitudes Whether the trainee's aptitudes have been checked and updated.
 * @property bHasUpdatedStats Whether the trainee's stats have been checked and updated.
 * @property bHasUpdatedSkillPoints Whether the trainee's skill points have been checked and updated.
 * @property bTemporaryRunningStyleAptitudesUpdated This flag is set when the trainee has
 * not yet set its aptitudes but has read the running style aptitudes on the race prep screen.
 * @property bHasSetRunningStyle Whether the trainee has set its preferred running style
 * in the race prep screen.
 * @property fanCountClass The trainee's current FanCountClass based on its fan count.
 * @property bIsInitialized Whether we have updated the aptitudes, stats, and skill points.
 * @property bHasCompletedMaidenRace Whether we have completed the maiden race.
 * @property trackSurface The trainee's preferred `TrackSurface`.
 * @property trackDistance The trainee's preferred `TrackDistance`.
 * @property runningStyle The trainee's preferred `RunningStyle`.
 */
class Trainee {
    companion object {
        const val TAG: String = "[${MainActivity.loggerTag}]Trainee"

        /** Stores the trainee's current stat values. */
        data class Stats(
            var speed: Int = -1,
            var stamina: Int = -1,
            var power: Int = -1,
            var guts: Int = -1,
            var wit: Int = -1,
        ) {
            /** Sets a stat value by its name.
             *
             * @param statName The stat to set.
             * @param value The value to set the stat to.
             */
            fun setStat(statName: StatName, value: Int) {
                when (statName) {
                    StatName.SPEED -> speed = value
                    StatName.STAMINA -> stamina = value
                    StatName.POWER -> power = value
                    StatName.GUTS -> guts = value
                    StatName.WIT -> wit = value
                }
            }

            override fun toString(): String {
                return "Spd=$speed, Sta=$stamina, Pow=$power, Gut=$guts, Wit=$wit"
            }

            /** Converts the stats to an integer array.
             *
             * @return An array of stat values in order: Speed, Stamina, Power, Guts, Wit.
             */
            fun toIntArray(): IntArray {
                return intArrayOf(speed, stamina, power, guts, wit)
            }

            /** Converts the stats to a map keyed by `StatName`.
             *
             * @return A mapping of stat names to their values.
             */
            fun asMap(): Map<StatName, Int> {
                return mapOf(
                    StatName.SPEED to speed,
                    StatName.STAMINA to stamina,
                    StatName.POWER to power,
                    StatName.GUTS to guts,
                    StatName.WIT to wit,
                )
            }
        }
    }

    private val preferredDistanceOverride: String = SettingsHelper.getStringSetting("training", "preferredDistanceOverride")
    private val statTargetsByDistance = mutableMapOf<TrackDistance, Stats>()

    val stats: Stats = Stats()
    val trackSurfaceAptitudes: MutableMap<TrackSurface, Aptitude> = mutableMapOf(
        TrackSurface.TURF to Aptitude.G,
        TrackSurface.DIRT to Aptitude.G,
    )
    val trackDistanceAptitudes: MutableMap<TrackDistance, Aptitude> = mutableMapOf(
        TrackDistance.SPRINT to Aptitude.G,
        TrackDistance.MILE to Aptitude.G,
        TrackDistance.MEDIUM to Aptitude.G,
        TrackDistance.LONG to Aptitude.G,
    )
    val runningStyleAptitudes: MutableMap<RunningStyle, Aptitude> = mutableMapOf(
        RunningStyle.FRONT_RUNNER to Aptitude.G,
        RunningStyle.PACE_CHASER to Aptitude.G,
        RunningStyle.LATE_SURGER to Aptitude.G,
        RunningStyle.END_CLOSER to Aptitude.G,
    )
    var skillPoints: Int = 120 // From what I can tell, all trainees start at 120.
    var fans: Int = 1
    var mood: Mood = Mood.NORMAL

    var bHasUpdatedAptitudes: Boolean = false
    var bHasUpdatedStats: Boolean = false
    var bHasUpdatedSkillPoints: Boolean = false
    var bTemporaryRunningStyleAptitudesUpdated: Boolean = false
    var bHasSetRunningStyle: Boolean = false

    var fanCountClass: FanCountClass = FanCountClass.DEBUT

    val bIsInitialized: Boolean
        get() = bHasUpdatedAptitudes && bHasUpdatedStats && bHasUpdatedSkillPoints

    val bHasCompletedMaidenRace: Boolean
        get() = fanCountClass.ordinal > FanCountClass.MAIDEN.ordinal

    /** Calculates the highest weighted key based on aptitude for the passed Enum
     *
     * See trackDistance getter for example.
     *
     * @param aptitudeMap A mapping of the passed enum's names to their current aptitudes.
     * @param defaultMaxKey The default value in case  no aptitudes could be detected.
     *
     * @return The passed enum's name for the associated highest aptitude.
     */
    inline fun <reified T : Enum<T>> getMaxAptitude(
        aptitudeMap: MutableMap<T, Aptitude>,
        defaultMaxKey: T,
    ): T {
        var maxKey = defaultMaxKey
        var maxVal: Aptitude = Aptitude.G

        for ((key, aptitude) in aptitudeMap) {
            if (aptitude > maxVal) {
                // If aptitude is higher, pick it.
                maxKey = key
                maxVal = aptitude
            } else if (aptitude == maxVal && key < maxKey) {
                // If aptitudes are the same, we want to pick the higher
                // priority key.
                maxKey = key
                maxVal = aptitude
            }
        }

        return maxKey
    }

    val trackSurface: TrackSurface
        get() = getMaxAptitude<TrackSurface>(
            aptitudeMap=trackSurfaceAptitudes,
            defaultMaxKey=TrackSurface.TURF,
        )

    val trackDistance: TrackDistance
        get() = TrackDistance.fromName(preferredDistanceOverride) ?: getMaxAptitude<TrackDistance>(
            aptitudeMap=trackDistanceAptitudes,
            defaultMaxKey=TrackDistance.MEDIUM,
        )
    
    val runningStyle: RunningStyle
        get() = getMaxAptitude<RunningStyle>(
            aptitudeMap=runningStyleAptitudes,
            defaultMaxKey=RunningStyle.FRONT_RUNNER,
        )

    init {
        setStatTargetsByDistances()
    }

    /** Retrieves a single stat value by its name.
     *
     * @param statName The stat to retrieve.
     * @return The current value of the specified stat.
     */
    fun getStat(statName: StatName): Int {
        return when (statName) {
            StatName.SPEED -> stats.speed
            StatName.STAMINA -> stats.stamina
            StatName.POWER -> stats.power
            StatName.GUTS -> stats.guts
            StatName.WIT -> stats.wit
        }
    }

    /**
	 * Retrieves the stat targets for a given track distance.
	 *
	 * If no distance is provided, the trainee's calculated preferred track distance is used.
	 * If the distance is not found in the mapping, a default set of stat targets is returned.
	 *
	 * @param distance The track distance to get stat targets for. Defaults to the trainee's
	 * preferred track distance if NULL.
	 * @return A mapping of stat names to their target values.
	 */
    fun getStatTargetsByDistance(distance: TrackDistance? = null): Map<StatName, Int> {
        // If distance is NULL, we want to use the calculated preferred track distance.
        val distance: TrackDistance = distance ?: trackDistance

        // Return a default set of stat targets if the distance does not exist in the mapping.
        if (distance !in statTargetsByDistance) {
            return mapOf(
                StatName.SPEED to 600,
                StatName.STAMINA to 600,
                StatName.POWER to 600,
                StatName.GUTS to 300,
                StatName.WIT to 300,
            )
        }

        return statTargetsByDistance[distance]!!.asMap()
    }

    /** Sets the trainee's stats with optional values.
     *
     * Only non-null values will be updated.
     *
     * @param speed The new speed value, or NULL to leave unchanged.
     * @param stamina The new stamina value, or NULL to leave unchanged.
     * @param power The new power value, or NULL to leave unchanged.
     * @param guts The new guts value, or NULL to leave unchanged.
     * @param wit The new wit value, or NULL to leave unchanged.
     */
    fun setTraineeStats(
        speed: Int? = null,
        stamina: Int? = null,
        power: Int? = null,
        guts: Int? = null,
        wit: Int? = null,
    ) {
        if (speed != null) {
            stats.speed = speed
        }
        if (stamina != null) {
            stats.stamina = stamina
        }
        if (power != null) {
            stats.power = power
        }
        if (guts != null) {
            stats.guts = guts
        }
        if (wit != null) {
            stats.wit = wit
        }
    }

    /** Sets the trainee's aptitude for a specific running style.
     *
     * @param runningStyle The running style to set the aptitude for.
     * @param aptitude The aptitude value to assign.
     */
    fun setRunningStyleAptitude(runningStyle: RunningStyle, aptitude: Aptitude) {
        runningStyleAptitudes[runningStyle] = aptitude
    }

    /** Returns the trainee's aptitude for a specified `TrackSurface`.
     *
     * @param trackSurface The track surface to check the aptitude for.
     * @return The aptitude value for the specified track surface.
     */
    fun checkTrackSurfaceAptitude(trackSurface: TrackSurface): Aptitude {
        return trackSurfaceAptitudes[trackSurface] ?: Aptitude.G
    }

    /** Returns the trainee's aptitude for a specified `TrackDistance`.
     *
     * @param trackDistance The track distance to check the aptitude for.
     * @return The aptitude value for the specified track distance.
     */
    fun checkTrackDistanceAptitude(trackDistance: TrackDistance): Aptitude {
        return trackDistanceAptitudes[trackDistance] ?: Aptitude.G
    }

    /** Returns the trainee's aptitude for a specified `RunningStyle`.
     *
     * @param runningStyle The running style to check the aptitude for.
     * @return The aptitude value for the specified running style.
     */
    fun checkRunningStyleAptitude(runningStyle: RunningStyle): Aptitude {
        return runningStyleAptitudes[runningStyle] ?: Aptitude.G
    }

    /** Detects the trainee's aptitudes for the specified enum.
     *
     * This scans a single row of aptitudes in the Umamusume Details dialog
     * where the row is determined by the `label` parameter.
     *
     * See `updateTrackSurfaceAptitudes()` for an example.
     *
     * @param label This is the label at the far left of the row in the dialog.
     * This determines which row in the dialog that we want to read.
     * @return a mapping of the passed enum's names to the aptitude values.
     */
    inline fun <reified T : Enum<T>> findAptitudesInBitmap(
        imageUtils: CustomImageUtils,
        label: ComponentInterface,
    ): Map<T, Aptitude>? {
        val result = mutableMapOf<T, Aptitude>()

        val bitmap: Bitmap = imageUtils.getSourceBitmap()
        val point: Point? = label.find(imageUtils = imageUtils).first
        if (point == null) {
            MessageLog.e(TAG, "findAptitudesInBitmap<${T::class.simpleName}>:: point is NULL.")
            return null
        }

        enumEntries<T>().forEachIndexed { index, option ->
            val croppedBitmap: Bitmap? = imageUtils.createSafeBitmap(
                bitmap,
                imageUtils.relX(point.x, 108 + (index * 190)),
                imageUtils.relY(point.y, -25),
                imageUtils.relWidth(176),
                imageUtils.relHeight(52),
                "Trainee::findAptitudesInBitmap<${T::class.simpleName}>:: crop bitmap.",
            )
            if (croppedBitmap == null) {
                MessageLog.e(TAG, "findAptitudesInBitmap<${T::class.simpleName}>:: Failed to create cropped bitmap: ${option}.")
                return@forEachIndexed
            }
            for (aptitude in Aptitude.entries) {
                if (imageUtils.findImageWithBitmap("stat_aptitude_${aptitude.name}", croppedBitmap, suppressError = true) != null) {
                    result[option] = aptitude
                    break
                }
            }
        }

        return result.toMap()
    }

    /** Updates the trainee's track surface aptitudes from the current screen.
     *
     * @param imageUtils A reference to a CustomImageUtils instance.
     */
    private fun updateTrackSurfaceAptitudes(imageUtils: CustomImageUtils) {
        val aptitudes = findAptitudesInBitmap<TrackSurface>(
            imageUtils = imageUtils,
            label = LabelStatTrackSurface,
        )

        if (aptitudes == null) {
            return
        }

        for ((key, value) in aptitudes) {
            trackSurfaceAptitudes[key] = value
        }
    }

    /** Updates the trainee's track distance aptitudes from the current screen.
     *
     * @param imageUtils A reference to a CustomImageUtils instance.
     */
    private fun updateTrackDistanceAptitudes(imageUtils: CustomImageUtils) {
        val aptitudes = findAptitudesInBitmap<TrackDistance>(
            imageUtils = imageUtils,
            label = LabelStatDistance,
        )

        if (aptitudes == null) {
            return
        }

        for ((key, value) in aptitudes) {
            trackDistanceAptitudes[key] = value
        }
    }

    /** Updates the trainee's running style aptitudes from the current screen.
     *
     * @param imageUtils A reference to a CustomImageUtils instance.
     */
    private fun updateRunningStyleAptitudes(imageUtils: CustomImageUtils) {
        val aptitudes = findAptitudesInBitmap<RunningStyle>(
            imageUtils = imageUtils,
            label = LabelStatStyle,
        )

        if (aptitudes == null) {
            return
        }

        for ((key, value) in aptitudes) {
            runningStyleAptitudes[key] = value
        }
    }

    /** Updates all aptitudes for trainee.
     *
     * Requires the Umamusume Details dialog to be opened.
     *
     * @param imageUtils A reference to a CustomImageUtils instance.
     */
    fun updateAptitudes(imageUtils: CustomImageUtils) {
        updateTrackSurfaceAptitudes(imageUtils = imageUtils)
        updateTrackDistanceAptitudes(imageUtils = imageUtils)
        updateRunningStyleAptitudes(imageUtils = imageUtils)

        bHasUpdatedAptitudes = true
    }

    /** Updates the trainee's skill points from the current screen.
     *
     * @param imageUtils A reference to a CustomImageUtils instance.
     * @param sourceBitmap Optional pre-captured bitmap to analyze.
     * @param skillPointsLocation Optional pre-determined location of skill points on screen.
     */
    fun updateSkillPoints(imageUtils: CustomImageUtils, sourceBitmap: Bitmap? = null, skillPointsLocation: Point? = null) {
        val res = imageUtils.determineSkillPoints(sourceBitmap, skillPointsLocation)
        if (res != -1) {
            skillPoints = res
        }

        bHasUpdatedSkillPoints = skillPoints != -1
    }

    /** Updates the trainee's stats from the current screen.
     *
     * When sourceBitmap and skillPointsLocation are provided, uses parallel threading
     * for faster processing. Otherwise, falls back to sequential processing.
     *
     * @param imageUtils A reference to a CustomImageUtils instance.
     * @param sourceBitmap Optional pre-captured bitmap to analyze.
     * @param skillPointsLocation Optional pre-determined location of skill points on screen.
     * @param externalLatch Optional external latch for synchronization with other threads.
     */
    fun updateStats(imageUtils: CustomImageUtils, sourceBitmap: Bitmap? = null, skillPointsLocation: Point? = null, externalLatch: CountDownLatch? = null) {
        // If sourceBitmap and skillPointsLocation are provided, use threading for parallel processing.
        if (sourceBitmap != null && skillPointsLocation != null) {
            val statLatch = externalLatch ?: CountDownLatch(5)
            val waitLatch = CountDownLatch(5) // Internal latch for waiting, regardless of external latch.
            val threadSafeResults = ConcurrentHashMap<StatName, Int>()

            // Create 5 threads, one for each stat.
            for (statName in StatName.entries) {
                Thread {
                    try {
                        if (!BotService.isRunning) {
                            return@Thread
                        }
                        val statValue = imageUtils.determineSingleStatValue(statName, sourceBitmap, skillPointsLocation)
                        threadSafeResults[statName] = statValue
                    } catch (e: Exception) {
                        Log.e(TAG, "[ERROR] Error processing stat $statName: ${e.stackTraceToString()}")
                        threadSafeResults[statName] = -1
                    } finally {
                        statLatch.countDown()
                        waitLatch.countDown()
                    }
                }.apply { isDaemon = true }.start()
            }

            // Wait for all threads to complete using the internal wait latch.
            try {
                waitLatch.await(10, java.util.concurrent.TimeUnit.SECONDS)
            } catch (_: InterruptedException) {
                MessageLog.e(TAG, "Stat processing timed out.")
            }

            // Update stats with thread-safe results.
            val statMapping = threadSafeResults.toMap()
            for ((statName, newValue) in statMapping) {
                val oldValue = getStat(statName)
                val diff = abs(newValue - oldValue)
                // If our previous stat value is <= 0, that means we haven't set it yet.
                if (oldValue <= 0 || diff < 150) {
                    stats.setStat(statName, newValue)
                    bHasUpdatedStats = true
                } else {
                    MessageLog.w(TAG, "New $statName stat value has changed too much since last update: old=$oldValue, new=$newValue")
                }
            }
        } else {
            // Use the original sequential method.
            val statMapping: Map<StatName, Int> = imageUtils.determineStatValues()

            // It is possible that we misread a stat value. We want to make sure we
            // don't update our stats if they change too wildly from the previous values.
            for ((statName, newValue) in statMapping) {
                val oldValue = getStat(statName)
                val diff = abs(newValue - oldValue)
                // If our previous stat value is <= 0, that means we haven't set it yet.
                if (oldValue <= 0 || diff < 150) {
                    stats.setStat(statName, newValue)
                    bHasUpdatedStats = true
                } else {
                    MessageLog.w(TAG, "New $statName stat value has changed too much since last update: old=$oldValue, new=$newValue")
                }
            }
        }
    }

    /** Detects the trainee's current mood from the screen.
     *
     * @param imageUtils A reference to a CustomImageUtils instance.
     * @param sourceBitmap Optional pre-captured bitmap to analyze.
     * @return The detected mood, or NULL if no mood could be determined.
     */
    fun checkMood(imageUtils: CustomImageUtils, sourceBitmap: Bitmap? = null): Mood? {
        return if (sourceBitmap != null) {
            // Use findImageWithBitmap for thread-safe operations.
            when {
                imageUtils.findImageWithBitmap(IconMoodAwful.template.path, sourceBitmap, region = IconMoodAwful.template.region, suppressError = true) != null -> Mood.AWFUL
                imageUtils.findImageWithBitmap(IconMoodBad.template.path, sourceBitmap, region = IconMoodBad.template.region, suppressError = true) != null -> Mood.BAD
                imageUtils.findImageWithBitmap(IconMoodNormal.template.path, sourceBitmap, region = IconMoodNormal.template.region, suppressError = true) != null -> Mood.NORMAL
                imageUtils.findImageWithBitmap(IconMoodGood.template.path, sourceBitmap, region = IconMoodGood.template.region, suppressError = true) != null -> Mood.GOOD
                imageUtils.findImageWithBitmap(IconMoodGreat.template.path, sourceBitmap, region = IconMoodGreat.template.region, suppressError = true) != null -> Mood.GREAT
                else -> null
            }
        } else {
            // Use the original ComponentInterface.check() method.
            when {
                IconMoodAwful.check(imageUtils = imageUtils) -> Mood.AWFUL
                IconMoodBad.check(imageUtils = imageUtils) -> Mood.BAD
                IconMoodNormal.check(imageUtils = imageUtils) -> Mood.NORMAL
                IconMoodGood.check(imageUtils = imageUtils) -> Mood.GOOD
                IconMoodGreat.check(imageUtils = imageUtils) -> Mood.GREAT
                else -> null
            }
        }
    }

    /** Updates the trainee's mood state from the current screen.
     *
     * If no mood can be detected, the current mood state remains unchanged.
     *
     * @param imageUtils A reference to a CustomImageUtils instance.
     * @param sourceBitmap Optional pre-captured bitmap to analyze.
     */
    fun updateMood(imageUtils: CustomImageUtils, sourceBitmap: Bitmap? = null) {
        // If checkMood returns NULL, then make no change to the mood state.
        mood = checkMood(imageUtils, sourceBitmap) ?: mood
    }

    /**
	 * Sets up stat targets for different race distances by reading values from SQLite settings.
     * These targets are used to determine training priorities based on the expected race distance.
	 */
	fun setStatTargetsByDistances() {
		for (trackDistance in TrackDistance.entries) {
            val newStats = Stats()
            for (statName in StatName.entries) {
                val statNameString = statName.name.lowercase()
                val trackDistanceString = trackDistance.name.lowercase().replaceFirstChar { it.uppercase() }
                val target: Int = SettingsHelper.getIntSetting(
                    "trainingStatTarget",
                    "training${trackDistanceString}StatTarget_${statNameString}StatTarget",
                )
                newStats.setStat(statName, target)
            }
            statTargetsByDistance[trackDistance] = newStats
        }
	}

    /** Logs the trainee's current state to the message log. */
    fun logInfo() {
        MessageLog.i(TAG, "[TRAINEE] Current State:\n${this}")
    }

    /** Returns a formatted string of the trainee's preferred aptitudes.
     *
     * @return A multi-line string containing track surface, distance, and running style.
     */
    fun getAptitudesString(): String {
        return "TrackSurface: $trackSurface\nTrackDistance: $trackDistance\nRunningStyle: $runningStyle"
    }

    /** Returns a formatted string of the trainee's current stats.
     *
     * @return A string representation of all stats.
     */
    fun getStatsString(): String {
        return stats.toString()
    }

    override fun toString(): String {
        val aptitudesString = getAptitudesString()
        val statsString = getStatsString()
        return "Aptitudes: $aptitudesString" +
            "\nStats: $statsString" +
            "\nSkill Points: $skillPoints" +
            "\nMood: $mood" +
            "\nFans: $fans" +
            "\nFanCountClass: $fanCountClass"
    }
}
