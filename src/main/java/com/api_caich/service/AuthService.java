package com.api_caich.service;

import com.api_caich.constants.SecurityConstants;
import com.api_caich.dto.LoginRequestDTO;
import com.api_caich.dto.LoginResponseDTO;
import com.api_caich.dto.RefreshTokenRequestDTO;
import com.api_caich.dto.RegisterRequestDTO;
import com.api_caich.entity.Usuario;
import com.api_caich.enums.EstadoUsuario;
import com.api_caich.enums.Role;
import com.api_caich.exception.BusinessException;
import com.api_caich.repository.UsuarioRepository;
import com.api_caich.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
            String accessToken = jwtTokenUtil.generateToken(userDetails);

            Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

            // Actualizar última conexión
            usuario.setUltimaConexion(java.time.LocalDateTime.now());
            usuarioRepository.save(usuario);

            // Crear refresh token
            var refreshToken = refreshTokenService.createRefreshToken(usuario.getUsuarioId());

            return LoginResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .type(SecurityConstants.TOKEN_TYPE)
                    .usuarioId(usuario.getUsuarioId())
                    .nombreCompleto(usuario.getNombreCompleto())
                    .email(usuario.getEmail())
                    .rol(usuario.getRol())
                    .expiresIn(com.api_caich.constants.AppConstants.JWT_EXPIRATION_SECONDS)
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Intento de login fallido para: {}", loginRequest.getEmail());
            throw new BusinessException(SecurityConstants.ERROR_INVALID_CREDENTIALS);
        }
    }

    @Transactional
    public LoginResponseDTO register(RegisterRequestDTO registerRequest) {
        // Verificar si el email ya existe
        //TODO: Considerar que email puede ser opcional
        if (usuarioRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BusinessException("El email ya esta registrado");
        }

        // Validar rol usando el enum
        Role role;
        try {
            role = Role.fromNombre(registerRequest.getRol());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Rol no valido: " + registerRequest.getRol());
        }

        // Validar que no se registre como Administrador
        //TODO: Revisar si es necesario permitir el registro de administradores
        if ("Administrador".equalsIgnoreCase(registerRequest.getRol())) {
            throw new BusinessException("No se puede registrar como Administrador");
        }

        // Crear nuevo usuario
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(registerRequest.getNombreCompleto());
        usuario.setEmail(registerRequest.getEmail());
        usuario.setFolio(registerRequest.getFolio());
        usuario.setIdAlumno(registerRequest.getIdAlumno());
        usuario.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        usuario.setTelefono(registerRequest.getTelefono());
        usuario.setRol(role.getNombre());
        usuario.setEstado(EstadoUsuario.ACTIVO.getValor());
        // usuario.setFechaRegistro(java.time.LocalDateTime.now());

        Usuario savedUsuario = usuarioRepository.save(usuario);
        log.info("Usuario registrado exitosamente: {} - {}", savedUsuario.getEmail(), savedUsuario.getRol());

        // Autenticar automáticamente después del registro
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUsuario.getEmail());
        String accessToken = jwtTokenUtil.generateToken(userDetails);
        var refreshToken = refreshTokenService.createRefreshToken(savedUsuario.getUsuarioId());

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .type(SecurityConstants.TOKEN_TYPE)
                .usuarioId(savedUsuario.getUsuarioId())
                .nombreCompleto(savedUsuario.getNombreCompleto())
                .email(savedUsuario.getEmail())
                .rol(savedUsuario.getRol())
                .expiresIn(com.api_caich.constants.AppConstants.JWT_EXPIRATION_SECONDS)
                .build();
    }

    @Transactional
    public LoginResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequest) {
        // Verificar el refresh token
        var refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenRequest.getRefreshToken());
        Usuario usuario = refreshToken.getUsuario();

        // Generar nuevo access token
        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
        String newAccessToken = jwtTokenUtil.generateToken(userDetails);

        // Rotar el refresh token (opcional - para mayor seguridad)
        var newRefreshToken = refreshTokenService.createRefreshToken(usuario.getUsuarioId());

        log.info("Token refrescado para usuario: {}", usuario.getEmail());

        return LoginResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .type(SecurityConstants.TOKEN_TYPE)
                .usuarioId(usuario.getUsuarioId())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .expiresIn(com.api_caich.constants.AppConstants.JWT_EXPIRATION_SECONDS)
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }
    }

    @Transactional
    public void logoutAllSessions(Long usuarioId) {
        refreshTokenService.revokeAllUserTokens(usuarioId);
    }
}