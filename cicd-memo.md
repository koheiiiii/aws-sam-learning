# 🚀 CI/CD完全ガイド

## 目次
- [CI/CD基本概念](#cicd基本概念)
- [CI/CDツール比較](#cicdツール比較)
- [GitHub Actions実例](#github-actions実例)
- [環境別デプロイ戦略](#環境別デプロイ戦略)
- [実用的なワークフロー例](#実用的なワークフロー例)
- [セキュリティ設定](#セキュリティ設定)
- [トラブルシューティング](#トラブルシューティング)

---

## 🔄 CI/CD基本概念

### CI (Continuous Integration) - 継続的インテグレーション
**「コードをプッシュするたびに自動でテスト・ビルドする仕組み」**

```
開発者: コードをプッシュ
   ↓
CI: 自動で実行
   ├─ コード品質チェック（リント）
   ├─ 自動テスト実行
   ├─ ビルド
   └─ テスト結果通知
```

**💡 メリット:**
- ✅ 早期のバグ発見
- ✅ コード品質の維持
- ✅ チーム開発での競合回避
- ✅ 手動作業の削減

### CD (Continuous Deployment/Delivery) - 継続的デプロイ

**Continuous Delivery（継続的デリバリー）:**
- テスト通過後、**手動承認**でデプロイ可能状態に
- より慎重なアプローチ

**Continuous Deployment（継続的デプロイ）:**
- テスト通過後、**自動で本番デプロイ**
- より高速なリリースサイクル

```
CI完了 → CD開始
   ↓
ステージング環境へデプロイ
   ↓
テスト環境での検証
   ↓
本番環境へデプロイ（手動 or 自動）
```

---

## 🛠️ CI/CDツール比較

| ツール | 特徴 | 料金 | おすすめ用途 | 学習コスト |
|--------|------|------|------------|-----------|
| **GitHub Actions** | GitHub統合・豊富な無料枠 | 🆓/💰 | OSS・個人・小中規模 | 🟢 低い |
| **GitLab CI/CD** | GitLab統合・設定柔軟 | 🆓/💰 | 企業・プライベート | 🟡 中程度 |
| **CircleCI** | 高速・使いやすい | 🆓/💰 | スタートアップ・高速CI | 🟢 低い |
| **Jenkins** | オンプレ・カスタマイズ性抜群 | 🆓 | 企業・レガシー・複雑な要件 | 🔴 高い |
| **AWS CodePipeline** | AWS統合・スケーラブル | 💰 | AWSメイン・エンタープライズ | 🟡 中程度 |
| **Azure DevOps** | Microsoft統合 | 🆓/💰 | .NET・Azure・企業 | 🟡 中程度 |

### 📊 選択の指針

```
🆓 無料で始めたい → GitHub Actions, GitLab CI
🚀 高速CI/CDが欲しい → CircleCI, GitHub Actions
🏢 企業の複雑な要件 → Jenkins, GitLab CI
☁️ AWSメイン → CodePipeline
🔧 フルカスタマイズ → Jenkins
```

---

## 🎯 GitHub Actions実例

### 基本的なJavaプロジェクトのワークフロー

```yaml
# .github/workflows/ci.yml
name: Java CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  JAVA_VERSION: '17'

jobs:
  # テスト・ビルド
  test:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK
      uses: actions/setup-java@v3
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Run tests
      run: ./gradlew test
      
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Gradle Tests
        path: build/test-results/test/*.xml
        reporter: java-junit
        
    - name: Build JAR
      run: ./gradlew build
      
    - name: Upload build artifact
      uses: actions/upload-artifact@v3
      with:
        name: jar-artifact
        path: build/libs/*.jar

  # コード品質チェック
  quality:
    runs-on: ubuntu-latest
    needs: test
    
    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v3
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        
    - name: Run SpotBugs
      run: ./gradlew spotbugsMain
      
    - name: Run Checkstyle
      run: ./gradlew checkstyleMain
      
    - name: Upload SpotBugs results
      uses: actions/upload-artifact@v3
      with:
        name: spotbugs-results
        path: build/reports/spotbugs/
        
  # セキュリティチェック
  security:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        scan-type: 'fs'
        scan-ref: '.'

  # デプロイ（本番環境）
  deploy-production:
    runs-on: ubuntu-latest
    needs: [test, quality]
    if: github.ref == 'refs/heads/main' && github.event_name != 'pull_request'
    environment: production
    
    steps:
    - name: Download build artifact
      uses: actions/download-artifact@v3
      with:
        name: jar-artifact
        
    - name: Deploy to production
      run: |
        echo "🚀 Deploying to production..."
        # 実際のデプロイスクリプト実行
        # scp *.jar user@prod-server:/app/
        # ssh user@prod-server "sudo systemctl restart myapp"
```

### Node.js プロジェクトの例

```yaml
# .github/workflows/node-ci.yml
name: Node.js CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    strategy:
      matrix:
        node-version: [16.x, 18.x, 20.x]
    
    steps:
    - uses: actions/checkout@v4
    - name: Use Node.js ${{ matrix.node-version }}
      uses: actions/setup-node@v3
      with:
        node-version: ${{ matrix.node-version }}
        cache: 'npm'
        
    - run: npm ci
    - run: npm run build --if-present
    - run: npm test
    - run: npm run lint
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
```

---

## 🌍 環境別デプロイ戦略

### 環境構成例

```
開発フロー:
feature/xxx → develop → staging → main → production

環境マッピング:
- feature branches → プレビュー環境（動的作成）
- develop branch → 開発環境（development）
- main branch → ステージング環境（staging）
- tags/releases → 本番環境（production）
```

### 環境別ワークフロー

```yaml
name: Environment-based Deploy

on:
  push:
    branches: [main, develop]
    tags: ['v*']

jobs:
  # 開発環境
  deploy-dev:
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    environment: 
      name: development
      url: https://dev.myapp.com
    
    steps:
    - name: Deploy to dev
      run: |
        echo "🧪 Deploying to development..."
        # kubectl apply -f k8s/dev/
        
  # ステージング環境
  deploy-staging:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment:
      name: staging
      url: https://staging.myapp.com
    
    steps:
    - name: Deploy to staging
      run: |
        echo "🎭 Deploying to staging..."
        # kubectl apply -f k8s/staging/
        
  # 本番環境（タグベース）
  deploy-production:
    if: startsWith(github.ref, 'refs/tags/v')
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://myapp.com
    needs: [test, security] # 必須チェック完了後
    
    steps:
    - name: Deploy to production
      run: |
        echo "🚀 Deploying to production..."
        echo "Version: ${GITHUB_REF#refs/tags/}"
        # kubectl apply -f k8s/prod/
```

### 環境設定の管理

```yaml
# 環境変数とシークレットの使い分け
jobs:
  deploy:
    steps:
    - name: Set environment variables
      run: |
        if [[ "${{ github.ref }}" == "refs/heads/develop" ]]; then
          echo "DATABASE_URL=${{ secrets.DEV_DATABASE_URL }}" >> $GITHUB_ENV
          echo "API_KEY=${{ secrets.DEV_API_KEY }}" >> $GITHUB_ENV
        elif [[ "${{ github.ref }}" == "refs/heads/main" ]]; then
          echo "DATABASE_URL=${{ secrets.STAGING_DATABASE_URL }}" >> $GITHUB_ENV
          echo "API_KEY=${{ secrets.STAGING_API_KEY }}" >> $GITHUB_ENV
        fi
```

---

## 💼 実用的なワークフロー例

### モノリポ対応ワークフロー

```yaml
name: Monorepo CI/CD

on:
  push:
    paths:
      - 'services/**'
      - '.github/workflows/**'

jobs:
  detect-changes:
    runs-on: ubuntu-latest
    outputs:
      backend: ${{ steps.changes.outputs.backend }}
      frontend: ${{ steps.changes.outputs.frontend }}
      
    steps:
    - uses: actions/checkout@v4
    - uses: dorny/paths-filter@v2
      id: changes
      with:
        filters: |
          backend:
            - 'services/backend/**'
          frontend:
            - 'services/frontend/**'

  test-backend:
    needs: detect-changes
    if: ${{ needs.detect-changes.outputs.backend == 'true' }}
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ./services/backend
    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    - run: ./gradlew test

  test-frontend:
    needs: detect-changes
    if: ${{ needs.detect-changes.outputs.frontend == 'true' }}
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ./services/frontend
    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v3
      with:
        node-version: '18'
        cache: 'npm'
        cache-dependency-path: services/frontend/package-lock.json
    - run: npm ci
    - run: npm test
```

### マトリックス戦略での複数環境テスト

```yaml
name: Cross-platform testing

jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      fail-fast: false
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        java: ['11', '17', '21']
        include:
          - os: ubuntu-latest
            java: '17'
            upload-coverage: true
    
    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v3
      with:
        java-version: ${{ matrix.java }}
        distribution: 'temurin'
        
    - name: Run tests
      run: ./gradlew test
      
    - name: Upload coverage
      if: matrix.upload-coverage
      uses: codecov/codecov-action@v3
```

---

## 🔒 セキュリティ設定

### シークレット管理

```yaml
# 機密情報の適切な管理
jobs:
  deploy:
    steps:
    - name: Login to Docker Hub
      uses: docker/login-action@v2
      with:
        username: ${{ secrets.DOCKERHUB_USERNAME }}
        password: ${{ secrets.DOCKERHUB_TOKEN }} # パスワードではなくトークン使用
        
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v2
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: ap-northeast-1
```

### OpenID Connect (OIDC) を使用した認証

```yaml
# よりセキュアなAWS認証（推奨）
jobs:
  deploy:
    runs-on: ubuntu-latest
    permissions:
      id-token: write # OIDC トークン用
      contents: read
      
    steps:
    - name: Configure AWS credentials with OIDC
      uses: aws-actions/configure-aws-credentials@v2
      with:
        role-to-assume: arn:aws:iam::123456789012:role/GitHubActionsRole
        aws-region: ap-northeast-1
        # アクセスキー不要、一時的な認証情報を自動取得
```

### 脆弱性スキャン

```yaml
jobs:
  security-scan:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Dependency vulnerability scan
      uses: github/super-linter@v4
      env:
        DEFAULT_BRANCH: main
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        
    - name: Container vulnerability scan
      uses: aquasecurity/trivy-action@master
      with:
        image-ref: 'myapp:latest'
        format: 'sarif'
        output: 'trivy-results.sarif'
        
    - name: Upload Trivy scan results
      uses: github/codeql-action/upload-sarif@v2
      with:
        sarif_file: 'trivy-results.sarif'
```

---

## 🚨 トラブルシューティング

### よくある問題と解決方法

#### 問題1: ビルドが遅い
```yaml
# 解決策: キャッシュの活用
- name: Cache dependencies
  uses: actions/cache@v3
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
      ~/.m2
    key: ${{ runner.os }}-dependencies-${{ hashFiles('**/*.gradle*', '**/pom.xml') }}
    
# 並列実行の活用
jobs:
  test-unit:
    # 単体テスト
  test-integration:
    # 結合テスト（並列実行）
```

#### 問題2: 認証エラー
```yaml
# 権限設定の確認
jobs:
  deploy:
    permissions:
      contents: read
      packages: write # パッケージ公開時
      actions: write # ワークフロー制御時
```

#### 問題3: 環境依存の問題
```yaml
# 解決策: Dockerコンテナでの統一環境
jobs:
  test:
    runs-on: ubuntu-latest
    container:
      image: openjdk:17-jdk-slim
    services:
      postgres:
        image: postgres:13
        env:
          POSTGRES_PASSWORD: postgres
```

### デバッグ用設定

```yaml
# デバッグ情報の有効化
env:
  ACTIONS_RUNNER_DEBUG: true # ランナーの詳細ログ
  ACTIONS_STEP_DEBUG: true   # ステップの詳細ログ

# SSH デバッグ（緊急時用）
- name: Setup tmate session
  if: failure() # 失敗時のみ
  uses: mxschmitt/action-tmate@v3
```

### パフォーマンス監視

```yaml
name: Performance Monitoring

jobs:
  benchmark:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Run performance tests
      run: ./gradlew performanceTest
      
    - name: Store benchmark result
      uses: benchmark-action/github-action-benchmark@v1
      with:
        tool: 'jmh'
        output-file-path: build/reports/jmh/results.json
        github-token: ${{ secrets.GITHUB_TOKEN }}
        auto-push: true
        comment-on-alert: true
        alert-threshold: '200%' # パフォーマンス劣化の閾値
```

---

## 🎯 CI/CD のベストプラクティス

### 📝 設計原則

1. **Fast Feedback（高速フィードバック）**
   - テストは並列実行
   - 高速テストを先に実行
   - 早期失敗でリソース節約

2. **Build Once, Deploy Everywhere**
   - アーティファクトは一度だけビルド
   - 環境間での設定の違いは環境変数で吸収

3. **Security by Design**
   - シークレットは適切に管理
   - 最小権限の原則
   - 依存関係の脆弱性チェック

4. **Observability（可観測性）**
   - ログは構造化
   - メトリクス収集
   - アラート設定

---

*[戻る: メモハブ](./README-memos.md)*
