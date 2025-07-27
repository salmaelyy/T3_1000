package com.example.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.*;

public class AsyncAPIParser {

    public AsyncAPIData parseYaml(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = mapper.readTree(new File(filePath));
        AsyncAPIData data = new AsyncAPIData();

        String version = root.path("asyncapi").asText();
        data.setVersion(version);
        System.out.println("Gefundene AsyncAPI-Version: " + version);

        String pink = "\u001B[35m";
        String reset = "\u001B[0m";

        // AsyncAPI v1.x
        if (version.startsWith("1.")) {
            System.out.println("WARNUNG: AsyncAPI v1.x wird nicht unterstützt.");
            throw new IllegalStateException("AsyncAPI ab v2.x werden unterstützt.");
        }

        // AsyncAPI v2.x
        if (version.startsWith("2.")) {
            List<Producer> producers = new ArrayList<>();
            List<Consumer> consumers = new ArrayList<>();
            JsonNode channels = root.path("channels");

            channels.fields().forEachRemaining(entry -> {
                JsonNode channel = entry.getValue();
                channel.fieldNames().forEachRemaining(fieldName -> {
                    if (fieldName.startsWith("publish")) {
                        JsonNode publishNode = channel.path(fieldName);
                        String prodOp = publishNode.path("operationId").asText(null);
                        String msgRef = publishNode.path("message").path("$ref").asText();
                        if (prodOp != null && msgRef != null && !msgRef.isEmpty()) {
                            producers.add(new Producer(prodOp, extractMessageName(msgRef)));
                        }
                    }
                    if (fieldName.startsWith("subscribe")) {
                        JsonNode subscribeNode = channel.path(fieldName);
                        String consOp = subscribeNode.path("operationId").asText(null);
                        String msgRef = subscribeNode.path("message").path("$ref").asText();
                        if (consOp != null && msgRef != null && !msgRef.isEmpty()) {
                            consumers.add(new Consumer(consOp, extractMessageName(msgRef)));
                        }
                    }
                });
            });

            boolean flowFound = false;
            for (Producer p : producers) {
                for (Consumer c : consumers) {
                    if (p.message.equals(c.message)) {
                        data.addFlow(p.operationId, c.operationId, p.message);
                        System.out.println("  Flow: " + p.operationId + " -> " + c.operationId + " : " + p.message);
                        flowFound = true;
                    }
                }
            }

            System.out.println("Anzahl extrahierter Flows: " + data.getFlows().size());

            if (data.getFlows().isEmpty()) {
                System.out.println(pink + "Keine gültigen Channels mit publish + subscribe gefunden." + reset);
                throw new IllegalStateException("KEIN FLOW: Keine gültigen Channels mit publish + subscribe gefunden.");
            }

            return data;
        }

        // AsyncAPI v3.x
        if (version.startsWith("3.")) {
            return new AsyncAPIv3Parser().parseYaml(filePath);
        }

        System.out.println("WARNUNG: Unbekannte AsyncAPI-Version: " + version + ". Es wird kein Flow extrahiert.");
        throw new IllegalStateException("Unbekannte AsyncAPI-Version: " + version);
    }

    static class Producer {
        String operationId, message;
        Producer(String op, String msg) { this.operationId = op; this.message = msg; }
    }

    static class Consumer {
        String operationId, message;
        Consumer(String op, String msg) { this.operationId = op; this.message = msg; }
    }

    private String extractMessageName(String messageRef) {
        if (messageRef.contains("/")) {
            return messageRef.substring(messageRef.lastIndexOf("/") + 1);
        } else {
            return "UnknownMessage";
        }
    }
}
//TODO: maybe metadaten in die diagramme?