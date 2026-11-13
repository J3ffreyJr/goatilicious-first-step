package com.goatilicious.controllers;

import com.goatilicious.dtos.DashboardResposta;
import com.goatilicious.services.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    /** Só staff (regra no SecurityConfig: tudo o que não está listado exige STAFF). */
    @GetMapping
    public DashboardResposta resumo() {
        return service.resumo();
    }
}
