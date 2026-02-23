import * as fs from "fs"
import * as path from "path"
import { parseLogs, LogFileInput, DayRecord } from "../lib/eventLogParser"

// Get log file path from command line arguments.
const args = process.argv.slice(2)

if (args.length === 0) {
    console.error("Usage: npx tsx testEventLogParser.ts <path-to-log-file>")
    process.exit(1)
}

const logFilePath = args[0]

if (!fs.existsSync(logFilePath)) {
    console.error(`Error: File does not exist at path: ${logFilePath}`)
    process.exit(1)
}

try {
    const content = fs.readFileSync(logFilePath, "utf8")
    const fileName = path.basename(logFilePath)

    const inputs: LogFileInput[] = [{ name: fileName, content }]

    console.log(`Analyzing log file: ${fileName}...\n`)

    const result = parseLogs(inputs)

    console.log("--- Parse Results ---")
    console.log(`Total records: ${result.records.length}`)
    console.log(`Total days: ${result.records.filter((r) => r.kind === "day").length}`)
    console.log(`Total gaps: ${result.records.filter((r) => r.kind === "gap").length}`)
    console.log(`Total file dividers: ${result.records.filter((r) => r.kind === "fileDivider").length}`)
    console.log(`Errors encountered: ${result.errors.length}`)

    if (result.errors.length > 0) {
        console.log("\n--- Errors ---")
        result.errors.forEach((err, i) => console.log(`[${i + 1}] ${err.message}`))
    }

    console.log("\n--- Metadata ---")
    console.log(result.meta)

    if (result.records.length > 0) {
        // Find the first day.
        const firstDay = result.records.find((r) => r.kind === "day")

        // Reverse search for the last day.
        const lastDay = [...result.records].reverse().find((r) => r.kind === "day")

        // Copy objects and remove bulky 'triggers' for cleaner output.
        console.log("\n--- First Day Found ---")
        if (firstDay) {
            const fdDisplay = { ...firstDay } as DayRecord
            delete fdDisplay.triggers
            console.log(fdDisplay)
        } else {
            console.log("None")
        }

        console.log("\n--- Last Day Found ---")
        if (lastDay) {
            const ldDisplay = { ...lastDay } as DayRecord
            delete ldDisplay.triggers
            console.log(ldDisplay)
        } else {
            console.log("None")
        }
    }
} catch (error) {
    console.error("An error occurred during parsing:")
    console.error(error)
}
