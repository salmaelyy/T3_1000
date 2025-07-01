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
            if (!flowFound) {
                System.out.println("KEIN FLOW: Es gibt keinen Channel, bei dem ein Producer (publish) und ein Consumer (subscribe) dieselbe Message verwenden. "
                        + "Bitte prüfe, ob mindestens ein Channel sowohl publish als auch subscribe für die gleiche Message hat.");
            }

            System.out.println("Anzahl extrahierter Flows: " + data.getFlows().size());
            if (data.getFlows().isEmpty()) {
                throw new IllegalStateException("KEIN FLOW: Keine Channels in AsyncAPI-Datei gefunden");            }
            return data;
        }

        // AsyncAPI v3.x: Operationen prüfen
        if (version.startsWith("3.")) {
            JsonNode operations = root.path("operations");
            if (operations.isMissingNode() || !operations.fields().hasNext()) {
                System.out.println("KEIN FLOW: AsyncAPI v3.x-Datei enthält keine Operationen. "
                        + "Ohne Operationen (send/receive) können keine Flows extrahiert werden. "
                        + "Bitte ergänze das Feld 'operations' auf Top-Level.");
                throw new IllegalStateException("Keine Operationen in AsyncAPI v3.x-Datei gefunden.");
            }

            // Falls keine Flows entstehen:
            if (data.getFlows().isEmpty()) {
                System.out.println("KEIN FLOW: Es wurden zwar Operationen gefunden, aber keine passenden send/receive-Kombinationen für dieselbe Message und denselben Channel.");
                throw new IllegalStateException("Keine Datenflüsse extrahiert (siehe vorherige Meldung).");
            }
            return data;
        }

        //Andere Versionen
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
