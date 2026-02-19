import { useContext, useState, FC } from "react"
import { View, Text, ScrollView, StyleSheet, TouchableOpacity, Image } from "react-native"
import { Divider } from "react-native-paper"
import { useTheme } from "../../context/ThemeContext"
import { BotStateContext, defaultSettings } from "../../context/BotStateContext"
import CustomSelect from "../../components/CustomSelect"
import CustomCheckbox from "../../components/CustomCheckbox"
import CustomButton from "../../components/CustomButton"
import CustomScrollView from "../../components/CustomScrollView"
import PageHeader from "../../components/PageHeader"
import { Input } from "../../components/ui/input"
import { CircleCheckBig, Trash2 } from "lucide-react-native"
import skillsData from "../../data/skills.json"
import icons from "../SkillSettings/icons"

interface Skill {
    id: number
    name_en: string
    desc_en: string
    icon_id: number
    cost: number
    eval_pt: number
    pt_ratio: number
    rarity: number
    condition: string
    precondition: string
    inherited: boolean
    community_tier: number | null
    versions: number[]
    upgrade: number | null
    downgrade: number | null
}

export interface SkillPlanSettingsProps {
    planKey: string
    name: string
    title: string
    description: string
}

export interface DynamicSkillPlanSettingsProps {
    [key: string]: SkillPlanSettingsProps
}

export const skillPlanSettingsPages: DynamicSkillPlanSettingsProps = {
    skillPointCheck: {
        planKey: "skillPointCheck",
        name: "SkillPlanSettingsSkillPointCheck",
        title: "Skill Point Check",
        description:
            "Configure the skills to buy when the skill point threshold has been reached.\n\nEvaluated ratings are sourced from Umamusume Wiki and community tier list ratings are sourced from Game8.",
    },
    preFinals: {
        planKey: "preFinals",
        name: "SkillPlanSettingsPreFinals",
        title: "Pre-Finals",
        description: "Configure the skills to buy just before the finale season.\n\nEvaluated ratings are sourced from Umamusume Wiki and community tier list ratings are sourced from Game8.",
    },
    careerComplete: {
        planKey: "careerComplete",
        name: "SkillPlanSettingsCareerComplete",
        title: "Career Complete",
        description: "Configure the skills to buy after the career has completed.\n\nEvaluated ratings are sourced from Umamusume Wiki and community tier list ratings are sourced from Game8.",
    },
}

const SkillPlanSettings: FC<SkillPlanSettingsProps> = ({ planKey, name, title, description }) => {
    const { colors } = useTheme()
    const bsc = useContext(BotStateContext)

    const { settings, setSettings } = bsc

    // Merge current skills settings with defaults to handle missing properties.
    const combinedConfig = { ...defaultSettings.skills.plans, ...settings.skills.plans }

    const { enabled, strategy, enableBuyInheritedUniqueSkills, enableBuyNegativeSkills, plan } = combinedConfig[planKey]

    const [searchQuery, setSearchQuery] = useState("")

    // Parse skill plan from CSV string.
    const planIds: number[] = plan && plan !== "" && typeof plan === "string" ? plan.split(",").map((s) => Number(s)) : []
    // Convert skills.json to array.
    const skillData: Skill[] = Object.values(skillsData)

    // Filter skills based on search and preferences.
    const filteredSkills = skillData.filter((skill) => skill.name_en.toLowerCase().includes(searchQuery.toLowerCase()))

    const updateSkillsSetting = (key: string, value: any) => {
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
    }

    const handleSkillPress = (skill: Skill) => {
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
    }

    const clearAllSkillsFromPlan = () => {
        updateSkillsSetting("plan", "")
    }

    const styles = StyleSheet.create({
        root: {
            flex: 1,
            flexDirection: "column",
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
        description: {
            fontSize: 14,
            color: colors.foreground,
            opacity: 0.7,
            marginBottom: 16,
            lineHeight: 20,
        },
        title: {
            fontSize: 24,
            fontWeight: "bold",
            color: colors.foreground,
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
        terrainButton: {
            padding: 12,
            borderRadius: 8,
            marginRight: 8,
        },
        terrainButtonText: {
            fontSize: 14,
            fontWeight: "600",
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

    const renderOptions = () => {
        return (
            <>
                <View style={styles.inputContainer}>
                    <CustomCheckbox
                        id={`enable-buy-inherited-unique-skills-${name}`}
                        checked={enableBuyInheritedUniqueSkills}
                        onCheckedChange={(checked) => updateSkillsSetting("enableBuyInheritedUniqueSkills", checked)}
                        label="Purchase All Inherited Unique Skills"
                        description={"When enabled, the bot will attempt to purchase all inherited unique skills regardless of their evaluated rating or community tier list rating."}
                        style={{ marginTop: 16 }}
                    />
                    <CustomCheckbox
                        id={`enable-buy-negative-skills-${name}`}
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
                    {strategy == "optimize_rank" && (
                        <View style={styles.warningContainer}>
                            <Text style={styles.warningText}>⚠️ Warning: Optimize Rank ignores any of the Skill Style Overrides set in the Skill Settings page.</Text>
                        </View>
                    )}
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
            <ScrollView nestedScrollEnabled={true} showsVerticalScrollIndicator={false} showsHorizontalScrollIndicator={false} contentContainerStyle={{ flexGrow: 1 }}>
                <View className="m-1">
                    <Text style={styles.description}>{description}</Text>
                    <Divider style={{ marginBottom: 16 }} />
                    <CustomCheckbox
                        id={`enable-career-complete-skill-plan-${planKey}`}
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
        </View>
    )
}

export default SkillPlanSettings
