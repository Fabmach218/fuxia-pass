package com.app.pasarela.repository;

import java.util.Date;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.pasarela.model.Pago;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer>{
    @Query("SELECT sum(p.monto) FROM Pago p WHERE TO_CHAR(p.fechaHora, 'yyyy-MM-dd') = TO_CHAR(NOW(), 'yyyy-MM-dd') AND p.tarjeta.id = :tarjetaId")
    Double getSumMontoTarjetaHoy(@Param("tarjetaId") Integer tarjetaId);
}
