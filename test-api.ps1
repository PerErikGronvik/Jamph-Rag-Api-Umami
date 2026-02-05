# Test script for Jamph-Rag-Api-Umami
# Tests the complete infrastructure: Frontend → Backend → Ollama

Write-Host "🧪 Testing Jamph-Rag-Api-Umami Infrastructure" -ForegroundColor Cyan
Write-Host "=" * 60

# Test 1: Check Ollama
Write-Host "`n[1/4] Testing Ollama (localhost:11434)..." -ForegroundColor Yellow
try {
    $ollamaResponse = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ Ollama is running" -ForegroundColor Green
    Write-Host "   Available models:" -ForegroundColor Gray
    $ollamaResponse.models | ForEach-Object { Write-Host "   - $($_.name)" -ForegroundColor Gray }
} catch {
    Write-Host "❌ Ollama is NOT running" -ForegroundColor Red
    Write-Host "   Start with: ollama serve" -ForegroundColor Yellow
}

# Test 2: Check Backend API Health
Write-Host "`n[2/4] Testing Backend API (localhost:8004)..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-RestMethod -Uri "http://localhost:8004/health" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ Backend API is running" -ForegroundColor Green
    Write-Host "   Status: $($healthResponse.status)" -ForegroundColor Gray
    Write-Host "   Service: $($healthResponse.service)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Backend API is NOT running" -ForegroundColor Red
    Write-Host "   Start with: mvn exec:java -Dexec.mainClass=`"no.jamph.ragumami.ApplicationKt`"" -ForegroundColor Yellow
}

# Test 3: Test Chat Endpoint
Write-Host "`n[3/4] Testing /api/chat endpoint..." -ForegroundColor Yellow
try {
    $chatBody = @{
        message = "Hello, respond with just 'OK' if you can hear me"
    } | ConvertTo-Json

    $chatResponse = Invoke-RestMethod -Uri "http://localhost:8004/api/chat" `
        -Method POST `
        -ContentType "application/json" `
        -Body $chatBody `
        -TimeoutSec 30 `
        -ErrorAction Stop

    Write-Host "✅ Chat endpoint is working" -ForegroundColor Green
    Write-Host "   Response: $($chatResponse.response.Substring(0, [Math]::Min(100, $chatResponse.response.Length)))..." -ForegroundColor Gray
} catch {
    Write-Host "❌ Chat endpoint failed" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Test SQL Endpoint
Write-Host "`n[4/4] Testing /api/sql endpoint..." -ForegroundColor Yellow
try {
    $sqlBody = @{
        query = "Show all websites"
    } | ConvertTo-Json

    $sqlResponse = Invoke-RestMethod -Uri "http://localhost:8004/api/sql" `
        -Method POST `
        -ContentType "application/json" `
        -Body $sqlBody `
        -TimeoutSec 30 `
        -ErrorAction Stop

    Write-Host "✅ SQL endpoint is working" -ForegroundColor Green
    Write-Host "   Response: $($sqlResponse.sql.Substring(0, [Math]::Min(100, $sqlResponse.sql.Length)))..." -ForegroundColor Gray
} catch {
    Write-Host "❌ SQL endpoint failed" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Summary
Write-Host "`n" + ("=" * 60)
Write-Host "📊 Test Summary" -ForegroundColor Cyan
Write-Host "=" * 60
Write-Host "`nArchitecture Flow:" -ForegroundColor White
Write-Host "Frontend (localhost:5173) → Backend (localhost:8004) → Ollama (localhost:11434)" -ForegroundColor Gray

Write-Host "`nNext Steps:" -ForegroundColor White
Write-Host "1. Make API calls from your Umami frontend (localhost:5173)" -ForegroundColor Gray
Write-Host "2. See README_API.md for JavaScript/React examples" -ForegroundColor Gray
Write-Host "3. Monitor logs for OLLAMA_SUCCESS, OLLAMA_RETRY, OLLAMA_TIMEOUT" -ForegroundColor Gray

Write-Host "`n✨ Done!" -ForegroundColor Green
