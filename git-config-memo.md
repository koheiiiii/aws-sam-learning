# 🔧 Git基本設定メモ

**Git設定の優先順位とファイル配置を理解する実用ガイド**

## 📚 目次

- [リポジトリ設定](#リポジトリ設定)
- [git configコマンド](#git-configコマンド)
  - [基本概念](#基本概念)
  - [優先順位の仕組み](#優先順位の仕組み)
  - [設定の確認方法](#設定の確認方法)
  - [実際の使い分けシナリオ](#実際の使い分けシナリオ)

---

## リポジトリ設定

### リモートリポジトリの追加
```bash
# 複数のリモートリポジトリを追加可能
git remote add origin https://github.com/user/repo.git
git remote add gitlab https://gitlab.com/user/repo.git

# リモート確認
git remote -v

# 特定のリモートにプッシュ
git push origin main
git push gitlab main
```

---

## git configコマンド

### 基本概念

**🤔 git configって何？**

**「Gitの設定を変更するコマンド」** → あなたの名前、メール、認証方法などを設定

**😎 よく使う設定例:**
```bash
# あなたの名前を設定（コミットに表示される）
git config --global user.name "山田太郎"

# メールアドレスを設定
git config --global user.email "yamada@example.com"

# 認証設定（パスワード自動保存）
git config --global credential.helper manager
```

**🎯 主な用途:**
- ✅ **ユーザー名・メールアドレス設定** ← 超重要！
- ✅ **認証設定（credential.helper）** ← パスワード面倒を解決！
- ✅ **リポジトリ固有の設定** ← プロジェクトごとの個別設定

### 優先順位の仕組み

**🔄 どこに保存される？優先順位は？**

**簡単に言うと：「より具体的な設定が勝つ」**

| 優先順位 | コマンド | どこに保存？ | いつ使う？ | 例 |
|---------|----------|-------------|-----------|-----|
| 🥇 **1位** | `git config` | **このプロジェクト**<br>`.git/config` | 🏢 会社プロジェクトだけ別設定 | 会社用メール |
| 🥈 **2位** | `git config --global` | **あなたの全プロジェクト**<br>`~/.gitconfig` | 🏠 **普段はこれ使う！** | 個人用メール |
| 🥉 **3位** | `git config --system` | **PC全体**<br>`/etc/gitconfig` | ⚙️ Git初期設定 | 自動設定 |

**💡 実例で理解しよう：**

```bash
# 【普段の設定】すべてのプロジェクトで使う
git config --global user.email "personal@gmail.com"
git config --global user.name "山田太郎" 

# 【会社プロジェクト】このプロジェクトだけ会社メール
cd /work/company-project
git config user.email "yamada@company.com"  # ← --globalなし

# 結果：
# 会社プロジェクト → yamada@company.com を使用（優先順位1位）
# その他プロジェクト → personal@gmail.com を使用（優先順位2位）
```

**🔧 どのファイルに書き込まれているか確認：**

```bash
# どこで設定されているか一目瞭然
git config --show-origin user.name
git config --show-origin user.email
git config --show-origin credential.helper
```

**🔍 設定ファイルを直接見る:**
```bash
# あなたの個人設定ファイル
notepad ~/.gitconfig
# または
cat ~/.gitconfig

# このプロジェクトの設定ファイル
notepad .git/config
# または
cat .git/config
```

### 設定の確認方法

**🔎 設定の確認方法（超便利！）:**

**📝 今の設定を確認したい時：**
```bash
# 現在の設定値をサクッと確認
git config user.name      # → 山田太郎
git config user.email     # → yamada@example.com  
git config credential.helper  # → manager
```

**🔍 どこで設定されているか知りたい時：**
```bash
# どのファイルの設定が使われているか表示
git config --show-origin user.name
# → file:C:/Users/ユーザー名/.gitconfig  山田太郎

git config --show-origin user.email  
# → file:.git/config  yamada@company.com  ← このプロジェクトだけ会社メール

# 全部まとめて確認
git config --list --show-origin
```

**📋 実際の出力例：**
```
file:C:/Program Files/Git/etc/gitconfig    credential.helper=manager
file:C:/Users/ユーザー名/.gitconfig          user.email=personal@gmail.com
file:C:/Users/ユーザー名/.gitconfig          user.name=山田太郎
file:.git/config                          user.email=yamada@company.com
file:.git/config                          remote.origin.url=https://github.com/...
```

**💡 この例から読み取れること：**
- 📁 **システム設定**: `credential.helper=manager` が全体で有効
- 🏠 **個人設定**: 普段は `personal@gmail.com` と `山田太郎` を使用
- 🏢 **このプロジェクト**: メールだけ `yamada@company.com` に上書き
- ✅ **最終的に使われる設定**: `yamada@company.com` + `山田太郎` + `manager`

**🎯 よく使うコマンドまとめ:**
```bash
# 今の設定値をサクッと確認
git config user.name
git config user.email
git config credential.helper

# どこで設定されているか確認
git config --show-origin user.name
git config --show-origin credential.helper
```

### 実際の使い分けシナリオ

**💡 実際の使い分けシナリオ:**

**📝 シナリオ1: 初回セットアップ**
```bash
# 最初に一回だけ設定（全プロジェクト共通）
git config --global user.name "山田太郎"
git config --global user.email "yamada.personal@gmail.com"
git config --global credential.helper manager

# 確認
git config --list --global
```

**📝 シナリオ2: 会社プロジェクトで別メール使いたい**
```bash
# 会社のプロジェクトフォルダに移動
cd C:\work\company-project

# このプロジェクトだけ会社のメールを使用
git config user.email "yamada@company.co.jp"
git config user.name "山田太郎（会社）"

# 確認 → 会社メールが優先される
git config user.email
# → yamada@company.co.jp

# 他のプロジェクトは個人メールのまま
cd C:\personal\my-project
git config user.email
# → yamada.personal@gmail.com
```

**📝 シナリオ3: 設定がおかしくなった時のリセット**
```bash
# 特定の設定を削除
git config --global --unset user.email
git config --unset user.email  # このプロジェクトのみ

# 設定し直し
git config --global user.email "yamada@example.com"
```

---

## 💡 まとめ

**🎯 重要ポイント:**
1. **優先順位**: プロジェクト設定 > ユーザー設定 > システム設定
2. **よく使うのは**: `git config --global`（ユーザー設定）
3. **確認方法**: `git config --show-origin`で設定元を特定

**🚀 次に読むべきメモ:**
- [Git認証メモ](./git-auth-memo.md) ← credential.helperの詳細
- [ネットワークメモ](./network-memo.md) ← プロキシでのgit設定

**📝 追記・改善:**
- 新しい設定項目を発見したら追加
- 実際の使用で困ったパターンがあれば追記
- より良いワークフローがあれば更新
