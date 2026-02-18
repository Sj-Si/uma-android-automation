import { useContext, useState, useMemo, useCallback, memo, useEffect, useRef } from "react"
import { MessageLogContext } from "../../context/MessageLogContext"
import { BotStateContext } from "../../context/BotStateContext"
import { useSettings } from "../../context/SettingsContext"
import { StyleSheet, Text, View, TextInput, TouchableOpacity, Animated } from "react-native"
import * as Clipboard from "expo-clipboard"
import { Copy, Plus, Minus, Type, X, ArrowUp, ArrowDown, ArrowUpAZ, ArrowDownZA } from "lucide-react-native"
import { Popover, PopoverContent, PopoverTrigger } from "../ui/popover"
import { AlertDialog, AlertDialogAction, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "../ui/alert-dialog"
import { CustomScrollView } from "../CustomScrollView"

const styles = StyleSheet.create({
    logInnerContainer: {
        flex: 1,
        width: "100%",
        backgroundColor: "#2f2f2f",
        borderStyle: "solid",
        borderRadius: 25,
        marginBottom: 10,
        elevation: 10,
        position: "relative",
    },
    searchContainer: {
        flexDirection: "row",
        alignItems: "center",
        paddingHorizontal: 15,
        paddingVertical: 10,
        backgroundColor: "#3a3a3a",
        borderTopLeftRadius: 25,
        borderTopRightRadius: 25,
    },
    searchInput: {
        flex: 1,
        backgroundColor: "transparent",
        color: "white",
        paddingHorizontal: 12,
        paddingVertical: 8,
        fontSize: 12,
    },
    searchInputContainer: {
        flex: 1,
        flexDirection: "row",
        alignItems: "center",
        backgroundColor: "#4a4a4a",
        borderRadius: 8,
        marginRight: 8,
    },
    clearButton: {
        padding: 4,
        marginRight: 8,
    },
    actionButton: {
        padding: 8,
        borderRadius: 6,
        backgroundColor: "#5a5a5a",
        marginLeft: 4,
    },
    logContainer: {
        flex: 1,
        paddingHorizontal: 15,
        paddingBottom: 10,
        marginTop: 10,
    },
    logText: {
        color: "white",
        fontFamily: "monospace",
    },
    logTextWarning: {
        color: "#ffa500",
        fontFamily: "monospace",
    },
    logTextError: {
        color: "#ff4444",
        fontFamily: "monospace",
    },
    logItem: {
        paddingVertical: 1,
        paddingHorizontal: 2,
    },
    popoverContentContainer: {
        flexDirection: "row",
        alignItems: "center",
        gap: 12,
    },
    popoverButtonContainer: {
        flexDirection: "row",
        gap: 8,
    },
    popoverButton: {
        alignItems: "center",
        justifyContent: "center",
        paddingVertical: 6,
        paddingHorizontal: 8,
        borderRadius: 4,
        backgroundColor: "#5a5a5a",
        width: 28,
        height: 28,
    },
    fontSizeDisplay: {
        color: "white",
        fontSize: 12,
        fontWeight: "600",
    },
    floatingButtonContainer: {
        position: "absolute",
        bottom: 15,
        right: 15,
        flexDirection: "column",
        gap: 6,
        zIndex: 1000,
    },
    floatingButton: {
        width: 36,
        height: 36,
        borderRadius: 18,
        backgroundColor: "#5a5a5a",
        alignItems: "center",
        justifyContent: "center",
        elevation: 3,
        opacity: 0.7,
    },
})

interface LogMessage {
    id: string
    text: string
    type: "normal" | "warning" | "error"
    messageId?: number
}

// Memoized LogItem component for better performance.
const LogItem = memo(({ item, fontSize, onLongPress, enableMessageIdDisplay }: { item: LogMessage; fontSize: number; onLongPress: (message: string) => void; enableMessageIdDisplay: boolean }) => {
    const getTextStyle = useCallback(() => {
        const baseStyle = {
            fontSize: fontSize,
            lineHeight: fontSize * 1.5,
        }

        switch (item.type) {
            case "warning":
                return { ...styles.logTextWarning, ...baseStyle }
            case "error":
                return { ...styles.logTextError, ...baseStyle }
            default:
                return { ...styles.logText, ...baseStyle }
        }
    }, [item.type, fontSize])

    // Trim leading newlines when message ID is present to maintain alignment.
    const displayText = useMemo(() => {
        if (enableMessageIdDisplay && item.messageId !== undefined) {
            // Remove leading newlines and whitespace to keep alignment with message ID.
            return item.text.replace(/^[\n\r\s]+/, "")
        }
        return item.text
    }, [item.text, item.messageId, enableMessageIdDisplay])

    return (
        <TouchableOpacity style={styles.logItem} onLongPress={() => onLongPress(item.text)} delayLongPress={500}>
            <View style={{ flexDirection: "row", alignItems: "flex-start" }}>
                {enableMessageIdDisplay && item.messageId !== undefined && <Text style={[getTextStyle(), { color: "gray", minWidth: 40 }]}>[{item.messageId}]</Text>}
                <Text style={[getTextStyle(), { flex: 1, flexShrink: 1 }]}>{displayText}</Text>
            </View>
        </TouchableOpacity>
    )
})

const MessageLog = () => {
    const mlc = useContext(MessageLogContext)
    const bsc = useContext(BotStateContext)
    const { saveSettingsImmediate } = useSettings()
    const [searchQuery, setSearchQuery] = useState("")
    const [showErrorDialog, setShowErrorDialog] = useState(false)
    const [errorMessage, setErrorMessage] = useState("")
    const [sortOrder, setSortOrder] = useState<"asc" | "desc">("asc")
    const scrollViewRef = useRef<any>(null)
    const [scrollOffset, setScrollOffset] = useState(0)
    const [contentHeight, setContentHeight] = useState(0)
    const [viewportHeight, setViewportHeight] = useState(0)

    const fontSize = bsc.settings.misc.messageLogFontSize

    // Animated values for smooth scroll button transitions.
    const topButtonOpacity = useRef(new Animated.Value(0)).current
    const bottomButtonOpacity = useRef(new Animated.Value(0)).current
    const topHideTimeoutRef = useRef<NodeJS.Timeout | null>(null)
    const bottomHideTimeoutRef = useRef<NodeJS.Timeout | null>(null)

    // Determine if scrolling is needed and scroll buttons visibility.
    const needsScrolling = contentHeight > viewportHeight + 10 // Add buffer to account for rounding.
    const scrollThreshold = 50 // Increased threshold for more reliable detection.
    const maxScrollOffset = Math.max(0, contentHeight - viewportHeight)

    // Check if at top or bottom of log.
    const isAtTop = scrollOffset <= scrollThreshold
    const isAtBottom = needsScrolling && maxScrollOffset > 0 && scrollOffset >= Math.max(0, maxScrollOffset - scrollThreshold)

    const showScrollButtons = needsScrolling && contentHeight > 0 && viewportHeight > 0
    const showScrollToTop = showScrollButtons && !isAtTop
    const showScrollToBottom = showScrollButtons && !isAtBottom

    const showError = useCallback((message: string) => {
        setErrorMessage(message)
        setShowErrorDialog(true)
    }, [])

    // Format settings when settings change.
    const formattedSettingsString = useMemo(() => {
        const settings = bsc.settings

        // Training stat targets by distance.
        const sprintTargetsString = `Sprint: \n\t\tSpeed: ${settings.trainingStatTarget.trainingSprintStatTarget_speedStatTarget}\t\tStamina: ${settings.trainingStatTarget.trainingSprintStatTarget_staminaStatTarget}\t\tPower: ${settings.trainingStatTarget.trainingSprintStatTarget_powerStatTarget}\n\t\tGuts: ${settings.trainingStatTarget.trainingSprintStatTarget_gutsStatTarget}\t\t\tWit: ${settings.trainingStatTarget.trainingSprintStatTarget_witStatTarget}`
        const mileTargetsString = `Mile: \n\t\tSpeed: ${settings.trainingStatTarget.trainingMileStatTarget_speedStatTarget}\t\tStamina: ${settings.trainingStatTarget.trainingMileStatTarget_staminaStatTarget}\t\tPower: ${settings.trainingStatTarget.trainingMileStatTarget_powerStatTarget}\n\t\tGuts: ${settings.trainingStatTarget.trainingMileStatTarget_gutsStatTarget}\t\t\tWit: ${settings.trainingStatTarget.trainingMileStatTarget_witStatTarget}`
        const mediumTargetsString = `Medium: \n\t\tSpeed: ${settings.trainingStatTarget.trainingMediumStatTarget_speedStatTarget}\t\tStamina: ${settings.trainingStatTarget.trainingMediumStatTarget_staminaStatTarget}\t\tPower: ${settings.trainingStatTarget.trainingMediumStatTarget_powerStatTarget}\n\t\tGuts: ${settings.trainingStatTarget.trainingMediumStatTarget_gutsStatTarget}\t\t\tWit: ${settings.trainingStatTarget.trainingMediumStatTarget_witStatTarget}`
        const longTargetsString = `Long: \n\t\tSpeed: ${settings.trainingStatTarget.trainingLongStatTarget_speedStatTarget}\t\tStamina: ${settings.trainingStatTarget.trainingLongStatTarget_staminaStatTarget}\t\tPower: ${settings.trainingStatTarget.trainingLongStatTarget_powerStatTarget}\n\t\tGuts: ${settings.trainingStatTarget.trainingLongStatTarget_gutsStatTarget}\t\t\tWit: ${settings.trainingStatTarget.trainingLongStatTarget_witStatTarget}`

        // Racing plan settings.
        const racingPlanString =
            settings.racing.racingPlan && settings.racing.racingPlan !== "[]" && typeof settings.racing.racingPlan === "string"
                ? `${JSON.parse(settings.racing.racingPlan).length} Race(s) Selected`
                : "None Selected"

        return `🏁 Campaign Selected: ${settings.general.scenario !== "" ? `${settings.general.scenario}` : "Please select one in the Select Campaign option"}
👤 Profile Selected: ${settings.misc.currentProfileName ? `${settings.misc.currentProfileName}` : "Default Profile"}

---------- Training Event Options ----------
🎭 Special Event Overrides: ${
            Object.keys(settings.trainingEvent.specialEventOverrides).length === 0
                ? "No Special Event Overrides"
                : `${Object.keys(settings.trainingEvent.specialEventOverrides).length} Special Event Overrides applied`
        }
👤 Character Event Overrides: ${
            Object.keys(settings.trainingEvent.characterEventOverrides).length === 0
                ? "No Character Event Overrides"
                : `${Object.keys(settings.trainingEvent.characterEventOverrides).length} Character Event Override(s) applied`
        }
💪 Support Event Overrides: ${
            Object.keys(settings.trainingEvent.supportEventOverrides).length === 0
                ? "No Support Event Overrides"
                : `${Object.keys(settings.trainingEvent.supportEventOverrides).length} Support Event Override(s) applied`
        }
🔋 Prioritize Energy Options: ${settings.trainingEvent.enablePrioritizeEnergyOptions ? "✅" : "❌"}

---------- Training Options ----------
🚫 Training Blacklist: ${settings.training.trainingBlacklist.length === 0 ? "No Trainings blacklisted" : `${settings.training.trainingBlacklist.join(", ")}`}
📊 Stat Prioritization: ${
            settings.training.statPrioritization.length === 0 ? "Using Default Stat Prioritization: Speed, Stamina, Power, Wit, Guts" : `${settings.training.statPrioritization.join(", ")}`
        }
🔍 Maximum Failure Chance Allowed: ${settings.training.maximumFailureChance}%
⚠️ Enable Riskier Training: ${settings.training.enableRiskyTraining ? "✅" : "❌"}${
            settings.training.enableRiskyTraining
                ? `\n   📊 Minimum Main Stat Gain Threshold: ${settings.training.riskyTrainingMinStatGain}\n   🎯 Risky Training Maximum Failure Chance: ${settings.training.riskyTrainingMaxFailureChance}%`
                : ""
        }
🔄 Disable Training on Maxed Stat: ${settings.training.disableTrainingOnMaxedStat ? "✅" : "❌"}
✨ Focus on Sparks for Stat Targets: ${settings.training.focusOnSparkStatTarget.length === 0 ? "None" : settings.training.focusOnSparkStatTarget.join(", ")}
📏 Preferred Distance Override: ${settings.training.preferredDistanceOverride === "Default" ? "Default" : settings.training.preferredDistanceOverride}
🌈 Enable Rainbow Training Bonus: ${settings.training.enableRainbowTrainingBonus ? "✅" : "❌"}
💡 Prioritize Skill Hints: ${settings.training.enablePrioritizeSkillHints ? "✅" : "❌"}
☀️ Must Rest Before Summer: ${settings.training.mustRestBeforeSummer ? "✅" : "❌"}
📏 Manual Stat Cap: ${settings.training.manualStatCap}
🎯 Train Wit During Finale: ${settings.training.trainWitDuringFinale ? "✅" : "❌"}

---------- Training Stat Targets by Distance ----------
${sprintTargetsString}
${mileTargetsString}
${mediumTargetsString}
${longTargetsString}

---------- Tesseract OCR Optimization ----------
🔍 OCR Threshold: ${settings.ocr.ocrThreshold}
🔍 Enable Automatic OCR retry: ${settings.ocr.enableAutomaticOCRRetry ? "✅" : "❌"}
🔍 Minimum OCR Confidence: ${settings.ocr.ocrConfidence}
🔍 Hide OCR String Comparison Results: ${settings.debug.enableHideOCRComparisonResults ? "✅" : "❌"}

---------- Racing Options ----------
👥 Prioritize Farming Fans: ${settings.racing.enableFarmingFans ? "✅" : "❌"}
⏰ Modulo Days to Farm Fans: ${settings.racing.enableFarmingFans ? `${settings.racing.daysToRunExtraRaces} days` : "❌"}
🔄 Disable Race Retries: ${settings.racing.disableRaceRetries ? "✅" : "❌"}
\t🔄 Allow Daily Free Race Retry: ${settings.racing.enableFreeRaceRetry ? "✅" : "❌"}
🏁 Stop on Mandatory Race: ${settings.racing.enableStopOnMandatoryRaces ? "✅" : "❌"}
🏃 Force Racing Every Day: ${settings.racing.enableForceRacing ? "✅" : "❌"}
🏁 Enable User In-Game Race Agenda: ${settings.racing.enableUserInGameRaceAgenda ? "✅" : "❌"}
🏁 Selected User In-Game Race Agenda: ${settings.racing.selectedUserAgenda}
🏁 Enable Racing Plan: ${settings.racing.enableRacingPlan ? "✅" : "❌"}
🏁 Racing Plan is Mandatory: ${settings.racing.enableMandatoryRacingPlan ? "✅" : "❌"}
🏁 Racing Plan: ${racingPlanString}
👥 Minimum Fans Threshold: ${settings.racing.minFansThreshold}
🏃 Preferred Terrain: ${settings.racing.preferredTerrain}
🏆 Preferred Grades: ${settings.racing.preferredGrades.join(", ")}
🏃 Preferred Distances: ${settings.racing.preferredDistances.join(", ")}
📅 Look Ahead Days: ${settings.racing.lookAheadDays} days
⏰ Smart Racing Check Interval: ${settings.racing.smartRacingCheckInterval} days
🎯 Junior Year Race Strategy: ${settings.racing.juniorYearRaceStrategy}
🎯 Classic/Senior Year Race Strategy: ${settings.racing.originalRaceStrategy}
📊 Minimum Quality Threshold: ${settings.racing.minimumQualityThreshold}
⏱️ Time Decay Factor: ${settings.racing.timeDecayFactor}
📈 Improvement Threshold: ${settings.racing.improvementThreshold}

---------- Skill Options ----------
🔍 Skill Point Check: ${settings.skills.enableSkillPointCheck ? `Stop on ${settings.skills.skillPointCheck} Skill Points or more` : "❌"}
🏃 Running Style Override: ${settings.skills.preferredRunningStyle}
🛣️ Track Distance Override: ${settings.skills.preferredTrackDistance}
🛣️ Track Surface Override: ${settings.skills.preferredTrackSurface}
📅 Pre-Finals Skill Plan: ${settings.skills.plans.preFinals.enabled ? "✅" : "❌"}${
    settings.skills.plans.preFinals.enabled
        ? `\n\t💲 Buy All Inherited Unique Skills: ${settings.skills.plans.preFinals.enableBuyInheritedUniqueSkills ? "✅" : "❌"}\n\t💲 Buy All Negative Skills: ${settings.skills.plans.preFinals.enableBuyNegativeSkills ? "✅" : "❌"}\n\t💸 Spending Strategy: ${settings.skills.plans.preFinals.strategy ? "✅" : "❌"}`
        : ""
}
📅 CareerComplete Skill Plan: ${settings.skills.plans.careerComplete.enabled ? "✅" : "❌"}${
    settings.skills.plans.careerComplete.enabled
        ? `\n\t💲 Buy All Inherited Unique Skills: ${settings.skills.plans.careerComplete.enableBuyInheritedUniqueSkills ? "✅" : "❌"}\n\t💲 Buy All Negative Skills: ${settings.skills.plans.careerComplete.enableBuyNegativeSkills ? "✅" : "❌"}\n\t💸 Spending Strategy: ${settings.skills.plans.careerComplete.strategy ? "✅" : "❌"}`
        : ""
}

---------- Misc Options ----------
🔍 Popup Check: ${settings.general.enablePopupCheck ? "✅" : "❌"}
🔍 Enable Crane Game Attempt: ${settings.general.enableCraneGameAttempt ? "✅" : "❌"}
🛑 Stop Before Finals: ${settings.general.enableStopBeforeFinals ? "✅" : "❌"}
⏰ Wait Delay: ${settings.general.waitDelay}s

---------- Debug Options ----------
🐛 Debug Mode: ${settings.debug.enableDebugMode ? "✅" : "❌"}
🔍 Minimum Template Match Confidence: ${settings.debug.templateMatchConfidence}
🔍 Custom Scale: ${settings.debug.templateMatchCustomScale}
📹 Enable Screen Recording: ${
            settings.debug.enableScreenRecording ? `✅ (${settings.debug.recordingBitRate} Mbps, ${settings.debug.recordingFrameRate} FPS, ${settings.debug.recordingResolutionScale}x scale)` : "❌"
        }
🔍 Start Template Matching Test: ${settings.debug.debugMode_startTemplateMatchingTest ? "✅" : "❌"}
🔍 Start Single Training OCR Test: ${settings.debug.debugMode_startSingleTrainingOCRTest ? "✅" : "❌"}
🔍 Start Comprehensive Training OCR Test: ${settings.debug.debugMode_startComprehensiveTrainingOCRTest ? "✅" : "❌"}
🔍 Start Date OCR Test: ${settings.debug.debugMode_startDateOCRTest ? "✅" : "❌"}
🔍 Start Race List Detection Test: ${settings.debug.debugMode_startRaceListDetectionTest ? "✅" : "❌"}
🔍 Start Aptitudes Detection Test: ${settings.debug.debugMode_startAptitudesDetectionTest ? "✅" : "❌"}
🔍 Start Main Screen OCR Test: ${settings.debug.debugMode_startMainScreenOCRTest ? "✅" : "❌"}
🔍 Start Training Screen OCR Test: ${settings.debug.debugMode_startTrainingScreenOCRTest ? "✅" : "❌"}
🔍 Hide String Comparison Results: ${settings.debug.enableHideOCRComparisonResults ? "✅" : "❌"}

****************************************`
    }, [bsc.settings])

    // Save the formatted string to the context for persistence.
    useEffect(() => {
        bsc.setSettings({
            ...bsc.settings,
            misc: { ...bsc.settings.misc, formattedSettingsString: formattedSettingsString },
        })
    }, [formattedSettingsString])

    // Don't add formattedSettingsString if logs are already present (Android already copied it).
    const introMessage = useMemo(() => {
        const hasLogs = mlc.messageLog.length > 0
        const baseMessage = `****************************************\nWelcome to ${bsc.appName} v${bsc.appVersion}\n****************************************`

        if (hasLogs) {
            // If logs exist, Android already copied the settings string, so don't include it.
            return baseMessage
        }

        // Only include settings string if enabled and no logs exist yet.
        return bsc.settings.misc.enableSettingsDisplay ? `${baseMessage}\n\n${formattedSettingsString}` : baseMessage
    }, [bsc.appName, bsc.appVersion, bsc.settings.misc.enableSettingsDisplay, formattedSettingsString, mlc.messageLog.length])

    // Process log messages with color coding and virtualization.
    const processedMessages = useMemo((): LogMessage[] => {
        // Add intro message as the first item.
        const introLines = introMessage.split("\n")
        const introMessages = introLines.map((line, index) => ({
            id: `intro-${index}`,
            text: line,
            type: "normal" as const,
        }))

        // Process actual log messages.
        const logMessages = mlc.messageLog.map((entry, index) => {
            let type: "normal" | "warning" | "error" = "normal"

            if (entry.message.includes("[ERROR]")) {
                type = "error"
            } else if (entry.message.includes("[WARNING]") || entry.message.includes("[WARN]")) {
                type = "warning"
            }

            return {
                id: `log-${index}-${entry.message.substring(0, 20)}`,
                text: entry.message,
                type,
                messageId: entry.id,
            }
        })

        // Parse timestamp from message text (format: HH:MM:SS.mmm).
        const parseTimestamp = (text: string): number => {
            // Match timestamps like "00:00:00.462", allowing optional leading whitespace/newlines.
            const match = text.match(/^\s*(\d{2}):(\d{2}):(\d{2})\.(\d{3})/)
            if (match) {
                const [, hours, minutes, seconds, milliseconds] = match
                return parseInt(hours) * 3600000 + parseInt(minutes) * 60000 + parseInt(seconds) * 1000 + parseInt(milliseconds)
            }
            // Return -1 for messages without valid timestamps (e.g., "--:--:--.---").
            return -1
        }

        // Sort log messages by timestamp (primary) and messageId (secondary/tiebreaker).
        const sortedLogMessages = [...logMessages].sort((a, b) => {
            const timestampA = parseTimestamp(a.text)
            const timestampB = parseTimestamp(b.text)

            // Primary sort by timestamp for chronological order.
            if (timestampA !== timestampB) {
                return sortOrder === "desc" ? timestampB - timestampA : timestampA - timestampB
            }

            // Secondary sort by messageId when timestamps are equal.
            const idA = a.messageId ?? 0
            const idB = b.messageId ?? 0
            return sortOrder === "desc" ? idB - idA : idA - idB
        })

        // Always keep intro message at the top, regardless of sort order.
        return [...introMessages, ...sortedLogMessages]
    }, [mlc.messageLog, introMessage, sortOrder])

    // Filter messages based on search query (excluding intro messages).
    const filteredMessages = useMemo(() => {
        if (!searchQuery.trim()) {
            // Always return a new array reference to ensure FlashList detects the change.
            return [...processedMessages]
        }

        const query = searchQuery.toLowerCase()
        return processedMessages.filter((message) => {
            // Only search log messages, not intro messages.
            if (message.id.startsWith("intro-")) {
                return false
            }
            return message.text.toLowerCase().includes(query)
        })
    }, [processedMessages, searchQuery])

    // Force the CustomScrollView to refresh the FlashList when search is cleared by using a key that changes.
    // This ensures a complete remount when transitioning from searching to having no search query.
    const listKey = useMemo(() => (searchQuery.trim().length === 0 ? "all-messages" : `search-${searchQuery}`), [searchQuery])

    // Scroll to top when data changes (search or sort).
    useEffect(() => {
        // Use setTimeout to ensure the scroll happens after the list has updated.
        const timeoutId = setTimeout(() => {
            scrollViewRef.current?.scrollToOffset({
                offset: 0,
                animated: false,
            })
        }, 0)
        return () => clearTimeout(timeoutId)
    }, [listKey, sortOrder])

    // Toggle sort order between ascending and descending.
    const toggleSortOrder = useCallback(() => {
        setSortOrder((prev) => (prev === "asc" ? "desc" : "asc"))
    }, [])

    // Scroll to top of the list.
    const scrollToTop = useCallback(() => {
        scrollViewRef.current?.scrollToOffset({
            offset: 0,
            animated: true,
        })
    }, [])

    // Scroll to bottom of the list.
    const scrollToBottom = useCallback(() => {
        if (filteredMessages.length > 0) {
            try {
                scrollViewRef.current?.scrollToIndex({
                    index: filteredMessages.length - 1,
                    animated: true,
                })
            } catch (error) {
                // Fallback to scrolling to a large offset if scrollToIndex fails.
                scrollViewRef.current?.scrollToOffset({
                    offset: 999999,
                    animated: true,
                })
            }
        }
    }, [filteredMessages.length])

    // Handle scroll events to track position.
    const handleScroll = useCallback((event: any) => {
        const nativeEvent = event.nativeEvent
        const offset = nativeEvent?.contentOffset?.y ?? 0
        const contentHeight = nativeEvent?.contentSize?.height ?? 0
        const layoutHeight = nativeEvent?.layoutMeasurement?.height ?? 0

        setScrollOffset(Math.max(0, offset))

        // Update content and viewport height from scroll event if available.
        if (contentHeight > 0) {
            setContentHeight(contentHeight)
        }
        if (layoutHeight > 0) {
            setViewportHeight(layoutHeight)
        }
    }, [])

    // Handle scroll end to get final position.
    const handleScrollEnd = useCallback((event: any) => {
        const nativeEvent = event.nativeEvent
        const offset = nativeEvent?.contentOffset?.y ?? 0
        setScrollOffset(Math.max(0, offset))
    }, [])

    // Handle content size changes to update content height.
    const handleContentSizeChange = useCallback((width: number, height: number) => {
        if (height > 0) {
            setContentHeight(height)
        }
    }, [])

    // Handle layout changes to update viewport height.
    const handleLayout = useCallback((event: any) => {
        const { height } = event.nativeEvent.layout
        if (height > 0) {
            setViewportHeight(height)
        }
    }, [])

    // Animate scroll button visibility with smooth transitions.
    useEffect(() => {
        // Clear any pending timeouts.
        if (topHideTimeoutRef.current) {
            clearTimeout(topHideTimeoutRef.current)
            topHideTimeoutRef.current = null
        }
        if (bottomHideTimeoutRef.current) {
            clearTimeout(bottomHideTimeoutRef.current)
            bottomHideTimeoutRef.current = null
        }

        // Animate top scroll button.
        if (showScrollToTop) {
            Animated.timing(topButtonOpacity, {
                toValue: 1,
                duration: 100,
                useNativeDriver: true,
            }).start()
        } else {
            topHideTimeoutRef.current = setTimeout(() => {
                Animated.timing(topButtonOpacity, {
                    toValue: 0,
                    duration: 100,
                    useNativeDriver: true,
                }).start()
            })
        }

        // Animate bottom scroll button.
        if (showScrollToBottom) {
            Animated.timing(bottomButtonOpacity, {
                toValue: 1,
                duration: 100,
                useNativeDriver: true,
            }).start()
        } else {
            bottomHideTimeoutRef.current = setTimeout(() => {
                Animated.timing(bottomButtonOpacity, {
                    toValue: 0,
                    duration: 100,
                    useNativeDriver: true,
                }).start()
            })
        }

        return () => {
            if (topHideTimeoutRef.current) {
                clearTimeout(topHideTimeoutRef.current)
            }
            if (bottomHideTimeoutRef.current) {
                clearTimeout(bottomHideTimeoutRef.current)
            }
        }
    }, [showScrollToTop, showScrollToBottom, topButtonOpacity, bottomButtonOpacity])

    // Font size control functions.
    const increaseFontSize = useCallback(async () => {
        const newFontSize = Math.min(fontSize + 1, 24)
        const updatedSettings = {
            ...bsc.settings,
            misc: { ...bsc.settings.misc, messageLogFontSize: newFontSize },
        }
        bsc.setSettings(updatedSettings)
        await saveSettingsImmediate(updatedSettings)
    }, [fontSize, bsc.settings, bsc.setSettings, saveSettingsImmediate])

    const decreaseFontSize = useCallback(async () => {
        const newFontSize = Math.max(fontSize - 1, 8)
        const updatedSettings = {
            ...bsc.settings,
            misc: { ...bsc.settings.misc, messageLogFontSize: newFontSize },
        }
        bsc.setSettings(updatedSettings)
        await saveSettingsImmediate(updatedSettings)
    }, [fontSize, bsc.settings, bsc.setSettings, saveSettingsImmediate])

    // Clear search query.
    const clearSearch = useCallback(() => {
        setSearchQuery("")
    }, [])

    // Copy all messages to clipboard.
    const copyToClipboard = useCallback(async () => {
        try {
            const allText = introMessage + "\n" + mlc.messageLog.map((entry) => entry.message).join("\n")
            await Clipboard.setStringAsync(allText)
        } catch (error) {
            showError("Failed to copy to clipboard")
        }
    }, [mlc.messageLog, introMessage, showError])

    // Copy individual message on long press.
    const handleLongPress = useCallback(
        async (message: string) => {
            try {
                await Clipboard.setStringAsync(message)
            } catch (error) {
                showError("Failed to copy message")
            }
        },
        [showError],
    )

    // Render individual log item.
    const renderLogItem = useCallback(
        ({ item }: { item: LogMessage }) => <LogItem item={item} fontSize={fontSize} onLongPress={handleLongPress} enableMessageIdDisplay={bsc.settings.misc.enableMessageIdDisplay} />,
        [fontSize, handleLongPress, bsc.settings.misc.enableMessageIdDisplay],
    )

    // Key extractor for FlatList.
    const keyExtractor = useCallback((item: LogMessage) => item.id, [])

    return (
        <View style={styles.logInnerContainer}>
            {/* Search Bar */}
            <View style={styles.searchContainer}>
                <View style={styles.searchInputContainer}>
                    <TextInput
                        style={styles.searchInput}
                        placeholder="Search messages..."
                        placeholderTextColor="#888"
                        value={searchQuery}
                        onChangeText={setSearchQuery}
                        autoCorrect={false}
                        autoCapitalize="none"
                    />
                    {searchQuery.length > 0 && (
                        <TouchableOpacity style={styles.clearButton} onPress={clearSearch}>
                            <X size={16} color="#888" />
                        </TouchableOpacity>
                    )}
                </View>
                <TouchableOpacity style={styles.actionButton} onPress={copyToClipboard}>
                    <Copy size={16} color="white" />
                </TouchableOpacity>
                <TouchableOpacity style={styles.actionButton} onPress={toggleSortOrder}>
                    {sortOrder === "asc" ? <ArrowUpAZ size={16} color="white" /> : <ArrowDownZA size={16} color="white" />}
                </TouchableOpacity>
                <Popover>
                    <PopoverTrigger asChild>
                        <TouchableOpacity style={styles.actionButton}>
                            <Type size={16} color="white" />
                        </TouchableOpacity>
                    </PopoverTrigger>
                    <PopoverContent className="bg-black w-auto p-2" align="end" side="bottom">
                        <View style={styles.popoverContentContainer}>
                            <Text style={styles.fontSizeDisplay}>Font Size: {fontSize}pt</Text>
                            <View style={styles.popoverButtonContainer}>
                                <TouchableOpacity style={styles.popoverButton} onPress={decreaseFontSize}>
                                    <Minus size={16} color="white" />
                                </TouchableOpacity>
                                <TouchableOpacity style={styles.popoverButton} onPress={increaseFontSize}>
                                    <Plus size={16} color="white" />
                                </TouchableOpacity>
                            </View>
                        </View>
                    </PopoverContent>
                </Popover>
            </View>

            {/* Log Messages */}
            <View style={styles.logContainer}>
                <CustomScrollView
                    ref={scrollViewRef}
                    key={listKey}
                    targetProps={{
                        data: filteredMessages,
                        renderItem: renderLogItem,
                        keyExtractor: keyExtractor,
                        removeClippedSubviews: true,
                        onScroll: handleScroll,
                        onMomentumScrollEnd: handleScrollEnd,
                        onScrollEndDrag: handleScrollEnd,
                        scrollEventThrottle: 16,
                        onContentSizeChange: handleContentSizeChange,
                        onLayout: handleLayout,
                    }}
                    hideScrollbar={true}
                />
            </View>

            {/* Floating Scroll Buttons */}
            {showScrollButtons && (
                <View style={styles.floatingButtonContainer}>
                    <Animated.View
                        style={{
                            opacity: topButtonOpacity,
                            pointerEvents: showScrollToTop ? "auto" : "none",
                        }}
                    >
                        <TouchableOpacity style={styles.floatingButton} onPress={scrollToTop}>
                            <ArrowUp size={16} color="white" />
                        </TouchableOpacity>
                    </Animated.View>
                    <Animated.View
                        style={{
                            opacity: bottomButtonOpacity,
                            pointerEvents: showScrollToBottom ? "auto" : "none",
                        }}
                    >
                        <TouchableOpacity style={styles.floatingButton} onPress={scrollToBottom}>
                            <ArrowDown size={16} color="white" />
                        </TouchableOpacity>
                    </Animated.View>
                </View>
            )}

            {/* Error Dialog */}
            <AlertDialog open={showErrorDialog} onOpenChange={setShowErrorDialog}>
                <AlertDialogContent onDismiss={() => setShowErrorDialog(false)}>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Error</AlertDialogTitle>
                        <AlertDialogDescription>{errorMessage}</AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogAction onPress={() => setShowErrorDialog(false)}>
                            <Text>OK</Text>
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </View>
    )
}

export default MessageLog
