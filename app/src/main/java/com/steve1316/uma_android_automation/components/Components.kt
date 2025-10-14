package com.steve1316.uma_android_automation.components

import com.steve1316.uma_android_automation.components.ComponentUtils
import com.steve1316.uma_android_automation.utils.ImageUtils
import com.steve1316.uma_android_automation.utils.MessageLog
import com.steve1316.uma_android_automation.utils.Screen

import android.graphics.Bitmap
import org.opencv.core.Point

data class Template(val name: String, val region: IntArray = Screen.FULL)

enum class TemplateComparisonMode {
    AND,
    OR,
}

interface BaseComponentInterface {
    // Logging tag.
    val TAG: String
    // List of templates which must all match (logical AND)
    val templates: List<Template>

    fun check(imageUtils: ImageUtils, tries: Int = 1): Boolean
}

interface ComponentInterface : BaseComponentInterface {
    /** Finds image on screen and returns its location if it exists. */
    fun find(imageUtils: ImageUtils, tries: Int = 1): Pair<Point?, Bitmap> {
        val template = templates.first()
        val (point, bitmap) = imageUtils.findImage(template.name, region=template.region, tries=tries)

        if (point == null) {
            return Pair(null, bitmap)
        }

        return Pair(point, bitmap)
    }

    /** Finds image on screen and returns boolean whether it exists. */
    override fun check(imageUtils: ImageUtils, tries: Int): Boolean {
        return find(imageUtils=imageUtils, tries=tries).first != null
    }

    fun confirm(imageUtils: ImageUtils, tries: Int = 1): Boolean {
        return imageUtils.confirmLocation(templates.first().name, tries=tries, region=templates.first().region)
    }

    fun click(imageUtils: ImageUtils, tries: Int = 1, taps: Int = 1): Boolean {
        val point = find(imageUtils=imageUtils, tries=tries).first
        if (point == null) {
            return false
        }

        ComponentUtils.tap(point.x, point.y, templates.first().name, taps=taps)
        return true
    }
}

interface ScreenInterface : ComponentInterface {
    // Which method to use when checking templates.
    // Can only be "and" or "or".
    // In "and" mode, all templates are required to exist for check() to return true.
    // In "or" mode, only one of the templates is required.
    val comparisonMode: TemplateComparisonMode
        get() = TemplateComparisonMode.AND

    override fun check(imageUtils: ImageUtils, tries: Int): Boolean {
        // If any of the templates are not found, this component doesnt exist on screen.
        for (template in templates) {
            val (point, _) = imageUtils.findImage(template.name, region=template.region, tries=tries)
            MessageLog.i("ScreenInterface::check()", "Checking: ${template.name} -> ${point == null}")
            if (point == null && comparisonMode == TemplateComparisonMode.AND) {
                return false
            }
        }
        return true
    }
}

// More complex buttons have multiple templates which could match them based
// on the button state. This interface handles this by finding buttons
// where one state is active at a time. Logical OR between templates, essentially.
interface MultiStateButtonInterface : ComponentInterface {
    /** Finds image on screen and returns its location if it exists. */
    override fun find(imageUtils: ImageUtils, tries: Int): Pair<Point?, Bitmap> {
        for (template in templates) {
            val (point, bitmap) = imageUtils.findImage(template.name, region=template.region, tries=tries)
            if (point != null) {
                return Pair(point, bitmap)
            }
        }
        return Pair(null, Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    }

    /** Finds image on screen and returns boolean whether it exists. */
    override fun check(imageUtils: ImageUtils, tries: Int): Boolean {
        return find(imageUtils=imageUtils, tries=tries).first != null
    }

    override fun click(imageUtils: ImageUtils, tries: Int, taps: Int): Boolean {
        val point = find(imageUtils=imageUtils, tries=tries).first
        if (point == null) {
            return false
        }

        ComponentUtils.tap(point.x, point.y, templates.first().name, taps=taps)
        return true
    }
}
