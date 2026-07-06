# Loads .env into the CURRENT PowerShell session so `mvn spring-boot:run`
# services pick up DB/Redis passwords, JWT secret, API keys, etc.
# Usage (run once per terminal tab, before starting a service):
#     . .\load-env.ps1
# The leading "dot space" is required so the variables stay in your session.

$envFile = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path $envFile)) { Write-Host "No .env found at $envFile" -ForegroundColor Red; return }

$count = 0
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }   # skip blanks & comments
    if ($line -notmatch "=") { return }

    $name, $value = $line -split "=", 2
    $name  = $name.Trim()
    $value = $value.Trim()
    $value = ($value -replace '\s+#.*$', '').Trim()           # strip inline "# comment"
    $value = $value.Trim('"').Trim("'")                        # strip surrounding quotes

    [Environment]::SetEnvironmentVariable($name, $value, "Process")
    $count++
}
Write-Host "Loaded $count variables from .env into this session." -ForegroundColor Green
