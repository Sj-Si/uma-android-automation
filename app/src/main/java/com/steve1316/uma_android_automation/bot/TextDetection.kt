package com.steve1316.uma_android_automation.bot

import androidx.preference.PreferenceManager
import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.data.CharacterData
import com.steve1316.uma_android_automation.data.SupportData
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.UserConfig
import net.ricecode.similarity.JaroWinklerStrategy
import net.ricecode.similarity.StringSimilarityServiceImpl

class TextDetection(private val game: Game) {
	private val TAG: String = "TextDetection"
		
	private var result = ""
	private var confidence = 0.0
	private var category = ""
	private var eventTitle = ""
	private var supportCardTitle = ""
	private var eventOptionRewards: ArrayList<String> = arrayListOf()
	
    private var character = UserConfig.config.events.selectedCharacter
	private var minimumConfidence = UserConfig.config.ocr.ocrConfidence

	/**
	 * Fix incorrect characters determined by OCR by replacing them with their Japanese equivalents.
	 */
	private fun fixIncorrectCharacters() {
		MessageLog.i(TAG, "[TEXT-DETECTION] Now attempting to fix incorrect characters in: $result")
		
		if (result.last() == '/') {
			result = result.replace("/", "！")
		}
		
		result = result.replace("(", "（").replace(")", "）")
		MessageLog.i(TAG, "[TEXT-DETECTION] Finished attempting to fix incorrect characters: $result")
	}
	
	/**
	 * Attempt to find the most similar string from data compared to the string returned by OCR.
	 */
	private fun findMostSimilarString() {
		if (!UserConfig.config.bHideComparisonResults) {
			MessageLog.i(TAG, "[TEXT-DETECTION] Now starting process to find most similar string to: $result\n")
		} else {
			MessageLog.i(TAG, "[TEXT-DETECTION] Now starting process to find most similar string to: $result")
		}
		
		// Remove any detected whitespaces.
		result = result.replace(" ", "")
		
		// Use the Jaro Winkler algorithm to compare similarities the OCR detected string and the rest of the strings inside the data classes.
		val service = StringSimilarityServiceImpl(JaroWinklerStrategy())
		
		// Attempt to find the most similar string inside the data classes starting with the Character-specific events.
		if (UserConfig.config.events.bSelectAllCharacters) {
			CharacterData.characters.keys.forEach { characterKey ->
				CharacterData.characters[characterKey]?.forEach { (eventName, eventOptions) ->
					val score = service.score(result, eventName)
					if (!UserConfig.config.bHideComparisonResults) {
						MessageLog.i(TAG, "[CHARA] $characterKey \"${result}\" vs. \"${eventName}\" confidence: $score")
					}
					
					if (score >= confidence) {
						confidence = score
						eventTitle = eventName
						eventOptionRewards = eventOptions
						category = "character"
						character = characterKey
					}
				}
			}
		} else {
			CharacterData.characters[character]?.forEach { (eventName, eventOptions) ->
				val score = service.score(result, eventName)
				if (!UserConfig.config.bHideComparisonResults) {
					MessageLog.i(TAG, "[CHARA] $character \"${result}\" vs. \"${eventName}\" confidence: $score")
				}
				
				if (score >= confidence) {
					confidence = score
					eventTitle = eventName
					eventOptionRewards = eventOptions
					category = "character"
				}
			}
		}
		
		// Now move on to the Character-shared events.
		CharacterData.characters["Shared"]?.forEach { (eventName, eventOptions) ->
			val score = service.score(result, eventName)
			if (!UserConfig.config.bHideComparisonResults) {
				MessageLog.i(TAG, "[CHARA-SHARED] \"${result}\" vs. \"${eventName}\" confidence: $score")
			}
			
			if (score >= confidence) {
				confidence = score
				eventTitle = eventName
				eventOptionRewards = eventOptions
				category = "character-shared"
			}
		}
		
		// Finally, do the same with the user-selected Support Cards.
		if (!UserConfig.config.events.bSelectAllSupportCards) {
			UserConfig.config.events.selectedSupportCards.forEach { supportCardName ->
				SupportData.supports[supportCardName]?.forEach { (eventName, eventOptions) ->
					val score = service.score(result, eventName)
					if (!UserConfig.config.bHideComparisonResults) {
						MessageLog.i(TAG, "[SUPPORT] $supportCardName \"${result}\" vs. \"${eventName}\" confidence: $score")
					}
					
					if (score >= confidence) {
						confidence = score
						eventTitle = eventName
						supportCardTitle = supportCardName
						eventOptionRewards = eventOptions
						category = "support"
					}
				}
			}
		} else {
			SupportData.supports.forEach { (supportName, support) ->
				support.forEach { (eventName, eventOptions) ->
					val score = service.score(result, eventName)
					if (!UserConfig.config.bHideComparisonResults) {
						MessageLog.i(TAG, "[SUPPORT] $supportName \"${result}\" vs. \"${eventName}\" confidence: $score")
					}
					
					if (score >= confidence) {
						confidence = score
						eventTitle = eventName
						supportCardTitle = supportName
						eventOptionRewards = eventOptions
						category = "support"
					}
				}
			}
		}
		
		if (!UserConfig.config.bHideComparisonResults) {
			MessageLog.i(TAG, "[TEXT-DETECTION] Finished process to find similar string.")
		} else {
			MessageLog.i(TAG, "[TEXT-DETECTION] Finished process to find similar string.")
		}
		MessageLog.i(TAG, "[TEXT-DETECTION] Event data fetched for \"${eventTitle}\".")
	}

	/**
	 * Parses a date string from the game and converts it to a structured Game.Date object.
	 * 
	 * This function handles two types of date formats: Pre-Debut and regular date strings.
	 * 
	 * For Pre-Debut dates, the function calculates the current turn based on remaining turns
	 * and determines the month within the Pre-Debut phase (which spans 12 turns).
	 * 
	 * For regular dates, the function parses the year (Junior/Classic/Senior), phase (Early/Late),
	 * and month (Jan-Dec) components. If exact matches aren't found in the predefined mappings,
	 * it uses Jaro Winkler similarity scoring to find the best match.
	 * 
	 * @param dateString The date string to parse (e.g., "Classic Year Early Jan" or "Pre-Debut")
	 *
	 * @return A Game.Date object containing the parsed year, phase, month, and calculated turn number.
	 */
	fun determineDateFromString(dateString: String): Game.Date {
		if (dateString == "") {
			MessageLog.e(TAG, "Received date string from OCR was empty. Defaulting to \"Senior Year Early Jan\" at turn number 49.")
			return Game.Date(3, "Early", 1, 49)
		} else if (dateString.lowercase().contains("debut")) {
			// Special handling for the Pre-Debut phase.
			val turnsRemaining = ImageUtils.determineDayForExtraRace()

			// Pre-Debut ends on Early July (turn 13), so we calculate backwards.
			// This includes the Race day.
			val totalTurnsInPreDebut = 12
			val currentTurnInPreDebut = totalTurnsInPreDebut - turnsRemaining + 1

			val month = ((currentTurnInPreDebut - 1) / 2) + 1
			return Game.Date(1, "Pre-Debut", month, currentTurnInPreDebut)
		}

		// Example input is "Classic Year Early Jan".
		val years = mapOf(
			"Junior Year" to 1,
			"Classic Year" to 2,
			"Senior Year" to 3
		)
		val months = mapOf(
			"Jan" to 1,
			"Feb" to 2,
			"Mar" to 3,
			"Apr" to 4,
			"May" to 5,
			"Jun" to 6,
			"Jul" to 7,
			"Aug" to 8,
			"Sep" to 9,
			"Oct" to 10,
			"Nov" to 11,
			"Dec" to 12
		)

		// Split the input string by whitespace.
		val parts = dateString.trim().split(" ")
		if (parts.size < 3) {
			MessageLog.w(TAG, "[TEXT-DETECTION] Invalid date string format: $dateString")
			return Game.Date(3, "Early", 1, 49)
		}
 
		// Extract the parts with safe indexing using default values.
		val yearPart = parts.getOrNull(0)?.let { first -> 
			parts.getOrNull(1)?.let { second -> "$first $second" }
		} ?: "Senior Year"
		val phase = parts.getOrNull(2) ?: "Early"
		val monthPart = parts.getOrNull(3) ?: "Jan"

		// Find the best match for year using Jaro Winkler if not found in mapping.
		var year = years[yearPart]
		if (year == null) {
			val service = StringSimilarityServiceImpl(JaroWinklerStrategy())
			var bestYearScore = 0.0
			var bestYear = 3

			years.keys.forEach { yearKey ->
				val score = service.score(yearPart, yearKey)
				if (score > bestYearScore) {
					bestYearScore = score
					bestYear = years[yearKey]!!
				}
			}
			year = bestYear
			MessageLog.i(TAG, "[TEXT-DETECTION] Year not found in mapping, using best match: $yearPart -> $year")
		}

		// Find the best match for month using Jaro Winkler if not found in mapping.
		var month = months[monthPart]
		if (month == null) {
			val service = StringSimilarityServiceImpl(JaroWinklerStrategy())
			var bestMonthScore = 0.0
			var bestMonth = 1

			months.keys.forEach { monthKey ->
				val score = service.score(monthPart, monthKey)
				if (score > bestMonthScore) {
					bestMonthScore = score
					bestMonth = months[monthKey]!!
				}
			}
			month = bestMonth
			MessageLog.i(TAG, "[TEXT-DETECTION] Month not found in mapping, using best match: $monthPart -> $month")
		}

		// Calculate the turn number.
		// Each year has 24 turns (12 months x 2 phases each).
		// Each month has 2 turns (Early and Late).
		val turnNumber = ((year - 1) * 24) + ((month - 1) * 2) + (if (phase == "Early") 1 else 2)

		return Game.Date(year, phase, month, turnNumber)
	}
	
	fun start(): Pair<ArrayList<String>, Double> {
		if (minimumConfidence > 1.0) {
			minimumConfidence = 0.8
		}
		
		// Reset to default values.
		result = ""
		confidence = 0.0
		category = ""
		eventTitle = ""
		supportCardTitle = ""
		eventOptionRewards.clear()
		
		var increment = 0.0
		
		val startTime: Long = System.currentTimeMillis()
		while (true) {
			// Perform Tesseract OCR detection.
			if ((255.0 - UserConfig.config.ocr.ocrThreshold - increment) > 0.0) {
				result = ImageUtils.findText(increment)
			} else {
				break
			}
			
			if (result.isNotEmpty() && result != "empty!") {
				// Make some minor improvements by replacing certain incorrect characters with their Japanese equivalents.
				fixIncorrectCharacters()
				
				// Now attempt to find the most similar string compared to the one from OCR.
				findMostSimilarString()
				
				when (category) {
					"character" -> {
						if (!UserConfig.config.bHideComparisonResults) {
							MessageLog.i(TAG, "[RESULT] Character $character Event Name = $eventTitle with confidence = $confidence")
						}
					}
					"character-shared" -> {
						if (!UserConfig.config.bHideComparisonResults) {
							MessageLog.i(TAG, "[RESULT] Character Shared Event Name = $eventTitle with confidence = $confidence")
						}
					}
					"support" -> {
						if (!UserConfig.config.bHideComparisonResults) {
							MessageLog.i(TAG, "[RESULT] Support $supportCardTitle Event Name = $eventTitle with confidence = $confidence")
						}
					}
				}
				
				if (UserConfig.config.ocr.bEnableOcrAutomaticRetry && !UserConfig.config.bHideComparisonResults) {
					MessageLog.i(TAG, "[RESULT] Threshold incremented by $increment")
				}
				
				if (confidence < minimumConfidence && UserConfig.config.ocr.bEnableOcrAutomaticRetry) {
					increment += 5.0
				} else {
					break
				}
			} else {
				increment += 5.0
			}
		}
		
		val endTime: Long = System.currentTimeMillis()
		MessageLog.d(TAG, "Total Runtime for detecting Text: ${endTime - startTime}ms")
		
		return Pair(eventOptionRewards, confidence)
	}
}