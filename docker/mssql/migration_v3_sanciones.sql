-- =============================================
-- Script de Migración: Agregar tabla de Sanciones
-- Versión: 1.0
-- Descripción: Crea la tabla Sanciones para el seguimiento de sanciones a alumnos
-- =============================================

USE CaichDB;
GO

-- Crear tabla de Sanciones
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[Sanciones]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[Sanciones] (
        [SancionId] BIGINT IDENTITY(1,1) NOT NULL,
        [AlumnoId] BIGINT NOT NULL,
        [TipoSancion] NVARCHAR(50) NOT NULL,
        [Motivo] NVARCHAR(1000) NOT NULL,
        [Descripcion] NVARCHAR(2000) NULL,
        [FechaInicio] DATE NOT NULL,
        [FechaFin] DATE NULL,
        [Estado] NVARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
        [CreadoPor] BIGINT NOT NULL,
        [AnuladoPor] BIGINT NULL,
        [FechaAnulacion] DATETIME2(7) NULL,
        [MotivoAnulacion] NVARCHAR(500) NULL,
        [TicketRelacionado] BIGINT NULL,
        [FechaCreacion] DATETIME2(7) NOT NULL DEFAULT GETDATE(),
        [UltimaActualizacion] DATETIME2(7) NOT NULL DEFAULT GETDATE(),
        
        CONSTRAINT [PK_Sanciones] PRIMARY KEY CLUSTERED ([SancionId] ASC),
        CONSTRAINT [FK_Sanciones_Alumno] FOREIGN KEY([AlumnoId]) 
            REFERENCES [dbo].[Usuarios] ([UsuarioId]),
        CONSTRAINT [FK_Sanciones_CreadoPor] FOREIGN KEY([CreadoPor]) 
            REFERENCES [dbo].[Usuarios] ([UsuarioId]),
        CONSTRAINT [FK_Sanciones_AnuladoPor] FOREIGN KEY([AnuladoPor]) 
            REFERENCES [dbo].[Usuarios] ([UsuarioId]),
        CONSTRAINT [FK_Sanciones_TicketRelacionado] FOREIGN KEY([TicketRelacionado]) 
            REFERENCES [dbo].[Tickets] ([TicketId])
    );

    PRINT 'Tabla Sanciones creada exitosamente';
END
ELSE
BEGIN
    PRINT 'La tabla Sanciones ya existe';
END
GO

-- Crear índices para mejorar rendimiento
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Sanciones_AlumnoId')
BEGIN
    CREATE NONCLUSTERED INDEX [IX_Sanciones_AlumnoId] 
    ON [dbo].[Sanciones] ([AlumnoId]);
    
    PRINT 'Índice IX_Sanciones_AlumnoId creado';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Sanciones_Estado')
BEGIN
    CREATE NONCLUSTERED INDEX [IX_Sanciones_Estado] 
    ON [dbo].[Sanciones] ([Estado]);
    
    PRINT 'Índice IX_Sanciones_Estado creado';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Sanciones_AlumnoEstado')
BEGIN
    CREATE NONCLUSTERED INDEX [IX_Sanciones_AlumnoEstado] 
    ON [dbo].[Sanciones] ([AlumnoId], [Estado]);
    
    PRINT 'Índice IX_Sanciones_AlumnoEstado creado';
END
GO

-- Agregar constraint de validación para TipoSancion
IF NOT EXISTS (SELECT * FROM sys.check_constraints WHERE name = 'CK_Sanciones_TipoSancion')
BEGIN
    ALTER TABLE [dbo].[Sanciones]
    ADD CONSTRAINT [CK_Sanciones_TipoSancion] 
    CHECK ([TipoSancion] IN ('ADVERTENCIA', 'SUSPENSION', 'SUSPENSION_PERMANENTE'));
    
    PRINT 'Constraint CK_Sanciones_TipoSancion creado';
END
GO

-- Agregar constraint de validación para Estado
IF NOT EXISTS (SELECT * FROM sys.check_constraints WHERE name = 'CK_Sanciones_Estado')
BEGIN
    ALTER TABLE [dbo].[Sanciones]
    ADD CONSTRAINT [CK_Sanciones_Estado] 
    CHECK ([Estado] IN ('ACTIVA', 'CUMPLIDA', 'ANULADA'));
    
    PRINT 'Constraint CK_Sanciones_Estado creado';
END
GO

PRINT 'Migración de Sanciones completada exitosamente';
GO
