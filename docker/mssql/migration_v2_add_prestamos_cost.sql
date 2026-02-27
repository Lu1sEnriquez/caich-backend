-- =====================================================
-- MIGRATION: Add CostoUnitario and ObservacionesDevolucion to Ticket_Prestamos
-- =====================================================
-- Descripción: Agrega campos para soportar costos opcionales en préstamos
-- y observaciones al momento de la devolución
-- Fecha: 2026-02-25
-- =====================================================

USE CaichDB;
GO

BEGIN TRANSACTION;
BEGIN TRY
    -- Verificar si la tabla Ticket_Prestamos existe
    IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Ticket_Prestamos')
    BEGIN
        PRINT 'Actualizando tabla Ticket_Prestamos...';
        
        -- Agregar columna CostoUnitario si no existe
        IF NOT EXISTS (
            SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_NAME = 'Ticket_Prestamos' AND COLUMN_NAME = 'CostoUnitario'
        )
        BEGIN
            ALTER TABLE [Ticket_Prestamos]
            ADD [CostoUnitario] DECIMAL(10,2) NULL;
            PRINT 'Columna CostoUnitario agregada';
        END
        ELSE
        BEGIN
            PRINT 'Columna CostoUnitario ya existe';
        END
        
        -- Agregar columna ObservacionesDevolucion si no existe
        IF NOT EXISTS (
            SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_NAME = 'Ticket_Prestamos' AND COLUMN_NAME = 'ObservacionesDevolucion'
        )
        BEGIN
            ALTER TABLE [Ticket_Prestamos]
            ADD [ObservacionesDevolucion] NVARCHAR(500) NULL;
            PRINT 'Columna ObservacionesDevolucion agregada';
        END
        ELSE
        BEGIN
            PRINT 'Columna ObservacionesDevolucion ya existe';
        END
        
        PRINT 'Tabla Ticket_Prestamos actualizada correctamente';
    END
    ELSE IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'PrestamosMateriales')
    BEGIN
        PRINT 'Actualizando tabla PrestamosMateriales (legacy)...';
        
        -- Agregar columna CostoUnitario si no existe
        IF NOT EXISTS (
            SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_NAME = 'PrestamosMateriales' AND COLUMN_NAME = 'CostoUnitario'
        )
        BEGIN
            ALTER TABLE [PrestamosMateriales]
            ADD [CostoUnitario] DECIMAL(10,2) NULL;
            PRINT 'Columna CostoUnitario agregada a PrestamosMateriales';
        END
        
        -- Agregar columna ObservacionesDevolucion si no existe
        IF NOT EXISTS (
            SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_NAME = 'PrestamosMateriales' AND COLUMN_NAME = 'ObservacionesDevolucion'
        )
        BEGIN
            ALTER TABLE [PrestamosMateriales]
            ADD [ObservacionesDevolucion] NVARCHAR(500) NULL;
            PRINT 'Columna ObservacionesDevolucion agregada a PrestamosMateriales';
        END
        
        PRINT 'Tabla PrestamosMateriales actualizada correctamente';
    END
    ELSE
    BEGIN
        PRINT 'ADVERTENCIA: Ni Ticket_Prestamos ni PrestamosMateriales encontradas';
        PRINT 'La tabla será creada cuando se ejecute CaichDB.sql';
    END
    
    COMMIT TRANSACTION;
    PRINT 'Migración completada exitosamente';
END TRY
BEGIN CATCH
    ROLLBACK TRANSACTION;
    PRINT 'ERROR en migración:';
    PRINT ERROR_MESSAGE();
END CATCH;
GO
