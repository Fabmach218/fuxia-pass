package com.app.pasarela.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.pasarela.integration.reniec.ReniecApi;
import com.app.pasarela.integration.reniec.UserReniec;
import com.app.pasarela.model.Usuario;
import com.app.pasarela.service.IUsuarioService;

@Controller
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private IUsuarioService _dataUsuarios;  

    @Autowired
    private ReniecApi _reniecApi;

    @RequestMapping(value = "/validarDNI", method = RequestMethod.GET)
    public String validarDNI(Model model){
        return "usuario/formDNI";
    }

    @RequestMapping(value = "/validarDNI", method = RequestMethod.POST)
    public String validarDNI(Model model, @Valid String dni, RedirectAttributes redirectAttributes){
        
        UserReniec usuarioReniec = null; 
        usuarioReniec = _reniecApi.findExitsUserByDni(dni);
        
        if(usuarioReniec != null){
            
            if(_dataUsuarios.findByUsername(dni) == null){

                Usuario usuario = new Usuario();
                usuario.setUsername(dni);
                usuario.setNombres(usuarioReniec.getNombres());
                usuario.setApellidos(usuarioReniec.getApePat() + " " + usuarioReniec.getApeMat());

                redirectAttributes.addFlashAttribute("usuario", usuario);
                redirectAttributes.addFlashAttribute("mensaje", "DNI validado, puede proceder a crear la tarjeta");
                return "redirect:/usuario/create";

            }else{

                model.addAttribute("mensaje", "Ya existe un usuario con ese número de DNI!!!");
                return "usuario/formDNI";

            }

        }else{

            model.addAttribute("mensaje", "El DNI no está registrado en la RENIEC!!!");
            return "usuario/formDNI";
        }

    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String create(Model model) {
        model.addAttribute("title", "Registro de usuario");
        /*
        Usuario usuario = new Usuario();
        usuario.setUsername("Admin123");
        usuario.setTipoUsuario("A");
        model.addAttribute("usuario", usuario);
         */ //Descomentar para registrar un administrador 
        return "usuario/create";
    } 
    
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String createPost(Model model, @Valid @ModelAttribute Usuario usuario, BindingResult result, RedirectAttributes redirectAttributes ){
        model.addAttribute("title", "Registro de usuario");
        if(result.hasFieldErrors()) {
            redirectAttributes.addFlashAttribute("mensaje", "No se registro un cliente");
            return "redirect:/usuario/create";
        }else{
            _dataUsuarios.registrar(usuario);
            model.addAttribute("usuario", usuario);
        }
        return "redirect:/usuario/login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(Model model) {
        model.addAttribute("title", "Inicio de sesión");
        model.addAttribute("usuario", new Usuario());
        return "usuario/login";
    }  

}
