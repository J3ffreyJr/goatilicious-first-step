package com.goatilicious.services;

import com.goatilicious.dtos.PedidoRequest;
import com.goatilicious.dtos.PedidoRequest.ItemRequest;
import com.goatilicious.models.Cliente;
import com.goatilicious.models.ItemPedido;
import com.goatilicious.models.Pedido;
import com.goatilicious.models.Produto;
import com.goatilicious.repositories.ClienteRepository;
import com.goatilicious.repositories.PedidoRepository;
import com.goatilicious.repositories.ProdutoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         ProdutoRepository produtoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public Pedido registrar(PedidoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setOrigem(request.origem());

        for (ItemRequest itemRequest : request.itens()) {
            Produto produto = produtoRepository.findById(itemRequest.produtoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Produto " + itemRequest.produtoId() + " não encontrado"));

            ItemPedido item = new ItemPedido();
            item.setProduto(produto);
            item.setQuantidade(itemRequest.quantidade());
            item.setPrecoAplicado(produto.getPrecoUnitario());
            pedido.adicionarItem(item);
        }

        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listar() {
        return pedidoRepository.findAll();
    }

    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
    }
}
