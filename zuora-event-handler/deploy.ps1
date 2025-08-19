# Zuora Event Handler デプロイスクリプト (PowerShell版)
# 使用方法: .\deploy.ps1 [環境名]
# 例: .\deploy.ps1 dev

param(
    [string]$Environment = "dev"
)

# エラー時に停止
$ErrorActionPreference = "Stop"

Write-Host "===== Zuora Event Handler デプロイ開始 =====" -ForegroundColor Green
Write-Host "環境: $Environment" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Green

# 有効な環境のチェック
$ValidEnvironments = @("dev", "qa", "prod")
if ($Environment -notin $ValidEnvironments) {
    Write-Host "❌ エラー: 無効な環境です。dev, qa, prod のいずれかを指定してください" -ForegroundColor Red
    Write-Host "使用方法: .\deploy.ps1 [環境名]"
    exit 1
}

# 確認プロンプト（本番環境の場合）
if ($Environment -eq "prod") {
    Write-Host "⚠️  警告: 本番環境にデプロイしようとしています" -ForegroundColor Yellow
    $confirmation = Read-Host "続行しますか？ (y/N)"
    if ($confirmation -ne "y" -and $confirmation -ne "Y") {
        Write-Host "デプロイをキャンセルしました" -ForegroundColor Yellow
        exit 0
    }
}

try {
    # ビルド
    Write-Host "🔨 ビルド実行中..." -ForegroundColor Blue
    Set-Location "ZuoraEventHandler"
    ./gradlew build --no-daemon
    Set-Location ".."

    # SAMビルド
    Write-Host "📦 SAMビルド実行中..." -ForegroundColor Blue
    sam build --config-env $Environment

    # デプロイ
    Write-Host "🚀 デプロイ実行中..." -ForegroundColor Blue
    sam deploy --config-env $Environment

    Write-Host "✅ デプロイ完了!" -ForegroundColor Green
    Write-Host "環境: $Environment" -ForegroundColor Cyan
    Write-Host "スタック名: $Environment-zuora-event-handler" -ForegroundColor Cyan
}
catch {
    Write-Host "❌ デプロイ中にエラーが発生しました: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
