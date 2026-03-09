package com.steve1316.uma_android_automation.bot

import android.util.Log
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.utils.CustomImageUtils
import com.steve1316.automation_library.utils.SettingsHelper
import com.steve1316.automation_library.utils.MessageLog
import net.ricecode.similarity.JaroWinklerStrategy
import net.ricecode.similarity.StringSimilarityServiceImpl
import org.json.JSONObject

/**
 * Recognizes training events by performing OCR on event titles and matching them against
 * known character and support card event data using string similarity algorithms.
 */
class TrainingEventRecognizer(private val game: Game, private val imageUtils: CustomImageUtils) {
	private val TAG: String = "[${MainActivity.loggerTag}]TrainingEventRecognizer"
	
	// Define event matching patterns to filter false positives during detection.
	val eventPatterns = mapOf(
		"New Year's Resolutions" to listOf("New Year's Resolutions", "Resolutions"),
		"New Year's Shrine Visit" to listOf("New Year's Shrine Visit", "Shrine Visit"),
		"Victory!" to listOf("Victory!"),
		"Solid Showing" to listOf("Solid Showing"),
		"Defeat" to listOf("Defeat"),
		"Get Well Soon!" to listOf("Get Well Soon"),
		"Don't Overdo It!" to listOf("Don't Overdo It"),
		"Extra Training" to listOf("Extra Training"),
		"Acupuncture (Just an Acupuncturist, No Worries! ☆)" to listOf("Acupuncture", "Just an Acupuncturist"),
		"Etsuko's Exhaustive Coverage" to listOf("Etsuko", "Exhaustive Coverage"),
        "Tutorial" to listOf("Tutorial"),
		"A Team at Last" to listOf("A Team at Last", "Team at Last")
	)
	
	// The full character event data should be stored in SQLite and will be loaded here.
	private val characterEventData: JSONObject? = try {
		val characterDataString = SettingsHelper.getStringSetting("trainingEvent", "characterEventData")
		if (characterDataString != null && characterDataString.isNotEmpty()) {
			val jsonObject = JSONObject(characterDataString)
			if (game.debugMode) MessageLog.d(TAG, "Character event data length: ${jsonObject.length()}.")
			jsonObject
		} else {
			null
		}
	} catch (e: Exception) {
		if (game.debugMode) MessageLog.d(TAG, "[DEBUG] Failed to load character event data from SQLite: ${e.message}")
		null
	}
	
	// The full support event data should be stored in SQLite and will be loaded here.
	private val supportEventData: JSONObject? = try {
		val supportDataString = SettingsHelper.getStringSetting("trainingEvent", "supportEventData")
		if (supportDataString != null && supportDataString.isNotEmpty()) {
			val jsonObject = JSONObject(supportDataString)
			if (game.debugMode) MessageLog.d(TAG, "Support event data length: ${jsonObject.length()}.")
			jsonObject
		} else {
			null
		}
	} catch (e: Exception) {
		if (game.debugMode) MessageLog.d(TAG, "[DEBUG] Failed to load support event data from SQLite: ${e.message}")
		null
	}
	
	private val hideComparisonResults: Boolean = SettingsHelper.getBooleanSetting("debug", "enableHideOCRComparisonResults")
	private val minimumConfidence = SettingsHelper.getIntSetting("ocr", "ocrConfidence").toDouble() / 100.0
	private val threshold = SettingsHelper.getIntSetting("ocr", "ocrThreshold").toDouble()
	private val enableAutomaticRetry = SettingsHelper.getBooleanSetting("ocr", "enableAutomaticOCRRetry")

	private val stringSimilarityService = StringSimilarityServiceImpl(JaroWinklerStrategy())

	// Cache OCR matching results to avoid redundant string comparisons.
	private val ocrMatchingCache = mutableMapOf<String, MatchingResult>()

    /**
    * Data class to hold a quadruple of values.
    */
    data class Quadruple<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    /**
     * Data class to hold the result of finding the most similar string.
     */
    private data class MatchingResult(
        val confidence: Double,
        val category: String,
        val eventTitle: String,
        val supportCardTitle: String,
        val eventOptionRewards: ArrayList<String>,
        val character: String
    )

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Attempt to find the most similar string from data compared to the string returned by OCR.
	 *
	 * @param ocrResult The string result from OCR detection.
	 * @return A MatchingResult containing the best match found, or default values if no match is found.
	 */
	private fun findMostSimilarString(ocrResult: String): MatchingResult {
		MessageLog.i(TAG, "[TRAINING_EVENT_RECOGNIZER] Now starting process to find most similar string to: $ocrResult")
		
		// Check cache first to avoid redundant comparisons.
		ocrMatchingCache[ocrResult]?.let {
			MessageLog.i(TAG, "[TRAINING_EVENT_RECOGNIZER] Using cached result for: $ocrResult")
			return it
		}
		
		// Initialize result with default values.
		var confidence = 0.0
		var category = ""
		var eventTitle = ""
		var supportCardTitle = ""
		var eventOptionRewards: ArrayList<String> = arrayListOf()
		var character = ""
		
		// Check if this matches any special event patterns first to filter false positives.
		var matchedSpecialEvent: String? = null
		for ((eventName, patterns) in eventPatterns) {
			if (patterns.any { pattern -> ocrResult.contains(pattern) }) {
				matchedSpecialEvent = eventName
				break
			}
		}
		val isSpecialEvent = matchedSpecialEvent != null
		if (isSpecialEvent) {
			MessageLog.i(TAG, "[TRAINING_EVENT_RECOGNIZER] Detected special event pattern: $matchedSpecialEvent. Will restrict search to this event.")
			eventTitle = matchedSpecialEvent
		}
		
		// Remove any detected whitespaces.
		val processedResult = ocrResult.replace(" ", "")
		
		// Attempt to find the most similar string inside the character event data.
		if (characterEventData != null) {
			characterEventData.keys().forEach { characterKey ->
				val characterEvents = characterEventData.getJSONObject(characterKey)
				characterEvents.keys().forEach { eventName ->
					// Skip if this is a special event and the event name doesn't match our detected pattern.
					if (isSpecialEvent && eventName != matchedSpecialEvent) {
						return@forEach
					}
					
					val eventOptionsArray = characterEvents.getJSONArray(eventName)
					val eventOptions = ArrayList<String>()
					for (i in 0 until eventOptionsArray.length()) {
						eventOptions.add(eventOptionsArray.getString(i))
					}
					
					val score = stringSimilarityService.score(processedResult, eventName)
					if (!hideComparisonResults) {
						MessageLog.i(TAG, "[CHARA] $characterKey \"${processedResult}\" vs. \"${eventName}\" confidence: ${game.decimalFormat.format(score)}")
					}
					
					if (score >= confidence) {
						confidence = score
						eventTitle = eventName
						eventOptionRewards = eventOptions
						category = "character"
						character = characterKey
						
						// Early exit when we've found a match that meets the minimum confidence.
						if (score >= minimumConfidence) {
							val result = MatchingResult(confidence, category, eventTitle, supportCardTitle, eventOptionRewards, character)
							ocrMatchingCache[ocrResult] = result
							return result
						}
					}
				}
			}
		}
		
		// Finally, do the same with all Support Cards.
		if (supportEventData != null) {
			supportEventData.keys().forEach { supportName ->
				val supportEvents = supportEventData.getJSONObject(supportName)
				supportEvents.keys().forEach { eventName ->
					// Skip if this is a special event and the event name doesn't match our detected pattern.
					if (isSpecialEvent && eventName != matchedSpecialEvent) {
						return@forEach
					}
					
					val eventOptionsArray = supportEvents.getJSONArray(eventName)
					val eventOptions = ArrayList<String>()
					for (i in 0 until eventOptionsArray.length()) {
						eventOptions.add(eventOptionsArray.getString(i))
					}
					
					val score = stringSimilarityService.score(processedResult, eventName)
					if (!hideComparisonResults) {
						MessageLog.i(TAG, "[SUPPORT] $supportName \"${processedResult}\" vs. \"${eventName}\" confidence: $score")
					}
					
					if (score >= confidence) {
						confidence = score
						eventTitle = eventName
						supportCardTitle = supportName
						eventOptionRewards = eventOptions
						category = "support"
						
						// Early exit when we've found a match that meets the minimum confidence.
						if (score >= minimumConfidence) {
							val result = MatchingResult(confidence, category, eventTitle, supportCardTitle, eventOptionRewards, character)
							ocrMatchingCache[ocrResult] = result
							return result
						}
					}
				}
			}
		}

		MessageLog.i(TAG, "${if (!hideComparisonResults) "\n" else ""}[TRAINING_EVENT_RECOGNIZER] Finished process to find similar string.")
		MessageLog.i(TAG, "[TRAINING_EVENT_RECOGNIZER] Event data fetched for \"${eventTitle}\".")
		
		// Cache result before returning.
		val result = MatchingResult(confidence, category, eventTitle, supportCardTitle, eventOptionRewards, character)
		ocrMatchingCache[ocrResult] = result
		return result
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Starts the training event recognition process by performing OCR on the event title
	 * and matching it against known event data.
	 *
	 * @return A quadruple containing the event option rewards, confidence score, event title, and character/support name.
	 */
	fun start(): Quadruple<ArrayList<String>, Double, String, String> {
		MessageLog.i(TAG, "\n********************")

		// Initialize best result with default values.
		var bestResult = MatchingResult(0.0, "", "", "", arrayListOf(), "")
		
		var increment = 0.0
		
		val startTime: Long = System.currentTimeMillis()
		while (true) {
			// Perform Tesseract OCR detection.
			val ocrResult: String = if ((255.0 - threshold - increment) > 0.0) {
				imageUtils.findEventTitle(increment)
			} else {
				break
			}
			
			if (ocrResult.isNotEmpty() && ocrResult != "") {
				// Now attempt to find the most similar string compared to the one from OCR.
				val matchingResult = findMostSimilarString(ocrResult)
				if (matchingResult.eventTitle.isNotEmpty() && eventPatterns.containsKey(matchingResult.eventTitle)) {
					MessageLog.i(TAG, "[TRAINING_EVENT_RECOGNIZER] Special event \"${matchingResult.eventTitle}\" detected.")
					bestResult = matchingResult
					break
				}
				
				// Update best result if this one is better.
				if (matchingResult.confidence >= bestResult.confidence) {
					bestResult = matchingResult
				}
				
				when (matchingResult.category) {
					"character" -> {
						MessageLog.i(TAG, "\n[RESULT] Character ${matchingResult.character} Event Name = ${matchingResult.eventTitle} with confidence = ${game.decimalFormat.format(matchingResult.confidence)}")
					}
					"support" -> {
						MessageLog.i(TAG, "\n[RESULT] Support ${matchingResult.supportCardTitle} Event Name = ${matchingResult.eventTitle} with confidence = ${game.decimalFormat.format(matchingResult.confidence)}")
					}
				}
				
				if (enableAutomaticRetry && !hideComparisonResults) {
					MessageLog.i(TAG, "\n[RESULT] Threshold incremented by $increment")
				}
				
				// Round confidence to 2 decimal places to match display precision and avoid floating point issues.
				val roundedConfidence = Math.round(matchingResult.confidence * 100.0) / 100.0
				if (roundedConfidence < minimumConfidence && enableAutomaticRetry) {
					increment += 5.0
				} else {
					break
				}
			} else {
				increment += 5.0
			}
		}
		
		val endTime: Long = System.currentTimeMillis()
		Log.d(TAG, "Total Runtime for recognizing training event: ${endTime - startTime}ms")
		MessageLog.i(TAG, "********************")
		
		val characterOrSupportName = when (bestResult.category) {
			"character" -> bestResult.character
			"support" -> bestResult.supportCardTitle
			else -> ""
		}
		
		return Quadruple(bestResult.eventOptionRewards, bestResult.confidence, bestResult.eventTitle, characterOrSupportName)
	}
}
