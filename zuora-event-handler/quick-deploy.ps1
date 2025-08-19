# クイック開発デプロイスクリプト
# 個人開発用の高速デプロイ

Write-Host "🚀 クイック開発デプロイ開始" -ForegroundColor Green
Write-Host "環境: dev (個人開発)" -ForegroundColor Cyan

$ErrorActionPreference = "Stop"

try {
    # ビルド（キャッシュありで高速化）
    Write-Host "🔨 ビルド中..." -ForegroundColor Blue
    Set-Location "ZuoraEventHandler"
    ./gradlew build --no-daemon --build-cache
    Set-Location ".."
    
    # SAMビルドとデプロイを連続実行
    Write-Host "📦 SAMビルド & デプロイ中..." -ForegroundColor Blue
    sam build --cached --parallel
    sam deploy --no-confirm-changeset
    
    Write-Host "✅ デプロイ完了!" -ForegroundColor Green
    Write-Host "スタック名: dev-zuora-event-handler" -ForegroundColor Cyan
    
    # ログを表示するオプション
    $showLogs = Read-Host "Lambda のログを表示しますか？ (y/N)"
    if ($showLogs -eq "y" -or $showLogs -eq "Y") {
        Write-Host "📋 ログを表示中..." -ForegroundColor Blue
        sam logs --stack-name dev-zuora-event-handler --tail
    }
}
catch {
    Write-Host "❌ エラー: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
