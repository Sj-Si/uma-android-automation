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
typealias OnEntryDetectedCallback = (ScrollList, Int, ComponentInterface, Point, Bitmap) -> Boolean

/**
 *
 * @param game Reference to the bot's Game instance.
 * @param bboxList The bounding region of the full list.
 * @param bboxEntries The refined [bboxList] with a buffer on the top and bottom
 * to prevent partial entries.
 * @param entryHeight The estimated height of a single entry in the list.
 */
class ScrollList private constructor(
    private val game: Game,
    private val bboxList: BoundingBox,
    private val bboxEntries: BoundingBox,
    private val entryHeight: Int,
) {
    companion object {
        private val TAG: String = "[${MainActivity.loggerTag}]ScrollList"

        /** Creates a new ScrollList instance.
         *
         * @param game Reference to the bot's Game instance.
         * @param entryHeight The estimated height of a single entry in the list.
         * We need this so we can create a buffer region on the top and bottom
         * of the list to avoid detecting partially cut off entries.
         *
         * @return On success, the ScrollList instance. Otherwise, NULL.
         */
        fun create(game: Game, entryHeight: Int, bitmap: Bitmap? = null): ScrollList? {
            val bboxList: BoundingBox? = getListBoundingRegion(game, bitmap)
            if (bboxList == null) {
                return null
            }

            val bboxEntries: BoundingBox = getListEntriesBoundingRegion(bboxList, entryHeight)

            return ScrollList(game, bboxList, bboxEntries, entryHeight)
        }

        /** Gets the bounding region for the list on the screen.
         *
         * @param game Reference to the bot's Game instance.
         * @param bitmap Optional bitmap used for detecting list bounding region.
         * If not specified, a screenshot will be taken and used instead.
         * NOTE: This parameter must be specified in thread-safe contexts.
         *
         * @return On success, the bounding region. On failure, NULL.
         */
        private fun getListBoundingRegion(
            game: Game,
            bitmap: Bitmap? = null,
            debugString: String = "",
        ): BoundingBox? {
            val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

            val listTopLeftBitmap: Bitmap? = IconScrollListTopLeft.template.getBitmap(game.imageUtils)
            if (listTopLeftBitmap == null) {
                MessageLog.e(TAG, "[SCROLL_LIST] Failed to load IconScrollListTopLeft bitmap.")
                return null
            }

            val listBottomRightBitmap: Bitmap? = IconScrollListBottomRight.template.getBitmap(game.imageUtils)
            if (listBottomRightBitmap == null) {
                MessageLog.e(TAG, "[SCROLL_LIST] Failed to load IconScrollListBottomRight bitmap.")
                return null
            }

            val listTopLeft: Point? = IconScrollListTopLeft.findImageWithBitmap(game.imageUtils, bitmap)
            if (listTopLeft == null) {
                MessageLog.e(TAG, "[SCROLL_LIST] Failed to find top left corner of race list.")
                return null
            }
            val listBottomRight: Point? = IconScrollListBottomRight.findImageWithBitmap(game.imageUtils, bitmap)
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

            if (game.debugMode) {
                game.imageUtils.saveBitmap(bitmap, "getListBoundingRegion_$debugString", bbox)
            }

            return bbox
        }

        /** Gets the refined bounding region for all entries in the list.
         *
         * This helps prevent us from detecting entries that are partially cut-off
         * at the top and bottom of the list.
         *
         * @param bboxList The full list bounding region that we want to refine.
         * @param entryHeight The height of a single entry in the list. We use this
         * to create a padding region of half an entry's height at the top and bottom
         * of the list.
         */
        private fun getListEntriesBoundingRegion(
            bboxList: BoundingBox,
            entryHeight: Int,
        ): BoundingBox {
            MessageLog.e(TAG, "EntryHeight=$entryHeight")
            return BoundingBox(
                x = bboxList.x,
                y = bboxList.y + (entryHeight / 2),
                w = bboxList.w,
                h = bboxList.h - entryHeight,
            )
        }
    }

    /** Gets the bounding region of the scroll bar on screen.
     *
     * @param bitmap Optional bitmap used for debugging.
     * @param bboxSkillList The bounding region of the list on the screen.
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
            x = bboxList.x,
            y = bboxList.y,
            w = 10,
            h = bboxList.h,
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
        stopScrolling(bboxList)
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
        stopScrolling(bboxList)
        // Small delay for list to stabilize.
        game.wait(1.0, skipWaitingForLoading = true)
    }

    /** Scrolls down in the list.
     *
     * @param startLoc An optional starting location to swipe from.
     * If not specified, then the swipe starts from the center of the list.
     */
    private fun scrollDown(startLoc: Point? = null) {
        val x0: Int = (startLoc?.x ?: bboxList.x + (bboxList.w / 2)).toInt()
        val y0: Int = (startLoc?.y ?: bboxList.y + (bboxList.h / 2)).toInt()
        game.gestureUtils.swipe(
            x0.toFloat(),
            y0.toFloat(),
            x0.toFloat(),
            (bboxList.y - entryHeight).toFloat(),
            duration=1000,
        )
        stopScrolling(bboxList)
    }

    /** Scrolls up in the list.
     *
     * @param startLoc An optional starting location to swipe from.
     * If not specified, then the swipe starts from the center of the list.
     */
    private fun scrollUp(startLoc: Point? = null) {
        val x0: Int = (startLoc?.x ?: bboxList.x + (bboxList.w / 2)).toInt()
        val y0: Int = (startLoc?.y ?: bboxList.y + (bboxList.h / 2)).toInt()
        game.gestureUtils.swipe(
            x0.toFloat(),
            y0.toFloat(),
            x0.toFloat(),
            (bboxList.y + bboxList.h + entryHeight).toFloat(),
            duration=1000,
        )
        stopScrolling(bboxList)
    }

    fun process(
        entryComponents: List<ComponentInterface>,
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

        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            bitmap = game.imageUtils.getSourceBitmap()

            // SCROLLBAR CHANGE DETECTION LOGIC
            val scrollBarBitmap: Bitmap? = game.imageUtils.createSafeBitmap(
                bitmap,
                bboxScrollBar,
                "bboxScrollBar",
            )
            if (scrollBarBitmap == null) {
                MessageLog.e(TAG, "process: createSafeBitmap for scrollbar returned NULL.")
                return false
            }

            // If the scrollbar hasn't changed after scrolling,
            // that means we've reached the end of the list.
            if (prevScrollBarBitmap != null && scrollBarBitmap.sameAs(prevScrollBarBitmap)) {
                return true
            }

            prevScrollBarBitmap = scrollBarBitmap

            val entries: MutableList<Pair<ComponentInterface, Point>> = mutableListOf()
            for (component in entryComponents) {
                val points: List<Point> = component.findAllWithBitmap(
                    game.imageUtils,
                    sourceBitmap = bitmap,
                    region = component.template.region,
                )
                entries.addAll(points.map { Pair<ComponentInterface, Point>(component, it) })
            }
            entries.sortBy { it.second.y }
            entries.retainAll { it.second.y >= bboxEntries.y && it.second.y <= bboxEntries.y + bboxEntries.h }

            for ((index, entry) in entries.withIndex()) {
                val (component, loc) = entry
                val bboxEntry: BoundingBox = BoundingBox(
                    x = bboxEntries.x,
                    y = (loc.y - game.imageUtils.relHeight(entryHeight / 2)).toInt(),
                    w = bboxEntries.w,
                    h = game.imageUtils.relHeight(entryHeight).toInt(),
                )

                val cropped: Bitmap? = game.imageUtils.createSafeBitmap(
                    bitmap,
                    bboxEntry,
                    "ScrollList.process entry_$index",
                )
                if (cropped == null) {
                    MessageLog.e(TAG, "Failed to create cropped bitmap for entry $index.")
                    return false
                }
                if (onEntry(this, index, component, loc, cropped)) {
                    return true
                }
            }

            scrollDown(if (entries.isEmpty()) null else entries.last().second)

            // Slight delay to allow screen to settle before next iteration.
            game.wait(0.5, skipWaitingForLoading = true)
        }

        MessageLog.e(TAG, "process: Timed out.")
        return false
    }
}
