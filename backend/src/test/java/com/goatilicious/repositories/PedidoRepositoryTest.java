package com.goatilicious.repositories;

import com.goatilicious.enums.OrigemPedido;
import com.goatilicious.enums.StatusPedido;
import com.goatilicious.enums.TamanhoProduto;
import com.goatilicious.enums.TipoEstoque;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.ItemPedido;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PedidoRepositoryTest {

    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private TestEntityManager em;

    private Cliente cliente;
    private Produto produto;

    @BeforeEach
    void preparar() {
        Cliente c = new Cliente();
        c.setNome("Ana");
        c.setEmail("ana@mail.com");
        cliente = clienteRepository.save(c);

        Produto p = new Produto();
        p.setSabor("Baunilha & Mel");
        p.setTamanho(TamanhoProduto.ML_500);
        p.setPrecoUnitario(new BigDecimal("350.00"));
        p.setTipoEstoque(TipoEstoque.PRONTA_ENTREGA);
        produto = produtoRepository.save(p);
    }

    private Pedido novoPedido() {
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setOrigem(OrigemPedido.ONLINE);

        ItemPedido item = new ItemPedido();
        item.setProduto(produto);
        item.setQuantidade(2);
        item.setPrecoAplicado(produto.getPrecoUnitario());
        pedido.adicionarItem(item);
        return pedido;
    }

    @Test
    void deveGravarPedidoComItensEmCascata() {
        Pedido salvo = pedidoRepository.saveAndFlush(novoPedido());

        assertNotNull(salvo.getIdPedido());
        assertEquals(1, salvo.getItens().size());
        assertNotNull(salvo.getItens().get(0).getIdItem());
    }

    @Test
    void pedidoNovoFicaPendenteComDataPreenchida() {
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        Pedido salvo = pedidoRepository.saveAndFlush(novoPedido());

        assertEquals(StatusPedido.PENDENTE, salvo.getStatus());
        assertNotNull(salvo.getDataPedido());
        assertTrue(salvo.getDataPedido().isAfter(antes));
    }

    @Test
    void deveRecarregarPedidoDoBancoComClienteEItens() {
        Long id = pedidoRepository.saveAndFlush(novoPedido()).getIdPedido();
        em.clear();

        Pedido lido = pedidoRepository.findById(id).orElseThrow();

        assertEquals("Ana", lido.getCliente().getNome());
        assertEquals(OrigemPedido.ONLINE, lido.getOrigem());
        assertEquals(1, lido.getItens().size());
        assertEquals(2, lido.getItens().get(0).getQuantidade());
        assertEquals(0, new BigDecimal("700.00").compareTo(lido.getTotal()));
    }

    @Test
    void deveListarTodosOsPedidos() {
        pedidoRepository.save(novoPedido());
        pedidoRepository.save(novoPedido());

        assertEquals(2, pedidoRepository.findAll().size());
    }
}
