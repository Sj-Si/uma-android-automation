package com.steve1316.uma_android_automation.utils.types

import com.steve1316.uma_android_automation.data.RaceData
import com.steve1316.uma_android_automation.utils.MessageLog

import java.time.Month
import java.util.Locale

// ENUMS

enum class Aptitude {
    G,
    F,
    E,
    D,
    C,
    B,
    A,
    S;

    companion object {
        private val nameMap = entries.associateBy { it.name }
        private val ordinalMap = entries.associateBy { it.ordinal }

        fun fromName(value: String): Aptitude? = nameMap[value.uppercase()]
        fun fromOrdinal(ordinal: Int): Aptitude? = ordinalMap[ordinal]
    }
}

enum class RunningStyle {
    FRONT_RUNNER,
    PACE_CHASER,
    LATE_SURGER,
    END_CLOSER;

    companion object {
        private val nameMap = entries.associateBy { it.name }
        private val ordinalMap = entries.associateBy { it.ordinal }

        fun fromName(value: String): RunningStyle? = nameMap[value.uppercase()]
        fun fromOrdinal(ordinal: Int): RunningStyle? = ordinalMap[ordinal]
    }
}

enum class TrackSurface {
    TURF,
    DIRT;

    companion object {
        private val nameMap = entries.associateBy { it.name }
        private val ordinalMap = entries.associateBy { it.ordinal }

        fun fromName(value: String): TrackSurface? = nameMap[value.uppercase()]
        fun fromOrdinal(ordinal: Int): TrackSurface? = ordinalMap[ordinal]
    }
}

enum class TrackDistance {
    SPRINT,
    MILE,
    MEDIUM,
    LONG;

    companion object {
        private val nameMap = entries.associateBy { it.name }
        private val ordinalMap = entries.associateBy { it.ordinal }

        fun fromName(value: String): TrackDistance? = nameMap[value.uppercase()]
        fun fromOrdinal(ordinal: Int): TrackDistance? = ordinalMap[ordinal]
    }
}

enum class Mood {
    AWFUL,
    BAD,
    NORMAL,
    GOOD,
    GREAT;

    companion object {
        private val nameMap = entries.associateBy { it.name }
        private val ordinalMap = entries.associateBy { it.ordinal }

        fun fromName(value: String): Mood? = nameMap[value.uppercase()]
        fun fromOrdinal(ordinal: Int): Mood? = ordinalMap[ordinal]
    }
}

enum class RaceGrade {
    DEBUT,
    MAIDEN,
    PRE_OP,
    OP,
    G3,
    G2,
    G1,
    EX;

    companion object {
        private val nameMap = entries.associateBy { it.name }
        private val ordinalMap = entries.associateBy { it.ordinal }

        fun fromName(value: String): RaceGrade? = nameMap[value.uppercase()]
        fun fromOrdinal(ordinal: Int): RaceGrade? = ordinalMap[ordinal]
    }
}

enum class DatePhase {
    EARLY,
    LATE;

    companion object {
        private val nameMap = entries.associateBy { it.name }
        private val ordinalMap = entries.associateBy { it.ordinal }

        fun fromName(value: String): DatePhase? = nameMap[value.uppercase()]
        fun fromOrdinal(ordinal: Int): DatePhase? = ordinalMap[ordinal]
    }
}

enum class DateYear {
    JUNIOR,
    CLASSIC,
    SENIOR;

    companion object {
        private val nameMap = entries.associateBy { it.name }
        private val ordinalMap = entries.associateBy { it.ordinal }

        fun fromName(value: String): DateYear? = nameMap[value.uppercase()]
        fun fromOrdinal(ordinal: Int): DateYear? = ordinalMap[ordinal]
    }
}

// DATA CLASSES

fun getMonthNumber(month: String): Int {
    return try {
        MessageLog.i("getMonthNumber", "$month => ${Month.valueOf(month.uppercase(Locale.getDefault())).value}")
        Month.valueOf(month.uppercase(Locale.getDefault())).value
    } catch (e: IllegalArgumentException) {
        -1
    }
}

data class GameDate(
    val year: Int,
    val month: Int,
    val phase: Int,
) {
    // Calendar starting at Jan 1 of Junior year.
    // Each year has 24 turns since each month is two phases.
    val day: Int = (year * (12 * 2)) + (month * (phase + 1))

    constructor(year: String = "junior", month: String = "january", phase: String = "early") : this(
        DateYear.fromName(year)?.ordinal ?: 0,
        getMonthNumber(month),
        DatePhase.fromName(phase)?.ordinal ?: 0,
    )

    fun getRaces(): MutableMap<String, RaceInfo> {
        return RaceData.races[day]!!
    }

    fun asString(): String {
        return "${year}-${month}-${phase} (${day})"
    }
}

data class RaceInfo(
    val name: String,
    val grade: RaceGrade,
    val year: String,
    val month: String,
    val phase: String,
    val trackName: String,
    val trackSurface: TrackSurface,
    val trackDistance: TrackDistance,
    val lengthMeters: Int,
) {
    val date: GameDate = GameDate(year, month, phase)
    constructor(
        name: String,
        grade: String,
        year: String,
        month: String,
        phase: String,
        trackName: String,
        trackSurface: String,
        trackDistance: String,
        lengthMeters: String,
    ) : this(
        name,
        RaceGrade.fromName(grade) ?: RaceGrade.MAIDEN,
        year,
        month,
        phase,
        trackName,
        TrackSurface.fromName(trackSurface) ?: TrackSurface.TURF,
        TrackDistance.fromName(trackDistance) ?: TrackDistance.MEDIUM,
        lengthMeters.toIntOrNull() ?: -1,
    )
}