# test-phase6.ps1 - Simple Version

Write-Host "Testing Phase 6 Endpoints..." -ForegroundColor Cyan

# Test 1: Health Check
Write-Host "`n[1] Health Check" -ForegroundColor Green
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/health" -Method Get | ConvertTo-Json

# Test 2: Info Endpoint
Write-Host "`n[2] Info Endpoint" -ForegroundColor Green
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/info" -Method Get | ConvertTo-Json

# Test 3: Create Short URL
Write-Host "`n[3] Create Short URL" -ForegroundColor Green
$body = @{
    originalUrl = "https://example.com"
    expirationDays = 30
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/urls" -Method Post -ContentType "application/json" -Body $body | ConvertTo-Json

# Test 4: Create Orchestrated URL
Write-Host "`n[4] Create Orchestrated URL" -ForegroundColor Green
$body = @{
    originalUrl = "https://github.com"
    expirationDays = 30
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/urls/orchestrated" -Method Post -ContentType "application/json" -Body $body | ConvertTo-Json

Write-Host "`nDone!" -ForegroundColor Cyan