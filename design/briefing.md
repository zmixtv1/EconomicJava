# Briefing para redesenho — Controle de Despesas

> Copie deste ponto até o fim do arquivo e cole no Claude Design.

---

Preciso do redesenho de um app web de **orçamento pessoal**, já construído e
funcionando. Não é um conceito: as telas abaixo existem, os dados são reais e
vêm de uma API. Quero uma versão visualmente melhor **mantendo todas as
funcionalidades e a estrutura de navegação**.

## O que o app faz

A pessoa monta o orçamento do mês em **6 passos, nesta ordem**, com botão
"Avançar" no rodapé de cada um:

1. **Receitas** — o que entra (salário, freelas)
2. **Despesas fixas** — o que se repete todo mês (aluguel, contas)
3. **Despesas variáveis** — gastos do mês (mercado, transporte)
4. **Economia** — reserva de emergência, investimentos
5. **Dívidas** — parcelas e financiamentos
6. **Visão geral** — o fechamento: quanto sobrou e para onde foi

Existe um **seletor de mês** no topo, sempre visível. Trocar o mês recarrega
tudo. Um lançamento marcado como "repete todo mês" aparece automaticamente em
todos os meses seguintes, sem ser relançado.

## Conceito central: pontual x mensal

Cada lançamento é **pontual** (tem uma data) **ou mensal** (tem uma vigência
que começa num mês e pode não ter fim). Dívidas têm número de parcelas, e a
lista mostra "Parcela 8 de 48", que muda conforme o mês navegado.

Isso precisa ficar visualmente claro na lista — hoje uso um selo "MENSAL" ao
lado da descrição e a coluna "Quando" diz "Todo mês desde 01/2026".

---

## Telas

### 1. Entrada (pública)

Cartão de ~1000×600 com **painel deslizante**, no estilo do "Diprella": um
painel de 40% da largura desliza da esquerda para a direita quando a pessoa
alterna entre Entrar e Criar conta, e o formulário ocupa os 60% restantes.

```
┌──────────────────────────────────────────────────────────┐
│ ░░░░░░░░░░░░░░░░░  │                                     │
│ ░ [logo]        ░  │            Entrar                   │
│ ░               ░  │     Use sua conta para ver o mês    │
│ ░ Primeira vez  ░  │                                     │
│ ░ aqui?         ░  │   [ G  Continuar com o Google  ]    │
│ ░               ░  │   ──────── ou com e-mail ────────   │
│ ░ Crie sua      ░  │                                     │
│ ░ conta em      ░  │   E-mail                            │
│ ░ 6 passos      ░  │   [__________________________]      │
│ ░               ░  │   Senha                             │
│ ░ [CRIAR CONTA] ░  │   [__________________________]      │
│ ░               ░  │                                     │
│ ░░░░░░░░░░░░░░░░░  │        [    ENTRAR    ]             │
│   painel desliza→  │      Esqueci minha senha            │
└──────────────────────────────────────────────────────────┘
```

Em "Criar conta" o painel vai para a direita e o formulário ganha o campo
**Nome** antes de e-mail e senha (mínimo 8 caracteres).

Abaixo de ~720px de largura, empilhar: formulário em cima, painel virando uma
faixa embaixo com o texto e o botão de alternar.

### 2. Telas auxiliares de conta (cartão único, centralizado)

- **Esqueceu a senha** — campo de e-mail + "Enviar link"
- **Falta confirmar seu e-mail** — texto + botão "Reenviar o link"
- **Verifique seu e-mail** — confirmação de que o link de recuperação foi enviado
- **Confirmando seu e-mail…** — estado de carregamento, vira erro se o link expirou
- **Criar nova senha** — campo de senha nova (chega por link do e-mail)

### 3. Aplicação — estrutura fixa

```
┌────────────┬────────────────────────────────────────────┐
│ [logo] Orç │ [☰]  Receitas      [🌙]  [agosto de 2026 ▾]│
├────────────┼────────────────────────────────────────────┤
│ ▸ Receitas①│  Comece pelo que entra no mês: salário…    │
│   Fixas   ②│  ┌──────────────────────────────────────┐  │
│   Variáv. ③│  │ RECEITAS NO MÊS                      │  │
│   Economia④│  │ R$ 8.300,00                          │  │
│   Dívidas ⑤│  │ 2 lançamentos                        │  │
│   Visão   ⑥│  └──────────────────────────────────────┘  │
│            │  ┌──────────────────────────────────────┐  │
│            │  │ NOVO LANÇAMENTO                      │  │
│            │  │ Descrição      Valor(R$)  Categoria  │  │
│            │  │ [__________]   [_____]    [______▾]  │  │
│            │  │ ☑ Repete todo mês                    │  │
│            │  │ A partir de      Até (opcional)      │  │
│            │  │ [ago 2026 ▾]     [_________▾]        │  │
│            │  │ [ Adicionar ]                        │  │
│            │  └──────────────────────────────────────┘  │
│            │  ┌──────────────────────────────────────┐  │
│            │  │ DESCRIÇÃO  CATEGORIA  QUANDO   VALOR │  │
│            │  │ Salário    (Salário)  Todo mês  7.200│  │
│            │  │  MENSAL               desde     [E][R]│ │
│            │  │ Freela     (Freelance) 14/08    1.100│  │
│            │  └──────────────────────────────────────┘  │
│ Rodrigo    │  [← Voltar]  PASSO 1 DE 6  [Avançar →]     │
│ [→] Sair   │  ▓▓▓░░░░░░░░░░░░░░                         │
└────────────┴────────────────────────────────────────────┘
```

**Menu lateral:** 236px, recolhe para 62px só com ícones ao clicar no
hambúrguer. Itens numerados de 1 a 6; o passo atual fica destacado e os já
visitados ficam marcados. Abaixo de 900px vira gaveta sobreposta.

**Barra superior:** hambúrguer, título da seção, alternador de tema
(claro/escuro) e seletor de mês.

**Rodapé de passos:** "Voltar" (com o nome do passo anterior), contador
"Passo N de 6" com barra de progresso, e "Avançar". No passo 5 o botão vira
"Ver o resumo"; no passo 6 vira "Recomeçar".

### 4. Passos 1 a 5 — todos com a mesma estrutura

- Cartão de destaque: rótulo, valor grande do tipo no mês, nº de lançamentos
- Formulário "Novo lançamento" (vira "Editando #12" ao editar)
- Tabela de lançamentos com Editar / Remover

**O formulário muda conforme o tipo:**

| Tipo | Campos extras |
|---|---|
| Receitas, Economia | caixa "Repete todo mês" (marcada por padrão) |
| Despesas fixas | sempre mensal, sem a caixa |
| Despesas variáveis | sempre pontual, só campo Data |
| Dívidas | mensal + campo **Parcelas** (o fim é calculado sozinho) |

Quando é mensal: campos "A partir de" e "Até (opcional)" no formato mês/ano.
Quando é pontual: campo Data.

### 5. Passo 6 — Visão geral

```
┌────────────────────────────────────────────────────────┐
│ DISPONÍVEL PARA GASTAR                                 │
│ R$ 1.793,75                                            │
│ De R$ 8.300,00 que entraram, R$ 6.506,25 já têm destino│
└────────────────────────────────────────────────────────┘
┌────────┬────────┬────────┬────────┬────────┐
│RECEITAS│ FIXAS  │VARIÁVEIS│ECONOMIA│DÍVIDAS │
│8.300,00│3.174,60│1.151,65 │1.200,00│ 980,00 │
└────────┴────────┴────────┴────────┴────────┘
┌────────────────────────────────────────────────────────┐
│ PARA ONDE FOI O DINHEIRO                               │
│      ╭─────╮      ● Despesa fixa    38.2%   3.174,60   │
│     ╱       ╲     ● Despesa variável 13.9%  1.151,65   │
│    │DISPONÍV.│    ● Dívida          11.8%    980,00    │
│    │ 1.793,75│    ● Economia        14.5%  1.200,00    │
│     ╲       ╱     ● Disponível      21.6%  1.793,75    │
│      ╰─────╯                                           │
└────────────────────────────────────────────────────────┘
┌────────────────────────────────────────────────────────┐
│ SAÍDAS POR CATEGORIA                                   │
│ Moradia                                     1.850,00   │
│ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓     │
│ Financiamento de veículo                      980,00   │
│ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                               │
└────────────────────────────────────────────────────────┘
```

---

## Regras que o desenho precisa respeitar

**O gráfico de rosca**

- Tem no máximo **5 fatias**, sempre nesta ordem fixa: despesa fixa, despesa
  variável, dívida, economia, disponível. **Não reordenar por valor** — a cor
  precisa seguir a categoria, não a posição, para não trocar de cor a cada mês.
- "Disponível" (o que sobrou) é uma fatia, porque o gráfico mostra o destino da
  **receita**, não só a divisão dos gastos.
- Os percentuais vêm prontos do servidor e somam exatamente 100%.
- As cores atuais foram validadas para daltonismo (protanopia e deuteranopia) e
  contraste. Se mudar a paleta, ela precisa manter fatias vizinhas
  distinguíveis. Cores atuais:
  - claro: `#0891b2` `#1e40af` `#0d9488` `#1d4ed8` `#0f766e`
  - escuro: `#4dd8ee` `#3b82f6` `#2dd4bf` `#2563eb` `#14b8a6`

**Estados que precisam existir em cada tela**

- **Carregando** — esqueleto, não spinner no meio da tela
- **Vazio** — "Nada lançado neste mês", sem parecer erro
- **Erro** — faixa com a mensagem e botão "Tentar de novo"
- **Saldo negativo** — o valor de "Disponível" fica vermelho, o texto muda para
  "As saídas passam as entradas em R$ X", e a fatia "Disponível" **some** do
  gráfico

**Tema**

Claro por padrão, com alternador para escuro. O escuro **não é o claro
invertido** — precisa de seus próprios tons. Os dois tratados com o mesmo
cuidado.

**Responsivo**

Nada de encolher a tela inteira com `transform: scale()`. Abaixo de 720px
(entrada) e 900px (app) o conteúdo empilha, e a tabela vira cartões com rótulo
à esquerda e valor à direita.

**Movimento**

Animações discretas que expliquem de onde a coisa veio: blocos entrando em
cascata, a rosca se desenhando do zero, o número principal contando até o valor
ao trocar de mês. Tudo desligado quando o sistema pede `prefers-reduced-motion`.

**Acessibilidade**

Nada identificado só por cor — a legenda do gráfico traz rótulo, percentual e
valor em texto. Foco visível em todo elemento interativo.

---

## Identidade atual

- **Paleta:** ciano e azul sobre cinza-azulado. O cinza carrega a interface, o
  azul comanda a ação, o ciano marca o que é dado (total, barras, foco).
- **Gradiente de ação:** `linear-gradient(135deg, #1265c6, #0aa9c7)` nos botões
  principais e no número em destaque.
- **Números de dinheiro** em fonte tabular, para as colunas alinharem.
- A tela de entrada usa **neumorfismo** (relevo por sombra dupla, sem borda);
  o resto do app usa cartões com borda e sombra suave.

Fique à vontade para propor outra direção visual — só me diga o que está
mudando e por quê. O que não pode mudar é a ordem dos 6 passos, o conceito de
pontual x mensal, e as regras do gráfico acima.

## Formato da entrega

Quero as telas em alta fidelidade, no claro e no escuro, em desktop e celular:

1. Entrada (Entrar e Criar conta)
2. Um passo de tipo (use "Despesas fixas" com 3 lançamentos)
3. Visão geral completa
4. Estados: vazio, carregando, erro e saldo negativo

Use valores em reais realistas, como os dos exemplos acima.
