import { useCallback, useEffect, useState } from 'react';
import { api, ApiError } from '../api/client';
import type { Lancamento, LancamentoInput, ResumoMensal, TipoLancamento } from '../types';

interface Estado {
  resumo: ResumoMensal | null;
  lancamentos: Lancamento[];
  carregando: boolean;
  erro: string | null;
}

const ESTADO_INICIAL: Estado = { resumo: null, lancamentos: [], carregando: true, erro: null };

/**
 * O resumo e a lista são buscados juntos porque precisam refletir exatamente o
 * mesmo mês — se viessem em momentos diferentes, o total do topo poderia não
 * bater com as linhas logo abaixo.
 */
export function useOrcamento(mes: string, tipo: TipoLancamento | null, ativo: boolean) {
  const [estado, setEstado] = useState<Estado>(ESTADO_INICIAL);

  const carregar = useCallback(async () => {
    if (!ativo) {
      return;
    }
    setEstado((anterior) => ({ ...anterior, carregando: true, erro: null }));
    try {
      const [resumo, lancamentos] = await Promise.all([
        api.obterResumo(mes),
        api.listarLancamentos(mes, tipo ?? undefined),
      ]);
      setEstado({ resumo, lancamentos, carregando: false, erro: null });
    } catch (problema) {
      setEstado({
        ...ESTADO_INICIAL,
        carregando: false,
        erro: problema instanceof ApiError ? problema.message : 'Erro inesperado ao carregar os dados.',
      });
    }
  }, [mes, tipo, ativo]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const criar = useCallback(
    async (lancamento: LancamentoInput) => {
      await api.criarLancamento(lancamento);
      await carregar();
    },
    [carregar],
  );

  const atualizar = useCallback(
    async (id: number, lancamento: LancamentoInput) => {
      await api.atualizarLancamento(id, lancamento);
      await carregar();
    },
    [carregar],
  );

  const remover = useCallback(
    async (id: number) => {
      await api.removerLancamento(id);
      await carregar();
    },
    [carregar],
  );

  return { ...estado, recarregar: carregar, criar, atualizar, remover };
}
