import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import type { CategoriaOpcao, Lancamento, LancamentoInput, TipoLancamento, TipoOpcao } from '../types';

interface Props {
  tipo: TipoLancamento;
  tipos: TipoOpcao[];
  categorias: CategoriaOpcao[];
  emEdicao: Lancamento | null;
  aoSalvar: (lancamento: LancamentoInput) => Promise<void>;
  aoCancelar: () => void;
  /** Mês em que a pessoa está: o padrão do formulário acompanha a navegação. */
  mes: string;
}

/**
 * Aceita as três formas que a pessoa digita: "1.234,56", "1234,56" e "1234.56".
 * Se tem vírgula, ela é o separador decimal e os pontos são de milhar.
 */
function paraNumero(texto: string): number {
  const limpo = texto.trim();
  return Number(limpo.includes(',') ? limpo.replace(/\./g, '').replace(',', '.') : limpo);
}

function estadoInicial(lancamento: Lancamento | null, mes: string, padraoRecorrente: boolean) {
  return {
    categoria: lancamento?.categoria ?? '',
    descricao: lancamento?.descricao ?? '',
    valor: lancamento ? String(lancamento.valor) : '',
    recorrente: lancamento ? lancamento.recorrente : padraoRecorrente,
    data: lancamento?.data ?? `${mes}-01`,
    vigenciaInicio: (lancamento?.vigenciaInicio ?? `${mes}-01`).slice(0, 7),
    vigenciaFim: lancamento?.vigenciaFim ? lancamento.vigenciaFim.slice(0, 7) : '',
    parcelas: lancamento?.parcelas ? String(lancamento.parcelas) : '',
  };
}

export function FormularioLancamento({
  tipo, tipos, categorias, emEdicao, aoSalvar, aoCancelar, mes,
}: Props) {
  const definicao = useMemo(() => tipos.find((t) => t.nome === tipo), [tipos, tipo]);

  // Despesa fixa é mensal por definição; despesa variável é sempre pontual.
  // Nos demais tipos a pessoa escolhe.
  const mensalPorNatureza = definicao?.mensal ?? false;
  const sempreP = tipo === 'DESPESA_VARIAVEL';
  const podeEscolher = !mensalPorNatureza && !sempreP;
  const aceitaParcelas = tipo === 'DIVIDA';

  // Salário, aporte e parcela quase sempre se repetem — o formulário já abre
  // marcado. Só a despesa variável começa como lançamento avulso.
  const padraoRecorrente = !sempreP;

  const [campos, setCampos] = useState(() => estadoInicial(emEdicao, mes, padraoRecorrente));
  const [errosCampo, setErrosCampo] = useState<Record<string, string>>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    setCampos(estadoInicial(emEdicao, mes, padraoRecorrente));
    setErrosCampo({});
    setErroGeral(null);
  }, [emEdicao, tipo, mes, padraoRecorrente]);

  const recorrente = mensalPorNatureza || (podeEscolher && campos.recorrente);

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    setSalvando(true);
    setErrosCampo({});
    setErroGeral(null);

    const valorNumerico = paraNumero(campos.valor);
    if (!Number.isFinite(valorNumerico) || valorNumerico <= 0) {
      setErrosCampo({ valor: 'Informe um valor maior que zero' });
      setSalvando(false);
      return;
    }

    const lancamento: LancamentoInput = {
      tipo,
      categoria: campos.categoria || (categorias[0]?.nome ?? ''),
      descricao: campos.descricao.trim(),
      valor: valorNumerico,
      // O back exige data OU vigência, nunca os dois.
      data: recorrente ? null : campos.data,
      vigenciaInicio: recorrente ? `${campos.vigenciaInicio}-01` : null,
      vigenciaFim: recorrente && campos.vigenciaFim && !campos.parcelas ? `${campos.vigenciaFim}-01` : null,
      parcelas: recorrente && aceitaParcelas && campos.parcelas ? Number(campos.parcelas) : null,
    };

    try {
      await aoSalvar(lancamento);
      setCampos(estadoInicial(null, mes, padraoRecorrente));
    } catch (problema) {
      if (problema instanceof ApiError && problema.campos) {
        setErrosCampo(problema.campos);
      } else {
        setErroGeral(problema instanceof ApiError ? problema.message : 'Não foi possível salvar.');
      }
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form className="cartao formulario" onSubmit={enviar}>
      <h2>{emEdicao ? `Editando #${emEdicao.id}` : 'Novo lançamento'}</h2>

      <div className="linha">
        <div className="campo">
          <label htmlFor="descricao">Descrição</label>
          <input
            id="descricao"
            maxLength={120}
            placeholder={tipo === 'DIVIDA' ? 'Parcela do carro' : 'Aluguel, mercado, salário…'}
            value={campos.descricao}
            onChange={(e) => setCampos({ ...campos, descricao: e.target.value })}
            required
          />
          {errosCampo.descricao && <span className="erro-campo">{errosCampo.descricao}</span>}
        </div>

        <div className="campo estreito">
          <label htmlFor="valor">Valor (R$)</label>
          <input
            id="valor"
            inputMode="decimal"
            placeholder="1.850,00"
            value={campos.valor}
            onChange={(e) => setCampos({ ...campos, valor: e.target.value })}
            required
          />
          {errosCampo.valor && <span className="erro-campo">{errosCampo.valor}</span>}
        </div>

        <div className="campo estreito">
          <label htmlFor="categoria">Categoria</label>
          <select
            id="categoria"
            value={campos.categoria || categorias[0]?.nome || ''}
            onChange={(e) => setCampos({ ...campos, categoria: e.target.value })}
          >
            {categorias.map((opcao) => (
              <option key={opcao.nome} value={opcao.nome}>
                {opcao.rotulo}
              </option>
            ))}
          </select>
        </div>
      </div>

      {podeEscolher && (
        <label className="alternador">
          <input
            type="checkbox"
            checked={campos.recorrente}
            onChange={(e) => setCampos({ ...campos, recorrente: e.target.checked })}
          />
          <span>Repete todo mês</span>
        </label>
      )}

      <div className="linha">
        {recorrente ? (
          <>
            <div className="campo estreito">
              <label htmlFor="vigencia-inicio">A partir de</label>
              <input
                id="vigencia-inicio"
                type="month"
                value={campos.vigenciaInicio}
                onChange={(e) => setCampos({ ...campos, vigenciaInicio: e.target.value })}
                required
              />
            </div>

            {aceitaParcelas ? (
              <div className="campo estreito">
                <label htmlFor="parcelas">Parcelas</label>
                <input
                  id="parcelas"
                  type="number"
                  min={1}
                  max={600}
                  placeholder="48"
                  value={campos.parcelas}
                  onChange={(e) => setCampos({ ...campos, parcelas: e.target.value })}
                />
                <span className="dica">O mês da quitação é calculado sozinho.</span>
              </div>
            ) : (
              <div className="campo estreito">
                <label htmlFor="vigencia-fim">Até (opcional)</label>
                <input
                  id="vigencia-fim"
                  type="month"
                  value={campos.vigenciaFim}
                  onChange={(e) => setCampos({ ...campos, vigenciaFim: e.target.value })}
                />
                <span className="dica">Em branco = sem previsão de acabar.</span>
              </div>
            )}
          </>
        ) : (
          <div className="campo estreito">
            <label htmlFor="data">Data</label>
            <input
              id="data"
              type="date"
              value={campos.data}
              onChange={(e) => setCampos({ ...campos, data: e.target.value })}
              required
            />
          </div>
        )}
      </div>

      {erroGeral && <p className="alerta erro">{erroGeral}</p>}

      <div className="acoes">
        <button type="submit" className="botao primario" disabled={salvando}>
          {salvando ? 'Salvando…' : emEdicao ? 'Salvar alterações' : 'Adicionar'}
        </button>
        {emEdicao && (
          <button type="button" className="botao" onClick={aoCancelar}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  );
}
