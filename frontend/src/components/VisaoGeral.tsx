import { GraficoDestino } from './GraficoDestino';
import { useContagem } from '../hooks/useContagem';
import type { ResumoMensal } from '../types';

interface Props {
  resumo: ResumoMensal | null;
  carregando: boolean;
}

const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function VisaoGeral({ resumo, carregando }: Props) {
  const disponivelAnimado = useContagem(resumo?.disponivel ?? 0);

  if (!resumo) {
    return <div className="cartao painel esqueleto" aria-busy={carregando} />;
  }

  const sobrando = resumo.disponivel >= 0;

  return (
    <>
      {/* O número que a pessoa abriu o app para ver fica sozinho, grande e
          primeiro. Os outros são contexto dele. */}
      <section className="cartao destaque">
        <span className="rotulo">Disponível para gastar</span>
        <strong className={`heroi ${sobrando ? '' : 'negativo'}`}>
          {MOEDA.format(disponivelAnimado)}
        </strong>
        <span className="subtitulo">
          {sobrando
            ? `De ${MOEDA.format(resumo.receitas)} que entraram, ${MOEDA.format(resumo.comprometido)} já têm destino.`
            : `As saídas passam as entradas em ${MOEDA.format(Math.abs(resumo.disponivel))} neste mês.`}
        </span>
      </section>

      <section className="tiras">
        <Tira rotulo="Receitas" valor={resumo.receitas} />
        <Tira rotulo="Despesas fixas" valor={resumo.despesasFixas} />
        <Tira rotulo="Despesas variáveis" valor={resumo.despesasVariaveis} />
        <Tira rotulo="Economia" valor={resumo.economia} />
        <Tira rotulo="Dívidas" valor={resumo.dividas} />
      </section>

      <GraficoDestino fatias={resumo.destino} disponivel={resumo.disponivel} />

      {resumo.porCategoria.length > 0 && (
        <section className="cartao">
          <h2>Saídas por categoria</h2>
          <ul className="barras">
            {resumo.porCategoria.map((linha) => (
              <li key={linha.categoria}>
                <div className="barra-topo">
                  <span>{linha.categoriaRotulo}</span>
                  <span className="barra-valor">{MOEDA.format(linha.total)}</span>
                </div>
                <div className="barra-trilho">
                  <div
                    className="barra-preenchida"
                    style={{
                      width: `${(linha.total / resumo.porCategoria[0].total) * 100}%`,
                    }}
                  />
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  );
}

function Tira({ rotulo, valor }: { rotulo: string; valor: number }) {
  return (
    <div className="cartao tira">
      <span className="rotulo">{rotulo}</span>
      <strong className="tira-valor">{MOEDA.format(valor)}</strong>
    </div>
  );
}
