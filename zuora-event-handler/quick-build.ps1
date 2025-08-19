# =====================================
# Quick Build - PowerShell版
# Gradle build + SAM build を連続実行
# =====================================

Write-Host "Quick Build 実行中..." -ForegroundColor Green
Write-Host ""

# Gradle buildで高速コンパイル確認
Write-Host "[1/2] Gradle Build..." -ForegroundColor Cyan
Set-Location ZuoraEventHandler
$gradleResult = & .\gradlew.bat build --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Gradle Build 失敗" -ForegroundColor Red
    Write-Host "[TIP] 詳細確認は: build-commands.bat を使用してください" -ForegroundColor Yellow
    Set-Location ..
    Read-Host "Press Enter to continue"
    exit 1
}
Write-Host "[OK] Gradle Build 完了" -ForegroundColor Green
Set-Location ..

# SAM buildでLambda用パッケージング
Write-Host "[2/2] SAM Build..." -ForegroundColor Cyan
$samResult = sam build
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] SAM Build 失敗" -ForegroundColor Red
    Write-Host "[TIP] 詳細確認は: build-commands.bat を使用してください" -ForegroundColor Yellow
    Read-Host "Press Enter to continue"
    exit 1
}

Write-Host ""
Write-Host "[SUCCESS] Quick Build 完了！" -ForegroundColor Green
Write-Host "[OUTPUT] Lambda ready: .aws-sam\build\ZuoraEventHandler\" -ForegroundColor Blue
Write-Host ""
Write-Host "次のアクション:" -ForegroundColor Yellow
Write-Host "  sam local invoke ZuoraEventHandler --event events/event.json" -ForegroundColor Gray
Write-Host "  または run-sam-local.bat" -ForegroundColor Gray
Write-Host ""
Read-Host "Press Enter to continue"
