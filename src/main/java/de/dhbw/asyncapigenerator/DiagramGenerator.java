package de.dhbw.asyncapigenerator;

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
            String label = useShortLabels ? AsyncAPIParseUtils.shortenName(name) : name;
            String alias = AsyncAPIParseUtils.sanitize(AsyncAPIParseUtils.extractCore(label));
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
                    .append(AsyncAPIParseUtils.escape(label))
                    .append("\"\n");
        }
        for (AsyncAPIData.Flow flow : data.getFlows()) {
            sb.append("    ") //neue zeile bzw einrückung
                    .append(nameToAlias.get(flow.from)) //alias von sender
                    .append("->>")//nachricht von links nach rechts (mermaid syntax)
                    .append(nameToAlias.get(flow.to))
                    .append(": ")
                    .append(AsyncAPIParseUtils.escape(flow.message))
                    .append("\n");
        }
        return sb.toString();
    }

    public static class DiagramResult {
        public final String mode; // "short" oder "full"
        public final String content; // mermaid text des Diagramms
        public DiagramResult(String mode, String content) { this.mode = mode; this.content = content; }
    }
}