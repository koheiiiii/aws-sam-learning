package com.fujifilm.fb.spf.subscription.zuora.complement;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Zuoraオーダーデータの統合検証を行うメインクラス.
 */
public class OrderValidator {
  private static final Logger logger = LogManager.getLogger(OrderValidator.class);

  /**
   * オーダー検証結果の列挙型.
   */
  public enum ValidationResult {
    VALID,              // 有効なオーダー
    INVALID_FORMAT,     // フォーマット不正
    MISSING_REQUIRED,   // 必須項目不足
    DUPLICATE,          // 重複オーダー
    UNAUTHORIZED,       // 認証エラー
    BUSINESS_RULE_ERROR // ビジネスルール違反
  }

  public OrderValidator() {
    logger.debug("OrderValidator initialized");
  }

  /**
   * オーダーデータの包括的検証.
   * 
   * @param orderData 検証対象のオーダーデータ
   * @return 検証結果
   */
  public ValidationResult validateOrder(JsonNode orderData) {
    logger.info("===== オーダー検証開始 =====");
    
    try {
      // 1. ValidationContext作成
      ValidationContext context = new ValidationContext();
      
      // 2. 基本フォーマット検証
      ValidationResult formatResult = validateFormat(orderData);
      if (formatResult != ValidationResult.VALID) {
        return formatResult;
      }

      // 3. 必須項目検証
      ValidationResult requiredResult = validateRequiredFields(orderData);
      if (requiredResult != ValidationResult.VALID) {
        return requiredResult;
      }

      // 4. 重複検証
      ValidationResult duplicateResult = validateDuplicate(orderData);
      if (duplicateResult != ValidationResult.VALID) {
        return duplicateResult;
      }

      // 5. 並列バリデータ実行
      logger.info("===== 並列バリデータ実行開始 =====");
      
      // グループ化されたバリデーション実行
      validateSubscriptionStructure(orderData, context);
      validateOrderActions(orderData, context);
      validateProductRules(orderData, context);
      
      logger.info("===== 並列バリデータ実行完了 =====");

      // 6. ValidationContextの結果を確認
      if (context.hasViolations()) {
        logger.warn("❌ ビジネスルール違反が検出されました。違反数: {}", context.getViolationCount());
        for (ValidationContext.ValidationViolation violation : context.getViolations()) {
          logger.warn("違反: {}", violation.toString());
        }
        return ValidationResult.BUSINESS_RULE_ERROR;
      }

      logger.info("✅ オーダー検証完了 - すべて有効");
      return ValidationResult.VALID;

    } catch (Exception e) {
      logger.error("オーダー検証中にエラーが発生しました", e);
      return ValidationResult.INVALID_FORMAT;
    }
  }

  /**
   * 基本フォーマット検証.
   */
  private ValidationResult validateFormat(JsonNode orderData) {
    if (orderData == null || orderData.isEmpty()) {
      logger.warn("❌ オーダーデータが空です");
      return ValidationResult.INVALID_FORMAT;
    }

    if (!orderData.isObject()) {
      logger.warn("❌ オーダーデータがJSONオブジェクト形式ではありません");
      return ValidationResult.INVALID_FORMAT;
    }

    logger.debug("✅ 基本フォーマット検証 - OK");
    return ValidationResult.VALID;
  }

  /**
   * 必須項目検証.
   */
  private ValidationResult validateRequiredFields(JsonNode orderData) {
    String[] requiredFields = {
        "OrderId", 
        "AccountId", 
        "subscriptions"
    };

    for (String field : requiredFields) {
      JsonNode fieldNode = orderData.get(field);
      if (fieldNode == null || fieldNode.isNull() || 
          (fieldNode.isTextual() && fieldNode.asText().trim().isEmpty())) {
        logger.warn("❌ 必須項目が不足しています: {}", field);
        return ValidationResult.MISSING_REQUIRED;
      }
    }

    // OrderIdの形式検証
    String orderId = orderData.get("OrderId").asText();
    if (!orderId.matches("^[a-zA-Z0-9\\-_]{1,50}$")) {
      logger.warn("❌ OrderIdの形式が不正です: {}", orderId);
      return ValidationResult.INVALID_FORMAT;
    }

    logger.debug("✅ 必須項目検証 - OK");
    return ValidationResult.VALID;
  }

  /**
   * 重複検証（簡易実装）.
   */
  private ValidationResult validateDuplicate(JsonNode orderData) {
    String orderId = orderData.get("OrderId").asText();
    
    // TODO: 実際にはDynamoDBでOrderIdの重複チェックを行う
    if (orderId.equals("DUPLICATE-ORDER-123")) {
      logger.warn("❌ 重複オーダーが検出されました: {}", orderId);
      return ValidationResult.DUPLICATE;
    }

    logger.debug("✅ 重複検証 - OK");
    return ValidationResult.VALID;
  }

  // =============================================================================
  // グループ化されたバリデーション関数
  // =============================================================================

  /**
   * サブスクリプション構造関連の検証.
   */
  private void validateSubscriptionStructure(JsonNode orderData, ValidationContext context) {
    logger.debug("===== サブスクリプション構造検証開始 =====");
    
    validateSingleSubscription(orderData, context);
    validateSchedulingOptions(orderData, context);
    validateOrderLineItems(orderData, context);
    
    logger.debug("===== サブスクリプション構造検証完了 =====");
  }

  /**
   * オーダーアクション関連の検証.
   */
  private void validateOrderActions(JsonNode orderData, ValidationContext context) {
    logger.debug("===== オーダーアクション検証開始 =====");
    
    validateSingleCreateSubscription(orderData, context);
    validateSingleCancelSubscription(orderData, context);
    validateCancellationPolicy(orderData, context);
    validateTriggerDateConsistency(orderData, context);
    
    logger.debug("===== オーダーアクション検証完了 =====");
  }

  /**
   * 商品関連ルールの検証.
   */
  private void validateProductRules(JsonNode orderData, ValidationContext context) {
    logger.debug("===== 商品ルール検証開始 =====");
    
    validateUtilityRemoveProduct(orderData, context);
    validateUtilityQuantityNotDecrease(orderData, context);
    
    logger.debug("===== 商品ルール検証完了 =====");
  }

  // =============================================================================
  // 個別検証関数
  // =============================================================================

  /**
   * 1. subscriptionsの要素が1つであることを検証.
   */
  private void validateSingleSubscription(JsonNode orderData, ValidationContext context) {
    JsonNode subscriptions = orderData.get("subscriptions");
    
    if (subscriptions == null || !subscriptions.isArray()) {
      context.addViolation("OrderValidator", "MISSING_SUBSCRIPTIONS", "subscriptions配列が見つかりません");
      return;
    }
    
    if (subscriptions.size() != 1) {
      context.addViolation("OrderValidator", "INVALID_SUBSCRIPTION_COUNT", 
        "subscriptionsの要素数は1つである必要があります。実際: " + subscriptions.size());
    }
  }

  /**
   * 2. CreateSubscriptionが1つだけ含まれることを検証.
   */
  private void validateSingleCreateSubscription(JsonNode orderData, ValidationContext context) {
    JsonNode subscription = getFirstSubscription(orderData);
    if (subscription == null) return;

    JsonNode orderActions = subscription.get("OrderActions");
    if (orderActions == null || !orderActions.isArray()) return;

    int createCount = 0;
    for (JsonNode action : orderActions) {
      String actionType = getActionType(action);
      if ("CreateSubscription".equals(actionType)) {
        createCount++;
      }
    }

    if (createCount > 1) {
      context.addViolation("OrderValidator", "MULTIPLE_CREATE_SUBSCRIPTION",
        "CreateSubscriptionは1つまでしか含められません。実際: " + createCount);
    }
  }

  /**
   * 3. CancelSubscriptionが1つだけ含まれることを検証.
   */
  private void validateSingleCancelSubscription(JsonNode orderData, ValidationContext context) {
    JsonNode subscription = getFirstSubscription(orderData);
    if (subscription == null) return;

    JsonNode orderActions = subscription.get("OrderActions");
    if (orderActions == null || !orderActions.isArray()) return;

    int cancelCount = 0;
    for (JsonNode action : orderActions) {
      String actionType = getActionType(action);
      if ("CancelSubscription".equals(actionType)) {
        cancelCount++;
      }
    }

    if (cancelCount > 1) {
      context.addViolation("OrderValidator", "MULTIPLE_CANCEL_SUBSCRIPTION",
        "CancelSubscriptionは1つまでしか含められません。実際: " + cancelCount);
    }
  }

  /**
   * 4. CancelSubscriptionがある場合のcancellationPolicy検証.
   */
  private void validateCancellationPolicy(JsonNode orderData, ValidationContext context) {
    JsonNode subscription = getFirstSubscription(orderData);
    if (subscription == null) return;

    JsonNode orderActions = subscription.get("OrderActions");
    if (orderActions == null || !orderActions.isArray()) return;

    for (JsonNode action : orderActions) {
      String actionType = getActionType(action);
      if ("CancelSubscription".equals(actionType)) {
        // CancellationPolicyがSpecificDateかチェック
        JsonNode policy = action.get("cancellationPolicy");
        if (policy == null || !"SpecificDate".equalsIgnoreCase(policy.asText())) {
          context.addViolation("OrderValidator", "INVALID_CANCELLATION_POLICY",
            "CancelSubscriptionの場合、cancellationPolicyは'SpecificDate'である必要があります");
        }

        // CancellationEffectiveDateが設定されているかチェック
        JsonNode effectiveDate = action.get("cancellationEffectiveDate");
        if (effectiveDate == null || effectiveDate.isNull() || 
            effectiveDate.asText().trim().isEmpty()) {
          context.addViolation("OrderValidator", "MISSING_CANCELLATION_EFFECTIVE_DATE",
            "cancellationPolicy='SpecificDate'の場合、cancellationEffectiveDateが必要です");
        }
      }
    }
  }

  /**
   * 5. RenewSubscriptionがない場合のTriggerDate統一性検証.
   */
  private void validateTriggerDateConsistency(JsonNode orderData, ValidationContext context) {
    JsonNode subscription = getFirstSubscription(orderData);
    if (subscription == null) return;

    JsonNode orderActions = subscription.get("OrderActions");
    if (orderActions == null || !orderActions.isArray()) return;

    // RenewSubscriptionの存在チェック
    boolean hasRenew = false;
    for (JsonNode action : orderActions) {
      if ("RenewSubscription".equals(getActionType(action))) {
        hasRenew = true;
        break;
      }
    }

    if (hasRenew) return; // RenewSubscriptionがあれば統一性チェック不要

    // TriggerDateの統一性チェック
    String firstTriggerDate = null;
    for (JsonNode action : orderActions) {
      JsonNode triggerDate = action.get("triggerDate");
      if (triggerDate != null && !triggerDate.isNull()) {
        String dateStr = triggerDate.asText().trim();
        if (!dateStr.isEmpty()) {
          if (firstTriggerDate == null) {
            firstTriggerDate = dateStr;
          } else if (!firstTriggerDate.equals(dateStr)) {
            context.addViolation("OrderValidator", "TRIGGER_DATE_INCONSISTENCY",
              "RenewSubscriptionがない場合、すべてのtriggerDateは同一である必要があります");
            break;
          }
        }
      }
    }
  }

  /**
   * 6. 利用権品目のRemoveProductが含まれていないことを検証.
   */
  private void validateUtilityRemoveProduct(JsonNode orderData, ValidationContext context) {
    JsonNode subscription = getFirstSubscription(orderData);
    if (subscription == null) return;

    JsonNode orderActions = subscription.get("OrderActions");
    if (orderActions == null || !orderActions.isArray()) return;

    for (JsonNode action : orderActions) {
      String actionType = getActionType(action);
      if ("RemoveProduct".equals(actionType) && isUtilityProduct(action)) {
        context.addViolation("OrderValidator", "UTILITY_REMOVE_PRODUCT_FORBIDDEN",
          "利用権品目のRemoveProductは許可されていません");
      }
    }
  }

  /**
   * 7. 利用権品目のUpdateProductでQuantity減少禁止検証.
   */
  private void validateUtilityQuantityNotDecrease(JsonNode orderData, ValidationContext context) {
    JsonNode subscription = getFirstSubscription(orderData);
    if (subscription == null) return;

    JsonNode orderActions = subscription.get("OrderActions");
    if (orderActions == null || !orderActions.isArray()) return;

    for (JsonNode action : orderActions) {
      String actionType = getActionType(action);
      if ("UpdateProduct".equals(actionType) && isUtilityProduct(action)) {
        JsonNode quantity = action.get("quantity");
        JsonNode originalQuantity = action.get("originalQuantity");
        
        if (quantity != null && originalQuantity != null) {
          double newQty = quantity.asDouble();
          double origQty = originalQuantity.asDouble();
          
          if (newQty < origQty) {
            context.addViolation("OrderValidator", "UTILITY_QUANTITY_DECREASE_FORBIDDEN",
              String.format("利用権品目のQuantity減少は禁止されています。%f → %f", origQty, newQty));
          }
        }
      }
    }
  }

  /**
   * 8. schedulingOptionsが設定されていないことを検証.
   */
  private void validateSchedulingOptions(JsonNode orderData, ValidationContext context) {
    JsonNode schedulingOptions = orderData.get("schedulingOptions");
    if (schedulingOptions != null && !schedulingOptions.isNull()) {
      context.addViolation("OrderValidator", "SCHEDULING_OPTIONS_FORBIDDEN",
        "schedulingOptionsは設定されていてはいけません");
    }
  }

  /**
   * 9. orderLineItemsが設定されていないことを検証.
   */
  private void validateOrderLineItems(JsonNode orderData, ValidationContext context) {
    JsonNode orderLineItems = orderData.get("orderLineItems");
    if (orderLineItems != null && !orderLineItems.isNull()) {
      context.addViolation("OrderValidator", "ORDER_LINE_ITEMS_FORBIDDEN",
        "orderLineItemsは設定されていてはいけません");
    }
  }

  // ユーティリティメソッド

  private JsonNode getFirstSubscription(JsonNode orderData) {
    JsonNode subscriptions = orderData.get("subscriptions");
    if (subscriptions != null && subscriptions.isArray() && subscriptions.size() > 0) {
      return subscriptions.get(0);
    }
    return null;
  }

  private String getActionType(JsonNode action) {
    JsonNode type = action.get("type");
    return type != null ? type.asText() : "";
  }

  private boolean isUtilityProduct(JsonNode action) {
    JsonNode productCode = action.get("productCode");
    JsonNode productName = action.get("productName");
    
    if (productCode != null) {
      String code = productCode.asText().toLowerCase();
      if (code.contains("utility") || code.contains("usage") || code.contains("right")) {
        return true;
      }
    }
    
    if (productName != null) {
      String name = productName.asText().toLowerCase();
      if (name.contains("利用権") || name.contains("usage") || name.contains("utility")) {
        return true;
      }
    }
    
    return false;
  }
}
