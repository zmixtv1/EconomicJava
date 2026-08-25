import { IconeSetaDireita, IconeSetaEsquerda } from './Icones';
import { SECOES, indiceDe, type Secao } from '../navegacao';

interface Props {
  secao: Secao;
  aoNavegar: (chave: string) => void;
}

/**
 * O rodapé de cada passo. Além dos botões, mostra em que ponto do roteiro a
 * pessoa está — sem isso um fluxo de seis telas vira um labirinto.
 */
export function PassoNavegacao({ secao, aoNavegar }: Props) {
  const indice = indiceDe(secao);
  const total = SECOES.length;
  const anterior = indice > 0 ? SECOES[indice - 1] : null;
  const proxima = indice < total - 1 ? SECOES[indice + 1] : null;
  const ultimo = proxima === null;

  return (
    <nav className="passos" aria-label="Navegação entre os passos">
      <button
        type="button"
        className="botao"
        onClick={() => anterior && aoNavegar(anterior.chave)}
        disabled={anterior === null}
      >
        <IconeSetaEsquerda />
        <span>{anterior ? anterior.rotulo : 'Voltar'}</span>
      </button>

      <div className="passos-progresso">
        <span className="passos-contador">
          Passo {indice + 1} de {total}
        </span>
        <div
          className="passos-trilho"
          role="progressbar"
          aria-valuenow={indice + 1}
          aria-valuemin={1}
          aria-valuemax={total}
        >
          <div className="passos-preenchido" style={{ width: `${((indice + 1) / total) * 100}%` }} />
        </div>
      </div>

      {ultimo ? (
        <button type="button" className="botao" onClick={() => aoNavegar(SECOES[0].chave)}>
          <span>Recomeçar</span>
          <IconeSetaDireita />
        </button>
      ) : (
        <button type="button" className="botao primario" onClick={() => aoNavegar(proxima.chave)}>
          <span>{indice === total - 2 ? 'Ver o resumo' : 'Avançar'}</span>
          <IconeSetaDireita />
        </button>
      )}
    </nav>
  );
}
