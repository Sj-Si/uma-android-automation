import React, { useMemo, useContext, useState, useRef, FC, useCallback } from "react"
import { View, Text, ScrollView, StyleSheet, TouchableOpacity, Image } from "react-native"
import { Divider } from "react-native-paper"
import { useTheme } from "../../context/ThemeContext"
import { BotStateContext, defaultSettings } from "../../context/BotStateContext"
import { SkillPlanSettingsProps } from "./config"
import CustomSelect from "../../components/CustomSelect"
import CustomCheckbox from "../../components/CustomCheckbox"
import CustomButton from "../../components/CustomButton"
import CustomScrollView from "../../components/CustomScrollView"
import PageHeader from "../../components/PageHeader"
import WarningContainer from "../../components/WarningContainer"
import { SearchPageProvider } from "../../context/SearchPageContext"
import { Input } from "../../components/ui/input"
import { CircleCheckBig, Trash2 } from "lucide-react-native"
import { usePerformanceLogging } from "../../hooks/usePerformanceLogging"
import skillsData from "../../data/skills.json"
import icons from "../SkillSettings/icons"

/**
 * Represents a skill entry from the `skills.json` data file.
 */
interface Skill {
    /** The unique skill ID. */
    id: number
    /** The skill ID for the inherited version of the skill. Same as ID if skill can't be inherited. */
    gene_id: number
    /** The English display name of the skill. */
    name_en: string
    /** The English description of the skill. */
    desc_en: string
    /** The icon ID used for rendering the skill icon. */
    icon_id: number
    /** The skill point cost to purchase this skill. */
    cost: number
    /** The evaluated point value of the skill. */
    eval_pt: number
    /** The point-to-cost ratio for ranking efficiency. */
    pt_ratio: number
    /** The rarity tier of the skill. */
    rarity: number
    /** The activation condition string for the skill. */
    condition: string
    /** The precondition string that must be met before activation. */
    precondition: string
    /** Whether this is an inherited unique skill. */
    inherited: boolean
    /** The community tier list rating, or null if unrated. */
    community_tier: number | null
    /** The game version numbers where this skill is available. */
    versions: number[]
    /** The ID of the upgraded version of this skill, or null. */
    upgrade: number | null
    /** The ID of the downgraded version of this skill, or null. */
    downgrade: number | null
}

// Convert skills.json to array.
const skillData: Skill[] = Object.values(skillsData)

/**
 * The Skill Plan Settings page.
 * Configures a specific skill plan's purchasing strategy, inherited/negative skill options,
 * and a searchable list of skills to add to the plan.
 * @param planKey - The key identifying this plan in the settings object.
 * @param name - The navigation name for this plan's screen.
 * @param title - The display title for this plan.
 * @param description - The description shown at the top of the plan page.
 */
const SkillPlanSettings: FC<SkillPlanSettingsProps> = ({ planKey, name, title, description }) => {
    usePerformanceLogging(name)
    const { colors } = useTheme()
    const bsc = useContext(BotStateContext)

    const { settings, setSettings } = bsc

    // Merge current skills settings with defaults to handle missing properties.
    const combinedConfig = { ...defaultSettings.skills.plans, ...settings.skills.plans }

    const { enabled, strategy, enableBuyInheritedUniqueSkills, enableBuyNegativeSkills, plan } = combinedConfig[planKey]

    const [searchQuery, setSearchQuery] = useState("")
    const [showSelected, setShowSelected] = useState(false)
    const scrollViewRef = useRef<ScrollView>(null)

    // Parse skill plan from CSV string.
    const planIds: number[] = useMemo(() => {
        return plan && plan !== "" && typeof plan === "string" ? plan.split(",").map((s) => Number(s)) : []
    }, [plan])

    // Set showSelected to False whenever we have no selected skills.
    React.useEffect(() => {
        if (planIds.length === 0) {
            setShowSelected(false)
        }
    }, [planIds, setShowSelected])

    // Filter skills based on search and preferences.
    const filteredSkills = useMemo(() => {
        const skills: Skill[] = showSelected ? skillData.filter((skill: Skill) => planIds.includes(skill.id)) : skillData
        return skills.filter((skill: Skill) => skill.name_en.toLowerCase().includes(searchQuery.toLowerCase()))
    }, [searchQuery, planIds, showSelected])

    /**
     * Update a skill plan setting.
     * @param key The key of the setting to update.
     * @param value The value to set the setting to.
     */
    const updateSkillsSetting = useCallback(
        (key: string, value: any) => {
            setSettings({
                ...bsc.settings,
                skills: {
                    ...bsc.settings.skills,
                    plans: {
                        ...bsc.settings.skills.plans,
                        [planKey]: {
                            ...bsc.settings.skills.plans[planKey],
                            [key]: value,
                        },
                    },
                },
            })
        },
        [bsc.settings, planKey, setSettings]
    )

    /**
     * Toggle the selection of a skill within the skill plan.
     * If the skill is already present in the plan, it will be removed. Otherwise, it is added to the plan.
     * @param skill The specific skill instance to add or remove.
     */
    const handleSkillPress = useCallback(
        (skill: Skill) => {
            // Determine if this should be added to the skill plan or removed.
            const isSelected = planIds.includes(skill.id)

            let newPlanIds: number[] = []
            if (isSelected) {
                // Remove the skill from the skill plan.
                newPlanIds = planIds.filter((id) => id !== skill.id)
            } else {
                // Add the skill to the skill plan.
                newPlanIds = [...planIds, skill.id]
            }

            // Update the racing plan with the changes.
            updateSkillsSetting("plan", newPlanIds.join(","))
        },
        [planIds, updateSkillsSetting]
    )

    /**
     * Remove all skills from the current skill plan.
     */
    const clearAllSkillsFromPlan = useCallback(() => {
        updateSkillsSetting("plan", "")
    }, [updateSkillsSetting])

    const styles = useMemo(
        () =>
            StyleSheet.create({
                root: {
                    flex: 1,
                    flexDirection: "column",
                    margin: 10,
                    backgroundColor: colors.background,
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
                skillItem: {
                    backgroundColor: colors.card,
                    padding: 16,
                    borderRadius: 8,
                    marginBottom: 8,
                    flexDirection: "row",
                    justifyContent: "space-between",
                    alignItems: "center",
                },
                skillName: {
                    fontSize: 16,
                    fontWeight: "600",
                    color: colors.foreground,
                },
                skillDescription: {
                    fontSize: 14,
                    color: colors.foreground,
                    opacity: 0.7,
                    marginTop: 4,
                },
                skillSubtext: {
                    fontSize: 14,
                    color: colors.primary,
                    marginTop: 4,
                },
                input: {
                    borderWidth: 1,
                    borderColor: colors.border,
                    borderRadius: 8,
                    padding: 12,
                    fontSize: 16,
                    color: colors.foreground,
                    backgroundColor: colors.background,
                    marginBottom: 12,
                },
                inputLabel: {
                    fontSize: 16,
                    color: colors.foreground,
                    marginBottom: 8,
                },
                inputDescription: {
                    fontSize: 14,
                    color: colors.foreground,
                    opacity: 0.7,
                    marginTop: 8,
                },
                inputContainer: {
                    marginBottom: 16,
                },
            }),
        [colors]
    )

    const renderOptions = () => {
        return (
            <>
                <View style={styles.inputContainer}>
                    <CustomCheckbox
                        searchId={`enable-buy-inherited-unique-skills-${name}`}
                        checked={enableBuyInheritedUniqueSkills}
                        onCheckedChange={(checked) => updateSkillsSetting("enableBuyInheritedUniqueSkills", checked)}
                        label="Purchase All Inherited Unique Skills"
                        description={"When enabled, the bot will attempt to purchase all inherited unique skills regardless of their evaluated rating or community tier list rating."}
                        style={{ marginTop: 16 }}
                    />
                    <CustomCheckbox
                        searchId={`enable-buy-negative-skills-${name}`}
                        checked={enableBuyNegativeSkills}
                        onCheckedChange={(checked) => updateSkillsSetting("enableBuyNegativeSkills", checked)}
                        label="Purchase All Negative Skills"
                        description={"When enabled, the bot will attempt to purchase all negative skills (i.e. Firm Conditions ×)."}
                        style={{ marginTop: 16 }}
                    />
                </View>
                <View style={styles.inputContainer}>
                    <Text style={styles.inputLabel}>Automated Skill Point Spending Strategy</Text>
                    <CustomSelect
                        options={[
                            { value: "default", label: "Do Not Spend Remaining Points" },
                            { value: "optimize_skills", label: "Best Skills First" },
                            { value: "optimize_rank", label: "Optimize Rank" },
                        ]}
                        value={strategy}
                        defaultValue={defaultSettings.skills.plans[planKey].strategy}
                        onValueChange={(value) => updateSkillsSetting("strategy", value)}
                        placeholder="Select Strategy"
                    />
                    {strategy == "optimize_rank" && <WarningContainer>⚠️ Warning: Optimize Rank ignores any of the Skill Style Overrides set in the Skill Settings page.</WarningContainer>}
                    <Text style={styles.inputDescription}>
                        This option determines what the bot does with any remaining skill points after it has purchased all of the skills from the Planned Skills section and the other options on this
                        page.
                    </Text>
                    <Text style={styles.inputDescription}>
                        Best Skills First will use a community skill tier list to purchase better skills first and then within each tier it will attempt to optimize rank since the skills within each
                        tier are not ordered.
                    </Text>
                    <Text style={styles.inputDescription}>
                        Optimize Rank will purchase skills in a way which will result in the highest trainee rank. Avoid this option if you wish to train an uma up for TT or CM.
                    </Text>
                </View>
            </>
        )
    }

    const renderSkillList = () => {
        return (
            <View style={styles.section}>
                <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 12, gap: 12 }}>
                    <View style={{ flex: 1 }}>
                        <Text style={styles.sectionTitle}>Planned Skills</Text>
                        <Text style={[styles.inputDescription, { marginTop: 0 }]}>
                            Selected {planIds.length} / {filteredSkills.length} skills
                        </Text>
                    </View>
                    <View style={{ flexDirection: "row", gap: 8 }}>
                        <CustomButton icon={<Trash2 size={16} />} onPress={() => clearAllSkillsFromPlan()}>
                            Clear
                        </CustomButton>
                    </View>
                </View>

                <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 12 }}>
                    <CustomCheckbox
                        searchId={`show-selected-skills-${name}`}
                        checked={planIds.length === 0 ? false : showSelected}
                        disabled={planIds.length === 0}
                        onCheckedChange={(checked) => setShowSelected(checked && planIds.length !== 0)}
                        label="Show Only Selected Skills"
                    />
                </View>

                <View style={{ flexDirection: "row", marginBottom: 12 }}>
                    <View style={{ flex: 1 }}>
                        <Text style={[styles.inputDescription, { marginTop: 0 }]}>Select skills that the bot will always attempt to buy.</Text>
                    </View>
                </View>

                <View style={{ marginBottom: 16 }}>
                    <Input style={styles.input} value={searchQuery} onChangeText={setSearchQuery} placeholder="Search skills by name..." />
                    <View style={{ height: 700 }}>
                        <CustomScrollView
                            targetProps={{
                                data: filteredSkills,
                                renderItem: ({ item: skill }) => (
                                    <TouchableOpacity onPress={() => handleSkillPress(skill)} style={styles.skillItem}>
                                        <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
                                            <Image source={icons[skill.icon_id]} style={{ width: 64, height: 64, marginRight: 8 }} />
                                            <View style={{ flex: 1 }}>
                                                <Text style={styles.skillName}>{skill.name_en}</Text>
                                                <Text style={styles.skillDescription}>{skill.desc_en}</Text>
                                                <Text style={styles.skillSubtext}>ID: {skill.id}</Text>
                                            </View>
                                            {planIds.includes(skill.id) && <CircleCheckBig size={18} color={"green"} />}
                                        </View>
                                    </TouchableOpacity>
                                ),
                                nestedScrollEnabled: true,
                            }}
                            position="right"
                            horizontal={false}
                            persistentScrollbar={true}
                            indicatorStyle={{
                                width: 10,
                                backgroundColor: colors.foreground,
                            }}
                            containerStyle={{
                                flex: 1,
                            }}
                            minIndicatorSize={50}
                        />
                    </View>
                </View>
            </View>
        )
    }

    return (
        <View style={styles.root}>
            <PageHeader title={`${title} Plan`} />
            <SearchPageProvider page={name} scrollViewRef={scrollViewRef}>
                <ScrollView ref={scrollViewRef} nestedScrollEnabled={true} showsVerticalScrollIndicator={false} showsHorizontalScrollIndicator={false} contentContainerStyle={{ flexGrow: 1 }}>
                    <View className="m-1">
                        <Text style={styles.description}>{description}</Text>
                        <Divider style={{ marginBottom: 16 }} />
                        <CustomCheckbox
                            searchId={`enable-skill-plan-${planKey}`}
                            checked={enabled}
                            onCheckedChange={(checked) => updateSkillsSetting("enabled", checked)}
                            label={`Enable ${title} Plan (Beta)`}
                            description={"When enabled, the bot will attempt to purchase skills based on the following configuration."}
                        />
                        {enabled && (
                            <>
                                {renderOptions()}
                                <Divider style={{ marginBottom: 16 }} />
                                {renderSkillList()}
                            </>
                        )}
                    </View>
                </ScrollView>
            </SearchPageProvider>
        </View>
    )
}

export default React.memo(SkillPlanSettings)
