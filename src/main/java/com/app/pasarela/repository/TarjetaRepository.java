package com.app.pasarela.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.pasarela.model.Tarjeta;

@Repository
public interface TarjetaRepository extends JpaRepository<Tarjeta, Integer>{
    Tarjeta findById(int id);
    @Query(value = "SELECT t FROM Tarjeta t WHERE t.credenciales = :credenciales")
    Tarjeta findByCredenciales(@Param("credenciales") String credenciales);
}
