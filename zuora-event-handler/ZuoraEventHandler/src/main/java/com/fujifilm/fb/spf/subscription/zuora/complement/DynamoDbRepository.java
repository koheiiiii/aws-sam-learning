package com.fujifilm.fb.spf.subscription.zuora.complement;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * DynamoDBのサブスクリプション情報を操作するリポジトリクラスです。
 * <p>
 * サブスクリプション情報の登録や取得など、DynamoDBへの操作をまとめて管理します。
 * </p>
 */
public class DynamoDbRepository {
    private final DynamoDbClient ddb;
    private final String tableName;

    /**
     * 指定したテーブル名でDynamoDBリポジトリを生成します。
     *
     * @param tableName 操作対象のDynamoDBテーブル名
     */
    public DynamoDbRepository(String tableName) {
        this.ddb = DynamoDbClient.create();
        this.tableName = tableName;
    }

    // ★ テスト用コンストラクタ
    public DynamoDbRepository(DynamoDbClient ddb, String tableName) {
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
        item.put("orderId", AttributeValue.builder().s(orderId).build());
        item.put("publicSubscriptionId", AttributeValue.builder().s(publicSubscriptionId).build());
        item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());
        PutItemRequest request = PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            .build();

        ddb.putItem(request);
    }
}
