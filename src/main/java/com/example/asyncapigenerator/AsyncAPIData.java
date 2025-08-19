// src/main/java/com/example/asyncapigenerator/AsyncAPIData.java
package com.example.asyncapigenerator;

import java.util.*;

public class AsyncAPIData {
    private String version;
    private String title;
    private String description;

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public static class Flow {
        public final String from;
        public final String to;
        public final String message;

        public Flow(String from, String to, String message) {
            this.from = from;
            this.to = to;
            this.message = message;
        }
    }

    private final List<Flow> flows = new ArrayList<>();
    private final Set<String> flowKeys = new LinkedHashSet<>();

    // NEW: optional notes per participant (e.g., kafka groupId/clientId)
    private final Map<String, LinkedHashSet<String>> participantNotes = new LinkedHashMap<>();

    public void addFlow(String from, String to, String message) {
        String key = from + "->" + to + ":" + message;
        if (flowKeys.add(key)) {
            flows.add(new Flow(from, to, message));
        }
    }

    public List<Flow> getFlows() { return flows; }

    public void addParticipantNote(String participant, String note) {
        if (participant == null || note == null || note.isBlank()) return;
        participantNotes.computeIfAbsent(participant, k -> new LinkedHashSet<>()).add(note);
    }

    public Map<String, LinkedHashSet<String>> getParticipantNotes() {
        return participantNotes;
    }

    public void validateFlows() {
        if (flows.isEmpty()) {
            throw new IllegalStateException(
                    "KEIN FLOW: Keine passenden publish/subscribe (v2) oder send/receive (v3) Kombinationen gefunden."
            );
        }
    }
}