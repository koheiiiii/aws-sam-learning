public class sample2 {
    package com.fujifilm.fb.spf.subscription.zuora.complement;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import org.mockito.Mockito;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import java.util.Arrays;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fujifilm.fb.spf.subscription.zuora.complement.AbisApiClient.ArrangementOrderRequest;
import com.fujifilm.fb.spf.subscription.zuora.complement.AbisApiClient.ArrangementResponse;
import com.fujifilm.fb.spf.subscription.zuora.complement.AbisApiClient.ValidationNotificationRequest;
import com.fujifilm.fb.spf.subscription.zuora.complement.OrderValidator.Result;
import com.fujifilm.fb.spf.subscription.zuora.complement.provider.SubscriptionProvider;
import com.zuora.ApiException;
import com.zuora.JSON;
import com.zuora.ZuoraClient;
import com.zuora.api.ContactsApi;
import com.zuora.api.ContactsApi.GetContactApi;
import com.zuora.api.OrdersApi;
import com.zuora.api.OrdersApi.CreateOrderApi;
import com.zuora.api.OrdersApi.DeleteOrderApi;
import com.zuora.api.OrdersApi.GetOrderApi;
import com.zuora.api.OrdersApi.UpdateOrderCustomFieldsApi;
import com.zuora.api.OrdersApi.UpdateSubscriptionCustomFieldsApi;
import com.zuora.model.ContactResponse;
import com.zuora.model.CreateOrderRequest;
import com.zuora.model.CreateOrderResponse;
import com.zuora.model.GetOrderResponse;
import com.zuora.model.Order;
import com.zuora.model.OrderAction;
import com.zuora.model.OrderActionRemoveProduct;
import com.zuora.model.OrderActionType;
import com.zuora.model.OrderStatus;
import com.zuora.model.OrderSubscriptions;
import com.zuora.model.UpdateOrderCustomFieldsRequest;
import com.zuora.model.UpdateSubscriptionCustomFieldsRequest;
import com.zuora.model.GetSubscriptionRatePlanChargesWithAllSegments;

import dagger.Lazy;
import jakarta.inject.Singleton;

/**
 * {@link OrderCreatedEventHandler}のテストクラス
 */
public class OrderCreatedEventHandlerTest {

    private static final String PROP_NAME_ORGANIZATION_LABEL = "organizationLabel";

    @dagger.Component(modules = { EnvironmentsModule.class, JavaNetModule.class, ZuoraModule.class,
            AWSClientModule.class,
            JsonModule.class })
    @Singleton
    interface Component {

        OrderCreatedEventHandler handler();

        ObjectMapper objectMapper();
    }

    @Mock
    private Lazy<ZuoraClient> zuoraClient;

    @Mock
    private ZuoraClient mockZuoraClient;

    @Mock
    private OrdersApi ordersApi;

    @Mock
    private ContactsApi contactsApi;

    @Mock
    private GetOrderApi getOrderApi;

    @Mock
    private CreateOrderApi createOrderApi;

    @Mock
    private DeleteOrderApi deleteOrderApi;

    @Mock
    private GetContactApi getContactApi;

    @Mock
    private UpdateSubscriptionCustomFieldsApi updateSubscriptionCustomFieldsApi;

    @Mock
    private UpdateOrderCustomFieldsApi updateOrderCustomFieldsApi;

    @Mock
    private OrderValidator orderValidator;

    @Mock
    private CorrectedOrderBuilder correctedOrderBuilder;

    @Mock
    private SubscriptionIdGenerator subscriptionIdGenerator;

    @Mock
    private DynamoDbRepository dynamoDbRepository;

    @Mock
    private AbisApiClient abisApiClient;

    @Mock
    private SubscriptionProvider subscriptionProvider;

    private OrderCreatedEventHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(zuoraClient.get()).thenReturn(mockZuoraClient);
        when(mockZuoraClient.ordersApi()).thenReturn(ordersApi);
        when(mockZuoraClient.contactsApi()).thenReturn(contactsApi);
        handler = new OrderCreatedEventHandler(zuoraClient, orderValidator, correctedOrderBuilder,
                subscriptionIdGenerator, dynamoDbRepository, abisApiClient, subscriptionProvider);
        JSON.createGson().create();
    }


        @Test
        void testProcessValidOrder() throws Exception {
                // ...既存のArrange/Act/Assert...
        }
        // --- shouldConvertToCancelSubscriptionOrder のシンプルなテスト ---

        @Test
        void testShouldConvertToCancelSubscriptionOrder_allAllowed_returnsTrue() throws Exception {
                // Arrange
                OrderCreatedEventHandler spyHandler = spy(handler);
                String subscriptionNumber = "SUB-001";
                OrderAction removeAction = new OrderAction();
                removeAction.setType(OrderActionType.REMOVEPRODUCT);
                OrderActionRemoveProduct removeProduct = new OrderActionRemoveProduct();
                removeProduct.setRatePlanId("RP-001");
                removeAction.setRemoveProduct(removeProduct);
                HashMap<String, List<OrderAction>> map = new HashMap<>();
                map.put(subscriptionNumber, List.of(removeAction));

                // 返すチャージは全てOKなもの
                GetSubscriptionRatePlanChargesWithAllSegments charge = new GetSubscriptionRatePlanChargesWithAllSegments();
                charge.setModel(com.zuora.model.ChargeModel.FLATFEE);
                charge.setPrice(BigDecimal.ZERO);
                doReturn(List.of(charge)).when(spyHandler).getPreviewRatePlanCharges(Mockito.eq(subscriptionNumber), Mockito.eq(removeAction));

                // Act
                boolean result = spyHandler.shouldConvertToCancelSubscriptionOrder(map);

                // Assert
                assertEquals(true, result);
        }

        @Test
        void testShouldConvertToCancelSubscriptionOrder_notAllowed_returnsFalse() throws Exception {
                // Arrange
                OrderCreatedEventHandler spyHandler = spy(handler);
                String subscriptionNumber = "SUB-001";
                OrderAction removeAction = new OrderAction();
                removeAction.setType(OrderActionType.REMOVEPRODUCT);
                OrderActionRemoveProduct removeProduct = new OrderActionRemoveProduct();
                removeProduct.setRatePlanId("RP-001");
                removeAction.setRemoveProduct(removeProduct);
                HashMap<String, List<OrderAction>> map = new HashMap<>();
                map.put(subscriptionNumber, List.of(removeAction));

                // 返すチャージはNGなもの
                GetSubscriptionRatePlanChargesWithAllSegments charge = new GetSubscriptionRatePlanChargesWithAllSegments();
                charge.setModel(com.zuora.model.ChargeModel.PERUNIT); // FLATFEE以外
                charge.setPrice(BigDecimal.ONE);
                doReturn(List.of(charge)).when(spyHandler).getPreviewRatePlanCharges(Mockito.eq(subscriptionNumber), Mockito.eq(removeAction));

                // Act
                boolean result = spyHandler.shouldConvertToCancelSubscriptionOrder(map);

                // Assert
                assertEquals(false, result);
        }

                @Test
        void testBuildCancelSubscriptionOrder() {
                Order original = new Order();
                original.setOrderDate(java.time.LocalDate.of(2025, 8, 29));
                original.setExistingAccountNumber("ACC-001");
                String subNum = "SUB-001";
                OrderAction action = new OrderAction();
                action.setType(OrderActionType.REMOVEPRODUCT);
                OrderCreatedEventHandler handler = new OrderCreatedEventHandler(zuoraClient, orderValidator, correctedOrderBuilder, subscriptionIdGenerator, dynamoDbRepository, abisApiClient, subscriptionProvider);
                Order result = handler.buildCancelSubscriptionOrder(original, subNum, action);
                assertEquals(original.getOrderDate(), result.getOrderDate());
                assertEquals(original.getExistingAccountNumber(), result.getExistingAccountNumber());
                assertEquals(subNum, result.getSubscriptions().get(0).getSubscriptionNumber());
                assertEquals(OrderActionType.CANCELSUBSCRIPTION, result.getSubscriptions().get(0).getOrderActions().get(0).getType());
        }
        @Test
        void testCreateCancelOrderAction() {
                OrderAction src = new OrderAction();
                src.setTriggerDates(List.of(new com.zuora.model.TriggerDate().name(com.zuora.model.TriggerDateName.CONTRACTEFFECTIVE).triggerDate(java.time.LocalDate.of(2025, 8, 29))));
                OrderCreatedEventHandler handler = new OrderCreatedEventHandler(zuoraClient, orderValidator, correctedOrderBuilder, subscriptionIdGenerator, dynamoDbRepository, abisApiClient, subscriptionProvider);
                OrderAction result = handler.createCancelOrderAction(src);
                assertEquals(OrderActionType.CANCELSUBSCRIPTION, result.getType());
                assertEquals(java.time.LocalDate.of(2025, 8, 29), result.getCancelSubscription().getCancellationEffectiveDate());
        }

        @Test
        void testGetContractEffectiveDate() {
                OrderCreatedEventHandler handler = new OrderCreatedEventHandler(zuoraClient, orderValidator, correctedOrderBuilder, subscriptionIdGenerator, dynamoDbRepository, abisApiClient, subscriptionProvider);
                // 該当あり
                List<com.zuora.model.TriggerDate> list = List.of(new com.zuora.model.TriggerDate().name(com.zuora.model.TriggerDateName.CONTRACTEFFECTIVE).triggerDate(java.time.LocalDate.of(2025, 8, 29)));
                assertEquals(java.time.LocalDate.of(2025, 8, 29), handler.getContractEffectiveDate(list));
                // 該当なし
                List<com.zuora.model.TriggerDate> list2 = List.of(new com.zuora.model.TriggerDate().name(com.zuora.model.TriggerDateName.SERVICEACTIVATION));
                assertEquals(null, handler.getContractEffectiveDate(list2));
                // null
                assertEquals(null, handler.getContractEffectiveDate(null));
        }

        @Test
        void testIsFlatFeeChargeWithZeroPrice_and_IsOneTimeCharge() {
                OrderCreatedEventHandler handler = new OrderCreatedEventHandler(zuoraClient, orderValidator, correctedOrderBuilder, subscriptionIdGenerator, dynamoDbRepository, abisApiClient, subscriptionProvider);
                com.zuora.model.GetSubscriptionRatePlanChargesWithAllSegments charge1 = new com.zuora.model.GetSubscriptionRatePlanChargesWithAllSegments();
                charge1.setModel(com.zuora.model.ChargeModel.FLATFEE);
                charge1.setPrice(java.math.BigDecimal.ZERO);
                assertEquals(true, handler.isFlatFeeChargeWithZeroPrice(charge1));
                charge1.setPrice(java.math.BigDecimal.ONE);
                assertEquals(false, handler.isFlatFeeChargeWithZeroPrice(charge1));
                charge1.setModel(com.zuora.model.ChargeModel.PERUNIT);
                assertEquals(false, handler.isFlatFeeChargeWithZeroPrice(charge1));

                com.zuora.model.GetSubscriptionRatePlanChargesWithAllSegments charge2 = new com.zuora.model.GetSubscriptionRatePlanChargesWithAllSegments();
                charge2.setType(com.zuora.model.ChargeType.ONETIME);
                assertEquals(true, handler.isOneTimeCharge(charge2));
                charge2.setType(com.zuora.model.ChargeType.RECURRING);
                assertEquals(false, handler.isOneTimeCharge(charge2));
        }

        @Test
        void testGetPreviewRatePlanCharges_nullパターン() throws Exception {
                // subscriptionProvider.getがnull
                OrderCreatedEventHandler handler = new OrderCreatedEventHandler(zuoraClient, orderValidator, correctedOrderBuilder, subscriptionIdGenerator, dynamoDbRepository, abisApiClient, subscriptionProvider);
                OrderSubscriptions sub = new OrderSubscriptions();
                sub.setSubscriptionNumber("SUB-001");
                OrderAction action = new OrderAction();
                action.setRemoveProduct(new OrderActionRemoveProduct().ratePlanId("RP-001"));
                org.mockito.Mockito.when(subscriptionProvider.get("SUB-001")).thenReturn(null);
                assertEquals(List.of(), handler.getPreviewRatePlanCharges(sub, action));
                // ratePlansがnull
                com.zuora.model.GetSubscriptionResponse resp = new com.zuora.model.GetSubscriptionResponse();
                org.mockito.Mockito.when(subscriptionProvider.get("SUB-001")).thenReturn(resp);
                assertEquals(List.of(), handler.getPreviewRatePlanCharges(sub, action));
                // removeProductがnull
                resp.setRatePlans(List.of(new com.zuora.model.SubscriptionRatePlan().id("RP-001")));
                action.setRemoveProduct(null);
                assertEquals(List.of(), handler.getPreviewRatePlanCharges(sub, action));
        }
    @Test
    void testProcessCorrectableOrder() throws Exception {
        // Arrange
        Order order = createValidOrder();
        GetOrderResponse response = new GetOrderResponse().order(order).success(true);
        ConstraintViolation violation = new ConstraintViolation("Test violation", true);
        Result validationResult = new Result(Arrays.asList(violation));
        when(ordersApi.getOrderApi("O-00000041")).thenReturn(getOrderApi);
        when(getOrderApi.execute()).thenReturn(response);
        when(orderValidator.validate(order)).thenReturn(validationResult);
        when(correctedOrderBuilder.buildCorrectedOrderRequest(order, OrderStatus.PENDING))
                .thenReturn(new CreateOrderRequest());
        when(ordersApi.createOrderApi(any(CreateOrderRequest.class))).thenReturn(createOrderApi);
        when(createOrderApi.execute()).thenReturn(new CreateOrderResponse().orderNumber("O-CORRECTED"));
        when(ordersApi.deleteOrderApi("O-00000041")).thenReturn(deleteOrderApi);
        when(deleteOrderApi.execute()).thenReturn(null);
        when(subscriptionIdGenerator.generate()).thenReturn("PUB-12345");
        when(ordersApi.updateSubscriptionCustomFieldsApi(anyString(), any(UpdateSubscriptionCustomFieldsRequest.class)))
                .thenReturn(updateSubscriptionCustomFieldsApi);
        when(updateSubscriptionCustomFieldsApi.execute()).thenReturn(null);
        when(ordersApi.updateOrderCustomFieldsApi(anyString(), any(UpdateOrderCustomFieldsRequest.class)))
                .thenReturn(updateOrderCustomFieldsApi);
        when(updateOrderCustomFieldsApi.execute()).thenReturn(null);
        when(contactsApi.getContactApi(anyString())).thenReturn(getContactApi);
        when(getContactApi.execute()).thenReturn(createContactResponse());
        ArrangementResponse abisResponse = new ArrangementResponse("accepted", "https://localhost/index.html", null,
                null);
        when(abisApiClient.createArrangementOrder(any(ArrangementOrderRequest.class))).thenReturn(abisResponse);

        // Act
        handler.handleEvent(createJsonEvent("O-00000041"));

        // Assert
        verify(correctedOrderBuilder, times(1)).buildCorrectedOrderRequest(order, OrderStatus.PENDING);
        verify(ordersApi, times(1)).deleteOrderApi("O-00000041");
        verify(abisApiClient, times(1)).createArrangementOrder(any(ArrangementOrderRequest.class));
    }

    @Test
    void testProcessUncorrectableOrder() throws Exception {
        // Arrange
        Order order = createValidOrder();
        GetOrderResponse response = new GetOrderResponse().order(order).success(true);
        ConstraintViolation violation = new ConstraintViolation("Uncorrectable violation", false);
        Result validationResult = new Result(Arrays.asList(violation));
        when(ordersApi.getOrderApi("O-00000041")).thenReturn(getOrderApi);
        when(getOrderApi.execute()).thenReturn(response);
        when(orderValidator.validate(order)).thenReturn(validationResult);
        when(correctedOrderBuilder.buildCorrectedOrderRequest(order, OrderStatus.DRAFT))
                .thenReturn(new CreateOrderRequest());
        when(ordersApi.createOrderApi(any(CreateOrderRequest.class))).thenReturn(createOrderApi);
        when(createOrderApi.execute()).thenReturn(new CreateOrderResponse().orderNumber("O-DRAFT"));
        when(ordersApi.deleteOrderApi("O-00000041")).thenReturn(deleteOrderApi);
        when(deleteOrderApi.execute()).thenReturn(null);
        when(contactsApi.getContactApi(anyString())).thenReturn(getContactApi);
        when(getContactApi.execute()).thenReturn(createContactResponse());

        // Act
        handler.handleEvent(createJsonEvent("O-00000041"));

        // Assert
        verify(correctedOrderBuilder, times(1)).buildCorrectedOrderRequest(order, OrderStatus.DRAFT);
        verify(ordersApi, times(1)).deleteOrderApi("O-00000041");
        verify(abisApiClient, times(1)).sendValidationNotification(any(ValidationNotificationRequest.class));
    }

    private Order createValidOrder() {
        Order order = new Order();
        order.setOrderNumber("O-00000041");
        order.setCreatedBy("user123");
        OrderSubscriptions subscription = new OrderSubscriptions();
        subscription.setSubscriptionNumber("SUB-001");
        OrderAction action = new OrderAction();
        action.setType(OrderActionType.CREATESUBSCRIPTION);
        subscription.setOrderActions(Arrays.asList(action));
        order.setSubscriptions(Arrays.asList(subscription));
        order.putAdditionalProperty(PROP_NAME_ORGANIZATION_LABEL, "ORGANIZATION-001");
        return order;
    }

    private ContactResponse createContactResponse() {
        ContactResponse contact = new ContactResponse();
        contact.setWorkEmail("test@example.com");
        return contact;
    }

    private com.fasterxml.jackson.databind.JsonNode createJsonEvent(String orderNumber) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = String.format("{\"OrderNumber\":\"%s\"}", orderNumber);
        return mapper.readTree(json);
    }

    @Test
    @Disabled
    void executeHandleEventInSandboxEnvironment()
            throws JsonMappingException, JsonProcessingException, ApiException {
        OrderCreatedEventHandler sut = DaggerOrderCreatedEventHandlerTest_Component.create().handler();
        ObjectMapper objectMapper = DaggerOrderCreatedEventHandlerTest_Component.create().objectMapper();
        sut.handleEvent(objectMapper.readTree("{\"OrderNumber\":\"O-00000041\"}"));
    }
}

}
