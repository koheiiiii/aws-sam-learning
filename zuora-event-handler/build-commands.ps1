# =====================================
# Build Commands - PowerShell版
# AWS SAM Build Commands Menu
# =====================================

function Show-Menu {
    Clear-Host
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "AWS SAM Build Commands" -ForegroundColor Blue
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host ""
    
    Write-Host "使用可能なコマンド:" -ForegroundColor Yellow
    Write-Host "[1] Gradle build (高速開発用)" -ForegroundColor Green
    Write-Host "[2] SAM build (Lambda用パッケージング)" -ForegroundColor Green
    Write-Host "[3] SAM build --use-container (クリーンビルド)" -ForegroundColor Green
    Write-Host "[4] 全部クリーンして再ビルド" -ForegroundColor Cyan
    Write-Host "[5] ビルド成果物の確認" -ForegroundColor Magenta
    Write-Host "[6] Gradle clean build (依存関係リフレッシュ)" -ForegroundColor Cyan
    Write-Host "[7] 完全クリーンビルド (キャッシュ全削除)" -ForegroundColor Red
    Write-Host "[8] 依存関係確認・解決" -ForegroundColor Yellow
    Write-Host "[9] トラブルシューティングビルド" -ForegroundColor Red
    Write-Host "[0] 終了" -ForegroundColor Gray
    Write-Host ""
}

function Invoke-GradleBuild {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "[1] Gradle Build 実行中..." -ForegroundColor Cyan
    Write-Host "=====================================" -ForegroundColor Blue
    
    Set-Location ZuoraEventHandler
    & .\gradlew.bat build
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[SUCCESS] Gradle Build 成功！" -ForegroundColor Green
        Write-Host "[OUTPUT] 成果物: ZuoraEventHandler\build\classes\java\main\" -ForegroundColor Blue
    } else {
        Write-Host ""
        Write-Host "[ERROR] Gradle Build 失敗" -ForegroundColor Red
    }
    Set-Location ..
    Write-Host ""
}

function Invoke-SamBuild {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "[2] SAM Build 実行中..." -ForegroundColor Cyan
    Write-Host "=====================================" -ForegroundColor Blue
    
    sam build
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[SUCCESS] SAM Build 成功！" -ForegroundColor Green
        Write-Host "[OUTPUT] 成果物: .aws-sam\build\ZuoraEventHandler\" -ForegroundColor Blue
    } else {
        Write-Host ""
        Write-Host "[ERROR] SAM Build 失敗" -ForegroundColor Red
    }
    Write-Host ""
}

function Invoke-SamBuildContainer {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "[3] SAM Build (Container) 実行中..." -ForegroundColor Cyan
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "Dockerコンテナ内でクリーンビルド..." -ForegroundColor Yellow
    
    sam build --use-container
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[SUCCESS] SAM Build (Container) 成功！" -ForegroundColor Green
        Write-Host "[OUTPUT] 成果物: .aws-sam\build\ZuoraEventHandler\" -ForegroundColor Blue
    } else {
        Write-Host ""
        Write-Host "[ERROR] SAM Build (Container) 失敗" -ForegroundColor Red
    }
    Write-Host ""
}

function Invoke-CleanRebuild {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "[4] クリーン＆再ビルド 実行中..." -ForegroundColor Cyan
    Write-Host "=====================================" -ForegroundColor Blue
    
    Write-Host "Gradleクリーン..." -ForegroundColor Yellow
    Set-Location ZuoraEventHandler
    & .\gradlew.bat clean
    Write-Host ""
    
    Write-Host "SAMクリーン..." -ForegroundColor Yellow
    Set-Location ..
    if (Test-Path .aws-sam) {
        Remove-Item .aws-sam -Recurse -Force
    }
    Write-Host ""
    
    Write-Host "SAM Build..." -ForegroundColor Yellow
    sam build
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[SUCCESS] クリーン＆再ビルド 成功！" -ForegroundColor Green
        Write-Host "[OUTPUT] 成果物: .aws-sam\build\ZuoraEventHandler\" -ForegroundColor Blue
    } else {
        Write-Host ""
        Write-Host "[ERROR] クリーン＆再ビルド 失敗" -ForegroundColor Red
    }
    Write-Host ""
}

function Show-BuildArtifacts {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "[5] ビルド成果物の確認" -ForegroundColor Cyan
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host ""
    
    Write-Host "Gradle Build成果物:" -ForegroundColor Yellow
    $gradlePath = "ZuoraEventHandler\build\classes\java\main"
    if (Test-Path $gradlePath) {
        Write-Host "[OK] $gradlePath - 存在" -ForegroundColor Green
        $classCount = (Get-ChildItem -Path $gradlePath -Recurse -Filter "*.class" | Measure-Object).Count
        Write-Host "    クラスファイル数: $classCount個" -ForegroundColor Blue
    } else {
        Write-Host "[ERROR] Gradle Build成果物が見つかりません" -ForegroundColor Red
    }
    Write-Host ""
    
    Write-Host "SAM Build成果物:" -ForegroundColor Yellow
    $samPath = ".aws-sam\build\ZuoraEventHandler"
    if (Test-Path $samPath) {
        Write-Host "[OK] $samPath - 存在" -ForegroundColor Green
        Write-Host "    フォルダ/ファイル:" -ForegroundColor Blue
        Get-ChildItem -Path $samPath | ForEach-Object { 
            Write-Host "    $($_.Name)" -ForegroundColor Gray 
        }
    } else {
        Write-Host "[ERROR] SAM Build成果物が見つかりません" -ForegroundColor Red
    }
    Write-Host ""
}

function Invoke-GradleCleanBuild {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "[6] Gradle Clean Build 実行中..." -ForegroundColor Cyan
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "Gradleクリーン + 依存関係リフレッシュ..." -ForegroundColor Yellow
    
    Set-Location ZuoraEventHandler
    & .\gradlew.bat clean build --refresh-dependencies
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[SUCCESS] Gradle Clean Build 成功！" -ForegroundColor Green
        Write-Host "[OUTPUT] 成果物: ZuoraEventHandler\build\classes\java\main\" -ForegroundColor Blue
        Write-Host "[INFO] 依存関係も最新に更新されました" -ForegroundColor Yellow
    } else {
        Write-Host ""
        Write-Host "[ERROR] Gradle Clean Build 失敗" -ForegroundColor Red
    }
    Set-Location ..
    Write-Host ""
}

function Invoke-FullCleanBuild {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "[7] 完全クリーンビルド 実行中..." -ForegroundColor Cyan
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "全キャッシュ・成果物を削除..." -ForegroundColor Red
    
    Set-Location ZuoraEventHandler
    & .\gradlew.bat clean
    
    if (Test-Path "build") { Remove-Item "build" -Recurse -Force }
    if (Test-Path ".gradle") { Remove-Item ".gradle" -Recurse -Force }
    Set-Location ..
    if (Test-Path ".aws-sam") { Remove-Item ".aws-sam" -Recurse -Force }
    
    Write-Host ""
    Write-Host "完全リビルド..." -ForegroundColor Yellow
    Set-Location ZuoraEventHandler
    & .\gradlew.bat build --refresh-dependencies
    if ($LASTEXITCODE -eq 0) {
        Set-Location ..
        sam build
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "[SUCCESS] 完全クリーンビルド 成功！" -ForegroundColor Green
            Write-Host "[OUTPUT] 成果物: .aws-sam\build\ZuoraEventHandler\" -ForegroundColor Blue
            Write-Host "[INFO] 全てのキャッシュをクリアして再構築しました" -ForegroundColor Yellow
        } else {
            Write-Host ""
            Write-Host "[ERROR] SAM Build 失敗" -ForegroundColor Red
        }
    } else {
        Write-Host ""
        Write-Host "[ERROR] Gradle Build 失敗" -ForegroundColor Red
        Set-Location ..
    }
    Write-Host ""
}

function Show-DependencyCheck {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "[8] 依存関係確認・解決 実行中..." -ForegroundColor Cyan
    Write-Host "=====================================" -ForegroundColor Blue
    
    Set-Location ZuoraEventHandler
    Write-Host "依存関係ツリー表示:" -ForegroundColor Yellow
    Write-Host ""
    & .\gradlew.bat dependencies --configuration runtimeClasspath
    
    Write-Host ""
    Write-Host "依存関係の競合チェック:" -ForegroundColor Yellow
    Write-Host ""
    & .\gradlew.bat dependencyInsight --dependency com.amazonaws
    
    Write-Host ""
    Write-Host "依存関係のリフレッシュ:" -ForegroundColor Yellow
    & .\gradlew.bat build --refresh-dependencies --info | Select-String -Pattern "download|resolve|conflict"
    Set-Location ..
    
    Write-Host ""
    Write-Host "[SUCCESS] 依存関係確認完了" -ForegroundColor Green
    Write-Host ""
}

function Invoke-TroubleshootBuild {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "[9] トラブルシューティングビルド" -ForegroundColor Cyan
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "詳細診断とビルド実行..." -ForegroundColor Yellow
    Write-Host ""
    
    Write-Host "[Step 1] 環境確認:" -ForegroundColor Magenta
    & .\check-versions.bat
    Write-Host ""
    
    Write-Host "[Step 2] ワークスペースクリーン:" -ForegroundColor Magenta
    Set-Location ZuoraEventHandler
    if (Test-Path "build") { Remove-Item "build" -Recurse -Force }
    if (Test-Path ".gradle") { Remove-Item ".gradle" -Recurse -Force }
    Set-Location ..
    if (Test-Path ".aws-sam") { Remove-Item ".aws-sam" -Recurse -Force }
    Write-Host "[OK] ワークスペースクリーン完了" -ForegroundColor Green
    Write-Host ""
    
    Write-Host "[Step 3] 権限確認 (Gradleラッパー):" -ForegroundColor Magenta
    Set-Location ZuoraEventHandler
    if (-not (Test-Path "gradlew.bat")) {
        Write-Host "[ERROR] gradlew.bat が見つかりません" -ForegroundColor Red
        Set-Location ..
        return
    }
    Write-Host "[OK] gradlew.bat 存在確認" -ForegroundColor Green
    Write-Host ""
    
    Write-Host "[Step 4] 詳細ログでGradleビルド:" -ForegroundColor Magenta
    & .\gradlew.bat build --info --stacktrace
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Gradle Build 成功" -ForegroundColor Green
        Set-Location ..
        Write-Host ""
        
        Write-Host "[Step 5] 詳細ログでSAMビルド:" -ForegroundColor Magenta
        sam build --debug
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "[SUCCESS] トラブルシューティングビルド 成功！" -ForegroundColor Green
            Write-Host "[OUTPUT] 成果物: .aws-sam\build\ZuoraEventHandler\" -ForegroundColor Blue
        } else {
            Write-Host ""
            Write-Host "[ERROR] SAM Build 失敗 - ログを確認してください" -ForegroundColor Red
        }
    } else {
        Write-Host ""
        Write-Host "[ERROR] Gradle Build 失敗 - ログを確認してください" -ForegroundColor Red
        Set-Location ..
    }
    Write-Host ""
}

# メイン処理
while ($true) {
    Show-Menu
    $choice = Read-Host "番号を選択してください (0-9)"
    
    switch ($choice) {
        "1" { Invoke-GradleBuild }
        "2" { Invoke-SamBuild }
        "3" { Invoke-SamBuildContainer }
        "4" { Invoke-CleanRebuild }
        "5" { Show-BuildArtifacts }
        "6" { Invoke-GradleCleanBuild }
        "7" { Invoke-FullCleanBuild }
        "8" { Show-DependencyCheck }
        "9" { Invoke-TroubleshootBuild }
        "0" { 
            Write-Host ""
            Write-Host "=====================================" -ForegroundColor Blue
            Write-Host "Build Commands 終了" -ForegroundColor Blue
            Write-Host "=====================================" -ForegroundColor Blue
            break 
        }
        default { 
            Write-Host "無効な選択です。0-9の番号を入力してください。" -ForegroundColor Red
            Start-Sleep -Seconds 2
        }
    }
    
    if ($choice -ne "0") {
        Read-Host "Press Enter to continue"
    }
}
