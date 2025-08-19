// src/main/java/com/example/asyncapigenerator/FileProcessor.java
package com.example.asyncapigenerator;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

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

                // Metadaten-Panel bauen
                String metaHtml = buildMetaHtml(data);

                if (diagrams.size() == 1) {
                    // Nur ein Diagramm (Langform)
                    DiagramGenerator.DiagramResult d = diagrams.get(0);
                    String mmdPath  = new File(outputDirMmd,  baseName + ".mmd").getPath();
                    String htmlPath = new File(outputDirHtml, baseName + ".html").getPath();
                    writer.writeToFile(mmdPath, d.content);
                    // Single-Diagramm + Meta: Toggle-Template wiederverwenden (Short = Full)
                    writer.writeHtmlWithMermaidToggleWithMeta(htmlPath, d.content, d.content, metaHtml);
                    System.out.println("Diagramm (" + d.mode + ") für " + yamlFile.getName() + " erfolgreich erzeugt.");
                } else {
                    // Zwei Diagramme (Short + Full)
                    String shortContent = diagrams.stream()
                            .filter(d -> d.mode.equals("short"))
                            .findFirst().map(d -> d.content).orElse("");
                    String fullContent  = diagrams.stream()
                            .filter(d -> d.mode.equals("full"))
                            .findFirst().map(d -> d.content).orElse("");

                    String shortMmdPath = new File(outputDirMmd,  baseName + "_short.mmd").getPath();
                    String fullMmdPath  = new File(outputDirMmd,  baseName + "_full.mmd").getPath();
                    writer.writeToFile(shortMmdPath, shortContent);
                    writer.writeToFile(fullMmdPath, fullContent);

                    String toggleHtmlPath = new File(outputDirHtml, baseName + ".html").getPath();
                    writer.writeHtmlWithMermaidToggleWithMeta(toggleHtmlPath, shortContent, fullContent, metaHtml);
                    System.out.println("Diagramme (Kurz + Lang) für " + yamlFile.getName() + " erfolgreich erzeugt.");
                }

                System.out.println("-------------------------------------------------------------------------------------------------------------");
            } catch (Exception e) {
                System.out.println("Fehler beim Verarbeiten der Datei: " + yamlFile.getName());
                System.out.println("-------------------------------------------------------------------------------------------------------------");
                e.printStackTrace(System.out);
            }
        }
    }

    private String buildMetaHtml(AsyncAPIData data) {
        // Teilnehmer sammeln
        Set<String> participants = new LinkedHashSet<>();
        for (AsyncAPIData.Flow f : data.getFlows()) {
            participants.add(f.from);
            participants.add(f.to);
        }

        String title = esc(nullToDash(data.getTitle()));
        String version = esc(nullToDash(data.getVersion()));
        String description = esc(nullToDash(data.getDescription()));
        int flowCount = data.getFlows().size();
        int participantCount = participants.size();

        String listItems = participants.stream()
                .map(p -> "<li><code>" + esc(p) + "</code></li>")
                .collect(Collectors.joining("\n"));

        // NEW: Kafka notes per participant (if any)
        String kafkaSection;
        if (data.getParticipantNotes().isEmpty()) {
            kafkaSection = "<div class=\"muted\" style=\"margin-top:10px;\">Kafka</div><div class=\"muted\">—</div>";
        } else {
            StringBuilder ks = new StringBuilder();
            ks.append("<div class=\"muted\" style=\"margin-top:10px;\">Kafka</div><ul class=\"list\">");
            data.getParticipantNotes().forEach((p, notes) -> {
                ks.append("<li><code>").append(esc(p)).append("</code>: ")
                        .append(esc(String.join("; ", notes))).append("</li>");
            });
            ks.append("</ul>");
            kafkaSection = ks.toString();
        }

        String template = """
            <h2>Metadaten</h2>
            <table>
              <tr><td class="muted">Title</td><td>%s</td></tr>
              <tr><td class="muted">Version</td><td>%s</td></tr>
              <tr><td class="muted">Beschreibung</td><td>%s</td></tr>
              <tr><td class="muted">Flows</td><td>%d</td></tr>
              <tr><td class="muted">Teilnehmer</td><td>%d</td></tr>
            </table>
            <div class="muted" style="margin-top:10px;">Teilnehmer</div>
            <ul class="list">
            %s
            </ul>
            %s
            """;

        return String.format(template, title, version, description, flowCount, participantCount, listItems, kafkaSection);
    }
    private static String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private static String esc(String s) {
        return s.replace("&","&amp;")
                .replace("<","&lt;")
                .replace(">","&gt;")
                .replace("\"","&quot;");
    }
}