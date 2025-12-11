package com.api_caich.security;

import com.api_caich.entity.Usuario;
import com.api_caich.enums.Role;
import com.api_caich.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        if (!"Activo".equals(usuario.getEstado())) {
            throw new UsernameNotFoundException("Usuario inactivo: " + email);
        }

        Role role = Role.fromNombre(usuario.getRol());
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role.getAuthority());

        return new CustomUserDetails(usuario, Collections.singletonList(authority));

    }
}
