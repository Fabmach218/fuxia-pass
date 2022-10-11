package com.app.pasarela.util;

public class Methods {
    
    public static long generarAleatorio(long min, long max){
        return (long)(Math.random() * (max - min + 1) + min);
    }

}
