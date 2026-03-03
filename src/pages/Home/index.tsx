import * as Application from "expo-application"
import MessageLog from "../../components/MessageLog"
import { useContext, useEffect, useRef, useState } from "react"
import { BotStateContext } from "../../context/BotStateContext"
import { useSettings } from "../../context/SettingsContext"
import { logWithTimestamp, logErrorWithTimestamp } from "../../lib/logger"
import { Animated, DeviceEventEmitter, StyleSheet, View, NativeModules } from "react-native"
import { Snackbar } from "react-native-paper"
import { MessageLogContext } from "../../context/MessageLogContext"
import { useTheme } from "../../context/ThemeContext"
import { Text } from "../../components/ui/text"
import { AlertDialog, AlertDialogAction, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "../../components/ui/alert-dialog"
import { Ionicons } from "@expo/vector-icons"
import { Tooltip, TooltipContent, TooltipTrigger } from "../../components/ui/tooltip"
import PageHeader from "../../components/PageHeader"
import { usePerformanceLogging } from "../../hooks/usePerformanceLogging"

import scenarios from "../../data/scenarios.json"
import SelectButton from "../../components/SelectButton"

const styles = StyleSheet.create({
    root: {
        flex: 1,
        flexDirection: "column",
        alignItems: "center",
        paddingHorizontal: 10,
        paddingVertical: 10,
    },
    contentContainer: {
        flex: 1,
        width: "100%",
        flexDirection: "column",
    },
    button: {
        width: 100,
    },
})

/**
 * The main Home page of the application.
 * Displays the Start/Stop button for the bot, a message log, and handles bot lifecycle events including settings persistence and readiness checks.
 */
const Home = () => {
    usePerformanceLogging("Home")
    const { StartModule } = NativeModules

    const { isDark, colors } = useTheme()
    const [isRunning, setIsRunning] = useState<boolean>(false)
    const [showNotReadyDialog, setShowNotReadyDialog] = useState<boolean>(false)
    const [snackbarOpen, setSnackbarOpen] = useState<boolean>(false)
    const [snackbarMessage, setSnackbarMessage] = useState<string>("")
    const [deviceMetrics, setDeviceMetrics] = useState<{ width: number; height: number; dpi: number } | null>(null)
    const [unsupportedReason, setUnsupportedReason] = useState<string | null>(null)

    const bsc = useContext(BotStateContext)
    const mlc = useContext(MessageLogContext)
    const { saveSettings } = useSettings()

    const pulseAnim = useRef(new Animated.Value(1)).current

    useEffect(() => {
        let animation: Animated.CompositeAnimation | null = null

        if (unsupportedReason) {
            // Pulsate the icon to grab attention when there's an unsupported device.
            animation = Animated.loop(
                Animated.sequence([
                    Animated.timing(pulseAnim, {
                        toValue: 1.25,
                        duration: 700,
                        useNativeDriver: true,
                    }),
                    Animated.timing(pulseAnim, {
                        toValue: 1,
                        duration: 700,
                        useNativeDriver: true,
                    }),
                ])
            )
            animation.start()
        } else {
            pulseAnim.setValue(1)
        }

        return () => {
            animation?.stop()
        }
    }, [unsupportedReason])

    useEffect(() => {
        const mediaProjectionSubscription = DeviceEventEmitter.addListener("MediaProjectionService", (data) => {
            setIsRunning(data["message"] === "Running")
        })

        const botServiceSubscription = DeviceEventEmitter.addListener("BotService", (data) => {
            if (data["message"] === "Running") {
                mlc.setMessageLog([])
            }
        })

        getVersion()
        fetchDeviceMetrics()

        return () => {
            mediaProjectionSubscription.remove()
            botServiceSubscription.remove()
        }
    }, [])

    /**
     * Fetch device metrics from NativeModule.
     */
    const fetchDeviceMetrics = async () => {
        try {
            const metrics = await StartModule.getDeviceDimensions()
            setDeviceMetrics(metrics)

            const { width, dpi } = metrics
            const isWidth1080 = width === 1080

            if (!isWidth1080) {
                setUnsupportedReason(`unsupported width: ${width} (suggested 1080)`)
            } else if (dpi !== 240 && dpi !== 450) {
                setUnsupportedReason(`unsupported DPI: ${dpi} (suggested 240 or 450)`)
            } else {
                setUnsupportedReason(null)
            }
        } catch (error) {
            logErrorWithTimestamp("[Home] Failed to fetch device dimensions:", error)
        }
    }

    /**
     * Grab the program name and version.
     */
    const getVersion = () => {
        const appName = Application.applicationName || "App"
        var version = Application.nativeApplicationVersion || "0.0.0"
        version += " (" + (Application.nativeBuildVersion || "0") + ")"
        logWithTimestamp(`Android app ${appName} version is ${version}`)
        bsc.setAppName(appName)
        bsc.setAppVersion(version)
    }

    /**
     * Handles the button press for starting or stopping the bot.
     */
    const handleButtonPress = async () => {
        if (isRunning) {
            StartModule.stop()
        } else if (bsc.readyStatus) {
            // Save settings before starting the bot.
            // Also has the added benefit of only writing to the SQLite database when the bot is started instead of every time the settings are changed.
            logWithTimestamp("[Home] Saving settings before starting bot...")
            try {
                await saveSettings()
                logWithTimestamp("[Home] Settings saved successfully, starting bot...")
            } catch (error) {
                logErrorWithTimestamp("[Home] Failed to save settings:", error)
                setSnackbarMessage(`Failed to save settings before starting: ${error}`)
                setSnackbarOpen(true)
            }
            StartModule.start()
        } else {
            setShowNotReadyDialog(true)
        }
    }

    /** Gets the appropriate icon name for the SelectButton based on device state. */
    const getSelectButtonIconName = (): string => {
        if (bsc.settings.general.scenario === "") {
            return ""
        } else if (isRunning) {
            return "stop-outline"
        } else {
            return "play-outline"
        }
    }

    /** Gets the SelectButton variant based on device state. */
    const getSelectButtonVariant = (): any => {
        if (unsupportedReason) {
            return "warning"
        } else if (isRunning) {
            return "error"
        } else if (deviceMetrics && bsc.settings.general.scenario !== "") {
            return "success"
        } else {
            return isDark ? "default" : "secondary"
        }
    }

    /** Returns a status indicator based on the device state. */
    const renderStatus = (): React.ReactElement | null => {
        const warningTextLines: string[] = [
            "Current Display: ${deviceMetrics?.width}x${deviceMetrics?.height} (${deviceMetrics?.dpi} DPI).",
            "",
            "Warning: Performance may be degraded due to ${unsupportedReason}.",
            "",
            "Supported Configurations:",
            "• 1080x1920 @ 240 DPI",
            "• 1080x2340 @ 450 DPI",
            "",
            "Note: Height is not as important to meet as the width. In addition, DPI is tied to the width and height together. How to calculate your specific DPI:",
            "",
            "DPI = sqrt(width^2 + height^2) / diagonal",
            "",
            "where width and height of the screen is in pixels, and diagonal is the diagonal size of the physical screen in inches.",
        ]
        const warningText: string = warningTextLines.join("\n")

        if (unsupportedReason) {
            return (
                <Tooltip delayDuration={150}>
                    <TooltipTrigger>
                        <Animated.View style={{ transform: [{ scale: pulseAnim }] }}>
                            <Ionicons name="alert-circle-outline" size={24} color={colors.warning} />
                        </Animated.View>
                    </TooltipTrigger>
                    <TooltipContent sideOffset={12} side="bottom" style={{ maxWidth: 350, backgroundColor: colors.warningBg, borderColor: colors.warningBorder, borderWidth: 1 }}>
                        <Text style={{ color: colors.warningText }}>{warningText}</Text>
                    </TooltipContent>
                </Tooltip>
            )
        } else if (deviceMetrics && !bsc.readyStatus && !isRunning) {
            return (
                <Tooltip delayDuration={150}>
                    <TooltipTrigger>
                        <Ionicons name="information-circle-outline" size={24} color={colors.info} />
                    </TooltipTrigger>
                    <TooltipContent sideOffset={12} side="bottom">
                        <Text>Select a Scenario in Settings to start</Text>
                    </TooltipContent>
                </Tooltip>
            )
        }

        return null
    }

    return (
        <View style={styles.root}>
            <PageHeader
                title=""
                showHomeButton={false}
                style={{ width: "100%" }}
                centerComponent={
                    <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
                        <SelectButton
                            variant={getSelectButtonVariant()}
                            iconName={getSelectButtonIconName()}
                            options={scenarios}
                            placeholder={deviceMetrics ? "Select a Scenario" : "Not Ready"}
                            value={bsc.settings.general.scenario}
                            onValueChange={(value) => {
                                const newScenario = value || ""
                                bsc.setSettings({ ...bsc.settings, general: { ...bsc.settings.general, scenario: newScenario } })
                                bsc.setReadyStatus(newScenario !== "")
                            }}
                            onPress={handleButtonPress}
                        />
                        {renderStatus()}
                    </View>
                }
            />

            <View style={styles.contentContainer}>
                <MessageLog />
            </View>

            <AlertDialog open={showNotReadyDialog} onOpenChange={setShowNotReadyDialog}>
                <AlertDialogContent onDismiss={() => setShowNotReadyDialog(false)}>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Not Ready</AlertDialogTitle>
                        <AlertDialogDescription>A scenario must be selected before starting the bot. Please go to Settings to select a scenario.</AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogAction onPress={() => setShowNotReadyDialog(false)}>
                            <Text>OK</Text>
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            <Snackbar
                visible={snackbarOpen}
                onDismiss={() => setSnackbarOpen(false)}
                action={{
                    label: "Close",
                    onPress: () => {
                        setSnackbarOpen(false)
                    },
                }}
                style={{ backgroundColor: "red", borderRadius: 10 }}
            >
                {snackbarMessage}
            </Snackbar>
        </View>
    )
}

export default Home
