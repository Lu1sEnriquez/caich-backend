package com.api_caich.config;

import com.api_caich.enums.Role;
import com.api_caich.security.JwtAuthenticationFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers(
                                "/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/health",
                                "/health/detailed",
                                "/cuentas-bancarias/**"
                                )
                        .permitAll()

                        // Endpoints de administración
                        .requestMatchers(
                                "/usuarios/**",
                                "/reportes/**",
                                "/auditoria/**",
                                "/configuracion/**",
                                "/health/database",
                                "/pagos/**")
                        .hasRole(Role.ADMINISTRADOR.getRoleValue())

                        // Endpoints PACIENTE
                        .requestMatchers(
                                "/pagos/registrar-propio",
                                "/tickets/mis-tickets",
                                "/tickets/{ticketId}/comprobante"
                        ).hasRole(Role.PACIENTE.getRoleValue())

                        // Endpoints ALUMNO personales
                        .requestMatchers(
                                "/prestamos/mis-prestamos"
                        ).hasRole(Role.ALUMNO.getRoleValue())

                        // Endpoints de terapeuta
                        .requestMatchers(
                                "/tickets/**",
                                "/prestamos/**",
                                "/citas/**",
                                "/configuracion-horarios/**")
                        .hasAnyRole(Role.ADMINISTRADOR.getRoleValue(), Role.TERAPEUTA.getRoleValue())

                        // Endpoints compartidos
                        .requestMatchers(
                                "/productos/**",
                                "/espacios/**",
                                "/inventario/productos/stock-bajo",
                                "/inventario/productos/sin-stock")
                        .hasAnyRole(Role.ADMINISTRADOR.getRoleValue(), Role.TERAPEUTA.getRoleValue(), Role.ALUMNO.getRoleValue())

                        // Endpoints de testing de roles
                        .requestMatchers(
                                "/health/auth",
                                "/health/admin",
                                "/health/terapeuta",
                                "/health/alumno",
                                "/health/paciente")
                        .authenticated()

                        // Endpoints personales
                        .requestMatchers(
                                "/notificaciones/**")
                        .authenticated()
                        // .permitAll()

                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200")); // Angular frontend
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hora de cache para pre-flight

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}

// Por ejemplo:
// Si usas .hasRole("ADMIN"), Spring internamente busca la autoridad "ROLE_ADMIN".
// Si usas .hasAuthority("ROLE_ADMIN"), Spring no agrega ningún prefijo — lo toma literal.
// Así que:
// .hasRole("Administrador") busca "ROLE_Administrador".
// .hasRole("ADMIN") busca "ROLE_ADMIN".

//Estos nombres y authorities deben coincidir con los definidos en el enum Role.java: