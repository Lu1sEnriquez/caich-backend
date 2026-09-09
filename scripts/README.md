# Scripts de operación de CAICH

## Despliegue Windows con IIS

La guía exacta de instalación, IIS, servicios Windows, validación y
actualizaciones está en `../../caich-front/DEPLOYMENT_IIS.md`. En Windows
Server, después de la instalación inicial, actualiza ambos componentes con:

```powershell
.\scripts\windows\deploy-caich.ps1 -Component All
```

Los scripts `reset-local-db.ps1` y `test-monedero-e2e.ps1` que se describen
abajo son exclusivamente para desarrollo local. Nunca se ejecutan en
producción.

## Reiniciar la base con el seed

Desde `caich-api` ejecuta:

```powershell
.\scripts\reset-local-db.ps1
```

El script pide escribir `REINICIAR`, elimina el volumen local de SQL Server y crea una base limpia. Después reinicia el backend y carga el seed básico: usuarios, productos, cubículos y horarios, sin tickets, pagos, recargas ni movimientos.

Para usarlo sin confirmación interactiva:

Para cargar además los escenarios completos de demostración (tickets, pagos, monederos y notificaciones), ejecuta:

```powershell
.\scripts\reset-local-db.ps1 -Demo
```

En ambos casos se eliminan las pruebas que hayas hecho manualmente.

## Probar monedero de extremo a extremo

Con SQL Server y backend ya iniciados:

```powershell
.\scripts\test-monedero-e2e.ps1
```

El test crea una recarga por Tesorería, la confirma, la aprueba como Administración y genera una reserva futura de cubículo pagada con monedero. Por eso deja registros de prueba; ejecuta primero `reset-local-db.ps1` si necesitas regresar al seed original.
