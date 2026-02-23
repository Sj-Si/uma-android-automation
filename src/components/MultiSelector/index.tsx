import React, { useMemo,  useState, useEffect, useRef } from "react"
import { View, Text, StyleSheet, TouchableOpacity, Modal, TextInput, ScrollView, ViewStyle } from "react-native"
import { useTheme } from "../../context/ThemeContext"
import CustomCheckbox from "../CustomCheckbox"
import CustomButton from "../CustomButton"
import { Search, X } from "lucide-react-native"

interface MultiSelectorProps {
    title: string
    description: string
    options: string[]
    selectedOptions: string[]
    onSelectionChange: (selectedOptions: string[]) => void
    selectAllLabel?: string
    selectAllDescription?: string
    selectIndividualLabel?: string
    style?: ViewStyle
    selectAll?: boolean
}

const MultiSelector: React.FC<MultiSelectorProps> = ({
    title,
    description,
    options,
    selectedOptions,
    onSelectionChange,
    selectAllLabel = "Select All",
    selectAllDescription = "Select all available options",
    selectIndividualLabel = "Select Individual Items",
    style,
    selectAll: propSelectAll,
}) => {
    const { colors, isDark } = useTheme()
    const [selectAll, setSelectAll] = useState(false)
    const [modalVisible, setModalVisible] = useState(false)
    const [searchQuery, setSearchQuery] = useState("")
    const scrollViewRef = useRef<ScrollView | null>(null)
    const lastTouchY = useRef(0)
    const currentScrollY = useRef(0)
    const isScrolling = useRef(false)

    // Update selectAll state when selectedOptions changes or prop changes.
    useEffect(() => {
        if (propSelectAll !== undefined) {
            setSelectAll(propSelectAll)
        } else {
            setSelectAll(selectedOptions.length === options.length && options.length > 0)
        }
    }, [selectedOptions, options, propSelectAll])

    // Close modal when selectAll is checked.
    useEffect(() => {
        if (selectAll && modalVisible) {
            setModalVisible(false)
        }
    }, [selectAll, modalVisible])

    const handleSelectAll = (checked: boolean) => {
        if (checked) {
            onSelectionChange([...options])
        } else {
            onSelectionChange([])
        }
    }

    const handleOptionToggle = (option: string, checked: boolean) => {
        if (selectAll) return // Disable individual selection when "Select All" is checked

        if (checked) {
            onSelectionChange([...selectedOptions, option])
        } else {
            onSelectionChange(selectedOptions.filter((item) => item !== option))
        }
    }

    const clearAll = () => {
        onSelectionChange([])
        setSelectAll(false)
    }

    const handleTouchStart = (event: any) => {
        const touch = event.nativeEvent.touches[0]
        lastTouchY.current = touch.pageY
        isScrolling.current = false
    }

    const handleTouchMove = (event: any) => {
        if (!scrollViewRef.current) return

        const touch = event.nativeEvent.touches[0]
        const currentY = touch.pageY
        const deltaY = lastTouchY.current - currentY

        // Only scroll if there's significant movement.
        if (Math.abs(deltaY) > 1) {
            isScrolling.current = true
            // Use a balanced scroll factor for smooth but responsive movement.
            const scrollFactor = 2.0
            const newScrollY = Math.max(0, currentScrollY.current + deltaY * scrollFactor)
            currentScrollY.current = newScrollY

            scrollViewRef.current.scrollTo({
                y: newScrollY,
                animated: false,
            })
            lastTouchY.current = currentY
        }
    }

    const handleTouchEnd = () => {
        isScrolling.current = false
    }

    // Filter options based on search query.
    const filteredOptions = options.filter((option) => option.toLowerCase().includes(searchQuery.toLowerCase()))

    const styles = useMemo(() => StyleSheet.create({
        container: {
            marginBottom: 24,
        },
        title: {
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
        selectAllContainer: {
            marginBottom: 16,
        },
        multiSelectorContainer: {
            flexDirection: "row",
            alignItems: "center",
            justifyContent: "space-between",
            marginBottom: 16,
        },
        multiSelectorButton: {
            flexDirection: "row",
            alignItems: "center",
            backgroundColor: colors.primary,
            paddingHorizontal: 16,
            paddingVertical: 12,
            borderRadius: 8,
            flex: 1,
            marginRight: 12,
        },
        multiSelectorButtonDisabled: {
            flexDirection: "row",
            alignItems: "center",
            backgroundColor: colors.muted || colors.border,
            paddingHorizontal: 16,
            paddingVertical: 12,
            borderRadius: 8,
            flex: 1,
            marginRight: 12,
            opacity: 0.6,
        },
        multiSelectorButtonText: {
            color: colors.background,
            fontWeight: "600",
            marginLeft: 8,
        },
        multiSelectorButtonTextDisabled: {
            color: colors.foreground,
            fontWeight: "600",
            marginLeft: 8,
        },
        selectedCount: {
            fontSize: 12,
            color: colors.foreground,
            opacity: 0.6,
        },
        disabledNote: {
            fontSize: 12,
            color: colors.foreground,
            opacity: 0.5,
            fontStyle: "italic",
        },
        modalOverlay: {
            flex: 1,
            backgroundColor: "rgba(0, 0, 0, 0.5)",
            justifyContent: "center",
            alignItems: "center",
        },
        modalContent: {
            backgroundColor: colors.background,
            borderRadius: 16,
            padding: 20,
            width: "90%",
            maxHeight: "80%",
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
        searchContainer: {
            flexDirection: "row",
            alignItems: "center",
            backgroundColor: colors.card,
            borderWidth: 1,
            borderColor: colors.border,
            borderRadius: 8,
            paddingHorizontal: 12,
            marginBottom: 20,
        },
        searchInput: {
            flex: 1,
            paddingVertical: 12,
            color: colors.foreground,
            fontSize: 16,
            backgroundColor: "transparent",
        },
        optionsList: {
            maxHeight: 400,
        },
        optionItem: {
            marginBottom: 8,
            paddingLeft: 8,
            borderWidth: 1,
            borderColor: colors.border,
            borderRadius: 8,
        },
        noResults: {
            textAlign: "center",
            color: colors.foreground,
            opacity: 0.6,
            padding: 20,
        },
        buttonRow: {
            flexDirection: "row",
            justifyContent: "space-between",
            marginTop: 20,
        },
        clearSearchButton: {
            padding: 8,
            marginLeft: 8,
        },
    }), [colors])

    return (
        <View style={[styles.container, style]}>
            <Text style={styles.title}>{title}</Text>
            <Text style={styles.description}>{description}</Text>

            <View style={styles.selectAllContainer}>
                <CustomCheckbox
                    id={`select-all-${title.toLowerCase().replace(/\s+/g, "-")}`}
                    checked={selectAll}
                    onCheckedChange={handleSelectAll}
                    label={selectAllLabel}
                    description={selectAllDescription}
                    className="my-2"
                />
            </View>

            <View style={styles.multiSelectorContainer}>
                <TouchableOpacity style={selectAll ? styles.multiSelectorButtonDisabled : styles.multiSelectorButton} onPress={() => setModalVisible(true)} disabled={selectAll}>
                    <Search size={20} color={selectAll ? colors.foreground : colors.background} />
                    <Text style={selectAll ? styles.multiSelectorButtonTextDisabled : styles.multiSelectorButtonText}>{selectAll ? "All Selected" : selectIndividualLabel}</Text>
                </TouchableOpacity>
            </View>

            <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap" }}>
                <Text style={[styles.selectedCount, { flexShrink: 0 }]}>
                    {selectedOptions.length} of {options.length} selected
                </Text>

                {selectAll && <Text style={[styles.disabledNote, { flex: 1, marginLeft: 12 }]}>Individual selection is disabled when "Select All" is enabled</Text>}
            </View>

            {/* Modal for individual selection */}
            <Modal animationType="slide" transparent={true} visible={modalVisible && !selectAll} onRequestClose={() => setModalVisible(false)}>
                <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={() => setModalVisible(false)}>
                    <TouchableOpacity style={styles.modalContent} activeOpacity={1} onPress={(e) => e.stopPropagation()}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>{title}</Text>
                            <TouchableOpacity style={styles.closeButton} onPress={() => setModalVisible(false)}>
                                <X size={24} color={colors.foreground} />
                            </TouchableOpacity>
                        </View>

                        <View style={styles.searchContainer}>
                            <Search size={20} color={colors.foreground} />
                            <TextInput style={styles.searchInput} placeholder="Search..." placeholderTextColor={colors.foreground + "80"} value={searchQuery} onChangeText={setSearchQuery} />
                            {searchQuery.length > 0 && (
                                <TouchableOpacity style={styles.clearSearchButton} onPress={() => setSearchQuery("")}>
                                    <X size={16} color={colors.foreground} />
                                </TouchableOpacity>
                            )}
                        </View>

                        <ScrollView
                            style={styles.optionsList}
                            showsVerticalScrollIndicator={false}
                            nestedScrollEnabled={true}
                            onTouchStart={handleTouchStart}
                            onTouchMove={handleTouchMove}
                            onTouchEnd={handleTouchEnd}
                            ref={scrollViewRef}
                            onScroll={(event) => {
                                const offsetY = event.nativeEvent.contentOffset.y
                                currentScrollY.current = offsetY
                            }}
                        >
                            {filteredOptions.length > 0 ? (
                                filteredOptions.map((option) => (
                                    <View key={option} style={styles.optionItem}>
                                        <CustomCheckbox
                                            id={`modal-option-${option.toLowerCase().replace(/\s+/g, "-")}`}
                                            checked={selectedOptions.includes(option)}
                                            onCheckedChange={(checked) => handleOptionToggle(option, checked)}
                                            label={option}
                                            className="my-1"
                                        />
                                    </View>
                                ))
                            ) : (
                                <Text style={styles.noResults}>No results found</Text>
                            )}
                        </ScrollView>

                        <View style={styles.buttonRow}>
                            <CustomButton onPress={() => clearAll()} variant="destructive">
                                Clear All
                            </CustomButton>
                            <CustomButton onPress={() => handleSelectAll(true)} variant={isDark ? "default" : "outline"}>
                                Select All
                            </CustomButton>
                        </View>
                    </TouchableOpacity>
                </TouchableOpacity>
            </Modal>
        </View>
    )
}

export default MultiSelector
