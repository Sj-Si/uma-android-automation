package com.steve1316.uma_android_automation.bot.dialog

import com.steve1316.uma_android_automation.utils.AppEvent
import com.steve1316.uma_android_automation.utils.EventBus
import com.steve1316.uma_android_automation.utils.Screen
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.*

interface DialogInterface {
    val TAG: String // logging tag
    val name: String // Template name
    val buttons: List<ComponentInterface>

    fun check(tries: Int = 1): Boolean {
        return (
            ImageUtils.findImage("dialog_title_gradient", tries=tries, region=Screen.TOP_HALF) &&
            ImageUtils.findImage("dialog_title_${name}", tries=tries, region=Screen.TOP_HALF)
        )
    }
}

// Simple object used to store a list of all dialog objects.
// This is used to easily iterate over all dialogs.
object DialogObjects {
    val items: List<DialogInterface> = listOf<DialogInterface>(
        DialogConfirmExchange,
        DialogConfirmRestoreRP,
        DialogConcertSkipConfirmation,
        DialogDailySale,
        DialogDateChanged,
        DialogExternalLink,
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
        DialogTrophyWon,
        DialogTryAgain,
        DialogViewStory,
    )
}

object DialogConfirmExchange : DialogInterface {
    override val name: String = "confirm_exchange"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogConfirmRestoreRP : DialogInterface {
    override val name: String = "confirm_restore_rp"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonNo,
        ButtonRestore,
    )
}

object DialogConcertSkipConfirmation : DialogInterface {
    override val name: String = "concert_skip_confirmation"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
    )
}

object DialogDailySale : DialogInterface {
    override val name: String = "daily_sale"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonShop,
    )
}

object DialogDateChanged : DialogInterface {
    override val name: String = "date_changed"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonOk,
    )
}

object DialogExternalLink : DialogInterface {
    override val name: String = "external_link"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
    )
}

object DialogInfirmary : DialogInterface {
    override val name: String = "infirmary"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
    )
}

object DialogItemsSelected : DialogInterface {
    override val name: String = "items_selected"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonRaceExclamationShiftedUp,
    )
}

object DialogNotices : DialogInterface {
    override val name: String = "notices"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogOpenSoon : DialogInterface {
    override val name: String = "open_soon"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogPresents : DialogInterface {
    override val name: String = "presents"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonCollectAll,
    )
}

object DialogPurchaseDailyRaceTicket : DialogInterface {
    override val name: String = "purchase_daily_race_ticket"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
    )
}

object DialogRaceDetails : DialogInterface {
    override val name: String = "race_details"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonRaceExclamation,
    )
}

object DialogRacePlayback : DialogInterface {
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
    override val name: String = "recreation"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
    )
}

object DialogRest : DialogInterface {
    override val name: String = "rest"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        Checkbox,
    )
}

object DialogRestAndRecreation : DialogInterface {
    override val name: String = "rest_and_recreation"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
    )
}

object DialogRewardsCollected : DialogInterface {
    override val name: String = "rewards_collected"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogSessionError : DialogInterface {
    override val name: String = "session_error"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonTitleScreen,
    )
}

object DialogSongAcquired : DialogInterface {
    override val name: String = "song_acquired"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogSpecialMissions : DialogInterface {
    override val name: String = "special_missions"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonOk,
        ButtonCollectAll,
    )
}

object DialogStrategy : DialogInterface {
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

object DialogTrophyWon : DialogInterface {
    override val name: String = "trophy_won"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonClose,
    )
}

object DialogTryAgain : DialogInterface {
    override val name: String = "try_again"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonEndCareer,
        ButtonTryAgain,
    )
}

object DialogViewStory : DialogInterface {
    override val name: String = "view_story"
    override val buttons: List<ComponentInterface> = listOf<ComponentInterface>(
        ButtonCancel,
        ButtonOk,
        RadioLandscape,
        RadioPortrait,
        RadioVoiceOff,
    )
}


