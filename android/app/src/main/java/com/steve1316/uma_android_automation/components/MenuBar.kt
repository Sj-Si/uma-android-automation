package com.steve1316.uma_android_automation.components

import android.graphics.Bitmap

import com.steve1316.automation_library.data.SharedData
import com.steve1316.automation_library.utils.TextUtils
import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.utils.CustomImageUtils
import com.steve1316.uma_android_automation.utils.types.BoundingBox

/** Allows interaction with the menu bar at bottom of screen (as seen at home page).
 *
 * The menu bar may not always be on the screen depending on the location in the game.
 * However, after detecting the locations of the buttons once, those locations
 * won't ever change and can be reused.
 *
 * Must be instantiated using the [MenuBar.create] function in the companion object.
 * This is because the menu bar may fail to be detected. See example below:
 *      val menuBar: MenuBar? = MenuBar.create(game)
 */
class MenuBar private constructor(
    private val game: Game,
    private val buttons: Map<MenuBarButtonName, MenuBarButton>,
    private val bbox: BoundingBox,
) {
    enum class MenuBarButtonName {
        ENHANCE,
        STORY,
        HOME,
        RACE,
        SCOUT;

        companion object {
            private val nameMap = entries.associateBy { it.name }
            private val ordinalMap = entries.associateBy { it.ordinal }

            fun fromName(value: String): MenuBarButtonName? = nameMap[value.uppercase()]
            fun fromOrdinal(ordinal: Int): MenuBarButtonName? = ordinalMap[ordinal]
        }
    }

    companion object {
        private val TAG: String = "[${MainActivity.loggerTag}]MenuBar"

        // A mapping of ranges of colors for each button. Used to detect
        // if a button is active.
        private val COLOR_RANGES: Map<MenuBarButtonName, Pair<String, String>> = mapOf(
            // (300, 25, 25) to (345, 100, 100)
            MenuBarButtonName.ENHANCE to Pair("#403040", "#ff0040"),
            // (0, 25, 25) to (50, 100, 100)
            MenuBarButtonName.STORY to Pair("#403030", "#ffd500"),
            // (160, 25, 25) to (240, 100, 100)
            MenuBarButtonName.HOME to Pair("#30403a", "#0000ff"),
            // (60, 25, 25) to (150, 100, 100)
            MenuBarButtonName.RACE to Pair("#404030", "#00ff80"),
            // (270, 25, 25) to (340, 100, 100)
            MenuBarButtonName.SCOUT to Pair("#383040", "#ff0055"),
        )

        /** Stores all useful data for a single button on the menu bar.
         *
         * @param game Reference to the bot's Game instance.
         * @param name This button's name.
         * @param bbox The bounding region of this button's text.
         * was originally detected. This is useful for checking the state of the
         * menu bar in the future.
         */
        data class MenuBarButton(
            val game: Game,
            val name: MenuBarButtonName,
            val bbox: BoundingBox,
        ) {
            /** Clicks the button and validates that it is now active.
             *
             * @param timeoutMs The max time allowed when validating if the button is
             * before returning false.
             *
             * @return Whether the button is now active.
             */
            fun click(timeoutMs: Int = 3000): Boolean {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    game.tap(bbox.cx.toDouble(), bbox.cy.toDouble(), "ok", taps = 1)
                    // Need to delay before checking if button is active since tapping
                    // causes an animation which will cause isActive to always return true.
                    game.wait(0.5)
                    if (isActive()) {
                        // Add a delay for elements to become interactive after
                        // switching tabs. Unsure why nothing is interactive for
                        // such a long time when switching tabs in this game.
                        game.wait(1.0, skipWaitingForLoading = true)
                        return true
                    }
                }
                return false
            }

            /** Checks whether the button is currently active.
             *
             * @param bitmap Optional Bitmap to use in the detection.
             * If not specified, then a screenshot is taken instead.
             *
             * @return Whether the button is active.
             */
            fun isActive(bitmap: Bitmap? = null): Boolean {
                var bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
                // Crop to only use the bottom half of the bbox.
                // This helps since the top half can overlap with the image above it
                // which can cause our color detection to be less accurate.
                val bboxSmall: BoundingBox = BoundingBox(
                    x = bbox.x,
                    y = bbox.cy,
                    w = bbox.w,
                    h = (bbox.h * 0.5).toInt(),
                )
                bitmap = game.imageUtils.createSafeBitmap(bitmap, bboxSmall, "isActive bottom half")!!
                val range: Pair<String, String> = COLOR_RANGES[name]!!
                val colorPerc: Double = game.imageUtils.checkColorRangeInBitmap(
                    bitmap,
                    range.first,
                    range.second,
                    "${name.name}_color",
                )

                /* Optionally check white percentage as well.
                val whitePerc: Double = game.imageUtils.checkColorRangeInBitmap(
                    bitmap,
                    Triple<Double, Double, Double>(0.0, 0.0, 250.0),
                    Triple<Double, Double, Double>(180.0, 10.0, 255.0),
                    "${name.name}_white",
                )
                */

                return colorPerc >= 40.0
            }
        }

        /** Factory function used to create a MenuBar instance.
         *
         * @param game Reference to the bot's Game instance.
         * @param maxAttempts The number of times to attempt to detect the menu
         * bar on screen before we abort the instantiation.
         *
         * @return The MenuBar instance on success, otherwise NULL.
         */
        fun create(game: Game, maxAttempts: Int = 3): MenuBar? {
            // Dimensions calculated from a 1080x1920 screen.
            val bbox: BoundingBox = BoundingBox(
                x = 0,
                y = ((1850.0 / 1920.0) * SharedData.displayHeight.toDouble()).toInt(),
                w = SharedData.displayWidth,
                h = ((50.0 / 1920.0) * SharedData.displayHeight.toDouble()).toInt(),
            )

            val buttons: MutableMap<MenuBarButtonName, MenuBarButton> = mutableMapOf()

            var result: MenuBar? = null
            var numAttempts: Int = 0
            while (result == null && numAttempts < maxAttempts) {
                MessageLog.i(TAG, "Attempting to find MenuBar. Attempt #${numAttempts + 1}.")

                val buttonBitmap: Bitmap = game.imageUtils.createSafeBitmap(
                    game.imageUtils.getSourceBitmap(),
                    bbox,
                    "MenuBar create cropped",
                )!!
                val results: List<Pair<String, BoundingBox>> = game.imageUtils.findTextLocations(
                    cropRegion = BoundingBox(0, 0, buttonBitmap.width, buttonBitmap.height),
                    sourceBitmap = buttonBitmap,
                    grayscale = true,
                    thresh = false,
                    scale = 2.0,
                    debugName = "MenuBar",
                )

                // Extract the text locations for each button and generate our button mapping.
                for (result in results) {
                    val text: String = result.first
                    val bboxText = BoundingBox(
                        x = bbox.x + result.second.x,
                        y = bbox.y + result.second.y,
                        w = result.second.w,
                        h = result.second.h,
                    )

                    val match: String? = TextUtils.matchStringInList(
                        query = text,
                        choices = MenuBarButtonName.entries.map { it.name.lowercase() },
                        threshold = 0.8,
                    )

                    if (match == null) {
                        continue
                    }

                    val menuBarButtonName: MenuBarButtonName? = MenuBarButtonName.fromName(match)
                    // Ignore errant detections. We'll assert buttons later.
                    if (menuBarButtonName == null) {
                        continue
                    }
                    buttons[menuBarButtonName] = MenuBarButton(
                        game,
                        menuBarButtonName,
                        bboxText,
                    )
                }

                val missing: List<String> = MenuBarButtonName.entries
                    .filter { it !in buttons.keys }
                    .map { it.name }
                if (missing.size != 0) {
                    MessageLog.e(TAG, "Failed to detect all menu bar buttons. Missing: $missing")
                    buttons.clear()
                    numAttempts++
                    continue
                }

                MessageLog.d(TAG, "MenuBar detected successfully.")
                return MenuBar(game, buttons.toMap(), bbox)
            }
            MessageLog.e(TAG, "Failed to detect menu bar after $numAttempts attempts. MenuBar not instantiated.")
            return null
        }
    }

    /** Switches to the specified menu bar tab.
     *
     * @param menuBarButtonName The MenuBarButtonName to switch to.
     * @param timeoutMs The max time limit of this operation before failing.
     *
     * @return Whether the [menuBarButtonName] tab was selected and is now active.
     */
    fun goToTab(menuBarButtonName: MenuBarButtonName, timeoutMs: Int = 3000): Boolean {
        val button: MenuBarButton = buttons[menuBarButtonName]!!
        MessageLog.i(TAG, "Navigating to menu: ${menuBarButtonName.name}")
        val result: Boolean = button.click(timeoutMs)
        if (!result) {
            MessageLog.w(TAG, "Failed to verify menu after clicking: ${menuBarButtonName.name}")
        }
        return result
    }

    fun goToEnhance(timeoutMs: Int = 3000): Boolean {
        return goToTab(MenuBarButtonName.ENHANCE, timeoutMs)
    }

    fun goToStory(timeoutMs: Int = 3000): Boolean {
        return goToTab(MenuBarButtonName.STORY, timeoutMs)
    }

    fun goToHome(timeoutMs: Int = 3000): Boolean {
        return goToTab(MenuBarButtonName.HOME, timeoutMs)
    }

    fun goToRace(timeoutMs: Int = 3000): Boolean {
        return goToTab(MenuBarButtonName.RACE, timeoutMs)
    }

    fun goToScout(timeoutMs: Int = 3000): Boolean {
        return goToTab(MenuBarButtonName.SCOUT, timeoutMs)
    }

    /** Returns the current active menu bar tab.
     *
     * @return The current active tab name.
     * Otherwise, if no active tabs were detected, returns NULL.
     */
    fun getActiveTab(): MenuBarButtonName? {
        val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
        for ((menuBarButtonName, button) in buttons) {
            if (button.isActive(bitmap)) {
                MessageLog.d(TAG, "Active Tab: ${menuBarButtonName.name}")
                return menuBarButtonName
            }
        }
        MessageLog.w(TAG, "Failed to determine active menu bar tab.")
        return null
    }

    /** Checks if the current active menu bar tab is equal to the passed tab name.
     *
     * @param menuBarButtonName The MenuBarButtonName to check for.
     *
     * @return Whether the specified tab ([menuBarButtonName]) is active.
     */
    fun checkActiveTab(menuBarButtonName: MenuBarButtonName): Boolean {
        val button: MenuBarButton = buttons[menuBarButtonName]!!
        return button.isActive()
    }

    /** Checks if the menu bar is visible on the screen.
     *
     * @return Whether the menu bar is on screen.
     */
    fun check(): Boolean {
        return getActiveTab() != null
    }

    /** Waits for the menu bar to be on screen.
     *
     * @param timeoutMs The max wait time before failing.
     *
     * @return Whether the menu bar was successfully detected within the allotted time.
     */
    fun await(timeoutMs: Int = 5000): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (check()) {
                return true
            }
        }
        MessageLog.w(TAG, "Timed out awaiting menu bar.")
        return false
    }
}
