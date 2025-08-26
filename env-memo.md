# 🌍 環境変数・PATH設定完全ガイド

## 目次
- [環境変数基本概念](#環境変数基本概念)
- [PATH環境変数の仕組み](#path環境変数の仕組み)
- [よく使う環境変数](#よく使う環境変数)
- [環境変数の確認方法](#環境変数の確認方法)
- [環境変数の設定方法](#環境変数の設定方法)
- [トラブルシューティング](#トラブルシューティング)
- [実用的なスクリプト例](#実用的なスクリプト例)

---

## 🤔 環境変数基本概念

### 環境変数って何？
**簡単に言うと：「PCがプログラムの場所や設定を覚えておくためのメモ」**

### 😩 よくある問題
```bash
git --version
# 'git' は、内部コマンドまたは外部コマンド、
# 操作可能なプログラムまたはバッチ ファイルとして認識されていません。
```

### ✨ 環境変数が解決
```bash
git --version
# git version 2.40.0  ← ちゃんと動く！
```

### 🎯 環境変数とは
- OSが管理する**キー・バリューペア**（名前と値のセット）
- プログラムが「どこに何があるか」を知るための設定
- どのフォルダからでもアクセス可能

---

## 💡 よく使う環境変数

| 変数名 | 何をするもの？ | 例 | なぜ必要？ |
|-------|-------------|-----|-----------|
| `PATH` | **コマンドの場所**を教える | `C:\Program Files\Git\bin` | `git`コマンドを見つけるため |
| `JAVA_HOME` | **Javaの場所**を教える | `C:\Program Files\Java\jdk-17` | Javaアプリが動くため |
| `NODE_PATH` | **Node.jsライブラリ**の場所 | `C:\Users\user\node_modules` | Node.jsが必要なファイルを見つけるため |
| `PYTHONPATH` | **Pythonライブラリ**の場所 | `/usr/lib/python3.9` | Pythonが必要なファイルを見つけるため |
| `HOME` | **ユーザーのホームディレクトリ** | `C:\Users\username` | 設定ファイルの保存場所 |
| `TEMP` | **一時ファイル**の保存場所 | `C:\Users\username\AppData\Local\Temp` | 一時ファイルの作成 |

---

## 🔍 PATH環境変数の仕組み

### PATHって何？
**「コマンドを探しに行く場所のリスト」**

### 🚀 動作の流れ
```
1. あなた：git status と入力
   ↓
2. Windows：「git って何？どこにある？」
   ↓
3. Windows：「PATH を見てみよう...」
   ↓
4. Windows：PATH の1番目のフォルダを確認 → 見つからない
   ↓
5. Windows：PATH の2番目のフォルダを確認 → 発見！
   ↓
6. Windows：実行！
```

### 📝 PATH設定例
```
PATH=C:\Windows\System32;C:\Program Files\Git\bin;C:\Python39\Scripts
     ①システムコマンド    ②Gitコマンド           ③Pythonコマンド
```

### 💡 探索の流れ
```
1. git status を実行
   ↓
2. ① C:\Windows\System32\git.exe を探す → ❌ 見つからない
   ↓
3. ② C:\Program Files\Git\bin\git.exe を探す → ✅ 見つかった！
   ↓
4. 実行完了！
```

### 🚨 もしPATHに入ってなかったら？
```bash
git status
# エラー: 'git' は認識されていません
```

---

## 🔍 環境変数の確認方法

### PowerShell でサクッと確認

```powershell
# PATH の中身を確認（1行で全部）
$env:PATH

# PATH を見やすく1行ずつ表示
$env:PATH -split ';'

# 他の環境変数も確認
$env:JAVA_HOME
$env:PYTHONPATH
$env:HOME

# 全環境変数をざっと見たい時
Get-ChildItem Env:
```

### 📋 実際の出力例（PATHの例）
```
C:\Windows\System32
C:\Program Files\Git\bin
C:\Program Files\Java\jdk-17\bin
C:\Users\ユーザー名\AppData\Local\Programs\Python\Python39\Scripts
```

### 💡 この例からわかること
- `git` コマンド → `C:\Program Files\Git\bin` から実行される
- `java` コマンド → `C:\Program Files\Java\jdk-17\bin` から実行される
- `python` コマンド → Python39の Scripts フォルダから実行される

### 🚨 よくある問題の確認

```powershell
# 特定のコマンドがPATHにあるか確認
where.exe git
# → C:\Program Files\Git\bin\git.exe  ← 見つかった！

where.exe python
# → 何も表示されない ← PATHに入ってない！

# 現在のフォルダからコマンド実行を試す
git --version
# → git version 2.40.0 ← 動く！

python --version
# → 'python' は、内部コマンド... ← 動かない！
```

### Bash（Git Bash）でも確認

```bash
# 全環境変数表示
printenv

# 特定の変数確認
echo $PATH
echo $JAVA_HOME
echo $PYTHONPATH

# PATH内容を見やすく表示
echo $PATH | tr ':' '\n'   # Linux/Mac
echo $PATH | tr ';' '\n'   # Windows
```

---

## 🔧 環境変数の設定方法

### PowerShell での設定

#### 一時的な設定（現在のセッションのみ）
```powershell
# 環境変数の設定
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

# PATH への追加
$env:PATH += ";C:\Program Files\Java\jdk-17\bin"

# 確認
$env:JAVA_HOME
$env:PATH
```

#### 永続的な設定（再起動後も有効）
```powershell
# ユーザー環境変数設定
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-17", "User")

# システム環境変数設定（管理者権限必要）
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-17", "Machine")

# PATH に追加
$userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
$newPath = "$userPath;C:\Program Files\Java\jdk-17\bin"
[Environment]::SetEnvironmentVariable("PATH", $newPath, "User")
```

### Windows GUI での設定

1. `Win + R` → `sysdm.cpl` と入力
2. 「詳細設定」タブ → 「環境変数」ボタン
3. **ユーザー環境変数** または **システム環境変数** で編集

#### PATHへの追加手順
1. 環境変数ダイアログで `PATH` を選択
2. 「編集」ボタンをクリック
3. 「新規」ボタンで追加したいパスを入力
4. 「OK」で保存

### CMD（コマンドプロンプト）での設定

```cmd
# 一時的な設定
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%PATH%;C:\Program Files\Java\jdk-17\bin

# 永続的な設定
setx JAVA_HOME "C:\Program Files\Java\jdk-17"
setx PATH "%PATH%;C:\Program Files\Java\jdk-17\bin"
```

---

## 🚨 トラブルシューティング

### よくある問題と解決方法

#### 問題1: コマンドが認識されない
```bash
# 症状
python --version
# 'python' は認識されていません

# 確認方法
where.exe python
# 何も表示されない

# 解決方法
1. Python がインストールされているか確認
2. Python のインストール場所を確認
3. PATH に追加
```

#### 問題2: 古いバージョンが実行される
```bash
# 症状  
java -version
# java version "1.8" （古いバージョン）

# 原因
# PATH の順序で、古い Java が先に見つかる

# 解決方法
# PATH の順序を変更して、新しいバージョンを先に配置
```

#### 問題3: 環境変数が反映されない
```bash
# 原因
# 1. ターミナルやエディタを再起動していない
# 2. User と System の設定が競合している

# 解決方法
# 1. ターミナル・VS Code を再起動
# 2. PC を再起動
# 3. 環境変数の優先順位を確認
```

### 🔍 診断用クイックチェック

```powershell
# 環境変数診断スクリプト
Write-Host "🔍 環境変数診断開始" -ForegroundColor Yellow

# 主要なコマンドの確認
$commands = @('git', 'java', 'python', 'node', 'npm')
foreach ($cmd in $commands) {
    $path = Get-Command $cmd -ErrorAction SilentlyContinue
    if ($path) {
        Write-Host "✅ $cmd : $($path.Source)" -ForegroundColor Green
    } else {
        Write-Host "❌ $cmd : 見つかりません" -ForegroundColor Red
    }
}

# 主要な環境変数の確認
Write-Host "`n📊 主要環境変数"
$envVars = @('JAVA_HOME', 'PYTHON_HOME', 'NODE_PATH')
foreach ($var in $envVars) {
    $value = [Environment]::GetEnvironmentVariable($var)
    if ($value) {
        Write-Host "✅ $var : $value" -ForegroundColor Green
    } else {
        Write-Host "❌ $var : 設定されていません" -ForegroundColor Yellow
    }
}

# PATH の項目数確認
$pathItems = $env:PATH -split ';'
Write-Host "`n📈 PATH項目数: $($pathItems.Count)"
Write-Host "PATH内容（上位5項目）:"
$pathItems | Select-Object -First 5 | ForEach-Object { Write-Host "  $_" }
```

---

## 💡 実用的なスクリプト例

### 開発環境セットアップスクリプト

```powershell
# dev-env-setup.ps1
param(
    [switch]$Java,
    [switch]$Python,
    [switch]$Node
)

Write-Host "🚀 開発環境セットアップ開始" -ForegroundColor Green

if ($Java) {
    Write-Host "☕ Java環境の設定"
    $javaHome = "C:\Program Files\Java\jdk-17"
    
    if (Test-Path $javaHome) {
        [Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "User")
        $userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
        if ($userPath -notlike "*$javaHome\bin*") {
            [Environment]::SetEnvironmentVariable("PATH", "$userPath;$javaHome\bin", "User")
        }
        Write-Host "✅ Java環境設定完了" -ForegroundColor Green
    } else {
        Write-Host "❌ Java not found at $javaHome" -ForegroundColor Red
    }
}

if ($Python) {
    Write-Host "🐍 Python環境の設定"
    # Python検索とPATH追加ロジック
}

if ($Node) {
    Write-Host "📦 Node.js環境の設定" 
    # Node.js検索とPATH追加ロジック
}

Write-Host "🔄 設定を反映するためにターミナルを再起動してください" -ForegroundColor Yellow
```

### 環境変数バックアップ・復元スクリプト

```powershell
# env-backup.ps1
param(
    [switch]$Backup,
    [switch]$Restore,
    [string]$BackupFile = "env-backup.json"
)

if ($Backup) {
    Write-Host "💾 環境変数をバックアップ中..."
    
    $envBackup = @{
        UserVariables = @{}
        SystemVariables = @{}
    }
    
    # ユーザー環境変数
    $userVars = [Environment]::GetEnvironmentVariables("User")
    foreach ($key in $userVars.Keys) {
        $envBackup.UserVariables[$key] = $userVars[$key]
    }
    
    # システム環境変数
    $systemVars = [Environment]::GetEnvironmentVariables("Machine")
    foreach ($key in $systemVars.Keys) {
        $envBackup.SystemVariables[$key] = $systemVars[$key]
    }
    
    $envBackup | ConvertTo-Json -Depth 10 | Out-File -FilePath $BackupFile -Encoding UTF8
    Write-Host "✅ バックアップ完了: $BackupFile" -ForegroundColor Green
}

if ($Restore) {
    Write-Host "🔄 環境変数を復元中..."
    
    if (Test-Path $BackupFile) {
        $envBackup = Get-Content $BackupFile -Encoding UTF8 | ConvertFrom-Json
        
        # 復元処理（安全のため確認を求める）
        $confirm = Read-Host "環境変数を復元しますか？ (y/N)"
        if ($confirm -eq 'y') {
            # 復元ロジック
            Write-Host "✅ 復元完了" -ForegroundColor Green
        }
    } else {
        Write-Host "❌ バックアップファイルが見つかりません: $BackupFile" -ForegroundColor Red
    }
}
```

### 開発環境切り替えスクリプト

```powershell
# env-switcher.ps1
param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("java8", "java11", "java17", "python38", "python39", "default")]
    [string]$Environment
)

function Set-JavaEnvironment {
    param([string]$Version)
    
    $javaHomes = @{
        "java8" = "C:\Program Files\Java\jdk1.8.0_291"
        "java11" = "C:\Program Files\Java\jdk-11"
        "java17" = "C:\Program Files\Java\jdk-17"
    }
    
    $javaHome = $javaHomes[$Version]
    if (Test-Path $javaHome) {
        $env:JAVA_HOME = $javaHome
        $env:PATH = "$javaHome\bin;" + ($env:PATH -replace '[^;]*java[^;]*;', '')
        Write-Host "✅ $Version 環境に切り替えました" -ForegroundColor Green
        java -version
    } else {
        Write-Host "❌ $Version が見つかりません: $javaHome" -ForegroundColor Red
    }
}

switch ($Environment) {
    "java8" { Set-JavaEnvironment "java8" }
    "java11" { Set-JavaEnvironment "java11" }
    "java17" { Set-JavaEnvironment "java17" }
    "default" { 
        Write-Host "🔄 デフォルト環境に戻します"
        # PATH をリセット
    }
}
```

### 使用例

```bash
# 診断実行
.\env-diagnosis.ps1

# 開発環境セットアップ
.\dev-env-setup.ps1 -Java -Python

# 環境バックアップ
.\env-backup.ps1 -Backup

# Java環境切り替え
.\env-switcher.ps1 -Environment java17
```

---

*[戻る: メモハブ](./README-memos.md)*
