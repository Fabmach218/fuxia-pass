package com.app.pasarela.util;

import java.util.Base64;

public class Methods {
    
    public static long generarAleatorio(long min, long max){
        return (long)(Math.random() * (max - min + 1) + min);
    }

    public static String encodeBase64(String text){
        return Base64.getEncoder().encodeToString(text.getBytes());
    }

    public static String decodeBase64(String base64String){
        return new String(Base64.getDecoder().decode(base64String));
    }

}
