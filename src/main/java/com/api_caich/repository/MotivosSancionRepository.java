package com.api_caich.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api_caich.entity.MotivosSancion;

@Repository
public interface MotivosSancionRepository extends JpaRepository<MotivosSancion, Long> {

}
