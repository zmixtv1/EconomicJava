/** Espelha o contrato da API. Se o back mudar, o TypeScript quebra aqui primeiro. */

export interface Usuario {
  id: number;
  nome: string;
  email: string;
}

export interface Sessao {
  token: string;
  expiraEm: string;
  usuario: Usuario;
}

export interface RegistroInput {
  nome: string;
  email: string;
  senha: string;
}

export interface LoginInput {
  email: string;
  senha: string;
}

/** O que esta instalação oferece na tela de entrada. */
export interface OpcoesDeEntrada {
  google: boolean;
  googleClientId: string | null;
}

export type TipoLancamento =
  | 'RECEITA'
  | 'DESPESA_FIXA'
  | 'DESPESA_VARIAVEL'
  | 'ECONOMIA'
  | 'DIVIDA';

export interface TipoOpcao {
  nome: TipoLancamento;
  rotulo: string;
  comprometeReceita: boolean;
  /** Tipos mensais por natureza: o formulário já abre no modo recorrente. */
  mensal: boolean;
}

export interface CategoriaOpcao {
  nome: string;
  rotulo: string;
  tipo: TipoLancamento;
}

export interface Lancamento {
  id: number;
  tipo: TipoLancamento;
  tipoRotulo: string;
  categoria: string;
  categoriaRotulo: string;
  descricao: string;
  valor: number;
  recorrente: boolean;
  /** ISO yyyy-MM-dd. Só nos pontuais. */
  data: string | null;
  vigenciaInicio: string | null;
  vigenciaFim: string | null;
  parcelas: number | null;
  /** "parcela 7 de 48" — calculada para o mês consultado. */
  parcelaAtual: number | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface LancamentoInput {
  tipo: TipoLancamento;
  categoria: string;
  descricao: string;
  valor: number;
  data?: string | null;
  vigenciaInicio?: string | null;
  vigenciaFim?: string | null;
  parcelas?: number | null;
}

export interface FatiaDestino {
  chave: string;
  rotulo: string;
  valor: number;
  percentual: number;
}

export interface TotalPorCategoria {
  categoria: string;
  categoriaRotulo: string;
  tipo: TipoLancamento;
  total: number;
}

export interface ResumoMensal {
  mes: string;
  inicio: string;
  fim: string;
  receitas: number;
  despesasFixas: number;
  despesasVariaveis: number;
  economia: number;
  dividas: number;
  comprometido: number;
  disponivel: number;
  destino: FatiaDestino[];
  porCategoria: TotalPorCategoria[];
}
