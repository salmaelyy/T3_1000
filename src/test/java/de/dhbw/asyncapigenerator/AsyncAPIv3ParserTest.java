package de.dhbw.asyncapigenerator;

import org.junit.jupiter.api.Test;

import static de.dhbw.asyncapigenerator.TestResources.asyncapiPath;
import static org.junit.jupiter.api.Assertions.*;


public class AsyncAPIv3ParserTest {

    // prüft: V3-Spec mit Kafka-Bindings wird korrekt geparst und Kafka-Infos (groupId/clientId) in operationId übernommen
    @Test
    void parsesV3OperationsWithKafkaNotes() throws Exception {
        String v3SpecPath = asyncapiPath("v3-ops-kafka.yaml");

        AsyncAPIParser parser = new AsyncAPIParser();
        AsyncAPIData data = parser.parseYaml(v3SpecPath);

        assertNotNull(data);
        assertFalse(data.getFlows().isEmpty(), "Es sollten Flows entstehen.");

        // Bei v3 werden Kafka-Infos an die operationId (Teilnehmer) angehängt.
        boolean anyParticipantContainsKafkaNotes = data.getFlows().stream().anyMatch(f ->
                f.from.contains("groupId=") || f.from.contains("clientId=") ||
                        f.to.contains("groupId=")   || f.to.contains("clientId=")
        );
        assertTrue(anyParticipantContainsKafkaNotes,
                "Kafka-Infos (groupId/clientId) sollten in Teilnehmernamen (operationId) vorkommen.");
    }
}
