package br.com.comandavision.api.pix;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.comandavision.api.pagamento.StatusPagamento;

// As cobranças são lidas fora de transação (antes de chamar o Mercado Pago), por isso
// pagamento e comanda já vêm carregados junto.
public interface CobrancaPixRepository extends JpaRepository<CobrancaPix, Long> {
    @EntityGraph(attributePaths = { "pagamento", "pagamento.comanda" })
    Optional<CobrancaPix> findByPagamentoId(Long pagamentoId);

    @EntityGraph(attributePaths = { "pagamento", "pagamento.comanda" })
    Optional<CobrancaPix> findByPedidoMercadoPago(String pedidoMercadoPago);

    @EntityGraph(attributePaths = { "pagamento", "pagamento.comanda" })
    List<CobrancaPix> findByPagamentoComandaIdAndPagamentoStatus(Long comandaId, StatusPagamento status);
}
