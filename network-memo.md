# 🌐 ネットワーク・セキュリティ完全ガイド

## 目次
- [VPN・プロキシ基本知識](#vpn・プロキシ基本知識)
- [SSL証明書とは](#ssl証明書とは)
- [JavaでのSSL証明書エラー](#javaでのssl証明書エラー)
- [SSL証明書トラブルシューティング](#ssl証明書トラブルシューティング)
- [企業環境での実践対応](#企業環境での実践対応)
- [ネットワーク診断ツール](#ネットワーク診断ツール)

---

## 🌐 VPN・プロキシ基本知識

### 🤔 VPNとプロキシの違い

| 項目 | **VPN** | **プロキシ** |
|------|---------|-------------|
| **目的** | 🔒 安全な通信路を作る | 🚪 代理アクセス |
| **暗号化** | ✅ 通信全体を暗号化 | ❌ 基本的になし |
| **接続範囲** | 🌍 PC全体の通信 | 🎯 特定アプリのみ |
| **速度** | 🟡 やや遅い | ⚡ 比較的高速 |
| **用途** | リモートワーク・セキュリティ | アクセス制限回避・キャッシュ |

### 🔐 VPN（Virtual Private Network）
**「インターネット上に安全な専用道路を作る技術」**

```
あなたのPC → 暗号化トンネル → VPNサーバー → 目的地サーバー
     ↑_____________安全な通信路_____________↑
```

**🏢 企業でのVPN使用例:**
```
在宅勤務者: 自宅のPC
     ↓ VPN接続
会社ネットワーク: 社内システムに安全アクセス
     ↓
・社内Git（GitHub Enterprise）
・社内API
・データベース
```

**💡 VPNの種類:**
- **企業VPN**: 会社システムへ安全接続
- **商用VPN**: NordVPN、ExpressVPNなど
- **地域制限回避**: 海外サービスアクセス

### 🚪 プロキシ（Proxy）
**「代理でアクセスしてくれるサーバー」**

```
あなた → プロキシサーバー → 目的地
          ↑
      代理でアクセス
```

**🏢 企業でのプロキシ使用例:**
```
社員PC → 会社プロキシ → インターネット
            ↑
    ・アクセス制限
    ・ログ記録
    ・キャッシュ高速化
```

**💡 プロキシの種類:**
- **HTTP Proxy**: Web閲覧用
- **HTTPS Proxy**: 暗号化通信用  
- **SOCKS Proxy**: 全プロトコル対応

### 🔧 Git でのプロキシ設定

```bash
# プロキシ設定
git config --global http.proxy http://proxy.company.com:8080
git config --global https.proxy http://proxy.company.com:8080

# 認証が必要な場合
git config --global http.proxy http://username:password@proxy.company.com:8080

# プロキシ確認
git config --list | grep proxy

# プロキシ削除
git config --global --unset http.proxy
git config --global --unset https.proxy
```

**🔄 プロキシ切り替えスクリプト（PowerShell）:**
```powershell
# proxy-toggle.ps1
param([switch]$Enable, [switch]$Disable)

$proxyUrl = "http://proxy.company.com:8080"

if ($Enable) {
    Write-Host "🔗 プロキシを有効にします: $proxyUrl"
    git config --global http.proxy $proxyUrl
    git config --global https.proxy $proxyUrl
    Write-Host "✅ プロキシ設定完了"
}

if ($Disable) {
    Write-Host "❌ プロキシを無効にします"
    git config --global --unset http.proxy
    git config --global --unset https.proxy
    Write-Host "✅ プロキシ削除完了"
}

Write-Host "📊 現在のプロキシ設定:"
git config --list | grep proxy
```

**使用例:**
```bash
# プロキシ有効化
.\proxy-toggle.ps1 -Enable

# プロキシ無効化
.\proxy-toggle.ps1 -Disable
```

---

## 🔒 SSL証明書とは

### 🤔 SSL証明書って何？
**簡単に言うと：「このサイトが本物かどうか証明する身分証明書」**

**🏠 身近な例:**
```
あなた: https://github.com にアクセス
ブラウザ: 「GitHubのSSL証明書を確認中...」
証明書: 「私は本物のGitHubです。信頼できる機関が保証します」
ブラウザ: 「OK、安全に接続します」 → 🔒マークが表示
```

### 🔍 SSL証明書の仕組み
```
1. クライアント: 「接続したいです」
   ↓
2. サーバー: 「これが私の証明書です」
   ↓
3. クライアント: 「証明書を検証中...」
   - 発行者は信頼できるか？
   - 有効期限は大丈夫か？
   - ドメイン名は正しいか？
   ↓
4. 検証OK → 🔒安全な通信開始
   検証NG → ❌エラー表示
```

### 🏆 信頼できる認証局（CA）
- **Let's Encrypt** - 無料SSL証明書
- **DigiCert** - 企業向け高信頼
- **GlobalSign** - 国際的に信頼
- **自己署名証明書** - テスト環境用（本番非推奨）

---

## ☕ JavaでのSSL証明書エラー

### 🚨 よくあるエラーメッセージ
```
javax.net.ssl.SSLHandshakeException: 
sun.security.validator.ValidatorException: 
PKIX path building failed: 
sun.security.provider.certpath.SunCertPathBuilderException: 
unable to find valid certification path to requested target
```

**😰 日本語で言うと：**
「接続先のSSL証明書を信頼できません。怪しいサイトかもしれません。」

### 🎯 SSL証明書エラーが起こる場面

**1. 企業内での内部API呼び出し**
```java
// 社内APIにアクセス
RestTemplate restTemplate = new RestTemplate();
String result = restTemplate.getForObject("https://internal-api.company.com", String.class);
// エラー: SSL証明書が信頼されていません
```

**2. プロキシ環境下での外部API接続**
```java
// GitHub APIにアクセス（プロキシ経由）
HttpsURLConnection connection = (HttpsURLConnection) 
    new URL("https://api.github.com/user").openConnection();
// エラー: プロキシのSSL証明書が信頼されていません
```

**3. 自己署名証明書（オレオレ証明書）**
```bash
# 開発環境でよくある
https://localhost:8443/api/test
# エラー: 自己署名証明書は信頼されません
```

---

## 🛠️ SSL証明書トラブルシューティング

### 📊 解決方法の比較

| 方法 | セキュリティ | 難易度 | 推奨度 | いつ使う？ |
|------|-------------|-------|--------|-----------|
| **証明書を信頼ストアに追加** | ⭐⭐⭐ | 🟡 中 | 🏆 **推奨** | 正式な証明書 |
| **JVM起動時にSSL無効化** | ❌ 危険 | 🟢 簡単 | ⚠️ 開発のみ | テスト環境 |
| **コード内でSSL無効化** | ❌ 危険 | 🟢 簡単 | ⚠️ 開発のみ | プロトタイプ |
| **プロキシ設定で回避** | 🟡 中 | 🟡 中 | 🟢 良い | 企業環境 |

### 🏆 推奨：証明書を信頼ストアに追加

#### 🔍 ステップ1: 証明書の取得

**方法1: ブラウザから取得**
```
1. Chrome で https://internal-api.company.com にアクセス
2. アドレスバーの🔒マークをクリック
3. 「証明書」をクリック
4. 「詳細」タブ → 「ファイルにコピー」
5. Base64形式で保存 → company-cert.crt
```

**方法2: OpenSSLコマンド**
```bash
# 証明書をダウンロード
openssl s_client -connect internal-api.company.com:443 -showcerts < /dev/null 2>/dev/null | openssl x509 -outform PEM > company-cert.pem
```

#### 🔧 ステップ2: Javaの信頼ストア（cacerts）に追加

```bash
# 1. Java のインストール場所確認
where java
# 例: C:\Program Files\Java\jdk-17\bin\java.exe

# 2. cacerts ファイルの場所
cd "C:\Program Files\Java\jdk-17\lib\security"

# 3. 証明書を追加（管理者権限で実行）
keytool -import -alias company-internal -keystore cacerts -file company-cert.pem
# パスワード入力: changeit (デフォルト)

# 4. 追加確認
keytool -list -keystore cacerts -alias company-internal
```

#### 🎯 プロジェクト固有の信頼ストア作成（推奨）

```bash
# 専用の信頼ストアを作成
keytool -import -alias company-internal -keystore my-truststore.jks -file company-cert.pem
# 新しいパスワードを設定（例: mypassword）

# Javaアプリ起動時に指定
java -Djavax.net.ssl.trustStore=my-truststore.jks \
     -Djavax.net.ssl.trustStorePassword=mypassword \
     -jar myapp.jar
```

### ⚠️ 緊急時：SSL検証無効化（開発環境のみ）

#### 方法1: JVM起動時オプション
```bash
java -Dcom.sun.net.ssl.checkRevocation=false \
     -Dtrust_all_cert=true \
     -jar myapp.jar
```

#### 方法2: Spring Boot設定
```yaml
# application-dev.yml （開発環境のみ）
server:
  ssl:
    enabled: false
    
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db?useSSL=false&allowPublicKeyRetrieval=true
```

#### 方法3: コード内で無効化（最後の手段）
```java
@Configuration
@Profile("dev") // 開発環境のみ
public class SSLDisableConfig {
    
    @PostConstruct
    public void disableSSLVerification() {
        try {
            // すべての証明書を信頼（危険！）
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## 🔧 ネットワーク診断ツール

### 🔍 SSL接続診断コマンド

```bash
# 1. SSLハンドシェイクテスト
openssl s_client -connect github.com:443
# 成功すると証明書チェーン表示

# 2. Java固有のSSLデバッグ
java -Djavax.net.debug=ssl,handshake -jar myapp.jar

# 3. 現在の信頼ストア内容確認
keytool -list -keystore "$JAVA_HOME/lib/security/cacerts" | grep -i company

# 4. 証明書の有効期限確認
keytool -list -v -keystore cacerts -alias company-internal

# 5. curl でのSSL確認
curl -v https://github.com
```

### 💡 実用的なPowerShellスクリプト

```powershell
# ssl-cert-checker.ps1
param(
    [Parameter(Mandatory=$true)]
    [string]$hostname,
    [int]$port = 443
)

Write-Host "🔍 Checking SSL certificate for $hostname:$port" -ForegroundColor Yellow

try {
    $tcpClient = New-Object System.Net.Sockets.TcpClient($hostname, $port)
    $sslStream = New-Object System.Net.Security.SslStream($tcpClient.GetStream())
    $sslStream.AuthenticateAsClient($hostname)
    
    $cert = $sslStream.RemoteCertificate
    Write-Host "✅ SSL connection successful" -ForegroundColor Green
    Write-Host "Subject: $($cert.Subject)"
    Write-Host "Issuer: $($cert.Issuer)"  
    Write-Host "Valid from: $($cert.GetEffectiveDateString())"
    Write-Host "Valid to: $($cert.GetExpirationDateString())"
    
    # 有効期限チェック
    $expiryDate = [DateTime]$cert.GetExpirationDateString()
    $daysUntilExpiry = ($expiryDate - (Get-Date)).Days
    
    if ($daysUntilExpiry -lt 30) {
        Write-Host "⚠️  Certificate expires in $daysUntilExpiry days!" -ForegroundColor Red
    } else {
        Write-Host "✅ Certificate valid for $daysUntilExpiry more days" -ForegroundColor Green
    }
    
    $sslStream.Close()
    $tcpClient.Close()
    
} catch {
    Write-Host "❌ SSL connection failed: $($_.Exception.Message)" -ForegroundColor Red
}
```

**使用例:**
```bash
.\ssl-cert-checker.ps1 -hostname github.com
.\ssl-cert-checker.ps1 -hostname internal-api.company.com -port 8443
```

### 🌐 ネットワーク診断コマンド集

```bash
# 基本的な接続確認
ping google.com
nslookup github.com
telnet github.com 443

# プロキシ経由確認
curl --proxy http://proxy.company.com:8080 https://github.com

# Git接続テスト
git ls-remote https://github.com/octocat/Hello-World.git

# Java SSL接続テスト
java -Djavax.net.debug=ssl:handshake -Djavax.net.ssl.trustStore=cacerts \
     -cp . SSLTest https://api.github.com
```

---

## 🏢 企業環境での実践対応

### 📝 段階的トラブルシューティング

```
1. 問題の特定
   ├─ エラーメッセージの確認
   ├─ 接続先URLの確認  
   ├─ プロキシ環境かどうか確認
   └─ VPN接続状況の確認

2. 一時的回避（開発継続のため）
   ├─ SSL無効化で動作確認
   ├─ プロキシ設定の調整
   └─ 別の接続方法試行

3. 正式対応（セキュリティ確保）
   ├─ 証明書取得
   ├─ 信頼ストアへの追加
   ├─ 本番環境での動作確認
   └─ 設定の文書化

4. チーム共有
   ├─ 解決手順の記録
   ├─ ナレッジベース更新
   └─ 類似問題の予防策整備
```

### 🔧 企業環境での実用コマンド

```bash
# 企業プロキシ設定例
git config --global http.proxy http://proxy.corp.com:8080
git config --global https.proxy http://proxy.corp.com:8080

# プロキシ認証付き
git config --global http.proxy http://username:password@proxy.corp.com:8080

# 社内認証局証明書追加
keytool -import -alias corp-ca -keystore $JAVA_HOME/lib/security/cacerts -file corp-ca.crt

# Maven での企業環境設定
mvn -Dhttps.proxyHost=proxy.corp.com -Dhttps.proxyPort=8080 clean install

# Gradle での企業環境設定
./gradlew -Dhttps.proxyHost=proxy.corp.com -Dhttps.proxyPort=8080 build
```

### 💡 クイック診断スクリプト

```powershell
# network-diagnosis.ps1
Write-Host "🔍 ネットワーク・SSL診断開始" -ForegroundColor Yellow

# 基本接続確認
Write-Host "`n📡 基本接続確認"
Test-Connection -ComputerName google.com -Count 2

# プロキシ設定確認
Write-Host "`n🚪 Git プロキシ設定"
git config --list | Select-String proxy

# SSL証明書確認
Write-Host "`n🔒 SSL証明書確認"
$uri = "https://github.com"
try {
    $req = [System.Net.WebRequest]::Create($uri)
    $req.GetResponse().Close()
    Write-Host "✅ $uri - SSL接続成功" -ForegroundColor Green
} catch {
    Write-Host "❌ $uri - SSL接続失敗: $($_.Exception.Message)" -ForegroundColor Red
}

# Java truststore確認
Write-Host "`n☕ Java TrustStore情報"
if ($env:JAVA_HOME) {
    $cacerts = "$env:JAVA_HOME\lib\security\cacerts"
    if (Test-Path $cacerts) {
        Write-Host "TrustStore location: $cacerts"
        $certCount = (keytool -list -keystore $cacerts -storepass changeit 2>$null | Select-String "Certificate fingerprint").Count
        Write-Host "Trusted certificates: $certCount"
    }
} else {
    Write-Host "JAVA_HOME not set" -ForegroundColor Red
}
```

---

*[戻る: メモハブ](./README-memos.md)*
