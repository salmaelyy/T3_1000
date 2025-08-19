package com.example.asyncapigenerator;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileProcessorIntegrationTest {

    private File copy(String name, Path dir) throws Exception {
        URL u = getClass().getResource("/asyncapi/" + name);
        assertNotNull(u, "missing resource: /asyncapi/" + name);
        Path target = dir.resolve(name);
        Files.copy(Path.of(u.toURI()), target);
        return target.toFile();
    }

    @Test
    void processesFolderAndCreatesOutputs() throws Exception {
        Path tmp = Files.createTempDirectory("asyncapi-it");
        Path asyncapi = Files.createDirectory(tmp.resolve("asyncapi"));

        copy("v2-kafka.yaml", asyncapi);
        copy("v3-ops-kafka.yaml", asyncapi);

        // run
        FileProcessor fp = new FileProcessor();
        fp.processFolder(asyncapi.toFile());

        File mmd = new File("generated-sources/mmd");
        File html = new File("generated-sources/html");
        assertTrue(mmd.exists(), "mmd dir should exist");
        assertTrue(html.exists(), "html dir should exist");

        // Mindestens für beide Dateien sollte Output entstehen
        assertTrue(new File(mmd, "v2-kafka_full.mmd").exists() || new File(mmd, "v2-kafka.mmd").exists());
        assertTrue(new File(html, "v2-kafka.html").exists());

        assertTrue(new File(mmd, "v3-ops-kafka_full.mmd").exists() || new File(mmd, "v3-ops-kafka.mmd").exists());
        assertTrue(new File(html, "v3-ops-kafka.html").exists());
    }
}