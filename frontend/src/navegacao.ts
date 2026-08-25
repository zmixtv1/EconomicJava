import { useEffect, useState } from 'react';
import {
  IconeDivida,
  IconeEconomia,
  IconeFixa,
  IconePainel,
  IconeReceita,
  IconeVariavel,
} from './components/Icones';
import type { TipoLancamento } from './types';

export interface Secao {
  chave: string;
  rotulo: string;
  /** Frase curta que explica o passo, mostrada no topo da seção. */
  ajuda: string;
  Icone: (props: { className?: string }) => React.JSX.Element;
  /** Nulo na visão geral, que não é a aba de um tipo só. */
  tipo: TipoLancamento | null;
}

/**
 * A ordem é o roteiro de montagem do orçamento: primeiro o que entra, depois
 * o que já tem destino certo, e só no fim o resumo — que só faz sentido
 * depois que os números existem.
 */
export const SECOES: Secao[] = [
  {
    chave: 'receitas',
    rotulo: 'Receitas',
    ajuda: 'Comece pelo que entra no mês: salário, freelas, rendimentos.',
    Icone: IconeReceita,
    tipo: 'RECEITA',
  },
  {
    chave: 'despesas-fixas',
    rotulo: 'Despesas fixas',
    ajuda: 'O que se repete todo mês com valor previsível: aluguel, contas, assinaturas.',
    Icone: IconeFixa,
    tipo: 'DESPESA_FIXA',
  },
  {
    chave: 'despesas-variaveis',
    rotulo: 'Despesas variáveis',
    ajuda: 'Os gastos deste mês em particular: mercado, transporte, lazer.',
    Icone: IconeVariavel,
    tipo: 'DESPESA_VARIAVEL',
  },
  {
    chave: 'economia',
    rotulo: 'Economia',
    ajuda: 'Quanto sai para reserva de emergência e investimentos.',
    Icone: IconeEconomia,
    tipo: 'ECONOMIA',
  },
  {
    chave: 'dividas',
    rotulo: 'Dívidas',
    ajuda: 'Parcelas e financiamentos. Informe o total de parcelas e o app conta o resto.',
    Icone: IconeDivida,
    tipo: 'DIVIDA',
  },
  {
    chave: 'visao-geral',
    rotulo: 'Visão geral',
    ajuda: 'O fechamento: quanto sobrou e para onde foi o dinheiro.',
    Icone: IconePainel,
    tipo: null,
  },
];

export const indiceDe = (secao: Secao) => SECOES.findIndex((s) => s.chave === secao.chave);

function lerHash(): Secao {
  const chave = window.location.hash.replace(/^#\/?/, '');
  return SECOES.find((secao) => secao.chave === chave) ?? SECOES[0];
}

/**
 * Roteamento pelo hash da URL, sem dependência de router.
 *
 * Dá o que importa aqui: link direto para um passo, botão voltar do navegador
 * funcionando e recarregar a página sem perder o lugar.
 */
export function useSecao(): [Secao, (chave: string) => void] {
  const [secao, setSecao] = useState(lerHash);

  useEffect(() => {
    const aoMudar = () => setSecao(lerHash());
    window.addEventListener('hashchange', aoMudar);
    return () => window.removeEventListener('hashchange', aoMudar);
  }, []);

  return [secao, (chave: string) => { window.location.hash = `#/${chave}`; }];
}
