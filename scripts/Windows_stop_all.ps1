# Windows PowerShell script
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$logDir = Join-Path $root "logs"

if (-not (Test-Path $logDir)) {
    Write-Host "No logs directory found: $logDir"
    exit 0
}

Get-ChildItem -Path $logDir -Filter "*.pid" | ForEach-Object {
    $pidFile = $_.FullName
    try {
        $pidValue = Get-Content $pidFile -Raw | ForEach-Object { $_.Trim() }
        if ($pidValue) {
            Write-Host "Stopping PID $pidValue from $pidFile"
            Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
        }
    } catch {
        Write-Host "Failed to stop process from ${pidFile}: $($_.Exception.Message)"
    }
}

Write-Host "Done."
