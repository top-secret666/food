# E2E smoke test for Food Delivery backend
# Usage: .\scripts\test-e2e.ps1
# Optional: $env:REQUIRE_EMAIL_VERIFIED = "false" before starting services for faster auth test

$ErrorActionPreference = "Stop"

function Test-Port($port) {
    (Test-NetConnection -ComputerName localhost -Port $port -WarningAction SilentlyContinue).TcpTestSucceeded
}

function Wait-Health($url, $name, $maxSec = 120) {
    Write-Host "Waiting for $name ($url)..." -ForegroundColor Yellow
    $deadline = (Get-Date).AddSeconds($maxSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
            if ($r.StatusCode -eq 200) {
                Write-Host "  OK: $name" -ForegroundColor Green
                return $true
            }
        } catch { }
        Start-Sleep -Seconds 3
    }
    Write-Host "  FAIL: $name not ready" -ForegroundColor Red
    return $false
}

Write-Host "`n=== Port check ===" -ForegroundColor Cyan
$ports = @{
    "user-db"      = 5432
    "restaurant-db"= 5433
    "order-db"     = 5434
    "keycloak"     = 8080
    "restaurant"   = 8081
    "order"        = 8082
    "user-service" = 8084
    "kafka"        = 9092
    "mailhog"      = 8025
}
foreach ($k in $ports.Keys) {
    $ok = Test-Port $ports[$k]
    $color = if ($ok) { "Green" } else { "Red" }
    Write-Host ("  {0,-14} :{1} -> {2}" -f $k, $ports[$k], $ok) -ForegroundColor $color
}

Write-Host "`n=== Health checks ===" -ForegroundColor Cyan
$allOk = $true
$allOk = (Wait-Health "http://localhost:8084/actuator/health" "user-service") -and $allOk
$allOk = (Wait-Health "http://localhost:8081/actuator/health" "restaurant-service") -and $allOk
$allOk = (Wait-Health "http://localhost:8082/actuator/health" "order-service") -and $allOk
if (-not $allOk) {
    Write-Host "`nServices not ready. Start stack first:" -ForegroundColor Red
    Write-Host "  docker compose --profile all up -d --build" -ForegroundColor Yellow
    exit 1
}

Write-Host "`n=== 1. Restaurants (seed data) ===" -ForegroundColor Cyan
$restaurants = Invoke-RestMethod "http://localhost:8081/api/restaurants"
Write-Host "  Found $($restaurants.Count) restaurants"
$restaurants | ForEach-Object { Write-Host "    [$($_.id)] $($_.name)" }
if ($restaurants.Count -lt 1) { throw "No seed restaurants" }

$dishes = Invoke-RestMethod "http://localhost:8081/api/restaurants/1/dishes"
Write-Host "  Restaurant 1 dishes: $($dishes.Count)"
$dishes | Select-Object -First 3 | ForEach-Object { Write-Host "    dish $($_.id): $($_.name) = $($_.price)" }

Write-Host "`n=== 2. Register + Login ===" -ForegroundColor Cyan
$email = "e2e-$(Get-Date -Format 'yyyyMMddHHmmss')@test.local"
$password = "password123"
$registerBody = @{ email = $email; password = $password; fullName = "E2E User" } | ConvertTo-Json
try {
    $user = Invoke-RestMethod -Method POST -Uri "http://localhost:8084/api/auth/register" `
        -ContentType "application/json" -Body $registerBody
    Write-Host "  Registered: $($user.email) (id=$($user.id))"
} catch {
    Write-Host "  Register failed: $($_.Exception.Message)" -ForegroundColor Red
    throw
}

Write-Host "  Check Mailhog for verification link: http://localhost:8025" -ForegroundColor Yellow
Write-Host "  Or set REQUIRE_EMAIL_VERIFIED=false and restart user-service for dev test" -ForegroundColor Yellow

$loginBody = @{ email = $email; password = $password } | ConvertTo-Json
try {
    $tokenResp = Invoke-RestMethod -Method POST -Uri "http://localhost:8084/api/auth/login" `
        -ContentType "application/json" -Body $loginBody
    $token = $tokenResp.access_token
    Write-Host "  Login OK, token received" -ForegroundColor Green
} catch {
    Write-Host "  Login failed (email not verified?): $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  Open Mailhog, click verify link, then re-run script or login manually." -ForegroundColor Yellow
    exit 2
}

$headers = @{ Authorization = "Bearer $token" }
$me = Invoke-RestMethod -Uri "http://localhost:8084/api/users/me" -Headers $headers
Write-Host "  /users/me -> $($me.email) (id=$($me.id))" -ForegroundColor Green

Write-Host "`n=== 3. Place order ===" -ForegroundColor Cyan
$dishId = $dishes[0].id
$orderBody = @{
    restaurantId = 1
    paymentMethod = "CARD"
    items = @(@{ dishId = $dishId; quantity = 2 })
} | ConvertTo-Json -Depth 5

$order = Invoke-RestMethod -Method POST -Uri "http://localhost:8082/api/orders" `
    -Headers $headers -ContentType "application/json" -Body $orderBody
Write-Host "  Order id=$($order.id) totalPrice=$($order.totalPrice) status=$($order.status)" -ForegroundColor Green
$expected = $dishes[0].price * 2
if ($order.totalPrice -eq $expected) {
    Write-Host "  Price OK (expected $expected)" -ForegroundColor Green
} else {
    Write-Host "  Price mismatch: expected $expected, got $($order.totalPrice)" -ForegroundColor Red
}

Write-Host "`n=== ALL SMOKE TESTS PASSED ===" -ForegroundColor Green
