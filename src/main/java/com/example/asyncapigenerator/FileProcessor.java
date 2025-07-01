package com.example.asyncapigenerator;

import java.io.File;

public class FileProcessor {
    private final AsyncAPIParser parser = new AsyncAPIParser();
    private final DiagramGenerator generator = new DiagramGenerator();
    private final FileWriterUtil writer = new FileWriterUtil();

    public void processFolder(File folder) {
        System.out.println("Starte Verarbeitung des Ordners: " + folder.getAbsolutePath());

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
                System.out.println("Verarbeite Datei: " + yamlFile.getName());
                AsyncAPIData data = parser.parseYaml(yamlFile.getAbsolutePath());
                data.validateFlows();

                String mmd = generator.generateMermaid(data);

                // Dateinamen für Output (gleicher Name wie Input, aber andere Endung)
                String baseName = yamlFile.getName().replaceAll("\\.ya?ml$", "");
                writer.writeToFile(new File(outputDirMmd, baseName + ".mmd").getPath(), mmd);
                writer.writeHtmlWithMermaid(new File(outputDirHtml, baseName + ".html").getPath(), mmd);

                System.out.println("Diagramm für " + yamlFile.getName() + " erfolgreich erzeugt.");
                System.out.println("-------------------------------------------------------------------------------------------------------------");
            } catch (Exception e) {
                System.out.println("Fehler beim Verarbeiten der Datei: " + yamlFile.getName());
                e.printStackTrace();
            }
        }
    }

}


// TODO: Unterstützung für weitere AsyncAPI v3.x Features ergänzen (z.B. neue Felder, Operationen prüfen)
// TODO: Fehlerausgaben und Exception-Meldungen noch klarer gestalten
// TODO: Automatisierte Generierung von Beispiel-Messages aus der AsyncAPI-Spezifikation implementieren

