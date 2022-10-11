package com.app.pasarela.controller.rest;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.pasarela.integration.reniec.ReniecApi;
import com.app.pasarela.integration.reniec.UserReniec;
import com.app.pasarela.model.Pago;
import com.app.pasarela.model.Tarjeta;
import com.app.pasarela.model.dto.ModelPagoAbono;
import com.app.pasarela.model.dto.ModelTarjetaCreate;
import com.app.pasarela.repository.PagoRepository;
import com.app.pasarela.repository.TarjetaRepository;

@RestController
@RequestMapping(value = "api/tarjeta", produces = "application/json")
public class TarjetaRestController {
    
    @Autowired
    private TarjetaRepository _dataTarjetas;

    @Autowired
    private PagoRepository _dataPagos;

    @GetMapping(value = "getTarjetasByDNI/{dni}", produces = "application/json")
    public ResponseEntity<List<Tarjeta>> getTarjetasByDNI(@PathVariable String dni){
        return new ResponseEntity<List<Tarjeta>>(_dataTarjetas.findByDni(dni), HttpStatus.OK);
    }

    @PatchMapping(value = "editarTarjeta/{id}", produces = "application/json")
    public ResponseEntity<Tarjeta> editarTarjeta(@PathVariable int id, @RequestParam boolean active){
        
        Tarjeta t = _dataTarjetas.findById(id);

        if(t != null){
            t.setActive(active);
            _dataTarjetas.save(t);
            _dataTarjetas.flush();
            return new ResponseEntity<Tarjeta>(t, HttpStatus.OK);
        }else{
            return new ResponseEntity<Tarjeta>(HttpStatus.NOT_FOUND);
        }

    }

    @PostMapping(value = "/pagar", produces = "application/json")
    public ResponseEntity<Map<String, Object>> pagar(@RequestBody ModelPagoAbono form){

        Tarjeta tarjeta = _dataTarjetas.findByCredenciales(form.getNroTarjeta(), form.getDueMonth() + "/" + form.getDueYear(), form.getCvv(), form.getNombre().toUpperCase());

        Map<String, Object> respuesta = new HashMap<>();

        String status = "";
        String mensaje = "";

        if(validarTarjeta(tarjeta)){

            if(form.getMoneda().equals(tarjeta.getMoneda())){
                
                if(tarjeta.getSaldo() >= form.getMonto()){

                    Pago p = new Pago();
                    p.setTarjeta(tarjeta);
                    p.setMonto(form.getMonto());
                    p.setFechaHora(new Date());
                    _dataPagos.save(p);

                    tarjeta.setSaldo(tarjeta.getSaldo() - form.getMonto());
                    _dataTarjetas.save(tarjeta);
                    status = "success";
                    mensaje = "Pago realizado con éxito!!!";

                }else{
                    status = "error";
                    mensaje = "No tiene suficiente saldo para pagar.";
                }

            }else{

                status = "reload";
                String moneda = tarjeta.getMoneda();

                if(moneda.equals("USD")){
                    form.setMoneda("PEN");
                    Double monto = form.getMonto() * form.getTcCompra();
                    monto = Math.rint(monto * 100) / 100;
                    form.setMonto(monto);
                }else{
                    form.setMoneda("USD");
                    Double monto = form.getMonto() / form.getTcVenta();
                    monto = Math.rint(monto * 100) / 100;
                    form.setMonto(monto);
                }

                mensaje = "Se recalculó el monto total en " + moneda + ".";

            }

        }else{

            status = "error";
            mensaje = "Los datos son incorrectos.";

        }

        if(status.equals("success")){
            respuesta.put("tarjeta", null);
        }else{
            respuesta.put("tarjeta", form);
        }

        respuesta.put("status", status);
        respuesta.put("mensaje", mensaje);

        return new ResponseEntity<Map<String,Object>>(respuesta, HttpStatus.OK);

    }

    @PostMapping(value = "/abonar", produces = "application/json")
    public ResponseEntity<Map<String, Object>> abonar(@RequestBody ModelPagoAbono form){

        Tarjeta tarjeta = _dataTarjetas.findByCredenciales(form.getNroTarjeta(), form.getDueMonth() + "/" + form.getDueYear(), form.getCvv(), form.getNombre().toUpperCase());

        Map<String, Object> respuesta = new HashMap<>();

        String status = "";
        String mensaje = "";

        if(validarTarjeta(tarjeta)){

            if(form.getMoneda().equals(tarjeta.getMoneda())){

                tarjeta.setSaldo(tarjeta.getSaldo() + form.getMonto());
                _dataTarjetas.save(tarjeta);
                status = "success";
                mensaje = "Abono realizado con éxito!!!";

            }else{

                status = "reload";
                String moneda = tarjeta.getMoneda();

                if(moneda.equals("USD")){
                    form.setMoneda("PEN");
                    Double monto = form.getMonto() * form.getTcCompra();
                    monto = Math.rint(monto * 100) / 100;
                    form.setMonto(monto);
                }else{
                    form.setMoneda("USD");
                    Double monto = form.getMonto() / form.getTcVenta();
                    monto = Math.rint(monto * 100) / 100;
                    form.setMonto(monto);
                }

                mensaje = "Se recalculó el monto total en " + moneda + ".";

            }

        }else{

            status = "error";
            mensaje = "Los datos son incorrectos.";

        }

        if(status.equals("success")){
            respuesta.put("tarjeta", null);
        }else{
            respuesta.put("tarjeta", form);
        }

        respuesta.put("status", status);
        respuesta.put("mensaje", mensaje);

        return new ResponseEntity<Map<String,Object>>(respuesta, HttpStatus.OK);

    }

    public boolean validarTarjeta(Tarjeta t){

        if(t == null || t.isActive() == false){
            return false;
        }else{
            return true;
        }

    }

}
