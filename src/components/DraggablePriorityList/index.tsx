import React, { useState, useEffect, useRef } from "react"
import { View, Text, TouchableOpacity, LayoutChangeEvent, ViewStyle, ScrollView } from "react-native"
import DragList, { DragListRenderItemInfo } from "react-native-draglist"
import { Checkbox } from "../ui/checkbox"
import { Label } from "../ui/label"
import { Text as UIText } from "../ui/text"
import { useTheme } from "../../context/ThemeContext"
import { Grip } from "lucide-react-native"

interface PriorityItem {
    /** The unique identifier for this item. */
    id: string
    /** The display label for this item. */
    label: string
    /** Optional description text displayed below the label. */
    description?: string | null
}

interface DraggablePriorityListProps {
    /** The full list of available priority items. */
    items: PriorityItem[]
    /** The IDs of currently selected items, in priority order. */
    selectedItems: string[]
    /** Callback fired when items are selected or deselected. */
    onSelectionChange: (selectedItems: string[]) => void
    /** Callback fired when the order of selected items changes via drag. */
    onOrderChange: (orderedItems: string[]) => void
    /** Optional NativeWind class name. */
    className?: string
    /** Optional custom style for the container. */
    style?: ViewStyle
}

/**
 * A drag-and-drop list that allows users to select items and reorder them by priority.
 * Selected items display a numbered badge and a drag handle for reordering.
 * Uses `react-native-draglist` for gesture-based drag interactions.
 * @param items The full list of available priority items.
 * @param selectedItems The IDs of currently selected items, in priority order.
 * @param onSelectionChange Callback fired when items are selected or deselected.
 * @param onOrderChange Callback fired when the order of selected items changes via drag.
 * @param className Optional NativeWind class name.
 * @param style Optional custom style for the container.
 */
const DraggablePriorityList: React.FC<DraggablePriorityListProps> = ({ items, selectedItems, onSelectionChange, onOrderChange, className = "", style }) => {
    const { colors, isDark } = useTheme()

    const [orderedItems, setOrderedItems] = useState<string[]>(items.map((item) => item.id))
    const dragOrderRef = useRef<string[]>([]) // Track drag order separately.
    const dragListRef = useRef<any>(null)

    const scrollOffset = useRef<number>(0)

    const [contentHeight, setContentHeight] = useState(0)
    const [containerHeight, setContainerHeight] = useState(0)

    /**
     * Callback fired when the container layout changes.
     * @param event The layout event.
     */
    const handleContainerLayout = (event: LayoutChangeEvent) => {
        setContainerHeight(event.nativeEvent.layout.height)
    }

    /**
     * Callback fired when the content size changes.
     * @param width The width of the content.
     * @param height The height of the content.
     */
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

    /**
     * Callback fired when the order of items changes.
     * @param fromIndex The index of the item being moved.
     * @param toIndex The index where the item is moved to.
     */
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

    /**
     * Toggles the selection state of an item.
     * @param itemId The ID of the item to toggle.
     */
    const toggleItem = (itemId: string) => {
        const newSelection = selectedItems.includes(itemId) ? selectedItems.filter((id) => id !== itemId) : [...selectedItems, itemId]

        onSelectionChange(newSelection)
    }

    /** Scrolls up the list by roughly one page of results. */
    const scrollUp = () => {
        if (dragListRef.current && dragListRef.current.scrollToIndex) {
            const newOffset = Math.max(0, scrollOffset.current - (containerHeight * 0.75))
            dragListRef.current.scrollToOffset({ offset: newOffset, animated: true })
        }
    }

    /** Scrolls down the list by roughly one page of results. */
    const scrollDown = () => {
        if (dragListRef.current && dragListRef.current.scrollToIndex) {
            const newOffset = scrollOffset.current + (containerHeight * 0.75)
            dragListRef.current.scrollToOffset({ offset: newOffset, animated: true })
        }
    }

    /**
     * Renders a single item in the list.
     * @param info The render item information.
     * @returns The rendered item.
     */
    const renderItem = (info: DragListRenderItemInfo<PriorityItem>) => {
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
        <View style={{ flexGrow: 1}}>
            <Text style={{ fontSize: 12, color: colors.mutedForeground, paddingBottom: 10 }}>Drag items to reorder. Top to bottom = highest to lowest priority.</Text>

            {/* Always show the DragList, regardless of selection state */}
            <ScrollView scrollEnabled={true} nestedScrollEnabled={true} style={{ flexGrow: 1}}>
                <DragList
                    scrollEnabled={true}
                    ref={dragListRef}
                    data={orderedItems.map((id) => items.find((item) => item.id === id)!).filter(Boolean)}
                    keyExtractor={(item) => item.id}
                    onReordered={handleReordered}
                    renderItem={renderItem}
                    style={style}
                    onLayout={handleContainerLayout}
                    onContentSizeChange={handleContentSizeChange}
                    onScroll={(e) => {scrollOffset.current = e.nativeEvent.contentOffset.y}}
                    showsVerticalScrollIndicator={false}
                />

                {/* Scroll helper buttons for very long lists */}
                {contentHeight > containerHeight && (
                    <View style={{ flexDirection: "row", justifyContent: "space-between", marginTop: 10 }}>
                        <TouchableOpacity style={{ borderColor: colors.primary }} className="px-3 py-1 border rounded" onPress={scrollUp}>
                            <Text style={{ color: colors.foreground }} className="text-xs">
                                ↑ Scroll Up
                            </Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={{ borderColor: colors.primary }} className="px-3 py-1 border rounded" onPress={scrollDown}>
                            <Text style={{ color: colors.foreground }} className="text-xs">
                                ↓ Scroll Down
                            </Text>
                        </TouchableOpacity>
                    </View>
                )}
            </ScrollView>

            {/* Show message below the list when no items are selected */}
            {selectedItems.length === 0 && <Text style={{ fontSize: 12, color: colors.mutedForeground, paddingTop: 10 }}>No items selected. Select items to set priority order.</Text>}
        </View>
    )
}

export default React.memo(DraggablePriorityList)
