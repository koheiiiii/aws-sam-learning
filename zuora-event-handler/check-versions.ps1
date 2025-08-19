# =====================================
# AWS SAM Java Version Checker - PowerShell版
# =====================================

function Show-Header {
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "AWS SAM Java Version Checker" -ForegroundColor Blue
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host ""
}

function Check-LocalJavaVersion {
    Write-Host "[1/3] Local Java Version (for sam build):" -ForegroundColor Green
    Write-Host "-------------------------------------" -ForegroundColor Gray
    
    try {
        $javaVersion = java -version 2>&1
        if ($LASTEXITCODE -eq 0) {
            $javaVersion | ForEach-Object { Write-Host $_ -ForegroundColor White }
            Write-Host "[OK] Local Java 確認完了" -ForegroundColor Green
        } else {
            Write-Host "[ERROR] Java が見つかりません" -ForegroundColor Red
            Write-Host "[TIP] Java 21をインストールしてPATHを設定してください" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "[ERROR] Java の確認に失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

function Check-DockerLambdaJavaVersion {
    Write-Host "[2/3] Docker Lambda Java Version (for sam local invoke):" -ForegroundColor Green
    Write-Host "----------------------------------------------------------" -ForegroundColor Gray
    Write-Host "Reading runtime from template.yaml..." -ForegroundColor Yellow
    
    try {
        if (Test-Path "template.yaml") {
            $templateContent = Get-Content "template.yaml"
            $runtimeLine = $templateContent | Select-String -Pattern "Runtime:"
            
            if ($runtimeLine) {
                $runtime = ($runtimeLine -split ":")[1].Trim()
                Write-Host "Found Runtime: $runtime" -ForegroundColor Cyan
                
                # Extract version number from runtime (java21 -> 21)
                $version = $runtime -replace "java", ""
                $dockerImage = "public.ecr.aws/lambda/java:$version"
                Write-Host "Checking Docker image: $dockerImage" -ForegroundColor Yellow
                
                # Docker command execution
                $dockerResult = docker run --rm --entrypoint="sh" $dockerImage -c "java -version" 2>&1
                if ($LASTEXITCODE -eq 0) {
                    $dockerResult | ForEach-Object { Write-Host $_ -ForegroundColor White }
                    Write-Host "[OK] Docker Lambda Java 確認完了" -ForegroundColor Green
                } else {
                    Write-Host "[ERROR] Docker Lambda Java の確認に失敗" -ForegroundColor Red
                    Write-Host "[TIP] Dockerが起動していることを確認してください" -ForegroundColor Yellow
                }
            } else {
                Write-Host "[ERROR] template.yaml で Runtime が見つかりません" -ForegroundColor Red
            }
        } else {
            Write-Host "[ERROR] template.yaml が見つかりません" -ForegroundColor Red
        }
    } catch {
        Write-Host "[ERROR] Docker確認に失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

function Check-SamCliVersion {
    Write-Host "[3/3] SAM CLI Version:" -ForegroundColor Green
    Write-Host "----------------------" -ForegroundColor Gray
    
    try {
        $samVersion = sam --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host $samVersion -ForegroundColor White
            Write-Host "[OK] SAM CLI 確認完了" -ForegroundColor Green
        } else {
            Write-Host "[ERROR] SAM CLI が見つかりません" -ForegroundColor Red
            Write-Host "[TIP] AWS SAM CLIをインストールしてPATHを設定してください" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "[ERROR] SAM CLI確認に失敗: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

function Show-Summary {
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host "Version Check Complete" -ForegroundColor Blue
    Write-Host "=====================================" -ForegroundColor Blue
    Write-Host ""
    
    # 環境チェックサマリー
    Write-Host "環境チェックサマリー:" -ForegroundColor Yellow
    
    # Java check
    $javaOk = $false
    try {
        java -version 2>$null
        if ($LASTEXITCODE -eq 0) { $javaOk = $true }
    } catch {}
    
    if ($javaOk) {
        Write-Host "[✓] Local Java: OK" -ForegroundColor Green
    } else {
        Write-Host "[✗] Local Java: NG" -ForegroundColor Red
    }
    
    # Docker check
    $dockerOk = $false
    try {
        docker version 2>$null
        if ($LASTEXITCODE -eq 0) { $dockerOk = $true }
    } catch {}
    
    if ($dockerOk) {
        Write-Host "[✓] Docker: OK" -ForegroundColor Green
    } else {
        Write-Host "[✗] Docker: NG" -ForegroundColor Red
    }
    
    # SAM CLI check
    $samOk = $false
    try {
        sam --version 2>$null
        if ($LASTEXITCODE -eq 0) { $samOk = $true }
    } catch {}
    
    if ($samOk) {
        Write-Host "[✓] SAM CLI: OK" -ForegroundColor Green
    } else {
        Write-Host "[✗] SAM CLI: NG" -ForegroundColor Red
    }
    
    Write-Host ""
    
    # 推奨アクション
    if ($javaOk -and $dockerOk -and $samOk) {
        Write-Host "[SUCCESS] 全ての環境が正常です！" -ForegroundColor Green
        Write-Host "[NEXT] quick-build.ps1 でビルドを開始できます" -ForegroundColor Cyan
    } else {
        Write-Host "[WARNING] 一部の環境に問題があります" -ForegroundColor Yellow
        Write-Host "[TIP] 上記の NG 項目を修正してください" -ForegroundColor Yellow
    }
}

# メイン実行
Show-Header
Check-LocalJavaVersion
Check-DockerLambdaJavaVersion  
Check-SamCliVersion
Show-Summary

Read-Host "Press Enter to continue"
