package com.example.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.*;

/**
 * AsyncAPI v3.x Parser mit Kafka-Bindings-Unterstützung
 * - Liest Metadaten aus info (title, version, description) für Diagramm-Header
 * - Erkennt Kafka groupId/clientId aus bindings.kafka (bei subscribe, publish oder operations)
 * - Fügt diese optional in den Node-Namen ein: "operationId (groupId=..., clientId=...)"
 */
public class AsyncAPIv3Parser {

    public AsyncAPIData parseYaml(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = mapper.readTree(new File(filePath));
        AsyncAPIData data = new AsyncAPIData();

        // >>> Metadaten aus "info"
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

        // ==== Methode 1: Direkt aus channels lesen ====
        JsonNode channels = root.path("channels");
        channels.fields().forEachRemaining(channelEntry -> {
            String channelName = channelEntry.getKey();
            JsonNode channelNode = channelEntry.getValue();

            // subscribe
            JsonNode subscribeOp = channelNode.path("subscribe");
            if (!subscribeOp.isMissingNode()) {
                String opId = subscribeOp.path("operationId").asText("subscribe_" + channelName);
                String msgRef = subscribeOp.path("message").path("$ref").asText();
                String messageName = extractMessageName(msgRef);
                String kafkaInfo = extractKafkaBindingInfo(subscribeOp.path("bindings").path("kafka"));
                receiveOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            }

            // publish
            JsonNode publishOp = channelNode.path("publish");
            if (!publishOp.isMissingNode()) {
                String opId = publishOp.path("operationId").asText("publish_" + channelName);
                String msgRef = publishOp.path("message").path("$ref").asText();
                String messageName = extractMessageName(msgRef);
                String kafkaInfo = extractKafkaBindingInfo(publishOp.path("bindings").path("kafka"));
                sendOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            }
        });

        // ==== Methode 2: Aus operations lesen ====
        JsonNode operations = root.path("operations");
        operations.fields().forEachRemaining(opEntry -> {
            String opId = opEntry.getKey();
            JsonNode opNode = opEntry.getValue();
            String action = opNode.path("action").asText();

            String channelRef = opNode.path("channel").path("$ref").asText();
            if (channelRef.isEmpty()) channelRef = opNode.path("channel").asText();
            String channelName = extractRefName(channelRef);

            String msgRef = opNode.path("message").path("$ref").asText();
            String messageName = extractMessageName(msgRef);

            String kafkaInfo = extractKafkaBindingInfo(opNode.path("bindings").path("kafka"));

            if ("send".equalsIgnoreCase(action)) {
                sendOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            } else if ("receive".equalsIgnoreCase(action)) {
                receiveOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            }
        });

        // ==== Flows erzeugen ====
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

        if (!flowFound) {
            System.out.println("KEIN FLOW: Keine passenden send/receive-Operationen für denselben Channel und dieselbe Message gefunden.");
        }

        System.out.println("Anzahl extrahierter Flows: " + data.getFlows().size());
        if (data.getFlows().isEmpty()) {
            throw new IllegalStateException("KEIN FLOW: Keine kombinierten send/receive-Operationen in AsyncAPI v3.x-Datei gefunden");
        }

        return data;
    }

    // ==== Hilfsklassen & Methoden ====
    static class Operation {
        String operationId, channel, message;
        Operation(String op, String ch, String msg) {
            this.operationId = op;
            this.channel = ch;
            this.message = msg;
        }
    }

    private String extractMessageName(String ref) {
        if (ref != null && ref.contains("/")) {
            return ref.substring(ref.lastIndexOf("/") + 1);
        } else {
            return "UnknownMessage";
        }
    }

    private String extractRefName(String ref) {
        if (ref == null || ref.isEmpty()) return "UnknownChannel";
        if (ref.contains("/")) {
            return ref.substring(ref.lastIndexOf("/") + 1);
        }
        return ref;
    }

    private String extractKafkaBindingInfo(JsonNode kafkaNode) {
        if (kafkaNode.isMissingNode()) return "";
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
