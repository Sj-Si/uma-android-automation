package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.MainApplication
import com.steve1316.uma_android_automation.utils.GameUtils
import com.steve1316.uma_android_automation.utils.Screen
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.UserConfig
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.utils.MyAccessibilityService
import org.opencv.core.Point

object ComponentUtils {
    /**
    * Performs a tap on the screen at the coordinates and then will wait until the game processes the server request and gets a response back.
    *
    * @param x The x-coordinate.
    * @param y The y-coordinate.
    * @param imageName The template image name to use for tap location randomization.
    * @param taps The number of taps.
    * @param ignoreWaiting Flag to ignore checking if the game is busy loading.
    */
    fun tap(x: Double, y: Double, imageName: String, taps: Int = 1, ignoreWaiting: Boolean = false) {
        // Perform the tap.
        MyAccessibilityService.getInstance().tap(x, y, imageName, taps = taps)

        if (!ignoreWaiting) {
            // Now check if the game is waiting for a server response from the tap and wait if necessary.
            GameUtils.wait(0.20)
            GameUtils.waitForLoading()
        }
    }

    /**
    * Find and tap the specified image.
    *
    * @param imageName Name of the button image file in the /assets/images/ folder.
    * @param tries Number of tries to find the specified button. Defaults to 3.
    * @param region Specify the region consisting of (x, y, width, height) of the source screenshot to template match. Defaults to (0, 0, 0, 0) which is equivalent to searching the full image.
    * @param taps Specify the number of taps on the specified image. Defaults to 1.
    * @param suppressError Whether or not to suppress saving error messages to the log in failing to find the button. Defaults to false.
    * @return True if the button was found and clicked. False otherwise.
    */
	fun findAndTapImage(imageName: String, tries: Int = 3, region: IntArray = Screen.FULL, taps: Int = 1, suppressError: Boolean = false, loggingTag: String = "ComponentUtils"): Boolean {
		if (UserConfig.config.bEnableDebugMode) {
			MessageLog.d(loggingTag, "Now attempting to find and click the \"$imageName\" button.")
		}

		val tempLocation: Point? = ImageUtils.findImage(imageName, tries = tries, region = region, suppressError = suppressError).first

		return if (tempLocation != null) {
			MessageLog.d(loggingTag, "Found and going to tap: $imageName")
			tap(tempLocation.x, tempLocation.y, imageName, taps = taps)
			true
		} else {
			false
		}
	}
}
