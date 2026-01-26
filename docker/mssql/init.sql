-- init.sql

-- 1. Restaurar la Base de Datos si no existe
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'CaichDB')
BEGIN
    PRINT 'Restaurando CaichDB desde backup...'

    RESTORE DATABASE CaichDB
    FROM DISK = '/usr/src/app/CaichDB.bak'
    WITH
        -- Mueve los archivos internos a la ruta de Linux del contenedor
        MOVE 'CaichDB' TO '/var/opt/mssql/data/CaichDB.mdf',
        MOVE 'CaichDB_log' TO '/var/opt/mssql/data/CaichDB_log.ldf',
        REPLACE,
        RECOVERY;
END
GO

USE CaichDB;
GO

-- 2. Crear el Login en el Servidor (Nivel Instancia)
IF NOT EXISTS (SELECT name FROM sys.sql_logins WHERE name = 'myuser')
BEGIN
    CREATE LOGIN myuser WITH PASSWORD = 'UserP@ssw0rd!';
END
GO

-- 3. Arreglar el Usuario en la BD (Usuario Huérfano)
-- Al restaurar, el usuario 'myuser' ya existe dentro de la BD, pero pierde
-- el vínculo con el Login del servidor. Hay que reconectarlos.

IF EXISTS (SELECT * FROM sys.database_principals WHERE name = 'myuser')
BEGIN
    -- Si el usuario venía en el backup, lo reconectamos al login
    ALTER USER myuser WITH LOGIN = myuser;
END
ELSE
BEGIN
    -- Si el usuario NO venía en el backup, lo creamos de cero
    CREATE USER myuser FOR LOGIN myuser;
    ALTER ROLE db_owner ADD MEMBER myuser;
END
GO