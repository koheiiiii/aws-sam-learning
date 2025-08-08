package com.fujifilm.fb.spf.subscription.zuora.complement;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import jakarta.inject.Singleton;

/**
 * AWS SDKクライアントの依存性注入を提供するDaggerモジュールです。
 * <p>
 * DynamoDBクライアントを適切なリージョン設定で提供します。
 * 本番環境ではLambda実行リージョンが自動的に使用されます。
 * </p>
 */
@Module
public class AwsClientModule {
    
    /**
     * DynamoDBクライアントを提供します。
     * <p>
     * リージョンはAWS_REGION環境変数または AWS SDK のデフォルト設定が使用されます。
     * Lambda環境では実行リージョンが自動的に設定されます。
     * </p>
     * 
     * @return DynamoDBクライアントのシングルトンインスタンス
     */
    @Provides
    @Singleton
    DynamoDbClient provideDynamoDbClient() {
        return DynamoDbClient.builder()
                .region(getAwsRegion())
                .build();
    }
    
    /**
     * AWS リージョンを取得します。
     * <p>
     * 1. AWS_REGION 環境変数
     * 2. AWS SDK のデフォルト設定
     * 3. フォールバック: ap-northeast-1 (東京)
     * </p>
     * 
     * @return 使用するAWSリージョン
     */
    private Region getAwsRegion() {
        String regionEnv = System.getenv("AWS_REGION");
        if (regionEnv != null && !regionEnv.isEmpty()) {
            return Region.of(regionEnv);
        }
        
        // AWS SDK のデフォルトリージョン解決を試行
        try {
            return Region.of(System.getProperty("aws.region", "ap-northeast-1"));
        } catch (Exception e) {
            // フォールバック: 東京リージョン
            return Region.AP_NORTHEAST_1;
        }
    }
}
