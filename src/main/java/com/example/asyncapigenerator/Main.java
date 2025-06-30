package com.example.asyncapigenerator;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar generator.jar <asyncapi-folder>");
            return;
        }

        File inputFolder = new File(args[0]);
        if (!inputFolder.isDirectory()) {
            System.out.println("Kein Ordner gefunden: " + args[0]);
            return;
        }

        FileProcessor processor = new FileProcessor();
        processor.processFolder(inputFolder);
    }
}
