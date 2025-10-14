package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.utils.Screen

object BannerRunnerSelect : ComponentInterface {
    override val TAG: String = "BannerRunnerSelect"
    override val templates: List<Template> = listOf<Template>(
        Template("banner_runner_select", Screen.TOP),
    )
}