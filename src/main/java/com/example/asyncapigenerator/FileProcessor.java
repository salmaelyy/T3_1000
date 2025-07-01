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


// TODO:
// Aktuell funktioniert der Parser nur für eine einzelne AsyncAPI-YAML-Datei bzw. für ein gemeinsames Modell,
// in dem alle Producer und Consumer im selben Dokument/Channel definiert sind.
//
// Damit das Programm auch mit mehreren AsyncAPI-Dateien (z.B. je Service eine Datei) funktioniert,
// müssen die extrahierten Flows aus allen Dateien gesammelt und zu einem Gesamtdiagramm zusammengeführt werden.
//
// Schritte zur Erweiterung:
// 1. Alle YAML-Dateien im Input-Ordner einlesen und jeweils die Flows extrahieren.
// 2. Die Flows aus allen Dateien in einer gemeinsamen Datenstruktur (z.B. einer zentralen AsyncAPIData-Instanz) sammeln.
// 3. Beim Diagramm-Generator alle gesammelten Flows berücksichtigen und daraus ein gemeinsames Mermaid-Diagramm erzeugen.
// 4. Optional: Duplikate und Self-Calls vermeiden, Teilnehmernamen vereinheitlichen.
//
// Hinweis:
// Siehe auch die Diskussionen in der AsyncAPI-Community und Tools wie Modelina oder Generator-Templates,
// die ein ähnliches Vorgehen vorschlagen [1][3].
//
// Beispiel für die Zusammenführung:
// - Datei 1: Producer auf Topic A
// - Datei 2: Consumer auf Topic A
// => Diagramm zeigt: Producer -> Topic A -> Consumer

