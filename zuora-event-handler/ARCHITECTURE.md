# プロジェクト構成図

```
zuora-event-handler/
├── 📋 設定ファイル
│   ├── template.yaml          # SAMテンプレート (Runtime: java21)
│   ├── samconfig.toml         # SAM設定
│   └── env-vars.json          # ローカル実行用環境変数
│
├── 🔧 便利スクリプト (Windows)
│   ├── check-versions.bat     # バージョン確認
│   ├── build-commands.bat     # ビルドコマンド集
│   ├── quick-build.bat        # 高速ビルド (Gradle → SAM)
│   ├── test-commands.bat      # テストコマンド集
│   ├── run-local-java.bat     # 直接Java実行
│   └── run-sam-local.bat      # sam local invoke
│
├── 📖 ドキュメント
│   ├── README.md              # プロジェクト概要・使用方法
│   ├── TECHNICAL_NOTES.md     # 技術詳細・アーキテクチャ
│   └── 作業用メモ.md          # 開発メモ
│
├── 📦 イベント・テストデータ
│   └── events/
│       └── event.json         # Lambda実行用テストイベント
│
├── ☁️ Lambda関数コード
│   └── ZuoraEventHandler/
│       ├── build.gradle       # Gradle設定
│       ├── gradlew.bat       # Gradleラッパー
│       │
│       └── src/
│           ├── main/java/com/fujifilm/fb/spf/subscription/zuora/complement/
│           │   ├── ZuoraEventHandlerApp.java    # Lambda entrypoint
│           │   ├── config/
│           │   │   ├── AppComponent.java        # Dagger root component
│           │   │   └── SslModule.java          # SSL設定module
│           │   └── ssl/
│           │       └── SslConfig.java          # SSL証明書設定
│           │
│           ├── test/java/     # ユニットテスト
│           └── resources/
│               └── log4j2.xml # ログ設定
│
└── 🔨 ビルド成果物 (.gitignoreされる)
    ├── .aws-sam/
    │   └── build/             # sam buildの結果
    └── ZuoraEventHandler/
        ├── build/             # Gradle buildの結果
        └── bin/               # コンパイル結果
```

## Java実行フロー図

```
開発フェーズ: sam build
┌─────────────────────────────────────────────────────┐
│  Windows PC (ローカル開発環境)                         │
│  ┌─────────────────────────────────────────────────┐  │
│  │  Local JDK (Java 21.0.7 Corretto)              │  │
│  │  ├── Gradle Compile                            │  │
│  │  ├── Dependency Resolution                     │  │
│  │  └── JAR Packaging                             │  │
│  └─────────────────────────────────────────────────┘  │
│                       ↓                             │
│  📦 .aws-sam/build/ZuoraEventHandler/               │
│     ├── ZuoraEventHandler.jar                      │
│     ├── lib/ (dependencies)                        │
│     └── log4j2.xml                                 │
└─────────────────────────────────────────────────────┘

テストフェーズ: sam local invoke
┌─────────────────────────────────────────────────────┐
│  Docker Container (Lambda Simulation)               │
│  ┌─────────────────────────────────────────────────┐  │
│  │  Docker Lambda Java (21.0.7 Corretto)          │  │
│  │  ├── Lambda Runtime API                        │  │
│  │  ├── 📦 Mount: .aws-sam/build/                 │  │
│  │  ├── 🌍 env-vars.json                          │  │
│  │  └── 🚀 handleRequest() execution              │  │
│  └─────────────────────────────────────────────────┘  │
│                       ↓                             │
│  📊 Lambda Response + CloudWatch Logs              │
└─────────────────────────────────────────────────────┘

本番フェーズ: AWS Lambda
┌─────────────────────────────────────────────────────┐
│  AWS Cloud (Production)                             │
│  ┌─────────────────────────────────────────────────┐  │
│  │  AWS Lambda Runtime (Java 21)                  │  │
│  │  ├── Managed JVM                               │  │
│  │  ├── AWS Secrets Manager                       │  │
│  │  ├── DynamoDB Integration                      │  │
│  │  └── 🌐 Real Zuora API Calls                   │  │
│  └─────────────────────────────────────────────────┘  │
│                       ↓                             │
│  📈 CloudWatch Metrics & Logs                      │
└─────────────────────────────────────────────────────┘
```

## 技術スタック依存関係図

```
┌─────────────────────────────────────────────────────┐
│                Application Layer                     │
│  ┌─────────────────────────────────────────────────┐  │
│  │  ZuoraEventHandlerApp.java                      │  │
│  │  ├── @Inject SslConfig                         │  │
│  │  ├── AWS Lambda Handler                        │  │
│  │  └── Zuora API Client                          │  │
│  └─────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│                Framework Layer                       │
│  ┌──────────────────┐  ┌──────────────────┐         │
│  │  Dagger 2 DI     │  │  Log4j2 Logging  │         │
│  │  ├── @Component  │  │  ├── JSON Format │         │
│  │  ├── @Module     │  │  └── CloudWatch  │         │
│  │  └── @Provides   │  └──────────────────┘         │
│  └──────────────────┘                               │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│                Integration Layer                     │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐  │
│  │  AWS SDK v2  │ │  Zuora SDK   │ │  SSL Config  │  │
│  │  ├── DynamoDB│ │  ├── OAuth2  │ │  ├── Trust   │  │
│  │  ├── SSM     │ │  ├── REST API│ │  │    All     │  │
│  │  └── Secrets │ │  └── JSON    │ │  └── Local   │  │
│  └──────────────┘ └──────────────┘ └──────────────┘  │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│                Runtime Layer                         │
│  ┌──────────────────┐  ┌──────────────────┐         │
│  │  Java 21         │  │  AWS Lambda      │         │
│  │  ├── Corretto    │  │  ├── Runtime API │         │
│  │  ├── GraalVM     │  │  ├── Event Loop  │         │
│  │  └── Hotspot     │  │  └── Context     │         │
│  └──────────────────┘  └──────────────────┘         │
└─────────────────────────────────────────────────────┘
```
