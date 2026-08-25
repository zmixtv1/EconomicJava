# Rodando no Raspberry Pi 4

Guia para publicar o app num Pi 4 com Raspberry Pi OS Lite (só terminal).

---

## 0. Confira que o sistema é 64 bits

**Faça isto antes de qualquer outra coisa.** As imagens do projeto são `arm64`;
num sistema de 32 bits nada sobe.

```bash
uname -m
```

- `aarch64` → certo, siga em frente
- `armv7l` → é 32 bits. Reinstale com **Raspberry Pi OS Lite (64-bit)** usando o
  Raspberry Pi Imager. Não há como contornar.

---

## 1. Sistema em dia e fuso correto

```bash
sudo apt update && sudo apt full-upgrade -y
sudo timedatectl set-timezone America/Sao_Paulo
```

O fuso importa: o app decide o mês de cada lançamento por data.

---

## 2. Segure o desgaste do cartão SD

Sem SSD, o inimigo é escrita constante — e a maior parte dela não é sua, é log
do sistema.

```bash
# Limita o journal a 50 MB e o mantém em memória
sudo mkdir -p /etc/systemd/journald.conf.d
printf '[Journal]\nStorage=volatile\nRuntimeMaxUse=50M\n' | \
  sudo tee /etc/systemd/journald.conf.d/limite.conf
sudo systemctl restart systemd-journald
```

Os logs do Docker já vêm limitados pelo `docker-compose.pi.yml`.

---

## 3. Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
```

Saia e entre de novo na sessão SSH (o grupo só vale no próximo login), depois:

```bash
docker run --rm hello-world
```

---

## 4. Traga o código

```bash
sudo apt install -y git
git clone <URL-DO-SEU-REPOSITORIO> ~/EconomicJava
cd ~/EconomicJava
```

---

## 5. Crie os segredos

```bash
cp .env.example .env
cp .env.secrets.example .env.secrets

# Chave do JWT e senha do banco, geradas na hora
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')|" .env.secrets
sed -i "s|^DATABASE_PASSWORD=.*|DATABASE_PASSWORD=$(openssl rand -base64 24 | tr -d '\n')|" .env
```

Depois edite o `.env` e preencha:

| Variável | O que pôr |
|---|---|
| `URL_DO_APP` | endereço público do front (passo 7) |
| `CORS_ORIGENS` | o mesmo endereço, sem barra no final |
| `SMTP_*` | credenciais do provedor (passo 6) |
| `GOOGLE_CLIENT_ID` | o ID do cliente OAuth |

---

## 6. E-mail: sem isso ninguém consegue entrar

Atenção a esta parte. O cadastro **exige confirmação por e-mail**. Se o envio
não funcionar, o cadastro responde 202 normalmente, mas o link nunca chega — e
como o login é bloqueado até confirmar, **ninguém consegue criar conta**.

O Mailpit não sobe no Pi (ele é ferramenta de desenvolvimento).

**Brevo** é o caminho grátis sem precisar de domínio próprio: crie a conta,
verifique um endereço remetente (seu Gmail serve) e pegue as credenciais SMTP.

```bash
SMTP_HOST=smtp-relay.brevo.com
SMTP_PORT=587
SMTP_USUARIO=<o login que a Brevo te der>
SMTP_AUTH=true
SMTP_TLS=true
EMAIL_REMETENTE=Controle de Despesas <seu-email-verificado@gmail.com>
```

A senha vai no `.env.secrets`, em `SMTP_SENHA` — esse arquivo é lido em formato
bruto justamente porque chaves de API costumam ter `$`.

---

## 7. Publique na internet sem abrir porta

A maioria das operadoras usa CGNAT: sua casa não tem IP público, e
redirecionamento de porta simplesmente não funciona. Teste:

```bash
# Se o IP daqui for diferente do IP do seu roteador, você está atrás de CGNAT
curl -s https://api.ipify.org; echo
```

**Opção A — Tailscale Funnel (grátis, sem domínio)**

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
sudo tailscale funnel 8080
```

Devolve um endereço `https://<nome>.<rede>.ts.net` estável, com certificado
válido. Não abre porta nenhuma no roteador.

**Opção B — Cloudflare Tunnel (grátis, precisa de domínio)**

Melhor se você já tem ou vai comprar um domínio. Instale o `cloudflared`, crie
o túnel no painel da Cloudflare e aponte para `localhost:8080`.

Nos dois casos o Caddy fica opcional: o túnel já entrega o HTTPS.

---

## 8. Suba

```bash
cd ~/EconomicJava
docker compose -f docker-compose.yml -f docker-compose.pi.yml up -d --build
```

O primeiro build compila o projeto com Maven dentro do contêiner e leva
**vários minutos** no Pi. Os próximos reaproveitam o cache.

```bash
docker compose ps
curl -s localhost:8080/actuator/health
```

Como os serviços usam `restart: unless-stopped`, tudo volta sozinho ao reiniciar.

---

## 9. Ligue o front ao back

No painel da Vercel, defina `VITE_API_URL` com o endereço do passo 7 e refaça o
deploy. Depois acerte, com o **mesmo endereço**:

- `CORS_ORIGENS` no `.env` do Pi (e reinicie a API)
- **Origens JavaScript autorizadas** no console do Google

Os três precisam bater exatamente — sem barra no final.

---

## 10. Backup

Com o banco no cartão SD, isto não é opcional.

```bash
chmod +x deploy/backup.sh
crontab -e
```

Acrescente:

```
0 3 * * * /home/pi/EconomicJava/deploy/backup.sh >> /home/pi/backup.log 2>&1
```

Guarda as últimas 7 cópias em `~/backups`. **Copie-as para fora do Pi de vez em
quando** — backup no mesmo cartão que pode falhar não é backup.

---

## Manutenção

```bash
# Ver o que está acontecendo
docker compose logs -f api

# Atualizar depois de um git push
git pull && docker compose -f docker-compose.yml -f docker-compose.pi.yml up -d --build

# Memória e CPU
docker stats --no-stream
```

---

## Se algo der errado

| Sintoma | Causa provável |
|---|---|
| `exec format error` | sistema de 32 bits (volte ao passo 0) |
| API reiniciando sem parar | falta `JWT_SECRET` — veja `docker compose logs api` |
| Cadastro responde 202 mas o e-mail não chega | SMTP não configurado (passo 6) |
| Front carrega mas toda chamada falha | `CORS_ORIGENS` diferente da URL real |
| Botão do Google não aparece | `GOOGLE_CLIENT_ID` vazio |
| `The given origin is not allowed` | falta a URL nas origens autorizadas do Google |
