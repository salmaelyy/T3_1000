// src/main/java/com/example/asyncapigenerator/AsyncAPIv3Parser.java
package com.example.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AsyncAPI v3.x Parser mit Kafka-Bindings-Unterstützung
 * - Liest Metadaten aus info (title, version, description)
 * - Erkennt Kafka groupId/clientId aus bindings.kafka
 * - Robust gegen inline message-Definitionen/oneOf
 * - Vermeidet Doppelflows
 */
public class AsyncAPIv3Parser {

    public AsyncAPIData parseYaml(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = mapper.readTree(new File(filePath));
        AsyncAPIData data = new AsyncAPIData();

        // Metadaten
        if (root.has("info")) {
            JsonNode info = root.get("info");
            if (info.has("title")) data.setTitle(info.get("title").asText());
            if (info.has("version")) data.setVersion(info.get("version").asText());
            if (info.has("description")) data.setDescription(info.get("description").asText());
        }

        // KEIN erneutes „Gefundene AsyncAPI…“ – das macht der Aufrufer schon

        List<Operation> sendOps = new ArrayList<>();
        List<Operation> receiveOps = new ArrayList<>();

        // (Optional) Legacy-ähnlich: v3-Channels könnten publish/subscribe NICHT mehr haben,
        // aber wenn doch (gemischte Beispiele), tolerieren wir es:
        JsonNode channels = root.path("channels");
        channels.fields().forEachRemaining(channelEntry -> {
            String channelName = channelEntry.getKey();
            JsonNode channelNode = channelEntry.getValue();

            // subscribe (legacy-stil, nicht v3-Standard – aber toleriert)
            JsonNode subscribeOp = channelNode.path("subscribe");
            if (!subscribeOp.isMissingNode()) {
                String opId = subscribeOp.path("operationId").asText("subscribe_" + channelName);
                String messageName = extractMessage(subscribeOp.path("message"));
                String kafkaInfo = extractKafkaBindingInfo(subscribeOp.path("bindings").path("kafka"));
                receiveOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            }

            // publish
            JsonNode publishOp = channelNode.path("publish");
            if (!publishOp.isMissingNode()) {
                String opId = publishOp.path("operationId").asText("publish_" + channelName);
                String messageName = extractMessage(publishOp.path("message"));
                String kafkaInfo = extractKafkaBindingInfo(publishOp.path("bindings").path("kafka"));
                sendOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            }
        });

        // v3-Standard: operations
        JsonNode operations = root.path("operations");
        operations.fields().forEachRemaining(opEntry -> {
            String opId = opEntry.getKey();
            JsonNode opNode = opEntry.getValue();
            String action = opNode.path("action").asText();

            String channelRef = opNode.path("channel").path("$ref").asText();
            if (channelRef.isEmpty()) channelRef = opNode.path("channel").asText();
            String channelName = extractRefName(channelRef);

            String messageName = extractMessage(opNode.path("message"));
            String kafkaInfo = extractKafkaBindingInfo(opNode.path("bindings").path("kafka"));

            if ("send".equalsIgnoreCase(action)) {
                sendOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            } else if ("receive".equalsIgnoreCase(action)) {
                receiveOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            }
        });

        // Flows erzeugen (dedupliziert via AsyncAPIData.addFlow)
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

    // ==== Hilfsklassen & Methoden ====
    static class Operation {
        final String operationId, channel, message;
        Operation(String op, String ch, String msg) {
            this.operationId = op;
            this.channel = ch;
            this.message = msg;
        }
    }

    private String extractMessage(JsonNode messageNode) {
        if (messageNode == null || messageNode.isMissingNode() || messageNode.isNull()) return "UnknownMessage";
        // $ref
        String ref = messageNode.path("$ref").asText(null);
        if (ref != null) return extractRefTail(ref);
        // name
        String name = messageNode.path("name").asText(null);
        if (name != null) return name;
        // oneOf -> nimm ersten referenzierten Namen
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

    private String extractRefName(String ref) {
        if (ref == null || ref.isEmpty()) return "UnknownChannel";
        return extractRefTail(ref);
    }

    private String extractRefTail(String ref) {
        int i = ref.lastIndexOf('/');
        return i >= 0 ? ref.substring(i + 1) : ref;
    }

    private String extractKafkaBindingInfo(JsonNode kafkaNode) {
        if (kafkaNode == null || kafkaNode.isMissingNode()) return "";
        String groupId = kafkaNode.path("groupId").asText(null);
        String clientId = kafkaNode.path("clientId").asText(null);

        StringBuilder sb = new StringBuilder();
        if (groupId != null || clientId != null) {
            sb.append(" (");
            if (groupId != null) sb.append("groupId=").append(groupId);
            if (clientId != null) {
                if (groupId != null) sb.append(", ");
                sb.append("clientId=").append(clientId);
            }
            sb.append(")");
        }
        return sb.toString();
    }
}