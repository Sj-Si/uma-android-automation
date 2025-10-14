package com.steve1316.uma_android_automation.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.preference.PreferenceManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.MainApplication
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Integer.max
import java.text.DecimalFormat
import androidx.core.graphics.scale
import androidx.core.graphics.get
import androidx.core.graphics.createBitmap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.sqrt
import kotlin.text.replace

import com.steve1316.uma_android_automation.utils.UserConfig
import com.steve1316.uma_android_automation.utils.GameUtils
import com.steve1316.uma_android_automation.utils.Screen
import com.steve1316.uma_android_automation.utils.TesseractApi
import com.steve1316.uma_android_automation.components.*
import kotlinx.coroutines.runBlocking
import java.io.InputStream

/** Utility functions for image processing via CV like OpenCV. */
class ImageUtils() {
    private val TAG: String = "ImageUtils"
	private val matchMethod: Int = Imgproc.TM_CCOEFF_NORMED
	private val decimalFormat = DecimalFormat("#.###")
	private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private lateinit var context: Context
    private lateinit var tessApi: TesseractApi

    // Stores the file path to the saved match image file for debugging purposes.
    private var matchFilePath: String = ""

	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
    private val bEnableDebugMode: Boolean = UserConfig.config.bEnableDebugMode

    ////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	// Device configuration
	private val isLowerEnd: Boolean = (Screen.WIDTH == 720)
	private val isDefault: Boolean = (Screen.WIDTH == 1080)
	val isTablet: Boolean = (Screen.WIDTH == 1600 && Screen.HEIGHT == 2560) || (Screen.WIDTH == 2560 && Screen.HEIGHT == 1600) // Galaxy Tab S7 1600x2560 Portrait Mode
	private val isLandscape: Boolean = (Screen.WIDTH == 2560 && Screen.HEIGHT == 1600) // Galaxy Tab S7 1600x2560 Landscape Mode
	private val isSplitScreen: Boolean = false // Uma Musume Pretty Derby is only playable in Portrait mode.

    // Scales
	private val lowerEndScales: MutableList<Double> = generateSequence(0.50) { it + 0.01 }
		.takeWhile { it <= 0.70 }
		.toMutableList()

	private val middleEndScales: MutableList<Double> = generateSequence(0.50) { it + 0.01 }
		.takeWhile { it <= 3.00 }
		.toMutableList()

	private val tabletSplitPortraitScales: MutableList<Double> = generateSequence(0.50) { it + 0.01 }
		.takeWhile { it <= 1.00 }
		.toMutableList()

	private val tabletSplitLandscapeScales: MutableList<Double> = generateSequence(0.50) { it + 0.01 }
		.takeWhile { it <= 1.00 }
		.toMutableList()

	private val tabletPortraitScales: MutableList<Double> = generateSequence(1.00) { it + 0.01 }
		.takeWhile { it <= 2.00 }
		.toMutableList()

    data class RaceDetails (
        val fans: Int,
        val hasDoublePredictions: Boolean,
    )

    data class ScaleConfidenceResult(
        val scale: Double,
        val confidence: Double,
    )

    data class BarFillResult(
        val fillPercent: Double,
        val filledSegments: Int,
        val dominantColor: String,
    )

    init {
        context = MainApplication.getApplicationContext()
        tessApi = TesseractApi(context)
    }

    private fun openAsset(relPath: String): InputStream? {
        return context.assets?.open(relPath)
    }

    // ===========================================================================
    // UTILITY FUNCTIONS
    // ===========================================================================

    /**
	 * Convert absolute x-coordinate on 1080p to relative coordinate on different resolutions for the width.
	 *
	 * @param oldX The old absolute x-coordinate based off of the 1080p resolution.
	 * @return The new relative x-coordinate based off of the current resolution.
	 */
	fun relWidth(oldX: Int): Int {
		return if (isDefault) {
			oldX
		} else {
			(oldX.toDouble() * (Screen.WIDTH.toDouble() / 1080.0)).toInt()
		}
	}

	/**
	 * Convert absolute y-coordinate on 1080p to relative coordinate on different resolutions for the height.
	 *
	 * @param oldY The old absolute y-coordinate based off of the 1080p resolution.
	 * @return The new relative y-coordinate based off of the current resolution.
	 */
	fun relHeight(oldY: Int): Int {
		return if (isDefault) {
			oldY
		} else {
			(oldY.toDouble() * (Screen.HEIGHT.toDouble() / 2340.0)).toInt()
		}
	}

	/**
	 * Helper function to calculate the x-coordinate with relative offset.
	 *
	 * @param baseX The base x-coordinate.
	 * @param offset The offset to add/subtract from the base coordinate and to make relative to.
	 * @return The calculated relative x-coordinate.
	 */
	fun relX(baseX: Double, offset: Int): Int {
		return baseX.toInt() + relWidth(offset)
	}

	/**
	 * Helper function to calculate relative y-coordinate with relative offset.
	 *
	 * @param baseY The base y-coordinate.
	 * @param offset The offset to add/subtract from the base coordinate and to make relative to.
	 * @return The calculated relative y-coordinate.
	 */
	fun relY(baseY: Double, offset: Int): Int {
		return baseY.toInt() + relHeight(offset)
	}

    // ===========================================================================
    // BITMAP OPERATIONS
    // ===========================================================================

    /**
	 * Open the source and template image files and return Bitmaps for them.
	 *
	 * @param templateName File name of the template image.
	 * @return A Pair of source and template Bitmaps.
	 */
	fun getBitmaps(templateName: String): Pair<Bitmap, Bitmap?> {
		var sourceBitmap: Bitmap? = null

		while (sourceBitmap == null) {
			sourceBitmap = MediaProjectionService.takeScreenshotNow(saveImage = bEnableDebugMode)
		}

		var templateBitmap: Bitmap?

		// Get the Bitmap from the template image file inside the specified folder.
		openAsset("images/$templateName.png").use { inputStream ->
			// Get the Bitmap from the template image file and then start matching.
			templateBitmap = BitmapFactory.decodeStream(inputStream)
		}

		return if (templateBitmap != null) {
			Pair(sourceBitmap, templateBitmap)
		} else {
			if (bEnableDebugMode) {
				MessageLog.e(TAG, "The template Bitmap is null.")
			}

			Pair(sourceBitmap, templateBitmap)
		}
	}

	/**
	 * Acquire the Bitmap for only the source screenshot.
	 *
	 * @return Bitmap of the source screenshot.
	 */
	private fun getSourceBitmap(): Bitmap {
		var sourceBitmap: Bitmap? = null
		while (sourceBitmap == null) {
			sourceBitmap = MediaProjectionService.takeScreenshotNow(saveImage = bEnableDebugMode)
		}

		return sourceBitmap
	}

    /**
	 * Safely creates a bitmap with bounds checking to prevent IllegalArgumentException.
	 * Clamps individual dimensions to source bitmap bounds if they exceed limits.
	 *
	 * @param sourceBitmap The source bitmap to crop from.
	 * @param x The x coordinate for the crop.
	 * @param y The y coordinate for the crop.
	 * @param width The width of the crop.
	 * @param height The height of the crop.
	 * @param description String description used for logging purposes.
	 * @return The cropped bitmap or null if bounds are still invalid after clamping.
	 */
	private fun createSafeBitmap(sourceBitmap: Bitmap?, x: Int, y: Int, width: Int, height: Int, description: String): Bitmap? {
		if (sourceBitmap == null) {
			MessageLog.d("ImageUtils::createSafeBitmap()", "sourceBitmap is NULL.")
			return null
		}
		// Clamp individual dimensions to source bitmap bounds.
		val clampedX = x.coerceIn(0, sourceBitmap.width)
		val clampedY = y.coerceIn(0, sourceBitmap.height)
		val clampedWidth = width.coerceIn(1, sourceBitmap.width - clampedX)
		val clampedHeight = height.coerceIn(1, sourceBitmap.height - clampedY)

		// Check if any dimensions were clamped and log a warning.
		if (x != clampedX || y != clampedY || width != clampedWidth || height != clampedHeight) {
			MessageLog.w(TAG, "Clamped bounds for $description: original(x=$x, y=$y, width=$width, height=$height) -> clamped(x=$clampedX, y=$clampedY, width=$clampedWidth, height=$clampedHeight), sourceBitmap=${sourceBitmap.width}x${sourceBitmap.height}")
		}

		// Final validation to ensure the clamped dimensions are still valid.
		if (clampedX < 0 || clampedY < 0 || clampedWidth <= 0 || clampedHeight <= 0 ||
			clampedX + clampedWidth > sourceBitmap.width || clampedY + clampedHeight > sourceBitmap.height) {
			MessageLog.e(TAG, "Invalid bounds for $description after clamping: x=$clampedX, y=$clampedY, width=$clampedWidth, height=$clampedHeight, sourceBitmap=${sourceBitmap.width}x${sourceBitmap.height}")
			return null
		}

		return Bitmap.createBitmap(sourceBitmap, clampedX, clampedY, clampedWidth, clampedHeight)
	}

    // ===========================================================================
    // MATCHING FUNCTIONS
    // ===========================================================================

    /**
	 * Match between the source Bitmap from /files/temp/ and the template Bitmap from the assets folder.
	 *
	 * @param sourceBitmap Bitmap from the /files/temp/ folder.
	 * @param templateBitmap Bitmap from the assets folder.
	 * @param templateName Name of the template image to use in debugging log messages.
	 * @param region Specify the region consisting of (x, y, width, height) of the source screenshot to template match. Defaults to (0, 0, 0, 0) which is equivalent to searching the full image.
	 * @param useSingleScale Whether to use only the single custom scale or to use a range based off of it.
	 * @param customConfidence Specify a custom confidence. Defaults to the confidence set in the app's settings.
	 * @param testScale Scale used by testing. Defaults to 0.0 which will fallback to the other scale conditions.
	 * @return Pair of (success: Boolean, location: Point?) where success indicates if a match was found and location contains the match coordinates if found.
	 */
	private fun match(
        sourceBitmap: Bitmap,
        templateBitmap: Bitmap,
        templateName: String,
        region: IntArray = Screen.FULL,
        useSingleScale: Boolean = false,
        customConfidence: Double = 0.0,
        testScale: Double = 0.0,
    ): Pair<Boolean, Point?> {
		// If a custom region was specified, crop the source screenshot.
		val srcBitmap = if (!region.contentEquals(Screen.FULL)) {
			// Validate region bounds to prevent IllegalArgumentException with creating a crop area that goes beyond the source Bitmap.
			val x = max(0, region[0].coerceAtMost(sourceBitmap.width))
			val y = max(0, region[1].coerceAtMost(sourceBitmap.height))
			val width = region[2].coerceAtMost(sourceBitmap.width - x)
			val height = region[3].coerceAtMost(sourceBitmap.height - y)

			createSafeBitmap(sourceBitmap, x, y, width, height, "match region crop") ?: sourceBitmap
		} else {
			sourceBitmap
		}

		val setConfidence: Double = if (templateName == "training_rainbow") {
			MessageLog.i(TAG, "For detection of rainbow training, confidence will be forcibly set to 0.9 to avoid false positives.")
			0.9
		} else if (customConfidence == 0.0) {
			UserConfig.config.ocr.ocrConfidence
		} else {
			customConfidence
		}

		// Scale images if the device is not 1080p which is supported by default.
		val scales: MutableList<Double> = when {
			testScale != 0.0 -> {
				mutableListOf(testScale)
			}
			UserConfig.config.debugOcrScale != 1.0 && !useSingleScale -> {
				mutableListOf(
                    UserConfig.config.debugOcrScale - 0.02,
                    UserConfig.config.debugOcrScale - 0.01,
                    UserConfig.config.debugOcrScale,
                    UserConfig.config.debugOcrScale + 0.01,
                    UserConfig.config.debugOcrScale + 0.02,
                    UserConfig.config.debugOcrScale + 0.03,
                    UserConfig.config.debugOcrScale + 0.04,
                )
			}
			UserConfig.config.debugOcrScale != 1.0 && useSingleScale -> {
				mutableListOf(UserConfig.config.debugOcrScale)
			}
			isLowerEnd -> {
				lowerEndScales.toMutableList()
			}
			!isLowerEnd && !isDefault && !isTablet -> {
				middleEndScales.toMutableList()
			}
			isTablet && isSplitScreen && isLandscape -> {
				tabletSplitLandscapeScales.toMutableList()
			}
			isTablet && isSplitScreen && !isLandscape -> {
				tabletSplitPortraitScales.toMutableList()
			}
			isTablet && !isSplitScreen && !isLandscape -> {
				tabletPortraitScales.toMutableList()
			}
			else -> {
				mutableListOf(1.0)
			}
		}

		while (scales.isNotEmpty()) {
			val newScale: Double = decimalFormat.format(scales.removeAt(0)).toDouble()

			val tmp: Bitmap = if (newScale != 1.0) {
				templateBitmap.scale((templateBitmap.width * newScale).toInt(), (templateBitmap.height * newScale).toInt())
			} else {
				templateBitmap
			}

			// Create the Mats of both source and template images.
			val sourceMat = Mat()
			val templateMat = Mat()
			Utils.bitmapToMat(srcBitmap, sourceMat)
			Utils.bitmapToMat(tmp, templateMat)

			// Clamp template dimensions to source dimensions if template is too large.
			val clampedTemplateMat = if (templateMat.cols() > sourceMat.cols() || templateMat.rows() > sourceMat.rows()) {
				MessageLog.d(TAG, "Image sizes for match assertion failed - sourceMat: ${sourceMat.size()}, templateMat: ${templateMat.size()}")
				// Create a new Mat with clamped dimensions.
				val clampedWidth = minOf(templateMat.cols(), sourceMat.cols())
				val clampedHeight = minOf(templateMat.rows(), sourceMat.rows())
				Mat(templateMat, Rect(0, 0, clampedWidth, clampedHeight))
			} else {
				templateMat
			}

			// Make the Mats grayscale for the source and the template.
			Imgproc.cvtColor(sourceMat, sourceMat, Imgproc.COLOR_BGR2GRAY)
			Imgproc.cvtColor(clampedTemplateMat, clampedTemplateMat, Imgproc.COLOR_BGR2GRAY)

			// Create the result matrix.
			val resultColumns: Int = sourceMat.cols() - clampedTemplateMat.cols() + 1
			val resultRows: Int = sourceMat.rows() - clampedTemplateMat.rows() + 1
			val resultMat = Mat(resultRows, resultColumns, CvType.CV_32FC1)

			// Now perform the matching and localize the result.
			Imgproc.matchTemplate(sourceMat, clampedTemplateMat, resultMat, matchMethod)
			val mmr: Core.MinMaxLocResult = Core.minMaxLoc(resultMat)

			var matchLocation = Point()
			var matchCheck = false

			// Format minVal or maxVal.
			val minVal: Double = decimalFormat.format(mmr.minVal).toDouble()
			val maxVal: Double = decimalFormat.format(mmr.maxVal).toDouble()

			// Depending on which matching method was used, the algorithms determine which location was the best.
			if ((matchMethod == Imgproc.TM_SQDIFF || matchMethod == Imgproc.TM_SQDIFF_NORMED) && mmr.minVal <= (1.0 - setConfidence)) {
				matchLocation = mmr.minLoc
				matchCheck = true
				if (bEnableDebugMode) {
					MessageLog.d(TAG, "Match found for \"$templateName\" with $minVal <= ${1.0 - setConfidence} at Point $matchLocation using scale: $newScale.")
				}
			} else if ((matchMethod != Imgproc.TM_SQDIFF && matchMethod != Imgproc.TM_SQDIFF_NORMED) && mmr.maxVal >= setConfidence) {
				matchLocation = mmr.maxLoc
				matchCheck = true
				if (bEnableDebugMode) {
					MessageLog.d(TAG, "Match found for \"$templateName\" with $maxVal >= $setConfidence at Point $matchLocation using scale: $newScale.")
				}
			} else {
				if (bEnableDebugMode) {
					if ((matchMethod != Imgproc.TM_SQDIFF && matchMethod != Imgproc.TM_SQDIFF_NORMED)) {
						MessageLog.d(TAG, "Match not found for \"$templateName\" with $maxVal not >= $setConfidence at Point ${mmr.maxLoc} using scale $newScale.")
					} else {
						MessageLog.d(TAG, "Match not found for \"$templateName\" with $minVal not <= ${1.0 - setConfidence} at Point ${mmr.minLoc} using scale $newScale.")
					}
				}
			}

			if (matchCheck) {
				if (bEnableDebugMode && matchFilePath != "") {
					// Draw a rectangle around the supposed best matching location and then save the match into a file in /files/temp/ directory. This is for debugging purposes to see if this
					// algorithm found the match accurately or not.
					Imgproc.rectangle(sourceMat, matchLocation, Point(matchLocation.x + templateMat.cols(), matchLocation.y + templateMat.rows()), Scalar(0.0, 0.0, 0.0), 10)
					Imgcodecs.imwrite("$matchFilePath/match.png", sourceMat)
				}

				// Center the coordinates so that any tap gesture would be directed at the center of that match location instead of the default
				// position of the top left corner of the match location.
				matchLocation.x += (templateMat.cols() / 2)
				matchLocation.y += (templateMat.rows() / 2)

				// If a custom region was specified, readjust the coordinates to reflect the fullscreen source screenshot.
				if (!region.contentEquals(Screen.FULL)) {
					matchLocation.x = sourceBitmap.width - (sourceBitmap.width - (region[0] + matchLocation.x))
					matchLocation.y = sourceBitmap.height - (sourceBitmap.height - (region[1] + matchLocation.y))
				}

				return Pair(true, matchLocation)
			}

			if (!BotService.isRunning) {
				throw InterruptedException()
			}

			sourceMat.release()
			templateMat.release()
			clampedTemplateMat.release()
			resultMat.release()
		}

		return Pair(false, null)
	}

    /**
	 * Search through the whole source screenshot for all matches to the template image.
	 *
	 * @param sourceBitmap Bitmap from the /files/temp/ folder.
	 * @param templateBitmap Bitmap from the assets folder.
	 * @param region Specify the region consisting of (x, y, width, height) of the source screenshot to template match. Defaults to (0, 0, 0, 0) which is equivalent to searching the full image.
	 * @param customConfidence Specify a custom confidence. Defaults to the confidence set in the app's settings.
	 * @return ArrayList of Point objects that represents the matches found on the source screenshot.
	 */
	private fun matchAll(sourceBitmap: Bitmap, templateBitmap: Bitmap, region: IntArray = Screen.FULL, customConfidence: Double = 0.0): java.util.ArrayList<Point> {
		// Create a local matchLocations list for this method
		var matchLocation = Point()
		val matchLocations = arrayListOf<Point>()
		
		// If a custom region was specified, crop the source screenshot.
		val srcBitmap = if (!region.contentEquals(Screen.FULL)) {
			// Validate region bounds to prevent IllegalArgumentException with creating a crop area that goes beyond the source Bitmap.
			val x = max(0, region[0].coerceAtMost(sourceBitmap.width))
			val y = max(0, region[1].coerceAtMost(sourceBitmap.height))
			val width = region[2].coerceAtMost(sourceBitmap.width - x)
			val height = region[3].coerceAtMost(sourceBitmap.height - y)

			createSafeBitmap(sourceBitmap, x, y, width, height, "matchAll region crop") ?: sourceBitmap
		} else {
			sourceBitmap
		}

		// Scale images if the device is not 1080p which is supported by default.
		val scales: MutableList<Double> = when {
			UserConfig.config.debugOcrScale != 1.0 -> {
				mutableListOf(
                    UserConfig.config.debugOcrScale - 0.02,
                    UserConfig.config.debugOcrScale - 0.01,
                    UserConfig.config.debugOcrScale,
                    UserConfig.config.debugOcrScale + 0.01,
                    UserConfig.config.debugOcrScale + 0.02,
                    UserConfig.config.debugOcrScale + 0.03,
                    UserConfig.config.debugOcrScale + 0.04,
                )
			}
			isLowerEnd -> {
				lowerEndScales.toMutableList()
			}
			!isLowerEnd && !isDefault && !isTablet -> {
				middleEndScales.toMutableList()
			}
			isTablet && isSplitScreen && isLandscape -> {
				tabletSplitLandscapeScales.toMutableList()
			}
			isTablet && isSplitScreen && !isLandscape -> {
				tabletSplitPortraitScales.toMutableList()
			}
			isTablet && !isSplitScreen && !isLandscape -> {
				tabletPortraitScales.toMutableList()
			}
			else -> {
				mutableListOf(1.0)
			}
		}

		val setConfidence: Double = if (customConfidence == 0.0) {
			UserConfig.config.ocr.ocrConfidence
		} else {
			customConfidence
		}

		var matchCheck = false
		var newScale = 0.0
		val sourceMat = Mat()
		val templateMat = Mat()
		var resultMat = Mat()
		var clampedTemplateMat: Mat? = null

		// Set templateMat at whatever scale it found the very first match for the next while loop.
		while (!matchCheck && scales.isNotEmpty()) {
			newScale = decimalFormat.format(scales.removeAt(0)).toDouble()

			val tmp: Bitmap = if (newScale != 1.0) {
				templateBitmap.scale((templateBitmap.width * newScale).toInt(), (templateBitmap.height * newScale).toInt())
			} else {
				templateBitmap
			}

			// Create the Mats of both source and template images.
			Utils.bitmapToMat(srcBitmap, sourceMat)
			Utils.bitmapToMat(tmp, templateMat)

			// Clamp template dimensions to source dimensions if template is too large.
			clampedTemplateMat = if (templateMat.cols() > sourceMat.cols() || templateMat.rows() > sourceMat.rows()) {
				MessageLog.d(TAG, "Image sizes for matchAll assertion failed - sourceMat: ${sourceMat.size()}, templateMat: ${templateMat.size()}")
				// Create a new Mat with clamped dimensions.
				val clampedWidth = minOf(templateMat.cols(), sourceMat.cols())
				val clampedHeight = minOf(templateMat.rows(), sourceMat.rows())
				Mat(templateMat, Rect(0, 0, clampedWidth, clampedHeight))
			} else {
				templateMat
			}

			// Make the Mats grayscale for the source and the template.
			Imgproc.cvtColor(sourceMat, sourceMat, Imgproc.COLOR_BGR2GRAY)
			Imgproc.cvtColor(clampedTemplateMat, clampedTemplateMat, Imgproc.COLOR_BGR2GRAY)

			// Create the result matrix.
			val resultColumns: Int = sourceMat.cols() - clampedTemplateMat.cols() + 1
			val resultRows: Int = sourceMat.rows() - clampedTemplateMat.rows() + 1
			if (resultColumns < 0 || resultRows < 0) {
				break
			}

			resultMat = Mat(resultRows, resultColumns, CvType.CV_32FC1)

			// Now perform the matching and localize the result.
			Imgproc.matchTemplate(sourceMat, clampedTemplateMat, resultMat, matchMethod)
			val mmr: Core.MinMaxLocResult = Core.minMaxLoc(resultMat)

			// Depending on which matching method was used, the algorithms determine which location was the best.
			if ((matchMethod == Imgproc.TM_SQDIFF || matchMethod == Imgproc.TM_SQDIFF_NORMED) && mmr.minVal <= (1.0 - setConfidence)) {
				matchLocation = mmr.minLoc
				matchCheck = true

				// Draw a rectangle around the match on the source Mat. This will prevent false positives and infinite looping on subsequent matches.
				Imgproc.rectangle(sourceMat, matchLocation, Point(matchLocation.x + clampedTemplateMat.cols(), matchLocation.y + clampedTemplateMat.rows()), Scalar(0.0, 0.0, 0.0), 20)

				// Center the location coordinates and then save it.
				matchLocation.x += (clampedTemplateMat.cols() / 2)
				matchLocation.y += (clampedTemplateMat.rows() / 2)

				// If a custom region was specified, readjust the coordinates to reflect the fullscreen source screenshot.
				if (!region.contentEquals(Screen.FULL)) {
					matchLocation.x = sourceBitmap.width - (sourceBitmap.width - (region[0] + matchLocation.x))
					matchLocation.y = sourceBitmap.height - (sourceBitmap.height - (region[1] + matchLocation.y))
				}

				matchLocations.add(matchLocation)
			} else if ((matchMethod != Imgproc.TM_SQDIFF && matchMethod != Imgproc.TM_SQDIFF_NORMED) && mmr.maxVal >= setConfidence) {
				matchLocation = mmr.maxLoc
				matchCheck = true

				// Draw a rectangle around the match on the source Mat. This will prevent false positives and infinite looping on subsequent matches.
				Imgproc.rectangle(sourceMat, matchLocation, Point(matchLocation.x + clampedTemplateMat.cols(), matchLocation.y + clampedTemplateMat.rows()), Scalar(0.0, 0.0, 0.0), 20)

				// Center the location coordinates and then save it.
				matchLocation.x += (clampedTemplateMat.cols() / 2)
				matchLocation.y += (clampedTemplateMat.rows() / 2)

				// If a custom region was specified, readjust the coordinates to reflect the fullscreen source screenshot.
				if (!region.contentEquals(Screen.FULL)) {
					matchLocation.x = sourceBitmap.width - (sourceBitmap.width - (region[0] + matchLocation.x))
					matchLocation.y = sourceBitmap.height - (sourceBitmap.height - (region[1] + matchLocation.y))
				}

				matchLocations.add(matchLocation)
			}

			if (!BotService.isRunning) {
				throw InterruptedException()
			}
		}

		// Loop until all other matches are found and break out when there are no more to be found.
		while (matchCheck) {
			// Now perform the matching and localize the result.
			Imgproc.matchTemplate(sourceMat, clampedTemplateMat, resultMat, matchMethod)
			val mmr: Core.MinMaxLocResult = Core.minMaxLoc(resultMat)

			// Format minVal or maxVal.
			val minVal: Double = decimalFormat.format(mmr.minVal).toDouble()
			val maxVal: Double = decimalFormat.format(mmr.maxVal).toDouble()

			if (clampedTemplateMat != null && (matchMethod == Imgproc.TM_SQDIFF || matchMethod == Imgproc.TM_SQDIFF_NORMED) && mmr.minVal <= (1.0 - setConfidence)) {
				val tempMatchLocation: Point = mmr.minLoc

				// Draw a rectangle around the match on the source Mat. This will prevent false positives and infinite looping on subsequent matches.
				Imgproc.rectangle(sourceMat, tempMatchLocation, Point(tempMatchLocation.x + clampedTemplateMat.cols(), tempMatchLocation.y + clampedTemplateMat.rows()), Scalar(0.0, 0.0, 0.0), 20)

				if (bEnableDebugMode) {
					MessageLog.d(TAG, "Match All found with $minVal <= ${1.0 - setConfidence} at Point $matchLocation with scale: $newScale.")
					Imgcodecs.imwrite("$matchFilePath/matchAll.png", sourceMat)
				}

				// Center the location coordinates and then save it.
				tempMatchLocation.x += (clampedTemplateMat.cols() / 2)
				tempMatchLocation.y += (clampedTemplateMat.rows() / 2)

				// If a custom region was specified, readjust the coordinates to reflect the fullscreen source screenshot.
				if (!region.contentEquals(Screen.FULL)) {
					tempMatchLocation.x = sourceBitmap.width - (sourceBitmap.width - (region[0] + tempMatchLocation.x))
					tempMatchLocation.y = sourceBitmap.height - (sourceBitmap.height - (region[1] + tempMatchLocation.y))
				}

				if (!matchLocations.contains(tempMatchLocation) && !matchLocations.contains(Point(tempMatchLocation.x + 1.0, tempMatchLocation.y)) &&
					!matchLocations.contains(Point(tempMatchLocation.x, tempMatchLocation.y + 1.0)) && !matchLocations.contains(Point(tempMatchLocation.x + 1.0, tempMatchLocation.y + 1.0))) {
					matchLocations.add(tempMatchLocation)
				}
			} else if (clampedTemplateMat != null && (matchMethod != Imgproc.TM_SQDIFF && matchMethod != Imgproc.TM_SQDIFF_NORMED) && mmr.maxVal >= setConfidence) {
				val tempMatchLocation: Point = mmr.maxLoc

				// Draw a rectangle around the match on the source Mat. This will prevent false positives and infinite looping on subsequent matches.
				Imgproc.rectangle(sourceMat, tempMatchLocation, Point(tempMatchLocation.x + clampedTemplateMat.cols(), tempMatchLocation.y + clampedTemplateMat.rows()), Scalar(0.0, 0.0, 0.0), 20)

				if (bEnableDebugMode) {
					MessageLog.d(TAG, "Match All found with $maxVal >= $setConfidence at Point $matchLocation with scale: $newScale.")
					Imgcodecs.imwrite("$matchFilePath/matchAll.png", sourceMat)
				}

				// Center the location coordinates and then save it.
				tempMatchLocation.x += (clampedTemplateMat.cols() / 2)
				tempMatchLocation.y += (clampedTemplateMat.rows() / 2)

				// If a custom region was specified, readjust the coordinates to reflect the fullscreen source screenshot.
				if (!region.contentEquals(Screen.FULL)) {
					tempMatchLocation.x = sourceBitmap.width - (sourceBitmap.width - (region[0] + tempMatchLocation.x))
					tempMatchLocation.y = sourceBitmap.height - (sourceBitmap.height - (region[1] + tempMatchLocation.y))
				}

				if (!matchLocations.contains(tempMatchLocation) && !matchLocations.contains(Point(tempMatchLocation.x + 1.0, tempMatchLocation.y)) &&
					!matchLocations.contains(Point(tempMatchLocation.x, tempMatchLocation.y + 1.0)) && !matchLocations.contains(Point(tempMatchLocation.x + 1.0, tempMatchLocation.y + 1.0))) {
					matchLocations.add(tempMatchLocation)
				}
			} else {
				break
			}

			if (!BotService.isRunning) {
				throw InterruptedException()
			}
		}

		sourceMat.release()
		templateMat.release()
		clampedTemplateMat?.release()
		resultMat.release()

		return matchLocations
	}

    // ===========================================================================
    // IMAGE DETECTION
    // ===========================================================================

    /**
	 * Finds the location of the specified image from the /images/ folder inside assets.
	 *
	 * @param templateName File name of the template image.
	 * @param tries Number of tries before failing. Defaults to 5.
	 * @param region Specify the region consisting of (x, y, width, height) of the source screenshot to template match. Defaults to (0, 0, 0, 0) which is equivalent to searching the full image.
	 * @param suppressError Whether or not to suppress saving error messages to the log. Defaults to false.
	 * @return Pair object consisting of the Point object containing the location of the match and the source screenshot. Can be null.
	 */
	fun findImage(
        templateName: String,
        tries: Int = 5,
        region: IntArray = Screen.FULL,
        suppressError: Boolean = false,
        customConfidence: Double = 0.0,
    ): Pair<Point?, Bitmap> = runBlocking {
		var numberOfTries = tries

		if (bEnableDebugMode) {
			MessageLog.d(TAG, "Starting process to find the ${templateName.uppercase()} button image...")
		}

		var (sourceBitmap, templateBitmap) = getBitmaps(templateName)

		while (numberOfTries > 0) {
			if (templateBitmap != null) {
				val (resultFlag, location) = match(sourceBitmap, templateBitmap, templateName, region, customConfidence=customConfidence)
				if (!resultFlag) {
					numberOfTries -= 1
					if (numberOfTries <= 0) {
						if (bEnableDebugMode && !suppressError) {
							MessageLog.w(TAG, "Failed to find the ${templateName.uppercase()} button.")
						}

						break
					}

					MessageLog.d(TAG, "Failed to find the ${templateName.uppercase()} button. Trying again...")
					GameUtils.wait(0.1, imageUtils=this@ImageUtils)
					sourceBitmap = getSourceBitmap()
				} else {
					MessageLog.i(TAG, "[SUCCESS] Found the ${templateName.uppercase()} at $location.")
					return@runBlocking Pair(location, sourceBitmap)
				}
			}
		}

		return@runBlocking Pair(null, sourceBitmap)
	}

    /**
	 * Confirms whether or not the bot is at the specified location from the /headers/ folder inside assets.
	 *
	 * @param templateName File name of the template image.
	 * @param tries Number of tries before failing. Defaults to 5.
	 * @param region Specify the region consisting of (x, y, width, height) of the source screenshot to template match. Defaults to (0, 0, 0, 0) which is equivalent to searching the full image.
	 * @param suppressError Whether or not to suppress saving error messages to the log.
	 * @return True if the current location is at the specified location. False otherwise.
	 */
	fun confirmLocation(templateName: String, tries: Int = 5, region: IntArray = Screen.FULL, suppressError: Boolean = false): Boolean {
		var numberOfTries = tries

		if (bEnableDebugMode) {
			MessageLog.d(TAG, "Starting process to find the ${templateName.uppercase()} header image...")
		}

		var (sourceBitmap, templateBitmap) = getBitmaps(templateName)

		while (numberOfTries > 0) {
			if (templateBitmap != null) {
				val (resultFlag, _) = match(sourceBitmap, templateBitmap, templateName, region)
				if (!resultFlag) {
					numberOfTries -= 1
					if (numberOfTries <= 0) {
						break
					}

					GameUtils.wait(0.1, imageUtils=this)
					sourceBitmap = getSourceBitmap()
				} else {
					MessageLog.i(TAG, "[SUCCESS] Current location confirmed to be at ${templateName.uppercase()}.")
					return true
				}
			} else {
				break
			}
		}

		if (bEnableDebugMode && !suppressError) {
			MessageLog.w(TAG, "Failed to confirm the bot location at ${templateName.uppercase()}.")
		}

		return false
	}

    /**
	 * Finds all occurrences of the specified image in the images folder.
	 *
	 * @param templateName File name of the template image.
	 * @param region Specify the region consisting of (x, y, width, height) of the source screenshot to template match. Defaults to (0, 0, 0, 0) which is equivalent to searching the full image.
	 * @return An ArrayList of Point objects containing all the occurrences of the specified image or null if not found.
	 */
	fun findAll(templateName: String, region: IntArray = Screen.FULL): ArrayList<Point> {
		val (sourceBitmap, templateBitmap) = getBitmaps(templateName)

		if (templateBitmap != null) {
			val matchLocations = matchAll(sourceBitmap, templateBitmap, region = region)
			
			// Sort the match locations by ascending x and y coordinates.
			matchLocations.sortBy { it.x }
			matchLocations.sortBy { it.y }

			if (bEnableDebugMode) {
				MessageLog.d(TAG, "Found match locations for $templateName: $matchLocations.")
			} else {
				MessageLog.d(TAG, "[DEBUG] Found match locations for $templateName: $matchLocations.")
			}

			return matchLocations
		}
		
		return arrayListOf()
	}

	/**
	 * Find all occurrences of the specified image in the images folder using a provided source bitmap. Useful for parallel processing to avoid exceeding the maxImages buffer.
	 *
	 * @param templateName File name of the template image.
	 * @param sourceBitmap The source bitmap to search in.
	 * @param region Specify the region consisting of (x, y, width, height) of the source screenshot to template match. Defaults to (0, 0, 0, 0) which is equivalent to searching the full image.
	 * @return An ArrayList of Point objects containing all the occurrences of the specified image or null if not found.
	 */
	private fun findAllWithBitmap(templateName: String, sourceBitmap: Bitmap, region: IntArray = Screen.FULL): ArrayList<Point> {
		var templateBitmap: Bitmap?
		openAsset("images/$templateName.png").use { inputStream ->
			templateBitmap = BitmapFactory.decodeStream(inputStream)
		}

		if (templateBitmap != null) {
			val matchLocations = matchAll(sourceBitmap, templateBitmap, region = region)
			
			// Sort the match locations by ascending x and y coordinates.
			matchLocations.sortBy { it.x }
			matchLocations.sortBy { it.y }

			if (bEnableDebugMode) {
				MessageLog.d(TAG, "Found match locations for $templateName: $matchLocations.")
			} else {
				MessageLog.d(TAG, "[DEBUG] Found match locations for $templateName: $matchLocations.")
			}

			return matchLocations
		}
		
		return arrayListOf()
	}

    /**
	 * Check if the color at the specified coordinates matches the given RGB value.
	 *
	 * @param x X coordinate to check.
	 * @param y Y coordinate to check.
	 * @param rgb Expected RGB values as red, blue and green (0-255).
	 * @param tolerance Tolerance for color matching (0-255). Defaults to 0 for exact match.
	 * @return True if the color at the coordinates matches the expected RGB values within tolerance, false otherwise.
	 */
	fun checkColorAtCoordinates(x: Int, y: Int, rgb: IntArray, tolerance: Int = 0): Boolean {
		val sourceBitmap = getSourceBitmap()

		// Check if coordinates are within bounds.
		if (x < 0 || y < 0 || x >= sourceBitmap.width || y >= sourceBitmap.height) {
			if (bEnableDebugMode) {
                MessageLog.w(TAG, "Coordinates ($x, $y) are out of bounds for bitmap size ${sourceBitmap.width}x${sourceBitmap.height}")
            }
			return false
		}

		// Get the pixel color at the specified coordinates.
		val pixel = sourceBitmap[x, y]

		// Extract RGB values from the pixel.
		val actualRed = android.graphics.Color.red(pixel)
		val actualGreen = android.graphics.Color.green(pixel)
		val actualBlue = android.graphics.Color.blue(pixel)

		// Check if the colors match within the specified tolerance.
		val redMatch = kotlin.math.abs(actualRed - rgb[0]) <= tolerance
		val greenMatch = kotlin.math.abs(actualGreen - rgb[1]) <= tolerance
		val blueMatch = kotlin.math.abs(actualBlue - rgb[2]) <= tolerance

		if (bEnableDebugMode) {
			MessageLog.d(TAG, "Color check at ($x, $y): Expected RGB(${rgb[0]}, ${rgb[1]}, ${rgb[2]}), Actual RGB($actualRed, $actualGreen, $actualBlue), Match: ${redMatch && greenMatch && blueMatch}")
		}

		return redMatch && greenMatch && blueMatch
	}

    // ===========================================================================
    // TEXT-BASED UTILITIES
    // ===========================================================================

    /**
	 * Constructs the final integer value from matched template locations of numbers by analyzing spatial arrangement.
	 *
	 * The function is designed for OCR-like scenarios where individual character templates
	 * are matched separately and need to be reconstructed into a complete number.
	 *
	 * If matchResults contains: {"+" -> [(10, 20)], "1" -> [(15, 20)], "2" -> [(20, 20)]}, it returns: 12 (from string "+12").
	 *
	 * @param matchResults Map of template names (e.g., "0", "1", "2", "+") to their match locations.
	 *
	 * @return The constructed integer value or -1 if it failed.
	 */
	private fun constructIntegerFromMatches(matchResults: Map<String, MutableList<Point>>): Int {
		// Collect all matches with their template names.
		val allMatches = mutableListOf<Pair<String, Point>>()
		matchResults.forEach { (templateName, points) ->
			points.forEach { point ->
				allMatches.add(Pair(templateName, point))
			}
		}

		if (allMatches.isEmpty()) {
			if (bEnableDebugMode) {
                MessageLog.w(TAG, "No matches found to construct integer value.")
            }
			return 0
		}

		// Sort matches by x-coordinate (left to right).
		allMatches.sortBy { it.second.x }
		if (bEnableDebugMode) {
            MessageLog.d(TAG, "Sorted matches: ${allMatches.map { "${it.first}@(${it.second.x}, ${it.second.y})" }}")
        }

		// Construct the string representation and then validate the format: start with + and contain only digits after.
		val constructedString = allMatches.joinToString("") { it.first }
		MessageLog.i(TAG, "Constructed string: \"$constructedString\".")

		// Extract the numeric part and convert to integer.
		return try {
			val numericPart = if (constructedString.startsWith("+") && constructedString.substring(1).isNotEmpty()) {
				constructedString.substring(1)
			} else {
				constructedString
			}

			val result = numericPart.toInt()
			if (bEnableDebugMode) {
                MessageLog.d(TAG, "Successfully constructed integer value: $result from \"$constructedString\".")
            }
			result
		} catch (e: NumberFormatException) {
			MessageLog.e(TAG, "Could not convert \"$constructedString\" to integer: ${e.stackTraceToString()}")
			0
		}
	}

	/**
	 * Calculates the Pearson correlation coefficient between two arrays of pixel values.
	 *
	 * The Pearson correlation coefficient measures the linear correlation between two variables,
	 * ranging from -1 (perfect negative correlation) to +1 (perfect positive correlation).
	 * A value of 0 indicates no linear correlation.
	 *
	 * @param array1 First array of pixel values from the template image.
	 * @param array2 Second array of pixel values from the matched region.
	 * @return Correlation coefficient between -1.0 and +1.0, or 0.0 if arrays are invalid
	 */
	private fun calculateCorrelation(array1: DoubleArray, array2: DoubleArray): Double {
		if (array1.size != array2.size || array1.isEmpty()) {
			return 0.0
		}

		val n = array1.size
		val sum1 = array1.sum()
		val sum2 = array2.sum()
		val sum1Sq = array1.sumOf { it * it }
		val sum2Sq = array2.sumOf { it * it }
		val pSum = array1.zip(array2).sumOf { it.first * it.second }

		// Calculate the numerator: n*Σ(xy) - Σx*Σy
		val num = pSum - (sum1 * sum2 / n)
		// Calculate the denominator: sqrt((n*Σx² - (Σx)²) * (n*Σy² - (Σy)²))
		val den = sqrt((sum1Sq - sum1 * sum1 / n) * (sum2Sq - sum2 * sum2 / n))

		// Return the correlation coefficient, handling division by zero.
		return if (den == 0.0) 0.0 else num / den
	}

    /**
	 * Perform OCR text detection using Tesseract along with some image manipulation via thresholding to make the cropped screenshot black and white using OpenCV.
	 *
	 * @param increment Increments the threshold by this value. Defaults to 0.0.
	 * @return The detected String in the cropped region.
	 */
	fun findText(increment: Double = 0.0): String {
		val (sourceBitmap, templateBitmap) = getBitmaps("shift")

		// Acquire the location of the energy text image.
		val (_, energyTemplateBitmap) = getBitmaps("career/energy")
		val (_, matchLocation) = match(sourceBitmap, energyTemplateBitmap!!, "career/energy")
		if (matchLocation == null) {
			MessageLog.w(TAG, "Could not proceed with OCR text detection due to not being able to find the energy template on the source image.")
			return "empty!"
		}

		// Use the match location acquired from finding the energy text image and acquire the (x, y) coordinates of the event title container right below the location of the energy text image.
		val newX: Int
		val newY: Int
		var croppedBitmap: Bitmap? = if (isTablet) {
			newX = max(0, matchLocation.x.toInt() - relWidth(250))
			newY = max(0, matchLocation.y.toInt() + relHeight(154))
			createSafeBitmap(sourceBitmap, newX, newY, relWidth(746), relHeight(85), "findText tablet crop")
		} else {
			newX = max(0, matchLocation.x.toInt() - relWidth(125))
			newY = max(0, matchLocation.y.toInt() + relHeight(116))
			createSafeBitmap(sourceBitmap, newX, newY, relWidth(645), relHeight(65), "findText phone crop")
		}
		if (croppedBitmap == null) {
			MessageLog.e(TAG, "Failed to create cropped bitmap for text detection")
			return "empty!"
		}

		val tempImage = Mat()
		Utils.bitmapToMat(croppedBitmap, tempImage)
		if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debugEventTitleText.png", tempImage)

		// Now see if it is necessary to shift the cropped region over by 70 pixels or not to account for certain events.
		val (shiftMatch, _) = match(croppedBitmap, templateBitmap!!, "shift")
		croppedBitmap = if (shiftMatch) {
			MessageLog.d(TAG, "Shifting the region over by 70 pixels!")
			createSafeBitmap(sourceBitmap, relX(newX.toDouble(), 70), newY, 645 - 70, 65, "findText shifted crop") ?: croppedBitmap
		} else {
			MessageLog.d(TAG, "Do not need to shift.")
			croppedBitmap
		}

		// Make the cropped screenshot grayscale.
		val cvImage = Mat()
		Utils.bitmapToMat(croppedBitmap, cvImage)
		Imgproc.cvtColor(cvImage, cvImage, Imgproc.COLOR_BGR2GRAY)

		// Save the cropped image before converting it to black and white in order to troubleshoot issues related to differing device sizes and cropping.
		if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debugEventTitleText_afterCrop.png", cvImage)

		// Thresh the grayscale cropped image to make it black and white.
		val bwImage = Mat()
		Imgproc.threshold(cvImage, bwImage, UserConfig.config.ocr.ocrThreshold.toDouble() + increment, 255.0, Imgproc.THRESH_BINARY)
		if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debugEventTitleText_afterThreshold.png", bwImage)

		// Convert the Mat directly to Bitmap and then pass it to the text reader.
		val resultBitmap = createBitmap(bwImage.cols(), bwImage.rows())
		Utils.matToBitmap(bwImage, resultBitmap)

        val result = tessApi.ocr(resultBitmap, singleLineMode=true)

		tempImage.release()
		cvImage.release()
		bwImage.release()

		return result
	}

    // ===========================================================================
    // COMPLEX GAME-SPECIFIC UTILITIES
    // ===========================================================================

	/**
	 * Find the failure percentage chance on the currently selected stat.
	 * Note: The game displays FAILURE rate directly (70% means 70% chance to fail, 30% to succeed).
	 * Parameters are optional to allow for thread-safe operations.
	 *
	 * @param sourceBitmap Bitmap of the source image separately taken. Defaults to null.
	 * @param trainingSelectionLocation Point location of the template image separately taken. Defaults to null.
	 *
	 * @return Integer representing the FAILURE percentage as shown on screen.
	 */
	fun findTrainingFailureChance(sourceBitmap: Bitmap? = null, trainingSelectionLocation: Point? = null): Int {
		// Crop the source screenshot to hold the success percentage only.
		val (trainingSelectionLocation, sourceBitmap) = if (sourceBitmap == null && trainingSelectionLocation == null) {
			TrainingFailureChance.find(imageUtils=this)
		} else {
			Pair(trainingSelectionLocation, sourceBitmap)
		}

		if (trainingSelectionLocation == null) {
			return -1
		}

		val croppedBitmap: Bitmap? = if (isTablet) {
			createSafeBitmap(sourceBitmap!!, relX(trainingSelectionLocation.x, -65), relY(trainingSelectionLocation.y, 23), relWidth(130), relHeight(50), "findTrainingFailureChance tablet")
		} else {
			createSafeBitmap(sourceBitmap!!, relX(trainingSelectionLocation.x, -45), relY(trainingSelectionLocation.y, 15), relWidth(100), relHeight(37), "findTrainingFailureChance phone")
		}
		if (croppedBitmap == null) {
			MessageLog.e(TAG, "Failed to create cropped bitmap for training failure chance detection.")
			return -1
		}

		val resizedBitmap = croppedBitmap.scale(croppedBitmap.width * 2, croppedBitmap.height * 2)

		// Save the cropped image for debugging purposes.
		val tempMat = Mat()
		Utils.bitmapToMat(resizedBitmap, tempMat)
		Imgproc.cvtColor(tempMat, tempMat, Imgproc.COLOR_BGR2GRAY)
		if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debugTrainingFailureChance_afterCrop.png", tempMat)

		// Create a InputImage object for Google's ML OCR.
		val resultBitmap = createBitmap(tempMat.cols(), tempMat.rows())
		Utils.matToBitmap(tempMat, resultBitmap)
		val inputImage: InputImage = InputImage.fromBitmap(resultBitmap, 0)

		// Use CountDownLatch to make the async operation synchronous.
		val latch = CountDownLatch(1)
		var result = -1
		var mlkitFailed = false

		textRecognizer.process(inputImage)
			.addOnSuccessListener { text ->
				if (text.textBlocks.isNotEmpty()) {
					for (block in text.textBlocks) {
						try {
							// The game shows failure rate directly
							result = block.text.replace("%", "").trim().toInt()
							MessageLog.i(TAG, "Detected Training failure chance with Google ML Kit: ${result}%")
						} catch (_: NumberFormatException) {
						}
					}
				}
				latch.countDown()
			}
			.addOnFailureListener {
				MessageLog.e(TAG, "Failed to do text detection via Google's ML Kit. Falling back to Tesseract.")
				mlkitFailed = true
				latch.countDown()
			}

		// Wait for the async operation to complete.
		try {
			latch.await(5, TimeUnit.SECONDS)
		} catch (_: InterruptedException) {
			MessageLog.e(TAG, "Google ML Kit operation timed out")
		}

		// Fallback to Tesseract if ML Kit failed or didn't find result.
		if (mlkitFailed || result == -1) {
            val detectedText = tessApi.ocr(resultBitmap, singleLineMode=true).replace("%", "")
			try {
				MessageLog.i(TAG, "Detected training failure chance with Tesseract: $detectedText")
				val cleanedResult = detectedText.replace(Regex("[^0-9]"), "")
				// The game shows failure rate directly
				result = cleanedResult.toInt()
			} catch (_: NumberFormatException) {
				MessageLog.e(TAG, "Could not convert \"${detectedText}\" to integer.")
				result = -1
			} catch (e: Exception) {
				MessageLog.e(TAG, "Cannot perform OCR using Tesseract: ${e.stackTraceToString()}")
				result = -1
			}
		}

		if (bEnableDebugMode) {
			MessageLog.d(TAG, "Failure chance detected to be at $result%.")
		} else {
			MessageLog.d(TAG, "Failure chance detected to be at $result%.")
		}

		tempMat.release()

		return result
	}

	/**
	 * Determines the day number to see if today is eligible for doing an extra race.
	 *
	 * @return Number of the day.
	 */
	fun determineDayForExtraRace(): Int {
		var result = -1
		val (energyTextLocation, sourceBitmap) = LabelEnergy.find(imageUtils=this)

		if (energyTextLocation != null) {
			// Crop the source screenshot to only contain the day number.
			val croppedBitmap: Bitmap? = if (UserConfig.config.campaign == "Ao Haru") {
				if (isTablet) {
					createSafeBitmap(sourceBitmap, relX(energyTextLocation.x, -(260 * 1.32).toInt()), relY(energyTextLocation.y, -(140 * 1.32).toInt()), relWidth(135), relHeight(100), "determineDayForExtraRace Ao Haru tablet")
				} else {
					createSafeBitmap(sourceBitmap, relX(energyTextLocation.x, -260), relY(energyTextLocation.y, -140), relWidth(105), relHeight(75), "determineDayForExtraRace Ao Haru phone")
				}
			} else {
				if (isTablet) {
					createSafeBitmap(sourceBitmap, relX(energyTextLocation.x, -(246 * 1.32).toInt()), relY(energyTextLocation.y, -(96 * 1.32).toInt()), relWidth(175), relHeight(116), "determineDayForExtraRace default tablet")
				} else {
					createSafeBitmap(sourceBitmap, relX(energyTextLocation.x, -246), relY(energyTextLocation.y, -100), relWidth(140), relHeight(95), "determineDayForExtraRace default phone")
				}
			}
			if (croppedBitmap == null) {
				MessageLog.e(TAG, "Failed to create cropped bitmap for day detection.")
				return -1
			}

			val resizedBitmap = croppedBitmap.scale(croppedBitmap.width * 2, croppedBitmap.height * 2)

			// Make the cropped screenshot grayscale.
			val cvImage = Mat()
			Utils.bitmapToMat(resizedBitmap, cvImage)
			Imgproc.cvtColor(cvImage, cvImage, Imgproc.COLOR_BGR2GRAY)
			if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debugDayForExtraRace_afterCrop.png", cvImage)

			// Thresh the grayscale cropped image to make it black and white.
			val bwImage = Mat()
			Imgproc.threshold(cvImage, bwImage, UserConfig.config.ocr.ocrThreshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)
			if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debugDayForExtraRace_afterThreshold.png", bwImage)

			// Create a InputImage object for Google's ML OCR.
			val resultBitmap = createBitmap(bwImage.cols(), bwImage.rows())
			Utils.matToBitmap(bwImage, resultBitmap)
			val inputImage: InputImage = InputImage.fromBitmap(resultBitmap, 0)

			// Use CountDownLatch to make the async operation synchronous.
			val latch = CountDownLatch(1)
			var mlkitFailed = false

			textRecognizer.process(inputImage)
				.addOnSuccessListener { text ->
					if (text.textBlocks.isNotEmpty()) {
						for (block in text.textBlocks) {
							try {
								MessageLog.i(TAG, "Detected Day Number for Extra Race with Google ML Kit: ${block.text}")
								result = block.text.toInt()
							} catch (_: NumberFormatException) {
							}
						}
					}
					latch.countDown()
				}
				.addOnFailureListener {
					MessageLog.e(TAG, "Failed to do text detection via Google's ML Kit. Falling back to Tesseract.")
					mlkitFailed = true
					latch.countDown()
				}

			// Wait for the async operation to complete.
			try {
				latch.await(5, TimeUnit.SECONDS)
			} catch (_: InterruptedException) {
				MessageLog.e(TAG, "Google ML Kit operation timed out")
			}

			// Fallback to Tesseract if ML Kit failed or didn't find result.
			if (mlkitFailed || result == -1) {
				val detectedText = tessApi.ocr(resultBitmap, singleLineMode=true).replace("%", "")
				try {
					MessageLog.i(TAG, "Detected day for extra racing with Tesseract: $detectedText")
					val cleanedResult = detectedText.replace(Regex("[^0-9]"), "")
					result = cleanedResult.toInt()
				} catch (_: NumberFormatException) {
					MessageLog.e(TAG, "Could not convert \"${detectedText}\" to integer.")
					result = -1
				} catch (e: Exception) {
					MessageLog.e(TAG, "Cannot perform OCR using Tesseract: ${e.stackTraceToString()}")
					result = -1
				}
			}

			cvImage.release()
			bwImage.release()
		}

		return result
	}

	/**
	 * Determine the amount of fans that the extra race will give only if it matches the double star prediction.
	 *
	 * @param extraRaceLocation Point object of the extra race's location.
	 * @param sourceBitmap Bitmap of the source screenshot.
	 * @param doubleStarPredictionBitmap Bitmap of the double star prediction template image.
	 * @param forceRacing Flag to allow the extra race to forcibly pass double star prediction check. Defaults to false.
	 * @return Number of fans to be gained from the extra race or -1 if not found as an object.
	 */
	fun determineExtraRaceFans(extraRaceLocation: Point, sourceBitmap: Bitmap, doubleStarPredictionBitmap: Bitmap, forceRacing: Boolean = false): RaceDetails {
		// Crop the source screenshot to show only the fan amount and the predictions.
		val croppedBitmap = if (isTablet) {
			createSafeBitmap(sourceBitmap, relX(extraRaceLocation.x, -(173 * 1.34).toInt()), relY(extraRaceLocation.y, -(106 * 1.34).toInt()), relWidth(220), relHeight(125), "determineExtraRaceFans prediction tablet")
		} else {
			createSafeBitmap(sourceBitmap, relX(extraRaceLocation.x, -173), relY(extraRaceLocation.y, -106), relWidth(163), relHeight(96), "determineExtraRaceFans prediction phone")
		}
		if (croppedBitmap == null) {
			MessageLog.e(TAG, "Failed to create cropped bitmap for extra race prediction detection.")
			return RaceDetails(-1, false)
		}

		val cvImage = Mat()
		Utils.bitmapToMat(croppedBitmap, cvImage)
		if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debugExtraRacePrediction.png", cvImage)

		// Determine if the extra race has double star prediction.
		val (predictionCheck, _) = match(croppedBitmap, doubleStarPredictionBitmap, "race_extra_double_prediction")

		return if (forceRacing || predictionCheck) {
			if (bEnableDebugMode && !forceRacing) {
                MessageLog.d(TAG, "This race has double predictions. Now checking how many fans this race gives.")
            }
			else if (bEnableDebugMode) {
                MessageLog.d(TAG, "Check for double predictions was skipped due to the force racing flag being enabled. Now checking how many fans this race gives.")
            }

			// Crop the source screenshot to show only the fans.
			val croppedBitmap2 = if (isTablet) {
				createSafeBitmap(sourceBitmap, relX(extraRaceLocation.x, -(625 * 1.40).toInt()), relY(extraRaceLocation.y, -(75 * 1.34).toInt()), relWidth(320), relHeight(45), "determineExtraRaceFans fans tablet")
			} else {
				createSafeBitmap(sourceBitmap, relX(extraRaceLocation.x, -625), relY(extraRaceLocation.y, -75), relWidth(250), relHeight(35), "determineExtraRaceFans fans phone")
			}
			if (croppedBitmap2 == null) {
				MessageLog.e(TAG, "Failed to create cropped bitmap for extra race fans detection.")
				return RaceDetails(-1, predictionCheck)
			}

			// Make the cropped screenshot grayscale.
			Utils.bitmapToMat(croppedBitmap2, cvImage)
			Imgproc.cvtColor(cvImage, cvImage, Imgproc.COLOR_BGR2GRAY)
			if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debugExtraRaceFans_afterCrop.png", cvImage)

			// Convert the Mat directly to Bitmap and then pass it to the text reader.
			var resultBitmap = createBitmap(cvImage.cols(), cvImage.rows())
			Utils.matToBitmap(cvImage, resultBitmap)

			// Thresh the grayscale cropped image to make it black and white.
			val bwImage = Mat()
			Imgproc.threshold(cvImage, bwImage, UserConfig.config.ocr.ocrThreshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)
			if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debugExtraRaceFans_afterThreshold.png", bwImage)

			resultBitmap = createBitmap(bwImage.cols(), bwImage.rows())
			Utils.matToBitmap(bwImage, resultBitmap)

			var result = tessApi.ocr(resultBitmap, singleLineMode=true)

			cvImage.release()
			bwImage.release()

			// Format the string to be converted to an integer.
			MessageLog.i(TAG, "Detected number of fans from Tesseract before formatting: $result")
			result = result
				.replace(",", "")
				.replace(".", "")
				.replace("+", "")
				.replace("-", "")
				.replace(">", "")
				.replace("<", "")
				.replace("(", "")
				.replace("人", "")
				.replace("ォ", "")
				.replace("fans", "").trim()

			try {
				MessageLog.d(TAG, "Converting $result to integer for fans")
				val cleanedResult = result.replace(Regex("[^0-9]"), "")
				RaceDetails(cleanedResult.toInt(), predictionCheck)
			} catch (_: NumberFormatException) {
				RaceDetails(-1, predictionCheck)
			}
		} else {
			MessageLog.d(TAG, "This race has no double prediction.")
			return RaceDetails(-1, false)
		}
	}

	/**
	 * Determine the number of skill points.
	 *
	 * @return Number of skill points or -1 if not found.
	 */
	fun determineSkillPoints(): Int {
		val (skillPointLocation, sourceBitmap) = StatTableHeaderSkillPoints.find(imageUtils=this)

		return if (skillPointLocation != null) {
			val croppedBitmap = if (isTablet) {
				createSafeBitmap(sourceBitmap, relX(skillPointLocation.x, -75), relY(skillPointLocation.y, 45), relWidth(150), relHeight(70), "determineSkillPoints tablet")
			} else {
				createSafeBitmap(sourceBitmap, relX(skillPointLocation.x, -70), relY(skillPointLocation.y, 28), relWidth(135), relHeight(70), "determineSkillPoints phone")
			}
			if (croppedBitmap == null) {
				MessageLog.e(TAG, "Failed to create cropped bitmap for skill points detection.")
				return -1
			}

			// Make the cropped screenshot grayscale.
			val cvImage = Mat()
			Utils.bitmapToMat(croppedBitmap, cvImage)
			Imgproc.cvtColor(cvImage, cvImage, Imgproc.COLOR_BGR2GRAY)
			if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debugSkillPoints_afterCrop.png", cvImage)

			// Thresh the grayscale cropped image to make it black and white.
			val bwImage = Mat()
			Imgproc.threshold(cvImage, bwImage, UserConfig.config.ocr.ocrThreshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)
			if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debugSkillPoints_afterThreshold.png", bwImage)

			// Create a InputImage object for Google's ML OCR.
			val resultBitmap = createBitmap(bwImage.cols(), bwImage.rows())
			Utils.matToBitmap(bwImage, resultBitmap)
			val inputImage: InputImage = InputImage.fromBitmap(resultBitmap, 0)

			// Use CountDownLatch to make the async operation synchronous.
			var result = ""
			val latch = CountDownLatch(1)
			var mlkitFailed = false

			textRecognizer.process(inputImage)
				.addOnSuccessListener { text ->
					if (text.textBlocks.isNotEmpty()) {
						for (block in text.textBlocks) {
							MessageLog.i(TAG, "Detected the number of skill points with Google ML Kit: ${block.text}")
							result = block.text
						}
					}
					latch.countDown()
				}
				.addOnFailureListener {
					MessageLog.e(TAG, "Failed to do text detection via Google's ML Kit. Falling back to Tesseract.")
					mlkitFailed = true
					latch.countDown()
				}

			// Wait for the async operation to complete.
			try {
				latch.await(5, TimeUnit.SECONDS)
			} catch (_: InterruptedException) {
				MessageLog.e(TAG, "Google ML Kit operation timed out")
			}

			if (mlkitFailed || result == "") {
				result = tessApi.ocr(resultBitmap, singleLineMode=true)
			}

			cvImage.release()
			bwImage.release()

			MessageLog.i(TAG, "Detected number of skill points before formatting: $result")
			try {
				MessageLog.d(TAG, "Converting $result to integer for skill points")
				val cleanedResult = result.replace(Regex("[^0-9]"), "")
				cleanedResult.toInt()
			} catch (_: NumberFormatException) {
				-1
			}
		} else {
			MessageLog.e(TAG, "Could not start the process of detecting skill points.")
			-1
		}
	}

    /**
	 * Analyze the relationship bars on the Training screen for the currently selected training. Parameter is optional to allow for thread-safe operations.
	 *
	 * @param sourceBitmap Bitmap of the source image separately taken. Defaults to null.
	 *
	 * @return A list of the results for each relationship bar.
	 */
	fun analyzeRelationshipBars(sourceBitmap: Bitmap? = null): ArrayList<BarFillResult> {
		val customRegion = intArrayOf(Screen.WIDTH - (Screen.WIDTH / 3), 0, (Screen.WIDTH / 3), Screen.HEIGHT - (Screen.HEIGHT / 3))

		// Take a single screenshot first to avoid buffer overflow.
		val sourceBitmap = sourceBitmap ?: getSourceBitmap()

		var allStatBlocks = mutableListOf<Point>()

		val latch = CountDownLatch(6)

		// Create arrays to store results from each thread.
		val speedBlocks = arrayListOf<Point>()
		val staminaBlocks = arrayListOf<Point>()
		val powerBlocks = arrayListOf<Point>()
		val gutsBlocks = arrayListOf<Point>()
		val witBlocks = arrayListOf<Point>()
		val friendshipBlocks = arrayListOf<Point>()

		// Start parallel threads for each findAll call, passing the same source bitmap.
		Thread {
			speedBlocks.addAll(findAllWithBitmap("stat_speed_block", sourceBitmap, region = customRegion))
			latch.countDown()
		}.start()

		Thread {
			staminaBlocks.addAll(findAllWithBitmap("stat_stamina_block", sourceBitmap, region = customRegion))
			latch.countDown()
		}.start()

		Thread {
			powerBlocks.addAll(findAllWithBitmap("stat_power_block", sourceBitmap, region = customRegion))
			latch.countDown()
		}.start()

		Thread {
			gutsBlocks.addAll(findAllWithBitmap("stat_guts_block", sourceBitmap, region = customRegion))
			latch.countDown()
		}.start()

		Thread {
			witBlocks.addAll(findAllWithBitmap("stat_wit_block", sourceBitmap, region = customRegion))
			latch.countDown()
		}.start()

		Thread {
			friendshipBlocks.addAll(findAllWithBitmap("stat_friendship_block", sourceBitmap, region = customRegion))
			latch.countDown()
		}.start()

		// Wait for all threads to complete.
		try {
			latch.await(10, TimeUnit.SECONDS)
		} catch (_: InterruptedException) {
			MessageLog.e(TAG, "Parallel findAll operations timed out.")
		}

		// Combine all results.
		allStatBlocks.addAll(speedBlocks)
		allStatBlocks.addAll(staminaBlocks)
		allStatBlocks.addAll(powerBlocks)
		allStatBlocks.addAll(gutsBlocks)
		allStatBlocks.addAll(witBlocks)
		allStatBlocks.addAll(friendshipBlocks)

		// Filter out duplicates based on exact coordinate matches.
		allStatBlocks = allStatBlocks.distinctBy { "${it.x},${it.y}" }.toMutableList()

		// Sort the combined stat blocks by ascending y-coordinate.
		allStatBlocks.sortBy { it.y }

		// Define HSV color ranges.
		val blueLower = Scalar(10.0, 150.0, 150.0)
		val blueUpper = Scalar(25.0, 255.0, 255.0)
		val greenLower = Scalar(40.0, 150.0, 150.0)
		val greenUpper = Scalar(80.0, 255.0, 255.0)
		val orangeLower = Scalar(100.0, 150.0, 150.0)
		val orangeUpper = Scalar(130.0, 255.0, 255.0)

		val (_, maxedTemplateBitmap) = getBitmaps("stat_maxed")
		val results = arrayListOf<BarFillResult>()

		for ((index, statBlock) in allStatBlocks.withIndex()) {
			if (bEnableDebugMode) {
                MessageLog.d(TAG, "Processing stat block #${index + 1} at position: (${statBlock.x}, ${statBlock.y})")
            }

			val croppedBitmap = createSafeBitmap(sourceBitmap, relX(statBlock.x, -9), relY(statBlock.y, 107), 111, 13, "analyzeRelationshipBars stat block ${index + 1}")
			if (croppedBitmap == null) {
				MessageLog.e(TAG, "Failed to create cropped bitmap for stat block #${index + 1}.")
				continue
			}

			val (isMaxed, _) = match(croppedBitmap, maxedTemplateBitmap!!, "stat_maxed")
			if (isMaxed) {
				// Skip if the relationship bar is already maxed.
				if (bEnableDebugMode) {
                    MessageLog.d(TAG, "Relationship bar #${index + 1} is full.")
                }
				results.add(BarFillResult(100.0, 5, "orange"))
				continue
			}

			val barMat = Mat()
			Utils.bitmapToMat(croppedBitmap, barMat)

			// Convert to RGB and then to HSV for better color detection.
			val rgbMat = Mat()
			Imgproc.cvtColor(barMat, rgbMat, Imgproc.COLOR_BGR2RGB)
			if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debug_relationshipBar${index + 1}AfterRGB.png", rgbMat)
			val hsvMat = Mat()
			Imgproc.cvtColor(rgbMat, hsvMat, Imgproc.COLOR_RGB2HSV)

			val blueMask = Mat()
			val greenMask = Mat()
			val orangeMask = Mat()

			// Count the pixels for each color.
			Core.inRange(hsvMat, blueLower, blueUpper, blueMask)
			Core.inRange(hsvMat, greenLower, greenUpper, greenMask)
			Core.inRange(hsvMat, orangeLower, orangeUpper, orangeMask)
			val bluePixels = Core.countNonZero(blueMask)
			val greenPixels = Core.countNonZero(greenMask)
			val orangePixels = Core.countNonZero(orangeMask)

			// Sum the colored pixels.
			val totalColoredPixels = bluePixels + greenPixels + orangePixels
			val totalPixels = barMat.rows() * barMat.cols()

			// Estimate the fill percentage based on the total colored pixels.
			val fillPercent = if (totalPixels > 0) {
				(totalColoredPixels.toDouble() / totalPixels.toDouble()) * 100.0
			} else 0.0

			// Estimate the filled segments (each segment is about 20% of the whole bar).
			val filledSegments = (fillPercent / 20).coerceAtMost(5.0).toInt()

			val dominantColor = when {
				orangePixels > greenPixels && orangePixels > bluePixels -> "orange"
				greenPixels > bluePixels -> "green"
				bluePixels > 0 -> "blue"
				else -> "none"
			}

			blueMask.release()
			greenMask.release()
			orangeMask.release()
			hsvMat.release()
			barMat.release()

			if (bEnableDebugMode) {
                val msgString = "Relationship bar #${index + 1} is $fillPercent% filled with " +
                    "$filledSegments filled segments and the dominant color is $dominantColor"
                MessageLog.d(TAG, msgString)
            }
			results.add(BarFillResult(fillPercent, filledSegments, dominantColor))
		}

		return results
	}

	/**
	 * Determines the preferred race distance based on aptitude levels (S, A, B) for each distance type on the Full Stats popup.
	 *
	 * This function analyzes the aptitude display for four race distances: Sprint, Mile, Medium, and Long.
	 * It uses template matching to detect S, A, and B aptitude levels and returns the distance with the
	 * highest aptitude found. The priority order is S > A > B, with S aptitude being returned immediately
	 * since it's the best possible outcome.
	 *
	 * @return The preferred distance (Sprint, Mile, Medium, or Long) or Medium as default if no aptitude is detected.
	 */
	fun determinePreferredDistance(): String {
		val (distanceLocation, sourceBitmap) = LabelStatDistance.find(imageUtils=this)
		if (distanceLocation == null) {
			MessageLog.e(TAG, "Could not determine the preferred distance. Setting to Medium by default.")
			return "Medium"
		}

		val (_, statAptitudeSTemplate) = getBitmaps("stat_aptitude_S")
		val (_, statAptitudeATemplate) = getBitmaps("stat_aptitude_A")
		val (_, statAptitudeBTemplate) = getBitmaps("stat_aptitude_B")

		val distances = listOf("Sprint", "Mile", "Medium", "Long")
		var bestAptitudeDistance = ""
		var bestAptitudeLevel = -1 // -1 = none, 0 = B, 1 = A, 2 = S

		for (i in 0 until 4) {
			val distance = distances[i]
			val croppedBitmap = createSafeBitmap(sourceBitmap, relX(distanceLocation.x, 108 + (i * 190)), relY(distanceLocation.y, -25), 176, 52, "determinePreferredDistance distance $distance")
			if (croppedBitmap == null) {
				MessageLog.e(TAG, "Failed to create cropped bitmap for distance $distance.")
				continue
			}

			when {
				match(croppedBitmap, statAptitudeSTemplate!!, "stat_aptitude_S").first -> {
					// S aptitude found - this is the best possible, return immediately.
					return distance
				}
				bestAptitudeLevel < 1 && match(croppedBitmap, statAptitudeATemplate!!, "stat_aptitude_A").first -> {
					// A aptitude found (pick the leftmost aptitude) - better than B, but keep looking for S.
					bestAptitudeDistance = distance
					bestAptitudeLevel = 1
				}
				bestAptitudeLevel < 0 && match(croppedBitmap, statAptitudeBTemplate!!, "stat_aptitude_B").first -> {
					// B aptitude found - only use if no A aptitude found yet.
					bestAptitudeDistance = distance
					bestAptitudeLevel = 0
				}
			}
		}

		return bestAptitudeDistance.ifEmpty {
			MessageLog.w(TAG, "Could not determine the preferred distance with at least B aptitude. Setting to Medium by default.")
			"Medium"
		}
	}

	/**
	 * Reads the 5 stat values on the Main screen.
	 *
	 * @return The mapping of all 5 stats names to their respective integer values.
	 */
	fun determineStatValues(statValueMapping: MutableMap<String, Int>): MutableMap<String, Int> {
		val (skillPointsLocation, sourceBitmap) = StatTableHeaderSkillPoints.find(imageUtils=this)

		if (skillPointsLocation != null) {
			// Process all stats at once using the mapping
			statValueMapping.forEach { (statName, _) ->
				val croppedBitmap = when (statName) {
					"Speed" -> createSafeBitmap(sourceBitmap, relX(skillPointsLocation.x, -862), relY(skillPointsLocation.y, 25), relWidth(98), relHeight(42), "determineStatValues Speed stat")
					"Stamina" -> createSafeBitmap(sourceBitmap, relX(skillPointsLocation.x, -862 + 170), relY(skillPointsLocation.y, 25), relWidth(98), relHeight(42), "determineStatValues Stamina stat")
					"Power" -> createSafeBitmap(sourceBitmap, relX(skillPointsLocation.x, -862 + 170*2), relY(skillPointsLocation.y, 25), relWidth(98), relHeight(42), "determineStatValues Power stat")
					"Guts" -> createSafeBitmap(sourceBitmap, relX(skillPointsLocation.x, -862 + 170*3), relY(skillPointsLocation.y, 25), relWidth(98), relHeight(42), "determineStatValues Guts stat")
					"Wit" -> createSafeBitmap(sourceBitmap, relX(skillPointsLocation.x, -862 + 170*4), relY(skillPointsLocation.y, 25), relWidth(98), relHeight(42), "determineStatValues Wit stat")
					else -> {
						MessageLog.e(TAG, "determineStatValue() received an incorrect stat name of $statName.")
						return@forEach
					}
				}
				if (croppedBitmap == null) {
					MessageLog.e(TAG, "Failed to create cropped bitmap for reading $statName stat value.")
					statValueMapping[statName] = -1
					return@forEach
				}

				// Make the cropped screenshot grayscale.
				val cvImage = Mat()
				Utils.bitmapToMat(croppedBitmap, cvImage)
				Imgproc.cvtColor(cvImage, cvImage, Imgproc.COLOR_BGR2GRAY)
				if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debug${statName}StatValue_afterCrop.png", cvImage)

				val resultBitmap = createBitmap(cvImage.cols(), cvImage.rows())
				Utils.matToBitmap(cvImage, resultBitmap)
				
                val result = tessApi.ocr(resultBitmap, singleLineMode=true)

				cvImage.release()

				MessageLog.i(TAG, "Detected number of stats for $statName from Tesseract before formatting: $result")
				if (result.lowercase().contains("max") || result.lowercase().contains("ax")) {
					MessageLog.i(TAG, "$statName seems to be maxed out. Setting it to 1200.")
					statValueMapping[statName] = 1200
				} else {
					try {
						MessageLog.d(TAG, "Converting $result to integer for $statName stat value")
						val cleanedResult = result.replace(Regex("[^0-9]"), "")
						statValueMapping[statName] = cleanedResult.toInt()
					} catch (_: NumberFormatException) {
						statValueMapping[statName] = -1
					}
				}
			}
		} else {
			MessageLog.e(TAG, "Could not start the process of detecting stat values.")
		}

		return statValueMapping
	}

	/**
	 * Performs OCR on the date region of the game screen to extract the current date string.
	 *
	 * @return The detected date string from the game screen, or empty string if detection fails.
	 */
	fun determineDayNumber(): String {
		val (energyLocation, sourceBitmap) = LabelEnergy.find(imageUtils=this)
		var result = ""
		if (energyLocation != null) {
			val croppedBitmap = createSafeBitmap(sourceBitmap, relX(energyLocation.x, -268), relY(energyLocation.y, -180), relWidth(308), relHeight(35), "determineDayNumber")
			if (croppedBitmap == null) {
				MessageLog.e(TAG, "Failed to create cropped bitmap for day number detection.")
				return ""
			}

			// Make the cropped screenshot grayscale.
			val cvImage = Mat()
			Utils.bitmapToMat(croppedBitmap, cvImage)
			Imgproc.cvtColor(cvImage, cvImage, Imgproc.COLOR_BGR2GRAY)
			if (bEnableDebugMode) Imgcodecs.imwrite("$matchFilePath/debug_dateString_afterCrop.png", cvImage)

			// Create a InputImage object for Google's ML OCR.
			val resultBitmap = createBitmap(cvImage.cols(), cvImage.rows())
			Utils.matToBitmap(cvImage, resultBitmap)
			val inputImage: InputImage = InputImage.fromBitmap(resultBitmap, 0)

			// Use CountDownLatch to make the async operation synchronous.
			val latch = CountDownLatch(1)
			var mlkitFailed = false

			textRecognizer.process(inputImage)
				.addOnSuccessListener { text ->
					if (text.textBlocks.isNotEmpty()) {
						for (block in text.textBlocks) {
							MessageLog.i(TAG, "Detected the date with Google ML Kit: ${block.text}")
							result = block.text
						}
					}
					latch.countDown()
				}
				.addOnFailureListener {
					MessageLog.e(TAG, "Failed to do text detection via Google's ML Kit. Falling back to Tesseract.")
					mlkitFailed = true
					latch.countDown()
				}

			// Wait for the async operation to complete.
			try {
				latch.await(5, TimeUnit.SECONDS)
			} catch (_: InterruptedException) {
				MessageLog.e(TAG, "Google ML Kit operation timed out")
			}

			// Fallback to Tesseract if ML Kit failed or didn't find result.
			if (mlkitFailed || result == "") {
				result = tessApi.ocr(resultBitmap, singleLineMode=true)
			}

			if (bEnableDebugMode) {
				MessageLog.d(TAG, "Date string detected to be at \"$result\".")
			} else {
				MessageLog.d(TAG, "Date string detected to be at \"$result\".")
			}
		} else {
			MessageLog.e(TAG, "Could not start the process of detecting the date string.")
		}

		return result
	}

    /**
	 * Determines the stat gain values from training. Parameters are optional to allow for thread-safe operations.
	 *
	 * This function uses template matching to find individual digits and the "+" symbol in the
	 * stat gain area of the training screen. It processes templates for digits 0-9 and the "+"
	 * symbol, then constructs the final integer value by analyzing the spatial arrangement
	 * of detected matches.
	 *
	 * @param trainingName Name of the currently selected training to determine which stats to read.
	 * @param sourceBitmap Bitmap of the source image separately taken. Defaults to null.
	 * @param skillPointsLocation Point location of the template image separately taken. Defaults to null.
	 *
	 * @return Array of 5 detected stat gain values as integers, or -1 for failed detections.
	 */
	fun determineStatGainFromTraining(trainingName: String, sourceBitmap: Bitmap? = null, skillPointsLocation: Point? = null): IntArray {
		val templates = listOf("+", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
		val statNames = listOf("Speed", "Stamina", "Power", "Guts", "Wit")
		// Define a mapping of training types to their stat indices
		val trainingToStatIndices = mapOf(
			"Speed" to listOf(0, 2),
			"Stamina" to listOf(1, 3),
			"Power" to listOf(1, 2),
			"Guts" to listOf(0, 2, 3),
			"Wit" to listOf(0, 4)
		)

		val (skillPointsLocation, sourceBitmap) = if (sourceBitmap == null && skillPointsLocation == null) {
			StatTableHeaderSkillPoints.find(imageUtils=this)
		} else {
			Pair(skillPointsLocation, sourceBitmap)
		}

		val threadSafeResults = IntArray(5)

		if (skillPointsLocation != null) {
			// Pre-load all template bitmaps to avoid thread contention
			val templateBitmaps = mutableMapOf<String, Bitmap?>()
			for (templateName in templates) {
				openAsset("images/$templateName.png").use { inputStream ->
					templateBitmaps[templateName] = BitmapFactory.decodeStream(inputStream)
				}
			}

			// Process all stats in parallel using threads.
			val statLatch = CountDownLatch(5)
			for (i in 0 until 5) {
				Thread {
					try {
						// Stop the Thread early if the selected Training would not offer stats for the stat to be checked.
						// Speed gives Speed and Power
						// Stamina gives Stamina and Guts
						// Power gives Stamina and Power
						// Guts gives Speed, Power and Guts
						// Wits gives Speed and Wits
						val validIndices = trainingToStatIndices[trainingName] ?: return@Thread
						if (i !in validIndices) return@Thread

						val statName = statNames[i]
						val xOffset = i * 180 // All stats are evenly spaced at 180 pixel intervals.

						val croppedBitmap = createSafeBitmap(sourceBitmap!!, relX(skillPointsLocation.x, -934 + xOffset), relY(skillPointsLocation.y, -103), relWidth(150), relHeight(82), "determineStatGainFromTraining $statName")
						if (croppedBitmap == null) {
							MessageLog.e(TAG, "Failed to create cropped bitmap for $statName stat gain detection.")
							threadSafeResults[i] = 0
							statLatch.countDown()
							return@Thread
						}

						// Convert to Mat and then turn it to grayscale.
						val sourceMat = Mat()
						Utils.bitmapToMat(croppedBitmap, sourceMat)
						val sourceGray = Mat()
						Imgproc.cvtColor(sourceMat, sourceGray, Imgproc.COLOR_BGR2GRAY)

						val workingMat = Mat()
						sourceGray.copyTo(workingMat)

						var matchResults = mutableMapOf<String, MutableList<Point>>()
						templates.forEach { template ->
							matchResults[template] = mutableListOf()
						}

						for (templateName in templates) {
							val templateBitmap = templateBitmaps[templateName]
							if (templateBitmap != null) {
								matchResults = processStatGainTemplateWithTransparency(templateName, templateBitmap, workingMat, matchResults)
							} else {
								MessageLog.e(TAG, "Could not load template \"$templateName\".")
							}
						}

						// Analyze results and construct the final integer value for this region.
						val finalValue = constructIntegerFromMatches(matchResults)
						threadSafeResults[i] = finalValue
						MessageLog.i(TAG, "$statName region final constructed value: $finalValue.")

						// Draw final visualization with all matches for this region.
						if (bEnableDebugMode) {
							val resultMat = Mat()
							Utils.bitmapToMat(croppedBitmap, resultMat)
							templates.forEachIndexed { index, templateName ->
								matchResults[templateName]?.forEach { point ->
									val templateBitmap = templateBitmaps[templateName]
									if (templateBitmap != null) {
										val templateWidth = templateBitmap.width
										val templateHeight = templateBitmap.height

										// Calculate the bounding box coordinates.
										val x1 = (point.x - templateWidth/2).toInt()
										val y1 = (point.y - templateHeight/2).toInt()
										val x2 = (point.x + templateWidth/2).toInt()
										val y2 = (point.y + templateHeight/2).toInt()

										// Draw the bounding box.
										Imgproc.rectangle(resultMat, Point(x1.toDouble(), y1.toDouble()), Point(x2.toDouble(), y2.toDouble()), Scalar(0.0, 0.0, 0.0), 2)

										// Add text label.
										Imgproc.putText(resultMat, templateName, Point(point.x, point.y), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, Scalar(0.0, 0.0, 0.0), 1)
									}
								}
							}

							Imgcodecs.imwrite("$matchFilePath/debug_trainingStatGain_${statNames[i]}_thread${i + 1}.png", resultMat)
						}

						sourceMat.release()
						sourceGray.release()
						workingMat.release()
					} catch (e: Exception) {
						MessageLog.e(TAG, "Error processing stat ${statNames[i]}: ${e.stackTraceToString()}")
						threadSafeResults[i] = 0
					} finally {
						statLatch.countDown()
					}
				}.start()
			}

			// Wait for all threads to complete.
			try {
				statLatch.await(30, TimeUnit.SECONDS)
			} catch (_: InterruptedException) {
				MessageLog.e(TAG, "Stat processing timed out")
			}

			MessageLog.i(TAG, "All 5 stat regions processed. Results: ${threadSafeResults.contentToString()}")
		} else {
			MessageLog.e(TAG, "Could not find the skill points location to start determining stat gains.")
		}

		return threadSafeResults
	}

	/**
	 * Processes a single template with transparency to find all valid matches in the working matrix through a multi-stage algorithm.
	 *
	 * The algorithm uses two validation criteria:
	 * - Pixel match ratio: Ensures sufficient pixel-level similarity.
	 * - Correlation coefficient: Validates statistical correlation between template and matched region.
	 *
	 * @param templateName Name of the template being processed (used for logging and debugging).
	 * @param templateBitmap Bitmap of the template image (must have 4-channel RGBA format with transparency).
	 * @param workingMat Working matrix to search in (grayscale source image).
	 * @param matchResults Map to store match results, organized by template name.
	 *
	 * @return The modified matchResults mapping containing all valid matches found for this template
	 */
	private fun processStatGainTemplateWithTransparency(templateName: String, templateBitmap: Bitmap, workingMat: Mat, matchResults: MutableMap<String, MutableList<Point>>): MutableMap<String, MutableList<Point>> {
		// These values have been tested for the best results against the dynamic background.
		val matchConfidence = 0.9
		val minPixelMatchRatio = 0.1
		val minPixelCorrelation = 0.85

		// Convert template to Mat and then to grayscale.
		val templateMat = Mat()
		val templateGray = Mat()
		Utils.bitmapToMat(templateBitmap, templateMat)
		Imgproc.cvtColor(templateMat, templateGray, Imgproc.COLOR_BGR2GRAY)

		// Check if template has an alpha channel (transparency).
		if (templateMat.channels() != 4) {
			MessageLog.e(TAG, "Template \"$templateName\" is not transparent and is a requirement.")
			templateMat.release()
			templateGray.release()
			return matchResults
		}

		// Extract alpha channel for the alpha mask.
		val alphaChannels = ArrayList<Mat>()
		Core.split(templateMat, alphaChannels)
		val alphaMask = alphaChannels[3] // Alpha channel is the 4th channel.

		// Create binary mask for non-transparent pixels.
		val validPixels = Mat()
		Core.compare(alphaMask, Scalar(0.0), validPixels, Core.CMP_GT)

		// Check transparency ratio.
		val nonZeroPixels = Core.countNonZero(alphaMask)
		val totalPixels = alphaMask.rows() * alphaMask.cols()
		val transparencyRatio = nonZeroPixels.toDouble() / totalPixels
		if (transparencyRatio < 0.1) {
			MessageLog.w(TAG, "Template \"$templateName\" appears to be mostly transparent!")
			alphaChannels.forEach { it.release() }
			validPixels.release()
			alphaMask.release()
			templateMat.release()
			templateGray.release()
			return matchResults
		}

		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////

		var continueSearching = true
		var searchMat = Mat()
		var xOffset = 0
		workingMat.copyTo(searchMat)

		while (continueSearching) {
			var failedPixelMatchRatio = false
			var failedPixelCorrelation = false

			// Template match with the alpha mask.
			val result = Mat()
			Imgproc.matchTemplate(searchMat, templateGray, result, Imgproc.TM_CCORR_NORMED, alphaMask)
			val mmr = Core.minMaxLoc(result)
			val matchVal = mmr.maxVal
			val matchLocation = mmr.maxLoc

			if (matchVal >= matchConfidence) {
				val x = matchLocation.x.toInt()
				val y = matchLocation.y.toInt()
				val h = templateGray.rows()
				val w = templateGray.cols()

				// Validate that the match location is within bounds.
				if (x >= 0 && y >= 0 && x + w <= searchMat.cols() && y + h <= searchMat.rows()) {
					// Extract the matched region from the source image.
					val matchedRegion = Mat(searchMat, Rect(x, y, w, h))

					// Create masked versions of the template and matched region using only non-transparent pixels.
					val templateValid = Mat()
					val regionValid = Mat()
					templateGray.copyTo(templateValid, validPixels)
					matchedRegion.copyTo(regionValid, validPixels)

					// For the first test, compare pixel-by-pixel equality between the matched region and template to calculate match ratio.
					val templateComparison = Mat()
					Core.compare(matchedRegion, templateGray, templateComparison, Core.CMP_EQ)
					val matchingPixels = Core.countNonZero(templateComparison)
					val pixelMatchRatio = matchingPixels.toDouble() / (w * h)
					if (pixelMatchRatio < minPixelMatchRatio) {
						failedPixelMatchRatio = true
					}

					// Extract pixel values into double arrays for correlation calculation.
					val templateValidMat = Mat()
					val regionValidMat = Mat()
					templateValid.convertTo(templateValidMat, CvType.CV_64F)
					regionValid.convertTo(regionValidMat, CvType.CV_64F)
					val templateArray = DoubleArray(templateValid.total().toInt())
					val regionArray = DoubleArray(regionValid.total().toInt())
					templateValidMat.get(0, 0, templateArray)
					regionValidMat.get(0, 0, regionArray)

					// For the second test, validate the match quality by performing correlation calculation.
					val pixelCorrelation = calculateCorrelation(templateArray, regionArray)
					if (pixelCorrelation < minPixelCorrelation) {
						failedPixelCorrelation = true
					}

					// If both tests passed, then the match is valid.
					if (!failedPixelMatchRatio && !failedPixelCorrelation) {
						val centerX = (x + xOffset) + (w / 2)
						val centerY = y + (h / 2)

						// Check for overlap with existing matches within 10 pixels on both axes.
						val hasOverlap = matchResults.values.flatten().any { existingPoint ->
							val existingX = existingPoint.x
							val existingY = existingPoint.y

							// Check if the new match overlaps with existing match within 10 pixels.
							val xOverlap = kotlin.math.abs(centerX - existingX) < 10
							val yOverlap = kotlin.math.abs(centerY - existingY) < 10

							xOverlap && yOverlap
						}

						if (!hasOverlap) {
							MessageLog.d(TAG, "Found valid match for template \"$templateName\" at ($centerX, $centerY).")
							matchResults[templateName]?.add(Point(centerX.toDouble(), centerY.toDouble()))
						}
					}

					// Draw a box to prevent re-detection in the next loop iteration.
					Imgproc.rectangle(searchMat, Point(x.toDouble(), y.toDouble()), Point((x + w).toDouble(), (y + h).toDouble()), Scalar(0.0, 0.0, 0.0), 10)

					templateComparison.release()
					matchedRegion.release()
					templateValid.release()
					regionValid.release()
					templateValidMat.release()
					regionValidMat.release()

					// Crop the Mat horizontally to exclude the supposed matched area.
					val cropX = x + w
					val remainingWidth = searchMat.cols() - cropX
					when {
						remainingWidth < templateGray.cols() -> {
							continueSearching = false
						}
						else -> {
							val newSearchMat = Mat(searchMat, Rect(cropX, 0, remainingWidth, searchMat.rows()))
							searchMat.release()
							searchMat = newSearchMat
							xOffset += cropX
						}
					}
				} else {
					// Stop searching when the source has been traversed.
					continueSearching = false
				}
			} else {
				// No match found above threshold, stop searching for this template.
				continueSearching = false
			}

			result.release()

			// Safety check to prevent infinite loops.
			if ((matchResults[templateName]?.size ?: 0) > 10) {
				continueSearching = false
			}
			if (!BotService.isRunning) {
				throw InterruptedException()
			}
		}

		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////

		searchMat.release()
		alphaChannels.forEach { it.release() }
		validPixels.release()
		alphaMask.release()
		templateMat.release()
		templateGray.release()

		return matchResults
	}

    /**
    * Starts a test to determine what scales are working on this device by looping through some template images.
    *
    * @return A mapping of template image names used to test and their lists of working scales.
    */
    fun startTemplateMatchingTest(): MutableMap<String, MutableList<ScaleConfidenceResult>> {
        val results = mutableMapOf<String, MutableList<ScaleConfidenceResult>>(
            "career/energy" to mutableListOf(),
            "career/tazuna" to mutableListOf(),
            "career/skill_points" to mutableListOf()
        )

        val defaultConfidence = 0.8
        val testScaleDecimalFormat = DecimalFormat("#.##")
        val testConfidenceDecimalFormat = DecimalFormat("#.##")

        for (key in results.keys) {
            val (sourceBitmap, templateBitmap) = getBitmaps(key)

            // First, try the default values of 1.0 for scale and 0.8 for confidence.
            val (success, _) = match(
                sourceBitmap,
                templateBitmap!!,
                key,
                useSingleScale = true,
                customConfidence = defaultConfidence,
                testScale = 1.0,
            )
            if (success) {
                MessageLog.i(TAG, "[TEST] Initial test for $key succeeded at the default values.")
                results[key]?.add(ScaleConfidenceResult(1.0, defaultConfidence))
                continue // If it works, skip to the next template.
            }

            // If not, try all scale/confidence combinations.
            val scalesToTest = mutableListOf<Double>()
            var scale = 0.5
            while (scale <= 3.0) {
                scalesToTest.add(testScaleDecimalFormat.format(scale).toDouble())
                scale += 0.1
            }

            for (testScale in scalesToTest) {
                var confidence = 0.6
                while (confidence <= 1.0) {
                    val formattedConfidence = testConfidenceDecimalFormat.format(confidence).toDouble()
                    val (testSuccess, _) = match(
                        sourceBitmap,
                        templateBitmap,
                        key,
                        useSingleScale = true,
                        customConfidence = formattedConfidence,
                        testScale = testScale,
                    )
                    if (testSuccess) {
                        MessageLog.i(TAG, "[TEST] Test for $key succeeded at scale $testScale and confidence $formattedConfidence.")
                        results[key]?.add(ScaleConfidenceResult(testScale, formattedConfidence))
                    }
                    confidence += 0.1
                }
            }
        }

        return results
    }
}
