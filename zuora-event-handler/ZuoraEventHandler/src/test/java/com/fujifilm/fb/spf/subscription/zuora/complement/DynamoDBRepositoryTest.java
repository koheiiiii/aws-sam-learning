package com.fujifilm.fb.spf.subscription.zuora.complement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

class DynamoDbRepositoryTest {
    // DynamoDbClientのモックを使用して、DynamoDbRepositoryのテストを行います
    private DynamoDbClient mockClient;
    private DynamoDbRepository repository;

    @BeforeEach
    void setUp() {
        mockClient = mock(DynamoDbClient.class);
        // テスト用にDynamoDbClientを差し替えられるように、DynamoDbRepositoryに追加コンストラクタが必要です
        repository = new DynamoDbRepository(mockClient, "test-table");
    }

    @Test
    @DisplayName("正常系: 正しい値で登録できる")
    void testPutSubscriptionRecordSuccess() {
        assertDoesNotThrow(() -> {
            repository.putSubscriptionRecord("order-1234", "1234-5678-9012");
        });
        // putItemが呼ばれていることを確認
        verify(mockClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("異常系: orderIdがnullの場合は例外")
    void testPutSubscriptionRecordOrderIdNull() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
            repository.putSubscriptionRecord(null, "1234-5678-9012")
        );
        assertEquals("orderId is required", e.getMessage());
    }

    @Test
    @DisplayName("異常系: publicSubscriptionIdがnullの場合は例外")
    void testPutSubscriptionRecordPublicSubscriptionIdNull() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
            repository.putSubscriptionRecord("order-1234", null)
        );
        assertEquals("publicSubscriptionId is required", e.getMessage());
    }

    @Test
    @DisplayName("異常系: publicSubscriptionIdが空文字の場合は例外")
    void testPutSubscriptionRecordPublicSubscriptionIdEmpty() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
            repository.putSubscriptionRecord("order-1234", "")
        );
        assertEquals("publicSubscriptionId is required", e.getMessage());
    }

    @Test
    @DisplayName("異常系: publicSubscriptionIdが不正な形式の場合は例外")
    void testPutSubscriptionRecordPublicSubscriptionIdInvalidFormat() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
            repository.putSubscriptionRecord("order-1234", "abcd-efgh-ijkl")
        );
        assertEquals("publicSubscriptionId must be in nnnn-nnnn-nnnn format", e.getMessage());
    }

    @Test
    @DisplayName("異常系: 重複するpublicSubscriptionIdの場合はConditionalCheckFailedException")
    void testPutSubscriptionRecordDuplicatePublicSubscriptionId() {
        // Mockで ConditionalCheckFailedException を発生させる
        when(mockClient.putItem(any(PutItemRequest.class)))
            .thenThrow(ConditionalCheckFailedException.builder()
                .message("Conditional check failed")
                .build());

        // ConditionalCheckFailedException が発生することを確認
        assertThrows(ConditionalCheckFailedException.class, () ->
            repository.putSubscriptionRecord("order-1234", "1234-5678-9012")
        );
        
        // putItemが呼ばれていることを確認
        verify(mockClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("正常系: conditionExpressionが正しく設定されている")
    void testPutSubscriptionRecordConditionExpression() {
        // putItemRequestをキャプチャして条件式を確認
        ArgumentCaptor<PutItemRequest> requestCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
        
        repository.putSubscriptionRecord("order-1234", "1234-5678-9012");
        
        verify(mockClient).putItem(requestCaptor.capture());
        PutItemRequest capturedRequest = requestCaptor.getValue();
        
        // conditionExpression が正しく設定されていることを確認
        assertEquals("attribute_not_exists(public_subscription_id)", 
                    capturedRequest.conditionExpression());
    }
}