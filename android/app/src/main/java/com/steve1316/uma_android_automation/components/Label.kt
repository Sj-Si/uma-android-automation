/** Defines label components.
 *
 * These are non-clickable regions of text on screen.
 */

package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.components.Region

object LabelStatDistance : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelStatDistance"
    override val template = Template("components/label/stat_distance", region = Region.topHalf)
}

object LabelStatTrackSurface : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelStatTrackSurface"
    override val template = Template("components/label/stat_track_surface", region = Region.topHalf)
}

object LabelStatStyle : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelStatStyle"
    override val template = Template("components/label/stat_style", region = Region.topHalf)
}

object LabelUmamusumeClassFans : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelUmamusumeClassFans"
    override val template = Template("components/label/umamusume_class_fans", region = Region.middle)
}

object LabelStatTableHeaderSkillPoints : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelStatTableHeaderSkillPoints"
    override val template = Template("components/label/stat_table_header_skill_points", region = Region.bottomHalf)
}

object LabelTrainingFailureChance : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelTrainingFailureChance"
    override val template = Template("components/label/training_failure_chance", region = Region.bottomHalf)
}

object LabelWinToBecomeRank : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelWinToBecomeRank"
    override val template = Template("components/label/win_to_become_rank")
}

object LabelUnityCupOpponentSelectionLaurel : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelUnityCupOpponentSelectionLaurel"
    override val template = Template("components/label/unitycup_opponent_selection_laurel", region = Region.leftHalf)
}

object LabelEnergy : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelEnergy"
    override val template = Template("components/label/energy")
}

object LabelEnergyBarLeftPart : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelEnergyBarLeftPart"
    override val template = Template("components/label/energy_bar_left_part")
}

object LabelEnergyBarRightPart : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelEnergyBarRightPart"
    override val template = Template("components/label/energy_bar_right_part_0")
}

object LabelEnergyBarExtendedRightPart : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelEnergyBarExtendedRightPart"
    override val template = Template("components/label/energy_bar_right_part_1")
}

object LabelSkillListScreenSkillPoints : ComponentInterface {
    override val TAG: String = "LabelSkillListScreenSkillPoints"
    override val template = Template("components/label/skill_list_screen_skill_points", region = Region.topHalf)
}

object LabelScheduledRace : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelScheduledRace"
    override val template = Template("components/label/scheduled_race", region = Region.bottomHalf)
}

object LabelTrainingCannotPerform : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelTrainingCannotPerform"
    override val template = Template("components/label/training_cannot_perform", region = Region.middle)
}

object LabelTrophyWonDialogTitle : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelTrophyWonDialogTitle"
    override val template = Template("components/label/trophy_won")
}

object LabelRaceMenuBanner : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelRaceMenuBanner"
    override val template = Template("components/label/race_menu_banner")
}

object LabelTeamTrialsExtraRewardWithEveryWin : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelTeamTrialsExtraRewardWithEveryWin"
    override val template = Template("components/label/team_trials_extra_reward_with_every_win")
}

object LabelTeamTrialsSelectOpponent : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelTeamTrialsSelectOpponent"
    override val template = Template("components/label/team_trials_select_opponent", region = Region.topHalf)
}

object LabelShopStarPiece : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelShopStarPiece"
    override val template = Template("components/label/shop_star_piece")
}

object LabelShopAlarmClock : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelShopAlarmClock"
    override val template = Template("components/label/shop_alarm_clock")
}

object LabelShopPleasingParfait : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelShopPleasingParfait"
    override val template = Template("components/label/shop_pleasing_parfait")
}

object LabelYouHaveReachedTheDailyTicketPurchaseLimit : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelYouHaveReachedTheDailyTicketPurchaseLimit"
    override val template = Template("components/label/you_have_reached_the_daily_ticket_purchase_limit")
}

object LabelRaceFinished : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelRaceFinished"
    override val template = Template("components/label/race_finished")
}

object LabelTeamTrialsExtraRewardsOpponent : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelTeamTrialsExtraRewardsOpponent"
    override val template = Template("components/label/team_trials_extra_reward_opponent", region = Region.rightHalf)
}

object LabelClubIneligibleDonation : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelClubIneligibleDonation"
    override val template = Template("components/label/club_ineligible_donation", region = Region.middle)
}

object LabelCurrentItemRequestStatus : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelCurrentItemRequestStatus"
    override val template = Template("components/label/current_item_request_status", region = Region.topHalf)
}

object LabelNone : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]LabelNone"
    override val template = Template("components/label/none", region = Region.middle)
}
