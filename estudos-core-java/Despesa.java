import java.time.LocalDate;

/**
 * Representa uma despesa. Esta classe e o "molde" dos objetos que vao dentro
 * da ArrayList — e, no futuro, ela vira a @Entity do Spring Boot praticamente
 * sem mudanca nenhuma.
 */
public class Despesa {

    private int id;
    private String nome;
    private double valor;
    private LocalDate data;
    private Categoria categoria;

    public Despesa(int id, String nome, double valor, LocalDate data, Categoria categoria) {
        this.id = id;
        this.nome = nome;
        this.valor = valor;
        this.data = data;
        this.categoria = categoria;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
}
