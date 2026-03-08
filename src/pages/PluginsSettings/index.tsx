import React, {useState, useContext, useEffect} from 'react';
import {
    StyleSheet,
    View,
    Text,
    TouchableOpacity,
    Modal,
    Dimensions,
    ScrollView,
    ViewStyle,
    useWindowDimensions,
} from 'react-native';
import { useTheme } from "../../context/ThemeContext"
import { BotStateContext, defaultSettings } from "../../context/BotStateContext"
import PageHeader from "../../components/PageHeader"
import { Divider } from "react-native-paper"
import DraggablePriorityList from "../../components/DraggablePriorityList"
import CustomButton from "../../components/CustomButton"
import CustomCheckbox from "../../components/CustomCheckbox"
import CustomSelect from "../../components/CustomSelect"
import CustomTitle from "../../components/CustomTitle"

import pluginsJson from "../../data/plugins.json"

interface PluginConfig {
    name: string
    description: string
    enabled: boolean
}

export const pluginConfigs: PluginConfig[] = pluginsJson as PluginConfig[]

interface PriorityListItem {
    name: string
    description: string
    defaultEnabled: boolean
}

interface PriorityListProps {
    items: PriorityListItem[]
    selectedItems: string[]
    setSelectedItems: (value: string[]) => void
    mode: "checkbox" | "priority"
    useModal: boolean
    style?: ViewStyle
    title?: string
    description?: string
}

const PriorityList: React.FC<PriorityListProps> = ({
    items,
    selectedItems,
    setSelectedItems,
    mode = "checkbox",
    useModal = false,
    style = { height: 200 },
    title = "",
    description = "",
}) => {
    const { colors } = useTheme()

    const [modalVisible, setModalVisible] = useState(false)

    const defaultSelectedItems: PriorityListItem[] = items.filter((item) => item.defaultEnabled === true)

    const getListItems = () => items.map((item) => ({
        id: item.name,
        label: item.name,
        description: item.description,
    }))

    const toggleItem = (item: string) => {
        if (selectedItems.includes(item)) {
            setSelectedItems(selectedItems.filter((s) => s !== item))
        } else {
            setSelectedItems([...selectedItems, item])
        }
    }

    const onReset = () => {
        setSelectedItems(defaultSelectedItems.map((item) => item.name))
    }

    const onSelectAll = () => {
        if (selectedItems.length === items.length) {
            setSelectedItems([])
            return
        }

        // Add any missing items to the current list, preserving order.
        const missingItems = items.filter((item) => !selectedItems.includes(item.name))
        setSelectedItems([...selectedItems, ...(missingItems.map((item) => item.name))])
    }

    const styles = StyleSheet.create({
        section: {
            marginBottom: 24,
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
    
    const renderPriority = () => (
        <DraggablePriorityList
            items={getListItems()}
            selectedItems={selectedItems}
            onSelectionChange={setSelectedItems}
            onOrderChange={(orderedItems) => setSelectedItems(orderedItems)}
            style={style}
        />
    )

    const renderCheckbox = () => (
        items.map((item) => (
            <CustomCheckbox
                key={item.name}
                id={`item-${item.name.toLowerCase()}`}
                checked={selectedItems.includes(item.name)}
                onCheckedChange={() => toggleItem(item.name)}
                label={item.name}
                className="my-2"
            />
        ))
    )

    const renderList = () => (
        <>
            {mode === "priority" ? renderPriority() : renderCheckbox()}
            <View style={styles.buttonRow}>
                <CustomButton onPress={() => onReset()} variant="destructive">Reset</CustomButton>
                <CustomButton onPress={() => onSelectAll()} variant="outline">Select All</CustomButton>
            </View>
        </>
    )

    const renderModal = () => (
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
                        {renderList()}
                    </TouchableOpacity>
                </TouchableOpacity>
            </Modal>
        </View>
    )

    const renderNoModal = () => (
        <View style={styles.section}>
            <CustomTitle title={title} description={description} />
            {renderList()}
        </View>
    )

    if (useModal === true) {
        return renderModal()
    } else {
        return renderNoModal()
    }
}

const PluginsSettings = () => {
    const { height } = useWindowDimensions()
    const { colors } = useTheme()
    const bsc = useContext(BotStateContext)

    const { settings, setSettings } = bsc

    const [enabledPlugins, setPlugins] = useState<string[]>(() =>
        settings.plugins?.enabledPlugins !== undefined ? settings.plugins.enabledPlugins : defaultSettings.plugins.enabledPlugins,
    )

    const [saleItems, setSaleItems] = useState<string[]>(() =>
        settings.plugins?.saleItems !== undefined ? settings.plugins.saleItems : defaultSettings.plugins.saleItems,
    )

    const pluginsSettings = {
        ...defaultSettings.plugins,
        ...settings.plugins,
        enabledPlugins: enabledPlugins,
        saleItems: saleItems,
    }

    const {
        enableTeamTrialsUseParfaitOnExtraRewards,
        dailyRaceName,
        enableLegendRaceUseParfait,
        clubRequestShoeType,
        enableClubDonation,
    } = pluginsSettings
    
    useEffect(() => {
        updateSetting("enabledPlugins", enabledPlugins)
    }, [enabledPlugins])

    useEffect(() => {
        updateSetting("saleItems", saleItems)
    }, [saleItems])

    // Sync local state when settings change (e.g., when switching profiles).
    useEffect(() => {
        if (settings.plugins?.enabledPlugins !== undefined) {
            setPlugins(settings.plugins.enabledPlugins)
        }
    }, [settings.plugins?.enabledPlugins])

    useEffect(() => {
        if (settings.plugins?.saleItems !== undefined) {
            setSaleItems(settings.plugins.saleItems)
        }
    }, [settings.plugins?.saleItems])

    const updateSetting = (key: keyof typeof settings.plugins, value: any) => {
        setSettings({
            ...bsc.settings,
            plugins: {
                ...bsc.settings.plugins,
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
            <PageHeader title="Plugins Settings" />
            <Text style={styles.description}>
                Configure settings for plugins.
            </Text>
            <Divider style={{ marginBottom: 16 }} />
            <ScrollView
                scrollEnabled={true}
                nestedScrollEnabled={true}
                showsVerticalScrollIndicator={false}
                showsHorizontalScrollIndicator={false}
                contentContainerStyle={{ flexGrow: 1 }}
            >
                <View style={styles.section}>
                    <PriorityList
                        items={pluginConfigs.map((item) => ({
                            name: item.name,
                            description: item.description,
                            defaultEnabled: item.enabled,
                        }))}
                        selectedItems={enabledPlugins}
                        setSelectedItems={(value) => setPlugins(value)}
                        mode={"priority"}
                        useModal={false}
                        style={{ height: height / 3 }}
                        title={"Plugins"}
                        description={"Enable and re-order plugins to run."}
                    />
                </View>
                <Divider style={{ marginBottom: 16 }} />
                {enabledPlugins.includes("Team Trials") && (
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
                {enabledPlugins.includes("Daily Races") && (
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
                {enabledPlugins.includes("Legend Race") && (
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
                {enabledPlugins.includes("Club Activity") && (
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
                        <View style={styles.inputContainer}>
                            <CustomCheckbox
                                id="enable-club-donation"
                                checked={enableClubDonation}
                                onCheckedChange={(checked) => updateSetting("enableClubDonation", checked)}
                                label="Enable Donations to Club"
                                description="When enabled, the bot will attempt to donate to all club members."
                                className="my-2"
                            />
                        </View>
                    </View>
                )}
                {enabledPlugins.includes("Daily Sale") && (
                    <View style={styles.section}>
                        <View className="m-1">
                            <PriorityList
                                items={defaultSettings.plugins.saleItems.map((item) => ({
                                    name: item,
                                    description: "",
                                    defaultEnabled: true,
                                }))}
                                selectedItems={saleItems}
                                setSelectedItems={(value) => setSaleItems(value)}
                                mode={"checkbox"}
                                useModal={true}
                                title={"Daily Sale Items to Purchase"}
                                description={"Select which Daily Sale items to purchase. If the Daily Sale plugin is disabled, then this setting has no effect."}
                            />
                        </View>
                    </View>
                )}
            </ScrollView>
        </View>
    )
}

export default PluginsSettings
