package de.dhbw.asyncapigenerator;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static de.dhbw.asyncapigenerator.TestResources.asyncapiPath;
import static org.junit.jupiter.api.Assertions.*;

public class FileProcessorIntegrationTest {

    // prüft: kompletter Durchlauf über Input-Ordner -> erzeugt .mmd- und .html-Ausgaben in generated-sources für V2- und V3-Beispieldateien
    @Test
    void processesFolderAndCreatesOutputs() throws Exception {
        Path temp = Files.createTempDirectory("asyncapi-temp");
        Path asyncapi = Files.createDirectory(temp.resolve("asyncapi"));

        Path v2 = Path.of(asyncapiPath("v2-kafka.yaml"));
        Path v3 = Path.of(asyncapiPath("v3-ops-kafka.yaml"));

        Files.copy(v2, asyncapi.resolve("v2-kafka.yaml"));
        Files.copy(v3, asyncapi.resolve("v3-ops-kafka.yaml"));

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
