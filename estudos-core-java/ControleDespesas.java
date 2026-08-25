import java.util.ArrayList;
import java.util.List;

/**
 * O coracao do sistema: guarda as despesas em uma LISTA DINAMICA (ArrayList)
 * e faz os calculos.
 *
 * Diferenca que importa: um array comum (new Despesa[10]) tem tamanho fixo —
 * se voce cadastrar a 11a despesa, estoura. A ArrayList cresce sozinha.
 */
public class ControleDespesas {

    private final List<Despesa> despesas = new ArrayList<>();
    private int proximoId = 1;

    // ---------- C R U D ----------

    /** CREATE — adiciona no fim da lista e devolve a despesa ja com id. */
    public Despesa adicionar(String nome, double valor, java.time.LocalDate data, Categoria categoria) {
        Despesa despesa = new Despesa(proximoId, nome, valor, data, categoria);
        despesas.add(despesa);
        proximoId++;
        return despesa;
    }

    /** READ — devolve uma copia da lista, para ninguem de fora mexer na original. */
    public List<Despesa> listarTodas() {
        return new ArrayList<>(despesas);
    }

    /** READ — procura pelo id. Retorna null se nao encontrar. */
    public Despesa buscarPorId(int id) {
        for (Despesa despesa : despesas) {
            if (despesa.getId() == id) {
                return despesa;
            }
        }
        return null;
    }

    /** UPDATE — altera os dados da despesa. Retorna false se o id nao existir. */
    public boolean atualizar(int id, String nome, double valor, java.time.LocalDate data, Categoria categoria) {
        Despesa despesa = buscarPorId(id);
        if (despesa == null) {
            return false;
        }
        despesa.setNome(nome);
        despesa.setValor(valor);
        despesa.setData(data);
        despesa.setCategoria(categoria);
        return true;
    }

    /** DELETE — remove da lista. Retorna false se o id nao existir. */
    public boolean remover(int id) {
        Despesa despesa = buscarPorId(id);
        if (despesa == null) {
            return false;
        }
        despesas.remove(despesa);
        return true;
    }

    public boolean estaVazio() {
        return despesas.isEmpty();
    }

    public int quantidade() {
        return despesas.size();
    }

    // ---------- C A L C U L O S ----------

    /** Soma tudo que esta na lista. */
    public double somarTotal() {
        double total = 0;
        for (Despesa despesa : despesas) {
            total += despesa.getValor();
        }
        return total;
    }

    /** Soma so as despesas de um ano especifico. */
    public double somarTotalDoAno(int ano) {
        double total = 0;
        for (Despesa despesa : despesas) {
            if (despesa.getData().getYear() == ano) {
                total += despesa.getValor();
            }
        }
        return total;
    }

    /**
     * A MATRIZ.
     *
     * Monta uma tabela de 12 linhas (janeiro..dezembro) por N colunas
     * (uma para cada Categoria). Cada celula [mes][categoria] guarda o total
     * gasto naquele mes, naquela categoria, no ano pedido.
     *
     * Exemplo: relatorio[0][Categoria.LAZER.ordinal()] = gasto com lazer em janeiro.
     */
    public double[][] relatorioMensalPorCategoria(int ano) {
        double[][] relatorio = new double[12][Categoria.values().length];

        for (Despesa despesa : despesas) {
            if (despesa.getData().getYear() != ano) {
                continue;
            }
            int linha = despesa.getData().getMonthValue() - 1;   // getMonthValue() da 1..12, a matriz usa 0..11
            int coluna = despesa.getCategoria().ordinal();
            relatorio[linha][coluna] += despesa.getValor();
        }

        return relatorio;
    }
}
