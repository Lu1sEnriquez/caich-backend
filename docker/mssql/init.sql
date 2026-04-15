-- init.sql
USE master;
GO

-- 1. Eliminar la BD si existe para limpiar el desastre de tipos de datos
IF EXISTS (SELECT name FROM sys.databases WHERE name = 'CaichDB')
BEGIN
    PRINT 'Eliminando base de datos existente para limpieza profunda...';
    ALTER DATABASE CaichDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE CaichDB;
END
GO

-- 2. Crear la Base de Datos limpia
PRINT 'Creando CaichDB desde cero...';
CREATE DATABASE CaichDB;
GO

USE CaichDB;
GO

-- 3. Crear el Login en el Servidor si no existe
IF NOT EXISTS (SELECT name FROM sys.sql_logins WHERE name = 'myuser')
BEGIN
    CREATE LOGIN myuser WITH PASSWORD = 'UserP@ssw0rd!';
END
GO

-- 4. Crear el Usuario en la BD y darle permisos de dueño
IF NOT EXISTS (SELECT * FROM sys.database_principals WHERE name = 'myuser')
BEGIN
    CREATE USER myuser FOR LOGIN myuser;
    EXEC sp_addrolemember 'db_owner', 'myuser';
END
GO

-- 5. Crear el esquema CAICH y dejarlo como predeterminado
IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = 'CAICH')
BEGIN
    EXEC('CREATE SCHEMA CAICH AUTHORIZATION dbo');
END
GO

ALTER USER myuser WITH DEFAULT_SCHEMA = CAICH;
GO