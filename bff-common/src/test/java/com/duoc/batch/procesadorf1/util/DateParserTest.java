package com.duoc.batch.procesadorf1.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DateParserTest {

    @Test
    void parseaFormatoISO() {
        assertThat(DateParser.parse("2024-06-30")).isEqualTo(LocalDate.of(2024, 6, 30));
    }

    @Test
    void parseaFormatoDiaMesAnio() {
        assertThat(DateParser.parse("03-04-2024")).isEqualTo(LocalDate.of(2024, 4, 3));
        assertThat(DateParser.parse("24/03/2024")).isEqualTo(LocalDate.of(2024, 3, 24));
    }

    @Test
    void rechazaFechasInvalidas() {
        assertThat(DateParser.parse("2024-13-01")).isNull();
        assertThat(DateParser.parse("no-es-una-fecha")).isNull();
        assertThat(DateParser.parse("")).isNull();
        assertThat(DateParser.parse(null)).isNull();
    }
}