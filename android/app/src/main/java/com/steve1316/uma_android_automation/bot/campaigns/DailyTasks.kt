package com.steve1316.uma_android_automation.bot.campaigns

import android.graphics.Bitmap

import org.opencv.core.Point

import com.steve1316.automation_library.data.SharedData
import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.SettingsHelper

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Campaign
import com.steve1316.uma_android_automation.bot.Game

import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.PluginFactory
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult

import com.steve1316.uma_android_automation.components.*

class DailyTasks(game: Game) : Campaign(game) {
    override val TAG: String = "[${MainActivity.loggerTag}]DailyTasks"

    // The ordered list of plugins from the settings.
    val pluginsSetting: List<String> = SettingsHelper.getStringArraySetting("dailyTasks", "plugins")
        .map { it.replace("\\s+".toRegex(), "") }

    private val bEnableChampionsMeeting: Boolean = true
    private val bEnableClubActivity: Boolean = true
    private val bEnableDailyRaces: Boolean = true
    private val bEnableDailySale: Boolean = true
    private val bEnableLegendRace: Boolean = true
    private val bEnablePresents: Boolean = true
    private val bEnableSpecialMissions: Boolean = true
    private val bEnableTeamTrials: Boolean = true

    override fun handleDialogs(dialog: DialogInterface?): Pair<Boolean, DialogInterface?> {
        val dialog: DialogInterface? = dialog ?: DialogUtils.getDialog(game.imageUtils)
        return Pair(false, dialog)
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

    /** Handles navigating to the home screen from the title menu.
     *
     * @param timeoutMs The max time this operation can run before failing.
     *
     * @return Whether the bot is currently at the home screen.
     */
    private fun handleTitleMenu(timeoutMs: Int = 60000): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val bitmap: Bitmap = game.imageUtils.getSourceBitmap()
            when {
                pluginDialogHandler() is DialogHandlerResult.Handled -> {}
                // If we see the Shop button, then we're at the home page.
                // Since we're at home page, we're done.
                ButtonShop.check(game.imageUtils, sourceBitmap = bitmap) -> return true
                ButtonTitleScreen.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                ButtonSkip.click(game.imageUtils, sourceBitmap = bitmap) -> {}
                else -> game.tap(350.0, 750.0, "ok", taps = 1)
            }
        }

        MessageLog.e(TAG, "[DAILY_TASKS] Timed out going to home screen from title.")
        return false
    }

	override fun start() {
		MessageLog.i(TAG, "[DAILY_TASKS] Starting process for handling the Daily Tasks.")

        for (pluginName in pluginsSetting) {
            val plugin: Plugin? = PluginFactory.create(pluginName, game, dialogHandler = ::pluginDialogHandler)
            if (plugin == null) {
                continue
            }

            val result: Boolean = plugin.start()
            if (result) {
                MessageLog.i(TAG, "[DAILY_TASKS] [${plugin.name}] Completed successfully.")
            } else {
                MessageLog.w(TAG, "[DAILY_TASKS] [${plugin.name}] Failed.")
            }
        }
	}
}
