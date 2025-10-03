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