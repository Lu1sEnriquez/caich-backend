<#
Compila, despliega y reinicia de forma ordenada los servicios de CAICH.
Uso: .\scripts\windows\deploy-caich.ps1 -Component All|Api|Frontend
#>
[CmdletBinding()]
param(
    [ValidateSet('All', 'Api', 'Frontend')]
    [string]$Component = 'All'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Test-IsAdministrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-IsAdministrator)) {
    $process = Start-Process -FilePath 'powershell.exe' -Verb RunAs -Wait -PassThru `
        -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`" -Component $Component"
    exit $process.ExitCode
}

$apiRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$workspaceRoot = Split-Path -Parent $apiRoot
$frontendRoot = Join-Path $workspaceRoot 'caich-front'
$apiDestination = 'C:\inetpub\wwwroot\caich-api'
$apiJarSource = Join-Path $apiRoot 'target\api-caich-0.0.1-SNAPSHOT.jar'
$apiJarDestination = Join-Path $apiDestination 'caich-api.jar'

function Stop-CaichService([string]$Name) {
    $service = Get-Service -Name $Name -ErrorAction SilentlyContinue
    if ($service -and $service.Status -ne 'Stopped') { Stop-Service -Name $Name -Force }
}
function Start-CaichService([string]$Name) {
    if (Get-Service -Name $Name -ErrorAction SilentlyContinue) { Start-Service -Name $Name }
}

$apiServiceExists = $null -ne (Get-Service -Name 'CAICH-API' -ErrorAction SilentlyContinue)
if ($Component -in @('All', 'Api')) {
    if ($apiServiceExists) { Stop-CaichService 'CAICH-API' }
    Push-Location $apiRoot
    try {
        & .\mvnw.cmd clean package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw 'Falló el empaquetado del backend.' }
    } finally { Pop-Location }
    New-Item -ItemType Directory -Path $apiDestination -Force | Out-Null
    Copy-Item -LiteralPath $apiJarSource -Destination $apiJarDestination -Force
    if ($apiServiceExists) { Start-CaichService 'CAICH-API' }
}

if ($Component -in @('All', 'Frontend')) {
    Stop-CaichService 'CAICH-Frontend'
    & (Join-Path $frontendRoot 'deploy-standalone.ps1') -Destination 'C:\inetpub\wwwroot\caich' -ApiBaseUrl '/caich-api' -BasePath '/caich'
    Start-CaichService 'CAICH-Frontend'
}

if ($Component -in @('All', 'Api') -and $apiServiceExists) {
    $deadline = (Get-Date).AddSeconds(90)
    do {
        try {
            $health = Invoke-RestMethod -Uri 'http://localhost:8080/caich-api/health' -TimeoutSec 3
            if ($health.status -eq 'SUCCESS') { break }
        } catch { }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $deadline)
    if ($health.status -ne 'SUCCESS') { throw 'CAICH-API no respondió correctamente después del despliegue.' }
} elseif ($Component -in @('All', 'Api')) {
    Write-Host 'JAR desplegado. Instala el servicio con setup-caich-services.ps1 para iniciarlo automáticamente.' -ForegroundColor Yellow
}

Write-Host "Despliegue $Component finalizado." -ForegroundColor Green
