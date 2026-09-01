package com.duoc.batch.procesadorf1.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Parser de fechas tolerante a multiples formatos presentes en los CSV.
 * Soportados: yyyy-MM-dd, dd-MM-yyyy, dd/MM/yyyy, yyyy/MM/dd.
 * Devuelve null si la fecha no es parseable o es invalida (p.ej. 2024-13-01).
 */
public final class DateParser {

    private DateParser() {
    }

    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ISO_LOCAL_DATE,          // yyyy-MM-dd
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    public static LocalDate parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(v, formatter);
            } catch (DateTimeParseException ignored) {
                // intentar siguiente formato
            }
        }
        return null;
    }
}
