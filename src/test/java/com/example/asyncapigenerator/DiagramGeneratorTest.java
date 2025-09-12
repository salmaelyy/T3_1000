package com.example.asyncapigenerator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//Prüft: Kann der DiagramGenerator aus den Daten ein Mermaid-Diagramm bauen und sind dort die Kafka-Infos enthalten?

public class DiagramGeneratorTest {

    private final String v2SpecPath = "src/test/resources/asyncapi/v2-kafka.yaml";

    @Test
    void generatesMermaidAndKafkaNotesIfPresent() throws Exception {
        AsyncAPIParser parser = new AsyncAPIParser();
        AsyncAPIData data = parser.parseYaml(v2SpecPath);

        DiagramGenerator gen = new DiagramGenerator();
        List<DiagramGenerator.DiagramResult> diagrams = gen.generateMermaid(data);
        assertFalse(diagrams.isEmpty());

        String all = diagrams.stream()
                .map(d -> d.content)
                .reduce("", (a, b) -> a + "\n" + b);

        // Bei v2 hängen wir die Kafka-Infos an den MESSAGE-Namen (Label).
        boolean hasKafkaNotesInLabels = all.contains("groupId=") || all.contains("clientId=");
        assertTrue(hasKafkaNotesInLabels,
                "Kafka-Notes (groupId/clientId) sollten im Diagramm-Text (Label) vorkommen.");
    }
}
