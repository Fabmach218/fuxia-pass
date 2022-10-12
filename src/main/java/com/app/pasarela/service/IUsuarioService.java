package com.app.pasarela.service;

import org.springframework.stereotype.Service;

import com.app.pasarela.model.Usuario;

@Service
public interface IUsuarioService {
    public Usuario findByUsername(String username);
    public Usuario registrar(Usuario u);
}
