/**
 * Props for the `SkillPlanSettings` component.
 * Each instance configures a specific skill plan (e.g. `skillPointCheck`, `preFinals`, `careerComplete`).
 */
export interface SkillPlanSettingsProps {
    /** The key identifying this plan in the settings object. */
    planKey: string
    /** The navigation name for this plan's screen. */
    name: string
    /** The display title for this plan. */
    title: string
    /** The description shown at the top of the plan page. */
    description: string
}

/**
 * Dynamic map of plan keys to their settings page props.
 */
export interface DynamicSkillPlanSettingsProps {
    [key: string]: SkillPlanSettingsProps
}

/** Registry of all available skill plan settings pages and their configuration. */
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
