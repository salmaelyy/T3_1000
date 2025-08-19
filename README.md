# AsyncAPI → Mermaid Diagram Generator (Java, Maven)

Dieses Projekt verarbeitet **AsyncAPI**-Spezifikationen (v2.x und v3.x) und erzeugt daraus **Mermaid Sequence-Diagramme** inklusive **HTML-Ansicht** mit Toggle (Kurz/Lang) und **Metadaten-Panel**.

> ⚠️ Fokus ist ein schlanker Parser für die für die Diagrammerstellung relevanten Teile der Spezifikation – kein vollumfänglicher AsyncAPI-Interpreter.

---

## Inhalte

- [Funktionen](#funktionen)
- [Unterstützte AsyncAPI-Teile](#unterstützte-asyncapi-teile)
- [Kafka-Bindings (Hinweistexte)](#kafka-bindings-hinweistexte)
- [Flow-Ermittlung (Matching & Bridging)](#flow-ermittlung-matching--bridging)
- [Ausgaben & Dateistruktur](#ausgaben--dateistruktur)
- [Voraussetzungen](#voraussetzungen)
- [Build & Ausführung](#build--ausführung)
- [Tests & Testdaten](#tests--testdaten)
- [Projektstruktur](#projektstruktur)
- [Bekannte Einschränkungen](#bekannte-einschränkungen)
- [Tipps zur Fehlerbehebung](#tipps-zur-fehlerbehebung)
- [Weiterentwicklung](#weiterentwicklung)
- [Lizenz](#lizenz)

---

## Funktionen

- **Parsing AsyncAPI v2.x & v3.x**
  - v1 wird explizit abgewiesen.
- **Flow-Erzeugung** aus Producer/Consumer (siehe Matching & Bridging unten).
- **Mermaid Sequence-Diagramm**-Generierung
  - Automatisch **Kurz + Lang** ab **>5 Teilnehmern**, sonst nur Lang.
  - **Kurzlabels** für Teilnehmer zur besseren Übersicht (technisch saubere Aliase).
- **HTML-Export** mit
  - Toggle **Kurz/Lang**,
  - Toggle **Metadaten** (Titel, Version, Beschreibung, Anzahl Flows/Teilnehmer, Liste der Teilnehmer).
- **Optionale Kafka-Infos** (groupId/clientId) als **Hinweise** im Diagramm (unterschiedlich für v2/v3, siehe unten).
- **Datei-Batchverarbeitung** eines Verzeichnisses mit `.yaml/.yml` Dateien.

---

## Unterstützte AsyncAPI-Teile

### Versionen
- **v2.x**: Verarbeitung über `AsyncAPIParser`
- **v3.x**: Verarbeitung über `AsyncAPIv3Parser`
- **v1.x**: nicht unterstützt → `IllegalStateException`

### Erwartete Felder (vereinfachter Ausschnitt)

**v2.x**
- `info.title`, `info.version`, `info.description` (optional; für Metadaten)
- `channels`
  - pro Channel: Knoten, die mit **`publish`** bzw. **`subscribe`** **beginnen** (z. B. `publish`, `publish__1`, …)
    - `operationId` (Producer/Consumer-Name im Diagramm)
    - `message.$ref` (z. B. `#/components/messages/MyMessage`)
    - optional: `bindings.kafka.{groupId, clientId}`

**v3.x**
- `info.title`, `info.version`, `info.description` (optional; für Metadaten)
- `channels` (optional **zusätzlich** zu `operations`):
  - evtl. `publish`/`subscribe`-Knoten analog v2 (werden berücksichtigt)
  - optional: `bindings.kafka.{groupId, clientId}` (Channel-Level)
- `operations` (**primär** in v3)
  - je Operation:
    - `action`: `send` **oder** `receive`
    - `operationId`: Name im Diagramm
    - `channel.$ref`: Verweis auf Channel
    - `message.$ref`: Verweis auf Message
    - optional: `bindings.kafka.{groupId, clientId}` (Operation-Level)

> In v3 werden **Channel-** und **Operation-Bindings** gemerged (Operation > Channel).

---

## Kafka-Bindings (Hinweistexte)

Wenn **`bindings.kafka`** vorhanden ist, werden **groupId/clientId** als reine Hinweise in den Diagrammen ergänzt (keine Logikänderung).

- **AsyncAPI v2.x**: Hinweise werden **an den *Message*-Namen** angehängt, z. B.
  - `UserSignedUp (groupId=analytics-group, clientId=analytics-service)`
- **AsyncAPI v3.x**: Hinweise werden **an die *operationId* (Teilnehmer)** angehängt, z. B.
  - `recvSignup (groupId=analytics-group, clientId=analytics-svc)`

> In **Kurzdiagrammen** werden Teilnehmernamen **verkürzt** (nur alphanumerische Kernteile); dabei können Klammern/`groupId=`/`clientId=` im Label entfallen. Die **Langform** enthält die vollständigen Hinweise.

---

## Flow-Ermittlung (Matching & Bridging)

Es gibt **zwei** Arten von Kanten/Flows:

1. **Direkter Flow (Producer → Consumer)**  
   Entsteht **nur**, wenn **Channel** *und* **Message** übereinstimmen.  
   - v2: `publish` und `subscribe` auf **demselben Channel** und **derselben Message-Ref**  
   - v3: `send` und `receive` Operationen referenzieren **denselben Channel** und **dieselbe Message-Ref**

2. **Bridging über Topic-Knoten**  
   Zusätzlich werden für jeden Channel **Topic-Knoten** erzeugt, wodurch auch reine „Producer → Topic“ und „Topic → Consumer“-Kanten sichtbar werden:
   - `ProducerOp → topic:<channel>`
   - `topic:<channel> → ConsumerOp`

**Wichtig:** Ein **direkter** Producer→Consumer-Flow wird **nicht** erzeugt, wenn nur die Message gleich ist, der **Channel aber unterschiedlich**. In diesem Fall sieht man **nur Bridging** über die entsprechenden `topic:<channel>`-Knoten.

---

## Ausgaben & Dateistruktur

Standard-Ausgabe liegt **im Projekt-Root** unter `generated-sources/` (nicht unter `target/`).  
Der Basisordner kann via System-Property überschrieben werden: `-Dout.dir=/absoluter/pfad`.

```
generated-sources/
├── mmd/
│   ├── <name>_short.mmd   # nur, wenn >5 Teilnehmer (Kurz + Lang)
│   └── <name>_full.mmd    # immer (Lang)
└── html/
    └── <name>.html        # Toggle Kurz/Lang + Metadaten
```

**HTML-Viewer**  
- Mermaid wird per **CDN** (v10, ESM) geladen. Offline-Nutzung erfordert eigenes Bundling.

**Benennung**
- `<name>` entspricht dem YAML-Dateinamen ohne Erweiterung.

---

## Voraussetzungen

- **Java 17**
- **Maven 3.8+**
- (Optional) **bash** für die Hilfsskripte unter `scripts/`

---

## Build & Ausführung

### 1) Tests ausführen
```bash
mvn test
```

### 2) Generator starten

**Variante A – Skript (empfohlen):**
```bash
./scripts/run.sh
```
- Baut das Projekt (Tests übersprungen) und startet `Main`.

**Variante B – Main Klasse:**
- Es gibt eine `Main`-Klasse, die den **Ordner `asyncapi/` im Projekt-Root** verarbeitet.

> ⚠ Hinweis: Das Skript akzeptiert zwar einen Pfadparameter, die aktuelle `Main`-Klasse liest jedoch fest `asyncapi/`. Falls du andere Ordner verarbeiten willst, bitte `Main` anpassen.



### 3) Aufräumen
```bash
./scripts/clean.sh
```
- Ruft `mvn clean` auf und löscht zusätzlich `generated-sources/{mmd,html}`.

---

## Tests & Testdaten

Die Unit-/Integrationstests liegen unter `src/test/java` und nutzen Test-YAMLs in `src/test/resources/asyncapi/`:

- `v2-kafka.yaml`  
  - V2-Beispiel mit Kafka-Bindings → direkte Flows (gleiches Channel+Message) **und** Bridging-Kanten.
- `v3-ops-kafka.yaml`  
  - V3-Operations (send/receive) + Kafka-Bindings (Merge: Operation > Channel). Hinweise an Teilnehmernamen.
- `channels_no_pubsub.yaml`  
  - Keine publish/subscribe/send/receive-Kombination → `IllegalStateException` wird erwartet.
- `v2-separated-same-msg-different-channels.yaml`  
  - Gleiche Message, **unterschiedliche Channels** → **nur Bridging**, **kein** direkter Flow.
- `v2-separated-different-messages.yaml`  
  - Unterschiedliche Messages → **nur Bridging**.

Die Tests prüfen u. a.:
- Erkennung der Spezversions (v2/v3)
- Generierung von direkten Flows und Bridging-Flows gemäß Matching-Regeln
- Anzeige der Kafka-Hinweise (v2: an Message, v3: am Teilnehmer)
- Integration: Erstellung der Ausgabedateien in `generated-sources/{mmd,html}`

---

## Projektstruktur

```
src/
├── main/java/com/example/asyncapigenerator/
│   ├── AsyncAPIData.java         # Model (Metadaten + Flows)
│   ├── AsyncAPIParser.java       # Parser für v2.x (+ Bridging-Logik, Kafka-Hinweise an Messages)
│   ├── AsyncAPIv3Parser.java     # Parser für v3.x (send/receive; Kafka-Hinweise an Teilnehmer; Merge op>channel)
│   ├── DiagramGenerator.java     # Mermaid-Generator (Kurz/Lang, Aliase, SequenceDiagram)
│   ├── FileWriterUtil.java       # Schreiben MMD/HTML, HTML-Template inkl. Metadaten-Panel
│   ├── FileProcessor.java        # Batch-Verarbeitung eines Ordners (Standard: ProjektRoot/asyncapi)
│   └── Main.java                 # Einstiegspunkt
├── test/java/com/example/asyncapigenerator/
│   ├── AsyncAPIParserTest.java
│   ├── AsyncAPIv3ParserTest.java
│   ├── DiagramGeneratorTest.java
│   └── FileProcessorIntegrationTest.java
└── test/resources/asyncapi/
    ├── v2-kafka.yaml
    ├── v3-ops-kafka.yaml
    ├── channels_no_pubsub.yaml
    ├── v2-separated-same-msg-different-channels.yaml
    └── v2-separated-different-messages.yaml
```

**Hilfsskripte**
- `scripts/run.sh` – Build + Start
- `scripts/clean.sh` – Aufräumen (Maven clean + generierte Diagramme löschen)

---

## Bekannte Einschränkungen

- Kein vollständiger AsyncAPI-Support (z. B. **servers**, **security**, **bindings** außer Kafka/ID-Felder, **payloads/schemas** werden **nicht** gerendert).
- **Input-Ordner** ist aktuell in `Main` **hart auf `asyncapi/`** gesetzt. Das Skript erlaubt Parameter, die die aktuelle `Main` jedoch nicht nutzt.
- **Kurzlabels** entfernen Nicht-Alphanumerisches – Kafka-Hinweise sind dann nur in der **Langform** sichtbar.
- **Mermaid via CDN**: Offline-Betrieb erfordert zusätzliches Bundling/Hosting.
- Keine dedizierte **CLI** mit Optionen (z. B. zur Auswahl des Ausgabeverzeichnisses) – nur `-Dout.dir=…` für den Ausgabepfad.

---

## Tipps zur Fehlerbehebung

- **„The goal … requires a project … no POM in this directory“**  
  → Maven aus dem **Projekt-Root** aufrufen (wo die `pom.xml` liegt), nicht aus `scripts/`.

- **„KEIN FLOW …“ (IllegalStateException)**  
  → Es wurden keine passenden Paare gefunden (`publish/subscribe` in v2 bzw. `send/receive` in v3 mit *gleichem Channel + gleicher Message*). Prüfe die YAMLS.

- **Keine Ausgabedateien**  
  - Liegen die Input-Dateien im erwarteten Ordner? Standard: `./asyncapi/*.yaml`  
  - Schreibrechte im Zielordner vorhanden?  
  - Alternativ `-Dout.dir=/pfad` setzen.

- **Kafka-Hinweise fehlen**  
  - v2: nur wenn `bindings.kafka` am jeweiligen `publish/subscribe`-Knoten vorhanden ist. Anzeige an der **Message**.
  - v3: Merge aus Operation- und Channel-Bindings; Anzeige an den **Teilnehmern** (operationId).

---

## Lizenz

...

---

## Beispiel (v3, stark gekürzt)

```yaml
asyncapi: 3.0.0
info:
  title: Demo
  version: 1.0.0
channels:
  signup:
    messages:
      userSignedUp:
        $ref: '#/components/messages/UserSignedUp'
operations:
  sendSignup:
    action: send
    operationId: Auth_sendSignup
    channel: { $ref: "#/channels/signup" }
    message: { $ref: "#/channels/signup/messages/userSignedUp" }
    bindings:
      kafka: { clientId: auth-svc }
  recvSignup:
    action: receive
    operationId: Analytics_recvSignup
    channel: { $ref: "#/channels/signup" }
    message: { $ref: "#/channels/signup/messages/userSignedUp" }
    bindings:
      kafka: { groupId: analytics-group, clientId: analytics-svc }
```

Ergebnis (Langform, sinngemäß): `Auth_sendSignup (clientId=auth-svc) → Analytics_recvSignup (groupId=analytics-group, clientId=analytics-svc) : userSignedUp`
