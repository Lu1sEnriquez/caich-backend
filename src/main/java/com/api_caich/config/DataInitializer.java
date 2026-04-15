package com.api_caich.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.api_caich.enums.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.api_caich.entity.Espacio;
import com.api_caich.entity.HorarioBase;
import com.api_caich.entity.MotivosSancion;
import com.api_caich.entity.Producto;
import com.api_caich.entity.Usuario;
import com.api_caich.enums.EstadoUsuario;
import com.api_caich.repository.EspacioRepository;
import com.api_caich.repository.HorarioBaseRepository;
import com.api_caich.repository.MotivosSancionRepository;
import com.api_caich.repository.ProductoRepository;
import com.api_caich.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final EspacioRepository espacioRepository;
    private final HorarioBaseRepository horarioBaseRepository;
    private final PasswordEncoder passwordEncoder;
    private final MotivosSancionRepository motivosSancionRepository;

    @Override
    public void run(String... args) {
        initializeUsers();
        initializeProducts();
        initializeEspacios();
        initializeHorarioBase();
        initializeMotivosSancion();
    }

    private void initializeUsers() {
        if (usuarioRepository.count() > 0) {
            System.out.println("⏭️  Usuarios ya inicializados");
            return;
        }

        // Admin
        Usuario admin = new Usuario();
        admin.setNombreCompleto("Administrador Sistema");
        admin.setEmail("admin@caich.com");
        admin.setPasswordHash(passwordEncoder.encode("123456"));
        admin.setRol(Role.ADMINISTRADOR.getNombre());
        admin.setEstado(EstadoUsuario.ACTIVO.getValor());
        usuarioRepository.save(admin);

        Usuario recep2 = new Usuario();
        recep2.setNombreCompleto("Carlos López Recepcionista");
        recep2.setEmail("recepcion1@caich.com");
        recep2.setPasswordHash(passwordEncoder.encode("123456"));
        recep2.setRol(Role.ADMINISTRADOR.getNombre());
        recep2.setEstado(EstadoUsuario.ACTIVO.getValor());
        usuarioRepository.save(recep2);

        // Terapeutas
        Usuario terapeuta1 = new Usuario();
        terapeuta1.setNombreCompleto("Dr. Juan Martínez Terapeuta");
        terapeuta1.setEmail("terapeuta1@caich.com");
        terapeuta1.setPasswordHash(passwordEncoder.encode("123456"));
        terapeuta1.setRol(Role.TERAPEUTA.getNombre());
        terapeuta1.setEstado(EstadoUsuario.ACTIVO.getValor());
        usuarioRepository.save(terapeuta1);

        Usuario terapeuta2 = new Usuario();
        terapeuta2.setNombreCompleto("Dra. Ana Rodríguez Terapeuta");
        terapeuta2.setEmail("terapeuta2@caich.com");
        terapeuta2.setPasswordHash(passwordEncoder.encode("123456"));
        terapeuta2.setRol(Role.TERAPEUTA.getNombre());
        terapeuta2.setEstado(EstadoUsuario.ACTIVO.getValor());
        usuarioRepository.save(terapeuta2);

        // Pacientes
        Usuario paciente1 = new Usuario();
        paciente1.setNombreCompleto("Pedro Hernández Paciente");
        paciente1.setEmail("paciente1@caich.com");
        paciente1.setPasswordHash(passwordEncoder.encode("123456"));
        paciente1.setRol(Role.PACIENTE.getNombre());
        paciente1.setEstado(EstadoUsuario.ACTIVO.getValor());
        usuarioRepository.save(paciente1);

        Usuario paciente2 = new Usuario();
        paciente2.setNombreCompleto("Laura Sánchez Paciente");
        paciente2.setEmail("paciente2@caich.com");
        paciente2.setPasswordHash(passwordEncoder.encode("123456"));
        paciente2.setRol(Role.PACIENTE.getNombre());
        paciente2.setEstado(EstadoUsuario.ACTIVO.getValor());
        usuarioRepository.save(paciente2);

        Usuario Alumno1 = new Usuario();
        Alumno1.setNombreCompleto("Alumno1");
        Alumno1.setEmail("alumno1@caich.com");
        Alumno1.setPasswordHash(passwordEncoder.encode("123456"));
        Alumno1.setRol(Role.ALUMNO.getNombre());
        Alumno1.setEstado(EstadoUsuario.ACTIVO.getValor());
        usuarioRepository.save(Alumno1);

        Usuario Alumno2 = new Usuario();
        Alumno2.setNombreCompleto("Alumno2");
        Alumno2.setEmail("alumno2@caich.com");
        Alumno2.setPasswordHash(passwordEncoder.encode("123456"));
        Alumno2.setRol(Role.ALUMNO.getNombre());
        Alumno2.setEstado(EstadoUsuario.ACTIVO.getValor());
        usuarioRepository.save(Alumno2);

        System.out.println("✅ 7 usuarios de prueba creados (password: 123456)");
    }

    private void initializeProducts() {
        if (productoRepository.count() > 0) {
            System.out.println("⏭️  Productos ya inicializados");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // Productos vendibles
        Producto vendible1 = new Producto();
        vendible1.setNombre("Aceite Esencial de Lavanda");
        vendible1.setCodigo("PROD-001");
        vendible1.setDescripcion("Aceite esencial puro de lavanda 30ml");
        vendible1.setCategoria("Aceites Esenciales");
        vendible1.setPrecio(new BigDecimal("250.00"));
        vendible1.setStock(50);
        vendible1.setStockMinimo(10);
        vendible1.setEsVendible(true);
        vendible1.setEsPrestable(false);
        vendible1.setEstaActivo(true);
        vendible1.setFechaCreacion(now);
        vendible1.setUltimaActualizacion(now);
        productoRepository.save(vendible1);

        Producto vendible2 = new Producto();
        vendible2.setNombre("Crema de Árnica");
        vendible2.setCodigo("PROD-002");
        vendible2.setDescripcion("Crema de árnica para masajes terapéuticos 100g");
        vendible2.setCategoria("Cremas");
        vendible2.setPrecio(new BigDecimal("180.00"));
        vendible2.setStock(30);
        vendible2.setStockMinimo(5);
        vendible2.setEsVendible(true);
        vendible2.setEsPrestable(false);
        vendible2.setEstaActivo(true);
        vendible2.setFechaCreacion(now);
        vendible2.setUltimaActualizacion(now);
        productoRepository.save(vendible2);

        // Productos prestables
        Producto prestable1 = new Producto();
        prestable1.setNombre("Camilla Portátil");
        prestable1.setCodigo("EQUIP-001");
        prestable1.setDescripcion("Camilla portátil plegable para terapia");
        prestable1.setCategoria("Equipamiento");
        prestable1.setPrecio(new BigDecimal("100.00")); // Costo por préstamo
        prestable1.setStock(5);
        prestable1.setStockMinimo(1);
        prestable1.setEsVendible(false);
        prestable1.setEsPrestable(true);
        prestable1.setEstaActivo(true);
        prestable1.setFechaCreacion(now);
        prestable1.setUltimaActualizacion(now);
        productoRepository.save(prestable1);

        Producto prestable2 = new Producto();
        prestable2.setNombre("Kit de Ultrasonido");
        prestable2.setCodigo("EQUIP-002");
        prestable2.setDescripcion("Equipo de ultrasonido terapéutico profesional");
        prestable2.setCategoria("Equipamiento");
        prestable2.setPrecio(new BigDecimal("150.00")); // Costo por préstamo
        prestable2.setStock(3);
        prestable2.setStockMinimo(1);
        prestable2.setEsVendible(false);
        prestable2.setEsPrestable(true);
        prestable2.setEstaActivo(true);
        prestable2.setFechaCreacion(now);
        prestable2.setUltimaActualizacion(now);
        productoRepository.save(prestable2);

        System.out.println("✅ 4 productos de prueba creados (2 vendibles, 2 prestables)");
    }

    private void initializeEspacios() {
        if (espacioRepository.count() > 0) {
            System.out.println("⏭️  Espacios ya inicializados");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // Espacio 1
        Espacio espacio1 = new Espacio();
        espacio1.setNombre("Cubículo A");
        espacio1.setDescripcion("Cubículo principal equipado con camilla y equipo de terapia física");
        espacio1.setCapacidad(2);
        espacio1.setCostoPorHora(new BigDecimal("200.00"));
        espacio1.setEstaActivo(true);
        espacio1.setFechaCreacion(now);
        espacio1.setUltimaActualizacion(now);
        espacioRepository.save(espacio1);

        // Espacio 2
        Espacio espacio2 = new Espacio();
        espacio2.setNombre("Cubículo B");
        espacio2.setDescripcion("Cubículo secundario para consultas y terapias individuales");
        espacio2.setCapacidad(1);
        espacio2.setCostoPorHora(new BigDecimal("150.00"));
        espacio2.setEstaActivo(true);
        espacio2.setFechaCreacion(now);
        espacio2.setUltimaActualizacion(now);
        espacioRepository.save(espacio2);

        // Espacio 3
        Espacio espacio3 = new Espacio();
        espacio3.setNombre("Sala Grupal");
        espacio3.setDescripcion("Sala amplia para terapias grupales y actividades especiales");
        espacio3.setCapacidad(6);
        espacio3.setCostoPorHora(new BigDecimal("350.00"));
        espacio3.setEstaActivo(true);
        espacio3.setFechaCreacion(now);
        espacio3.setUltimaActualizacion(now);
        espacioRepository.save(espacio3);

        System.out.println("✅ 3 espacios de prueba creados (Cubículo A, Cubículo B, Sala Grupal)");
    }

    private void initializeHorarioBase() {
        if (horarioBaseRepository.count() > 0) {
            System.out.println("⏭️  Horario base ya inicializado");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        HorarioBase horarioGlobal = new HorarioBase();
        horarioGlobal.setEspacio(null); // Global (sin espacio asociado)
        horarioGlobal.setHoraApertura(LocalTime.parse("07:00"));
        horarioGlobal.setHoraCierre(LocalTime.parse("16:00"));
        horarioGlobal.setIntervaloMinutos(60);
        horarioGlobal.setDuracionMinimaMinutos(60);
        horarioGlobal.setDuracionMaximaMinutos(120);
        horarioGlobal.setDiasOperativos("1,2,3,4,5"); // Lunes a Viernes
        horarioGlobal.setEstaActivo(true);
        horarioGlobal.setFechaCreacion(now);
        horarioGlobal.setUltimaActualizacion(now);
        horarioBaseRepository.save(horarioGlobal);

        System.out.println("✅ Horario base global creado (L-V 07:00-16:00, intervalos de 1h)");
    }

    private void initializeMotivosSancion() {
        if (motivosSancionRepository.count() > 0) {
            System.out.println("⏭️  Motivos de sanción ya inicializados");
            return;
        }

        MotivosSancion motivos = new MotivosSancion();
        motivos.setDescripcion("RETRASO");

        MotivosSancion motivos2 = new MotivosSancion();
        motivos2.setDescripcion("NO CANCELÓ");

        MotivosSancion motivos3 = new MotivosSancion();
        motivos3.setDescripcion("JUICIO INAPROPIADO");

        MotivosSancion motivos4 = new MotivosSancion();
        motivos4.setDescripcion("SIN FILIPINA");

        MotivosSancion motivos5 = new MotivosSancion();
        motivos5.setDescripcion("FUERA DE LABORATORIO");

        MotivosSancion motivos6 = new MotivosSancion();
        motivos6.setDescripcion("GORRA");

        MotivosSancion motivos7 = new MotivosSancion();
        motivos7.setDescripcion("TENIS");

        MotivosSancion motivos8 = new MotivosSancion();
        motivos8.setDescripcion("COMIDA");

        MotivosSancion motivos9 = new MotivosSancion();
        motivos9.setDescripcion("ROPA FRENTE");

        motivosSancionRepository.save(motivos);
        motivosSancionRepository.save(motivos2);
        motivosSancionRepository.save(motivos3);
        motivosSancionRepository.save(motivos4);
        motivosSancionRepository.save(motivos5);
        motivosSancionRepository.save(motivos6);
        motivosSancionRepository.save(motivos7);
        motivosSancionRepository.save(motivos8);
        motivosSancionRepository.save(motivos9);

        System.out.println("✅ 9 motivos de sanción creados");

    }

}