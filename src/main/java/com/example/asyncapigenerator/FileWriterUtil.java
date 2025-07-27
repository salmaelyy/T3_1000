package com.example.asyncapigenerator;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class FileWriterUtil {

    public void writeToFile(String fileName, String content) throws Exception {
        Files.write(Paths.get(fileName), content.getBytes(StandardCharsets.UTF_8));
    }

    // HTML mit nur einem Mermaid-Diagramm (für einfache Fälle)
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
            body {
              margin: 0;
              padding: 0;
              font-family: sans-serif;
              background-color: #f8f8f8;
            }
            .mermaid-container {
              width: 100%;
              height: 100vh;
              overflow: auto;
              padding: 20px;
              box-sizing: border-box;
            }
            .mermaid {
              font-size: 14px;
            }
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

    // HTML mit Umschaltfunktion für Kurz- und Langform
    public void writeHtmlWithMermaidToggle(String fileName, String shortCode, String fullCode) throws Exception {
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
          
                    body {
                      font-family: sans-serif;
                      background-color: #f8f8f8;
                      padding: 20px;
                    }
                
                    .diagram-wrapper {
                      overflow: auto;
                      min-width: 1600px;
                    }
                
                    select {
                      margin-bottom: 20px;
                      padding: 5px 10px;
                      font-size: 14px;
                    }
                
                   .mermaid {
                             visibility: hidden;
                             position: absolute;
                             font-size: 12px;
                             line-height: 1.2;
                           }
                
                           .mermaid.active {
                             visibility: visible;
                             position: static;
                           }
                
                  </style>
        </head>
        <body>
          <label for="diagramSelect">Diagramm auswählen:</label>
          <select id="diagramSelect">
            <option value="short">Kurzform</option>
            <option value="full">Langform</option>
          </select>

          <div class="diagram-wrapper">
            <div class="mermaid active" id="diagram-short">
              %%SHORT%%
            </div>
            <div class="mermaid" id="diagram-full">
              %%FULL%%
            </div>
          </div>

          <script>
            const select = document.getElementById("diagramSelect");
            const shortDiv = document.getElementById("diagram-short");
            const fullDiv = document.getElementById("diagram-full");

            select.addEventListener("change", () => {
              shortDiv.classList.toggle("active", select.value === "short");
              fullDiv.classList.toggle("active", select.value === "full");
            });
          </script>
        </body>
        </html>
        """;

        html = html.replace("%%SHORT%%", shortCode);
        html = html.replace("%%FULL%%", fullCode);

        writeToFile(fileName, html);
    }
}
//TODO: Bei den langformen in der html werden die vierecke nicht mehr angezeigt