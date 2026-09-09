<#
Instala CAICH-API y CAICH-Frontend como servicios de Windows supervisados.

Ejecutar una sola vez. El script solicita elevación UAC por sí mismo, descarga
WinSW si aún no existe y conserva secretos/configuración fuera de Git en
C:\ProgramData\CAICH\config\api.env.
#>
[CmdletBinding()]
param(
    # En producción la BD ya existe y su configuración debe capturarse antes
    # de crear/iniciar el servicio.
    [switch]$Production
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Test-IsAdministrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-IsAdministrator)) {
    $productionArgument = if ($Production) { ' -Production' } else { '' }
    $process = Start-Process -FilePath 'powershell.exe' -Verb RunAs -Wait -PassThru `
        -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"$productionArgument"
    exit $process.ExitCode
}

$serviceRoot = 'C:\ProgramData\CAICH\services'
$configRoot = 'C:\ProgramData\CAICH\config'
$logRoot = 'C:\ProgramData\CAICH\logs'
$frontendRoot = 'C:\inetpub\wwwroot\caich'
$apiRoot = 'C:\inetpub\wwwroot\caich-api'
$apiJar = Join-Path $apiRoot 'caich-api.jar'
$winsw = Join-Path $serviceRoot 'WinSW-x64.exe'

foreach ($path in @($serviceRoot, $configRoot, $logRoot, 'C:\ProgramData\CAICH\uploads')) {
    New-Item -ItemType Directory -Path $path -Force | Out-Null
}

if (-not (Test-Path -LiteralPath (Join-Path $frontendRoot 'server.js'))) {
    throw "No está desplegado el frontend en $frontendRoot. Ejecuta primero .\scripts\windows\deploy-caich.ps1 -Component Frontend."
}
if (-not (Test-Path -LiteralPath $apiJar)) {
    throw "No está desplegado el JAR en $apiJar. Ejecuta primero deploy-caich.ps1 -Component Api."
}

if (-not (Test-Path -LiteralPath $winsw)) {
    Write-Host 'Descargando WinSW…' -ForegroundColor Cyan
    Invoke-WebRequest -Uri 'https://github.com/winsw/winsw/releases/latest/download/WinSW-x64.exe' -OutFile $winsw
}

$java = (Get-Command java.exe -ErrorAction Stop).Source
$node = (Get-Command node.exe -ErrorAction Stop).Source
$apiEnv = Join-Path $configRoot 'api.env'
if (-not (Test-Path -LiteralPath $apiEnv)) {
    if ($Production) {
        @"
DB_URL=CAMBIA_POR_LA_CADENA_JDBC_DE_PRODUCCION
DB_USERNAME=CAMBIA_POR_EL_USUARIO_DE_BD
DB_PASSWORD=CAMBIA_POR_LA_CONTRASENA_DE_BD
JWT_SECRET=GENERA_UN_SECRETO_BASE64_LARGO
APP_ENVIRONMENT=production
SERVER_ADDRESS=127.0.0.1
FILE_UPLOAD_DIR=C:\ProgramData\CAICH\uploads
"@ | Set-Content -LiteralPath $apiEnv -Encoding utf8 -NoNewline
        throw "Se creó $apiEnv. Completa sus valores de producción y ejecuta de nuevo este script."
    }

    $jwtBytes = [byte[]]::new(48)
    # Compatible con Windows PowerShell 5.1 y PowerShell moderno.
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($jwtBytes)
    } finally {
        $rng.Dispose()
    }
    $jwtSecret = [Convert]::ToBase64String($jwtBytes)
    @"
DB_URL=jdbc:sqlserver://localhost:1438;databaseName=CaichDB;encrypt=false;trustServerCertificate=true
DB_USERNAME=myuser
DB_PASSWORD=UserP@ssw0rd!
JWT_SECRET=$jwtSecret
APP_ENVIRONMENT=production
SERVER_ADDRESS=127.0.0.1
FILE_UPLOAD_DIR=C:\ProgramData\CAICH\uploads
"@ | Set-Content -LiteralPath $apiEnv -Encoding utf8 -NoNewline
}

if ($Production -and (Get-Content -LiteralPath $apiEnv -Raw) -match 'CAMBIA_|GENERA_') {
    throw "Completa los valores reales de producción en $apiEnv antes de instalar los servicios."
}

$apiRunner = Join-Path $serviceRoot 'run-api.cmd'
@"
@echo off
setlocal EnableExtensions
for /f "usebackq tokens=1,* delims==" %%A in ("$apiEnv") do set "%%A=%%B"
"$java" -jar "$apiJar"
"@ | Set-Content -LiteralPath $apiRunner -Encoding ascii -NoNewline

$frontendRunner = Join-Path $serviceRoot 'run-frontend.cmd'
@"
@echo off
set NODE_ENV=production
set HOSTNAME=127.0.0.1
set PORT=3000
"$node" "$frontendRoot\server.js"
"@ | Set-Content -LiteralPath $frontendRunner -Encoding ascii -NoNewline

function Install-CaichService {
    param([string]$Id, [string]$Name, [string]$Runner, [string]$WorkingDirectory)

    $serviceExe = Join-Path $serviceRoot "$Id.exe"
    $serviceXml = Join-Path $serviceRoot "$Id.xml"
    Copy-Item -LiteralPath $winsw -Destination $serviceExe -Force
    @"
<service>
  <id>$Id</id>
  <name>$Name</name>
  <description>$Name de CAICH.</description>
  <executable>C:\Windows\System32\cmd.exe</executable>
  <arguments>/c ""$Runner""</arguments>
  <workingdirectory>$WorkingDirectory</workingdirectory>
  <logpath>$logRoot</logpath>
  <log mode="roll-by-size"><sizeThreshold>10240</sizeThreshold><keepFiles>14</keepFiles></log>
  <onfailure action="restart" delay="10 sec" />
  <onfailure action="restart" delay="20 sec" />
  <onfailure action="restart" delay="30 sec" />
  <resetfailure>1 hour</resetfailure>
</service>
"@ | Set-Content -LiteralPath $serviceXml -Encoding utf8 -NoNewline

    if (-not (Get-Service -Name $Id -ErrorAction SilentlyContinue)) {
        & $serviceExe install
    }
    Set-Service -Name $Id -StartupType Automatic
    if ((Get-Service -Name $Id).Status -ne 'Running') { Start-Service -Name $Id }
}

Install-CaichService -Id 'CAICH-API' -Name 'CAICH API' -Runner $apiRunner -WorkingDirectory $apiRoot
Install-CaichService -Id 'CAICH-Frontend' -Name 'CAICH Frontend' -Runner $frontendRunner -WorkingDirectory $frontendRoot
Write-Host 'Servicios CAICH instalados y configurados para iniciar automáticamente.' -ForegroundColor Green
