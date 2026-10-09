package br.com.comandavision.api.comanda;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import jakarta.persistence.EntityManagerFactory;
import br.com.comandavision.api.exception.GlobalExceptionHandler;

// Não usa credenciais do Supabase nem inicia a aplicação contra um banco real.
// Execute somente em um PostgreSQL local descartável chamado comandavision_test.
@EnabledIfEnvironmentVariable(named = "COMANDAVISION_TEST_DB_URL",
        matches = "jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/comandavision_test")
@SpringJUnitConfig(ComandaAberturaPostgresTest.Configuracao.class)
class ComandaAberturaPostgresTest {
    @Autowired DataSource dataSource;
    @Autowired ComandaService service;
    @MockitoSpyBean ComandaRepository repository;
    MockMvc mvc;

    @BeforeEach
    void preparar() throws Exception {
        executar("TRUNCATE comandavision.itens_comanda, comandavision.comandas, comandavision.produtos, comandavision.categorias RESTART IDENTITY");
        mvc = MockMvcBuilders.standaloneSetup(new ComandaController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void permiteDescricaoNova() throws Exception {
        abrir("Mesa 01").andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificacao").value("Mesa 01"))
                .andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    void bloqueiaDescricaoIgualEnquantoAbertaComMensagem409() throws Exception {
        abrir("Mesa 01").andExpect(status().isCreated());
        abrir("Mesa 01").andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value("Já existe uma comanda aberta com essa descrição."));
        assertEquals(1, quantidadeComandas());
    }

    @Test
    void ignoraMaiusculasMinusculasEEspacosNasExtremidades() throws Exception {
        abrir("Mesa 01").andExpect(status().isCreated());
        for (String descricao : List.of("mesa 01", "MESA 01", "  MeSa 01  ")) {
            abrir(descricao).andExpect(status().isConflict());
        }
        assertEquals(1, quantidadeComandas());
    }

    @Test
    void comparaTambemRegistrosAntigosComEspacos() throws Exception {
        executar("INSERT INTO comandavision.comandas (identificacao) VALUES ('  Mesa 01  ')");
        abrir("mesa 01").andExpect(status().isConflict());
    }

    @Test
    void permiteReutilizarDepoisDeFecharComandaComItens() throws Exception {
        abrir("Mesa 01").andExpect(status().isCreated());
        executar("INSERT INTO comandavision.categorias (nome) VALUES ('Bebidas')");
        executar("INSERT INTO comandavision.produtos (categoria_id, nome, preco) VALUES (1, 'Água', 5)");
        mvc.perform(post("/api/comandas/1/itens").contentType(MediaType.APPLICATION_JSON)
                .content("{\"produtoId\":1,\"quantidade\":1}"))
                .andExpect(status().isCreated());
        mvc.perform(patch("/api/comandas/1/fechar")).andExpect(status().isOk());
        abrir("  MESA 01  ").andExpect(status().isCreated());
        assertEquals(2, quantidadeComandas());
    }

    @Test
    void permiteReutilizarDepoisDeCancelar() throws Exception {
        abrir("Mesa 01").andExpect(status().isCreated());
        mvc.perform(patch("/api/comandas/1/cancelar")).andExpect(status().isOk());
        abrir("mesa 01").andExpect(status().isCreated());
    }

    @Test
    void apenasUmaDasDuasRequisicoesSimultaneasAbreAComanda() throws Exception {
        // Ambas passam pela consulta ANTES de qualquer INSERT. Assim o teste
        // exercita obrigatoriamente o índice e a tradução da violação pelo serviço.
        var barreira = new CyclicBarrier(2);
        // Spring delega a interface do repositório ao proxy JPA original.
        // callRealMethod não é válido para os métodos abstratos dessa interface.
        var persistenciaReal = org.mockito.Mockito.mockingDetails(repository)
                .getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocacao -> {
            barreira.await(15, TimeUnit.SECONDS);
            return persistenciaReal.answer(invocacao);
        }).when(repository).saveAndFlush(any(Comanda.class));

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> abrir("Mesa 01").andReturn().getResponse());
            var segunda = executor.submit(() -> abrir("  MESA 01  ").andReturn().getResponse());
            var resultados = List.of(primeira.get(30, TimeUnit.SECONDS), segunda.get(30, TimeUnit.SECONDS));
            assertEquals(List.of(201, 409), resultados.stream().map(resposta -> resposta.getStatus()).sorted().toList());
            var conflito = resultados.stream().filter(resposta -> resposta.getStatus() == 409).findFirst().orElseThrow();
            assertTrue(conflito.getContentAsString(StandardCharsets.UTF_8)
                    .contains("Já existe uma comanda aberta com essa descrição."));
        }
        assertEquals(1, quantidadeComandas());
    }

    @Test
    void bancoBloqueiaInsercaoDiretaQueNaoPassaPeloServico() throws Exception {
        executar("INSERT INTO comandavision.comandas (identificacao) VALUES ('Mesa 01')");
        var erro = assertThrows(SQLException.class,
                () -> executar("INSERT INTO comandavision.comandas (identificacao) VALUES ('  MESA 01  ')"));
        assertEquals("23505", erro.getSQLState());
        assertEquals(1, quantidadeComandas());
    }

    @Test
    void migracaoRelataDuplicidadesSemAlterarRegistros() throws Exception {
        executar("DROP INDEX comandavision.uk_comandas_abertas_identificacao");
        executar("INSERT INTO comandavision.comandas (identificacao) VALUES ('Mesa 01'), ('  MESA 01  ')");
        try (var conexao = dataSource.getConnection()) {
            conexao.setAutoCommit(false);
            var erro = assertThrows(SQLException.class,
                    () -> executarRecurso(conexao, "V8__identificacao_unica_em_comandas_abertas.sql"));
            assertEquals("23505", erro.getSQLState());
            assertTrue(erro.getMessage().contains("IDs: {1,2}"));
            conexao.rollback();
        }
        assertEquals(2, quantidadeComandas());
        try (var conexao = dataSource.getConnection(); var comando = conexao.createStatement();
                var dados = comando.executeQuery("SELECT identificacao, status FROM comandavision.comandas ORDER BY id")) {
            assertTrue(dados.next());
            assertEquals("Mesa 01", dados.getString(1));
            assertEquals("ABERTA", dados.getString(2));
            assertTrue(dados.next());
            assertEquals("  MESA 01  ", dados.getString(1));
            assertEquals("ABERTA", dados.getString(2));
        }
        // Somente dados descartáveis deste teste: simula revisão manual do conflito.
        executar("UPDATE comandavision.comandas SET status = 'FECHADA', fechada_em = CURRENT_TIMESTAMP WHERE id = 1");
        try (var conexao = dataSource.getConnection()) {
            conexao.setAutoCommit(false);
            executarRecurso(conexao, "V8__identificacao_unica_em_comandas_abertas.sql");
            conexao.commit();
        }
        assertEquals(2, quantidadeComandas());
    }

    private org.springframework.test.web.servlet.ResultActions abrir(String identificacao) throws Exception {
        return mvc.perform(post("/api/comandas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identificacao\":\"" + identificacao + "\",\"observacao\":\"Preservar dados\"}"));
    }

    private void executar(String sql) throws SQLException {
        try (var conexao = dataSource.getConnection(); var comando = conexao.createStatement()) {
            comando.execute(sql);
        }
    }

    private long quantidadeComandas() throws SQLException {
        try (var conexao = dataSource.getConnection(); var comando = conexao.createStatement();
                var dados = comando.executeQuery("SELECT count(*) FROM comandavision.comandas")) {
            dados.next();
            return dados.getLong(1);
        }
    }

    private static void executarRecurso(Connection conexao, String migration) throws SQLException, IOException {
        try (var recurso = ComandaAberturaPostgresTest.class.getResourceAsStream("/db/migration/" + migration);
                var comando = conexao.createStatement()) {
            if (recurso == null) throw new IOException("Migração não encontrada: " + migration);
            comando.execute(new String(recurso.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackages = { "br.com.comandavision.api.comanda", "br.com.comandavision.api.produto" })
    @Import(ComandaService.class)
    static class Configuracao {
        @Bean
        DataSource dataSource() throws Exception {
            String url = System.getenv("COMANDAVISION_TEST_DB_URL");
            if (url == null || !url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/comandavision_test")) {
                throw new IllegalArgumentException("Os testes exigem um banco local descartável comandavision_test");
            }
            var fonte = new DriverManagerDataSource(url,
                    System.getenv().getOrDefault("COMANDAVISION_TEST_DB_USER", "comandavision_test"),
                    System.getenv().getOrDefault("COMANDAVISION_TEST_DB_PASSWORD", ""));
            try (var conexao = fonte.getConnection(); var comando = conexao.createStatement()) {
                conexao.setAutoCommit(false);
                comando.execute("DROP SCHEMA IF EXISTS comandavision CASCADE");
                for (String migration : List.of("V1__criar_schema_comandavision.sql",
                        "V2__criar_tabelas_categorias_e_produtos.sql", "V3__criar_tabelas_comandas_e_itens.sql",
                        "V7__adicionar_imagem_url_em_produtos.sql")) {
                    executarRecurso(conexao, migration);
                }
                conexao.commit();
            }
            // O schema de teste tem as tabelas necessárias até V7. A própria
            // ferramenta de produção aplica V8, incluindo parsing e transação.
            Flyway.configure().dataSource(fonte).schemas("comandavision")
                    .baselineOnMigrate(true).baselineVersion("7").load().migrate();
            return fonte;
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var fabrica = new LocalContainerEntityManagerFactoryBean();
            fabrica.setDataSource(dataSource);
            fabrica.setPackagesToScan("br.com.comandavision.api.comanda", "br.com.comandavision.api.produto", "br.com.comandavision.api.categoria");
            fabrica.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            fabrica.setJpaPropertyMap(Map.of("hibernate.default_schema", "comandavision", "hibernate.hbm2ddl.auto", "none"));
            return fabrica;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory fabrica) {
            return new JpaTransactionManager(fabrica);
        }
    }
}
