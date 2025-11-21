package com.impactai.impactai.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class ImpactReportFormatter {

    public String formatComment(ImpactAnalysisService.ImpactReport report, String riskScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 🔍 PR Impact Analysis\n\n");
        sb.append("### Changed Nodes\n");
        for (String changed : report.getChangedNodes()) {
            sb.append("- `").append(changed).append("`\n");
        }
        sb.append("\n### Impacted Nodes (direct + downstream)\n");
        for (String impacted : report.getAllImpactedNodes()) {
            if (!report.getChangedNodes().contains(impacted)) {
                sb.append("- `").append(impacted).append("`\n");
            }
        }
        sb.append("\n### Impact Depth: ").append(report.getImpactDepth()).append("\n");
        sb.append("### Risk: **").append(riskScore).append("**\n");

        sb.append("\n---\n*This comment was generated automatically by Impact-AI.*\n");

        return sb.toString();
    }
}
