package com.example.asyncapigenerator;

import java.util.LinkedHashSet;
import java.util.Set;

public class DiagramGenerator {
    public String generateMermaid(AsyncAPIData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("sequenceDiagram\n");

        // Teilnehmer sammeln
        Set<String> participants = new LinkedHashSet<>();
        for (AsyncAPIData.Flow flow : data.getFlows()) {
            participants.add(flow.from);
            participants.add(flow.to);
        }

        // Teilnehmer deklarieren
        participants.forEach(p -> sb.append("    participant ").append(sanitize(p)).append("\n"));

        // Nachrichtenfluss
        for (AsyncAPIData.Flow flow : data.getFlows()) {
            sb.append("    ")
                    .append(sanitize(flow.from))
                    .append("->>")
                    .append(sanitize(flow.to))
                    .append(": ")
                    .append(flow.message)
                    .append("\n");
        }

        return sb.toString();
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
