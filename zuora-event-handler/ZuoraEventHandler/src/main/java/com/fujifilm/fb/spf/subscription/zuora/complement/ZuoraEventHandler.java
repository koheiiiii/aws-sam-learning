package com.fujifilm.fb.spf.subscription.zuora.complement;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openapitools.client.model.Order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuora.sdk.ZuoraClient;


/**
 * Handler for requests to Lambda function.
 */
public class ZuoraEventHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private static final Logger logger = LogManager.getLogger(ZuoraEventHandler.class);
    
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
    String clientId = System.getenv("ZUORA_CLIENT_ID");
    String zuoraEndpoint = System.getenv("ZUORA_ENDPOINT");
    logger.info("Client ID: {}", clientId);
    logger.info("Zuora Endpoint: {}", zuoraEndpoint);

    logger.info("===== SSM Parameter Storeからシークレット取得 =====");
    SsmClient ssmClient = SsmClient.create();
    GetParameterRequest paramRequest = GetParameterRequest.builder()
        .name(System.getenv("ZUORA_CLIENT_SECRET_PARAM"))
        .withDecryption(true)
        .build();
    GetParameterResponse paramResponse = ssmClient.getParameter(paramRequest);
    String clientSecret = paramResponse.parameter().value();
    logger.info("Client Secret: {}", clientSecret);

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
