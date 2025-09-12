package com.example.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.*;

public class AsyncAPIParser {

    public AsyncAPIData parseYaml(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = mapper.readTree(new File(filePath)); //lies die datei als baum ein
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
            throw new IllegalStateException("AsyncAPI ab v2.x werden unterstützt.");
        }
        if (specVersion.startsWith("3.")) {
            // v3 an Spezialparser delegieren
            return new AsyncAPIv3Parser().parseYaml(filePath);
        }
        if (!specVersion.startsWith("2.")) {
            throw new IllegalStateException("Unbekannte AsyncAPI-Version: " + specVersion);
        }

        JsonNode channels = root.path("channels");
        List<Producer> producers = new ArrayList<>(); //alle prod
        List<Consumer> consumers = new ArrayList<>();
        Map<String, List<Producer>> channelPublishes = new LinkedHashMap<>(); //prod innerhalb des channels
        //Map<String, List<Consumer>> channelSubscribes = new LinkedHashMap<>(); //channel name und entsprechende cons
        final var channelSubscribes = new LinkedHashMap<String, List<Consumer>>(); //channel name und entsprechende cons


        channels.fields().forEachRemaining(entry -> {
            String channelName = entry.getKey();
            JsonNode channel = entry.getValue();

            channel.fieldNames().forEachRemaining(fieldName -> {
                if (fieldName.startsWith("publish")) {
                    JsonNode publishNode = channel.path(fieldName);
                    String opId  = valueOrNull(publishNode.path("operationId"));
                    String msgRef = valueOrNull(publishNode.path("message").path("$ref"));
                    String msgName = extractMessageName(msgRef);
                    msgName = appendKafkaInfo(publishNode, msgName); //kafka bindings zb client id

                    if (opId != null && msgName != null) {
                        Producer p = new Producer(opId, msgName, channelName);
                        producers.add(p);
                        channelPublishes.computeIfAbsent(channelName, k -> new ArrayList<>()).add(p);
                    }
                } else if (fieldName.startsWith("subscribe")) {
                    JsonNode subscribeNode = channel.path(fieldName);
                    String opId  = valueOrNull(subscribeNode.path("operationId"));
                    String msgRef = valueOrNull(subscribeNode.path("message").path("$ref"));
                    String msgName = extractMessageName(msgRef);
                    msgName = appendKafkaInfo(subscribeNode, msgName);

                    if (opId != null && msgName != null) {
                        Consumer c = new Consumer(opId, msgName, channelName);
                        consumers.add(c);
                        channelSubscribes.computeIfAbsent(channelName, k -> new ArrayList<>()).add(c); //Sucht in der Map nach channelName
                    }
                }
            });
        });

        Set<String> seen = new LinkedHashSet<>();

        for (Producer p : producers) {
            for (Consumer c : consumers) {
                if (p.channel.equals(c.channel) && p.message.equals(c.message)) {
                    String key = p.operationId + "->" + c.operationId + ":" + p.message;
                    if (seen.add(key)) { //true wenn neu
                        data.addFlow(p.operationId, c.operationId, p.message);
                    }
                }
            }
        }

        // Topic-Bridging
        // iteration pro channel über alle prod in publish und füge kante hinzu
        for (Map.Entry<String, List<Producer>> e : channelPublishes.entrySet()) {
            String topicNode = "topic:" + e.getKey();
            for (Producer p : e.getValue()) {
                if (seen.add(p.operationId + "->" + topicNode + ":" + p.message)) {
                    data.addFlow(p.operationId, topicNode, p.message);
                }
            }
        }
        for (Map.Entry<String, List<Consumer>> e : channelSubscribes.entrySet()) {
            String topicNode = "topic:" + e.getKey();
            for (Consumer c : e.getValue()) {
                if (seen.add(topicNode + "->" + c.operationId + ":" + c.message)) {
                    data.addFlow(topicNode, c.operationId, c.message);
                }
            }
        }
        data.validateFlows();
        return data;
    }

    private String appendKafkaInfo(JsonNode operationNode, String messageName) {
        JsonNode kafkaBindings = operationNode.path("bindings").path("kafka");
        if (!kafkaBindings.isMissingNode()) {
            String groupId = valueOrNull(kafkaBindings.path("groupId"));
            String clientId = valueOrNull(kafkaBindings.path("clientId"));
            StringBuilder sb = new StringBuilder(messageName);
            boolean hasInfo = false;
            if (groupId != null) { sb.append(" (groupId=").append(groupId); hasInfo = true; }
            if (clientId != null) { sb.append(hasInfo ? ", clientId=" : " (clientId=").append(clientId); hasInfo = true; }
            if (hasInfo) sb.append(")");
            return sb.toString();
        }
        return messageName;
    }

    // ==== Hilfen ====
    static class Producer {
        String operationId, message, channel;
        Producer(String op, String msg, String ch) { this.operationId = op; this.message = msg; this.channel = ch; }
    }
    static class Consumer {
        String operationId, message, channel;
        Consumer(String op, String msg, String ch) { this.operationId = op; this.message = msg; this.channel = ch; }
    }

    //"#/components/messages/OrderCreated" -> "OrderCreated"
    private String extractMessageName(String messageRef) {
        if (messageRef == null || messageRef.isEmpty()) return null;
        int i = messageRef.lastIndexOf('/');
        return i >= 0 ? messageRef.substring(i + 1) : messageRef;
    }

    private String valueOrNull(JsonNode n) {
        return (n == null || n.isMissingNode() || n.isNull()) ? null : n.asText(null);
    }
}