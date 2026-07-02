# Этап 23: перегенерировать base64-строки для GitHub Secrets.
#
# Зачем: после смены keystore или паролей нужно обновить секреты
# в GitHub (Settings → Secrets and variables → Actions).
#
# Запускать из корня проекта (PowerShell):
#   .\tools\encode_secrets.ps1
#
# Результат: перезаписывает два файла в secrets/:
#   - secrets/KEYSTORE_BASE64.txt            (для Secret KEYSTORE_BASE64)
#   - secrets/KEYSTORE_PROPERTIES_BASE64.txt (для Secret KEYSTORE_PROPERTIES_BASE64)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$keystoreFile = Join-Path $projectRoot "keystore\spell-tracker-release.jks"
$propertiesFile = Join-Path $projectRoot "keystore.properties"
$outDir = Join-Path $projectRoot "secrets"

if (-not (Test-Path $keystoreFile)) {
    Write-Error "❌ Не найден $keystoreFile — сначала запустите .\tools\generate_keystore.ps1"
}
if (-not (Test-Path $propertiesFile)) {
    Write-Error "❌ Не найден $propertiesFile — сначала запустите .\tools\generate_keystore.ps1"
}

if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir | Out-Null
}

$keystoreB64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystoreFile))
$propsB64    = [Convert]::ToBase64String([IO.File]::ReadAllBytes($propertiesFile))

[IO.File]::WriteAllText((Join-Path $outDir "KEYSTORE_BASE64.txt"), $keystoreB64, [System.Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $outDir "KEYSTORE_PROPERTIES_BASE64.txt"), $propsB64, [System.Text.UTF8Encoding]::new($false))

Write-Host "✅ Обновлено:"
Write-Host "   $outDir\KEYSTORE_BASE64.txt"
Write-Host "   $outDir\KEYSTORE_PROPERTIES_BASE64.txt"
Write-Host ""
Write-Host "📋 Скопируйте содержимое в GitHub Secrets:"
Write-Host "   Settings → Secrets and variables → Actions → New repository secret"
Write-Host "   KEYSTORE_BASE64            ← содержимое KEYSTORE_BASE64.txt"
Write-Host "   KEYSTORE_PROPERTIES_BASE64 ← содержимое KEYSTORE_PROPERTIES_BASE64.txt"