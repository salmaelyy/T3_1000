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
        public String from;
        public String to;
        public String message;

        public Flow(String from, String to, String message) {
            this.from = from;
            this.to = to;
            this.message = message;
        }
    }

    private final List<Flow> flows = new ArrayList<>();

    public void addFlow(String from, String to, String message) {
        flows.add(new Flow(from, to, message));
    }

    public List<Flow> getFlows() { return flows; }

    public void validateFlows() {
        if (flows.isEmpty()) {
            throw new IllegalStateException(
                    "KEIN FLOW: Keine passenden publish/subscribe (v2) oder send/receive (v3) Kombinationen gefunden."
            );
        }
    }
}