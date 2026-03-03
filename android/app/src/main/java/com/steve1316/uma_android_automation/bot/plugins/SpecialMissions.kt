package com.steve1316.uma_android_automation.bot.plugins

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MessageLog

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.bot.Game
import com.steve1316.uma_android_automation.bot.plugins.Plugin
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerCallback
import com.steve1316.uma_android_automation.bot.plugins.DialogHandlerResult
import com.steve1316.uma_android_automation.utils.ScrollList
import com.steve1316.uma_android_automation.utils.ScrollListEntry

import com.steve1316.uma_android_automation.components.ButtonBack
import com.steve1316.uma_android_automation.components.ButtonCollectAll
import com.steve1316.uma_android_automation.components.ButtonEventMissions
import com.steve1316.uma_android_automation.components.ButtonEventMissionsTabLimitedTime
import com.steve1316.uma_android_automation.components.ButtonHomeSpecialMissions
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabDaily
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabMain
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabSpecial
import com.steve1316.uma_android_automation.components.ButtonSpecialMissionsTabTitles
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.DialogInterface
import com.steve1316.uma_android_automation.components.IconEventExclusiveMissionsListBottomRight
import com.steve1316.uma_android_automation.components.IconEventExclusiveMissionsListTopLeft
import com.steve1316.uma_android_automation.components.MenuBar
import com.steve1316.uma_android_automation.components.PageInterface
import com.steve1316.uma_android_automation.components.PageSpecialMissions

class SpecialMissions(
    game: Game,
    menuBar: MenuBar,
    commonDialogHandler: DialogHandlerCallback? = null,
) : Plugin(game, menuBar, commonDialogHandler) {
    override val TAG: String = "[${MainActivity.loggerTag}]SpecialMissions"

    private val missionsTabs: List<ComponentInterface> = listOf(
        ButtonSpecialMissionsTabDaily,
        ButtonSpecialMissionsTabMain,
        ButtonSpecialMissionsTabTitles,
        ButtonSpecialMissionsTabSpecial,
        ButtonEventMissionsTabLimitedTime,
    )

    private var bHasHandledSpecialMissions: Boolean = false
    private var bHasHandledEventExclusiveMissions: Boolean = false

    override fun handleDialogs(dialog: DialogInterface?): DialogHandlerResult {
        val result: DialogHandlerResult = super.handleDialogs(dialog)
        if (result !is DialogHandlerResult.Unhandled) {
            return result
        }

        when (result.dialog.name) {
            "event_exclusive_missions" -> {
                // TODO: Need to add logic for other entries in this dialog.
                // Only event mission is currently in the dialog. Need to wait
                // for legend races to become available again.
                if (!bHasHandledEventExclusiveMissions) {
                    handleEventExclusiveMisisons()
                } else {
                    result.dialog.close(game.imageUtils)
                }
            }
            // Logic handled in [handleEventExclusiveMisisons]
            "event_missions" -> result.dialog.close(game.imageUtils)
            "rewards_collected" -> result.dialog.close(game.imageUtils)
            // Logic handled in [handleEventExclusiveMisisons]
            "special_missions" -> result.dialog.close(game.imageUtils)
            "story_unlocked" -> result.dialog.close(game.imageUtils)
            else -> return DialogHandlerResult.Unhandled(result.dialog)
        }
        game.wait(0.5, skipWaitingForLoading = true)
        return DialogHandlerResult.Handled(result.dialog)
    }

    override fun checkPage(bitmap: Bitmap?): PageInterface? {
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()

        return listOf<PageInterface>(
            PageSpecialMissions,
        ).find { it.check(game.imageUtils, bitmap) }
    }

    private fun handleEventExclusiveMisisons() {
        val scrollList: ScrollList? = ScrollList.create(
            this,
            listTopLeftComponent = IconEventExclusiveMissionsListTopLeft,
            listBottomRightComponent = IconEventExclusiveMissionsListBottomRight,
        )
        if (scrollList == null) {
            MessageLog.e(TAG, "[$name] Failed to detect EventExclusiveMissions list.")
            return
        }
        
        /** Handle each entry in the list.
         * 
         * @return False to continue the list processing. True to end list processing early.
         */
        fun onListEntry(scrollList: ScrollList, entry: ScrollListEntry): Boolean {
            MessageLog.d(TAG, "[$name] Handling EventExclusiveMissions entry #${entry.index}.")
            val x: Double = (entry.bbox.x + (entry.bbox.w / 2).toInt()).toDouble()
            val y: Double = (entry.bbox.y + (entry.bbox.h / 2).toInt()).toDouble()
            game.tap(x, y)
            game.wait(1.0)
            if (waitForButton(missionsTabs) == null) {
                MessageLog.w(TAG, "[$name] Timed out waiting for mission tabs to appear for event exclusive mission entry #${entry.index}.")
                // Need to bail out since we're stuck in an unknown state.
                return true
            }
            handleMissionsTabs()

            // Let the dialog handler take care of any missions screens that are
            // inside a dialog. It will close the dialog when complete.
            val dialogResult: DialogHandlerResult = handleDialogs()
            if (dialogResult is DialogHandlerResult.Handled) {
                return false
            }

            // If missions screen isn't in a dialog, we need to return by clicking
            // the back button which should exist in all non-dialog missions screens.
            waitForButton(ButtonBack, bShouldClickButton = true)
            // Now wait for game to load.
            game.wait(1.0)
            return false
        }

        scrollList.process(::onListEntry)

        bHasHandledEventExclusiveMissions = true
    }

    private fun handleTab(tab: ComponentInterface) {
        MessageLog.d(TAG, "[$name] Handling tab: ${tab::class.simpleName}")
        game.wait(0.25, skipWaitingForLoading = true)
        tab.click(game.imageUtils)
        game.wait(0.25, skipWaitingForLoading = true)
        if (ButtonCollectAll.checkDisabled(game.imageUtils) == true) {
            MessageLog.d(TAG, "[$name] Tab has no rewards to collect. Moving on...")
            return
        }
        ButtonCollectAll.click(game.imageUtils)
        game.wait(0.5)
        handleDialogs()
    }

    private fun handleMissionsTabs() {
        MessageLog.d(TAG, "[$name] Handling Missions tabs...")
        missionsTabs.forEach { handleTab(it) }
    }

    override fun progress(bitmap: Bitmap?): PageInterface? {
        val currentPage: PageInterface? = super.progress(bitmap)

        // We do this after super call to avoid taking unnecessary screenshots.
        val bitmap: Bitmap = bitmap ?: game.imageUtils.getSourceBitmap()
        when (currentPage) {
            PageSpecialMissions -> {
                if (!bHasHandledSpecialMissions) {
                    MessageLog.d(TAG, "[$name] PageSpecialMissions: Handling special missions...")
                    handleMissionsTabs()
                } else {
                    MessageLog.d(TAG, "[$name] PageSpecialMissions: Handling event missions...")
                    if (!ButtonEventMissions.click(game.imageUtils)) {
                        MessageLog.d(TAG, "[$name] PageSpecialMissions: No available event missions.")
                        bHasHandledEventExclusiveMissions = true
                    }
                }
            }
            else -> {}
        }

        bIsComplete = bHasHandledSpecialMissions && bHasHandledEventExclusiveMissions

        if (bIsComplete) {
            MessageLog.d(TAG, "[$name] SpecialMissions is complete. Returning...")
            ButtonBack.click(game.imageUtils)
        }

        return null
    }

     override fun goToStart(): Boolean {
        super.goToStart()

        if (PageSpecialMissions.check(game.imageUtils)) {
            return true
        }

        if (!goToHome()) {
            MessageLog.e(TAG, "[$name] Failed to go to MenuBar Home tab. Cannot continue.")
            return false
        }

        if (waitForButton(ButtonHomeSpecialMissions, bShouldClickButton = true) == null) {
            MessageLog.w(TAG, "[$name] Failed to find Special Missions button.")
            return false
        }

        return waitForPage(PageSpecialMissions) != null
    }
}
