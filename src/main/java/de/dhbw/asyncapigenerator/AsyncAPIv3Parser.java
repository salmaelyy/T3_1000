package de.dhbw.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

public class AsyncAPIv3Parser {

    record Operation(String operationId, String channel, String message){}

    public AsyncAPIData parseYaml(JsonNode root, AsyncAPIData data) {

        if (root.has("info")) {
            JsonNode info = root.get("info");
            if (info.has("title")) data.setTitle(info.get("title").asText());
            if (info.has("version")) data.setVersion(info.get("version").asText());
            if (info.has("description")) data.setDescription(info.get("description").asText());
        }

        List<Operation> sendOps = new ArrayList<>(); //sammler listen für send und receive operationen
        List<Operation> receiveOps = new ArrayList<>();

        // v3 channels (manche Modelle tragen trotzdem publish/subscribe)
        JsonNode channels = root.path("channels");
        channels.fields().forEachRemaining(channelEntry -> {
            String channelName = channelEntry.getKey();
            JsonNode channelNode = channelEntry.getValue();
            JsonNode channelKafka = channelNode.path("bindings").path("kafka");

            JsonNode subscribeOp = channelNode.path("subscribe");
            if (!subscribeOp.isMissingNode()) {
                String opId = subscribeOp.path("operationId").asText("subscribe_" + channelName); //fallback
                String msgRef = subscribeOp.path("message").path("$ref").asText();
                String messageName = AsyncAPIParseUtils.extractMessageName(msgRef);
                String kafkaInfo = mergeKafkaBindingInfo(subscribeOp.path("bindings").path("kafka"), channelKafka);
                receiveOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            }

            JsonNode publishOp = channelNode.path("publish");
            if (!publishOp.isMissingNode()) {
                String opId = publishOp.path("operationId").asText("publish_" + channelName);
                String msgRef = publishOp.path("message").path("$ref").asText();
                String messageName = AsyncAPIParseUtils.extractMessageName(msgRef);
                String kafkaInfo = mergeKafkaBindingInfo(publishOp.path("bindings").path("kafka"), channelKafka);
                sendOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            }
        });

        JsonNode operations = root.path("operations");
        operations.fields().forEachRemaining(opEntry -> {
            String opId = opEntry.getKey();
            JsonNode opNode = opEntry.getValue();
            String action = opNode.path("action").asText();

            String channelRef = opNode.path("channel").path("$ref").asText();
            if (channelRef.isEmpty()) channelRef = opNode.path("channel").asText();
            String channelName = AsyncAPIParseUtils.extractRefName(channelRef);

            JsonNode channelNode = channels.path(channelName);
            JsonNode channelKafka = channelNode.path("bindings").path("kafka");

            String msgRef = opNode.path("message").path("$ref").asText();
            String messageName = AsyncAPIParseUtils.extractMessageName(msgRef);

            // op.kafka > channel.kafka
            String kafkaInfo = mergeKafkaBindingInfo(opNode.path("bindings").path("kafka"), channelKafka);

            if ("send".equalsIgnoreCase(action)) {
                sendOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            } else if ("receive".equalsIgnoreCase(action)) {
                receiveOps.add(new Operation(opId + kafkaInfo, channelName, messageName));
            }
        });

        for (Operation send : sendOps) {
            for (Operation recv : receiveOps) {
                if (send.channel.equals(recv.channel) && send.message.equals(recv.message)) {
                    data.addFlow(send.operationId, recv.operationId, send.message);
                }
            }
        }
        data.validateFlows();
        return data;
    }

    // opKafka>channelKafka
    private String mergeKafkaBindingInfo(JsonNode opKafka, JsonNode channelKafka) {
        String groupId = AsyncAPIParseUtils.firstNonNull(opKafka.path("groupId").asText(null),
                channelKafka.isMissingNode() ? null : channelKafka.path("groupId").asText(null));
        String clientId = AsyncAPIParseUtils.firstNonNull(opKafka.path("clientId").asText(null),
                channelKafka.isMissingNode() ? null : channelKafka.path("clientId").asText(null));

        if (groupId == null && clientId == null) return "";
        StringBuilder sb = new StringBuilder(" (");
        boolean first = true;
        if (groupId != null) { sb.append("groupId=").append(groupId); first = false; }
        if (clientId != null) { if (!first) sb.append(", "); sb.append("clientId=").append(clientId); }
        sb.append(")");
        return sb.toString();
    }
}