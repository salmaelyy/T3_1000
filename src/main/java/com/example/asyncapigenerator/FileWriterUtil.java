// src/main/java/com/example/asyncapigenerator/FileWriterUtil.java
package com.example.asyncapigenerator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileWriterUtil {

    public void writeToFile(String fileName, String content) throws Exception {
        Files.write(Paths.get(fileName), content.getBytes(StandardCharsets.UTF_8));
    }

    // HTML mit nur einem Mermaid-Diagramm
    public void writeHtmlWithMermaidSingle(String fileName, String mermaidCode) throws Exception {
        String html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1" />
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
          <div class="mermaid-container">
            <div class="mermaid">
              %%DIAGRAM%%
            </div>
          </div>
        </body>
        </html>
        """;
        html = html.replace("%%DIAGRAM%%", mermaidCode);
        writeToFile(fileName, html);
    }

    // HTML mit Umschalter (Kurz/Lang) – bestehend
    public void writeHtmlWithMermaidToggle(String fileName, String shortCode, String fullCode) throws Exception {
        String html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <title>Mermaid Diagramm</title>
          <script type="module">
            import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
            mermaid.initialize({ startOnLoad: true });

            window.addEventListener("DOMContentLoaded", () => {
              const toggle = document.getElementById("toggle");
              const shortDiv = document.getElementById("diagram-short");
              const fullDiv = document.getElementById("diagram-full");

              async function renderActiveDiagram() {
                const activeDiv = toggle.checked ? fullDiv : shortDiv;
                await mermaid.run({ nodes: [activeDiv] });
              }

              toggle.addEventListener("change", () => {
                shortDiv.classList.toggle("active", !toggle.checked);
                fullDiv.classList.toggle("active", toggle.checked);
                renderActiveDiagram();
              });

              renderActiveDiagram();
            });
          </script>
          <style>
            body { font-family: sans-serif; background-color: #f8f8f8; padding: 20px; }
            .diagram-wrapper { overflow: auto; min-width: 1600px; }
            .switch { position: relative; display: inline-block; width: 60px; height: 34px; margin-bottom: 20px; }
            .switch input { opacity: 0; width: 0; height: 0; }
            .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #ccc; transition: .4s; border-radius: 34px; }
            .slider:before { position: absolute; content: ""; height: 26px; width: 26px; left: 4px; bottom: 4px; background-color: white; transition: .4s; border-radius: 50%; }
            input:checked + .slider { background-color: #4caf50; }
            input:checked + .slider:before { transform: translateX(26px); }
            .label-text { font-size: 14px; margin-left: 10px; vertical-align: middle; }
            .mermaid { visibility: hidden; position: absolute; font-size: 12px; line-height: 1.2; }
            .mermaid.active { visibility: visible; position: static; }
          </style>
        </head>
        <body>
          <label class="switch"><input type="checkbox" id="toggle"><span class="slider"></span></label>
          <span class="label-text">Langform anzeigen</span>

          <div class="diagram-wrapper">
            <div class="mermaid active" id="diagram-short">%%SHORT%%</div>
            <div class="mermaid" id="diagram-full">%%FULL%%</div>
          </div>
        </body>
        </html>
        """;
        html = html.replace("%%SHORT%%", shortCode);
        html = html.replace("%%FULL%%",  fullCode);
        writeToFile(fileName, html);
    }

    // NEU: HTML mit Umschalter (Kurz/Lang) + zusätzlichem Metadaten-Toggle
    public void writeHtmlWithMermaidToggleWithMeta(String fileName, String shortCode, String fullCode, String metaHtml) throws Exception {
        String html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <title>Mermaid Diagramm</title>
          <script type="module">
            import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
            mermaid.initialize({ startOnLoad: true });

            window.addEventListener("DOMContentLoaded", () => {
              const toggle = document.getElementById("toggle");
              const metaToggle = document.getElementById("toggle-meta");
              const shortDiv = document.getElementById("diagram-short");
              const fullDiv = document.getElementById("diagram-full");
              const metaPanel = document.getElementById("meta-panel");

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
                metaPanel.classList.toggle("open", metaToggle.checked);
              });

              // Initial
              renderActiveDiagram();
            });
          </script>
          <style>
            :root { --panel-w: 360px; }
            body { font-family: sans-serif; background-color: #f8f8f8; padding: 20px; margin:0; }
            .toolbar { display:flex; gap:16px; align-items:center; flex-wrap:wrap; margin-bottom:16px; }
            .switch { position: relative; display: inline-block; width: 60px; height: 34px; }
            .switch input { opacity: 0; width: 0; height: 0; }
            .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0;
                      background-color: #ccc; transition: .4s; border-radius: 34px; }
            .slider:before { position: absolute; content: ""; height: 26px; width: 26px; left: 4px; bottom: 4px;
                             background-color: white; transition: .4s; border-radius: 50%; }
            input:checked + .slider { background-color: #4caf50; }
            input:checked + .slider:before { transform: translateX(26px); }
            .label-text { font-size: 14px; margin-left: 8px; vertical-align: middle; }

            .layout { display:flex; gap:16px; }
            .diagram-wrapper { overflow:auto; min-width: 800px; flex:1 1 auto; background:#fff; border-radius:8px; padding:12px; }
            .mermaid { visibility:hidden; position:absolute; font-size:12px; line-height:1.2; }
            .mermaid.active { visibility:visible; position:static; }

            .meta-panel { width: var(--panel-w); max-width: 90vw; background:#fff; border-radius:8px; padding:16px;
                          box-shadow: 0 0 0 1px #e5e7eb; height: fit-content; display:none; }
            .meta-panel.open { display:block; }
            .meta-panel h2 { margin: 0 0 8px 0; font-size: 16px; }
            .meta-panel table { border-collapse: collapse; width:100%; font-size:14px; }
            .meta-panel td { padding:6px 8px; vertical-align: top; border-bottom: 1px solid #f0f0f0; }
            .muted { color:#666; }
            .list { margin:6px 0 0 0; padding-left: 16px; max-height: 260px; overflow:auto; }
            code { background:#f3f4f6; padding:2px 4px; border-radius:4px; }
          </style>
        </head>
        <body>
          <div class="toolbar">
            <label class="switch"><input type="checkbox" id="toggle"><span class="slider"></span></label>
            <span class="label-text">Langform anzeigen</span>

            <label class="switch"><input type="checkbox" id="toggle-meta"><span class="slider"></span></label>
            <span class="label-text">Metadaten anzeigen</span>
          </div>

          <div class="layout">
            <div class="diagram-wrapper">
              <div class="mermaid active" id="diagram-short">%%SHORT%%</div>
              <div class="mermaid" id="diagram-full">%%FULL%%</div>
            </div>

            <aside class="meta-panel" id="meta-panel">
              %%META%%
            </aside>
          </div>
        </body>
        </html>
        """;
        html = html.replace("%%SHORT%%", shortCode);
        html = html.replace("%%FULL%%",  fullCode);
        html = html.replace("%%META%%",  metaHtml);
        writeToFile(fileName, html);
    }
}