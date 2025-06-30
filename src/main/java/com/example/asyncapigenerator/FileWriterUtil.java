package com.example.asyncapigenerator;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class FileWriterUtil {
    public void writeToFile(String fileName, String content) throws Exception {
        Files.write(Paths.get(fileName), content.getBytes(StandardCharsets.UTF_8));
    }

    public void writeHtmlWithMermaid(String fileName, String mermaidCode) throws Exception {
        String html = "<!DOCTYPE html>\n<html>\n<head>\n<script type=\"module\">" +
                "import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';" +
                "mermaid.initialize({ startOnLoad: true });</script>\n</head>\n<body>\n<div class=\"mermaid\">\n" +
                mermaidCode + "\n</div>\n</body>\n</html>";
        writeToFile(fileName, html);
    }
}
