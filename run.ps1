# ============================================================
# Run the Jenkins AI Monitor
# Usage: .\run.ps1
# ============================================================

# Load environment variables from .env file
if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        if ($_ -match "^\s*([^#][^=]+)=(.+)$") {
            $name  = $matches[1].Trim()
            $value = $matches[2].Trim()
            [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
            Write-Host "  Loaded: $name"
        }
    }
    Write-Host ""
} else {
    Write-Host "WARNING: .env file not found. Copy .env.example to .env and fill in your values." -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "Starting Jenkins AI Monitor..." -ForegroundColor Cyan
mvn spring-boot:run
