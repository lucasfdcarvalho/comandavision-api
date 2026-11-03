# ComandaVision API

API do projeto acadêmico ComandaVision. O serviço centraliza regras de comandas, cardápio, usuários, indicadores, pagamentos e Pix, com autenticação pelo Supabase e persistência em PostgreSQL.

> **Projeto acadêmico:** o ambiente pode conter dados simulados para demonstração. Eles não representam vendas reais verificadas.

O cliente móvel está em [comandavision-app](https://github.com/lucasfdcarvalho/comandavision-app).

## Recursos

- autenticação JWT emitida pelo Supabase;
- papéis `DONO` e `FUNCIONARIO`;
- categorias, produtos e imagens do cardápio;
- abertura, itens, fechamento, cancelamento e histórico de comandas;
- indicadores e histórico gerencial restritos ao proprietário;
- pagamentos manuais com correção auditada;
- cobranças Pix pelo Mercado Pago;
- idempotência, controle de concorrência e reconciliação de Pix;
- gestão da equipe pela Admin API do Supabase;
- OpenAPI/Swagger e endpoint de health check.

## Tecnologias

- Java 25;
- Spring Boot 4.1;
- Spring Security OAuth2 Resource Server;
- Spring Data JPA;
- PostgreSQL e Supabase;
- Flyway;
- springdoc OpenAPI;
- Maven Wrapper;
- Docker e Render.

As versões exatas estão em [`pom.xml`](./pom.xml).

## Autorização

| Recurso | `DONO` | `FUNCIONARIO` | Público |
| --- | :---: | :---: | :---: |
| `/api/dashboard/**` | Sim | Não | Não |
| `/api/usuarios/**` | Sim | Não | Não |
| Demais rotas `/api/**` | Sim | Sim | Não |
| Correção de pagamento manual | Sim | Sim | Não |
| `/health` e documentação OpenAPI | — | — | Sim |
| Webhook do Mercado Pago | — | — | Assinatura obrigatória |

Ocultar um botão no aplicativo não concede segurança. As permissões acima são aplicadas novamente pela API a partir do claim `user_role` do JWT.

## Pré-requisitos

- JDK 25;
- Docker, opcional para build e testes isolados;
- projeto Supabase separado para desenvolvimento;
- credenciais de teste do Mercado Pago, apenas se o fluxo Pix for exercitado.

As migrações usam recursos do Supabase, incluindo `auth.users` e papéis próprios da plataforma. Para desenvolvimento comum, prefira um projeto Supabase separado. Um PostgreSQL puro exige stubs equivalentes e deve ser usado somente em ambiente descartável.

## Variáveis de ambiente

| Variável | Obrigatória | Descrição |
| --- | :---: | --- |
| `SUPABASE_DB_URL` | Sim | URL JDBC do PostgreSQL |
| `SUPABASE_DB_USERNAME` | Sim | usuário do banco |
| `SUPABASE_DB_PASSWORD` | Sim | senha do banco |
| `SUPABASE_PROJECT_URL` | Sim | URL do projeto, usada para JWT e Admin API |
| `SUPABASE_SERVICE_ROLE_KEY` | Para gestão da equipe | chave administrativa do servidor |
| `MERCADOPAGO_ACCESS_TOKEN` | Para Pix integrado | token da conta ou da conta de teste |
| `MERCADOPAGO_MODO_TESTE` | Não | `true` ativa o pagador de teste; padrão `false` |
| `MERCADOPAGO_EMAIL_PAGADOR` | Para Pix fora do modo teste | e-mail do estabelecimento configurado no provedor |
| `MERCADOPAGO_WEBHOOK_SECRET` | Para webhook | segredo usado para validar notificações |
| `PORT` | Não | porta HTTP; padrão `8080` |

Exemplo apenas com placeholders:

```dotenv
SUPABASE_DB_URL=jdbc:postgresql://host:5432/postgres?sslmode=require
SUPABASE_DB_USERNAME=usuario_de_desenvolvimento
SUPABASE_DB_PASSWORD=senha_de_desenvolvimento
SUPABASE_PROJECT_URL=https://seu-projeto.supabase.co
SUPABASE_SERVICE_ROLE_KEY=chave_de_um_projeto_de_desenvolvimento

MERCADOPAGO_ACCESS_TOKEN=token_de_teste
MERCADOPAGO_MODO_TESTE=true
MERCADOPAGO_EMAIL_PAGADOR=conta-de-teste@example.com
MERCADOPAGO_WEBHOOK_SECRET=segredo_de_teste

PORT=8080
```

Nunca use valores de produção em testes, commits, issues ou logs. A `SUPABASE_SERVICE_ROLE_KEY` ignora RLS e deve existir somente no servidor e no gerenciador de secrets da hospedagem.

## Executar localmente

Configure as variáveis em um terminal e use o Maven Wrapper.

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Com Docker:

```bash
docker build -t comandavision-api .
docker run --rm -p 8080:8080 --env-file .env comandavision-api
```

Use esse `.env` apenas localmente e mantenha-o fora do Git.

## Documentação HTTP

Com a API em execução:

- health check: `GET /health`;
- OpenAPI JSON: `GET /v3/api-docs`;
- Swagger UI: `/swagger-ui.html`.

O health check confirma que o processo HTTP responde `OK`; ele não substitui uma verificação completa das integrações externas.

Todos os endpoints em `/api/**`, exceto os explicitamente públicos, exigem `Authorization: Bearer <jwt>`. O token deve ser emitido pelo mesmo projeto Supabase configurado em `SUPABASE_PROJECT_URL` e conter `user_role` igual a `DONO` ou `FUNCIONARIO`.

## Banco e migrações

O Flyway executa automaticamente ao iniciar a aplicação. As migrações existentes estão em `src/main/resources/db/migration` e cobrem as versões V1 a V11.

Regras para novas alterações:

1. nunca edite uma migração já aplicada;
2. crie a próxima versão, começando por V12;
3. teste a sequência completa em banco descartável;
4. faça backup lógico antes de uma migração publicada;
5. prefira uma migração corretiva posterior a restaurar o banco sobre dados novos.

V10 adiciona a chave de idempotência dos pagamentos. V11 registra motivo, usuário e data das correções manuais.

## Pagamentos e Pix

### Pagamentos manuais

Registros manuais podem ser corrigidos por `DONO` ou `FUNCIONARIO`. A operação exige motivo, preserva o pagamento e grava autoria e data; ela corrige o histórico do sistema e não representa devolução de dinheiro.

### Pix integrado

- a criação usa chave de idempotência para reduzir cobranças duplicadas;
- tentativas repetidas podem reutilizar uma cobrança pendente compatível;
- resultados incertos são reconciliados com o Mercado Pago;
- o webhook público só é aceito após validação da assinatura;
- um Pix vinculado a cobrança integrada não pode ser corrigido pela rota local de pagamento manual.

Devoluções de Pix integrado devem ocorrer no provedor e ser confirmadas antes de qualquer atualização correspondente no ComandaVision. Esses mecanismos reduzem riscos de repetição e concorrência, mas não constituem garantia financeira nem homologação do provedor.

## Testes

Execute a suíte apenas com configurações de desenvolvimento ou banco descartável:

```powershell
.\mvnw.cmd test
```

Os testes PostgreSQL de concorrência são habilitados somente quando `COMANDAVISION_TEST_DB_URL` aponta para `localhost` ou `127.0.0.1` e para o banco `comandavision_test`. Use também:

```text
COMANDAVISION_TEST_DB_URL
COMANDAVISION_TEST_DB_USERNAME
COMANDAVISION_TEST_DB_PASSWORD
```

O teste de contexto completo precisa dos papéis `anon`, `authenticated`, `supabase_auth_admin` e da tabela `auth.users`, existentes no Supabase. Em PostgreSQL puro, forneça somente stubs descartáveis; nunca aponte a suíte para produção.

## Estrutura principal

```text
src/main/java/br/com/comandavision/api/
├── autenticacao/
├── categoria/
├── comanda/
├── dashboard/
├── health/
├── pagamento/
├── pix/
├── produto/
├── security/
└── usuario/

src/main/resources/db/migration/  # migrações Flyway
src/test/java/                    # testes unitários, web e PostgreSQL
```

## Publicação

O serviço do projeto está conectado ao Render. Um push em `main` inicia um novo deploy e, durante a inicialização, o Flyway pode aplicar migrações no banco configurado no ambiente publicado.

Antes de atualizar `main`:

- revise o diff e as migrações;
- execute a suíte completa em banco descartável;
- confirme um backup válido;
- verifique a compatibilidade com a versão atual do aplicativo;
- não inclua secrets no Git.

Migrações aplicadas não possuem rollback automático. A ausência de erros no build não garante que Mercado Pago, Supabase Auth ou o aplicativo estejam configurados corretamente no ambiente publicado.
