package com.app.pasarela.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.pasarela.model.Pago;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer>{
    
}
