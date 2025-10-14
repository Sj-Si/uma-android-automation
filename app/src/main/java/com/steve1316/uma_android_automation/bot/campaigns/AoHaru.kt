package com.steve1316.uma_android_automation.bot.campaigns

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Campaign
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.utils.MessageLog
import org.opencv.core.Point
import com.steve1316.uma_android_automation.utils.GameUtils
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.components.*
import com.steve1316.uma_android_automation.components.ComponentUtils

import kotlinx.coroutines.*

class AoHaru(game: Game, coroutineScope: CoroutineScope) : Campaign(game, coroutineScope) {
	override val TAG: String = "AoHaru"
	private var tutorialChances = 3
	private var aoHaruRaceFirstTime: Boolean = true

	override fun handleTrainingEvent() {
		handleTrainingEventAoHaru()
	}

	override fun handleRaceEvents(): Boolean {
		// Check for Ao Haru specific race screens first.
		if (aoHaruRaceFirstTime && game.imageUtils.confirmLocation("aoharu_set_initial_team", tries = 1)) {
			ButtonClose.click(imageUtils=game.imageUtils)
			handleRaceEventsAoHaru()
			return true
		} else if (game.imageUtils.confirmLocation("aoharu_race", tries = 1)) {
			handleRaceEventsAoHaru()
			return true
		}

		// Fall back to the regular race handling logic.
		return super.handleRaceEvents()
	}

	override fun checkCampaignSpecificConditions(): Boolean {
		return false
	}

	/**
	 * Checks for Ao Haru's tutorial first before handling a Training Event.
	 */
	private fun handleTrainingEventAoHaru() {
		if (tutorialChances > 0) {
			if (game.imageUtils.confirmLocation("aoharu_tutorial", tries = 2)) {
				MessageLog.i(TAG, "[AOHARU] Detected tutorial for Ao Haru. Closing it now...")
				
				// If the tutorial is detected, select the second option to close it.
				val trainingOptionLocations: ArrayList<Point> = game.imageUtils.findAll("career/training_event_active")
				ComponentUtils.tap(
                    trainingOptionLocations[1].x,
                    trainingOptionLocations[1].y,
                    "career/training_event_active"
                )
				tutorialChances = 0
			} else {
				tutorialChances -= 1
				game.handleTrainingEvent()
			}
		} else {
			game.handleTrainingEvent()
		}
	}
	
	/**
	 * Handles the Ao Haru's race event.
	 */
	private fun handleRaceEventsAoHaru() {
		MessageLog.i(TAG, "[AOHARU] Starting process to handle Ao Haru race...")
		aoHaruRaceFirstTime = false
		
		// Head to the next screen with the 3 racing options.
		ButtonAoHaruRace.click(imageUtils=game.imageUtils)
		GameUtils.wait(7.0, imageUtils=game.imageUtils)
		
		if (ButtonAoHaruFinalRace.click(imageUtils=game.imageUtils, tries=10)) {
			MessageLog.i(TAG, "[AOHARU] Final race detected. Racing it now...")
			ButtonAoHaruSelectRace.click(imageUtils=game.imageUtils)
		} else {
			// Run the first option if it has more than 3 double circles and if not, run the second option.
			var racingOptions = game.imageUtils.findAll("aoharu/aoharu_race_option")
			ComponentUtils.tap(
                racingOptions[0].x,
                racingOptions[0].y,
                "aoharu/aoharu_race_option"
            )
			
            ButtonAoHaruSelectRace.click(imageUtils=game.imageUtils, tries=10)
			GameUtils.wait(2.0, imageUtils=game.imageUtils)
			
			val doubleCircles = game.imageUtils.findAll("race_prediction_double_circle")
			if (doubleCircles.size >= 3) {
				MessageLog.i(TAG, "[AOHARU] First race has sufficient double circle predictions. Selecting it now...")
				ButtonAoHaruSelectRace.click(imageUtils=game.imageUtils, tries=10)
			} else {
				MessageLog.i(TAG, "[AOHARU] First race did not have the sufficient double circle predictions. Selecting the 2nd race now...")
				ButtonCancel.click(imageUtils=game.imageUtils, tries=10)
				GameUtils.wait(1.0, imageUtils=game.imageUtils)
				
				racingOptions = game.imageUtils.findAll("aoharu/aoharu_race_option")
				ComponentUtils.tap(
                    racingOptions[1].x,
                    racingOptions[1].y,
                    "aoharu/aoharu_race_option"
                )

                ButtonAoHaruSelectRace.click(imageUtils=game.imageUtils, tries=30)
                ButtonAoHaruSelectRace.click(imageUtils=game.imageUtils, tries=30)
                ButtonAoHaruSelectRace.click(imageUtils=game.imageUtils, tries=10)
			}
		}
		
		GameUtils.wait(7.0, imageUtils=game.imageUtils)
		
		// Now run the race and skip to the end.
		ButtonAoHaruRunRace.click(imageUtils=game.imageUtils, tries=30)
		GameUtils.wait(1.0, imageUtils=game.imageUtils)
        ButtonSkip.click(imageUtils=game.imageUtils, tries=30)
		GameUtils.wait(3.0, imageUtils=game.imageUtils)
		
        ButtonRaceEnd.click(imageUtils=game.imageUtils, tries=30)
		GameUtils.wait(1.0, imageUtils=game.imageUtils)
		ButtonRaceEnd.click(imageUtils=game.imageUtils, tries=30)
	}
}