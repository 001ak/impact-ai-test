package com.impactai.impactai.util;

import com.impactai.impactai.model.LineRange;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatchParser {

    private static final Pattern HUNK_HEADER_PATTERN = Pattern.compile("@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@");

    /**
     * Parse changed line ranges from a unified diff patch (GitHub API)
     * @param patch unified diff string
     * @return List of ranges (startLine, endLine) in the NEW file that were changed (only for add/modify)
     */
    public static List<LineRange> extractChangedLineRanges(String patch) {
        List<LineRange> ranges = new ArrayList<>();
        if (patch == null || patch.isEmpty()) return ranges;

        String[] lines = patch.split("\n");
        int currentNewLine = 0;
        int hunkStart = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher m = HUNK_HEADER_PATTERN.matcher(line);
            if (m.find()) {
                // Start of hunk
                int newStart = Integer.parseInt(m.group(3));
                int newCount = Integer.parseInt(m.group(4));
                int hunkEnd = (newCount == 0) ? newStart : (newStart + newCount - 1);

                // Parse down to changed lines (skip context)
                int newLineNum = newStart;
                i++; // Move to next line in patch

                int rangeStart = -1;
                int rangeEnd = -1;

                for (; i < lines.length; i++) {
                    if (lines[i].startsWith("@@")) { // new hunk
                        i--; break;
                    }
                    char c = lines[i].isEmpty() ? ' ' : lines[i].charAt(0);
                    if (c == '+') {
                        // Added/modified line in NEW file
                        if (rangeStart == -1) rangeStart = newLineNum;
                        rangeEnd = newLineNum;
                        newLineNum++;
                    } else if (c == '-') {
                        // Deleted lines, do not increment newLineNum
                    } else {
                        // Context line, treat as separator for ranges
                        if (rangeStart != -1) {
                            ranges.add(new LineRange(rangeStart, rangeEnd));
                            rangeStart = rangeEnd = -1;
                        }
                        newLineNum++;
                    }
                }
                if (rangeStart != -1) {
                    ranges.add(new LineRange(rangeStart, rangeEnd));
                }
            }
        }
        return ranges;
    }
}
