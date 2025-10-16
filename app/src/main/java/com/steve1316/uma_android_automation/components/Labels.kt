package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.utils.Screen

object LabelConnecting : ComponentInterface {
    override val TAG: String = "LabelConnecting"
    override val templates: List<Template> = listOf<Template>(
        Template("connecting", Screen.TOP),
    )
}

object LabelConnectionError : ComponentInterface {
    override val TAG: String = "LabelConnectionError"
    override val templates: List<Template> = listOf<Template>(
        Template("connection_error", Screen.MIDDLE),
    )
}

object LabelNowLoading : ComponentInterface {
    override val TAG: String = "LabelNowLoading"
    override val templates: List<Template> = listOf<Template>(
        Template("now_loading", Screen.BOTTOM_HALF),
    )
}

object LabelStatDistance : ComponentInterface {
    override val TAG: String = "LabelStatDistance"
    override val templates: List<Template> = listOf<Template>(
        Template("stat_distance", Screen.MIDDLE),
    )
}

object LabelStatTrackSurface : ComponentInterface {
    override val TAG: String = "LabelStatTrackSurface"
    override val templates: List<Template> = listOf<Template>(
        Template("stat_track_surface", Screen.MIDDLE),
    )
}

object LabelStatStyle : ComponentInterface {
    override val TAG: String = "LabelStatStyle"
    override val templates: List<Template> = listOf<Template>(
        Template("stat_style", Screen.MIDDLE),
    )
}

object LabelTeamTrialsSelectOpponent : ComponentInterface {
    override val TAG: String = "LabelTeamTrialsSelectOpponent"
    override val templates: List<Template> = listOf<Template>(
        Template("team_trials_select_opponent", Screen.TOP_HALF),
    )
}

object LabelTeamTrialsRaceResultWin : ComponentInterface {
    override val TAG: String = "LabelTeamTrialsRaceResultWin"
    override val templates: List<Template> = listOf<Template>(
        Template("team_trials_win", Screen.MIDDLE),
    )
}

object LabelTeamTrialsRaceResultLose : ComponentInterface {
    override val TAG: String = "LabelTeamTrialsRaceResultLose"
    override val templates: List<Template> = listOf<Template>(
        Template("team_trials_lose", Screen.MIDDLE),
    )
}

object LabelTeamTrialsRaceResultDraw : ComponentInterface {
    override val TAG: String = "LabelTeamTrialsRaceResultDraw"
    override val templates: List<Template> = listOf<Template>(
        Template("team_trials_draw", Screen.MIDDLE),
    )
}