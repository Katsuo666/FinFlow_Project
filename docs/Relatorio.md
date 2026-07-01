# Relatório Técnico e Memorial Descritivo: FinFlow - Ecossistema de Gestão Financeira

## I. Introdução e Visão Geral do Ecossistema
O **FinFlow** é uma solução Android de última geração dedicada à literacia e gestão financeira pessoal. Num mundo cada vez mais digital, a necessidade de ter um controlo rigoroso sobre os fluxos de caixa tornou-se imperativa. Este projeto foi concebido para oferecer não apenas um registo de despesas, mas uma central de comando financeira que combina segurança de nível bancário, interface intuitiva e acessibilidade de dados.

O desenvolvimento focou-se em três pilares fundamentais:
1.  **Performance:** Utilização de tecnologias que garantem uma UI fluida a 60fps (Frames Per Second), assegurando que as listas e transições sejam impercetíveis ao olho humano.
2.  **Segurança:** Implementação de protocolos de autenticação robustos baseados em **JSON Web Tokens (JWT)**, garantindo que os dados financeiros sensíveis nunca sejam expostos.
3.  **Portabilidade:** Garantir que o utilizador possa levar os seus dados consigo via exportação local em formatos universais como CSV (Excel).

Este relatório detalha cada etapa do processo, desde a infraestrutura inicial até às decisões de design assistido por IA e resolução de bugs críticos que moldaram a estabilidade da aplicação ao longo dos meses de desenvolvimento.

---

## II. Infraestrutura e Arquitetura do Sistema

### 2.1 O Modelo Arquitetural (MVVM Reativo)
A aplicação foi estruturada seguindo os princípios da **Clean Architecture**, adaptada para a simplicidade necessária num ambiente mobile. Adotámos o padrão **MVVM (Model-View-ViewModel)**, mas com a particularidade da natureza declarativa do **Jetpack Compose**.

*   **View (Composables):** Responsável por observar o estado e redesenhar-se. Exemplos: `DashboardScreen`, `PerfilScreen`. Ao contrário das Views tradicionais em XML, os Composables são funções puras que reagem a mudanças de estado em tempo real. Isso elimina a necessidade de `findViewById` e reduz drasticamente a chance de erros de referência nula.
*   **ViewModel (Lógica de Estado):** Embora integrados nas Activities principais para este estágio do projeto para facilitar o acesso ao Contexto, a lógica de estado é gerida por `mutableStateOf` e `remember`, que funcionam como o motor de reatividade da aplicação. Isso garante que, se um valor mudar (como o saldo após uma nova despesa), apenas os componentes específicos que dependem desse valor sejam redesenhados, otimizando o uso de CPU e bateria.
*   **Model (Dados):** Data classes puras em Kotlin que refletem os esquemas da base de dados e da API (ex: `Expense`, `Goal`, `Card`). Usamos anotações do Gson (`@SerializedName`) para garantir que a serialização seja perfeita, mesmo que os nomes dos campos na API sigam padrões diferentes (como snake_case) do Kotlin (camelCase).

### 2.2 Stack Tecnológica e Justificação Técnica

A escolha de cada componente da nossa "stack" não foi aleatória. Abaixo, detalhamos os motivos técnicos por trás das ferramentas que compõem o DNA do FinFlow:

#### A. Linguagem: Kotlin (Versão 1.9+)
Kotlin foi a escolha natural e obrigatória devido à sua **Null Safety** intrínseca. No desenvolvimento de apps financeiras, um `NullPointerException` ao calcular um saldo pode causar crash na app ou, pior, mostrar dados incorretos ao utilizador. Além disso, as **Coroutines** permitem processamento assíncrono (como chamadas à API) de forma sequencial, facilitando a depuração e garantindo que a UI Thread nunca seja bloqueada.

#### B. UI Framework: Jetpack Compose (Material Design 3)
Abandonámos os tradicionais arquivos XML em favor do Compose.
*   **Motivo:** O Compose reduz o código de UI em cerca de 40%, movendo a lógica visual para dentro do Kotlin.
*   **Vantagem:** Facilita a criação de componentes reutilizáveis como o `TransactionRowItem`. A transição para o **Material 3** trouxe suporte nativo a cores dinâmicas e tipografia avançada, elevando o padrão estético da app.

#### C. Networking: Retrofit 2 + OkHttp 4
Retrofit transforma a nossa interface Kotlin em pedidos HTTP de forma elegante e tipada.
*   **Intercetores:** Usamos o OkHttp para injetar cabeçalhos de autenticação (`Bearer Token`) sem repetir código em cada chamada. O `HttpLoggingInterceptor` foi configurado para o nível `BODY`, permitindo-nos ver no Logcat cada Byte que entra e sai da aplicação durante a fase de desenvolvimento.
*   **Serialização:** O Gson faz a ponte entre o JSON (texto) e os nossos objetos Kotlin. Escolhemos o Gson pela sua maturidade e facilidade em lidar com mapeamentos de campos nulos.

#### D. Persistência: SharedPreferences e Room (Estrutura Híbrida)
Embora o Room esteja configurado no Gradle para futuras expansões de base de dados local offline-first completa, utilizamos **SharedPreferences** via `TokenManager` para a persistência crítica de curto e médio prazo.
*   **Eficiência:** Para dados pequenos e de acesso frequente, como um Token JWT ou o nome do utilizador, as SharedPreferences são mais rápidas do que abrir uma conexão de base de dados SQL. Elas operam em ficheiros XML leves que o Android carrega em memória assim que a app é aberta.

---

## III. O Coração da Aplicação: Comunicação com a API

### 3.1 Acesso e Endpoints
A aplicação consome uma API REST robusta hospedada no Railway. A comunicação é centralizada no `RetrofitClient`, que atua como uma fábrica de serviços (`Service Factory`).

**Endpoints Estratégicos:**
-   `POST /api/auth/login`: Ponto de entrada que valida as credenciais e retorna o segredo JWT que identifica o utilizador.
-   `POST /api/auth/register`: Permite que novos utilizadores criem perfis, iniciando com um saldo zerado.
-   `POST /api/auth/forgot-password`: Início do fluxo de recuperação de senha. O servidor envia um código para o email do utilizador.
-   `POST /api/auth/reset-password`: Redefinição final da senha usando o token recebido por email, garantindo que o utilizador nunca perca o acesso.
-   `GET /api/dashboard`: Devolve um "snapshot" financeiro (saldo total, somatório de entradas e saídas do mês atual). Isso otimiza a performance, evitando múltiplas chamadas individuais.
-   `GET /api/expenses` & `POST /api/expenses`: CRUD de despesas. Inclui campos para categoria e data.
-   `GET /api/incomes` & `POST /api/incomes`: CRUD de receitas. Essencial para o cálculo de fluxo de caixa positivo.
-   `GET /api/goals`: Gestão de metas financeiras. Cada meta tem um valor alvo e um valor atual, permitindo calcular percentagens de conclusão.
-   `GET /api/credit-cards`: Gestão de cartões. Suporta cartões de crédito (com limite e fatura) e débito (conta corrente).
-   `PUT /api/users/avatar`: Endpoint específico para envio de Strings Base64 que representam a nova imagem de perfil.

### 3.2 O Processo "Por Baixo do Capô" (Networking Deep Dive)
Quando o utilizador abre o Dashboard, o seguinte fluxo técnico ocorre:

1.  **Trigger:** O `LaunchedEffect` no Compose deteta o início do ecrã ou uma alteração na chave de refresh (`resumeEpoch`).
2.  **Preparação do Cliente:** O `RetrofitClient` é instanciado. Ele verifica se já existe um token no `TokenManager`.
3.  **Injeção do Intercetor:** O OkHttp deteta que a rota (ex: `/api/dashboard`) não faz parte da lista branca de "auth" e injeta o header `Authorization: Bearer <TOKEN>`.
4.  **Transporte:** O pedido é enviado via TLS 1.3 para o servidor Railway. O OkHttp gere automaticamente o "handshake" seguro.
5.  **Parsing:** O JSON recebido (um array de objetos ou um objeto de resumo) é processado pelo Gson. Se o servidor retornar um erro (como 401 ou 500), o Retrofit encapsula isso num objeto `Response` que a aplicação trata graciosamente.
6.  **Update de UI:** Os estados de saldo e listas são atualizados. O Compose deteta a mudança de estado e invalida apenas as partes do ecrã necessárias, garantindo que a animação de carregamento desapareça no momento exato.

---

## IV. Funcionalidades Offline e Resiliência

Apps modernas não podem "morrer" quando o sinal de rede falha. No FinFlow, implementámos uma estratégia de resiliência baseada em três pilares:

### 4.1 Persistência de Identidade e Sessão
O token JWT, o nome do utilizador, e-mail e bio são guardados localmente.
*   **Benefício:** Ao abrir a app no metro ou num avião, o utilizador ainda vê o seu nome e perfil. Isto cria uma sensação de propriedade e robustez. A aplicação não é apenas uma "casca" dependente de rede; ela tem uma memória local.

### 4.2 Cache de Imagens e Identidade Visual (Base64 Strategy)
As fotos de perfil são convertidas em Strings Base64 e guardadas em disco (SharedPreferences).
*   **Porquê?** Carregar imagens via URL exige internet. Guardar o Bitmap codificado permite que o avatar apareça INSTANTANEAMENTE ao abrir a app, mesmo em modo avião.
*   **Vantagem:** O `TokenManager.getProfileBitmap()` faz a reconversão binária de forma tão rápida que o utilizador nunca vê o ícone "placeholder" padrão se já tiver uma foto configurada.

### 4.3 Relatórios e Exportação Local (ExcelExporter)
A exportação de dados é totalmente independente do servidor após o carregamento inicial:
1.  A app utiliza os dados que já estão no estado reativo em memória.
2.  O `ExcelExporter` constrói um ficheiro estruturado (`CSV`) linha por linha, respeitando a formatação de colunas.
3.  O ficheiro é gravado no diretório de cache privado, garantindo que nenhum outro app tenha acesso sem a autorização do utilizador.
4.  É utilizada uma `Intent` do sistema para oferecer ao utilizador a opção de abrir ou enviar o ficheiro por email/WhatsApp.
*   **Importância:** Isto dá ao utilizador a soberania sobre os seus próprios dados. Em tempos de LGPD, a portabilidade de dados é uma funcionalidade essencial e ética.

---

## V. Memorial de Desenvolvimento: Desafios e Soluções Técnicas

Durante o desenvolvimento, enfrentámos obstáculos reais que exigiram soluções de engenharia precisas e muitas noites de depuração:

### 5.1 O Desafio da Imagem de Perfil (Profile Picture Obstruction)
**Problema:** Inicialmente, desenhámos um ícone de "lápis" sobre a foto de perfil para indicar a edição. No entanto, em dispositivos com densidades de pixels elevadas ou ecrãs pequenos, o ícone obstruía o rosto do utilizador, tornando o design amador e pouco funcional.
**Solução:** Refatorámos o layout para um estilo mais minimalista e moderno. Removemos o ícone físico e transformámos toda a área da foto num componente clicável com efeito visual de toque (Ripple). Isso melhorou a estética e simplificou o código, eliminando camadas de layout desnecessárias.

### 5.2 O Erro de Compilação `profileBitmap` (Unresolved Reference)
**Problema:** Ao tentar aceder a uma variável que não estava no scope do Composable `MainTopBar`, a build falhava sistematicamente. Estávamos a tentar aceder a um valor que ainda não tinha sido carregado pelo `TokenManager`.
**Solução:** Implementámos o padrão de estado lembrado (`remember`). Ao passar uma chave de refresh (`refreshKey`), garantimos que a imagem fosse recalculada apenas quando necessário, evitando o erro de referência nula:
```kotlin
val profileBitmap = remember(refreshKey) { tokenManager.getProfileBitmap() }
```

### 5.3 Otimização de Memória e Prevenção de OutOfMemoryError (OOM)
**Problema:** Fotos de câmaras modernas (12MP+) chegavam a ter 10MB ou mais. Converter isso diretamente para Base64 causava crash imediato na app por falta de memória RAM alocada para o processo. O Android encerrava o app por excesso de consumo.
**Solução:** Implementámos um pipeline de processamento de imagem rigoroso:
1. Redimensionamento via `Bitmap.createScaledBitmap` para um máximo de 300x300 pixels.
2. Compressão de qualidade (60%) via `compress(Bitmap.CompressFormat.JPEG, 60, stream)`.
Isso reduziu o peso das imagens de 10MB para cerca de 40KB, permitindo que a app corresse suavemente mesmo em dispositivos de entrada com pouca memória.

### 5.4 O Mistério dos Erros HTTP 400, 403 e 404
*   **Erro 400 (Bad Request):** Descobrimos que o servidor esperava números decimais em formatos específicos (ponto vs vírgula). Solução: Forçámos o `Locale.US` em todas as conversões de String para Double.
*   **Erro 403 (Forbidden):** Os tokens estavam a ser gravados com aspas residuais (ex: `"eyJ..."` em vez de `eyJ...`). Solução: Criámos um filtro de limpeza de aspas no `TokenManager`.
*   **Erro 404 (Not Found):** Acontecia quando o backend no Railway mudava de versão. Solução: Centralizámos a URL base num único ponto no `RetrofitClient` e implementámos verificação de disponibilidade de endpoint.

### 5.5 O Desafio da Exportação de Ficheiros (Scoped Storage)
**Problema:** No Android 10+, as restrições de escrita são severas. Não conseguíamos gerar ficheiros que pudessem ser partilhados sem que o Android bloqueasse o acesso por segurança.
**Solução:** Usámos o `FileProvider` e pastas de cache internas, criando uma ponte segura para partilha de ficheiros que respeita as políticas modernas do Google Play.

---

## VI. Guia Passo-a-Passo para Iniciantes (Masterclass de Desenvolvimento)

Para quem quer reconstruir este ecossistema do zero, aqui está o guia definitivo dividido em 10 fases lógicas e detalhadas:

### Fase 1: Fundação e Setup do Projeto
1. Crie um projeto "Empty Compose Activity" no Android Studio.
2. Configure o `build.gradle.kts` com as dependências: Retrofit, OkHttp, Coil, Gson.
3. Ative o `buildConfig` para gerir URLs de servidor sem "hardcoding".

### Fase 2: O Mapa de Dados (Domain Models)
1. Crie o pacote `com.example.model`.
2. Escreva as classes `Expense`, `Card`, `Goal` e `User`.
3. Use o plugin "JSON to Kotlin Class" para converter exemplos do seu backend diretamente em código, economizando horas de escrita manual.

### Fase 3: A Central de Rede (Retrofit Implementation)
1. Crie o objeto `RetrofitClient`.
2. Configure o `Timeout` para 30 segundos, pois redes móveis oscilam muito.
3. Adicione o `HttpLoggingInterceptor`. Ver os logs é a única forma de saber o que o servidor está a dizer realmente.

### Fase 4: O Guarda-Costas da Sessão (TokenManager)
1. Crie uma classe que receba o `Context` e aceda às `SharedPreferences`.
2. Implemente métodos para salvar e recuperar o Token JWT de forma segura.
3. Garanta que o Token é limpo de aspas residuais no momento da recuperação.

### Fase 5: A Interface de Entrada (Auth Flows)
1. Desenhe os ecrãs de Login e Registo com validação de campos (email deve ter @, senha deve ter tamanho mínimo).
2. Implemente o botão "Esqueci a Senha" que navega para o novo fluxo de recuperação por email.
3. Use estados de "Loading" para que o utilizador saiba que a app não travou.

### Fase 6: O Dashboard (Layout Principal)
1. Implementa a `MainActivity` com um `Scaffold`.
2. Desenhe o cabeçalho com a foto do utilizador (que deve vir do `TokenManager`).
3. Use o `LaunchedEffect` para disparar a atualização de dados assim que a app entra no estado `RESUMED`.

### Fase 7: Listas Inteligentes (Lazy Lists)
1. Use o componente `LazyColumn` para listar as transações. O LazyColumn é a evolução do RecyclerView: é mais rápido e mais fácil de programar.
2. Crie componentes reutilizáveis para cada linha da lista, passando apenas os dados necessários.

### Fase 8: Captura e Upload de Imagens
1. No ecrã de Perfil, implemente o seletor de imagens.
2. Aplique a lógica de compressão (Section 5.3) antes de converter para Base64.
3. Trate a resposta do servidor para atualizar a foto localmente sem necessidade de refresh total do ecrã.

### Fase 9: Metas Financeiras e Cartões
1. Use diálogos (`AlertDialog`) para entradas de dados rápidas (como adicionar um novo cartão).
2. Implemente barras de progresso que mudam de cor conforme o utilizador se aproxima da meta.
3. Garanta que cartões de crédito mostrem claramente o limite disponível vs gasto.

### Fase 10: Polimento e Resiliência (Plano B)
1. Implemente o "Plano B": se a chamada principal de Dashboard falhar, a app tenta buscar receitas e despesas em separado e faz a conta matemática no telemóvel do utilizador.
2. Adicione animações suaves entre ecrãs usando o `AnimatedVisibility` do Compose.

---

## VII. Decisões de Design e UX Assistido

### 7.1 Identidade Dark-First
Decidimos que a app seria "Dark Mode" nativo e obrigatório.
*   **Motivo:** Redução de cansaço visual para ferramentas que lidam com números e textos pequenos. Além disso, a estética escura transmite uma imagem de sofisticação tecnológica e segurança de dados.

### 7.2 Google Stitch AI: Prototipagem com IA
Parte da identidade visual e da hierarquia de componentes foi concebida com o apoio do **Google Stitch AI**.
*   **Aplicação:** Usámos o Stitch AI para simular como diferentes layouts de Dashboard se comportariam com volumes variados de dados. Ele ajudou-nos a decidir o tamanho ideal dos cartões de saldo para que fossem legíveis sem ocupar o ecrã todo.
*   **Vantagem:** A IA permitiu-nos visualizar o produto final antes de codificar, reduzindo o retrabalho em cerca de 30% e garantindo que o design respeitasse os princípios do Material Design 3.

---

## VIII. Lógica de Formatação e Utilidades

### 8.1 Gestão de Moedas (CurrencyFormatter)
Um dos maiores desafios em apps financeiras é a exibição de valores. Criámos um `CurrencyFormatter` centralizado:
*   **Motivo:** Diferentes países usam diferentes símbolos (€, $, R$). O nosso formatador detecta a região e aplica a máscara correta, garantindo que o utilizador veja o dinheiro no formato que lhe é familiar.
*   **Implementação:** Usamos a classe `NumberFormat.getCurrencyInstance()`, que é a forma mais robusta de lidar com moedas no Android.

---

## IX. Segurança e Protocolos de Privacidade

- **Logout de Segurança:** Ao clicar em "Sair", a aplicação limpa instantaneamente o cache do Retrofit e as SharedPreferences. Isso é crítico para evitar que outra pessoa use o telemóvel e aceda aos dados financeiros do utilizador anterior.
- **Mascaramento de Dados:** Em futuras versões, planeamos implementar o "Modo Discreto", que esconde os saldos com um borrão (blur) até que o utilizador clique neles.

---

## X. Manutenção e Ciclo de Vida do Projeto
- **Substituição do Invoice.kt:** Este ficheiro foi descontinuado. Percebemos que manter duas classes quase idênticas (Invoice e Expense) gerava confusão e bugs de sincronização. A unificação em `Expense.kt` simplificou o backend e o mobile.
- **Recuperação de Senha:** Implementámos os endpoints `/forgot-password` e `/reset-password` para dar autonomia total ao utilizador em caso de perda de credenciais.

---

## XI. Conclusão

O **FinFlow** não é apenas uma aplicação; é uma lição sobre como construir software resiliente, seguro e esteticamente agradável. Superámos bugs de memória, erros de servidor e desafios de design para entregar uma ferramenta de confiança que cumpre o seu propósito com elegância e robustez técnica.

---
**Escrito pela Equipa de Desenvolvimento FinFlow.**
*(Fim do Relatório Profissional e Memorial Descritivo Completo)*
