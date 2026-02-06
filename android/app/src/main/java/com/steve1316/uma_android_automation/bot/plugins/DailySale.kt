package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap
import org.opencv.core.Point

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.data.SharedData
import com.steve1316.automation_library.utils.SettingsHelper

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult
import com.steve1316.uma_android_automation.utils.ScrollList
import com.steve1316.uma_android_automation.utils.types.BoundingBox

import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageHome
import com.steve1316.uma_android_automation.components.PageDailySale
import com.steve1316.uma_android_automation.components.ButtonShopExchange
import com.steve1316.uma_android_automation.components.ButtonShopExchangeDisabled
import com.steve1316.uma_android_automation.components.ButtonShopEndSale
import com.steve1316.uma_android_automation.components.ButtonBack
import com.steve1316.uma_android_automation.components.ButtonHomeShopDailySale
import com.steve1316.uma_android_automation.components.ButtonShopDailySales
import com.steve1316.uma_android_automation.components.LabelShopStarPiece
import com.steve1316.uma_android_automation.components.LabelShopAlarmClock
import com.steve1316.uma_android_automation.components.LabelShopPleasingParfait

enum class SaleItem {
    STAR_PIECE,
    ALARM_CLOCK,
    PLEASING_PARFAIT;

    companion object {
        private val nameMap = entries.associateBy { it.name }

        fun fromName(value: String): SaleItem? = nameMap[value.replace(" ", "_").uppercase()]
    }
}

class DailySale(
    game: Game,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]DailySale"

    private val saleItemsToBuy: List<SaleItem> = SettingsHelper.getStringArraySetting("dailyTasks", "saleItems")
        .mapNotNull { it -> SaleItem.fromName(it) }
    override val bIsEnabled: Boolean = super.bIsEnabled && saleItemsToBuy.isNotEmpty()

    private var bSaleExpired: Boolean = false

    override fun handleDialogs(dialog: DialogInterface?): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "confirm_exchange" -> result.dialog.ok(game.imageUtils)
            "end_sale_confirmation" -> result.dialog.ok(game.imageUtils)
            "exchange_complete" -> result.dialog.close(game.imageUtils)
            "return_to_shops" -> {
                bSaleExpired = true
                result.dialog.close(game.imageUtils)
            }
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

        if (!ButtonHomeShopDailySale.click(game.imageUtils)) {
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

    private fun onListEntry(
        scrollList: ScrollList,
        index: Int,
        component: ComponentInterface,
        point: Point,
        bitmap: Bitmap,
    ): Boolean {
        var prevNames: Set<String> = setOf()

        var numToHandle: Int = listOf<Boolean>(
            saleItemsToBuy.contains(SaleItem.STAR_PIECE),
            saleItemsToBuy.contains(SaleItem.STAR_PIECE),
            saleItemsToBuy.contains(SaleItem.ALARM_CLOCK),
            saleItemsToBuy.contains(SaleItem.PLEASING_PARFAIT),
        ).count { it }

        var numHandled: Int = 0

        // Create a list of components to search for only if they are items
        // which we actually want to buy.
        val componentsToFind: List<ComponentInterface> = saleItemsToBuy.mapNotNull {
            when (it) {
                SaleItem.STAR_PIECE -> LabelShopStarPiece
                SaleItem.ALARM_CLOCK -> LabelShopAlarmClock
                SaleItem.PLEASING_PARFAIT -> LabelShopPleasingParfait
                else -> null
            }
        }
        if (componentsToFind.isEmpty()) {
            return false
        }

        val componentToBuy: ComponentInterface? = componentsToFind.firstOrNull {
            it.check(game.imageUtils)
        }

        if (componentToBuy == null) {
            return false
        }

        if (component != ButtonShopExchangeDisabled) {
            // Don't use the passed bitmap since we want to try and click
            // on the most updated location.

            // Find the component in question on the screen then crop
            // that entry out of the image.
            val (newLoc, bitmap) = componentToBuy.find(game.imageUtils)
            if (newLoc == null) {
                return false
            }
            val bbox: BoundingBox = BoundingBox(
                x = 0,
                y = (point.y - (bitmap.height / 2)).toInt(),
                w = (bitmap.width).toInt(),
                h = (bitmap.height).toInt(),
            )
            val cropped: Bitmap? = game.imageUtils.createSafeBitmap(
                game.imageUtils.getSourceBitmap(),
                bbox,
                "onListEntry",
            )
            if (cropped == null) {
                return false
            }

            // Finally, click the button in this entry.
            if (!ButtonShopExchange.click(game.imageUtils, sourceBitmap = cropped)) {
                return false
            }

            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 5000) {
                val dialogResult: DialogHandlerResult = handleDialogs()
                // If the sale expired, then we need to break immediately
                // and handle this case.
                if (bSaleExpired) {
                    return false
                }
                // If we finished purchasing this item, break to proceed
                // to the next entry in the list.
                if (
                    dialogResult is DialogHandlerResult.Handled &&
                    dialogResult.dialog.name == "exchange_complete"
                ) {
                    return false
                }
            }
        }

        // Return true if we bought everything. Stops the scroll list loop.
        //numHandled >= componentsToFind.size
        return false
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

        val entryComponents: List<ComponentInterface> = listOf(
            ButtonShopExchange,
            ButtonShopExchangeDisabled,
        )

        scrollList.process(entryComponents, onEntry = ::onListEntry)

        if (!bSaleExpired) {
            ButtonShopEndSale.click(game.imageUtils, tries = 10)
            handleDialogs()
            game.wait(0.5)
            game.waitForLoading()
        }
        game.wait(0.5)
        ButtonBack.click(game.imageUtils)
        game.wait(0.5)
        game.waitForLoading()
        return true
    }
}
