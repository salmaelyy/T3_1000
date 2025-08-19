package com.example.asyncapigenerator;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FileWriterUtil {

    public void writeToFile(String fileName, String content) throws Exception {
        Files.createDirectories(Paths.get(fileName).getParent());
        Files.write(Paths.get(fileName), content.getBytes(StandardCharsets.UTF_8));
    }

    // für Integrationstest & Meta-Panel nutzbar
    public String buildMetaHtml(AsyncAPIData data) {
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
            """;
        return String.format(template, title, version, description, flowCount, participantCount, listItems);
    }

    public void writeHtmlWithMermaidToggleWithMeta(String fileName, String shortCode, String fullCode, String metaHtml) throws Exception {
        String html = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8" />
      <title>Mermaid Diagramm</title>
      <script type="module">
        import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
        mermaid.initialize({ startOnLoad: true });

        window.addEventListener("DOMContentLoaded", () => {
          const toggle = document.getElementById("toggle");
          const metaToggle = document.getElementById("toggle-meta");
          const shortDiv = document.getElementById("diagram-short");
          const fullDiv = document.getElementById("diagram-full");
          const metaDiv = document.getElementById("meta");

          async function renderActiveDiagram() {
            const activeDiv = toggle.checked ? fullDiv : shortDiv;
            await mermaid.run({ nodes: [activeDiv] });
          }

          toggle.addEventListener("change", () => {
            shortDiv.classList.toggle("active", !toggle.checked);
            fullDiv.classList.toggle("active", toggle.checked);
            renderActiveDiagram();
          });

          metaToggle.addEventListener("change", () => {
            metaDiv.classList.toggle("active", metaToggle.checked);
          });

          renderActiveDiagram();
        });
      </script>
      <style>
        body { font-family: sans-serif; background-color: #f8f8f8; padding: 20px; }
        .diagram-wrapper { overflow: auto; min-width: 1600px; }
        .panel { margin: 12px 0; }
        .switch { position: relative; display: inline-block; width: 60px; height: 34px; vertical-align: middle; }
        .switch input { opacity: 0; width: 0; height: 0; }
        .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #ccc; transition: .4s; border-radius: 34px; }
        .slider:before { position: absolute; content: ""; height: 26px; width: 26px; left: 4px; bottom: 4px; background-color: white; transition: .4s; border-radius: 50%; }
        input:checked + .slider { background-color: #4caf50; }
        input:checked + .slider:before { transform: translateX(26px); }
        .label-text { font-size: 14px; margin: 0 14px 0 8px; vertical-align: middle; }
        .mermaid { visibility: hidden; position: absolute; font-size: 12px; line-height: 1.2; }
        .mermaid.active { visibility: visible; position: static; }
        #meta { display: none; background: #fff; padding: 16px; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,.08); max-width: 960px; }
        #meta.active { display: block; }
        #meta table { border-collapse: collapse; }
        #meta td { padding: 6px 10px; }
        #meta td.muted { color: #666; }
        #meta ul.list { margin: 6px 0 0 0; padding-left: 20px; }
      </style>
    </head>
    <body>
      <div class="panel">
        <label class="switch">
          <input type="checkbox" id="toggle"><span class="slider"></span>
        </label>
        <span class="label-text">Langform anzeigen</span>

        <label class="switch" style="margin-left:24px;">
          <input type="checkbox" id="toggle-meta"><span class="slider"></span>
        </label>
        <span class="label-text">Metadaten anzeigen</span>
      </div>

      <div id="meta">%META%</div>

      <div class="diagram-wrapper">
        <div class="mermaid active" id="diagram-short">%SHORT%</div>
        <div class="mermaid" id="diagram-full">%FULL%</div>
      </div>
    </body>
    </html>
    """;
        html = html.replace("%SHORT%", shortCode)
                .replace("%FULL%",  fullCode)
                .replace("%META%",  metaHtml);
        writeToFile(fileName, html);
    }

    // (optional) ein Diagramm ohne Toggle
    public void writeHtmlWithMermaidSingle(String fileName, String mermaidCode) throws Exception {
        String html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <title>Mermaid Diagramm</title>
          <script type="module">
            import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
            mermaid.initialize({ startOnLoad: true });
          </script>
          <style>
            body { margin: 0; padding: 0; font-family: sans-serif; background-color: #f8f8f8; }
            .mermaid-container { width: 100%; height: 100vh; overflow: auto; padding: 20px; box-sizing: border-box; }
            .mermaid { font-size: 14px; }
          </style>
        </head>
        <body>
          <div class="mermaid-container"><div class="mermaid">%%DIAGRAM%%</div></div>
        </body>
        </html>
        """;
        html = html.replace("%%DIAGRAM%%", mermaidCode);
        writeToFile(fileName, html);
    }

    private static String nullToDash(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private static String esc(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
}