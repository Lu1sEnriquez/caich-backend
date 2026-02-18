package com.api_caich.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.usuario.ActualizarUsuarioDTO;
import com.api_caich.dto.usuario.CambiarEstadoDTO;
import com.api_caich.dto.usuario.CambiarPasswordDTO;
import com.api_caich.dto.usuario.CrearUsuarioDTO;
import com.api_caich.dto.usuario.FotoPerfilDTO;
import com.api_caich.dto.usuario.UsuarioDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Users", description = "APIs for user management")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Get paginated list of all users")
    public ResponseEntity<DataResponseDTO<Page<UsuarioDTO>>> obtenerTodosUsuarios(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "nombreCompleto") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String direction,
            @Parameter(description = "Role filter") @RequestParam(required = false) String rol,
            @Parameter(description = "Status filter") @RequestParam(required = false) String estado) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UsuarioDTO> usuarios = usuarioService.obtenerTodosUsuarios(pageable, rol, estado);
        return ResponseEntity.ok(DataResponseDTO.success(usuarios, "Users retrieved successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search users", description = "Search users by name, email, folio or student ID")
    public ResponseEntity<DataResponseDTO<Page<UsuarioDTO>>> buscarUsuarios(
            @Parameter(description = "Search term") @RequestParam String search,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombreCompleto"));
        Page<UsuarioDTO> usuarios = usuarioService.buscarUsuarios(search, pageable);
        return ResponseEntity.ok(DataResponseDTO.success(usuarios, "Search results retrieved"));
    }

    @GetMapping("/rol/{rol}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get users by role", description = "Get paginated list of users by role")
    public ResponseEntity<DataResponseDTO<Page<UsuarioDTO>>> obtenerUsuariosPorRol(
            @Parameter(description = "User role") @PathVariable String rol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombreCompleto"));
        Page<UsuarioDTO> usuarios = usuarioService.obtenerUsuariosPorRol(rol, pageable);
        return ResponseEntity.ok(DataResponseDTO.success(usuarios, "Users by role retrieved"));
    }

    @GetMapping("/rol/{rol}/activos")
    @PreAuthorize("hasAnyRole('ADMIN', 'TERAPEUTA')")
    @Operation(summary = "Get active users by role", description = "Get list of active users by role")
    public ResponseEntity<DataResponseDTO<List<UsuarioDTO>>> obtenerUsuariosActivosPorRol(
            @Parameter(description = "User role") @PathVariable String rol) {

        List<UsuarioDTO> usuarios = usuarioService.obtenerUsuariosActivosPorRol(rol);
        return ResponseEntity.ok(DataResponseDTO.success(usuarios, "Active users by role retrieved"));
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get users by status", description = "Get paginated list of users by status")
    public ResponseEntity<DataResponseDTO<Page<UsuarioDTO>>> obtenerUsuariosPorEstado(
            @Parameter(description = "User status") @PathVariable String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombreCompleto"));
        Page<UsuarioDTO> usuarios = usuarioService.obtenerUsuariosPorEstado(estado, pageable);
        return ResponseEntity.ok(DataResponseDTO.success(usuarios, "Users by status retrieved"));
    }

    @GetMapping("/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN') or #usuarioId == authentication.principal.id")
    @Operation(summary = "Get user by ID", description = "Get user details by ID")
    public ResponseEntity<DataResponseDTO<UsuarioDTO>> obtenerUsuarioPorId(
            @Parameter(description = "User ID") @PathVariable Long usuarioId) {

        UsuarioDTO usuario = usuarioService.obtenerUsuarioPorId(usuarioId);
        return ResponseEntity.ok(DataResponseDTO.success(usuario, "User retrieved successfully"));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by email", description = "Get user details by email")
    public ResponseEntity<DataResponseDTO<UsuarioDTO>> obtenerUsuarioPorEmail(
            @Parameter(description = "User email") @PathVariable String email) {

        UsuarioDTO usuario = usuarioService.obtenerUsuarioPorEmail(email);
        return ResponseEntity.ok(DataResponseDTO.success(usuario, "User retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create a new user")
    public ResponseEntity<DataResponseDTO<UsuarioDTO>> crearUsuario(
            @Valid @RequestBody CrearUsuarioDTO crearUsuarioDTO,
            @AuthenticationPrincipal CustomUserDetails authentication) {
        Long usuarioCreadorId = authentication.getId();
        UsuarioDTO usuario = usuarioService.crearUsuario(crearUsuarioDTO, usuarioCreadorId);
        return ResponseEntity.ok(DataResponseDTO.success(usuario, "User created successfully"));
    }

    @PutMapping("/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN') or #usuarioId == authentication.principal.id")
    @Operation(summary = "Update user", description = "Update user details")
    public ResponseEntity<DataResponseDTO<UsuarioDTO>> actualizarUsuario(
            @Parameter(description = "User ID") @PathVariable Long usuarioId,
            @Valid @RequestBody ActualizarUsuarioDTO actualizarUsuarioDTO,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioActualizadorId = authentication.getId();
        UsuarioDTO usuario = usuarioService.actualizarUsuario(usuarioId, actualizarUsuarioDTO, usuarioActualizadorId);
        return ResponseEntity.ok(DataResponseDTO.success(usuario, "User updated successfully"));
    }

    @PutMapping("/{usuarioId}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change user status", description = "Change user status (Activo, Suspendido, Inactivo)")
    public ResponseEntity<DataResponseDTO<UsuarioDTO>> cambiarEstadoUsuario(
            @Parameter(description = "User ID") @PathVariable Long usuarioId,
            @Valid @RequestBody CambiarEstadoDTO cambiarEstadoDTO,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioActualizadorId = authentication.getId();
        UsuarioDTO usuario = usuarioService.cambiarEstadoUsuario(usuarioId, cambiarEstadoDTO, usuarioActualizadorId);
        return ResponseEntity.ok(DataResponseDTO.success(usuario, "User status updated successfully"));
    }

    @PutMapping("/{usuarioId}/password")
    @PreAuthorize("hasRole('ADMIN') or #usuarioId == authentication.principal.id")
    @Operation(summary = "Change password", description = "Change user password")
    public ResponseEntity<BaseResponseDTO> cambiarPassword(
            @Parameter(description = "User ID") @PathVariable Long usuarioId,
            @Valid @RequestBody CambiarPasswordDTO cambiarPasswordDTO,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioActualizadorId = authentication.getId();
        usuarioService.cambiarPassword(usuarioId, cambiarPasswordDTO, usuarioActualizadorId);
        return ResponseEntity.ok(BaseResponseDTO.success("Password changed successfully"));
    }

    @DeleteMapping("/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Delete user (only if no related data exists)")
    public ResponseEntity<BaseResponseDTO> eliminarUsuario(
            @Parameter(description = "User ID") @PathVariable Long usuarioId,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioEliminadorId = authentication.getId();
        usuarioService.eliminarUsuario(usuarioId, usuarioEliminadorId);
        return ResponseEntity.ok(BaseResponseDTO.success("User deleted successfully"));
    }

    @GetMapping("/stats/activos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get active users count", description = "Get count of active users")
    public ResponseEntity<DataResponseDTO<Long>> contarUsuariosActivos() {
        Long count = usuarioService.contarUsuariosActivos();
        return ResponseEntity.ok(DataResponseDTO.success(count, "Active users count retrieved"));
    }

    @GetMapping("/stats/nuevos-pacientes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get new patients count", description = "Get count of new patients in last 30 days")
    public ResponseEntity<DataResponseDTO<Long>> contarNuevosPacientesUltimoMes() {
        Long count = usuarioService.contarNuevosPacientesUltimoMes();
        return ResponseEntity.ok(DataResponseDTO.success(count, "New patients count retrieved"));
    }

    @PostMapping("/{usuarioId}/foto")
    @PreAuthorize("hasRole('ADMIN') or #usuarioId == authentication.principal.id")
    @Operation(summary = "Upload profile photo", description = "Upload and update user profile photo")
    public ResponseEntity<DataResponseDTO<FotoPerfilDTO>> subirFotoPerfil(
            @Parameter(description = "User ID") @PathVariable Long usuarioId,
            @Parameter(description = "Profile photo file") @RequestParam("foto") MultipartFile archivo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        FotoPerfilDTO resultado = usuarioService.actualizarFotoPerfil(
                usuarioId, archivo, userDetails.getId());

        return ResponseEntity.ok(DataResponseDTO.success(resultado, "Profile photo updated successfully"));
    }

    @DeleteMapping("/{usuarioId}/foto")
    @PreAuthorize("hasRole('ADMIN') or #usuarioId == authentication.principal.id")
    @Operation(summary = "Delete profile photo", description = "Delete user profile photo")
    public ResponseEntity<BaseResponseDTO> eliminarFotoPerfil(
            @Parameter(description = "User ID") @PathVariable Long usuarioId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        usuarioService.eliminarFotoPerfil(usuarioId, userDetails.getId());
        return ResponseEntity.ok(BaseResponseDTO.success("Profile photo deleted successfully"));
    }
}
