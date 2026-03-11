-- Migration V4: Add celular, actividad, maestro columns to Ticket_Espacios
-- These fields capture student contact and activity info for cubicle reservations

ALTER TABLE Ticket_Espacios ADD Celular NVARCHAR(20) NULL;
ALTER TABLE Ticket_Espacios ADD Actividad NVARCHAR(255) NULL;
ALTER TABLE Ticket_Espacios ADD Maestro NVARCHAR(255) NULL;

GO
