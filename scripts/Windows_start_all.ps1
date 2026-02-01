# Windows PowerShell script
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$cpFile = Join-Path $root "target\classpath.txt"

Write-Host "Building project..."
Push-Location $root
mvn -q -DskipTests package
mvn -q -DincludeScope=runtime ("-Dmdep.outputFile=$cpFile") dependency:build-classpath
Pop-Location

if (-not (Test-Path $cpFile)) {
    throw "Failed to generate classpath file: $cpFile"
}

$depCp = (Get-Content $cpFile -Raw).Trim()
$sep = ";"
$fullCp = "target/classes" + $sep + $depCp

function Start-ServiceWindow {
    param(
        [string]$Title,
        [string]$MainClass
    )
    $cmd = @(
        "-NoExit",
        "-Command",
        "`$Host.UI.RawUI.WindowTitle='$Title'; Set-Location -LiteralPath '$root'; java -cp '$fullCp' $MainClass"
    )
    Start-Process -FilePath "powershell.exe" -ArgumentList $cmd
    Start-Sleep -Seconds 1
}

Write-Host "Launching services in order (new windows with titles)..."
Start-ServiceWindow -Title "center-server" -MainClass "org.centerServer.Main"
Start-ServiceWindow -Title "edge-server-2" -MainClass "org.edgeServer2.Main"
Start-ServiceWindow -Title "edge-server-4" -MainClass "org.edgeServer4.Main"
Start-ServiceWindow -Title "edge-server-1" -MainClass "org.edgeServer1.Main"
Start-ServiceWindow -Title "edge-server-3" -MainClass "org.edgeServer3.Main"
Start-ServiceWindow -Title "data-client-server" -MainClass "org.dataClient.ServerMain"

Write-Host "All services started in separate windows."
