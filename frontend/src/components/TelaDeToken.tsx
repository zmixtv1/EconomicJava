import { useEffect, useRef, useState, type FormEvent } from 'react';
import { api, ApiError } from '../api/client';
import type { Usuario } from '../types';

interface Props {
  acao: 'confirmar' | 'redefinir';
  /** Vem da query da URL do e-mail. Nulo quando alguém abre a rota na mão. */
  token: string | null;
  aoEntrar: (usuario: Usuario) => void;
  aoVoltar: () => void;
}

/**
 * As duas telas que chegam por link de e-mail.
 *
 * Confirmar age sozinha e já entra — a pessoa provou que é dona do endereço.
 * Redefinir precisa da senha nova antes de agir.
 */
export function TelaDeToken({ acao, token, aoEntrar, aoVoltar }: Props) {
  const confirmando = acao === 'confirmar';

  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(token ? null : 'Link incompleto. Abra a mensagem de e-mail de novo.');
  const [trabalhando, setTrabalhando] = useState(confirmando && token !== null);
  // O React 18+ roda o efeito duas vezes em desenvolvimento; sem isto o token
  // de uso único seria queimado na primeira e a segunda mostraria "expirado".
  const jaTentou = useRef(false);

  useEffect(() => {
    if (!confirmando || !token || jaTentou.current) {
      return;
    }
    jaTentou.current = true;

    api.confirmarEmail(token)
      .then(aoEntrar)
      .catch((problema) => {
        setErro(problema instanceof ApiError ? problema.message : 'Não foi possível confirmar.');
        setTrabalhando(false);
      });
  }, [confirmando, token, aoEntrar]);

  async function redefinir(evento: FormEvent) {
    evento.preventDefault();
    if (!token) {
      return;
    }
    setTrabalhando(true);
    setErro(null);

    try {
      aoEntrar(await api.redefinirSenha(token, senha));
    } catch (problema) {
      setErro(
        problema instanceof ApiError
          ? (problema.campos?.senha ?? problema.message)
          : 'Não foi possível redefinir a senha.',
      );
      setTrabalhando(false);
    }
  }

  return (
    <div className="tela-login">
      <div className="cartao login">
        <span className="marca grande" aria-hidden="true" />

        {confirmando ? (
          <>
            <h1>{erro ? 'Não deu certo' : 'Confirmando seu e-mail…'}</h1>
            <p className="subtitulo">
              {erro ?? 'Só um instante, estamos ativando sua conta.'}
            </p>
            {erro && (
              <button type="button" className="botao primario" onClick={aoVoltar}>
                Voltar para a entrada
              </button>
            )}
          </>
        ) : (
          <form onSubmit={redefinir}>
            <h1>Criar nova senha</h1>
            <p className="subtitulo">Escolha uma senha que você não usa em outro lugar.</p>

            <label htmlFor="nova-senha">Nova senha</label>
            <input
              id="nova-senha"
              type="password"
              autoComplete="new-password"
              minLength={8}
              value={senha}
              onChange={(evento) => setSenha(evento.target.value)}
              disabled={!token}
              required
            />
            <span className="dica">Mínimo de 8 caracteres.</span>

            {erro && <p className="alerta erro">{erro}</p>}

            <button type="submit" className="botao primario" disabled={trabalhando || !token}>
              {trabalhando ? 'Salvando…' : 'Salvar e entrar'}
            </button>

            <button type="button" className="link" onClick={aoVoltar}>
              Voltar para a entrada
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
