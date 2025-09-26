package com.steve1316.uma_android_automation.bot.campaigns

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Campaign
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.utils.MessageLog
import org.opencv.core.Point
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.utils.GameUtils
class AoHaru(game: Game) : Campaign(game) {
	override val TAG: String = "AoHaru"
	private var tutorialChances = 3
	private var aoHaruRaceFirstTime: Boolean = true

	override fun handleTrainingEvent() {
		handleTrainingEventAoHaru()
	}

	override fun handleRaceEvents(): Boolean {
		// Check for Ao Haru specific race screens first.
		if (aoHaruRaceFirstTime && ImageUtils.confirmLocation("aoharu_set_initial_team", tries = 1)) {
			game.findAndTapImage("race_accept_trophy")
			handleRaceEventsAoHaru()
			return true
		} else if (ImageUtils.confirmLocation("aoharu_race", tries = 1)) {
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
			if (ImageUtils.confirmLocation("aoharu_tutorial", tries = 2)) {
				MessageLog.i(TAG, "[AOHARU] Detected tutorial for Ao Haru. Closing it now...")
				
				// If the tutorial is detected, select the second option to close it.
				val trainingOptionLocations: ArrayList<Point> = ImageUtils.findAll("training_event_active")
				game.gestureUtils.tap(trainingOptionLocations[1].x, trainingOptionLocations[1].y, "training_event_active")
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
		game.findAndTapImage("aoharu_race")
		GameUtils.wait(7.0)
		
		if (game.findAndTapImage("aoharu_final_race", tries = 10)) {
			MessageLog.i(TAG, "[AOHARU] Final race detected. Racing it now...")
			game.findAndTapImage("aoharu_select_race")
		} else {
			// Run the first option if it has more than 3 double circles and if not, run the second option.
			var racingOptions = ImageUtils.findAll("aoharu_race_option")
			game.gestureUtils.tap(racingOptions[0].x, racingOptions[0].y, "aoharu_race_option")
			
			game.findAndTapImage("aoharu_select_race", tries = 10)
			GameUtils.wait(2.0)
			
			val doubleCircles = ImageUtils.findAll("race_prediction_double_circle")
			if (doubleCircles.size >= 3) {
				MessageLog.i(TAG, "[AOHARU] First race has sufficient double circle predictions. Selecting it now...")
				game.findAndTapImage("aoharu_select_race", tries = 10)
			} else {
				MessageLog.i(TAG, "[AOHARU] First race did not have the sufficient double circle predictions. Selecting the 2nd race now...")
				game.findAndTapImage("cancel", tries = 10)
				GameUtils.wait(1.0)
				
				racingOptions = ImageUtils.findAll("aoharu_race_option")
				game.gestureUtils.tap(racingOptions[1].x, racingOptions[1].y, "aoharu_race_option")
				
				game.findAndTapImage("aoharu_select_race", tries = 30)
				game.findAndTapImage("aoharu_select_race", tries = 30)
				game.findAndTapImage("aoharu_select_race", tries = 10)
			}
		}
		
		GameUtils.wait(7.0)
		
		// Now run the race and skip to the end.
		game.findAndTapImage("aoharu_run_race", tries = 30)
		GameUtils.wait(1.0)
		game.findAndTapImage("race_skip_manual", tries = 30)
		GameUtils.wait(3.0)
		
		game.findAndTapImage("race_end", tries = 30)
		GameUtils.wait(1.0)
		game.findAndTapImage("race_end", tries = 30)
	}
}