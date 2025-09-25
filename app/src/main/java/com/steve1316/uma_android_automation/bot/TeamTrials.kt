package com.steve1316.uma_android_automation.bot

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.graphics.Bitmap
import androidx.preference.PreferenceManager

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.utils.EventBus
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.opencv.core.Point
import java.text.DecimalFormat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.intArrayOf

enum class TeamTrialsLocation {
	MENU,
	SELECT_OPPONENT,
	MATCH_CONFIRMATION,
	MATCH_IN_PROGRESS,
	MATCH_END_EXTRA_REWARDS,
	MATCH_RESULTS,
	UNKNOWN,
}

fun findImage(game: Game, templateName: String, tries: Int = 10, region: IntArray = intArrayOf(0, 0, 0, 0)): Pair<Point?, Bitmap> {
	return game.imageUtils.findImage(templateName, tries, region)
}

class TeamTrials(private val game: Game, private val coroutineScope: CoroutineScope, private val eventBus: EventBus) {
	private val tag: String = "[${MainActivity.Companion.loggerTag}]TeamTrials"

	// =====================================================================
	// Functions for finding images.
	// =====================================================================
	fun checkBanner(tries: Int = 50, region: IntArray = game.imageUtils.regionTopHalf): Pair<Point?, Boolean> {
		// This function can be finicky since toasts for completing missions will cover it up.
		// TODO: Maybe check for a Mission toast?
		val point = findImage(game, "team_trials_banner", tries, region).first
		return Pair(point, point != null)
	}

	fun checkButtonTeamRace(tries: Int = 10, region: IntArray = game.imageUtils.regionMiddle): Pair<Point?, Boolean> {
		val point = findImage(game, "team_trials_team_race", tries, region).first
		return Pair(point, point != null)
	}

	fun checkLabelSelectOpponent(tries: Int = 10, region: IntArray = game.imageUtils.regionTopHalf): Pair<Point?, Boolean> {
		val point = findImage(game, "team_trials_select_opponent", tries, region).first
		return Pair(point, point != null)
	}

	fun checkLabelRaceWithHorseshoe(tries: Int = 10, region: IntArray = intArrayOf(0, 0, 0, 0)): Pair<Point?, Boolean> {
		val point = findImage(game, "team_trials_race_horseshoe", tries, region).first
		return Pair(point, point != null)
	}


	// =====================================================================
	// Functions for checking location state.
	// =====================================================================
	fun isAtMenu(): Boolean {
		// 1. Page with Team Trials and Edit Team buttons.
		return checkButtonTeamRace().second
	}

	fun isAtSelectOpponent(): Boolean {
		// 2. Page where you select the opponent.
		return checkLabelSelectOpponent().second
	}

	fun isAtMatchConfirmation(): Boolean {
		// 3. Page after selecting opponent.
		return checkLabelRaceWithHorseshoe().second
	}

	// 4. Items Selected dialog.

	// BEGIN LOOP
	fun isAtMatchInProgress(): Boolean {
		// 5. Page with the See Results button.
		return true
	}

	// 6. Win/Lose/Draw overlay.

	// END LOOP

	// New High Score overlay.

	fun isAtMatchEndExtraRewards(): Boolean {
		// 7.0. Page just before match results page.
		// Only shows up if extra rewards were obtained.
		// Similar to MatchResults page but is missing Race Again and Race Results buttons.
		return true
	}

	fun isAtMatchResults(): Boolean {
		// 7.1. Page with the Race Results button.
		// Clicking Race Again either pops up the RP warning dialog or
		// returns to the SelectOpponent page.
		return true
	}


	fun checkLocation(): TeamTrialsLocation {
		if (isAtMenu()) {
			return TeamTrialsLocation.MENU
		} else if (isAtSelectOpponent()) {
			return TeamTrialsLocation.SELECT_OPPONENT
		} else if (isAtMatchConfirmation()) {
			return TeamTrialsLocation.MATCH_CONFIRMATION
		} else if (isAtMatchInProgress()) {
			return TeamTrialsLocation.MATCH_IN_PROGRESS
		} else if (isAtMatchEndExtraRewards()) {
			return TeamTrialsLocation.MATCH_END_EXTRA_REWARDS
		} else if (isAtMatchResults()) {
			return TeamTrialsLocation.MATCH_RESULTS
		} else {
			return TeamTrialsLocation.UNKNOWN
		}
	}

	// =====================================================================
	// Functions for automatic navigation.
	// =====================================================================

	fun gotoTeamTrialsFromMenuBar(): Boolean {
		if (!game.checkMenuBar()) {
			return false
		}

		if (!game.gotoRaceMenu()) {
			return false
		}

		if (!game.gotoTeamTrialsMenu()) {
			return false
		}

		return isAtMenu()
	}

	fun gotoMenu(): Pair<TeamTrialsLocation, Boolean> {
		val loc = checkLocation()

		if (!game.checkMenuBar()) {
			// If we cant find the menu bar anywhere, we don't know how to get back to home.
			// Check if we can detect our current location to inform the calling function.
			return Pair(loc, false)
		}

		return when (loc) {
			TeamTrialsLocation.MENU -> Pair(TeamTrialsLocation.MENU, true)
			TeamTrialsLocation.SELECT_OPPONENT -> {
				game.findAndTapImage("back", tries = 10, region = game.imageUtils.regionBottomHalf)
				gotoMenu() // confirm location
			}
			TeamTrialsLocation.MATCH_CONFIRMATION -> {
				game.findAndTapImage("back", tries = 10, region = game.imageUtils.regionBottomHalf)
				gotoMenu()
			}
			else -> {
				gotoTeamTrialsFromMenuBar()
				gotoMenu()
			}
		}
	}

	fun gotoSelectOpponent(): Pair<TeamTrialsLocation, Boolean> {
		val loc = checkLocation()

		if (!game.checkMenuBar()) {
			// If we cant find the menu bar anywhere, we don't know how to get back to home.
			// Check if we can detect our current location to inform the calling function.
			return Pair(loc, false)
		}

		return when (loc) {
			TeamTrialsLocation.MENU -> {
				game.findAndTapImage("team_trials_team_race", tries = 10, region = game.imageUtils.regionBottomHalf)
				gotoSelectOpponent() // confirm location
			}
			TeamTrialsLocation.SELECT_OPPONENT -> Pair(TeamTrialsLocation.SELECT_OPPONENT, true)
			TeamTrialsLocation.MATCH_CONFIRMATION -> {
				game.findAndTapImage("back", tries = 10, region = game.imageUtils.regionBottomHalf)
				gotoSelectOpponent() // confirm location
			}
			// All other locations, we don't know how to navigate from.
			else -> Pair(loc, false)
		}
	}

	// =====================================================================
	// Helper functions for main loop.
	// =====================================================================

	fun handleMenu(): Boolean {
		if (gotoSelectOpponent().first != TeamTrialsLocation.MENU) {
			MessageLog.log("\n[WARN] mainloop:: Failed to navigate to MENU")
			return false
		}
		return true
	}

	fun handleSelectOpponent(): Boolean {
		// Select the opponent based on the position of the "Select Opponent" label
		// since the opponent buttons are all too dynamic for static image OCR.
		val opponentLocation: Point? = checkLabelSelectOpponent().first
		return if (opponentLocation != null) {
			game.tap(
				opponentLocation.x,
				opponentLocation.y + game.imageUtils.relHeight(200),
				"team_trials_select_opponent",
				taps = 3,
			)
			true
		} else {
			false
		}
	}

	fun handleMatchConfirmation(): Boolean {
		return game.findAndTapImage("next", tries = 10, region = game.imageUtils.regionBottomHalf)
	}

	fun handleMatchInProgress(): Boolean {
		return true
	}

	fun handleMatchEndExtraRewards(): Boolean {
		return true
	}

	fun handleMatchResults(): Boolean {
		return true
	}

	fun handleDialogs(): Boolean {
		return true
	}

	// =====================================================================
	// Main Loop
	// =====================================================================
	fun mainloop(): Boolean {
		val loc = checkLocation()

		val isMenuBarVisible = game.checkMenuBar()

		return when (loc) {
			TeamTrialsLocation.MENU -> handleMenu()
			TeamTrialsLocation.SELECT_OPPONENT -> handleSelectOpponent()
			TeamTrialsLocation.MATCH_CONFIRMATION -> handleMatchConfirmation()
			TeamTrialsLocation.MATCH_IN_PROGRESS -> handleMatchInProgress()
			TeamTrialsLocation.MATCH_END_EXTRA_REWARDS -> handleMatchEndExtraRewards()
			TeamTrialsLocation.MATCH_RESULTS -> handleMatchResults()
			TeamTrialsLocation.UNKNOWN -> {
				if (isMenuBarVisible) {
					gotoMenu().second
				//} else if (game.checkDialog()) {
				//	  handleDialogs()
				//} else if (game.checkDialogItemsSelected()) {
				//	  game.clickDialogButtonItemsSelectedRace()
				} else {
					MessageLog.log("\n[INFO] mainloop: Unknown location.")
					false
				}
			}
		}
	}
}