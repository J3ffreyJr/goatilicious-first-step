package com.goatilicious.repositories;

import com.goatilicious.enums.OrigemPedido;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.Factura;
import com.goatilicious.models.ItemPedido;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class FacturaRepositoryTest {

    @Autowired
    private FacturaRepository facturaRepository;
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProdutoRepository produtoRepository;

    private Pedido pedido;

    @BeforeEach
    void preparar() {
        Cliente c = new Cliente();
        c.setNome("Ana");
        c.setEmail("ana@mail.com");
        Cliente cliente = clienteRepository.save(c);

        Produto p = new Produto();
        p.setSabor("Cereja");
        p.setTamanho(TamanhoProduto.ML_500);
        p.setPrecoUnitario(new BigDecimal("350.00"));
        p.setTipoEstoque(TipoEstoque.PRONTA_ENTREGA);
        Produto produto = produtoRepository.save(p);

        Pedido novo = new Pedido();
        novo.setCliente(cliente);
        novo.setOrigem(OrigemPedido.ONLINE);
        ItemPedido item = new ItemPedido();
        item.setProduto(produto);
        item.setQuantidade(2);
        item.setPrecoAplicado(produto.getPrecoUnitario());
        novo.adicionarItem(item);
        pedido = pedidoRepository.saveAndFlush(novo);
    }

    @Test
    void gravaFacturaEEncontraPorPedido() {
        facturaRepository.saveAndFlush(Factura.rascunhoDe(pedido));

        Optional<Factura> encontrada = facturaRepository.findByPedidoIdPedido(pedido.getIdPedido());

        assertTrue(encontrada.isPresent());
        assertNotNull(encontrada.get().getIdFactura());
        assertNotNull(encontrada.get().getDataEmissao());
        assertEquals(0, new BigDecimal("700.00").compareTo(encontrada.get().getValorTotal()));
    }

    @Test
    void devolveVazioParaPedidoSemFactura() {
        assertTrue(facturaRepository.findByPedidoIdPedido(999L).isEmpty());
    }

    @Test
    void naoPermiteDuasFacturasParaOMesmoPedido() {
        facturaRepository.saveAndFlush(Factura.rascunhoDe(pedido));

        assertThrows(DataIntegrityViolationException.class,
                () -> facturaRepository.saveAndFlush(Factura.rascunhoDe(pedido)));
    }
}
