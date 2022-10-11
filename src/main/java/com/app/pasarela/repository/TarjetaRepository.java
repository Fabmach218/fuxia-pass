package com.app.pasarela.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.pasarela.model.Tarjeta;

@Repository
public interface TarjetaRepository extends JpaRepository<Tarjeta, Integer>{
    
    Tarjeta findById(int id);
    List<Tarjeta> findByDni(String dni);

    @Query(value = "SELECT t FROM Tarjeta t WHERE t.nroTarjeta = :nroTarjeta AND t.dueDate = :dueDate AND t.cvv = :cvv AND t.nombre = :nombre")
    Tarjeta findByCredenciales(@Param("nroTarjeta") String nroTarjeta, @Param("dueDate") String dueDate, @Param("cvv") String cvv, @Param("nombre") String nombre);

}
