package com.steve1316.uma_android_automation.bot.campaigns

import android.graphics.Bitmap

import org.opencv.core.Point

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Campaign
import com.steve1316.uma_android_automation.bot.Game

import com.steve1316.automation_library.data.SharedData
import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult
import com.steve1316.uma_android_automation.bot.plugins.ChampionsMeeting
import com.steve1316.uma_android_automation.bot.plugins.ClubActivity
import com.steve1316.uma_android_automation.bot.plugins.DailyRaces
import com.steve1316.uma_android_automation.bot.plugins.DailySale
import com.steve1316.uma_android_automation.bot.plugins.LegendRace
import com.steve1316.uma_android_automation.bot.plugins.Presents
import com.steve1316.uma_android_automation.bot.plugins.SpecialMissions
import com.steve1316.uma_android_automation.bot.plugins.TeamTrials

import com.steve1316.uma_android_automation.components.*

class DailyTasks(game: Game) : Campaign(game) {
    override val TAG: String = "[${MainActivity.loggerTag}]DailyTasks"

    private val bEnableChampionsMeeting: Boolean = true
    private val bEnableClubActivity: Boolean = true
    private val bEnableDailyRaces: Boolean = true
    private val bEnableDailySale: Boolean = true
    private val bEnableLegendRace: Boolean = true
    private val bEnablePresents: Boolean = true
    private val bEnableSpecialMissions: Boolean = true
    private val bEnableTeamTrials: Boolean = true

    override fun handleDialogs(dialog: DialogInterface?): Pair<Boolean, DialogInterface?> {
        return super.handleDialogs(dialog)
    }

    fun pluginDialogHandler(dialog: DialogInterface? = null): DialogHandlerResult {
        val result: Pair<Boolean, DialogInterface?> = handleDialogs(dialog)
        val dialog: DialogInterface = result.second ?: return DialogHandlerResult.NoDialogDetected

        when (dialog.name) {
            "date_changed" -> {
                dialog.close(game.imageUtils)
                handleTitleMenu()
            }
            "notices" -> dialog.close(game.imageUtils)
            "open_soon" -> {
                dialog.close(game.imageUtils)
            }
            "story_unlocked" -> dialog.close(game.imageUtils)
            else -> return DialogHandlerResult.Unhandled(dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(dialog)
    }

    private fun selectLegendRace(bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        // Always select the hardest race.
        val locs: ArrayList<Point> = IconExtraRacePill.findAll(game.imageUtils, sourceBitmap = bitmap)
        if (locs.isEmpty()) {
            return false
        }

        game.tap(locs.first().x, locs.first().y, IconExtraRacePill.template.path)

        game.waitForLoading()

        return true
    }

    private fun handleTitleMenu(): Boolean {
        val maxTimeMs = 60000 // 60 seconds
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxTimeMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                pluginDialogHandler() is DialogHandlerResult.Handled -> {}
                ButtonMenuBarHome.check(game.imageUtils, sourceBitmap = bitmap) -> return true
                ButtonTitleScreen.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                ButtonSkip.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                else -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }

        MessageLog.e(TAG, "[DAILY_TASKS] handleTitleMenu timed out.")
        return false
    }

	override fun start() {
		MessageLog.i(TAG, "[DAILY_TASKS] Starting process for handling the Daily Tasks.")

        // Order determines when each plugin is run.
        // TODO: Maybe make this user configurable?
        val plugins = listOf(
            ::TeamTrials,
            ::DailyRaces,
            ::ChampionsMeeting,
            ::LegendRace,
            ::ClubActivity,
            ::SpecialMissions,
            ::Presents,
            ::DailySale,
        )

        for (plugin in plugins) {
            val instance: Plugin = plugin(game, ::pluginDialogHandler)
            val result: Boolean = instance.start()
            val className: String = instance::class.simpleName ?: "UNKNOWN"
            if (result) {
                MessageLog.i(TAG, "[DAILY_TASKS] [$className] Completed.")
            } else {
                MessageLog.w(TAG, "[DAILY_TASKS] [$className] Timed out.")
            }
        }
	}
}
