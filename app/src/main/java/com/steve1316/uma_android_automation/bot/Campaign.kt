package com.steve1316.uma_android_automation.bot

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.EventBus
import com.steve1316.uma_android_automation.utils.UserConfig
import com.steve1316.uma_android_automation.utils.GameUtils
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.utils.AppEvent
import com.steve1316.uma_android_automation.utils.Screen
import com.steve1316.uma_android_automation.dialog.DialogListener
import com.steve1316.uma_android_automation.components.*

import org.opencv.core.Point
import com.steve1316.uma_android_automation.utils.BotService

import com.steve1316.uma_android_automation.utils.types.Aptitude
import com.steve1316.uma_android_automation.utils.types.Mood
import com.steve1316.uma_android_automation.utils.types.TrackSurface
import com.steve1316.uma_android_automation.utils.types.TrackDistance
import com.steve1316.uma_android_automation.utils.types.RunningStyle
import com.steve1316.uma_android_automation.utils.types.Date

import kotlinx.coroutines.*

/**
 * Base campaign class that contains all shared logic for campaign automation.
 * Campaign-specific logic should be implemented in subclasses by overriding the appropriate methods.
 * By default, URA Finale is handled by this base class.
 */
open class Campaign(val game: Game, val coroutineScope: CoroutineScope) {
	protected open val TAG: String = "Campaign"

    protected var bHasSetQuickMode: Boolean = false
    protected var bHasSetSkip: Boolean = false
    protected var bHasReadStats: Boolean = false

    protected var date: Date = Date(1, "Early", 1, 1)
    protected var preferredDistance: TrackDistance = TrackDistance.MEDIUM
    protected var preferredTrackSurface: TrackSurface = TrackSurface.TURF
    protected var preferredRunningStyle: RunningStyle = RunningStyle.FRONT_RUNNER
    protected var skillPoints: Int = 0

    init {
        EventBus.subscribe<AppEvent.DialogEvent>(coroutineScope) { event ->
            MessageLog.d(TAG, "DialogEvent (${event.dialog?.name})")
        }
    }

	/**
	 * Campaign-specific training event handling.
	 */
	open fun handleTrainingEvent() {
		game.handleTrainingEvent()
	}

	/**
	 * Campaign-specific race event handling.
	 */
	open fun handleRaceEvents(): Boolean {
		return game.handleRaceEvents()
	}

	/**
	 * Campaign-specific checks for special screens or conditions.
	 */
	open fun checkCampaignSpecificConditions(): Boolean {
		return false
	}

    fun handleDialogs(): Boolean {
        val dialog = DialogListener.getDialog(game.imageUtils)
        if (dialog == null) {
            return false
        }

        when (dialog.name) {
            "career_complete" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: career_complete")
                dialog.close(imageUtils=game.imageUtils)
            }
            "concert_skip_confirmation" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: concert_skip_confirmation")
                Checkbox.click(imageUtils=game.imageUtils)
                dialog.close(imageUtils=game.imageUtils)
            }
            "follow_trainer" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: follow_trainer")
                dialog.close(imageUtils=game.imageUtils)
            }
            "infirmary" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: infirmary")
                Checkbox.click(imageUtils=game.imageUtils)
                dialog.ok(imageUtils=game.imageUtils)
            }
            "insufficient_fans" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: insufficient_fans")
                game.encounteredRacingPopup = true
                game.skipRacing = false
                dialog.ok(imageUtils=game.imageUtils, tries=5)
            }
            "menu" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: menu")
                dialog.close(imageUtils=game.imageUtils)
            }
            "quick_mode_settings" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: quick_mode_settings")
                RadioCareerQuickShortenAllEvents.click(imageUtils=game.imageUtils)
                dialog.ok(imageUtils=game.imageUtils)
                bHasSetQuickMode = true
            }
            "race_details_career" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: race_details_career")
                dialog.ok(imageUtils=game.imageUtils)
            }
            "race_playback" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: race_playback")
                RadioPortrait.click(imageUtils=game.imageUtils)
                Checkbox.click(imageUtils=game.imageUtils)
                dialog.ok(imageUtils=game.imageUtils)
            }
            "race_recommendations" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: race_recommendations")
                ButtonRaceRecommendationsForgeYourOwnPath.click(imageUtils=game.imageUtils)
                Checkbox.click(imageUtils=game.imageUtils)
                dialog.ok(imageUtils=game.imageUtils)
            }
            "recreation" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: recreation")
                Checkbox.click(imageUtils=game.imageUtils)
                dialog.ok(imageUtils=game.imageUtils)
            }
            "rest" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: rest")
                Checkbox.click(imageUtils=game.imageUtils)
                dialog.ok(imageUtils=game.imageUtils)
            }
            "rest_and_recreation" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: rest_and_recreation")
                // Does not have checkbox like the others.
                // TODO: Go through menu to set this option.
                dialog.ok(imageUtils=game.imageUtils)
            }
            "song_acquired" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: song_acquired")
                dialog.close(imageUtils=game.imageUtils)
            }
            "strategy" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: strategy")
                // TODO: Click appropriate strategy.

                dialog.ok(imageUtils=game.imageUtils)
            }
            "story_unlocked" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: story_unlocked")
                dialog.close(imageUtils=game.imageUtils)
            }
            "trophy_won" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: trophy_won")
                dialog.close(imageUtils=game.imageUtils)
            }
            "try_again" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: try_again")
                // TODO: Add user config to set clock threshold.
                dialog.ok(imageUtils=game.imageUtils)
            }
            "umamusume_details" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: umamusume_details")
                preferredDistance = game.imageUtils.determinePreferredDistance()
                MessageLog.i(TAG, "[STATS] Preferred distance set to ${preferredDistance}.")
                preferredTrackSurface = game.imageUtils.determinePreferredTrackSurface()
                MessageLog.i(TAG, "[STATS] Preferred track surface set to ${preferredTrackSurface}")
                preferredRunningStyle = game.imageUtils.determinePreferredRunningStyle()
                MessageLog.i(TAG, "[STATS] Preferred running style set to ${preferredRunningStyle}")
                dialog.close(imageUtils=game.imageUtils)
                bHasReadStats = true
            }
            "view_story" -> {
                MessageLog.d(TAG, "Dialog Event: Campaign: view_story")
                RadioPortrait.click(imageUtils=game.imageUtils)
                RadioVoiceOff.click(imageUtils=game.imageUtils)
                dialog.close(imageUtils=game.imageUtils)
            }
            else -> {
                MessageLog.w(TAG, "Dialog Event: Campaign: UNKNOWN")
                return false
            }
        }

        return true
    }
    
    fun checkMandatoryRaceDay(): Boolean {
        MessageLog.i(TAG, "Checking if at home screen and it is a mandatory race day.")
        return ButtonCareerHomeRaceDayRace.check(imageUtils=game.imageUtils)
    }

    /**
	 * Checks if the day number is odd to be eligible to run an extra race, excluding Summer where extra racing is not allowed.
	 *
	 * @return True if the day number is odd. Otherwise false.
	 */
	fun checkExtraRaceDay(): Boolean {
		val dayNumber = game.imageUtils.determineDayForExtraRace()
		MessageLog.i(TAG, "Current remaining number of days before the next mandatory race: $dayNumber.")

		// If the setting to force racing extra races is enabled, always return true.
		if (UserConfig.config.bEnableForceRacing) return true

		return UserConfig.config.bEnableFarmingFans &&
            dayNumber % UserConfig.config.daysToRunExtraRaces == 0 &&
            !game.raceRepeatWarningCheck &&
            !ButtonRaceSelectExtraLocked.check(imageUtils=game.imageUtils)
            !ButtonRaceSelectExtraLockedUmaFinals.check(imageUtils=game.imageUtils)
            !ButtonRestAndRecreation.check(imageUtils=game.imageUtils)
	}

	fun checkInjury(): Boolean {
        val (infirmaryLocation, _) = ButtonInfirmary.find(imageUtils=game.imageUtils)
		return if (infirmaryLocation != null && game.imageUtils.checkColorAtCoordinates(
				infirmaryLocation.x.toInt(),
				infirmaryLocation.y.toInt() + 15,
				intArrayOf(151, 105, 243),
				10
			)) {
			if (ButtonInfirmary.click(imageUtils=game.imageUtils)) {
				GameUtils.wait(0.3)
				if (ButtonInfirmary.confirm(imageUtils=game.imageUtils)) {
					MessageLog.i(TAG, "Injury detected and attempted to heal.")
					true
				} else {
					false
				}
			} else {
				MessageLog.w(TAG, "Injury detected but attempt to rest failed.")
				false
			}
		} else {
			MessageLog.i(TAG, "No injury detected.")
			false
		}
    }

    fun checkMood(): Mood {
        return when {
            MoodAwful.check(imageUtils=game.imageUtils)-> Mood.AWFUL
            MoodBad.check(imageUtils=game.imageUtils) -> Mood.BAD
            MoodNormal.check(imageUtils=game.imageUtils) -> Mood.NORMAL
            MoodGood.check(imageUtils=game.imageUtils) -> Mood.GOOD
            MoodGreat.check(imageUtils=game.imageUtils) -> Mood.GREAT
            else -> Mood.AWFUL
        }
    }

    fun checkDate(): Date {
        MessageLog.d(TAG, "Checking date...")
        val dateString = game.imageUtils.determineDayNumber()
        date = game.textDetection.determineDateFromString(dateString)
        MessageLog.d(TAG, "Current Date: ${date}")
        return date
    }

    fun checkSkillPoints(): Int {
        MessageLog.d(TAG, "Checking skill points...")
        skillPoints = game.imageUtils.determineSkillPoints()
        MessageLog.d(TAG, "Current Skill Points: ${skillPoints}")
        return skillPoints
    }

    fun selectRaceFromList(onlyG1: Boolean = false): Boolean {
        val (statusLocation, _) = ButtonRaceListFullStats.find(imageUtils=game.imageUtils)
        if (statusLocation == null) {
            MessageLog.e(TAG, "Failed to find list of races.")
            return false
        }

        game.gestureUtils.swipe(
            statusLocation.x.toFloat(),
            statusLocation.y.toFloat() + 300,
            statusLocation.x.toFloat(),
            statusLocation.y.toFloat() + 888,
        )
        GameUtils.wait(1.0)

        // Now determine the best extra race with the following parameters: highest fans and double star prediction.
        // First find the fans of only the extra races on the screen that match the double star prediction. Read only 3 extra races.
        var count = 0
        val maxCount = game.imageUtils.findAll("career/race_selection_fans", region = Screen.BOTTOM_HALF).size
        if (maxCount == 0) {
            MessageLog.w(TAG, "Failed to find any races.")
            return false
        } else {
            MessageLog.i(TAG, "[RACE] There are $maxCount extra race options currently on screen.")
        }
        val listOfFans = mutableListOf<Int>()
        val extraRaceLocation = mutableListOf<Point>()
        val doublePredictionLocations = game.imageUtils.findAll("race_extra_double_prediction")
        if (doublePredictionLocations.size == 1) {
            MessageLog.i(TAG, "[RACE] There is only one race with double predictions. Selecting...")
            ComponentUtils.tap(
                doublePredictionLocations[0].x,
                doublePredictionLocations[0].y,
                "race_extra_double_prediction",
                ignoreWaiting = true
            )
        } else {
            val (sourceBitmap, templateBitmap) = game.imageUtils.getBitmaps("race_extra_double_prediction")
            val listOfRaces: ArrayList<ImageUtils.RaceDetails> = arrayListOf()
            while (count < maxCount) {
                // Save the location of the selected extra race.
                val (selectedExtraRace, _) = IconSelectionBracket.find(imageUtils=game.imageUtils)
                if (selectedExtraRace == null) {
                    MessageLog.e(TAG, "Unable to find the location of the selected extra race. Canceling the racing process and doing something else.")
                    break
                }
                extraRaceLocation.add(selectedExtraRace)

                // Determine its fan gain and save it.
                val raceDetails: ImageUtils.RaceDetails = game.imageUtils.determineExtraRaceFans(extraRaceLocation[count], sourceBitmap, templateBitmap!!, forceRacing = UserConfig.config.bEnableForceRacing)
                listOfRaces.add(raceDetails)
                if (count == 0 && raceDetails.fans == -1) {
                    // If the fans were unable to be fetched or the race does not have double predictions for the first attempt, skip racing altogether.
                    listOfFans.add(raceDetails.fans)
                    break
                }
                if (onlyG1 && !raceDetails.isG1) {
                    listOfFans.add(raceDetails.fans)
                    break
                }
                listOfFans.add(raceDetails.fans)

                // Select the next extra race.
                if (count + 1 < maxCount) {
                    if (game.imageUtils.isTablet) {
                        ComponentUtils.tap(
                            game.imageUtils.relX(extraRaceLocation[count].x, (-100 * 1.36).toInt()).toDouble(),
                            game.imageUtils.relY(extraRaceLocation[count].y, (150 * 1.50).toInt()).toDouble(),
                            "selection_bracket",
                            ignoreWaiting = true,
                        )
                    } else {
                        ComponentUtils.tap(
                            game.imageUtils.relX(extraRaceLocation[count].x, -100).toDouble(),
                            game.imageUtils.relY(extraRaceLocation[count].y, 150).toDouble(),
                            "selection_bracket",
                            ignoreWaiting = true,
                        )
                    }
                }

                GameUtils.wait(0.5)

                count++
            }

            val fansList = listOfRaces.joinToString(", ") { it.fans.toString() }
            MessageLog.i(TAG, "[RACE] Number of fans detected for each extra race are: $fansList")

            // Next determine the maximum fans and select the extra race.
            val maxFans: Int? = listOfFans.maxOrNull()
            if (maxFans != null) {
                if (maxFans == -1) {
                    MessageLog.w(TAG, "Max fans was returned as -1. Canceling the racing process and doing something else.")
                    return false
                }

                // Get the index of the maximum fans or the one with the double predictions if available when force racing is enabled.
                val index = if (!UserConfig.config.bEnableForceRacing) {
                    listOfFans.indexOf(maxFans)
                } else {
                    // When force racing is enabled, prioritize races with double predictions.
                    val doublePredictionIndex = listOfRaces.indexOfFirst { it.hasDoublePredictions }
                    if (doublePredictionIndex != -1) {
                        MessageLog.i(TAG, "[RACE] Force racing enabled - selecting race with double predictions.")
                        doublePredictionIndex
                    } else {
                        // Fall back to the race with maximum fans if no double predictions found
                        MessageLog.i(TAG, "[RACE] Force racing enabled but no double predictions found - falling back to race with maximum fans.")
                        listOfFans.indexOf(maxFans)
                    }
                }

                MessageLog.i(TAG, "[RACE] Selecting the extra race at option #${index + 1}.")

                // Select the extra race that matches the double star prediction and the most fan gain.
                ComponentUtils.tap(
                    extraRaceLocation[index].x - game.imageUtils.relWidth((100 * 1.36).toInt()),
                    extraRaceLocation[index].y - game.imageUtils.relHeight(70),
                    "selection_bracket",
                    ignoreWaiting = true
                )
            } else if (extraRaceLocation.isNotEmpty()) {
                // If no maximum is determined, select the very first extra race.
                MessageLog.i(TAG, "[RACE] Selecting the first extra race on the list by default.")
                ComponentUtils.tap(
                    extraRaceLocation[0].x - game.imageUtils.relWidth((100 * 1.36).toInt()),
                    extraRaceLocation[0].y - game.imageUtils.relHeight(70),
                    "selection_bracket",
                    ignoreWaiting = true
                )
            } else {
                MessageLog.w(TAG, "Failed to detect any races.")
                return false
            }
        }

        return true
    }

    fun selectRace(onlyG1: Boolean = false): Boolean {
        if (!ButtonRaces.click(imageUtils=game.imageUtils)) {
            MessageLog.e(TAG, "handleRace:: Failed to click Races button.")
            return false
        }

        if (!selectRaceFromList(onlyG1=onlyG1)) {
            return false
        }

        // Race button at bottom of list of races.
        // This opens the Race Details dialog. The rest is handled from there.
        return ButtonRace.click(imageUtils=game.imageUtils)
    }

    fun handleTraining() {
        MessageLog.i(TAG, "[TRAINING] Starting Training process...")

		// Enter the Training screen.
		if (ButtonTraining.click(imageUtils=game.imageUtils)) {
			// Acquire the percentages and stat gains for each training.
			GameUtils.wait(0.5)
			game.analyzeTrainings()

			if (game.trainingMap.isEmpty()) {
				MessageLog.i(TAG, "[TRAINING] Backing out of Training and returning on the Main screen.")
                ButtonBack.click(imageUtils=game.imageUtils)
				GameUtils.wait(0.5)

				if (ScreenCareerHome.check(imageUtils=game.imageUtils)) {
					MessageLog.i(TAG, "[TRAINING] Will recover energy due to either failure chance was high enough to do so or no failure chances were detected via OCR.")
					game.recoverEnergy()
				} else {
					MessageLog.e(TAG, "Failed to return to ScreenCareerHome.")
				}
			} else {
				// Now select the training option with the highest weight.
				game.executeTraining()

				game.firstTrainingCheck = false
			}

			game.raceRepeatWarningCheck = false
			MessageLog.i(TAG, "[TRAINING] Training process completed.")
		} else {
			MessageLog.e(TAG, "Cannot start the Training process. Moving on...")
		}
    }

    fun handleMainScreen(): Boolean {
        checkDate()
        checkSkillPoints()

        if (!bHasSetSkip) {
            // Set the skip speed to its max.
            ButtonCareerSkipOff.click(imageUtils=game.imageUtils)
            ButtonCareerSkip1.click(imageUtils=game.imageUtils)
            if (ButtonCareerSkip2.check(imageUtils=game.imageUtils)) {
                bHasSetSkip = true
            }
        } else if (!bHasSetQuickMode) {
            ButtonCareerQuick.click(imageUtils=game.imageUtils)
            ButtonCareerQuickEnabled.click(imageUtils=game.imageUtils)
        } else if (!bHasReadStats) {
            MessageLog.w(TAG, "bHasReadStats = $bHasReadStats")
            ButtonHomeFullStats.click(imageUtils=game.imageUtils)
        } else if (
            UserConfig.config.bEnableSkillPointCheck &&
            skillPoints >= UserConfig.config.skillPointCheckThreshold
        ) {
            MessageLog.i(TAG, "Reached configured skill point threshold. Stopping bot...")
            return false
        } else if (UserConfig.config.bEnableForceRacing) {
            selectRace()
        } else if (checkMandatoryRaceDay()) {
            selectRace()
        } else if (
            IconCriteriaPill.check(imageUtils=game.imageUtils) &&
            IconFans.check(imageUtils=game.imageUtils)
        ) {
            // Need more fans (or maiden race)
            selectRace()
        } else if (IconProgressFans.check(imageUtils=game.imageUtils)) {
            // Need more fans for goal
            selectRace()
        } else if (IconCriteriaRaces.check(imageUtils=game.imageUtils)) {
            // Need more G1 wins for goal
            selectRace(onlyG1=true)
        } else if (checkExtraRaceDay()) {
            selectRace()
        } else if (checkInjury()) {
            ButtonInfirmary.click(imageUtils=game.imageUtils)
        } else if (checkMood() < Mood.NORMAL) {
            if (!ButtonRecreation.click(imageUtils=game.imageUtils)) {
                ButtonRestAndRecreation.click(imageUtils=game.imageUtils)
            }
        } else {
            handleTraining()
        }

        return true
    }

    fun handleRaceScreen(): Boolean {
        return true
    }

	/**
	 * Main automation loop that handles all shared logic.
	 */
	fun start() {
		while (true) {
            // Try to handle any campaign and game dialogs.
            handleDialogs()

            game.handleDialogs()

            if (ScreenCareerHome.check(imageUtils=game.imageUtils)) {
                if (!handleMainScreen()) {
                    break
                }
            } else if (ScreenRace.check(imageUtils=game.imageUtils)) {
                game.handleStandaloneRace()
            } else if (ScreenCareerTrainingEvent.check(imageUtils=game.imageUtils)) {
                handleTrainingEvent()
            } else if (ButtonInheritance.click(imageUtils=game.imageUtils)) {
                MessageLog.d(TAG, "Handled inheritance event.")
            } else if (ScreenCareerRaceSelectMandatoryGoal.check(imageUtils=game.imageUtils)) {
                selectRace()
            } else if (ButtonCompleteCareer.check(imageUtils=game.imageUtils)) {
                // click skills button
                // buy skills
            } else if (ButtonCraneGame.check(imageUtils=game.imageUtils)) {
                if (UserConfig.config.bEnableSkipCraneGame) {
                    MessageLog.d(TAG, "Crane Game event detected. Auto failing since skip crane game setting is enabled.")
                    ButtonCraneGame.click(imageUtils=game.imageUtils)
                } else {
                    MessageLog.i(TAG, "Crane Game event detected. Stopping bot...")
                    game.notificationMessage = "Crane Game event detected. Stopping bot..."
                    break
                }
            } else if (
                ButtonOk.check(imageUtils=game.imageUtils) &&
                LabelOrdinaryCuties.check(imageUtils=game.imageUtils)
            ) {
                MessageLog.d(TAG, "Crane Game event completed.")
                ButtonOk.click(imageUtils=game.imageUtils)
            } else if (ButtonRaceEnd.click(imageUtils=game.imageUtils)) {
                MessageLog.d(TAG, "Ended leftover race.")
            } else if (LabelConnectionError.check(imageUtils=game.imageUtils)) {
                MessageLog.i(TAG, "Stopping bot due to connection error...")
                game.notificationMessage = "Stopping bot due to connection error..."
                break
            } else if (!BotService.isRunning) {
                throw InterruptedException()
            } else {
                // Click any NEXT buttons that pop up.
                MessageLog.d(TAG, "Fallthrough. Clicking NEXT if it exists.")
                ButtonNext.click(imageUtils=game.imageUtils)
                //MessageLog.d(TAG, "No conditions caught.")
            }
		}
	}
}