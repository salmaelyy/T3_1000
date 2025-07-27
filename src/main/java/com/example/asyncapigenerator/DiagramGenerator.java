package com.example.asyncapigenerator;

import java.util.*;

public class DiagramGenerator {

    public enum Mode {
        SHORT_ONLY, FULL_ONLY, BOTH
    }

    public List<DiagramResult> generateMermaid(AsyncAPIData data) {
        Set<String> participants = new LinkedHashSet<>();
        for (AsyncAPIData.Flow flow : data.getFlows()) {
            participants.add(flow.from);
            participants.add(flow.to);
        }

        int participantCount = participants.size();
        Mode mode = participantCount > 5 ? Mode.BOTH : Mode.FULL_ONLY;

        List<DiagramResult> results = new ArrayList<>();

        if (mode == Mode.FULL_ONLY) {
            results.add(new DiagramResult("full", buildDiagram(data, false)));
        } else if (mode == Mode.BOTH) {
            results.add(new DiagramResult("short", buildDiagram(data, true)));
            results.add(new DiagramResult("full", buildDiagram(data, false)));
        }

        return results;
    }

    private String buildDiagram(AsyncAPIData data, boolean useShortNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("sequenceDiagram\n");

        Set<String> participants = new LinkedHashSet<>();
        for (AsyncAPIData.Flow flow : data.getFlows()) {
            participants.add(flow.from);
            participants.add(flow.to);
        }

        Map<String, String> nameToAlias = new HashMap<>();
        Map<String, Integer> aliasCount = new HashMap<>();

        for (String name : participants) {
            String alias = useShortNames ? shortenName(name) : sanitize(name);

            String baseAlias = alias;
            int count = aliasCount.getOrDefault(baseAlias, 0);
            while (nameToAlias.containsValue(alias)) {
                count++;
                alias = baseAlias + count;
            }
            aliasCount.put(baseAlias, count);

            nameToAlias.put(name, alias);
            sb.append("    participant ").append(alias).append(" as \"").append(alias).append("\"\n");
        }

        for (AsyncAPIData.Flow flow : data.getFlows()) {
            sb.append("    ")
                    .append(nameToAlias.get(flow.from))
                    .append("->>")
                    .append(nameToAlias.get(flow.to))
                    .append(": ")
                    .append(flow.message)
                    .append("\n");
        }

        return sb.toString();
    }

    private String shortenName(String name) {
        String base = extractCore(name);
        String first7 = base.length() > 7 ? base.substring(0, 7) : base;

        String lastUpper = "";
        for (int i = base.length() - 1; i >= 0; i--) {
            char c = base.charAt(i);
            if (Character.isUpperCase(c)) {
                lastUpper = String.valueOf(c);
                break;
            }
        }

        return first7 + lastUpper + ".";
    }

    private String extractCore(String raw) {
        return sanitize(raw.replaceAll("[^a-zA-Z0-9]", ""));
    }

    private String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    public static class DiagramResult {
        public final String mode; // "short" oder "full"
        public final String content;

        public DiagramResult(String mode, String content) {
            this.mode = mode;
            this.content = content;
        }
    }
}