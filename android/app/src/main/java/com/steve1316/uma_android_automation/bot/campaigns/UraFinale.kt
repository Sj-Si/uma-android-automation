package com.steve1316.uma_android_automation.bot.campaigns

import com.steve1316.automation_library.utils.SettingsHelper
import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.BotService
import com.steve1316.automation_library.utils.DiscordUtils
import com.steve1316.automation_library.data.SharedData

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.Campaign

import com.steve1316.uma_android_automation.types.DateYear
import com.steve1316.uma_android_automation.types.DateMonth
import com.steve1316.uma_android_automation.types.DatePhase
import com.steve1316.uma_android_automation.types.FanCountClass
import com.steve1316.uma_android_automation.types.BoundingBox

import com.steve1316.uma_android_automation.components.*

import android.util.Log
import android.graphics.Bitmap
import org.opencv.core.Point
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class UraFinale(game: Game) : Campaign(game) {
    override fun handleTrainingEvent() {
        trainingEvent.handleTrainingEvent()
    }

    override fun handleRaceEvents(isScheduledRace: Boolean): Boolean {
        val bDidRace: Boolean = racing.handleRaceEvents(isScheduledRace)
        bNeedToCheckFans = bDidRace

        return bDidRace
    }

    override fun checkCampaignSpecificConditions(): Boolean {
        return false
    }

    override fun openFansDialog() {
        MessageLog.d(TAG, "Opening fans dialog...")
        ButtonHomeFansInfo.click(game.imageUtils, region = game.imageUtils.regionTopHalf, tries = 10)
        bHasTriedCheckingFansToday = true
        game.wait(game.dialogWaitDelay, skipWaitingForLoading = true)
    }
}
