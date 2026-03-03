/** Defines button components.
 *
 * Buttons are any element on screen that can be clicked to
 * perform an action.
 *
 * Do not add checkboxes or radio buttons to this file.
 * Those have their own files.
 *
 * Some buttons may have multiple different states. These should use
 * the MultiStateButtonInterface interface instead of ComponentInterface.
 */

package com.steve1316.uma_android_automation.components

import android.graphics.Bitmap
import org.opencv.core.Point

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.components.Region
import com.steve1316.uma_android_automation.utils.CustomImageUtils

object ButtonAgenda : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonAgenda"
    override val template = Template("components/button/agenda")
}

object ButtonAutoSelect : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonAutoSelect"
    override val template = Template("components/button/auto_select")
}

object ButtonBack : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonBack"
    override val template = Template("components/button/back", region = Region.bottomHalf)
}

object ButtonBackGreen : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonBackGreen"
    override val template = Template("components/button/back_green", region = Region.bottomHalf)
}

object ButtonBeginShowdown : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonBeginShowdown"
    override val template = Template("components/button/begin_showdown")
}

object ButtonBorrowSupportCard : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonBorrowSupportCard"
    override val template = Template("components/button/borrow_support_card")
}

object ButtonBurger : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonBurger"
    override val template = Template("components/button/burger", region = Region.bottomHalf)
}

object ButtonCancel : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCancel"
    override val template = Template("components/button/cancel", region = Region.bottomHalf)
}

object ButtonChangeRunningStyle : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChangeRunningStyle"
    override val template = Template("components/button/change")
}

object ButtonClose : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonClose"
    override val template = Template("components/button/close", region = Region.bottomHalf)
}

object ButtonCollectAll : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCollectAll"
    override val template = Template("components/button/collect_all", region = Region.bottomHalf)
}

object ButtonCollect : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCollect"
    override val template = Template("components/button/rewards_collect", region = Region.rightHalf)
}


object ButtonConfirm : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonConfirm"
    override val template = Template("components/button/confirm", region = Region.bottomHalf)
}

object ButtonConfirmExclamation : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonConfirmExclamation"
    override val template = Template("components/button/confirm_exclamation", region = Region.bottomHalf)
}

object ButtonDailyRaces : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRaces"
    override val template = Template("components/button/daily_races")
}

object ButtonDailyRacesDoneForToday : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesDoneForToday"
    override val template = Template("components/button/daily_races_done_for_today")
}

object ButtonDailyRacesJupiterCup : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesJupiterCup"
    override val template = Template("components/button/jupiter_cup")
}

object ButtonDailyRacesJupiterCupRaceSelection : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesJupiterCupRaceSelection"
    override val template = Template("components/button/jupiter_cup_race_selection")
}

object ButtonDailyRacesMoonlightSho : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesMoonlightSho"
    override val template = Template("components/button/moonlight_sho")
}

object ButtonDailyRacesMoonlightShoRaceSelection : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesMoonlightShoRaceSelection"
    override val template = Template("components/button/moonlight_sho_race_selection")
}

object ButtonDailyRacesReplay : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesReplay"
    override val template = Template("components/button/daily_races_replay")
}

object ButtonDailyRacesPlacing : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesPlacing"
    override val template = Template("components/button/daily_races_placing")
}

object ButtonComplete : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonComplete"
    override val template = Template("components/button/complete", region = Region.bottomHalf)
}

object ButtonEditTeam : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonEditTeam"
    override val template = Template("components/button/edit_team")
}

object ButtonFollow : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonFollow"
    override val template = Template("components/button/follow")
}

object ButtonFinish : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonFinish"
    override val template = Template("components/button/finish")
}

object ButtonGiveUp : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonGiveUp"
    override val template = Template("components/button/give_up")
}

object ButtonToHome : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonToHome"
    override val template = Template("components/button/to_home")
}

object ButtonHomeSpecialMissions : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHomeSpecialMissions"
    override val template = Template("components/button/ribbon_home_special_missions")
}

object ButtonHomePresents : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHomePresents"
    override val template = Template("components/button/home_presents")
}

object ButtonSpecialMissionsTabDaily : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSpecialMissionsTabDaily"
    override val template = Template("components/button/special_missions_tab_daily")
}

object ButtonSpecialMissionsTabMain : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSpecialMissionsTabMain"
    override val template = Template("components/button/special_missions_tab_main")
}

object ButtonSpecialMissionsTabTitles : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSpecialMissionsTabTitles"
    override val template = Template("components/button/special_missions_tab_titles")
}

object ButtonSpecialMissionsTabSpecial : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSpecialMissionsTabSpecial"
    override val template = Template("components/button/special_missions_tab_special")
}

object ButtonEventMissions : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonEventMissions"
    override val template = Template("components/button/ribbon_event_missions", region = Region.bottomHalf)
}

object ButtonEventInfo : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonEventInfo"
    override val template = Template("components/button/event_info", region = Region.bottomHalf)
}

object ButtonEventMissionsTabLimitedTime : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonEventMissionsTabLimitedTime"
    override val template = Template("components/button/event_missions_tab_limited_time")
}

object ButtonLater : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonLater"
    override val template = Template("components/button/later")
}

object ButtonLog : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonLog"
    override val template = Template("components/button/log", region = Region.bottomHalf)
}

object ButtonNext : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonNext"
    override val template = Template("components/button/next", region = Region.bottomHalf)
}

object ButtonNextWithImage : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonNextWithImage"
    override val template = Template("components/button/next_with_image", region = Region.bottomHalf)
}

object ButtonNextRaceEnd : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonNextRaceEnd"
    override val template = Template("components/button/next_race_end", region = Region.bottomHalf)
}

object ButtonNo : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonNo"
    override val template = Template("components/button/no", region = Region.bottomHalf)
}

object ButtonOk : MultiStateButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonOk"
    override val templates: List<Template> = listOf(
        Template("components/button/ok", region = Region.bottomHalf),
        Template("components/button/ok_gray", region = Region.bottomHalf),
    )
}

object ButtonOptions : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonOptions"
    override val template = Template("components/button/options", region = Region.bottomHalf)
}

object ButtonLearn : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonLearn"
    override val template = Template("components/button/learn")
}

object ButtonReset : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonReset"
    override val template = Template("components/button/reset", region = Region.bottomHalf)
}

object ButtonRace : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRace"
    override val template = Template("components/button/race", region = Region.bottomHalf)
}

object ButtonRaceAgain : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceAgain"
    override val template = Template("components/button/race_again", region = Region.bottomHalf)
}

object ButtonRaceDetails : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceDetails"
    override val template = Template("components/button/race_details", region = Region.bottomHalf)
}

object ButtonRaceEvents : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceEvents"
    override val template = Template("components/button/race_events")
}

object ButtonRaceExclamation : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceExclamation"
    override val template = Template("components/button/race_exclamation", region = Region.bottomHalf)
}

object ButtonRaceExclamationShiftedUp : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceExclamationShiftedUp"
    override val template = Template("components/button/race_exclamation_shifted_up", region = Region.middle)
}

object ButtonCirclePlus : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCirclePlus"
    override val template = Template("components/button/circle_plus")
}

object ButtonCircleMinus : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCircleMinus"
    override val template = Template("components/button/circle_minus")
}

object ButtonRaceManual : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceManual"
    override val template = Template("components/button/race_manual", region = Region.bottomHalf)
}

object ButtonRaceRecommendationsCenterStage : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceRecommendationsCenterStage"
    override val template = Template("components/button/race_recommendations_center_stage")
}

object ButtonRaceRecommendationsPathToFame : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceRecommendationsPathToFame"
    override val template = Template("components/button/race_recommendations_path_to_fame")
}

object ButtonRaceRecommendationsForgeYourOwnPath : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceRecommendationsForgeYourOwnPath"
    override val template = Template("components/button/race_recommendations_forge_your_own_path")
}

object ButtonRaceResults : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceResults"
    override val template = Template("components/button/race_results")
}

object ButtonRestore : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRestore"
    override val template = Template("components/button/restore")
}

object ButtonRetry : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRetry"
    override val template = Template("components/button/retry")
}

object ButtonResume : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonResume"
    override val template = Template("components/button/resume")
}

object ButtonSave : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSave"
    override val template = Template("components/button/save", region = Region.bottomHalf)
}

object ButtonSaveSchedule : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSaveSchedule"
    override val template = Template("components/button/save_schedule", region = Region.bottomHalf)
}

object ButtonSaveAndExit : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSaveAndExit"
    override val template = Template("components/button/save_and_exit", region = Region.bottomHalf)
}

object ButtonSeeResults : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSeeResults"
    override val template = Template("components/button/see_results", region = Region.bottomHalf)
}

object ButtonSelectOpponent : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSelectOpponent"
    override val template = Template("components/button/select_opponent", region = Region.bottomHalf)
}

object ButtonSelectLegacy : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSelectLegacy"
    override val template = Template("components/button/select_legacy")
}

object ButtonShop : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShop"
    override val template = Template("components/button/shop")
}

object ButtonReturnToShops : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonReturnToShops"
    override val template = Template("components/button/return_to_shops")
}

object ButtonClub : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonClub"
    override val template = Template("components/button/club")
}

object ButtonClubItemRequest : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonClubItemRequest"
    override val template = Template("components/button/club_item_request")
}

// Used to detect if we are at club screen.
object ButtonClubEmoji : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonClubEmoji"
    override val template = Template("components/button/club_emoji")
}

object ButtonClubViewRequests : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonClubViewRequests"
    override val template = Template("components/button/club_view_requests")
}

object ButtonDonateToAll0 : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDonateToAll0"
    override val template = Template("components/button/donate_to_all_0")
}

object ButtonDonateToAll1 : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDonateToAll1"
    override val template = Template("components/button/donate_to_all_1")
}

object ButtonShoesSprint : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShoesSprint"
    override val template = Template("components/button/shoes_sprint")
}

object ButtonShoesMile : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShoesMile"
    override val template = Template("components/button/shoes_mile")
}

object ButtonShoesMedium : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShoesMedium"
    override val template = Template("components/button/shoes_medium")
}

object ButtonShoesLong : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShoesLong"
    override val template = Template("components/button/shoes_long")
}

object ButtonShoesDirt : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShoesDirt"
    override val template = Template("components/button/shoes_dirt")
}

object ButtonSkip : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSkip"
    override val template = Template("components/button/skip", region = Region.bottomHalf)
}

object ButtonSkills : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSkills"
    override val template = Template("components/button/skills", region = Region.bottomHalf)
}

object ButtonStartCareer : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonStartCareer"
    override val template = Template("components/button/start_career", region = Region.bottomHalf)
}

object ButtonStartCareerOffset : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonStartCareerOffset"
    override val template = Template("components/button/start_career_offset", region = Region.bottomHalf)
}

object ButtonTeamRace : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamRace"
    override val template = Template("components/button/team_race")
}

object ButtonTeamTrials : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamTrials"
    override val template = Template("components/button/team_trials")
}

object ButtonTeamTrialsTallying : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamTrialsTallying"
    override val template = Template("components/button/team_trials_tallying")
}

object ButtonTeamTrialsRaceResultsNext : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamTrialsRaceResultsNext"
    override val template = Template("components/button/team_trials_race_results_next")
}

object ButtonTeamTrialsSeeAllRaceResults : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamTrialsSeeAllRaceResults"
    override val template = Template("components/button/team_trials_see_all_race_results")
}

object ButtonHomeShop : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHomeShop"
    override val template = Template("components/button/home_shop")
}

object ButtonHomeShopDailySale : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHomeShopDailySale"
    override val template = Template("components/button/home_shop_daily_sale")
}

object ButtonShopDailySales : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShopDailySales"
    override val template = Template("components/button/shop_daily_sales")
}

object ButtonShopEndSale : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShopEndSale"
    override val template = Template("components/button/end_sale", region = Region.bottomHalf)
}

object ButtonShopExchange : MultiStateButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShopExchange"
    override val templates: List<Template> = listOf(
        Template("components/button/shop_exchange", region = Region.rightHalf),
        Template("components/button/shop_exchange_disabled", region = Region.rightHalf),
    )

    // Special case since this component's disabled state actually requires a
    // different template to detect.
    override fun checkDisabled(imageUtils: CustomImageUtils, sourceBitmap: Bitmap?): Boolean? {
        val sourceBitmap: Bitmap = sourceBitmap ?: imageUtils.getSourceBitmap()
        for ((index, template) in templates.withIndex()) {
            val point: Point? = imageUtils.findImageWithBitmap(
                template.path,
                sourceBitmap = sourceBitmap,
                region = template.region,
                customConfidence = template.confidence,
                suppressError = true,
            )
            if (point != null) {
                return index != 0
            }
        }
        return null
    }
}

object ButtonTitleScreen : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTitleScreen"
    override val template = Template("components/button/title_screen")
}

object ButtonTryAgain : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTryAgain"
    override val template = Template("components/button/try_again", region = Region.bottomHalf)
}

object ButtonViewResults : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonViewResults"
    override val template = Template("components/button/view_results", region = Region.bottomHalf)
}

object ButtonWatchConcert : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonWatchConcert"
    override val template = Template("components/button/watch_concert", region = Region.bottomHalf)
}

object ButtonRaceStrategyFront : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceStrategyFront"
    override val template = Template("components/button/strategy_front_select", region = Region.middle)
}

object ButtonRaceStrategyPace : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceStrategyPace"
    override val template = Template("components/button/strategy_pace_select", region = Region.middle)
}

object ButtonRaceStrategyLate : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceStrategyLate"
    override val template = Template("components/button/strategy_late_select", region = Region.middle)
}

object ButtonRaceStrategyEnd : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceStrategyEnd"
    override val template = Template("components/button/strategy_end_select", region = Region.middle)
}

object ButtonChampionsMeeting : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChampionsMeeting"
    override val template = Template("components/button/champions_meeting")
}

object ButtonChampionsMeetingEntry : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChampionsMeetingEntry"
    override val template = Template("components/button/champions_meeting_entry")
}

object ButtonChampionsMeetingRegistrationsOpenEntry : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChampionsMeetingRegistrationsOpenEntry"
    override val template = Template("components/button/champions_meeting_registrations_open_entry")
}

object ButtonChampionsMeetingFinalRoundResults : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChampionsMeetingFinalRoundResults"
    override val template = Template("components/button/champions_meeting_final_round_results", region = Region.bottomHalf)
}

object ButtonChampionsMeetingChangeRegistration : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChampionsMeetingChangeRegistration"
    override val template = Template("components/button/champions_meeting_change_registration")
}

object ButtonChampionsMeetingRace : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChampionsMeetingRace"
    override val template = Template("components/button/champions_meeting_race")
}

object ButtonSpecialMissions : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSpecialMissions"
    override val template = Template("components/button/ribbon_special_missions")
}

object ButtonRunnerHistory : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRunnerHistory"
    override val template = Template("components/button/ribbon_runner_history")
}

object ButtonLegendRace : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonLegendRace"
    override val template = Template("components/button/legend_race")
}

object ButtonExchange : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonExchange"
    override val template = Template("components/button/exchange")
}

// More complex buttons

object ButtonTeamTrialsQuickModeOff : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamTrialsQuickModeOff"
    override val template = Template("components/button/team_trials_quick_mode_off")
}

object ButtonTeamTrialsQuickModeOn : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamTrialsQuickModeOn"
    override val template = Template("components/button/team_trials_quick_mode_on")
}

object ButtonTeamTrialsQuickMode : MultiStateButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamTrialsQuickMode"
    override val templates: List<Template> = listOf(
        Template("components/button/team_trials_quick_mode_off"),
        Template("components/button/team_trials_quick_mode_on"),
    )
}

object ButtonDailyRacesMultiRaceOff : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesMultiRaceOff"
    override val template = Template("components/button/multi_race_off")
}

object ButtonDailyRacesMultiRaceOn : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesMultiRaceOn"
    override val template = Template("components/button/multi_race_on")
}

object ButtonDailyRacesMultiRace : MultiStateButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesMultiRace"
    override val templates: List<Template> = listOf(
        Template("components/button/multi_race_off"),
        Template("components/button/multi_race_on"),
    )
}

object ButtonCompleteCareer : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCompleteCareer"
    override val template = Template("components/button/complete_career", region = Region.bottomHalf)
}

object ButtonCareerEndSkills : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCareerEndSkills"
    override val template = Template("components/button/career_end_skills")
}

object ButtonCraneGame : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCraneGame"
    override val template = Template("components/button/crane_game", region = Region.bottomHalf)
}

object ButtonCraneGameOk : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCraneGameOk"
    override val template = Template("components/button/crane_game_ok", region = Region.bottomHalf)
}

object ButtonInheritance : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonInheritance"
    override val template = Template("components/button/inheritance", region = Region.bottomHalf)
}

object ButtonPredictions : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonPredictions"
    override val template = Template("components/button/predictions", region = Region.bottomHalf)
}

object ButtonRunners : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRunners"
    override val template = Template("components/button/runners", region = Region.middle)
}

object ButtonUnityCupRace : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonUnityCupRace"
    override val template = Template("components/button/unitycup_race", region = Region.bottomHalf)
}

object ButtonUnityCupRaceFinal : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonUnityCupRaceFinal"
    override val template = Template("components/button/unitycup_race_final", region = Region.bottomHalf)
}

object ButtonUnityCupSeeAllRaceResults : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonUnityCupSeeAllRaceResults"
    override val template = Template("components/button/unitycup_see_all_race_results", region = Region.bottomHalf)
}

object ButtonUnityCupTeam : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonUnityCupTeam"
    override val template = Template("components/button/unitycup_team", region = Region.bottomHalf)
}

object ButtonUnityCupWatchMainRace : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonUnityCupWatchMainRace"
    override val template = Template("components/button/unitycup_watch_main_race", region = Region.bottomHalf)
}

object ButtonRest : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRest"
    override val template = Template("components/button/rest", region = Region.bottomHalf)
}

object ButtonRestAndRecreation : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRestAndRecreation"
    override val template = Template("components/button/rest_and_recreation", region = Region.bottomHalf)
}

object ButtonInfirmary : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonInfirmary"
    override val template = Template("components/button/infirmary", region = Region.bottomHalf)
}

object ButtonRecreation : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRecreation"
    override val template = Template("components/button/recreation", region = Region.bottomHalf)
}

object ButtonEndCareer : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonEndCareer"
    override val template = Template("components/button/end_career", region = Region.bottomHalf)
}

object ButtonRaceListFullStats : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceListFullStats"
    override val template = Template("components/button/race_list_full_stats", region = Region.middle)
}

object ButtonSkillListFullStats : ComponentInterface {
    override val TAG: String = "ButtonSkillListFullStats"
    override val template = Template("components/button/skill_list_full_stats", region = Region.topHalf)
}

object ButtonHomeFullStats : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHomeFullStats"
    override val template = Template("components/button/home_full_stats", region = Region.middle)
}

object ButtonHome : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHome"
    override val template = Template("components/button/home")
}

object ButtonTrainingSpeed : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTrainingSpeed"
    override val template = Template("components/button/training_speed", region = Region.bottomHalf)
}

object ButtonTrainingStamina : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTrainingStamina"
    override val template = Template("components/button/training_stamina", region = Region.bottomHalf)
}

object ButtonTrainingPower : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTrainingPower"
    override val template = Template("components/button/training_power", region = Region.bottomHalf)
}

object ButtonTrainingGuts : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTrainingGuts"
    override val template = Template("components/button/training_guts", region = Region.bottomHalf)
}

object ButtonTrainingWit : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTrainingWit"
    override val template = Template("components/button/training_wit", region = Region.bottomHalf)
}

object ButtonTraining : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTraining"
    override val template = Template("components/button/training", region = Region.bottomHalf)
}

object ButtonRaces : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaces"
    override val template = Template("components/button/races", region = Region.bottomHalf)
}

object ButtonHomeFansInfo : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHomeFansInfo"
    override val template = Template("components/button/home_fans_info", region = Region.leftHalf)
}

object ButtonSkillUp : ComponentInterface {
    override val TAG: String = "ButtonSkillUp"
    override val template = Template("components/button/skill_up", region = Region.rightHalf)
}

object ButtonSkillDown : ComponentInterface {
    override val TAG: String = "ButtonSkillDown"
    override val template = Template("components/button/skill_down", region = Region.rightHalf)
}

object ButtonRaceExclamationPink : ComponentInterface {
    override val TAG: String = "ButtonRaceExclamationPink"
    override val template = Template("components/button/race_exclamation_pink", region = Region.bottomHalf)
}

object ButtonPlacing : ComponentInterface {
    override val TAG: String = "ButtonPlacing"
    override val template = Template("components/button/placing", region = Region.bottomHalf)
}

object ButtonClaim : ComponentInterface {
    override val TAG: String = "ButtonClaim"
    override val template = Template("components/button/claim", region = Region.bottomHalf)
}

object ButtonRewards : ComponentInterface {
    override val TAG: String = "ButtonRewards"
    override val template = Template("components/button/rewards", region = Region.bottomHalf)
}

object ButtonReplayWithImage : ComponentInterface {
    override val TAG: String = "ButtonReplayWithImage"
    override val template = Template("components/button/replay_with_image", region = Region.bottomHalf)
}
