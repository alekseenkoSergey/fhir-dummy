package org.example.fhirdummy.utility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class JsonTextTest {
    private final JsonMapper json = new JsonMapper();

    @Test
    void trimsTextInAllAccessors() {
        var source = json.readTree("{\"field\":\"  value \\t\\n\"}");

        assertEquals("value", JsonText.optional(source, "field"));
        assertEquals("value", JsonText.required(source, "field"));
        assertEquals("value", JsonText.identifier(source, "field"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"field\":null}", "{\"field\":\"\"}", "{\"field\":\" \\t\\n\"}"})
    void treatsMissingNullAndBlankTextAsAbsent(String input) {
        var source = json.readTree(input);

        assertNull(JsonText.optional(source, "field"));
        assertNull(JsonText.identifier(source, "field"));
        var exception = assertThrows(IllegalArgumentException.class, () -> JsonText.required(source, "field"));
        assertEquals("Expected nonempty text for field field", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"42", "-42", "0", "9223372036854775808"})
    void convertsIntegralIdentifiersWithoutLosingPrecision(String number) {
        assertEquals(number, JsonText.identifier(json.readTree("{\"field\":" + number + "}"), "field"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"42", "1.5", "true", "[]", "{}"})
    void rejectsNonTextValuesInTextAccessors(String value) {
        var source = json.readTree("{\"field\":" + value + "}");

        assertEquals("Expected text for field field",
                assertThrows(IllegalArgumentException.class, () -> JsonText.optional(source, "field")).getMessage());
        assertEquals("Expected text for field field",
                assertThrows(IllegalArgumentException.class, () -> JsonText.required(source, "field")).getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "true", "[]", "{}"})
    void rejectsIdentifiersThatAreNeitherTextNorIntegralNumbers(String value) {
        var source = json.readTree("{\"field\":" + value + "}");

        assertEquals("Expected text for field field",
                assertThrows(IllegalArgumentException.class, () -> JsonText.identifier(source, "field")).getMessage());
    }
}
