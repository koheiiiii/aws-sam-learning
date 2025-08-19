# GitLab CI/CD セットアップガイド

## 1. GitLab環境変数の設定

GitLabプロジェクトで以下の手順を実行してください：

1. GitLabプロジェクト > **Settings** > **CI/CD** > **Variables**
2. 以下の環境変数を追加：

### AWS_ACCESS_KEY_ID
- **Type**: Variable
- **Environments**: All
- **Flags**: ✅ Protected, ✅ Masked
- **Value**: あなたのAWSアクセスキーID

### AWS_SECRET_ACCESS_KEY  
- **Type**: Variable
- **Environments**: All
- **Flags**: ✅ Protected, ✅ Masked
- **Value**: あなたのAWSシークレットアクセスキー

## 2. AWSアクセスキーの作成

### IAM ユーザーの作成
1. AWS Console > **IAM** > **Users** > **Create user**
2. ユーザー名: `gitlab-ci-zuora-handler`
3. **Attach policies directly** を選択
4. 以下のポリシーをアタッチ：
   - `AWSCloudFormationFullAccess`
   - `AWSLambda_FullAccess` 
   - `IAMFullAccess`
   - `AmazonS3FullAccess`
   - `AmazonAPIGatewayAdministrator`

### アクセスキーの作成
1. 作成したユーザーを選択
2. **Security credentials** タブ
3. **Create access key**
4. **Application running outside AWS** を選択
5. アクセスキーIDとシークレットアクセスキーをコピー

## 3. ブランチ保護の設定

### develop ブランチ
1. GitLabプロジェクト > **Settings** > **Repository** > **Protected branches**
2. ブランチ: `develop`
3. **Allowed to merge**: Maintainers
4. **Allowed to push**: No one

### main ブランチ  
1. ブランチ: `main`
2. **Allowed to merge**: Maintainers  
3. **Allowed to push**: No one

## 4. デプロイフローの確認

### 開発フロー
```bash
# 1. フィーチャーブランチで開発
git checkout -b feature/new-feature
# ... 開発作業 ...

# 2. developにマージリクエスト
git push origin feature/new-feature
# GitLabでMerge Request作成 → develop

# 3. QA環境に自動デプロイ（developマージ時）
# Pipeline: test → build → deploy-qa

# 4. QA確認後、mainにマージリクエスト  
# GitLabでMerge Request作成 develop → main

# 5. 本番環境デプロイ（手動実行）
# Pipeline: test → build → deploy-prod (manual)
```

## 5. トラブルシューティング

### よくあるエラー

1. **AWS認証エラー**
   - 環境変数が正しく設定されているか確認
   - IAMユーザーの権限を確認

2. **S3バケットエラー**  
   - リージョンが正しいか確認（ap-northeast-1）
   - バケット名の重複確認

3. **Lambda関数エラー**
   - Java 21ランタイムの対応確認
   - メモリ・タイムアウト設定の確認

### ログの確認方法
1. GitLab > **CI/CD** > **Pipelines**
2. 失敗したジョブをクリック
3. ログを確認してエラー内容を特定

## 6. セキュリティ考慮事項

- 環境変数は必ず **Protected** と **Masked** を有効にする
- IAMユーザーは最小権限の原則に従う  
- アクセスキーは定期的にローテーションする
- 本番デプロイは必ず手動実行にする
