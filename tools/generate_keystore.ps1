# Этап 23: генерация release-keystore (НЕ для коммита в репозиторий).
#
# Создаёт keystore/spell-tracker-release.jks с alias=spell-tracker,
# RSA 2048, срок 10000 дней (~27 лет).
#
# Пароли по умолчанию: storePassword=spell-tracker-store,
# keyPassword=spell-tracker-store (PKCS12 требует одинаковые).
# Эти же значения записаны в keystore.properties.
#
# Использование:
#   .\tools\generate_keystore.ps1
#
# После выполнения:
#   - keystore/spell-tracker-release.jks создаётся (добавлен в .gitignore)
#   - keystore.properties создаётся, если его ещё нет
#
# ⚠️  Если уже установлен APK, подписанный старым ключом (например,
# debug-keystore из стоковой сборки), обновление поверх **не получится**
# из-за разных подписей. Перед установкой нового APK нужно удалить
# старую версию приложения.

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$keystoreDir = Join-Path $projectRoot "keystore"
$keystoreFile = Join-Path $keystoreDir "spell-tracker-release.jks"
$propertiesFile = Join-Path $projectRoot "keystore.properties"

$storePass = "spell-tracker-store"
$keyPass = "spell-tracker-store"
$alias = "spell-tracker"
$dname = "CN=Spell Tracker, OU=Personal, O=vk241, C=RU"

if (-not (Test-Path $keystoreDir)) {
    New-Item -ItemType Directory -Path $keystoreDir | Out-Null
    Write-Host "Создал $keystoreDir"
}

if (Test-Path $keystoreFile) {
    Write-Host "⚠️  $keystoreFile уже существует. Перезаписать? (y/N)"
    $answer = Read-Host
    if ($answer -ne "y" -and $answer -ne "Y") {
        Write-Host "Отменено."
        exit 0
    }
    Remove-Item $keystoreFile -Force
}

Write-Host "Генерирую $keystoreFile ..."
keytool -genkeypair -v `
    -keystore "$keystoreFile" `
    -alias "$alias" `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storepass "$storePass" `
    -keypass "$keyPass" `
    -dname "$dname"

if (-not (Test-Path $propertiesFile)) {
    @"
storeFile=keystore/spell-tracker-release.jks
storePassword=$storePass
keyAlias=$alias
keyPassword=$keyPass
"@ | Set-Content -Path $propertiesFile -Encoding UTF8
    Write-Host "Создал $propertiesFile"
} else {
    Write-Host "$propertiesFile уже есть — не трогаю."
}

Write-Host ""
Write-Host "✅ Готово. Соберите release APK:"
Write-Host "   .\gradlew.bat assembleRelease"