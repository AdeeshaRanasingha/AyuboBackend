# ==============================
# Notification Service Runner
#
# 1) Copy run-notification.local.ps1.example -> run-notification.local.ps1
# 2) Put your real Gmail + Twilio values inside run-notification.local.ps1
# 3) Run: ./run-notification.sh   OR   .\run-notification.ps1
#
# run-notification.local.ps1 is gitignored - never commit secrets.
# ==============================

$localFile = Join-Path $PSScriptRoot "run-notification.local.ps1"
if (Test-Path $localFile) {
    Write-Host "Loading secrets from run-notification.local.ps1 ..." -ForegroundColor DarkGray
    . $localFile
} else {
    Write-Host "WARNING: Missing run-notification.local.ps1 - copy from run-notification.local.ps1.example and fill in values." -ForegroundColor Yellow
}

# Keep notification-service on the fixed port used by the project.
$targetPort = 8086

# Prevent accidental fallback ports by cleaning up stale local notification-service process.
$listener = Get-NetTCPConnection -LocalPort $targetPort -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) {
    $portOwnerPid = $listener.OwningProcess
    $proc = Get-CimInstance Win32_Process -Filter "ProcessId = $portOwnerPid" -ErrorAction SilentlyContinue
    $name = if ($proc) { $proc.Name } else { "<unknown>" }
    $cmdLine = if ($proc) { $proc.CommandLine } else { "" }

    if ($name -eq "java.exe" -and $cmdLine -like "*notification-service*") {
        Write-Host "Port $targetPort is occupied by stale notification-service (PID $portOwnerPid). Stopping it ..." -ForegroundColor Yellow
        Stop-Process -Id $portOwnerPid -Force
        Start-Sleep -Seconds 1
    } else {
        Write-Host "Port $targetPort is already used by PID $portOwnerPid ($name)." -ForegroundColor Red
        Write-Host "Stop that process first, then run this script again." -ForegroundColor Red
        exit 1
    }
}

$env:SERVER_PORT = "$targetPort"

Write-Host "Environment variables set for notification-service." -ForegroundColor Green
Write-Host "Starting notification-service on port $targetPort..." -ForegroundColor Cyan

mvn spring-boot:run
