# Finanças Modernas

Aplicativo Android nativo para gerenciar finanças pessoais com visual claro, moderno e focado em rapidez.

## O que já vem no projeto
- Tela **Lançamentos** com totais de receitas, despesas e saldo.
- Filtro por situação: **Todos**, **Receb./Pagos** e **A receber/A pagar**.
- FAB para abrir o formulário de lançamento.
- Cadastro com:
  - descrição
  - valor
  - data
  - tipo (receita ou despesa)
  - recorrência única, mensal ou parcelada
- Ações por item: **editar**, **excluir** e **marcar como pago/recebido**.
- Tela **Relatórios** com filtro por período e situação.
- Botão **Compartilhar PDF**.
- PDF compacto com totais logo abaixo dos itens.
- Armazenamento local offline em arquivo JSON interno.
- Ícone do app com visual de finanças.
- Workflow do **GitHub Actions** para gerar APK automaticamente.

## Decisões aplicadas no app
- **Parcelado**: o valor informado é o valor de **cada parcela**. Exemplo: valor 100 e 3 parcelas gera 3 lançamentos de 100 em meses consecutivos.
- **Mensal**: ao salvar, o app gera 12 lançamentos futuros com o mesmo valor.
- **Edição**: altera somente o item selecionado, não a série inteira.

## Stack usada
- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Geração nativa de PDF com `PdfDocument`

## Como abrir localmente
1. Abra a pasta do projeto no Android Studio.
2. Faça o sync do Gradle.
3. Rode em emulador ou aparelho Android.

## Como gerar APK pelo GitHub
1. Suba este projeto para um repositório no GitHub.
2. Vá na aba **Actions**.
3. Execute o workflow **Build APK** manualmente, ou faça um push para `main`/`master`.
4. Ao final da execução, baixe o artefato **app-debug-apk**.

O APK gerado fica no caminho:
- `app/build/outputs/apk/debug/app-debug.apk`

## Observação importante
Este projeto inclui workflow para gerar o APK no GitHub sem depender do `gradle-wrapper.jar`, usando Gradle 9.3.1 no próprio runner.

## Ajuste aplicado para build no GitHub
Como o projeto usa `org.jetbrains.kotlin.android` com AGP 9.1, foram adicionadas as flags temporárias abaixo em `gradle.properties` para desativar o built-in Kotlin e evitar a falha de build no CI:

- `android.builtInKotlin=false`
- `android.newDsl=false`

Isso mantém o projeto compilando com o plugin Kotlin atual.


## Observação de compatibilidade AGP 9

Este projeto usa o **built-in Kotlin** do AGP 9.x. Por isso:
- o plugin `org.jetbrains.kotlin.android` foi removido
- o antigo bloco `android { kotlinOptions { ... } }` não é usado

Se surgir erro relacionado a Kotlin no build, confira se o repositório está usando os arquivos mais recentes deste ZIP.
