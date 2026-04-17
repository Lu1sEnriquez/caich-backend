package com.api_caich.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.api_caich.entity.*;
import com.api_caich.enums.*;
import com.api_caich.repository.*;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Seeder de datos de prueba. Corre después de DataInitializer (Order 2).
 * Genera tickets, citas, reservas, préstamos, pagos, sanciones, notificaciones, etc.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class TestDataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final EspacioRepository espacioRepository;
    private final TicketRepository ticketRepository;
    private final PagoRepository pagoRepository;
    private final NotificacionRepository notificacionRepository;
    private final SancionRepository sancionRepository;
    private final ExcepcionHorarioRepository excepcionHorarioRepository;
    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // Solo ejecutar si los escenarios demo ya fueron sembrados (evitar duplicados)
        if (ticketRepository.findByFolio("TK-0001").isPresent()) {
            System.out.println("⏭️  Escenarios demo ya existentes, saltando seeder");
            return;
        }

        System.out.println("🌱 Iniciando seeder de datos de prueba...");

        seedUsuariosAdicionales();
        seedProductosAdicionales();
        seedCuentasBancarias();
        seedExcepcionesHorario();

        // Obtener entidades base para las relaciones
        var admin = usuarioRepository.findByEmail("admin@caich.com").orElseThrow();
        var recep = usuarioRepository.findByEmail("recepcion1@caich.com").orElseThrow();
        var terapeuta1 = usuarioRepository.findByEmail("terapeuta1@caich.com").orElseThrow();
        var terapeuta2 = usuarioRepository.findByEmail("terapeuta2@caich.com").orElseThrow();
        var paciente1 = usuarioRepository.findByEmail("paciente1@caich.com").orElseThrow();
        var paciente2 = usuarioRepository.findByEmail("paciente2@caich.com").orElseThrow();
        var alumno1 = usuarioRepository.findByEmail("alumno1@caich.com").orElseThrow();
        var alumno2 = usuarioRepository.findByEmail("alumno2@caich.com").orElseThrow();
        var paciente3 = usuarioRepository.findByEmail("paciente3@caich.com").orElseThrow();
        var paciente4 = usuarioRepository.findByEmail("paciente4@caich.com").orElseThrow();
        var paciente5 = usuarioRepository.findByEmail("paciente5@caich.com").orElseThrow();
        var alumno3 = usuarioRepository.findByEmail("alumno3@caich.com").orElseThrow();
        var alumno4 = usuarioRepository.findByEmail("alumno4@caich.com").orElseThrow();
        var alumno5 = usuarioRepository.findByEmail("alumno5@caich.com").orElseThrow();
        var terapeuta3 = usuarioRepository.findByEmail("terapeuta3@caich.com").orElseThrow();

        var espacios = espacioRepository.findAll();
        var cubiculoA = espacios.stream().filter(e -> e.getNombre().equals("Cubículo A")).findFirst().orElseThrow();
        var cubiculoB = espacios.stream().filter(e -> e.getNombre().equals("Cubículo B")).findFirst().orElseThrow();
        var salaGrupal = espacios.stream().filter(e -> e.getNombre().equals("Sala Grupal")).findFirst().orElseThrow();

        var productos = productoRepository.findAll();
        var aceiteLavanda = productos.stream().filter(p -> p.getCodigo().equals("PROD-001")).findFirst().orElseThrow();
        var cremaArnica = productos.stream().filter(p -> p.getCodigo().equals("PROD-002")).findFirst().orElseThrow();
        var camillaPortatil = productos.stream().filter(p -> p.getCodigo().equals("EQUIP-001")).findFirst().orElseThrow();
        var kitUltrasonido = productos.stream().filter(p -> p.getCodigo().equals("EQUIP-002")).findFirst().orElseThrow();
        var gelFrio = productos.stream().filter(p -> p.getCodigo().equals("PROD-003")).findFirst().orElseThrow();
        var vendaElastica = productos.stream().filter(p -> p.getCodigo().equals("PROD-004")).findFirst().orElseThrow();
        var tensElectro = productos.stream().filter(p -> p.getCodigo().equals("EQUIP-003")).findFirst().orElseThrow();
        var pelotaEjercicio = productos.stream().filter(p -> p.getCodigo().equals("EQUIP-004")).findFirst().orElseThrow();
        var bandaResistencia = productos.stream().filter(p -> p.getCodigo().equals("EQUIP-005")).findFirst().orElseThrow();

        LocalDateTime now = LocalDateTime.now();
        int folioCounter = 1;

        // =====================================================================
        // CASO 1: Cita completada y pagada con efectivo (hace 2 semanas)
        // Paciente1 con Terapeuta1 - Sesión de fisioterapia completada
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.COMPLETADO);
            t.setEstadoFinanciero(EstadoFinanciero.PAGADO);
            t.setMontoTotal(new BigDecimal("350.00"));
            t.setMontoPagado(new BigDecimal("350.00"));
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketCita cita = new TicketCita();
            cita.setTicket(t);
            cita.setPaciente(paciente1);
            cita.setTerapeuta(terapeuta1);
            cita.setFechaInicio(now.minusDays(14).withHour(9).withMinute(0));
            cita.setFechaFin(now.minusDays(14).withHour(10).withMinute(0));
            cita.setPrecioCobrado(new BigDecimal("350.00"));
            cita.setNotasTerapia("Paciente presenta dolor lumbar crónico. Se aplicó terapia manual y ejercicios de estabilización. Buena evolución.");
            cita.setModalidad("Presencial");
            t.setCita(cita);
            ticketRepository.save(t);

            Pago pago = new Pago();
            pago.setTicket(t);
            pago.setMonto(new BigDecimal("350.00"));
            pago.setMetodoPago(MetodoPago.EFECTIVO);
            pago.setEstado(EstadoPagoTransaccion.APROBADO);
            pago.setFechaPago(now.minusDays(14).withHour(10).withMinute(15));
            pago.setVerificadoPor(recep);
            pago.setNotas("Pago en efectivo al finalizar la sesión");
            pagoRepository.save(pago);
        }

        // =====================================================================
        // CASO 2: Cita completada con pago por transferencia (hace 10 días)
        // Paciente2 con Terapeuta2 - Sesión online
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(admin);
            t.setEstadoTicket(EstadoTicket.COMPLETADO);
            t.setEstadoFinanciero(EstadoFinanciero.PAGADO);
            t.setMontoTotal(new BigDecimal("300.00"));
            t.setMontoPagado(new BigDecimal("300.00"));
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketCita cita = new TicketCita();
            cita.setTicket(t);
            cita.setPaciente(paciente2);
            cita.setTerapeuta(terapeuta2);
            cita.setFechaInicio(now.minusDays(10).withHour(11).withMinute(0));
            cita.setFechaFin(now.minusDays(10).withHour(12).withMinute(0));
            cita.setPrecioCobrado(new BigDecimal("300.00"));
            cita.setNotasTerapia("Sesión de rehabilitación de rodilla post operatoria. Ejercicios de rango de movimiento.");
            cita.setModalidad("Online");
            t.setCita(cita);
            ticketRepository.save(t);

            Pago pago = new Pago();
            pago.setTicket(t);
            pago.setMonto(new BigDecimal("300.00"));
            pago.setMetodoPago(MetodoPago.TRANSFERENCIA);
            pago.setEstado(EstadoPagoTransaccion.APROBADO);
            pago.setReferencia("REF-BBVA-20260330-001");
            pago.setFechaPago(now.minusDays(10).withHour(10).withMinute(30));
            pago.setVerificadoPor(admin);
            pago.setNotas("Transferencia BBVA verificada");
            pagoRepository.save(pago);
        }

        // =====================================================================
        // CASO 3: Cita agendada a futuro (próxima semana) - Pendiente de pago
        // Paciente3 con Terapeuta1
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.AGENDADO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(new BigDecimal("350.00"));
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketCita cita = new TicketCita();
            cita.setTicket(t);
            cita.setPaciente(paciente3);
            cita.setTerapeuta(terapeuta1);
            cita.setFechaInicio(now.plusDays(3).withHour(8).withMinute(0));
            cita.setFechaFin(now.plusDays(3).withHour(9).withMinute(0));
            cita.setPrecioCobrado(new BigDecimal("350.00"));
            cita.setModalidad("Presencial");
            t.setCita(cita);
            ticketRepository.save(t);
        }

        // =====================================================================
        // CASO 4: Cita agendada a futuro con pago parcial (depósito)
        // Paciente4 con Terapeuta2
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.AGENDADO);
            t.setEstadoFinanciero(EstadoFinanciero.PAGO_PARCIAL);
            t.setMontoTotal(new BigDecimal("400.00"));
            t.setMontoPagado(new BigDecimal("200.00"));
            t.setCostoAdicional(new BigDecimal("50.00"));
            t.setMotivoCostoAdicional("Material especial para terapia con vendaje neuromuscular");
            ticketRepository.save(t);

            TicketCita cita = new TicketCita();
            cita.setTicket(t);
            cita.setPaciente(paciente4);
            cita.setTerapeuta(terapeuta2);
            cita.setFechaInicio(now.plusDays(5).withHour(10).withMinute(0));
            cita.setFechaFin(now.plusDays(5).withHour(11).withMinute(0));
            cita.setPrecioCobrado(new BigDecimal("350.00"));
            cita.setModalidad("Presencial");
            t.setCita(cita);
            ticketRepository.save(t);

            Pago pago = new Pago();
            pago.setTicket(t);
            pago.setMonto(new BigDecimal("200.00"));
            pago.setMetodoPago(MetodoPago.DEPOSITO);
            pago.setEstado(EstadoPagoTransaccion.APROBADO);
            pago.setReferencia("DEP-BANORTE-20260410");
            pago.setFechaPago(now.minusDays(2));
            pago.setVerificadoPor(admin);
            pago.setNotas("Anticipo del 50% aprobado");
            pagoRepository.save(pago);
        }

        // =====================================================================
        // CASO 5: Cita cancelada (hace 5 días) - Reembolso
        // Paciente1 con Terapeuta2
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.CANCELADO);
            t.setEstadoFinanciero(EstadoFinanciero.REEMBOLSADO);
            t.setMontoTotal(new BigDecimal("350.00"));
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketCita cita = new TicketCita();
            cita.setTicket(t);
            cita.setPaciente(paciente1);
            cita.setTerapeuta(terapeuta2);
            cita.setFechaInicio(now.minusDays(5).withHour(14).withMinute(0));
            cita.setFechaFin(now.minusDays(5).withHour(15).withMinute(0));
            cita.setPrecioCobrado(new BigDecimal("350.00"));
            cita.setModalidad("Presencial");
            t.setCita(cita);
            ticketRepository.save(t);
        }

        // =====================================================================
        // CASO 6: Cita en progreso HOY - con pago pendiente de revisión
        // Paciente5 con Terapeuta1
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(admin);
            t.setEstadoTicket(EstadoTicket.EN_PROGRESO);
            t.setEstadoFinanciero(EstadoFinanciero.EN_REVISION);
            t.setMontoTotal(new BigDecimal("350.00"));
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketCita cita = new TicketCita();
            cita.setTicket(t);
            cita.setPaciente(paciente5);
            cita.setTerapeuta(terapeuta1);
            cita.setFechaInicio(now.withHour(8).withMinute(0));
            cita.setFechaFin(now.withHour(9).withMinute(0));
            cita.setPrecioCobrado(new BigDecimal("350.00"));
            cita.setNotasTerapia("Evaluación inicial. Paciente con cervicalgia.");
            cita.setModalidad("Presencial");
            t.setCita(cita);
            ticketRepository.save(t);

            Pago pago = new Pago();
            pago.setTicket(t);
            pago.setMonto(new BigDecimal("350.00"));
            pago.setMetodoPago(MetodoPago.TRANSFERENCIA);
            pago.setEstado(EstadoPagoTransaccion.PENDIENTE_REVISION);
            pago.setComprobanteUrl("/uploads/comprobantes/comp_paciente5_001.jpg");
            pago.setReferencia("REF-HSBC-20260413");
            pago.setFechaPago(now.minusHours(1));
            pago.setNotas("Comprobante subido por paciente, pendiente de verificar");
            pagoRepository.save(pago);
        }

        // =====================================================================
        // CASO 7: Ticket de venta de productos - Completado y pagado
        // Admin vende aceite y crema a Paciente2
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.COMPLETADO);
            t.setEstadoFinanciero(EstadoFinanciero.PAGADO);
            t.setMontoTotal(new BigDecimal("430.00"));
            t.setMontoPagado(new BigDecimal("430.00"));
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketVenta v1 = new TicketVenta();
            v1.setTicket(t);
            v1.setProducto(aceiteLavanda);
            v1.setCantidad(1);
            v1.setPrecioUnitario(new BigDecimal("250.00"));
            v1.setSubtotal(new BigDecimal("250.00"));
            t.getVentas().add(v1);

            TicketVenta v2 = new TicketVenta();
            v2.setTicket(t);
            v2.setProducto(cremaArnica);
            v2.setCantidad(1);
            v2.setPrecioUnitario(new BigDecimal("180.00"));
            v2.setSubtotal(new BigDecimal("180.00"));
            t.getVentas().add(v2);

            ticketRepository.save(t);

            Pago pago = new Pago();
            pago.setTicket(t);
            pago.setMonto(new BigDecimal("430.00"));
            pago.setMetodoPago(MetodoPago.TARJETA);
            pago.setEstado(EstadoPagoTransaccion.APROBADO);
            pago.setReferencia("VISA-****1234");
            pago.setFechaPago(now.minusDays(7));
            pago.setVerificadoPor(recep);
            pagoRepository.save(pago);
        }

        // =====================================================================
        // CASO 8: Ticket de venta con pago rechazado (comprobante ilegible)
        // Paciente3 compra gel frío
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.COMPLETADO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(new BigDecimal("120.00"));
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketVenta v1 = new TicketVenta();
            v1.setTicket(t);
            v1.setProducto(gelFrio);
            v1.setCantidad(1);
            v1.setPrecioUnitario(new BigDecimal("120.00"));
            v1.setSubtotal(new BigDecimal("120.00"));
            t.getVentas().add(v1);

            ticketRepository.save(t);

            Pago pagoRechazado = new Pago();
            pagoRechazado.setTicket(t);
            pagoRechazado.setMonto(new BigDecimal("120.00"));
            pagoRechazado.setMetodoPago(MetodoPago.TRANSFERENCIA);
            pagoRechazado.setEstado(EstadoPagoTransaccion.RECHAZADO);
            pagoRechazado.setComprobanteUrl("/uploads/comprobantes/comp_borroso_001.jpg");
            pagoRechazado.setMotivoRechazo("Comprobante ilegible, no se distingue el monto ni la referencia");
            pagoRechazado.setFechaPago(now.minusDays(3));
            pagoRechazado.setVerificadoPor(admin);
            pagoRepository.save(pagoRechazado);
        }

        // =====================================================================
        // CASO 9: Ticket de venta múltiple de productos (hace 1 semana)
        // Varias unidades - Pagado en efectivo
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(admin);
            t.setEstadoTicket(EstadoTicket.COMPLETADO);
            t.setEstadoFinanciero(EstadoFinanciero.PAGADO);
            t.setMontoTotal(new BigDecimal("850.00"));
            t.setMontoPagado(new BigDecimal("850.00"));
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketVenta v1 = new TicketVenta();
            v1.setTicket(t);
            v1.setProducto(aceiteLavanda);
            v1.setCantidad(2);
            v1.setPrecioUnitario(new BigDecimal("250.00"));
            v1.setSubtotal(new BigDecimal("500.00"));
            t.getVentas().add(v1);

            TicketVenta v2 = new TicketVenta();
            v2.setTicket(t);
            v2.setProducto(vendaElastica);
            v2.setCantidad(5);
            v2.setPrecioUnitario(new BigDecimal("45.00"));
            v2.setSubtotal(new BigDecimal("225.00"));
            t.getVentas().add(v2);

            TicketVenta v3 = new TicketVenta();
            v3.setTicket(t);
            v3.setProducto(gelFrio);
            v3.setCantidad(1);
            v3.setPrecioUnitario(new BigDecimal("120.00"));
            v3.setSubtotal(new BigDecimal("120.00"));
            t.getVentas().add(v3);

            ticketRepository.save(t);

            Pago pago = new Pago();
            pago.setTicket(t);
            pago.setMonto(new BigDecimal("850.00"));
            pago.setMetodoPago(MetodoPago.EFECTIVO);
            pago.setEstado(EstadoPagoTransaccion.APROBADO);
            pago.setFechaPago(now.minusDays(7));
            pago.setVerificadoPor(recep);
            pagoRepository.save(pago);
        }

        // =====================================================================
        // CASO 10: Reserva de espacio (Cubículo A) por Alumno1 - Completada
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.COMPLETADO);
            t.setEstadoFinanciero(EstadoFinanciero.PAGADO);
            t.setMontoTotal(BigDecimal.ZERO); // Gratis para alumnos
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketEspacio te = new TicketEspacio();
            te.setTicket(t);
            te.setEspacio(cubiculoA);
            te.setHoraInicioReserva(now.minusDays(8).withHour(8).withMinute(0));
            te.setHoraFinReserva(now.minusDays(8).withHour(10).withMinute(0));
            te.setCostoCobrado(BigDecimal.ZERO);
            te.setCelular("6441234567");
            te.setActividad("Práctica de técnicas de vendaje");
            te.setMaestro("Lic. Roberto Méndez");
            t.getEspacios().add(te);
            ticketRepository.save(t);
        }

        // =====================================================================
        // CASO 11: Reserva de espacio (Sala Grupal) por Alumno2 - Agendada a futuro
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(admin);
            t.setEstadoTicket(EstadoTicket.AGENDADO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(BigDecimal.ZERO);
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketEspacio te = new TicketEspacio();
            te.setTicket(t);
            te.setEspacio(salaGrupal);
            te.setHoraInicioReserva(now.plusDays(2).withHour(13).withMinute(0));
            te.setHoraFinReserva(now.plusDays(2).withHour(15).withMinute(0));
            te.setCostoCobrado(BigDecimal.ZERO);
            te.setCelular("6449876543");
            te.setActividad("Taller de rehabilitación grupal");
            te.setMaestro("Dr. Martha González");
            t.getEspacios().add(te);
            ticketRepository.save(t);
        }

        // =====================================================================
        // CASO 12: Reserva de espacio por externo (Cubículo B) - Con costo, pagado
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.COMPLETADO);
            t.setEstadoFinanciero(EstadoFinanciero.PAGADO);
            t.setMontoTotal(new BigDecimal("300.00"));
            t.setMontoPagado(new BigDecimal("300.00"));
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketEspacio te = new TicketEspacio();
            te.setTicket(t);
            te.setEspacio(cubiculoB);
            te.setHoraInicioReserva(now.minusDays(6).withHour(10).withMinute(0));
            te.setHoraFinReserva(now.minusDays(6).withHour(12).withMinute(0));
            te.setCostoCobrado(new BigDecimal("300.00"));
            te.setCelular("6441112233");
            te.setActividad("Consulta externa de rehabilitación");
            t.getEspacios().add(te);
            ticketRepository.save(t);

            Pago pago = new Pago();
            pago.setTicket(t);
            pago.setMonto(new BigDecimal("300.00"));
            pago.setMetodoPago(MetodoPago.EFECTIVO);
            pago.setEstado(EstadoPagoTransaccion.APROBADO);
            pago.setFechaPago(now.minusDays(6).withHour(10).withMinute(0));
            pago.setVerificadoPor(recep);
            pagoRepository.save(pago);
        }

        // =====================================================================
        // CASO 13: Reserva de espacio cancelada
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.CANCELADO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(BigDecimal.ZERO);
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketEspacio te = new TicketEspacio();
            te.setTicket(t);
            te.setEspacio(cubiculoA);
            te.setHoraInicioReserva(now.minusDays(3).withHour(14).withMinute(0));
            te.setHoraFinReserva(now.minusDays(3).withHour(16).withMinute(0));
            te.setCostoCobrado(BigDecimal.ZERO);
            te.setCelular("6445556677");
            te.setActividad("Práctica clínica - CANCELADA");
            te.setMaestro("Lic. Ana Torres");
            t.getEspacios().add(te);
            ticketRepository.save(t);
        }

        // =====================================================================
        // CASO 14: Préstamo de equipo - ACTIVO (Alumno1 tiene camilla)
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.EN_PROGRESO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(BigDecimal.ZERO);
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketPrestamo tp = new TicketPrestamo();
            tp.setTicket(t);
            tp.setProducto(camillaPortatil);
            tp.setResponsable(alumno1);
            tp.setFechaPrestamo(now.minusDays(1).withHour(8).withMinute(0));
            tp.setFechaDevolucionEstimada(now.withHour(16).withMinute(0));
            tp.setEstado(EstadoPrestamo.ACTIVO);
            t.getPrestamos().add(tp);
            ticketRepository.save(t);
        }

        // =====================================================================
        // CASO 15: Préstamo de equipo - DEVUELTO (Alumno2 devolvió ultrasonido)
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(admin);
            t.setEstadoTicket(EstadoTicket.COMPLETADO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(BigDecimal.ZERO);
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketPrestamo tp = new TicketPrestamo();
            tp.setTicket(t);
            tp.setProducto(kitUltrasonido);
            tp.setResponsable(alumno2);
            tp.setFechaPrestamo(now.minusDays(5).withHour(9).withMinute(0));
            tp.setFechaDevolucionEstimada(now.minusDays(5).withHour(14).withMinute(0));
            tp.setFechaDevolucionReal(now.minusDays(5).withHour(13).withMinute(30));
            tp.setEstado(EstadoPrestamo.DEVUELTO);
            tp.setObservacionesDevolucion("Equipo devuelto en buen estado, antes de la hora estimada");
            t.getPrestamos().add(tp);
            ticketRepository.save(t);
        }

        // =====================================================================
        // CASO 16: Préstamo VENCIDO (Alumno3 no devolvió TENS)
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.EN_PROGRESO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(BigDecimal.ZERO);
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketPrestamo tp = new TicketPrestamo();
            tp.setTicket(t);
            tp.setProducto(tensElectro);
            tp.setResponsable(alumno3);
            tp.setFechaPrestamo(now.minusDays(3).withHour(10).withMinute(0));
            tp.setFechaDevolucionEstimada(now.minusDays(2).withHour(16).withMinute(0));
            tp.setEstado(EstadoPrestamo.VENCIDO);
            t.getPrestamos().add(tp);
            ticketRepository.save(t);
        }

        // =====================================================================
        // CASO 17: Préstamo PERDIDO (Alumno4 perdió pelota de ejercicio)
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(admin);
            t.setEstadoTicket(EstadoTicket.COMPLETADO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(new BigDecimal("80.00"));
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(new BigDecimal("80.00"));
            t.setMotivoCostoAdicional("Reposición de pelota de ejercicio perdida");
            ticketRepository.save(t);

            TicketPrestamo tp = new TicketPrestamo();
            tp.setTicket(t);
            tp.setProducto(pelotaEjercicio);
            tp.setResponsable(alumno4);
            tp.setFechaPrestamo(now.minusDays(10).withHour(8).withMinute(0));
            tp.setFechaDevolucionEstimada(now.minusDays(10).withHour(14).withMinute(0));
            tp.setEstado(EstadoPrestamo.PERDIDO);
            tp.setObservacionesDevolucion("Alumno reportó pérdida del equipo. Se generó cargo por reposición.");
            t.getPrestamos().add(tp);
            ticketRepository.save(t);
        }

        // =====================================================================
        // CASO 18: Ticket mixto - Cita + venta de productos (hace 4 días)
        // Paciente2 con Terapeuta1: terapia + compra de crema
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.COMPLETADO);
            t.setEstadoFinanciero(EstadoFinanciero.PAGADO);
            t.setMontoTotal(new BigDecimal("530.00"));
            t.setMontoPagado(new BigDecimal("530.00"));
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketCita cita = new TicketCita();
            cita.setTicket(t);
            cita.setPaciente(paciente2);
            cita.setTerapeuta(terapeuta1);
            cita.setFechaInicio(now.minusDays(4).withHour(9).withMinute(0));
            cita.setFechaFin(now.minusDays(4).withHour(10).withMinute(0));
            cita.setPrecioCobrado(new BigDecimal("350.00"));
            cita.setNotasTerapia("Seguimiento de rodilla. Mejora notable en rango de movimiento. Se recomienda continuar con ejercicios en casa.");
            cita.setModalidad("Presencial");
            t.setCita(cita);

            TicketVenta v1 = new TicketVenta();
            v1.setTicket(t);
            v1.setProducto(cremaArnica);
            v1.setCantidad(1);
            v1.setPrecioUnitario(new BigDecimal("180.00"));
            v1.setSubtotal(new BigDecimal("180.00"));
            t.getVentas().add(v1);

            ticketRepository.save(t);

            Pago pago = new Pago();
            pago.setTicket(t);
            pago.setMonto(new BigDecimal("530.00"));
            pago.setMetodoPago(MetodoPago.EFECTIVO);
            pago.setEstado(EstadoPagoTransaccion.APROBADO);
            pago.setFechaPago(now.minusDays(4).withHour(10).withMinute(15));
            pago.setVerificadoPor(recep);
            pagoRepository.save(pago);
        }

        // =====================================================================
        // CASO 19: Ticket mixto - Reserva espacio + préstamo de equipo (Alumno1)
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.COMPLETADO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(BigDecimal.ZERO);
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketEspacio te = new TicketEspacio();
            te.setTicket(t);
            te.setEspacio(cubiculoB);
            te.setHoraInicioReserva(now.minusDays(9).withHour(13).withMinute(0));
            te.setHoraFinReserva(now.minusDays(9).withHour(15).withMinute(0));
            te.setCostoCobrado(BigDecimal.ZERO);
            te.setCelular("6441234567");
            te.setActividad("Práctica de electroterapia");
            te.setMaestro("Lic. Roberto Méndez");
            t.getEspacios().add(te);

            TicketPrestamo tp = new TicketPrestamo();
            tp.setTicket(t);
            tp.setProducto(kitUltrasonido);
            tp.setResponsable(alumno1);
            tp.setFechaPrestamo(now.minusDays(9).withHour(13).withMinute(0));
            tp.setFechaDevolucionEstimada(now.minusDays(9).withHour(15).withMinute(0));
            tp.setFechaDevolucionReal(now.minusDays(9).withHour(14).withMinute(50));
            tp.setEstado(EstadoPrestamo.DEVUELTO);
            tp.setObservacionesDevolucion("Todo en orden");
            t.getPrestamos().add(tp);

            ticketRepository.save(t);
        }

        // =====================================================================
        // CASO 20: Cita borrador (recién creada, sin confirmar)
        // =====================================================================
        {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(admin);
            t.setEstadoTicket(EstadoTicket.BORRADOR);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(new BigDecimal("350.00"));
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketCita cita = new TicketCita();
            cita.setTicket(t);
            cita.setPaciente(paciente1);
            cita.setTerapeuta(terapeuta3);
            cita.setFechaInicio(now.plusDays(7).withHour(11).withMinute(0));
            cita.setFechaFin(now.plusDays(7).withHour(12).withMinute(0));
            cita.setPrecioCobrado(new BigDecimal("350.00"));
            cita.setModalidad("Presencial");
            t.setCita(cita);
            ticketRepository.save(t);
        }

        // =====================================================================
        // CASO 21: Múltiples citas agendadas para la próxima semana (para Terapeuta1)
        // =====================================================================
        for (int i = 1; i <= 4; i++) {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.AGENDADO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(new BigDecimal("350.00"));
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            Usuario paciente = (i % 4 == 0) ? paciente4 : (i % 3 == 0) ? paciente3 : (i % 2 == 0) ? paciente2 : paciente1;

            TicketCita cita = new TicketCita();
            cita.setTicket(t);
            cita.setPaciente(paciente);
            cita.setTerapeuta(terapeuta1);
            cita.setFechaInicio(now.plusDays(i + 1).withHour(8 + i).withMinute(0));
            cita.setFechaFin(now.plusDays(i + 1).withHour(9 + i).withMinute(0));
            cita.setPrecioCobrado(new BigDecimal("350.00"));
            cita.setModalidad("Presencial");
            t.setCita(cita);
            ticketRepository.save(t);
        }

        // =====================================================================
        // CASO 22: Historial de citas completadas (últimas 3 semanas - para reportes)
        // =====================================================================
        String[] notasTerapia = {
            "Terapia de hombro. Se aplicaron ultrasonidos y ejercicios de movilidad activa.",
            "Control de fascitis plantar. Aplicación de crioterapia y estiramientos.",
            "Rehabilitación de muñeca. Ejercicios de fortalecimiento progresivo.",
            "Terapia respiratoria. Ejercicios de expansión torácica y drenaje postural.",
            "Evaluación postural. Se detectó escoliosis leve. Plan de tratamiento de 8 sesiones.",
            "Masoterapia cervical. Contractura muscular por estrés laboral."
        };

        Usuario[] pacientesArray = {paciente1, paciente2, paciente3, paciente4, paciente5};
        Usuario[] terapeutasArray = {terapeuta1, terapeuta2, terapeuta3};

        for (int i = 0; i < 6; i++) {
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(i % 2 == 0 ? recep : admin);
            t.setEstadoTicket(EstadoTicket.COMPLETADO);
            t.setEstadoFinanciero(EstadoFinanciero.PAGADO);
            t.setMontoTotal(new BigDecimal("350.00"));
            t.setMontoPagado(new BigDecimal("350.00"));
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketCita cita = new TicketCita();
            cita.setTicket(t);
            cita.setPaciente(pacientesArray[i % 5]);
            cita.setTerapeuta(terapeutasArray[i % 3]);
            cita.setFechaInicio(now.minusDays(20 - (i * 3)).withHour(9 + (i % 4)).withMinute(0));
            cita.setFechaFin(now.minusDays(20 - (i * 3)).withHour(10 + (i % 4)).withMinute(0));
            cita.setPrecioCobrado(new BigDecimal("350.00"));
            cita.setNotasTerapia(notasTerapia[i]);
            cita.setModalidad(i == 3 ? "Online" : "Presencial");
            t.setCita(cita);
            ticketRepository.save(t);

            Pago pago = new Pago();
            pago.setTicket(t);
            pago.setMonto(new BigDecimal("350.00"));
            pago.setMetodoPago(i % 3 == 0 ? MetodoPago.EFECTIVO : i % 3 == 1 ? MetodoPago.TRANSFERENCIA : MetodoPago.TARJETA);
            pago.setEstado(EstadoPagoTransaccion.APROBADO);
            pago.setFechaPago(now.minusDays(20 - (i * 3)));
            pago.setVerificadoPor(i % 2 == 0 ? recep : admin);
            pagoRepository.save(pago);
        }

        // =====================================================================
        // CASO 23: Reservas de espacios variadas para esta semana
        // =====================================================================
        {
            // Reserva hoy - Cubículo A
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.AGENDADO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(BigDecimal.ZERO);
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketEspacio te = new TicketEspacio();
            te.setTicket(t);
            te.setEspacio(cubiculoA);
            te.setHoraInicioReserva(now.withHour(13).withMinute(0));
            te.setHoraFinReserva(now.withHour(15).withMinute(0));
            te.setCostoCobrado(BigDecimal.ZERO);
            te.setCelular("6442223344");
            te.setActividad("Práctica de técnicas manuales");
            te.setMaestro("Lic. María Luisa Valenzuela");
            t.getEspacios().add(te);
            ticketRepository.save(t);
        }
        {
            // Reserva mañana - Sala Grupal
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(admin);
            t.setEstadoTicket(EstadoTicket.AGENDADO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(BigDecimal.ZERO);
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketEspacio te = new TicketEspacio();
            te.setTicket(t);
            te.setEspacio(salaGrupal);
            te.setHoraInicioReserva(now.plusDays(1).withHour(8).withMinute(0));
            te.setHoraFinReserva(now.plusDays(1).withHour(11).withMinute(0));
            te.setCostoCobrado(BigDecimal.ZERO);
            te.setCelular("6445557788");
            te.setActividad("Clase práctica de rehabilitación deportiva");
            te.setMaestro("Dr. Carlos Navarro");
            t.getEspacios().add(te);
            ticketRepository.save(t);
        }
        {
            // Reserva + préstamo programado para Alumno5
            Ticket t = new Ticket();
            t.setFolio(String.format("TK-%04d", folioCounter++));
            t.setCreadoPor(recep);
            t.setEstadoTicket(EstadoTicket.AGENDADO);
            t.setEstadoFinanciero(EstadoFinanciero.PENDIENTE);
            t.setMontoTotal(BigDecimal.ZERO);
            t.setMontoPagado(BigDecimal.ZERO);
            t.setCostoAdicional(BigDecimal.ZERO);
            ticketRepository.save(t);

            TicketEspacio te = new TicketEspacio();
            te.setTicket(t);
            te.setEspacio(cubiculoB);
            te.setHoraInicioReserva(now.plusDays(4).withHour(9).withMinute(0));
            te.setHoraFinReserva(now.plusDays(4).withHour(11).withMinute(0));
            te.setCostoCobrado(BigDecimal.ZERO);
            te.setCelular("6446668899");
            te.setActividad("Práctica de fortalecimiento con bandas terapéuticas");
            te.setMaestro("Lic. Patricia Navarro");
            t.getEspacios().add(te);

            TicketPrestamo tp = new TicketPrestamo();
            tp.setTicket(t);
            tp.setProducto(bandaResistencia);
            tp.setResponsable(alumno5);
            tp.setFechaPrestamo(now.plusDays(4).withHour(9).withMinute(0));
            tp.setFechaDevolucionEstimada(now.plusDays(4).withHour(11).withMinute(0));
            tp.setEstado(EstadoPrestamo.ACTIVO);
            t.getPrestamos().add(tp);

            ticketRepository.save(t);
        }

        // =====================================================================
        // SANCIONES
        // =====================================================================
        seedSanciones(admin, alumno1, alumno2, alumno3, alumno4);

        // =====================================================================
        // NOTIFICACIONES
        // =====================================================================
        seedNotificaciones(admin, recep, terapeuta1, terapeuta2, terapeuta3,
                paciente1, paciente2, paciente3, paciente4, paciente5,
                alumno1, alumno2, alumno3, alumno4, alumno5);

        // =====================================================================
        // MOVIMIENTOS DE INVENTARIO
        // =====================================================================
        seedMovimientosInventario(admin, recep, aceiteLavanda, cremaArnica, gelFrio, vendaElastica,
                camillaPortatil, kitUltrasonido);

        System.out.println("✅ Seeder completado: " + (folioCounter - 1) + " tickets creados con sus relaciones");
        System.out.println("   - Citas (completadas, agendadas, en progreso, canceladas, borradores)");
        System.out.println("   - Ventas de productos (pagadas, rechazadas, múltiples)");
        System.out.println("   - Reservas de espacios (alumno gratis, externo con costo, cancelada)");
        System.out.println("   - Préstamos (activo, devuelto, vencido, perdido)");
        System.out.println("   - Tickets mixtos (cita+venta, espacio+préstamo)");
        System.out.println("   - Pagos variados (efectivo, transferencia, depósito, tarjeta, rechazado, pendiente)");
        System.out.println("   - Sanciones, notificaciones, movimientos de inventario");
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE SEED
    // =========================================================================

    private void seedUsuariosAdicionales() {
        if (usuarioRepository.findByEmail("paciente3@caich.com").isPresent()) {
            return;
        }

        // Pacientes adicionales
        crearUsuario("María García López", "paciente3@caich.com", Role.PACIENTE, EstadoUsuario.ACTIVO, "6441001001");
        crearUsuario("Roberto Díaz Martínez", "paciente4@caich.com", Role.PACIENTE, EstadoUsuario.ACTIVO, "6441001002");
        crearUsuario("Elena Ruiz Fernández", "paciente5@caich.com", Role.PACIENTE, EstadoUsuario.ACTIVO, "6441001003");
        crearUsuario("José Ramírez Torres", "paciente6@caich.com", Role.PACIENTE, EstadoUsuario.SUSPENDIDO, "6441001004");
        crearUsuario("Carmen Vega Morales", "paciente7@caich.com", Role.PACIENTE, EstadoUsuario.INACTIVO, null);

        // Alumnos adicionales
        crearUsuarioAlumno("Diego Moreno Castillo", "alumno3@caich.com", "ALU-2024-003", "2024003");
        crearUsuarioAlumno("Sofía Herrera Juárez", "alumno4@caich.com", "ALU-2024-004", "2024004");
        crearUsuarioAlumno("Miguel Ángel Soto", "alumno5@caich.com", "ALU-2024-005", "2024005");

        // Terapeuta adicional
        crearUsuario("Lic. Patricia Navarro Terapeuta", "terapeuta3@caich.com", Role.TERAPEUTA, EstadoUsuario.ACTIVO, "6449001001");

        System.out.println("✅ 9 usuarios adicionales creados (5 pacientes, 3 alumnos y 1 terapeuta)");
    }

    private void crearUsuario(String nombre, String email, Role rol, EstadoUsuario estado, String telefono) {
        Usuario u = new Usuario();
        u.setNombreCompleto(nombre);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        u.setRol(rol.getNombre());
        u.setEstado(estado.getValor());
        u.setTelefono(telefono);
        usuarioRepository.save(u);
    }

    private void crearUsuarioAlumno(String nombre, String email, String folio, String idAlumno) {
        Usuario u = new Usuario();
        u.setNombreCompleto(nombre);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        u.setRol(Role.ALUMNO.getNombre());
        u.setEstado(EstadoUsuario.ACTIVO.getValor());
        u.setFolio(folio);
        u.setIdAlumno(idAlumno);
        usuarioRepository.save(u);
    }

    private void seedProductosAdicionales() {
        if (productoRepository.findByCodigo("PROD-003").isPresent()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // Vendibles adicionales
        Producto p3 = new Producto();
        p3.setNombre("Gel Frío Terapéutico");
        p3.setCodigo("PROD-003");
        p3.setDescripcion("Gel frío para terapia de crioterapia 250ml");
        p3.setCategoria("Geles");
        p3.setPrecio(new BigDecimal("120.00"));
        p3.setStock(25);
        p3.setStockMinimo(5);
        p3.setEsVendible(true);
        p3.setEsPrestable(false);
        p3.setEstaActivo(true);
        p3.setFechaCreacion(now);
        p3.setUltimaActualizacion(now);
        productoRepository.save(p3);

        Producto p4 = new Producto();
        p4.setNombre("Venda Elástica 10cm");
        p4.setCodigo("PROD-004");
        p4.setDescripcion("Venda elástica de compresión 10cm x 5m");
        p4.setCategoria("Material Clínico");
        p4.setPrecio(new BigDecimal("45.00"));
        p4.setStock(100);
        p4.setStockMinimo(20);
        p4.setEsVendible(true);
        p4.setEsPrestable(false);
        p4.setEstaActivo(true);
        p4.setFechaCreacion(now);
        p4.setUltimaActualizacion(now);
        productoRepository.save(p4);

        Producto p5 = new Producto();
        p5.setNombre("Aceite de Romero");
        p5.setCodigo("PROD-005");
        p5.setDescripcion("Aceite esencial de romero para masajes 30ml");
        p5.setCategoria("Aceites Esenciales");
        p5.setPrecio(new BigDecimal("220.00"));
        p5.setStock(15);
        p5.setStockMinimo(5);
        p5.setEsVendible(true);
        p5.setEsPrestable(false);
        p5.setEstaActivo(true);
        p5.setFechaCreacion(now);
        p5.setUltimaActualizacion(now);
        productoRepository.save(p5);

        Producto p6 = new Producto();
        p6.setNombre("Cinta Kinesiológica");
        p6.setCodigo("PROD-006");
        p6.setDescripcion("Cinta kinesiológica 5cm x 5m varios colores");
        p6.setCategoria("Material Clínico");
        p6.setPrecio(new BigDecimal("150.00"));
        p6.setStock(40);
        p6.setStockMinimo(10);
        p6.setEsVendible(true);
        p6.setEsPrestable(false);
        p6.setEstaActivo(true);
        p6.setFechaCreacion(now);
        p6.setUltimaActualizacion(now);
        productoRepository.save(p6);

        // Producto agotado
        Producto p7 = new Producto();
        p7.setNombre("Crema Caliente Muscular");
        p7.setCodigo("PROD-007");
        p7.setDescripcion("Crema con efecto calor para contracturas 100g");
        p7.setCategoria("Cremas");
        p7.setPrecio(new BigDecimal("195.00"));
        p7.setStock(0);
        p7.setStockMinimo(5);
        p7.setEsVendible(true);
        p7.setEsPrestable(false);
        p7.setEstaActivo(true);
        p7.setFechaCreacion(now);
        p7.setUltimaActualizacion(now);
        productoRepository.save(p7);

        // Producto stock crítico
        Producto p8 = new Producto();
        p8.setNombre("Parches de Calor");
        p8.setCodigo("PROD-008");
        p8.setDescripcion("Parches térmicos adhesivos (paquete de 5)");
        p8.setCategoria("Material Clínico");
        p8.setPrecio(new BigDecimal("85.00"));
        p8.setStock(3);
        p8.setStockMinimo(10);
        p8.setEsVendible(true);
        p8.setEsPrestable(false);
        p8.setEstaActivo(true);
        p8.setFechaCreacion(now);
        p8.setUltimaActualizacion(now);
        productoRepository.save(p8);

        // Producto inactivo
        Producto p9 = new Producto();
        p9.setNombre("Gel Conductor Descontinuado");
        p9.setCodigo("PROD-009");
        p9.setDescripcion("Gel conductor para ultrasonido - DESCONTINUADO");
        p9.setCategoria("Geles");
        p9.setPrecio(new BigDecimal("90.00"));
        p9.setStock(2);
        p9.setStockMinimo(5);
        p9.setEsVendible(true);
        p9.setEsPrestable(false);
        p9.setEstaActivo(false);
        p9.setFechaCreacion(now.minusMonths(6));
        p9.setUltimaActualizacion(now.minusDays(30));
        productoRepository.save(p9);

        // Prestables adicionales
        Producto e3 = new Producto();
        e3.setNombre("TENS Electroestimulador");
        e3.setCodigo("EQUIP-003");
        e3.setDescripcion("Equipo TENS para electroestimulación terapéutica");
        e3.setCategoria("Equipamiento");
        e3.setPrecio(new BigDecimal("0.00"));
        e3.setStock(4);
        e3.setStockMinimo(1);
        e3.setEsVendible(false);
        e3.setEsPrestable(true);
        e3.setEstaActivo(true);
        e3.setFechaCreacion(now);
        e3.setUltimaActualizacion(now);
        productoRepository.save(e3);

        Producto e4 = new Producto();
        e4.setNombre("Pelota de Ejercicio 65cm");
        e4.setCodigo("EQUIP-004");
        e4.setDescripcion("Pelota suiza para ejercicios de rehabilitación 65cm");
        e4.setCategoria("Equipamiento");
        e4.setPrecio(new BigDecimal("0.00"));
        e4.setStock(6);
        e4.setStockMinimo(2);
        e4.setEsVendible(false);
        e4.setEsPrestable(true);
        e4.setEstaActivo(true);
        e4.setFechaCreacion(now);
        e4.setUltimaActualizacion(now);
        productoRepository.save(e4);

        Producto e5 = new Producto();
        e5.setNombre("Banda de Resistencia Set");
        e5.setCodigo("EQUIP-005");
        e5.setDescripcion("Set de 5 bandas de resistencia de diferentes niveles");
        e5.setCategoria("Equipamiento");
        e5.setPrecio(new BigDecimal("0.00"));
        e5.setStock(8);
        e5.setStockMinimo(2);
        e5.setEsVendible(false);
        e5.setEsPrestable(true);
        e5.setEstaActivo(true);
        e5.setFechaCreacion(now);
        e5.setUltimaActualizacion(now);
        productoRepository.save(e5);

        System.out.println("✅ 10 productos adicionales creados (7 vendibles, 3 prestables)");
    }

    private void seedCuentasBancarias() {
        if (cuentaBancariaRepository.count() > 0) {
            return;
        }

        CuentaBancaria c1 = new CuentaBancaria();
        c1.setBanco("BBVA");
        c1.setNumeroCuenta("0123456789");
        c1.setClabe("012345678901234567");
        c1.setTitular("CAICH Centro de Atención Integral");
        c1.setEstaActiva(true);
        cuentaBancariaRepository.save(c1);

        CuentaBancaria c2 = new CuentaBancaria();
        c2.setBanco("Banorte");
        c2.setNumeroCuenta("9876543210");
        c2.setClabe("072345678901234567");
        c2.setTitular("CAICH Servicios Terapéuticos SA de CV");
        c2.setEstaActiva(true);
        cuentaBancariaRepository.save(c2);

        CuentaBancaria c3 = new CuentaBancaria();
        c3.setBanco("Santander");
        c3.setNumeroCuenta("5555666677");
        c3.setClabe("014345678901234567");
        c3.setTitular("CAICH Centro Integral");
        c3.setEstaActiva(false); // Cuenta desactivada
        cuentaBancariaRepository.save(c3);

        System.out.println("✅ 3 cuentas bancarias creadas (2 activas, 1 inactiva)");
    }

    private void seedExcepcionesHorario() {
        if (excepcionHorarioRepository.count() > 0) {
            return;
        }

        var espacios = espacioRepository.findAll();
        var cubiculoA = espacios.stream().filter(e -> e.getNombre().equals("Cubículo A")).findFirst().orElse(null);
        var cubiculoB = espacios.stream().filter(e -> e.getNombre().equals("Cubículo B")).findFirst().orElse(null);

        // Día festivo global - Día del Trabajo
        ExcepcionHorario exc1 = ExcepcionHorario.builder()
                .espacio(null) // Global
                .fecha(LocalDate.of(2026, 5, 1))
                .todoElDia(true)
                .motivo("Día del Trabajo - Cerrado")
                .estaActivo(true)
                .build();
        excepcionHorarioRepository.save(exc1);

        // Día festivo global - Día de la Independencia
        ExcepcionHorario exc2 = ExcepcionHorario.builder()
                .espacio(null)
                .fecha(LocalDate.of(2026, 9, 16))
                .todoElDia(true)
                .motivo("Día de la Independencia - Cerrado")
                .estaActivo(true)
                .build();
        excepcionHorarioRepository.save(exc2);

        // Mantenimiento parcial en Cubículo A (próxima semana)
        ExcepcionHorario exc3 = ExcepcionHorario.builder()
                .espacio(cubiculoA)
                .fecha(LocalDate.now().plusDays(6))
                .todoElDia(false)
                .horaInicioBloqueo(LocalTime.of(7, 0))
                .horaFinBloqueo(LocalTime.of(12, 0))
                .motivo("Mantenimiento preventivo del equipo")
                .estaActivo(true)
                .build();
        excepcionHorarioRepository.save(exc3);

        // Cubículo B cerrado todo el día por fumigación
        ExcepcionHorario exc4 = ExcepcionHorario.builder()
                .espacio(cubiculoB)
                .fecha(LocalDate.now().plusDays(8))
                .todoElDia(true)
                .motivo("Fumigación programada")
                .estaActivo(true)
                .build();
        excepcionHorarioRepository.save(exc4);

        // Evento especial - Jornada de salud (horario extendido marcado como excepción)
        ExcepcionHorario exc5 = ExcepcionHorario.builder()
                .espacio(null)
                .fecha(LocalDate.now().plusDays(15))
                .todoElDia(false)
                .horaInicioBloqueo(LocalTime.of(13, 0))
                .horaFinBloqueo(LocalTime.of(16, 0))
                .motivo("Jornada de Salud ITSON - Solo servicio matutino")
                .estaActivo(true)
                .build();
        excepcionHorarioRepository.save(exc5);

        System.out.println("✅ 5 excepciones de horario creadas (2 festivos, 1 mantenimiento, 1 fumigación, 1 evento)");
    }

    private void seedSanciones(Usuario admin, Usuario alumno1, Usuario alumno2, Usuario alumno3, Usuario alumno4) {
        // Sanción 1: Advertencia por retraso (alumno1) - Cumplida
        Sancion s1 = new Sancion();
        s1.setAlumno(alumno1);
        s1.setTipoSancion(TipoSancion.ADVERTENCIA);
        s1.setMotivo("RETRASO");
        s1.setDescripcion("El alumno llegó 25 minutos tarde a su turno de práctica el 01/04/2026");
        s1.setFechaInicio(LocalDate.now().minusDays(12));
        s1.setFechaFin(LocalDate.now().minusDays(12));
        s1.setEstado(EstadoSancion.CUMPLIDA);
        s1.setCreadoPor(admin);
        sancionRepository.save(s1);

        // Sanción 2: Suspensión activa por no cancelar reserva (alumno3)
        Sancion s2 = new Sancion();
        s2.setAlumno(alumno3);
        s2.setTipoSancion(TipoSancion.SUSPENSION);
        s2.setMotivo("NO CANCELÓ");
        s2.setDescripcion("No se presentó a la reserva de cubículo ni canceló con anticipación. Segunda falta en el mes.");
        s2.setFechaInicio(LocalDate.now().minusDays(2));
        s2.setFechaFin(LocalDate.now().plusDays(5));
        s2.setEstado(EstadoSancion.ACTIVA);
        s2.setCreadoPor(admin);
        sancionRepository.save(s2);

        // Sanción 3: Advertencia por sin filipina (alumno2)
        Sancion s3 = new Sancion();
        s3.setAlumno(alumno2);
        s3.setTipoSancion(TipoSancion.ADVERTENCIA);
        s3.setMotivo("SIN FILIPINA");
        s3.setDescripcion("Se presentó a laboratorio sin filipina reglamentaria");
        s3.setFechaInicio(LocalDate.now().minusDays(5));
        s3.setEstado(EstadoSancion.CUMPLIDA);
        s3.setCreadoPor(admin);
        sancionRepository.save(s3);

        // Sanción 4: Suspensión anulada (alumno1) - Error administrativo
        Sancion s4 = new Sancion();
        s4.setAlumno(alumno1);
        s4.setTipoSancion(TipoSancion.SUSPENSION);
        s4.setMotivo("JUICIO INAPROPIADO");
        s4.setDescripcion("Sanción generada por error. Se confundió de alumno.");
        s4.setFechaInicio(LocalDate.now().minusDays(20));
        s4.setFechaFin(LocalDate.now().minusDays(13));
        s4.setEstado(EstadoSancion.ANULADA);
        s4.setCreadoPor(admin);
        s4.setAnuladoPor(admin);
        s4.setFechaAnulacion(LocalDateTime.now().minusDays(19));
        s4.setMotivoAnulacion("Error administrativo - Se confundió al alumno con otro estudiante");
        sancionRepository.save(s4);

        // Sanción 5: Suspensión activa por comida en laboratorio (alumno4)
        Sancion s5 = new Sancion();
        s5.setAlumno(alumno4);
        s5.setTipoSancion(TipoSancion.SUSPENSION);
        s5.setMotivo("COMIDA");
        s5.setDescripcion("Llevó alimentos al área de práctica clínica. Reincidencia.");
        s5.setFechaInicio(LocalDate.now().minusDays(1));
        s5.setFechaFin(LocalDate.now().plusDays(3));
        s5.setEstado(EstadoSancion.ACTIVA);
        s5.setCreadoPor(admin);
        sancionRepository.save(s5);

        // Sanción 6: Advertencia por gorra (alumno2)
        Sancion s6 = new Sancion();
        s6.setAlumno(alumno2);
        s6.setTipoSancion(TipoSancion.ADVERTENCIA);
        s6.setMotivo("GORRA");
        s6.setDescripcion("Usó gorra dentro del laboratorio");
        s6.setFechaInicio(LocalDate.now().minusDays(8));
        s6.setEstado(EstadoSancion.CUMPLIDA);
        s6.setCreadoPor(admin);
        sancionRepository.save(s6);

        // Sanción 7: Advertencia por tenis (alumno4)
        Sancion s7 = new Sancion();
        s7.setAlumno(alumno4);
        s7.setTipoSancion(TipoSancion.ADVERTENCIA);
        s7.setMotivo("TENIS");
        s7.setDescripcion("Se presentó con tenis en lugar de calzado clínico reglamentario");
        s7.setFechaInicio(LocalDate.now().minusDays(15));
        s7.setEstado(EstadoSancion.CUMPLIDA);
        s7.setCreadoPor(admin);
        sancionRepository.save(s7);

        System.out.println("✅ 7 sanciones creadas (advertencias, suspensiones, anulada)");
    }

    private void seedNotificaciones(Usuario admin, Usuario recep, Usuario terapeuta1, Usuario terapeuta2,
                                     Usuario terapeuta3, Usuario paciente1, Usuario paciente2,
                                     Usuario paciente3, Usuario paciente4, Usuario paciente5,
                                     Usuario alumno1, Usuario alumno2, Usuario alumno3,
                                     Usuario alumno4, Usuario alumno5) {
        LocalDateTime now = LocalDateTime.now();

        // Para admin - Pagos pendientes
        crearNotificacion(admin, "PAGO_PENDIENTE", "Pago pendiente de revisión",
                "El paciente Elena Ruiz ha subido un comprobante de transferencia por $350.00. Requiere verificación.",
                false, null);

        crearNotificacion(admin, "STOCK_BAJO", "Stock bajo: Parches de Calor",
                "El producto 'Parches de Calor' tiene solo 3 unidades en stock (mínimo: 10).",
                false, null);

        crearNotificacion(admin, "STOCK_AGOTADO", "Producto agotado: Crema Caliente Muscular",
                "El producto 'Crema Caliente Muscular' se ha agotado.",
                true, now.minusDays(1));

        crearNotificacion(admin, "PRESTAMO_VENCIDO", "Préstamo vencido",
                "El alumno Diego Moreno no ha devuelto el TENS Electroestimulador. Vencimiento: hace 2 días.",
                false, null);

        // Para recepcionista
        crearNotificacion(recep, "CITA_NUEVA", "Nueva cita agendada",
                "Se ha agendado una nueva cita para María García López con Dr. Juan Martínez.",
                true, now.minusHours(5));

        crearNotificacion(recep, "PAGO_PENDIENTE", "Comprobante rechazado",
                "Se rechazó el comprobante del ticket TK-0008. Motivo: Imagen ilegible.",
                true, now.minusDays(2));

        // Para terapeuta1 - Agenda del día
        crearNotificacion(terapeuta1, "CITA_RECORDATORIO", "Recordatorio: Cita en 1 hora",
                "Tiene una cita con Elena Ruiz Fernández hoy a las 8:00 AM. Motivo: Cervicalgia.",
                false, null);

        crearNotificacion(terapeuta1, "CITA_CANCELADA", "Cita cancelada",
                "La cita con Pedro Hernández del pasado jueves fue cancelada por el paciente.",
                true, now.minusDays(5));

        crearNotificacion(terapeuta1, "CITA_NUEVA", "Nueva cita para próxima semana",
                "Se agendó una nueva cita con María García López para el miércoles a las 8:00 AM.",
                false, null);

        // Para terapeuta2
        crearNotificacion(terapeuta2, "CITA_RECORDATORIO", "Recordatorio de citas de la semana",
                "Tiene 2 citas programadas esta semana. Revise su agenda.",
                false, null);

        // Para terapeuta3
        crearNotificacion(terapeuta3, "CITA_NUEVA", "Nueva valoración asignada",
                "Se le asignó una valoración inicial para el próximo viernes a las 11:00 AM.",
                false, null);

        // Para paciente1
        crearNotificacion(paciente1, "CITA_CONFIRMADA", "Cita confirmada",
                "Su cita de terapia con Dr. Juan Martínez ha sido confirmada para la próxima semana.",
                false, null);

        crearNotificacion(paciente1, "PAGO_APROBADO", "Pago aprobado",
                "Su pago de $350.00 por la sesión de fisioterapia ha sido verificado y aprobado.",
                true, now.minusDays(13));

        // Para paciente2
        crearNotificacion(paciente2, "CITA_COMPLETADA", "Resumen de sesión",
                "Su sesión de rehabilitación de rodilla ha sido completada. El terapeuta dejó notas de seguimiento.",
                true, now.minusDays(4));

        crearNotificacion(paciente2, "PROMOCION", "Paquete de sesiones disponible",
                "Pregunta por nuestro paquete de 5 sesiones con 10% de descuento.",
                false, null);

        // Para paciente3
        crearNotificacion(paciente3, "CITA_RECORDATORIO", "Próxima cita en 3 días",
                "Recuerde su cita con Dr. Juan Martínez programada para el miércoles a las 8:00 AM.",
                false, null);

        crearNotificacion(paciente3, "PAGO_RECHAZADO", "Comprobante de pago rechazado",
                "El comprobante que envió no es legible. Por favor suba una nueva imagen del comprobante.",
                false, null);

        // Para paciente4
        crearNotificacion(paciente4, "PAGO_PARCIAL", "Anticipo registrado correctamente",
                "Su anticipo de $200.00 para la próxima cita quedó aplicado. Restan $200.00 por cubrir.",
                false, null);

        // Para paciente5
        crearNotificacion(paciente5, "PAGO_PENDIENTE", "Pago en revisión",
                "Su comprobante ya fue recibido y está siendo validado por administración.",
                false, null);

        // Para alumno1
        crearNotificacion(alumno1, "PRESTAMO_RECORDATORIO", "Préstamo activo - Devolver hoy",
                "Recuerde devolver la Camilla Portátil antes de las 4:00 PM de hoy.",
                false, null);

        crearNotificacion(alumno1, "SANCION_INFO", "Advertencia registrada",
                "Se ha registrado una advertencia por retraso en su expediente.",
                true, now.minusDays(12));

        // Para alumno2
        crearNotificacion(alumno2, "RESERVA_CONFIRMADA", "Reserva de Sala Grupal confirmada",
                "Su reserva de la Sala Grupal para el taller de rehabilitación ha sido confirmada.",
                false, null);

        // Para alumno3
        crearNotificacion(alumno3, "SANCION_ACTIVA", "Suspensión temporal activa",
                "Tiene una suspensión activa hasta el " + LocalDate.now().plusDays(5) + " por no haber cancelado su reserva a tiempo.",
                false, null);

        crearNotificacion(alumno3, "PRESTAMO_VENCIDO", "Préstamo vencido - Acción requerida",
                "El TENS Electroestimulador que tiene en préstamo debió ser devuelto hace 2 días. Acuda a recepción.",
                false, null);

        // Para alumno4
        crearNotificacion(alumno4, "PRESTAMO_CARGO", "Cargo por reposición generado",
                "Se registró un cargo de $80.00 por reposición del material extraviado. Pase a caja para regularizarlo.",
                false, null);

        // Para alumno5
        crearNotificacion(alumno5, "RESERVA_CONFIRMADA", "Reserva y material listos",
                "Su práctica con bandas terapéuticas quedó apartada para la próxima semana en Cubículo B.",
                false, null);

        System.out.println("✅ 26 notificaciones creadas para diferentes usuarios");
    }

    private void crearNotificacion(Usuario usuario, String tipo, String titulo, String mensaje,
                                    boolean leida, LocalDateTime fechaLeida) {
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setLeida(leida);
        n.setFechaLeida(fechaLeida);
        notificacionRepository.save(n);
    }

    private void seedMovimientosInventario(Usuario admin, Usuario recep,
                                            Producto aceiteLavanda, Producto cremaArnica,
                                            Producto gelFrio, Producto vendaElastica,
                                            Producto camillaPortatil, Producto kitUltrasonido) {
        LocalDateTime now = LocalDateTime.now();

        // Entrada inicial de inventario
        crearMovimiento(aceiteLavanda, "ENTRADA", 50, 0, 50, "Inventario inicial", "INV-INIT-001", admin, now.minusDays(30));
        crearMovimiento(cremaArnica, "ENTRADA", 30, 0, 30, "Inventario inicial", "INV-INIT-002", admin, now.minusDays(30));
        crearMovimiento(gelFrio, "ENTRADA", 25, 0, 25, "Inventario inicial", "INV-INIT-003", admin, now.minusDays(30));
        crearMovimiento(vendaElastica, "ENTRADA", 100, 0, 100, "Inventario inicial", "INV-INIT-004", admin, now.minusDays(30));

        // Ventas (salidas)
        crearMovimiento(aceiteLavanda, "VENTA", 3, 50, 47, "Ventas tickets TK-0007 y TK-0009", "TK-VENTAS-001", recep, now.minusDays(7));
        crearMovimiento(cremaArnica, "VENTA", 2, 30, 28, "Ventas tickets TK-0007 y TK-0018", "TK-VENTAS-002", recep, now.minusDays(5));
        crearMovimiento(vendaElastica, "VENTA", 5, 100, 95, "Venta ticket TK-0009", "TK-VENTAS-003", recep, now.minusDays(7));
        crearMovimiento(gelFrio, "VENTA", 2, 25, 23, "Ventas tickets TK-0008 y TK-0009", "TK-VENTAS-004", recep, now.minusDays(5));

        // Reposición de stock
        crearMovimiento(aceiteLavanda, "ENTRADA", 20, 47, 67, "Reposición de proveedor - Factura F-2026-0045", "REPO-001", admin, now.minusDays(3));

        // Ajuste por merma
        crearMovimiento(cremaArnica, "AJUSTE", -2, 28, 26, "Merma por producto vencido - Lote L220", "AJUSTE-001", admin, now.minusDays(2));

        // Préstamo salida/entrada
        crearMovimiento(camillaPortatil, "PRESTAMO", -1, 5, 4, "Préstamo a Alumno1", "PREST-001", recep, now.minusDays(1));
        crearMovimiento(kitUltrasonido, "PRESTAMO", -1, 3, 2, "Préstamo a Alumno2", "PREST-002", recep, now.minusDays(5));
        crearMovimiento(kitUltrasonido, "DEVOLUCION", 1, 2, 3, "Devolución por Alumno2 - buen estado", "DEV-001", recep, now.minusDays(5).plusHours(5));

        System.out.println("✅ 13 movimientos de inventario creados (entradas, ventas, ajustes, préstamos)");
    }

    private void crearMovimiento(Producto producto, String tipo, int cantidad,
                                  int stockAnterior, int stockNuevo, String motivo,
                                  String referencia, Usuario realizadoPor, LocalDateTime fecha) {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setProducto(producto);
        mov.setTipoMovimiento(tipo);
        mov.setCantidad(cantidad);
        mov.setStockAnterior(stockAnterior);
        mov.setStockNuevo(stockNuevo);
        mov.setMotivo(motivo);
        mov.setReferencia(referencia);
        mov.setRealizadoPor(realizadoPor);
        mov.setFechaMovimiento(fecha);
        movimientoInventarioRepository.save(mov);
    }
}
