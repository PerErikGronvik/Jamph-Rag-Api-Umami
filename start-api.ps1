# Quick start script for Jamph-Rag-Api-Umami
# Run from project root directory

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

Write-Host "🚀 Starting Jamph-Rag-Api-Umami" -ForegroundColor Cyan
Write-Host "=" * 60

# Check if in correct directory
if (-not (Test-Path (Join-Path $ProjectRoot "pom.xml"))) {
    Write-Host "❌ Error: pom.xml not found!" -ForegroundColor Red
    Write-Host "Please run this script from the project root directory." -ForegroundColor Yellow
    exit 1
}

# Step 1: Check if Ollama is running
Write-Host "`n[1/3] Checking Ollama..." -ForegroundColor Yellow
try {
    $null = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -TimeoutSec 2 -ErrorAction Stop
    Write-Host "✅ Ollama is running on http://localhost:11434" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Warning: Ollama is not running!" -ForegroundColor Yellow
    Write-Host "   Start Ollama first: ollama serve" -ForegroundColor Gray
    $response = Read-Host "`nContinue anyway? (y/n)"
    if ($response -ne 'y') {
        Write-Host "Exiting..." -ForegroundColor Yellow
        exit 0
    }
}

# Step 2: Check if port 8004 is available
Write-Host "`n[2/3] Checking port 8004..." -ForegroundColor Yellow
$portInUse = Get-NetTCPConnection -LocalPort 8004 -ErrorAction SilentlyContinue
if ($portInUse) {
    Write-Host "❌ Error: Port 8004 is already in use!" -ForegroundColor Red
    Write-Host "   Kill the process or change API_PORT environment variable." -ForegroundColor Yellow
    Write-Host "   Process using port 8004:" -ForegroundColor Gray
    Get-Process -Id $portInUse.OwningProcess | Format-Table Id,ProcessName,StartTime
    exit 1
} else {
    Write-Host "✅ Port 8004 is available" -ForegroundColor Green
}

# Step 3: Build and run
Write-Host "`n[3/3] Building and starting API..." -ForegroundColor Yellow
Write-Host "This may take a moment on first run..." -ForegroundColor Gray

try {
    # Check if already built
    $jarPath = "target\api-1.0-SNAPSHOT-jar-with-dependencies.jar"
    $needsBuild = -not (Test-Path $jarPath)
    
    if ($needsBuild) {
        Write-Host "Building project..." -ForegroundColor Gray
        mvn clean package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            Write-Host "❌ Build failed!" -ForegroundColor Red
            exit 1
        }
    }
    
    Write-Host "`n✨ Starting server on http://localhost:8004" -ForegroundColor Green
    Write-Host "Press Ctrl+C to stop`n" -ForegroundColor Gray
    Write-Host "Ready to receive requests from frontend (localhost:5173)" -ForegroundColor Cyan
    Write-Host ""
    
    # Run the application
    java -jar $jarPath
}
