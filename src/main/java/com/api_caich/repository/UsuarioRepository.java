package com.api_caich.repository;

import com.api_caich.entity.Usuario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndUsuarioIdNot(String email, Long usuarioId);

    List<Usuario> findByRol(String rol);

    List<Usuario> findByEstado(String estado);

    List<Usuario> findByRolAndEstado(String rol, String estado);

    Page<Usuario> findByRol(String rol, Pageable pageable);

    Page<Usuario> findByEstado(String estado, Pageable pageable);

    Page<Usuario> findByRolAndEstado(String rol, String estado, Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE u.rol = :rol AND u.estado = 'Activo'")
    List<Usuario> findActivosByRol(@Param("rol") String rol);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol = 'Paciente' AND u.fechaCreacion >= DATEADD(DAY, -30, CURRENT_TIMESTAMP)")
    Long countNuevosPacientesUltimoMes();

    @Query("SELECT u FROM Usuario u WHERE " +
            "LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.folio) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.idAlumno) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Usuario> searchUsuarios(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.estado = 'Activo'")
    Long countUsuariosActivos();
}