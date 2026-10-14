# RELATÓRIO FINAL DE PROJETO
**Curso:** [Introduzir Nome do Curso]  
**Disciplina:** [Introduzir Nome da Disciplina]  
**Projeto:** FinFlow - Ecossistema de Gestão Financeira  
**Autor:** Marcos António Santos Canastro  
**Data:** 1 de julho de 2026  

---

## ÍNDICE
1. [Introdução e Enquadramento](#i-introdução-e-enquadramento)
2. [Objetivos do Projeto](#ii-objetivos-do-projeto)
3. [Metodologia de Desenvolvimento](#iii-metodologia-de-desenvolvimento)
4. [Infraestrutura e Arquitetura do Sistema](#iv-infraestrutura-e-arquitetura-do-sistema)
5. [Comunicação com a API e Segurança](#v-comunicação-com-a-api-e-segurança)
6. [Estratégias de Resiliência e Offline](#vi-estratégias-de-resiliência-e-offline)
7. [Análise Crítica: Desafios e Soluções](#vii-análise-crítica-desafios-e-soluções)
8. [Manual de Implementação (Técnico)](#viii-manual-de-implementação-técnico)
9. [Design e User Experience (UX)](#ix-design-e-user-experience-ux)
10. [Guia de Instalação e Utilização (Para Iniciantes)](#x-guia-de-instalação-e-utilização-para-iniciantes)
11. [Testes e Garantia de Qualidade](#xi-testes-e-garantia-de-qualidade)
12. [Conclusão](#xii-conclusão)
13. [Bibliografia e Webgrafia](#xiii-bibliografia-e-webgrafia)

---

## I. Introdução e Enquadramento
O projeto **FinFlow** surge no âmbito da unidade curricular de [Nome da Disciplina] como uma solução Android dedicada à literacia e gestão financeira pessoal. Num mundo digitalizado, a capacidade de monitorizar fluxos de caixa de forma centralizada é essencial. Este relatório descreve o desenvolvimento de uma aplicação móvel que integra tecnologias modernas como Jetpack Compose e Retrofit, conectando-se a um servidor remoto para garantir a persistência e segurança dos dados do utilizador.

---

## II. Objetivos do Projeto
O desenvolvimento do FinFlow foi orientado pelos seguintes objetivos académicos e técnicos:
*   **Gestão Dinâmica:** Permitir o registo e monitorização de receitas, despesas, metas de poupança e cartões de crédito.
*   **Integração de APIs:** Demonstrar competência no consumo de serviços RESTful com autenticação JWT.
*   **Experiência do Utilizador:** Implementar uma interface reativa, acessível e profissional seguindo as diretrizes do Material Design 3.
*   **Portabilidade de Dados:** Oferecer ferramentas de exportação local (Excel/PDF) para autonomia do utilizador.

---

## III. Metodologia de Desenvolvimento
Para a execução deste projeto, adotou-se uma **metodologia iterativa e incremental**. O desenvolvimento foi dividido em sprints lógicas (Infraestrutura, Segurança, UI, e Funcionalidades Extra), permitindo testes contínuos de integração com o backend Railway. O uso de ferramentas de Inteligência Artificial, como o **Google Stitch AI**, auxiliou na prototipagem rápida da interface, enquanto o sistema de controle de versões Git garantiu a integridade do código.

---

## IV. Infraestrutura e Arquitetura do Sistema

### 4.1 O Modelo Arquitetural (MVVM Reativo)
A aplicação adota o padrão **MVVM (Model-View-ViewModel)** adaptado para a natureza declarativa do **Jetpack Compose**.
*   **View (Composables):** Funções puras que reagem a mudanças de estado em tempo real, eliminando a verbosidade do XML tradicional.
*   **ViewModel:** Lógica de gestão de estado e ponte entre a interface e os serviços de rede.
*   **Model:** Data classes estruturadas que espelham rigorosamente os contratos da API (ex: `Expense`, `Goal`, `Card`).

### 4.2 Stack Tecnológica
*   **Linguagem:** Kotlin (Null Safety e Coroutines para operações assíncronas).
*   **UI Framework:** Jetpack Compose (Material 3).
*   **Networking:** Retrofit 2 + OkHttp 4 com Intercetores de segurança.
*   **Persistência Local:** SharedPreferences para gestão de tokens e perfis.

---

## V. Comunicação com a API e Segurança

### 5.1 Endpoints e Fluxo de Dados
A comunicação é centralizada no `RetrofitClient`, conectando-se ao servidor Railway.
*   **Autenticação:** Fluxo robusto de Login e Registo com proteção de rotas via JWT.
*   **Dashboard Aggregation:** Uso de endpoints agregadores para reduzir a latência inicial da aplicação.

### 5.2 Protocolos de Segurança
Implementou-se um sistema de interceção automática que injeta o token de autorização apenas em rotas protegidas, garantindo que credenciais sensíveis nunca sejam enviadas de forma insegura. O processo de logout garante a limpeza total da cache e da memória RAM do dispositivo.

---

## VI. Estratégias de Resiliência e Offline

### 6.1 Persistência de Identidade
A aplicação guarda localmente metadados essenciais (nome, email e avatar) para garantir que a interface não pareça "quebrada" em caso de perda de conexão.

### 6.2 Cache de Imagens (Base64)
Fotos de perfil são convertidas em strings Base64 e armazenadas localmente, permitindo o carregamento instantâneo do avatar do utilizador mesmo sem acesso à internet.

### 6.3 Exportação Local (Relatórios)
A funcionalidade de exportação CSV/Excel opera de forma autónoma, utilizando os dados em memória para gerar ficheiros no diretório de cache privado, garantindo a portabilidade dos dados financeiros.

---

## VII. Análise Crítica: Desafios e Soluções

Durante o ciclo de vida do projeto, superaram-se obstáculos técnicos significativos:

### 7.1 Gestão de Memória (OutOfMemoryError)
*   **Desafio:** Imagens de alta resolução causavam instabilidade na conversão para Base64.
*   **Solução:** Implementação de um pipeline de compressão JPEG e redimensionamento automático (300x300 pixels).

### 7.2 Correção de Erros HTTP (400, 403 e 404)
*   **Erro 400:** Corrigido através da normalização do `Locale.US` para garantir que valores decimais batessem com o padrão do servidor.
*   **Erro 403:** Identificado como lixo de caracteres (aspas) nos tokens salvos; resolvido com um filtro de limpeza no `TokenManager`.
*   **Erro 404:** Resolvido com a centralização e sincronização da URL base do servidor.

### 7.3 Resolvendo o Erro de Referência `profileBitmap`
Um erro crítico de compilação foi resolvido através da implementação do padrão `remember` com chave de refresh, garantindo que a UI se atualize sincronizadamente com o estado global da aplicação.

---

## VIII. Manual de Implementação (Técnico)

1.  **Fundação:** Configuração do ambiente Android Studio e dependências Gradle.
2.  **Modelação:** Criação do domínio de dados (Models) compatíveis com JSON.
3.  **Conetividade:** Implementação do Singleton `RetrofitClient` e interface `ApiService`.
4.  **Sessão:** Desenvolvimento do `TokenManager` para persistência de segurança.
5.  **Interface Base:** Criação das Activities de Login e Registo.
6.  **Painel Central:** Construção do Dashboard Reativo com `LaunchedEffect`.
7.  **Listagem:** Implementação de `LazyColumn` para histórico de transações.
8.  **Media:** Integração de seletores de galeria e processamento de imagem.
9.  **Interação:** Desenvolvimento de Diálogos para CRUD de Metas e Cartões.
10. **Refinamento:** Implementação da lógica de "Plano B" para falhas de rede.

---

## IX. Design e User Experience (UX)

### 9.1 Dark Mode Nativo
A escolha por um tema escuro visa reduzir a fadiga visual e otimizar o consumo energético em ecrãs OLED, transmitindo uma estética de profissionalismo financeiro.

### 9.2 Prototipagem com Stich AI
O design visual foi acelerado pelo **Google Stich AI**, que permitiu testar a hierarquia visual do Dashboard antes da codificação final, garantindo que elementos críticos como o "Saldo Total" tivessem a proeminência correta.

---

## X. Guia de Instalação e Utilização (Para Iniciantes)

### 10.1 Preparação do Ambiente
1.  Instalação do **Android Studio Koala**.
2.  Configuração do **Java JDK 17**.
3.  Acesso à Internet para Sincronização Gradle.

### 10.2 Montagem do Projeto
1.  **Importação:** No Android Studio, selecione `File > Open` e navegue até ao projeto.
2.  **Bibliotecas e Dependências:** Verifique no `build.gradle.kts` a presença das bibliotecas base:
    *   **Retrofit & OkHttp:** Para consumo da API REST.
    *   **Jetpack Compose:** Framework de UI.
    *   **Gson:** Serialização de dados.
    *   **Coil:** Processamento de imagens.
3.  **Sincronização:** Aguardar o `Gradle Sync` (necessário acesso à internet).
4.  **Execução:** Executar em dispositivo físico ou Emulador (API 24+).

### 10.3 Fluxo de Utilização
1.  **Acesso:** Login ou Registo de conta.
2.  **Dashboard:** Visualização de saldos e transações rápidas.
3.  **Metas:** Definição de objetivos de poupança.
4.  **Relatórios:** Exportação de dados para PDF.

---

## XI. Testes e Garantia de Qualidade
Para garantir a estabilidade do ecossistema FinFlow, foram realizados os seguintes procedimentos:
*   **Testes de Interface (UI):** Validação manual de todos os fluxos de navegação em diferentes tamanhos de ecrã.
*   **Testes de Integração:** Verificação da comunicação entre a app e a API Railway usando o `HttpLoggingInterceptor`.
*   **Testes de Stress de Memória:** Carregamento de múltiplas imagens de alta resolução para validar o pipeline de compressão.
*   **Validação de Regras de Negócio:** Teste exaustivo dos cálculos de saldo e percentagens no Dashboard.

---

## XII. Conclusão
O **FinFlow** demonstra o equilíbrio entre robustez técnica e simplicidade operacional. A superação dos desafios de integração de rede e gestão de memória resultou numa aplicação escalável e segura. O projeto cumpre integralmente os requisitos enunciados, oferecendo uma base sólida para futuras expansões no mercado de Fintech.

---

## XIII. Bibliografia e Webgrafia
*   **Android Developers:** Documentação oficial do Jetpack Compose e Material 3. [https://developer.android.com](https://developer.android.com)
*   **Retrofit:** Documentação oficial da biblioteca de rede Square Open Source. [https://square.github.io/retrofit/](https://square.github.io/retrofit/)
*   **Kotlin Coroutines:** Guia oficial de programação assíncrona. [https://kotlinlang.org](https://kotlinlang.org)
*   **Material Design 3:** Diretrizes de design da Google. [https://m3.material.io](https://m3.material.io)
*   **Railway App:** Documentação de infraestrutura de backend. [https://docs.railway.app](https://docs.railway.app)

---
**Documentação Final - FinFlow Team**
