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

    fun check(tries: Int = 1): Boolean {
        return (
            ImageUtils.findImage("dialog/dialog_title_${name}", tries=tries, region=Screen.TOP_HALF).first != null
        )
    }
}

// Simple object used to store a list of all dialog objects.
// This is used to easily iterate over all dialogs.
object DialogObjects {
    val items: List<DialogInterface> = listOf<DialogInterface>(
        DialogCareerComplete,
        DialogConfirmExchange,
        DialogConfirmRestoreRP,
        DialogConcertSkipConfirmation,
        DialogDailySale,
        DialogDateChanged,
        DialogExternalLink,
        DialogFollowTrainer,
        DialogInfirmary,
        DialogItemsSelected,
        DialogNotices,
        DialogOpenSoon,
        DialogPresents,
        DialogPurchaseDailyRaceTicket,
        DialogRaceDetails,
        DialogRacePlayback,
        DialogRaceRecommendations,
        DialogRecreation,
        DialogRest,
        DialogRestAndRecreation,
        DialogRewardsCollected,
        DialogSessionError,
        DialogSongAcquired,
        DialogSpecialMissions,
        DialogStrategy,
        DialogStoryUnlocked,
        DialogTrophyWon,
        DialogTryAgain,
        DialogViewStory,
    )
}

object DialogCareerComplete : DialogInterface {
    override val TAG: String = "DialogCareerComplete"
    override val name: String = "career_complete"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
        ButtonEditTeam,
    )
}

object DialogConfirmExchange : DialogInterface {
    override val TAG: String = "DialogConfirmExchange"
    override val name: String = "confirm_exchange"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogConfirmRestoreRP : DialogInterface {
    override val TAG: String = "DialogConfirmRestoreRP"
    override val name: String = "confirm_restore_rp"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonNo,
        ButtonRestore,
    )
}

object DialogConcertSkipConfirmation : DialogInterface {
    override val TAG: String = "DialogConcertSkipConfirmation"
    override val name: String = "concert_skip_confirmation"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
    )
}

object DialogDailySale : DialogInterface {
    override val TAG: String = "DialogDailySale"
    override val name: String = "daily_sale"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonShop,
    )
}

object DialogDateChanged : DialogInterface {
    override val TAG: String = "DialogDateChanged"
    override val name: String = "date_changed"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonOk,
    )
}

object DialogEpithet : DialogInterface {
    override val TAG: String = "DialogEpithet"
    override val name: String = "epithet"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonConfirmExclamation,
        Checkbox,
    )
}

object DialogExternalLink : DialogInterface {
    override val TAG: String = "DialogExternalLink"
    override val name: String = "external_link"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
    )
}

object DialogFollowTrainer : DialogInterface {
    override val TAG: String = "DialogFollowTrainer"
    override val name: String = "follow_trainer"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonFollow,
    )
}

object DialogInfirmary : DialogInterface {
    override val TAG: String = "DialogInfirmary"
    override val name: String = "infirmary"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
    )
}

object DialogItemsSelected : DialogInterface {
    override val TAG: String = "DialogItemsSelected"
    override val name: String = "items_selected"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonRaceExclamationShiftedUp,
    )
}

object DialogNotices : DialogInterface {
    override val TAG: String = "DialogNotices"
    override val name: String = "notices"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogOpenSoon : DialogInterface {
    override val TAG: String = "DialogOpenSoon"
    override val name: String = "open_soon"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogPresents : DialogInterface {
    override val TAG: String = "DialogPresents"
    override val name: String = "presents"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonCollectAll,
    )
}

object DialogPurchaseDailyRaceTicket : DialogInterface {
    override val TAG: String = "DialogPurchaseDailyRaceTicket"
    override val name: String = "purchase_daily_race_ticket"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
    )
}

object DialogRaceDetails : DialogInterface {
    override val TAG: String = "DialogRaceDetails"
    override val name: String = "race_details"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonRaceExclamation,
    )
}

object DialogRacePlayback : DialogInterface {
    override val TAG: String = "DialogRacePlayback"
    override val name: String = "race_playback"
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
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonRaceRecommendationsCenterStage,
        ButtonRaceRecommendationsPathToFame,
        ButtonRaceRecommendationsForgeYourOwnPath,
        Checkbox,
        ButtonConfirm,
    )
}

object DialogRecreation : DialogInterface {
    override val TAG: String = "DialogRecreation"
    override val name: String = "recreation"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
    )
}

object DialogRest : DialogInterface {
    override val TAG: String = "DialogRest"
    override val name: String = "rest"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
    )
}

object DialogRestAndRecreation : DialogInterface {
    override val TAG: String = "DialogRestAndRecreation"
    override val name: String = "rest_and_recreation"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
    )
}

object DialogRewardsCollected : DialogInterface {
    override val TAG: String = "DialogRewardsCollected"
    override val name: String = "rewards_collected"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogSessionError : DialogInterface {
    override val TAG: String = "DialogSessionError"
    override val name: String = "session_error"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonTitleScreen,
    )
}

object DialogSongAcquired : DialogInterface {
    override val TAG: String = "DialogSongAcquired"
    override val name: String = "song_acquired"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogSpecialMissions : DialogInterface {
    override val TAG: String = "DialogSpecialMissions"
    override val name: String = "special_missions"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonOk,
        ButtonCollectAll,
    )
}

object DialogStrategy : DialogInterface {
    override val TAG: String = "DialogStrategy"
    override val name: String = "strategy"
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
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonToHome,
    )
}

object DialogTrophyWon : DialogInterface {
    override val TAG: String = "DialogTrophyWon"
    override val name: String = "trophy_won"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogTryAgain : DialogInterface {
    override val TAG: String = "DialogTryAgain"
    override val name: String = "try_again"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonEndCareer,
        ButtonTryAgain,
    )
}

object DialogViewStory : DialogInterface {
    override val TAG: String = "DialogViewStory"
    override val name: String = "view_story"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        RadioLandscape,
        RadioPortrait,
        RadioVoiceOff,
    )
}


