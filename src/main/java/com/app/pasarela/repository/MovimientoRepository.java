package com.app.pasarela.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.pasarela.model.Movimiento;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Integer>{
    @Query("SELECT COALESCE(sum(m.monto) * -1, 0.0) FROM Movimiento m WHERE TO_CHAR(m.fechaHora, 'yyyy-MM-dd') = TO_CHAR(NOW(), 'yyyy-MM-dd') AND m.tarjeta.id = :tarjetaId AND m.monto < 0")
    Double getSumMontoTarjetaHoy(@Param("tarjetaId") Integer tarjetaId);

    @Query("SELECT m FROM Movimiento m WHERE m.tarjeta.id = :tarjetaId AND m.fechaHora BETWEEN TO_TIMESTAMP(:fechaInicio, 'yyyy-MM-dd HH24:mi:ss') AND TO_TIMESTAMP(:fechaFin, 'yyyy-MM-dd HH24:mi:ss') ORDER BY m.fechaHora DESC")
    List<Movimiento> getMovimientosTarjeta(@Param("tarjetaId") Integer tarjetaId, @Param("fechaInicio") String fechaInicio, @Param("fechaFin") String fechaFin);

    @Query("SELECT COALESCE(sum(m.monto), 0.0) FROM Movimiento m WHERE m.tarjeta.id = :tarjetaId AND m.fechaHora < to_timestamp(:fechaInicio, 'yyyy-MM-dd HH24:mi:ss')")
    Double getSaldoInicialFecha(@Param("tarjetaId") Integer tarjetaId, @Param("fechaInicio") String fechaInicio);

    @Query("SELECT COALESCE(sum(m.monto), 0.0) FROM Movimiento m WHERE m.tarjeta.id = :tarjetaId AND m.fechaHora < to_timestamp(:fechaFin, 'yyyy-MM-dd HH24:mi:ss')")
    Double getSaldoFinalFecha(@Param("tarjetaId") Integer tarjetaId, @Param("fechaFin") String fechaFin);
}
