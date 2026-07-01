# FinFlow API — Documentação de Endpoints

Última atualização: 2026-06-30

## 🌐 URL Base

- **Produção (Railway):** https://finflow-backend-production-5d94.up.railway.app
- **Local:** http://localhost:8080

## 🔐 Autenticação

- Rotas que exigem token: todas exceto `/api/auth/**` e `/api/health`.
- Header: `Authorization: Bearer <token_jwt>`
- Content-Type: `application/json` em todos os POST/PUT.

---

## 📊 Tabela completa de endpoints

| Método | Rota | Autenticação | Descrição |
|--------|------|--------------|-----------|
| `POST` | `/api/auth/register` | Público | Registar novo utilizador |
| `POST` | `/api/auth/login` | Público | Login (devolve token JWT + email) |
| `POST` | `/api/auth/forgot-password` | Público | Recuperação: Enviar e-mail |
| `POST` | `/api/auth/reset-password` | Público | Recuperação: Definir nova senha |
| `GET`  | `/api/health` | Público | Health check (estado da app, BD, uptime) |
| `GET`  | `/api/dashboard` | Token | Indicadores financeiros do utilizador |
| `GET`  | `/api/incomes` | Token | Listar todas as receitas |
| `GET`  | `/api/incomes/{id}` | Token | Ver uma receita específica |
| `POST` | `/api/incomes` | Token | Criar uma receita |
| `PUT`  | `/api/incomes/{id}` | Token | Atualizar uma receita |
| `DELETE` | `/api/incomes/{id}` | Token | Eliminar uma receita |
| `GET`  | `/api/expenses` | Token | Listar todas as despesas |
| `GET`  | `/api/expenses/{id}` | Token | Ver uma despesa específica |
| `POST` | `/api/expenses` | Token | Criar uma despesa |
| `PUT`  | `/api/expenses/{id}` | Token | Atualizar uma despesa |
| `DELETE` | `/api/expenses/{id}` | Token | Eliminar uma despesa |
| `GET`  | `/api/goals` | Token | Listar metas do utilizador |
| `GET`  | `/api/goals/{id}` | Token | Ver uma meta específica |
| `POST` | `/api/goals` | Token | Criar uma meta |
| `PUT`  | `/api/goals/{id}` | Token | Atualizar uma meta |
| `DELETE` | `/api/goals/{id}` | Token | Eliminar uma meta |
| `GET`  | `/api/categories` | Token | Listar categorias do utilizador |
| `POST` | `/api/categories` | Token | Criar uma categoria |
| `DELETE` | `/api/categories/{id}` | Token | Eliminar uma categoria |
| `GET`  | `/api/users/profile` | Token | Obter dados do perfil |
| `PUT`  | `/api/users/profile` | Token | Atualizar nome, apelido, biografia |
| `PUT`  | `/api/users/avatar` | Token | Atualizar foto de perfil |
| `PUT`  | `/api/users/password` | Token | Alterar password (valida antiga) |
| `GET`  | `/api/credit-cards` | Token | Listar cartões do utilizador |
| `GET`  | `/api/credit-cards/{id}` | Token | Ver detalhe de um cartão |
| `POST` | `/api/credit-cards` | Token | Criar um cartão |
| `PUT`  | `/api/credit-cards/{id}` | Token | Atualizar um cartão |
| `DELETE` | `/api/credit-cards/{id}` | Token | Eliminar um cartão |

---

## 📝 Exemplos de corpos (body) para POST/PUT

**Receita / Despesa:**

```json
{
  "description": "Descrição",
  "amount": 100.00,
  "date": "2026-06-18",
  "categoryId": 1
}
```

**Meta:**

```json
{
  "name": "Nome da meta",
  "targetAmount": 5000.00,
  "currentAmount": 500.00,
  "deadline": "2026-12-31"
}
```

**Categoria:**

```json
{
  "name": "Nome da categoria"
}
```

**Perfil:**

```json
{
  "firstName": "Nome",
  "lastName": "Apelido",
  "bio": "Biografia"
}
```

**Avatar:**

```json
{
  "avatarUrl": "https://exemplo.com/foto.jpg"
}
```

**Password:**

```json
{
  "oldPassword": "password_antiga",
  "newPassword": "password_nova"
}
```

**Esqueci a Senha:**

```json
{
  "email": "utilizador@email.com"
}
```

**Reset de Senha:**

```json
{
  "token": "token-recebido-por-email",
  "newPassword": "nova-password-segura"
}
```

**Cartão de crédito:**

```json
{
  "name": "Visa Millennium",
  "cardholderName": "NOME TITULAR",
  "lastFourDigits": "4321",
  "expirationDate": "2031-05-01",
  "type": "CREDIT",
  "limitAmount": 1500.00,
  "currentBill": 300.00,
  "brand": "VISA"
}
```

**Cartão de débito:**

```json
{
  "name": "ActivoBank Debito",
  "cardholderName": "NOME TITULAR",
  "lastFourDigits": "9876",
  "expirationDate": "2028-12-01",
  "type": "DEBIT",
  "limitAmount": null,
  "currentBill": null,
  "brand": "MASTERCARD"
}
```

---

## 📅 Formato de datas

- Todas as datas: **yyyy-MM-dd** (ex.: `"2026-06-18"`)

---

## ⚠️ Erros comuns

- Não enviar token nos endpoints públicos (`/api/auth/**`, `/api/health`)
- Não usar `Content-Type` diferente de `application/json`
- Não enviar número completo do cartão (apenas 4 últimos dígitos)
- Datas no formato errado (não usar `dd/MM/yyyy` nem `MM/yyyy`)

---