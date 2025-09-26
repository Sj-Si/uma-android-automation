package com.steve1316.uma_android_automation.utils

import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.ScreenRegion
import com.steve1316.uma_android_automation.utils.ImageUtils

object ComponentDetection {
    private val TAG: String = "ComponentDetection"
    /**
	 * Checks if the bot is at the Main screen or the screen with available options to undertake.
	 * This will also make sure that the Main screen does not contain the option to select a race.
	 *
	 * @return True if the bot is at the Main screen. Otherwise false.
	 */
	fun checkMainScreen(tag: String = TAG): Boolean {
        return ImageUtils.findImage("tazuna", tries = 1, region = ScreenRegion.TOP_HALF).first != null &&
            ImageUtils.findImage("race_select_mandatory", tries = 1, region = ScreenRegion.BOTTOM_HALF, suppressError = true).first == null
	}
}