import React, { useMemo } from "react"
import { StyleSheet, Text, View } from "react-native"
import type { FileDividerRecord } from "../../lib/eventLogParser"
import { useTheme } from "../../context/ThemeContext"

type Props = {
    /** The file divider record containing the filename to display. */
    divider: FileDividerRecord
}

/**
 * Renders a horizontal line divider with a centered filename label.
 * Used to visually separate log entries originating from different files.
 * @param divider The file divider record containing the filename.
 */
const FileDivider: React.FC<Props> = ({ divider }) => {
    const { colors } = useTheme()

    const styles = useMemo(
        () =>
            StyleSheet.create({
                container: {
                    flexDirection: "row",
                    alignItems: "center",
                    marginVertical: 12,
                    marginHorizontal: 12,
                },
                line: {
                    flex: 1,
                    height: 1,
                    backgroundColor: colors.lightlyMuted,
                },
                text: {
                    marginHorizontal: 12,
                    fontSize: 12,
                    fontWeight: "500",
                    color: colors.lightlyMuted,
                },
            }),
        [colors]
    )

    return (
        <View style={styles.container}>
            <View style={styles.line} />
            <View style={{ alignItems: "center" }}>
                <Text style={[styles.text, { marginBottom: 2 }]}>{divider.fileName}</Text>
                {divider.traineeName && (
                    <View style={{ backgroundColor: colors.primary, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 4 }}>
                        <Text style={{ fontSize: 10, fontWeight: "bold", color: colors.primaryForeground }}>{divider.traineeName}</Text>
                    </View>
                )}
            </View>
            <View style={styles.line} />
        </View>
    )
}

export default FileDivider
