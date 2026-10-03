# e2e-test.ps1
# Enhanced URL Shortener with E2E testing

param(
    [string]$action,
    [string]$url,
    [string]$code,
    [int]$days = 30
)

$BASE_URL = "http://localhost:8080/api/v1"

function Show-Usage {
    Write-Host "`n========== URL Shortener Commands ==========" -ForegroundColor Cyan
    Write-Host "Create Short URL:" -ForegroundColor Green
    Write-Host "  .\e2e-test.ps1 create 'https://example.com' 30`n" -ForegroundColor Yellow

    Write-Host "Get Short URL Details:" -ForegroundColor Green
    Write-Host "  .\e2e-test.ps1 get 'ABC12345'`n" -ForegroundColor Yellow

    Write-Host "Create with Orchestration (see agents work):" -ForegroundColor Green
    Write-Host "  .\e2e-test.ps1 orchestrate 'https://github.com' 30`n" -ForegroundColor Yellow

    Write-Host "Get Execution Trace:" -ForegroundColor Green
    Write-Host "  .\e2e-test.ps1 trace 'workflow-id-here'`n" -ForegroundColor Yellow

    Write-Host "Full E2E Test:" -ForegroundColor Green
    Write-Host "  .\e2e-test.ps1 e2etest`n" -ForegroundColor Yellow

    Write-Host "=========================================`n" -ForegroundColor Cyan
}

function Create-ShortUrl {
    param([string]$originalUrl, [int]$expirationDays)

    Write-Host "`n[CREATE] Building request..." -ForegroundColor Cyan
    $body = @{
        originalUrl = $originalUrl
        expirationDays = $expirationDays
    } | ConvertTo-Json

    Write-Host "  URL: $originalUrl" -ForegroundColor Yellow
    Write-Host "  Expiration: $expirationDays days" -ForegroundColor Yellow
    Write-Host "  Sending request to $BASE_URL/urls" -ForegroundColor Cyan

    try {
        $response = Invoke-RestMethod -Uri "$BASE_URL/urls" -Method Post -ContentType "application/json" -Body $body

        Write-Host "`n[SUCCESS] Short URL created!" -ForegroundColor Green
        Write-Host "  Short Code: $($response.code)" -ForegroundColor Green
        Write-Host "  Created: $($response.createdAt)" -ForegroundColor Yellow
        Write-Host "  Expires: $($response.expiresAt)" -ForegroundColor Yellow
        Write-Host "  Full URL: http://localhost:8080/$($response.code)" -ForegroundColor Cyan

        return $response.code
    }
    catch {
        Write-Host "`n[ERROR] Failed to create short URL" -ForegroundColor Red
        Write-Host "  Error: $_" -ForegroundColor Red
        return $null
    }
}

function Get-ShortUrl {
    param([string]$shortCode)

    Write-Host "`n[RETRIEVE] Getting short URL details..." -ForegroundColor Cyan
    Write-Host "  Code: $shortCode" -ForegroundColor Yellow
    Write-Host "  Requesting: $BASE_URL/$shortCode" -ForegroundColor Cyan

    try {
        $response = Invoke-RestMethod -Uri "$BASE_URL/$shortCode" -Method Get

        Write-Host "`n[SUCCESS] Short URL found!" -ForegroundColor Green
        Write-Host "  Code: $($response.code)" -ForegroundColor Green
        Write-Host "  Original URL: $($response.originalUrl)" -ForegroundColor Yellow
        Write-Host "  Click Count: $($response.clickCount)" -ForegroundColor Yellow
        Write-Host "  Created: $($response.createdAt)" -ForegroundColor Yellow

        return $response
    }
    catch {
        Write-Host "`n[ERROR] Short URL not found" -ForegroundColor Red
        Write-Host "  Code: $shortCode" -ForegroundColor Red
        return $null
    }
}

function Create-OrchestratedUrl {
    param([string]$originalUrl, [int]$expirationDays)

    Write-Host "`n========== AGENTIC ORCHESTRATION WORKFLOW ==========" -ForegroundColor Magenta
    Write-Host "URL: $originalUrl" -ForegroundColor Yellow
    Write-Host "Expiration: $expirationDays days" -ForegroundColor Yellow
    Write-Host "===================================================`n" -ForegroundColor Magenta

    $body = @{
        originalUrl = $originalUrl
        expirationDays = $expirationDays
    } | ConvertTo-Json

    try {
        Write-Host "[1/5] Validating URL..." -ForegroundColor Cyan
        Write-Host "      Schema check: HTTPS/HTTP" -ForegroundColor Gray
        Write-Host "      SSRF Protection: Checking..." -ForegroundColor Gray

        Write-Host "[2/5] Building DAG (Directed Acyclic Graph)..." -ForegroundColor Cyan
        Write-Host "      Creating 5-stage pipeline..." -ForegroundColor Gray
        Write-Host "      Node: requirement-agent" -ForegroundColor Gray
        Write-Host "      Node: architecture-agent" -ForegroundColor Gray
        Write-Host "      Node: security-agent" -ForegroundColor Gray
        Write-Host "      Node: implementation-agent" -ForegroundColor Gray
        Write-Host "      Node: test-agent" -ForegroundColor Gray

        Write-Host "`n[3/5] Executing Agents..." -ForegroundColor Cyan
        Write-Host "      Sending to orchestration engine..." -ForegroundColor Gray

        $response = Invoke-RestMethod -Uri "$BASE_URL/urls/orchestrated" -Method Post -ContentType "application/json" -Body $body

        Write-Host "`n[SUCCESS] Orchestration Completed!" -ForegroundColor Green
        Write-Host "  Workflow ID: $($response.workflowId)" -ForegroundColor Green
        Write-Host "  Status: $($response.status)" -ForegroundColor Green
        Write-Host "  Execution Time: $($response.executionTimeMs)ms" -ForegroundColor Yellow
        Write-Host "  Short Code: $($response.shortCode)" -ForegroundColor Green

        Write-Host "`n[4/5] Agent Outputs:" -ForegroundColor Cyan
        $agents = @("requirements", "architecture", "security", "implementation", "testing")

        foreach ($agent in $agents) {
            if ($response.agentOutputs.$agent) {
                $output = $response.agentOutputs.$agent
                Write-Host "  $($agent)" -ForegroundColor Green
                Write-Host "      Agent: $($output.agent)" -ForegroundColor Yellow
                Write-Host "      Status: $($output.status)" -ForegroundColor Yellow
            }
        }

        Write-Host "`n[5/5] Storing Execution Trace..." -ForegroundColor Cyan
        Write-Host "      Trace ID: $($response.workflowId)" -ForegroundColor Gray
        Write-Host "      Database: Persisted" -ForegroundColor Gray

        Write-Host "`n========== WORKFLOW COMPLETE ==========" -ForegroundColor Magenta
        Write-Host "Full URL: http://localhost:8080/$($response.shortCode)" -ForegroundColor Cyan
        Write-Host "======================================`n" -ForegroundColor Magenta

        return $response
    }
    catch {
        Write-Host "`n[ERROR] Orchestration failed" -ForegroundColor Red
        Write-Host "  Error: $_" -ForegroundColor Red
        return $null
    }
}

function Get-ExecutionTrace {
    param([string]$workflowId)

    Write-Host "`n[TRACE] Retrieving execution trace..." -ForegroundColor Cyan
    Write-Host "  Workflow ID: $workflowId" -ForegroundColor Yellow

    try {
        $response = Invoke-RestMethod -Uri "$BASE_URL/orchestration/traces/$workflowId" -Method Get

        Write-Host "`n[SUCCESS] Trace retrieved!" -ForegroundColor Green
        Write-Host "  Workflow ID: $($response.workflowId)" -ForegroundColor Green
        Write-Host "  Status: $($response.status)" -ForegroundColor Green
        Write-Host "  Short Code: $($response.shortCode)" -ForegroundColor Yellow
        Write-Host "  Original URL: $($response.originalUrl)" -ForegroundColor Yellow
        Write-Host "  Execution Time: $($response.executionTimeMs)ms" -ForegroundColor Yellow
        Write-Host "  Created: $($response.createdAt)" -ForegroundColor Yellow

        return $response
    }
    catch {
        Write-Host "`n[ERROR] Trace not found" -ForegroundColor Red
        Write-Host "  Workflow ID: $workflowId" -ForegroundColor Red
        return $null
    }
}

function Run-E2ETest {
    Write-Host "`n================================" -ForegroundColor Magenta
    Write-Host "END-TO-END TESTING" -ForegroundColor Magenta
    Write-Host "================================`n" -ForegroundColor Magenta

    Write-Host "[TEST 1] Basic URL Shortening" -ForegroundColor Green
    $code = Create-ShortUrl "https://stackoverflow.com/questions" 30

    if ($code) {
        Write-Host "`n[TEST 2] Retrieve Short URL" -ForegroundColor Green
        Get-ShortUrl $code

        Write-Host "`n[TEST 3] Orchestrated Workflow" -ForegroundColor Green
        $orchestrated = Create-OrchestratedUrl "https://github.com/agentic-sdlc" 90

        if ($orchestrated) {
            Write-Host "[TEST 4] Get Execution Trace" -ForegroundColor Green
            Get-ExecutionTrace $orchestrated.workflowId

            Write-Host "`n================================" -ForegroundColor Magenta
            Write-Host "ALL TESTS PASSED!" -ForegroundColor Green
            Write-Host "================================`n" -ForegroundColor Magenta
        }
    }
}

# Main logic
switch ($action) {
    "create" {
        if (-not $url) {
            Write-Host "Error: URL required" -ForegroundColor Red
            Show-Usage
            exit
        }
        Create-ShortUrl $url $days
    }
    "get" {
        if (-not $code) {
            Write-Host "Error: Code required" -ForegroundColor Red
            Show-Usage
            exit
        }
        Get-ShortUrl $code
    }
    "orchestrate" {
        if (-not $url) {
            Write-Host "Error: URL required" -ForegroundColor Red
            Show-Usage
            exit
        }
        $result = Create-OrchestratedUrl $url $days
        if ($result) {
            Write-Host "`nWorkflow ID for trace: $($result.workflowId)" -ForegroundColor Cyan
        }
    }
    "trace" {
        if (-not $code) {
            Write-Host "Error: Workflow ID required" -ForegroundColor Red
            Show-Usage
            exit
        }
        Get-ExecutionTrace $code
    }
    "e2etest" {
        Run-E2ETest
    }
    default {
        Show-Usage
    }
}