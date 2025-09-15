package de.dhbw.asyncapigenerator;

import com.fasterxml.jackson.databind.JsonNode;

public class AsyncAPIParseUtils {

    //"#/components/messages/OrderCreated" -> "OrderCreated"
    static String extractMessageName(String ref) {
          if (ref != null && ref.contains("/")) return ref.substring(ref.lastIndexOf("/") + 1);
          return (ref == null || ref.isBlank()) ? "UnknownMessage" : ref;
      }

    static String extractRefName(String ref) {
        if (ref == null || ref.isEmpty()) return "UnknownChannel";
        if (ref.contains("/")) return ref.substring(ref.lastIndexOf("/") + 1);
        return ref;
      }

    static String valueOrNull(JsonNode n) {
        return (n == null || n.isMissingNode() || n.isNull()) ? null : n.asText(null);
    }

    // wenn a da ist nimm a sonst b für bindings
    static String firstNonNull(String a, String b) { return a != null ? a : b; }

    //wenn string null/leer dann - sonst original text
    static String nullToDash(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    //„sichere“ Varianten, die der Browser als reines Zeichen anzeigt (könnt edie zeichen sonst falsch interpretieren
    static String esc(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    static String extractCore(String raw) { return raw.replaceAll("[^a-zA-Z0-9_]", ""); }

    static String sanitize(String input)   { return input.replaceAll("[^a-zA-Z0-9_]", "_"); }

    // statt " ersetzt sie durch \" sonst gehen labelns in mermaid kaputt
    static String escape(String s)         { return s.replace("\"","\\\""); }

    static String shortenName(String name) {
        String base = AsyncAPIParseUtils.extractCore(name);
        if (base.isEmpty()) return "N";
        String first7 = base.length() > 7 ? base.substring(0, 7) : base; //kürzen
        char firstUpper = Character.toUpperCase(base.charAt(0));
        char lastUpper = 0;
        for (int i = base.length() - 1; i >= 1; i--) { //rückwärts
            char c = base.charAt(i);
            if (Character.isUpperCase(c)) { lastUpper = c; break; }
        }
        String suffix = (lastUpper != 0 && lastUpper != firstUpper) ? String.valueOf(lastUpper) : "";// letzte Großbuchstabe != wie der erste, wird er als Suffix genommen
        boolean shortened = base.length() > 7 || !suffix.isEmpty(); //als kurz markieren
        return shortened ? first7 + suffix + "." : first7;
    }

}
