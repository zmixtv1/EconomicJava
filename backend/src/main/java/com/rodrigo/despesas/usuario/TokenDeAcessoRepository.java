package com.rodrigo.despesas.usuario;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenDeAcessoRepository extends JpaRepository<TokenDeAcesso, Long> {

    Optional<TokenDeAcesso> findByTokenHash(String tokenHash);

    /**
     * Ao emitir um token novo, os anteriores do mesmo tipo deixam de valer.
     * Sem isso, um link antigo interceptado continuaria funcionando depois de
     * a pessoa pedir outro.
     */
    @Modifying
    @Query("""
            update TokenDeAcesso t
               set t.usadoEm = :agora
             where t.usuario.id = :usuarioId
               and t.tipo = :tipo
               and t.usadoEm is null
            """)
    int invalidarAnteriores(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") TokenDeAcesso.Tipo tipo,
            @Param("agora") Instant agora);
}
