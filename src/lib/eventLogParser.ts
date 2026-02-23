export type LogFileInput = {
    name: string
    content: string
}

export type DayActions = {
    energy: boolean
    mood: boolean
    injury: boolean
    training: boolean
    race: boolean
}

export type DayRecord = {
    kind: "day"
    dayNumber: number
    dateText?: string
    summary: string
    actions: DayActions
    fileName: string
    triggers?: DayTriggers
    year?: string
    trainingType?: string
    trainingStatGains?: number[]
    timestamp?: number // Timestamp in milliseconds from file start (HH*3600000 + MM*60000 + SS*1000 + mmm)
}

export type GapRecord = {
    kind: "gap"
    from: number
    to: number
}

export type FileDividerRecord = {
    kind: "fileDivider"
    fileName: string
}

export type ParseError = {
    message: string
    fileName: string
}

export type ParseResult = {
    records: Array<DayRecord | GapRecord | FileDividerRecord>
    errors: ParseError[]
    meta: {
        firstDay?: number
        lastDay?: number
        filesProcessed: number
    }
}

export type DayTriggers = {
    energy: string[]
    mood: string[]
    injury: string[]
    training: string[]
    race: string[]
}

const ACTION_KEYS = ["training", "race", "energy", "mood", "injury"] as const
type ActionKey = (typeof ACTION_KEYS)[number]

const REGEX = {
    // Example: "[DATE] It is currently Junior Year Late Dec / Turn Number 24.".
    // New Example: "[DATE] New date: JUNIOR YEAR LATE JANUARY (Turn 2)"
    dateTurn: /(?:Turn Number|Turn)\s+\(?(\d+)\)?/i,
    // Example: "[INFO] Detected date: Junior Year Late Dec".
    dateDetectedText: /\[INFO\][^\n]*Detected date:\s*(.+)$/i,
    // Example: "[DATE] It is currently Senior Year Late Dec / Turn Number 24."
    // Example: "[DATE] It is currently Finale Qualifier / Turn Number 73."
    // New Example: "[DATE] New date: JUNIOR YEAR LATE JANUARY (Turn 2)"
    // This captures the date text from the formatted date line (used for Finals and regular dates).
    dateFormattedText: /\[DATE\][^\n]*(?:It is currently|New date:)\s+(.+?)\s+(?:\/ Turn Number|\(Turn)/i,
    // Extract year from date text: "Junior Year", "Classic Year", or "Senior Year".
    yearExtract: /(Junior|Classic|Senior)\s+Year/i,
    // Extract training type: "[TRAINING] Executing the Power Training."
    trainingExecution: /\[TRAINING\]\s+Executing\s+the\s+(\w+)\s+Training/i,
    // Extract stat gains: "[INFO] Speed Training stat gains: [14, 0, 6, 0, 0]"
    // New Example: "SPEED Training: stats={SPEED=10, STAMINA=0, POWER=6, GUTS=0, WIT=0}"
    trainingStatGains: /(?:\[INFO\]\s+)?(\w+)\s+Training(?:.*?stat\s+gains:\s+\[([\d,\s]+)\]|:\s+stats=\{SPEED=(-?\d+),\s*STAMINA=(-?\d+),\s*POWER=(-?\d+),\s*GUTS=(-?\d+),\s*WIT=(-?\d+)\})/i,
    // Extract timestamp: "00:12:22.190" or "00:00:14.810"
    timestamp: /^(\d{2}):(\d{2}):(\d{2})\.(\d{3})/,
}

/** Generic matcher that supports substring contains checks only. */
type LineMatcher = {
    substr?: string[]
    negativeSubstr?: string[]
}

function matchesLine(line: string, matcher: LineMatcher): boolean {
    const l = line
    if (matcher.negativeSubstr && matcher.negativeSubstr.some((s) => l.toLowerCase().includes(s.toLowerCase()))) return false
    const substrOk = matcher.substr ? matcher.substr.some((s) => l.toLowerCase().includes(s.toLowerCase())) : false
    return substrOk
}

const MATCHERS: Record<ActionKey, LineMatcher> = {
    training: {
        substr: ["Process to execute training completed", " stat gains: [", "[TRAINING] Executing the ", " with a focus on building relationship bars"],
    },
    race: {
        substr: ["Racing process for Mandatory Race is completed", "Racing process for Extra Race is completed"],
    },
    energy: {
        substr: ["Successfully recovered energy"],
    },
    mood: {
        substr: ["Recovering mood now"],
    },
    injury: {
        substr: ["Injury detected and attempted to heal"],
    },
}

function sanitizeSummary(text?: string): string {
    if (!text) return ""
    const trimmed = text.trim()
    // Keep concise.
    return trimmed.length > 140 ? trimmed.slice(0, 137) + "..." : trimmed
}

function composeSummary(actions: DayActions, firstNotable?: string): string {
    const labels: string[] = []
    if (actions.training) labels.push("Training")
    if (actions.race) labels.push("Race")
    if (actions.energy) labels.push("Recover Energy")
    if (actions.mood) labels.push("Recover Mood")
    if (actions.injury) labels.push("Recover Injury")
    if (labels.length > 0) {
        return labels.join(" + ")
    }
    return sanitizeSummary(firstNotable) || "No notable actions detected."
}

/**
 * Determines the year for a given day number and date text.
 * Finals (turns 73-75) are assigned to "Senior" year.
 */
function determineYear(dayNumber: number, dateText?: string): string | undefined {
    // Finals occur at turns 73, 74, and 75, and belong to Senior Year.
    if (dayNumber >= 73 && dayNumber <= 75) {
        return "Senior"
    }
    // Check if date text contains "Finale" (case-insensitive).
    if (dateText && /finale/i.test(dateText)) {
        return "Senior"
    }
    // Fall back to regex extraction from date text.
    if (dateText) {
        const yearMatch = dateText.match(REGEX.yearExtract)
        if (yearMatch) {
            return yearMatch[1]
        }
    }
    return undefined
}

export function parseLogs(files: LogFileInput[]): ParseResult {
    const sorted = [...files].sort((a, b) => a.name.localeCompare(b.name))
    const errors: ParseError[] = []
    const dayMap = new Map<
        number,
        {
            dayNumber: number
            dateText?: string
            actions: DayActions
            firstNotable?: string
            fileName: string
            triggers: DayTriggers
            year?: string
            trainingType?: string
            trainingStatGains?: number[]
            ended?: boolean // True if day ended with [END] or "Now saving Message Log"
            timestamp?: number // Timestamp in milliseconds from file start
        }
    >()

    let lastDaySeen: number | undefined
    let firstDaySeen: number | undefined

    for (const file of sorted) {
        const lines = file.content.split(/\r?\n/)
        let currentDay: number | undefined
        let foundAnyDay = false
        let pendingDateText: string | undefined

        // First pass: detect if this file starts at a day earlier than lastDaySeen.
        let firstDayInThisFile: number | undefined
        for (const line of lines) {
            const m = line.match(REGEX.dateTurn)
            if (m) {
                firstDayInThisFile = parseInt(m[1], 10)
                break
            }
        }
        if (typeof firstDayInThisFile === "number") {
            if (typeof lastDaySeen === "number" && firstDayInThisFile < lastDaySeen) {
                errors.push({
                    message: `File ${file.name} starts at Day ${firstDayInThisFile} which is earlier than last seen Day ${lastDaySeen}. Skipping file.`,
                    fileName: file.name,
                })
                continue
            }
        }

        // Store stat gains by training type before execution is detected.
        const statGainsByType = new Map<string, number[]>()

        for (const raw of lines) {
            const line = raw

            // Extract timestamp from line for elapsed time calculation.
            const timestampMatch = line.match(REGEX.timestamp)
            const lineTimestamp = timestampMatch
                ? parseInt(timestampMatch[1], 10) * 3600000 + parseInt(timestampMatch[2], 10) * 60000 + parseInt(timestampMatch[3], 10) * 1000 + parseInt(timestampMatch[4], 10)
                : undefined

            const dateDetected = line.match(REGEX.dateDetectedText)
            if (dateDetected) {
                pendingDateText = dateDetected[1].trim()
            }

            // Also extract date text from formatted date line (used for Finals and as fallback).
            // Example: "[DATE] It is currently Senior Year Late Dec / Turn Number 73."
            const dateFormatted = line.match(REGEX.dateFormattedText)
            if (dateFormatted) {
                pendingDateText = dateFormatted[1].trim()
            }

            const m = line.match(REGEX.dateTurn)
            if (m) {
                const detectedDay = parseInt(m[1], 10)
                foundAnyDay = true
                if (firstDaySeen === undefined) firstDaySeen = detectedDay
                if (lastDaySeen === undefined || detectedDay > lastDaySeen) lastDaySeen = detectedDay

                let existing = dayMap.get(detectedDay)

                // If day exists but ended in a previous file (with [END] or "Now saving Message Log"),
                // replace it with the new occurrence from this file (bot restarted).
                if (existing && existing.ended && existing.fileName !== file.name) {
                    // Remove the old ended day and create a new one for this file.
                    dayMap.delete(detectedDay)
                    existing = undefined
                }

                // Only set currentDay if this day doesn't exist, belongs to this file, or was just replaced.
                if (!existing || existing.fileName === file.name) {
                    currentDay = detectedDay

                    if (!dayMap.has(currentDay)) {
                        const year = determineYear(currentDay, pendingDateText)
                        dayMap.set(currentDay, {
                            dayNumber: currentDay,
                            dateText: pendingDateText,
                            actions: { energy: false, mood: false, injury: false, training: false, race: false },
                            firstNotable: undefined,
                            fileName: file.name,
                            triggers: { energy: [], mood: [], injury: [], training: [], race: [] },
                            year,
                            trainingType: undefined,
                            trainingStatGains: undefined,
                            ended: false,
                            timestamp: lineTimestamp,
                        })
                    } else {
                        const existingDay = dayMap.get(currentDay)!
                        if (!existingDay.dateText && pendingDateText) {
                            existingDay.dateText = pendingDateText
                            // Re-determine year in case dateText provides new information.
                            existingDay.year = determineYear(currentDay, pendingDateText)
                        } else if (!existingDay.year) {
                            // If year is still missing, try to determine it from available info.
                            existingDay.year = determineYear(currentDay, existingDay.dateText || pendingDateText)
                        }
                        // Update timestamp if this one is earlier (first detection in file).
                        if (lineTimestamp && (!existingDay.timestamp || lineTimestamp < existingDay.timestamp)) {
                            existingDay.timestamp = lineTimestamp
                        }
                    }
                    // Clear stat gains map when a new day is detected.
                    statGainsByType.clear()
                } else {
                    // This day already exists and belongs to a different file - don't process it in this file.
                    currentDay = undefined
                }
                continue
            }

            // Mark day as ended if we encounter [END] or "Now saving Message Log".
            if (currentDay !== undefined && (line.includes("[END]") || line.includes("Now saving Message Log"))) {
                const day = dayMap.get(currentDay)!
                if (day) {
                    day.ended = true
                }
                currentDay = undefined
                statGainsByType.clear()
                continue
            }

            if (currentDay === undefined) {
                // Skip lines before the first detected day in this file.
                continue
            }

            const day = dayMap.get(currentDay)!

            // Extract training stat gains for any training type (usually logged before execution).
            const statGainsMatch = line.match(REGEX.trainingStatGains)
            if (statGainsMatch) {
                const trainingType = statGainsMatch[1].toLowerCase()
                let gains: number[] = []
                if (statGainsMatch[2]) {
                    // Old format
                    gains = statGainsMatch[2]
                        .split(",")
                        .map((s) => parseInt(s.trim(), 10))
                        .filter((n) => !isNaN(n))
                } else if (statGainsMatch[3] !== undefined) {
                    // New format
                    gains = [parseInt(statGainsMatch[3], 10), parseInt(statGainsMatch[4], 10), parseInt(statGainsMatch[5], 10), parseInt(statGainsMatch[6], 10), parseInt(statGainsMatch[7], 10)]
                }

                if (gains.length === 5) {
                    statGainsByType.set(trainingType, gains)
                    // If execution already happened for this type, update stat gains immediately.
                    if (day.trainingType && day.trainingType.toLowerCase() === trainingType) {
                        day.trainingStatGains = gains
                    }
                }
            }

            // Extract training execution type and match with stored stat gains.
            const trainingExec = line.match(REGEX.trainingExecution)
            if (trainingExec) {
                const executedType = trainingExec[1]
                day.trainingType = executedType
                // Match stat gains if available for this training type (from earlier in the log).
                const storedGains = statGainsByType.get(executedType.toLowerCase())
                if (storedGains) {
                    day.trainingStatGains = storedGains
                }
            }

            for (const key of ACTION_KEYS) {
                if (matchesLine(line, MATCHERS[key])) {
                    day.actions[key] = true
                    day.triggers[key].push(line)
                    if (!day.firstNotable && (key === "training" || key === "race")) {
                        day.firstNotable = line.trim()
                    }
                }
            }
        }

        if (!foundAnyDay) {
            errors.push({
                message: `No days found in ${file.name}.`,
                fileName: file.name,
            })
        }
    }

    const dayNumbers = Array.from(dayMap.keys()).sort((a, b) => a - b)
    const records: Array<DayRecord | GapRecord | FileDividerRecord> = []
    let prevDay: number | undefined
    let prevFileName: string | undefined

    for (const d of dayNumbers) {
        const entry = dayMap.get(d)!

        // Insert gap if needed.
        if (prevDay !== undefined && d > prevDay + 1) {
            records.push({ kind: "gap", from: prevDay + 1, to: d - 1 })
        }

        // Insert file divider if fileName changes (for consecutive days or after gaps).
        // Also insert at the very beginning for the first file.
        if ((prevFileName && entry.fileName !== prevFileName) || prevFileName === undefined) {
            records.push({ kind: "fileDivider", fileName: entry.fileName })
        }

        records.push({
            kind: "day",
            dayNumber: d,
            dateText: entry.dateText,
            summary: composeSummary(entry.actions, entry.firstNotable),
            actions: entry.actions,
            fileName: entry.fileName,
            triggers: entry.triggers,
            year: entry.year,
            trainingType: entry.trainingType,
            trainingStatGains: entry.trainingStatGains,
            timestamp: entry.timestamp,
        })
        prevDay = d
        prevFileName = entry.fileName
    }

    return {
        records,
        errors,
        meta: {
            firstDay: firstDaySeen,
            lastDay: lastDaySeen,
            filesProcessed: sorted.length,
        },
    }
}

export function formatGapText(gap: GapRecord): string {
    return gap.from === gap.to ? `Day ${gap.from} missing.` : `Days ${gap.from}–${gap.to} missing.`
}

export type YearSummary = {
    year: string
    energyCount: number
    moodCount: number
    injuryCount: number
    raceCount: number
    trainingCount: number
    totalStatGains: {
        speed: number
        stamina: number
        power: number
        guts: number
        wit: number
    }
    trainingCounts: {
        speed: number
        stamina: number
        power: number
        guts: number
        wit: number
    }
    elapsedTimeMs?: number // Elapsed time in milliseconds for this year
    elapsedTimeFormatted?: string // "HH:MM:SS"
    elapsedTimeHuman?: string // "X hours and Y minutes"
    hasFinals?: boolean // True if this year summary includes Finals days (turns 73-75)
}

export type YearSummariesResult = {
    summaries: YearSummary[]
    totalElapsedTimeMs?: number
    totalElapsedTimeFormatted?: string
    totalElapsedTimeHuman?: string
}

function formatElapsedTime(ms: number): { formatted: string; human: string } {
    const totalSeconds = Math.floor(ms / 1000)
    const hours = Math.floor(totalSeconds / 3600)
    const minutes = Math.floor((totalSeconds % 3600) / 60)
    const seconds = totalSeconds % 60

    const formatted = `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`

    let human = ""
    if (hours > 0) {
        human += `${hours} ${hours === 1 ? "hour" : "hours"}`
    }
    if (minutes > 0) {
        if (human) human += " and "
        human += `${minutes} ${minutes === 1 ? "minute" : "minutes"}`
    }
    if (!human && seconds > 0) {
        human = `${seconds} ${seconds === 1 ? "second" : "seconds"}`
    }
    if (!human) human = "0 seconds"

    return { formatted, human }
}

export function aggregateYearSummaries(records: DayRecord[]): YearSummariesResult {
    // Filter to only days with valid year field.
    const daysWithYear = records.filter((r) => r.year)

    // Group by year.
    const yearMap = new Map<string, DayRecord[]>()
    for (const day of daysWithYear) {
        const year = day.year!
        if (!yearMap.has(year)) {
            yearMap.set(year, [])
        }
        yearMap.get(year)!.push(day)
    }

    // Aggregate statistics for each year.
    const summaries: YearSummary[] = []
    const yearOrder = ["Junior", "Classic", "Senior"]
    let totalElapsedTimeMs = 0

    for (const year of yearOrder) {
        const days = yearMap.get(year)
        if (!days || days.length === 0) continue

        let energyCount = 0
        let moodCount = 0
        let injuryCount = 0
        let raceCount = 0
        let trainingCount = 0
        const statTotals = { speed: 0, stamina: 0, power: 0, guts: 0, wit: 0 }
        const trainingCounts = { speed: 0, stamina: 0, power: 0, guts: 0, wit: 0 }
        let yearFirstTimestamp: number | undefined
        let yearLastTimestamp: number | undefined
        let hasFinals = false

        for (const day of days) {
            // Check if this day is Finals (turns 73-75).
            if (day.dayNumber >= 73 && day.dayNumber <= 75) {
                hasFinals = true
            }
            // Track timestamps for elapsed time calculation.
            if (day.timestamp !== undefined) {
                if (yearFirstTimestamp === undefined || day.timestamp < yearFirstTimestamp) {
                    yearFirstTimestamp = day.timestamp
                }
                if (yearLastTimestamp === undefined || day.timestamp > yearLastTimestamp) {
                    yearLastTimestamp = day.timestamp
                }
            }

            if (day.actions.energy) energyCount++
            if (day.actions.mood) moodCount++
            if (day.actions.injury) injuryCount++
            if (day.actions.race) raceCount++
            if (day.actions.training) {
                trainingCount++
                // Sum stat gains if available.
                if (day.trainingStatGains && day.trainingStatGains.length === 5) {
                    statTotals.speed += day.trainingStatGains[0]
                    statTotals.stamina += day.trainingStatGains[1]
                    statTotals.power += day.trainingStatGains[2]
                    statTotals.guts += day.trainingStatGains[3]
                    statTotals.wit += day.trainingStatGains[4]
                }
                // Count training type executions.
                if (day.trainingType) {
                    const type = day.trainingType.toLowerCase() as keyof typeof trainingCounts
                    if (type in trainingCounts) {
                        trainingCounts[type]++
                    }
                }
            }
        }

        // Calculate elapsed time for this year.
        let elapsedTimeMs: number | undefined
        let elapsedTimeFormatted: string | undefined
        let elapsedTimeHuman: string | undefined

        if (yearFirstTimestamp !== undefined && yearLastTimestamp !== undefined) {
            elapsedTimeMs = yearLastTimestamp - yearFirstTimestamp
            const formatted = formatElapsedTime(elapsedTimeMs)
            elapsedTimeFormatted = formatted.formatted
            elapsedTimeHuman = formatted.human
            // Sum to total elapsed time.
            totalElapsedTimeMs += elapsedTimeMs
        }

        summaries.push({
            year,
            energyCount,
            moodCount,
            injuryCount,
            raceCount,
            trainingCount,
            totalStatGains: statTotals,
            trainingCounts,
            elapsedTimeMs,
            elapsedTimeFormatted,
            elapsedTimeHuman,
            hasFinals: year === "Senior" ? hasFinals : undefined,
        })
    }

    // Calculate total elapsed time (sum of all year elapsed times).
    let totalElapsedTimeFormatted: string | undefined
    let totalElapsedTimeHuman: string | undefined

    if (totalElapsedTimeMs > 0) {
        const formatted = formatElapsedTime(totalElapsedTimeMs)
        totalElapsedTimeFormatted = formatted.formatted
        totalElapsedTimeHuman = formatted.human
    }

    return {
        summaries,
        totalElapsedTimeMs: totalElapsedTimeMs > 0 ? totalElapsedTimeMs : undefined,
        totalElapsedTimeFormatted,
        totalElapsedTimeHuman,
    }
}
