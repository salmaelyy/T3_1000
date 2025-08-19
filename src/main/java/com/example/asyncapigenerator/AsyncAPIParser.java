// src/main/java/com/example/asyncapigenerator/AsyncAPIParser.java
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

        if (root.has("info")) {
            JsonNode info = root.get("info");
            if (info.has("title"))       data.setTitle(info.get("title").asText());
            if (info.has("version"))     data.setVersion(info.get("version").asText());
            if (info.has("description")) data.setDescription(info.get("description").asText());
        }

        String specVersion = root.path("asyncapi").asText();
        System.out.println("Gefundene AsyncAPI-Spezifikation: " + specVersion);

        if (specVersion.startsWith("1.")) {
            throw new IllegalStateException("AsyncAPI v1.x wird nicht unterstützt. Bitte v2.x oder v3.x verwenden.");
        }
        if (specVersion.startsWith("3.")) {
            return new AsyncAPIv3Parser().parseYaml(filePath);
        }
        if (!specVersion.startsWith("2.")) {
            throw new IllegalStateException("Unbekannte AsyncAPI-Version: " + specVersion);
        }

        JsonNode channels = root.path("channels");
        List<Producer> producers = new ArrayList<>();
        List<Consumer> consumers = new ArrayList<>();
        Map<String, List<Producer>> channelPublishes = new LinkedHashMap<>();
        Map<String, List<Consumer>> channelSubscribes = new LinkedHashMap<>();

        channels.fields().forEachRemaining(entry -> {
            String channelName = entry.getKey();
            JsonNode channel = entry.getValue();

            channel.fieldNames().forEachRemaining(fieldName -> {
                if (fieldName.startsWith("publish")) {
                    JsonNode publishNode = channel.path(fieldName);
                    String opId   = valueOrNull(publishNode.path("operationId"));
                    String msgRef = valueOrNull(publishNode.path("message").path("$ref"));
                    String msgName = msgRef != null ? extractMessageName(msgRef)
                            : extractInlineMessageName(publishNode.path("message"));
                    // NEW: kafka note
                    String kafkaNote = buildKafkaNote(publishNode.path("bindings").path("kafka"));

                    if (opId != null && msgName != null) {
                        Producer p = new Producer(opId, msgName, channelName);
                        producers.add(p);
                        channelPublishes.computeIfAbsent(channelName, k -> new ArrayList<>()).add(p);
                        if (!kafkaNote.isBlank()) data.addParticipantNote(opId, kafkaNote);
                    }
                } else if (fieldName.startsWith("subscribe")) {
                    JsonNode subscribeNode = channel.path(fieldName);
                    String opId   = valueOrNull(subscribeNode.path("operationId"));
                    String msgRef = valueOrNull(subscribeNode.path("message").path("$ref"));
                    String msgName = msgRef != null ? extractMessageName(msgRef)
                            : extractInlineMessageName(subscribeNode.path("message"));
                    // NEW: kafka note
                    String kafkaNote = buildKafkaNote(subscribeNode.path("bindings").path("kafka"));

                    if (opId != null && msgName != null) {
                        Consumer c = new Consumer(opId, msgName, channelName);
                        consumers.add(c);
                        channelSubscribes.computeIfAbsent(channelName, k -> new ArrayList<>()).add(c);
                        if (!kafkaNote.isBlank()) data.addParticipantNote(opId, kafkaNote);
                    }
                }
            });
        });

        // Service -> Service (gleiche Message)
        for (Producer p : producers) {
            for (Consumer c : consumers) {
                if (Objects.equals(p.message, c.message)) {
                    data.addFlow(p.operationId, c.operationId, p.message);
                }
            }
        }
        // Service -> Topic
        for (Map.Entry<String, List<Producer>> e : channelPublishes.entrySet()) {
            String topicNode = "topic:" + e.getKey();
            for (Producer p : e.getValue()) {
                data.addFlow(p.operationId, topicNode, p.message);
            }
        }
        // Topic -> Service
        for (Map.Entry<String, List<Consumer>> e : channelSubscribes.entrySet()) {
            String topicNode = "topic:" + e.getKey();
            for (Consumer c : e.getValue()) {
                data.addFlow(topicNode, c.operationId, c.message);
            }
        }

        data.validateFlows();
        return data;
    }

    // NEW: build a compact kafka note like "kafka: groupId=foo, clientId=bar"
    private String buildKafkaNote(JsonNode kafkaNode) {
        if (kafkaNode == null || kafkaNode.isMissingNode()) return "";
        String groupId  = valueOrNull(kafkaNode.path("groupId"));
        String clientId = valueOrNull(kafkaNode.path("clientId"));
        StringBuilder sb = new StringBuilder();
        if (groupId != null || clientId != null) {
            sb.append("kafka:");
            if (groupId != null)  sb.append(" groupId=").append(groupId);
            if (clientId != null) sb.append(groupId != null ? ", " : " ").append("clientId=").append(clientId);
        }
        return sb.toString();
    }

    static class Producer {
        final String operationId, message, channel;
        Producer(String op, String msg, String ch) { this.operationId = op; this.message = msg; this.channel = ch; }
    }
    static class Consumer {
        final String operationId, message, channel;
        Consumer(String op, String msg, String ch) { this.operationId = op; this.message = msg; this.channel = ch; }
    }

    private String extractMessageName(String messageRef) {
        if (messageRef == null || messageRef.isEmpty()) return null;
        int i = messageRef.lastIndexOf('/');
        return i >= 0 ? messageRef.substring(i + 1) : messageRef;
    }

    private String extractInlineMessageName(JsonNode messageNode) {
        if (messageNode == null || messageNode.isMissingNode() || messageNode.isNull()) return null;
        String name = valueOrNull(messageNode.path("name"));
        if (name != null) return name;
        JsonNode oneOf = messageNode.path("oneOf");
        if (oneOf.isArray() && oneOf.size() > 0) {
            for (JsonNode n : oneOf) {
                String ref = valueOrNull(n.path("$ref"));
                if (ref != null) return extractMessageName(ref);
            }
        }
        return "Message";
    }

    private String valueOrNull(JsonNode n) {
        return (n == null || n.isMissingNode() || n.isNull()) ? null : n.asText(null);
    }
}