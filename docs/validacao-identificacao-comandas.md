# Identificação única entre comandas abertas

O campo usado para mesa/descrição é `identificacao`, e não `observacao`.
Antes desta alteração, `POST /api/comandas` salvava diretamente sem consulta nem
restrição de unicidade. O modelo atual não possui estabelecimento/tenant: a regra
vale no banco da instalação atual.

## Regra e concorrência

- O repositório compara `lower(btrim(identificacao))` somente para status `ABERTA`.
- O serviço consulta antes de salvar, mantém a capitalização para exibição e
  remove espaços das extremidades da identificação recebida.
- A V8 cria um índice único parcial sobre a mesma expressão. Assim, duas
  transações que passam pela consulta inicial não conseguem ambas confirmar.
- `saveAndFlush` permite capturar a violação do índice específico e retornar
  `409` com `mensagem: "Já existe uma comanda aberta com essa descrição."` no
  `ErroResponse` existente. Outras violações mantêm o tratamento anterior.
- O driver PostgreSQL já existente fica disponível na compilação para ler o
  nome estruturado da restrição e o SQLState, sem depender do idioma do banco.
- `FECHADA` e `CANCELADA` deixam a identificação disponível novamente.
- Não se alteram espaços internos, acentos ou a observação.

## Duplicidades existentes e implantação

Diagnóstico somente de leitura realizado em 08/10/2026 no banco configurado:
um grupo com identificação normalizada `mesa 1`, três comandas `ABERTA`,
IDs **1, 2 e 3**. Nenhum registro foi alterado ou excluído. Repita o diagnóstico
antes de implantar, pois os dados podem mudar.

Execute `src/main/resources/db/diagnostico/comandas_abertas_duplicadas.sql`
no banco da API para obter IDs e identificações originais. Essa consulta é
somente de leitura e não faz parte das migrations automáticas.

A V8 obtém um bloqueio de escrita na tabela dentro da transação do Flyway,
verifica duplicidades e cria o índice. Leituras continuam disponíveis. Havendo
duplicidade, a migração falha informando os IDs, sem modificar dados.

Revise as comandas conflitantes com o responsável pelo atendimento: identifique
se representam mesas diferentes ou comandas que deveriam ser encerradas.
Preserve o histórico e use fechamento/cancelamento apenas quando corresponder
ao estado real do atendimento. A correção não automatiza essa decisão.

Depois da revisão, atualize a API com o código e a nova migration. O Flyway
aplica a V8 na inicialização, conforme a configuração existente. Não altere as
migrations V1–V7. Com conflitos ainda existentes, a inicialização com V8 falha;
portanto, faça a revisão antes da atualização da API em uso.

O aplicativo também precisa receber a alteração em `NovaComandaScreen.tsx`
para mostrar o erro no pop-up. Identificação e observação permanecem preenchidas;
fechar o pop-up não navega nem tenta abrir novamente a comanda.

## Testes

Verificação em 08/10/2026: **83 testes da API passaram**, sem falhas ou testes
ignorados entre os selecionados. Comando executado numa cópia isolada do projeto:
`mvn -o '-Dtest=!ComandavisionApiApplicationTests' test`, com PostgreSQL 16 local.
O teste de contexto completo foi excluído para não inicializar migrations contra
o Supabase. O `npm run typecheck` do front-end também passou. Execução visual no
Android/iPhone continua pendente.

Os testes unitários de `ComandaServiceTest` verificam consulta preventiva,
identificação nova, tradução da restrição específica e preservação de outros erros.

`ComandaAberturaPostgresTest` usa PostgreSQL real, repositório JPA e serviço
transacional, controller e tratamento HTTP por MockMvc. Testa:

1. Identificação nova retorna 201.
2. Duplicidade aberta retorna 409 e a mensagem em português.
3. Maiúsculas/minúsculas e espaços nas extremidades são equivalentes.
4. Registros antigos com espaços também são considerados.
5. Fechamento pelo fluxo com itens e cancelamento permitem reutilizar.
6. Duas requisições passam pela consulta antes do INSERT: apenas uma retorna
   201, a outra retorna 409, e há uma única comanda persistida.
7. Inserção direta no banco também é bloqueada pelo índice.
8. A V8 relata duplicidades e preserva IDs, textos e estados originais.

Os testes de PostgreSQL exigem um banco **local e descartável** chamado
`comandavision_test`. Eles recriam o schema `comandavision` nesse banco.
Não usam `SUPABASE_DB_*`; sem `COMANDAVISION_TEST_DB_URL` são ignorados.
As migrations reais V1, V2, V3 e V7 preparam as tabelas do cenário; a V8 é
aplicada pelo próprio Flyway sobre esse schema de teste com baseline em V7;
as migrations de autenticação do Supabase não são necessárias a esse teste.

Em PowerShell, com Java 25 e esse PostgreSQL local disponível:

```powershell
$env:COMANDAVISION_TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:55432/comandavision_test'
$env:COMANDAVISION_TEST_DB_USER = 'comandavision_test'
$env:COMANDAVISION_TEST_DB_PASSWORD = '' # use a senha do seu banco de testes
.\mvnw.cmd '-Dtest=ComandaServiceTest,ComandaAberturaPostgresTest' test
```

O teste existente `ComandavisionApiApplicationTests` depende da configuração
completa da aplicação/Supabase. Não execute esse teste apontando ao banco em uso
para validar esta alteração, pois a inicialização aplica migrations.

## Verificação manual

Após atualizar a API e aplicar a V8, abra `Mesa 01` no aplicativo. Tente abrir
`mesa 01` ou `  MESA 01  ` em outro aparelho: deve aparecer o pop-up de erro,
mantendo identificação e observação. Feche-o, corrija a identificação e tente
novamente. Após fechar a comanda original pelo fluxo normal, abra `Mesa 01`
novamente. Teste também duas tentativas simultâneas em uma identificação nova.

## Arquivos

- `pom.xml` (escopo do driver PostgreSQL existente)
- `src/main/java/br/com/comandavision/api/comanda/ComandaRepository.java`
- `src/main/java/br/com/comandavision/api/comanda/ComandaService.java`
- `src/main/java/br/com/comandavision/api/comanda/ComandaController.java`
- `src/main/java/br/com/comandavision/api/comanda/ComandaIdentificacaoDuplicadaException.java`
- `src/main/resources/db/migration/V8__identificacao_unica_em_comandas_abertas.sql`
- `src/main/resources/db/diagnostico/comandas_abertas_duplicadas.sql`
- `src/test/java/br/com/comandavision/api/comanda/ComandaServiceTest.java`
- `src/test/java/br/com/comandavision/api/comanda/ComandaAberturaPostgresTest.java`
- `docs/validacao-identificacao-comandas.md`
- `PROXIMOS_PASSOS.md` (notas locais, já ignoradas pelo Git)
- Front-end: `src/screens/comandas/NovaComandaScreen.tsx`
