import React from "react"
import type { GapRecord } from "../../lib/eventLogParser"
import WarningContainer from "../WarningContainer"

type Props = {
    gap: GapRecord
}

const GapsNotice: React.FC<Props> = ({ gap }) => {
    return <WarningContainer style={{ marginBottom: 10, marginTop: 0 }}>⚠️ {gap.from === gap.to ? `Day ${gap.from} missing.` : `Days ${gap.from}-${gap.to} missing.`}</WarningContainer>
}

export default GapsNotice
