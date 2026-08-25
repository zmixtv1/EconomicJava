import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Menu de console do controle de despesas.
 *
 * Rodar:
 *   javac -d out src/*.java
 *   java -cp out Main
 */
public class Main {

    private static final Scanner ENTRADA = new Scanner(System.in);
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] NOMES_MESES = {
        "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
        "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private static final ControleDespesas controle = new ControleDespesas();

    public static void main(String[] args) {
        boolean rodando = true;

        while (rodando) {
            mostrarMenu();
            String opcao = ENTRADA.nextLine().trim();

            switch (opcao) {
                case "1" -> adicionarDespesa();
                case "2" -> listarDespesas();
                case "3" -> atualizarDespesa();
                case "4" -> removerDespesa();
                case "5" -> mostrarRelatorioMensal();
                case "0" -> {
                    System.out.println("\nAte mais!");
                    rodando = false;
                }
                default -> System.out.println("\n[!] Opcao invalida.");
            }
        }
    }

    private static void mostrarMenu() {
        System.out.println("\n=========================================");
        System.out.println("   CONTROLE DE DESPESAS");
        System.out.println("=========================================");
        System.out.printf("   %d despesa(s) | Total: %s%n", controle.quantidade(), moeda(controle.somarTotal()));
        System.out.println("-----------------------------------------");
        System.out.println("   1 - Adicionar despesa");
        System.out.println("   2 - Listar despesas");
        System.out.println("   3 - Atualizar despesa");
        System.out.println("   4 - Remover despesa");
        System.out.println("   5 - Relatorio mensal por categoria");
        System.out.println("   0 - Sair");
        System.out.print("\nEscolha: ");
    }

    // ---------- O P E R A C O E S ----------

    private static void adicionarDespesa() {
        System.out.println("\n--- NOVA DESPESA ---");
        String nome = lerTexto("Nome da conta: ");
        double valor = lerValor("Valor (ex: 149,90): ");
        LocalDate data = lerData("Data (dd/MM/aaaa): ");
        Categoria categoria = lerCategoria();

        Despesa nova = controle.adicionar(nome, valor, data, categoria);
        System.out.printf("%n[ok] Despesa #%d cadastrada.%n", nova.getId());
    }

    private static void listarDespesas() {
        List<Despesa> lista = controle.listarTodas();

        if (lista.isEmpty()) {
            System.out.println("\n[!] Nenhuma despesa cadastrada ainda.");
            return;
        }

        System.out.println("\n--- DESPESAS ---");
        System.out.printf("%-5s %-22s %-12s %-14s %s%n", "ID", "NOME", "DATA", "CATEGORIA", "VALOR");
        System.out.println("-".repeat(68));

        for (Despesa despesa : lista) {
            System.out.printf("%-5d %-22s %-12s %-14s %s%n",
                despesa.getId(),
                cortar(despesa.getNome(), 22),
                despesa.getData().format(FORMATO_DATA),
                despesa.getCategoria().getRotulo(),
                moeda(despesa.getValor()));
        }

        System.out.println("-".repeat(68));
        System.out.printf("%-55s %s%n", "TOTAL", moeda(controle.somarTotal()));
    }

    private static void atualizarDespesa() {
        if (controle.estaVazio()) {
            System.out.println("\n[!] Nenhuma despesa cadastrada ainda.");
            return;
        }

        listarDespesas();
        int id = lerInteiro("\nID da despesa a atualizar: ");

        Despesa atual = controle.buscarPorId(id);
        if (atual == null) {
            System.out.println("\n[!] Nao existe despesa com esse ID.");
            return;
        }

        System.out.println("\n--- NOVOS DADOS ---");
        String nome = lerTexto("Nome da conta: ");
        double valor = lerValor("Valor (ex: 149,90): ");
        LocalDate data = lerData("Data (dd/MM/aaaa): ");
        Categoria categoria = lerCategoria();

        controle.atualizar(id, nome, valor, data, categoria);
        System.out.printf("%n[ok] Despesa #%d atualizada.%n", id);
    }

    private static void removerDespesa() {
        if (controle.estaVazio()) {
            System.out.println("\n[!] Nenhuma despesa cadastrada ainda.");
            return;
        }

        listarDespesas();
        int id = lerInteiro("\nID da despesa a remover: ");

        if (controle.remover(id)) {
            System.out.printf("%n[ok] Despesa #%d removida.%n", id);
        } else {
            System.out.println("\n[!] Nao existe despesa com esse ID.");
        }
    }

    /** Le a matriz de ControleDespesas e imprime como tabela. */
    private static void mostrarRelatorioMensal() {
        if (controle.estaVazio()) {
            System.out.println("\n[!] Nenhuma despesa cadastrada ainda.");
            return;
        }

        int ano = lerInteiro("\nAno do relatorio (ex: " + LocalDate.now().getYear() + "): ");
        double[][] relatorio = controle.relatorioMensalPorCategoria(ano);
        Categoria[] categorias = Categoria.values();

        System.out.printf("%n--- RELATORIO %d ---%n", ano);

        System.out.printf("%-6s", "MES");
        for (Categoria categoria : categorias) {
            System.out.printf("%14s", categoria.getRotulo());
        }
        System.out.printf("%14s%n", "TOTAL MES");
        System.out.println("-".repeat(6 + 14 * (categorias.length + 1)));

        // Percorre a matriz: linha = mes, coluna = categoria.
        for (int mes = 0; mes < 12; mes++) {
            double totalDoMes = 0;
            System.out.printf("%-6s", NOMES_MESES[mes]);

            for (int cat = 0; cat < categorias.length; cat++) {
                System.out.printf("%14s", moeda(relatorio[mes][cat]));
                totalDoMes += relatorio[mes][cat];
            }

            System.out.printf("%14s%n", moeda(totalDoMes));
        }

        System.out.println("-".repeat(6 + 14 * (categorias.length + 1)));

        // Rodape: soma de cada coluna (total do ano por categoria).
        System.out.printf("%-6s", "ANO");
        for (int cat = 0; cat < categorias.length; cat++) {
            double totalDaCategoria = 0;
            for (int mes = 0; mes < 12; mes++) {
                totalDaCategoria += relatorio[mes][cat];
            }
            System.out.printf("%14s", moeda(totalDaCategoria));
        }
        System.out.printf("%14s%n", moeda(controle.somarTotalDoAno(ano)));
    }

    // ---------- L E I T U R A   D E   E N T R A D A ----------

    private static String lerTexto(String rotulo) {
        while (true) {
            System.out.print(rotulo);
            String texto = ENTRADA.nextLine().trim();
            if (!texto.isEmpty()) {
                return texto;
            }
            System.out.println("[!] Nao pode ficar em branco.");
        }
    }

    private static double lerValor(String rotulo) {
        while (true) {
            System.out.print(rotulo);
            String texto = ENTRADA.nextLine().trim().replace(",", ".");
            try {
                double valor = Double.parseDouble(texto);
                if (valor <= 0) {
                    System.out.println("[!] O valor precisa ser maior que zero.");
                    continue;
                }
                return valor;
            } catch (NumberFormatException e) {
                System.out.println("[!] Valor invalido. Use numeros, ex: 149,90");
            }
        }
    }

    private static int lerInteiro(String rotulo) {
        while (true) {
            System.out.print(rotulo);
            try {
                return Integer.parseInt(ENTRADA.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("[!] Digite um numero inteiro.");
            }
        }
    }

    private static LocalDate lerData(String rotulo) {
        while (true) {
            System.out.print(rotulo);
            String texto = ENTRADA.nextLine().trim();
            try {
                return LocalDate.parse(texto, FORMATO_DATA);
            } catch (DateTimeParseException e) {
                System.out.println("[!] Data invalida. Use o formato dd/MM/aaaa, ex: 05/03/2026");
            }
        }
    }

    private static Categoria lerCategoria() {
        Categoria[] categorias = Categoria.values();

        System.out.println("Categoria:");
        for (int i = 0; i < categorias.length; i++) {
            System.out.printf("  %d - %s%n", i + 1, categorias[i].getRotulo());
        }

        while (true) {
            int numero = lerInteiro("Escolha: ");
            Categoria escolhida = Categoria.porNumero(numero);
            if (escolhida != null) {
                return escolhida;
            }
            System.out.printf("[!] Escolha um numero de 1 a %d.%n", categorias.length);
        }
    }

    // ---------- F O R M A T A C A O ----------

    private static String moeda(double valor) {
        return String.format(Locale.of("pt", "BR"), "R$ %,.2f", valor);
    }

    private static String cortar(String texto, int tamanho) {
        if (texto.length() <= tamanho) {
            return texto;
        }
        return texto.substring(0, tamanho - 3) + "...";
    }
}
