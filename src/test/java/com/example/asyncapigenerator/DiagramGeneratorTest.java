package com.example.asyncapigenerator;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DiagramGeneratorTest {

    private String res(String name) {
        var u = getClass().getResource("/asyncapi/" + name);
        assertNotNull(u, "Test-Resource fehlt: " + name);
        try {
            return java.nio.file.Path.of(u.toURI()).toString();
        } catch (java.net.URISyntaxException e) {
            throw new RuntimeException("Ungültige Resource-URI für " + name, e);
        }
    }



    @Test
    void generatesMermaidAndKafkaNotesIfPresent() throws Exception {
        AsyncAPIParser parser = new AsyncAPIParser();
        AsyncAPIData data = parser.parseYaml(res("v2-kafka.yaml"));

        DiagramGenerator gen = new DiagramGenerator();
        List<DiagramGenerator.DiagramResult> diagrams = gen.generateMermaid(data);
        assertFalse(diagrams.isEmpty());

        String all = diagrams.stream().map(d -> d.content).reduce("", (a,b) -> a + "\n" + b);

        // Bei v2 hängen wir die Kafka-Infos an den MESSAGE-Namen (Label).
        boolean hasKafkaNotesInLabels = all.contains("groupId=") || all.contains("clientId=");
        assertTrue(hasKafkaNotesInLabels,
                "Kafka-Notes (groupId/clientId) sollten im Diagramm-Text (Label) vorkommen.");
    }
}