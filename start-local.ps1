# Start all microservices in local (no Docker) mode
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$mvn = Join-Path $root ".tools\apache-maven-3.9.6\bin\mvn.cmd"
if (-not (Test-Path $mvn)) {
  Write-Host "Maven not found at $mvn. Download Apache Maven 3.9.6 into food\.tools first." -ForegroundColor Red
  exit 1
}

$tmp = Join-Path $root ".tmp"
$logs = Join-Path $root "logs"
New-Item -ItemType Directory -Force -Path $tmp, $logs | Out-Null
$env:TEMP = $tmp
$env:TMP = $tmp
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000,http://localhost:3001,https://reactfistapp.vercel.app"
# Must match REACT_APP_GOOGLE_CLIENT_ID (Google Cloud → OAuth Web client)
if (-not $env:GOOGLE_CLIENT_ID) {
  $env:GOOGLE_CLIENT_ID = "934997964943-dli89dipdsr9fbgbf6804mdghoc9rd3i.apps.googleusercontent.com"
}
$jvm = "-Djava.io.tmpdir=$tmp"

function Start-LocalService($name) {
  Write-Host "Starting $name ..." -ForegroundColor Cyan
  $out = Join-Path $logs "$name.txt"
  $err = Join-Path $logs "$name.err.txt"
  Start-Process -FilePath $mvn `
    -ArgumentList @("-f",(Join-Path $root "$name\pom.xml"),"spring-boot:run","-Dspring-boot.run.profiles=local","-Dspring-boot.run.jvmArguments=$jvm") `
    -RedirectStandardOutput $out -RedirectStandardError $err -WindowStyle Hidden
}

Start-LocalService "user-service"
Start-Sleep -Seconds 6
Start-LocalService "restaurant-service"
Start-LocalService "order-service"

Write-Host ""
Write-Host "Local stack launching (H2 + JWT, no Docker / no Keycloak / no Kafka)." -ForegroundColor Green
Write-Host "  user-service        http://localhost:8084"
Write-Host "  restaurant-service  http://localhost:8081"
Write-Host "  order-service       http://localhost:8082"
Write-Host ""
Write-Host "Demo accounts (password for all: aroma123)"
Write-Host "  user@aroma.app      regular customer (USER)"
Write-Host "  manager@aroma.app   kitchen / delivery (MANAGER)"
Write-Host "  admin@aroma.app     accounts + catalog (ADMIN)"
Write-Host "Google Client ID: $env:GOOGLE_CLIENT_ID"
Write-Host ""
Write-Host "Frontend: cd ..\react_fistapp && npm start  -> http://localhost:3000"
