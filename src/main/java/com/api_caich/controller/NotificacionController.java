package com.api_caich.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api_caich.dto.BaseResponseDTO;
import com.api_caich.dto.DataResponseDTO;
import com.api_caich.dto.notificacion.NotificacionDTO;
import com.api_caich.security.CustomUserDetails;
import com.api_caich.service.NotificacionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "APIs for user notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping("/no-leidas")
    @Operation(summary = "Get unread notifications", description = "Get all unread notifications for current user")
    public ResponseEntity<DataResponseDTO<List<NotificacionDTO>>> obtenerNoLeidas(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long usuarioId = userDetails.getId();
        List<NotificacionDTO> notificaciones = notificacionService.obtenerNoLeidas(usuarioId);

        return ResponseEntity.ok(DataResponseDTO.success(notificaciones, "Unread notifications retrieved"));
    }

    @GetMapping
    @Operation(summary = "Get all notifications", description = "Get all notifications for current user with optional limit")
    public ResponseEntity<DataResponseDTO<List<NotificacionDTO>>> obtenerTodas(
            @AuthenticationPrincipal CustomUserDetails authentication,
            @RequestParam(defaultValue = "0") int limite) {

        Long usuarioId = authentication.getId();
        List<NotificacionDTO> notificaciones = notificacionService.obtenerTodas(usuarioId, limite);

        return ResponseEntity.ok(DataResponseDTO.success(notificaciones, "Notifications retrieved"));
    }

    @PostMapping("/{notificacionId}/marcar-leida")
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    public ResponseEntity<BaseResponseDTO> marcarComoLeida(
            @PathVariable Long notificacionId,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        notificacionService.marcarComoLeida(notificacionId, usuarioId);

        return ResponseEntity.ok(BaseResponseDTO.success("Notification marked as read"));
    }

    @PostMapping("/marcar-todas-leidas")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read for current user")
    public ResponseEntity<DataResponseDTO<Integer>> marcarTodasComoLeidas(@AuthenticationPrincipal CustomUserDetails authentication) {
        Long usuarioId = authentication.getId();
        int marcadas = notificacionService.marcarTodasComoLeidas(usuarioId);

        return ResponseEntity.ok(DataResponseDTO.success(marcadas, marcadas + " notifications marked as read"));
    }

    @GetMapping("/contador-no-leidas")
    @Operation(summary = "Get unread count", description = "Get count of unread notifications for current user")
    public ResponseEntity<DataResponseDTO<Long>> obtenerContadorNoLeidas(@AuthenticationPrincipal CustomUserDetails authentication) {
        Long usuarioId = authentication.getId();
        List<NotificacionDTO> notificaciones = notificacionService.obtenerNoLeidas(usuarioId);

        return ResponseEntity.ok(DataResponseDTO.success((long) notificaciones.size(), "Unread count retrieved"));
    }

    @DeleteMapping("/{notificacionId}")
    @Operation(summary = "Delete notification", description = "Delete a notification that belongs to current user")
    public ResponseEntity<BaseResponseDTO> eliminarNotificacion(
            @PathVariable Long notificacionId,
            @AuthenticationPrincipal CustomUserDetails authentication) {

        Long usuarioId = authentication.getId();
        notificacionService.eliminarNotificacion(notificacionId, usuarioId);

        return ResponseEntity.ok(BaseResponseDTO.success("Notification deleted successfully"));
    }
}