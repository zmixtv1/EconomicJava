import { useEffect, useId, useState } from 'react';
import type { FatiaDestino } from '../types';

interface Props {
  fatias: FatiaDestino[];
  /** Vai no buraco da rosca: o número que a pessoa veio ver. */
  disponivel: number;
}

const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

const RAIO = 62;
const ESPESSURA = 22;
const CIRCUNFERENCIA = 2 * Math.PI * RAIO;

/** Respiro de 2px entre fatias, na cor da superfície — separa sem precisar de borda. */
const RESPIRO = 2;

/**
 * A ordem das fatias é fixa e vem do servidor; ela nunca é reordenada por
 * valor. Isso mantém cada categoria com a mesma cor mês após mês (a cor segue
 * a entidade, não a posição no ranking) e preserva a validação de contraste
 * entre vizinhas, que foi feita justamente nesta ordem.
 */
const CLASSE_POR_CHAVE: Record<string, string> = {
  DESPESA_FIXA: 'serie-1',
  DESPESA_VARIAVEL: 'serie-2',
  DIVIDA: 'serie-3',
  ECONOMIA: 'serie-4',
  DISPONIVEL: 'serie-5',
};

export function GraficoDestino({ fatias, disponivel }: Props) {
  const [emFoco, setEmFoco] = useState<string | null>(null);
  const [desenhado, setDesenhado] = useState(false);
  const idTitulo = useId();

  // Os arcos entram com comprimento zero e crescem até o valor real: a rosca
  // se desenha em vez de aparecer pronta. Um quadro de atraso basta para o
  // navegador registrar o estado inicial e animar a diferença.
  useEffect(() => {
    setDesenhado(false);
    const quadro = requestAnimationFrame(() => setDesenhado(true));
    return () => cancelAnimationFrame(quadro);
  }, [fatias]);

  if (fatias.length === 0) {
    return (
      <div className="cartao grafico vazio-grafico">
        <h2>Para onde foi o dinheiro</h2>
        <p className="subtitulo">Lance uma receita e uma despesa para o gráfico aparecer.</p>
      </div>
    );
  }

  const total = fatias.reduce((soma, fatia) => soma + fatia.valor, 0);
  const destaque = fatias.find((fatia) => fatia.chave === emFoco) ?? null;

  // Cada fatia vira um arco desenhado com dash: comprimento proporcional ao
  // valor, menos o respiro, e deslocamento acumulado.
  let percorrido = 0;
  const arcos = fatias.map((fatia) => {
    const comprimento = (fatia.valor / total) * CIRCUNFERENCIA;
    const arco = {
      chave: fatia.chave,
      rotulo: fatia.rotulo,
      dash: Math.max(comprimento - RESPIRO, 0.6),
      deslocamento: -percorrido,
    };
    percorrido += comprimento;
    return arco;
  });

  const resumoTexto = fatias
    .map((fatia) => `${fatia.rotulo}: ${fatia.percentual}%`)
    .join(', ');

  return (
    <div className="cartao grafico">
      <h2 id={idTitulo}>Para onde foi o dinheiro</h2>

      <div className="grafico-corpo">
        <div className="rosca-area">
          <svg
            viewBox="0 0 160 160"
            className="rosca"
            role="img"
            aria-labelledby={idTitulo}
            aria-description={resumoTexto}
          >
            {/* -90° para a primeira fatia começar no topo. */}
            <g transform="rotate(-90 80 80)">
              {arcos.map((arco) => (
                <circle
                  key={arco.chave}
                  cx="80"
                  cy="80"
                  r={RAIO}
                  fill="none"
                  className={`fatia ${CLASSE_POR_CHAVE[arco.chave] ?? 'serie-1'} ${
                    emFoco && emFoco !== arco.chave ? 'apagada' : ''
                  }`}
                  strokeWidth={ESPESSURA}
                  strokeDasharray={
                    desenhado
                      ? `${arco.dash} ${CIRCUNFERENCIA - arco.dash}`
                      : `0 ${CIRCUNFERENCIA}`
                  }
                  strokeDashoffset={arco.deslocamento}
                  onMouseEnter={() => setEmFoco(arco.chave)}
                  onMouseLeave={() => setEmFoco(null)}
                />
              ))}
            </g>

            <text x="80" y="74" className="rosca-rotulo" textAnchor="middle">
              {destaque ? destaque.rotulo : 'Disponível'}
            </text>
            <text x="80" y="94" className="rosca-valor" textAnchor="middle">
              {MOEDA.format(destaque ? destaque.valor : disponivel)}
            </text>
          </svg>
        </div>

        {/* A legenda traz rótulo, valor e percentual: é também a leitura em
            texto do gráfico, para quem não distingue as cores. */}
        <ul className="legenda">
          {fatias.map((fatia) => (
            <li
              key={fatia.chave}
              className={emFoco && emFoco !== fatia.chave ? 'apagada' : ''}
              onMouseEnter={() => setEmFoco(fatia.chave)}
              onMouseLeave={() => setEmFoco(null)}
            >
              <span className={`marcador ${CLASSE_POR_CHAVE[fatia.chave] ?? 'serie-1'}`} aria-hidden="true" />
              <span className="legenda-rotulo">{fatia.rotulo}</span>
              <span className="legenda-percentual">{fatia.percentual}%</span>
              <span className="legenda-valor">{MOEDA.format(fatia.valor)}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
