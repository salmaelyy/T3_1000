package de.dhbw.asyncapigenerator;

import org.junit.jupiter.api.Test;

import java.util.List;
import static de.dhbw.asyncapigenerator.TestResources.asyncapiPath;

import static org.junit.jupiter.api.Assertions.*;



public class DiagramGeneratorTest {

    // prüft: aus V2-Spec wird ein Mermaid-Diagramm generiert und Kafka-Infos erscheinen in den Nachrichten-Labels
    private final String v2SpecPath = asyncapiPath("v2-kafka.yaml");

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
