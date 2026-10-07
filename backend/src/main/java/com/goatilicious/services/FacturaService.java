package com.goatilicious.services;

import com.goatilicious.dtos.PedidoRequest.ItemRequest;
import com.goatilicious.dtos.RevisaoRequest;
import com.goatilicious.enums.StatusPedido;
import com.goatilicious.models.Factura;
import com.goatilicious.models.ItemPedido;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import com.goatilicious.repositories.FacturaRepository;
import com.goatilicious.repositories.PedidoRepository;
import com.goatilicious.repositories.ProdutoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;

    public FacturaService(FacturaRepository facturaRepository,
                          PedidoRepository pedidoRepository,
                          ProdutoRepository produtoRepository) {
        this.facturaRepository = facturaRepository;
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
    }

    /** Chamado ao registar um pedido: cria a fatura rascunho com o total atual. */
    public Factura gerarRascunho(Pedido pedido) {
        return facturaRepository.save(Factura.rascunhoDe(pedido));
    }

    public List<Factura> listar() {
        return facturaRepository.findAll();
    }

    public Factura buscarPorId(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura não encontrada"));
    }

    public Factura buscarPorPedido(Long idPedido) {
        return facturaRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura não encontrada"));
    }

    /**
     * Revisão do funcionário: a lista recebida passa a ser a lista final de itens do pedido.
     * Produtos que já estavam no pedido mantêm o preço congelado; produtos novos usam o preço atual.
     * Toda a validação acontece antes de qualquer alteração.
     */
    @Transactional
    public Factura revisar(Long idPedido, RevisaoRequest request) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O pedido já foi revisto");
        }

        Factura factura = buscarPorPedido(idPedido);

        Set<Long> vistos = new HashSet<>();
        for (ItemRequest itemRequest : request.itens()) {
            if (!vistos.add(itemRequest.produtoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Produto " + itemRequest.produtoId() + " repetido na lista de itens");
            }
        }

        Map<Long, BigDecimal> precosCongelados = new HashMap<>();
        for (ItemPedido existente : pedido.getItens()) {
            precosCongelados.put(existente.getProduto().getIdProduto(), existente.getPrecoAplicado());
        }

        List<ItemPedido> novosItens = new ArrayList<>();
        for (ItemRequest itemRequest : request.itens()) {
            Produto produto = produtoRepository.findById(itemRequest.produtoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Produto " + itemRequest.produtoId() + " não encontrado"));

            ItemPedido item = new ItemPedido();
            item.setProduto(produto);
            item.setQuantidade(itemRequest.quantidade());
            item.setPrecoAplicado(precosCongelados.getOrDefault(produto.getIdProduto(), produto.getPrecoUnitario()));
            novosItens.add(item);
        }

        pedido.getItens().clear();
        novosItens.forEach(pedido::adicionarItem);
        pedido.setStatus(StatusPedido.REVISADO);
        pedidoRepository.save(pedido);

        factura.finalizar(request.condicaoPagamento(), pedido.getTotal());
        return facturaRepository.save(factura);
    }
}
