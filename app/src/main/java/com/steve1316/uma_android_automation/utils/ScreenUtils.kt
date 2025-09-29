package com.steve1316.uma_android_automation.utils

import com.steve1316.uma_android_automation.utils.MediaProjectionService

/** Helper function wrapper for accessing screen information and regions.
*
* Uses the MediaProjectionService to retrieve display dimensions.
*/
object Screen {
    var WIDTH: Int = 0
        get() {
            return MediaProjectionService.displayWidth
        }

    var HEIGHT: Int = 0
        get() {
            return MediaProjectionService.displayHeight
        }

    // SCREEN REGIONS

    // TODO: Separate tablet landscape scale to non-splitscreen and splitscreen scales.

    var FULL: IntArray = intArrayOf(0, 0, 0, 0)

    // Top part of screen containing title bars and whatnot.
    var TOP: IntArray = intArrayOf(0, 0, 0, 0)
        get() {
            return intArrayOf(0, 0, WIDTH, HEIGHT / 6)
        }

    var TOP_HALF: IntArray = intArrayOf(0, 0, 0, 0)
        get() {
            return intArrayOf(0, 0, WIDTH, HEIGHT / 2)
        }

    // 50% of total height of display, centered vertically.
    var MIDDLE: IntArray = intArrayOf(0, 0, 0, 0)
        get() {
            return intArrayOf(0, HEIGHT / 4, WIDTH, HEIGHT / 2)
        }

    var BOTTOM_HALF: IntArray = intArrayOf(0, 0, 0, 0)
        get() {
            return intArrayOf(0, HEIGHT / 2, WIDTH, HEIGHT / 2)
        }

    // Bottom part of screen containing menu bars and whatnot.
    var BOTTOM: IntArray = intArrayOf(0, 0, 0, 0)
        get() {
            return intArrayOf(0, HEIGHT / 6, WIDTH, HEIGHT / 6)
        }
}
