package de.dhbw.asyncapigenerator;

import java.net.URL;
import java.nio.file.Path;

/** Gemeinsame Helfer für Test-Resourcen. */
public final class TestResources {
    private TestResources() {}

    /**
     * Liefert den absoluten Dateisystempfad zu einer Ressource unter src/test/resources.
     * Beispiel: asyncapiPath("v3-ops-kafka.yaml")
     */
    public static String asyncapiPath(String name) {
        return resourcePath("/asyncapi/" + name);
    }

    /**
     * Generischer Loader: nimmt einen absoluten Ressourcenpfad wie "/asyncapi/v2-kafka.yaml"
     * und gibt den Dateisystempfad zurück (für APIs, die einen Pfad verlangen).
     */
    public static String resourcePath(String absoluteResourcePath) {
        if (absoluteResourcePath == null || !absoluteResourcePath.startsWith("/")) {
            throw new IllegalArgumentException("Pfad muss mit '/' beginnen, z.B. /asyncapi/v2-kafka.yaml");
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url = cl.getResource(absoluteResourcePath.substring(1)); // ohne führendes '/'
        if (url == null) {
            throw new IllegalArgumentException("Test-Resource fehlt: " + absoluteResourcePath);
        }
        try {
            return Path.of(url.toURI()).toString();
        } catch (Exception e) {
            throw new IllegalStateException("Ungültige Resource-URI: " + url, e);
        }
    }
}
