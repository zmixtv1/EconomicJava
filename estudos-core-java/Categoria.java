/**
 * As categorias possiveis de uma despesa.
 *
 * Um enum e uma lista fixa de constantes. Cada constante tem um indice
 * automatico (ordinal), comecando em 0 — e e exatamente esse indice que
 * usamos como COLUNA da matriz do relatorio mensal.
 */
public enum Categoria {

    ALIMENTACAO("Alimentacao"),
    MORADIA("Moradia"),
    TRANSPORTE("Transporte"),
    LAZER("Lazer"),
    SAUDE("Saude"),
    OUTROS("Outros");

    private final String rotulo;

    Categoria(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /**
     * Converte o numero digitado no menu (1, 2, 3...) na categoria correspondente.
     * Retorna null se o numero nao existir.
     */
    public static Categoria porNumero(int numero) {
        Categoria[] todas = values();
        if (numero < 1 || numero > todas.length) {
            return null;
        }
        return todas[numero - 1];
    }
}
