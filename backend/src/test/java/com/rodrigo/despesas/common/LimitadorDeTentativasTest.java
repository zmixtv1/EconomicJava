package com.rodrigo.despesas.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.rodrigo.despesas.config.SegurancaProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LimitadorDeTentativasTest {

    private static LimitadorDeTentativas comLimite(int limite) {
        return new LimitadorDeTentativas(new SegurancaProperties(limite));
    }

    @Test
    @DisplayName("libera até o limite e barra a partir da tentativa seguinte")
    void barraDepoisDoLimite() {
        LimitadorDeTentativas limitador = comLimite(3);

        assertThat(limitador.permitir("10.0.0.1")).isTrue();
        assertThat(limitador.permitir("10.0.0.1")).isTrue();
        assertThat(limitador.permitir("10.0.0.1")).isTrue();

        assertThat(limitador.permitir("10.0.0.1")).isFalse();
        assertThat(limitador.permitir("10.0.0.1")).isFalse();
    }

    @Test
    @DisplayName("a cota é por origem: um IP bloqueado não afeta os outros")
    void cotaEhPorOrigem() {
        LimitadorDeTentativas limitador = comLimite(2);

        limitador.permitir("10.0.0.1");
        limitador.permitir("10.0.0.1");
        assertThat(limitador.permitir("10.0.0.1")).isFalse();

        // Quem nunca tentou continua entrando normalmente.
        assertThat(limitador.permitir("10.0.0.2")).isTrue();
        assertThat(limitador.permitir("203.0.113.7")).isTrue();
    }

    @Test
    @DisplayName("limite de uma tentativa já barra a segunda")
    void limiteDeUmaTentativa() {
        LimitadorDeTentativas limitador = comLimite(1);

        assertThat(limitador.permitir("10.0.0.9")).isTrue();
        assertThat(limitador.permitir("10.0.0.9")).isFalse();
    }
}
