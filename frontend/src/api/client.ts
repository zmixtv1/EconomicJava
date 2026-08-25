import type {
  CategoriaOpcao,
  Lancamento,
  LancamentoInput,
  LoginInput,
  OpcoesDeEntrada,
  RegistroInput,
  ResumoMensal,
  Sessao,
  TipoLancamento,
  TipoOpcao,
  Usuario,
} from '../types';

/**
 * Em dev fica vazio e o proxy do Vite manda para localhost:8080.
 * Em produção, VITE_API_URL aponta para o domínio HTTPS da VPS.
 */
const BASE_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');

const CHAVE_TOKEN = 'despesas.token';

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly campos?: Record<string, string>,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

// sessionStorage e não localStorage: fechou a aba, a sessão acaba.
let token: string | null = sessionStorage.getItem(CHAVE_TOKEN);

function guardarToken(novo: string): void {
  token = novo;
  sessionStorage.setItem(CHAVE_TOKEN, novo);
}

export function esquecerToken(): void {
  token = null;
  sessionStorage.removeItem(CHAVE_TOKEN);
}

export function temToken(): boolean {
  return token !== null;
}

async function requisicao<T>(caminho: string, init: RequestInit = {}): Promise<T> {
  const cabecalhos = new Headers(init.headers);
  cabecalhos.set('Accept', 'application/json');
  if (init.body !== undefined) {
    cabecalhos.set('Content-Type', 'application/json');
  }
  if (token) {
    cabecalhos.set('Authorization', `Bearer ${token}`);
  }

  let resposta: Response;
  try {
    resposta = await fetch(`${BASE_URL}${caminho}`, { ...init, headers: cabecalhos });
  } catch {
    throw new ApiError(0, 'Não foi possível falar com o servidor. Ele está no ar?');
  }

  if (resposta.status === 204) {
    return undefined as T;
  }

  const corpo = await resposta.text();
  const dados: unknown = corpo ? JSON.parse(corpo) : null;

  if (!resposta.ok) {
    const problema = dados as {
      detail?: string;
      title?: string;
      erros?: Record<string, string>;
    } | null;

    throw new ApiError(
      resposta.status,
      problema?.detail ?? problema?.title ?? `Erro ${resposta.status}`,
      problema?.erros,
    );
  }

  return dados as T;
}

/** Todo caminho que devolve sessão passa por aqui e guarda o token. */
async function abrirSessao(caminho: string, dados: object): Promise<Usuario> {
  const sessao = await requisicao<Sessao>(caminho, { method: 'POST', body: JSON.stringify(dados) });
  guardarToken(sessao.token);
  return sessao.usuario;
}

export const api = {
  /** O que a tela de entrada deve oferecer (botão do Google configurado ou não). */
  opcoesDeEntrada: () => requisicao<OpcoesDeEntrada>('/api/v1/auth/opcoes'),

  /**
   * Não devolve sessão: o cadastro responde 202 exista ou não a conta, e a
   * pessoa só entra depois de clicar no link que chega por e-mail.
   */
  criarConta: (dados: RegistroInput) =>
    requisicao<void>('/api/v1/auth/registro', { method: 'POST', body: JSON.stringify(dados) }),

  reenviarConfirmacao: (email: string) =>
    requisicao<void>('/api/v1/auth/reenviar-confirmacao', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  confirmarEmail: (token: string) => abrirSessao('/api/v1/auth/confirmar', { token }),

  pedirRecuperacao: (email: string) =>
    requisicao<void>('/api/v1/auth/recuperar', { method: 'POST', body: JSON.stringify({ email }) }),

  redefinirSenha: (token: string, senha: string) =>
    abrirSessao('/api/v1/auth/redefinir', { token, senha }),

  entrarComGoogle: (credential: string) => abrirSessao('/api/v1/auth/google', { credential }),

  entrar: (dados: LoginInput) => abrirSessao('/api/v1/auth/login', dados),

  /** Confere se o token guardado ainda vale e devolve quem é o dono dele. */
  eu: () => requisicao<Usuario>('/api/v1/auth/eu'),

  listarTipos: () => requisicao<TipoOpcao[]>('/api/v1/tipos'),

  listarCategorias: (tipo?: TipoLancamento) =>
    requisicao<CategoriaOpcao[]>(`/api/v1/categorias${tipo ? `?tipo=${tipo}` : ''}`),

  listarLancamentos: (mes: string, tipo?: TipoLancamento) =>
    requisicao<Lancamento[]>(`/api/v1/lancamentos?mes=${mes}${tipo ? `&tipo=${tipo}` : ''}`),

  obterResumo: (mes: string) => requisicao<ResumoMensal>(`/api/v1/lancamentos/resumo?mes=${mes}`),

  criarLancamento: (lancamento: LancamentoInput) =>
    requisicao<Lancamento>('/api/v1/lancamentos', {
      method: 'POST',
      body: JSON.stringify(lancamento),
    }),

  atualizarLancamento: (id: number, lancamento: LancamentoInput) =>
    requisicao<Lancamento>(`/api/v1/lancamentos/${id}`, {
      method: 'PUT',
      body: JSON.stringify(lancamento),
    }),

  removerLancamento: (id: number) =>
    requisicao<void>(`/api/v1/lancamentos/${id}`, { method: 'DELETE' }),
};
