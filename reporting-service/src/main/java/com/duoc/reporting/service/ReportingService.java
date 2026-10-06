package com.duoc.reporting.service;

import com.duoc.reporting.model.Reporte;
import com.duoc.reporting.repository.ReporteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportingService {
    private final ReporteRepository repo;

    public ReportingService(ReporteRepository repo) {
        this.repo = repo;
    }

    public List<Reporte> findAll() {
        return repo.findAll();
    }
}
