import { useMemo } from "react"
import { View, Text, StyleSheet, TouchableOpacity, ViewStyle } from "react-native"
import { useNavigation, DrawerActions } from "@react-navigation/native"
import { Ionicons } from "@expo/vector-icons"
import { useTheme } from "../../context/ThemeContext"

interface PageHeaderProps {
    /** The title to display in the header. */
    title: string
    /** Whether to show the Home button (default: true). */
    showHomeButton?: boolean
    /** Optional right-side component to render (e.g., ThemeToggle). */
    rightComponent?: React.ReactNode
    /** Optional additional styles for the header container. */
    style?: ViewStyle
}

/**
 * A reusable header component for pages that includes:
 * - A hamburger menu button to open the drawer.
 * - A Home button for quick navigation to the Home page.
 * - A page title.
 * - An optional right-side component.
 */
const PageHeader = ({ title, showHomeButton = true, rightComponent, style }: PageHeaderProps) => {
    const { colors } = useTheme()
    const navigation = useNavigation()

    const openDrawer = () => {
        navigation.dispatch(DrawerActions.openDrawer())
    }

    const goHome = () => {
        navigation.navigate("Home" as never)
    }

    const styles = useMemo(() => StyleSheet.create({
        header: {
            flexDirection: "row",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 20,
        },
        headerLeft: {
            flexDirection: "row",
            alignItems: "center",
            gap: 8,
        },
        menuButton: {
            padding: 8,
            borderRadius: 8,
        },
        homeButton: {
            padding: 8,
            borderRadius: 8,
        },
        title: {
            fontSize: 24,
            fontWeight: "bold",
            color: colors.foreground,
        },
    }), [colors])

    return (
        <View style={[styles.header, style]}>
            <View style={styles.headerLeft}>
                <TouchableOpacity onPress={openDrawer} style={styles.menuButton} activeOpacity={0.7}>
                    <Ionicons name="menu" size={28} color={colors.foreground} />
                </TouchableOpacity>
                {showHomeButton && (
                    <TouchableOpacity onPress={goHome} style={styles.homeButton} activeOpacity={0.7}>
                        <Ionicons name="home" size={24} color={colors.foreground} />
                    </TouchableOpacity>
                )}
                <Text style={styles.title}>{title}</Text>
            </View>
            {rightComponent && <View>{rightComponent}</View>}
        </View>
    )
}

export default PageHeader
