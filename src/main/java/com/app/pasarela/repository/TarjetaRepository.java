package com.app.pasarela.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.pasarela.model.Tarjeta;

@Repository
public interface TarjetaRepository extends JpaRepository<Tarjeta, Integer>{
    Tarjeta findById(int id);
    Tarjeta findByCredenciales(byte[] credenciales);
}
