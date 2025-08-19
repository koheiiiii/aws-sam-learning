# AWS SAM Java Lambda - 技術メモ

## 📖 目次
- [🏗️ Build処理の違い](#-build処理の違い)
- [🚀 Lambda実行環境の違い](#-lambda実行環境の違い)
- [📦 main関数とApplicationプラグインの役割](#-main関数とapplicationプラグインの役割)
- [🐳 Docker Lambda イメージの仕組み](#-docker-lambda-イメージの仕組み)
- [🔒 SSL証明書エラー対策と環境変数処理](#-ssl証明書エラー対策と環境変数処理)
- [🧪 テスト戦略](#-テスト戦略)
- [⚡ PowerShell自動化スクリプト](#-powershell自動化スクリプト)
- [🛠️ トラブルシューティング](#-トラブルシューティング)

---

## 🏗️ Build処理の違いWS SAM Java Lambda - 技術メモ

## � Build処理の違い

### Gradle build vs SAM build

| 項目 | Gradle build | SAM build |
|------|--------------|-----------|
| **実行場所** | `ZuoraEventHandler/` フォルダ | プロジェクトルート |
| **コマンド** | `.\gradlew build` | `sam build` |
| **実際の処理** | **Gradleが直接コンパイル** | **内部でGradleを呼び出し** |
| **成果物の場所** | `build/` フォルダ | `.aws-sam/build/` フォルダ |
| **目的** | Java開発の標準ビルド | Lambda用にパッケージング |

### 📋 詳細な処理フロー

#### Gradle build
```
.\gradlew build
    ↓
Javaソースコード (.java)
    ↓
コンパイル (javac)
    ↓
バイトコード (.class)
    ↓
build/classes/java/main/com/fujifilm/.../ZuoraEventHandlerApp.class
```

#### SAM build
```
sam build
    ↓
template.yamlを読み取り (Runtime: java21)
    ↓
Gradle Buildワークフローを起動
    ↓
ZuoraEventHandler/ で .\gradlew build 実行
    ↓
コンパイル結果 + 全依存関係を .aws-sam/build/ にパッケージング
```

### 🎯 重要なポイント

✅ **基本は同じ**: どちらもJavaソースコード → バイトコード変換  
✅ **SAM buildの真の価値**: Gradle build + Lambda用最適化  
- 全依存関係をflat構造でパッケージング  
- Lambda Runtime APIとの連携準備  
- Dockerコンテナでの実行準備  

---

## 🔧 IDE実行・デバッグ用のmain関数実装

### Lambda vs 通常Javaアプリケーションの違い

| 項目 | Lambda関数 | 通常Javaアプリ |
|------|------------|---------------|
| **エントリーポイント** | `handleRequest()` メソッド | `main()` メソッド |
| **実行環境** | AWS Lambda Runtime | JVM |
| **呼び出し元** | AWS Lambda Runtime API | OSのプロセス起動 |
| **IDE実行** | ❌ 直接実行不可 | ✅ Run/Debugボタンで実行可能 |

### 🎯 main関数実装の目的

SAM Lambdaプロジェクトに**開発・デバッグ用**のmain関数を追加：

```java
public class ZuoraEventHandlerApp implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    // Lambda用のハンドラー（本番で使用）
    public APIGatewayProxyResponseEvent handleRequest(...) {
        // 実際のLambda処理
    }
    
    // IDE実行・デバッグ用（開発でのみ使用）
    public static void main(String[] args) {
        // テスト用データでhandleRequestを呼び出し
        ZuoraEventHandlerApp handler = new ZuoraEventHandlerApp();
        APIGatewayProxyRequestEvent testRequest = new APIGatewayProxyRequestEvent();
        testRequest.setBody("{\"OrderId\": \"test-order-12345\"}");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(testRequest, null);
        logger.info("Status Code: {}", response.getStatusCode());
        logger.info("Response Body: {}", response.getBody());
    }
}
```

### 📋 Gradle applicationプラグイン設定

#### build.gradle での設定
```gradle
plugins {
    id 'java'
    id 'application'  // ← main関数実行機能を追加
}

// アプリケーション設定
application {
    mainClass = 'com.fujifilm.fb.spf.subscription.zuora.complement.ZuoraEventHandlerApp'
}
```

#### 各設定の役割

| 設定項目 | 役割 | 効果 |
|---------|------|------|
| **`id 'application'`** | Gradleに実行可能アプリ機能を追加 | `./gradlew run` コマンドが使用可能に |
| **`mainClass = '...'`** | 実行するクラスのmain関数を指定 | `run` タスク実行時にこのmain関数が呼ばれる |

#### 実行フロー
```bash
./gradlew run 
    ↓
applicationプラグインが動作
    ↓ 
mainClassで指定されたクラスを検索
    ↓
ZuoraEventHandlerApp.main(String[] args) 実行
    ↓
テスト用データでhandleRequestメソッド呼び出し
    ↓
実際のLambda処理ロジックが実行される
```

### 🔄 実行方法の使い分け

| 実行方法 | 用途 | 特徴 | コマンド |
|---------|------|------|---------|
| **JUnitテスト** | 単体テスト | 高速、モック可能 | `./gradlew test` |
| **main関数実行** | 統合デバッグ | IDE連携、実環境接続 | `./gradlew run` |
| **sam local invoke** | Lambda環境テスト | 本番相当環境 | `sam local invoke` |

### 💡 開発ワークフローでの活用

```bash
# 1. 日常開発（高速サイクル）
コード修正 → ./gradlew run (main関数) → デバッグ・動作確認

# 2. 単体テスト
./gradlew test (JUnit) → ロジックの正確性確認

# 3. 統合テスト  
sam local invoke (Lambda環境) → 本番環境での最終確認
```

### ⚠️ 注意点

- **main関数**: 開発・デバッグ専用（本番Lambda環境では使用されない）
- **handleRequest**: 実際のLambda関数（本番環境で使用される）
- **applicationプラグイン**: 開発効率向上のためのツール設定

### 📦 成果物の比較

**Gradle build成果物**: `ZuoraEventHandler/build/`
```
build/classes/java/main/
├── com/fujifilm/.../ZuoraEventHandlerApp.class
├── DaggerApplicationComponent.class
└── その他の.classファイル
```

**SAM build成果物**: `.aws-sam/build/ZuoraEventHandler/`
```
├── com/ (クラスファイル)          ← Gradleからコピー
├── lib/ (依存関係JAR)            ← 全依存関係をまとめてパッケージ
│   ├── aws-lambda-java-core-1.2.3.jar
│   ├── zuora-sdk-java-3.8.0.jar
│   └── 80個以上のJARファイル
├── META-INF/                     ← JARメタデータ
└── log4j2.xml                    ← 設定ファイル
```

---

## �🔧 Java実行環境の違い

| フェーズ | 使用されるJava | 場所 | 確認方法 |
|---------|---------------|------|----------|
| **sam build** | ローカルJDK | Windows PC | `java -version` |
| **sam local invoke** | Docker Lambda Java | Dockerコンテナ | `check-versions.bat` |
| **AWS Lambda本番** | AWS Lambda Runtime | AWSクラウド | CloudWatch Logs |

### 📋 詳細説明

#### 1️⃣ sam build
- **目的**: JavaソースコードをコンパイルしてJARファイルを作成
- **実行場所**: 開発者のローカルPC
- **使用Java**: PCにインストールされたJDK
- **成果物**: `.aws-sam/build/ZuoraEventHandler/` にコンパイル済みJAR

#### 2️⃣ sam local invoke
- **目的**: Lambdaをローカルでテスト実行
- **実行場所**: Dockerコンテナ内
- **使用Java**: AWS公式LambdaランタイムのJava
- **特徴**: 本番環境に最も近いテスト環境

#### 3️⃣ AWS Lambda本番
- **目的**: 実際のLambda関数実行
- **実行場所**: AWSクラウド
- **使用Java**: AWS管理のLambdaランタイム
- **監視**: CloudWatch Logsでログ確認

---

## 🐳 Docker Lambda イメージの仕組み

### イメージ構造
```
public.ecr.aws/lambda/java:21
├── Amazon Linux 2ベース
├── Java 21 (Amazon Corretto)
├── Lambda Runtime API
└── エントリーポイント（Lambda Handler）
```

### template.yamlとの連携
```yaml
Runtime: java21  # ← この設定が重要
```
↓
```bash
# 実際に使われるDockerイメージ
public.ecr.aws/lambda/java:21
```

### バージョン確認コマンドの仕組み
```bash
# 通常のLambda起動（失敗する）
docker run public.ecr.aws/lambda/java:21 java -version
# エラー: "entrypoint requires the handler name"

# エントリーポイント上書き（成功）
docker run --rm --entrypoint="sh" public.ecr.aws/lambda/java:21 -c "java -version"
# 成功: Javaバージョンが表示される
```

**理由**: LambdaコンテナはLambda関数専用設計のため、通常のLinuxコマンドは直接実行できない

---

## 🔒 SSL証明書エラー対策と環境変数処理

### SSL証明書問題の発生場所
| 環境 | SSL問題 | 原因 |
|------|---------|------|
| **ローカルJava実行** | なし | 正常な証明書ストア |
| **Docker Lambda** | **あり** | 限定的な証明書ストア |
| **AWS Lambda本番** | なし | AWS管理の証明書 |

### エラーの詳細
```bash
javax.net.ssl.SSLHandshakeException: PKIX path building failed
```
- **意味**: 「この Zuora の SSL 証明書、信じていいのかわからない！」とJVMがパニック
- **原因**: Docker環境での証明書チェーンの相違

### SslConfig.java による解決策

#### 🔧 完全なSSL無効化実装
```java
/**
 * SSL証明書チェック無効化 (sam local invoke用)
 * javax.net.ssl.SSLHandshakeException: PKIX path building failed エラーを解消
 */
public static void disableSslVerification() {
    try {
        // 【1】全ての証明書を信頼するTrustManager作成
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                // 【1-A】サーバーが信頼できる認証局のリストを返す → 空で「全部OK」
                public X509Certificate[] getAcceptedIssuers() { return null; }
                // 【1-B】クライアント証明書チェック → 何もしない「全部OK」  
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                // 【1-C】サーバー証明書チェック → 何もしない「全部OK」
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
        
        // 【2】SSLContextに「証明書チェック無効化」を設定
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        
        // 【3】HostnameVerifierも無効化
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        
    } catch (Exception e) {
        System.err.println("SSL無効化設定エラー: " + e.getMessage());
    }
}
```

#### 各部分の必要性

| コード部分 | 必要性 | 理由 |
|-----------|-------|------|
| **【1-C】checkServerTrusted** | ✅ **最重要** | PKIX path building failed の直接的解消 |
| **【1-A】getAcceptedIssuers** | ✅ **必須** | X509TrustManagerインターフェース実装要件 |
| **【1-B】checkClientTrusted** | ✅ **必須** | X509TrustManagerインターフェース実装要件 |
| **【2】SSLContext設定** | ✅ **必須** | HttpsURLConnection全体への適用 |
| **【3】HostnameVerifier** | ⚠️ **推奨** | ホスト名不一致エラーも予防 |

#### 自動適用の仕組み
```java
// ZuoraEventHandlerApp.java コンストラクタ
public ZuoraEventHandlerApp() {
    // sam local invoke環境でSSL証明書エラー回避
    SslConfig.disableSslVerification();
    DaggerApplicationComponent.create().inject(this);
}
```

### 環境変数とデフォルト値処理

#### 問題: IDE実行時の環境変数NULL

| 実行方法 | 環境変数読み込み | 結果 |
|----------|------------------|------|
| **sam local invoke** | ✅ template.yamlから | 設定値使用 |
| **IDE main関数** | ❌ 環境変数なし | ❌ zuoraEndpoint = NULL |

#### 解決策: デフォルト値の自動設定

```java
// 環境変数取得時にデフォルト値を設定
String zuoraEndpoint = System.getenv("ZUORA_ENDPOINT");
if (zuoraEndpoint == null) {
    zuoraEndpoint = "https://rest.apisandbox.zuora.com/v1/";
    logger.info("環境変数なし、デフォルト値使用");
}

// Secrets Manager パラメータも同様
String secretParam = System.getenv("ZUORA_API_SECRET");
if (secretParam == null) {
    secretParam = "qa/zuora/apis";
    logger.info("環境変数なし、デフォルト値使用");
}
```

#### 実行環境別の動作

| 実行方法 | 環境変数 | SSL証明書 | 結果 |
|----------|----------|----------|------|
| **IDE main関数** | デフォルト値使用 | SSL無効化適用 | ✅ 完全動作 |
| **sam local invoke** | template.yaml使用 | SSL無効化適用 | ✅ 完全動作 |
| **AWS Lambda本番** | 環境変数使用 | SSL正常動作 | ✅ 完全動作 |

### ⚠️ セキュリティ考慮事項

- **SSL無効化**: 開発・テスト環境限定の対策
- **本番環境**: この設定は不要（AWS Lambda環境では証明書正常）
- **用途限定**: Zuora API接続の証明書エラー回避のみ

---

### 解決策: SslConfig
```java
@Component
public class SslConfig {
    public boolean isLocalEnvironment() {
        return "true".equals(System.getenv("FORCE_LOCAL_SSL_CONFIG")) ||
               System.getenv("AWS_LAMBDA_FUNCTION_NAME") == null;
    }
}
```

### 環境変数での制御
- `FORCE_LOCAL_SSL_CONFIG=true`: Docker環境でSSL緩和を強制
- env-vars.jsonで設定: sam local invoke用

---

## 🏗️ プロジェクト構成

### 依存関係管理
```gradle
dependencies {
    // AWS Lambda
    implementation 'com.amazonaws:aws-lambda-java-core:1.2.3'
    implementation 'com.amazonaws:aws-lambda-java-events:3.11.0'
    
    // Zuora SDK
    implementation 'com.zuora.sdk:zuora-sdk-java:3.8.0'
    
    // Dependency Injection
    implementation 'com.google.dagger:dagger:2.57'
    annotationProcessor 'com.google.dagger:dagger-compiler:2.57'
    
    // AWS SDK v2
    implementation 'software.amazon.awssdk:dynamodb:2.32.14'
    implementation 'software.amazon.awssdk:ssm:2.32.12'
}
```

### Dagger 2 DIパターン
```java
@Component(modules = SslModule.class)
public interface AppComponent {
    ZuoraEventHandlerApp app();
}

@Module
public class SslModule {
    @Provides
    SslConfig provideSslConfig() {
        return new SslConfig();
    }
}
```

---

## 🚀 実行方法まとめ

### 1. バージョン確認
```batch
.\check-versions.bat
```

### 2. ビルドコマンド（推奨）

**方法A: インタラクティブメニュー**
```batch
.\build-commands.bat
```
- [1] Gradle build (高速開発用)
- [2] SAM build (Lambda用パッケージング)
- [3] SAM build --use-container (クリーンビルド)
- [4] 全部クリーンして再ビルド
- [5] ビルド成果物の確認
- [6] Gradle clean build (依存関係リフレッシュ)
- [7] 完全クリーンビルド (キャッシュ全削除)
- [8] 依存関係確認・解決
- [9] トラブルシューティングビルド

**方法B: 高速ビルド**
```batch
.\quick-build.bat
```
Gradle build → SAM build を自動実行

**方法C: 従来のSAMコマンド**
```bash
sam build
```

### 3. テストコマンド
```batch
.\test-commands.bat
```
- [1] Gradle Test (ユニットテスト)
- [2] Main Method Test (直接実行)
- [3] SAM Local Invoke Test
- [4] 全部テスト (Build + Test)

### 4. ローカル実行（2つの方法）

**方法A: sam local invoke（Docker）**
```bash
sam local invoke ZuoraEventHandler --event events/event.json --env-vars env-vars.json
```

**方法B: 直接Java実行（高速）**
```batch
.\run-local-java.bat
```

### 5. デプロイ
```bash
sam deploy --guided
```

---

## 📝 開発のベストプラクティス

### 1. ビルド戦略
- ✅ **日常開発**: `.\quick-build.bat`で高速ビルド
- ✅ **トラブル時**: `.\build-commands.bat` → [9]トラブルシューティング
- ✅ **依存関係更新**: `.\build-commands.bat` → [6]依存関係リフレッシュ  
- ✅ **完全リセット**: `.\build-commands.bat` → [7]完全クリーンビルド

### 2. 環境の一貫性
- ✅ `template.yaml`のRuntimeを信頼できる情報源とする
- ✅ `check-versions.bat`で定期的にバージョン確認
- ✅ ローカルJavaとLambda Javaのバージョンを統一

### 3. テスト戦略
- ✅ 開発中は`run-local-java.bat`で高速テスト
- ✅ デプロイ前は`sam local invoke`で本番環境に近いテスト
- ✅ SSL関連は両方の環境で確認

### 4. ログ・デバッグ
- ✅ Log4j2でJSON形式の構造化ログ
- ✅ 環境検出ログで実行コンテキストを明確化
- ✅ SSL設定の有効/無効をログで確認

---

## 🔄 トラブルシューティング

### よくある問題と解決法

| 問題 | 症状 | 解決法 |
|------|------|--------|
| **SSL証明書エラー** | `PKIX path building failed` | `FORCE_LOCAL_SSL_CONFIG=true` |
| **Docker起動失敗** | `manifest not found` | `template.yaml`のRuntimeを確認 |
| **Java Version不一致** | ビルドエラー | `check-versions.bat`でバージョン確認 |
| **依存関係エラー** | `ClassNotFoundException` | `sam build --use-container` |
| **Gradle build失敗** | コンパイルエラー | `.\build-commands.bat` → [9]トラブルシューティング |
| **SAM build遅い** | ビルド時間長い | `.\quick-build.bat`で高速確認 |
| **成果物不一致** | 古いコードが実行される | `.\build-commands.bat` → [7]完全クリーンビルド |
| **依存関係競合** | `ClassNotFoundException` | `.\build-commands.bat` → [8]依存関係確認・解決 |
| **キャッシュ問題** | 一貫性のない動作 | `.\build-commands.bat` → [7]完全クリーンビルド |

---

## 📚 参考リソース

- [AWS SAM Developer Guide](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/)
- [AWS Lambda Java Runtime](https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html)
- [Zuora SDK Documentation](https://github.com/Zuora/zuora-sdk-java)
- [Dagger 2 User Guide](https://dagger.dev/users-guide)

---

## 🧪 テスト環境とテスト戦略

### 実行環境の比較

| 実行方法 | 環境 | Java実行場所 | 特徴 | 用途 |
|---------|------|-------------|------|------|
| **`./gradlew test`** | **ローカルPC** | Windows + ローカルJVM | ✅高速 ❌環境差異検出不可 | JUnit単体テスト |
| **`./gradlew run`** | **ローカルPC** | Windows + ローカルJVM | ✅IDE連携 ✅デバッグ可能 | 開発中の統合デバッグ |
| **`sam local invoke`** | **Dockerコンテナ** | Linux + Lambda Runtime | ✅本番相当 ❌起動遅い | 統合テスト・デプロイ前確認 |

### 🎯 **結論: 3層のテスト環境が必要！**

#### **理由1: テストピラミッド戦略**
```
        /\      sam local invoke
       /  \     (統合テスト - 少数・遅い・高信頼)
      /____\    
     /      \   ./gradlew run (main関数)
    /        \  (統合デバッグ - 中程度・IDE連携)
   /__________\  
  /            \ ./gradlew test (JUnit)
 /              \ (単体テスト - 多数・高速・基本機能)
/______________\
```

#### **理由2: 開発効率 vs 品質保証**
- **開発中**: `./gradlew test` で高速フィードバック
- **リリース前**: `sam local invoke` で本番環境確認

#### **理由3: 異なる問題を検出**
- **ローカルテスト**: ロジック、アルゴリズム、単体機能
- **Lambdaテスト**: 環境依存、リソース制限、統合動作

### 📋 **実装済み環境別対応**

#### **SslConfig - 環境自動検出**
```java
@Component  
public class SslConfig {
    public boolean isLocalEnvironment() {
        // ローカルPC環境 (gradlew test) → true
        // Docker Lambda環境 (sam local invoke) → false
        return "true".equals(System.getenv("FORCE_LOCAL_SSL_CONFIG")) ||
               System.getenv("AWS_LAMBDA_FUNCTION_NAME") == null;
    }
}
```

#### **環境変数管理**
```json
// env-vars.json (sam local invoke用)
{
  "ZuoraEventHandler": {
    "ZUORA_TENANT": "your-tenant",
    "FORCE_LOCAL_SSL_CONFIG": "true"  // Docker環境でSSL緩和
  }
}
```

### 🔄 **推奨テストフロー**

#### **日常開発 (高速サイクル)**
```bash
コード修正
    ↓
./gradlew test          # JUnit単体テスト (3-5秒)
    ↓ (成功時)
./gradlew run           # main関数でデバッグ確認 (10-15秒)
    ↓ (成功時)
sam local invoke        # Lambda環境統合テスト (30秒)
```

#### **IDE開発 (デバッグ重視)**
```bash
コード修正
    ↓
IDE Run/Debug           # main関数をIDE実行
    ↓ (ブレークポイント設定)
ステップ実行・変数確認   # IDEデバッガで詳細確認
    ↓ (問題解決後)
./gradlew test          # JUnit回帰テスト
```

#### **リリース前 (完全確認)**
```bash
./gradlew clean test                    # 完全なローカルテスト
    ↓
sam build --use-container              # クリーン環境ビルド
    ↓  
sam local invoke --debug               # 詳細ログ統合テスト
    ↓
実際のAWSデプロイ
```

### 🎯 **テスト戦略マトリックス**

| テスト種別 | JUnit(`gradlew test`) | main関数(`gradlew run`) | Lambda(`sam local invoke`) |
|-----------|---------------------|------------------------|----------------------------|
| **単体テスト** | ✅ メイン | ❌ 不要 | ❌ 不要 |
| **統合デバッグ** | ❌ 分離実行 | ✅ メイン | ✅ 最終確認 |
| **IDE連携** | ✅ テストランナー | ✅ Run/Debug | ❌ 外部実行 |
| **ブレークポイント** | ✅ テスト範囲 | ✅ 全フロー | ❌ 制限あり |
| **実環境接続** | ❌ モック推奨 | ✅ 可能 | ✅ 可能 |
| **SSL証明書テスト** | ❌ 関係なし | ✅ ローカル設定 | ✅ Docker設定 |
| **環境依存テスト** | ❌ 検出不可 | ❌ 検出不可 | ✅ 本番相当 |
| **実行速度** | ✅ 最高速 | ✅ 高速 | ❌ 遅い |

### 💡 **ベストプラクティス**

1. **開発時**: `./gradlew test` (JUnit) を基本とし、高速フィードバック重視
2. **デバッグ時**: `./gradlew run` (main関数) でIDE連携、詳細確認
1. **開発時**: `./gradlew test` (JUnit) を基本とし、高速フィードバック重視
2. **デバッグ時**: `./gradlew run` (main関数) でIDE連携、詳細確認
3. **機能完成時**: `sam local invoke` で統合動作確認  
4. **デプロイ前**: 3つの環境すべてでテストを実行
5. **CI/CD**: 全環境でのテストを自動化
6. **トラブル時**: 環境別に問題切り分け

---

**最終更新**: 2025年8月20日  
**プロジェクト**: zuora-event-handler  
**Author**: AWS SAM Learning Project
