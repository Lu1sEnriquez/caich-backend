-- Migración inicial para desarrollo local.
-- Crea la base, el usuario de aplicación y el esquema CAICH.
-- Hibernate (ddl-auto=update) crea y evoluciona las tablas mientras el proyecto está en desarrollo.

USE master;
GO

IF DB_ID(N'CaichDB') IS NULL
BEGIN
    PRINT 'Creando CaichDB...';
    CREATE DATABASE CaichDB;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = N'myuser')
BEGIN
    CREATE LOGIN myuser WITH PASSWORD = 'UserP@ssw0rd!';
END
GO

USE CaichDB;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'myuser')
BEGIN
    CREATE USER myuser FOR LOGIN myuser;
    ALTER ROLE db_owner ADD MEMBER myuser;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'CAICH')
BEGIN
    EXEC(N'CREATE SCHEMA CAICH AUTHORIZATION dbo');
END
GO

ALTER USER myuser WITH DEFAULT_SCHEMA = CAICH;
GO
