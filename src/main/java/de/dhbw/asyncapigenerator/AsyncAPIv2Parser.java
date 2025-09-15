package de.dhbw.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

public class AsyncAPIv2Parser {

    record Producer(String operationId, String message, String channel) {}
    record Consumer(String operationId, String message, String channel) {}

    public AsyncAPIData parseYaml(JsonNode root, AsyncAPIData data) {

        JsonNode channels = root.path("channels");
        if (!channels.isObject() || channels.size() == 0) {
            data.validateFlows();
            return data;
        }
        List<Producer> producers = new ArrayList<>(); //alle prod
        List<Consumer> consumers = new ArrayList<>();
        Map<String, List<Producer>> channelPublishes = new LinkedHashMap<>(); //prod innerhalb des channels
        Map<String, List<Consumer>> channelSubscribes = new LinkedHashMap<>(); //channel name und entsprechende cons


        channels.fields().forEachRemaining(entry -> {
            String channelName = entry.getKey();
            JsonNode channel = entry.getValue();

            channel.fieldNames().forEachRemaining(fieldName -> {
                if (fieldName.startsWith("publish")) {
                    JsonNode publishNode = channel.path(fieldName);
                    String opId = AsyncAPIParseUtils.valueOrNull(publishNode.path("operationId"));
                    if (opId == null || opId.isBlank()) opId = channelName + " [" + fieldName + "]";
                    String msgRef = AsyncAPIParseUtils.valueOrNull(publishNode.path("message").path("$ref"));
                    String msgName = AsyncAPIParseUtils.extractMessageName(msgRef);
                    msgName = appendKafkaInfo(publishNode, msgName); //kafka bindings zb client id

                    if (opId != null && msgName != null) {
                        Producer p = new Producer(opId, msgName, channelName);
                        producers.add(p);
                        channelPublishes.computeIfAbsent(channelName, k -> new ArrayList<>()).add(p);
                    }
                } else if (fieldName.startsWith("subscribe")) {
                    JsonNode subscribeNode = channel.path(fieldName);
                    String opId = AsyncAPIParseUtils.valueOrNull(subscribeNode.path("operationId"));
                    String msgRef = AsyncAPIParseUtils.valueOrNull(subscribeNode.path("message").path("$ref"));
                    String msgName = AsyncAPIParseUtils.extractMessageName(msgRef);
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
            String groupId = AsyncAPIParseUtils.valueOrNull(kafkaBindings.path("groupId"));
            String clientId = AsyncAPIParseUtils.valueOrNull(kafkaBindings.path("clientId"));
            StringBuilder sb = new StringBuilder(messageName);
            boolean hasInfo = false;
            if (groupId != null) {
                sb.append(" (groupId=").append(groupId);
                hasInfo = true;
            }
            if (clientId != null) {
                sb.append(hasInfo ? ", clientId=" : " (clientId=").append(clientId);
                hasInfo = true;
            }
            if (hasInfo) sb.append(")");
            return sb.toString();
        }
        return messageName;
    }
}


