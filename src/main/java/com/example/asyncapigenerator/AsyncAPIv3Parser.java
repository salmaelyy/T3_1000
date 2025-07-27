package com.example.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.*;

public class AsyncAPIv3Parser {

    public AsyncAPIData parseYaml(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = mapper.readTree(new File(filePath));
        AsyncAPIData data = new AsyncAPIData();

        String version = root.path("asyncapi").asText();
        data.setVersion(version);

        List<Operation> sendOps = new ArrayList<>();
        List<Operation> receiveOps = new ArrayList<>();

        // Methode 1: Aus channels direkt lesen
        JsonNode channels = root.path("channels");
        channels.fields().forEachRemaining(channelEntry -> {
            String channelName = channelEntry.getKey();
            JsonNode channelNode = channelEntry.getValue();

            JsonNode subscribeOp = channelNode.path("subscribe");
            if (!subscribeOp.isMissingNode()) {
                String opId = subscribeOp.path("operationId").asText("subscribe_" + channelName);
                String msgRef = subscribeOp.path("message").path("$ref").asText();
                String messageName = extractMessageName(msgRef);
                receiveOps.add(new Operation(opId, channelName, messageName));
            }

            JsonNode publishOp = channelNode.path("publish");
            if (!publishOp.isMissingNode()) {
                String opId = publishOp.path("operationId").asText("publish_" + channelName);
                String msgRef = publishOp.path("message").path("$ref").asText();
                String messageName = extractMessageName(msgRef);
                sendOps.add(new Operation(opId, channelName, messageName));
            }
        });

        // Methode 2: Aus operations lesen (falls vorhanden)
        JsonNode operations = root.path("operations");
        operations.fields().forEachRemaining(opEntry -> {
            String opId = opEntry.getKey();
            JsonNode opNode = opEntry.getValue();
            String action = opNode.path("action").asText(); // "send" oder "receive"

            String channelRef = opNode.path("channel").path("$ref").asText();
            if (channelRef.isEmpty()) channelRef = opNode.path("channel").asText(); // fallback
            String channelName = extractRefName(channelRef);

            String msgRef = opNode.path("message").path("$ref").asText();
            String messageName = extractMessageName(msgRef);

            if ("send".equalsIgnoreCase(action)) {
                sendOps.add(new Operation(opId, channelName, messageName));
            } else if ("receive".equalsIgnoreCase(action)) {
                receiveOps.add(new Operation(opId, channelName, messageName));
            }
        });

        boolean flowFound = false;
        for (Operation send : sendOps) {
            for (Operation recv : receiveOps) {
                if (send.channel.equals(recv.channel) && send.message.equals(recv.message)) {
                    data.addFlow(send.operationId, recv.operationId, send.message);
                    System.out.printf("  Flow: %s → %s : %s\n", send.operationId, recv.operationId, send.message);
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
}