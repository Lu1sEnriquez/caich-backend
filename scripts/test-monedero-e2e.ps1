<#
Prueba E2E local del flujo de monedero.

Valida:
  1. Alumno solicita recarga por Tesorería.
  2. Tesorería confirma el pago.
  3. Administración aprueba la recarga y se acredita el saldo.
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

function Invoke-CaichMultipart {
    param(
        [string]$Path,
        [string]$Token,
        [hashtable]$Fields,
        [hashtable]$JsonParts = @{}
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
$tesoreriaToken = Login 'tesoreria@caich.com'
$adminToken = Login 'admin@caich.com'

$saldoInicial = [decimal](Invoke-CaichJson -Method GET -Path '/monedero/mi-saldo' -Token $alumnoToken).data.saldo

# 1) Alumno solicita una recarga por Tesorería.
$recarga = Invoke-CaichMultipart -Path '/monedero/recargas/tesoreria' -Token $alumnoToken -Fields @{} -JsonParts @{
    datos = @{ monto = $MontoRecarga; concepto = 'Prueba E2E de monedero' }
}
Assert-That ($recarga.data.estado -eq 'PENDIENTE_TESORERIA') 'La recarga no quedó pendiente de Tesorería.'
$recargaId = $recarga.data.recargaMonederoId

# 2) Tesorería confirma el pago, sin acreditar saldo todavía.
$confirmada = Invoke-CaichJson -Method POST -Path "/monedero/recargas/tesoreria/$recargaId/confirmar" -Token $tesoreriaToken
Assert-That ($confirmada.data.estado -eq 'PENDIENTE_REVISION') 'Tesorería no envió la recarga a revisión administrativa.'

$pendientesAdmin = Invoke-CaichJson -Method GET -Path '/admin/monedero/recargas/pendientes' -Token $adminToken
Assert-That (@($pendientesAdmin.data.recargaMonederoId) -contains $recargaId) 'La recarga no llegó a la cola administrativa.'

# 3) Administración aprueba y acredita el saldo.
$aprobada = Invoke-CaichJson -Method POST -Path "/admin/monedero/recargas/$recargaId/aprobar" -Token $adminToken
Assert-That ($aprobada.data.estado -eq 'APROBADO') 'La recarga no fue aprobada.'
$saldoTrasRecarga = [decimal](Invoke-CaichJson -Method GET -Path '/monedero/mi-saldo' -Token $alumnoToken).data.saldo
Assert-That ($saldoTrasRecarga -eq ($saldoInicial + $MontoRecarga)) 'El saldo no aumentó con la recarga aprobada.'

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
Assert-That ($saldoFinal -eq ($saldoTrasRecarga - $montoReserva)) 'El saldo final no coincide con el costo de la reserva.'

Write-Host ''
Write-Host 'PRUEBA E2E SUPERADA' -ForegroundColor Green
[PSCustomObject]@{
    RecargaId = $recargaId
    FolioRecarga = $recarga.data.folio
    SaldoInicial = $saldoInicial
    SaldoTrasRecarga = $saldoTrasRecarga
    TicketReserva = $reserva.data.folio
    MontoReserva = $montoReserva
    SaldoFinal = $saldoFinal
    FechaReserva = $slot.Fecha
} | Format-List
