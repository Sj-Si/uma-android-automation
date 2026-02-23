import React, { useMemo } from "react"
import { View, Text, StyleSheet } from "react-native"
import { useTheme } from "../../context/ThemeContext"
import CustomButton from "../CustomButton"
import WarningContainer from "../WarningContainer"
import { SettingsCategory } from "../../hooks/useProfileManager"

interface ProfileComparisonProps {
    comparison: Record<string, { current: any; profile: any }>
    onConfirm: () => void
    onCancel: () => void
    actionType?: "switch" | "overwrite"
    category?: SettingsCategory
}

const CATEGORY_NAMES: Partial<Record<SettingsCategory, string>> = {
    training: "Training Settings:",
}

const formatValue = (value: any): string => {
    if (Array.isArray(value)) {
        return value.join(", ") || "[]"
    }
    if (typeof value === "object" && value !== null) {
        return JSON.stringify(value)
    }
    return String(value)
}

const ProfileComparison: React.FC<ProfileComparisonProps> = ({ comparison, onConfirm, onCancel, actionType = "switch", category = "training" }) => {
    const { colors } = useTheme()

    const hasChanges = Object.keys(comparison).length > 0

    const title = useMemo(() => (actionType === "overwrite" ? "Overwriting current settings will change:" : "Switching profile will change:"), [actionType])

    const sectionTitle = useMemo(() => CATEGORY_NAMES[category] || "Settings:", [category])

    const buttonLabel = useMemo(() => (actionType === "overwrite" ? "Overwrite Settings" : "Apply Profile"), [actionType])

    /**
     * NOTE: This component no longer contains its own ScrollView.
     * It is designed to be a pure presentation component rendered within
     * the unified ScrollView of the ProfileManagerModal. This simplifies
     * the layout and prevents nested scroll conflicts.
     */
    const styles = useMemo(() => StyleSheet.create({
        container: {
            marginTop: 16,
            padding: 16,
        },
        title: {
            fontSize: 16,
            fontWeight: "bold",
            color: colors.foreground,
            marginBottom: 12,
        },
        section: {
            marginBottom: 16,
        },
        sectionTitle: {
            fontSize: 14,
            fontWeight: "600",
            color: colors.foreground,
            marginBottom: 8,
        },
        changeItem: {
            marginBottom: 8,
            padding: 8,
            backgroundColor: colors.background,
            borderRadius: 4,
        },
        changeKey: {
            fontSize: 12,
            fontWeight: "600",
            color: colors.foreground,
            marginBottom: 4,
        },
        changeValue: {
            fontSize: 11,
            color: colors.foreground,
            marginLeft: 8,
        },
        changeRow: {
            flexDirection: "row",
            marginBottom: 2,
        },
        buttonRow: {
            flexDirection: "row",
            gap: 8,
            marginTop: 12,
        },
    }), [colors])

    if (!hasChanges) {
        return null
    }

    return (
        <WarningContainer style={styles.container}>
            <Text style={styles.title}>{title}</Text>

            <View style={styles.section}>
                <Text style={styles.sectionTitle}>{sectionTitle}</Text>
                {Object.entries(comparison).map(([key, { current, profile }]) => (
                    <View key={key} style={styles.changeItem}>
                        <Text style={styles.changeKey}>{key}:</Text>
                        <View style={styles.changeRow}>
                            <Text style={[styles.changeValue, { color: colors.destructive }]}>Current: {formatValue(current)}</Text>
                        </View>
                        <View style={styles.changeRow}>
                            <Text style={[styles.changeValue, { color: colors.primary }]}>→ Profile: {formatValue(profile)}</Text>
                        </View>
                    </View>
                ))}
            </View>

            <View style={styles.buttonRow}>
                <CustomButton onPress={onCancel} variant="outline">
                    Cancel
                </CustomButton>
                <CustomButton onPress={onConfirm} variant="default">
                    {buttonLabel}
                </CustomButton>
            </View>
        </WarningContainer>
    )
}

export default ProfileComparison
