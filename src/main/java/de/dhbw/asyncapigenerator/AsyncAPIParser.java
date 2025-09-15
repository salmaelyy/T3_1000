package de.dhbw.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;

public class AsyncAPIParser {

    public AsyncAPIData parseYaml(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = mapper.readTree(new File(filePath)); //lies die datei als baum ein
        AsyncAPIData data = new AsyncAPIData();

        if (root.has("info")) {
            JsonNode info = root.get("info");
            if (info.has("title"))       data.setTitle(info.get("title").asText());
            if (info.has("version"))     data.setVersion(info.get("version").asText());
            if (info.has("description")) data.setDescription(info.get("description").asText());
        }

        String specVersion = root.path("asyncapi").asText();
        System.out.println("Gefundene AsyncAPI-Spezifikation: " + specVersion);

        if (specVersion.startsWith("1.")) {
            throw new IllegalStateException("AsyncAPI ab v2.x werden unterstützt.");
        }
        else if (specVersion.startsWith("3.")) {
            // v3 an Spezialparser delegieren
            return new AsyncAPIv3Parser().parseYaml(root, data);
        }
        else if (specVersion.startsWith("2.")) {
            return new AsyncAPIv2Parser().parseYaml(root, data);
        }
            throw new IllegalStateException("Unbekannte AsyncAPI-Version: " + specVersion);
        }
    }

