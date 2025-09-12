package com.example.asyncapigenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncAPIv3ParserTest {

    @Test
    void parsesV3OperationsWithKafkaNotes() throws Exception {
        String v3SpecPath = "src/test/resources/asyncapi/v3-ops-kafka.yaml";

        AsyncAPIv3Parser p = new AsyncAPIv3Parser();
        AsyncAPIData d = p.parseYaml(v3SpecPath); //enthält alle flows

        assertFalse(d.getFlows().isEmpty());
        // Bei v3 hängen wir (groupId/clientId) an die operationId (Teilnehmer) an.
        boolean anyParticipantContainsKafkaNotes = d.getFlows().stream().anyMatch(f ->
                        f.from.contains("groupId=") || f.from.contains("clientId=") ||
                        f.to.contains("groupId=")   || f.to.contains("clientId="));

        assertTrue(anyParticipantContainsKafkaNotes,
                "Kafka-Infos (groupId/clientId) sollten in Teilnehmernamen (operationId) vorkommen.");
    }

}