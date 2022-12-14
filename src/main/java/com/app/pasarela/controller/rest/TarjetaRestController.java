package com.app.pasarela.controller.rest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.pasarela.integration.reniec.ReniecApi;
import com.app.pasarela.integration.reniec.UserReniec;
import com.app.pasarela.model.Movimiento;
import com.app.pasarela.model.Request;
import com.app.pasarela.model.Tarjeta;
import com.app.pasarela.model.Token;
import com.app.pasarela.model.dto.ModelActivarTarjeta;
import com.app.pasarela.model.dto.ModelBuscarTarjeta;
import com.app.pasarela.model.dto.ModelMovimientos;
import com.app.pasarela.model.dto.ModelPagoAbono;
import com.app.pasarela.model.dto.ModelRespuestaSaldo;
import com.app.pasarela.model.dto.ModelTarjetaCreate;
import com.app.pasarela.model.dto.ModelTransferencia;
import com.app.pasarela.repository.MovimientoRepository;
import com.app.pasarela.repository.RequestRepository;
import com.app.pasarela.repository.TarjetaRepository;
import com.app.pasarela.repository.TokenRepository;
import com.app.pasarela.service.IUsuarioService;
import com.app.pasarela.util.Constants;
import com.app.pasarela.util.Methods;

@RestController
@RequestMapping(value = "api/tarjeta", produces = "application/json")
public class TarjetaRestController {
    
    @Autowired
    private TarjetaRepository _dataTarjetas;

    @Autowired
    private MovimientoRepository _dataMovimientos;

    @Autowired
    private TokenRepository _dataTokens;

    @Autowired
    private RequestRepository _dataRequests;

    @Autowired
    private ReniecApi _reniecApi;

    @Autowired
    private IUsuarioService _dataUsuarios;

    @PostMapping(value = "/crearTarjeta", produces = "application/json")
    public ResponseEntity<Map<String,Object>> crearTarjeta(@RequestHeader(required = true) String apikey, @RequestBody ModelTarjetaCreate form){

        Token t = validarToken(apikey);

        Map<String, Object> respuesta = new HashMap<>();

        String status = "";
        String mensaje = "";

        if(t == null){

            status = "error";
            mensaje = "No est?? autorizado a utilizar este servicio, revise sus credenciales";

            respuesta.put("status", status);
            respuesta.put("tarjeta", null);
            respuesta.put("mensaje", mensaje);

            return new ResponseEntity<Map<String,Object>>(respuesta, HttpStatus.FORBIDDEN);
        }

        UserReniec user = _reniecApi.findExitsUserByDni(form.getDni());

        if(user != null){

            Tarjeta tarjeta = new Tarjeta();

            tarjeta.setDni(form.getDni());
            tarjeta.setTipo(form.getTipo());
            tarjeta.setMoneda(form.getMoneda());

            String nroTarjetaJunto = "";

            if(form.getTipo().equals("V")){
                nroTarjetaJunto = Methods.generarAleatorio(4000000000000000L, 4999999999999999L) + "";   
            }else{
                nroTarjetaJunto = Methods.generarAleatorio(5000000000000000L, 5999999999999999L) + "";
            }
            
            String nroTarjetaFormateado = "";

            for(int i = 0; i < nroTarjetaJunto.length(); i++){
                
                nroTarjetaFormateado += nroTarjetaJunto.charAt(i);

                if((i+1) % 4 == 0){
                    nroTarjetaFormateado += " ";
                }
            }

            nroTarjetaFormateado = nroTarjetaFormateado.trim(); //Quitamos el espacio al final

            String dueDate = Methods.generarAleatorio(1, 12) + "/" + Methods.generarAleatorio(2026, 2029); //Generamos una fecha entre el 2026 y el 2029

            while(dueDate.length() < 7){
                dueDate = "0" + dueDate;
            }

            tarjeta.setDueDate(Methods.obtenerUltimoDiaMes(dueDate));

            String cvv = Methods.generarAleatorio(10, 999) + "";
            
            while(cvv.length() < 3){
                cvv = "0" + cvv;
            }

            String credenciales = nroTarjetaFormateado + "," + dueDate + "," + cvv + "," + form.getNombre().toUpperCase();
            
            tarjeta.setCredenciales(Methods.encodeBase64(credenciales));
            tarjeta.setActive(false);
            tarjeta.setUsuario(_dataUsuarios.findByUsername(form.getDni()));

            Double maxDiario = 0.0;

            if(form.getMoneda().equals("PEN")){
                maxDiario = Constants.maxPENDefault;
            }else{
                maxDiario = Constants.maxUSDDefault;
            }

            tarjeta.setSaldo(0.0);            
            tarjeta.setLimDiario(maxDiario);
            Tarjeta tarjetaCreated = _dataTarjetas.save(tarjeta);

            status = "success";
            mensaje = "Tarjeta creada con ??xito.";

            respuesta.put("status", status);
            respuesta.put("tarjeta", tarjetaCreated);
            respuesta.put("mensaje", mensaje);

            Request r = new Request();

            r.setToken(t);
            r.setFechaHora(new Date());
            r.setHttpMethod("POST");
            r.setAction("crearTarjeta");
            r.setStatus(status);
            _dataRequests.save(r);

            return new ResponseEntity<Map<String,Object>>(respuesta, HttpStatus.CREATED);

        }else{

            status = "error";
            mensaje = "No se encuentra el DNI en la RENIEC.";

            respuesta.put("status", status);
            respuesta.put("tarjeta", null);
            respuesta.put("mensaje", mensaje);

            Request r = new Request();

            r.setToken(t);
            r.setFechaHora(new Date());
            r.setHttpMethod("POST");
            r.setAction("crearTarjeta");
            r.setStatus(status);
            _dataRequests.save(r);

            return new ResponseEntity<Map<String,Object>>(respuesta, HttpStatus.NOT_FOUND);

        }

    }

    @PutMapping(value = "/activar", produces = "application/json")
    public ResponseEntity<String> activar(@RequestHeader(required = true) String apikey, @RequestBody ModelActivarTarjeta form){

        Token t = validarToken(apikey);

        String mensaje = "";

        if(t == null){

            mensaje = "No est?? autorizado a utilizar este servicio, revise sus credenciales.";

            return new ResponseEntity<String>(mensaje, HttpStatus.FORBIDDEN);
        }

        String credenciales = form.getNroTarjeta() + "," + form.getDueMonth() + "/" + form.getDueYear() + "," + form.getCvv() + "," + form.getNombre().toUpperCase();
        String credencialesEncode = Methods.encodeBase64(credenciales);
        Tarjeta tarjeta = _dataTarjetas.findByCredenciales(credencialesEncode);

        Request r = new Request();

        r.setToken(t);
        r.setFechaHora(new Date());
        r.setHttpMethod("PUT");
        r.setAction("activar");

        if(existeTarjeta(tarjeta)){

            tarjeta.setActive(form.isActive());
            _dataTarjetas.save(tarjeta);
            
            if(form.isActive()){
                mensaje = "Tarjeta activada con ??xito.";
            }else{
                mensaje = "Tarjeta desactivada con ??xito.";
            }

            r.setStatus("success");
            _dataRequests.save(r);

            return new ResponseEntity<String>(mensaje, HttpStatus.OK);

        }else{
            mensaje = "La tarjeta no existe o est?? vencida.";
            r.setStatus("error");
            _dataRequests.save(r);
            return new ResponseEntity<String>(mensaje, HttpStatus.NOT_FOUND);
        }

    }

    @PostMapping(value = "/saldo", produces = "application/json")
    public ResponseEntity<ModelRespuestaSaldo> saldo(@RequestHeader(required = true) String apikey, @RequestBody ModelBuscarTarjeta form){

        Token t = validarToken(apikey);

        ModelRespuestaSaldo respuesta = new ModelRespuestaSaldo();

        String status = "";
        String mensaje = "";

        if(t == null){

            status = "error";
            mensaje = "No est?? autorizado a utilizar este servicio, revise sus credenciales.";

            respuesta.setStatus(status);
            respuesta.setMensaje(mensaje);

            return new ResponseEntity<ModelRespuestaSaldo>(respuesta, HttpStatus.FORBIDDEN);
        }

        String credenciales = form.getNroTarjeta() + "," + form.getDueMonth() + "/" + form.getDueYear() + "," + form.getCvv() + "," + form.getNombre().toUpperCase();
        String credencialesEncode = Methods.encodeBase64(credenciales);
        Tarjeta tarjeta = _dataTarjetas.findByCredenciales(credencialesEncode);

        Request r = new Request();

        r.setToken(t);
        r.setFechaHora(new Date());
        r.setHttpMethod("POST");
        r.setAction("saldo");

        if(existeTarjeta(tarjeta)){

            respuesta.setMoneda(tarjeta.getMoneda());
            respuesta.setSaldo(tarjeta.getSaldo());
            respuesta.setActive(tarjeta.isActive());
            status = "success";
            mensaje = "Se obtuvo el saldo de la tarjeta.";
            r.setStatus(status);
            _dataRequests.save(r);

            respuesta.setStatus(status);
            respuesta.setMensaje(mensaje);

            return new ResponseEntity<ModelRespuestaSaldo>(respuesta, HttpStatus.OK);

        }else{
            status = "error";
            mensaje = "La tarjeta no existe o est?? vencida.";
            r.setStatus(status);
            _dataRequests.save(r);

            respuesta.setStatus(status);
            respuesta.setMensaje(mensaje);
            return new ResponseEntity<ModelRespuestaSaldo>(respuesta, HttpStatus.NOT_FOUND);
        }

    }

    @PostMapping(value = "/pagar", produces = "application/json")
    public ResponseEntity<Map<String, Object>> pagar(@RequestHeader(required = true) String apikey, @RequestBody ModelPagoAbono form){

        Token t = validarToken(apikey);

        Map<String, Object> respuesta = new HashMap<>();

        String status = "";
        String mensaje = "";

        if(t == null){

            status = "error";
            mensaje = "No est?? autorizado a utilizar este servicio, revise sus credenciales.";

            respuesta.put("status", status);
            respuesta.put("tarjeta", null);
            respuesta.put("mensaje", mensaje);

            return new ResponseEntity<Map<String,Object>>(respuesta, HttpStatus.FORBIDDEN);
        }

        String credenciales = form.getNroTarjeta() + "," + form.getDueMonth() + "/" + form.getDueYear() + "," + form.getCvv() + "," + form.getNombre().toUpperCase();
        String credencialesEncode = Methods.encodeBase64(credenciales);
        Tarjeta tarjeta = _dataTarjetas.findByCredenciales(credencialesEncode);

        if(validarTarjeta(tarjeta)){

            Double montoGastadoHoy = _dataMovimientos.getSumMontoTarjetaHoy(tarjeta.getId());
            if(montoGastadoHoy == null){ montoGastadoHoy = 0.0;}

            if(form.getMoneda().equals(tarjeta.getMoneda())){
            
                if(tarjeta.getSaldo() >= form.getMonto()){

                    if(montoGastadoHoy + form.getMonto() <= tarjeta.getLimDiario()){

                        Movimiento m = new Movimiento();
                        m.setTarjeta(tarjeta);
                        m.setMonto(form.getMonto() * -1);
                        m.setFechaHora(new Date());
                        m.setTipo("P");
                        m.setDescripcion(form.getDescripcion());
                        _dataMovimientos.save(m);

                        tarjeta.setSaldo(tarjeta.getSaldo() - form.getMonto());
                        _dataTarjetas.save(tarjeta);
                        status = "success";
                        mensaje = "Pago realizado con ??xito!!!";

                    }else{
                        status = "error";
                        mensaje = "Ha superado su l??mite diario, no se puede procesar el pago!!!";
                    }

                }else{
                    status = "error";
                    mensaje = "No tiene suficiente saldo para pagar.";
                }

            }else{

                status = "reload";
                String moneda = tarjeta.getMoneda();
                form.setMoneda(moneda);

                if(moneda.equals("USD")){ //La moneda de la tarjeta es d??lares, por lo que hay que convertir el monto del formulario a d??lares.
                    Double monto = form.getMonto() / form.getTcCompra(); //El banco est?? comprando d??lares, por lo que se aplica el TC de compra.
                    monto = Math.rint(monto * 100) / 100;
                    form.setMonto(monto);
                }else{ //Y viceversa
                    Double monto = form.getMonto() * form.getTcVenta(); //El banco est?? vendiendo d??lares, por lo que se aplica el TC de venta.
                    monto = Math.rint(monto * 100) / 100;
                    form.setMonto(monto);
                } //Siempre el usuario paga un poco m??s al banco, este nunca pierde.

                mensaje = "Se recalcul?? el monto total en " + moneda + ". Vuelva a hacer clic para procesar el pago.";

            }
            
            

        }else{

            status = "error";
            mensaje = "No se encuentra la tarjeta, verifique que los datos est??n correctos y que su tarjeta est?? activa para compras por internet.";

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

        Map<String, Object> respuesta = new HashMap<>();

        String status = "";
        String mensaje = "";

        if(t == null){

            status = "error";
            mensaje = "No est?? autorizado a utilizar este servicio, revise sus credenciales.";

            respuesta.put("status", status);
            respuesta.put("tarjeta", null);
            respuesta.put("mensaje", mensaje);
            return new ResponseEntity<Map<String,Object>>(respuesta, HttpStatus.FORBIDDEN);
        }

        String credenciales = form.getNroTarjeta() + "," + form.getDueMonth() + "/" + form.getDueYear() + "," + form.getCvv() + "," + form.getNombre().toUpperCase();
        String credencialesEncode = Methods.encodeBase64(credenciales);
        Tarjeta tarjeta = _dataTarjetas.findByCredenciales(credencialesEncode);

        if(validarTarjeta(tarjeta)){

            if(form.getMoneda().equals(tarjeta.getMoneda())){

                Movimiento m = new Movimiento();
                m.setTarjeta(tarjeta);
                m.setMonto(form.getMonto());
                m.setFechaHora(new Date());
                m.setTipo("A");
                m.setDescripcion(form.getDescripcion());
                _dataMovimientos.save(m);

                tarjeta.setSaldo(tarjeta.getSaldo() + form.getMonto());
                _dataTarjetas.save(tarjeta);
                status = "success";
                mensaje = "Abono realizado con ??xito!!!";

            }else{

                status = "reload";
                String moneda = tarjeta.getMoneda();
                form.setMoneda(moneda);

                if(moneda.equals("USD")){ //La moneda de la tarjeta es d??lares, por lo que hay que convertir el monto del formulario a d??lares.
                    Double monto = form.getMonto() / form.getTcVenta(); //El banco est?? vendiendo d??lares, por lo que se aplica el TC de venta.
                    monto = Math.rint(monto * 100) / 100;
                    form.setMonto(monto);
                }else{ //Y viceversa
                    Double monto = form.getMonto() * form.getTcCompra(); //El banco est?? comprando d??lares, por lo que se aplica el TC de compra.
                    monto = Math.rint(monto * 100) / 100;
                    form.setMonto(monto);
                } //Siempre el banco da un poco menos al usuario, nunca pierde.

                mensaje = "Se recalcul?? el monto total en " + moneda + ". Vuelva a hacer clic para procesar el abono.";

            }

        }else{

            status = "error";
            mensaje = "No se encuentra la tarjeta, verifique que los datos est??n correctos y que su tarjeta est?? activa para compras por internet.";

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

    @PostMapping(value = "/transferir", produces = "application/json")
    public ResponseEntity<Map<String, Object>> transferir(@RequestHeader(required = true) String apikey, @RequestBody ModelTransferencia form){

        Token t = validarToken(apikey);

        Map<String, Object> respuesta = new HashMap<>();

        String status = "";
        String mensaje = "";

        if(t == null){

            status = "error";
            mensaje = "No est?? autorizado a utilizar este servicio, revise sus credenciales.";

            respuesta.put("status", status);
            respuesta.put("mensaje", mensaje);
            return new ResponseEntity<Map<String,Object>>(respuesta, HttpStatus.FORBIDDEN);
        }

        String credencialesOrigen = form.getNroTarjetaOrigen() + "," + form.getDueMonthOrigen() + "/" + form.getDueYearOrigen() + "," + form.getCvvOrigen() + "," + form.getNombreOrigen().toUpperCase();
        String credencialesOrigenEncode = Methods.encodeBase64(credencialesOrigen);
        Tarjeta tarjetaOrigen = _dataTarjetas.findByCredenciales(credencialesOrigenEncode);

        String credencialesDestino = form.getNroTarjetaDestino() + "," + form.getDueMonthDestino() + "/" + form.getDueYearDestino() + "," + form.getCvvDestino() + "," + form.getNombreDestino().toUpperCase();
        String credencialesDestinoEncode = Methods.encodeBase64(credencialesDestino);
        Tarjeta tarjetaDestino = _dataTarjetas.findByCredenciales(credencialesDestinoEncode);

        boolean validarTarjetas = validarTarjeta(tarjetaOrigen) && validarTarjeta(tarjetaDestino);

        if(validarTarjetas){

            boolean validarMonedas = form.getMonedaOrigen().equals(tarjetaOrigen.getMoneda()) && form.getMonedaDestino().equals(tarjetaDestino.getMoneda());

            if(validarMonedas){

                if(!tarjetaOrigen.equals(tarjetaDestino)){

                    if(tarjetaOrigen.getSaldo() >= form.getMontoOrigen()){

                        Double montoGastadoHoy = _dataMovimientos.getSumMontoTarjetaHoy(tarjetaOrigen.getId());

                        if(montoGastadoHoy + form.getMontoOrigen() <= tarjetaOrigen.getLimDiario()){
                            Movimiento transferenciaOrigen = new Movimiento();
                            transferenciaOrigen.setTarjeta(tarjetaOrigen);
                            transferenciaOrigen.setMonto(form.getMontoOrigen() * -1);
                            transferenciaOrigen.setFechaHora(new Date());
                            transferenciaOrigen.setTipo("T");
                            transferenciaOrigen.setDescripcion(form.getDescripcion());
                            _dataMovimientos.save(transferenciaOrigen);

                            tarjetaOrigen.setSaldo(tarjetaOrigen.getSaldo() - form.getMontoOrigen());
                            _dataTarjetas.save(tarjetaOrigen);

                            Movimiento transferenciaDestino = new Movimiento();
                            transferenciaDestino.setTarjeta(tarjetaDestino);
                            transferenciaDestino.setMonto(form.getMontoDestino());
                            transferenciaDestino.setFechaHora(new Date());
                            transferenciaDestino.setTipo("T");
                            transferenciaDestino.setDescripcion(form.getDescripcion());
                            _dataMovimientos.save(transferenciaDestino);

                            tarjetaDestino.setSaldo(tarjetaDestino.getSaldo() + form.getMontoDestino());
                            _dataTarjetas.save(tarjetaDestino);

                            status = "success";
                            mensaje = "Transferencia realizada exitosamente!!!";
                        }else{
                            status = "error";
                            mensaje = "L??mite diario superado!!!";    
                        }

                        
                    }else{
                        status = "error";
                        mensaje = "Saldo insuficiente!!!";    
                    }

                }else{
                    status = "error";
                    mensaje = "No se puede transferir a su misma cuenta!!!";
                }

            }else{

                status = "error";

                if(!form.getMonedaOrigen().equals(tarjetaOrigen.getMoneda())){
                    mensaje = "La moneda de la cuenta de origen no coincide!!!";
                }else{
                    mensaje = "La moneda de la cuenta de destino no coincide!!!";
                }

            }

        }else{

            status = "error";
            
            if(!validarTarjeta(tarjetaOrigen)){
                mensaje = "No se encuentra la tarjeta de origen!!!";
            }else{
                mensaje = "No se encuentra la tarjeta de destino!!!";
            }

        }

        respuesta.put("status", status);
        respuesta.put("mensaje", mensaje);

        Request r = new Request();

        r.setToken(t);
        r.setFechaHora(new Date());
        r.setHttpMethod("POST");
        r.setAction("transferir");
        r.setStatus(status);
        _dataRequests.save(r);

        return new ResponseEntity<Map<String,Object>>(respuesta, HttpStatus.OK);

    }

    @PostMapping(value = "/movimientos", produces = "application/json")
    public ResponseEntity<Map<String, Object>> movimientos(@RequestHeader(required = true) String apikey, @RequestBody ModelMovimientos form) throws ParseException{

        Token t = validarToken(apikey);

        Map<String, Object> respuesta = new HashMap<>();

        String status = "";
        String mensaje = "";

        if(t == null){

            status = "error";
            mensaje = "No est?? autorizado a utilizar este servicio, revise sus credenciales.";

            respuesta.put("status", status);
            respuesta.put("mensaje", mensaje);
            return new ResponseEntity<Map<String,Object>>(respuesta, HttpStatus.FORBIDDEN);
        }

        String credenciales = form.getNroTarjeta() + "," + form.getDueMonth() + "/" + form.getDueYear() + "," + form.getCvv() + "," + form.getNombre().toUpperCase();
        String credencialesEncode = Methods.encodeBase64(credenciales);
        Tarjeta tarjeta = _dataTarjetas.findByCredenciales(credencialesEncode);

        if(existeTarjeta(tarjeta)){

            if(form.getFechaFin().compareTo(form.getFechaInicio()) >= 0){
                
                List<Movimiento> listaMovimientos = _dataMovimientos.getMovimientosTarjeta(tarjeta.getId(), form.getFechaInicio() + " 00:00:00", form.getFechaFin() + " 23:59:59");

                Double saldoInicial = _dataMovimientos.getSaldoInicialFecha(tarjeta.getId(), form.getFechaInicio() + " 00:00:00");
                Double saldoFinal = _dataMovimientos.getSaldoFinalFecha(tarjeta.getId(), form.getFechaFin() + " 23:59:59");

                Movimiento mSaldoFinal = new Movimiento();
                mSaldoFinal.setDescripcion("Saldo Final");
                mSaldoFinal.setFechaHora(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(form.getFechaFin() + " 23:59:59"));
                mSaldoFinal.setMonto(saldoFinal);
                mSaldoFinal.setTarjeta(tarjeta);
                mSaldoFinal.setTipo("F");

                Movimiento mSaldoInicial = new Movimiento();
                mSaldoInicial.setDescripcion("Saldo Inicial");
                mSaldoInicial.setFechaHora(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(form.getFechaInicio() + " 00:00:00"));
                mSaldoInicial.setMonto(saldoInicial);
                mSaldoInicial.setTarjeta(tarjeta);
                mSaldoInicial.setTipo("I");

                if(listaMovimientos != null){
                    listaMovimientos.add(0, mSaldoFinal);
                    listaMovimientos.add(mSaldoInicial);
                }else{
                    listaMovimientos = new ArrayList<Movimiento>();
                    listaMovimientos.add(mSaldoFinal);
                    listaMovimientos.add(mSaldoInicial);
                }

                status = "success";
                mensaje = "Se calcularon los movimientos entre las fechas especificadas!!!";

                respuesta.put("movimientos", listaMovimientos);

            }else{
                status = "error";
                mensaje = "La fecha de inicio no puede ser mayor a la de fin!!!";
            }

        }else{

            status = "error";
            mensaje = "La tarjeta no existe o est?? vencida!!!";

        }

        respuesta.put("status", status);
        respuesta.put("mensaje", mensaje);

        Request r = new Request();

        r.setToken(t);
        r.setFechaHora(new Date());
        r.setHttpMethod("POST");
        r.setAction("movimientos");
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

    public boolean existeTarjeta(Tarjeta t){

        if(t == null || new Date().after(t.getDueDate())){
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
