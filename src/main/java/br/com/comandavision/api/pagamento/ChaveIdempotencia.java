package br.com.comandavision.api.pagamento;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class ChaveIdempotencia {
    // Sem uma chave fornecida pelo cliente antigo nao existe como distinguir com certeza
    // um retry de dois pagamentos identicos e intencionais. A janela curta cobre duplo toque
    // e retry de rede sem bloquear por minutos dois pagamentos legitimos de mesmo valor.
    static final Duration JANELA_COMPATIBILIDADE = Duration.ofSeconds(30);

    private ChaveIdempotencia() {
    }

    public static Resolucao obrigatoria(String valor) {
        String chave = validar(valor);
        return new Resolucao(chave, List.of(chave), false);
    }

    public static Resolucao resolverTemporaria(
            String valor,
            UUID usuarioId,
            String operacao,
            Long comandaId,
            List<String> dados,
            Instant agora) {
        // Cabecalho presente, mesmo vazio, segue a validacao estrita dos clientes novos.
        if (valor != null) {
            return obrigatoria(valor);
        }

        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(operacao, "operacao");
        Objects.requireNonNull(comandaId, "comandaId");
        Objects.requireNonNull(dados, "dados");
        Objects.requireNonNull(agora, "agora");

        long tamanhoJanela = JANELA_COMPATIBILIDADE.toSeconds();
        long janelaAtual = Math.floorDiv(agora.getEpochSecond(), tamanhoJanela);
        String atual = gerarDeterministica(usuarioId, operacao, comandaId, dados, janelaAtual);
        String anterior = gerarDeterministica(usuarioId, operacao, comandaId, dados, janelaAtual - 1);
        return new Resolucao(atual, List.of(atual, anterior), true);
    }

    public static String validar(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ChaveIdempotenciaInvalidaException(
                    "Informe o cabeçalho Idempotency-Key com um UUID para registrar o pagamento");
        }

        String normalizada = valor.trim().toLowerCase(Locale.ROOT);
        try {
            UUID uuid = UUID.fromString(normalizada);
            if (!uuid.toString().equals(normalizada)) {
                throw new IllegalArgumentException("UUID fora do formato canônico");
            }
            return normalizada;
        } catch (IllegalArgumentException exception) {
            throw new ChaveIdempotenciaInvalidaException(
                    "O cabeçalho Idempotency-Key deve conter um UUID válido");
        }
    }

    private static String gerarDeterministica(
            UUID usuarioId,
            String operacao,
            Long comandaId,
            List<String> dados,
            long janela) {
        try {
            MessageDigest resumo = MessageDigest.getInstance("SHA-256");
            List<String> partes = new ArrayList<>();
            partes.add("compat-pagamentos-v1");
            partes.add(usuarioId.toString());
            partes.add(operacao);
            partes.add(comandaId.toString());
            partes.addAll(dados);
            partes.add(Long.toString(janela));

            for (String parte : partes) {
                byte[] bytes = Objects.toString(parte, "").getBytes(StandardCharsets.UTF_8);
                resumo.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                resumo.update(bytes);
            }

            byte[] hash = resumo.digest();
            // Mantem o formato UUID v4 exigido pelo provedor, mas o conteudo e derivado
            // deterministicamente para que o mesmo retry legado reutilize a mesma chave.
            hash[6] = (byte) ((hash[6] & 0x0f) | 0x40);
            hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
            ByteBuffer bytesUuid = ByteBuffer.wrap(hash);
            return new UUID(bytesUuid.getLong(), bytesUuid.getLong()).toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    public record Resolucao(String efetiva, List<String> candidatas, boolean legada) {
        public Resolucao {
            Objects.requireNonNull(efetiva, "efetiva");
            candidatas = List.copyOf(candidatas);
            if (candidatas.isEmpty() || !candidatas.contains(efetiva)) {
                throw new IllegalArgumentException("A chave efetiva deve estar entre as candidatas");
            }
        }
    }
}
