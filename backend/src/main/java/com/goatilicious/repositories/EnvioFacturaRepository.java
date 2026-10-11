package com.goatilicious.repositories;

import com.goatilicious.models.EnvioFactura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnvioFacturaRepository extends JpaRepository<EnvioFactura, Long> {

    List<EnvioFactura> findByFacturaIdFacturaOrderByDataEnvioDescIdEnvioDesc(Long idFactura);
}
