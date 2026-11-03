package br.com.comandavision.api.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ChaveIdempotenciaTest {
    private static final UUID USUARIO = UUID.fromString("b35bbcb0-1128-45a2-bd16-cb8d99be154f");

    @Test
    void preservaUuidEnviadoPeloClienteNovo() {
        String chave = "5c6ad2ad-b438-4bd1-9b2c-aebcf3cc87ef";

        ChaveIdempotencia.Resolucao resolucao = ChaveIdempotencia.resolverTemporaria(
                chave, USUARIO, "pagamento-manual", 10L, List.of("DINHEIRO", "20"), Instant.EPOCH);

        assertEquals(chave, resolucao.efetiva());
        assertEquals(List.of(chave), resolucao.candidatas());
        assertFalse(resolucao.legada());
    }

    @Test
    void geraChaveDeterministicaParaClienteAntigoSemCabecalho() {
        Instant agora = Instant.parse("2026-11-03T12:03:00Z");

        ChaveIdempotencia.Resolucao primeira = ChaveIdempotencia.resolverTemporaria(
                null, USUARIO, "pagamento-manual", 10L, List.of("DINHEIRO", "20"), agora);
        ChaveIdempotencia.Resolucao repetida = ChaveIdempotencia.resolverTemporaria(
                null, USUARIO, "pagamento-manual", 10L, List.of("DINHEIRO", "20"), agora);

        assertEquals(primeira, repetida);
        assertTrue(primeira.legada());
        assertEquals(2, primeira.candidatas().size());
        assertEquals(4, UUID.fromString(primeira.efetiva()).version());
    }

    @Test
    void reconheceRetryQueAtravessaFronteiraDaJanela() {
        ChaveIdempotencia.Resolucao antes = ChaveIdempotencia.resolverTemporaria(
                null, USUARIO, "pix-integrado", 10L, List.of("40"),
                Instant.parse("2026-11-03T12:00:29Z"));
        ChaveIdempotencia.Resolucao depois = ChaveIdempotencia.resolverTemporaria(
                null, USUARIO, "pix-integrado", 10L, List.of("40"),
                Instant.parse("2026-11-03T12:00:31Z"));

        assertTrue(depois.candidatas().contains(antes.efetiva()));
    }

    @Test
    void permiteNovaIntencaoLegadaDepoisDaJanelaDeCompatibilidade() {
        ChaveIdempotencia.Resolucao primeira = ChaveIdempotencia.resolverTemporaria(
                null, USUARIO, "pagamento-manual", 10L, List.of("DINHEIRO", "20"),
                Instant.parse("2026-11-03T12:00:01Z"));
        ChaveIdempotencia.Resolucao posterior = ChaveIdempotencia.resolverTemporaria(
                null, USUARIO, "pagamento-manual", 10L, List.of("DINHEIRO", "20"),
                Instant.parse("2026-11-03T12:01:01Z"));

        assertFalse(posterior.candidatas().contains(primeira.efetiva()));
    }

    @Test
    void separaIntencoesPorUsuarioOperacaoComandaEConteudo() {
        Instant agora = Instant.parse("2026-11-03T12:03:00Z");
        ChaveIdempotencia.Resolucao base = ChaveIdempotencia.resolverTemporaria(
                null, USUARIO, "pagamento-manual", 10L, List.of("DINHEIRO", "20"), agora);

        assertNotEquals(base.efetiva(), ChaveIdempotencia.resolverTemporaria(
                null, UUID.fromString("35f72406-1291-45cb-a637-8404a687c931"),
                "pagamento-manual", 10L, List.of("DINHEIRO", "20"), agora).efetiva());
        assertNotEquals(base.efetiva(), ChaveIdempotencia.resolverTemporaria(
                null, USUARIO, "pix-integrado", 10L, List.of("20"), agora).efetiva());
        assertNotEquals(base.efetiva(), ChaveIdempotencia.resolverTemporaria(
                null, USUARIO, "pagamento-manual", 11L, List.of("DINHEIRO", "20"), agora).efetiva());
        assertNotEquals(base.efetiva(), ChaveIdempotencia.resolverTemporaria(
                null, USUARIO, "pagamento-manual", 10L, List.of("DINHEIRO", "21"), agora).efetiva());
    }

    @Test
    void naoTrataCabecalhoVazioComoClienteLegado() {
        assertThrows(ChaveIdempotenciaInvalidaException.class,
                () -> ChaveIdempotencia.resolverTemporaria(
                        " ", USUARIO, "pagamento-manual", 10L, List.of("DINHEIRO", "20"), Instant.EPOCH));
    }
}
