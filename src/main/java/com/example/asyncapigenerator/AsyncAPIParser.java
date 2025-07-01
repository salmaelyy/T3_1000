package com.example.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AsyncAPIParser {

    public AsyncAPIData parseYaml(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = mapper.readTree(new File(filePath));

        AsyncAPIData data = new AsyncAPIData();
        JsonNode channels = root.path("channels");

        // 1. Publisher und Subscriber sammeln
        List<Producer> producers = new ArrayList<>();
        List<Consumer> consumers = new ArrayList<>();

        channels.fields().forEachRemaining(entry -> {
            String channelName = entry.getKey();
            JsonNode channel = entry.getValue();

            // Publisher
            channel.fieldNames().forEachRemaining(fieldName -> {
                if (fieldName.startsWith("publish")) {
                    JsonNode publishNode = channel.path(fieldName);
                    String prodOp = publishNode.path("operationId").asText(null);
                    String msgRef = publishNode.path("message").path("$ref").asText();
                    if (prodOp != null && msgRef != null && !msgRef.isEmpty()) {
                        producers.add(new Producer(prodOp, extractMessageName(msgRef)));
                    }
                }
                // Subscriber
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

        // 2. Flows erzeugen: alle Producer-Consumer-Kombis mit gleichem Message-Typ
        for (Producer p : producers) {
            for (Consumer c : consumers) {
                if (p.message.equals(c.message)) {
                    data.addFlow(p.operationId, c.operationId, p.message);
                    System.out.println("  Flow: " + p.operationId + " -> " + c.operationId + " : " + p.message);
                }
            }
        }

        System.out.println("Anzahl extrahierter Flows: " + data.getFlows().size());
        return data;
    }

    // Hilfsklassen:
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
