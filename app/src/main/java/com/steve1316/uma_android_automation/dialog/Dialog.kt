package com.steve1316.uma_android_automation.dialog

import com.steve1316.uma_android_automation.utils.Screen
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.*

interface DialogInterface {
    val TAG: String // logging tag
    val name: String // Template name
    val buttons: List<ComponentInterface>
    // The close button is just which ever button is used primarily to close the dialog
    // If not specified, the first button in Buttons will be used.
    val closeButton: ComponentInterface?
    // The ok button is typically used in a dialog with two primary buttons
    // and it closes the dialog while accepting the dialog.
    // If not specified, no default is selected unlike the closeButton.
    // This is because some dialogs may have a close button and a checkbox, but no
    // ok button.
    // If there is only one button in the dialog, then okButton will be set to that.
    val okButton: ComponentInterface?

    fun check(imageUtils: ImageUtils, tries: Int = 1): Boolean {
        return (
            imageUtils.findImage(
                "dialog/dialog_title_${name}",
                tries=tries,
                region=Screen.TOP_HALF,
                customConfidence=0.95,
            ).first != null
        )
    }

    fun close(imageUtils: ImageUtils, tries: Int = 1): Boolean {
        if (closeButton == null) {
            return buttons.getOrNull(0)?.click(imageUtils=imageUtils, tries=tries) ?: false
        }
        return closeButton?.click(imageUtils=imageUtils, tries=tries) ?: false
    }

    fun ok(imageUtils: ImageUtils, tries: Int = 1): Boolean {
        if (okButton == null) {
            return if (buttons.size == 1) {
                close(imageUtils=imageUtils, tries=tries)
            } else {
                false
            }
        }
        return okButton?.click(imageUtils=imageUtils, tries=tries) ?: false
    }
}

// Simple object used to store a list of all dialog objects.
// This is used to easily iterate over all dialogs.
object DialogObjects {
    val items: List<DialogInterface> = listOf<DialogInterface>(
        DialogCareerComplete,               // Career
        DialogConcertSkipConfirmation,      // Career
        DialogConfirmExchange,              // Main Screen
        DialogConfirmRestoreRP,             // Team Trials
        DialogContinueCareer,               // Main Screen
        DialogDailySale,                    // Team Trials, Special Events, Daily Races
        DialogDateChanged,                  // Anywhere
        DialogExternalLink,                 // Main Screen
        DialogFollowTrainer,                // Career
        DialogInfirmary,                    // Career
        DialogInsufficientFans,             // Career
        DialogItemsSelected,                // Team Trials, Special Events, Daily Races
        DialogMenu,                         // Career
        DialogNotices,                      // Main Screen
        DialogOpenSoon,                     // Shop (only when clicking inactive daily sales button)
        DialogPresents,                     // Main Screen (i think?)
        DialogPurchaseDailyRaceTicket,      // Daily Races
        DialogQuickModeSettings,            // Career
        DialogRaceDetails,                  // Daily Races, Special Events
        DialogRaceDetailsCareer,            // Career
        DialogRacePlayback,                 // Career
        DialogRaceRecommendations,          // Career
        DialogRecreation,                   // Career
        DialogRest,                         // Career
        DialogRestAndRecreation,            // Career
        DialogRewardsCollected,             // Main Screen, Special Events
        DialogSessionError,                 // Anywhere
        DialogSongAcquired,                 // Career
        DialogSpecialMissions,              // Main Screen, Special Events
        DialogStrategy,                     // Career
        DialogStoryUnlocked,                // Main Screen, end of career
        DialogTrophyWon,                    // Career
        DialogTryAgain,                     // Career
        DialogUmamusumeDetails,             // Career
        DialogViewStory,                    // Main Screen, end of career
    )
}

object DialogCareerComplete : DialogInterface {
    override val TAG: String = "DialogCareerComplete"
    override val name: String = "career_complete"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonEditTeam
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
        ButtonEditTeam,
    )
}

object DialogConcertSkipConfirmation : DialogInterface {
    override val TAG: String = "DialogConcertSkipConfirmation"
    override val name: String = "concert_skip_confirmation"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonOk
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
    )
}

object DialogConfirmExchange : DialogInterface {
    override val TAG: String = "DialogConfirmExchange"
    override val name: String = "confirm_exchange"
    override val closeButton = null
    override val okButton = null
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogContinueCareer : DialogInterface {
    override val TAG: String = "DialogContinueCareer"
    override val name: String = "continue_career"
    override val closeButton = null
    override val okButton = ButtonResume
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonResume,
    )
}

object DialogConfirmRestoreRP : DialogInterface {
    override val TAG: String = "DialogConfirmRestoreRP"
    override val name: String = "confirm_restore_rp"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonRestore
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonNo,
        ButtonRestore,
    )
}

object DialogDailySale : DialogInterface {
    override val TAG: String = "DialogDailySale"
    override val name: String = "daily_sale"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonShop
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonShop,
    )
}

object DialogDateChanged : DialogInterface {
    override val TAG: String = "DialogDateChanged"
    override val name: String = "date_changed"
    override val closeButton = null
    override val okButton = null
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonOk,
    )
}

object DialogEpithet : DialogInterface {
    override val TAG: String = "DialogEpithet"
    override val name: String = "epithet"
    override val closeButton = null
    override val okButton = null
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonConfirmExclamation,
        Checkbox,
    )
}

object DialogExternalLink : DialogInterface {
    override val TAG: String = "DialogExternalLink"
    override val name: String = "external_link"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonOk
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
    )
}

object DialogFollowTrainer : DialogInterface {
    override val TAG: String = "DialogFollowTrainer"
    override val name: String = "follow_trainer"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonFollow
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonFollow,
    )
}

object DialogInfirmary : DialogInterface {
    override val TAG: String = "DialogInfirmary"
    override val name: String = "infirmary"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonOk
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
    )
}

object DialogInsufficientFans : DialogInterface {
    override val TAG: String = "DialogInsufficientFans"
    override val name: String = "insufficient_fans"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonRace
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonRace,
    )
}

object DialogItemsSelected : DialogInterface {
    override val TAG: String = "DialogItemsSelected"
    override val name: String = "items_selected"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonRaceExclamationShiftedUp
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonRaceExclamationShiftedUp,
    )
}

object DialogMenu : DialogInterface {
    override val TAG: String = "DialogMenu"
    override val name: String = "menu"
    override val closeButton = null
    override val okButton = null
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
        ButtonOptions,
        ButtonSaveAndExit,
        ButtonGiveUp,
    )
}

object DialogNotices : DialogInterface {
    override val TAG: String = "DialogNotices"
    override val name: String = "notices"
    override val closeButton = null
    override val okButton = null
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogOpenSoon : DialogInterface {
    override val TAG: String = "DialogOpenSoon"
    override val name: String = "open_soon"
    override val closeButton = null
    override val okButton = null
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogPresents : DialogInterface {
    override val TAG: String = "DialogPresents"
    override val name: String = "presents"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonCollectAll
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonCollectAll,
    )
}

object DialogPurchaseDailyRaceTicket : DialogInterface {
    override val TAG: String = "DialogPurchaseDailyRaceTicket"
    override val name: String = "purchase_daily_race_ticket"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonOk
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
    )
}

object DialogQuickModeSettings : DialogInterface {
    override val TAG: String = "DialogQuickModeSettings"
    override val name: String = "quick_mode_settings"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonConfirm
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonConfirm,
        RadioCareerQuickShortenAllEvents,
    )
}

object DialogRaceDetails : DialogInterface {
    override val TAG: String = "DialogRaceDetails"
    override val name: String = "race_details"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonRaceExclamation
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonRaceExclamation,
    )
}

object DialogRaceDetailsCareer : DialogInterface {
    override val TAG: String = "DialogRaceDetailsCareer"
    override val name: String = "race_details"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonRace
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonRace,
    )
}

object DialogRacePlayback : DialogInterface {
    override val TAG: String = "DialogRacePlayback"
    override val name: String = "race_playback"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonOk
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
        RadioLandscape,
        RadioPortrait,
    )
}

object DialogRaceRecommendations : DialogInterface {
    override val TAG: String = "DialogRaceRecommendations"
    override val name: String = "race_recommendations"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonConfirm
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonConfirm,
        ButtonRaceRecommendationsCenterStage,
        ButtonRaceRecommendationsPathToFame,
        ButtonRaceRecommendationsForgeYourOwnPath,
        Checkbox,
    )
}

object DialogRecreation : DialogInterface {
    override val TAG: String = "DialogRecreation"
    override val name: String = "recreation"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonOk
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
    )
}

object DialogRest : DialogInterface {
    override val TAG: String = "DialogRest"
    override val name: String = "rest"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonOk
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
    )
}

object DialogRestAndRecreation : DialogInterface {
    // This one doesn't have a checkbox to not ask again for some reason.
    override val TAG: String = "DialogRestAndRecreation"
    override val name: String = "rest_and_recreation"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonOk
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
    )
}

object DialogRewardsCollected : DialogInterface {
    override val TAG: String = "DialogRewardsCollected"
    override val name: String = "rewards_collected"
    override val closeButton = null
    override val okButton = null
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogSessionError : DialogInterface {
    override val TAG: String = "DialogSessionError"
    override val name: String = "session_error"
    override val closeButton = null
    override val okButton = null
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonTitleScreen,
    )
}

object DialogSongAcquired : DialogInterface {
    override val TAG: String = "DialogSongAcquired"
    override val name: String = "song_acquired"
    override val closeButton = null
    override val okButton = null
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogSpecialMissions : DialogInterface {
    override val TAG: String = "DialogSpecialMissions"
    override val name: String = "special_missions"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonCollectAll
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonOk,
        ButtonCollectAll,
    )
}

object DialogStrategy : DialogInterface {
    override val TAG: String = "DialogStrategy"
    override val name: String = "strategy"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonConfirm
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonConfirm,
        ButtonRaceStrategyFront,
        ButtonRaceStrategyPace,
        ButtonRaceStrategyLate,
        ButtonRaceStrategyEnd,
    )
}

object DialogStoryUnlocked : DialogInterface {
    override val TAG: String = "DialogStoryUnlocked"
    override val name: String = "story_unlocked"
    override val closeButton = null
    override val okButton = null
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonToHome,
    )
}

object DialogTrophyWon : DialogInterface {
    override val TAG: String = "DialogTrophyWon"
    override val name: String = "trophy_won"
    override val closeButton = null
    override val okButton = null
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogTryAgain : DialogInterface {
    override val TAG: String = "DialogTryAgain"
    override val name: String = "try_again"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonTryAgain
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonEndCareer,
        ButtonTryAgain,
    )
}

object DialogUmamusumeDetails : DialogInterface {
    override val TAG: String = "DialogUmamusumeDetails"
    override val name: String = "umamusume_details"
    override val closeButton = null
    override val okButton = null
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogViewStory : DialogInterface {
    override val TAG: String = "DialogViewStory"
    override val name: String = "view_story"
    override val closeButton = null
    override val okButton: ComponentInterface = ButtonOk
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        RadioLandscape,
        RadioPortrait,
        RadioVoiceOff,
    )
}
