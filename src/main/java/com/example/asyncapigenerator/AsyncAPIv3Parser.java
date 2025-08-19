package com.example.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.*;

/**
 * AsyncAPI v3.x Parser
 * - Liest Metadaten
 * - Erzeugt Flows aus operations (send/receive) und ggf. aus channels.publish/subscribe (falls vorhanden)
 * - Merged Kafka-Bindings aus Operation UND Channel (Operation > Channel)
 */
public class AsyncAPIv3Parser {

    public AsyncAPIData parseYaml(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = mapper.readTree(new File(filePath));
        AsyncAPIData data = new AsyncAPIData();

        if (root.has("info")) {
            JsonNode info = root.get("info");
            if (info.has("title")) data.setTitle(info.get("title").asText());
            if (info.has("version")) data.setVersion(info.get("version").asText());
            if (info.has("description")) data.setDescription(info.get("description").asText());
        }

        String specVersion = root.path("asyncapi").asText();
        System.out.println("Gefundene AsyncAPI-Spezifikation: " + specVersion);

        List<Operation> sendOps = new ArrayList<>();
        List<Operation> receiveOps = new ArrayList<>();

        // --- v3 channels (manche Modelle tragen trotzdem publish/subscribe) ---
        JsonNode channels = root.path("channels");
        channels.fields().forEachRemaining(channelEntry -> {
            String channelName = channelEntry.getKey();
            JsonNode channelNode = channelEntry.getValue();
            JsonNode channelKafka = channelNode.path("bindings").path("kafka");

            JsonNode subscribeOp = channelNode.path("subscribe");
            if (!subscribeOp.isMissingNode()) {
                String opId = subscribeOp.path("operationId").asText("subscribe_" + channelName);
                String msgRef = subscribeOp.path("message").path("$ref").asText();
                String messageName = extractMessageName(msgRef);
                String kafkaInfo = mergeKafkaBindingInfo(subscribeOp.path("bindings").path("kafka"), channelKafka);
                receiveOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            }

            JsonNode publishOp = channelNode.path("publish");
            if (!publishOp.isMissingNode()) {
                String opId = publishOp.path("operationId").asText("publish_" + channelName);
                String msgRef = publishOp.path("message").path("$ref").asText();
                String messageName = extractMessageName(msgRef);
                String kafkaInfo = mergeKafkaBindingInfo(publishOp.path("bindings").path("kafka"), channelKafka);
                sendOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            }
        });

        // --- v3 operations (send/receive, referenzieren channel) ---
        JsonNode operations = root.path("operations");
        operations.fields().forEachRemaining(opEntry -> {
            String opId = opEntry.getKey();
            JsonNode opNode = opEntry.getValue();
            String action = opNode.path("action").asText();

            String channelRef = opNode.path("channel").path("$ref").asText();
            if (channelRef.isEmpty()) channelRef = opNode.path("channel").asText();
            String channelName = extractRefName(channelRef);

            // ggf. Channel-Bindings ziehen
            JsonNode channelNode = channels.path(channelName);
            JsonNode channelKafka = channelNode.path("bindings").path("kafka");

            String msgRef = opNode.path("message").path("$ref").asText();
            String messageName = extractMessageName(msgRef);

            // Merge: op.kafka > channel.kafka
            String kafkaInfo = mergeKafkaBindingInfo(opNode.path("bindings").path("kafka"), channelKafka);

            if ("send".equalsIgnoreCase(action)) {
                sendOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            } else if ("receive".equalsIgnoreCase(action)) {
                receiveOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            }
        });

        boolean flowFound = false;
        for (Operation send : sendOps) {
            for (Operation recv : receiveOps) {
                if (send.channel.equals(recv.channel) && send.message.equals(recv.message)) {
                    data.addFlow(send.operationId, recv.operationId, send.message);
                    System.out.printf("  Flow: %s → %s : %s%n", send.operationId, recv.operationId, send.message);
                    flowFound = true;
                }
            }
        }

        System.out.println("Anzahl extrahierter Flows: " + data.getFlows().size());
        data.validateFlows();
        return data;
    }

    // ==== Hilfen ====
    static class Operation {
        String operationId, channel, message;
        Operation(String op, String ch, String msg) { this.operationId = op; this.channel = ch; this.message = msg; }
    }

    private String extractMessageName(String ref) {
        if (ref != null && ref.contains("/")) return ref.substring(ref.lastIndexOf("/") + 1);
        return (ref == null || ref.isBlank()) ? "UnknownMessage" : ref;
    }

    private String extractRefName(String ref) {
        if (ref == null || ref.isEmpty()) return "UnknownChannel";
        if (ref.contains("/")) return ref.substring(ref.lastIndexOf("/") + 1);
        return ref;
    }

    /** Merge op.kafka > channel.kafka; erzeugt " (groupId=..., clientId=...)" oder "" */
    private String mergeKafkaBindingInfo(JsonNode opKafka, JsonNode channelKafka) {
        String groupId = firstNonNull(opKafka.path("groupId").asText(null),
                channelKafka.isMissingNode() ? null : channelKafka.path("groupId").asText(null));
        String clientId = firstNonNull(opKafka.path("clientId").asText(null),
                channelKafka.isMissingNode() ? null : channelKafka.path("clientId").asText(null));

        if (groupId == null && clientId == null) return "";
        StringBuilder sb = new StringBuilder(" (");
        boolean first = true;
        if (groupId != null) { sb.append("groupId=").append(groupId); first = false; }
        if (clientId != null) { if (!first) sb.append(", "); sb.append("clientId=").append(clientId); }
        sb.append(")");
        return sb.toString();
    }

    private String firstNonNull(String a, String b) { return a != null ? a : b; }
}