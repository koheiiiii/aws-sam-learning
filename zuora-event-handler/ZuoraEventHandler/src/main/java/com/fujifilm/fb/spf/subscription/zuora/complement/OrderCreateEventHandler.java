package com.fujifilm.fb.spf.subscription.zuora.complement;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Zuoraオーダー作成イベントを処理するハンドラー.
 */
public class OrderCreateEventHandler {
  
  private static final Logger logger = LogManager.getLogger(OrderCreateEventHandler.class);
  
  @Inject
  OrderValidator orderValidator;
  
  @Inject
  DynamoDbRepository dynamoDbRepository;
  
  @Inject
  SubscriptionIdGenerator subscriptionIdGenerator;
  
  /**
   * コンストラクタ.
   */
  @Inject
  public OrderCreateEventHandler() {
    logger.debug("OrderCreateEventHandler initialized");
  }
  
  /**
   * オーダー作成イベントを処理する.
   * 
   * @param input Lambda入力イベント
   * @param context Lambdaコンテキスト
   * @return 処理結果のレスポンス
   */
  public APIGatewayProxyResponseEvent handleOrderCreateEvent(
      APIGatewayProxyRequestEvent input, Context context) {
    
    logger.info("===== オーダー作成イベント処理開始 =====");
    
    try {
      // 1. リクエストボディからオーダーデータを取得
      String body = input.getBody();
      logger.info("Request body: {}", body);
      
      ObjectMapper mapper = new ObjectMapper();
      JsonNode orderData = mapper.readTree(body);
      
      // 2. OrderValidatorで検証
      OrderValidator.ValidationResult validationResult = orderValidator.validateOrder(orderData);
      
      // 3. switch分で分岐処理
      switch (validationResult) {
        case VALID:
          return handleValidOrder(orderData);
          
        case INVALID_FORMAT:
          logger.warn("❌ フォーマット不正のため処理を中断");
          return createErrorResponse(400, 
              "INVALID_FORMAT", 
              "オーダーデータのフォーマットが不正です");
              
        case MISSING_REQUIRED:
          logger.warn("❌ 必須項目不足のため処理を中断");
          return createErrorResponse(400, 
              "MISSING_REQUIRED", 
              "必須項目が不足しています");
              
        case DUPLICATE:
          logger.warn("❌ 重複オーダーのため処理を中断");
          return createErrorResponse(409, 
              "DUPLICATE_ORDER", 
              "既に同じオーダーが処理済みです");
              
        case UNAUTHORIZED:
          logger.warn("❌ 認証エラーのため処理を中断");
          return createErrorResponse(403, 
              "UNAUTHORIZED", 
              "認証に失敗しました");
              
        case BUSINESS_RULE_ERROR:
          logger.warn("❌ ビジネスルール違反のため処理を中断");
          return createErrorResponse(422, 
              "BUSINESS_RULE_ERROR", 
              "ビジネスルールに違反しています");
              
        default:
          logger.error("❌ 未知の検証結果: {}", validationResult);
          return createErrorResponse(500, 
              "UNKNOWN_ERROR", 
              "内部エラーが発生しました");
      }
      
    } catch (Exception e) {
      logger.error("オーダー作成イベント処理中にエラーが発生しました", e);
      return createErrorResponse(500, 
          "INTERNAL_ERROR", 
          "内部エラーが発生しました");
    }
  }
  
  /**
   * 有効なオーダーの処理.
   * 
   * @param orderData 検証済みのオーダーデータ
   * @return 成功レスポンス
   */
  private APIGatewayProxyResponseEvent handleValidOrder(JsonNode orderData) {
    logger.info("✅ 有効なオーダーの処理を開始");
    
    try {
      String orderId = orderData.get("OrderId").asText();
      
      // 1. Public Subscription ID生成
      String publicSubscriptionId = subscriptionIdGenerator.generate();
      logger.info("生成されたpublicSubscriptionId: {}", publicSubscriptionId);
      
      // 2. DynamoDBに記録
      dynamoDbRepository.putSubscriptionRecord(orderId, publicSubscriptionId);
      
      // 3. Zuora Subscription作成（実装は後回し）
      logger.info("ZuoraでのSubscription作成処理は実装待ちです");
      logger.info("設定予定のpublicSubscriptionId: {}", publicSubscriptionId);
      
      // 成功レスポンス
      String responseBody = String.format(
          "{\"status\":\"SUCCESS\",\"publicSubscriptionId\":\"%s\",\"orderId\":\"%s\"}", 
          publicSubscriptionId, orderId
      );
      
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(200)
          .withBody(responseBody);
          
    } catch (Exception e) {
      logger.error("有効なオーダー処理中にエラーが発生しました", e);
      return createErrorResponse(500, 
          "ORDER_PROCESSING_ERROR", 
          "オーダー処理中にエラーが発生しました");
    }
  }
  
  /**
   * エラーレスポンスを生成.
   * 
   * @param statusCode HTTPステータスコード
   * @param errorCode エラーコード
   * @param message エラーメッセージ
   * @return エラーレスポンス
   */
  private APIGatewayProxyResponseEvent createErrorResponse(
      int statusCode, String errorCode, String message) {
    
    String responseBody = String.format(
        "{\"status\":\"ERROR\",\"errorCode\":\"%s\",\"message\":\"%s\"}", 
        errorCode, message
    );
    
    return new APIGatewayProxyResponseEvent()
        .withStatusCode(statusCode)
        .withBody(responseBody);
  }
}
