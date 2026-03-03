/** @format */

import React, { useRef, useState, useMemo } from "react"
import { View, LayoutChangeEvent, ViewStyle, PressableProps, StyleSheet } from "react-native"
import { useTheme } from "../../context/ThemeContext"
import { Option, Select, SelectContent, SelectGroup, SelectItem, SelectLabel, NativeSelectScrollView } from "../ui/select"
import CustomButton from "../CustomButton"
import { Ionicons } from "@expo/vector-icons"
import * as SelectPrimitive from "@rn-primitives/select"

interface SelectOption {
    value: string
    label: string
    disabled?: boolean
}

interface SelectButtonProps extends PressableProps {
    variant?: "default" | "destructive" | "outline" | "secondary" | "ghost" | "link" | "success" | "info" | "warning" | "error"
    size?: "default" | "sm" | "lg" | "icon"
    style?: ViewStyle
    className?: string
    isLoading?: boolean
    fontSize?: number
    icon?: React.ReactElement
    iconName?: string
    iconPosition?: "left" | "right"
    options?: SelectOption[]
    groupLabel?: string
    portalHost?: string
    defaultValue?: string
    placeholder?: string
    value?: string
    setValue?: React.Dispatch<React.SetStateAction<string>>
    onValueChange?: (value: string | undefined) => void
    onPress?: () => void
}

const SelectButton: React.FC<SelectButtonProps> = ({
    variant = "default",
    size = "default",
    style,
    className = "",
    isLoading = false,
    fontSize,
    icon,
    iconName,
    iconPosition = "left",
    options = [],
    groupLabel,
    portalHost,
    defaultValue,
    placeholder,
    value,
    setValue,
    onValueChange,
    onPress,
    ...props
}) => {
    const { colors, isDark } = useTheme()

    const triggerRef = useRef<View>(null)
    const [triggerWidth, setTriggerWidth] = useState<number>(0)

    const currentLabel = options.find((item) => item.value === value || item.value === defaultValue)?.label

    /**
     * Determine the background color based on variant and theme.
     * @returns The background color for the button.
     */
    const getBackgroundColor = () => {
        switch (variant) {
            case "destructive":
                return colors.destructive
            case "outline":
                return isDark ? "black" : "white"
            case "secondary":
                return isDark ? colors.primary : colors.secondary
            case "ghost":
                return "transparent"
            case "link":
                return "transparent"
            case "success":
                return colors.success
            case "info":
                return colors.info
            case "warning":
                return colors.warning
            case "error":
                return colors.error
            default:
                return isDark ? colors.secondary : colors.primary
        }
    }

    /**
     * Determine the text color based on variant and theme.
     * @returns The text color for the button.
     */
    const getTextColor = () => {
        switch (variant) {
            case "destructive":
                return "white"
            case "outline":
                return isDark ? "white" : "black"
            case "secondary":
                return isDark ? "black" : "white"
            case "success":
                return colors.successContent
            case "info":
                return colors.infoContent
            case "warning":
                return colors.warningContent
            case "error":
                return colors.errorContent
            default:
                return "black"
        }
    }

    const getSelectContentBackgroundColor = () => {
        if (variant === "default") {
            return isDark ? colors.primary : colors.secondary
        } else {
            return getBackgroundColor()
        }
    }

    const styles = useMemo(
        () =>
            StyleSheet.create({
                container: {
                    flexDirection: "row",
                    overflow: "hidden",
                    alignItems: "center",
                },
                button: {
                    flex: 1,
                    borderTopRightRadius: 0,
                    borderBottomRightRadius: 0,
                },
                buttonDropdown: {
                    borderTopLeftRadius: 0,
                    borderBottomLeftRadius: 0,
                },
                verticalRuleContainer: {
                    flex: 1,
                    justifyContent: "center",
                    alignItems: "center",
                },
                verticalRule: {
                    width: 1,
                    height: "70%",
                    backgroundColor: colors.border,
                },
            }),
        [colors]
    )

    /**
     * Callback fired when the trigger layout changes.
     * The trigger layout is used to determine the width of the dropdown content.
     * @param event The layout change event.
     */
    const onTriggerLayout = (event: LayoutChangeEvent) => {
        const { width: measuredWidth } = event.nativeEvent.layout
        setTriggerWidth(measuredWidth)
    }

    const onPressButton = () => {
        if (onPress) {
            onPress()
        }
    }

    const handleValueChange = (option: Option) => {
        if (onValueChange) {
            onValueChange(option?.value || "")
        }
        if (setValue) {
            setValue(option?.value || "")
        }
    }

    const getIcon = (): React.ReactElement | undefined => {
        if (icon) {
            return icon
        } else if (iconName) {
            return <Ionicons name={iconName} size={20} color={getTextColor()} />
        } else {
            return undefined
        }
    }

    return (
        <Select onValueChange={handleValueChange} value={value as any} defaultValue={defaultValue as any}>
            <View style={styles.container} ref={triggerRef} onLayout={onTriggerLayout}>
                <CustomButton style={styles.button} variant={variant} icon={getIcon()} iconPosition={iconPosition} size={size} isLoading={false} onPress={onPressButton}>
                    {currentLabel ?? placeholder}
                </CustomButton>
                <View style={styles.verticalRuleContainer}>
                    <View style={styles.verticalRule} />
                </View>
                <SelectPrimitive.Trigger asChild>
                    <CustomButton style={styles.buttonDropdown} variant={variant} size={size} isLoading={false}>
                        <Ionicons name="caret-down" size={20} color={getTextColor()} />
                    </CustomButton>
                </SelectPrimitive.Trigger>
            </View>
            <SelectContent
                style={{
                    width: triggerWidth,
                    backgroundColor: getSelectContentBackgroundColor(),
                    borderColor: getSelectContentBackgroundColor(),
                }}
                align="end"
                sideOffset={1}
                position="popper"
                portalHost={portalHost}
            >
                <NativeSelectScrollView>
                    <SelectGroup>
                        {groupLabel && <SelectLabel>{groupLabel}</SelectLabel>}
                        {options &&
                            options.map((option) => (
                                <SelectItem key={option.value} label={option.label} value={option.value} disabled={option.disabled}>
                                    {option.label}
                                </SelectItem>
                            ))}
                    </SelectGroup>
                </NativeSelectScrollView>
            </SelectContent>
        </Select>
    )
}

export default React.memo(SelectButton)
