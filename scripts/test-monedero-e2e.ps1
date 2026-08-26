<#
Prueba E2E local del flujo de monedero.

Valida:
  1. Flujo directo: Tesorería confirma el pago y acredita el saldo.
  2. Flujo con recibo: al adjuntarlo se envía automáticamente a Finanzas y Administración acredita el saldo.
  3. Sólo el dueño puede ver el ticket y puede cancelar una solicitud aún pendiente.
  4. Alumno reserva un cubículo y paga desde su monedero.

La prueba crea datos reales en la base LOCAL: una recarga aprobada y una reserva.
Ejecutar con el backend y SQL Server iniciados:
  .\scripts\test-monedero-e2e.ps1
#>

[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080/api',
    [decimal]$MontoRecarga = 500.00
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

function Assert-That {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw "FALLO E2E: $Message" }
}

function Invoke-CaichJson {
    param(
        [ValidateSet('GET', 'POST')][string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$Body
    )

    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    $params = @{ Method = $Method; Uri = "$BaseUrl$Path"; Headers = $headers }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json'
        $params.Body = ($Body | ConvertTo-Json -Depth 8 -Compress)
    }
    try {
        return Invoke-RestMethod @params
    }
    catch {
        throw "Solicitud $Method $Path falló: $($_.Exception.Message)"
    }
}

function Invoke-CaichBinary {
    param(
        [string]$Path,
        [string]$Token
    )

    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    try {
        $response = Invoke-WebRequest -Method GET -Uri "$BaseUrl$Path" -Headers $headers -UseBasicParsing
        return [int]$response.StatusCode
    }
    catch {
        throw "Solicitud binaria GET $Path falló: $($_.Exception.Message)"
    }
}

function Invoke-CaichMultipart {
    param(
        [string]$Path,
        [string]$Token,
        [hashtable]$Fields,
        [hashtable]$JsonParts = @{},
        [hashtable]$Files = @{}
    )

    $client = [System.Net.Http.HttpClient]::new()
    $form = [System.Net.Http.MultipartFormDataContent]::new()
    try {
        $client.DefaultRequestHeaders.Authorization =
            [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $Token)

        foreach ($entry in $Fields.GetEnumerator()) {
            $form.Add([System.Net.Http.StringContent]::new([string]$entry.Value), [string]$entry.Key)
        }
        foreach ($entry in $JsonParts.GetEnumerator()) {
            $content = [System.Net.Http.StringContent]::new(
                ($entry.Value | ConvertTo-Json -Depth 8 -Compress),
                [System.Text.Encoding]::UTF8,
                'application/json')
            $form.Add($content, [string]$entry.Key)
        }
        foreach ($entry in $Files.GetEnumerator()) {
            $fileSpec = $entry.Value
            $filePath = [string]$fileSpec.Path
            Assert-That (Test-Path -LiteralPath $filePath) "No se encontró el archivo de prueba: $filePath"
            $content = [System.Net.Http.ByteArrayContent]::new([System.IO.File]::ReadAllBytes($filePath))
            $content.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse([string]$fileSpec.ContentType)
            $form.Add($content, [string]$entry.Key, [System.IO.Path]::GetFileName($filePath))
        }

        $response = $client.PostAsync("$BaseUrl$Path", $form).GetAwaiter().GetResult()
        $payload = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "HTTP $([int]$response.StatusCode) en $Path`: $payload"
        }
        return $payload | ConvertFrom-Json
    }
    finally {
        $form.Dispose()
        $client.Dispose()
    }
}

function Login([string]$Email) {
    $response = Invoke-CaichJson -Method POST -Path '/auth/login' -Body @{ email = $Email; password = '123456' }
    Assert-That ($null -ne $response.data.accessToken) "No se pudo iniciar sesión como $Email"
    return $response.data.accessToken
}

function Get-DisponibleParaReserva([string]$AlumnoToken) {
    $espaciosResponse = Invoke-CaichJson -Method GET -Path '/espacios' -Token $AlumnoToken
    $espacios = @($espaciosResponse.data)
    Assert-That ($espacios.Count -gt 0) 'No hay espacios configurados para la prueba.'

    foreach ($offset in 1..45) {
        $fecha = (Get-Date).Date.AddDays($offset).ToString('yyyy-MM-dd')
        foreach ($espacio in $espacios) {
            $disponibilidad = Invoke-CaichJson -Method GET -Path "/reservas/alumno/disponibilidad/$($espacio.espacioId)/$fecha" -Token $AlumnoToken
            $slot = @($disponibilidad.data.slots | Where-Object { $_.disponible }) | Select-Object -First 1
            if ($slot) {
                return @{ EspacioId = $espacio.espacioId; Fecha = $fecha; Inicio = $slot.inicio; Fin = $slot.fin }
            }
        }
    }
    throw 'No se encontró un horario disponible para reservar durante los próximos 45 días.'
}

Write-Host 'Iniciando prueba E2E de monedero...' -ForegroundColor Cyan
$alumnoToken = Login 'alumno1@caich.com'
$alumno2Token = Login 'alumno2@caich.com'
$tesoreriaToken = Login 'tesoreria@caich.com'
$adminToken = Login 'admin@caich.com'

$saldoInicial = [decimal](Invoke-CaichJson -Method GET -Path '/monedero/mi-saldo' -Token $alumnoToken).data.saldo

# 1) Flujo directo: alumno solicita y Tesorería acredita el pago en ventanilla.
$recargaDirecta = Invoke-CaichMultipart -Path '/monedero/recargas/tesoreria' -Token $alumnoToken -Fields @{} -JsonParts @{
    datos = @{ monto = $MontoRecarga; concepto = 'Prueba E2E - confirmación directa de Tesorería' }
}
Assert-That ($recargaDirecta.data.estado -eq 'PENDIENTE_TESORERIA') 'La recarga directa no quedó pendiente de Tesorería.'
$recargaDirectaId = $recargaDirecta.data.recargaMonederoId

$confirmadaDirecta = Invoke-CaichJson -Method POST -Path "/monedero/recargas/tesoreria/$recargaDirectaId/confirmar" -Token $tesoreriaToken
Assert-That ($confirmadaDirecta.data.estado -eq 'APROBADO') 'Tesorería no acreditó la recarga directa.'
$saldoTrasTesoreria = [decimal](Invoke-CaichJson -Method GET -Path '/monedero/mi-saldo' -Token $alumnoToken).data.saldo
Assert-That ($saldoTrasTesoreria -eq ($saldoInicial + $MontoRecarga)) 'El saldo no aumentó al confirmar Tesorería.'

# 2) Flujo con recibo: alumno adjunta el recibo de ventanilla y Finanzas lo revisa.
$recargaFinanzas = Invoke-CaichMultipart -Path '/monedero/recargas/tesoreria' -Token $alumnoToken -Fields @{} -JsonParts @{
    datos = @{ monto = $MontoRecarga; concepto = 'Prueba E2E - recibo para Finanzas' }
}
Assert-That ($recargaFinanzas.data.estado -eq 'PENDIENTE_TESORERIA') 'La recarga para Finanzas no quedó pendiente de Tesorería.'
$recargaFinanzasId = $recargaFinanzas.data.recargaMonederoId

$reciboPrueba = Join-Path ([System.IO.Path]::GetTempPath()) "caich-recibo-e2e-$([guid]::NewGuid().ToString('N')).png"
[System.IO.File]::WriteAllBytes($reciboPrueba, [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9JfOAAAAAASUVORK5CYII='))
try {
    $conRecibo = Invoke-CaichMultipart -Path "/monedero/recargas/tesoreria/$recargaFinanzasId/comprobante" -Token $alumnoToken -Fields @{} -Files @{
        comprobante = @{ Path = $reciboPrueba; ContentType = 'image/png' }
    }
    Assert-That ($conRecibo.data.estado -eq 'PENDIENTE_REVISION') 'Adjuntar el recibo no envió la recarga a Finanzas automáticamente.'
    Assert-That ((Invoke-CaichBinary -Path "/monedero/recargas/$recargaFinanzasId/comprobante" -Token $alumnoToken) -eq 200) 'El alumno no pudo abrir su comprobante.'
    Assert-That ((Invoke-CaichBinary -Path "/monedero/recargas/$recargaFinanzasId/comprobante" -Token $adminToken) -eq 200) 'Finanzas no pudo abrir el comprobante para revisarlo.'

    $comprobanteAjenoBloqueado = $false
    try {
        $null = Invoke-CaichBinary -Path "/monedero/recargas/$recargaFinanzasId/comprobante" -Token $alumno2Token
    }
    catch {
        $comprobanteAjenoBloqueado = $true
    }
    Assert-That $comprobanteAjenoBloqueado 'Otro alumno pudo abrir un comprobante ajeno.'

    $rutaPublicaBloqueada = $false
    try {
        $null = Invoke-CaichBinary -Path $conRecibo.data.comprobanteUrl
    }
    catch {
        $rutaPublicaBloqueada = $true
    }
    Assert-That $rutaPublicaBloqueada 'El comprobante de recarga sigue accesible por una URL pública.'

}
finally {
    Remove-Item -LiteralPath $reciboPrueba -Force -ErrorAction SilentlyContinue
}

$pendientesAdmin = Invoke-CaichJson -Method GET -Path '/admin/monedero/recargas/pendientes' -Token $adminToken
Assert-That (@($pendientesAdmin.data.recargaMonederoId) -contains $recargaFinanzasId) 'La recarga con recibo no apareció en la cola de Finanzas.'

$aprobadaFinanzas = Invoke-CaichJson -Method POST -Path "/admin/monedero/recargas/$recargaFinanzasId/aprobar" -Token $adminToken
Assert-That ($aprobadaFinanzas.data.estado -eq 'APROBADO') 'Finanzas no aprobó la recarga con recibo.'
$saldoTrasRecargas = [decimal](Invoke-CaichJson -Method GET -Path '/monedero/mi-saldo' -Token $alumnoToken).data.saldo
Assert-That ($saldoTrasRecargas -eq ($saldoInicial + ($MontoRecarga * 2))) 'El saldo no coincide tras completar ambos flujos.'

# 3) Ticket privado y cancelación sin afectar el saldo.
$recargaCancelable = Invoke-CaichMultipart -Path '/monedero/recargas/tesoreria' -Token $alumnoToken -Fields @{} -JsonParts @{
    datos = @{ monto = 10.00; concepto = 'Prueba E2E - cancelación' }
}
$recargaCancelableId = $recargaCancelable.data.recargaMonederoId
$ticketPropio = Invoke-CaichJson -Method GET -Path "/monedero/recargas/tesoreria/$recargaCancelableId/ticket" -Token $alumnoToken
Assert-That ($ticketPropio.data.recargaMonederoId -eq $recargaCancelableId) 'El alumno no pudo ver su propio ticket.'

$ticketAjenoBloqueado = $false
try {
    $null = Invoke-CaichJson -Method GET -Path "/monedero/recargas/tesoreria/$recargaCancelableId/ticket" -Token $alumno2Token
}
catch {
    $ticketAjenoBloqueado = $true
}
Assert-That $ticketAjenoBloqueado 'Otro alumno pudo consultar un ticket ajeno.'

$cancelada = Invoke-CaichJson -Method POST -Path "/monedero/recargas/tesoreria/$recargaCancelableId/cancelar" -Token $alumnoToken
Assert-That ($cancelada.data.estado -eq 'RECHAZADO' -and $cancelada.data.motivoRechazo -eq 'Cancelada por el alumno') 'La solicitud pendiente no se marcó como cancelada.'
$saldoTrasCancelacion = [decimal](Invoke-CaichJson -Method GET -Path '/monedero/mi-saldo' -Token $alumnoToken).data.saldo
Assert-That ($saldoTrasCancelacion -eq $saldoTrasRecargas) 'Cancelar una solicitud modificó el saldo.'

# 4) Alumno reserva un cubículo con pago automático desde monedero.
$slot = Get-DisponibleParaReserva -AlumnoToken $alumnoToken
$reserva = Invoke-CaichMultipart -Path '/reservas/alumno/con-monedero' -Token $alumnoToken -Fields @{
    espacioId = $slot.EspacioId
    fecha = $slot.Fecha
    horaInicio = ([datetime]$slot.Inicio).ToString('HH:mm')
    horaFin = ([datetime]$slot.Fin).ToString('HH:mm')
    celular = '6440000000'
    actividad = 'Prueba E2E de préstamo de cubículo'
    maestro = 'Prueba automática'
} 
Assert-That ($null -ne $reserva.data.ticketId) 'No se creó el ticket de reserva.'

$ticket = Invoke-CaichJson -Method GET -Path "/tickets/$($reserva.data.ticketId)" -Token $alumnoToken
Assert-That ($ticket.data.estadoFinanciero -eq 'PAGADO') 'El ticket de la reserva no quedó pagado con monedero.'

$saldoFinal = [decimal](Invoke-CaichJson -Method GET -Path '/monedero/mi-saldo' -Token $alumnoToken).data.saldo
$montoReserva = [decimal]$ticket.data.montoTotal
Assert-That ($saldoFinal -eq ($saldoTrasCancelacion - $montoReserva)) 'El saldo final no coincide con el costo de la reserva.'

Write-Host ''
Write-Host 'PRUEBA E2E SUPERADA' -ForegroundColor Green
[PSCustomObject]@{
    RecargaDirectaId = $recargaDirectaId
    RecargaFinanzasId = $recargaFinanzasId
    RecargaCanceladaId = $recargaCancelableId
    FolioRecargaDirecta = $recargaDirecta.data.folio
    FolioRecargaFinanzas = $recargaFinanzas.data.folio
    SaldoInicial = $saldoInicial
    SaldoTrasTesoreria = $saldoTrasTesoreria
    SaldoTrasRecargas = $saldoTrasRecargas
    TicketReserva = $reserva.data.folio
    MontoReserva = $montoReserva
    SaldoFinal = $saldoFinal
    FechaReserva = $slot.Fecha
} | Format-List
