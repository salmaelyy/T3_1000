// src/main/java/com/example/asyncapigenerator/FileProcessor.java
package com.example.asyncapigenerator;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

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

        // Basis-Pfad: Projekt-Root/generated-sources (keine Ausgabe in target/)
        File outRoot = new File(System.getProperty("user.dir"), "generated-sources");
        // Optional per System-Property überschreibbar: -Dout.dir=/pfad/wohin
        String override = System.getProperty("out.dir");
        if (override != null && !override.isBlank()) {
            outRoot = new File(override);
        }

        File outputDirMmd = new File(outRoot, "mmd");
        File outputDirHtml = new File(outRoot, "html");
        try {
            Files.createDirectories(outputDirMmd.toPath());
            Files.createDirectories(outputDirHtml.toPath());
        } catch (Exception e) {
            System.out.println("Konnte Ausgabeverzeichnisse nicht erstellen: " + e.getMessage());
            return;
        }

        for (File yamlFile : files) {
            try {
                System.out.println("Verarbeite Datei: " + yamlFile.getName());
                AsyncAPIData data = parser.parseYaml(yamlFile.getAbsolutePath());
                data.validateFlows();

                String baseName = yamlFile.getName().replaceAll("\\.ya?ml$", "");
                List<DiagramGenerator.DiagramResult> diagrams = generator.generateMermaid(data);

                String metaHtml = writer.buildMetaHtml(data);

                if (diagrams.size() == 1) {
                    DiagramGenerator.DiagramResult d = diagrams.get(0);
                    writer.writeToFile(new File(outputDirMmd, baseName + ".mmd").getPath(), d.content);
                    writer.writeHtmlWithMermaidToggleWithMeta(
                            new File(outputDirHtml, baseName + ".html").getPath(),
                            d.content, d.content, metaHtml
                    );
                    System.out.println("Diagramm (" + d.mode + ") für " + yamlFile.getName() + " erfolgreich erzeugt.");
                } else {
                    String shortContent = diagrams.stream().filter(x -> x.mode.equals("short")).findFirst().map(x -> x.content).orElse("");
                    String fullContent  = diagrams.stream().filter(x -> x.mode.equals("full")).findFirst().map(x -> x.content).orElse("");

                    writer.writeToFile(new File(outputDirMmd, baseName + "_short.mmd").getPath(), shortContent);
                    writer.writeToFile(new File(outputDirMmd, baseName + "_full.mmd").getPath(),  fullContent);

                    writer.writeHtmlWithMermaidToggleWithMeta(
                            new File(outputDirHtml, baseName + ".html").getPath(),
                            shortContent, fullContent, metaHtml
                    );
                    System.out.println("Diagramme (Kurz + Lang) für " + yamlFile.getName() + " erfolgreich erzeugt.");
                }
                System.out.println("-------------------------------------------------------------------------------------------------------------");
            } catch (Exception e) {
                System.out.println("Fehler beim Verarbeiten der Datei: " + yamlFile.getName());
                System.out.println("-------------------------------------------------------------------------------------------------------------");
                e.printStackTrace(System.out);
            }
        }

        System.out.println("\n>> Outputs liegen unter: " + outRoot.getAbsolutePath() + " {mmd, html}\n");
    }
}