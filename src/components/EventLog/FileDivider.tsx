import React, { useMemo } from "react"
import { StyleSheet, Text, View } from "react-native"
import type { FileDividerRecord } from "../../lib/eventLogParser"
import { useTheme } from "../../context/ThemeContext"

type Props = {
    divider: FileDividerRecord
}

const FileDivider: React.FC<Props> = ({ divider }) => {
    const { colors } = useTheme()

    const styles = useMemo(() => StyleSheet.create({
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
    }), [colors])

    return (
        <View style={styles.container}>
            <View style={styles.line} />
            <Text style={styles.text}>{divider.fileName}</Text>
            <View style={styles.line} />
        </View>
    )
}

export default FileDivider
