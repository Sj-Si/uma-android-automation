/** Defines banner components.
 *
 * These are images that sit near the top of the screen and say the page
 * name (i.e. Trainee Select). they also have an info button on their
 * right edge that opens the help overlay for that page.
 * 
 * Banners are typically in the same place at the top left of the screen.
 * 
 * It is possible that some banners may be too high up on the screen and could
 * become blocked by a mission completion toast.
 */

package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.components.ComponentInterface
import com.steve1316.uma_android_automation.components.Template
import com.steve1316.uma_android_automation.components.Region

object BannerLegacySelect : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]BannerLegacySelect"
    override val template = Template("components/banner/legacy_select", region = Region.topHalf)
}

object BannerScenarioSelect : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]BannerScenarioSelect"
    override val template = Template("components/banner/scenario_select", region = Region.topHalf)
}

object BannerSupportFormation : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]BannerSupportFormation"
    override val template = Template("components/banner/support_formation", region = Region.topHalf)
}

object BannerTraineeSelect : ComponentInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]BannerTraineeSelect"
    override val template = Template("components/banner/trainee_select", region = Region.topHalf)
}
