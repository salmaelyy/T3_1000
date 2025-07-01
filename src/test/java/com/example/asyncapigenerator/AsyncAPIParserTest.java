package com.example.asyncapigenerator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AsyncAPIParserTest {

    @Test
    void testParseValidV2File() throws Exception {
        AsyncAPIParser parser = new AsyncAPIParser();
        String filePath = "src/test/resources/asyncapi/valid_v2.yaml";
        AsyncAPIData result = parser.parseYaml(filePath);
        assertNotNull(result);
        assertEquals("2.6.0", result.getVersion());
    }



    @Test
    void testParseFileWithNoFlows() {
        AsyncAPIParser parser = new AsyncAPIParser();
        String filePath = "src/test/resources/asyncapi/no_flows.yaml";
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            parser.parseYaml(filePath);
        });
        assertTrue(exception.getMessage().toUpperCase().contains("KEIN FLOW"));
    }

    @Test
    void testParseInvalidYamlThrowsException() {
        AsyncAPIParser parser = new AsyncAPIParser();
        String filePath = "src/test/resources/asyncapi/invalid.yaml";
        assertThrows(Exception.class, () -> parser.parseYaml(filePath));
    }

    @Test
    void testParseFileWithMissingChannels() {
        AsyncAPIParser parser = new AsyncAPIParser();
        String filePath = "src/test/resources/asyncapi/missing_channels.yaml";
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            parser.parseYaml(filePath);
        });
        assertTrue(exception.getMessage().toLowerCase().contains("channels"));
    }
}
