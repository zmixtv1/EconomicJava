package com.rodrigo.despesas.common;

import com.rodrigo.despesas.config.SegurancaProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Contador de tentativas por IP, em janela fixa de um minuto.
 *
 * Existe para que força bruta de senha e varredura de e-mails deixem de ser
 * gratuitas: sem isso, alguém testa milhares de senhas por minuto contra
 * /auth/login e milhares de e-mails contra /auth/registro.
 *
 * É em memória e por instância — adequado para o desenho atual (um contêiner
 * na VPS). Se um dia a API rodar replicada, isto precisa virar Redis, senão o
 * limite se multiplica pelo número de instâncias.
 */
@Component
public class LimitadorDeTentativas {

    private static final Duration JANELA = Duration.ofMinutes(1);

    /** Teto de IPs rastreados, para o mapa não crescer sem fim sob varredura distribuída. */
    private static final int MAXIMO_DE_CHAVES = 20_000;

    private record Contagem(Instant inicio, AtomicInteger tentativas) {
    }

    private final Map<String, Contagem> porOrigem = new ConcurrentHashMap<>();
    private final int limite;

    public LimitadorDeTentativas(SegurancaProperties propriedades) {
        this.limite = propriedades.tentativasPorMinuto();
    }

    /** Devolve false quando a origem estourou a cota do minuto corrente. */
    public boolean permitir(String origem) {
        Instant agora = Instant.now();

        if (porOrigem.size() > MAXIMO_DE_CHAVES) {
            porOrigem.values().removeIf(contagem -> expirou(contagem, agora));
        }

        Contagem contagem = porOrigem.compute(origem, (chave, atual) ->
                atual == null || expirou(atual, agora)
                        ? new Contagem(agora, new AtomicInteger())
                        : atual);

        return contagem.tentativas().incrementAndGet() <= limite;
    }

    private static boolean expirou(Contagem contagem, Instant agora) {
        return Duration.between(contagem.inicio(), agora).compareTo(JANELA) >= 0;
    }

    public int limitePorMinuto() {
        return limite;
    }
}
