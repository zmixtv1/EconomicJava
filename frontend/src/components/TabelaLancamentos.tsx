import { useState } from 'react';
import type { Lancamento } from '../types';

interface Props {
  lancamentos: Lancamento[];
  carregando: boolean;
  aoEditar: (lancamento: Lancamento) => void;
  aoRemover: (id: number) => Promise<void>;
}

const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

function formatarData(iso: string): string {
  // Sem new Date(iso): isso interpretaria como UTC e mostraria o dia anterior.
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
}

function formatarMes(iso: string): string {
  const [ano, mes] = iso.split('-');
  return `${mes}/${ano}`;
}

/** "Todo mês desde 03/2026", "Parcela 7 de 48", "15/08/2026". */
function quando(lancamento: Lancamento): string {
  if (!lancamento.recorrente) {
    return formatarData(lancamento.data ?? '');
  }
  if (lancamento.parcelaAtual != null && lancamento.parcelas != null) {
    return `Parcela ${lancamento.parcelaAtual} de ${lancamento.parcelas}`;
  }
  const inicio = lancamento.vigenciaInicio ? formatarMes(lancamento.vigenciaInicio) : '';
  return lancamento.vigenciaFim
    ? `Mensal, ${inicio} a ${formatarMes(lancamento.vigenciaFim)}`
    : `Todo mês desde ${inicio}`;
}

export function TabelaLancamentos({ lancamentos, carregando, aoEditar, aoRemover }: Props) {
  const [removendo, setRemovendo] = useState<number | null>(null);

  if (!carregando && lancamentos.length === 0) {
    return (
      <div className="cartao vazio">
        <p>Nada lançado neste mês.</p>
      </div>
    );
  }

  async function remover(lancamento: Lancamento) {
    const aviso = lancamento.recorrente
      ? `Remover "${lancamento.descricao}"? Ele sai de todos os meses, não só deste.`
      : `Remover "${lancamento.descricao}"?`;

    if (!confirm(aviso)) {
      return;
    }
    setRemovendo(lancamento.id);
    try {
      await aoRemover(lancamento.id);
    } finally {
      setRemovendo(null);
    }
  }

  return (
    <div className="cartao tabela-container">
      <table className="tabela">
        <thead>
          <tr>
            <th>Descrição</th>
            <th>Categoria</th>
            <th>Quando</th>
            <th className="direita">Valor</th>
            <th aria-label="Ações" />
          </tr>
        </thead>
        <tbody>
          {lancamentos.map((lancamento) => (
            <tr key={lancamento.id} className={removendo === lancamento.id ? 'removendo' : undefined}>
              <td data-rotulo="Descrição">
                {lancamento.descricao}
                {lancamento.recorrente && <span className="selo">mensal</span>}
              </td>
              <td data-rotulo="Categoria">
                <span className="etiqueta">{lancamento.categoriaRotulo}</span>
              </td>
              <td data-rotulo="Quando">{quando(lancamento)}</td>
              <td data-rotulo="Valor" className="direita valor">
                {MOEDA.format(lancamento.valor)}
              </td>
              <td className="direita">
                <button
                  type="button"
                  className="botao pequeno"
                  onClick={() => aoEditar(lancamento)}
                  disabled={removendo === lancamento.id}
                >
                  Editar
                </button>
                <button
                  type="button"
                  className="botao pequeno perigo"
                  onClick={() => void remover(lancamento)}
                  disabled={removendo === lancamento.id}
                >
                  {removendo === lancamento.id ? '…' : 'Remover'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
