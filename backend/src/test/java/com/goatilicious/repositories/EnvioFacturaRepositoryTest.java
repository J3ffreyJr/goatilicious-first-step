package com.goatilicious.repositories;

import com.goatilicious.enums.OrigemPedido;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.EnvioFactura;
import com.goatilicious.models.Factura;
import com.goatilicious.models.ItemPedido;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EnvioFacturaRepositoryTest {

    @Autowired
    private EnvioFacturaRepository envioRepository;
    @Autowired
    private FacturaRepository facturaRepository;
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProdutoRepository produtoRepository;

    private Factura factura;

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

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setOrigem(OrigemPedido.ONLINE);
        ItemPedido item = new ItemPedido();
        item.setProduto(produto);
        item.setQuantidade(1);
        item.setPrecoAplicado(produto.getPrecoUnitario());
        pedido.adicionarItem(item);
        pedido = pedidoRepository.saveAndFlush(pedido);

        factura = facturaRepository.saveAndFlush(Factura.rascunhoDe(pedido));
    }

    @Test
    void gravaEnvioComDataPreenchida() {
        EnvioFactura salvo = envioRepository.saveAndFlush(EnvioFactura.sucesso(factura, "ana@mail.com"));

        assertNotNull(salvo.getIdEnvio());
        assertNotNull(salvo.getDataEnvio());
        assertTrue(salvo.isSucesso());
    }

    @Test
    void listaEnviosDaFacturaMaisRecentePrimeiro() {
        EnvioFactura primeiro = envioRepository.saveAndFlush(EnvioFactura.falha(factura, "ana@mail.com", "erro"));
        EnvioFactura segundo = envioRepository.saveAndFlush(EnvioFactura.sucesso(factura, "ana@mail.com"));

        List<EnvioFactura> envios = envioRepository
                .findByFacturaIdFacturaOrderByDataEnvioDescIdEnvioDesc(factura.getIdFactura());

        assertEquals(2, envios.size());
        assertEquals(segundo.getIdEnvio(), envios.get(0).getIdEnvio());
        assertEquals(primeiro.getIdEnvio(), envios.get(1).getIdEnvio());
    }

    @Test
    void naoMisturaEnviosDeOutraFactura() {
        envioRepository.saveAndFlush(EnvioFactura.sucesso(factura, "ana@mail.com"));

        assertTrue(envioRepository.findByFacturaIdFacturaOrderByDataEnvioDescIdEnvioDesc(999L).isEmpty());
    }
}
