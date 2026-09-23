# Controle de Despesas
<br>

### Importante!!!


Este projeto foi desenvolvido durante meus estudos de Java com forte apoio de ferramentas de IA. O objetivo principal foi explorar a construção de uma aplicação full-stack e entender, na prática, como Java, Spring Boot, banco de dados e outras tecnologias se integram.

Meu foco atual de estudos está nos fundamentos de Java, e as tecnologias mais avançadas utilizadas neste projeto foram exploradas como parte desse processo.

Aplicação full-stack multiusuário para registrar despesas pessoais e acompanhar o total
por período e por categoria. Cada pessoa cria sua conta e enxerga apenas os próprios dados.


<br>
<br>

#
| Camada | Tecnologia | Onde roda |
|---|---|---|
| Front-end | React 19 + TypeScript + Vite | Vercel |
| Back-end | Java 21 + Spring Boot 4.1 | VPS (Docker) |
| Banco | PostgreSQL 17 + Flyway | VPS (Docker) |
| Autenticação | JWT (HS256) + BCrypt | — |
| TLS / proxy | Caddy (Let's Encrypt) | VPS |

```
Navegador ──HTTPS──> Vercel (React)
                        │
                        │ HTTPS + Bearer <token>
                        ▼
                  Caddy (:443, TLS)
                        │ 127.0.0.1:8080
                        ▼
                Spring Boot (Docker)
                        │ 127.0.0.1:5432
                        ▼
                 PostgreSQL (Docker)
```

## Estrutura

```
backend/                 API Spring Boot
  src/main/java/com/rodrigo/despesas/
    common/              erros (ProblemDetail) e id do usuário autenticado
    config/              segurança, JWT, CORS
    despesa/             entidade, repositório, serviço, controller, DTOs
    usuario/             cadastro, login e emissão de token
  src/main/resources/
    db/migration/        migrations do Flyway
frontend/                SPA React + TypeScript
estudos-core-java/       CRUD em console (exercício de Core Java, sem framework)
docker-compose.yml       Postgres + API
Caddyfile                proxy HTTPS da VPS
```

## Rodando localmente

### Com Docker (igual à produção)

```bash
cp .env.example .env                  # banco e CORS
cp .env.secrets.example .env.secrets  # chave do JWT
docker compose up --build
```

A API sobe em `http://localhost:8080`. Crie sua conta pela própria tela do front.

> A chave do JWT fica em `.env.secrets` por um motivo prático: se ela contiver `$`,
> o Compose interpreta como variável e entrega a chave pela metade — todos os tokens
> ficariam inválidos sem nenhum erro no boot. Esse arquivo é lido com `format: raw`.

### Sem Docker

Precisa de um PostgreSQL local com banco `despesas`.

```bash
# back-end
cd backend
export JWT_SECRET="$(openssl rand -base64 48)"
./mvnw spring-boot:run

# front-end (outro terminal)
cd frontend
npm install
npm run dev          # http://localhost:5173
```

Em desenvolvimento o Vite faz proxy de `/api` para `localhost:8080`, então não há CORS
nem variável de ambiente para configurar.

## Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DATABASE_URL` | não | JDBC do Postgres (padrão: `localhost:5432/despesas`) |
| `DATABASE_USER` / `DATABASE_PASSWORD` | sim em prod | credenciais do banco |
| `JWT_SECRET` | **sim** | chave de assinatura dos tokens, mín. 32 caracteres (fica em `.env.secrets`) |
| `JWT_VALIDADE_MINUTOS` | não | validade do token (padrão: 480) |
| `CORS_ORIGENS` | sim em prod | URL exata do front (ex.: `https://app.vercel.app`) |

Sem `JWT_SECRET` a aplicação **não sobe**. É proposital: uma chave padrão em código
equivaleria a não ter autenticação — qualquer um poderia assinar um token válido para
qualquer conta.

Trocar a chave desconecta todo mundo (os tokens antigos param de validar), mas não
apaga nada: basta fazer login de novo.

No front, `VITE_API_URL` aponta para o domínio HTTPS da API (só em produção).

## API

Tudo sob `/api/v1`. Erros seguem RFC 9457 (`ProblemDetail`).

### Público

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth/registro` | cria a conta e já devolve o token (201) |
| `POST` | `/auth/login` | autentica e devolve o token |
| `GET` | `/actuator/health` | health check |

### Autenticado (`Authorization: Bearer <token>`)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/auth/eu` | dados da conta do token |
| `GET` | `/despesas?inicio&fim&categoria&page&size` | lista paginada |
| `GET` | `/despesas/resumo?inicio&fim` | total do período + quebra por categoria |
| `GET` | `/despesas/{id}` | busca uma |
| `POST` | `/despesas` | cria (201 + `Location`) |
| `PUT` | `/despesas/{id}` | atualiza |
| `DELETE` | `/despesas/{id}` | remove (204) |
| `GET` | `/categorias` | categorias disponíveis |

## Segurança

**Credenciais**
- Senha guardada só como hash **BCrypt** com custo 12 — nunca em texto puro, nem no log, nem na resposta.
- Senha igual ao próprio e-mail e as campeãs de lista de força bruta são recusadas no cadastro.
- Login com e-mail inexistente e login com senha errada devolvem exatamente a mesma
  resposta, para não revelar quem tem conta no sistema.

**Token**
- JWT HS256 com algoritmo **fixado** no decodificador — fecha a porta para confusão de algoritmo.
- O emissor é validado além da expiração: um token assinado com a mesma chave para
  outro propósito não é aceito.
- Sem `JWT_SECRET` (mínimo 32 caracteres) a aplicação não sobe.

**Autorização**
- O `subject` do token é o id do dono, e é dele que sai o filtro de **toda** consulta.
  O cliente não tem como pedir os dados de outra conta: o id não vem do request.
- `LancamentoRepository` **não expõe** `findById` — toda assinatura exige o dono, então
  o filtro não tem como ser esquecido em um método novo.
- Recurso de outra conta responde **404**, não 403. Um 403 confirmaria que aquele id existe.

**Superfície exposta**
- `/api/v1/auth/**` é limitado a 10 tentativas por IP por minuto (429 acima disso),
  aplicado **antes** de tocar o banco ou o BCrypt.
- Só a API é publicada no host, em `127.0.0.1`. O Postgres fica na rede interna do
  Compose, sem porta no host.
- Contêineres com `no-new-privileges` e todas as *capabilities* removidas (o Postgres
  recebe de volta só as cinco que precisa para inicializar o volume).
- Caddy aplica HSTS, `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`,
  `Content-Security-Policy: default-src 'none'` e `Permissions-Policy`.

**O que ainda falta**
- Não há confirmação de e-mail. Por isso o cadastro ainda revela se um e-mail já tem
  conta (responde 409) — a limitação de taxa reduz o problema, mas não o elimina.
- Não há revogação de token: sair do app apaga a credencial do navegador, mas um token
  já emitido continua válido até expirar (padrão de 8 horas).

## Testes

```bash
cd backend
./mvnw test      # unitários (não precisam de Docker)
./mvnw verify    # + integração: sobe um Postgres real via Testcontainers
```

Os testes de integração (`*IT`) garantem que as migrations batem com o mapeamento JPA,
que a soma fecha em centavos, que o cadastro e o login funcionam — e, principalmente,
que **uma conta não alcança os dados da outra** (nem lendo, nem editando, nem apagando).
Rodam no CI a cada push.

## Deploy

### Front (Vercel)

1. Importe o repositório, com **Root Directory = `frontend`**.
2. Defina `VITE_API_URL = https://api.seu-dominio.com.br`.
3. Deploy.

### Back (VPS)

```bash
# na VPS, com Docker instalado
git clone <repo> && cd EconomicJava
cp .env.example .env
cp .env.secrets.example .env.secrets
docker compose up -d --build
```

Depois aponte um registro `A` do seu domínio para o IP da VPS, ajuste o domínio no
`Caddyfile`, copie para `/etc/caddy/Caddyfile` e recarregue o Caddy.

> **Por que o domínio é obrigatório:** uma página servida em HTTPS (Vercel) não pode
> chamar um endereço HTTP. O navegador bloqueia por *mixed content*. Sem TLS na API,
> o app não funciona em produção — não é opcional.

## Decisões de projeto

- **`BigDecimal` + `NUMERIC(12,2)`** para dinheiro. `double` perde centavo: `0.1 + 0.2` dá `0.30000000000000004`.
- **Isolamento no repositório, não no controller.** Não existe um `findById` simples em
  `DespesaRepository`: toda assinatura exige o id do dono, então o filtro não tem como
  ser esquecido em um método novo.
- **Flyway com `ddl-auto: validate`.** O schema é versionado em SQL; o Hibernate só confere se bate. Nunca `update` em produção.
- **Somas no banco (`SUM`/`GROUP BY`)**, não em memória — a resposta não cresce com o histórico.
- **DTOs separados da entidade.** Mudar uma coluna não muda o contrato do front sem alguém perceber.
- **`PagedModel` em vez de `Page`.** O JSON do `Page` é interno do Spring Data e muda entre versões.
