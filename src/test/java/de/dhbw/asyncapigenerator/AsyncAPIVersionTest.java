package de.dhbw.asyncapigenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static de.dhbw.asyncapigenerator.TestResources.asyncapiPath;


class AsyncAPIVersionTest {

    // prüft: Spezifikation mit Version 1.x wird abgelehnt (Exception mit passender Meldung)
    @Test
     void rejectVersion1x() {
        AsyncAPIParser parser = new AsyncAPIParser();
        String invalidSpecPath = asyncapiPath("asyncapi-v1-example.yaml");

        Exception ex = assertThrows(Exception.class, () -> {
            parser.parseYaml(invalidSpecPath);
        });
        assertTrue(ex.getMessage().startsWith("AsyncAPI ab v2.x"),
                "Erwartet: Ablehnung von v1.x, aber war: " + ex.getMessage());
    }
}