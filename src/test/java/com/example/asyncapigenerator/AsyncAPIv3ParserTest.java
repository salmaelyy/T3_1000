package com.example.asyncapigenerator;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncAPIv3ParserTest {

    private String res(String name) {
        URL u = getClass().getResource("/asyncapi/" + name);
        assertNotNull(u, "Test-Resource fehlt: " + name);
        return Paths.get(u.getPath()).toString();
    }

    @Test
    void parsesV3OperationsWithKafkaNotes() throws Exception {
        AsyncAPIv3Parser p = new AsyncAPIv3Parser();
        AsyncAPIData d = p.parseYaml(res("v3-ops-kafka.yaml"));

        assertFalse(d.getFlows().isEmpty());
        // Bei v3 hängen wir (groupId/clientId) an die operationId (Teilnehmer) an.
        boolean anyParticipantContainsKafkaNotes = d.getFlows().stream().anyMatch(f ->
                f.from.contains("groupId=") || f.from.contains("clientId=") ||
                        f.to.contains("groupId=")   || f.to.contains("clientId="));

        assertTrue(anyParticipantContainsKafkaNotes,
                "Kafka-Infos (groupId/clientId) sollten in Teilnehmernamen (operationId) vorkommen.");
    }
}