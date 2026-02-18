package com.steve1316.uma_android_automation.bot

import android.graphics.Bitmap
import org.opencv.core.Point
import org.json.JSONArray
import org.json.JSONObject

import com.steve1316.automation_library.utils.SettingsHelper
import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.SkillList

import com.steve1316.uma_android_automation.types.RunningStyle
import com.steve1316.uma_android_automation.types.TrackDistance
import com.steve1316.uma_android_automation.types.TrackSurface
import com.steve1316.uma_android_automation.types.Aptitude
import com.steve1316.uma_android_automation.types.SkillCommunityTier

private const val USE_MOCK_DATA: Boolean = false
private const val MOCK_SKILL_POINTS: Int = 1495

/**
 * Handles operations based on the user's Skill Plan Settings.
 *
 * @param game A reference to the Game instance.
 */
class SkillPlan (private val game: Game) {
    private val TAG: String = "[${MainActivity.loggerTag}]SkillPlan"

    // Get user settings for skill plans.
    val skillSettingRunningStyleString = SettingsHelper.getStringSetting("skills", "preferredRunningStyle")
    val skillSettingTrackDistanceString = SettingsHelper.getStringSetting("skills", "preferredTrackDistance")
    val skillSettingTrackSurfaceString = SettingsHelper.getStringSetting("skills", "preferredTrackSurface")

    private val trainingSettingTrackDistanceString = SettingsHelper.getStringSetting("training", "preferredDistanceOverride")
    private val racingSettingRunningStyleString = SettingsHelper.getStringSetting("racing", "originalRaceStrategy")

    // Load the skill plans from settings.
    val skillPlans: Map<String, SkillPlanSettings> = try {
        val plansString = SettingsHelper.getStringSetting("skills", "plans")
        if (plansString.isNotEmpty()) {
            val jsonObject = JSONObject(plansString)
            val plansMap = mutableMapOf<String, SkillPlanSettings>()
            jsonObject.keys().forEach { planName ->
                val planData = jsonObject.getJSONObject(planName)
                val strategyString: String = planData.getString("strategy")
                val skillIds: List<Int> = planData
                    .getString("plan")
                    .split(",")
                    .map { it.trim() }
                    .mapNotNull { it.toIntOrNull() }
                val skillNames: List<String> = skillIds.mapNotNull { game.skillDatabase.getSkillName(it) }
                plansMap[planName] = SkillPlanSettings(
                    bIsEnabled = planData.getBoolean("enabled"),
                    strategy = SpendingStrategy.fromName(strategyString) ?: SpendingStrategy.DEFAULT,
                    bEnableBuyInheritedUniqueSkills = planData.getBoolean("enableBuyInheritedUniqueSkills"),
                    bEnableBuyNegativeSkills = planData.getBoolean("enableBuyNegativeSkills"),
                    skillNames = skillNames,
                )
            }
            plansMap
        } else {
            emptyMap()
        }
    } catch (e: Exception) {
        MessageLog.w(TAG, "Could not parse skill plan settings: ${e.message}")
        emptyMap()
    }

    enum class SpendingStrategy {
        DEFAULT,
        OPTIMIZE_SKILLS,
        OPTIMIZE_RANK;

        companion object {
            private val nameMap = entries.associateBy { it.name }
            private val ordinalMap = entries.associateBy { it.ordinal }

            fun fromName(value: String): SpendingStrategy? = nameMap[value.uppercase()]
            fun fromOrdinal(ordinal: Int): SpendingStrategy? = ordinalMap[ordinal]
        }
    }

    /** Data class used to store all skill plan settings for easier access. */
    data class SkillPlanSettings(
        val bIsEnabled: Boolean,
        val strategy: SpendingStrategy,
        val bEnableBuyInheritedUniqueSkills: Boolean,
        val bEnableBuyNegativeSkills: Boolean,
        val skillNames: List<String>,
    )

    /** Gets all available negative skills in the skill list.
     *
     * @param skillPlanSettings The SkillPlanSettings to use when purchasing.
     * @param skillList A reference to the SkillList instance.
     * @param skillsToBuy The current list of skill names that we plan to buy.
     * @param availableSkillPoints The amount of remaining skill points.
     *
     * @return A mapping of the new skill names and their prices that we want to buy.
     * This only includes the skills that we calculated in this function.
     * It does not include anything from [skillsToBuy].
     */
    private fun getNegativeSkills(
        skillPlanSettings: SkillPlanSettings,
        skillList: SkillList,
        skillsToBuy: List<String>,
        availableSkillPoints: Int,
    ): Map<String, Int> {
        if (!skillPlanSettings.bEnableBuyNegativeSkills) {
            return emptyMap()
        }

        val result: MutableMap<String, Int> = mutableMapOf()
        var remainingSkillPoints: Int = availableSkillPoints

        val entries: Map<String, SkillListEntry> = skillList.getNegativeSkills()
        for ((name, entry) in entries) {
            // Don't add any duplicate entries.
            if (name in skillsToBuy) {
                continue
            }

            if (entry.screenPrice <= remainingSkillPoints) {
                result[name] = entry.screenPrice
                remainingSkillPoints -= entry.screenPrice
                entry.buy()
            }
        }

        return result.toMap()
    }

    /** Gets all available inherited unique skills in the skill list.
     *
     * @param skillPlanSettings The SkillPlanSettings to use when purchasing.
     * @param skillList A reference to the SkillList instance.
     * @param skillsToBuy The current list of skill names that we plan to buy.
     * @param availableSkillPoints The amount of remaining skill points.
     *
     * @return A mapping of the new skill names and their prices that we want to buy.
     * This only includes the skills that we calculated in this function.
     * It does not include anything from [skillsToBuy].
     */
    private fun getInheritedUniqueSkills(
        skillPlanSettings: SkillPlanSettings,
        skillList: SkillList,
        skillsToBuy: List<String>,
        availableSkillPoints: Int,
    ): Map<String, Int> {
        if (!skillPlanSettings.bEnableBuyInheritedUniqueSkills) {
            return emptyMap()
        }

        val result: MutableMap<String, Int> = mutableMapOf()
        var remainingSkillPoints: Int = availableSkillPoints

        val entries: Map<String, SkillListEntry> = skillList.getInheritedUniqueSkills()
        for ((name, entry) in entries) {
            if (name in skillsToBuy || name in result) {
                continue
            }

            if (entry.screenPrice <= remainingSkillPoints) {
                result[name] = entry.screenPrice
                remainingSkillPoints -= entry.screenPrice
                entry.buy()
            }
        }

        return result.toMap()
    }

    /** Gets all available skills from the user's skill plan in the skill list.
     *
     * @param skillPlanSettings The SkillPlanSettings to use when purchasing.
     * @param skillList A reference to the SkillList instance.
     * @param skillsToBuy The current list of skill names that we plan to buy.
     * @param availableSkillPoints The amount of remaining skill points.
     *
     * @return A mapping of the new skill names and their prices that we want to buy.
     * This only includes the skills that we calculated in this function.
     * It does not include anything from [skillsToBuy].
     */
    private fun getUserPlannedSkills(
        skillPlanSettings: SkillPlanSettings,
        skillList: SkillList,
        skillsToBuy: List<String>,
        availableSkillPoints: Int,
    ): Map<String, Int> {
        if (skillPlanSettings.skillNames.isEmpty()) {
            return emptyMap()
        }

        val result: MutableMap<String, Int> = mutableMapOf()
        var remainingSkillPoints: Int = availableSkillPoints

        // If two different versions of one skill are in the skill list AND in the
        // skill plan, we want to buy the highest level version of that skill that we can afford.
        // For example, if Corner Recovery O and Swinging Maestro are both in the skill
        // plan, and both entries are in the skill list, then we want to buy Swinging Maestro.
        // However, if we do not have enough points for Swinging Maestro, then attempt to
        // buy Corner Recovery O instead.
        for (name in skillPlanSettings.skillNames) {
            // Don't add duplicate entries.
            if (name in skillsToBuy || name in result) {
                continue
            }

            val entry: SkillListEntry? = skillList.getEntry(name)
            if (entry == null) {
                MessageLog.e(TAG, "addUserPlannedSkills: Failed to find entry for \"$name\".")
                continue
            }

            // Handle exact matches.
            if (entry.bIsAvailable) {
                result[name] = entry.screenPrice
                remainingSkillPoints -= entry.screenPrice
                entry.buy()
                continue
            }

            // If there are no exact matches, then our only hope of buying
            // this skill is if it is in an in-place upgrade chain.
            // This is because getting a skill hint for an in-place chain skill
            // means that you can upgrade to any higher versions of it.
            // However if it isn't an in-place chain, then you cannot unlock the
            // upgraded version simply by buying a lower version. The higher
            // version requires a separate skill hint to unlock.

            // If no downgraded versions exist in our skill list, skip this
            // entry since we won't be able to buy it.
            val availableEntry: SkillListEntry = entry.getFirstAvailableDowngrade() ?: continue

            // Otherwise if there IS a downgraded version in the skill list, then
            // we need to purchase it for the planned skill to become available.

            // Get all skill entries from the available entry to the one from
            // the skill plan (inclusive).
            val upgrades: List<SkillListEntry> = availableEntry.getUpgradesUntil(name)

            // Handle in-place upgrade skill chains.
            if (upgrades.all { it.bIsInPlace }) {
                // Only add entries which we haven't already added.
                val unacquired: List<SkillListEntry> = upgrades
                    .filter {it.name !in skillsToBuy && it.name !in result }
            
                val totalPrice: Int = unacquired.sumOf { it.price }
                if (totalPrice <= remainingSkillPoints) {
                    unacquired.forEach { it.buy() }
                    // TODO: Does this need to be screenPrice instead of price?
                    val toAdd: Map<String, Int> = unacquired.associate { it.name to it.price }
                    result.putAll(toAdd)
                    remainingSkillPoints -= totalPrice
                }
                continue
            }
        }

        return result.toMap()
    }

    /** Gets all available negative, inherited unique, and user planned skills.
     *
     * These are used in every skill plan so these functions were grouped together.
     *
     * @param skillPlanSettings The SkillPlanSettings to use when purchasing.
     * @param skillList A reference to the SkillList instance.
     * @param skillsToBuy The current list of skill names that we plan to buy.
     * @param availableSkillPoints The amount of remaining skill points.
     *
     * @return A mapping of the new skill names and their prices that we want to buy.
     * This only includes the skills that we calculated in this function.
     * It does not include anything from [skillsToBuy].
     */
    private fun getSkillsToBuyCommon(
        skillPlanSettings: SkillPlanSettings,
        skillList: SkillList,
        skillsToBuy: List<String>,
        availableSkillPoints: Int,
    ): Map<String, Int> {
        val result: MutableMap<String, Int> = mutableMapOf()

        result += getNegativeSkills(
            skillPlanSettings = skillPlanSettings,
            skillList = skillList,
            skillsToBuy = skillsToBuy + result.keys.toList(),
            availableSkillPoints = availableSkillPoints - result.values.sum(),
        )

        result += getInheritedUniqueSkills(
            skillPlanSettings = skillPlanSettings,
            skillList = skillList,
            skillsToBuy = skillsToBuy + result.keys.toList(),
            availableSkillPoints = availableSkillPoints - result.values.sum(),
        )

        result += getUserPlannedSkills(
            skillPlanSettings = skillPlanSettings,
            skillList = skillList,
            skillsToBuy = skillsToBuy + result.keys.toList(),
            availableSkillPoints = availableSkillPoints - result.values.sum(),
        )

        return result.toMap()
    }

    /** Gets all available skills to buy following the default strategy.
     *
     * NOTE: Currently doesn't do anything but it's here since we have a strategy
     * option for DEFAULT so it'd be weird to not have a function ready for it.
     *
     * @param skillPlanSettings The SkillPlanSettings to use when purchasing.
     * @param skillList A reference to the SkillList instance.
     * @param skillsToBuy The current list of skill names that we plan to buy.
     * @param availableSkillPoints The amount of remaining skill points.
     *
     * @return A mapping of the new skill names and their prices that we want to buy.
     * This only includes the skills that we calculated in this function.
     * It does not include anything from [skillsToBuy].
     */
    private fun getSkillsToBuyDefaultStrategy(
        skillPlanSettings: SkillPlanSettings,
        skillList: SkillList,
        skillsToBuy: List<String>,
        availableSkillPoints: Int,
    ): Map<String, Int> {
        val result: MutableMap<String, Int> = mutableMapOf()

        return result.toMap()
    }

    /** Gets all available skills to buy following the OptimizeSkills strategy.
     *
     * This function attempts to calculate the optimal skills to purchase based
     * on a community made skill tier list. Skills within each tier are just in
     * alphabetical order, so within each tier we optimize the evaluated rank.
     *
     * We also filter skills to only include those that match the user-specified
     * aptitudes for running style, track distance, and track surface.
     *
     * @param skillPlanSettings The SkillPlanSettings to use when purchasing.
     * @param skillList A reference to the SkillList instance.
     * @param skillsToBuy The current list of skill names that we plan to buy.
     * @param availableSkillPoints The amount of remaining skill points.
     *
     * @return A mapping of the new skill names and their prices that we want to buy.
     * This only includes the skills that we calculated in this function.
     * It does not include anything from [skillsToBuy].
     */
    private fun getSkillsToBuyOptimizeSkillsStrategy(
        skillPlanSettings: SkillPlanSettings,
        skillList: SkillList,
        skillsToBuy: List<String>,
        availableSkillPoints: Int,
    ): Map<String, Int> {
        val result: MutableMap<String, Int> = mutableMapOf()
        var remainingSkillPoints: Int = availableSkillPoints

        // Get user specified running style.
        var preferredRunningStyle: RunningStyle? = when (skillSettingRunningStyleString.lowercase()) {
            "no_preference" -> null
            "inherit" -> RunningStyle.fromShortName(racingSettingRunningStyleString) ?: game.trainee.runningStyle
            else -> RunningStyle.fromName(skillSettingRunningStyleString)
        }

        // Get user specified track distance.
        var preferredTrackDistance: TrackDistance? = when (skillSettingTrackDistanceString.lowercase()) {
            "no_preference" -> null
            "inherit" -> TrackDistance.fromName(trainingSettingTrackDistanceString) ?: game.trainee.trackDistance
            else -> TrackDistance.fromName(skillSettingTrackDistanceString)
        }

        // Get user specified track surface.
        var preferredTrackSurface: TrackSurface? = when (skillSettingTrackSurfaceString.lowercase()) {
            "no_preference" -> null
            else -> TrackSurface.fromName(skillSettingTrackSurfaceString)
        }

        MessageLog.d(TAG, "Using preferred running style: $preferredRunningStyle")
        MessageLog.d(TAG, "Using preferred track distance: $preferredTrackDistance")
        MessageLog.d(TAG, "Using preferred track surface: $preferredTrackSurface")

        fun getFilteredSkills(remainingSkillPoints: Int): Map<String, SkillListEntry> {
            // Get only skills which match our aptitudes or user-specified styles or
            // are agnostic of style or track variables.
            val result: MutableMap<String, SkillListEntry> = mutableMapOf()

            result.putAll(skillList.getAptitudeIndependentSkills(preferredRunningStyle))

            if (preferredRunningStyle != null) {
                    result.putAll(skillList.getRunningStyleSkills(preferredRunningStyle))
                    result.putAll(skillList.getInferredRunningStyleSkills(preferredRunningStyle))
            }
            if (preferredTrackDistance != null) {
                result.putAll(skillList.getTrackDistanceSkills(preferredTrackDistance))
            }
            if (preferredTrackSurface != null) {
                result.putAll(skillList.getTrackSurfaceSkills(preferredTrackSurface))
            }

            result.values.removeAll { it.price > remainingSkillPoints }

            return result.toMap()
        }

        // Purchasing skills can cause others to become available, so we need to
        // loop over current skills then update the current skills at the end
        // of the loop until we run out of affordable skills.

        // Infinite loop protection.
        val maxIterations = 10
        var i = 0
        var remainingSkills: Map<String, SkillListEntry> = getFilteredSkills(remainingSkillPoints)
        while (remainingSkills.any { it.value.screenPrice <= remainingSkillPoints}) {
            MessageLog.v(TAG, "\nChecking skills. Iteration #$i.\n")
            // Group the remaining entries by their communityTier. Higher values are better.
            // This can contain a NULL group for skills that are not in the tier list.
            val groupedByCommunityTier: Map<Int?, List<SkillListEntry>> = remainingSkills.values
                .groupBy { it.communityTier }
                .toSortedMap(compareBy { it })

            // Iterate from highest tier to lowest.
            for ((communityTier, group) in groupedByCommunityTier) {
                // Ignore the NULL entries since they aren't ranked.
                if (communityTier == null) {
                    continue
                }

                MessageLog.v(TAG, "============ SKILL COMMUNITY TIER ${SkillCommunityTier.fromOrdinal(communityTier)} =============")

                // Sort the tier by its point ratio.
                val sortedByPointRatio: List<SkillListEntry> = group.sortedByDescending { it.evaluationPointRatio }
                for (entry in sortedByPointRatio) {
                    MessageLog.v(TAG, "\t${entry.name} -> price(shown): ${entry.price}(${entry.screenPrice}), rank(ratio): ${entry.evaluationPoints}(" + "%.2f".format(entry.evaluationPointRatio) + ")")
                    // Don't add duplicate entries.
                    if (entry.name in result || entry.name in skillsToBuy) {
                        continue
                    }

                    // If this skill isn't an in-place upgrade and we have already
                    // added its upgraded version to the list, then don't add it.
                    if (!entry.bIsAvailable) {
                        continue
                    }

                    // If we can't afford this skill, continue to the next.
                    if (entry.screenPrice > remainingSkillPoints) {
                        continue
                    }

                    result[entry.name] = entry.screenPrice
                    remainingSkillPoints -= entry.screenPrice
                    entry.buy()
                }
            }

            remainingSkills = getFilteredSkills(remainingSkillPoints)
            if (i++ > maxIterations) {
                break
            }
        }
        MessageLog.v(TAG, "-------------------------------------------------")

        // We may still have skill points after buying all aptitude-based skills.
        // Spend remaining points to optimize rank.
        result += getSkillsToBuyOptimizeRankStrategy(
            skillPlanSettings = skillPlanSettings,
            skillList = skillList,
            skillsToBuy = skillsToBuy + result.keys.toList(),
            availableSkillPoints = remainingSkillPoints,
        )

        return result.toMap()
    }

    /** Gets all available skills to buy following the OptimizeRank strategy.
     *
     * This function attempts to maximize the trainee's total rank by purchasing
     * skills with the highest evaluated points (rank) to price ratio.
     *
     * The user-specified skill aptitude overrides are ignored in this strategy.
     *
     * @param skillPlanSettings The SkillPlanSettings to use when purchasing.
     * @param skillList A reference to the SkillList instance.
     * @param skillsToBuy The current list of skill names that we plan to buy.
     * @param availableSkillPoints The amount of remaining skill points.
     *
     * @return A mapping of the new skill names and their prices that we want to buy.
     * This only includes the skills that we calculated in this function.
     * It does not include anything from [skillsToBuy].
     */
    private fun getSkillsToBuyOptimizeRankStrategy(
        skillPlanSettings: SkillPlanSettings,
        skillList: SkillList,
        skillsToBuy: List<String>,
        availableSkillPoints: Int,
    ): Map<String, Int> {
        val result: MutableMap<String, Int> = mutableMapOf()
        var remainingSkillPoints: Int = availableSkillPoints

        // Purchasing skills can cause others to become available, so we need to
        // loop over current skills then update the current skills at the end
        // of the loop until we run out of affordable skills.

        // Infinite loop protection.
        val maxIterations = 10
        var i = 0
        var remainingSkills: Map<String, SkillListEntry> = skillList.getAvailableSkills()
        while (remainingSkills.any { it.value.screenPrice <= remainingSkillPoints}) {
            MessageLog.v(TAG, "\nChecking skills. Iteration #$i.\n")
            var sortedByPointRatio: List<SkillListEntry> = remainingSkills.values
                .sortedByDescending { it.evaluationPointRatio }

            MessageLog.v(TAG, "========= SKILLS SORTED BY POINT RATIO ==========")
            for (entry in sortedByPointRatio) {
                MessageLog.v(TAG, "\t${entry.name} -> price(shown): ${entry.price}(${entry.screenPrice}), rank(ratio): ${entry.evaluationPoints}(" + "%.2f".format(entry.evaluationPointRatio) + ")")
                // Don't add duplicate entries.
                if (entry.name in result || entry.name in skillsToBuy) {
                    continue
                }

                // If we can't afford this skill, continue to the next.
                if (entry.screenPrice > remainingSkillPoints) {
                    continue
                }

                result[entry.name] = entry.screenPrice
                remainingSkillPoints -= entry.screenPrice
                entry.buy()
            }

            remainingSkills = skillList.getAvailableSkills()

            if (i++ > maxIterations) {
                break
            }
        }
        MessageLog.v(TAG, "-------------------------------------------------")

        return result.toMap()
    }

    /** Gets all available skills to buy for the user-specified spending strategy.
     *
     * @param skillPlanSettings The SkillPlanSettings to use when purchasing.
     * @param skillList A reference to the SkillList instance.
     * @param skillsToBuy The current list of skill names that we plan to buy.
     * @param availableSkillPoints The amount of remaining skill points.
     *
     * @return A mapping of the new skill names and their prices that we want to buy.
     * This only includes the skills that we calculated in this function.
     * It does not include anything from [skillsToBuy].
     */
    private fun getSkillsToBuy(
        skillPlanSettings: SkillPlanSettings,
        skillList: SkillList,
        availableSkillPoints: Int,
    ): Map<String, Int> {
        MessageLog.d(TAG, "[SKILLS] Beginning process of calculating skills to purchase...")

        if(!skillPlanSettings.bIsEnabled) {
            MessageLog.i(TAG, "[SKILLS] Skill plan is disabled. No skills will be purchased.")
            return emptyMap()
        }

        MessageLog.d(TAG, "======================= Skill Plan =======================")
        MessageLog.d(TAG, "Spending Strategy: ${skillPlanSettings.strategy}")
        MessageLog.d(TAG, "Buy Inherited Skills: ${skillPlanSettings.bEnableBuyInheritedUniqueSkills}")
        MessageLog.d(TAG, "Buy Negative Skills: ${skillPlanSettings.bEnableBuyNegativeSkills}")
        MessageLog.d(TAG, "User-Specified Skills:" + if (skillPlanSettings.skillNames.isEmpty()) " None" else "")
        for (name in skillPlanSettings.skillNames) {
            MessageLog.d(TAG, "\t$name")
        }
        MessageLog.d(TAG, "----------------------------------------------------------")

        val result: MutableMap<String, Int> = mutableMapOf()

        // We always perform these common operations.
        // Other strategies only add on top of these results.
        result += getSkillsToBuyCommon(
            skillPlanSettings = skillPlanSettings,
            skillList = skillList,
            skillsToBuy = result.keys.toList(),
            availableSkillPoints = availableSkillPoints - result.values.sum(),
        )

        result += when (skillPlanSettings.strategy) {
            SpendingStrategy.DEFAULT -> getSkillsToBuyDefaultStrategy(
                skillPlanSettings = skillPlanSettings,
                skillList = skillList,
                skillsToBuy = result.keys.toList(),
                availableSkillPoints = availableSkillPoints - result.values.sum(),
            )
            SpendingStrategy.OPTIMIZE_SKILLS -> getSkillsToBuyOptimizeSkillsStrategy(
                skillPlanSettings = skillPlanSettings,
                skillList = skillList,
                skillsToBuy = result.keys.toList(),
                availableSkillPoints = availableSkillPoints - result.values.sum(),
            )
            SpendingStrategy.OPTIMIZE_RANK -> getSkillsToBuyOptimizeRankStrategy(
                skillPlanSettings = skillPlanSettings,
                skillList = skillList,
                skillsToBuy = result.keys.toList(),
                availableSkillPoints = availableSkillPoints - result.values.sum(),
            )
        }

        MessageLog.d(TAG, "============== Skills To Buy ==============")
        for ((name, price) in result) {
            MessageLog.d(TAG, "\t$name: $price")
        }
        MessageLog.d(TAG, "\n\tTOTAL: ${result.values.sum()} / ${if (USE_MOCK_DATA) MOCK_SKILL_POINTS else skillList.skillPoints} pts")
        MessageLog.d(TAG, "===========================================")

        return result.toMap()
    }

    /** Handles SkillListEntry objects as they are detected.
     *
     * @param entry The SkillListEntry object that we detected.
     * @param point The location of the SkillUpButton for this entry.
     * @param skillsToBuy The list of skill names that we want to purchase.
     * @param skillList The SkillList instance which triggered this event.
     *
     * @return True if all [skillsToBuy] have been purchased. Otherwise, False.
     * [SkillList] uses a return of True to trigger an early exit of its loop.
     */
    private fun onSkillListEntryDetected(
        entry: SkillListEntry,
        point: Point,
        skillsToBuy: List<String>,
        skillList: SkillList,
    ): Boolean {
        if (entry.bIsObtained || entry.bIsVirtual) {
            return false
        }

        if (entry.name !in skillsToBuy) {
            return false
        }

        // Determine if there are other in-place versions of this skill that
        // we need to buy.
        if (entry.bIsInPlace) {
            val namesToBuy: List<String> = listOf(entry.name) +
                entry.getUpgradeNames().filter { it in skillsToBuy }

            for (name in namesToBuy) {
                val result: SkillListEntry? = skillList.buySkill(name, point)
                if (result != null) {
                    MessageLog.i(TAG, "Buying \"${result.name}\" for ${result.price} pts")
                }
            }
        } else {
            val result: SkillListEntry? = skillList.buySkill(entry.name, point)
            if (result != null) {
                MessageLog.i(TAG, "Buying \"${result.name}\" for ${result.price} pts")
            }
        }

        // Now check if we are done purchasing skills.
        val obtained: Map<String, SkillListEntry> = skillList.getObtainedSkills()
        // If we've purchased all planned skills, we return True to force
        // the skill list to exit early from its loop.
        if (skillsToBuy.all { it in obtained }) {
            MessageLog.i(TAG, "All skills purchased. Exiting loop early...")
            return true
        }

        return false
    }

    /** Main function for handling all skill plan purchasing logic.
     *
     * @return Whether the planned operations were successful.
     */
    fun start(skillPlanName: String? = null): Boolean {
        val bitmap: Bitmap = game.imageUtils.getSourceBitmap()

        val skillList = SkillList(game)

        // Verify that we are at the skill list screen.
        val bIsCareerComplete: Boolean = skillList.checkCareerCompleteSkillListScreen(bitmap)
        if (!bIsCareerComplete && !skillList.checkSkillListScreen(bitmap)) {
            MessageLog.e(TAG, "[SKILLS] Not at skill list screen. Aborting...")
            return false
        }

        // These skill plan names are manually set in the app settings.
        // If they are missing, this indicates a programmer error.
        val skillPlanSettings: SkillPlanSettings = if (skillPlanName == null) {
            if (bIsCareerComplete) {
                skillPlans["careerComplete"]!!
            } else {
                skillPlans["preFinals"]!!
            }
        } else {
            val tmpPlan: SkillPlanSettings? = skillPlans[skillPlanName]
            if (tmpPlan == null) {
                MessageLog.e(TAG, "Invalid skill plan name: $skillPlanName")
                return false
            }
            tmpPlan
        }

        // If no options are enabled for purchasing skills, then we should
        // exit early to avoid having to scan the whole skill list.
        if (
            skillPlanSettings.skillNames.isEmpty() &&
            skillPlanSettings.strategy == SpendingStrategy.DEFAULT &&
            !skillPlanSettings.bEnableBuyInheritedUniqueSkills &&
            !skillPlanSettings.bEnableBuyNegativeSkills
        ) {
            MessageLog.w(TAG, "[SKILLS] Skill Plan is empty and no options to purchase any skills are enabled. Aborting...")
            skillList.cancelAndExit()
            return true
        }

        // Purchasing skills depends on us knowing our aptitudes.
        // If we haven't acquired them yet, then we need to force check them.
        // This can happen if we start the bot at the skill list screen
        // or at the end of a career.
        if (!USE_MOCK_DATA && !game.trainee.bHasUpdatedAptitudes) {
            skillList.checkStats()
        }

        val skillPoints: Int = if (USE_MOCK_DATA) {
            MOCK_SKILL_POINTS
        } else {
            skillList.detectSkillPoints(bitmap) ?: 0
        }
        // The cheapest skills are all 70 points and with discounts
        // can be as low as 42 points. If anything is less than this, then
        // we should update this.
        if (skillPoints < 42) {
            MessageLog.i(TAG, "[SKILLS] Skill Points < 42. Cannot afford any skills. Aborting...")
            skillList.cancelAndExit()
            return true
        }
        
        // Gather all skill list entry data.
        skillList.parseSkillListEntries(bUseMockData = USE_MOCK_DATA)
        if (skillList.getAllSkills().isEmpty()) {
            MessageLog.e(TAG, "[SKILLS] Failed to detect skills.")
            skillList.cancelAndExit()
            return false
        }

        skillList.printSkillListEntries(verbose = true)

        // Calculate list of skills to purchase.
        val result: Map<String, Int> = getSkillsToBuy(
            skillPlanSettings = skillPlanSettings,
            skillList = skillList,
            availableSkillPoints = skillPoints,
        )

        // No skills to buy. Return to previous screen.
        if (result.isEmpty()) {
            skillList.cancelAndExit()
            return true
        }

        // Sell all skills so we can use the `skillList.getObtainedSkills`
        // to determine if we've finished buying all skills.
        skillList.sellAllSkills()

        // Go back through skill list and purchase skills.
        skillList.parseSkillListEntries { skillList: SkillList, entry: SkillListEntry, point: Point ->
            onSkillListEntryDetected(
                entry = entry,
                point = point,
                skillsToBuy = result.keys.toList(),
                skillList = skillList,
            )
        }

        skillList.confirmAndExit()

        return true
    }
}
