package com.rodrigo.despesas.lancamento;

import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * O front monta os menus e os selects a partir daqui, em vez de duplicar as
 * listas em TypeScript e sair de sincronia com o back.
 */
@RestController
@RequestMapping("/api/v1")
public class ReferenciaController {

    public record TipoResponse(String nome, String rotulo, boolean comprometeReceita, boolean mensal) {
    }

    public record CategoriaResponse(String nome, String rotulo, String tipo) {
    }

    @GetMapping("/tipos")
    public List<TipoResponse> tipos() {
        return Arrays.stream(TipoLancamento.values())
                .map(tipo -> new TipoResponse(
                        tipo.name(), tipo.getRotulo(), tipo.comprometeReceita(), tipo.exigeRecorrencia()))
                .toList();
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias(@RequestParam(required = false) TipoLancamento tipo) {
        List<Categoria> categorias = tipo == null ? Arrays.asList(Categoria.values()) : Categoria.doTipo(tipo);

        return categorias.stream()
                .map(categoria -> new CategoriaResponse(
                        categoria.name(), categoria.getRotulo(), categoria.getTipo().name()))
                .toList();
    }
}
