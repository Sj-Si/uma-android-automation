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
		MessageLog.i(tag, "Checking if the bot is sitting at the Main screen.")
		return if (ImageUtils.findImage("tazuna", tries = 1, region = ScreenRegion.TOP_HALF).first != null &&
			ImageUtils.findImage("race_select_mandatory", tries = 1, region = ScreenRegion.BOTTOM_HALF, suppressError = true).first == null) {
			MessageLog.i(tag, "Current bot location is at Main screen.")

			// Perform updates here if necessary.
			updateDate()
			if (preferredDistance == "") updatePreferredDistance()
			true
		} else if (!enablePopupCheck && ImageUtils.findImage("cancel", tries = 1, region = ScreenRegion.BOTTOM_HALF).first != null &&
			ImageUtils.findImage("race_confirm", tries = 1, region = ScreenRegion.BOTTOM_HALF).first != null) {
			// This popup is most likely the insufficient fans popup. Force an extra race to catch up on the required fans.
			MessageLog.i(tag, "There is a possible insufficient fans or maiden race popup.")
			encounteredRacingPopup = true
			skipRacing = false
			true
		} else {
			false
		}
	}
}