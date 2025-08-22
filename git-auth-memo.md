# 🔐 Git認証完全ガイド

## 目次
- [認証の基本知識](#認証の基本知識)
- [認証方式比較](#認証方式比較)
- [パブリック vs プライベート](#パブリック-vs-プライベート)
- [認証が必要な操作](#認証が必要な操作)
- [現在の認証フロー](#現在の認証フロー)
- [credential.helper設定](#credentialhelper設定)
- [認証トラブルシューティング](#認証トラブルシューティング)

---

## 🎓 認証の基本知識

### 🤔 認証って何？
**「あなたが本当にその人かどうか確認する仕組み」**

### 📚 主要な認証方式一覧

| 認証方式 | 簡単な説明 | 使用例 | セキュリティ | 使いやすさ |
|---------|-----------|-------|-------------|------------|
| **パスワード認証** | ID・パスワード入力 | 昔のGitHub | ❌ 低い | 😩 面倒 |
| **OAuth** | 他サービスでログイン | "GitHubでログイン" | ⭐ 高い | 😊 簡単 |
| **SSO** | 一度ログインで全サービス | 会社システム | ⭐ 高い | 😊 楽 |
| **SAML** | 企業向けSSO | 大企業システム | ⭐⭐ 非常に高い | 🤔 設定複雑 |
| **SSL/TLS** | 通信の暗号化 | HTTPS通信 | ⭐⭐ 非常に高い | 😌 透明 |

---

## 🔍 認証方式比較

### 🔑 OAuth（オーオース）
**「他のサービスのアカウントでログインする仕組み」**

```
あなた: GitHubにプッシュしたい
Git: 認証が必要です
ブラウザ: GitHubログイン画面を表示
あなた: GitHubでログイン
GitHub: 「Git Credential Managerに権限与えてもいい？」
あなた: OK
GitHub: 認証情報をGitに渡す
Git: プッシュ完了！
```

**💡 メリット:**
- ✅ パスワードをGitに直接渡さない（安全）
- ✅ GitHubが認証を管理（楽）
- ✅ 2段階認証も自動対応

### 🏢 SSO（Single Sign-On）
**「一度ログインしたら、複数サービスが使える仕組み」**

**例：会社での使用**
```
朝: Windowsにログイン
→ 会社メール（自動ログイン）
→ GitHub Enterprise（自動ログイン）
→ Slack（自動ログイン）
→ 社内システム（自動ログイン）
```

**💡 メリット:**
- ✅ パスワード1回で済む
- ✅ パスワード忘れが減る
- ✅ セキュリティ統一管理

### 🏭 SAML（サムル）
**「企業向けの高セキュリティSSO」**

**簡単に言うと:**
- 企業が認証サーバーを管理
- 社員情報・権限を一元管理
- GitHub、AWS、Office365などが連携

**例：**
```
社員: GitHub Enterpriseにアクセス
GitHub: 「会社の認証サーバーで確認してね」
会社サーバー: 社員情報確認 → OK
GitHub: ログイン完了
```

### 🔒 SSL/TLS
**「通信を暗号化する技術」**

**見分け方:**
- `http://` → ❌ 暗号化なし（危険）
- `https://` → ✅ SSL/TLS暗号化（安全）

**Git での使用:**
```bash
# 暗号化あり（推奨）
git clone https://github.com/user/repo.git

# 暗号化なし（非推奨）
git clone http://github.com/user/repo.git
```

**💡 現在の主流:**
1. **個人・小規模**: OAuth（GitHubログイン）
2. **企業**: SSO + SAML
3. **通信**: 必ずSSL/TLS（HTTPS）

---

## パブリック vs プライベート

| 項目 | **パブリックリポジトリ** | **プライベートリポジトリ** |
|------|-------------------------|--------------------------|
| **閲覧** | 🌍 誰でも閲覧可能 | 🔒 権限者のみ |
| **クローン** | ❌ 認証不要 | ✅ 認証必要 |
| **コード検索** | 🔍 Google等で検索される | 🚫 検索されない |
| **プッシュ** | ✅ 認証必要 | ✅ 認証必要 |
| **用途** | OSS・学習・ポートフォリオ | 企業・個人の機密プロジェクト |

---

## 認証が必要な操作

### ❌ 認証不要（パブリックリポジトリ）
```bash
git clone https://github.com/user/public-repo.git
git pull origin main
git fetch origin
```

### ✅ 認証必要
```bash
# すべてのプッシュ操作
git push origin main

# プライベートリポジトリの読み取り
git clone https://github.com/user/private-repo.git
git pull origin main
```

---

## 🚀 現在の認証フロー（2024年版）

### Git + GitHub の認証フロー（実際の動作）

```
1. あなた: git push origin main
   ↓
2. Git: 認証が必要 → credential.helper manager を起動
   ↓
3. Git Credential Manager: ブラウザでGitHubログイン画面を開く
   ↓
4. ブラウザ: GitHubログイン（OAuth認証）
   ↓
5. GitHub: 「Git Credential Managerに権限を与えますか？」
   ↓
6. あなた: 「Authorize」をクリック
   ↓
7. GitHub: 認証トークンをGit Credential Managerに渡す
   ↓
8. Git Credential Manager: トークンをWindows資格情報マネージャーに保存
   ↓
9. Git: プッシュ実行完了！
```

### 💡 ここで使われている技術
- **OAuth**: GitHubでのログイン認証
- **SSL/TLS**: HTTPS通信で暗号化
- **Token-based Authentication**: パスワードの代わりにトークン使用
- **Credential Manager**: 認証情報の安全な保存

### 🔄 2回目以降の動作
```
1. あなた: git push origin main
   ↓
2. Git: credential.helper manager に認証情報を要求
   ↓
3. Git Credential Manager: Windows資格情報マネージャーから保存済みトークン取得
   ↓
4. Git: 取得したトークンでGitHubに認証
   ↓
5. GitHub: トークン確認 → 認証OK
   ↓
6. Git: プッシュ実行完了！（ブラウザ起動なし）
```

---

## credential.helper設定

### 🤔 credential.helperって何？
簡単に言うと：**「GitHubの認証を自動化してくれる便利な仕組み」**

**😩 よくある問題：**
```bash
git push origin main
# 毎回認証が必要...面倒！
```

**✨ credential.helperが解決：**
```bash
git push origin main
# 初回のみブラウザで認証 → 2回目以降は自動！
```

### 🏆 推奨：Windows Credential Manager

**🎯 設定コマンド:**
```bash
git config --global credential.helper manager
```

**💡 何をしてくれる？**
- 🌐 **ブラウザ認証** → GitHubログインで安全・簡単
- 🔐 **Windowsの資格情報マネージャー**に安全に保存
- ⏰ **永続化** → PC再起動しても認証情報が残る
- 🔧 **GUI管理** → Windowsの設定画面で確認・削除可能
- 🚀 **全プロジェクト共通** → 一度設定すれば全部で使える

### 📊 認証方法の進化

| 時期 | 方法 | 操作 | セキュリティ |
|------|------|------|-------------|
| **昔（〜2021）** | パスワード直接 | ターミナルで入力 | ❌ 危険 |
| **少し前** | Personal Access Token | ターミナルで入力 | 🟡 面倒だが安全 |
| **現在（推奨）** | ブラウザ認証 | 🌐 ブラウザで自動 | ✅ 安全で簡単！ |

### 🔍 保存場所の確認
1. Windowsスタートメニュー → 「資格情報マネージャー」と入力
2. 「Web資格情報」タブを開く
3. `git:https://github.com` という項目があればOK！

### 📊 他の方法との比較

| 方法 | 保存期間 | セキュリティ | 管理しやすさ | おすすめ度 |
|------|----------|-------------|-------------|------------|
| `manager` | ⭐ 永続的 | ⭐ 暗号化 | ⭐ GUI管理 | 🏆 **最高** |
| `cache` | ❌ 15分だけ | ❌ メモリのみ | ❌ コマンドのみ | 👎 微妙 |
| 設定なし | ❌ 保存されない | ⭐ 安全 | ❌ 毎回入力 | 😩 面倒 |

### その他の設定（参考）

**キャッシュ設定（一時的）:**
```bash
# 15分だけキャッシュ
git config --global credential.helper cache

# 1時間キャッシュ
git config --global credential.helper 'cache --timeout=3600'
```

**💭 使用ケース:**
- 共有PCで一時作業する時
- セキュリティが厳格な環境

---

## 認証設定の確認と変更

### 現在の設定確認
```bash
# 認証設定確認
git config credential.helper

# どのファイルで設定されているか確認
git config --show-origin credential.helper

# 全設定一覧（認証関連を抽出）
git config --list | grep credential
```

### 認証設定の変更
```bash
# Windows Credential Manager使用（推奨）
git config --global credential.helper manager

# 特定リポジトリでのみ設定
git config credential.helper manager

# 設定削除
git config --global --unset credential.helper
```

---

## 認証トラブルシューティング

### 🚨 よくある問題と解決策

#### 問題1: 毎回ブラウザが開く
```bash
# 原因確認
git config credential.helper

# 設定されていない場合
git config --global credential.helper manager
```

#### 問題2: Personal Access Token入力を求められる
```bash
# Git for Windowsのバージョン確認
git --version

# 2.34未満の場合、アップデート推奨
# または、強制的にmanager使用
git config --global credential.helper manager
```

#### 問題3: 認証エラーが続く
```bash
# 保存された認証情報をクリア
git config --global credential.helper ""
git config --global credential.helper manager

# Windows資格情報マネージャーから古い情報削除
# スタートメニュー → 「資格情報マネージャー」
# git:https://github.com を削除
```

#### 問題4: 企業環境での認証失敗
```bash
# プロキシ設定確認
git config --list | grep proxy

# プロキシ経由での認証設定
git config --global http.proxy http://proxy.company.com:port
git config --global https.proxy http://proxy.company.com:port
```

### 💡 クイック診断コマンド
```bash
# 現在の認証設定を一覧表示
echo "=== Git認証設定診断 ==="
echo "credential.helper: $(git config credential.helper)"
echo "user.name: $(git config user.name)"
echo "user.email: $(git config user.email)"
echo "Git version: $(git --version)"
echo ""
echo "=== テスト用リモート接続 ==="
git ls-remote https://github.com/octocat/Hello-World.git
```

---

*[戻る: メモハブ](./README-memos.md)*
