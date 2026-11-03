package br.com.comandavision.api.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import br.com.comandavision.api.pagamento.dto.PagamentoResponse;
import br.com.comandavision.api.pagamento.dto.CorrigirPagamentoRequest;
import br.com.comandavision.api.pagamento.dto.RegistrarPagamentoRequest;
import br.com.comandavision.api.pix.CobrancaPixPendenteException;
import br.com.comandavision.api.pix.PixService;
import br.com.comandavision.api.pix.dto.CobrancaPixResponse;
import br.com.comandavision.api.pix.mercadopago.MercadoPagoClient;
import br.com.comandavision.api.pix.mercadopago.PedidoMercadoPago;
import jakarta.persistence.EntityManagerFactory;

// Nunca aceita Supabase nem outro host/banco. O schema é recriado apenas no banco local
// descartável chamado comandavision_test, normalmente fornecido por um contêiner temporário.
@EnabledIfEnvironmentVariable(
        named = "COMANDAVISION_TEST_DB_URL",
        matches = "jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/comandavision_test")
@SpringJUnitConfig(PagamentoConcorrenciaPostgresTest.Configuracao.class)
class PagamentoConcorrenciaPostgresTest {
    private static final Long COMANDA_ID = 1L;

    @Autowired
    DataSource dataSource;
    @Autowired
    PagamentoService pagamentoService;
    @Autowired
    PixService pixService;
    @Autowired
    MercadoPagoClient mercadoPagoClient;

    @BeforeEach
    void preparar() throws Exception {
        reset(mercadoPagoClient);
        executar("TRUNCATE comandavision.cobrancas_pix, comandavision.pagamentos, "
                + "comandavision.itens_comanda, comandavision.comandas, comandavision.produtos, "
                + "comandavision.categorias RESTART IDENTITY");
        executar("INSERT INTO comandavision.categorias (nome) VALUES ('Bebidas')");
        executar("INSERT INTO comandavision.produtos (categoria_id, nome, preco) VALUES (1, 'Suco', 100.00)");
        executar("INSERT INTO comandavision.comandas (identificacao, status, fechada_em) "
                + "VALUES ('Mesa PostgreSQL', 'FECHADA', CURRENT_TIMESTAMP)");
        executar("INSERT INTO comandavision.itens_comanda "
                + "(comanda_id, produto_id, quantidade, preco_unitario) VALUES (1, 1, 1, 100.00)");
    }

    @Test
    void migracaoCriaColunaEIndiceUnicoDeIdempotencia() throws Exception {
        assertEquals(1, consultarInteiro("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_schema = 'comandavision' AND table_name = 'pagamentos' "
                + "AND column_name = 'chave_idempotencia'"));
        assertEquals(1, consultarInteiro("SELECT count(*) FROM pg_indexes "
                + "WHERE schemaname = 'comandavision' AND tablename = 'pagamentos' "
                + "AND indexname = 'uk_pagamentos_chave_idempotencia'"));
        assertEquals(3, consultarInteiro("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_schema = 'comandavision' AND table_name = 'pagamentos' "
                + "AND column_name IN ('motivo_correcao', 'corrigido_por', 'corrigido_em')"));

        String chave = "60ad5bd8-f3a0-4f1c-941e-45b408c96530";
        executar("INSERT INTO comandavision.pagamentos "
                + "(comanda_id, forma, status, valor, pago_em, chave_idempotencia) "
                + "VALUES (1, 'DINHEIRO', 'CONFIRMADO', 10.00, CURRENT_TIMESTAMP, '" + chave + "')");
        SQLException erro = assertThrows(SQLException.class, () -> executar(
                "INSERT INTO comandavision.pagamentos "
                        + "(comanda_id, forma, status, valor, pago_em, chave_idempotencia) "
                        + "VALUES (1, 'DINHEIRO', 'CONFIRMADO', 10.00, CURRENT_TIMESTAMP, '" + chave + "')"));
        assertEquals("23505", erro.getSQLState());

        SQLException auditoriaIncompleta = assertThrows(SQLException.class, () -> executar(
                "INSERT INTO comandavision.pagamentos "
                        + "(comanda_id, forma, status, valor, pago_em, motivo_correcao) "
                        + "VALUES (1, 'DINHEIRO', 'ESTORNADO', 10.00, CURRENT_TIMESTAMP, 'Sem autoria')"));
        assertEquals("23514", auditoriaIncompleta.getSQLState());
    }

    @Test
    void correcaoManualPersisteMotivoUsuarioEDataSemExcluirPagamento() throws Exception {
        UUID usuarioId = UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f");
        PagamentoResponse registrado = pagamentoService.registrar(
                COMANDA_ID,
                new RegistrarPagamentoRequest(FormaPagamento.PIX, new BigDecimal("40.00"), "PIX-MANUAL"),
                "80629359-9ce0-4339-a095-4148d0db757e");

        pagamentoService.corrigirRegistro(
                COMANDA_ID,
                registrado.id(),
                new CorrigirPagamentoRequest("Pix manual registrado na comanda errada"),
                usuarioId);

        try (var conexao = dataSource.getConnection(); var comando = conexao.prepareStatement(
                "SELECT status, motivo_correcao, corrigido_por, corrigido_em "
                        + "FROM comandavision.pagamentos WHERE id = ?")) {
            comando.setLong(1, registrado.id());
            try (var dados = comando.executeQuery()) {
                assertTrue(dados.next());
                assertEquals("ESTORNADO", dados.getString("status"));
                assertEquals("Pix manual registrado na comanda errada", dados.getString("motivo_correcao"));
                assertEquals(usuarioId, dados.getObject("corrigido_por", UUID.class));
                assertTrue(dados.getObject("corrigido_em") != null);
            }
        }
        assertEquals(1, consultarInteiro("SELECT count(*) FROM comandavision.pagamentos"));
    }

    @Test
    void replayRetornaMesmoPagamentoSemDuplicarLinha() {
        String chave = "80629359-9ce0-4339-a095-4148d0db757e";
        RegistrarPagamentoRequest request = new RegistrarPagamentoRequest(
                FormaPagamento.DINHEIRO,
                new BigDecimal("40.00"),
                null);

        PagamentoResponse primeira = pagamentoService.registrar(COMANDA_ID, request, chave);
        PagamentoResponse repetida = pagamentoService.registrar(COMANDA_ID, request, chave);

        assertEquals(primeira.id(), repetida.id());
        assertEquals(1, consultarInteiroSemExcecao("SELECT count(*) FROM comandavision.pagamentos"));
    }

    @Test
    void clienteLegadoSemCabecalhoRecebeMesmoPagamentoNoRetry() {
        UUID usuarioId = UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f");
        RegistrarPagamentoRequest request = new RegistrarPagamentoRequest(
                FormaPagamento.DINHEIRO,
                new BigDecimal("40.00"),
                null);

        PagamentoResponse primeira = pagamentoService.registrar(COMANDA_ID, request, null, usuarioId);
        PagamentoResponse repetida = pagamentoService.registrar(COMANDA_ID, request, null, usuarioId);

        assertEquals(primeira.id(), repetida.id());
        assertEquals(1, consultarInteiroSemExcecao("SELECT count(*) FROM comandavision.pagamentos"));
    }

    @Test
    void retriesLegadosSimultaneosCriamUmaUnicaLinha() throws Exception {
        UUID usuarioId = UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f");
        RegistrarPagamentoRequest request = new RegistrarPagamentoRequest(
                FormaPagamento.DINHEIRO,
                new BigDecimal("40.00"),
                null);
        CyclicBarrier inicio = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> registrarLegadoDepoisDaBarreira(
                    inicio, request, usuarioId));
            var segunda = executor.submit(() -> registrarLegadoDepoisDaBarreira(
                    inicio, request, usuarioId));
            PagamentoResponse resposta1 = primeira.get(15, TimeUnit.SECONDS);
            PagamentoResponse resposta2 = segunda.get(15, TimeUnit.SECONDS);

            assertEquals(resposta1.id(), resposta2.id());
        }

        assertEquals(1, consultarInteiroSemExcecao("SELECT count(*) FROM comandavision.pagamentos"));
    }

    @Test
    void duasRequisicoesSimultaneasNaoUltrapassamSaldo() throws Exception {
        RegistrarPagamentoRequest request = new RegistrarPagamentoRequest(
                FormaPagamento.DINHEIRO,
                new BigDecimal("70.00"),
                null);
        CyclicBarrier inicio = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> registrarDepoisDaBarreira(
                    inicio, request, "d87ef5db-3c17-41ca-93a1-379bcd539f2d"));
            var segunda = executor.submit(() -> registrarDepoisDaBarreira(
                    inicio, request, "4f128afa-9634-442d-a269-274ec9a06572"));
            List<Object> resultados = List.of(
                    primeira.get(15, TimeUnit.SECONDS),
                    segunda.get(15, TimeUnit.SECONDS));

            assertEquals(1, resultados.stream().filter(PagamentoResponse.class::isInstance).count());
            assertEquals(1, resultados.stream()
                    .filter(ValorPagamentoExcedeSaldoException.class::isInstance).count());
        }

        assertEquals(1, consultarInteiroSemExcecao(
                "SELECT count(*) FROM comandavision.pagamentos WHERE status = 'CONFIRMADO'"));
        assertEquals(new BigDecimal("70.00"), consultarDecimal(
                "SELECT coalesce(sum(valor), 0) FROM comandavision.pagamentos WHERE status = 'CONFIRMADO'"));
    }

    @Test
    void pixConcorrentesCriamUmaUnicaCobrancaNoProvedorSimulado() throws Exception {
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        AtomicInteger sequencia = new AtomicInteger();
        when(mercadoPagoClient.criarPedidoPix(any(), anyString(), anyString(), anyString()))
                .thenAnswer(chamada -> pedido("ORD-PG-" + sequencia.incrementAndGet()));
        CyclicBarrier inicio = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> gerarPixDepoisDaBarreira(
                    inicio, "1c478b10-231d-4a7a-8286-462d97eeb8c1"));
            var segunda = executor.submit(() -> gerarPixDepoisDaBarreira(
                    inicio, "ccf97835-5baa-4aa8-ae49-0a48ee12a8b5"));
            List<Object> resultados = List.of(
                    primeira.get(15, TimeUnit.SECONDS),
                    segunda.get(15, TimeUnit.SECONDS));

            assertEquals(1, resultados.stream().filter(CobrancaPixResponse.class::isInstance).count());
            assertEquals(1, resultados.stream().filter(CobrancaPixPendenteException.class::isInstance).count());
        }

        verify(mercadoPagoClient, times(1)).criarPedidoPix(any(), anyString(), anyString(), anyString());
        assertEquals(1, consultarInteiroSemExcecao("SELECT count(*) FROM comandavision.cobrancas_pix"));
        assertEquals(1, consultarInteiroSemExcecao(
                "SELECT count(*) FROM comandavision.pagamentos WHERE status = 'PENDENTE'"));
    }

    @Test
    void pixLegadoRepetidoReaproveitaCobrancaPendente() {
        UUID usuarioId = UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f");
        when(mercadoPagoClient.estaConfigurado()).thenReturn(true);
        when(mercadoPagoClient.criarPedidoPix(any(), anyString(), anyString(), anyString()))
                .thenReturn(pedido("ORD-PG-LEGADO"));

        CobrancaPixResponse primeira = pixService.gerar(
                COMANDA_ID, new BigDecimal("40.00"), null, usuarioId);
        CobrancaPixResponse repetida = pixService.gerar(
                COMANDA_ID, new BigDecimal("40.00"), null, usuarioId);

        assertEquals(primeira.pagamentoId(), repetida.pagamentoId());
        verify(mercadoPagoClient, times(1)).criarPedidoPix(any(), anyString(), anyString(), anyString());
        assertEquals(1, consultarInteiroSemExcecao("SELECT count(*) FROM comandavision.cobrancas_pix"));
        assertEquals(1, consultarInteiroSemExcecao("SELECT count(*) FROM comandavision.pagamentos"));
    }

    private Object registrarDepoisDaBarreira(
            CyclicBarrier inicio,
            RegistrarPagamentoRequest request,
            String chave) throws Exception {
        inicio.await(5, TimeUnit.SECONDS);
        try {
            return pagamentoService.registrar(COMANDA_ID, request, chave);
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private PagamentoResponse registrarLegadoDepoisDaBarreira(
            CyclicBarrier inicio,
            RegistrarPagamentoRequest request,
            UUID usuarioId) throws Exception {
        inicio.await(5, TimeUnit.SECONDS);
        return pagamentoService.registrar(COMANDA_ID, request, null, usuarioId);
    }

    private Object gerarPixDepoisDaBarreira(CyclicBarrier inicio, String chave) throws Exception {
        inicio.await(5, TimeUnit.SECONDS);
        try {
            return pixService.gerar(COMANDA_ID, new BigDecimal("70.00"), chave);
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private static PedidoMercadoPago pedido(String id) {
        return new PedidoMercadoPago(id, "action_required", "waiting_transfer",
                new PedidoMercadoPago.Transacoes(List.of(new PedidoMercadoPago.PagamentoPedido(
                        "PAY-" + id,
                        "action_required",
                        new PedidoMercadoPago.MetodoPagamento("pix", "00020126580014br.gov.bcb.pix", "iVBORw0KGgo=")))));
    }

    private void executar(String sql) throws SQLException {
        try (var conexao = dataSource.getConnection(); var comando = conexao.createStatement()) {
            comando.execute(sql);
        }
    }

    private int consultarInteiro(String sql) throws SQLException {
        try (var conexao = dataSource.getConnection(); var comando = conexao.createStatement();
                var dados = comando.executeQuery(sql)) {
            assertTrue(dados.next());
            return dados.getInt(1);
        }
    }

    private int consultarInteiroSemExcecao(String sql) {
        try {
            return consultarInteiro(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private BigDecimal consultarDecimal(String sql) {
        try (var conexao = dataSource.getConnection(); var comando = conexao.createStatement();
                var dados = comando.executeQuery(sql)) {
            assertTrue(dados.next());
            return dados.getBigDecimal(1);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackages = {
            "br.com.comandavision.api.comanda",
            "br.com.comandavision.api.pagamento",
            "br.com.comandavision.api.pix"
    })
    @Import({ PagamentoService.class, PixService.class })
    static class Configuracao {
        @Bean
        DataSource dataSource() throws Exception {
            String url = System.getenv("COMANDAVISION_TEST_DB_URL");
            if (url == null || !url.matches(
                    "jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/comandavision_test")) {
                throw new IllegalArgumentException("Os testes exigem o banco local descartável comandavision_test");
            }
            var fonte = new DriverManagerDataSource(
                    url,
                    System.getenv().getOrDefault("COMANDAVISION_TEST_DB_USER", "comandavision_test"),
                    System.getenv().getOrDefault("COMANDAVISION_TEST_DB_PASSWORD", ""));
            try (var conexao = fonte.getConnection(); var comando = conexao.createStatement()) {
                conexao.setAutoCommit(false);
                comando.execute("DROP SCHEMA IF EXISTS comandavision CASCADE");
                comando.execute("DROP SCHEMA IF EXISTS auth CASCADE");
                comando.execute("DROP TABLE IF EXISTS public.flyway_schema_history");
                comando.execute("DROP FUNCTION IF EXISTS public.custom_access_token_hook(JSONB)");
                comando.execute("CREATE SCHEMA auth");
                comando.execute("CREATE TABLE auth.users (id UUID PRIMARY KEY)");
                comando.execute("DO $$ BEGIN "
                        + "IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN "
                        + "CREATE ROLE anon NOLOGIN; END IF; "
                        + "IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN "
                        + "CREATE ROLE authenticated NOLOGIN; END IF; "
                        + "IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'supabase_auth_admin') THEN "
                        + "CREATE ROLE supabase_auth_admin NOLOGIN; END IF; "
                        + "END $$");
                conexao.commit();
            }
            Flyway.configure()
                    .dataSource(fonte)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            return fonte;
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var fabrica = new LocalContainerEntityManagerFactoryBean();
            fabrica.setDataSource(dataSource);
            fabrica.setPackagesToScan("br.com.comandavision.api");
            fabrica.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            fabrica.setJpaPropertyMap(Map.of(
                    "hibernate.default_schema", "comandavision",
                    "hibernate.hbm2ddl.auto", "none"));
            return fabrica;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory fabrica) {
            return new JpaTransactionManager(fabrica);
        }

        @Bean
        MercadoPagoClient mercadoPagoClient() {
            return org.mockito.Mockito.mock(MercadoPagoClient.class);
        }
    }
}
