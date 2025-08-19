package com.fujifilm.fb.spf.subscription.zuora.complement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OrderValidator統合テストクラス.
 */
public class OrderValidatorIntegrationTest {

  private OrderValidator validator;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    validator = new OrderValidator();
    objectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("有効なオーダー - 全検証通過")
  void testValidOrder() throws Exception {
    String orderJson = """
        {
          "OrderId": "VALID-ORDER-001",
          "AccountId": "ACCOUNT-123",
          "subscriptions": [{
            "SubscriptionId": "SUB-001",
            "OrderActions": [{
              "type": "CreateSubscription",
              "triggerDate": "2025-01-01"
            }]
          }]
        }
        """;

    JsonNode orderData = objectMapper.readTree(orderJson);
    OrderValidator.ValidationResult result = validator.validateOrder(orderData);

    assertEquals(OrderValidator.ValidationResult.VALID, result);
  }

  @Test
  @DisplayName("subscriptions要素数が2の場合エラー")
  void testMultipleSubscriptionsError() throws Exception {
    String orderJson = """
        {
          "OrderId": "MULTI-SUB-001",
          "AccountId": "ACCOUNT-123",
          "subscriptions": [{
            "SubscriptionId": "SUB-001",
            "OrderActions": [{"type": "CreateSubscription"}]
          }, {
            "SubscriptionId": "SUB-002",
            "OrderActions": [{"type": "CreateSubscription"}]
          }]
        }
        """;

    JsonNode orderData = objectMapper.readTree(orderJson);
    OrderValidator.ValidationResult result = validator.validateOrder(orderData);

    assertEquals(OrderValidator.ValidationResult.BUSINESS_RULE_ERROR, result);
  }

  @Test
  @DisplayName("複数のCreateSubscriptionがある場合エラー")
  void testMultipleCreateSubscriptionError() throws Exception {
    String orderJson = """
        {
          "OrderId": "MULTI-CREATE-001",
          "AccountId": "ACCOUNT-123",
          "subscriptions": [{
            "SubscriptionId": "SUB-001",
            "OrderActions": [{
              "type": "CreateSubscription"
            }, {
              "type": "CreateSubscription"
            }]
          }]
        }
        """;

    JsonNode orderData = objectMapper.readTree(orderJson);
    OrderValidator.ValidationResult result = validator.validateOrder(orderData);

    assertEquals(OrderValidator.ValidationResult.BUSINESS_RULE_ERROR, result);
  }

  @Test
  @DisplayName("CancelSubscriptionのcancellationPolicyが不正な場合エラー")
  void testInvalidCancellationPolicyError() throws Exception {
    String orderJson = """
        {
          "OrderId": "INVALID-CANCEL-001",
          "AccountId": "ACCOUNT-123",
          "subscriptions": [{
            "SubscriptionId": "SUB-001",
            "OrderActions": [{
              "type": "CancelSubscription",
              "cancellationPolicy": "EndOfTerm"
            }]
          }]
        }
        """;

    JsonNode orderData = objectMapper.readTree(orderJson);
    OrderValidator.ValidationResult result = validator.validateOrder(orderData);

    assertEquals(OrderValidator.ValidationResult.BUSINESS_RULE_ERROR, result);
  }

  @Test
  @DisplayName("TriggerDateが不統一の場合エラー（RenewSubscriptionなし）")
  void testTriggerDateInconsistencyError() throws Exception {
    String orderJson = """
        {
          "OrderId": "INCONSISTENT-DATE-001",
          "AccountId": "ACCOUNT-123",
          "subscriptions": [{
            "SubscriptionId": "SUB-001",
            "OrderActions": [{
              "type": "CreateSubscription",
              "triggerDate": "2025-01-01"
            }, {
              "type": "UpdateProduct",
              "triggerDate": "2025-01-15"
            }]
          }]
        }
        """;

    JsonNode orderData = objectMapper.readTree(orderJson);
    OrderValidator.ValidationResult result = validator.validateOrder(orderData);

    assertEquals(OrderValidator.ValidationResult.BUSINESS_RULE_ERROR, result);
  }

  @Test
  @DisplayName("利用権品目のRemoveProductがある場合エラー")
  void testUtilityRemoveProductError() throws Exception {
    String orderJson = """
        {
          "OrderId": "UTILITY-REMOVE-001",
          "AccountId": "ACCOUNT-123",
          "subscriptions": [{
            "SubscriptionId": "SUB-001",
            "OrderActions": [{
              "type": "RemoveProduct",
              "productCode": "UTILITY-001",
              "productName": "利用権サービス"
            }]
          }]
        }
        """;

    JsonNode orderData = objectMapper.readTree(orderJson);
    OrderValidator.ValidationResult result = validator.validateOrder(orderData);

    assertEquals(OrderValidator.ValidationResult.BUSINESS_RULE_ERROR, result);
  }

  @Test
  @DisplayName("schedulingOptionsが設定されている場合エラー")
  void testSchedulingOptionsError() throws Exception {
    String orderJson = """
        {
          "OrderId": "SCHEDULING-001",
          "AccountId": "ACCOUNT-123",
          "subscriptions": [{
            "SubscriptionId": "SUB-001",
            "OrderActions": [{"type": "CreateSubscription"}]
          }],
          "schedulingOptions": {
            "scheduleType": "Immediate"
          }
        }
        """;

    JsonNode orderData = objectMapper.readTree(orderJson);
    OrderValidator.ValidationResult result = validator.validateOrder(orderData);

    assertEquals(OrderValidator.ValidationResult.BUSINESS_RULE_ERROR, result);
  }

  @Test
  @DisplayName("orderLineItemsが設定されている場合エラー")
  void testOrderLineItemsError() throws Exception {
    String orderJson = """
        {
          "OrderId": "ORDER-LINE-001",
          "AccountId": "ACCOUNT-123",
          "subscriptions": [{
            "SubscriptionId": "SUB-001",
            "OrderActions": [{"type": "CreateSubscription"}]
          }],
          "orderLineItems": [{
            "itemId": "ITEM-001"
          }]
        }
        """;

    JsonNode orderData = objectMapper.readTree(orderJson);
    OrderValidator.ValidationResult result = validator.validateOrder(orderData);

    assertEquals(OrderValidator.ValidationResult.BUSINESS_RULE_ERROR, result);
  }

  @Test
  @DisplayName("RenewSubscriptionがある場合はTriggerDate不統一でもOK")
  void testTriggerDateWithRenewSubscriptionOK() throws Exception {
    String orderJson = """
        {
          "OrderId": "RENEW-TRIGGER-001",
          "AccountId": "ACCOUNT-123",
          "subscriptions": [{
            "SubscriptionId": "SUB-001",
            "OrderActions": [{
              "type": "RenewSubscription",
              "triggerDate": "2025-01-01"
            }, {
              "type": "UpdateProduct",
              "triggerDate": "2025-01-15"
            }]
          }]
        }
        """;

    JsonNode orderData = objectMapper.readTree(orderJson);
    OrderValidator.ValidationResult result = validator.validateOrder(orderData);

    assertEquals(OrderValidator.ValidationResult.VALID, result);
  }
}
