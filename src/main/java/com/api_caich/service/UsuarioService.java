package com.api_caich.service;

import com.api_caich.dto.usuario.*;
import com.api_caich.entity.Usuario;
import com.api_caich.enums.Role;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final FileStorageService fileStorageService;

    public Page<UsuarioDTO> obtenerTodosUsuarios(Pageable pageable, String rol, String estado) {
        Page<Usuario> usuarios;

        if (rol != null && estado != null) {
            // Filtrar por rol y estado
            usuarios = usuarioRepository.findByRolAndEstado(rol, estado, pageable);
        } else if (rol != null) {
            // Filtrar solo por rol
            usuarios = usuarioRepository.findByRol(rol, pageable);
        } else if (estado != null) {
            // Filtrar solo por estado
            usuarios = usuarioRepository.findByEstado(estado, pageable);
        } else {
            // Sin filtros - comportamiento original
            usuarios = usuarioRepository.findAll(pageable);
        }

        return usuarios.map(this::mapToDTO);
    }

    public Page<UsuarioDTO> buscarUsuarios(String search, Pageable pageable) {
        Page<Usuario> usuarios = usuarioRepository.searchUsuarios(search, pageable);
        return usuarios.map(this::mapToDTO);
    }

    public Page<UsuarioDTO> obtenerUsuariosPorRol(String rol, Pageable pageable) {
        validarRol(rol);
        Page<Usuario> usuarios = usuarioRepository.findByRol(rol, pageable);
        return usuarios.map(this::mapToDTO);
    }

    public Page<UsuarioDTO> obtenerUsuariosPorEstado(String estado, Pageable pageable) {
        validarEstado(estado);
        Page<Usuario> usuarios = usuarioRepository.findByEstado(estado, pageable);
        return usuarios.map(this::mapToDTO);
    }

    public List<UsuarioDTO> obtenerUsuariosActivosPorRol(String rol) {
        validarRol(rol);
        List<Usuario> usuarios = usuarioRepository.findActivosByRol(rol);
        return usuarios.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public UsuarioDTO obtenerUsuarioPorId(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));
        return mapToDTO(usuario);
    }

    public UsuarioDTO obtenerUsuarioPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
        return mapToDTO(usuario);
    }

    @Transactional
    public UsuarioDTO crearUsuario(CrearUsuarioDTO crearUsuarioDTO, Long usuarioCreadorId) {
        // Validar email único
        if (usuarioRepository.existsByEmail(crearUsuarioDTO.getEmail())) {
            throw new BusinessException("El email ya está registrado: " + crearUsuarioDTO.getEmail());
        }

        // Validar rol
        Role role = Role.fromNombre(crearUsuarioDTO.getRol());

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(crearUsuarioDTO.getNombreCompleto());
        usuario.setEmail(crearUsuarioDTO.getEmail());
        usuario.setFolio(crearUsuarioDTO.getFolio());
        usuario.setIdAlumno(crearUsuarioDTO.getIdAlumno());
        usuario.setPasswordHash(passwordEncoder.encode(crearUsuarioDTO.getPassword()));
        usuario.setTelefono(crearUsuarioDTO.getTelefono());
        usuario.setRol(role.getNombre());
        usuario.setEstado("Activo");
        usuario.setFoto(crearUsuarioDTO.getFoto());
        usuario.setFechaCreacion(LocalDateTime.now());

        Usuario savedUsuario = usuarioRepository.save(usuario);

        log.info("Usuario creado: {} - {}", savedUsuario.getEmail(), savedUsuario.getRol());

        // Auditoría
        auditoriaService.registrarCambio("Usuarios", savedUsuario.getUsuarioId(), "Crear",
                "Todos los campos", null, null, usuarioCreadorId, "SYSTEM");

        return mapToDTO(savedUsuario);
    }

    @Transactional
    public UsuarioDTO actualizarUsuario(Long usuarioId, ActualizarUsuarioDTO actualizarUsuarioDTO,
            Long usuarioActualizadorId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        String cambios = "";

        // Validar y actualizar email si es diferente
        if (actualizarUsuarioDTO.getEmail() != null &&
                !actualizarUsuarioDTO.getEmail().equals(usuario.getEmail())) {

            if (usuarioRepository.existsByEmailAndUsuarioIdNot(actualizarUsuarioDTO.getEmail(), usuarioId)) {
                throw new BusinessException("El email ya está registrado: " + actualizarUsuarioDTO.getEmail());
            }
            cambios += "Email: " + usuario.getEmail() + " -> " + actualizarUsuarioDTO.getEmail() + "; ";
            usuario.setEmail(actualizarUsuarioDTO.getEmail());
        }

        // Actualizar otros campos
        if (actualizarUsuarioDTO.getNombreCompleto() != null) {
            cambios += "NombreCompleto: " + usuario.getNombreCompleto() + " -> "
                    + actualizarUsuarioDTO.getNombreCompleto() + "; ";
            usuario.setNombreCompleto(actualizarUsuarioDTO.getNombreCompleto());
        }

        if (actualizarUsuarioDTO.getFolio() != null) {
            cambios += "Folio: " + usuario.getFolio() + " -> " + actualizarUsuarioDTO.getFolio() + "; ";
            usuario.setFolio(actualizarUsuarioDTO.getFolio());
        }

        if (actualizarUsuarioDTO.getIdAlumno() != null) {
            cambios += "IdAlumno: " + usuario.getIdAlumno() + " -> " + actualizarUsuarioDTO.getIdAlumno() + "; ";
            usuario.setIdAlumno(actualizarUsuarioDTO.getIdAlumno());
        }

        if (actualizarUsuarioDTO.getTelefono() != null) {
            cambios += "Telefono: " + usuario.getTelefono() + " -> " + actualizarUsuarioDTO.getTelefono() + "; ";
            usuario.setTelefono(actualizarUsuarioDTO.getTelefono());
        }

        if (actualizarUsuarioDTO.getRol() != null) {
            Role newRole = Role.fromNombre(actualizarUsuarioDTO.getRol());
            cambios += "Rol: " + usuario.getRol() + " -> " + newRole.getNombre() + "; ";
            usuario.setRol(newRole.getNombre());
        }

        if (actualizarUsuarioDTO.getFoto() != null) {
            cambios += "Foto actualizada; ";
            usuario.setFoto(actualizarUsuarioDTO.getFoto());
        }

        Usuario updatedUsuario = usuarioRepository.save(usuario);

        log.info("Usuario actualizado: {} - Cambios: {}", updatedUsuario.getEmail(), cambios);

        // Auditoría si hubo cambios
        if (!cambios.isEmpty()) {
            auditoriaService.registrarCambio("Usuarios", usuarioId, "Actualizar",
                    cambios, null, null, usuarioActualizadorId, "SYSTEM");
        }

        return mapToDTO(updatedUsuario);
    }

    @Transactional
    public UsuarioDTO cambiarEstadoUsuario(Long usuarioId, CambiarEstadoDTO cambiarEstadoDTO,
            Long usuarioActualizadorId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        validarEstado(cambiarEstadoDTO.getEstado());

        String estadoAnterior = usuario.getEstado();
        usuario.setEstado(cambiarEstadoDTO.getEstado());

        Usuario updatedUsuario = usuarioRepository.save(usuario);

        log.info("Estado de usuario cambiado: {} - {} -> {}",
                usuario.getEmail(), estadoAnterior, cambiarEstadoDTO.getEstado());

        // Auditoría
        String cambios = "Estado: " + estadoAnterior + " -> " + cambiarEstadoDTO.getEstado();
        if (cambiarEstadoDTO.getMotivo() != null) {
            cambios += "; Motivo: " + cambiarEstadoDTO.getMotivo();
        }

        auditoriaService.registrarCambio("Usuarios", usuarioId, "Actualizar",
                cambios, null, null, usuarioActualizadorId, "SYSTEM");

        return mapToDTO(updatedUsuario);
    }

    @Transactional
    public void cambiarPassword(Long usuarioId, CambiarPasswordDTO cambiarPasswordDTO, Long usuarioActualizadorId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        // Verificar contraseña actual
        if (!passwordEncoder.matches(cambiarPasswordDTO.getCurrentPassword(), usuario.getPasswordHash())) {
            throw new BusinessException("La contraseña actual es incorrecta");
        }

        // Actualizar contraseña
        usuario.setPasswordHash(passwordEncoder.encode(cambiarPasswordDTO.getNewPassword()));
        usuarioRepository.save(usuario);

        log.info("Contraseña cambiada para usuario: {}", usuario.getEmail());

        // Auditoría
        auditoriaService.registrarCambio("Usuarios", usuarioId, "Actualizar",
                "Password cambiado", null, null, usuarioActualizadorId, "SYSTEM");
    }

    @Transactional
    public void eliminarUsuario(Long usuarioId, Long usuarioEliminadorId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        // Verificar si el usuario tiene datos relacionados antes de eliminar
        if (!usuario.getTicketsPaciente().isEmpty() ||
                !usuario.getTicketsTerapeuta().isEmpty() ||
                !usuario.getTicketsCreados().isEmpty()) {
            throw new BusinessException("No se puede eliminar el usuario porque tiene datos relacionados");
        }

        usuarioRepository.delete(usuario);

        log.info("Usuario eliminado: {} - {}", usuario.getEmail(), usuario.getRol());

        // Auditoría
        auditoriaService.registrarCambio("Usuarios", usuarioId, "Eliminar",
                "Usuario eliminado", null, null, usuarioEliminadorId, "SYSTEM");
    }

    @Transactional
    public FotoPerfilDTO actualizarFotoPerfil(Long usuarioId, MultipartFile archivo, Long usuarioActualizadorId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Validar que el usuario está actualizando su propia foto o es admin
        if (!usuarioId.equals(usuarioActualizadorId) &&
                !Role.fromNombre(obtenerUsuarioPorId(usuarioActualizadorId).getRol()).equals(Role.ADMINISTRADOR)) {
            throw new BusinessException("No tiene permisos para actualizar la foto de este usuario");
        }

        // Eliminar foto anterior si existe
        if (usuario.getFoto() != null && !usuario.getFoto().isEmpty()) {
            fileStorageService.eliminarFotoAnterior(usuario.getFoto());
        }

        // Almacenar nueva foto
        String nuevaFotoUrl = fileStorageService.almacenarFotoPerfil(archivo, usuarioId);
        usuario.setFoto(nuevaFotoUrl);

        usuarioRepository.save(usuario);

        log.info("Foto de perfil actualizada para usuario: {}", usuario.getEmail());

        // Auditoría
        auditoriaService.registrarCambio("Usuarios", usuarioId, "Actualizar",
                "Foto de perfil actualizada", null, null, usuarioActualizadorId, "SYSTEM");

        return FotoPerfilDTO.builder()
                .fotoUrl(nuevaFotoUrl)
                .mensaje("Foto de perfil actualizada exitosamente")
                .build();
    }

    @Transactional
    public void eliminarFotoPerfil(Long usuarioId, Long usuarioActualizadorId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Validar permisos
        if (!usuarioId.equals(usuarioActualizadorId) &&
                !Role.fromNombre(obtenerUsuarioPorId(usuarioActualizadorId).getRol()).equals(Role.ADMINISTRADOR)) {
            throw new BusinessException("No tiene permisos para eliminar la foto de este usuario");
        }

        if (usuario.getFoto() != null && !usuario.getFoto().isEmpty()) {
            fileStorageService.eliminarFotoAnterior(usuario.getFoto());
            usuario.setFoto(null);
            usuarioRepository.save(usuario);

            log.info("Foto de perfil eliminada para usuario: {}", usuario.getEmail());

            // Auditoría
            auditoriaService.registrarCambio("Usuarios", usuarioId, "Actualizar",
                    "Foto de perfil eliminada", null, null, usuarioActualizadorId, "SYSTEM");
        }
    }

    public Long contarUsuariosActivos() {
        return usuarioRepository.countUsuariosActivos();
    }

    public Long contarNuevosPacientesUltimoMes() {
        return usuarioRepository.countNuevosPacientesUltimoMes();
    }

    // Métodos de validación
    private void validarRol(String rol) {
        try {
            Role.fromNombre(rol);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Rol no válido: " + rol);
        }
    }

    private void validarEstado(String estado) {
        if (!List.of("Activo", "Suspendido", "Inactivo").contains(estado)) {
            throw new BusinessException("Estado no válido: " + estado);
        }
    }

    // Mapper
    private UsuarioDTO mapToDTO(Usuario usuario) {
        return UsuarioDTO.builder()
                .usuarioId(usuario.getUsuarioId())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .folio(usuario.getFolio())
                .idAlumno(usuario.getIdAlumno())
                .telefono(usuario.getTelefono())
                .rol(usuario.getRol())
                .estado(usuario.getEstado())
                .foto(usuario.getFoto())
                .ultimaConexion(usuario.getUltimaConexion())
                .fechaCreacion(usuario.getFechaCreacion())
                .build();
    }
}
