package com.app.pasarela.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.pasarela.integration.reniec.ReniecApi;
import com.app.pasarela.integration.reniec.UserReniec;
import com.app.pasarela.model.Tarjeta;
import com.app.pasarela.model.dto.ModelPagoAbono;
import com.app.pasarela.model.dto.ModelRespuestaPagoAbono;
import com.app.pasarela.model.dto.ModelRespuestaTarjeta;
import com.app.pasarela.model.dto.ModelTarjetaCreate;
import com.app.pasarela.repository.TarjetaRepository;
import com.app.pasarela.util.Constants;
import com.app.pasarela.util.Methods;

@Controller
@RequestMapping("/tarjeta")
public class TarjetaController {
    
    @Autowired
    private ReniecApi _reniecApi;

    @Autowired
    private RestTemplate _restTemplate;

    @Autowired
    private TarjetaRepository _dataTarjetas;

    @GetMapping("/validarDNI")
    public String validarDNI(Model model){
        return "tarjeta/formDNI";
    }

    @PostMapping("/validarDNI")
    public String create(Model model, @Valid String dni, RedirectAttributes redirectAttributes){
        
        UserReniec usuario = null; 
        usuario = _reniecApi.findExitsUserByDni(dni);
        
        if(usuario != null){
            
            ModelTarjetaCreate tarjeta = new ModelTarjetaCreate();
            tarjeta.setDni(dni);
            
            String nombre = "";

            if(usuario.getNombres().split(" ")[0].length() < 7){
                nombre = usuario.getNombres().split(" ")[0];
            }else{
                nombre = usuario.getNombres().charAt(0) + ".";
            }

            nombre += " " + usuario.getApePat() + " " + usuario.getApeMat().charAt(0) + ".";
            nombre.toUpperCase();
            tarjeta.setNombre(nombre);

            redirectAttributes.addFlashAttribute("tarjeta", tarjeta);
            redirectAttributes.addFlashAttribute("mensaje", "DNI validado, puede proceder a crear la tarjeta");
            return "redirect:/tarjeta/create";

        }else{

            model.addAttribute("mensaje", "El DNI no está registrado en la RENIEC!!!");
            return "tarjeta/formDNI";
        }

    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String create(Model model){
        return "tarjeta/create";
    }

    @RequestMapping(value = "/submitCreate", method = RequestMethod.POST)
    public String submitCreate(Model model, @Valid ModelTarjetaCreate tarjetaCreate, BindingResult result, RedirectAttributes redirectAttributes){

        if(!result.hasErrors()){

            Tarjeta tarjeta = new Tarjeta();

            String nroTarjetaJunto = Methods.generarAleatorio(4000000000000000L, 5999999999999999L) + "";
            
            String nroTarjetaFormateado = "";

            for(int i = 0; i < nroTarjetaJunto.length(); i++){
                
                nroTarjetaFormateado += nroTarjetaJunto.charAt(i);

                if((i+1) % 4 == 0){
                    nroTarjetaFormateado += " ";
                }
            }

            nroTarjetaFormateado = nroTarjetaFormateado.trim(); //Quitamos el espacio al final
            tarjeta.setNroTarjeta(nroTarjetaFormateado); //Guardamos el número de tarjeta con los espacios, para que se pueda usar con la máscara y se vea estético.
            
            if(nroTarjetaFormateado.charAt(0) == '4'){ //Si empieza con 4 es VISA, con 5 es MasterCard
                tarjeta.setTipo("V");
            }else{
                tarjeta.setTipo("M");
            }

            String dueDate = Methods.generarAleatorio(1, 12) + "/" + Methods.generarAleatorio(2023, 2027); //Generamos una fecha entre el 2023 y el 2027

            while(dueDate.length() < 7){
                dueDate = "0" + dueDate;
            }

            tarjeta.setDueDate(dueDate);

            String cvv = Methods.generarAleatorio(10, 999) + "";
            
            while(cvv.length() < 3){
                cvv = "0" + cvv;
            }

            tarjeta.setCvv(cvv);

            tarjeta.setActive(false);

            tarjeta.setNombre(tarjetaCreate.getNombre());
            tarjeta.setDni(tarjetaCreate.getDni());
            tarjeta.setMoneda(tarjetaCreate.getMoneda());

            tarjeta.setSaldo(0.0);            

            _dataTarjetas.save(tarjeta);

            redirectAttributes.addFlashAttribute("mensaje", "Tarjeta creada con éxito!!!");

            return "redirect:/";

        }else{
            model.addAttribute("mensaje", "Hay errores");
            System.out.println(result.getAllErrors());
            return "tarjeta/create";
        }
        
    }

    @RequestMapping(value = "/pagar", method = RequestMethod.GET)
    public String pagar(Model model){
        model.addAttribute("form", new ModelPagoAbono());
        return "tarjeta/pago";
    }

    @RequestMapping(value = "/pagar", method = RequestMethod.POST)
    public String pagar(Model model, @Valid ModelPagoAbono form, BindingResult result, RedirectAttributes redirectAttributes){

        HttpEntity<Object> entity = new HttpEntity<Object>(form);
        ResponseEntity<ModelRespuestaPagoAbono> responseEntity;

        try{
            
            responseEntity = _restTemplate.exchange(Constants.URLFuxiaPass + Constants.APITarjeta + Constants.pagar, HttpMethod.POST, entity, ModelRespuestaPagoAbono.class);

            ModelRespuestaPagoAbono respuesta = responseEntity.getBody();

            if(respuesta.getStatus().equals("reload")){
                
                model.addAttribute("mensajeRecarga", respuesta.getMensaje());

                ModelPagoAbono form2 = respuesta.getTarjeta();
                
                if(form2.getMoneda().equals("USD")){
                    form2.setMoneda("PEN");
                    form2.setMonto(Math.rint(form2.getMonto() * Constants.tcCompra * 100)/100); //TC Compra
                }else{
                    form2.setMoneda("USD");
                    form2.setMonto(Math.rint(form2.getMonto() / Constants.tcVenta * 100)/100); //TC Venta
                }

                model.addAttribute("form", form2);
                return "tarjeta/pago";
            }

            if(respuesta.getStatus().equals("error")){
                model.addAttribute("mensajeError", respuesta.getMensaje());
                model.addAttribute("form", respuesta.getTarjeta());
                return "tarjeta/pago";
            }

            if(respuesta.getStatus().equals("success")){
                redirectAttributes.addFlashAttribute("status", respuesta.getStatus());
                redirectAttributes.addFlashAttribute("mensaje", respuesta.getMensaje());
                return "redirect:/";
            }

        }catch(Exception e){
            System.out.println(e.getMessage());
        }

        return "redirect:/";
        

    }

    @RequestMapping(value = "/abonar", method = RequestMethod.GET)
    public String abonar(Model model){
        model.addAttribute("form", new ModelPagoAbono());
        return "tarjeta/abono";
    }

    @RequestMapping(value = "/abonar", method = RequestMethod.POST)
    public String abonar(Model model, @Valid ModelPagoAbono form, BindingResult result, RedirectAttributes redirectAttributes){

        HttpEntity<Object> entity = new HttpEntity<Object>(form);
        ResponseEntity<ModelRespuestaPagoAbono> responseEntity;

        try{
            
            responseEntity = _restTemplate.exchange(Constants.URLFuxiaPass + Constants.APITarjeta + Constants.abonar, HttpMethod.POST, entity, ModelRespuestaPagoAbono.class);

            ModelRespuestaPagoAbono respuesta = responseEntity.getBody();

            if(respuesta.getStatus().equals("reload")){
                
                model.addAttribute("mensajeRecarga", respuesta.getMensaje());

                ModelPagoAbono form2 = respuesta.getTarjeta();
                
                if(form2.getMoneda().equals("USD")){
                    form2.setMoneda("PEN");
                    form2.setMonto(Math.rint(form2.getMonto() * Constants.tcCompra * 100)/100); //TC Compra
                }else{
                    form2.setMoneda("USD");
                    form2.setMonto(Math.rint(form2.getMonto() / Constants.tcVenta * 100)/100); //TC Venta
                }

                model.addAttribute("form", form2);
                return "tarjeta/abono";
            }

            if(respuesta.getStatus().equals("error")){
                model.addAttribute("mensajeError", respuesta.getMensaje());
                model.addAttribute("form", respuesta.getTarjeta());
                return "tarjeta/abono";
            }

            if(respuesta.getStatus().equals("success")){
                redirectAttributes.addFlashAttribute("status", respuesta.getStatus());
                redirectAttributes.addFlashAttribute("mensaje", respuesta.getMensaje());
                return "redirect:/";
            }

        }catch(Exception e){
            System.out.println(e.getMessage());
        }

        return "redirect:/";
        

    }

}
