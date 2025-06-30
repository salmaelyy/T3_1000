package com.example.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AsyncAPIParser {
    public AsyncAPIData parseYaml(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = mapper.readTree(new File(filePath));
        AsyncAPIData data = new AsyncAPIData();

        // Mappe Message-Ref auf Topic (Channel)
        Map<String, String> messageRefToTopic = new HashMap<>();

        JsonNode channels = root.path("channels");
        channels.fields().forEachRemaining(entry -> {
            String channelName = entry.getKey();
            JsonNode channel = entry.getValue();

            // Publish: Producer → Topic
            JsonNode publish = channel.path("publish");
            if (!publish.isMissingNode()) {
                String producer = publish.path("operationId").asText("publisher");
                String messageRef = publish.path("message").path("$ref").asText();
                String messageName = extractMessageName(publish);
                data.addFlow(producer, channelName, messageName);
                if (!messageRef.isEmpty()) {
                    messageRefToTopic.put(messageRef, channelName);
                }
            }
        });

        // Subscribe: Topic → Consumer
        channels.fields().forEachRemaining(entry -> {
            String channelName = entry.getKey();
            JsonNode channel = entry.getValue();

            JsonNode subscribe = channel.path("subscribe");
            if (!subscribe.isMissingNode()) {
                String consumer = subscribe.path("operationId").asText("subscriber");
                String messageRef = subscribe.path("message").path("$ref").asText();
                String messageName = extractMessageName(subscribe);

                // Hole das Topic, auf das dieser Consumer hört
                String topic = messageRefToTopic.getOrDefault(messageRef, channelName);
                data.addFlow(topic, consumer, messageName);
            }
        });

        return data;
    }

    private String extractMessageName(JsonNode node) {
        JsonNode message = node.path("message");
        String ref = message.path("$ref").asText();
        if (ref.contains("/")) {
            return ref.substring(ref.lastIndexOf("/") + 1);
        } else {
            return "UnknownMessage";
        }
    }
}
