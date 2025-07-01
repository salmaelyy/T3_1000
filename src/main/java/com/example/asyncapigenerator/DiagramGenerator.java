package com.example.asyncapigenerator;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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

        // Aliase generieren
        Map<String, String> nameToAlias = new HashMap<>();
        Map<String, Integer> aliasCount = new HashMap<>();
        for (String name : participants) {
            String alias = shortenName(name);
            // Bei Dopplung: Zähler anhängen
            if (nameToAlias.containsValue(alias)) {
                int count = aliasCount.getOrDefault(alias, 1) + 1;
                aliasCount.put(alias, count);
                alias = alias + count;
            } else {
                aliasCount.put(alias, 1);
            }
            nameToAlias.put(name, alias);
            // Mermaid-Teilnehmer mit Alias
            sb.append("    participant ").append(alias).append(" as \"").append(alias).append("\"\n");
        }

        // Nachrichtenfluss mit Alias
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

    // Hilfsmethode zur Kürzung und Sanitisierung
    private String shortenName(String name) {
        // Beispiel: "authService_publishUserSignedUp" -> "AuthPubUserSignedUp"
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                // Nimm die ersten 3 Buchstaben groß geschrieben
                if (part.length() > 4) {
                    sb.append(Character.toUpperCase(part.charAt(0)))
                            .append(part.substring(1, 4));
                } else {
                    sb.append(Character.toUpperCase(part.charAt(0)))
                            .append(part.substring(1));
                }
            }
        }
        String result = sb.toString();
        // Wenn der Name schon kurz ist, gib ihn zurück
        result = result.length() < 8 ? name : result;
        // Sanitize für Mermaid
        return result.replaceAll("[^a-zA-Z0-9_]", "_");
    }


}
