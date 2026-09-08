package com.api_caich.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.api_caich.enums.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
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
@Profile("!prod")
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
        int created = 0;

        created += crearUsuarioSiNoExiste(
                "Administrador Sistema",
                "admin@caich.com",
                Role.ADMINISTRADOR,
                EstadoUsuario.ACTIVO,
                "ADM-001",
                null,
                "6441000000",
                LocalDateTime.now().minusMinutes(15));

        // Usuario demo TESORERIA
        created += crearUsuarioSiNoExiste(
            "Tesorería CAICH",
            "tesoreria@caich.com",
            Role.TESORERIA,
            EstadoUsuario.ACTIVO,
            "TES-001",
            null,
            "6445000000",
            LocalDateTime.now().minusMinutes(10));

        created += crearUsuarioSiNoExiste(
                "Coordinación Recepción CAICH",
                "recepcion1@caich.com",
                Role.ADMINISTRADOR,
                EstadoUsuario.ACTIVO,
                "ADM-002",
                null,
                "6441000001",
                LocalDateTime.now().minusHours(2));

        created += crearUsuarioSiNoExiste(
                "Dr. Juan Martínez Terapeuta",
                "terapeuta1@caich.com",
                Role.TERAPEUTA,
                EstadoUsuario.ACTIVO,
                "TER-001",
                null,
                "6442000001",
                LocalDateTime.now().minusHours(6));

        created += crearUsuarioSiNoExiste(
                "Dra. Ana Rodríguez Terapeuta",
                "terapeuta2@caich.com",
                Role.TERAPEUTA,
                EstadoUsuario.ACTIVO,
                "TER-002",
                null,
                "6442000002",
                LocalDateTime.now().minusHours(4));

        created += crearUsuarioSiNoExiste(
                "Pedro Hernández Paciente",
                "paciente1@caich.com",
                Role.PACIENTE,
                EstadoUsuario.ACTIVO,
                "PAC-001",
                null,
                "6443000001",
                LocalDateTime.now().minusDays(1));

        created += crearUsuarioSiNoExiste(
                "Laura Sánchez Paciente",
                "paciente2@caich.com",
                Role.PACIENTE,
                EstadoUsuario.ACTIVO,
                "PAC-002",
                null,
                "6443000002",
                LocalDateTime.now().minusHours(10));

        created += crearUsuarioSiNoExiste(
                "Luis Fernando Acosta",
                "alumno1@caich.com",
                Role.ALUMNO,
                EstadoUsuario.ACTIVO,
                "ALU-2024-001",
                "2024001",
                "6444000001",
                LocalDateTime.now().minusHours(8));

        created += crearUsuarioSiNoExiste(
                "Valeria Montaño Castro",
                "alumno2@caich.com",
                Role.ALUMNO,
                EstadoUsuario.ACTIVO,
                "ALU-2024-002",
                "2024002",
                "6444000002",
                LocalDateTime.now().minusHours(12));

        if (created == 0) {
            System.out.println("⏭️  Usuarios demo ya inicializados");
        } else {
            System.out.println("✅ " + created + " usuarios demo asegurados (password: 123456)");
        }
    }

    private int crearUsuarioSiNoExiste(String nombreCompleto, String email, Role role,
                                       EstadoUsuario estado, String folio, String idAlumno,
                                       String telefono, LocalDateTime ultimaConexion) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            return 0;
        }

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode("123456"));
        usuario.setRol(role.getNombre());
        usuario.setEstado(estado.getValor());
        usuario.setFolio(folio);
        usuario.setIdAlumno(idAlumno);
        usuario.setTelefono(telefono);
        usuario.setUltimaConexion(ultimaConexion);
        usuarioRepository.save(usuario);
        return 1;
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
