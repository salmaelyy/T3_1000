package com.example.asyncapigenerator;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class AsyncAPIVersionTest {

    @Test
    void rejectVersion1x() {
        AsyncAPIParser parser = new AsyncAPIParser(); // Top-Level Parser prüft die Version
        String invalidSpecPath = "src/test/resources/asyncapi/asyncapi-v1-example.yaml";

        Exception ex = assertThrows(Exception.class, () -> {
            parser.parseYaml(invalidSpecPath);
        });
        assertTrue(ex.getMessage().startsWith("AsyncAPI ab v2.x"),
                "Erwartet: Ablehnung von v1.x, aber war: " + ex.getMessage());
    }
}