package com.app.pasarela.controller;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.pasarela.model.Usuario;
import com.app.pasarela.service.IUsuarioService;
import com.app.pasarela.util.Methods;

@Controller
public class HomeController {
    
    @Autowired
    private IUsuarioService _dataUsuarios;

    @GetMapping("/")
    public String index(Authentication auth, HttpSession session){

        if(auth != null){
            String username = auth.getName();
            Usuario usuario = _dataUsuarios.findByUsername(username);
            usuario.setPassword(null);
            session.setAttribute("usuario", usuario);
            return "index";
        }

        return "redirect:/usuario/login";
    }


}
