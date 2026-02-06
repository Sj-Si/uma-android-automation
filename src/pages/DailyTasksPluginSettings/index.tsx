import React, {useState, useContext, useRef, useEffect} from 'react';
import {
    StyleSheet,
    View,
    Text,
    TouchableOpacity,
    LayoutChangeEvent,
    ViewStyle,
    ScrollView,
} from 'react-native';
import { useNavigation } from "@react-navigation/native"
import { useTheme } from "../../context/ThemeContext"
import { BotStateContext, defaultSettings } from "../../context/BotStateContext"
import PageHeader from "../../components/PageHeader"
import NavigationLink from "../../components/NavigationLink"
import DragList, {DragListRenderItemInfo} from 'react-native-draglist';
import { Grip } from "lucide-react-native"
import { Checkbox } from "../../components/ui/checkbox"
import { Label } from "../../components/ui/label"
import { Text as UIText } from "../../components/ui/text"
import { Divider } from "react-native-paper"
import CustomCheckbox from "../../components/CustomCheckbox"
import CustomSelect from "../../components/CustomSelect"
import CustomTitle from "../../components/CustomTitle"
import CustomButton from "../../components/CustomButton"

interface ListItem {
    id: string
    label: string
    description?: string | null
}

interface DragListProps {
    items: ListItem[]
    selectedItems: string[]
    setSelectedItems: (value: string[]) => void
    onSelectionChange: (selectedItems: string[]) => void
    onOrderChange: (orderedItems: string[]) => void
    className?: string
    style?: ViewStyle
}

const DraggableList: React.FC<DragListProps> = ({ items, selectedItems, setSelectedItems, onSelectionChange, onOrderChange, className = "", style}) => {
    const { colors, isDark } = useTheme()

    const [orderedItems, setOrderedItems] = useState<string[]>(items.map((item) => item.id))
    const dragOrderRef = useRef<string[]>([]) // Track drag order separately.
    const dragListRef = useRef<any>(null)

    const [contentHeight, setContentHeight] = useState(0)
    const [containerHeight, setContainerHeight] = useState(0)

    const handleContainerLayout = (event: LayoutChangeEvent) => {
        setContainerHeight(event.nativeEvent.layout.height)
    }

    const handleContentSizeChange = (width: number, height: number) => {
        setContentHeight(height)
    }

    // Sync orderedItems with selectedItems when selection changes.
    useEffect(() => {
        if (selectedItems.length === 0) {
            setOrderedItems(items.map((item) => item.id))
            dragOrderRef.current = [] // Clear the drag order.
            return
        }

        // Get deselected items that should remain visible.
        const deselectedItems = items.map((item) => item.id).filter((id) => !selectedItems.includes(id))
        
        // Use the selectedItems order as-is, then append deselected items.
        const finalOrdered = [...selectedItems, ...deselectedItems]
        setOrderedItems(finalOrdered)
        
        // Update drag order ref with the selected items in their order.
        dragOrderRef.current = selectedItems
    }, [selectedItems, items])

    const handleReordered = async (fromIndex: number, toIndex: number) => {
        const copy = [...orderedItems]
        const [removed] = copy.splice(fromIndex, 1)
        copy.splice(toIndex, 0, removed)

        setOrderedItems(copy)

        // Update the drag order ref with only the selected items in their new order.
        const selectedInNewOrder = copy.filter((id) => selectedItems.includes(id))
        dragOrderRef.current = selectedInNewOrder

        onOrderChange(selectedInNewOrder)
    }

    const toggleItem = (itemId: string) => {
        const newSelection = selectedItems.includes(itemId) ? selectedItems.filter((id) => id !== itemId) : [...selectedItems, itemId]

        onSelectionChange(newSelection)
    }

    const scrollToTop = () => {
        if (dragListRef.current && dragListRef.current.scrollToIndex) {
            dragListRef.current.scrollToIndex({ index: 0, animated: true })
        }
    }

    const scrollToBottom = () => {
        if (dragListRef.current && dragListRef.current.scrollToIndex) {
            const lastIndex = orderedItems.length - 1
            dragListRef.current.scrollToIndex({ index: lastIndex, animated: true })
        }
    }

    const selectAll = (setList: (value: string[]) => void, currentList: string[]) => {
        // Add any missing items from default settings to the current list, preserving order.
        const missingItems = defaultSettings.dailyTasks.plugins.filter((item) => !currentList.includes(item))
        setList([...currentList, ...missingItems])
    }

    const renderItem = (info: DragListRenderItemInfo<ListItem>) => {
        const { item, onDragStart, onDragEnd } = info
        const isSelected = selectedItems.includes(item.id)
        const priorityNumber = isSelected ? orderedItems.indexOf(item.id) + 1 : null

        return (
            <View key={item.id} style={{ marginVertical: 1 }} className={`mb-2 ${className}`}>
                <TouchableOpacity
                    style={{ justifyContent: "space-between", backgroundColor: colors.input }}
                    activeOpacity={0.7}
                    className="flex flex-row items-center gap-2 border border-border rounded-lg p-2"
                    onPress={() => toggleItem(item.id)}
                >
                    <View style={{ flex: 1, flexDirection: "row", gap: 10 }}>
                        {/* Priority Number */}
                        {isSelected && (
                            <View className="w-6 h-6 bg-primary rounded-full items-center justify-center">
                                <Text style={{ color: isDark ? "white" : "black" }}>{priorityNumber}</Text>
                            </View>
                        )}

                        {/* Checkbox for selection */}
                        <Checkbox id={`priority-${item.id}`} checked={isSelected} onCheckedChange={() => toggleItem(item.id)} className="dark:border-gray-400" />

                        <View className="flex-1 gap-1">
                            <Label style={{ color: colors.foreground }} className="text-sm" onPress={() => toggleItem(item.id)}>
                                {item.label}
                            </Label>
                            {item.description && <UIText className="text-muted-foreground text-xs">{item.description}</UIText>}
                        </View>
                    </View>

                    {/* Drag Handle */}
                    {isSelected && (
                        <View>
                            <Grip size={18} color={colors.primary} onPressIn={isSelected ? onDragStart : undefined} onPressOut={isSelected ? onDragEnd : undefined} />
                        </View>
                    )}
                </TouchableOpacity>
            </View>
        )
    }

    return (
        <View style={style}>
            <Text style={{ fontSize: 12, color: colors.mutedForeground, paddingBottom: 10 }}>Drag items to reorder. Top to bottom = highest to lowest priority.</Text>
            <DragList
                scrollEnabled={false}
                ref={dragListRef}
                data={orderedItems.map((id) => items.find((item) => item.id === id)!).filter(Boolean)}
                keyExtractor={(item) => item.id}
                onReordered={handleReordered}
                renderItem={renderItem}
                onLayout={handleContainerLayout}
                onContentSizeChange={handleContentSizeChange}
                showsVerticalScrollIndicator={false}
            />

            <View style={{ flexDirection: "row", justifyContent: "space-between", marginTop: 20 }}>
                <CustomButton
                    onPress={() => {
                        // For prioritization, reset to default and dismiss modal.
                        setSelectedItems(defaultSettings.dailyTasks.plugins)
                    }}
                    variant="destructive"
                >
                    Reset
                </CustomButton>
                <CustomButton onPress={() => selectAll(setSelectedItems, selectedItems)} variant="outline">
                    Select All
                </CustomButton>
            </View>

            {/* Scroll helper buttons for very long lists */}
            {contentHeight > containerHeight && (
                <View style={{ flexDirection: "row", justifyContent: "space-between", marginTop: 10 }}>
                    <TouchableOpacity style={{ borderColor: colors.primary }} className="px-3 py-1 border rounded" onPress={scrollToTop}>
                        <Text style={{ color: colors.foreground }} className="text-xs">
                            ↑ Scroll Up
                        </Text>
                    </TouchableOpacity>
                    <TouchableOpacity style={{ borderColor: colors.primary }} className="px-3 py-1 border rounded" onPress={scrollToBottom}>
                        <Text style={{ color: colors.foreground }} className="text-xs">
                            ↓ Scroll Down
                        </Text>
                    </TouchableOpacity>
                </View>
            )}


            {/* Show message below the list when no items are selected */}
            {selectedItems.length === 0 && <Text style={{ fontSize: 12, color: colors.mutedForeground, paddingTop: 10 }}>No items selected. Select items to set priority order.</Text>}
        </View>
    )
}

const DailyTasksPluginSettings = () => {
    const { colors } = useTheme()
    const navigation = useNavigation()
    const bsc = useContext(BotStateContext)

    const { settings, setSettings } = bsc

    const [pluginItems, setPluginItems] = useState<string[]>(() =>
        settings.dailyTasks?.plugins !== undefined ? settings.dailyTasks.plugins : defaultSettings.dailyTasks.plugins,
    )

    const dailyTasksSettings = {
        ...defaultSettings.dailyTasks,
        ...settings.dailyTasks,
        pluginItems: pluginItems,
    }

    const {
        enableTeamTrialsUseParfaitOnExtraRewards,
        dailyRaceName,
        enableLegendRaceUseParfait,
        clubRequestShoeType,
    } = dailyTasksSettings

    useEffect(() => {
        updateSetting("plugins", pluginItems)
    }, [pluginItems])
    
    // Sync local state when settings change (e.g., when switching profiles).
    useEffect(() => {
        if (settings.dailyTasks?.plugins !== undefined) {
            setPluginItems(settings.dailyTasks.plugins)
        }
    }, [settings.dailyTasks?.plugins])

    const updateSetting = (key: keyof typeof settings.dailyTasks, value: any) => {
        setSettings({
            ...bsc.settings,
            dailyTasks: {
                ...bsc.settings.dailyTasks,
                [key]: value,
            },
        })
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
    })

    return (
        <View style={styles.root}>
            <PageHeader title="Daily Tasks Plugin Settings" />
            <Text style={styles.description}>
                Allows configuration and priority of various plugins used by
                the daily tasks routine.
            </Text>
            <Divider style={{ marginBottom: 16 }} />
            <ScrollView nestedScrollEnabled={true} showsVerticalScrollIndicator={false} showsHorizontalScrollIndicator={false} contentContainerStyle={{ flexGrow: 1 }}>
                <View style={styles.section}>
                    <DraggableList
                        items={defaultSettings.dailyTasks.plugins.map((it) => ({
                            id: it,
                            label: it,
                        }))}
                        selectedItems={pluginItems}
                        setSelectedItems={setPluginItems}
                        onSelectionChange={setPluginItems}
                        onOrderChange={(orderedItems) => {
                            // Update the order when items are reordered.
                            setPluginItems(orderedItems)
                        }}
                    />
                </View>
                {pluginItems.includes("Team Trials") && (
                    <View style={styles.inputContainer}>
                        <CustomTitle title="Team Trials Settings" />
                        <CustomCheckbox
                            id="enable-team-trials-use-parfait-on-extra-rewards"
                            checked={enableTeamTrialsUseParfaitOnExtraRewards}
                            onCheckedChange={(checked) => updateSetting("enableTeamTrialsUseParfaitOnExtraRewards", checked)}
                            label="Use Parfait when Extra Rewards are Available"
                            description="When enabled, the bot will use a Pleasing Parfait item when the current team trials opponent gives an extra reward on every win."
                            className="my-2"
                        />
                    </View>
                )}
                {pluginItems.includes("Daily Races") && (
                    <View style={styles.inputContainer}>
                        <CustomTitle title="Daily Race Settings" />
                        <View style={styles.inputContainer}>
                            <Text style={styles.inputLabel}>Daily Race Selection</Text>
                            <CustomSelect
                                options={[
                                    { value: "moonlight_sho", label: "Moonlight Sho" },
                                    { value: "jupiter_cup", label: "Jupiter Cup" },
                                ]}
                                value={dailyRaceName}
                                onValueChange={(value) => updateSetting("dailyRaceName", value)}
                                placeholder="Select Daily Race"
                            />
                            <Text style={styles.inputDescription}>The race strategy to use for all races during Junior Year. If Auto is selected, the bot will auto-select the best strategy that puts them cloest to the front of the pack.</Text>
                        </View>
                    </View>
                )}
                {pluginItems.includes("Legend Race") && (
                    <View style={styles.inputContainer}>
                        <CustomTitle title="Legend Race Settings" />
                        <CustomCheckbox
                            id="enable-legend-race-use-parfait"
                            checked={enableLegendRaceUseParfait}
                            onCheckedChange={(checked) => updateSetting("enableLegendRaceUseParfait", checked)}
                            label="Use Parfait on Legend Races"
                            description="When enabled, the bot will use a Pleasing Parfait item for all legend races."
                            className="my-2"
                        />
                    </View>
                )}
                {pluginItems.includes("Club Activity") && (
                    <View style={styles.inputContainer}>
                        <CustomTitle title="Club Activity Settings" />
                        <View style={styles.inputContainer}>
                            <Text style={styles.inputLabel}>Request Shoe Type</Text>
                            <CustomSelect
                                options={[
                                    { value: "sprint", label: "Sprint" },
                                    { value: "mile", label: "Mile" },
                                    { value: "medium", label: "Medium" },
                                    { value: "long", label: "Long" },
                                    { value: "dirt", label: "Dirt" },
                                ]}
                                value={clubRequestShoeType}
                                onValueChange={(value) => updateSetting("clubRequestShoeType", value)}
                                placeholder="Select Shoe Type"
                            />
                            <Text style={styles.inputDescription}>The shoe type to request from club members.</Text>
                        </View>
                    </View>
                )}
            </ScrollView>
        </View>
    )
}

export default DailyTasksPluginSettings
