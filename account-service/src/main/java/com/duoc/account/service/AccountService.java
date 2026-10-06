package com.duoc.account.service;

import com.duoc.account.model.Cuenta;
import com.duoc.account.repository.CuentaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {
    private final CuentaRepository repo;

    public AccountService(CuentaRepository repo) {
        this.repo = repo;
    }

    public List<Cuenta> findAll() {
        return repo.findAll();
    }

    public Cuenta save(Cuenta cuenta) {
        return repo.save(cuenta);
    }
}
