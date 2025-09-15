package de.dhbw.asyncapigenerator;

import org.junit.jupiter.api.Test;


import static de.dhbw.asyncapigenerator.TestResources.asyncapiPath;
import static org.junit.jupiter.api.Assertions.*;

public class AsyncAPIParserTest {

    // prüft: V2-Spec wird geparst, normale und Bridging-Flows (topic:...) werden gefunden
    @Test
    void testValidV2File_parsesAndFindsMatchedAndBridgingFlows() throws Exception {
        AsyncAPIParser p = new AsyncAPIParser();
        AsyncAPIData d = p.parseYaml(asyncapiPath("v2-kafka.yaml"));

        assertNotNull(d);
        assertFalse(d.getFlows().isEmpty(), "Es sollten Flows gefunden werden.");

        // Mindestens ein Bridging-Flow (op -> topic:..., topic:... -> op)
        boolean hasTopicNode = d.getFlows().stream()
                .anyMatch(f -> f.from.startsWith("topic:") || f.to.startsWith("topic:"));
        assertTrue(hasTopicNode, "Bridging-Flows mit topic:-Knoten werden erwartet.");
    }

    // prüft: wenn ein Channel keine publish/subscribe enthält, wirft Parser IllegalStateException
    @Test
    void testChannelsWithoutPubSub_throws() {
        AsyncAPIParser p = new AsyncAPIParser();
        assertThrows(IllegalStateException.class, () ->
                p.parseYaml(asyncapiPath("channels_no_pubsub.yaml")));
    }

    // prüft: publish/subscribe mit gleicher Message, aber in unterschiedlichen Channels -> nur Bridging-Flows, kein direkter Flow
    @Test
    void testSeparatedPublishAndSubscribe_sameMessageButDifferentChannels_createsOnlyBridgingFlows() throws Exception {
        // In dieser Datei gibt es publish in channelA und subscribe in channelB mit derselben Message.
        // die Parser-Logik erzeugt KEINEN direkten Producer->Consumer Flow,
        // ABER erzeugt Bridging-Flows via topic:channelA / topic:channelB.
        AsyncAPIParser p = new AsyncAPIParser();
        AsyncAPIData d = p.parseYaml(asyncapiPath("v2-separated-same-msg-different-channels.yaml"));

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

    // prüft: publish/subscribe mit unterschiedlichen Messages -> nur Bridging-Flows, keine direkten Flows
    @Test
    void testNoMatchingMessage_butSeparatedPubAndSubDifferentMessages_yieldsBridgingOnly() throws Exception {
        AsyncAPIParser p = new AsyncAPIParser();
        AsyncAPIData d = p.parseYaml(asyncapiPath("v2-separated-different-messages.yaml"));
        assertFalse(d.getFlows().isEmpty(), "Bridging-Flows sollten existieren, obwohl Messages nicht matchen.");

        boolean onlyBridging = d.getFlows().stream()
                .allMatch(f -> f.from.startsWith("topic:") || f.to.startsWith("topic:"));
        assertTrue(onlyBridging, "Nur Bridging-Flows werden erwartet.");
    }
}