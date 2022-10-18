package com.app.pasarela.controller.rest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.pasarela.model.Pago;
import com.app.pasarela.model.Request;
import com.app.pasarela.model.Tarjeta;
import com.app.pasarela.model.Token;
import com.app.pasarela.model.dto.ModelPagoAbono;
import com.app.pasarela.repository.PagoRepository;
import com.app.pasarela.repository.RequestRepository;
import com.app.pasarela.repository.TarjetaRepository;
import com.app.pasarela.repository.TokenRepository;
import com.app.pasarela.util.Methods;

@RestController
@RequestMapping(value = "api/tarjeta", produces = "application/json")
public class TarjetaRestController {
    
    @Autowired
    private TarjetaRepository _dataTarjetas;

    @Autowired
    private PagoRepository _dataPagos;

    @Autowired
    private TokenRepository _dataTokens;

    @Autowired
    private RequestRepository _dataRequests;

    @PostMapping(value = "/pagar", produces = "application/json")
    public ResponseEntity<Map<String, Object>> pagar(@RequestHeader(required = true) String apikey, @RequestBody ModelPagoAbono form){

        Token t = validarToken(apikey);

        if(t == null){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        String credenciales = form.getNroTarjeta() + "," + form.getDueMonth() + "/" + form.getDueYear() + "," + form.getCvv() + "," + form.getNombre().toUpperCase();
        String credencialesEncode = Methods.encodeBase64(credenciales);
        Tarjeta tarjeta = _dataTarjetas.findByCredenciales(credencialesEncode);

        Map<String, Object> respuesta = new HashMap<>();

        String status = "";
        String mensaje = "";

        if(validarTarjeta(tarjeta)){

            Double montoGastadoHoy = _dataPagos.getSumMontoTarjetaHoy(tarjeta.getId());
            if(montoGastadoHoy == null){ montoGastadoHoy = 0.0;}

            if(form.getMoneda().equals(tarjeta.getMoneda())){
            
                if(tarjeta.getSaldo() >= form.getMonto()){

                    if(montoGastadoHoy + form.getMonto() <= tarjeta.getLimDiario()){

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
                        mensaje = "Ha superado su límite diario, no se puede procesar el pago!!!";
                    }

                }else{
                    status = "error";
                    mensaje = "No tiene suficiente saldo para pagar.";
                }

            }else{

                status = "reload";
                String moneda = tarjeta.getMoneda();
                form.setMoneda(moneda);

                if(moneda.equals("USD")){ //La moneda de la tarjeta es dólares, por lo que hay que convertir el monto del formulario a dólares.
                    Double monto = form.getMonto() / form.getTcCompra(); //El banco está comprando dólares, por lo que se aplica el TC de compra.
                    monto = Math.rint(monto * 100) / 100;
                    form.setMonto(monto);
                }else{ //Y viceversa
                    Double monto = form.getMonto() * form.getTcVenta(); //El banco está vendiendo dólares, por lo que se aplica el TC de venta.
                    monto = Math.rint(monto * 100) / 100;
                    form.setMonto(monto);
                } //Siempre el usuario paga un poco más al banco, este nunca pierde.

                mensaje = "Se recalculó el monto total en " + moneda + ". Vuelva a hacer clic para procesar el pago.";

            }
            
            

        }else{

            status = "error";
            mensaje = "No se encuentra la tarjeta, verifique que los datos estén correctos y que su tarjeta esté activa para compras por internet.";

        }

        if(status.equals("success")){
            respuesta.put("tarjeta", null);
        }else{
            respuesta.put("tarjeta", form);
        }

        respuesta.put("status", status);
        respuesta.put("mensaje", mensaje);

        Request r = new Request();

        r.setToken(t);
        r.setFechaHora(new Date());
        r.setHttpMethod("POST");
        r.setAction("pagar");
        r.setStatus(status);
        _dataRequests.save(r);

        return new ResponseEntity<Map<String,Object>>(respuesta, HttpStatus.OK);

    }

    @PostMapping(value = "/abonar", produces = "application/json")
    public ResponseEntity<Map<String, Object>> abonar(@RequestHeader(required = true) String apikey, @RequestBody ModelPagoAbono form){

        Token t = validarToken(apikey);

        if(t == null){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        String credenciales = form.getNroTarjeta() + "," + form.getDueMonth() + "/" + form.getDueYear() + "," + form.getCvv() + "," + form.getNombre().toUpperCase();
        String credencialesEncode = Methods.encodeBase64(credenciales);
        Tarjeta tarjeta = _dataTarjetas.findByCredenciales(credencialesEncode);

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
                form.setMoneda(moneda);

                if(moneda.equals("USD")){ //La moneda de la tarjeta es dólares, por lo que hay que convertir el monto del formulario a dólares.
                    Double monto = form.getMonto() / form.getTcVenta(); //El banco está vendiendo dólares, por lo que se aplica el TC de venta.
                    monto = Math.rint(monto * 100) / 100;
                    form.setMonto(monto);
                }else{ //Y viceversa
                    Double monto = form.getMonto() * form.getTcCompra(); //El banco está comprando dólares, por lo que se aplica el TC de compra.
                    monto = Math.rint(monto * 100) / 100;
                    form.setMonto(monto);
                } //Siempre el banco da un poco menos al usuario, nunca pierde.

                mensaje = "Se recalculó el monto total en " + moneda + ". Vuelva a hacer clic para procesar el abono.";

            }

        }else{

            status = "error";
            mensaje = "No se encuentra la tarjeta, verifique que los datos estén correctos y que su tarjeta esté activa para compras por internet.";

        }

        if(status.equals("success")){
            respuesta.put("tarjeta", null);
        }else{
            respuesta.put("tarjeta", form);
        }

        respuesta.put("status", status);
        respuesta.put("mensaje", mensaje);

        Request r = new Request();

        r.setToken(t);
        r.setFechaHora(new Date());
        r.setHttpMethod("POST");
        r.setAction("abonar");
        r.setStatus(status);
        _dataRequests.save(r);

        return new ResponseEntity<Map<String,Object>>(respuesta, HttpStatus.OK);

    }

    public boolean validarTarjeta(Tarjeta t){

        if(t == null || t.isActive() == false || new Date().after(t.getDueDate())){
            return false;
        }else{
            return true;
        }

    }

    public Token validarToken(String token){

        String tokenEncrypt = Methods.encodeBase64(token);

        Token t = _dataTokens.findTokenVigenteByToken(tokenEncrypt);

        if(t == null){
            return null;
        }

        return t;

    }

}
