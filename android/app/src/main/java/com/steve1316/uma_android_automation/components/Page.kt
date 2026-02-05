package com.steve1316.uma_android_automation.components

import android.graphics.Bitmap

import com.steve1316.automation_library.utils.MyAccessibilityService

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.utils.CustomImageUtils
import com.steve1316.uma_android_automation.components.*

enum class ComponentComparisonMode {
    AND,
    OR,
    ONLY_ONE;
}

interface PageInterface {
    val TAG: String

    // The comparison mode to use against the identifying components.
    val comparisonMode: ComponentComparisonMode
    // List of unique components that are used to verify the page.
    val identifyingComponents: List<ComponentInterface>
    // Buttons that allow the user to navigate to other pages.
    val prevButton: ComponentInterface?
    val nextButton: ComponentInterface?

    fun check(imageUtils: CustomImageUtils, bitmap: Bitmap? = null): Boolean {
        val bitmap: Bitmap = bitmap ?: imageUtils.getSourceBitmap()
        return when (comparisonMode) {
            ComponentComparisonMode.AND -> {
                identifyingComponents.all {
                    it.check(imageUtils, sourceBitmap = bitmap)
                }
            }
            ComponentComparisonMode.OR -> {
                identifyingComponents.any {
                    it.check(imageUtils, sourceBitmap = bitmap)
                }
            }
            ComponentComparisonMode.ONLY_ONE -> {
                identifyingComponents.count {
                    it.check(imageUtils, sourceBitmap = bitmap)
                } == 1
            }
        }
    }

    fun prev(imageUtils: CustomImageUtils, bitmap: Bitmap? = null): Boolean {
        val prevButton: ComponentInterface = prevButton ?: return false

        val bitmap: Bitmap = bitmap ?: imageUtils.getSourceBitmap()
        return prevButton.click(imageUtils, sourceBitmap = bitmap)
    }

    fun next(imageUtils: CustomImageUtils, bitmap: Bitmap? = null): Boolean {
        val nextButton: ComponentInterface = nextButton ?: return false

        val bitmap: Bitmap = bitmap ?: imageUtils.getSourceBitmap()
        return nextButton.click(imageUtils, sourceBitmap = bitmap)
    }
}

object PageTeamTrialsHome : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageTeamTrialsHome"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonTeamRace,
    )

    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = ButtonTeamRace
}

object PageTeamTrialsSelectOpponent : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageTeamTrialsSelectOpponent"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        LabelTeamTrialsSelectOpponent,
    )

    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = null
}

object PageTeamTrialsPreRace : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageTeamTrialsPreRace"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        IconTeamTrialsHorseshoe,
        ButtonNext,
    )

    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = ButtonNext
}

object PageTeamTrialsRaceQuickModeOff : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageTeamTrialsRaceQuickModeOff"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonTeamTrialsQuickModeOff,
    )

    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = ButtonSeeResults
}

object PageTeamTrialsRaceQuickModeOn : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageTeamTrialsRaceQuickModeOn"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonTeamTrialsQuickModeOn,
    )

    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = ButtonTeamTrialsSeeAllRaceResults
}

object PageTeamTrialsRaceFinished : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageTeamTrialsRaceFinished"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        LabelRaceFinished,
    )

    override val prevButton: ComponentInterface? = null
    override val nextButton: ComponentInterface? = ButtonNext
}

object PageTeamTrialsPreRaceResults : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageTeamTrialsPreRaceResults"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf()

    override val prevButton: ComponentInterface? = null
    override val nextButton: ComponentInterface? = ButtonNextWithImage

    override fun check(imageUtils: CustomImageUtils, bitmap: Bitmap?): Boolean {
        val bitmap: Bitmap = bitmap ?: imageUtils.getSourceBitmap()
        return !ButtonRaceAgain.check(imageUtils, sourceBitmap = bitmap) &&
            ButtonNextWithImage.check(imageUtils, sourceBitmap = bitmap)
    }
}

object PageTeamTrialsRaceResults : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageTeamTrialsRaceResults"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonRaceAgain,
        ButtonNextWithImage,
    )

    override val prevButton: ComponentInterface? = null
    override val nextButton: ComponentInterface? = ButtonNextWithImage
}


object PageDailySale : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageDailySale"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonShopEndSale,
    )

    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = null
}

//=====================================================================

object PageDailyRacesRaceSelection : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageDailyRacesRaceSelection"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonDailyRacesMoonlightSho,
    )

    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = null
}

object PageDailyRacesDifficultySelection : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageDailyRacesDifficultySelection"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.OR
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonDailyRacesMoonlightShoRaceSelection,
        ButtonDailyRacesJupiterCupRaceSelection
    )

    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = null
}

object PageExtraRacesRunnerSelection : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageExtraRacesRunnerSelection"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonBack,
        ButtonConfirm,
    )

    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = ButtonConfirm
}

// Appears before race prep screen.
object PageDailyRacesPreRacePrep : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageDailyRacesPreRacePrep"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonBack,
        ButtonNext,
    )

    // Brings to main menu.
    override val prevButton: ComponentInterface? = ButtonBack
    // Opens Items Selected dialog.
    override val nextButton: ComponentInterface? = ButtonNext
}

object PageDailyRacesRacePrep : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageDailyRacesRacePrep"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonBack,
        ButtonChangeRunningStyle
    )

    // Brings to main menu.
    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = null
}

object PageDailyRacesResultsPlacing : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageDailyRacesResultsPlacing"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonDailyRacesReplay,
        ButtonNext,
    )

    override val prevButton: ComponentInterface? = null
    override val nextButton: ComponentInterface? = ButtonNext
}

object PageDailyRacesResultsRewards : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageDailyRacesResultsRewards"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonRaceAgain,
        ButtonNextWithImage,
    )

    override val prevButton: ComponentInterface? = null
    override val nextButton: ComponentInterface? = ButtonNextWithImage
}

object PageClubHome : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageClubHome"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonClubEmoji,
    )

    override val prevButton: ComponentInterface? = null
    override val nextButton: ComponentInterface? = null
}

object PageSpecialMissions : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageClubHome"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonSpecialMissionsTabDaily,
        ButtonSpecialMissionsTabMain,
        ButtonSpecialMissionsTabTitles,
        ButtonSpecialMissionsTabSpecial,
    )

    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = null

    override fun check(imageUtils: CustomImageUtils, bitmap: Bitmap?): Boolean {
        val bitmap: Bitmap = bitmap ?: imageUtils.getSourceBitmap()
        // One item will always be selected and can't be found.
        // Thus the page exists if three of the four tabs exist.
        return identifyingComponents.count { it.check(imageUtils, sourceBitmap = bitmap) } == 3
    }
}

object PageEventMissions : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageClubHome"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonSpecialMissionsTabDaily,
        ButtonSpecialMissionsTabTitles,
        ButtonEventMissionsTabLimitedTime,
    )

    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = null

    override fun check(imageUtils: CustomImageUtils, bitmap: Bitmap?): Boolean {
        val bitmap: Bitmap = bitmap ?: imageUtils.getSourceBitmap()
        // One item will always be selected and can't be found.
        // Thus the page exists if two of the three tabs exist.
        return identifyingComponents.count { it.check(imageUtils, sourceBitmap = bitmap) } == 2
    }
}

object PageLegendRaceHome : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageLegendRaceHome"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonLegendRaceSpecialMissions,
    )

    override val prevButton: ComponentInterface? = ButtonBack
    override val nextButton: ComponentInterface? = null
}

object PageHome : PageInterface {
    override val TAG: String = "[${MainActivity.loggerTag}]PageHome"
    override val comparisonMode: ComponentComparisonMode = ComponentComparisonMode.AND
    override val identifyingComponents: List<ComponentInterface> = listOf(
        ButtonMenuBarHomeSelected,
    )

    override val prevButton: ComponentInterface? = null
    override val nextButton: ComponentInterface? = null
}
