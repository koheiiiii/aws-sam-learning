# =====================================
# Test & Run Commands - PowerShell版
# 全実行・テストコマンド統合スクリプト
# =====================================

param(
    [string]$Command = "",
    [string]$TestFile = "",
    [string]$TestClass = "",
    [string]$TestMethod = ""
)

# 文字化け対策
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "🚀 AWS SAM Lambda - テスト・実行スクリプト" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

function Show-Menu {
    Write-Host "📋 使用可能なコマンド:" -ForegroundColor Yellow
    Write-Host "[1] gradlew test        - 全ユニットテスト実行" -ForegroundColor Green
    Write-Host "[2] gradlew test (指定)  - 特定テストファイル/クラス/メソッド実行" -ForegroundColor Green
    Write-Host "[3] gradlew run         - main関数実行 (IDE統合テスト)" -ForegroundColor Blue
    Write-Host "[4] sam local invoke    - Lambda環境テスト" -ForegroundColor Magenta
    Write-Host "[5] sam build           - SAMビルドのみ" -ForegroundColor White
    Write-Host "[6] 統合テスト           - build → test → run → sam invoke" -ForegroundColor Cyan
    Write-Host "[7] クイックテスト       - test → run (ビルドスキップ)" -ForegroundColor Yellow
    Write-Host "[0] 終了" -ForegroundColor Red
    Write-Host ""
}

function Test-GradleAll {
    Write-Host "🧪 全ユニットテスト実行中..." -ForegroundColor Green
    Set-Location "ZuoraEventHandler"
    try {
        .\gradlew test --info
        Write-Host "✅ 全ユニットテスト完了" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ ユニットテスト失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
    finally {
        Set-Location ".."
    }
}

function Test-GradleSpecific {
    param([string]$TestSpec)
    
    Write-Host "🎯 指定テスト実行中: $TestSpec" -ForegroundColor Green
    Set-Location "ZuoraEventHandler"
    
    try {
        # コマンドライン形式の入力を解析（誤入力対策）
        if ($TestSpec -match '-TestClass\s+"([^"]+)"') {
            $TestSpec = $matches[1]
            Write-Host "コマンドライン形式を検出 → クラス名抽出: $TestSpec" -ForegroundColor Yellow
        }
        elseif ($TestSpec -match "-TestClass\s+(\S+)") {
            $TestSpec = $matches[1]
            Write-Host "コマンドライン形式を検出 → クラス名抽出: $TestSpec" -ForegroundColor Yellow
        }
        elseif ($TestSpec -match '-TestMethod\s+"([^"]+)"') {
            $TestSpec = $matches[1]
            Write-Host "コマンドライン形式を検出 → メソッド名抽出: $TestSpec" -ForegroundColor Yellow
        }
        elseif ($TestSpec -match "-TestMethod\s+(\S+)") {
            $TestSpec = $matches[1]
            Write-Host "コマンドライン形式を検出 → メソッド名抽出: $TestSpec" -ForegroundColor Yellow
        }
        elseif ($TestSpec -match '-TestFile\s+"([^"]+)"') {
            $TestSpec = $matches[1]
            Write-Host "コマンドライン形式を検出 → ファイル名抽出: $TestSpec" -ForegroundColor Yellow
        }
        elseif ($TestSpec -match "-TestFile\s+(\S+)") {
            $TestSpec = $matches[1]
            Write-Host "コマンドライン形式を検出 → ファイル名抽出: $TestSpec" -ForegroundColor Yellow
        }

        if ($TestSpec -match ".*\.java$") {
            # ファイル指定の場合（.java拡張子を削除してクラス名に変換）
            $ClassName = $TestSpec -replace ".*[/\\]", "" -replace "\.java$", ""
            Write-Host "ファイル指定 → クラス名変換: $ClassName" -ForegroundColor Yellow
            .\gradlew test --tests "*$ClassName*" --info
        }
        elseif ($TestSpec -match "\w+Test$") {
            # テストクラス指定
            Write-Host "テストクラス指定: $TestSpec" -ForegroundColor Yellow
            .\gradlew test --tests "*$TestSpec*" --info
        }
        elseif ($TestSpec -match "\w+\.\w+") {
            # メソッド指定（ClassName.methodName）
            Write-Host "テストメソッド指定: $TestSpec" -ForegroundColor Yellow
            .\gradlew test --tests "*$TestSpec" --info
        }
        else {
            # 一般的な検索
            Write-Host "パターン検索: $TestSpec" -ForegroundColor Yellow
            .\gradlew test --tests "*$TestSpec*" --info
        }
        Write-Host "✅ 指定テスト完了" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ 指定テスト失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
    finally {
        Set-Location ".."
    }
}

function Run-MainMethod {
    Write-Host "🏃‍♂️ main関数実行中 (IDE統合テスト)..." -ForegroundColor Blue
    Set-Location "ZuoraEventHandler"
    try {
        .\gradlew run
        Write-Host "✅ main関数実行完了" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ main関数実行失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
    finally {
        Set-Location ".."
    }
}

function Invoke-SamLocal {
    Write-Host "🐳 SAM Local Invoke実行中..." -ForegroundColor Magenta
    try {
        Write-Host "SAMビルド中..." -ForegroundColor Yellow
        sam build
        Write-Host "Lambda関数実行中..." -ForegroundColor Yellow
        sam local invoke -e events/event.json
        Write-Host "✅ SAM Local Invoke完了" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ SAM Local Invoke失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Build-SamOnly {
    Write-Host "🏗️ SAMビルドのみ実行中..." -ForegroundColor White
    try {
        sam build
        Write-Host "✅ SAMビルド完了" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ SAMビルド失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Run-IntegratedTest {
    Write-Host "🔄 統合テスト実行中 (build → test → run → sam invoke)..." -ForegroundColor Cyan
    
    Write-Host "1/4 SAMビルド..." -ForegroundColor Yellow
    Build-SamOnly
    
    Write-Host "2/4 ユニットテスト..." -ForegroundColor Yellow
    Test-GradleAll
    
    Write-Host "3/4 main関数実行..." -ForegroundColor Yellow
    Run-MainMethod
    
    Write-Host "4/4 Lambda環境テスト..." -ForegroundColor Yellow
    sam local invoke -e events/event.json
    
    Write-Host "✅ 統合テスト完了" -ForegroundColor Green
}

function Run-QuickTest {
    Write-Host "⚡ クイックテスト実行中 (test → run)..." -ForegroundColor Yellow
    
    Write-Host "1/2 ユニットテスト..." -ForegroundColor Yellow
    Test-GradleAll
    
    Write-Host "2/2 main関数実行..." -ForegroundColor Yellow
    Run-MainMethod
    
    Write-Host "✅ クイックテスト完了" -ForegroundColor Green
}

# コマンドライン引数での実行
if ($Command -ne "") {
    switch ($Command.ToLower()) {
        "test" { 
            if ($TestFile -ne "" -or $TestClass -ne "" -or $TestMethod -ne "") {
                $TestSpec = if ($TestFile -ne "") { $TestFile } elseif ($TestClass -ne "") { $TestClass } else { $TestMethod }
                Test-GradleSpecific -TestSpec $TestSpec
            } else {
                Test-GradleAll 
            }
        }
        "run" { Run-MainMethod }
        "sam" { Invoke-SamLocal }
        "build" { Build-SamOnly }
        "integrated" { Run-IntegratedTest }
        "quick" { Run-QuickTest }
        default { 
            Write-Host "❌ 無効なコマンド: $Command" -ForegroundColor Red
            Show-Menu
        }
    }
    exit
}

# インタラクティブメニュー
while ($true) {
    Show-Menu
    $choice = Read-Host "番号を選択してください (0-7)"
    
    switch ($choice) {
        "1" { Test-GradleAll }
        "2" { 
            Write-Host ""
            Write-Host "📋 テスト指定方法:" -ForegroundColor Yellow
            Write-Host "  • テストクラス名: DynamoDBRepositoryTest" -ForegroundColor Gray
            Write-Host "  • ファイル名: DynamoDBRepositoryTest.java" -ForegroundColor Gray
            Write-Host "  • メソッド指定: DynamoDBRepositoryTest.testPutSubscriptionRecordSuccess" -ForegroundColor Gray
            Write-Host "  • 部分検索: Repository" -ForegroundColor Gray
            Write-Host ""
            $testSpec = Read-Host "テストファイル/クラス/メソッドを指定してください"
            if ($testSpec -ne "") {
                Test-GradleSpecific -TestSpec $testSpec
            } else {
                Write-Host "❌ テスト指定が空です" -ForegroundColor Red
            }
        }
        "3" { Run-MainMethod }
        "4" { Invoke-SamLocal }
        "5" { Build-SamOnly }
        "6" { Run-IntegratedTest }
        "7" { Run-QuickTest }
        "0" { 
            Write-Host "👋 終了します" -ForegroundColor Green
            exit 
        }
        default { 
            Write-Host "❌ 無効な選択です。0-7の番号を入力してください。" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    Write-Host "Press any key to continue..." -ForegroundColor Gray
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    Write-Host ""
}
