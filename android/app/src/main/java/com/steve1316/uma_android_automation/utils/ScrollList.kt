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

    // An estimate of the scrollbar's location within the list.
    private val bboxScrollBarRegionDefault = BoundingBox(
        x = bboxList.x + (bboxList.w - 35),
        y = bboxList.y + 10,
        w = 35,
        h = bboxList.h - 20,
    )

    // No known scrollbars that are anywhere near this small.
    private val bboxScrollBarMinArea: Int = 100
    private val bboxScrollBarMaxArea: Int = bboxScrollBarRegionDefault.w * bboxScrollBarRegionDefault.h
    // This can be updated if we successfully detect it later.
    private var bboxScrollBar: BoundingBox = getListScrollBarBoundingRegion().first

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
     * @param bitmap Optional bitmap used for detecting scroll bar.
     * Must not be cropped.
     * The only benefit to this is reduced time spent due to not needing to take
     * a new screenshot.
     *
     * @return If we detected a scrollbar on screen, we return the scrollbar
     * and its detected Thumb component as a pair. Otherwise we return the class's
     * [bboxScrollBar] and NULL as a pair.
     */
    private fun getListScrollBarBoundingRegion(
        bitmap: Bitmap? = null,
    ): Pair<BoundingBox, BoundingBox?> {
        val result: Pair<BoundingBox, BoundingBox>? = game.imageUtils.detectScrollBar(
            bitmap = bitmap,
            region = bboxScrollBarRegionDefault,
            minArea = bboxScrollBarMinArea,
            maxArea = bboxScrollBarMaxArea,
        )

        // If we detected a scrollbar, update our class state with the new bbox
        // since the scrollbar will be in the same position for the lifetime of
        // this class instance.
        if (result != null) {
            // Add the original region back to the x/y coords of the result.
            bboxScrollBar = BoundingBox(
                x = bboxScrollBarRegionDefault.x + result.first.x,
                y = bboxScrollBarRegionDefault.y + result.first.y,
                w = result.first.w,
                h = result.first.h,
            )

            val bboxThumb: BoundingBox = BoundingBox(
                x = bboxScrollBarRegionDefault.x + result.second.x,
                y = bboxScrollBarRegionDefault.y + result.second.y,
                w = result.second.w,
                h = result.second.h,
            )

            return Pair(bboxScrollBar, bboxThumb)
        }

        return Pair(bboxScrollBar, null)
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

    /** Scrolls to the top of the list.
     *
     * @param bitmap Optional source bitmap to use when detecting scrollbar.
     * @param bboxThumb Optional BoundingBox for the scrollbar's thumb item.
     * This saves us time having to try and detect it.
     */
    private fun scrollToTop(
        bitmap: Bitmap? = null,
        bboxThumb: BoundingBox? = null,
    ) {
        val bboxThumb: BoundingBox? = bboxThumb ?: getListScrollBarBoundingRegion().second

        if (bboxThumb == null) {
            MessageLog.d(TAG, "scrollToTop: No scrollbar thumb detected. Falling back to lazy scrolling.")
            game.gestureUtils.swipe(
                (bboxList.x + (bboxList.w / 2)).toFloat(),
                (bboxList.y + (bboxList.h / 2)).toFloat(),
                (bboxList.x + (bboxList.w / 2)).toFloat(),
                // High value here ensures we go all the way to top of list.
                // We can't use this method in [scrollToBottom] since negative Y
                // values aren't allowed by gestureUtils.
                (bboxList.y + (bboxList.h * 1000)).toFloat(),
            )
            stopScrolling()
        } else {
            game.gestureUtils.swipe(
                (bboxThumb.x + (bboxThumb.w.toFloat() / 2.0)).toFloat(),
                (bboxThumb.y + (bboxThumb.h.toFloat() / 2.0)).toFloat(),
                (bboxThumb.x + (bboxThumb.w.toFloat() / 2.0)).toFloat(),
                bboxList.y.toFloat(),
                duration = 1500L,
            )
        }

        // Small delay for list to stabilize.
        game.wait(1.0, skipWaitingForLoading = true)
    }

    /** Scrolls to the bottom of the list.
     *
     * @param bitmap Optional source bitmap to use when detecting scrollbar.
     * @param bboxThumb Optional BoundingBox for the scrollbar's thumb item.
     * This saves us time having to try and detect it.
     */
    private fun scrollToBottom(
        bitmap: Bitmap? = null,
        bboxThumb: BoundingBox? = null,
    ) {
        val bboxThumb: BoundingBox? = bboxThumb ?: getListScrollBarBoundingRegion().second

        if (bboxThumb == null) {
            MessageLog.d(TAG, "scrollToBottom: No scrollbar thumb detected. Falling back to lazy scrolling.")
            for (i in 0 until 20) {
                scrollDown(durationMs = 250L)
            }
            stopScrolling()
        } else {
            game.gestureUtils.swipe(
                (bboxThumb.x + (bboxThumb.w.toFloat() / 2.0)).toFloat(),
                (bboxThumb.y + (bboxThumb.h.toFloat() / 2.0)).toFloat(),
                (bboxThumb.x + (bboxThumb.w.toFloat() / 2.0)).toFloat(),
                (bboxList.y + bboxList.h).toFloat(),
                duration = 1500L,
            )
        }

        // Small delay for list to stabilize.
        game.wait(1.0, skipWaitingForLoading = true)
    }

    /** Scrolls to the specified percentage down the list.
     *
     * This function only works if a scrollbar and thumb can be detected.
     * Otherwise it has no way to know how far down the list it is.
     *
     * @param percent The percentage of the list to scroll to.
     *
     * @return Whether the operation was successful.
     * This doesn't say whether we actually scrolled to the requested percent,
     * only that we scrolled some amount. Attempting to validate the percent
     * scrolled would be too slow.
     */
    private fun scrollToPercent(percent: Int): Boolean {
        val percent: Int = percent.coerceIn(0, 100)

        val bboxes: Pair<BoundingBox, BoundingBox?> = getListScrollBarBoundingRegion()
        val bboxBar: BoundingBox = bboxes.first
        val bboxThumb: BoundingBox? = bboxes.second

        if (bboxThumb == null) {
            MessageLog.w(TAG, "scrollToPercent: Failed to detect scrollbar.")
            return false
        }

        val targetY: Int = bboxBar.y + (bboxBar.h.toDouble() * (percent.toDouble() / 100.0)).toInt()

        game.gestureUtils.swipe(
            (bboxThumb.x + (bboxThumb.w.toFloat() / 2.0)).toFloat(),
            (bboxThumb.y + (bboxThumb.h.toFloat() / 2.0)).toFloat(),
            (bboxThumb.x + (bboxThumb.w.toFloat() / 2.0)).toFloat(),
            targetY.toFloat(),
            duration = 1500L,
        )

        // Small delay for list to stabilize.
        game.wait(1.0, skipWaitingForLoading = true)

        return true
    }

    /** Scrolls down in the list.
     *
     * @param startLoc An optional starting location to swipe from.
     * If not specified, then the swipe starts from the center of the list.
     * @param entryHeight Optional height of an entry in the list. This is used
     * to determine how far to scroll. If not specified, we just scroll
     * to the bboxList bounds.
     * @param durationMs How long to spend swiping. Lower values will speed up
     * scrolling but will reduce scrolling precision. Anything below 250 is clamped
     * to 250 since anything lower wouldn't be registered by gestureUtils.
     */
    private fun scrollDown(
        startLoc: Point? = null,
        entryHeight: Int = 0,
        durationMs: Long = 1000L,
    ) {
        val durationMs: Long = durationMs.coerceAtLeast(250L)
        val x0: Int = (startLoc?.x ?: bboxList.x + (bboxList.w / 2)).toInt()
        val y0: Int = (startLoc?.y ?: bboxList.y + (bboxList.h / 2)).toInt()
        // Add some extra height since scrolling isn't accurate.
        val y1: Int = (bboxList.y - (entryHeight * 1.5)).toInt().coerceAtLeast(0)
        game.gestureUtils.swipe(
            x0.toFloat(),
            y0.toFloat(),
            x0.toFloat(),
            y1.toFloat(),
            duration = durationMs,
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
     * @param durationMs How long to spend swiping. Lower values will speed up
     * scrolling but will reduce scrolling precision. Anything below 250 is clamped
     * to 250 since anything lower wouldn't be registered by gestureUtils.
     */
    private fun scrollUp(
        startLoc: Point? = null,
        entryHeight: Int = 0,
        durationMs: Long = 1000L,
    ) {
        val durationMs: Long = durationMs.coerceAtLeast(250L)
        val x0: Int = (startLoc?.x ?: bboxList.x + (bboxList.w / 2)).toInt()
        val y0: Int = (startLoc?.y ?: bboxList.y + (bboxList.h / 2)).toInt()
        // Add some extra height since scrolling isn't accurate.
        val y1: Int = (bboxList.y + bboxList.h + (entryHeight * 1.5)).toInt().coerceAtLeast(0)
        game.gestureUtils.swipe(
            x0.toFloat(),
            y0.toFloat(),
            x0.toFloat(),
            y1.toFloat(),
            duration = durationMs,
        )
        stopScrolling()
    }

    /** Scrolls through a list and fires a callback on each entry. 
     *
     * @param maxTimeMs The maximum runtime for this process before it times out.
     * @param bScrollBottomToTop Whether to process the list in reverse order.
     * @param onEntry A callback function that is called for each detected entry.
     * This callback can return TRUE to force this loop to exit early; before
     * finishing the list. See [OnEntryDetectedCallback] for more info.
     *
     * @return Whether the operation was successful.
     */
    fun process(
        //entryComponents: List<ComponentInterface>,
        maxTimeMs: Int = MAX_PROCESS_TIME_DEFAULT_MS,
        bScrollBottomToTop: Boolean = false,
        onEntry: OnEntryDetectedCallback,
    ): Boolean {
        var bitmap = game.imageUtils.getSourceBitmap()

        if (bScrollBottomToTop) scrollToBottom(bitmap) else scrollToTop(bitmap)

        // Max time limit for the while loop to scroll through the list.
        val startTime: Long = System.currentTimeMillis()
        val maxTimeMs: Long = 60000

        // Stores all bboxes. Used to calculate average entry height.
        val entryBboxes: MutableList<BoundingBox> = mutableListOf()
        // Used as break point.
        var prevBboxScrollBarThumb: BoundingBox? = null

        var index: Int = 0
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            bitmap = game.imageUtils.getSourceBitmap()

            var bboxes: List<BoundingBox> = if (bScrollBottomToTop) {
                detectEntries(bitmap).reversed()
            } else {
                detectEntries(bitmap)
            }

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
            }

            entryBboxes.addAll(bboxes)
            val avgEntryHeight: Int = entryBboxes.map { it.h }.average().toInt()
            val scrollStartLoc: Point? = if (bboxes.isEmpty()) null else Point(bboxEntries.x.toDouble(), bboxes.last().y.toDouble())
            if (bScrollBottomToTop) {
                scrollUp(startLoc = scrollStartLoc, entryHeight = avgEntryHeight)
            } else {
                scrollDown(startLoc = scrollStartLoc, entryHeight = avgEntryHeight)
            }

            // Slight delay to allow screen to settle before next iteration.
            game.wait(0.5, skipWaitingForLoading = true)

            // SCROLLBAR CHANGE DETECTION LOGIC
            // Breaks loop if no change to Y position.
            val bboxScrollBarThumb: BoundingBox? = getListScrollBarBoundingRegion().second
            if (bboxScrollBarThumb == null) {
                MessageLog.d(TAG, "No scroll bar detected. Exiting loop.")
                return false
            }

            // If the scrollbar hasn't changed after scrolling,
            // that means we've reached the end of the list.
            if (prevBboxScrollBarThumb != null &&
                bboxScrollBarThumb != null &&
                bboxScrollBarThumb.y == prevBboxScrollBarThumb.y
            ) {
                MessageLog.d(TAG, "Reached end of scroll list. Exiting loop.")
                return true
            }

            prevBboxScrollBarThumb = bboxScrollBarThumb
        }

        MessageLog.e(TAG, "ScrollList.process: Timed out.")
        return false
    }
}
