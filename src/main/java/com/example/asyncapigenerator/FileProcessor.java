package com.example.asyncapigenerator;

import java.io.File;

public class FileProcessor {
    private final AsyncAPIParser parser = new AsyncAPIParser();
    private final DiagramGenerator generator = new DiagramGenerator();
    private final FileWriterUtil writer = new FileWriterUtil();

    public void processFolder(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            System.out.println("Keine YAML-Dateien gefunden.");
            return;
        }

        File outputDirMmd = new File("target/generated-sources/output/mmd");
        File outputDirHtml = new File("target/generated-sources/output/html");
        outputDirMmd.mkdirs();
        outputDirHtml.mkdirs();

        for (File yamlFile : files) {
            try {
                System.out.println("Verarbeite: " + yamlFile.getName());
                AsyncAPIData data = parser.parseYaml(yamlFile.getAbsolutePath());
                data.validateFlows();

                String mmd = generator.generateMermaid(data);

                String baseName = yamlFile.getName().replace(".yaml", "").replace(".yml", "");

                writer.writeToFile(new File(outputDirMmd, baseName + ".mmd").getPath(), mmd);
                writer.writeHtmlWithMermaid(new File(outputDirHtml, baseName + ".html").getPath(), mmd);
                System.out.println("Wurde verarbeitet: " + yamlFile.getName());
            } catch (Exception e) {
                System.out.println("Fehler bei Datei: " + yamlFile.getName());
                e.printStackTrace();
            }
        }
    }
}
