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

    // TODO: DATEADD es exclusivo de SQL Server. Se reemplazó por aritmética de fechas JPQL compatible.
    // Si se usa PostgreSQL: CURRENT_TIMESTAMP - INTERVAL '30 days'
    // Si se usa SQL Server: DATEADD(DAY, -30, CURRENT_TIMESTAMP)
    // La siguiente query JPQL es portable para ambas bases de datos.
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol = 'Paciente' AND u.fechaCreacion >= :fechaLimite")
    Long countNuevosPacientesUltimoMes(@Param("fechaLimite") java.time.LocalDateTime fechaLimite);

    @Query("SELECT u FROM Usuario u WHERE " +
            "LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.folio) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.idAlumno) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Usuario> searchUsuarios(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.estado = 'Activo'")
    Long countUsuariosActivos();

    /** Busca pacientes por nombre (parcial, sin distinción mayúsculas) para el selector de la reserva alumno. */
    @Query("SELECT u FROM Usuario u WHERE u.rol = 'Paciente' AND u.estado = 'Activo' " +
            "AND LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%')) " +
            "ORDER BY u.nombreCompleto ASC")
    List<Usuario> findPacientesActivosByNombreLike(@Param("nombre") String nombre);

    /** Busca un paciente por nombre completo exacto (sin distinción mayúsculas) para deduplicación. */
    Optional<Usuario> findByNombreCompletoIgnoreCaseAndRol(String nombreCompleto, String rol);
}