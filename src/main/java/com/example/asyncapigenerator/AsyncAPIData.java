package com.example.asyncapigenerator;

import java.util.*;

public class AsyncAPIData {
    private String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

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

    public List<Flow> getFlows() {
        return flows;
    }

    public void validateFlows() {
        if (flows.isEmpty()) {
            throw new IllegalStateException("Keine Datenflüsse extrahiert");
        }
    }
}
