package com.steve1316.uma_android_automation.bot

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

import org.opencv.core.Point

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.utils.DoublyLinkedList
import com.steve1316.uma_android_automation.utils.DoublyLinkedListNode

import com.steve1316.uma_android_automation.components.ButtonSkillUp

import com.steve1316.uma_android_automation.types.BoundingBox
import com.steve1316.uma_android_automation.types.SkillData
import com.steve1316.uma_android_automation.types.SkillType
import com.steve1316.uma_android_automation.types.RunningStyle
import com.steve1316.uma_android_automation.types.TrackDistance
import com.steve1316.uma_android_automation.types.TrackSurface
import com.steve1316.uma_android_automation.types.Aptitude

/** Represents a single skill entry in a skill list.
 *
 * This class acts as a doubly linked list node, with references to both its direct
 * upgrade and downgraded versions.
 *
 * @param game Reference to the bot's Game instance.
 * @param skillData The SkillData instance containing static skill information.
 * @param bIsObtained Whether this entry has been purchased.
 * @param bIsVirtual Whether this entry is considered a virtual entry in the skill list.
 * Virtual entries are in-place upgrades to skills that currently exist in the list.
 * Since these in-place upgrades do not appear in the list until all previous
 * versions have been purchased, we consider them to be "virtual" entries.
 * @param prev A pointer to this skill's downgrade SkillListEntry.
 * @param next A pointer to this skill's upgrade SkillListEntry.
 *
 * @property name The skill name (from skillData).
 * @property screenPrice The current price of the skill as it is shown in the game.
 * @property price The actual price of just this skill, ignoring previous version prices.
 * @property rawPrice [price] but without any discounts applied.
 * @property discount The current discount percentage / 100.
 * @property evaluationPoints The amount of rank gained upon purchasing this skill.
 * @property evaluationPointRatio The ratio of rank to [price].
 * @property bIsAvailable Whether this skill is available for purchase.
 * @property bIsInheritedUnique Whether this skill is a unique skill inherited from a legacy uma.
 * @property bIsNegative Whether this is a negative (purple icon) skill.
 * @property bIsInPlace Whether this skill can be upgraded in-place.
 * @property runningStyle The RunningStyle associated with this skill.
 * If no RunningStyle applies, then this value will be NULL.
 * @property trackDistance The TrackDistance associated with this skill.
 * If no TrackDistance applies, then this value will be NULL.
 * @property trackSurface The TrackSurface associated with this skill.
 * If no TrackSurface applies, then this value will be NULL.
 * @property inferredRunningStyles The inferred RunningStyles associated with this skill.
 * Can be empty if none apply.
 * @property communityTier The community ranking of this skill where lower values are better.
 * @property baseCost The base price of this skill without any discounts applied from SkillData.
 */
class SkillListEntry(
    private val game: Game,
    val skillData: SkillData,
    var bIsObtained: Boolean = false,
    var bIsVirtual: Boolean = false,
    // Pointers for linked-list style navigation.
    var prev: SkillListEntry? = null,
    var next: SkillListEntry? = null,
) {
    private val TAG: String = "[${MainActivity.loggerTag}]SkillListEntry"

    private val EVALUATION_POINT_APTITUDE_RATIO_MAP: Map<Aptitude, Double> = mapOf(
        Aptitude.S to 1.1,
        Aptitude.A to 1.1,
        Aptitude.B to 0.9,
        Aptitude.C to 0.9,
        Aptitude.D to 0.8,
        Aptitude.E to 0.8,
        Aptitude.F to 0.8,
        Aptitude.G to 0.7,
    )

    private val DISCOUNT_VALUES: List<Double> = listOf(
        0.0,
        0.1,
        0.2,
        0.3,
        0.35,
        0.4,
    )

    val name: String = skillData.name

    // Skills can't be cheaper than their max discount.
    private val minScreenPrice: Int = (skillData.cost * (1.0 - DISCOUNT_VALUES.last())).roundToInt()
    // For skills that aren't in-place upgradable, their screen price also includes any
    // previous unpurchased versions prices. The skillData.cost that we scraped does not
    // account for this and is only the price of the skill if its upgrades have all
    // been purchased. Double this cost is a safe margin of error since no lower
    // versions are more expensive than their upgraded variant.
    private val maxScreenPrice: Int = if (skillData.bIsInPlace) skillData.cost else skillData.cost * 2
    // This is the price tag shown on the screen for the skill.
    // NOTE: We need to make sure to call updateScreenPrice() after fully
    // setting up the entire upgrade chain linked list.
    var screenPrice: Int = maxScreenPrice
    // Copy of the screen price that is only ever modified in `updateScreenPrice`.
    // We use this so that we have a baseline for what the screen price was
    // when we originally read it with OCR.
    // This is necessary for some calculations since `screenPrice` is modified
    // in multiple functions.
    private var originalScreenPrice: Int = screenPrice

    val price: Int
        get() = calculatePrice()

    // The adjusted price without a discount applied.
    val rawPrice: Int
        get() = ceil(price.toDouble() / (1.0 - discount)).toInt()

    val discount: Double
        get() = calculateDiscount()

    val evaluationPoints: Int
        get() = calculateEvaluationPoints()
    
    val evaluationPointRatio: Double
        get() = calculateEvaluationPointRatio()

    val bIsAvailable: Boolean
        get() = !bIsObtained && !bIsVirtual
    
    // Wrappers around commonly used SkillData values
    // so we don't have to directly reference skillData.
    val bIsInheritedUnique: Boolean = skillData.bIsInheritedUnique
    val bIsNegative: Boolean = skillData.bIsNegative
    val bIsInPlace: Boolean = skillData.bIsInPlace

    val runningStyle: RunningStyle? = skillData.runningStyle
    val trackDistance: TrackDistance? = skillData.trackDistance
    val trackSurface: TrackSurface? = skillData.trackSurface
    val inferredRunningStyles: List<RunningStyle> = skillData.inferredRunningStyles

    val communityTier: Int? = skillData.communityTier
    val baseCost: Int = skillData.cost


    init {
        // Update linked list pointers if they were passed in.
        val prev: SkillListEntry? = prev
        if (prev != null) {
            prev.next = this
        }

        val next: SkillListEntry? = next
        if (next != null) {
            next.prev = this
        }
    }

    /** Rounds a discount value to the nearest predetermined valid value.
     *
     * Until hint level 3, discount value increases by 10%.
     * After this point, value only increases by 5% up to a max of 40%.
     *
     * @return The rounded discount value.
     */
    private fun Double.roundDiscount(): Double {
        return DISCOUNT_VALUES.minBy { abs(it - this) }
    }

    /** Calculates the discount amount based on the current price.
     *
     * @return The discount as a float between 0.0 and 1.0.
     * The discount is rounded to one of the valid discount amounts.
     * See: [Double.roundDiscount]
     */
    private fun calculateDiscount(): Double {
        if (screenPrice <= 0) {
            return 0.0
        }

        val res: Double = 1.0 - (price.toDouble() / skillData.cost.toDouble())
        return res.roundDiscount()
    }

    /** Calculates the standalone price of this entry.
     *
     * This is the price of this entry without including the prices
     * of any previous versions of the skill.
     *
     * @return The calculated price.
     */
    private fun calculatePrice(): Int {
        val prev: SkillListEntry? = prev
        if (prev == null) {
            return screenPrice
        }

        // For skills that have downgrades as separate entries in the skill list,
        // we need to subtract the downgrade's price from our screenPrice.
        // We need to calculate this adjusted value in order for
        // `calculateDiscount` to be accurate.
        // This is because the skills in this type of upgrade chain can have
        // different hint levels and thus different discount values.
        if (!bIsInPlace && !prev.bIsObtained && !prev.bIsVirtual) {
            val adjustedPrice: Int = (screenPrice - prev.price).coerceIn(0, 500)
            return adjustedPrice
        }

        return screenPrice
    }

    /** Gets the evaluation point modifier for any running style requirements.
     *
     * @return If this skill's activation conditions require a specific running style,
     * then we return the evaluation point modifier for that style.
     * If there are no such conditions, then we return NULL.
     */
    private fun getRunningStyleAptitudeEvaluationModifier(): Double? {
        val runningStyle: RunningStyle = runningStyle ?: return null
        val aptitude: Aptitude = game.trainee.checkRunningStyleAptitude(runningStyle)
        return EVALUATION_POINT_APTITUDE_RATIO_MAP[aptitude]
    }

    /** Gets the evaluation point modifier for any track distance requirements.
     *
     * @return If this skill's activation conditions require a specific track distance,
     * then we return the evaluation point modifier for that distance.
     * If there are no such conditions, then we return NULL.
     */
    private fun getTrackDistanceAptitudeEvaluationModifier(): Double? {
        val trackDistance: TrackDistance = trackDistance ?: return null
        val aptitude: Aptitude = game.trainee.checkTrackDistanceAptitude(trackDistance)
        return EVALUATION_POINT_APTITUDE_RATIO_MAP[aptitude]
    }

    /** Gets the evaluation point modifier for any track surface requirements.
     *
     * @return If this skill's activation conditions require a specific track surface,
     * then we return the evaluation point modifier for that surface.
     * If there are no such conditions, then we return NULL.
     */
    private fun getTrackSurfaceAptitudeEvaluationModifier(): Double? {
        val trackSurface: TrackSurface = trackSurface ?: return null
        val aptitude: Aptitude = game.trainee.checkTrackSurfaceAptitude(trackSurface)
        return EVALUATION_POINT_APTITUDE_RATIO_MAP[aptitude]
    }

    /** Calculates the evaluation points rewarded upon purchasing this skill.
     *
     * For skills that don't have an in-place downgrade, this function adds
     * the evaluation points for any unpurchased downgrades for this skill
     * to the result.
     *
     * @return The calculated evaluation points.
     */
    private fun calculateEvaluationPoints(): Int {
        var res: Int = skillData.evalPt

        val prev: SkillListEntry? = prev
        if (prev != null && prev.bIsAvailable) {
            res += prev.evaluationPoints
        }

        val modifier: Double = getRunningStyleAptitudeEvaluationModifier() ?:
            getTrackDistanceAptitudeEvaluationModifier() ?:
            getTrackSurfaceAptitudeEvaluationModifier() ?:
            1.0

        return (res * modifier).roundToInt()
    }

    /** Calculates the ratio of evaluation points to the price of this skill.
     *
     * @return The ratio of evaluation points to price.
     */
    private fun calculateEvaluationPointRatio(): Double {
        return evaluationPoints.toDouble() / screenPrice.toDouble()
    }

    /** Clamps a value to a valid range for the screen price of this skill.
     *
     * The [screenPrice] of a skill can only ever be within a specific range
     * of values based on the discounts and purchased downgrade versions.
     *
     * @param value The value to clamp.
     *
     * @return The clamped value.
     */
    private fun clampValueForScreenPrice(value: Int): Int {
        if (value in minScreenPrice..maxScreenPrice) {
            return value
        }

        val prev: SkillListEntry? = prev
        if (bIsInPlace || prev == null) {
            return skillData.cost
        }

        return if (prev.bIsObtained) skillData.cost else skillData.cost * 2
    }

    /** Manually set the screen price.
     *
     * This also updates the [originalScreenPrice] which is not modified
     * anywhere else.
     *
     * @param value The new screen price.
     */
    fun updateScreenPrice(value: Int) {
        val clampedValue: Int = clampValueForScreenPrice(value)
        screenPrice = clampedValue
        originalScreenPrice = clampedValue

        // Need to manually push changes to other in-place versions.
        if (bIsInPlace) {
            val next: SkillListEntry = next ?: return
            val nextBaseCost: Int = next.baseCost ?: return
            next.updateScreenPrice(((1.0 - discount) * nextBaseCost.toDouble()).roundToInt())
        }
    }

    /** Handler for when a downgraded version of this skill has been purchased.
     *
     * For skills that are in-place upgradable:
     *      When these skills are purchased, the next level of the skill
     *      replaces the current version in the skill list. So we need to
     *      update the new skill's bIsVirtual to false.
     *      We also need to update the price using the previous version's discount.
     * For skills that are not in-place upgradable:
     *      When these skills are purchased, any upgraded versions of the skill
     *      that exist in the list will have their price reduced by the price
     *      of the purchased skill.
     *
     * @param entry The downgraded version of this skill that was purchased.
     */
    fun onDowngradePurchased(entry: SkillListEntry) {
        if (bIsInPlace) {
            // Purchasing an in-place upgrade skill will make the next upgrade
            // available in the skill list. This entry is no longer virtual.
            bIsVirtual = false
            val prev: SkillListEntry? = prev
            if (prev != null) {
                screenPrice = (skillData.cost.toDouble() * (1.0 - prev.discount)).roundToInt()
            }
        } else {
            // Otherwise if the two skills are separate entries in the skill list,
            // then we need to subtract the purchased skill's price from our
            // screen price since that is how it is displayed in game.

            // We always want to calculate from the original screen price.
            // Otherwise, we may end up compounding this subtraction in
            // successive calls to this function.
            screenPrice = (originalScreenPrice - entry.price).coerceIn(0, 500)

            // Propagate forward.
            val next: SkillListEntry? = next
            if (next != null) {
                next.onDowngradePurchased(this)
            }
        }
    }

    /** Handler for when an upgraded version of this skill has been purchased.
     *
     * @param entry The upgraded version of this skill that was purchased.
     */
    fun onUpgradePurchased(entry: SkillListEntry) {
        // Whenever a higher version of a skill is purchased,
        // all lower versions are automatically purchased.
        bIsObtained = true

        // For in-place version chains, purchasing a higher version
        // replaces the older version in the list. Make sure we reflect this
        // change by updating replaced entries to be virtual since they no
        // longer exist in the skill list.
        if (bIsInPlace) {
            bIsVirtual = true
        }

        // Propagate backward.
        val prev: SkillListEntry? = prev
        if (prev != null) {
            prev.onUpgradePurchased(this)
        }
    }

    /** Handler for when a downgraded version of this skill has been sold.
     *
     * NOTE: See the [sell] function for more information.
     *
     * @param entry The downgraded version of this skill that was sold.
     */
    fun onDowngradeSold(entry: SkillListEntry) {
        // We can't sell the previous version of an in-place skill without
        // first selling this version. So do nothing here.
        if (bIsInPlace) {
            return
        }

        bIsVirtual = false
        bIsObtained = false

        screenPrice = originalScreenPrice

        // Propagate forward.
        val next: SkillListEntry? = next
        if (next != null) {
            next.onDowngradeSold(this)
        }
    }

    /** Buys the skill and updates other versions of the skill.
     *
     * Upon purchasing a skill, the other versions of the skill need to be
     * updated to reflect this change.
     *
     * For skills that aren't in-place upgrades, the downgraded version
     * of the skill needs to be purchased and upgrades need to have
     * their prices reduced by this skill's price.
     *
     * For skills that are in-place upgrades, the current version becomes
     * a virtual entry and the upgraded version is no longer virtual.
     *
     * @param skillUpLocation The screen location of the Skill Up button.
     * If not NULL, then the bot will click at the location.
     */
    fun buy(skillUpLocation: Point? = null) {
        if (skillUpLocation != null) {
            game.tap(
                skillUpLocation.x,
                skillUpLocation.y,
                ButtonSkillUp.template.path,
            )
        }

        bIsObtained = true

        // Propagate changes backward.
        val prev: SkillListEntry? = prev
        if (prev != null) {
            prev.onUpgradePurchased(this)
        }

        // Propagate changes forward.
        val next: SkillListEntry? = next
        if (next != null) {
            next.onDowngradePurchased(this)
        }
    }

    /** "Sell" this skill.
     *
     * This doesn't actually sell a skill since that isn't a thing you can do
     * after a skill has been obtained and confirmed.
     *
     * What this does is it resets the [bIsObtained] variable to false and propagates
     * this change to the other versions of this skill.
     * This will bring this entry back to its original state in the skill list
     * before this or the direct downgrade to this skill have been modified.
     * This also means that the upgraded versions of this skill will have their
     * prices updated to reflect this change.
     */
    fun sell() {
        // Can't sell a skill we don't have.
        if (!bIsObtained) {
            return
        }

        bIsObtained = false

        // Selling a skill has no effect on lower versions so we don't need
        // to propagate changes backward.

        // Propagate changes forward.
        val next: SkillListEntry? = next
        if (next != null) {
            next.onDowngradeSold(this)
        }
    }

    /** Returns the lowest downgraded version for this skill that is not virtual.
     *
     * This function is useful for when we know that we want to purchase this skill
     * but we don't know if it has any downgrades in the skill list. It isn't
     * super useful for anything other than in-place upgrade chains, however.
     *
     * For example, if we want to buy "Firm Conditions ◎" but it doesn't exist
     * in the skill list, but "Firm Conditions ×" does exist. This function would
     * return the entry for "Firm Conditions ×" which we could then upgrade twice
     * to get our desired skill.
     *
     * @return The first available version of this skill in the skill list.
     */
    fun getFirstAvailableDowngrade(): SkillListEntry? {
        var entry: SkillListEntry? = prev
        while (entry != null) {
            if (entry.bIsAvailable) {
                return entry
            }
            entry = entry.prev
        }

        return null
    }

    /** Returns a list of all of this skill's downgraded entries.
     *
     * @return The ordered list of this skill's downgraded version SkillListEntry objects.
     */
    fun getDowngrades(): List<SkillListEntry> {
        val result: MutableList<SkillListEntry> = mutableListOf()
        var entry: SkillListEntry? = this
        while (entry != null) {
            // Don't add ourself to the results.
            if (entry != this) {
                result.add(entry)
            }
            entry = entry.prev
        }
        // Reverse so list is in lowest to highest version order.
        return result.reversed().toList()
    }

    /** Returns a list of all of this skill's downgraded entry names.
     *
     * @return The ordered list of this skill's downgraded version names.
     */
    fun getDowngradeNames(): List<String> {
        return getDowngrades().map { it.name }
    }

    /** Returns a list of all of this skill's downgrades until a specific entry.
     *
     * This is effectively a custom linked list slicing function
     *
     * @param name The stopping point for the list of downgrades.
     *
     * @return The list of downgraded entries from the stopping point to the
     * current entry.
     */
    fun getDowngradesUntil(name: String): List<SkillListEntry> {
        val result: MutableList<SkillListEntry> = mutableListOf(this)

        // Early exit if passed our own name.
        if (this.name == name) {
            return result.toList()
        }

        val downgrades: List<SkillListEntry> = getDowngrades()
        var foundMatch = false
        for (entry in downgrades.reversed()) {
            result.add(entry)
            if (entry.name == name) {
                return result.toList()
            }
        }

        return emptyList()
    }

    /** Returns just the entry names from [getDowngradesUntil].
     *
     * This is effectively a custom linked list slicing function
     *
     * @param name The stopping point for the list of downgrades.
     *
     * @return The list of downgraded entry names from the stopping point
     * to the current entry.
     */
    fun getDowngradeNamesUntil(name: String): List<String> {
        return getDowngradesUntil(name).map { it.name }
    }

    /** Returns a list of all of this skill's upgraded entries.
     *
     * @return The ordered list of this skill's upgraded version SkillListEntry objects.
     */
    fun getUpgrades(): List<SkillListEntry> {
        val result: MutableList<SkillListEntry> = mutableListOf()
        var entry: SkillListEntry? = this
        while (entry != null) {
            // Don't add ourself to the results.
            if (entry != this) {
                result.add(entry)
            }
            entry = entry.next
        }
        return result.toList()
    }

    /** Returns a list of all of this skill's upgraded entry names.
     *
     * @return The ordered list of this skill's upgraded version names.
     */
    fun getUpgradeNames(): List<String> {
        return getUpgrades().map { it.name }
    }

    /** Returns a list of all of this skill's upgrades until a specific entry.
     *
     * This is effectively a custom linked list slicing function
     *
     * @param name The stopping point for the list of upgrades.
     *
     * @return The list of upgraded entries from the stopping point to the
     * current entry.
     */
    fun getUpgradesUntil(lastName: String): List<SkillListEntry> {
        val result: MutableList<SkillListEntry> = mutableListOf(this)

        if (name == lastName) {
            return result.toList()
        }

        val upgrades: List<SkillListEntry> = getUpgrades()
        var foundMatch = false
        for (entry in upgrades) {
            result.add(entry)
            if (entry.name == lastName) {
                return result.toList()
            }
        }

        return emptyList()
    }

    /** Returns just the entry names from [getUpgradesUntil].
     *
     * This is effectively a custom linked list slicing function
     *
     * @param name The stopping point for the list of upgrades.
     *
     * @return The list of upgraded entry names from the current entry to
     * the stopping point.
     */
    fun getUpgradeNamesUntil(name: String): List<String> {
        return getUpgradesUntil(name).map { it.name }
    }

    /** Returns the ordered list of all entries in this skill's upgrade chain.
     *
     * @return The ordered list of all SkillListEntries in the upgrade chain.
     */
    fun getVersions(): List<SkillListEntry> {
        return getDowngrades() + this + getUpgrades()
    }

    /** Returns the ordered list of all entry names in this skill's upgrade chain.
     *
     * @return The ordered list of skill names.
     */
    fun getVersionNames(): List<String> {
        return getVersions().map { it.name }
    }

    /** Returns a (slightly) user friendly string of this class's key properties.
     *
     * @return A formatted string containing this instance's information.
     */
    override fun toString(): String {
        val evaluationPointRatioString: String = "%.2f".format(evaluationPointRatio)
        return "{" +
            "name: \"${name}\", " +
            "virtual: $bIsVirtual, " +
            "obtained: $bIsObtained, " +
            "price: $price ($screenPrice), " +
            "discount: ${(discount * 100).roundToInt()}%, " +
            "evalPt: $evaluationPoints ($evaluationPointRatioString / pt)" +
            "}"
    }
}
