package com.api_caich.service;

import com.api_caich.entity.RefreshToken;
import com.api_caich.entity.Usuario;
import com.api_caich.exception.BusinessException;
import com.api_caich.exception.ResourceNotFoundException;
import com.api_caich.repository.RefreshTokenRepository;
import com.api_caich.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.jwt.refresh-expiration:604800000}") // 7 días por defecto
    private Long refreshTokenDurationMs;

    public RefreshToken createRefreshToken(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Invalidar tokens previos del usuario
        refreshTokenRepository.deleteByUsuario(usuario);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000));
        refreshToken.setActivo(true);

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token creado para usuario: {}", usuario.getEmail());

        return savedToken;
    }

    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Refresh token no encontrado"));

        if (!refreshToken.getActivo()) {
            throw new BusinessException("Refresh token inactivo");
        }

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException("Refresh token expirado");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Refresh token no encontrado"));

        refreshToken.setActivo(false);
        refreshTokenRepository.save(refreshToken);
        log.info("Refresh token revocado para usuario: {}", refreshToken.getUsuario().getEmail());
    }

    @Transactional
    public void revokeAllUserTokens(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        refreshTokenRepository.deleteByUsuario(usuario);
        log.info("Todos los refresh tokens revocados para usuario: {}", usuario.getEmail());
    }

    @Scheduled(cron = "0 0 2 * * ?") // Ejecutar diariamente a las 2 AM
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = refreshTokenRepository.deleteAllExpiredSince(now);
        log.info("Cleanup de refresh tokens: {} tokens expirados eliminados", deletedCount);
    }
}
