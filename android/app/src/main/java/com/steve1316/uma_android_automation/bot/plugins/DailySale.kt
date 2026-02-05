package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.data.SharedData

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult
import com.steve1316.uma_android_automation.utils.ScrollList

import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageHome
import com.steve1316.uma_android_automation.components.PageDailySale
import com.steve1316.uma_android_automation.components.ButtonShopExchange
import com.steve1316.uma_android_automation.components.ButtonShopExchangeDisabled
import com.steve1316.uma_android_automation.components.ButtonShopEndSale
import com.steve1316.uma_android_automation.components.ButtonBack
import com.steve1316.uma_android_automation.components.ButtonHomeShopDaily
import com.steve1316.uma_android_automation.components.ButtonShopDailySales
import com.steve1316.uma_android_automation.components.LabelShopStarPiece
import com.steve1316.uma_android_automation.components.LabelShopAlarmClock
import com.steve1316.uma_android_automation.components.LabelShopPleasingParfait

class DailySale(
    game: Game,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]DailySale"

    // TODO: Load from settings.
    private val bShouldBuyStarPieces: Boolean = true
    private val bShouldBuyAlarmClock: Boolean = true
    private val bShouldBuyPleasingParfait: Boolean = true

    override fun handleDialogs(dialog: DialogInterface?): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "confirm_exchange" -> result.dialog.ok(game.imageUtils)
            "end_sale_confirmation" -> result.dialog.ok(game.imageUtils)
            "exchange_complete" -> result.dialog.close(game.imageUtils)
            "return_to_shops" -> result.dialog.close(game.imageUtils)
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        return null
    }

    override fun goToStart(): Boolean {
        var dialogResult: DialogHandlerResult = handleDialogs()
        while (dialogResult is DialogHandlerResult.Handled) {
            dialogResult = handleDialogs()
        }

        if (dialogResult is DialogHandlerResult.Unhandled) {
            MessageLog.e(TAG, "Unhandled dialog prevented plugin execution: ${dialogResult.dialog.name}")
            return false
        }

        if (PageDailySale.check(game.imageUtils)) {
            return true
        }

        if (!PageHome.check(game.imageUtils)) {
            MessageLog.w(TAG, "Not at home menu. Cannot proceed.")
            return false
        }

        if (!ButtonHomeShopDaily.click(game.imageUtils)) {
            MessageLog.i(TAG, "No daily sale available. Cannot proceed.")
            return false
        }

        if (!waitForButton(ButtonShopDailySales)) {
            MessageLog.w(TAG, "No daily sale available. Cannot proceed.")
            return false
        }

        game.wait(0.5)
        game.waitForLoading()

        return waitForPage(PageDailySale)
    }

    override fun start(timeoutMs: Int): Boolean {
        if (!goToStart()) {
            MessageLog.e(TAG, "Failed to go to start screen for plugin.")
            return false
        }

        val scrollList: ScrollList? = ScrollList.create(
            game,
            entryHeight = (SharedData.displayHeight * 0.0979).toInt(),
        )

        if (scrollList == null) {
            MessageLog.e(TAG, "[DAILY_SALE] Failed to detect sale list.")
            return false
        }

        var prevNames: Set<String> = setOf()

        var numToHandle: Int = listOf<Boolean>(
            bShouldBuyStarPieces,
            bShouldBuyStarPieces,
            bShouldBuyAlarmClock,
            bShouldBuyPleasingParfait,
        ).count { it }

        var numHandled: Int = 0

        var bSaleExpired: Boolean = false
        
        val entryComponents: List<ComponentInterface> = listOf(
            ButtonShopExchange,
            ButtonShopExchangeDisabled,
        )
        scrollList.process(entryComponents) { _, _, component, loc, bitmap ->
            var bShouldBuyItem: Boolean = false
            when {
                LabelShopStarPiece.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    bShouldBuyItem = bShouldBuyStarPieces
                }
                LabelShopAlarmClock.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    bShouldBuyItem = bShouldBuyAlarmClock
                }
                LabelShopPleasingParfait.check(game.imageUtils, sourceBitmap = bitmap) -> {
                    bShouldBuyItem = bShouldBuyPleasingParfait
                }
            }

            if (bShouldBuyItem) {
                if (component != ButtonShopExchangeDisabled) {
                    game.tap(loc.x, loc.y, ButtonShopExchange.template.path, taps = 3)

                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < 5000) {
                        val dialogResult: DialogHandlerResult = handleDialogs()
                        if (dialogResult is DialogHandlerResult.Handled) {
                            if (dialogResult.dialog.name == "return_to_shops") {
                                // If the sale expired, then we need to break immediately
                                // and handle this case.
                                bSaleExpired = true
                                break
                            } else if (dialogResult.dialog.name == "exchange_complete") {
                                // If we finished purchasing this item, break to proceed
                                // to the next entry in the list.
                                break
                            }
                        }
                    }
                }

                numHandled++
            }

            // Return true if we bought everything to stop the scroll list loop.
            numHandled >= numToHandle
        }

        if (!bSaleExpired) {
            ButtonShopEndSale.click(game.imageUtils, tries = 10)
            handleDialogs()
        }
        ButtonBack.click(game.imageUtils)
        return true
    }
}
