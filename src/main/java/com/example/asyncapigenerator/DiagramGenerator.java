package com.example.asyncapigenerator;

import java.util.*;

public class DiagramGenerator {

    public enum Mode { SHORT_ONLY, FULL_ONLY, BOTH } // Diagrammmodi

    //// Sammle alle Teilnehmer (from/to) aus den Flows
    public List<DiagramResult> generateMermaid(AsyncAPIData data) {
        Set<String> participants = new LinkedHashSet<>();
        for (AsyncAPIData.Flow flow : data.getFlows()) {
            participants.add(flow.from);
            participants.add(flow.to);
        }
        int participantCount = participants.size();
        Mode mode = participantCount > 5 ? Mode.BOTH : Mode.FULL_ONLY;

        List<DiagramResult> results = new ArrayList<>();
        if (mode == Mode.FULL_ONLY) {
            results.add(new DiagramResult("full", buildDiagram(data, false)));
        } else { // BOTH
            results.add(new DiagramResult("short", buildDiagram(data, true)));
            results.add(new DiagramResult("full", buildDiagram(data, false)));
        }
        return results; // liste von diagrammen
    }

    private String buildDiagram(AsyncAPIData data, boolean useShortLabels) {
        StringBuilder sb = new StringBuilder();
        sb.append("sequenceDiagram\n");

        Set<String> participants = new LinkedHashSet<>();
        for (AsyncAPIData.Flow flow : data.getFlows()) {
            participants.add(flow.from);
            participants.add(flow.to);
        }
        //alias → ein interner technischer Name (buchstaben, Zahlen, Unterstrich erlaubt).
        //label → das, was später im Diagramm angezeigt wird (kann Sonderzeichen enthalten)
        Map<String, String> nameToAlias = new HashMap<>();
        Map<String, Integer> aliasCount = new HashMap<>(); // dient zur Sicherheit, falls 2 Teilnehmer denselben Kernnamen haben

        for (String name : participants) {
            String label = useShortLabels ? shortenName(name) : name;
            String alias = sanitize(extractCore(label));
            String baseAlias = alias; //// basis für evtl. Nummerierung
            int count = aliasCount.getOrDefault(baseAlias, 0);
            while (nameToAlias.containsValue(alias)) {
                count++;
                alias = baseAlias + count;
            }
            aliasCount.put(baseAlias, count);
            nameToAlias.put(name, alias);
            //participant <alias> as "<Label>"
            sb.append("    participant ")
                    .append(alias)
                    .append(" as \"")
                    .append(escape(label))
                    .append("\"\n");
        }
        for (AsyncAPIData.Flow flow : data.getFlows()) {
            sb.append("    ") //neue zeile bzw einrückung
                    .append(nameToAlias.get(flow.from)) //alias von sender
                    .append("->>")//nachricht von links nach rechts (mermaid syntax)
                    .append(nameToAlias.get(flow.to))
                    .append(": ")
                    .append(escape(flow.message))
                    .append("\n");
        }
        return sb.toString();
    }
    // ==== Hilfen ====
    private String shortenName(String name) {
        String base = extractCore(name);
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

    private String extractCore(String raw) { return raw.replaceAll("[^a-zA-Z0-9_]", ""); }
    private String sanitize(String input)   { return input.replaceAll("[^a-zA-Z0-9_]", "_"); }
    // statt " ersetzt sie durch \" sonst gehen labelns in mermaid kaputt
    private String escape(String s)         { return s.replace("\"","\\\""); }

    public static class DiagramResult {
        public final String mode; // "short" oder "full"
        public final String content; // mermaid text des Diagramms
        public DiagramResult(String mode, String content) { this.mode = mode; this.content = content; }
    }
}