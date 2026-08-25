import { useEffect, useState, type FormEvent } from 'react';
import { api, ApiError, esquecerToken } from '../api/client';
import { BotaoGoogle } from './BotaoGoogle';
import type { OpcoesDeEntrada, Usuario } from '../types';

interface Props {
  aoEntrar: (usuario: Usuario) => void;
}

type Modo = 'entrar' | 'criar' | 'esqueci';

/** Telas que substituem o cartão inteiro depois de uma ação por e-mail. */
type Aviso = { titulo: string; texto: string; oferecerReenvio: boolean } | null;

export function Autenticacao({ aoEntrar }: Props) {
  const [modo, setModo] = useState<Modo>('entrar');
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [errosCampo, setErrosCampo] = useState<Record<string, string>>({});
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [aviso, setAviso] = useState<Aviso>(null);
  const [opcoes, setOpcoes] = useState<OpcoesDeEntrada | null>(null);

  const criando = modo === 'criar';
  const recuperando = modo === 'esqueci';

  useEffect(() => {
    api.opcoesDeEntrada().then(setOpcoes).catch(() => setOpcoes({ google: false, googleClientId: null }));
  }, []);

  function trocarModo(novo: Modo) {
    setModo(novo);
    setErro(null);
    setErrosCampo({});
    setAviso(null);
  }

  function tratarFalha(problema: unknown, padrao: string) {
    esquecerToken();

    if (problema instanceof ApiError && problema.campos) {
      setErrosCampo(problema.campos);
      return;
    }
    if (problema instanceof ApiError) {
      // 403 é o código de e-mail não confirmado: a senha está certa.
      if (problema.status === 403) {
        setAviso({ titulo: 'Confirme seu e-mail', texto: problema.message, oferecerReenvio: true });
        return;
      }
      setErro(problema.message);
      return;
    }
    setErro(padrao);
  }

  async function entrar(evento: FormEvent) {
    evento.preventDefault();
    setEnviando(true);
    setErro(null);
    setErrosCampo({});

    try {
      aoEntrar(await api.entrar({ email: email.trim(), senha }));
    } catch (problema) {
      tratarFalha(problema, 'Não foi possível entrar.');
    } finally {
      setEnviando(false);
    }
  }

  async function criarConta(evento: FormEvent) {
    evento.preventDefault();
    setEnviando(true);
    setErro(null);
    setErrosCampo({});

    try {
      await api.criarConta({ nome: nome.trim(), email: email.trim(), senha });
      setAviso({
        titulo: 'Falta confirmar seu e-mail',
        texto: `Enviamos um link para ${email.trim()}. Abra a mensagem para ativar sua conta — o link vale por 24 horas.`,
        oferecerReenvio: true,
      });
    } catch (problema) {
      tratarFalha(problema, 'Não foi possível criar a conta.');
    } finally {
      setEnviando(false);
    }
  }

  async function pedirRecuperacao(evento: FormEvent) {
    evento.preventDefault();
    setEnviando(true);
    setErro(null);

    try {
      await api.pedirRecuperacao(email.trim());
      setAviso({
        titulo: 'Verifique seu e-mail',
        texto: `Se existe uma conta para ${email.trim()}, enviamos um link para criar uma nova senha. Ele vale por 1 hora.`,
        oferecerReenvio: false,
      });
    } catch (problema) {
      tratarFalha(problema, 'Não foi possível enviar o link.');
    } finally {
      setEnviando(false);
    }
  }

  async function entrarComGoogle(credential: string) {
    setErro(null);
    try {
      aoEntrar(await api.entrarComGoogle(credential));
    } catch (problema) {
      tratarFalha(problema, 'Não foi possível entrar com o Google.');
    }
  }

  async function reenviar() {
    try {
      await api.reenviarConfirmacao(email.trim());
      setAviso((atual) => atual && {
        ...atual,
        texto: 'Pronto, enviamos o link de novo. Confira sua caixa de entrada e o spam.',
      });
    } catch {
      setErro('Não foi possível reenviar agora. Tente em um minuto.');
    }
  }

  const google = opcoes?.google && opcoes.googleClientId
    ? (
      <>
        <BotaoGoogle
          clientId={opcoes.googleClientId}
          aoReceberToken={(credential) => void entrarComGoogle(credential)}
        />
        <div className="separador"><span>ou com e-mail</span></div>
      </>
    )
    : null;

  // ---------- telas que tomam o cartão inteiro ----------

  if (aviso) {
    return (
      <div className="tela-login">
        <div className="cartao login">
          <span className="marca grande" aria-hidden="true" />
          <h1>{aviso.titulo}</h1>
          <p className="subtitulo">{aviso.texto}</p>

          {aviso.oferecerReenvio && (
            <button type="button" className="botao" onClick={() => void reenviar()}>
              Reenviar o link
            </button>
          )}

          {erro && <p className="alerta erro">{erro}</p>}

          <button type="button" className="botao primario" onClick={() => trocarModo('entrar')}>
            Voltar para a entrada
          </button>
        </div>
      </div>
    );
  }

  if (recuperando) {
    return (
      <div className="tela-login">
        <form className="cartao login" onSubmit={pedirRecuperacao}>
          <span className="marca grande" aria-hidden="true" />
          <h1>Esqueceu a senha?</h1>
          <p className="subtitulo">
            Informe seu e-mail e mandamos um link para você criar uma nova.
          </p>

          <label htmlFor="email-recuperacao">E-mail</label>
          <input
            id="email-recuperacao"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(evento) => setEmail(evento.target.value)}
            required
          />

          {erro && <p className="alerta erro">{erro}</p>}

          <button type="submit" className="botao primario" disabled={enviando}>
            {enviando ? 'Enviando…' : 'Enviar link'}
          </button>
          <button type="button" className="link" onClick={() => trocarModo('entrar')}>
            Voltar para a entrada
          </button>
        </form>
      </div>
    );
  }

  // ---------- o cartão de painel deslizante ----------

  return (
    <div className="tela-entrada">
      <div className={`caixa ${criando ? 'criando' : ''}`}>

        <section className="lado lado-entrar" aria-hidden={criando}>
          <form onSubmit={entrar}>
            <h2>Entrar</h2>
            <p className="subtitulo">Use sua conta para ver o mês</p>

            {google}

            <label htmlFor="email">E-mail</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(evento) => setEmail(evento.target.value)}
              required={!criando}
            />
            {errosCampo.email && <span className="erro-campo">{errosCampo.email}</span>}

            <label htmlFor="senha">Senha</label>
            <input
              id="senha"
              type="password"
              autoComplete="current-password"
              value={senha}
              onChange={(evento) => setSenha(evento.target.value)}
              required={!criando}
            />

            {erro && <p className="alerta erro">{erro}</p>}

            <button type="submit" className="botao primario" disabled={enviando}>
              {enviando ? 'Entrando…' : 'Entrar'}
            </button>
            <button type="button" className="link" onClick={() => trocarModo('esqueci')}>
              Esqueci minha senha
            </button>
          </form>
        </section>

        <section className="lado lado-criar" aria-hidden={!criando}>
          <form onSubmit={criarConta}>
            <h2>Criar conta</h2>
            <p className="subtitulo">Leva menos de um minuto</p>

            {google}

            <label htmlFor="nome-novo">Nome</label>
            <input
              id="nome-novo"
              autoComplete="name"
              maxLength={120}
              value={nome}
              onChange={(evento) => setNome(evento.target.value)}
              required={criando}
            />
            {errosCampo.nome && <span className="erro-campo">{errosCampo.nome}</span>}

            <label htmlFor="email-novo">E-mail</label>
            <input
              id="email-novo"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(evento) => setEmail(evento.target.value)}
              required={criando}
            />
            {errosCampo.email && <span className="erro-campo">{errosCampo.email}</span>}

            <label htmlFor="senha-nova">Senha</label>
            <input
              id="senha-nova"
              type="password"
              autoComplete="new-password"
              minLength={8}
              value={senha}
              onChange={(evento) => setSenha(evento.target.value)}
              required={criando}
            />
            {errosCampo.senha
              ? <span className="erro-campo">{errosCampo.senha}</span>
              : <span className="dica">Mínimo de 8 caracteres.</span>}

            {erro && <p className="alerta erro">{erro}</p>}

            <button type="submit" className="botao primario" disabled={enviando}>
              {enviando ? 'Enviando…' : 'Criar conta'}
            </button>
          </form>
        </section>

        <aside className="troca">
          <span className="troca-circulo" aria-hidden="true" />
          <span className="troca-circulo pequeno" aria-hidden="true" />
          <span className="marca" aria-hidden="true" />

          <div className="troca-conteudo">
            <h2>{criando ? 'Que bom te ver!' : 'Primeira vez aqui?'}</h2>
            <p>
              {criando
                ? 'Já tem conta? Entre e continue de onde parou.'
                : 'Crie sua conta e monte o orçamento do mês em seis passos.'}
            </p>
            <button
              type="button"
              className="botao"
              onClick={() => trocarModo(criando ? 'entrar' : 'criar')}
            >
              {criando ? 'Entrar' : 'Criar conta'}
            </button>
          </div>
        </aside>

      </div>
    </div>
  );
}
