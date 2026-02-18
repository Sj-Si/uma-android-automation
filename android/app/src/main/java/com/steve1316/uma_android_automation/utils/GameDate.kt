package com.steve1316.uma_android_automation.utils

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.TextUtils

import com.steve1316.uma_android_automation.MainActivity
import com.steve1316.uma_android_automation.utils.CustomImageUtils
import com.steve1316.uma_android_automation.types.DateYear
import com.steve1316.uma_android_automation.types.DateMonth
import com.steve1316.uma_android_automation.types.DatePhase

/**
 * Represents the Game's date.
 *
 * This class stores the game's date representation and contains functions
 * used to convert and detect the current on screen date.
 *
 * @property year The date's year (junior, classic, senior).
 * @property month The date's month.
 * @property phase The date's phase (early, late).
 * @property day The current day (turn number).
 * @property bIsPreDebut Whether the game is in the Pre-Debut phase.
 * @property bIsFinaleSeason Whether the game is in the finale season.
 */
class GameDate {
    var year: DateYear = DateYear.JUNIOR
    var month: DateMonth = DateMonth.JANUARY
    var phase: DatePhase = DatePhase.EARLY
    var day: Int = 1
    var bIsPreDebut: Boolean = false
    var bIsFinaleSeason: Boolean = false

    /** Constructor that takes year/month/phase arguments to calculate the day. */
    constructor(
        year: DateYear,
        month: DateMonth,
        phase: DatePhase,
    ) {
        val day = toDay(year, month, phase)
        
        this.year = year
        this.month = month
        this.phase = phase
        this.day = day

        bIsPreDebut = this.day < 12
        bIsFinaleSeason = this.day > 72
    }

    /** Constructor that takes a day argument to calculate the year/month/phase. */
    constructor(day: Int) {
        val tmpDate = fromDay(day)
        this.year = tmpDate.year
        this.month = tmpDate.month
        this.phase = tmpDate.phase
        this.day = day

        bIsPreDebut = this.day < 12
        bIsFinaleSeason = this.day > 72
    }

    companion object {
        const val TAG: String = "[${MainActivity.loggerTag}]GameDate"

        /** Converts a year/month/phase to a day number.
         *
         * @param year The date's year.
         * @param month The date's month.
         * @param phase The date's phase.
         * @return The calculated day number.
         */
        fun toDay(year: DateYear, month: DateMonth, phase: DatePhase): Int {
            return ((year.ordinal * (DateMonth.entries.size * 2)) + (((month.ordinal + 1) * 2) + phase.ordinal)) - 1
        }

        /**
         * Converts a day (or turn number) into a GameDate object.
         *
         * @param day The day number to convert.
         *
         * @return The GameDate object generated from the day number or NULL if conversion failed.
         */
        fun fromDay(day: Int): GameDate {
            // Clamp minimum day to 1.
            if (day < 1) {
                return GameDate(
                    year = DateYear.JUNIOR,
                    month = DateMonth.JANUARY,
                    phase = DatePhase.EARLY,
                )
            }

            // No maximum day number but values over 72 will break the
            // formulae below, so we just use the alternative secondary
            // constructor and return that object.
            if (day > 72) {
                val tmpDate = GameDate(
                    year = DateYear.SENIOR,
                    month = DateMonth.DECEMBER,
                    phase = DatePhase.LATE,
                )
                tmpDate.day = day
                return tmpDate
            }

            // Day starts at 1, not 0, so we need to decrement for the following
            // formulae to work properly.
            val day = day - 1
            val y = day.floorDiv(24)
            val m = (day % 24).floorDiv(2)
            val p = day % 2
            val year: DateYear = DateYear.fromOrdinal(y)!!
            val month: DateMonth = DateMonth.fromOrdinal(m)!!
            val phase: DatePhase = DatePhase.fromOrdinal(p)!!
            return GameDate(
                year = year,
                month = month,
                phase = phase,
            )
        }

        /**
         * Detects the date on the screen.
         *
         * This is just a simple wrapper around [fromDateString].
         *
         * @param imageUtils A reference to a CustomImageUtils instance.
         * @return The detected GameDate object, or NULL if nothing could be detected.
         */
        fun detectDate(imageUtils: CustomImageUtils): GameDate? {
            return fromDateString(imageUtils = imageUtils)
        }

        /**
         * Converts a date string to a GameDate object.
         *
         * @param s The date string to convert from.
         *          If NULL, then imageUtils will be used to detect this value.
         * @param turnsLeft The number turns left until the next goal.
         *          If NULL, then imageUtils will be used to detect this value.
         * @param imageUtils The CustomImageUtils instance used to detect
         * the date string and turnsLeft if either are NULL.
         *
         * @return The GameDate object generated from the date string and turns left,
         * or NULL if conversion failed.
         */
        fun fromDateString(s: String? = null, turnsLeft: Int? = null, imageUtils: CustomImageUtils): GameDate? {
            val s: String? = s ?: imageUtils.determineDayString()
            if (s == null || s == "") {
                MessageLog.d(TAG, "fromDateString:: Passed string is NULL or empty.")
                return null
            }

            if (s.lowercase().contains("debut")) {
                val turnsLeft: Int? = turnsLeft ?: imageUtils.determineTurnsRemainingBeforeNextGoal()
                if (turnsLeft == null) {
                    MessageLog.e(TAG, "fromDateString:: In pre-debut but received turnsLeft=NULL. Cannot calculate date without turnsLeft.")
                    return null
                }

                if (turnsLeft <= 0) {
                    MessageLog.d(TAG, "fromDateString:: Debut race day.")
                    return GameDate(DateYear.JUNIOR, DateMonth.JUNE, DatePhase.LATE)
                }

                // Calculate the day number in pre-debut.
                val turnsInPreDebut = 12
                val currTurn = (turnsInPreDebut - turnsLeft).coerceIn(0, 12)
                return fromDay(currTurn)
            }

            if (s.lowercase().contains("finale")) {
                MessageLog.d(TAG, "fromDateString:: Finale season.")
                // Pass the cached day string to avoid redundant OCR operations.
                val (finalsDay, _) = getFinalsDay(imageUtils = imageUtils, cachedDayString = s)
                if (finalsDay == null) {
                    MessageLog.w(TAG, "fromDateString:: Could not determine day in finale season. Defaulting to first day of finale.")
                    return GameDate(day = 73)
                }
                return GameDate(day = finalsDay)
            }

            val years: List<String> = DateYear.entries.map { it.name }
            val months: List<String> = DateMonth.entries.map { it.shortName }
            val phases: List<String> = DatePhase.entries.map { it.name }

            // Split the input string by whitespace.
            val parts = s.trim().split(" ")
            if (parts.size < 3) {
                MessageLog.w(TAG, "fromDateString:: Invalid date string format: $s")
                return null
            }
    
            // Extract the parts with safe indexing using default values.
            val yearPart: String = parts.getOrNull(0) ?: DateYear.SENIOR.name
            val phasePart: String = parts.getOrNull(2) ?: DatePhase.EARLY.name
            val monthPart: String = parts.getOrNull(3) ?: DateMonth.JANUARY.shortName

            // Find the best match for the strings using Jaro Winkler if not found in mapping.
            val yearString: String? = TextUtils.matchStringInList(yearPart, years)
            if (yearString == null) {
                MessageLog.w(TAG, "fromDateString:: Invalid date format. Could not detect YEAR from $yearPart.")
                return null
            }

            val monthString: String? = TextUtils.matchStringInList(monthPart, months)
            if (monthString == null) {
                MessageLog.w(TAG, "fromDateString:: Invalid date format. Could not detect MONTH from $monthPart.")
                return null
            }

            val phaseString: String? = TextUtils.matchStringInList(phasePart, phases)
            if (phaseString == null) {
                MessageLog.w(TAG, "fromDateString:: Invalid date format. Could not detect PHASE from $phasePart.")
                return null
            }

            // Now try to convert the strings to their respective enums.
            val year: DateYear? = DateYear.fromName(yearString)
            if (year == null) {
                MessageLog.w(TAG, "fromDateString:: Invalid yearString: $yearString")
                return null
            }
            val month: DateMonth? = DateMonth.fromShortName(monthString)
            if (month == null) {
                MessageLog.w(TAG, "fromDateString:: Invalid monthString: $monthString")
                return null
            }
            val phase: DatePhase? = DatePhase.fromName(phaseString)
            if (phase == null) {
                MessageLog.w(TAG, "fromDateString:: Invalid phaseString: $phaseString")
                return null
            }

            val result = GameDate(year, month, phase)
            MessageLog.d(TAG, "fromDateString:: Detected $result")
            return result
        }

        /**
         * Determines whether a day number falls within the Summer dates.
         *
         * @param dayToCheck The day number to check.
         *
         * @return Boolean whether the day number falls within summer.
         */
        fun isSummer(dayToCheck: Int): Boolean {
            // Summer only takes place in classic and senior years.
            for (year in listOf(DateYear.CLASSIC, DateYear.SENIOR)) {
                if (
                    dayToCheck >= GameDate(year, DateMonth.JULY, DatePhase.EARLY).day &&
                    dayToCheck <= GameDate(year, DateMonth.AUGUST, DatePhase.LATE).day
                ) {
                    return true
                }
            }

            return false
        }

        /** Determines the day number in the finale season.
         *
         * @param imageUtils A reference to a CustomImageUtils instance.
         * @param cachedDayString Optional cached day string to avoid redundant OCR.
         * @param isOnMainScreen If true, skip the check in [determineDayString].
         * @return A Pair containing the day number and day string.
         */
        fun getFinalsDay(imageUtils: CustomImageUtils, cachedDayString: String? = null, isOnMainScreen: Boolean = false): Pair<Int?, String?> {
            // Get the day string first to check if we're in finals before doing expensive OCR operations.
            val dayString: String? = cachedDayString ?: imageUtils.determineDayString(isOnMainScreen)
            if (dayString == null) {
                MessageLog.w(TAG, "[DATE] Could not detect day string.")
                return Pair(null, null)
            }

            // Early exit if not in finale season.
            if (!dayString.lowercase().contains("finale")) {
                MessageLog.w(TAG, "[DATE] getFinalsDay:: Not in finale season. Day string: $dayString")
                return Pair(null, dayString)
            }

            val goalText = imageUtils.getGoalText().lowercase()

            fun goalTextMatch(target: String, query: String, threshold: Double = 0.9): Boolean {
                return TextUtils.findMostSimilarSubstring(target, query, threshold) != null
            }

            val finalsDay = when {
                goalTextMatch(goalText, "qualifier") -> {
                    MessageLog.d(TAG, "[DATE] Detected Finals Qualifier (Turn 73).")
                    73
                }
                goalTextMatch(goalText, "semifinal") -> {
                    MessageLog.d(TAG, "[DATE] Detected Finals Semifinal (Turn 74).")
                    74
                }
                goalTextMatch(goalText, "finals") -> {
                    MessageLog.d(TAG, "[DATE] Detected Finals Finals (Turn 75).")
                    75
                }
                else -> {
                    MessageLog.w(TAG, "Could not determine Finals date. Defaulting to turn 73.")
                    73
                }
            }
            return Pair(finalsDay, dayString)
        }
    }

    /** Converts the current date to a day number.
     * This is a wrapper around the GameDate.toDay function that just passes
     * our current year/month/phase as the arguments.
     *
     * @return The current day number.
     */
    fun toDay(): Int {
        return toDay(this.year, this.month, this.phase)
    }

    /**
     * Checks whether the current day is in the summer date range.
     *
     * This is a wrapper around the GameDate.isSummer function that just passes
     * our current date as the dayToCheck argument.
     *
     * @return Boolean whether the current date is in the summer date range.
     */
    fun isSummer(): Boolean {
        return isSummer(this.day)
    }

    /** Returns a string representation of the current date. */
    override fun toString(): String {
        if (bIsFinaleSeason) {
            return when (this.day) {
                73 -> "Finale Qualifier (Turn ${this.day})"
                74 -> "Finale Semi-Final (Turn ${this.day})"
                75 -> "Finale Finals (Turn ${this.day})"
                else -> "INVALID DAY (> 75): ${this.day}"
            }
        }
        return "${this.year.longName} ${this.phase} ${this.month} (Turn ${this.day})"
    }

    /** Updates class members to reflect the current day.
     *
     * @param day The day number to update to.
     */
    fun updateDay(day: Int) {
        val tmpDate: GameDate = fromDay(day)
        this.year = tmpDate.year
        this.month = tmpDate.month
        this.phase = tmpDate.phase
        this.day = day

        bIsPreDebut = this.day < 12
        bIsFinaleSeason = this.day > 72
    }

    /**
     * Returns the GameDate object for the next calendar day.
     *
     * @return The next day's GameDate object.
     */
    fun getNextDate(): GameDate? {
        return fromDay(this.day + 1)
    }

    /**
     * Updates the current date by detecting it from the screen.
     *
     * @param imageUtils A reference to a CustomImageUtils instance.
     * @param isOnMainScreen If true, skip the check in [determineDayString].
     * @return True if the date was updated successfully, false otherwise.
     */
    fun update(imageUtils: CustomImageUtils, isOnMainScreen: Boolean = false): Boolean {
        val (finalsDay, cachedDayString) = getFinalsDay(imageUtils, isOnMainScreen = isOnMainScreen)
        if (finalsDay != null) {
            updateDay(finalsDay)
        } else {
            // Pass the cached day string to avoid doing expensive OCR operations again.
            val tmpDate: GameDate? = fromDateString(s = cachedDayString, imageUtils = imageUtils)
            if (tmpDate == null) {
                MessageLog.e(TAG, "GameDate.fromDateString returned NULL.")
                return false
            }
            updateDay(tmpDate.day)
        }
        return true
    }
}
