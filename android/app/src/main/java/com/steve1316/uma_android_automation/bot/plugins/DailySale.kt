package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap
import org.opencv.core.Point

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.data.SharedData
import com.steve1316.automation_library.utils.SettingsHelper
import com.steve1316.automation_library.utils.TextUtils

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult
import com.steve1316.uma_android_automation.utils.ScrollList
import com.steve1316.uma_android_automation.utils.ScrollListEntry
import com.steve1316.uma_android_automation.types.BoundingBox

import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageDailySale
import com.steve1316.uma_android_automation.components.ButtonShopExchange
import com.steve1316.uma_android_automation.components.ButtonShopEndSale
import com.steve1316.uma_android_automation.components.ButtonBack
import com.steve1316.uma_android_automation.components.ButtonHomeShopDailySale
import com.steve1316.uma_android_automation.components.ButtonShopDailySales
import com.steve1316.uma_android_automation.components.ButtonHomeShop
import com.steve1316.uma_android_automation.components.MenuBar

class DailySale(
    game: Game,
    menuBar: MenuBar,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, menuBar, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]DailySale"

    enum class SaleItem {
        STAR_PIECE,
        ALARM_CLOCK,
        PLEASING_PARFAIT;

        companion object {
            private val nameMap = entries.associateBy { it.name }

            fun fromName(value: String): SaleItem? = nameMap[value.replace(" ", "_").uppercase()]
        }
    }

    private val saleItemsToBuy: List<SaleItem> = SettingsHelper.getStringArraySetting("dailyTasks", "saleItems")
        .mapNotNull { it -> SaleItem.fromName(it) }

    private var bSaleExpired: Boolean = false

    private val purchasedItems: MutableList<SaleItem> = mutableListOf()

    override fun handleDialogs(dialog: DialogInterface?, args: Map<String, Any>): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog, args)
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
        super.goToStart()

        if (PageDailySale.check(game.imageUtils)) {
            return true
        }

        if (!goToHome()) {
            MessageLog.e(TAG, "[$name] Failed to go to MenuBar Home tab. Cannot continue.")
            return false
        }

        if (waitForButton(ButtonHomeShop, bShouldClickButton = false) == null) {
            MessageLog.e(TAG, "[$name] Failed to find Shop button. Cannot proceed.")
            return false
        }

        if (waitForButton(ButtonHomeShopDailySale, bShouldClickButton = true) == null) {
            MessageLog.i(TAG, "[$name] No daily sale available. Cannot proceed.")
            return false
        }

        if (waitForButton(ButtonShopDailySales, bShouldClickButton = true) == null) {
            MessageLog.w(TAG, "[$name] No daily sale available in shop. Cannot proceed.")
            return false
        }

        return waitForPage(PageDailySale) != null
    }

    private fun onListEntry(
        scrollList: ScrollList,
        entry: ScrollListEntry,
    ): Boolean {
        fun extractText(bitmap: Bitmap): String {
            try {
                val detectedText = game.imageUtils.performOCROnRegion(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    useThreshold = false,
                    useGrayscale = true,
                    scale = 2.0,
                    ocrEngine = "mlkit",
                    debugName = "DailySale.onListEntry: extractText",
                    multiLine = true,
                )
                return detectedText
            } catch (e: Exception) {
                MessageLog.e(TAG, "[$name] Exception during text extraction: ${e.message}")
                return ""
            }
        }

        var prevNames: Set<String> = setOf()

        var buttonLoc: Point? = ButtonShopExchange.findImageWithBitmap(
            game.imageUtils,
            sourceBitmap = entry.bitmap,
        )

        if (buttonLoc == null) {
            MessageLog.e(TAG, "[$name] Failed to find any Exchange button in bitmap.")
            return false
        }

        var bIsDisabled: Boolean = ButtonShopExchange.checkDisabled(game.imageUtils, entry.bitmap) == true

        // Translate the location to the screen coordinates.
        buttonLoc = Point(buttonLoc.x + entry.bbox.x, buttonLoc.y + entry.bbox.y)

        val text: String = extractText(entry.bitmap).lowercase().replace("\n", " ")
        var match: SaleItem? = null
        for (saleItem in saleItemsToBuy) {
            val query: String = saleItem.name.lowercase().replace("_", " ")
            if (text.contains(query)) {
                match = saleItem
                break
            }
        }

        // Click the button if it is in our list of items to buy.
        if (match != null && !bIsDisabled) {
            game.tap(buttonLoc.x, buttonLoc.y, ButtonShopExchange.templates.first()?.path ?: "ok")
            // Clicking the exchange button will load from server. Need to wait.
            game.wait(0.5)
        } else {
            return false
        }

        // If star piece is selected, we buy two of them. Add one to total.
        val numItemsToBuy: Int = saleItemsToBuy.size + if (saleItemsToBuy.contains(SaleItem.STAR_PIECE)) 1 else 0

        // Finally, handle any dialogs.
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
                purchasedItems.add(match)
                // If we have purchased all requested items, then we return true
                // to end the list processing immediately.
                return purchasedItems.size == numItemsToBuy
            }
        }
        return false
    }

    override fun start(timeoutMs: Int): Boolean {
        MessageLog.i(TAG, "[$name] Starting...")

        if (!goToStart()) {
            MessageLog.e(TAG, "[$name] Failed to go to plugin's start screen.")
            // Attempt to return to home. Whether this fails here doesn't matter
            // since the plugin is already in a failure state.
            goToHome()
            return false
        }

        val scrollList: ScrollList? = ScrollList.create(game)
        if (scrollList == null) {
            MessageLog.e(TAG, "[$name] Failed to detect sale list.")
            return false
        }

        scrollList.process(onEntry = ::onListEntry)

        if (!bSaleExpired) {
            if (waitForButton(ButtonShopEndSale, bShouldClickButton = true) == null) {
                MessageLog.e(TAG, "[$name] Failed to find End Sale button.")
                return false
            }
            handleDialogs()
        }

        // Instead of returning to home, we want to click the back button since
        // this will take us to where we were before entering the sale.
        waitForButton(ButtonBack, bShouldClickButton = true)

        // Allow the game to load. We don't know where the Back button will take
        // us since we may have come here via a dialog so we need to ensure that
        // we are back where we came from before returning to the calling function.
        game.wait(0.5)

        return true
    }
}
