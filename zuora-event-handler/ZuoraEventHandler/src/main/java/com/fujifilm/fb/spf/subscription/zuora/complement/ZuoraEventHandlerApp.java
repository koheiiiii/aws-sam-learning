package com.fujifilm.fb.spf.subscription.zuora.complement;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import com.zuora.sdk.ZuoraClient;

import jakarta.inject.Inject;


/**
 * Handler for requests to Lambda function.
 */
public class ZuoraEventHandlerApp implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private static final Logger logger = LogManager.getLogger(ZuoraEventHandlerApp.class);
  
  @Inject
  SubscriptionIdGenerator subscriptionIdGenerator;
  
  @Inject
  DynamoDbRepository dynamoDbRepository;
  
  @Inject
  OrderValidator orderValidator;
  
  // Daggerコンポーネントの初期化（コンストラクタで実行）
  public ZuoraEventHandlerApp() {
    // sam local invoke環境でSSL証明書エラー回避
    SslConfig.disableSslVerification();
    
    DaggerApplicationComponent.create().inject(this);
  }
    
  public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
    logger.info("===== zuoraイベント受信 =====");
    
    String body = input.getBody();
    logger.info("Request body: {}", body);

    logger.info("===== orderIdを取得 =====");
    String orderId = null;
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode bodyNode = mapper.readTree(body);
      orderId = bodyNode.get("OrderId").asText();
    } catch (Exception e) {
      logger.error("Failed to parse OrderId from body", e);
    }
    logger.info("Order ID: {}", orderId);

    logger.info("===== オーダーバリデーション実行 =====");
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode bodyNode = mapper.readTree(body);
      OrderValidator.ValidationResult validationResult = orderValidator.validateOrder(bodyNode);
      
      if (validationResult != OrderValidator.ValidationResult.VALID) {
        logger.warn("❌ オーダーバリデーションエラー: {}", validationResult);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(400);
        response.setBody("{ \"error\": \"Order validation failed\", \"reason\": \"" + validationResult + "\" }");
        return response;
      }
      logger.info("✅ オーダーバリデーション成功");
    } catch (Exception e) {
      logger.error("オーダーバリデーション処理中にエラーが発生しました", e);
      APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
      response.setStatusCode(500);
      response.setBody("{ \"error\": \"Internal server error during validation\" }");
      return response;
    }

    logger.info("===== 環境変数から取得 =====");
    String zuoraEndpoint = System.getenv("ZUORA_ENDPOINT");
    // IDE実行時は環境変数がないため、デフォルト値を設定
    if (zuoraEndpoint == null) {
      zuoraEndpoint = "https://rest.apisandbox.zuora.com/v1/";
      logger.info("環境変数なし、デフォルト値使用");
    }
    logger.info("Zuora Endpoint: {}", zuoraEndpoint);

    logger.info("===== Secrets Managerからシークレット取得 =====");
    String clientId = null;
    String clientSecret = null;
    String secretParam = System.getenv("ZUORA_API_SECRET");
    // IDE実行時は環境変数がないため、デフォルト値を設定
    if (secretParam == null) {
      secretParam = "qa/zuora/apis";
      logger.info("環境変数なし、デフォルト値使用");
    }
    String sessionToken = System.getenv("AWS_SESSION_TOKEN");
    String endpoint = "http://localhost:2773/secretsmanager/get?secretId=" + secretParam;

    try {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("X-Aws-Parameters-Secrets-Token", sessionToken)
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String secretJson = response.body();

        logger.info("secretJson: {}", secretJson);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode secretNode = mapper.readTree(secretJson);

        String secretString = secretNode.get("SecretString").asText();
        JsonNode secretValue = mapper.readTree(secretString);
        clientId = secretValue.get("ClientID").asText();
        clientSecret = secretValue.get("ClientSecret").asText();
        logger.info("client_id: {}", clientId);
        logger.info("client_secret: {}", clientSecret);
    } catch (Exception e) {
        logger.warn("Extension経由で取得できなかったため、SDKでSecrets Managerから取得します: {}", e.getMessage());
        // SDKで直接取得する処理をここに書く
    }

    logger.info("===== DynamoDBに登録 =====");
    int maxRetry = 5;
    String publicSubscriptionId = null;

    for (int i = 0; i < maxRetry; i++) {
        logger.info("===== publicSubscriptionIdを生成 =====");
        String subscriptionId = subscriptionIdGenerator.generate();
        logger.info("publicSubscriptionId: {}", subscriptionId);
        try {
            dynamoDbRepository.putSubscriptionRecord(orderId, subscriptionId);
            // 登録成功
            publicSubscriptionId = subscriptionId;
            break;
        } catch (ConditionalCheckFailedException e) {
            // publicSubscriptionIdが重複していた場合はリトライ
            if (i == maxRetry - 1) {
                throw new IllegalStateException("publicSubscriptionIdの重複が解消できませんでした", e);
            }
            // 何もしないで次のループで再生成
        }
    }
    
    logger.info("===== ZuoraでSubscription作成 =====");
    try {
        logger.info("ZuoraでのSubscription作成処理は実装待ちです");
        logger.info("設定予定のpublic_subscription_id: {}", publicSubscriptionId);
        
    } catch (Exception e) {
        logger.error("Failed to create subscription in Zuora", e);
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(500)
            .withBody("{\"message\": \"Failed to create subscription in Zuora\"}");
    }

    return new APIGatewayProxyResponseEvent()
        .withStatusCode(200)
        .withBody("{\"message\": \"hello Lambda\"}");
    }

  /**
   * IDE実行・デバッグ用のmain関数
   * SAM Lambda環境では使用されない
   */
  public static void main(String[] args) {
    try {
      ZuoraEventHandlerApp handler = new ZuoraEventHandlerApp();
      
      // テスト用のLambdaリクエスト作成
      APIGatewayProxyRequestEvent testRequest = new APIGatewayProxyRequestEvent();
      testRequest.setBody("{\"OrderId\": \"test-order-12345\"}");
      
      // handleRequestメソッドを実行
      APIGatewayProxyResponseEvent response = handler.handleRequest(testRequest, null);
      
      // 結果表示
      logger.info("Status: {}, Body: {}", response.getStatusCode(), response.getBody());
      
    } catch (Exception e) {
      logger.error("Execution failed: {}", e.getMessage(), e);
    }
  }
}
