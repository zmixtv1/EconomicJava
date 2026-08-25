import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { FormularioLancamento } from './FormularioLancamento';
import { TabelaLancamentos } from './TabelaLancamentos';
import type {
  CategoriaOpcao,
  Lancamento,
  LancamentoInput,
  ResumoMensal,
  TipoLancamento,
  TipoOpcao,
} from '../types';

interface Props {
  tipo: TipoLancamento;
  rotulo: string;
  mes: string;
  tipos: TipoOpcao[];
  resumo: ResumoMensal | null;
  lancamentos: Lancamento[];
  carregando: boolean;
  criar: (lancamento: LancamentoInput) => Promise<void>;
  atualizar: (id: number, lancamento: LancamentoInput) => Promise<void>;
  remover: (id: number) => Promise<void>;
}

const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

function totalDoTipo(resumo: ResumoMensal | null, tipo: TipoLancamento): number {
  if (!resumo) return 0;
  switch (tipo) {
    case 'RECEITA': return resumo.receitas;
    case 'DESPESA_FIXA': return resumo.despesasFixas;
    case 'DESPESA_VARIAVEL': return resumo.despesasVariaveis;
    case 'ECONOMIA': return resumo.economia;
    case 'DIVIDA': return resumo.dividas;
  }
}

export function SecaoTipo({
  tipo, rotulo, mes, tipos, resumo, lancamentos, carregando, criar, atualizar, remover,
}: Props) {
  const [categorias, setCategorias] = useState<CategoriaOpcao[]>([]);
  const [emEdicao, setEmEdicao] = useState<Lancamento | null>(null);

  useEffect(() => {
    setEmEdicao(null);
    void api.listarCategorias(tipo).then(setCategorias).catch(() => setCategorias([]));
  }, [tipo]);

  async function salvar(lancamento: LancamentoInput) {
    if (emEdicao) {
      await atualizar(emEdicao.id, lancamento);
      setEmEdicao(null);
    } else {
      await criar(lancamento);
    }
  }

  return (
    <>
      <section className="cartao destaque compacto">
        <span className="rotulo">{rotulo} no mês</span>
        <strong className="heroi menor">{MOEDA.format(totalDoTipo(resumo, tipo))}</strong>
        <span className="subtitulo">
          {lancamentos.length === 1 ? '1 lançamento' : `${lancamentos.length} lançamentos`}
        </span>
      </section>

      <FormularioLancamento
        tipo={tipo}
        tipos={tipos}
        categorias={categorias}
        emEdicao={emEdicao}
        aoSalvar={salvar}
        aoCancelar={() => setEmEdicao(null)}
        mes={mes}
      />

      <TabelaLancamentos
        lancamentos={lancamentos}
        carregando={carregando}
        aoEditar={setEmEdicao}
        aoRemover={remover}
      />
    </>
  );
}
