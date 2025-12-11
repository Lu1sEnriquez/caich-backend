-- =====================================================
-- CATÁLOGOS BASE
-- =====================================================

CREATE DATABASE CaichDB
USE CaichDB

-- Tabla: Usuarios
-- Gestiona todos los usuarios del sistema (Admin, Terapeuta, Alumno, Paciente)
CREATE TABLE [Usuarios] (
    [UsuarioId] INT PRIMARY KEY IDENTITY(1, 1),
    [NombreCompleto] NVARCHAR(200) NOT NULL,
    [Email] NVARCHAR(150) NOT NULL,
    [Folio] NVARCHAR(50),
    [idAlumno] NVARCHAR(50), -- id del ITSON
    [PasswordHash] NVARCHAR(255) NOT NULL,
    [Telefono] NVARCHAR(20),
    [Rol] NVARCHAR(20) NOT NULL,
    [Estado] NVARCHAR(20) NOT NULL DEFAULT 'Activo',
    [Foto] NVARCHAR(500),
    
    [UltimaConexion] DATETIME,
    [FechaCreacion] DATETIME NOT NULL DEFAULT GETDATE(),
    [UltimaActualizacion] DATETIME NOT NULL DEFAULT GETDATE(),
    
    -- Constraints
    CONSTRAINT CHK_Usuarios_Rol CHECK (Rol IN ('Administrador', 'Terapeuta', 'Alumno', 'Paciente')),
    CONSTRAINT CHK_Usuarios_Estado CHECK (Estado IN ('Activo', 'Suspendido', 'Inactivo')),
    CONSTRAINT UQ_Usuarios_Email UNIQUE (Email)
);
GO

-- Índices para Usuarios
CREATE NONCLUSTERED INDEX IX_Usuarios_Rol ON [Usuarios](Rol);
CREATE NONCLUSTERED INDEX IX_Usuarios_Estado ON [Usuarios](Estado);
CREATE NONCLUSTERED INDEX IX_Usuarios_Email ON [Usuarios](Email);
GO

-- Tabla: Espacios (Cubículos/Consultorios)
-- Catálogo de espacios físicos disponibles para agendar
CREATE TABLE [Espacios] (
    [EspacioId] INT PRIMARY KEY IDENTITY(1, 1),
    [Nombre] NVARCHAR(100) NOT NULL,
    [Tipo] NVARCHAR(50) NOT NULL,
    [Descripcion] NVARCHAR(MAX),
    [Capacidad] INT NOT NULL DEFAULT 1,
    [CostoPorHora] DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    [EstaActivo] BIT NOT NULL DEFAULT 1,
    
    [FechaCreacion] DATETIME NOT NULL DEFAULT GETDATE(),
    [UltimaActualizacion] DATETIME NOT NULL DEFAULT GETDATE(),
    
    -- Constraints
    CONSTRAINT CHK_Espacios_Tipo CHECK (Tipo IN ('Cubículo', 'Consultorio', 'Sala')),
    CONSTRAINT CHK_Espacios_Capacidad CHECK (Capacidad > 0),
    CONSTRAINT CHK_Espacios_Costo CHECK (CostoPorHora >= 0)
);
GO

-- Índices para Espacios
CREATE NONCLUSTERED INDEX IX_Espacios_Activos ON [Espacios](EstaActivo);
CREATE NONCLUSTERED INDEX IX_Espacios_Tipo ON [Espacios](Tipo);
GO

-- Tabla: Productos (Inventario)
-- Catálogo de materiales, tests, libros y equipos
CREATE TABLE [Productos] (
    [ProductoId] INT PRIMARY KEY IDENTITY(1, 1),
    [Codigo] NVARCHAR(50) NOT NULL,
    [Nombre] NVARCHAR(200) NOT NULL,
    [Descripcion] NVARCHAR(MAX),
    [Categoria] NVARCHAR(50),
    [Precio] DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    [Stock] INT NOT NULL DEFAULT 0,
    [StockMinimo] INT NOT NULL DEFAULT 5,
    [EsVendible] BIT NOT NULL DEFAULT 1,
    [EsPrestable] BIT NOT NULL DEFAULT 0,
    [EstaActivo] BIT NOT NULL DEFAULT 1,
    [FechaCreacion] DATETIME NOT NULL DEFAULT GETDATE(),
    [UltimaActualizacion] DATETIME NOT NULL DEFAULT GETDATE(),
    
    -- Constraints
    CONSTRAINT CHK_Productos_Categoria CHECK (Categoria IN ('Material', 'Libro', 'Test', 'Equipo', 'Otro')),
    CONSTRAINT CHK_Productos_Precio CHECK (Precio >= 0),
    CONSTRAINT CHK_Productos_Stock CHECK (Stock >= 0),
    CONSTRAINT CHK_Productos_StockMinimo CHECK (StockMinimo >= 0),
    CONSTRAINT UQ_Productos_Codigo UNIQUE (Codigo)
);
GO

-- Índices para Productos
CREATE NONCLUSTERED INDEX IX_Productos_Codigo ON [Productos](Codigo);
CREATE NONCLUSTERED INDEX IX_Productos_Categoria ON [Productos](Categoria);
CREATE NONCLUSTERED INDEX IX_Productos_Activos ON [Productos](EstaActivo);
CREATE NONCLUSTERED INDEX IX_Productos_Stock ON [Productos](Stock);
GO

-- Tabla: CuentasBancarias
-- Catálogo de cuentas bancarias para recibir pagos
CREATE TABLE [CuentasBancarias] (
    [CuentaId] INT PRIMARY KEY IDENTITY(1, 1),
    [Banco] NVARCHAR(100) NOT NULL,
    [NumeroCuenta] NVARCHAR(50) NOT NULL,
    [CLABE] NVARCHAR(18),
    [Titular] NVARCHAR(200) NOT NULL,
    [EstaActiva] BIT NOT NULL DEFAULT 1,
    
    [FechaCreacion] DATETIME NOT NULL DEFAULT GETDATE(),
    [UltimaActualizacion] DATETIME NOT NULL DEFAULT GETDATE(),
);
GO

-- Índices para CuentasBancarias
CREATE NONCLUSTERED INDEX IX_Cuentas_Banco ON [CuentasBancarias](Banco);
CREATE NONCLUSTERED INDEX IX_Cuentas_Activas ON [CuentasBancarias](EstaActiva);
GO

-- =====================================================
-- OPERACIONES PRINCIPALES
-- =====================================================

-- Tabla: Tickets (Citas/Servicios)
-- Núcleo del sistema: registra cada cita o servicio
CREATE TABLE [Tickets] (
    [TicketId] INT PRIMARY KEY IDENTITY(1, 1),
    [Folio] NVARCHAR(50) NOT NULL,
    
    -- Relaciones
    [PacienteId] INT NOT NULL,
    [TerapeutaId] INT NOT NULL,
    [EspacioId] INT,
    [CuentaDestinoId] INT NOT NULL,
    
    -- Fechas y horarios
    [Fecha] DATETIME NOT NULL,
    [Duracion] INT, -- En minutos
    
    -- Información de la cita
    [Materia] NVARCHAR(200),
    [ConceptoIngreso] NVARCHAR(MAX),
    [ConceptoTransferencia] NVARCHAR(MAX),
    [Notas] NVARCHAR(MAX),
    [Modalidad] NVARCHAR(50) NOT NULL, -- "Presencial", "Online", etc.
    
    -- Datos de pago
    [MontoPagado] DECIMAL(10,2) NOT NULL,
    [Celular] NVARCHAR(20),
    
    -- Costos
    [CostoEspacio] DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    [CostoMateriales] DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    [CostoAdicional] DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    [CostoTotal] DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    
    -- Estados
    [EstadoTicket] NVARCHAR(20) NOT NULL DEFAULT 'Agendado',
    [EstadoPago] NVARCHAR(20) NOT NULL DEFAULT 'Pendiente',
    
    -- Auditoría
    [CreadoPor] INT,
    [FechaCreacion] DATETIME NOT NULL DEFAULT GETDATE(),
    [UltimaActualizacion] DATETIME NOT NULL DEFAULT GETDATE(),
    
    -- Constraints
    CONSTRAINT CHK_Tickets_MontoPagado CHECK (MontoPagado >= 0),
    CONSTRAINT CHK_Tickets_EstadoTicket CHECK (EstadoTicket IN ('Agendado', 'Completado', 'Cancelado', 'NoAsistio')),
    CONSTRAINT CHK_Tickets_EstadoPago CHECK (EstadoPago IN ('Pendiente', 'Pagado', 'Verificado', 'Rechazado')),
    CONSTRAINT CHK_Tickets_Duracion CHECK (Duracion IS NULL OR Duracion > 0),
    CONSTRAINT CHK_Tickets_Costos CHECK (CostoEspacio >= 0 AND CostoMateriales >= 0 AND CostoAdicional >= 0),
    CONSTRAINT UQ_Tickets_Folio UNIQUE (Folio),
    
    -- Foreign Keys
    CONSTRAINT FK_Tickets_Paciente FOREIGN KEY (PacienteId) REFERENCES [Usuarios](UsuarioId),
    CONSTRAINT FK_Tickets_Terapeuta FOREIGN KEY (TerapeutaId) REFERENCES [Usuarios](UsuarioId),
    CONSTRAINT FK_Tickets_Espacio FOREIGN KEY (EspacioId) REFERENCES [Espacios](EspacioId),
    CONSTRAINT FK_Tickets_Cuenta FOREIGN KEY (CuentaDestinoId) REFERENCES [CuentasBancarias](CuentaId),
    CONSTRAINT FK_Tickets_CreadoPor FOREIGN KEY (CreadoPor) REFERENCES [Usuarios](UsuarioId)
);
GO

-- Índices para Tickets
CREATE NONCLUSTERED INDEX IX_Tickets_Paciente ON [Tickets](PacienteId);
CREATE NONCLUSTERED INDEX IX_Tickets_Terapeuta ON [Tickets](TerapeutaId);
CREATE NONCLUSTERED INDEX IX_Tickets_Espacio ON [Tickets](EspacioId);
CREATE NONCLUSTERED INDEX IX_Tickets_Fecha ON [Tickets](Fecha);
CREATE NONCLUSTERED INDEX IX_Tickets_EstadoTicket ON [Tickets](EstadoTicket);
CREATE NONCLUSTERED INDEX IX_Tickets_EstadoPago ON [Tickets](EstadoPago);
CREATE NONCLUSTERED INDEX IX_Tickets_Folio ON [Tickets](Folio);
CREATE NONCLUSTERED INDEX IX_Tickets_FechaEstado ON [Tickets](Fecha, EstadoTicket);
GO

-- Tabla: TicketDetalles
-- Detalle de productos utilizados/vendidos en cada ticket
CREATE TABLE [TicketDetalles] (
    [DetalleId] INT PRIMARY KEY IDENTITY(1, 1),
    [TicketId] INT NOT NULL,
    [ProductoId] INT NOT NULL,
    [Cantidad] INT NOT NULL DEFAULT 1,
    [PrecioUnitario] DECIMAL(10,2) NOT NULL,
    [Subtotal] DECIMAL(10,2) NOT NULL,
    [TipoUso] NVARCHAR(20),
    
    [FechaCreacion] DATETIME NOT NULL DEFAULT GETDATE(),
    [UltimaActualizacion] DATETIME NOT NULL DEFAULT GETDATE(),
    
    -- Constraints
    CONSTRAINT CHK_TicketDetalles_Cantidad CHECK (Cantidad > 0),
    CONSTRAINT CHK_TicketDetalles_Precio CHECK (PrecioUnitario >= 0),
    CONSTRAINT CHK_TicketDetalles_TipoUso CHECK (TipoUso IN ('Venta', 'Prestamo', 'Uso')),
    
    -- Foreign Keys
    CONSTRAINT FK_TicketDetalles_Ticket FOREIGN KEY (TicketId) REFERENCES [Tickets](TicketId) ON DELETE CASCADE,
    CONSTRAINT FK_TicketDetalles_Producto FOREIGN KEY (ProductoId) REFERENCES [Productos](ProductoId)
);
GO

-- Índices para TicketDetalles
CREATE NONCLUSTERED INDEX IX_TicketDetalles_Ticket ON [TicketDetalles](TicketId);
CREATE NONCLUSTERED INDEX IX_TicketDetalles_Producto ON [TicketDetalles](ProductoId);
GO

-- Tabla: PrestamosMateriales
-- Registro específico de préstamos de materiales
CREATE TABLE [PrestamosMateriales] (
    [PrestamoId] INT PRIMARY KEY IDENTITY(1, 1),
    [TicketId] INT,
    [AlumnoId] INT NOT NULL,
    [ProductoId] INT NOT NULL,
    
    -- Fechas del préstamo
    [FechaPrestamo] DATETIME NOT NULL,
    [FechaDevolucionEstimada] DATETIME NOT NULL,
    [FechaDevolucionReal] DATETIME,
    
    [Cantidad] INT NOT NULL DEFAULT 1,
    [EstadoPrestamo] NVARCHAR(20) NOT NULL DEFAULT 'Activo',
    [Observaciones] NVARCHAR(MAX),
    
    [FechaCreacion] DATETIME NOT NULL DEFAULT GETDATE(),
    [UltimaActualizacion] DATETIME NOT NULL DEFAULT GETDATE(),
    
    -- Constraints
    CONSTRAINT CHK_Prestamos_Cantidad CHECK (Cantidad > 0),
    CONSTRAINT CHK_Prestamos_Estado CHECK (EstadoPrestamo IN ('Activo', 'Devuelto', 'Vencido', 'Perdido')),
    CONSTRAINT CHK_Prestamos_Fechas CHECK (FechaDevolucionEstimada >= FechaPrestamo),
    
    -- Foreign Keys
    CONSTRAINT FK_Prestamos_Ticket FOREIGN KEY (TicketId) REFERENCES [Tickets](TicketId),
    CONSTRAINT FK_Prestamos_Alumno FOREIGN KEY (AlumnoId) REFERENCES [Usuarios](UsuarioId),
    CONSTRAINT FK_Prestamos_Producto FOREIGN KEY (ProductoId) REFERENCES [Productos](ProductoId)
);
GO

-- Índices para PrestamosMateriales
CREATE NONCLUSTERED INDEX IX_Prestamos_Alumno ON [PrestamosMateriales](AlumnoId);
CREATE NONCLUSTERED INDEX IX_Prestamos_Producto ON [PrestamosMateriales](ProductoId);
CREATE NONCLUSTERED INDEX IX_Prestamos_Estado ON [PrestamosMateriales](EstadoPrestamo);
CREATE NONCLUSTERED INDEX IX_Prestamos_Fecha ON [PrestamosMateriales](FechaPrestamo);
CREATE NONCLUSTERED INDEX IX_Prestamos_FechaDevolucion ON [PrestamosMateriales](FechaDevolucionEstimada);
GO

-- =====================================================
-- CONFIGURACIÓN Y HORARIOS
-- =====================================================

-- Tabla: HorariosDisponibles
-- Define los horarios de operación del centro
CREATE TABLE [HorariosDisponibles] (
    [HorarioId] INT PRIMARY KEY IDENTITY(1, 1),
    [DiaSemana] INT NOT NULL,
    [HoraInicio] TIME NOT NULL,
    [HoraFin] TIME NOT NULL,
    [EstaActivo] BIT NOT NULL DEFAULT 1,
    
    -- Constraints
    CONSTRAINT CHK_Horarios_Dia CHECK (DiaSemana BETWEEN 0 AND 6), -- 0=Domingo, 6=Sábado
    CONSTRAINT CHK_Horarios_Horas CHECK (HoraFin > HoraInicio),
    CONSTRAINT UQ_Horarios_Dia UNIQUE (DiaSemana, HoraInicio, HoraFin)
);
GO

-- Índices para HorariosDisponibles
CREATE NONCLUSTERED INDEX IX_Horarios_Dia ON [HorariosDisponibles](DiaSemana);
CREATE NONCLUSTERED INDEX IX_Horarios_Activos ON [HorariosDisponibles](EstaActivo);
GO

-- Tabla: BloqueoHorarios
-- Bloqueos específicos de fechas/horas (vacaciones, eventos especiales)
CREATE TABLE [BloqueoHorarios] (
    [BloqueoId] INT PRIMARY KEY IDENTITY(1, 1),
    [EspacioId] INT, -- NULL = bloqueo global
    [FechaInicio] DATETIME NOT NULL,
    [FechaFin] DATETIME NOT NULL,
    [Motivo] NVARCHAR(300),
    [TodoElDia] BIT NOT NULL DEFAULT 0,
    [FechaCreacion] DATETIME NOT NULL DEFAULT GETDATE(),
    [UltimaActualizacion] DATETIME NOT NULL DEFAULT GETDATE(),
    [CreadoPor] INT,
    
    -- Constraints
    CONSTRAINT CHK_Bloqueos_Fechas CHECK (FechaFin > FechaInicio),
    
    -- Foreign Keys
    CONSTRAINT FK_Bloqueos_Espacio FOREIGN KEY (EspacioId) REFERENCES [Espacios](EspacioId),
    CONSTRAINT FK_Bloqueos_Usuario FOREIGN KEY (CreadoPor) REFERENCES [Usuarios](UsuarioId)
);
GO

-- Índices para BloqueoHorarios
CREATE NONCLUSTERED INDEX IX_Bloqueos_Espacio ON [BloqueoHorarios](EspacioId);
CREATE NONCLUSTERED INDEX IX_Bloqueos_Fecha ON [BloqueoHorarios](FechaInicio, FechaFin);
GO

-- Tabla: ConfiguracionEspacioHorario
-- Horarios específicos deshabilitados por espacio y fecha
CREATE TABLE [ConfiguracionEspacioHorario] (
    [ConfigId] INT PRIMARY KEY IDENTITY(1, 1),
    [EspacioId] INT NOT NULL,
    [Fecha] DATE NOT NULL,
    [HoraInicio] TIME NOT NULL,
    [HoraFin] TIME,
    [EstaDisponible] BIT NOT NULL DEFAULT 0,
    [FechaCreacion] DATETIME NOT NULL DEFAULT GETDATE(),
    [UltimaActualizacion] DATETIME NOT NULL DEFAULT GETDATE(),
    
    -- Constraints
    CONSTRAINT UQ_Config_Horario UNIQUE (EspacioId, Fecha, HoraInicio),
    
    -- Foreign Keys
    CONSTRAINT FK_Config_Espacio FOREIGN KEY (EspacioId) REFERENCES [Espacios](EspacioId)
);
GO

-- Índices para ConfiguracionEspacioHorario
CREATE NONCLUSTERED INDEX IX_Config_EspacioFecha ON [ConfiguracionEspacioHorario](EspacioId, Fecha);
GO

-- =====================================================
-- AUDITORÍA Y REPORTES
-- =====================================================

-- Tabla: HistorialCambios
-- Auditoría de cambios importantes en el sistema
CREATE TABLE [HistorialCambios] (
    [CambioId] INT PRIMARY KEY IDENTITY(1, 1),
    [Entidad] NVARCHAR(50) NOT NULL,
    [EntidadId] INT NOT NULL,
    [Accion] NVARCHAR(20) NOT NULL,
    [CamposModificados] NVARCHAR(MAX),
    [ValoresAnteriores] NVARCHAR(MAX),
    [ValoresNuevos] NVARCHAR(MAX),
    [UsuarioId] INT,
    [FechaCambio] DATETIME NOT NULL DEFAULT GETDATE(),
    [DireccionIP] NVARCHAR(45),
    
    -- Constraints
    CONSTRAINT CHK_Historial_Accion CHECK (Accion IN ('Crear', 'Actualizar', 'Eliminar')),
    
    -- Foreign Keys
    CONSTRAINT FK_Historial_Usuario FOREIGN KEY (UsuarioId) REFERENCES [Usuarios](UsuarioId)
);
GO

-- Índices para HistorialCambios
CREATE NONCLUSTERED INDEX IX_Historial_Entidad ON [HistorialCambios](Entidad, EntidadId);
CREATE NONCLUSTERED INDEX IX_Historial_Usuario ON [HistorialCambios](UsuarioId);
CREATE NONCLUSTERED INDEX IX_Historial_Fecha ON [HistorialCambios](FechaCambio);
GO

-- Tabla: MovimientosInventario
-- Registro de entrada/salida de productos del inventario
CREATE TABLE [MovimientosInventario] (
    [MovimientoId] INT PRIMARY KEY IDENTITY(1, 1),
    [ProductoId] INT NOT NULL,
    [TipoMovimiento] NVARCHAR(20) NOT NULL,
    [Cantidad] INT NOT NULL,
    [StockAnterior] INT NOT NULL,
    [StockNuevo] INT NOT NULL,
    [Motivo] NVARCHAR(200),
    [Referencia] NVARCHAR(100), -- Puede ser TicketId, PrestamoId, etc.
    [RealizadoPor] INT,
    [FechaMovimiento] DATETIME NOT NULL DEFAULT GETDATE(),
    
    [FechaCreacion] DATETIME NOT NULL DEFAULT GETDATE(),
    [UltimaActualizacion] DATETIME NOT NULL DEFAULT GETDATE(),
    
    -- Constraints
    CONSTRAINT CHK_Movimientos_Tipo CHECK (TipoMovimiento IN ('Entrada', 'Salida', 'Ajuste', 'Devolucion')),
    
    -- Foreign Keys
    CONSTRAINT FK_Movimientos_Producto FOREIGN KEY (ProductoId) REFERENCES [Productos](ProductoId),
    CONSTRAINT FK_Movimientos_Usuario FOREIGN KEY (RealizadoPor) REFERENCES [Usuarios](UsuarioId)
);
GO

-- Índices para MovimientosInventario
CREATE NONCLUSTERED INDEX IX_Movimientos_Producto ON [MovimientosInventario](ProductoId);
CREATE NONCLUSTERED INDEX IX_Movimientos_Tipo ON [MovimientosInventario](TipoMovimiento);
CREATE NONCLUSTERED INDEX IX_Movimientos_Fecha ON [MovimientosInventario](FechaMovimiento);
GO

-- Tabla: Notificaciones
-- Sistema de notificaciones para usuarios
CREATE TABLE [Notificaciones] (
    [NotificacionId] INT PRIMARY KEY IDENTITY(1, 1),
    [UsuarioId] INT NOT NULL,
    [Tipo] NVARCHAR(50) NOT NULL,
    [Titulo] NVARCHAR(200) NOT NULL,
    [Mensaje] NVARCHAR(MAX) NOT NULL,
    [Leida] BIT NOT NULL DEFAULT 0,
    [FechaLeida] DATETIME,
    [ReferenciaId] INT,
    [ReferenciaUrl] NVARCHAR(300),
    
    [FechaCreacion] DATETIME NOT NULL DEFAULT GETDATE(),
    [UltimaActualizacion] DATETIME NOT NULL DEFAULT GETDATE(),
    
    -- Constraints
    CONSTRAINT CHK_Notificaciones_Tipo CHECK (Tipo IN ('CitaProxima', 'PagoVerificado', 'PagoRechazado', 'MaterialVencido', 'StockBajo', 'General')),
    
    -- Foreign Keys
    CONSTRAINT FK_Notificaciones_Usuario FOREIGN KEY (UsuarioId) REFERENCES [Usuarios](UsuarioId)
);
GO

-- Índices para Notificaciones
CREATE NONCLUSTERED INDEX IX_Notificaciones_Usuario ON [Notificaciones](UsuarioId);
CREATE NONCLUSTERED INDEX IX_Notificaciones_Leida ON [Notificaciones](Leida);
CREATE NONCLUSTERED INDEX IX_Notificaciones_Tipo ON [Notificaciones](Tipo);
CREATE NONCLUSTERED INDEX IX_Notificaciones_Fecha ON [Notificaciones](FechaCreacion);
GO


CREATE TABLE RefreshTokens (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    UsuarioId INT NOT NULL,
    token NVARCHAR(500) NOT NULL UNIQUE,
    expiryDate DATETIME2 NOT NULL,
    fechaCreacion DATETIME2 NOT NULL DEFAULT GETDATE(),
    activo BIT NOT NULL DEFAULT 1,
    
    CONSTRAINT FK_RefreshTokens_Usuario FOREIGN KEY (UsuarioId) REFERENCES Usuarios(UsuarioId) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IX_RefreshTokens_Usuario ON RefreshTokens(UsuarioId) WHERE activo = 1;
CREATE INDEX IX_RefreshTokens_Expiry ON RefreshTokens(expiryDate);

-- =====================================================
-- DOCUMENTACIÓN DE PROPIEDADES EXTENDIDAS
-- =====================================================

EXEC sp_addextendedproperty
@name = N'MS_Description',
@value = 'Usuario del sistema: Administrador, Terapeuta, Alumno o Paciente',
@level0type = N'SCHEMA', @level0name = 'dbo',
@level1type = N'TABLE', @level1name = 'Usuarios';
GO

EXEC sp_addextendedproperty
@name = N'MS_Description',
@value = 'Núcleo del sistema - Registra citas/servicios prestados',
@level0type = N'SCHEMA', @level0name = 'dbo',
@level1type = N'TABLE', @level1name = 'Tickets';
GO

EXEC sp_addextendedproperty
@name = N'MS_Description',
@value = 'Duración de la cita en minutos - Calculado en backend',
@level0type = N'SCHEMA', @level0name = 'dbo',
@level1type = N'TABLE', @level1name = 'Tickets',
@level2type = N'COLUMN', @level2name = 'Duracion';
GO

EXEC sp_addextendedproperty
@name = N'MS_Description',
@value = 'Suma de CostoEspacio + CostoMateriales + CostoAdicional - Calculado en backend',
@level0type = N'SCHEMA', @level0name = 'dbo',
@level1type = N'TABLE', @level1name = 'Tickets',
@level2type = N'COLUMN', @level2name = 'CostoTotal';
GO

EXEC sp_addextendedproperty
@name = N'MS_Description',
@value = 'Productos utilizados en cada ticket',
@level0type = N'SCHEMA', @level0name = 'dbo',
@level1type = N'TABLE', @level1name = 'TicketDetalles';
GO

EXEC sp_addextendedproperty
@name = N'MS_Description',
@value = 'Cantidad * PrecioUnitario - Calculado en backend',
@level0type = N'SCHEMA', @level0name = 'dbo',
@level1type = N'TABLE', @level1name = 'TicketDetalles',
@level2type = N'COLUMN', @level2name = 'Subtotal';
GO