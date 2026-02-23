import React, { useState, useMemo, useCallback } from "react"
import { View, Text, StyleSheet, Modal, TouchableOpacity, ScrollView } from "react-native"
import { useTheme } from "../../context/ThemeContext"
import CustomButton from "../CustomButton"
import { Input } from "../ui/input"
import { X } from "lucide-react-native"
import { useProfileManager } from "../../hooks/useProfileManager"
import { Settings } from "../../context/BotStateContext"

const TABLE_HEADERS = ["", "SPD", "STA", "POW", "GUTS", "WIT"]
const DISTANCE_TYPES = ["Sprint", "Mile", "Med", "Long"]

interface ProfileCreationModalProps {
    visible: boolean
    onClose: () => void
    currentTrainingSettings: Settings["training"]
    currentTrainingStatTargetSettings: Settings["trainingStatTarget"]
    onProfileCreated?: (profileName: string) => void
    onError?: (message: string) => void
}

const ProfileCreationModal: React.FC<ProfileCreationModalProps> = ({ visible, onClose, currentTrainingSettings, currentTrainingStatTargetSettings, onProfileCreated, onError }) => {
    const { colors } = useTheme()
    const { createProfile } = useProfileManager(onError)
    const [profileName, setProfileName] = useState("")
    const [isCreating, setIsCreating] = useState(false)

    const styles = useMemo(() => StyleSheet.create({
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
            width: "90%",
            maxHeight: "80%",
        },
        header: {
            flexDirection: "row",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 20,
        },
        title: {
            fontSize: 20,
            fontWeight: "bold",
            color: colors.foreground,
        },
        closeButton: {
            padding: 4,
        },
        input: {
            marginBottom: 16,
        },
        settingsPreview: {
            marginTop: 16,
            marginBottom: 16,
            padding: 12,
            backgroundColor: colors.secondary,
            borderRadius: 8,
            height: 200,
        },
        previewTitle: {
            fontSize: 14,
            fontWeight: "600",
            color: colors.foreground,
            marginBottom: 8,
        },
        previewText: {
            fontSize: 12,
            color: colors.foreground,
            opacity: 0.7,
        },
        tableContainer: {
            marginTop: 12,
        },
        tableTitle: {
            fontSize: 12,
            fontWeight: "600",
            color: colors.foreground,
            marginBottom: 8,
        },
        table: {
            borderWidth: 1,
            borderColor: colors.foreground + "40",
            borderRadius: 4,
            overflow: "hidden",
            backgroundColor: colors.secondary,
        },
        tableRow: {
            flexDirection: "row",
            borderBottomWidth: 1,
            borderBottomColor: colors.foreground + "30",
        },
        tableCell: {
            flex: 1,
            padding: 8,
            borderRightWidth: 1,
            borderRightColor: colors.foreground + "30",
            alignItems: "center",
            justifyContent: "center",
            backgroundColor: colors.secondary,
        },
        tableHeaderText: {
            fontSize: 9,
            fontWeight: "600",
            color: colors.foreground,
        },
        tableCellText: {
            fontSize: 9,
            color: colors.foreground,
            opacity: 0.8,
        },
        buttonRow: {
            flexDirection: "row",
            justifyContent: "space-between",
            gap: 8,
            marginTop: 16,
        },
    }), [colors])

    // Format the training settings into a preview string.
    const settingsPreview = useMemo(() => {
        const settings: string[] = []
        settings.push(`Blacklist: ${currentTrainingSettings.trainingBlacklist.length > 0 ? currentTrainingSettings.trainingBlacklist.join(", ") : "None"}`)
        settings.push(`Prioritization: ${currentTrainingSettings.statPrioritization.length > 0 ? currentTrainingSettings.statPrioritization.join(", ") : "None"}`)
        settings.push(`Max Failure Chance: ${currentTrainingSettings.maximumFailureChance}%`)
        settings.push(`Disable on Maxed: ${currentTrainingSettings.disableTrainingOnMaxedStat ? "Yes" : "No"}`)
        settings.push(`Manual Stat Cap: ${currentTrainingSettings.manualStatCap}`)
        settings.push(`Focus on Sparks: ${currentTrainingSettings.focusOnSparkStatTarget ? "Yes" : "No"}`)
        settings.push(`Rainbow Bonus: ${currentTrainingSettings.enableRainbowTrainingBonus ? "Yes" : "No"}`)
        settings.push(`Preferred Distance: ${currentTrainingSettings.preferredDistanceOverride}`)
        settings.push(`Must Rest Before Summer: ${currentTrainingSettings.mustRestBeforeSummer ? "Yes" : "No"}`)
        settings.push(`Train Wit During Finale: ${currentTrainingSettings.trainWitDuringFinale ? "Yes" : "No"}`)
        settings.push(`Risky Training: ${currentTrainingSettings.enableRiskyTraining ? "Yes" : "No"}`)
        if (currentTrainingSettings.enableRiskyTraining) {
            settings.push(`  Min Stat Gain: ${currentTrainingSettings.riskyTrainingMinStatGain}`)
            settings.push(`  Max Failure: ${currentTrainingSettings.riskyTrainingMaxFailureChance}%`)
        }
        return settings.join("\n")
    }, [currentTrainingSettings])

    // Get stat target values for a given distance type.
    const getStatTargets = useCallback(
        (distance: (typeof DISTANCE_TYPES)[number]) => {
            const distanceMap: Record<(typeof DISTANCE_TYPES)[number], string> = {
                Sprint: "Sprint",
                Mile: "Mile",
                Med: "Medium",
                Long: "Long",
            }
            const prefix = `training${distanceMap[distance]}StatTarget`
            const settings = currentTrainingStatTargetSettings
            return {
                speed: settings[`${prefix}_speedStatTarget` as keyof typeof settings] as number,
                stamina: settings[`${prefix}_staminaStatTarget` as keyof typeof settings] as number,
                power: settings[`${prefix}_powerStatTarget` as keyof typeof settings] as number,
                guts: settings[`${prefix}_gutsStatTarget` as keyof typeof settings] as number,
                wit: settings[`${prefix}_witStatTarget` as keyof typeof settings] as number,
            }
        },
        [currentTrainingStatTargetSettings]
    )

    // Build table data for stat targets by distance.
    const tableData = useMemo(() => {
        return DISTANCE_TYPES.map((distance) => ({
            distance,
            ...getStatTargets(distance),
        }))
    }, [getStatTargets])

    const handleCreate = useCallback(async () => {
        if (!profileName.trim()) {
            return
        }

        try {
            setIsCreating(true)
            const createdProfileName = profileName.trim()
            await createProfile(createdProfileName, {
                training: currentTrainingSettings,
                trainingStatTarget: currentTrainingStatTargetSettings,
            })
            setProfileName("")
            onProfileCreated?.(createdProfileName)
            onClose()
        } catch (error) {
            const errorMessage = `Failed to create profile: ${error instanceof Error ? error.message : String(error)}`
            onError?.(errorMessage)
        } finally {
            setIsCreating(false)
        }
    }, [profileName, createProfile, currentTrainingSettings, currentTrainingStatTargetSettings, onProfileCreated, onClose, onError])

    const handleClose = useCallback(() => {
        setProfileName("")
        onClose()
    }, [onClose])

    return (
        <Modal visible={visible} transparent={true} animationType="fade" onRequestClose={handleClose}>
            <View style={styles.modal}>
                <View style={styles.modalContent}>
                    <View style={styles.header}>
                        <Text style={styles.title}>Create New Profile</Text>
                        <TouchableOpacity style={styles.closeButton} onPress={handleClose}>
                            <X size={24} color={colors.foreground} />
                        </TouchableOpacity>
                    </View>

                    <View style={styles.input}>
                        <Input placeholder="Profile name" value={profileName} onChangeText={setProfileName} style={{ color: colors.foreground, backgroundColor: colors.secondary }} />
                    </View>

                    <View style={styles.settingsPreview}>
                        <Text style={styles.previewTitle}>Current Training Settings (will be saved):</Text>
                        <ScrollView nestedScrollEnabled={true}>
                            <Text style={styles.previewText}>{settingsPreview}</Text>
                            <View style={styles.tableContainer}>
                                <Text style={styles.tableTitle}>Stat Targets by Distance:</Text>
                                <View style={styles.table}>
                                    {/* Header Row */}
                                    <View style={styles.tableRow}>
                                        {TABLE_HEADERS.map((header, index) => (
                                            <View key={index} style={[styles.tableCell, { borderRightWidth: index < TABLE_HEADERS.length - 1 ? 1 : 0 }]}>
                                                <Text style={styles.tableHeaderText}>{header}</Text>
                                            </View>
                                        ))}
                                    </View>
                                    {/* Data Rows */}
                                    {tableData.map((row, rowIndex) => (
                                        <View key={rowIndex} style={styles.tableRow}>
                                            <View style={[styles.tableCell, { borderRightWidth: 1 }]}>
                                                <Text style={styles.tableCellText}>{row.distance}</Text>
                                            </View>
                                            <View style={[styles.tableCell, { borderRightWidth: 1 }]}>
                                                <Text style={styles.tableCellText}>{row.speed}</Text>
                                            </View>
                                            <View style={[styles.tableCell, { borderRightWidth: 1 }]}>
                                                <Text style={styles.tableCellText}>{row.stamina}</Text>
                                            </View>
                                            <View style={[styles.tableCell, { borderRightWidth: 1 }]}>
                                                <Text style={styles.tableCellText}>{row.power}</Text>
                                            </View>
                                            <View style={[styles.tableCell, { borderRightWidth: 1 }]}>
                                                <Text style={styles.tableCellText}>{row.guts}</Text>
                                            </View>
                                            <View style={[styles.tableCell, { borderRightWidth: 0 }]}>
                                                <Text style={styles.tableCellText}>{row.wit}</Text>
                                            </View>
                                        </View>
                                    ))}
                                </View>
                            </View>
                        </ScrollView>
                    </View>

                    <View style={styles.buttonRow}>
                        <CustomButton onPress={handleClose} variant="outline" disabled={isCreating}>
                            Cancel
                        </CustomButton>
                        <CustomButton onPress={handleCreate} variant={isCreating || !profileName.trim() ? "destructive" : "default"} disabled={isCreating || !profileName.trim()}>
                            Create Profile
                        </CustomButton>
                    </View>
                </View>
            </View>
        </Modal>
    )
}

export default ProfileCreationModal
