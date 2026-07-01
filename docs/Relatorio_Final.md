# Relatório Final do Projeto: FinFlow

## 1. Introdução
O **FinFlow** é um ecossistema de gestão financeira pessoal desenvolvido para plataformas Android. O objetivo principal do projeto foi criar uma ferramenta intuitiva, segura e performante para que utilizadores pudessem monitorizar as suas receitas, despesas, metas financeiras e cartões de crédito em tempo real, utilizando uma infraestrutura moderna de nuvem.

---

## 2. Estrutura e Infraestrutura do Projeto

### Arquitetura (MVVM Light)
O projeto segue uma organização lógica onde a UI (User Interface) está separada da lógica de dados:
- **`ui/`**: Contém as Activities e Screens (Jetpack Compose).
- **`api/`**: Responsável pela comunicação com o mundo externo (Retrofit).
- **`model/`**: Definição das estruturas de dados (POJOs).
- **`utils/`**: Ferramentas de auxílio (Gestão de tokens, exportação de arquivos).

### Bibliotecas Principais e Motivação
| Biblioteca | Finalidade | Porquê? |
| :--- | :--- | :--- |
| **Jetpack Compose** | UI Declarativa | Representa o padrão moderno do Android, permitindo criar interfaces ricas com menos código e mais performance. |
| **Retrofit + OkHttp** | Cliente HTTP | É o padrão da indústria para consumir APIs REST, oferecendo segurança (SSL), tratamento de timeouts e interceptores. |
| **Moshi / Gson** | Serialização JSON | Converte automaticamente os dados recebidos da API em objetos Kotlin. |
| **Coil** | Carregamento de Imagens | Biblioteca leve e performante para lidar com imagens assíncronas. |
| **Material 3** | Design System | Garante uma interface moderna com componentes acessíveis e visual "premium". |

---

## 3. O "Coração" do Projeto: Integração com a API

### Como acessamos a API?
Utilizamos o padrão **Singleton** através do `RetrofitClient`. 
1. **Configuração**: Definimos a `BASE_URL` (hospedada no Railway).
2. **Interceptor de Segurança**: Criamos um `Interceptor` no OkHttp que injeta automaticamente o token JWT no cabeçalho `Authorization: Bearer <token>` em todas as requisições (exceto no login/registo).
3. **Timeouts**: Configuramos 30 segundos de espera para garantir que, mesmo em redes instáveis, a aplicação não falhe imediatamente.

### O que acontece por trás do processo?
Quando o utilizador clica em "Ver Despesas":
1. A UI chama o método `getExpenses()` da `ApiService`.
2. O Retrofit transforma essa chamada numa requisição HTTP GET.
3. O OkHttp adiciona o Token de autenticação guardado no `TokenManager`.
4. A requisição viaja até ao servidor, que valida o token.
5. O servidor responde com uma lista JSON.
6. O Retrofit converte esse JSON para uma `List<Expense>` e devolve para a UI atualizar o estado.

---

## 4. Diário de Bordo: Alterações e Desafios

### Dificuldades e Soluções
*   **Gestão de Imagens (Base64):**
    *   *Problema:* Enviar imagens pesadas causava erros de memória ou lentidão na API.
    *   *Solução:* Implementamos um redimensionamento agressivo (máx 300x300px) e compressão JPEG antes de converter para Base64 no `PerfilScreen.kt`.
*   **Sincronização de Estado:**
    *   *Problema:* Ao voltar de um ecrã de inserção, os dados no Dashboard não atualizavam.
    *   *Solução:* Implementamos um `resumeEpoch` (Timestamp) que força o `LaunchedEffect` a recarregar os dados da API sempre que a Activity volta ao foco (`onResume`).
*   **Segurança de Rotas:**
    *   *Problema:* Enviar o token JWT para a rota de login causava comportamentos inesperados no backend.
    *   *Solução:* Adicionamos uma lógica no interceptor para ignorar rotas que contenham `/api/auth`.

---

## 5. Guia Passo-a-Passo para Iniciantes (Como replicar este projeto)

### Passo 1: Configuração do Ambiente
- Instalar Android Studio (Koala ou superior).
- Criar um projeto "Empty Compose Activity".
- Adicionar as dependências no `libs.versions.toml` (Retrofit, Compose, Material3).

### Passo 2: Infraestrutura de Dados
- Criar as classes em `model/` que espelham o JSON da API.
- Configurar o `TokenManager` usando `SharedPreferences` para salvar o login.

### Passo 3: Comunicação em Rede
- Criar a interface `ApiService` com as rotas (GET, POST, DELETE).
- Criar o `RetrofitClient` para gerir a ligação.

### Passo 4: Construção da UI
- Começar pelo `LoginActivity` para autenticação.
- Criar o `MainActivity` com um `Scaffold` e `NavigationBar` para navegar entre as abas.
- Implementar o `DashboardScreen` como o centro de informações.

### Passo 5: Refinamento
- Implementar diálogos de confirmação (`AlertDialog`) para ações destrutivas como apagar conta.
- Adicionar feedback visual (`Toast` e `CircularProgressIndicator`).

---

## 6. Lógica e Motivações
- **Escolha do Dark Theme:** Economia de bateria em ecrãs OLED e menor cansaço visual para apps financeiras que são consultadas frequentemente.
- **Gráficos e Listas:** Usamos listas verticais simples para as transações para facilitar a leitura rápida, priorizando as mais recentes.
- **Base64 para Avatares:** Optamos por Base64 em vez de URLs de arquivos para simplificar o backend, evitando a necessidade de um servidor de storage complexo (como S3) nesta fase do projeto.

---

**Conclusão:** O FinFlow demonstra como uma aplicação moderna de Android pode ser robusta e ao mesmo tempo simples de manter, seguindo padrões de arquitetura limpos e focando na experiência do utilizador final.
