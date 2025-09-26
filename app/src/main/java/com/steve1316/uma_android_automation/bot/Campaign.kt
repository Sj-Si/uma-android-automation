package com.steve1316.uma_android_automation.bot

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.utils.MessageLog

/**
 * Base campaign class that contains all shared logic for campaign automation.
 * Campaign-specific logic should be implemented in subclasses by overriding the appropriate methods.
 * By default, URA Finale is handled by this base class.
 */
open class Campaign(val game: Game) {
	protected open val TAG: String = "Normal"

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

	/**
	 * Main automation loop that handles all shared logic.
	 */
	fun start() {
		while (true) {
			////////////////////////////////////////////////
			// Most bot operations start at the Main screen.
			if (game.checkMainScreen()) {
				var needToRace = false
				if (!game.encounteredRacingPopup) {
					// Refresh the stat values in memory.
					game.updateStatValueMapping()

					// If the required skill points has been reached, stop the bot.
					if (game.enableSkillPointCheck && game.imageUtils.determineSkillPoints() >= game.skillPointsRequired) {
						MessageLog.i(TAG, "[END] Bot has acquired the set amount of skill points. Exiting now...")
						game.notificationMessage = "Bot has acquired the set amount of skill points."
						break
					}

					// If force racing is enabled, skip all other activities and go straight to racing
					if (game.enableForceRacing) {
						MessageLog.i(TAG, "Force racing enabled - skipping all other activities and going straight to racing.")
						needToRace = true
					} else {
						// If the bot detected a injury, then rest.
						if (game.checkInjury()) {
							MessageLog.i(TAG, "A infirmary visit was attempted in order to heal an injury.")
							game.findAndTapImage("ok", region = game.imageUtils.regionMiddle)
							game.wait(3.0)
							game.skipRacing = false
						} else if (game.recoverMood()) {
							MessageLog.i(TAG, "Mood has recovered.")
							game.skipRacing = false
						} else if (!game.checkExtraRaceAvailability()) {
							MessageLog.i(TAG, "Training due to it not being an extra race day.")
							game.handleTraining()
							game.skipRacing = false
						} else {
							needToRace = true
						}
					}
				}

				 if (game.encounteredRacingPopup || needToRace) {
					MessageLog.i(TAG, "Racing by default.")
					if (!game.skipRacing && !handleRaceEvents()) {
						if (game.detectedMandatoryRaceCheck) {
							MessageLog.i(TAG, "[END] Stopping bot due to detection of Mandatory Race.")
							game.notificationMessage = "Stopping bot due to detection of Mandatory Race."
							break
						}
						game.findAndTapImage("back", tries = 1, region = game.imageUtils.regionBottomHalf)
						game.skipRacing = !game.enableForceRacing
						game.handleTraining()
					}
				}
			} else if (game.checkTrainingEventScreen()) {
				// If the bot is at the Training Event screen, that means there are selectable options for rewards.
				MessageLog.i(TAG, "Detected a Training Event on screen.")
				handleTrainingEvent()
				game.skipRacing = false
			} else if (game.handleInheritanceEvent()) {
				// If the bot is at the Inheritance screen, then accept the inheritance.
				MessageLog.i(TAG, "Accepted the Inheritance.")
				game.skipRacing = false
			} else if (game.checkMandatoryRacePrepScreen()) {
				MessageLog.i(TAG, "There is a Mandatory race to be run.")
				// If the bot is at the Main screen with the button to select a race visible, that means the bot needs to handle a mandatory race.
				if (!handleRaceEvents() && game.detectedMandatoryRaceCheck) {
					MessageLog.i(TAG, "[END] Stopping bot due to detection of Mandatory Race.")
					game.notificationMessage = "Stopping bot due to detection of Mandatory Race."
					break
				}
			} else if (game.checkRacingScreen()) {
				// If the bot is already at the Racing screen, then complete this standalone race.
				MessageLog.i(TAG, "There is a standalone race ready to be run.")
				game.handleStandaloneRace()
				game.skipRacing = false
			} else if (game.checkEndScreen()) {
				// Stop when the bot has reached the screen where it details the overall result of the run.
				MessageLog.i(TAG, "[END] Bot has reached the end of the run. Exiting now...")
				game.notificationMessage = "Bot has reached the end of the run"
				break
			} else if (checkCampaignSpecificConditions()) {
				MessageLog.i(TAG, "Campaign-specific checks complete.")
				game.skipRacing = false
				continue
			} else {
				MessageLog.i(TAG, "Did not detect the bot being at the following screens: Main, Training Event, Inheritance, Mandatory Race Preparation, Racing and Career End.")
			}

			// Various miscellaneous checks
			if (!game.performMiscChecks()) {
				break
			}
		}
	}
}