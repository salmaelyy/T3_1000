package com.example.asyncapigenerator;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        String folderPath = "asyncapi";
        File inputFolder = new File(folderPath);

        if (!inputFolder.isDirectory()) {
            System.out.println("Kein Ordner gefunden: " + folderPath);
            return;
        }

        System.out.println("\n====================================================================================================");
        System.out.println("Starte Verarbeitung des Ordners: " + inputFolder.getAbsolutePath());
        System.out.println("====================================================================================================\n");

        FileProcessor processor = new FileProcessor();
        processor.processFolder(inputFolder);

        System.out.println("====================================================================================================");
        System.out.println("Verarbeitung abgeschlossen.");
        System.out.println("====================================================================================================\n");
    }
}