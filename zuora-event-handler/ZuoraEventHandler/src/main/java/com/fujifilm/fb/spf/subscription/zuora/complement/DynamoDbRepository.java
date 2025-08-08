package com.fujifilm.fb.spf.subscription.zuora.complement;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * DynamoDBのサブスクリプション情報を操作するリポジトリクラスです。
 * <p>
 * サブスクリプション情報の登録や取得など、DynamoDBへの操作をまとめて管理します。
 * </p>
 */
@Singleton
public class DynamoDbRepository {
    private final DynamoDbClient ddb;
    private final String tableName;

    /**
     * 依存性注入によりDynamoDBクライアントを受け取るコンストラクタです。
     *
     * @param ddb DynamoDBクライアント（Daggerにより注入）
     */
    @Inject
    DynamoDbRepository(DynamoDbClient ddb) {
        this.ddb = ddb;
        // テーブル名は環境変数から取得（Lambda環境での標準的なパターン）
        String envTableName = System.getenv("DYNAMODB_TABLE_NAME");
        this.tableName = (envTableName != null && !envTableName.isEmpty()) 
                ? envTableName 
                : "subscription-zuora-order-handler"; // フォールバック値（開発・テスト用）
    }

    /**
     * テスト専用コンストラクタ - テーブル名を明示的に指定可能
     * <p>
     * 本番コードでは使用しないでください。
     * 単体テストでのMock注入専用です。
     * </p>
     * 
     * @param ddb DynamoDBクライアント
     * @param tableName テーブル名
     */
    DynamoDbRepository(DynamoDbClient ddb, String tableName) {
        this.ddb = ddb;
        this.tableName = tableName;
    }

    /**
     * サブスクリプション情報（orderId, publicSubscriptionId）をDynamoDBに登録します。
     * publicSubscriptionIdは「nnnn-nnnn-nnnn」形式である必要があります。
     *
     * @param orderId サブスクリプションに紐づく注文ID
     * @param publicSubscriptionId サブスクリプションID（nnnn-nnnn-nnnn形式）
     * @throws IllegalArgumentException 引数がnull/空、または形式不正の場合
     */
    public void putSubscriptionRecord(String orderId, String publicSubscriptionId) {
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (publicSubscriptionId == null || publicSubscriptionId.isEmpty()) {
            throw new IllegalArgumentException("publicSubscriptionId is required");
        }
        if (!publicSubscriptionId.matches("\\d{4}-\\d{4}-\\d{4}")) {
            throw new IllegalArgumentException("publicSubscriptionId must be in nnnn-nnnn-nnnn format");
        }

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("order_id", AttributeValue.builder().s(orderId).build());
        item.put("public_subscription_id", AttributeValue.builder().s(publicSubscriptionId).build());
        item.put("created_at", AttributeValue.builder().s(DateTimeFormatter.ISO_INSTANT.format(Instant.now())).build());
        PutItemRequest request = PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            // public_subscription_idが未登録（属性が存在しない）場合のみ登録
            .conditionExpression("attribute_not_exists(public_subscription_id)")
            .build();

        ddb.putItem(request);
    }
}
