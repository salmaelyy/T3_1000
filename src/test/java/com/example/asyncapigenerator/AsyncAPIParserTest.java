package com.example.asyncapigenerator;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncAPIParserTest {

    private String res(String name) {
        URL u = getClass().getResource("/asyncapi/" + name);
        assertNotNull(u, "Test-Resource fehlt: " + name);
        return Paths.get(u.getPath()).toString();
    }

    @Test
    void testValidV2File_parsesAndFindsMatchedAndBridgingFlows() throws Exception {
        AsyncAPIParser p = new AsyncAPIParser();
        AsyncAPIData d = p.parseYaml(res("v2-kafka.yaml"));

        assertNotNull(d);
        assertFalse(d.getFlows().isEmpty(), "Es sollten Flows gefunden werden.");

        // Mindestens ein Bridging-Flow (op -> topic:..., topic:... -> op)
        boolean hasTopicNode = d.getFlows().stream()
                .anyMatch(f -> f.from.startsWith("topic:") || f.to.startsWith("topic:"));
        assertTrue(hasTopicNode, "Bridging-Flows mit topic:-Knoten werden erwartet.");
    }

    @Test
    void testChannelsWithoutPubSub_throws() {
        AsyncAPIParser p = new AsyncAPIParser();
        assertThrows(IllegalStateException.class, () ->
                p.parseYaml(res("channels_no_pubsub.yaml")));
    }

    @Test
    void testSeparatedPublishAndSubscribe_sameMessageButDifferentChannels_createsOnlyBridgingFlows() throws Exception {
        // In dieser Datei gibt es publish in channelA und subscribe in channelB mit derselben Message.
        // Deine Parser-Logik erzeugt KEINEN direkten Producer->Consumer Flow,
        // ABER erzeugt Bridging-Flows via topic:channelA / topic:channelB.
        AsyncAPIParser p = new AsyncAPIParser();
        AsyncAPIData d = p.parseYaml(res("v2-separated-same-msg-different-channels.yaml"));

        assertFalse(d.getFlows().isEmpty(), "Es sollten Bridging-Flows entstehen.");

        // Es darf KEIN direkter Flow Producer->Consumer mit derselben Message entstehen (weil Channel verschieden)
        boolean hasDirect = d.getFlows().stream().anyMatch(f ->
                !f.from.startsWith("topic:") && !f.to.startsWith("topic:"));
        assertFalse(hasDirect, "Es sollten keine direkten Producer->Consumer-Flows entstehen (nur Bridging).");

        // Aber Bridging muß vorhanden sein
        boolean hasBridging = d.getFlows().stream().anyMatch(f ->
                f.from.startsWith("topic:") || f.to.startsWith("topic:"));
        assertTrue(hasBridging, "Bridging-Flows erwartet.");
    }

    @Test
    void testNoMatchingMessage_butSeparatedPubAndSubDifferentMessages_yieldsBridgingOnly() throws Exception {
        AsyncAPIParser p = new AsyncAPIParser();
        AsyncAPIData d = p.parseYaml(res("v2-separated-different-messages.yaml"));
        assertFalse(d.getFlows().isEmpty(), "Bridging-Flows sollten existieren, obwohl Messages nicht matchen.");

        boolean onlyBridging = d.getFlows().stream()
                .allMatch(f -> f.from.startsWith("topic:") || f.to.startsWith("topic:"));
        assertTrue(onlyBridging, "Nur Bridging-Flows werden erwartet.");
    }
}