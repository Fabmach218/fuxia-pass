package com.app.pasarela.integration.reniec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ReniecApi {
    
    @Autowired
    private RestTemplate restTemplate;


    public UserReniec findExitsUserByDni(String dni){

        UserReniec reniec = null;

        try {

           reniec = restTemplate.getForObject("https://app-reniec.herokuapp.com/api/persona/{dni}", UserReniec.class, dni);

        } catch (Exception e) {
            
            System.out.println("No existe el usuario");
        }
      

        return reniec;

    }
}
