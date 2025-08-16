package com.example.asyncapigenerator;

import java.io.File;
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

        File outputDirMmd = new File("generated-sources/mmd");
        File outputDirHtml = new File("generated-sources/html");
        outputDirMmd.mkdirs();
        outputDirHtml.mkdirs();

        for (File yamlFile : files) {
            try {
                System.out.println("Verarbeite Datei: " + yamlFile.getName());
                AsyncAPIData data = parser.parseYaml(yamlFile.getAbsolutePath());
                data.validateFlows();

                String baseName = yamlFile.getName().replaceAll("\\.ya?ml$", "");

                List<DiagramGenerator.DiagramResult> diagrams = generator.generateMermaid(data);

                if (diagrams.size() == 1) {
                    // Nur ein Diagramm (Langform)
                    DiagramGenerator.DiagramResult d = diagrams.get(0);
                    String mmdPath = new File(outputDirMmd, baseName + ".mmd").getPath();
                    String htmlPath = new File(outputDirHtml, baseName + ".html").getPath();
                    writer.writeToFile(mmdPath, d.content);
                    writer.writeHtmlWithMermaidSingle(htmlPath, d.content);                    System.out.println("Diagramm (" + d.mode + ") für " + yamlFile.getName() + " erfolgreich erzeugt.");
                } else if (diagrams.size() == 2) {
                    // Zwei Diagramme (Short + Full)
                    String shortContent = diagrams.stream()
                            .filter(d -> d.mode.equals("short"))
                            .findFirst()
                            .map(d -> d.content)
                            .orElse("");
                    String fullContent = diagrams.stream()
                            .filter(d -> d.mode.equals("full"))
                            .findFirst()
                            .map(d -> d.content)
                            .orElse("");

                    String shortMmdPath = new File(outputDirMmd, baseName + "_short.mmd").getPath();
                    String fullMmdPath = new File(outputDirMmd, baseName + "_full.mmd").getPath();
                    writer.writeToFile(shortMmdPath, shortContent);
                    writer.writeToFile(fullMmdPath, fullContent);

                    String toggleHtmlPath = new File(outputDirHtml, baseName + ".html").getPath();
                    writer.writeHtmlWithMermaidToggle(toggleHtmlPath, shortContent, fullContent);
                    System.out.println("Diagramme (Kurz + Lang) für " + yamlFile.getName() + " erfolgreich erzeugt.");
                }

                System.out.println("-------------------------------------------------------------------------------------------------------------");

            } catch (Exception e) {
                System.out.println("Fehler beim Verarbeiten der Datei: " + yamlFile.getName());
                System.out.println("-------------------------------------------------------------------------------------------------------------");
                e.printStackTrace();
            }
        }
    }
}