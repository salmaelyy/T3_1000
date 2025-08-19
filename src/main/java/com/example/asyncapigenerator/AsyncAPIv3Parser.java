// src/main/java/com/example/asyncapigenerator/AsyncAPIv3Parser.java
package com.example.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        List<Operation> sendOps = new ArrayList<>();
        List<Operation> receiveOps = new ArrayList<>();

        // Tolerate legacy publish/subscribe under channels (non-standard v3 but appears in examples)
        JsonNode channels = root.path("channels");
        channels.fields().forEachRemaining(channelEntry -> {
            String channelName = channelEntry.getKey();
            JsonNode channelNode = channelEntry.getValue();

            JsonNode subscribeOp = channelNode.path("subscribe");
            if (!subscribeOp.isMissingNode()) {
                String opId = subscribeOp.path("operationId").asText("subscribe_" + channelName);
                String messageName = extractMessage(subscribeOp.path("message"));
                receiveOps.add(new Operation(opId, channelName, messageName));
                String kafkaNote = buildKafkaNote(subscribeOp.path("bindings").path("kafka"));
                if (!kafkaNote.isBlank()) data.addParticipantNote(opId, kafkaNote);
            }

            JsonNode publishOp = channelNode.path("publish");
            if (!publishOp.isMissingNode()) {
                String opId = publishOp.path("operationId").asText("publish_" + channelName);
                String messageName = extractMessage(publishOp.path("message"));
                sendOps.add(new Operation(opId, channelName, messageName));
                String kafkaNote = buildKafkaNote(publishOp.path("bindings").path("kafka"));
                if (!kafkaNote.isBlank()) data.addParticipantNote(opId, kafkaNote);
            }
        });

        // v3 standard: operations
        JsonNode operations = root.path("operations");
        operations.fields().forEachRemaining(opEntry -> {
            String opId = opEntry.getKey();
            JsonNode opNode = opEntry.getValue();
            String action = opNode.path("action").asText();

            String channelRef = opNode.path("channel").path("$ref").asText();
            if (channelRef.isEmpty()) channelRef = opNode.path("channel").asText();
            String channelName = extractRefTail(channelRef);

            String messageName = extractMessage(opNode.path("message"));
            if ("send".equalsIgnoreCase(action)) {
                sendOps.add(new Operation(opId, channelName, messageName));
            } else if ("receive".equalsIgnoreCase(action)) {
                receiveOps.add(new Operation(opId, channelName, messageName));
            }
            // NEW: kafka note on operation
            String kafkaNote = buildKafkaNote(opNode.path("bindings").path("kafka"));
            if (!kafkaNote.isBlank()) data.addParticipantNote(opId, kafkaNote);
        });

        // Build flows (dedup done in AsyncAPIData.addFlow)
        for (Operation send : sendOps) {
            for (Operation recv : receiveOps) {
                if (Objects.equals(send.channel, recv.channel) && Objects.equals(send.message, recv.message)) {
                    data.addFlow(send.operationId, recv.operationId, send.message);
                    System.out.printf("  Flow: %s → %s : %s%n", send.operationId, recv.operationId, send.message);
                }
            }
        }

        System.out.println("Anzahl extrahierter Flows: " + data.getFlows().size());
        data.validateFlows();
        return data;
    }

    static class Operation {
        final String operationId, channel, message;
        Operation(String op, String ch, String msg) { this.operationId = op; this.channel = ch; this.message = msg; }
    }

    private String extractMessage(JsonNode messageNode) {
        if (messageNode == null || messageNode.isMissingNode() || messageNode.isNull()) return "UnknownMessage";
        String ref = messageNode.path("$ref").asText(null);
        if (ref != null) return extractRefTail(ref);
        String name = messageNode.path("name").asText(null);
        if (name != null) return name;
        JsonNode oneOf = messageNode.path("oneOf");
        if (oneOf.isArray() && oneOf.size() > 0) {
            for (JsonNode n : oneOf) {
                String r = n.path("$ref").asText(null);
                if (r != null) return extractRefTail(r);
                String nName = n.path("name").asText(null);
                if (nName != null) return nName;
            }
        }
        return "UnknownMessage";
    }

    private String extractRefTail(String ref) {
        if (ref == null || ref.isEmpty()) return "Unknown";
        int i = ref.lastIndexOf('/');
        return i >= 0 ? ref.substring(i + 1) : ref;
    }

    private String buildKafkaNote(JsonNode kafkaNode) {
        if (kafkaNode == null || kafkaNode.isMissingNode()) return "";
        String groupId  = kafkaNode.path("groupId").asText(null);
        String clientId = kafkaNode.path("clientId").asText(null);
        StringBuilder sb = new StringBuilder();
        if (groupId != null || clientId != null) {
            sb.append("kafka:");
            if (groupId != null)  sb.append(" groupId=").append(groupId);
            if (clientId != null) sb.append(groupId != null ? ", " : " ").append("clientId=").append(clientId);
        }
        return sb.toString();
    }
}