package com.steve1316.uma_android_automation.utils

import android.graphics.Bitmap
import org.opencv.core.Point

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game

import com.steve1316.automation_library.data.SharedData
import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.utils.types.BoundingBox

import com.steve1316.uma_android_automation.components.*

const val MAX_PROCESS_TIME_DEFAULT_MS = 60000

/** Callback that is called whenever an entry is detected while processing the list.
 *
 * @param ScrollList A reference to this class instance.
 * @param Int The index of this entry in the list.
 * @param Point The location of the detected entry's component.
 * This is the location of the component that is specified when calling
 * [ScrollList.process] in the [entryComponent] parameter.
 * @param Bitmap The current bitmap.
 *
 * @return Whether the [ScrollList.process] function should exit early.
 * For example, if we just want to search for a specific entry in the list
 * and we don't want to do anything after finding it, then we can return
 * True and the loop will stop as soon as we find the entry.
 */
typealias OnEntryDetectedCallback = (scrollList: ScrollList, entry: ScrollListEntry) -> Boolean

/** Stores a single entry's information in the scroll list.
 *
 * @param index The index of this entry in the list.
 * @param bitmap A single entry's bitmap, extracted from the screen.
 * @param bbox The bounding box for the [bitmap], in screen coordinates.
 */
data class ScrollListEntry(
    val index: Int,
    val bitmap: Bitmap,
    val bbox: BoundingBox,
)

/** Stores configuration for entry image detection.
 *
 * See [CustomImageUtils.detectRoundedRectangles] or
 * [CustomImageUtils.detectRectanglesGeneric] for more information.
 */
data class ScrollListEntryDetectionConfig (
    val bUseGeneric: Boolean = true,
    // The area parameters can be updated later to fit the scroll list's dims.
    var minArea: Int? = null,
    var maxArea: Int? = null,
    val blurSize: Int = if (bUseGeneric) 7 else 5,
    val epsilonScalar: Double = 0.02,
    // CustomImageUtils.detectRoundedRectangles params.
    val cannyLowerThreshold: Int = 30,
    val cannyUpperThreshold: Int = 50,
    val bUseAdaptiveThreshold: Boolean = true,
    val adaptiveThresholdBlockSize: Int = 11,
    val adaptiveThresholdConstant: Double = 2.0,
    // CustomImageUtils.detectRoundedRectangles params.
    val fillSeedPoint: Point = Point(10.0, 10.0),
    val fillLoDiffValue: Int = 1,
    val fillUpDiffValue: Int = 1,
    val morphKernelSize: Int = 100,
)

/** Handles parsing entries in a scrollable list.
 *
 * Example:
 *
 * val list: ScrollList? = ScrollList.create(game)
 * if (list == null) throw InvalidStateException()
 * scrollList.process() { scrollList: ScrollList, entry: ScrollListEntry ->
 *      imageUtils.saveBitmap(entry.bitmap, "entry_${entry.index}")
 *      // Return true to stop the scrollList loop if we've read 5 entries.
 *      entry.index > 5
 * }
 *
 * @param game Reference to the bot's Game instance.
 * @param bboxList The bounding region of the full list.
 * @param bboxEntries The refined [bboxList] with a buffer on the top and bottom
 * to prevent partial entries.
 * @param entryDetectionConfig The configuration for image detection.
 */
class ScrollList private constructor(
    private val game: Game,
    private val bboxList: BoundingBox,
    entryDetectionConfig: ScrollListEntryDetectionConfig,
) {
    // These are safe values for entry heights. We use these to calculate area limits
    // if no config is passed.
    private val defaultMinEntryHeight: Int = game.imageUtils.relHeight((SharedData.displayHeight * 0.0781).toInt()) // 150px on 1920h
    private val defaultMaxEntryHeight: Int = game.imageUtils.relHeight((SharedData.displayHeight * 0.1302).toInt()) // 250px on 1920h

    private val entryDetectionConfig = ScrollListEntryDetectionConfig(
        bUseGeneric = entryDetectionConfig.bUseGeneric,
        minArea = entryDetectionConfig.minArea ?: defaultMinEntryHeight * (bboxList.w.toDouble() * 0.7).toInt(),
        maxArea = entryDetectionConfig.maxArea ?: defaultMaxEntryHeight * bboxList.w,
        blurSize = entryDetectionConfig.blurSize,
        epsilonScalar = entryDetectionConfig.epsilonScalar,
        // detectRoundedRectangles params
        cannyLowerThreshold = entryDetectionConfig.cannyLowerThreshold,
        cannyUpperThreshold = entryDetectionConfig.cannyUpperThreshold,
        bUseAdaptiveThreshold = entryDetectionConfig.bUseAdaptiveThreshold,
        adaptiveThresholdBlockSize = entryDetectionConfig.adaptiveThresholdBlockSize,
        adaptiveThresholdConstant = entryDetectionConfig.adaptiveThresholdConstant,
        // detectRectanglesGeneric params
        fillSeedPoint = entryDetectionConfig.fillSeedPoint,
        fillLoDiffValue = entryDetectionConfig.fillLoDiffValue,
        fillUpDiffValue = entryDetectionConfig.fillUpDiffValue,
        morphKernelSize = entryDetectionConfig.morphKernelSize,
    )

    // Create a small padding within the bboxList. This is where the list entries
    // will reside. This prevents us from accidentally clicking outside of the
    // list.
    private val listPadding: Int = 5
    private val bboxEntries: BoundingBox = BoundingBox(
        x = bboxList.x + listPadding,
        y = bboxList.y + listPadding,
        w = bboxList.w - (listPadding * 2),
        h = bboxList.h - (listPadding * 2),
    )

    companion object {
        private val TAG: String = "[${MainActivity.loggerTag}]ScrollList"

        /** Creates a new ScrollList instance.
         *
         * @param game Reference to the bot's Game instance.
         * @param listTopLeftComponent An image component used to detect the top
         * left corner of the list.
         * @param listBottomRightComponent An image component used to detect the
         * bottom right corner of the list.
         * @param entryDetectionConfig Optional image detection configuration.
         *
         * @return On success, the ScrollList instance. Otherwise, NULL.
         */
        fun create(
            game: Game,
            bitmap: Bitmap? = null,
            listTopLeftComponent: ComponentInterface? = null,
            listBottomRightComponent: ComponentInterface? = null,
            entryDetectionConfig: ScrollListEntryDetectionConfig? = null,
        ): ScrollList? {
            val bboxList: BoundingBox? = getListBoundingRegion(
                game,
                bitmap,
                listTopLeftComponent,
                listBottomRightComponent,
            )
            if (bboxList == null) {
                return null
            }

            return ScrollList(game, bboxList, entryDetectionConfig ?: ScrollListEntryDetectionConfig())
        }

        /** Gets the bounding region for the list on the screen.
         *
         * @param game Reference to the bot's Game instance.
         * @param bitmap Optional bitmap used for detecting list bounding region.
         * If not specified, a screenshot will be taken and used instead.
         * NOTE: This parameter must be specified in thread-safe contexts.
         * @param listTopLeftComponent The Component used to detect the top left
         * corner of the list. Defaults to IconScrollListTopLeft.
         * @param listBottomRightComponent The Component used to detect the bottom
         * right corner of the list. Defaults to IconScrollListBottomRight.
         *
         * @return On success, the bounding region. On failure, NULL.
         */
        private fun getListBoundingRegion(
            game: Game,
            bitmap: Bitmap? = null,
            listTopLeftComponent: ComponentInterface? = null,
            listBottomRightComponent: ComponentInterface? = null,
            debugString: String = "",
        ): BoundingBox? {
            val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

            val listTopLeftComponent: ComponentInterface = listTopLeftComponent ?: IconScrollListTopLeft
            val listBottomRightComponent: ComponentInterface = listBottomRightComponent ?: IconScrollListBottomRight

            val listTopLeftBitmap: Bitmap? = listTopLeftComponent.template.getBitmap(game.imageUtils)
            if (listTopLeftBitmap == null) {
                MessageLog.e(TAG, "[SCROLL_LIST] Failed to load bitmap: ${listTopLeftComponent.template.path} ")
                return null
            }

            val listBottomRightBitmap: Bitmap? = listBottomRightComponent.template.getBitmap(game.imageUtils)
            if (listBottomRightBitmap == null) {
                MessageLog.e(TAG, "[SCROLL_LIST] Failed to load bitmap: ${listBottomRightComponent.template.path}")
                return null
            }

            val listTopLeft: Point? = listTopLeftComponent.findImageWithBitmap(game.imageUtils, bitmap)
            if (listTopLeft == null) {
                MessageLog.e(TAG, "[SCROLL_LIST] Failed to find top left corner of race list.")
                return null
            }
            val listBottomRight: Point? = listBottomRightComponent.findImageWithBitmap(game.imageUtils, bitmap)
            if (listBottomRight == null) {
                MessageLog.e(TAG, "[SCROLL_LIST] Failed to find bottom right corner of race list.")
                return null
            }
            val x0 = (listTopLeft.x - (listTopLeftBitmap.width / 2)).toInt()
            val y0 = (listTopLeft.y - (listTopLeftBitmap.height / 2)).toInt()
            val x1 = (listBottomRight.x + (listBottomRightBitmap.width / 2)).toInt()
            val y1 = (listBottomRight.y + (listBottomRightBitmap.height / 2)).toInt()
            val bbox = BoundingBox(
                x = x0,
                y = y0,
                w = x1 - x0,
                h = y1 - y0,
            )

            if (bbox.w <= 0 || bbox.h <= 0) {
                MessageLog.e(TAG, "[SCROLL_LIST] Invalid bounding box: $bbox")
                return null
            }

            if (game.debugMode) {
                game.imageUtils.saveBitmap(bitmap, "getListBoundingRegion_$debugString", bbox)
            }

            return bbox
        }
    }

    /** Detects locations of each entry in the visible portion of the list.
     *
     * @param bitmap An optional bitmap to use when detecting entries.
     * If not specified, a screenshot will be taken.
     *
     * @return A list of bounding boxes for each entry that we detected.
     */
    private fun detectEntries(bitmap: Bitmap? = null): List<BoundingBox> {
        // Extract a list of bounding boxes for each entry in the list.
        val rects: List<BoundingBox> = if (entryDetectionConfig.bUseGeneric) {
            game.imageUtils.detectRectanglesGeneric(
                bitmap = bitmap,
                region = bboxList,
                minArea = entryDetectionConfig.minArea,
                maxArea = entryDetectionConfig.maxArea,
                blurSize = entryDetectionConfig.blurSize,
                epsilonScalar = entryDetectionConfig.epsilonScalar,
                fillSeedPoint = entryDetectionConfig.fillSeedPoint,
                fillLoDiffValue = entryDetectionConfig.fillLoDiffValue,
                fillUpDiffValue = entryDetectionConfig.fillUpDiffValue,
                morphKernelSize = entryDetectionConfig.morphKernelSize,
            )
        } else {
            game.imageUtils.detectRoundedRectangles(
                bitmap = bitmap,
                region = bboxList,
                minArea = entryDetectionConfig.minArea,
                maxArea = entryDetectionConfig.maxArea,
                blurSize = entryDetectionConfig.blurSize,
                epsilonScalar = entryDetectionConfig.epsilonScalar,
                cannyLowerThreshold = entryDetectionConfig.cannyLowerThreshold,
                cannyUpperThreshold = entryDetectionConfig.cannyUpperThreshold,
                bUseAdaptiveThreshold = entryDetectionConfig.bUseAdaptiveThreshold,
                adaptiveThresholdBlockSize = entryDetectionConfig.adaptiveThresholdBlockSize,
                adaptiveThresholdConstant = entryDetectionConfig.adaptiveThresholdConstant,
            )
        }
        

        // Need to adjust the coordinates of each BoundingBox to be in screen
        // coordinates instead of being relative to [bboxList].
        val result: MutableList<BoundingBox> = rects.map {
            BoundingBox(
                x = it.x + bboxList.x,
                y = it.y + bboxList.y,
                w = it.w,
                h = it.h,
            )
        }.toMutableList()

        // Sort by screen position top to bottom.
        result.sortBy { it.y }

        return result.toList()
    }

    /** Gets the bounding region of the scroll bar on screen.
     *
     * @param bitmap Optional bitmap used for debugging.
     * @param bboxSkillList The bounding region of the list on the screen.
     * @param debugString An optional string to use for debugging purposes.
     *
     * @return On success, the bounding region. On failure, NULL.
     */
    private fun getListScrollBarBoundingRegion(
        bitmap: Bitmap? = null,
        bboxList: BoundingBox,
        debugString: String = "",
    ): BoundingBox? {
        val bboxScrollBar = BoundingBox(
            x = game.imageUtils.relX((bboxList.x + bboxList.w).toDouble(), -22),
            y = bboxList.y,
            w = 10,
            h = bboxList.h,
        )

        // The center column of pixels in the scrollbar.
        // This allows us to perform faster analysis on the scrollbar.
        val bboxScrollBarSingleColumn = BoundingBox(
            x = bboxScrollBar.x + (bboxScrollBar.w / 2),
            y = bboxScrollBar.y,
            w = 1,
            h = bboxScrollBar.h,
        )

        if (game.debugMode) {
            val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
            game.imageUtils.saveBitmap(bitmap, "bboxScrollBar_$debugString", bboxScrollBar)
            game.imageUtils.saveBitmap(bitmap, "bboxScrollBarSingleColumn_$debugString", bboxScrollBarSingleColumn)
        }

        return bboxScrollBarSingleColumn
    }

    /** Stops overscrolling of the list by clicking on screen.
     *
     * When scrolling the list, upon releasing the swipe gesture,
     * the list will continue scrolling a bit. For OCR to work properly,
     * we need the list to remain stationary ASAP.
     *
     * To prevent this overscrolling behavior, we simply click at a safe
     * location in the list in order to immediately stop the list's
     * scrolling animation.
     *
     * This location is randomized to help avoid bot detection
     * (if they even have any bot detection at all...)
     *
     * @param bbox Optional safe region for clicks that won't click any buttons.
     */
    private fun stopScrolling(bboxSafeZone: BoundingBox? = null) {
        val bboxSafeZone: BoundingBox = bboxSafeZone ?: BoundingBox(
            x = bboxEntries.x,
            y = bboxEntries.y,
            w = 1,
            h = bboxEntries.h,
        )
        // Define the bounding region for the tap.
        val x0: Int = game.imageUtils.relX(bboxSafeZone.x.toDouble(), 0)
        val x1: Int = game.imageUtils.relX(bboxSafeZone.x.toDouble(), bboxSafeZone.w)
        val y0: Int = game.imageUtils.relY(bboxSafeZone.y.toDouble(), 0)
        val y1: Int = game.imageUtils.relY(bboxSafeZone.y.toDouble(), bboxSafeZone.h)

        // Now select a random point within this region to click.
        val x: Double = (x0..x1).random().toDouble()
        val y: Double = (y0..y1).random().toDouble()

        // Tap to prevent overscrolling.
        game.tap(x, y, taps = 1, ignoreWaiting = true)
        // Small delay to allow list to stabilize and for click animation
        // to disappear before we try reading it.
        game.wait(0.2, skipWaitingForLoading = true)
    }

    /** Scrolls to the top of the list. */
    private fun scrollToTop() {
        game.gestureUtils.swipe(
            (bboxList.x + (bboxList.w / 2)).toFloat(),
            (bboxList.y + (bboxList.h / 2)).toFloat(),
            (bboxList.x + (bboxList.w / 2)).toFloat(),
            // high value here ensures we go all the way to top of list
            (bboxList.y + (bboxList.h * 1000)).toFloat(),
        )
        stopScrolling()
        // Small delay for list to stabilize.
        game.wait(1.0, skipWaitingForLoading = true)
    }

    /** Scrolls to the top of the list. */
    private fun scrollToBottom() {
        game.gestureUtils.swipe(
            (bboxList.x + (bboxList.w / 2)).toFloat(),
            (bboxList.y + (bboxList.h / 2)).toFloat(),
            (bboxList.x + (bboxList.w / 2)).toFloat(),
            // high value here ensures we go all the way to bottom of list
            (bboxList.y - (bboxList.h * 1000)).toFloat(),
        )
        stopScrolling()
        // Small delay for list to stabilize.
        game.wait(1.0, skipWaitingForLoading = true)
    }

    /** Scrolls down in the list.
     *
     * @param startLoc An optional starting location to swipe from.
     * If not specified, then the swipe starts from the center of the list.
     * @param entryHeight Optional height of an entry in the list. This is used
     * to determine how far to scroll. If not specified, we just scroll
     * to the bboxList bounds.
     */
    private fun scrollDown(startLoc: Point? = null, entryHeight: Int = 0) {
        val x0: Int = (startLoc?.x ?: bboxList.x + (bboxList.w / 2)).toInt()
        val y0: Int = (startLoc?.y ?: bboxList.y + (bboxList.h / 2)).toInt()
        game.gestureUtils.swipe(
            x0.toFloat(),
            y0.toFloat(),
            x0.toFloat(),
            // Add some extra height since scrolling isn't accurate.
            (bboxList.y - (entryHeight * 1.5)).toFloat(),
            duration=1000,
        )
        stopScrolling()
    }

    /** Scrolls up in the list.
     *
     * @param startLoc An optional starting location to swipe from.
     * If not specified, then the swipe starts from the center of the list.
     * @param entryHeight Optional height of an entry in the list. This is used
     * to determine how far to scroll. If not specified, we just scroll
     * to the bboxList bounds.
     */
    private fun scrollUp(startLoc: Point? = null, entryHeight: Int = 0) {
        val x0: Int = (startLoc?.x ?: bboxList.x + (bboxList.w / 2)).toInt()
        val y0: Int = (startLoc?.y ?: bboxList.y + (bboxList.h / 2)).toInt()
        game.gestureUtils.swipe(
            x0.toFloat(),
            y0.toFloat(),
            x0.toFloat(),
            // Add some extra height since scrolling isn't accurate.
            (bboxList.y + bboxList.h + (entryHeight * 1.5)).toFloat(),
            duration=1000,
        )
        stopScrolling()
    }

    /** Scrolls through a list and fires a callback on each entry. 
     *
     * @param maxTimeMs The maximum runtime for this process before it times out.
     * @param onEntry A callback function that is called for each detected entry.
     * This callback can return TRUE to force this loop to exit early; before
     * finishing the list. See [OnEntryDetectedCallback] for more info.
     *
     * @return Whether the operation was successful.
     */
    fun process(
        //entryComponents: List<ComponentInterface>,
        maxTimeMs: Int = MAX_PROCESS_TIME_DEFAULT_MS,
        onEntry: OnEntryDetectedCallback,
    ): Boolean {
        var bitmap = game.imageUtils.getSourceBitmap()

        val bboxScrollBar: BoundingBox? = getListScrollBarBoundingRegion(bitmap, bboxList)
        if (bboxScrollBar == null) {
            MessageLog.e(TAG, "process: getListScrollBarBoundingRegion() returned NULL.")
            return false
        }

        scrollToTop()

        // Max time limit for the while loop to scroll through the list.
        val startTime: Long = System.currentTimeMillis()
        val maxTimeMs: Long = 60000
        var prevScrollBarBitmap: Bitmap? = null

        var index: Int = 0

        // Stores all bboxes. Used to calculate average entry height.
        val entryBboxes: MutableList<BoundingBox> = mutableListOf()

        val prevBitmaps: MutableList<Bitmap> = mutableListOf()

        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            bitmap = game.imageUtils.getSourceBitmap()

            // SCROLLBAR CHANGE DETECTION LOGIC
            val scrollBarBitmap: Bitmap? = game.imageUtils.createSafeBitmap(
                bitmap,
                bboxScrollBar,
                "bboxScrollBar",
            )
            if (scrollBarBitmap == null) {
                MessageLog.e(TAG, "ScrollList.process: createSafeBitmap for scrollbar returned NULL.")
                return false
            }

            // If the scrollbar hasn't changed after scrolling,
            // that means we've reached the end of the list.
            if (prevScrollBarBitmap != null && scrollBarBitmap.sameAs(prevScrollBarBitmap)) {
                return true
            }

            prevScrollBarBitmap = scrollBarBitmap

            val bboxes: List<BoundingBox> = detectEntries(bitmap)
            for (bbox in bboxes) {
                val cropped: Bitmap? = game.imageUtils.createSafeBitmap(
                    bitmap,
                    bbox,
                    "ScrollList.process: cropped entry",
                )

                if (cropped == null) {
                    MessageLog.e(TAG, "Failed to create cropped bitmap for entry $index at $bbox.")
                    return false
                }

                if (onEntry(this, ScrollListEntry(index++, cropped, bbox))) {
                    MessageLog.d(TAG, "onEntry callback returned TRUE for entry $index. Exiting loop.")
                    return true
                }
                prevBitmaps.add(cropped)
            }

            prevBitmaps.clear()

            entryBboxes.addAll(bboxes)
            val avgEntryHeight: Int = entryBboxes.map { it.h }.average().toInt()
            val scrollStartLoc: Point? = if (bboxes.isEmpty()) null else Point(bboxEntries.x.toDouble(), bboxes.last().y.toDouble())
            scrollDown(startLoc = scrollStartLoc, entryHeight = avgEntryHeight)

            // Slight delay to allow screen to settle before next iteration.
            game.wait(0.5, skipWaitingForLoading = true)
        }

        MessageLog.e(TAG, "ScrollList.process: Timed out.")
        return false
    }
}
