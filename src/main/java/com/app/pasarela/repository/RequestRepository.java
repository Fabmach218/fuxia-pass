package com.app.pasarela.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.pasarela.model.Request;

public interface RequestRepository extends JpaRepository<Request, Integer>{
    
}
