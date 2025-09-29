package com.steve1316.uma_android_automation.utils

import com.steve1316.uma_android_automation.utils.MediaProjectionService

/** Helper function wrapper for accessing screen information and regions.
*
* Uses the MediaProjectionService to retrieve display dimensions.
*/
object Screen {
    MIDDLE = intArrayOf(0, HEIGHT / 4, WIDTH, HEIGHT / 2)
        BOTTOM_HALF = intArrayOf(0, HEIGHT / 2, WIDTH, HEIGHT / 2)

    var WIDTH: Int = 0
        get() {
            return MediaProjectionService.region.displayWidth
        }

    var HEIGHT: Int = 0
        get() {
            return MediaProjectionService.region.displayHeight
        }

    // TODO: Separate tablet landscape scale to non-splitscreen and splitscreen scales.
    var TOP_HALF: IntArray = intArrayOf(0, 0, 0, 0)
        get() {
            return intArrayOf(0, 0, WIDTH, HEIGHT / 2)
        }
    var MIDDLE: IntArray = intArrayOf(0, 0, 0, 0)
        get() {
            return intArrayOf(0, HEIGHT / 4, WIDTH, HEIGHT / 2)
        }
    var BOTTOM_HALF: IntArray = intArrayOf(0, 0, 0, 0)
        get() {
            return intArrayOf(0, HEIGHT / 2, WIDTH, HEIGHT / 2)
        }
}
