package com.goatilicious.config;

import com.goatilicious.services.UtilizadorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro staff a partir de variáveis de ambiente (GOATILICIOUS_STAFF_EMAIL,
 * GOATILICIOUS_STAFF_PASSWORD e, opcionalmente, GOATILICIOUS_STAFF_NAME). Nenhuma senha vive no código.
 * Sem as duas primeiras variáveis nada é criado.
 */
@Component
public class CarregadorStaff implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CarregadorStaff.class);

    private final UtilizadorService utilizadorService;
    private final String email;
    private final String senha;
    private final String nome;

    public CarregadorStaff(UtilizadorService utilizadorService,
                           @Value("${goatilicious.staff.email:}") String email,
                           @Value("${goatilicious.staff.password:}") String senha,
                           @Value("${goatilicious.staff.name:Staff}") String nome) {
        this.utilizadorService = utilizadorService;
        this.email = email;
        this.senha = senha;
        this.nome = nome;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() || senha.isBlank()) {
            log.warn("GOATILICIOUS_STAFF_EMAIL/GOATILICIOUS_STAFF_PASSWORD não definidos: nenhum staff criado.");
            return;
        }
        utilizadorService.criarStaff(nome, email, senha);
    }
}
