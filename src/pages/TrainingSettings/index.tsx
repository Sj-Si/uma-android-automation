import React, { useMemo,  useContext, useEffect, useState } from "react"
import { View, Text, ScrollView, StyleSheet, Modal, TouchableOpacity, Dimensions } from "react-native"
import { Snackbar } from "react-native-paper"
import { useTheme } from "../../context/ThemeContext"
import { BotStateContext, defaultSettings, Settings } from "../../context/BotStateContext"
import CustomButton from "../../components/CustomButton"
import CustomSlider from "../../components/CustomSlider"
import CustomCheckbox from "../../components/CustomCheckbox"
import CustomTitle from "../../components/CustomTitle"
import DraggablePriorityList from "../../components/DraggablePriorityList"
import CustomAccordion from "../../components/CustomAccordion"
import CustomSelect from "../../components/CustomSelect"
import ProfileSelector from "../../components/ProfileSelector"
import { useSettings } from "../../context/SettingsContext"
import { useProfileManager } from "../../hooks/useProfileManager"
import { applyMigrations } from "../../hooks/useSettingsManager"
import { databaseManager } from "../../lib/database"
import PageHeader from "../../components/PageHeader"

const TrainingSettings = () => {
    const { colors } = useTheme()
    const bsc = useContext(BotStateContext)
    const { saveSettingsImmediate } = useSettings()
    const { currentProfileName } = useProfileManager()
    const [blacklistModalVisible, setBlacklistModalVisible] = useState(false)
    const [prioritizationModalVisible, setPrioritizationModalVisible] = useState(false)
    const [sparkStatTargetModalVisible, setSparkStatTargetModalVisible] = useState(false)
    const [snackbarVisible, setSnackbarVisible] = useState(false)
    const [snackbarMessage, setSnackbarMessage] = useState("")

    const { settings, setSettings } = bsc

    // Initialize local state from settings, with fallback to defaults.
    const [statPrioritizationItems, setStatPrioritizationItems] = useState<string[]>(() =>
        settings.training?.statPrioritization !== undefined ? settings.training.statPrioritization : defaultSettings.training.statPrioritization,
    )
    const [blacklistItems, setBlacklistItems] = useState<string[]>(() =>
        settings.training?.trainingBlacklist !== undefined ? settings.training.trainingBlacklist : defaultSettings.training.trainingBlacklist,
    )
    const [sparkStatTargetItems, setSparkStatTargetItems] = useState<string[]>(() => {
        const value = settings.training?.focusOnSparkStatTarget
        // Ensure we always have an array (migration should handle this, but be safe).
        if (Array.isArray(value)) {
            return value
        }
        return defaultSettings.training.focusOnSparkStatTarget
    })

    // Merge current training settings with defaults to handle missing properties.
    // Include local state values to ensure blacklist and prioritization are current.
    const trainingSettings = {
        ...defaultSettings.training,
        ...settings.training,
        trainingBlacklist: blacklistItems,
        statPrioritization: statPrioritizationItems,
        focusOnSparkStatTarget: sparkStatTargetItems,
    }
    const trainingStatTargetSettings = { ...defaultSettings.trainingStatTarget, ...settings.trainingStatTarget }
    const {
        maximumFailureChance,
        disableTrainingOnMaxedStat,
        manualStatCap,
        enableRainbowTrainingBonus,
        preferredDistanceOverride,
        mustRestBeforeSummer,
        enableRiskyTraining,
        riskyTrainingMinStatGain,
        riskyTrainingMaxFailureChance,
        trainWitDuringFinale,
        enablePrioritizeSkillHints,
    } = trainingSettings

    useEffect(() => {
        updateTrainingSetting("statPrioritization", statPrioritizationItems)
    }, [statPrioritizationItems])

    useEffect(() => {
        updateTrainingSetting("trainingBlacklist", blacklistItems)
    }, [blacklistItems])

    useEffect(() => {
        updateTrainingSetting("focusOnSparkStatTarget", sparkStatTargetItems)
    }, [sparkStatTargetItems])

    // Sync local state when settings change (e.g., when switching profiles).
    useEffect(() => {
        if (settings.training?.trainingBlacklist !== undefined) {
            setBlacklistItems(settings.training.trainingBlacklist)
        }
    }, [settings.training?.trainingBlacklist])

    useEffect(() => {
        if (settings.training?.statPrioritization !== undefined) {
            setStatPrioritizationItems(settings.training.statPrioritization)
        }
    }, [settings.training?.statPrioritization])

    useEffect(() => {
        const value = settings.training?.focusOnSparkStatTarget
        if (value !== undefined && Array.isArray(value)) {
            setSparkStatTargetItems(value)
        }
    }, [settings.training?.focusOnSparkStatTarget])

    // Sync currentProfileName from profile manager to settings context.
    // Also check database directly to ensure we have the latest value.
    useEffect(() => {
        const syncProfileName = async () => {
            const profileName = currentProfileName || ""
            // Also check database to ensure we have the latest value.
            try {
                const dbProfileName = await databaseManager.getCurrentProfileName()
                const finalProfileName = dbProfileName || ""
                if (settings.misc.currentProfileName !== finalProfileName) {
                    setSettings({
                        ...bsc.settings,
                        misc: {
                            ...bsc.settings.misc,
                            currentProfileName: finalProfileName,
                        },
                    })
                }
            } catch (error) {
                // Fallback to hook value if database read fails.
                if (settings.misc.currentProfileName !== profileName) {
                    setSettings({
                        ...bsc.settings,
                        misc: {
                            ...bsc.settings.misc,
                            currentProfileName: profileName,
                        },
                    })
                }
            }
        }
        syncProfileName()
    }, [currentProfileName])

    const updateTrainingSetting = (key: keyof typeof settings.training, value: any) => {
        setSettings({
            ...bsc.settings,
            training: {
                ...bsc.settings.training,
                [key]: value,
            },
        })
    }

    const handleOverwriteSettings = async (profileSettings: Partial<Settings>) => {
        // Get the current profile name directly from the database to ensure we have the latest value.
        const dbProfileName = await databaseManager.getCurrentProfileName()

        // Merge profile settings with current settings to create a complete Settings object for migration.
        const mergedSettings = {
            ...bsc.settings,
            ...profileSettings,
        } as Settings

        // Apply migrations to the merged settings (this handles focusOnSparkStatTarget and any future migrations).
        const { settings: migratedSettings } = applyMigrations(mergedSettings)

        // Create the updated settings object with the migrated profile settings.
        const updatedSettings = {
            ...migratedSettings,
            misc: {
                ...bsc.settings.misc,
                ...migratedSettings.misc,
                currentProfileName: dbProfileName || "",
            },
        }
        // Apply the profile's settings to current settings.
        setSettings(updatedSettings)
        // Save settings immediately with the updated settings.
        await saveSettingsImmediate(updatedSettings)
    }

    const updateTrainingStatTarget = (key: keyof typeof settings.trainingStatTarget, value: any) => {
        setSettings({
            ...bsc.settings,
            trainingStatTarget: {
                ...bsc.settings.trainingStatTarget,
                [key]: value,
            },
        })
    }

    const styles = useMemo(() => StyleSheet.create({
        root: {
            flex: 1,
            flexDirection: "column",
            justifyContent: "center",
            margin: 10,
            backgroundColor: colors.background,
        },
        section: {
            marginBottom: 24,
        },
        row: {
            flexDirection: "row",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 16,
        },
        label: {
            fontSize: 16,
            color: colors.foreground,
            flex: 1,
        },
        pressableText: {
            fontSize: 16,
            color: colors.primary,
            textDecorationLine: "underline",
        },
        modal: {
            flex: 1,
            justifyContent: "center",
            alignItems: "center",
            backgroundColor: "rgba(70, 70, 70, 0.5)",
        },
        modalContent: {
            backgroundColor: colors.background,
            borderRadius: 12,
            padding: 20,
            width: Dimensions.get("window").width * 0.85,
            maxHeight: Dimensions.get("window").height * 0.7,
        },
        modalHeader: {
            flexDirection: "row",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 20,
        },
        modalTitle: {
            fontSize: 20,
            fontWeight: "bold",
            color: colors.foreground,
        },
        closeButton: {
            padding: 8,
        },
        closeText: {
            fontSize: 18,
            color: colors.primary,
        },
        buttonRow: {
            flexDirection: "row",
            justifyContent: "space-between",
            marginTop: 20,
        },
    }), [colors])

    const toggleStat = (stat: string, list: string[], setList: (value: string[]) => void) => {
        if (list.includes(stat)) {
            setList(list.filter((s) => s !== stat))
        } else {
            setList([...list, stat])
        }
    }

    const clearAll = (setList: (value: string[]) => void) => {
        setList([])
    }

    const selectAll = (setList: (value: string[]) => void, currentList: string[]) => {
        // Add any missing items from default settings to the current list, preserving order.
        const missingItems = defaultSettings.training.statPrioritization.filter((stat) => !currentList.includes(stat))
        setList([...currentList, ...missingItems])
    }

    const renderStatSelector = (
        title: string,
        selectedStats: string[],
        setSelectedStats: (value: string[]) => void,
        modalVisible: boolean,
        setModalVisible: React.Dispatch<React.SetStateAction<boolean>>,
        description?: string,
        mode: "checkbox" | "priority" = "checkbox",
    ) => (
        <View style={styles.section}>
            <View style={styles.row}>
                <Text style={styles.label}>{title}</Text>
                <TouchableOpacity onPress={() => setModalVisible(true)}>
                    <Text style={styles.pressableText}>{selectedStats.length === 0 ? "None" : selectedStats.join(", ")}</Text>
                </TouchableOpacity>
            </View>
            {description && <Text style={[styles.label, { fontSize: 14, color: colors.foreground, opacity: 0.7, marginTop: 4 }]}>{description}</Text>}

            <Modal visible={modalVisible} transparent={true} animationType="fade" onRequestClose={() => setModalVisible(false)}>
                <TouchableOpacity style={styles.modal} activeOpacity={1} onPress={() => setModalVisible(false)}>
                    <TouchableOpacity style={styles.modalContent} activeOpacity={1} onPress={(e) => e.stopPropagation()}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>{title}</Text>
                            <TouchableOpacity style={styles.closeButton} onPress={() => setModalVisible(false)}>
                                <Text style={styles.closeText}>✕</Text>
                            </TouchableOpacity>
                        </View>

                        {mode === "priority" ? (
                            <DraggablePriorityList
                                items={defaultSettings.training.statPrioritization.map((stat) => ({
                                    id: stat,
                                    label: stat,
                                }))}
                                selectedItems={selectedStats}
                                onSelectionChange={setSelectedStats}
                                onOrderChange={(orderedItems) => {
                                    // Update the order when items are reordered.
                                    setSelectedStats(orderedItems)
                                }}
                            />
                        ) : (
                            defaultSettings.training.statPrioritization.map((stat) => (
                                <CustomCheckbox
                                    key={stat}
                                    id={`stat-${stat.toLowerCase()}`}
                                    checked={selectedStats.includes(stat)}
                                    onCheckedChange={() => toggleStat(stat, selectedStats, setSelectedStats)}
                                    label={stat}
                                    className="my-2"
                                />
                            ))
                        )}

                        <View style={styles.buttonRow}>
                            <CustomButton
                                onPress={() => {
                                    if (mode === "priority") {
                                        // For prioritization, reset to default and dismiss modal.
                                        setSelectedStats(defaultSettings.training.statPrioritization)
                                        setModalVisible(false)
                                    } else {
                                        // For blacklist, just clear the list.
                                        clearAll(setSelectedStats)
                                    }
                                }}
                                variant="destructive"
                            >
                                Clear All
                            </CustomButton>
                            <CustomButton onPress={() => selectAll(setSelectedStats, selectedStats)} variant="outline">
                                Select All
                            </CustomButton>
                        </View>
                    </TouchableOpacity>
                </TouchableOpacity>
            </Modal>
        </View>
    )

    return (
        <View style={styles.root}>
            <PageHeader title="Training Settings" />

            <ScrollView nestedScrollEnabled={true} showsVerticalScrollIndicator={false} showsHorizontalScrollIndicator={false} contentContainerStyle={{ flexGrow: 1 }}>
                <View className="m-1">
                    <ProfileSelector
                        currentTrainingSettings={trainingSettings}
                        currentTrainingStatTargetSettings={trainingStatTargetSettings}
                        onOverwriteSettings={handleOverwriteSettings}
                        onNoChangesDetected={() => {
                            setSnackbarMessage("Current Training settings are already the same.")
                            setSnackbarVisible(true)
                        }}
                        onError={(message) => {
                            setSnackbarMessage(message)
                            setSnackbarVisible(true)
                        }}
                    />
                    {renderStatSelector(
                        "Blacklist",
                        blacklistItems,
                        (value) => setBlacklistItems(value),
                        blacklistModalVisible,
                        setBlacklistModalVisible,
                        "Select which stats to exclude from training. These stats will be skipped during training sessions.",
                        "checkbox",
                    )}

                    {renderStatSelector(
                        "Prioritization",
                        statPrioritizationItems,
                        (value) => setStatPrioritizationItems(value),
                        prioritizationModalVisible,
                        setPrioritizationModalVisible,
                        "Select the priority order of the stats. The stats will be trained in the order they are selected. If none are selected, then the default order will be used.",
                        "priority",
                    )}

                    <View style={styles.section}>
                        <CustomCheckbox
                            id="disable-training-on-maxed-stats"
                            checked={disableTrainingOnMaxedStat}
                            onCheckedChange={(checked) => updateTrainingSetting("disableTrainingOnMaxedStat", checked)}
                            label="Disable Training on Maxed Stats"
                            description="When enabled, training will be skipped for stats that have reached their maximum value."
                            className="my-2"
                        />
                        {disableTrainingOnMaxedStat && (
                            <CustomSlider
                                value={manualStatCap || defaultSettings.training.manualStatCap}
                                placeholder={defaultSettings.training.manualStatCap}
                                onValueChange={(value) => updateTrainingSetting("manualStatCap", value)}
                                min={1000}
                                max={2000}
                                step={10}
                                label="Manual Stat Cap"
                                labelUnit=""
                                showValue={true}
                                showLabels={true}
                                description="Set a custom stat cap for all stats. Training will be skipped when any stat reaches this value (if 'Disable Training on Maxed Stats' is enabled)."
                            />
                        )}
                    </View>

                    <View style={styles.section}>
                        <CustomSlider
                            value={maximumFailureChance}
                            placeholder={defaultSettings.training.maximumFailureChance}
                            onValueChange={(value) => updateTrainingSetting("maximumFailureChance", value)}
                            min={5}
                            max={95}
                            step={5}
                            label="Set Maximum Failure Chance"
                            labelUnit="%"
                            showValue={true}
                            showLabels={true}
                            description="Set the maximum acceptable failure chance for training sessions. Training with higher failure rates will be avoided."
                        />
                    </View>

                    <View style={styles.section}>
                        <CustomCheckbox
                            id="enable-riskier-training"
                            checked={enableRiskyTraining}
                            onCheckedChange={(checked) => updateTrainingSetting("enableRiskyTraining", checked)}
                            label="Enable Riskier Training"
                            description="When enabled, trainings with high main stat gains will use a separate, higher maximum failure chance threshold."
                            className="my-2"
                        />
                        {enableRiskyTraining && (
                            <>
                                <CustomSlider
                                    value={riskyTrainingMinStatGain || defaultSettings.training.riskyTrainingMinStatGain}
                                    placeholder={defaultSettings.training.riskyTrainingMinStatGain}
                                    onValueChange={(value) => updateTrainingSetting("riskyTrainingMinStatGain", value)}
                                    min={20}
                                    max={100}
                                    step={5}
                                    label="Minimum Main Stat Gain Threshold"
                                    labelUnit=""
                                    showValue={true}
                                    showLabels={true}
                                    description="When a training's main stat gain meets or exceeds this value, it will be considered for risky training."
                                />
                                <CustomSlider
                                    value={riskyTrainingMaxFailureChance || defaultSettings.training.riskyTrainingMaxFailureChance}
                                    placeholder={defaultSettings.training.riskyTrainingMaxFailureChance}
                                    onValueChange={(value) => updateTrainingSetting("riskyTrainingMaxFailureChance", value)}
                                    min={5}
                                    max={95}
                                    step={5}
                                    label="Risky Training Maximum Failure Chance"
                                    labelUnit="%"
                                    showValue={true}
                                    showLabels={true}
                                    description="Set the maximum acceptable failure chance for risky training sessions with high main stat gains."
                                />
                            </>
                        )}
                    </View>

                    {renderStatSelector(
                        "Focus on Sparks",
                        sparkStatTargetItems,
                        (value) => setSparkStatTargetItems(value),
                        sparkStatTargetModalVisible,
                        setSparkStatTargetModalVisible,
                        "Select which stats should receive priority to get to at least 600 to get the best chance to receive 3* sparks.",
                        "checkbox",
                    )}

                    <View style={styles.section}>
                        <CustomCheckbox
                            id="enable-prioritize-skill-hints"
                            checked={enablePrioritizeSkillHints}
                            onCheckedChange={(checked) => updateTrainingSetting("enablePrioritizeSkillHints", checked)}
                            label="Prioritize Skill Hints"
                            description="When enabled, the bot will prioritize acquiring skill hints, bypassing stat prioritization and blacklist, while still being constrained by the failure chance thresholds."
                            className="my-2"
                        />
                    </View>

                    <View style={styles.section}>
                        <CustomCheckbox
                            id="must-rest-before-summer"
                            checked={mustRestBeforeSummer}
                            onCheckedChange={(checked) => updateTrainingSetting("mustRestBeforeSummer", checked)}
                            label="Must Rest before Summer"
                            description="Forces the bot to rest during June Late Phase in Classic and Senior Years to ensure enough energy for Summer Training in July."
                            className="my-2"
                        />
                    </View>

                    <View style={styles.section}>
                        <CustomCheckbox
                            id="train-wit-during-finale"
                            checked={trainWitDuringFinale}
                            onCheckedChange={(checked) => updateTrainingSetting("trainWitDuringFinale", checked)}
                            label="Train Wit During Finale"
                            description="When enabled, the bot will train Wit during URA finale turns (73, 74, 75) instead of recovering energy or mood, even if the failure chance is high."
                            className="my-2"
                        />
                    </View>

                    <View style={styles.section}>
                        <CustomCheckbox
                            id="enable-rainbow-training-bonus"
                            checked={enableRainbowTrainingBonus}
                            onCheckedChange={(checked) => updateTrainingSetting("enableRainbowTrainingBonus", checked)}
                            label="Enable Rainbow Training Bonus"
                            description="When enabled (Year 2+), rainbow trainings receive a significant bonus to their score, making them more likely to be selected. This is highly dependent on device configuration and may result in false positives."
                            className="my-2"
                        />
                    </View>

                    <View style={styles.section}>
                        <View style={styles.row}>
                            <Text style={styles.label}>Preferred Distance Override</Text>
                            <CustomSelect
                                value={preferredDistanceOverride}
                                onValueChange={(value) => updateTrainingSetting("preferredDistanceOverride", value)}
                                options={[
                                    { label: "Auto", value: "Auto" },
                                    { label: "Sprint", value: "Sprint" },
                                    { label: "Mile", value: "Mile" },
                                    { label: "Medium", value: "Medium" },
                                    { label: "Long", value: "Long" },
                                ]}
                                placeholder="Select distance"
                                width={200}
                            />
                        </View>
                        <Text style={[styles.label, { fontSize: 14, color: colors.foreground, opacity: 0.7, marginTop: 4 }]}>
                            Set the preferred race distance for training targets. "Auto" will automatically determine based on character aptitudes reading from left to right (S {">"} A priority).
                            {"\n\n"}
                            For example, if Gold Ship has an aptitude of A for both Medium and Long, Auto will use Medium as the preferred distance. Whereas if Medium is A and Long is S, then Auto
                            will instead use Long as the preferred distance.
                        </Text>
                    </View>

                    {/* Stat Target Settings */}
                    <View style={styles.section}>
                        <CustomTitle
                            title="Stat Targets by Distance"
                            description="Set target values for each stat based on race distance. The bot will prioritize training stats that are below these targets."
                        />
                    </View>

                    {/* Distance Stat Targets Accordion */}
                    <CustomAccordion
                        type="single"
                        sections={[
                            {
                                value: "sprint",
                                title: "Sprint Distance",
                                children: (
                                    <>
                                        <CustomSlider
                                            value={trainingStatTargetSettings.trainingSprintStatTarget_speedStatTarget}
                                            placeholder={defaultSettings.trainingStatTarget.trainingSprintStatTarget_speedStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingSprintStatTarget_speedStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Sprint Speed Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingSprintStatTarget_staminaStatTarget}
                                            value={trainingStatTargetSettings.trainingSprintStatTarget_staminaStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingSprintStatTarget_staminaStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Sprint Stamina Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingSprintStatTarget_powerStatTarget}
                                            value={trainingStatTargetSettings.trainingSprintStatTarget_powerStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingSprintStatTarget_powerStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Sprint Power Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingSprintStatTarget_gutsStatTarget}
                                            value={trainingStatTargetSettings.trainingSprintStatTarget_gutsStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingSprintStatTarget_gutsStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Sprint Guts Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingSprintStatTarget_witStatTarget}
                                            value={trainingStatTargetSettings.trainingSprintStatTarget_witStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingSprintStatTarget_witStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Sprint Wit Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                    </>
                                ),
                            },
                            {
                                value: "mile",
                                title: "Mile Distance",
                                children: (
                                    <>
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingMileStatTarget_speedStatTarget}
                                            value={trainingStatTargetSettings.trainingMileStatTarget_speedStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingMileStatTarget_speedStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Mile Speed Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingMileStatTarget_staminaStatTarget}
                                            value={trainingStatTargetSettings.trainingMileStatTarget_staminaStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingMileStatTarget_staminaStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Mile Stamina Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingMileStatTarget_powerStatTarget}
                                            value={trainingStatTargetSettings.trainingMileStatTarget_powerStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingMileStatTarget_powerStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Mile Power Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingMileStatTarget_gutsStatTarget}
                                            value={trainingStatTargetSettings.trainingMileStatTarget_gutsStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingMileStatTarget_gutsStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Mile Guts Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingMileStatTarget_witStatTarget}
                                            value={trainingStatTargetSettings.trainingMileStatTarget_witStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingMileStatTarget_witStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Mile Wit Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                    </>
                                ),
                            },
                            {
                                value: "medium",
                                title: "Medium Distance",
                                children: (
                                    <>
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingMediumStatTarget_speedStatTarget}
                                            value={trainingStatTargetSettings.trainingMediumStatTarget_speedStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingMediumStatTarget_speedStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Medium Speed Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingMediumStatTarget_staminaStatTarget}
                                            value={trainingStatTargetSettings.trainingMediumStatTarget_staminaStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingMediumStatTarget_staminaStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Medium Stamina Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingMediumStatTarget_powerStatTarget}
                                            value={trainingStatTargetSettings.trainingMediumStatTarget_powerStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingMediumStatTarget_powerStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Medium Power Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingMediumStatTarget_gutsStatTarget}
                                            value={trainingStatTargetSettings.trainingMediumStatTarget_gutsStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingMediumStatTarget_gutsStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Medium Guts Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingMediumStatTarget_witStatTarget}
                                            value={trainingStatTargetSettings.trainingMediumStatTarget_witStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingMediumStatTarget_witStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Medium Wit Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                    </>
                                ),
                            },
                            {
                                value: "long",
                                title: "Long Distance",
                                children: (
                                    <>
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingLongStatTarget_speedStatTarget}
                                            value={trainingStatTargetSettings.trainingLongStatTarget_speedStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingLongStatTarget_speedStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Long Speed Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingLongStatTarget_staminaStatTarget}
                                            value={trainingStatTargetSettings.trainingLongStatTarget_staminaStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingLongStatTarget_staminaStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Long Stamina Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingLongStatTarget_powerStatTarget}
                                            value={trainingStatTargetSettings.trainingLongStatTarget_powerStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingLongStatTarget_powerStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Long Power Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingLongStatTarget_gutsStatTarget}
                                            value={trainingStatTargetSettings.trainingLongStatTarget_gutsStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingLongStatTarget_gutsStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Long Guts Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                        <CustomSlider
                                            placeholder={defaultSettings.trainingStatTarget.trainingLongStatTarget_witStatTarget}
                                            value={trainingStatTargetSettings.trainingLongStatTarget_witStatTarget}
                                            onValueChange={(value) => updateTrainingStatTarget("trainingLongStatTarget_witStatTarget", value)}
                                            min={100}
                                            max={1200}
                                            step={10}
                                            label="Long Wit Target"
                                            labelUnit=""
                                            showValue={true}
                                            showLabels={true}
                                        />
                                    </>
                                ),
                            },
                        ]}
                    />
                </View>
            </ScrollView>
            <Snackbar
                visible={snackbarVisible}
                onDismiss={() => setSnackbarVisible(false)}
                action={{
                    label: "Close",
                    onPress: () => {
                        setSnackbarVisible(false)
                    },
                }}
                style={{ backgroundColor: "red", borderRadius: 10 }}
                duration={4000}
            >
                {snackbarMessage}
            </Snackbar>
        </View>
    )
}

export default TrainingSettings
