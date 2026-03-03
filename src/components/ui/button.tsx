import { NativeOnlyAnimatedView } from "@/src/components/ui/native-only-animated-view"
import { TextClassContext } from "@/src/components/ui/text"
import { cn } from "@/src/lib/utils"
import { cva, type VariantProps } from "class-variance-authority"
import { Platform, Pressable } from "react-native"
import { useSharedValue, useAnimatedStyle, withTiming } from "react-native-reanimated"

const buttonVariants = cva(
    cn(
        "group shrink-0 flex-row items-center justify-center gap-2 rounded-md shadow-none",
        Platform.select({
            web: "focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive whitespace-nowrap outline-none transition-all focus-visible:ring-[3px] disabled:pointer-events-none [&_svg:not([class*='size-'])]:size-4 [&_svg]:pointer-events-none [&_svg]:shrink-0",
        })
    ),
    {
        variants: {
            variant: {
                default: cn("bg-primary active:bg-primary/90 shadow-sm shadow-black/5", Platform.select({ web: "hover:bg-primary/90" })),
                destructive: cn(
                    "bg-destructive active:bg-destructive/90 dark:bg-destructive/60 shadow-sm shadow-black/5",
                    Platform.select({
                        web: "hover:bg-destructive/90 focus-visible:ring-destructive/20 dark:focus-visible:ring-destructive/40",
                    })
                ),
                success: cn(
                    "bg-success active:bg-success/90 dark:bg-success/60 shadow-sm shadow-black/5",
                    Platform.select({
                        web: "hover:bg-success/90 focus-visible:ring-success/20 dark:focus-visible:ring-success/40",
                    })
                ),
                info: cn(
                    "bg-info active:bg-info/90 dark:bg-info/60 shadow-sm shadow-black/5",
                    Platform.select({
                        web: "hover:bg-info/90 focus-visible:ring-info/20 dark:focus-visible:ring-info/40",
                    })
                ),
                warning: cn(
                    "bg-warning active:bg-warning/90 dark:bg-warning/60 shadow-sm shadow-black/5",
                    Platform.select({
                        web: "hover:bg-warning/90 focus-visible:ring-warning/20 dark:focus-visible:ring-warning/40",
                    })
                ),
                error: cn(
                    "bg-error active:bg-error/90 dark:bg-error/60 shadow-sm shadow-black/5",
                    Platform.select({
                        web: "hover:bg-error/90 focus-visible:ring-error/20 dark:focus-visible:ring-error/40",
                    })
                ),
                outline: cn(
                    "border-border bg-background active:bg-accent dark:bg-input/30 dark:border-input dark:active:bg-input/50 border shadow-sm shadow-black/5",
                    Platform.select({
                        web: "hover:bg-accent dark:hover:bg-input/50",
                    })
                ),
                secondary: cn("bg-secondary active:bg-secondary/80 shadow-sm shadow-black/5", Platform.select({ web: "hover:bg-secondary/80" })),
                ghost: cn("active:bg-accent dark:active:bg-accent/50", Platform.select({ web: "hover:bg-accent dark:hover:bg-accent/50" })),
                link: "",
            },
            size: {
                default: cn("h-10 px-4 py-2 sm:h-9", Platform.select({ web: "has-[>svg]:px-3" })),
                sm: cn("h-9 gap-1.5 rounded-md px-3 sm:h-8", Platform.select({ web: "has-[>svg]:px-2.5" })),
                lg: cn("h-11 rounded-md px-6 sm:h-10", Platform.select({ web: "has-[>svg]:px-4" })),
                icon: "h-10 w-10 sm:h-9 sm:w-9",
            },
        },
        defaultVariants: {
            variant: "default",
            size: "default",
        },
    }
)

const buttonTextVariants = cva(cn("text-foreground text-sm font-medium", Platform.select({ web: "pointer-events-none transition-colors" })), {
    variants: {
        variant: {
            default: "text-primary-foreground",
            destructive: "text-white",
            outline: cn("group-active:text-accent-foreground", Platform.select({ web: "group-hover:text-accent-foreground" })),
            secondary: "text-secondary-foreground",
            ghost: "group-active:text-accent-foreground",
            link: cn("text-primary group-active:underline", Platform.select({ web: "underline-offset-4 hover:underline group-hover:underline" })),
            success: "text-success-foreground",
            info: "text-info-foreground",
            warning: "text-warning-foreground",
            error: "text-error-foreground",
        },
        size: {
            default: "",
            sm: "",
            lg: "",
            icon: "",
        },
    },
    defaultVariants: {
        variant: "default",
        size: "default",
    },
})

type ButtonProps = React.ComponentProps<typeof Pressable> & React.RefAttributes<typeof Pressable> & VariantProps<typeof buttonVariants>

function Button({ className, variant, size, disabled, onPressIn, onPressOut, ...props }: ButtonProps) {
    const opacity = useSharedValue(1)

    const animatedStyle = useAnimatedStyle(() => {
        return {
            opacity: disabled ? 0.5 : opacity.value,
        }
    })

    const handlePressIn = (event: any) => {
        if (!disabled) {
            opacity.value = withTiming(0.6, { duration: 100 })
        }
        onPressIn?.(event)
    }

    const handlePressOut = (event: any) => {
        if (!disabled) {
            opacity.value = withTiming(1, { duration: 100 })
        }
        onPressOut?.(event)
    }

    return (
        <TextClassContext.Provider value={buttonTextVariants({ variant, size })}>
            <NativeOnlyAnimatedView style={animatedStyle}>
                <Pressable
                    className={cn(buttonVariants({ variant, size }), className)}
                    style={({ pressed }) =>
                        Platform.OS === "web"
                            ? {
                                  opacity: disabled ? 0.5 : pressed ? 0.6 : 1,
                              }
                            : undefined
                    }
                    role="button"
                    disabled={disabled}
                    onPressIn={handlePressIn}
                    onPressOut={handlePressOut}
                    {...props}
                />
            </NativeOnlyAnimatedView>
        </TextClassContext.Provider>
    )
}

export { Button, buttonTextVariants, buttonVariants }
export type { ButtonProps }
