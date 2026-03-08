/** Defines button components.
 *
 * Buttons are any element on screen that can be clicked to
 * perform an action.
 *
 * Do not add checkboxes or radio buttons to this file.
 * Those have their own files.
 *
 * Some buttons may have multiple different states. These should use
 * the MultiStateButtonInterface interface instead of ButtonInterface.
 */

package com.steve1316.uma_android_automation.components

import android.graphics.Bitmap
import org.opencv.core.Point

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.components.ButtonInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.components.Region
import com.steve1316.uma_android_automation.utils.CustomImageUtils

object ButtonAgenda : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonAgenda"
    override val template = Template("components/button/agenda")
}

object ButtonAutoSelect : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonAutoSelect"
    override val template = Template("components/button/auto_select")
}

object ButtonBack : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonBack"
    override val template = Template("components/button/back", region = Region.bottomHalf)
}

object ButtonBackGreen : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonBackGreen"
    override val template = Template("components/button/back_green", region = Region.bottomHalf)
}

object ButtonBeginShowdown : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonBeginShowdown"
    override val template = Template("components/button/begin_showdown")
}

object ButtonBorrowSupportCard : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonBorrowSupportCard"
    override val template = Template("components/button/borrow_support_card")
}

object ButtonBurger : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonBurger"
    override val template = Template("components/button/burger", region = Region.bottomHalf)
}

object ButtonCancel : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCancel"
    override val template = Template("components/button/cancel", region = Region.bottomHalf)
}

object ButtonChangeRunningStyle : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChangeRunningStyle"
    override val template = Template("components/button/change")
}

object ButtonClose : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonClose"
    override val template = Template("components/button/close", region = Region.bottomHalf)
}

object ButtonCollectAll : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCollectAll"
    override val template = Template("components/button/collect_all", region = Region.bottomHalf)
}

object ButtonCollect : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCollect"
    override val template = Template("components/button/rewards_collect", region = Region.rightHalf)
}


object ButtonConfirm : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonConfirm"
    override val template = Template("components/button/confirm", region = Region.bottomHalf)
}

object ButtonConfirmExclamation : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonConfirmExclamation"
    override val template = Template("components/button/confirm_exclamation", region = Region.bottomHalf)
}


object ButtonDailyRaces : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRaces"
    override val template = Template("components/button/daily_races")
}

object ButtonDailyRacesDoneForToday : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesDoneForToday"
    override val template = Template("components/button/daily_races_done_for_today")
}

object ButtonDailyRacesJupiterCup : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesJupiterCup"
    override val template = Template("components/button/jupiter_cup")
}

object ButtonDailyRacesJupiterCupRaceSelection : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesJupiterCupRaceSelection"
    override val template = Template("components/button/jupiter_cup_race_selection")
}

object ButtonDailyRacesMoonlightSho : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesMoonlightSho"
    override val template = Template("components/button/moonlight_sho")
}

object ButtonDailyRacesMoonlightShoRaceSelection : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesMoonlightShoRaceSelection"
    override val template = Template("components/button/moonlight_sho_race_selection")
}

object ButtonDailyRacesReplay : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesReplay"
    override val template = Template("components/button/daily_races_replay")
}

object ButtonDailyRacesPlacing : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesPlacing"
    override val template = Template("components/button/daily_races_placing")
}

object ButtonComplete : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonComplete"
    override val template = Template("components/button/complete", region = Region.bottomHalf)
}

object ButtonEditTeam : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonEditTeam"
    override val template = Template("components/button/edit_team")
}

object ButtonFollow : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonFollow"
    override val template = Template("components/button/follow")
}

object ButtonFinish : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonFinish"
    override val template = Template("components/button/finish")
}

object ButtonGiveUp : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonGiveUp"
    override val template = Template("components/button/give_up")
}

object ButtonToHome : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonToHome"
    override val template = Template("components/button/to_home")
}

object ButtonHomeSpecialMissions : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHomeSpecialMissions"
    override val template = Template("components/button/ribbon_home_special_missions")
}

object ButtonHomePresents : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHomePresents"
    override val template = Template("components/button/home_presents")
}

object ButtonSpecialMissionsTabDaily : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSpecialMissionsTabDaily"
    override val template = Template("components/button/special_missions_tab_daily")
}

object ButtonSpecialMissionsTabMain : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSpecialMissionsTabMain"
    override val template = Template("components/button/special_missions_tab_main")
}

object ButtonSpecialMissionsTabTitles : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSpecialMissionsTabTitles"
    override val template = Template("components/button/special_missions_tab_titles")
}

object ButtonSpecialMissionsTabSpecial : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSpecialMissionsTabSpecial"
    override val template = Template("components/button/special_missions_tab_special")
}

object ButtonEventMissions : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonEventMissions"
    override val template = Template("components/button/ribbon_event_missions", region = Region.bottomHalf)
}

object ButtonEventInfo : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonEventInfo"
    override val template = Template("components/button/event_info", region = Region.bottomHalf)
}

object ButtonEventMissionsTabLimitedTime : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonEventMissionsTabLimitedTime"
    override val template = Template("components/button/event_missions_tab_limited_time")
}

object ButtonLater : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonLater"
    override val template = Template("components/button/later")
}

object ButtonLegendRace : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonLegendRace"
    override val template = Template("components/button/legend_race")
}

object ButtonLog : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonLog"
    override val template = Template("components/button/log", region = Region.bottomHalf)
}

object ButtonNext : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonNext"
    override val template = Template("components/button/next", region = Region.bottomHalf)
}

object ButtonNextWithImage : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonNextWithImage"
    override val template = Template("components/button/next_with_image", region = Region.bottomHalf)
}

object ButtonNextRaceEnd : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonNextRaceEnd"
    override val template = Template("components/button/next_race_end", region = Region.bottomHalf)
}

object ButtonNo : ButtonInterface {
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

object ButtonOptions : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonOptions"
    override val template = Template("components/button/options", region = Region.bottomHalf)
}

object ButtonLearn : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonLearn"
    override val template = Template("components/button/learn")
}

object ButtonReset : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonReset"
    override val template = Template("components/button/reset", region = Region.bottomHalf)
}

object ButtonRace : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRace"
    override val template = Template("components/button/race", region = Region.bottomHalf)
}

object ButtonRaceAgain : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceAgain"
    override val template = Template("components/button/race_again", region = Region.bottomHalf)
}

object ButtonRaceDetails : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceDetails"
    override val template = Template("components/button/race_details", region = Region.bottomHalf)
}

object ButtonRaceEvents : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceEvents"
    override val template = Template("components/button/race_events")
}

object ButtonRaceExclamation : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceExclamation"
    override val template = Template("components/button/race_exclamation", region = Region.bottomHalf)
}

object ButtonRaceExclamationShiftedUp : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceExclamationShiftedUp"
    override val template = Template("components/button/race_exclamation_shifted_up", region = Region.middle)
}

object ButtonCirclePlus : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCirclePlus"
    override val template = Template("components/button/circle_plus")
}

object ButtonCircleMinus : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCircleMinus"
    override val template = Template("components/button/circle_minus")
}

object ButtonRaceManual : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceManual"
    override val template = Template("components/button/race_manual", region = Region.bottomHalf)
}

object ButtonRaceRecommendationsCenterStage : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceRecommendationsCenterStage"
    override val template = Template("components/button/race_recommendations_center_stage")
}

object ButtonRaceRecommendationsPathToFame : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceRecommendationsPathToFame"
    override val template = Template("components/button/race_recommendations_path_to_fame")
}

object ButtonRaceRecommendationsForgeYourOwnPath : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceRecommendationsForgeYourOwnPath"
    override val template = Template("components/button/race_recommendations_forge_your_own_path")
}

object ButtonRaceResults : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceResults"
    override val template = Template("components/button/race_results")
}

object ButtonRestore : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRestore"
    override val template = Template("components/button/restore")
}

object ButtonRetry : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRetry"
    override val template = Template("components/button/retry")
}

object ButtonResume : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonResume"
    override val template = Template("components/button/resume")
}

object ButtonSave : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSave"
    override val template = Template("components/button/save", region = Region.bottomHalf)
}

object ButtonSaveSchedule : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSaveSchedule"
    override val template = Template("components/button/save_schedule", region = Region.bottomHalf)
}

object ButtonSaveAndExit : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSaveAndExit"
    override val template = Template("components/button/save_and_exit", region = Region.bottomHalf)
}

object ButtonSeeResults : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSeeResults"
    override val template = Template("components/button/see_results", region = Region.bottomHalf)
}

object ButtonSelectOpponent : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSelectOpponent"
    override val template = Template("components/button/select_opponent", region = Region.bottomHalf)
}

object ButtonSelectLegacy : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSelectLegacy"
    override val template = Template("components/button/select_legacy")
}

object ButtonShop : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShop"
    override val template = Template("components/button/shop")
}

object ButtonReturnToShops : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonReturnToShops"
    override val template = Template("components/button/return_to_shops")
}

object ButtonClub : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonClub"
    override val template = Template("components/button/club")
}

object ButtonClubItemRequest : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonClubItemRequest"
    override val template = Template("components/button/club_item_request")
}

// Used to detect if we are at club screen.
object ButtonClubEmoji : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonClubEmoji"
    override val template = Template("components/button/club_emoji")
}

object ButtonClubViewRequests : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonClubViewRequests"
    override val template = Template("components/button/club_view_requests")
}

object ButtonDonateToAll0 : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDonateToAll0"
    override val template = Template("components/button/donate_to_all_0")
}

object ButtonDonateToAll1 : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDonateToAll1"
    override val template = Template("components/button/donate_to_all_1")
}

object ButtonShoesSprint : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShoesSprint"
    override val template = Template("components/button/shoes_sprint")
}

object ButtonShoesMile : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShoesMile"
    override val template = Template("components/button/shoes_mile")
}

object ButtonShoesMedium : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShoesMedium"
    override val template = Template("components/button/shoes_medium")
}

object ButtonShoesLong : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShoesLong"
    override val template = Template("components/button/shoes_long")
}

object ButtonShoesDirt : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShoesDirt"
    override val template = Template("components/button/shoes_dirt")
}

object ButtonSkip : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSkip"
    override val template = Template("components/button/skip", region = Region.bottomHalf)
}

object ButtonSkills : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSkills"
    override val template = Template("components/button/skills", region = Region.bottomHalf)
}

object ButtonStartCareer : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonStartCareer"
    override val template = Template("components/button/start_career", region = Region.bottomHalf)
}

object ButtonStartCareerOffset : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonStartCareerOffset"
    override val template = Template("components/button/start_career_offset", region = Region.bottomHalf)
}

object ButtonTeamRace : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamRace"
    override val template = Template("components/button/team_race")
}

object ButtonTeamTrials : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamTrials"
    override val template = Template("components/button/team_trials")
}

object ButtonTeamTrialsTallying : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamTrialsTallying"
    override val template = Template("components/button/team_trials_tallying")
}

object ButtonTeamTrialsRaceResultsNext : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamTrialsRaceResultsNext"
    override val template = Template("components/button/team_trials_race_results_next")
}

object ButtonTeamTrialsSeeAllRaceResults : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamTrialsSeeAllRaceResults"
    override val template = Template("components/button/team_trials_see_all_race_results")
}

object ButtonHomeShop : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHomeShop"
    override val template = Template("components/button/home_shop")
}

object ButtonHomeShopDailySale : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHomeShopDailySale"
    override val template = Template("components/button/home_shop_daily_sale")
}

object ButtonShopDailySales : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonShopDailySales"
    override val template = Template("components/button/shop_daily_sales")
}

object ButtonShopEndSale : ButtonInterface {
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

object ButtonTitleScreen : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTitleScreen"
    override val template = Template("components/button/title_screen")
}

object ButtonTryAgain : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTryAgain"
    override val template = Template("components/button/try_again", region = Region.bottomHalf)
}

object ButtonViewResults : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonViewResults"
    override val template = Template("components/button/view_results", region = Region.bottomHalf)
}

object ButtonWatchConcert : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonWatchConcert"
    override val template = Template("components/button/watch_concert", region = Region.bottomHalf)
}

object ButtonRaceStrategyFront : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceStrategyFront"
    override val template = Template("components/button/strategy_front_select", region = Region.middle)
}

object ButtonRaceStrategyPace : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceStrategyPace"
    override val template = Template("components/button/strategy_pace_select", region = Region.middle)
}

object ButtonRaceStrategyLate : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceStrategyLate"
    override val template = Template("components/button/strategy_late_select", region = Region.middle)
}

object ButtonRaceStrategyEnd : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceStrategyEnd"
    override val template = Template("components/button/strategy_end_select", region = Region.middle)
}

object ButtonChampionsMeeting : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChampionsMeeting"
    override val template = Template("components/button/champions_meeting")
}

object ButtonChampionsMeetingEntry : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChampionsMeetingEntry"
    override val template = Template("components/button/champions_meeting_entry")
}

object ButtonChampionsMeetingRegistrationsOpenEntry : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChampionsMeetingRegistrationsOpenEntry"
    override val template = Template("components/button/champions_meeting_registrations_open_entry")
}

object ButtonChampionsMeetingFinalRoundResults : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChampionsMeetingFinalRoundResults"
    override val template = Template("components/button/champions_meeting_final_round_results", region = Region.bottomHalf)
}

object ButtonChampionsMeetingChangeRegistration : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChampionsMeetingChangeRegistration"
    override val template = Template("components/button/champions_meeting_change_registration")
}

object ButtonChampionsMeetingRace : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonChampionsMeetingRace"
    override val template = Template("components/button/champions_meeting_race")
}

object ButtonSpecialMissions : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonSpecialMissions"
    override val template = Template("components/button/ribbon_special_missions")
}

object ButtonRunnerHistory : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRunnerHistory"
    override val template = Template("components/button/ribbon_runner_history")
}

object ButtonLegendRace : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonLegendRace"
    override val template = Template("components/button/legend_race")
}

object ButtonExchange : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonExchange"
    override val template = Template("components/button/exchange")
}

// More complex buttons

object ButtonTeamTrialsQuickModeOff : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTeamTrialsQuickModeOff"
    override val template = Template("components/button/team_trials_quick_mode_off")
}

object ButtonTeamTrialsQuickModeOn : ButtonInterface {
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

object ButtonDailyRacesMultiRaceOff : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonDailyRacesMultiRaceOff"
    override val template = Template("components/button/multi_race_off")
}

object ButtonDailyRacesMultiRaceOn : ButtonInterface {
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

object ButtonCompleteCareer : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCompleteCareer"
    override val template = Template("components/button/complete_career", region = Region.bottomHalf)
}

object ButtonCareerEndSkills : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCareerEndSkills"
    override val template = Template("components/button/career_end_skills")
}

object ButtonCraneGame : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCraneGame"
    override val template = Template("components/button/crane_game", region = Region.bottomHalf)
}

object ButtonCraneGameOk : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonCraneGameOk"
    override val template = Template("components/button/crane_game_ok", region = Region.bottomHalf)
}

object ButtonInheritance : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonInheritance"
    override val template = Template("components/button/inheritance", region = Region.bottomHalf)
}

object ButtonPredictions : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonPredictions"
    override val template = Template("components/button/predictions", region = Region.bottomHalf)
}

object ButtonRunners : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRunners"
    override val template = Template("components/button/runners", region = Region.middle)
}

object ButtonUnityCupRace : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonUnityCupRace"
    override val template = Template("components/button/unitycup_race", region = Region.bottomHalf)
}

object ButtonUnityCupRaceFinal : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonUnityCupRaceFinal"
    override val template = Template("components/button/unitycup_race_final", region = Region.bottomHalf)
}

object ButtonUnityCupSeeAllRaceResults : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonUnityCupSeeAllRaceResults"
    override val template = Template("components/button/unitycup_see_all_race_results", region = Region.bottomHalf)
}

object ButtonUnityCupTeam : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonUnityCupTeam"
    override val template = Template("components/button/unitycup_team", region = Region.bottomHalf)
}

object ButtonUnityCupWatchMainRace : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonUnityCupWatchMainRace"
    override val template = Template("components/button/unitycup_watch_main_race", region = Region.bottomHalf)
}

object ButtonRest : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRest"
    override val template = Template("components/button/rest", region = Region.bottomHalf)
}

object ButtonRestAndRecreation : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRestAndRecreation"
    override val template = Template("components/button/rest_and_recreation", region = Region.bottomHalf)
}

object ButtonInfirmary : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonInfirmary"
    override val template = Template("components/button/infirmary", region = Region.bottomHalf)
}

object ButtonRecreation : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRecreation"
    override val template = Template("components/button/recreation", region = Region.bottomHalf)
}

object ButtonEndCareer : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonEndCareer"
    override val template = Template("components/button/end_career", region = Region.bottomHalf)
}

object ButtonRaceListFullStats : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaceListFullStats"
    override val template = Template("components/button/race_list_full_stats", region = Region.middle)
}

object ButtonSkillListFullStats : ButtonInterface {
    override val TAG: String = "ButtonSkillListFullStats"
    override val template = Template("components/button/skill_list_full_stats", region = Region.topHalf)
}

object ButtonHomeFullStats : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHomeFullStats"
    override val template = Template("components/button/home_full_stats", region = Region.middle)
}

object ButtonHome : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHome"
    override val template = Template("components/button/home")
}

object ButtonTrainingSpeed : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTrainingSpeed"
    override val template = Template("components/button/training_speed", region = Region.bottomHalf)
}

object ButtonTrainingStamina : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTrainingStamina"
    override val template = Template("components/button/training_stamina", region = Region.bottomHalf)
}

object ButtonTrainingPower : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTrainingPower"
    override val template = Template("components/button/training_power", region = Region.bottomHalf)
}

object ButtonTrainingGuts : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTrainingGuts"
    override val template = Template("components/button/training_guts", region = Region.bottomHalf)
}

object ButtonTrainingWit : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTrainingWit"
    override val template = Template("components/button/training_wit", region = Region.bottomHalf)
}

object ButtonTraining : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonTraining"
    override val template = Template("components/button/training", region = Region.bottomHalf)
}

object ButtonRaces : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonRaces"
    override val template = Template("components/button/races", region = Region.bottomHalf)
}

object ButtonHomeFansInfo : ButtonInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]ButtonHomeFansInfo"
    override val template = Template("components/button/home_fans_info", region = Region.leftHalf)
}

object ButtonSkillUp : ButtonInterface {
    override val TAG: String = "ButtonSkillUp"
    override val template = Template("components/button/skill_up", region = Region.rightHalf)
}

object ButtonSkillDown : ButtonInterface {
    override val TAG: String = "ButtonSkillDown"
    override val template = Template("components/button/skill_down", region = Region.rightHalf)
}

object ButtonRaceExclamationPink : ButtonInterface {
    override val TAG: String = "ButtonRaceExclamationPink"
    override val template = Template("components/button/race_exclamation_pink", region = Region.bottomHalf)
}

object ButtonPlacing : ButtonInterface {
    override val TAG: String = "ButtonPlacing"
    override val template = Template("components/button/placing", region = Region.bottomHalf)
}

object ButtonClaim : ButtonInterface {
    override val TAG: String = "ButtonClaim"
    override val template = Template("components/button/claim", region = Region.bottomHalf)
}

object ButtonRewards : ButtonInterface {
    override val TAG: String = "ButtonRewards"
    override val template = Template("components/button/rewards", region = Region.bottomHalf)
}

object ButtonReplayWithImage : ButtonInterface {
    override val TAG: String = "ButtonReplayWithImage"
    override val template = Template("components/button/replay_with_image", region = Region.bottomHalf)
}

object ButtonOverwrite : ButtonInterface {
    override val TAG: String = "ButtonOverwrite"
    override val template = Template("components/button/overwrite", region = Region.bottomHalf)
}
