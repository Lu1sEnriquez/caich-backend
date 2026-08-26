<#
Reinicia por completo la base local de CAICH y vuelve a cargar el seed.

ELIMINA solo el volumen Docker local `caich-api_sqlserver_data`:
usuarios, tickets, pagos, monederos, inventario y cualquier prueba manual.
Después inicia SQL Server y reinicia el backend para que Hibernate cree las
tablas y DataInitializer cargue el seed básico sin tickets.

Uso:
  .\scripts\reset-local-db.ps1
  .\scripts\reset-local-db.ps1 -Force
  .\scripts\reset-local-db.ps1 -Demo
  .\scripts\reset-local-db.ps1 -NoReiniciarBackend

Nunca usar este script en producción.
#>

[CmdletBinding()]
param(
    [switch]$Force,
    [switch]$NoReiniciarBackend,
    [switch]$Demo
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot 'target\api-caich-0.0.1-SNAPSHOT.jar'
$backendLog = Join-Path $projectRoot 'backend-run.log'
$backendErrorLog = Join-Path $projectRoot 'backend-run-error.log'

if (-not $Force) {
    $answer = Read-Host 'Se eliminarán todos los datos de la base LOCAL y se recargará el seed. Escribe REINICIAR para continuar'
    if ($answer -ne 'REINICIAR') {
        Write-Host 'Operación cancelada.' -ForegroundColor Yellow
        exit 0
    }
}

Push-Location $projectRoot
try {
    Write-Host 'Eliminando el volumen local de SQL Server...' -ForegroundColor Yellow
    & docker compose down -v
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo detener SQL Server local.' }

    Write-Host 'Creando una nueva base local...' -ForegroundColor Cyan
    & docker compose up -d --build
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar SQL Server local.' }

    $limit = (Get-Date).AddSeconds(90)
    do {
        $sqlStatus = (& docker inspect -f '{{.State.Health.Status}}' sqlserver_caich 2>$null).Trim()
        if ($sqlStatus -eq 'healthy') { break }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $limit)
    if ($sqlStatus -ne 'healthy') { throw 'SQL Server no quedó listo en el tiempo esperado.' }

    if (-not $NoReiniciarBackend) {
        if (-not (Test-Path -LiteralPath $jarPath)) {
            throw "No existe el backend compilado: $jarPath"
        }

        # Solo se detiene el proceso que escucha el backend local en 8080.
        $backendConnection = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($backendConnection) {
            & taskkill /PID $backendConnection.OwningProcess /F | Out-Null
            Start-Sleep -Seconds 2
        }

        $backendArgs = @('-jar', $jarPath)
        if ($Demo) { $backendArgs += '--app.seed.demo=true' }

        $seedMode = if ($Demo) { 'demo con tickets de ejemplo' } else { 'básico sin tickets' }
        Write-Host "Iniciando backend y cargando seed $seedMode..." -ForegroundColor Cyan
        Start-Process -FilePath 'java' -ArgumentList $backendArgs `
            -WorkingDirectory $projectRoot -WindowStyle Hidden `
            -RedirectStandardOutput $backendLog -RedirectStandardError $backendErrorLog

        $limit = (Get-Date).AddSeconds(90)
        do {
            try {
                $health = Invoke-RestMethod -Uri 'http://localhost:8080/api/health' -TimeoutSec 3
                if ($health.status -eq 'SUCCESS') { break }
            } catch { }
            Start-Sleep -Seconds 3
        } while ((Get-Date) -lt $limit)
        if ($health.status -ne 'SUCCESS') { throw 'El backend no respondió correctamente después del reinicio.' }
    }

    Write-Host ''
    Write-Host 'BASE LOCAL REINICIADA Y SEED CARGADO.' -ForegroundColor Green
    if ($Demo) {
        Write-Host 'Escenarios demo con tickets cargados. Contraseña de seed: 123456'
    } else {
        Write-Host 'Seed básico cargado: usuarios, productos, cubículos y horarios; sin tickets ni pagos.'
    }
}
finally {
    Pop-Location
}
