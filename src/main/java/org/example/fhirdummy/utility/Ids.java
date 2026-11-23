package org.example.fhirdummy.utility;

import static org.example.fhirdummy.utility.Constants.ID_DELIMITER;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Builds composite identifiers from clinic, patient, and device information using the shared delimiter.
 * Null parts become empty strings, so callers can construct references consistently even when a part is missing.
 */
public final class Ids {
    private Ids() {}

    public static String join(String... parts) {
        return Arrays.stream(parts)
                .map(part -> part == null ? "" : part)
                .collect(Collectors.joining(ID_DELIMITER));
    }
}
