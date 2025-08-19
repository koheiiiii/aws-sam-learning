package com.fujifilm.fb.spf.subscription.zuora.complement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * OrderCreateEventHandlerのテストクラス.
 */
class OrderCreateEventHandlerTest {

  @Mock
  private OrderValidator mockOrderValidator;
  
  @Mock
  private DynamoDbRepository mockDynamoDbRepository;
  
  @Mock
  private SubscriptionIdGenerator mockSubscriptionIdGenerator;
  
  @Mock
  private Context mockContext;
  
  private OrderCreateEventHandler handler;
  
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new OrderCreateEventHandler();
    
    // プライベートフィールドにモックを注入（リフレクション使用）
    try {
      var orderValidatorField = OrderCreateEventHandler.class.getDeclaredField("orderValidator");
      orderValidatorField.setAccessible(true);
      orderValidatorField.set(handler, mockOrderValidator);
      
      var dynamoDbRepositoryField = OrderCreateEventHandler.class.getDeclaredField("dynamoDbRepository");
      dynamoDbRepositoryField.setAccessible(true);
      dynamoDbRepositoryField.set(handler, mockDynamoDbRepository);
      
      var subscriptionIdGeneratorField = OrderCreateEventHandler.class.getDeclaredField("subscriptionIdGenerator");
      subscriptionIdGeneratorField.setAccessible(true);
      subscriptionIdGeneratorField.set(handler, mockSubscriptionIdGenerator);
      
    } catch (Exception e) {
      throw new RuntimeException("モック注入に失敗しました", e);
    }
  }
  
  @Test
  @DisplayName("正常系: 有効なオーダーデータで成功レスポンスが返る")
  void testHandleOrderCreateEvent_ValidOrder_ReturnsSuccess() {
    // Given
    String validOrderJson = """
        {
          "OrderId": "ORDER-12345",
          "AccountId": "ACC-67890",
          "SubscriptionData": {
            "PlanCode": "BASIC_PLAN"
          },
          "BillingAccount": {
            "Name": "Test Company"
          }
        }
        """;
    
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setBody(validOrderJson);
    
    when(mockOrderValidator.validateOrder(any())).thenReturn(OrderValidator.ValidationResult.VALID);
    when(mockSubscriptionIdGenerator.generate()).thenReturn("1234-5678-9012");
    
    // When
    APIGatewayProxyResponseEvent response = handler.handleOrderCreateEvent(request, mockContext);
    
    // Then
    assertEquals(200, response.getStatusCode().intValue());
    assertTrue(response.getBody().contains("SUCCESS"));
    assertTrue(response.getBody().contains("1234-5678-9012"));
    assertTrue(response.getBody().contains("ORDER-12345"));
    
    // DynamoDBへの登録が呼ばれたことを確認
    verify(mockDynamoDbRepository).putSubscriptionRecord("ORDER-12345", "1234-5678-9012");
  }
  
  @Test
  @DisplayName("異常系: フォーマット不正で400エラーが返る")
  void testHandleOrderCreateEvent_InvalidFormat_Returns400() {
    // Given
    String invalidOrderJson = """
        {
          "InvalidField": "test"
        }
        """;
    
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setBody(invalidOrderJson);
    
    when(mockOrderValidator.validateOrder(any())).thenReturn(OrderValidator.ValidationResult.INVALID_FORMAT);
    
    // When
    APIGatewayProxyResponseEvent response = handler.handleOrderCreateEvent(request, mockContext);
    
    // Then
    assertEquals(400, response.getStatusCode().intValue());
    assertTrue(response.getBody().contains("ERROR"));
    assertTrue(response.getBody().contains("INVALID_FORMAT"));
    
    // DynamoDBへの登録は呼ばれないことを確認
    verify(mockDynamoDbRepository, never()).putSubscriptionRecord(any(), any());
  }
  
  @Test
  @DisplayName("異常系: 必須項目不足で400エラーが返る")
  void testHandleOrderCreateEvent_MissingRequired_Returns400() {
    // Given
    String incompleteOrderJson = """
        {
          "OrderId": "ORDER-12345"
        }
        """;
    
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setBody(incompleteOrderJson);
    
    when(mockOrderValidator.validateOrder(any())).thenReturn(OrderValidator.ValidationResult.MISSING_REQUIRED);
    
    // When
    APIGatewayProxyResponseEvent response = handler.handleOrderCreateEvent(request, mockContext);
    
    // Then
    assertEquals(400, response.getStatusCode().intValue());
    assertTrue(response.getBody().contains("ERROR"));
    assertTrue(response.getBody().contains("MISSING_REQUIRED"));
    
    verify(mockDynamoDbRepository, never()).putSubscriptionRecord(any(), any());
  }
  
  @Test
  @DisplayName("異常系: 重複オーダーで409エラーが返る")
  void testHandleOrderCreateEvent_DuplicateOrder_Returns409() {
    // Given
    String duplicateOrderJson = """
        {
          "OrderId": "DUPLICATE-ORDER-123",
          "AccountId": "ACC-67890",
          "SubscriptionData": {
            "PlanCode": "BASIC_PLAN"
          },
          "BillingAccount": {
            "Name": "Test Company"
          }
        }
        """;
    
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setBody(duplicateOrderJson);
    
    when(mockOrderValidator.validateOrder(any())).thenReturn(OrderValidator.ValidationResult.DUPLICATE);
    
    // When
    APIGatewayProxyResponseEvent response = handler.handleOrderCreateEvent(request, mockContext);
    
    // Then
    assertEquals(409, response.getStatusCode().intValue());
    assertTrue(response.getBody().contains("ERROR"));
    assertTrue(response.getBody().contains("DUPLICATE_ORDER"));
    
    verify(mockDynamoDbRepository, never()).putSubscriptionRecord(any(), any());
  }
  
  @Test
  @DisplayName("異常系: 認証エラーで403エラーが返る")
  void testHandleOrderCreateEvent_Unauthorized_Returns403() {
    // Given
    String unauthorizedOrderJson = """
        {
          "OrderId": "ORDER-12345",
          "AccountId": "SUSPENDED_ACC-67890",
          "SubscriptionData": {
            "PlanCode": "BASIC_PLAN"
          },
          "BillingAccount": {
            "Name": "Test Company"
          }
        }
        """;
    
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setBody(unauthorizedOrderJson);
    
    when(mockOrderValidator.validateOrder(any())).thenReturn(OrderValidator.ValidationResult.UNAUTHORIZED);
    
    // When
    APIGatewayProxyResponseEvent response = handler.handleOrderCreateEvent(request, mockContext);
    
    // Then
    assertEquals(403, response.getStatusCode().intValue());
    assertTrue(response.getBody().contains("ERROR"));
    assertTrue(response.getBody().contains("UNAUTHORIZED"));
    
    verify(mockDynamoDbRepository, never()).putSubscriptionRecord(any(), any());
  }
  
  @Test
  @DisplayName("異常系: ビジネスルール違反で422エラーが返る")
  void testHandleOrderCreateEvent_BusinessRuleError_Returns422() {
    // Given
    String businessRuleErrorOrderJson = """
        {
          "OrderId": "ORDER-12345",
          "AccountId": "ACC-67890",
          "SubscriptionData": {
            "PlanCode": "INVALID_PREMIUM_PLAN"
          },
          "BillingAccount": {
            "Name": "Test Company"
          }
        }
        """;
    
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setBody(businessRuleErrorOrderJson);
    
    when(mockOrderValidator.validateOrder(any())).thenReturn(OrderValidator.ValidationResult.BUSINESS_RULE_ERROR);
    
    // When
    APIGatewayProxyResponseEvent response = handler.handleOrderCreateEvent(request, mockContext);
    
    // Then
    assertEquals(422, response.getStatusCode().intValue());
    assertTrue(response.getBody().contains("ERROR"));
    assertTrue(response.getBody().contains("BUSINESS_RULE_ERROR"));
    
    verify(mockDynamoDbRepository, never()).putSubscriptionRecord(any(), any());
  }
  
  @Test
  @DisplayName("異常系: JSON解析エラーで500エラーが返る")
  void testHandleOrderCreateEvent_JsonParseError_Returns500() {
    // Given
    String invalidJson = "{ invalid json }";
    
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    request.setBody(invalidJson);
    
    // When
    APIGatewayProxyResponseEvent response = handler.handleOrderCreateEvent(request, mockContext);
    
    // Then
    assertEquals(500, response.getStatusCode().intValue());
    assertTrue(response.getBody().contains("ERROR"));
    assertTrue(response.getBody().contains("INTERNAL_ERROR"));
    
    // バリデーターもDynamoDBも呼ばれないことを確認
    verify(mockOrderValidator, never()).validateOrder(any());
    verify(mockDynamoDbRepository, never()).putSubscriptionRecord(any(), any());
  }
}
