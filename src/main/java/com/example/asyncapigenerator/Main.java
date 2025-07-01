package com.example.asyncapigenerator;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        // Standardordner verwenden, wenn kein Argument übergeben wurde
        String folderPath = "asyncapi"; // relativer Pfad zum Projektverzeichnis

        File inputFolder = new File(folderPath);
        if (!inputFolder.isDirectory()) {
            System.out.println("Kein Ordner gefunden: " + folderPath);
            return;
        }

        FileProcessor processor = new FileProcessor();
        processor.processFolder(inputFolder);

    }
}