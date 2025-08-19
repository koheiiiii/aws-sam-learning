# 🚀 PowerShellスクリプト完全使用ガイド

## 📋 **基本的な実行方法**

### **1. すべてのテスト実行**
```powershell
.\test-run-commands.ps1 -Command test-all
```

### **2. 指定テスト実行（正しいクラス名を使用）**

#### **✅ DynamoDBリポジトリテスト**
```powershell
# ✅ 成功例：実際のクラス名で指定
.\test-run-commands.ps1 -Command test -TestClass "DynamoDbRepositoryTest"

# ✅ 成功例：部分検索
.\test-run-commands.ps1 -Command test -TestClass "*DynamoDb*"
```

#### **✅ 他のテストクラス**
```powershell
# Zuoraイベントハンドラーテスト
.\test-run-commands.ps1 -Command test -TestClass "ZuoraEventHandlerAppTest"

# サブスクリプションIDジェネレーターテスト
.\test-run-commands.ps1 -Command test -TestClass "SubscriptionIdGeneratorTest"
```

### **3. Gradlew Run実行**
```powershell
.\test-run-commands.ps1 -Command run
```

### **4. SAM Local実行**
```powershell
.\test-run-commands.ps1 -Command sam-invoke
```

### **5. インタラクティブメニュー**
```powershell
.\test-run-commands.ps1
```

## 📊 **実際のテストクラス名（重要！）**

| ファイル名 | 実際のクラス名 | Gradle指定 |
|------------|----------------|------------|
| `DynamoDBRepositoryTest.java` | `DynamoDbRepositoryTest` | ✅ `DynamoDbRepositoryTest` |
| `ZuoraEventHandlerAppTest.java` | `ZuoraEventHandlerAppTest` | ✅ `ZuoraEventHandlerAppTest` |
| `SubscriptionIdGeneratorTest.java` | `SubscriptionIdGeneratorTest` | ✅ `SubscriptionIdGeneratorTest` |

## ⚠️ **よくあるエラーと解決法**

### **Error: "No tests found for given includes"**
```
原因: ファイル名とクラス名が一致していない
解決: 実際のクラス名を使用する

❌ 間違い: "DynamoDBRepositoryTest"  (DBが大文字)
✅ 正しい: "DynamoDbRepositoryTest"   (Dbが正しい形)
```

### **文字化け問題**
```
原因: PowerShellの文字エンコーディング
解決: PowerShellで chcp 65001 を実行
```

### **パス問題**
```
原因: 実行ディレクトリが間違っている
解決: zuora-event-handlerディレクトリで実行
```

## 🔧 **トラブルシューティング**

### **1. 実際のテストクラス名を確認**
```powershell
Get-ChildItem -Recurse -Name "*.java" | Select-String "class.*Test"
```

### **2. Gradle直接実行で検証**
```powershell
cd ZuoraEventHandler
./gradlew test --tests "*DynamoDb*" --info
```

### **3. PowerShellエンコーディング設定**
```powershell
chcp 65001
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

## 📈 **高度な使用法**

### **正規表現パターン**
```powershell
# 部分検索
.\test-run-commands.ps1 -Command test -TestClass "*Repository*"

# 複数パターン（OR検索は直接Gradleで）
cd ZuoraEventHandler
./gradlew test --tests "*DynamoDb*" --tests "*Generator*"
```

### **メソッド単位実行（理論上可能）**
```powershell
# 注意：実際のメソッド名を確認してから実行
.\test-run-commands.ps1 -Command test -TestMethod "DynamoDbRepositoryTest.testPutSubscriptionRecordSuccess"
```

## 🎯 **成功例**

以下は動作が確認された実際の例です：

```powershell
# ✅ 成功例1: DynamoDBテスト実行
PS> .\test-run-commands.ps1 -Command test -TestClass "*DynamoDb*"
Result: テストが実行され、「異常系: publicSubscriptionIdが不正な形式の場合は例外」が確認できた

# ✅ 成功例2: 全テスト実行
PS> .\test-run-commands.ps1 -Command test-all
Result: 全テストが実行される

# ✅ 成功例3: MainMethod実行
PS> .\test-run-commands.ps1 -Command run
Result: ZuoraEventHandlerAppのmain関数が実行される
```
