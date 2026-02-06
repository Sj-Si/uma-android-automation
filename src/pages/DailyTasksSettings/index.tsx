import React, { useContext, useEffect, useState } from "react"
import { View, Text, ScrollView, StyleSheet, Modal, TouchableOpacity, Dimensions } from "react-native"
import { useNavigation } from "@react-navigation/native"
import { useTheme } from "../../context/ThemeContext"
import { BotStateContext, defaultSettings } from "../../context/BotStateContext"
import PageHeader from "../../components/PageHeader"
import NavigationLink from "../../components/NavigationLink"
import { Divider } from "react-native-paper"
import DraggablePriorityList from "../../components/DraggablePriorityList"
import CustomButton from "../../components/CustomButton"
import CustomCheckbox from "../../components/CustomCheckbox"

const DailyTasksSettings = () => {
    const { colors } = useTheme()
    const navigation = useNavigation()
    const bsc = useContext(BotStateContext)
    const [saleItemsModalVisible, setSaleItemsModalVisible] = useState(false)

    const { settings, setSettings } = bsc

    const [saleItems, setSaleItems] = useState<string[]>(() =>
        settings.dailyTasks?.saleItems !== undefined ? settings.dailyTasks.saleItems : defaultSettings.dailyTasks.saleItems,
    )

    useEffect(() => {
        updateSetting("saleItems", saleItems)
    }, [saleItems])

    // Sync local state when settings change (e.g., when switching profiles).
    useEffect(() => {
        if (settings.dailyTasks?.saleItems !== undefined) {
            setSaleItems(settings.dailyTasks.saleItems)
        }
    }, [settings.dailyTasks?.saleItems])

    const updateSetting = (key: keyof typeof settings.dailyTasks, value: any) => {
        setSettings({
            ...bsc.settings,
            dailyTasks: {
                ...bsc.settings.dailyTasks,
                [key]: value,
            },
        })
    }

    const toggleItem = (item: string, list: string[], setList: (value: string[]) => void) => {
        if (list.includes(item)) {
            setList(list.filter((s) => s !== item))
        } else {
            setList([...list, item])
        }
    }

    const clearAll = (setList: (value: string[]) => void) => {
        setList([])
    }

    const selectAll = (setList: (value: string[]) => void, currentList: string[]) => {
        // Add any missing items from default settings to the current list, preserving order.
        const missingItems = defaultSettings.dailyTasks.saleItems.filter((item) => !currentList.includes(item))
        setList([...currentList, ...missingItems])
    }

    const styles = StyleSheet.create({
        root: {
            flex: 1,
            flexDirection: "column",
            justifyContent: "center",
            margin: 10,
            backgroundColor: colors.background,
        },
        header: {
            flexDirection: "row",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 20,
        },
        headerLeft: {
            flexDirection: "row",
            alignItems: "center",
            gap: 12,
        },
        menuButton: {
            padding: 8,
            borderRadius: 8,
        },
        title: {
            fontSize: 24,
            fontWeight: "bold",
            color: colors.foreground,
        },
        description: {
            fontSize: 14,
            color: colors.foreground,
            opacity: 0.7,
            marginBottom: 16,
            lineHeight: 20,
        },
        section: {
            marginBottom: 24,
        },
        sectionTitle: {
            fontSize: 18,
            fontWeight: "600",
            color: colors.foreground,
            marginBottom: 12,
        },
        inputContainer: {
            marginBottom: 16,
        },
        inputLabel: {
            fontSize: 16,
            color: colors.foreground,
            marginBottom: 8,
        },
        input: {
            borderWidth: 1,
            borderColor: colors.border,
            borderRadius: 8,
            padding: 12,
            fontSize: 16,
            color: colors.foreground,
            backgroundColor: colors.background,
        },
        inputDescription: {
            fontSize: 14,
            color: colors.foreground,
            opacity: 0.7,
            marginTop: 4,
        },
        titleDescription: {
            fontSize: 14,
            color: colors.foreground,
            opacity: 0.7,
            marginBottom: 4,
        },
        warningContainer: {
            backgroundColor: colors.warningBg,
            borderLeftWidth: 4,
            borderLeftColor: colors.warningBorder,
            padding: 12,
            marginTop: 12,
            borderRadius: 8,
        },
        warningText: {
            fontSize: 14,
            color: colors.warningText,
            lineHeight: 20,
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
    })

    const renderSelector = (
        title: string,
        selectedItems: string[],
        setSelectedItems: (value: string[]) => void,
        modalVisible: boolean,
        setModalVisible: React.Dispatch<React.SetStateAction<boolean>>,
        description?: string,
        mode: "checkbox" | "priority" = "checkbox",
    ) => (
        <View style={styles.section}>
            <View style={styles.row}>
                <Text style={styles.label}>{title}</Text>
                <TouchableOpacity onPress={() => setModalVisible(true)}>
                    <Text style={styles.pressableText}>{selectedItems.length === 0 ? "None" : selectedItems.join(", ")}</Text>
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
                                items={defaultSettings.dailyTasks.saleItems.map((item) => ({
                                    id: item,
                                    label: item,
                                }))}
                                selectedItems={selectedItems}
                                onSelectionChange={setSelectedItems}
                                onOrderChange={(orderedItems) => {
                                    // Update the order when items are reordered.
                                    setSelectedItems(orderedItems)
                                }}
                            />
                        ) : (
                            defaultSettings.dailyTasks.saleItems.map((item) => (
                                <CustomCheckbox
                                    key={item}
                                    id={`item-${item.toLowerCase()}`}
                                    checked={selectedItems.includes(item)}
                                    onCheckedChange={() => toggleItem(item, selectedItems, setSelectedItems)}
                                    label={item}
                                    className="my-2"
                                />
                            ))
                        )}

                        <View style={styles.buttonRow}>
                            <CustomButton
                                onPress={() => {
                                    if (mode === "priority") {
                                        // For prioritization, reset to default and dismiss modal.
                                        setSelectedItems(defaultSettings.dailyTasks.saleItems)
                                        setModalVisible(false)
                                    } else {
                                        // For blacklist, just clear the list.
                                        clearAll(setSelectedItems)
                                    }
                                }}
                                variant="destructive"
                            >
                                Clear All
                            </CustomButton>
                            <CustomButton onPress={() => selectAll(setSelectedItems, selectedItems)} variant="outline">
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
            <PageHeader title="Daily Tasks Settings" />
            <Text style={styles.description}>
                Configure settings for the Daily Tasks routine.
            </Text>
            <Divider style={{ marginBottom: 16 }} />
            <ScrollView nestedScrollEnabled={true} showsVerticalScrollIndicator={false} showsHorizontalScrollIndicator={false} contentContainerStyle={{ flexGrow: 1 }}>
                <View style={styles.section}>
                    <View className="m-1">
                        <NavigationLink
                            title="Select Daily Task Plugins"
                            description="Enable and re-order plugins to run during daily tasks handling."
                            onPress={() => navigation.navigate("DailyTasksPluginSettings" as never)}
                            style={{ ...styles.section, marginTop: 0 }}
                        />
                    </View>
                </View>
                <View style={styles.section}>
                    <View className="m-1">
                        {renderSelector(
                            "Daily Sale Items to Purchase",
                            saleItems,
                            (value) => setSaleItems(value),
                            saleItemsModalVisible,
                            setSaleItemsModalVisible,
                            "Select which Daily Sale items to purchase. If the Daily Sale plugin is disabled, then this setting has no effect.",
                            "checkbox",
                        )}
                    </View>
                </View>
            </ScrollView>
        </View>
    )
}

export default DailyTasksSettings
