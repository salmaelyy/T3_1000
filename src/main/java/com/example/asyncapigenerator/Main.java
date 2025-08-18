// src/main/java/com/example/asyncapigenerator/Main.java
package com.example.asyncapigenerator;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        String folderPath = (args != null && args.length > 0) ? args[0] : "asyncapi";
        File inputFolder = new File(folderPath);

        if (!inputFolder.isDirectory()) {
            System.out.println("Kein Ordner gefunden: " + inputFolder.getAbsolutePath());
            return;
        }

        System.out.println("\n====================================================================================================");
        System.out.println("Starte Verarbeitung des Ordners: " + inputFolder.getAbsolutePath());
        System.out.println("====================================================================================================\n");

        new FileProcessor().processFolder(inputFolder);

        System.out.println("====================================================================================================");
        System.out.println("Verarbeitung abgeschlossen.");
        System.out.println("====================================================================================================\n");
    }
}