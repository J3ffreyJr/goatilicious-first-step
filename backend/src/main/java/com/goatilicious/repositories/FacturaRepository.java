package com.goatilicious.repositories;

import com.goatilicious.models.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    Optional<Factura> findByPedidoIdPedido(Long idPedido);
}
