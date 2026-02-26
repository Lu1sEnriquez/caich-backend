package com.api_caich.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        try { // <--- FALTABA ESTE TRY
            authenticationManager.authenticate(
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
        if (usuarioRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BusinessException("El email ya esta registrado");
        }

        Role role;
        try {
            role = Role.fromNombre(registerRequest.getRol());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Rol no valido: " + registerRequest.getRol());
        }

        if ("Administrador".equalsIgnoreCase(registerRequest.getRol())) {
            throw new BusinessException("No se puede registrar como Administrador");
        }

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(registerRequest.getNombreCompleto());
        usuario.setEmail(registerRequest.getEmail());
        usuario.setFolio(registerRequest.getFolio());
        usuario.setIdAlumno(registerRequest.getIdAlumno());
        usuario.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        usuario.setTelefono(registerRequest.getTelefono());
        usuario.setRol(role.getNombre());
        usuario.setEstado(EstadoUsuario.ACTIVO.getValor());

        Usuario savedUsuario = usuarioRepository.save(usuario);
        log.info("Usuario registrado exitosamente: {} - {}", savedUsuario.getEmail(), savedUsuario.getRol());

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUsuario.getEmail());
        String accessToken = jwtTokenUtil.generateToken(userDetails); // <--- CORREGIDO "Str ng"
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
        var refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenRequest.getRefreshToken());
        Usuario usuario = refreshToken.getUsuario();

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
        String newAccessToken = jwtTokenUtil.generateToken(userDetails);

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