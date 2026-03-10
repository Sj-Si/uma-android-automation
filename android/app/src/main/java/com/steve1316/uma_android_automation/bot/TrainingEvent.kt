package com.steve1316.uma_android_automation.bot

import com.steve1316.automation_library.utils.SettingsHelper
import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.MainActivity

import com.steve1316.uma_android_automation.components.ButtonClose
import com.steve1316.uma_android_automation.components.ButtonNext
import com.steve1316.uma_android_automation.components.IconTrainingEventHorseshoe

import net.ricecode.similarity.JaroWinklerStrategy
import net.ricecode.similarity.StringSimilarityServiceImpl
import org.opencv.core.Point
import org.json.JSONObject

class TrainingEvent(private val game: Game) {
    private val TAG: String = "[${MainActivity.loggerTag}]TrainingEvent"

    private val trainingEventRecognizer: TrainingEventRecognizer = TrainingEventRecognizer(game, game.imageUtils)

    private val enablePrioritizeEnergyOptions: Boolean = SettingsHelper.getBooleanSetting("trainingEvent", "enablePrioritizeEnergyOptions")
    
    private val positiveStatuses = listOf("Charming", "Fast Learner", "Practice Practice")
    private val negativeStatuses = listOf("Practice Poor", "Migraine", "Night Owl", "Slow Metabolism", "Slacker")

    // Load special event overrides from settings.
    private val specialEventOverrides: Map<String, EventOverride> = try {
        val overridesString = SettingsHelper.getStringSetting("trainingEvent", "specialEventOverrides")
        if (overridesString.isNotEmpty()) {
            val jsonObject = JSONObject(overridesString)
            val overridesMap = mutableMapOf<String, EventOverride>()
            jsonObject.keys().forEach { eventName ->
                val eventData = jsonObject.getJSONObject(eventName)
                overridesMap[eventName] = EventOverride(
                    selectedOption = eventData.getString("selectedOption"),
                    requiresConfirmation = eventData.getBoolean("requiresConfirmation")
                )
            }
            overridesMap
        } else {
            emptyMap()
        }
    } catch (e: Exception) {
        MessageLog.w(TAG, "Could not parse special event overrides: ${e.message}")
        emptyMap()
    }
    
    // Load character event overrides from settings.
    private val characterEventOverrides: Map<String, Int> = try {
        val overridesString = SettingsHelper.getStringSetting("trainingEvent", "characterEventOverrides")
        if (overridesString.isNotEmpty()) {
            val jsonObject = JSONObject(overridesString)
            val overridesMap = mutableMapOf<String, Int>()
            jsonObject.keys().forEach { eventKey ->
                overridesMap[eventKey] = jsonObject.getInt(eventKey)
            }
            overridesMap
        } else {
            emptyMap()
        }
    } catch (e: Exception) {
        MessageLog.w(TAG, "[WARNING] Could not parse character event overrides: ${e.message}")
        emptyMap()
    }
    
    // Load support event overrides from settings.
    private val supportEventOverrides: Map<String, Int> = try {
        val overridesString = SettingsHelper.getStringSetting("trainingEvent", "supportEventOverrides")
        if (overridesString.isNotEmpty()) {
            val jsonObject = JSONObject(overridesString)
            val overridesMap = mutableMapOf<String, Int>()
            jsonObject.keys().forEach { eventKey ->
                overridesMap[eventKey] = jsonObject.getInt(eventKey)
            }
            overridesMap
        } else {
            emptyMap()
        }
    } catch (e: Exception) {
        MessageLog.w(TAG, "[WARNING] Could not parse support event overrides: ${e.message}")
        emptyMap()
    }
    
    data class EventOverride(val selectedOption: String, val requiresConfirmation: Boolean)

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Functions to handle Training Events with the help of the TrainingEventRecognizer class.

    /**
     * Check if the given event title matches any special event overrides.
     *
     * @param eventTitle The detected event title from OCR
     * @return Pair of (optionIndex, requiresConfirmation) if match found, null otherwise
     */
    private fun checkSpecialEventOverride(eventTitle: String): Pair<Int, Boolean>? {
        for ((eventName, patterns) in trainingEventRecognizer.eventPatterns) {
            val override = specialEventOverrides[eventName]
            if (override != null) {
                // Check if any pattern matches the event title.
                val matches = patterns.any { pattern -> eventTitle.contains(pattern) }
                if (matches) {
                    MessageLog.i(TAG, "[TRAINING_EVENT] Detected special event: $eventName")

                    // Parse the option number from the setting (e.g., "Option 5: Energy +10" -> 5)
                    val optionMatch = Regex("Option (\\d+)").find(override.selectedOption)
                    val optionIndex = if (optionMatch != null) {
                        val optionNumber = optionMatch.groupValues[1].toInt()
                        MessageLog.i(TAG, "[TRAINING_EVENT] Using setting: ${override.selectedOption} (Option $optionNumber)")
                        optionNumber - 1
                    } else {
                        MessageLog.w(TAG, "Could not parse option number from setting: ${override.selectedOption}. Using option 1 by default.")
                        0
                    }

                    return Pair(optionIndex, override.requiresConfirmation)
                }
            }
        }

        return null
    }

    /**
     * Check if the given character event matches any character event overrides.
     *
     * @param characterName The detected character name
     * @param eventTitle The detected event title from OCR
     * @return The option index (0-based) if override found, null otherwise
     */
    private fun checkCharacterEventOverride(characterName: String, eventTitle: String): Int? {
        if (characterName.isEmpty()) return null
        
        val eventKey = "$characterName|$eventTitle"
        val override = characterEventOverrides[eventKey]
        if (override != null) {
            MessageLog.i(TAG, "[TRAINING_EVENT] Detected character event override: $eventKey -> Option ${override + 1}")
            return override
        }
        
        return null
    }

    /**
     * Check if the given support event matches any support event overrides.
     *
     * @param supportName The detected support name
     * @param eventTitle The detected event title from OCR
     * @return The option index (0-based) if override found, null otherwise
     */
    private fun checkSupportEventOverride(supportName: String, eventTitle: String): Int? {
        if (supportName.isEmpty()) return null
        
        val eventKey = "$supportName|$eventTitle"
        val override = supportEventOverrides[eventKey]
        if (override != null) {
            MessageLog.i(TAG, "[TRAINING_EVENT] Detected support event override: $eventKey -> Option ${override + 1}")
            return override
        }
        
        return null
    }

    /**
     * Handle the "A Team at Last" Unity Cup event by detecting options via OCR
     * and selecting based on user preference.
     *
     * This event is unique because:
     * - It may have 0 options or 2-5 options.
     * - The last option is always "Team Carrot" (default).
     * - Other options are character suggestions that need to be detected via OCR.
     *
     * @param optionLocations The list of detected option locations.
     * @return The 0-based index of the option to select, or 0 if no match found.
     */
    private fun selectUnityCupTeamNameEvent(optionLocations: ArrayList<Point>): Int {
        val numOptions = optionLocations.size
        MessageLog.i(TAG, "[TRAINING_EVENT] Handling \"A Team at Last\" event with $numOptions option(s).")

        // If 0-1 options, just return 0 (auto-completed or single option).
        if (numOptions <= 1) {
            MessageLog.i(TAG, "[TRAINING_EVENT] Event has $numOptions option(s). Selecting first/only option.")
            return 0
        }

        // Get the user's selected preference from settings.
        val override = specialEventOverrides["A Team at Last"]
        val selectedPreference = override?.selectedOption ?: "Default"
        MessageLog.i(TAG, "[TRAINING_EVENT] User preference for team name: $selectedPreference")

        // If user selected "Default", always select the first option.
        if (selectedPreference == "Default") {
            MessageLog.i(TAG, "[TRAINING_EVENT] Using default preference, selecting first option.")
            return 0
        }

        // If user selected "Team Carrot (Last Option)", always select the last option.
        if (selectedPreference == "Team Carrot (Last Option)") {
            MessageLog.i(TAG, "[TRAINING_EVENT] Using Team Carrot preference, selecting last option.")
            return numOptions - 1
        }

        // Define the possible team name options (excluding "Team Carrot" which is always last).
        val teamNameOptions = listOf(
            "Happy Hoppers, like Taiki suggested",
            "Sunny Runners, like Fukukitaru suggested",
            "Carrot Pudding, like Urara suggested",
            "Blue Bloom, like Rice Shower suggested"
        )

        // OCR each option except the last one (which is always "Team Carrot").
        val sourceBitmap = game.imageUtils.getSourceBitmap()
        val detectedOptions = mutableListOf<Pair<Int, String>>()

        for (i in 0 until numOptions - 1) {
            val optionCenter = optionLocations[i]
            val cropX = game.imageUtils.relX(optionCenter.x, 45)
            val cropY = game.imageUtils.relY(optionCenter.y, -30)
            val cropWidth = 800
            val cropHeight = 55

            val ocrText = game.imageUtils.performOCROnRegion(
                sourceBitmap,
                cropX,
                cropY,
                cropWidth,
                cropHeight,
                useThreshold = false,
                useGrayscale = true,
                scale = 1.0,
                ocrEngine = "tesseract",
                debugName = "selectUnityCupTeamNameEvent_option_${i + 1}"
            )

            MessageLog.i(TAG, "[TRAINING_EVENT] Option ${i + 1} OCR result: \"$ocrText\"")
            if (ocrText.isNotEmpty()) {
                detectedOptions.add(Pair(i, ocrText))
            }
        }

        // Use string similarity to find the best match for the user's preference.
        var bestMatchIndex = 0
        var bestMatchScore = 0.0

        for ((optionIndex, ocrText) in detectedOptions) {
            for (teamName in teamNameOptions) {
                // Use contains check first for exact match.
                if (ocrText.contains(teamName, ignoreCase = true) || teamName.contains(ocrText, ignoreCase = true)) {
                    if (teamName == selectedPreference) {
                        MessageLog.i(TAG, "[TRAINING_EVENT] Found exact match for \"$selectedPreference\" at option ${optionIndex + 1}.")
                        return optionIndex
                    }
                }

                // Check if this OCR text matches the user's preference.
                if (teamName == selectedPreference) {
                    val score = StringSimilarityServiceImpl(JaroWinklerStrategy()).score(ocrText.lowercase(), teamName.lowercase())

                    if (score > bestMatchScore) {
                        bestMatchScore = score
                        bestMatchIndex = optionIndex
                        MessageLog.i(TAG, "[TRAINING_EVENT] Option ${optionIndex + 1} matches preference with score: ${game.decimalFormat.format(score)}")
                    }
                }
            }
        }

        // If we found a good match, use it.
        if (bestMatchScore >= 0.8) {
            MessageLog.i(TAG, "[TRAINING_EVENT] Selected option ${bestMatchIndex + 1} based on similarity match (score: ${game.decimalFormat.format(bestMatchScore)}).")
            return bestMatchIndex
        }

        // Fallback to first option if no good match found.
        MessageLog.i(TAG, "[TRAINING_EVENT] No good match found for preference. Falling back to first option.")
        return 0
    }

    /**
     * Print a formatted summary of the training event and the selected option.
     *
     * @param eventTitle The detected event title from OCR.
     * @param ownerName The character or support card name that owns this event.
     * @param eventRewards List of reward strings for each option.
     * @param weights List of calculated weights for each option (can be null for override cases).
     * @param selectedOption The 0-based index of the selected option.
     * @param confidence The OCR matching confidence.
     */
    private fun printEventSummary(eventTitle: String, ownerName: String, eventRewards: ArrayList<String>, weights: List<Int>?, selectedOption: Int, confidence: Double) {
        val ownerInfo = if (ownerName.isNotEmpty()) " ($ownerName)" else ""
        MessageLog.i(TAG, "[TRAINING_EVENT] Event: \"$eventTitle\"$ownerInfo [Confidence: ${game.decimalFormat.format(confidence)}]")
        MessageLog.i(TAG, "[TRAINING_EVENT] Options:")
        
        eventRewards.forEachIndexed { index, reward ->
            // Create condensed reward summary (first line or truncated).
            val rewardLines = reward.split("\n").filter { it.isNotBlank() && !it.startsWith("---") }
            val condensed = if (rewardLines.size <= 3) {
                rewardLines.joinToString(", ")
            } else {
                rewardLines.take(3).joinToString(", ") + "..."
            }
            
            val weightInfo = if (weights != null && index < weights.size) " [Weight: ${weights[index]}]" else ""
            val selectionMarker = if (index == selectedOption) " <---- SELECTED" else ""
            MessageLog.i(TAG, "  Option ${index + 1}$weightInfo: $condensed$selectionMarker")
        }
        
        MessageLog.i(TAG, "[TRAINING_EVENT] Selected: Option ${selectedOption + 1}")
    }

    /**
     * Start text detection to determine what Training Event it is and the event rewards for each option.
     * It will then select the best option according to the user's preferences. By default, it will choose the first option.
     */
    fun handleTrainingEvent() {
        MessageLog.i(TAG, "\n********************")
        MessageLog.i(TAG, "[TRAINING_EVENT] Starting Training Event process on ${game.currentDate}.")

        // Double check if the bot is at the Main screen or not.
        if (game.checkMainScreen()) {
            MessageLog.i(TAG, "[TRAINING_EVENT] Bot is at the Main Screen. Ending the Training Event process.")
            MessageLog.i(TAG, "********************")
            return
        }

        val (eventRewards, confidence, eventTitle, characterOrSupportName) = trainingEventRecognizer.start()

        val regex = Regex("[a-zA-Z]+")
        var optionSelected = 0
        var specialEventHandled = false
        var isTutorialEvent = false
        var tutorialOptionCount = 0

        // Check for special event overrides first.
        val specialEventResult = checkSpecialEventOverride(eventTitle)

        // Handle Tutorial events by detecting the number of options on screen.
        if (eventTitle == "Tutorial") {
            isTutorialEvent = true
            // Detect the number of event options on the screen.
            val trainingOptionLocations: ArrayList<Point> = IconTrainingEventHorseshoe.findAll(game.imageUtils)
            tutorialOptionCount = trainingOptionLocations.size
            
            MessageLog.i(TAG, "[TRAINING_EVENT] Tutorial event detected for Unity Cup. Found $tutorialOptionCount option(s) on screen.")
            
            if (tutorialOptionCount == 2) {
                // If 2 options detected, select the last one (index 1).
                optionSelected = 1
                MessageLog.i(TAG, "[TRAINING_EVENT] Selecting last option (option 2) to dismiss Tutorial.")
            } else if (tutorialOptionCount == 5) {
                optionSelected = 4
                MessageLog.i(TAG, "[TRAINING_EVENT] Selecting last option (option 5) first, then will select first option to close.")
            } else {
                // Default to last option if count doesn't match expected values.
                optionSelected = if (tutorialOptionCount > 0) tutorialOptionCount - 1 else 0
                MessageLog.w(TAG, "[TRAINING_EVENT] Unexpected option count ($tutorialOptionCount). Selecting last option.")
            }
            
            specialEventHandled = true
        } else if (eventTitle == "A Team at Last") {
            // Handle "A Team at Last" Unity Cup event specially.
            MessageLog.i(TAG, "[TRAINING_EVENT] \"A Team at Last\" event detected for Unity Cup.")
            val trainingOptionLocations: ArrayList<Point> = IconTrainingEventHorseshoe.findAll(game.imageUtils)
            optionSelected = selectUnityCupTeamNameEvent(trainingOptionLocations)
            specialEventHandled = true
        } else if (specialEventResult != null) {
            val (selectedOptionIndex, _) = specialEventResult
            optionSelected = selectedOptionIndex
            
            // Ensure the selected option is within bounds.
            if (eventRewards.isNotEmpty() && optionSelected >= eventRewards.size) {
                MessageLog.w(TAG, "Selected special event option $optionSelected is out of bounds. Using last option.")
                optionSelected = eventRewards.size - 1
            }
            
            if (eventRewards.isNotEmpty() && optionSelected < eventRewards.size) {
                MessageLog.i(TAG, "[TRAINING_EVENT] Special event override applied: option ${optionSelected + 1}: \"${eventRewards[optionSelected]}\"")
            } else {
                MessageLog.i(TAG, "[TRAINING_EVENT] Special event override applied: option ${optionSelected + 1}")
            }
            specialEventHandled = true
        }

        if (eventRewards.isNotEmpty() && eventRewards[0] != "") {
            if (!specialEventHandled) {
                // Check for character or support event overrides.
                val characterOverride = checkCharacterEventOverride(characterOrSupportName, eventTitle)
                val supportOverride = checkSupportEventOverride(characterOrSupportName, eventTitle)
                
                if (characterOverride != null) {
                    optionSelected = characterOverride
                    
                    // Ensure the selected option is within bounds.
                    if (optionSelected >= eventRewards.size) {
                        MessageLog.w(TAG, "[WARNING] Selected character event option $optionSelected is out of bounds. Using last option.")
                        optionSelected = eventRewards.size - 1
                    }
                    
                    MessageLog.i(TAG, "[TRAINING_EVENT] Character event override applied.")
                    printEventSummary(eventTitle, characterOrSupportName, eventRewards, null, optionSelected, confidence)
                } else if (supportOverride != null) {
                    optionSelected = supportOverride
                    
                    // Ensure the selected option is within bounds.
                    if (optionSelected >= eventRewards.size) {
                        MessageLog.w(TAG, "[WARNING] Selected support event option $optionSelected is out of bounds. Using last option.")
                        optionSelected = eventRewards.size - 1
                    }
                    
                    MessageLog.i(TAG, "[TRAINING_EVENT] Support event override applied.")
                    printEventSummary(eventTitle, characterOrSupportName, eventRewards, null, optionSelected, confidence)
                } else {
                    // Initialize the List for normal event processing.
                    val selectionWeight = List(eventRewards.size) { 0 }.toMutableList()

                    // Sum up the stat gains with additional weight applied to stats that are prioritized.
                    eventRewards.forEach { reward ->
                        val formattedReward: List<String> = reward.split("\n")

                        formattedReward.forEach { line ->
                            val formattedLine: String = regex
                                .replace(line, "")
                                .replace("(", "")
                                .replace(")", "")
                                .trim()
                                .lowercase()

                            // Skip empty strings and divider lines (lines that are all dashes or start with 5 dashes).
                            if (line.trim().isEmpty() || line.trim().length >= 5 && line.trim().substring(0, 5).all { it == '-' }) {
                                return@forEach
                            }

                            MessageLog.i(TAG, "[TRAINING_EVENT] Original line is \"$line\".")
                            MessageLog.i(TAG, "[TRAINING_EVENT] Formatted line is \"$formattedLine\".")

                            var priorityStatCheck = false
                            if (line.lowercase().contains("can start dating")) {
                                MessageLog.i(TAG, "[TRAINING_EVENT] Adding weight for option #${optionSelected + 1} of 100 to unlock recreation/dating for this support.")
                                selectionWeight[optionSelected] += 100
                            } else if (line.lowercase().contains("event chain ended")) {
                                MessageLog.i(TAG, "[TRAINING_EVENT] Adding weight for option #${optionSelected + 1} of -200 for event chain ending.")
                                selectionWeight[optionSelected] += -300
                            } else if (line.lowercase().contains("(random)")) {
                                MessageLog.i(TAG, "[TRAINING_EVENT] Adding weight for option #${optionSelected + 1} of -10 for random reward.")
                                selectionWeight[optionSelected] += -10
                            } else if (line.lowercase().contains("randomly")) {
                                MessageLog.i(TAG, "[TRAINING_EVENT] Adding weight for option #${optionSelected + 1} of 50 for random options.")
                                selectionWeight[optionSelected] += 50
                            } else if (line.lowercase().contains("energy")) {
                                val finalEnergyValue = try {
                                    val energyValue = if (formattedLine.contains("/")) {
                                        val splits = formattedLine.split("/")
                                        var sum = 0
                                        for (split in splits) {
                                            sum += try {
                                                split.trim().toInt()
                                            } catch (_: NumberFormatException) {
                                                MessageLog.w(TAG, "[WARNING] Could not convert $formattedLine to a number for energy with a forward slash.")
                                                20
                                            }
                                        }
                                        sum
                                    } else {
                                        formattedLine.toInt()
                                    }

                                    if (enablePrioritizeEnergyOptions) {
                                        energyValue * 100
                                    } else {
                                        energyValue * 3
                                    }
                                } catch (_: NumberFormatException) {
                                    MessageLog.w(TAG, "[WARNING] Could not convert $formattedLine to a number for energy.")
                                    20
                                }
                                MessageLog.i(TAG, "[TRAINING_EVENT] Adding weight for option #${optionSelected + 1} of $finalEnergyValue for energy.")
                                selectionWeight[optionSelected] += finalEnergyValue
                            } else if (line.lowercase().contains("mood")) {
                                val moodWeight = if (formattedLine.contains("-")) -50 else 50
                                MessageLog.i(TAG, "[TRAINING-EVENT] Adding weight for option#${optionSelected + 1} of $moodWeight for ${if (moodWeight > 0) "positive" else "negative"} mood gain.")
                                selectionWeight[optionSelected] += moodWeight
                            } else if (line.lowercase().contains("bond")) {
                                val bondWeight = if (formattedLine.contains("-")) -20 else 20
                                MessageLog.i(TAG, "[TRAINING_EVENT] Adding weight for option #${optionSelected + 1} of $bondWeight for bond ${if (bondWeight > 0) "gain" else "loss"}.")
                                selectionWeight[optionSelected] += bondWeight
                            } else if (line.lowercase().contains("hint")) {
                                MessageLog.i(TAG, "[TRAINING_EVENT] Adding weight for option #${optionSelected + 1} of 25 for skill hint(s).")
                                selectionWeight[optionSelected] += 25
                            } else if (positiveStatuses.any { status -> line.contains(status) }) {
                                MessageLog.i(TAG, "[TRAINING_EVENT] Adding weight for option #${optionSelected + 1} of 25 for positive status effect.")
                                selectionWeight[optionSelected] += 25
                            } else if (negativeStatuses.any { status -> line.contains(status) }) {
                                MessageLog.i(TAG, "[TRAINING_EVENT] Adding weight for option #${optionSelected + 1} of -25 for negative status effect.")
                                selectionWeight[optionSelected] += -25
                            } else if (line.lowercase().contains("skill")) {
                                val finalSkillPoints = if (formattedLine.contains("/")) {
                                    val splits = formattedLine.split("/")
                                    var sum = 0
                                    for (split in splits) {
                                        sum += try {
                                            split.trim().toInt()
                                        } catch (_: NumberFormatException) {
                                            MessageLog.w(TAG, "[WARNING] Could not convert $formattedLine to a number for skill points with a forward slash.")
                                            10
                                        }
                                    }
                                    sum
                                } else {
                                    formattedLine.toInt()
                                }
                                MessageLog.i(TAG, "[TRAINING_EVENT] Adding weight for option #${optionSelected + 1} of $finalSkillPoints for skill points.")
                                selectionWeight[optionSelected] += finalSkillPoints
                            } else {
                                // Apply inflated weights to the prioritized stats based on their order.
                                game.training.statPrioritization.forEachIndexed { index, stat ->
                                    if (line.lowercase().contains(stat.name.lowercase())) {
                                        // Calculate weight bonus based on position (higher priority = higher bonus).
                                        val priorityBonus = when (index) {
                                            0 -> 50
                                            1 -> 40
                                            2 -> 30
                                            3 -> 20
                                            else -> 10
                                        }

                                        val finalStatValue = try {
                                            priorityStatCheck = true
                                            if (formattedLine.contains("/")) {
                                                val splits = formattedLine.split("/")
                                                var sum = 0
                                                for (split in splits) {
                                                    sum += try {
                                                        split.trim().toInt()
                                                    } catch (_: NumberFormatException) {
                                                        MessageLog.w(TAG, "[WARNING] Could not convert $formattedLine to a number for a priority stat with a forward slash.")
                                                        10
                                                    }
                                                }
                                                sum + priorityBonus
                                            } else {
                                                formattedLine.toInt() + priorityBonus
                                            }
                                        } catch (_: NumberFormatException) {
                                            MessageLog.w(TAG, "[WARNING] Could not convert $formattedLine to a number for a priority stat.")
                                            priorityStatCheck = false
                                            10
                                        }
                                        MessageLog.i(TAG, "[TRAINING_EVENT] Adding weight for option #${optionSelected + 1} of $finalStatValue for prioritized stat.")
                                        selectionWeight[optionSelected] += finalStatValue
                                    }
                                }

                                // Apply normal weights to the rest of the stats.
                                if (!priorityStatCheck) {
                                    val finalStatValue = try {
                                        if (formattedLine.contains("/")) {
                                            val splits = formattedLine.split("/")
                                            var sum = 0
                                            for (split in splits) {
                                                sum += try {
                                                    split.trim().toInt()
                                                } catch (_: NumberFormatException) {
                                                    MessageLog.w(TAG, "[WARNING] Could not convert $formattedLine to a number for non-prioritized stat with a forward slash.")
                                                    10
                                                }
                                            }
                                            sum
                                        } else {
                                            formattedLine.toInt()
                                        }
                                    } catch (_: NumberFormatException) {
                                        MessageLog.w(TAG, "[WARNING] Could not convert $formattedLine to a number for non-prioritized stat.")
                                        10
                                    }
                                    MessageLog.i(TAG, "[TRAINING_EVENT] Adding weight for option #${optionSelected + 1} of $finalStatValue for non-prioritized stat.")
                                    selectionWeight[optionSelected] += finalStatValue
                                }
                            }

                            MessageLog.i(TAG, "[TRAINING_EVENT] Final weight for option #${optionSelected + 1} is: ${selectionWeight[optionSelected]}.")
                        }

                        optionSelected++
                    }

                    // Select the best option that aligns with the stat prioritization made in the Training options.
                    val max: Int? = selectionWeight.maxOrNull()
                    optionSelected = if (max == null) {
                        0
                    } else {
                        selectionWeight.indexOf(max)
                    }

                    // Print the selection weights.
                    printEventSummary(eventTitle, characterOrSupportName, eventRewards, selectionWeight, optionSelected, confidence)
                }
            }

            // Print summary for special event overrides (character/support overrides are handled in their branches).
            if (specialEventHandled) {
                printEventSummary(eventTitle, characterOrSupportName, eventRewards, null, optionSelected, confidence)
            }
        } else {
            if (!specialEventHandled) {
                MessageLog.w(TAG, "First option will be selected since OCR failed to match the event title and no event rewards were found.")
                optionSelected = 0
            } else {
                MessageLog.w(TAG, "No event rewards were found, but special event override was applied.")
            }
        }

        // Wait briefly for the UI to fully render all option buttons.
        game.wait(0.1)
        
        val trainingOptionLocations: ArrayList<Point> = IconTrainingEventHorseshoe.findAll(game.imageUtils)
        
        // Handle Tutorial events specially.
        if (isTutorialEvent && trainingOptionLocations.isNotEmpty()) {
            if (tutorialOptionCount == 5) {
                // For 5-option Tutorial: select last option, wait, then select first option.
                val lastOptionLocation = try {
                    trainingOptionLocations[4]
                } catch (_: IndexOutOfBoundsException) {
                    trainingOptionLocations[trainingOptionLocations.size - 1]
                }
                
                game.tap(lastOptionLocation.x + game.imageUtils.relWidth(100), lastOptionLocation.y, IconTrainingEventHorseshoe.template.path)
                MessageLog.i(TAG, "[TRAINING_EVENT] Selected last option (option 5) for Tutorial to back out.")
                
                game.wait(1.0)
                
                // Find the training option locations again.
                val updatedTrainingOptionLocations: ArrayList<Point> = IconTrainingEventHorseshoe.findAll(game.imageUtils)
                if (updatedTrainingOptionLocations.isNotEmpty()) {
                    // Now select the first option to close.
                    val firstOptionLocation = updatedTrainingOptionLocations[0]
                    game.tap(firstOptionLocation.x + game.imageUtils.relWidth(100), firstOptionLocation.y, IconTrainingEventHorseshoe.template.path)
                    MessageLog.i(TAG, "[TRAINING_EVENT] Selected first option (option 1) to close Tutorial.")
                } else {
                    MessageLog.w(TAG, "[TRAINING_EVENT] Could not find training event options after waiting. Tutorial may have already closed.")
                }
            } else {
                // For 2-option Tutorial or other cases: select the determined option.
                val selectedLocation = try {
                    trainingOptionLocations[optionSelected]
                } catch (_: IndexOutOfBoundsException) {
                    trainingOptionLocations[trainingOptionLocations.size - 1]
                }
                
                game.tap(selectedLocation.x + game.imageUtils.relWidth(100), selectedLocation.y, IconTrainingEventHorseshoe.template.path)
                MessageLog.i(TAG, "[TRAINING_EVENT] Selected option ${optionSelected + 1} for Tutorial.")
            }
            
            // Wait 3 seconds after selecting Tutorial event options.
            MessageLog.i(TAG, "[TRAINING_EVENT] Waiting 3 seconds before handling Next/Close buttons for Tutorial.")
            game.wait(3.0)
            
            // Start searching for Next buttons and clicking them until Close button is found.
            var closeButtonFound = false
            var maxIterations = 20 // Prevent infinite loops.
            var iterationCount = 0
            
            while (!closeButtonFound && iterationCount < maxIterations) {
                iterationCount++
                
                // First check for Close button.
                if (ButtonClose.click(game.imageUtils)) {
                    MessageLog.i(TAG, "[TRAINING_EVENT] Close button found and clicked. Tutorial event handling complete.")
                    closeButtonFound = true
                    break
                }
                
                // If Close button not found, look for Next button.
                if (ButtonNext.click(game.imageUtils)) {
                    MessageLog.i(TAG, "[TRAINING_EVENT] Next button found and clicked. Waiting for next screen...")
                    game.wait(1.0)
                } else {
                    // Neither button found, wait a bit and try again.
                    MessageLog.d(TAG, "[TRAINING_EVENT] Neither Next nor Close button found. Waiting...")
                    game.wait(0.5)
                }
            }
            
            if (!closeButtonFound && iterationCount >= maxIterations) {
                MessageLog.w(TAG, "[TRAINING_EVENT] Reached maximum iterations while searching for Close button. Tutorial handling may be incomplete.")
            }
        } else {
            // Normal event handling.
            val selectedLocation: Point? = if (trainingOptionLocations.isNotEmpty()) {
                // Account for the situation where it could go out of bounds if the detected event options is incorrect and gives too many results.
                try {
                    trainingOptionLocations[optionSelected]
                } catch (_: IndexOutOfBoundsException) {
                    // Default to the first option.
                    trainingOptionLocations[0]
                }
            } else {
                IconTrainingEventHorseshoe.find(game.imageUtils, tries = 5).first
            }

            if (selectedLocation != null) {
                game.tap(selectedLocation.x + game.imageUtils.relWidth(100), selectedLocation.y, IconTrainingEventHorseshoe.template.path)
                
                // Check if this special event requires confirmation.
                if (specialEventResult != null) {
                    val (_, requiresConfirmation) = specialEventResult
                    if (requiresConfirmation) {
                        MessageLog.i(TAG, "[TRAINING_EVENT] Special event requires confirmation, waiting for dialog...")
                        
                        // Wait a moment for the confirmation dialog to appear.
                        game.wait(1.0)
                        
                        // Look for confirmation options and select the first one (Yes).
                        val confirmationLocations: ArrayList<Point> = IconTrainingEventHorseshoe.findAll(game.imageUtils)
                        if (confirmationLocations.isNotEmpty()) {
                            val confirmLocation = confirmationLocations[0]
                            game.tap(confirmLocation.x + game.imageUtils.relWidth(100), confirmLocation.y, IconTrainingEventHorseshoe.template.path)
                            MessageLog.i(TAG, "[TRAINING_EVENT] Special event confirmed.")
                        } else {
                            MessageLog.w(TAG, "Could not find confirmation options for special event.")
                        }
                    }
                }
            }
        }

        MessageLog.i(TAG, "[TRAINING_EVENT] Process to handle detected Training Event completed.")
        MessageLog.i(TAG, "********************")
    }
}