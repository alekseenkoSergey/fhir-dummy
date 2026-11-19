package org.example.fhirdummy.utility;

import static org.example.fhirdummy.utility.Constants.ID_DELIMITER;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class Ids {
    private Ids() {}

    public static String join(String... parts) {
        return Arrays.stream(parts)
                .map(part -> part == null ? "" : part)
                .collect(Collectors.joining(ID_DELIMITER));
    }
}
