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
  
  // Daggerコンポーネントの初期化（コンストラクタで実行）
  public ZuoraEventHandlerApp() {
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

    logger.info("===== 環境変数から取得 =====");
    String zuoraEndpoint = System.getenv("ZUORA_ENDPOINT");
    logger.info("Zuora Endpoint: {}", zuoraEndpoint);

    logger.info("===== Secrets Managerからシークレット取得 =====");
    String clientId = null;
    String clientSecret = null;
    String secretParam = System.getenv("ZUORA_API_SECRET");
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
        // ZuoraClient zuoraClient = new ZuoraClient(clientId, clientSecret, zuoraEndpoint);
        
        // OrderからSubscriptionを作成
        logger.info("===== orderIdからorderを取得 =====");
        // Order order = zuoraClient.orders().getOrder(orderId);
        // logger.info("Order retrieved: {}", order);
        
        // Subscription作成の準備
        logger.info("===== Subscription作成処理 =====");
        
        // TODO: 実際のSubscription作成処理
        // 以下は疑似コードです。実際のZuora SDK APIに合わせて修正が必要です。
        /*
        CreateSubscriptionRequest subscriptionRequest = new CreateSubscriptionRequest();
        subscriptionRequest.setAccountId(order.getAccountId());
        subscriptionRequest.setContractEffectiveDate(LocalDate.now());
        
        // カスタムフィールドにpublic_subscription_idを設定
        Map<String, Object> customFields = new HashMap<>();
        customFields.put("public_subscription_id__c", publicSubscriptionId);
        subscriptionRequest.setCustomFields(customFields);
        
        // Subscription作成実行
        Subscription subscription = zuoraClient.subscriptions().create(subscriptionRequest);
        logger.info("Subscription created: {}", subscription.getId());
        logger.info("Public Subscription ID set: {}", publicSubscriptionId);
        */
        
        logger.info("ZuoraでのSubscription作成処理は実装待ちです");
        logger.info("設定予定のpublic_subscription_id: {}", publicSubscriptionId);
        
    } catch (Exception e) {
        logger.error("Failed to create subscription in Zuora", e);
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(500)
            .withBody("{\"message\": \"Failed to create subscription in Zuora\"}");
    }
    
    // logger.info("===== orderIdからorderを取得 =====");
    // ZuoraClient zuoraClient = new ZuoraClient(clientId, clientSecret, zuoraEndpoint);
    // Order order = null;
    // try {
    //   order = zuoraClient.orders().getOrder(orderId);
    //   logger.info("Order retrieved: {}", order);
    // } catch (Exception e) {
    //   logger.error("Failed to get order from Zuora", e);
    //   return new APIGatewayProxyResponseEvent()
    //       .withStatusCode(500)
    //       .withBody("{\"message\": \"Failed to get order from Zuora\"}");
    // }

    return new APIGatewayProxyResponseEvent()
        .withStatusCode(200)
        .withBody("{\"message\": \"hello Lambda\"}");
  }
}
