# FinFlow

App Android de finanças pessoais para gerir despesas, receitas e metas financeiras. Escrita em Kotlin com Jetpack Compose e ligada a uma API REST com autenticação JWT.

## Funcionalidades

- **Conta:** registo, início de sessão e recuperação de password por e-mail (a ligação do e-mail reabre a app através de `finflow://reset-password`).
- **Dashboard:** receitas, despesas e limite disponível dos cartões de crédito, com a opção de vincular novos cartões.
- **Movimentos:** adicionar despesas e receitas (categoria, data, descrição e valor), listar, pesquisar e eliminar; mostra o total gasto e a categoria com maior gasto.
- **Metas:** criar metas, atualizar o valor guardado, acompanhar o progresso, concluir ou eliminar.
- **Relatórios:** saldo líquido, total de entradas e saídas, distribuição de gastos por categoria, fluxo de caixa e filtro por período, com exportação para **PDF** e **Excel**.
- **Perfil:** editar o nome, alterar a password, escolher a foto de perfil e eliminar a conta.
- Tema escuro (Material 3).

## Tecnologias

| | |
|---|---|
| Linguagem | Kotlin 2.2 |
| Interface | Jetpack Compose, Material 3 (Compose BOM 2024.09), Navigation Compose |
| Rede | Retrofit, OkHttp (com logging), conversores Moshi/Gson, Coroutines |
| Build | Gradle 9.4, Android Gradle Plugin 9.2, Java 11 (source/target) |
| Android | `minSdk` 24 (Android 7.0), `targetSdk` 35 |

## Estrutura

```
app/src/main/java/com/example/
├── api/      # ApiService (endpoints Retrofit) e RetrofitClient (URL base)
├── model/    # modelos de pedidos e respostas
├── ui/       # ecrãs (Login, Registo, Dashboard, Metas, Relatórios, Perfil...) e diálogos
│   ├── dialogs/
│   └── theme/
└── utils/    # TokenManager, exportação para PDF e Excel, formatação de moeda
docs/         # documentação da API e relatório do projeto
```

O token JWT recebido no início de sessão é guardado localmente (`SharedPreferences`) e enviado em todos os pedidos autenticados.

## Backend e API

A app comunica com uma API REST alojada na Railway. O backend **não faz parte deste repositório**: aqui está apenas a app Android. A API está documentada em:

- [`docs/API_ENDPOINTS.md`](docs/API_ENDPOINTS.md): tabela de rotas, autenticação e exemplos.
- [`docs/finflow-api.yaml`](docs/finflow-api.yaml): especificação OpenAPI 3.

O URL base está em `app/src/main/java/com/example/api/RetrofitClient.kt` (`BASE_URL`). Para usar outro servidor (por exemplo um backend local), altera esse valor.

## Como executar

Requisitos: Android Studio recente (compatível com AGP 9.2) e JDK 17 ou superior.

1. Clona o repositório e abre a pasta no Android Studio.
2. Espera pela sincronização do Gradle.
3. Executa a app num emulador ou dispositivo com Android 7.0 (API 24) ou superior.

Para gerar o APK de debug na linha de comandos:

```bash
./gradlew assembleDebug
```

O ficheiro fica em `app/build/outputs/apk/debug/`.

## Documentação

O relatório do projeto está em [`docs/Relatorio.md`](docs/Relatorio.md).

## Autor

Marcos Canastro, [github.com/Katsuo666](https://github.com/Katsuo666) · [portfolio](https://marcoscanastro.github.io)
