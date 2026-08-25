import { useCallback, useEffect, useState } from 'react';
import { api, ApiError, esquecerToken, temToken } from './api/client';
import { Autenticacao } from './components/Autenticacao';
import { IconeLua, IconeMenu, IconeSair, IconeSol } from './components/Icones';
import { PassoNavegacao } from './components/PassoNavegacao';
import { SecaoTipo } from './components/SecaoTipo';
import { TelaDeToken } from './components/TelaDeToken';
import { VisaoGeral } from './components/VisaoGeral';
import { useOrcamento } from './hooks/useOrcamento';
import { SECOES, indiceDe, useSecao } from './navegacao';
import { useTema } from './tema';
import type { TipoOpcao, Usuario } from './types';

/** yyyy-MM do mês corrente, no fuso local. */
function mesAtual(): string {
  const agora = new Date();
  return `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, '0')}`;
}

export default function App() {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [verificandoSessao, setVerificandoSessao] = useState(temToken);
  const [secao, irPara] = useSecao();
  const [mes, setMes] = useState(mesAtual);
  const [tipos, setTipos] = useState<TipoOpcao[]>([]);
  const [tema, alternarTema] = useTema();
  // No desktop o menu já nasce aberto; no celular ele é sobreposto e começa fechado.
  const [menuAberto, setMenuAberto] = useState(() => window.innerWidth > 900);

  const dados = useOrcamento(mes, secao.tipo, usuario !== null);

  const sair = useCallback(() => {
    esquecerToken();
    setUsuario(null);
    setTipos([]);
  }, []);

  useEffect(() => {
    if (!temToken()) {
      return;
    }
    api.eu()
      .then(setUsuario)
      .catch(() => esquecerToken())
      .finally(() => setVerificandoSessao(false));
  }, []);

  useEffect(() => {
    if (usuario === null) {
      return;
    }
    api.listarTipos()
      .then(setTipos)
      .catch((problema) => {
        if (problema instanceof ApiError && problema.status === 401) {
          sair();
        }
      });
  }, [usuario, sair]);

  // Rotas que chegam por link de e-mail e valem antes de existir sessão.
  const rotaDoEmail = window.location.hash.replace(/^#\/?/, '');
  if (rotaDoEmail === 'confirmar' || rotaDoEmail === 'redefinir') {
    return (
      <TelaDeToken
        acao={rotaDoEmail}
        token={new URLSearchParams(window.location.search).get('token')}
        aoEntrar={(entrando) => {
          // Tira o token da barra de endereços: ele já foi usado e não deve
          // ficar no histórico do navegador.
          window.history.replaceState(null, '', `${window.location.pathname}#/receitas`);
          setUsuario(entrando);
        }}
        aoVoltar={() => {
          window.history.replaceState(null, '', window.location.pathname);
          window.location.reload();
        }}
      />
    );
  }

  if (verificandoSessao) {
    return (
      <div className="tela-login">
        <div className="cabecalho-identidade">
          <span className="marca" aria-hidden="true" />
          <p className="subtitulo">Carregando…</p>
        </div>
      </div>
    );
  }

  if (usuario === null) {
    return <Autenticacao aoEntrar={setUsuario} />;
  }

  function navegar(chave: string) {
    irPara(chave);
    if (window.innerWidth <= 900) {
      setMenuAberto(false);
    }
  }

  return (
    <div className={`janela ${menuAberto ? 'menu-aberto' : ''}`}>
      <aside className="lateral">
        <div className="lateral-topo">
          <span className="marca" aria-hidden="true" />
          <span className="lateral-nome">Orçamento</span>
        </div>

        <nav className="menu" aria-label="Passos">
          {SECOES.map(({ chave, rotulo, Icone }, posicao) => (
            <button
              key={chave}
              type="button"
              className={`menu-item ${secao.chave === chave ? 'ativo' : ''} ${
                posicao < indiceDe(secao) ? 'concluido' : ''
              }`}
              aria-current={secao.chave === chave ? 'page' : undefined}
              onClick={() => navegar(chave)}
              title={rotulo}
            >
              <Icone />
              <span className="menu-rotulo">{rotulo}</span>
              <span className="menu-numero" aria-hidden="true">{posicao + 1}</span>
            </button>
          ))}
        </nav>

        <div className="lateral-rodape">
          <div className="lateral-usuario">
            <span className="menu-rotulo">{usuario.nome}</span>
          </div>
          <button type="button" className="menu-item" onClick={sair} title="Sair">
            <IconeSair />
            <span className="menu-rotulo">Sair</span>
          </button>
        </div>
      </aside>

      {/* Cobre o conteúdo no celular: clicar fora fecha o menu. */}
      <button
        type="button"
        className="cortina"
        aria-label="Fechar menu"
        onClick={() => setMenuAberto(false)}
      />

      <div className="conteudo">
        <header className="barra">
          <button
            type="button"
            className="botao icone"
            aria-label={menuAberto ? 'Recolher menu' : 'Abrir menu'}
            aria-expanded={menuAberto}
            onClick={() => setMenuAberto((aberto) => !aberto)}
          >
            <IconeMenu />
          </button>

          <h1>{secao.rotulo}</h1>

          <button
            type="button"
            className="botao icone"
            aria-label={tema === 'claro' ? 'Mudar para o tema escuro' : 'Mudar para o tema claro'}
            onClick={alternarTema}
          >
            {tema === 'claro' ? <IconeLua /> : <IconeSol />}
          </button>

          <label className="seletor-mes">
            <span className="apenas-leitores">Mês</span>
            <input type="month" value={mes} onChange={(evento) => setMes(evento.target.value)} />
          </label>
        </header>

        {/* A key faz o React remontar o bloco a cada passo, e é isso que
            dispara a animação de entrada de novo. */}
        <main className="pagina" key={secao.chave}>
          <p className="ajuda">{secao.ajuda}</p>

          {dados.erro && (
            <p className="alerta erro">
              {dados.erro}{' '}
              <button type="button" className="botao pequeno" onClick={() => void dados.recarregar()}>
                Tentar de novo
              </button>
            </p>
          )}

          {secao.tipo === null ? (
            <VisaoGeral resumo={dados.resumo} carregando={dados.carregando} />
          ) : (
            <SecaoTipo
              tipo={secao.tipo}
              rotulo={secao.rotulo}
              mes={mes}
              tipos={tipos}
              resumo={dados.resumo}
              lancamentos={dados.lancamentos}
              carregando={dados.carregando}
              criar={dados.criar}
              atualizar={dados.atualizar}
              remover={dados.remover}
            />
          )}

          <PassoNavegacao secao={secao} aoNavegar={navegar} />
        </main>
      </div>
    </div>
  );
}
