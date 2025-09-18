package com.steve1316.uma_android_automation.bot.campaigns

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import org.opencv.core.Point

import kotlin.system.measureTimeMillis

class DailyTasks(val game: Game) {
    private val dailyTasksTag: String = "[${MainActivity.loggerTag}]DailyTasks"
    
    private var teamTrialsComplete: Boolean = false
    private var dailyRacesComplete: Boolean = false
    private var legendRacesComplete: Boolean = false
    private var dailyMissionsCollected: Boolean = false

    fun start() {
        game.wait(2.0) // delay to allow bot toast to disappear
        while (
            teamTrialsComplete == false ||
            dailyRacesComplete == false ||
            legendRacesComplete == false ||
            dailyMissionsCollected == false
        ) {
            if (game.checkTitleScreen()) {
                handleTitleScreenOperations()
            } else if (game.checkMenuBar()) {
                // We start all operations from the home menu.

                // Give it a second for dialogs to pop up
                game.wait(1.0)

                // Close any dialogs that may have popped up
                while (game.checkDialog()) {
                    game.closeDialog()
                    game.wait(1.0)
                }

                if (!teamTrialsComplete) {
                    handleTeamTrials()
                    teamTrialsComplete = true
                } else if (!dailyRacesComplete) {
                    handleDailyRaces()
                    dailyRacesComplete = true
                } else if (!legendRacesComplete) {
                    handleLegendRaces()
                    legendRacesComplete = true
                } else if (!dailyMissionsCollected) {
                    collectDailyMissions()
                    dailyMissionsCollected = true
                } else {
                    game.printToLog("\n[INFO] All daily tasks complete. Stopping...")
                    game.notificationMessage = "Daily Tasks Completed"
                    break
                }
            }
        }
    }

    fun handleTitleScreenOperations() {
        // Navigate from title screen to home screen.
        game.clickTitleScreen()
        while (!game.checkMenuBar()) {
            game.progressAndIgnorePopups()
        }
    }

    fun collectDailyMissions() {
        if (!game.collectSpecialMissionRewards()) {
            game.printToLog("\n[INFO] collectDailyMissions:: Failed to collect special mission rewards.")
            return
        }
    }

    // ==================
    // TEAM TRIALS
    // ==================

    fun handleTeamTrialsLoop(): Boolean {
        // Shortcut to resume an existing team trials match
        
        if (!game.checkTeamTrialsRacesMenu()) {
            game.clickTeamTrialsOpponent()

            game.wait(2.0)

            if (!game.clickNextButton()) {
                game.printToLog("\n[INFO] handleTeamTrialsLoop:: Couldn't find 'Next' button.")
                return false
            }

            game.wait(0.5)

            if (!game.clickItemsSelectedDialogRaceButton()) {
                game.printToLog("\n[INFO] handleTeamTrialsLoop:: Failed to click Items Selected dialog race button.")
                return false
            }

            game.wait(3.0)
        } else {
            game.printToLog("\n[INFO] handleTeamTrialsLoop:: Resuming existing team trials match.")

            game.wait(1.0)

            game.printToLog("\n[INFO] CLICKING NEXT BUTTON")
            game.clickNextButton()

            game.wait(3.0)
        }

        var i = 0
        while (true) {
            // At most there are 5 races.
            if (i >= 5) {
                game.printToLog("\n[INFO] handleTeamTrialsLoop:: Max race loop count reached! Breaking loop.")
                break
            }
            
            // If Team Trials Next button shows up, we break out early.
            // This should only happen if user resumes a team trial that they
            // have already completed some races in.
            if (game.checkTeamTrialsNext()) {
                game.printToLog("\n[INFO] handleTeamTrialsLoop:: Found team_trials_next button! Breaking loop.")
                break
            }

            // Tap while waiting for the See Results button to appear.
            var startTime = System.currentTimeMillis()
            val durationLimitMs = 5000L // 5 seconds
            while (
                game.clickTeamTrialsRacesMenuSeeResults() &&
                System.currentTimeMillis() - startTime < durationLimitMs
            ) {
                // Keep clicking the See Results button until
                // either it goes away or we go past max time.
            }

            startTime = System.currentTimeMillis()
            while (
                !game.checkTeamTrialsRaceResult() &&
                System.currentTimeMillis() - startTime < durationLimitMs
            ) {
                // Wait for the team trials Win/Draw/Lose result to appear.
            }

            // Now that the result appeared, tap a few times to progress.
            game.progressAndIgnorePopups()

            i++
            game.printToLog("\n[INFO] handleTeamTrialsLoop:: Team Trials race loop count: ${i}")
        }

        game.printToLog("\n[INFO] Out of loop for team trials races")

        // After completing all races

        // Wait for the Team Trials Next button to appear.
        // This appears regardless of whether there are extra rewards.
        while (!game.checkTeamTrialsNext()) {
            // Tap to speed up intermediate results
            game.progressAndIgnorePopups()
        }

        if (game.checkTeamTrialsResultsExtraRewards()) {
            // If we got extra rewards, claim them by clicking Next.
            game.printToLog("\n[INFO] handleTeamTrialsLoop:: Detected extra rewards. Claiming...")
            // Until the Next button appears in the rewards screen...
            while (!game.checkNextButton()) {
                while (game.clickTeamTrialsNext()) {
                    // Keep clicking the Team Trials Next button until we get to the reward screen
                }
            }
            if (!game.clickNextButton()) {
                game.printToLog("\n[INFO] handleTeamTrialsLoop:: Couldn't find 'Next' button in rewards screen.")
                return false
            }
        }

        while (game.imageUtils.findImage("race_again", tries = 1).first == null) {
            game.progressAndIgnorePopups()
            game.wait(0.1)
        }

        if (game.checkDailySaleDialog()) {
            game.cancelDailySaleDialog()
            game.wait(0.5)
        }

        if (!game.clickTeamTrialsRaceAgain()) {
            game.printToLog("\n[INFO] handleTeamTrialsLoop:: Failed to click Race Again button.")
            return false
        }

        // If we are out of RP, return to home.
        if (game.checkRestoreDialog()) {
            if (!game.cancelRestoreDialog()) {
                game.printToLog("\n[INFO] handleTeamTrialsLoop:: Failed to close Restore dialog.")
                return false
            }

            game.wait(0.5)

            if (!game.clickTeamTrialsNext()) {
                game.printToLog("\n[INFO] handleTeamTrialsLoop:: Failed to click Team Trials 'Next' button.")
                return false
            }

            game.wait(0.5)

            if (!game.gotoHomeMenu()) {
                game.printToLog("\n[INFO] handleTeamTrialsLoop:: Failed to return to home.")
                return false
            }

            game.wait(0.5)

            return false
        }

        return true
    }

    fun handleTeamTrials() {
        if (!game.checkMenuBar()) {
            game.printToLog("\n[INFO] handleTeamTrials:: Could not find menu bar.")
            return
        }

        if (!game.gotoRaceMenu()) {
            game.printToLog("\n[INFO] handleTeamTrials:: Failed to go to race menu.")
            return
        }

        game.wait(0.5)

        if (!game.gotoTeamTrialsMenu()) {
            game.printToLog("\n[INFO] handleTeamTrials:: Failed to go to team trials menu.")
            return
        }

        game.wait(0.5)

        // Should be at Team Trials menu now.

        if (!game.gotoTeamRaceMenu()) {
            game.printToLog("\n[INFO] handleTeamTrials:: Failed to navigate to team race.")
            return
        }

        game.wait(0.5)

        // If we are out of RP, return to home.
        if (game.checkRestoreDialog()) {
            if (!game.cancelRestoreDialog()) {
                game.printToLog("\n[INFO] handleTeamTrialsLoop:: Failed to close Restore dialog.")
                return
            }

            game.wait(0.5)

            if (!game.gotoHomeMenu()) {
                game.printToLog("\n[INFO] handleTeamTrialsLoop:: Failed to return to home.")
                return
            }

            game.wait(0.5)

            return
        }

        game.wait(0.5)

        // Team Trials -> Team Race
        var i = 0
        while (true) {
            i++
            game.printToLog("\n[INFO] handleTeamTrials:: Loop count: ${i}.")
            if (!handleTeamTrialsLoop()) {
                break
            }
        }
    }

    // ==================
    // DAILY RACES
    // ==================

    fun handleDailyRacesLoop(): Boolean {
        game.printToLog("\n[INFO] handleDailyRacesLoop:: begin iteration")

        // Starts at the runner list
        if (!game.clickNextButton()) {
            game.printToLog("\n[INFO] handleDailyRacesLoop:: Could not find 'Next' button.")
            return false
        }

        game.wait(0.5)

        if (!game.clickItemsSelectedDialogRaceButton()) {
            game.printToLog("\n[INFO] handleDailyRacesLoop:: Failed to click Items Selected dialog race button.")
            return false
        }

        game.wait(3.0)

        game.handleStandaloneRaceNonCampaign()

        game.wait(1.0)

        if (!game.clickNextButton()) {
            game.printToLog("\n[INFO] handleDailyRacesLoop:: Couldn't find 'Next' button.")
            return false
        }

        game.wait(0.5)

        if (game.checkDailySaleDialog()) {
            game.cancelDailySaleDialog()
            game.wait(0.5)
        }

        // Keep tapping until we can click the Race Again button.
        while (!game.clickDailyRacesRaceAgain()) {
            game.progressAndIgnorePopups()
        }

        // If the ticket dialog pops up, we are done. Return to home.
        if (game.checkDailyRaceTicketDialog()) {
            if (!game.cancelDailyRaceTicketDialog()) {
                game.printToLog("\n[INFO] handleDailyRacesLoop:: Failed to close Daily Race Ticket dialog.")
                return false
            }

            game.wait(0.5)

            while (!game.clickDailyRacesNext()) {
                game.progressAndIgnorePopups()
            }

            game.wait(0.5)

            if (!game.gotoHomeMenu()) {
                game.printToLog("\n[INFO] handleDailyRacesLoop:: Failed to return to home.")
                return false
            }

            game.wait(0.5)

            return false
        }

        return true
    }

    fun handleDailyRaces() {
        if (!game.checkMenuBar()) {
            game.printToLog("\n[INFO] handleDailyRaces:: Could not find menu bar.")
            return
        }

        if (!game.gotoRaceMenu()) {
            game.printToLog("\n[INFO] handleDailyRaces:: Failed to go to race menu.")
            return
        }

        game.wait(0.5)

        if (!game.gotoDailyRacesMenu()) {
            game.printToLog("\n[INFO] handleDailyRaces:: Failed to go to daily races menu.")
            return
        }

        game.wait(0.5)

        if (!game.gotoDailyRacesRaceMenu("moonlight_sho")) {
            game.printToLog("\n[INFO] handleDailyRaces:: Failed to go to daily races race menu.")
            return
        }

        game.wait(0.5)

        if (!game.gotoDailyRacesRace("hard")) {
            game.printToLog("\n[INFO] handleDailyRaces:: Failed to go to daily races race menu race.")
            return
        }

        game.wait(0.5)

        // Check if we ran out of daily races.

        if (game.checkDailyRaceTicketDialog()) {
            if (!game.cancelDailyRaceTicketDialog()) {
                game.printToLog("\n[INFO] handleDailyRaces:: Failed to close daily race ticket dialog.")
                return
            }

            game.wait(0.5)

            if (!game.gotoHomeMenu()) {
                game.printToLog("\n[INFO] handleDailyRaces:: Failed to return to home.")
                return
            }

            game.wait(0.5)

            return
        }

        game.wait (0.5)

        // If not, then the Race Details dialog should open.

        if (!game.checkRaceDetailsDialog()) {
            game.printToLog("\n[INFO] handleDailyRaces:: Race Details dialog missing.")
            return
        }

        if (!game.clickRaceDetailsDialogRace()) {
            game.printToLog("\n[INFO] handleDailyRaces:: Failed to click Race! button in Race Details dialog.")
            return
        }

        game.wait(0.5)

        if (!game.clickRunnerSelectConfirm()) {
            game.printToLog("\n[INFO] handleDailyRaces:: Failed to click runner select confirm button.")
            return
        }

        game.wait(0.5)

        // Begin inner loop
        var i = 0
        while (true) {
            i++
            game.printToLog("\n[INFO] handleDailyRaces:: Loop count: ${i}.")
            if (!handleDailyRacesLoop()) {
                break
            }
        }
    }

    // ==================
    // LEGEND RACES
    // ==================

    fun handleLegendRacesLoop(): Boolean {
        game.printToLog("\n[INFO] handleLegendRacesLoop:: begin iteration")

        // Starts at the legend race menu

        if (!game.gotoLegendRaceRace("hard")) {
            game.printToLog("\n[INFO] handleLegendRaces:: Failed to go to legend race menu race.")

            // Go back to home since there are no races available.
            game.gotoHomeMenu()
            return false
        }

        game.wait(1.0)

        if (!game.checkRaceDetailsDialog()) {
            game.printToLog("\n[INFO] handleLegendRaces:: Race Details dialog missing.")
            return false
        }

        if (!game.clickRaceDetailsDialogRace()) {
            game.printToLog("\n[INFO] handleLegendRaces:: Failed to click Race! button in Race Details dialog.")
            return false
        }

        game.wait(0.5)

        if (!game.clickRunnerSelectConfirm()) {
            game.printToLog("\n[INFO] handleLegendRaces:: Failed to click runner select confirm button.")
            return false
        }

        game.wait(0.5)

        if (!game.clickNextButton()) {
            game.printToLog("\n[INFO] handleLegendRacesLoop:: Could not find 'Next' button.")
            return false
        }

        game.wait(0.5)

        if (!game.clickItemsSelectedDialogRaceButton()) {
            game.printToLog("\n[INFO] handleLegendRacesLoop:: Failed to click Items Selected dialog race button.")
            return false
        }

        game.wait(3.0)

        game.handleStandaloneRaceNonCampaign()

        while (!game.clickNextButton()) {
            // Keep clicking until it shows up.
            game.progressAndIgnorePopups()
        }

        game.wait(0.5)

        while (!game.clickNextButton()) {
            if (game.checkDailySaleDialog()) {
                game.cancelDailySaleDialog()
                game.wait(0.5)
            }
        }

        return true
    }

    fun handleLegendRaces() {
        if (!game.checkMenuBar()) {
            game.printToLog("\n[INFO] handleLegendRaces:: Could not find menu bar.")
            return
        }

        if (!game.gotoRaceMenu()) {
            game.printToLog("\n[INFO] handleLegendRaces:: Failed to go to race menu.")
            return
        }

        game.wait(0.5)

        if (!game.gotoRaceEventsMenu()) {
            game.printToLog("\n[INFO] handleLegendRaces:: Failed to go to race events menu.")
            return
        }

        game.wait(0.5)

        if (!game.gotoLegendRaceMenu()) {
            game.printToLog("\n[INFO] handleLegendRaces:: Failed to go to legend race menu.")
            return
        }

        game.wait(0.5)

        // Begin inner loop
        var i = 0
        while (true) {
            i++
            game.printToLog("\n[INFO] handleLegendRaces:: Loop count: ${i}.")
            if (!handleLegendRacesLoop()) {
                break
            }
        }

        game.wait(0.5)

        if (!game.collectLegendRaceSpecialMissionRewards()) {
            game.printToLog("\n[INFO] handleLegendRaces:: Failed to collect special mission rewards.")
            return
        }

        if (!game.gotoHomeMenu()) {
            game.printToLog("\n[INFO] handleLegendRaces:: Failed to return to home.")
            return
        }
    }
}