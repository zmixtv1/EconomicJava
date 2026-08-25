import { useEffect, useRef } from 'react';

interface Props {
  clientId: string;
  /** Recebe o ID token que o Google devolve; quem valida é o servidor. */
  aoReceberToken: (credential: string) => void;
}

/**
 * Botão oficial do Google Identity Services.
 *
 * O navegador recebe um ID token assinado pelo Google e o repassa para a nossa
 * API, que confere assinatura, emissor e destinatário. Nada é decidido aqui:
 * este componente só transporta o token.
 */
interface JanelaComGoogle extends Window {
  google?: {
    accounts: {
      id: {
        initialize: (config: {
          client_id: string;
          callback: (resposta: { credential: string }) => void;
        }) => void;
        renderButton: (elemento: HTMLElement, opcoes: Record<string, string | number>) => void;
      };
    };
  };
}

const FONTE = 'https://accounts.google.com/gsi/client';

/**
 * Promessa única para o módulo inteiro.
 *
 * Sem isto, a segunda instância do botão (a tela tem uma por painel) encontrava
 * a tag <script> já no DOM e resolvia na hora — antes de o script ter
 * carregado. Aí `window.google` ainda não existia e ela desistia em silêncio.
 * Com o StrictMode montando o efeito duas vezes, nenhuma das duas renderizava.
 */
let carregamento: Promise<void> | null = null;

function carregarScript(): Promise<void> {
  if ((window as JanelaComGoogle).google?.accounts?.id) {
    return Promise.resolve();
  }
  if (carregamento) {
    return carregamento;
  }

  carregamento = new Promise((resolver, rejeitar) => {
    const jaExiste = document.querySelector<HTMLScriptElement>(`script[src="${FONTE}"]`);
    const script = jaExiste ?? document.createElement('script');

    script.addEventListener('load', () => resolver());
    script.addEventListener('error', () => {
      carregamento = null; // permite tentar de novo numa próxima montagem
      rejeitar(new Error('Não foi possível carregar o Google'));
    });

    if (!jaExiste) {
      script.src = FONTE;
      script.async = true;
      script.defer = true;
      document.head.appendChild(script);
    }
  });

  return carregamento;
}

/**
 * O SDK do Google é global: `initialize` configura o cliente uma vez para a
 * página inteira, e cada botão só precisa de `renderButton`. Chamando nas duas
 * instâncias, o próprio Google avisa que só a última vale — então inicializamos
 * uma vez e guardamos aqui quem recebe o token.
 */
let inicializadoPara: string | null = null;
let ouvinte: ((credential: string) => void) | null = null;

function garantirInicializado(janela: JanelaComGoogle, clientId: string): void {
  if (inicializadoPara === clientId || !janela.google) {
    return;
  }
  janela.google.accounts.id.initialize({
    client_id: clientId,
    callback: (resposta) => ouvinte?.(resposta.credential),
  });
  inicializadoPara = clientId;
}

export function BotaoGoogle({ clientId, aoReceberToken }: Props) {
  const destino = useRef<HTMLDivElement>(null);
  // Os dois botões da tela levam ao mesmo lugar; o último a montar responde.
  ouvinte = aoReceberToken;

  useEffect(() => {
    let cancelado = false;

    void carregarScript()
      .then(() => {
        const janela = window as JanelaComGoogle;
        if (cancelado || !janela.google || !destino.current) {
          return;
        }

        garantirInicializado(janela, clientId);

        janela.google.accounts.id.renderButton(destino.current, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          text: 'continue_with',
          shape: 'pill',
          locale: 'pt-BR',
          width: 320,
        });
      })
      .catch(() => {
        // Sem internet ou script bloqueado: a entrada por e-mail continua ali.
      });

    return () => {
      cancelado = true;
    };
  }, [clientId]);

  return <div className="botao-google" ref={destino} />;
}
