import { useMemo } from "react"
import { Text, StyleSheet, TextStyle } from "react-native"
import { useTheme } from "../../context/ThemeContext"

interface CustomTitleProps {
    title: string
    description?: string
    style?: TextStyle
}

const CustomTitle = ({ title, description, style }: CustomTitleProps) => {
    const { colors } = useTheme()

    const styles = useMemo(() => StyleSheet.create({
        sectionTitle: {
            fontSize: 18,
            fontWeight: "600",
            color: colors.foreground,
            marginBottom: 12,
        },
        description: {
            fontSize: 14,
            color: colors.foreground,
            opacity: 0.7,
            marginBottom: 16,
            lineHeight: 20,
        },
    }), [colors])

    return (
        <>
            <Text style={[styles.sectionTitle, style]}>{title}</Text>
            {description && <Text style={styles.description}>{description}</Text>}
        </>
    )
}

export default CustomTitle
