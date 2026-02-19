package com.steve1316.uma_android_automation.bot

import android.util.Log
import android.graphics.Bitmap
import org.opencv.core.Point
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.data.SharedData

import com.steve1316.uma_android_automation.utils.ScrollList
import com.steve1316.uma_android_automation.utils.ScrollListEntry
import com.steve1316.uma_android_automation.types.BoundingBox
import com.steve1316.uma_android_automation.types.TrackDistance
import com.steve1316.uma_android_automation.types.TrackSurface
import com.steve1316.uma_android_automation.types.RunningStyle
import com.steve1316.uma_android_automation.types.SkillData

import com.steve1316.uma_android_automation.components.*

/** A callback that fires whenever we detect an entry in the skill list.
 *
 * @param skillList A reference to the SkillList instance which fired this callback.
 * @param entry The SkillListEntry instance which was detected.
 * @param skillUpButtonLocation The screen location of the SkillUpButton for this entry.
 *
 * @return Early exit flag. A value of True is used to exit from the entry detection
 * loop early.
 */
typealias OnEntryDetectedCallback = (
    skillList: SkillList,
    entry: SkillListEntry,
    skillUpButtonLocation: Point,
) -> Boolean

/** Handles interaction with the skill list and manages skill list entries.
 *
 * @param game Reference to the bot's Game instance.
 *
 * @property skillPoints The current remaining skill points.
 * Defaults to NULL if not yet detected.
 * The value is set in the [detectSkillPoints] function.
 */
class SkillList (private val game: Game) {
    private val TAG: String = "[${MainActivity.loggerTag}]SkillList"

    private var entries: Map<String, SkillListEntry> = generateSkillListEntries()
    var skillPoints: Int = 0
        private set

    /**
     * Detects and handles any dialog popups.
     *
     * To prevent the bot moving too fast, we add a 500ms delay to the
     * exit of this function whenever we close the dialog.
     * This gives the dialog time to close since there is a very short
     * animation that plays when a dialog closes.
     *
     * @return A pair of a boolean and a nullable DialogInterface.
     * The boolean is true when a dialog has been handled by this function.
     * The DialogInterface is the detected dialog, or NULL if no dialogs were found.
     */
    private fun handleDialogs(): Pair<Boolean, DialogInterface?> {
        val dialog: DialogInterface = DialogUtils.getDialog(game.imageUtils)
        if (dialog == null) {
            Log.d(TAG, "\n[SKILLS] No dialogs found.")
            return Pair(false, null)
        }

        when (dialog.name) {
            "skill_list_confirmation" -> {
                dialog.ok(game.imageUtils)
                // This dialog takes longer to close than others.
                // Add an extra delay to make sure we don't skip anything.
                game.wait(1.0)
            }
            "skill_list_confirm_exit" -> dialog.ok(game.imageUtils)
            "skills_learned" -> dialog.close(game.imageUtils)
            "umamusume_details" -> {
                game.trainee.updateAptitudes(game.imageUtils)
                dialog.close(game.imageUtils)
            }
            else -> {
                Log.w(TAG, "[SKILLS] Unknown dialog \"${dialog.name}\" detected so it will not be handled.")
                return Pair(false, dialog)
            }
        }

        game.wait(0.5, skipWaitingForLoading = true)
        return Pair(true, dialog)
    }

    /** Creates a mapping of skill names to SkillListEntry objects.
     *
     * This function uses the skill database to populate our [entries] object.
     * While building the [entries] object, we also make sure to preserve the
     * structure of skill upgrade chains by linking them together when
     * instantiating the SkillListEntry object.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    private fun generateSkillListEntries(): Map<String, SkillListEntry> {
        // Get list of unique upgrade chains.
        val upgradeChains: List<List<String>> = game.skillDatabase.skillUpgradeChains
            .values.toList()
            .toSet()
            .toList()

        val result: MutableMap<String, SkillListEntry> = mutableMapOf()
        
        for (chain in upgradeChains) {
            var prevEntry: SkillListEntry? = null
            for (name in chain) {
                if (name in result) {
                    continue
                }
                val skillData: SkillData? = game.skillDatabase.getSkillData(name)
                if (skillData == null) {
                    MessageLog.e(TAG, "Failed to get skill data for \"$name\".")
                    continue
                }
                // Because we're building this mapping before we've scanned
                // the skill list, all entries are virtual.
                val entry = SkillListEntry(game, skillData, bIsVirtual = true, prev = prevEntry)

                // Now add to our mapping.
                result[name] = entry

                prevEntry = entry
            }
        }
        return result
    }

    /** Gets the bounding region for all Skill Up (+) buttons in the skill list.
     *
     * @param bitmap Optional bitmap used for debugging.
     * @param bboxSkillListEntries The bounding region of all skill list entries on screen.
     *
     * @return On success, the bounding region. On failure, NULL.
     */
    private fun getSkillListSkillUpBoundingRegion(
        bitmap: Bitmap? = null,
        bboxSkillListEntries: BoundingBox,
        debugString: String = ""
    ): BoundingBox {
        // Smaller region used to detect SkillUp buttons in the list.
        val bbox = BoundingBox(
            x = game.imageUtils.relX((bboxSkillListEntries.x + bboxSkillListEntries.w).toDouble(), -125),
            y = bboxSkillListEntries.y,
            w = game.imageUtils.relWidth(70),
            h = bboxSkillListEntries.h,
        )
        if (game.debugMode) {
            val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
            game.imageUtils.saveBitmap(bitmap, "skillUpRegion_$debugString", bbox)
        }

        return bbox
    }

    /** Gets the bounding region for all Obtained Pill icons in the skill list.
     *
     * @param bitmap Optional bitmap used for debugging.
     * @param bboxSkillListEntries The bounding region of all skill list entries on screen.
     *
     * @return On success, the bounding region. On failure, NULL.
     */
    private fun getSkillListObtainedPillBoundingRegion(
        bitmap: Bitmap? = null,
        bboxSkillListEntries: BoundingBox,
        debugString: String = "",
    ): BoundingBox {
        val bbox = BoundingBox(
            x = game.imageUtils.relX((bboxSkillListEntries.x + bboxSkillListEntries.w).toDouble(), -260),
            y = bboxSkillListEntries.y,
            w = game.imageUtils.relWidth(140),
            h = bboxSkillListEntries.h,
        )
        if (game.debugMode) {
            val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
            game.imageUtils.saveBitmap(bitmap, "obtainedPillRegion_$debugString", bbox)
        }

        return bbox
    }

    /** Extracts all text from a bitmap.
     *
     * @param bitmap The bitmap to extract text from.
     *
     * @return The extracted text as a string.
     * An empty string is returned if nothing is detected.
     */
    private fun extractText(bitmap: Bitmap): String {
        try {
            val detectedText = game.imageUtils.performOCROnRegion(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                useThreshold = false,
                useGrayscale = true,
                scale = 2.0,
                ocrEngine = "mlKit",
                debugName = "analyzeSkillListEntry::extractText"
            )
            return detectedText
        } catch (e: Exception) {
            MessageLog.e(TAG, "Exception during text extraction: ${e.message}")
            return ""
        }
    }

    /** Extracts the skill points from a bitmap.
     *
     * @param bitmap Optional bitmap used to detect the skill points.
     * If not specified, a screenshot is taken and used instead.
     *
     * @return On success, the skill points as an integer. On failure, NULL.
     */
    fun detectSkillPoints(bitmap: Bitmap? = null): Int? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val templateBitmap: Bitmap? = LabelSkillListScreenSkillPoints.template.getBitmap(game.imageUtils)
        if (templateBitmap == null) {
            MessageLog.e(TAG, "Failed to load template bitmap for LabelSkillListScreenSkillPoints.")
            return null
        }

        val point: Point? = LabelSkillListScreenSkillPoints.findImageWithBitmap(game.imageUtils, bitmap)
        if (point == null) {
            MessageLog.e(TAG, "Failed to find LabelSkillListScreenSkillPoints.")
            return null
        }

        val bbox = BoundingBox(
            x = (point.x + templateBitmap.width).toInt(),
            y = (point.y - templateBitmap.height).toInt(),
            w = (templateBitmap.width * 1.5).toInt(),
            h = (templateBitmap.height * 2).toInt(),
        )

        val skillPointsBitmap: Bitmap? = game.imageUtils.createSafeBitmap(bitmap, bbox, "skillPointsBitmap")
        if (skillPointsBitmap == null) {
            MessageLog.e(TAG, "[SKILLS] detectSkillPoints: Failed to createSafeBitmap for skill points.")
            return null
        }

        val skillPointsString: String = extractText(skillPointsBitmap)
        val tmpSkillPoints: Int? = skillPointsString
            .replace("[^0-9]".toRegex(), "")
            .toIntOrNull()
        if (tmpSkillPoints != null) {
            skillPoints = tmpSkillPoints
        }
        return skillPoints
    }

    /** Confirms skill purchases and backs out of the Skill List screen. */
    fun confirmAndExit() {
        ButtonConfirm.click(game.imageUtils)
        game.wait(0.5, skipWaitingForLoading = true)
        // Two dialogs will appear if we purchase any skills.
        // First is the purchase confirmation.
        handleDialogs()
        // Second is the Skills Learned dialog.
        handleDialogs()
        // Return to previous screen.
        ButtonBack.click(game.imageUtils)
    }

    /** Aborts spending skill points and backs out of the Skill List screen. */
    fun cancelAndExit() {
        // Reset skills to prevent popup.
        ButtonReset.click(game.imageUtils)
        ButtonBack.click(game.imageUtils)
        game.wait(0.5, skipWaitingForLoading = true)
        // As a failsafe, handle dialogs to catch the dialog for
        // aborting spending skill points.
        handleDialogs()
    }

    /** Opens the stats dialog and parses it.
     *
     * This allows our dialog handler to update aptitudes for the trainee.
     * This is useful for when we start the bot at the skills list or at
     * the end of a career when aptitudes are unknown.
     *
     * Evaluating skills relies on these aptitudes being known since many
     * skills are dependent on things such as running style or track distance.
     */
    fun checkStats() {
        ButtonSkillListFullStats.click(game.imageUtils)
        game.wait(0.5, skipWaitingForLoading = true)
        handleDialogs()
    }

    /** Extracts the title (skill name) from a cropped skill list entry bitmap.
     *
     * @param bitmap A bitmap of a single cropped skill list entry.
     *
     * @return On success, the title string. On failure, NULL.
     */
    fun getSkillListEntryTitle(bitmap: Bitmap? = null, debugString: String = ""): String? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val bbox = BoundingBox(
            x = (bitmap.width * 0.142).toInt(),
            y = 0,
            w = (bitmap.width * 0.57).toInt(),
            h = (bitmap.height * 0.338).toInt(),
        )
        val croppedTitle = game.imageUtils.createSafeBitmap(bitmap, bbox, "bboxTitle_$debugString")
        if (croppedTitle == null) {
            Log.e(TAG, "[SKILLS] getSkillListEntryTitle: createSafeBitmap for croppedTitle returned NULL.")
            return null
        }
        if (game.debugMode) {
            game.imageUtils.saveBitmap(croppedTitle, filename = "bboxTitle_$debugString")
        }

        var skillName: String = extractText(croppedTitle)
        if (skillName == "") {
            Log.e(TAG, "[SKILLS] getSkillListEntryTitle: Failed to extract skill name.")
            return null
        }

        // Check if the skill has a special char (◎, ○, ×) at the end.
        val componentsToCheck: List<ComponentInterface> = listOf(
            IconSkillTitleDoubleCircle,
            IconSkillTitleCircle,
            IconSkillTitleX,
        )
        var match: ComponentInterface? = null
        for (component in componentsToCheck) {
            val point: Point? = component.findImageWithBitmap(game.imageUtils, croppedTitle)
            if (point != null) {
                match = component
                break
            }
        }

        // Get the appropriate char to append to the search string.
        // Typically the extracted title will end with "O" or "x"
        // but we can't just replace that character since some titles
        // actually end in those letters. So we just append this to the title
        // since it shouldn't cause fuzzy matching to fail.
        val iconChar: String = when (match) {
            IconSkillTitleDoubleCircle -> "◎"
            IconSkillTitleCircle -> "○"
            IconSkillTitleX -> "×"
            else -> ""
        }
        skillName += iconChar

        // Most negative skills have "Remove" in front of their skill
        // name in the title. The actual skill itself in the database does
        // not have this prefix. We need to get rid of this prefix as it
        // causes fuzzy matching to fail.
        skillName = if (skillName.startsWith("remove", ignoreCase = true)) {
            skillName.drop("remove".length)
        } else {
            skillName
        }

        return skillName
    }
    
    /** Extracts the price from a cropped skill list entry bitmap.
     *
     * @param bitmap A bitmap of a single cropped skill list entry.
     *
     * @return On success, the price as an integer. On failure, NULL.
     */
    fun getSkillListEntryPrice(bitmap: Bitmap? = null, debugString: String = ""): Int? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        val bbox = BoundingBox(
            x = (bitmap.width * 0.7935).toInt(),
            y = (bitmap.height * 0.372).toInt(),
            w = (bitmap.width * 0.1068).toInt(),
            h = (bitmap.height * 0.251).toInt(),
        )
        val croppedPrice = game.imageUtils.createSafeBitmap(bitmap, bbox, "bboxPrice_$debugString")
        if (croppedPrice == null) {
            Log.e(TAG, "[SKILLS] getSkillListEntryPrice: createSafeBitmap for croppedPrice returned NULL.")
            return null
        }

        if (game.debugMode) {
            game.imageUtils.saveBitmap(croppedPrice, filename = "bboxPrice_$debugString")
        }

        val price: Int? = extractText(croppedPrice).replace("[^0-9]".toRegex(), "").toIntOrNull()
        if (price == null) {
            Log.e(TAG, "[SKILLS] getSkillListEntryPrice: Failed to extract skill price.")
            return null
        }

        return price
    }

    /** Extracts all useful information from a single entry in the skill list.
     *
     * @param bitmap A bitmap of a single cropped skill list entry.
     * @param bIsObtained Whether this entry has the Obtained pill icon.
     * If not obtained, then this entry will have the skill up button.
     * This is important because it determines how we detect the skill's price.
     * @param debugString A string used in logging and saved bitmap filenames.
     *
     * @return On success, a SkillListEntry with the extracted info. Otherwise, NULL.
     */
    fun analyzeSkillListEntry(
        bitmap: Bitmap,
        bIsObtained: Boolean,
        debugString: String = "",
    ): SkillListEntry? {
        val latch = CountDownLatch(2)
        var skillPrice: Int? = null
        var skillName: String? = null

        // TITLE
        Thread {
            try {
                // Extract the skill name from the bitmap.
                val tmpSkillName: String? = getSkillListEntryTitle(bitmap, debugString)
                if (tmpSkillName == null) {
                    Log.e(TAG, "[SKILLS] getSkillListEntryTitle() returned NULL.")
                    return@Thread
                }
                skillName = game.skillDatabase.checkSkillName(tmpSkillName, fuzzySearch = true)
            } catch (e: Exception) {
                Log.e(TAG, "[ERROR] Error processing skill name: ${e.stackTraceToString()}")
            } finally {
                latch.countDown()
            }
        }.apply { isDaemon = true }.start()

        // PRICE
        Thread {
            try {
                // If the skill is already obtained, don't bother trying to get the price.
                val tmpSkillPrice: Int? = if (bIsObtained) 0 else getSkillListEntryPrice(bitmap, debugString)
                if (tmpSkillPrice == null) {
                    Log.e(TAG, "[SKILLS] getSkillListEntryPrice() returned NULL.")
                    return@Thread
                }
                skillPrice = tmpSkillPrice
            } catch (e: Exception) {
                Log.e(TAG, "[ERROR] Error processing skill price: ${e.stackTraceToString()}")
            } finally {
                latch.countDown()
            }
        }.apply { isDaemon = true }.start()

        try {
            latch.await(3, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Log.e(TAG, "[ERROR] analyzeSkillListEntry: Parallel analysis timed out.")
        }

        if (skillName == null) {
            MessageLog.e(TAG, "[SKILLS] analyzeSkillListEntry: Failed to parse skillName.")
            return null
        }

        if (skillPrice == null) {
            MessageLog.e(TAG, "[SKILLS] analyzeSkillListEntry: Failed to detect skillPrice.")
            return null
        }

        val entry: SkillListEntry? = entries[skillName]
        if (entry == null) {
            MessageLog.e(TAG, "analyzeSkillListEntry: Failed to find \"$skillName\" in entries.")
            return null
        }

        // Update the entry with our new data.
        entry.bIsObtained = bIsObtained
        entry.bIsVirtual = false
        // Very important we call this otherwise prices wont be accurate.
        entry.updateScreenPrice(skillPrice)

        return entry
    }

    /** Extracts all useful information from a single entry in the skill list in a thread safe manner.
     *
     * This version performs all operations synchronously so that we can
     * use it from inside another thread.
     *
     * @param bitmap A bitmap of a single cropped skill list entry.
     * @param bIsObtained Whether this entry has the Obtained pill icon.
     * If not obtained, then this entry will have the skill up button.
     * This is important because it determines how we detect the skill's price.
     * @param debugString A string used in logging and saved bitmap filenames.
     *
     * @return On success, a SkillListEntry with the extracted info. Otherwise, NULL.
     */
    fun analyzeSkillListEntryThreadSafe(
        bitmap: Bitmap,
        bIsObtained: Boolean,
        debugString: String = "",
    ): SkillListEntry? {
        // Extract the skill name from the bitmap.
        var skillName: String? = getSkillListEntryTitle(bitmap, debugString)
        if (skillName == null) {
            MessageLog.e(TAG, "analyzeSkillListEntryThreadSafe: getSkillListEntryTitle() returned NULL.")
            return null
        }
        skillName = game.skillDatabase.checkSkillName(skillName, fuzzySearch = true)


        // If the skill is already obtained, don't bother trying to get the price.
        val skillPrice: Int? = if (bIsObtained) 0 else getSkillListEntryPrice(bitmap, debugString)
        if (skillPrice == null) {
            MessageLog.e(TAG, "analyzeSkillListEntryThreadSafe: getSkillListEntryPrice() returned NULL.")
            return null
        }

        val entry: SkillListEntry? = entries[skillName]
        if (entry == null) {
            MessageLog.e(TAG, "analyzeSkillListEntryThreadSafe: Failed to find \"$skillName\" in entries.")
            return null
        }

        // Update the entry with our new data.
        entry.bIsObtained = bIsObtained
        entry.bIsVirtual = false
        // Very important we call this otherwise prices wont be accurate.
        entry.updateScreenPrice(skillPrice)

        return entry
    }

    /** Handles ScrollList callback events.
     *
     * @param entry The ScrollListEntry data object that triggered this event.
     *
     * @return A pair containing a SkillListEntry and a Point generated
     * by processing the [entry] detected by the ScrollList.
     * Returns a single NULL if there are any errors processing the entry.
     */
    private fun onScrollListEntry(entry: ScrollListEntry): Pair<SkillListEntry, Point>? {
        val skillUpLoc: Point? = ButtonSkillUp.findImageWithBitmap(
            game.imageUtils,
            sourceBitmap = entry.bitmap,
        )
        val obtainedPillLoc: Point? = IconObtainedPill.findImageWithBitmap(
            game.imageUtils,
            sourceBitmap = entry.bitmap,
        )

        if (skillUpLoc == null && obtainedPillLoc == null) {
            MessageLog.e(TAG, "[SKILLS] onScrollListEntry: Could not find SkillUp or ObtainedPill in bitmap for entry #${entry.index}.")
            if (game.debugMode) {
                game.imageUtils.saveBitmap(entry.bitmap, "SkillList_${entry.index}")
            }
            return null
        }

        val bIsObtained: Boolean = obtainedPillLoc != null
        // This location is relative to the entry.bitmap. We will need to translate
        // it to screen space using the entry.bbox later.
        val localPoint: Point = skillUpLoc ?:
            obtainedPillLoc ?:
            throw IllegalStateException("SkillUp and ObtainedPill locations are null.")

        // Calculate the bounding box for the skill info relative to
        // the location of the detected SkillUp and ObtainedPill icons.
        // x/y positions differ for the SkillUp and ObtainedPill images.
        // This refines our detected entry's bbox by using known
        // distances from the center of the SkillUp/ObtainedPill icons.
        // This helps since the auto entry detection from ScrollList
        // might not be 100% aligned to the entry.
        val bboxSkillBox = if (bIsObtained) {
            BoundingBox(
                x = (localPoint.x - (SharedData.displayWidth * 0.77)).toInt(),
                y = (localPoint.y - (SharedData.displayHeight * 0.0599)).toInt(),
                w = (SharedData.displayWidth * 0.91).toInt(),
                h = (SharedData.displayHeight * 0.12).toInt(),
            )
        } else {
            BoundingBox(
                x = (localPoint.x - (SharedData.displayWidth * 0.86)).toInt(),
                y = (localPoint.y - (SharedData.displayHeight * 0.0583)).toInt(),
                w = (SharedData.displayWidth * 0.91).toInt(),
                h = (SharedData.displayHeight * 0.12).toInt(),
            )
        }

        val croppedSkillBox = game.imageUtils.createSafeBitmap(entry.bitmap, bboxSkillBox, "bboxSkillBox_${entry.index}")
        if (croppedSkillBox == null) {
            MessageLog.e(TAG, "[SKILLS] onScrollListEntry: createSafeBitmap for skillBoxBitmap returned NULL.")
            return null
        }
        if (game.debugMode) {
            game.imageUtils.saveBitmap(croppedSkillBox, filename = "bboxSkillBox_$${entry.index}")
        }

        // Extract all the information from the entry. In this function, the
        // [entries] mapping is updated with the extracted information.
        val skillListEntry: SkillListEntry? = analyzeSkillListEntry(croppedSkillBox, bIsObtained, "${entry.index}")
        if (skillListEntry == null) {
            MessageLog.e(TAG, "[SKILLS] onScrollListEntry: (${entry.index}) SkillListEntry is NULL.")
            return null
        }

        // Finally, translate the localPoint to screen space before we return it.
        val point = Point(
            localPoint.x + entry.bbox.x,
            localPoint.y + entry.bbox.y,
        )

        return Pair(skillListEntry, point)
    }

    /** Gets all entries in the skill list.
     *
     * @param bUseMockData Whether to use fake skill list entry data
     * for debugging purposes.
     * @param onEntry A callback function that is called for each
     * SkillListEntry that we detect. This can be useful if we want to perform
     * an operation immediately upon detecting an entry.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun parseSkillListEntries(
        bUseMockData: Boolean = false,
        onEntry: OnEntryDetectedCallback? = null,
    ): Map<String, SkillListEntry> {
        if (bUseMockData) {
            Log.d(TAG, "\n[SKILLS] Using mock skill list entries.")
            return parseMockSkillListEntries()
        }

        val list: ScrollList? = ScrollList.create(game)
        if (list == null) {
            MessageLog.e(TAG, "Failed to instantiate ScrollList.")
            return emptyMap()
        }

        list.process { _, entry: ScrollListEntry ->
            val res: Pair<SkillListEntry, Point>? = onScrollListEntry(entry)
            if (onEntry != null && res != null) onEntry(this, res.first, res.second) else false
        }

        return entries
    }

    /** Gets skill list entries using mocked data.
     *
     * NOTE: This is just useful for debugging purposes.
     * The data was taken from a real run so these values are all valid.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun parseMockSkillListEntries(): Map<String, SkillListEntry> {
        val mockSkills: Map<String, Int> = mapOf(
            "Warning Shot!" to -1,
            "Triumphant Pulse" to 120,
            "Kyoto Racecourse ○" to 63,
            "Standard Distance ○" to 63,
            //"Standard Distance ×" to 35,
            "Summer Runner ○" to 81,
            "Cloudy Days ○" to 81,
            "Professor of Curvature" to 279,
            "Corner Adept ○" to 117,
            "Swinging Maestro" to 323,
            "Corner Recovery ○" to 170,
            "Straightaway Acceleration" to 119,
            "Calm in a Crowd" to 153,
            "Nimble Navigator" to 135,
            "Homestretch Haste" to 153,
            "Up-Tempo" to 104,
            "Steadfast" to 144,
            "Extra Tank" to 96,
            "Frenzied Pace Chasers" to 104,
            "Medium Straightaways ○" to 60,
            "Keeping the Lead" to 128,
            "Pressure" to 128,
            "Pace Chaser Corners ○" to 91,
            "Straight Descent" to 78,
            "Hydrate" to 144,
            "Late Surger Straightaways ○" to 84,
            "Fighter" to 84,
            "I Can See Right Through You" to 110,
            "Highlander" to 128,
            "Uma Stan" to 160,
            "Ignited Spirit SPD" to 180,
        )

        // Fix skill names in case any have been typed incorrectly.
        val fixedSkills: MutableMap<String, Int> = mutableMapOf()
        for ((name, price) in mockSkills) {
            val fixedName: String? = game.skillDatabase.checkSkillName(name, fuzzySearch = true)
            if (fixedName == null) {
                MessageLog.e(TAG, "parseMockSkillListEntries: \"$name\" not in database.")
                return emptyMap()
            }
            // In case of error, we don't want to give back partial data so
            // we just immediately return an empty list.
            val entry: SkillListEntry? = entries[fixedName]
            if (entry == null) {
                MessageLog.e(TAG, "parseMockSkillListEntries: \"$name\" not in entries.")
                return emptyMap()
            }
            fixedSkills[fixedName] = price
        }

        val result: MutableMap<String, SkillListEntry> = mutableMapOf()
        for ((name, price) in fixedSkills) {
            val entry: SkillListEntry? = entries[name]
            if (entry == null) {
                MessageLog.e(TAG, "parseMockSkillListEntries: \"$name\" not in entries.")
                return emptyMap()
            }
            // Manually update entries with data.
            entry.bIsObtained = price <= 0
            entry.bIsVirtual = false
            // Very important we call this otherwise prices wont be accurate.
            entry.updateScreenPrice(price)
            result[name] = entry
        }

        return result.toMap()
    }

    /** Checks whether we are at a skill list screen.
     *
     * @param bitmap Optional bitmap to use when detecting whether we are at
     * the skill list screen. If not specified, a screenshot will be taken
     * and used instead.
     *
     * @return Whether we are at a skill list screen.
     */
    fun checkSkillListScreen(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        if (
            ButtonSkillListFullStats.check(game.imageUtils, sourceBitmap = bitmap) &&
            LabelSkillListScreenSkillPoints.check(game.imageUtils, sourceBitmap = bitmap)
        ) {
            return true
        }

        // Try to handle any skill list specific dialogs that may be up.
        if (!handleDialogs().first) {
            return false
        }

        // Now we can check again if we're at the skill list.
        return (
            ButtonSkillListFullStats.check(game.imageUtils) &&
            LabelSkillListScreenSkillPoints.check(game.imageUtils)
        )
    }

    /** Checks whether we are at the career completion skill list screen.
     *
     * @param bitmap Optional bitmap to use when detecting whether we are at
     * the skill list screen. If not specified, a screenshot will be taken
     * and used instead.
     *
     * @return Whether we are at the career completion skill list screen.
     */
    fun checkCareerCompleteSkillListScreen(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        return (
            !ButtonLog.check(game.imageUtils, sourceBitmap = bitmap) &&
            checkSkillListScreen(bitmap)
        )
    }

    /** Prints all skill list entries to the MessageLog.
     *
     * @param skillListEntries Optional mapping to use when printing.
     * If not specified, then this class instance's [entries] map is used instead.
     * @param verbose Whether to print extra entry information.
     */
    fun printSkillListEntries(
        skillListEntries: Map<String, SkillListEntry>? = null,
        verbose: Boolean = false,
    ) {
        val skillListEntries: Map<String, SkillListEntry> = skillListEntries ?: getAvailableSkills()
        MessageLog.v(TAG, "================= Skill List Entries =================")
        for ((name, entry) in skillListEntries) {
            val entryString: String = if (verbose) {
                "$entry"
            } else {
                val extraString: String = if (entry.bIsVirtual) " (virtual)" else ""
                "${entry.price}${extraString}"
            }
            MessageLog.v(TAG, "\t${name}: $entryString")
        }
        MessageLog.v(TAG, "======================================================")
    }

    /** Purchases a skill.
     *
     * @param name The name of the skill to purchase.
     * @param skillUpButtonLocation The screen location of the SkillUpButton.
     *
     * @return The SkillListEntry for the passed [name] if it was found.
     * If no matching [name] exists, then NULL is returned.
     */
    fun buySkill(name: String, skillUpButtonLocation: Point): SkillListEntry? {
        val entry: SkillListEntry? = entries[name]
        if (entry == null) {
            MessageLog.w(TAG, "buySkill: \"$name\" not found.")
            return null
        }

        if (entry.screenPrice > skillPoints) {
            MessageLog.w(TAG, "buySkill: Not enough skill points (${skillPoints}pt) to buy \"$name\" (${entry.screenPrice}pt).")
            return null
        }

        entry.buy(skillUpButtonLocation)
        skillPoints -= entry.screenPrice

        return entry
    }

    /** Resets all skills back to their original states prior to purchasing. */
    fun sellAllSkills() {
        for ((name, entry) in getObtainedSkills()) {
            entry.sell()
        }
    }

    /** Returns all skill list entries.
     *
     * NOTE: This returns ALL entries, including ones that do not exist
     * in the actual skill list.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getAllSkills(): Map<String, SkillListEntry> {
        return entries
    }

    /** Returns skills that actually exist in the skill list (not virtual).
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getAvailableSkills(): Map<String, SkillListEntry> {
        return entries.filterValues { it.bIsAvailable }
    }

    /** Returns skills that do not exist in the skill list.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getVirtualSkills(): Map<String, SkillListEntry> {
        return getUnobtainedSkills().filterValues { it.bIsVirtual }
    }

    /** Returns all skills that have been purchased.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getObtainedSkills(): Map<String, SkillListEntry> {
        return getAllSkills().filterValues { it.bIsObtained }
    }

    /** Returns all skills that have not been purchased.
     *
     * @param includeVirtual Whether to include virtual skills in the result.
     * By default, only available skills will be included.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getUnobtainedSkills(includeVirtual: Boolean = false): Map<String, SkillListEntry> {
        val src: Map<String, SkillListEntry> = if (includeVirtual) getAllSkills() else getAvailableSkills()
        return src.filterValues { !it.bIsObtained }
    }

    /** Returns all negative skills (purple skills).
     *
     * @param includeVirtual Whether to include virtual skills in the result.
     * By default, only available skills will be included.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getNegativeSkills(includeVirtual: Boolean = false): Map<String, SkillListEntry> {
        val src: Map<String, SkillListEntry> = if (includeVirtual) getAllSkills() else getAvailableSkills()
        return src.filterValues { it.bIsNegative }
    }

    /** Returns all inherited unique skills.
     *
     * @param includeVirtual Whether to include virtual skills in the result.
     * By default, only available skills will be included.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getInheritedUniqueSkills(includeVirtual: Boolean = false): Map<String, SkillListEntry> {
        val src: Map<String, SkillListEntry> = if (includeVirtual) getAllSkills() else getAvailableSkills()
        return src.filterValues { it.bIsInheritedUnique }
    }

    /** Returns all skills that are not affected by a trainee's aptitudes.
     *
     * These skills are dependent on a running style, track distance, or track surface.
     * For example, the skill [Front Runner Savvy ○] specifies in its description
     * that it is for "(Front Runner)".
     *
     * This function also filters by skills whose running style is inferred and not
     * explicitly stated. See [getInferredRunningStyleSkills] for more details.
     *
     * @param runningStyle The optional RunningStyle to use when filtering the
     * inferred running styles. If not specified, then skills with ANY
     * inferred running styles are included in the results.
     * @param includeVirtual Whether to include virtual skills in the result.
     * By default, only available skills will be included.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getAptitudeIndependentSkills(
        runningStyle: RunningStyle? = null,
        includeVirtual: Boolean = false,
    ): Map<String, SkillListEntry> {
        val src: Map<String, SkillListEntry> = if (includeVirtual) getAllSkills() else getAvailableSkills()
        val inferredRunningStyleSkills: Map<String, SkillListEntry> = getInferredRunningStyleSkills(runningStyle, includeVirtual)
        return src.filterValues {
            it.runningStyle == null &&
            it.trackDistance == null &&
            it.trackSurface == null &&
            it.name !in inferredRunningStyleSkills
        }
    }

    /** Returns all skills for a RunningStyle.
     *
     * @param runningStyle The optional RunningStyle to use when filtering.
     * If not specified, then skills with ANY running style will be returned.
     * @param includeVirtual Whether to include virtual skills in the result.
     * By default, only available skills will be included.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getRunningStyleSkills(
        runningStyle: RunningStyle? = null,
        includeVirtual: Boolean = false,
    ): Map<String, SkillListEntry> {
        val src: Map<String, SkillListEntry> = if (includeVirtual) getAllSkills() else getAvailableSkills()
        // If null, then we want to return all skills that have any running style.
        if (runningStyle == null) {
            return src.filterValues { it.runningStyle != null }
        }
        return src.filterValues { it.runningStyle == runningStyle }
    }

    /** Returns all skills for a TrackDistance.
     *
     * @param trackDistance The optional TrackDistance to use when filtering.
     * If not specified, then skills with ANY track distance will be returned.
     * @param includeVirtual Whether to include virtual skills in the result.
     * By default, only available skills will be included.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getTrackDistanceSkills(
        trackDistance: TrackDistance? = null,
        includeVirtual: Boolean = false,
    ): Map<String, SkillListEntry> {
        val src: Map<String, SkillListEntry> = if (includeVirtual) getAllSkills() else getAvailableSkills()
        // If null, then we want to return all skills that have any track distance.
        if (trackDistance == null) {
            return src.filterValues { it.trackDistance != null }
        }
        return src.filterValues { it.trackDistance == trackDistance }
    }

    /** Returns all skills for a TrackSurface.
     *
     * @param trackSurface The optional TrackSurface to use when filtering.
     * If not specified, then skills with ANY track surface will be returned.
     * @param includeVirtual Whether to include virtual skills in the result.
     * By default, only available skills will be included.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getTrackSurfaceSkills(
        trackSurface: TrackSurface? = null,
        includeVirtual: Boolean = false,
    ): Map<String, SkillListEntry> {
        val src: Map<String, SkillListEntry> = if (includeVirtual) getAllSkills() else getAvailableSkills()
        // If null, then we want to return all skills that have any track surface.
        if (trackSurface == null) {
            return src.filterValues { it.trackSurface != null }
        }
        return src.filterValues { it.trackSurface == trackSurface }
    }

    /** Returns all skills that have an inferred RunningStyle.
     *
     * Unlike some skills which can only activate if a specific RunningStyle
     * is selected, inferred running style skills are skills which can be
     * activated when using ANY RunningStyle but require the trainee to be
     * in a specific positioning during the race. We refer to these kinds of
     * skills as "inferred running style skills". ...or at least I do.
     *
     * For example, some skills only activate if the trainee is in the lead or
     * well-positioned. These skills are typically only useful for specific
     * running styles however they technically CAN be activated by any style
     * under hyper-specific circumstances. We don't want to rely on ultra rare cases
     * so we only include inferred styles that match the passed runningStyle.
     *
     * @param runningStyle The optional RunningStyle to use when filtering.
     * If not specified, then skills with ANY running style will be returned.
     * @param includeVirtual Whether to include virtual skills in the result.
     * By default, only available skills will be included.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getInferredRunningStyleSkills(
        runningStyle: RunningStyle? = null,
        includeVirtual: Boolean = false,
    ): Map<String, SkillListEntry> {
        val src: Map<String, SkillListEntry> = if (includeVirtual) getAllSkills() else getAvailableSkills()

        // Get normal running style skills so we can filter them out later.
        val runningStyleSkills: Map<String, SkillListEntry> = getRunningStyleSkills(runningStyle, includeVirtual)

        // If null, then we want to return all skills that have any inferred running style.
        if (runningStyle == null) {
            return src
                .filterValues { it.inferredRunningStyles.isNotEmpty() }
                .filterKeys { it !in runningStyleSkills }
        }
        return src
            .filterValues { runningStyle in it.inferredRunningStyles }
            .filterKeys { it !in runningStyleSkills }
    }

    /** Gets all available skills their virtual upgrades.
     *
     * This effectively only adds virtual skills that have an in-place
     * downgrade in the available skill results.
     *
     * @return A mapping of skill names to SkillListEntry objects.
     */
    fun getAvailableSkillsWithVirtualUpgrades(): Map<String, SkillListEntry> {
        val result: Map<String, SkillListEntry> = getAvailableSkills().toMap()
        val entriesToAdd: MutableMap<String, SkillListEntry> = mutableMapOf()
        for ((name, entry) in result) {
            val upgrades: List<SkillListEntry> = entry.getUpgrades()
            for (upgrade in upgrades) {
                entriesToAdd[upgrade.name] = upgrade
            }
        }
        return result + entriesToAdd.toMap()
    }

    /** Returns a SkillListEntry for a given skill name.
     *
     * @param name The name to look up.
     *
     * @return If the [name] is found, its SkillListEntry is returned. Otherwise, NULL.
     */
    fun getEntry(name: String): SkillListEntry? {
        val result: SkillListEntry? = entries[name]
        if (result == null) {
            MessageLog.w(TAG, "getEntry: No entry found for \"$name\".")
        }
        return result
    }
}
