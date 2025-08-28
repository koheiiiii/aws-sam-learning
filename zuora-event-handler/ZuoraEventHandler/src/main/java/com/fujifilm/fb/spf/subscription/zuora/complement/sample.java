public class sample {
    public class OrderCreatedEventHandler extends OrderEventHandler {

    private static final Logger LOGGER = LogManager.getLogger(OrderCreatedEventHandler.class);
    private static final int MAX_RETRY_COUNT = Integer
            .getInteger("fb.spf.subscription.subscriptionIdRegistration.maxRetry", 5);
    private static final Integer ABIS_MAX_RETRY_COUNT = Integer
            .getInteger("fb.spf.subscription.abisSendRequest.maxRetry", 3);
    private static final Integer ABIS_RETRY_DELAY_SECONDS = Integer
            .getInteger("fb.spf.subscription.abisSendRequest.retryDelaySeconds", 3);
    private static final RetryPolicy<ArrangementResponse> ABIS_RETRY_POLICY = RetryPolicy.<ArrangementResponse>builder()
            .handle(IOException.class, InterruptedException.class)
            .withDelay(Duration.ofSeconds(ABIS_MAX_RETRY_COUNT))
            .withMaxRetries(ABIS_RETRY_DELAY_SECONDS)
            .build();
    private static final String CUSTOM_LINK_FIELD_NAME = "LinkToArrangementDetails__c";
    private final OrderValidator orderValidator;
    private final CorrectedOrderBuilder correctedOrderBuilder;
    private final SubscriptionIdGenerator subscriptionIdGenerator;
    private final DynamoDbRepository dynamoDbRepository;
    private final AbisApiClient abisApiClient;
    private final SubscriptionProvider subscriptionProvider;

    @Inject
    public OrderCreatedEventHandler(Lazy<ZuoraClient> zuoraClient, OrderValidator orderValidator,
            CorrectedOrderBuilder correctedOrderBuilder, SubscriptionIdGenerator subscriptionIdGenerator,
            DynamoDbRepository dynamoDbRepository, AbisApiClient abisApiClient,
            SubscriptionProvider subscriptionProvider) {
        super(zuoraClient);
        this.orderValidator = orderValidator;
        this.correctedOrderBuilder = correctedOrderBuilder;
        this.subscriptionIdGenerator = subscriptionIdGenerator;
        this.dynamoDbRepository = dynamoDbRepository;
        this.abisApiClient = abisApiClient;
        this.subscriptionProvider = subscriptionProvider;
    }

    @Override
    protected void process(Order order) throws ApiException {
        OrderValidator.Result validationResult = orderValidator.validate(order);
        switch (validationResult.getType()) {
            case VALID:
                processValidOrder(order);
                break;
            case CORRECTABLE:
                processCorrectableOrder(order, validationResult);
                break;
            case UNCORRECTABLE:
                processUncorrectableOrder(order, validationResult);
                break;
        }
    }

    // 不整合がなかった場合の処理
    private void processValidOrder(Order order) throws ApiException {
        // CancelSubscriptionオーダーに変換可能なRemoveProductオーダーの場合は変換して処理
        Map<String, List<OrderAction>> filterRemoveActionsBySubscription = filterRemoveActionsBySubscription(order);
        if (!filterRemoveActionsBySubscription.isEmpty() && shouldConvertToCancelSubscriptionOrder(filterRemoveActionsBySubscription))  {
            deleteOrder(order);
            createCorrectedOrder(createCancelSubscriptionOrder(order), OrderStatus.COMPLETED);
        }else {
            // 公開サブスクリプションIDの生成およびサブスクリプションへの反映（新規のみ）
            applyPublicSubscriptionId(order);
        }

        // Abisへの手配プロセス開始依頼（共通）
        ArrangementResponse response = sendCreateOrderRequestToAbis(order, null, Arrays.asList());

        // Abis手配詳細画面へのリンクのオーダーへの設定（共通）
        Map<String, Object> linkFieldUrl = new HashMap<>();
        linkFieldUrl.put(CUSTOM_LINK_FIELD_NAME, response.arrangementUrl());
        updateOrderCustomFields(order, linkFieldUrl);
    }

    // 不整合はあるが、補正不可能な不整合がなかった場合の処理
    private void processCorrectableOrder(Order order, Result validationResult) throws ApiException {
        String oldOrderNumber = order.getOrderNumber();
        createCorrectedOrder(order, OrderStatus.PENDING);
        deleteOrder(order);

        // 公開サブスクリプションIDの生成およびサブスクリプションへの反映
        applyPublicSubscriptionId(order);

        // Abisへの手配プロセス開始依頼(修正内容込み)
        List<String> messages = new ArrayList<>();
        for (ConstraintViolation violation : validationResult.getConstraintViolations()) {
            messages.add(violation.getMessage());
        }
        ArrangementResponse response = sendCreateOrderRequestToAbis(order, oldOrderNumber, messages);

        // Abis手配詳細画面へのリンクのオーダーへの設定
        Map<String, Object> linkFieldUrl = new HashMap<>();
        linkFieldUrl.put(CUSTOM_LINK_FIELD_NAME, response.arrangementUrl());
        updateOrderCustomFields(order, linkFieldUrl);
    }

    // 補正不可能な不整合があった場合の処理
    private void processUncorrectableOrder(Order order, Result validationResult)
            throws ApiException {
        createCorrectedOrder(order, OrderStatus.DRAFT);
        deleteOrder(order);

        // Abisへのバリデーション結果通知依頼
        List<String> messages = new ArrayList<>();
        for (ConstraintViolation violation : validationResult.getConstraintViolations()) {
            messages.add(violation.getMessage());
        }
        try {
            sendNotificationToAbisValidation(order, messages);
        } catch (IOException | InterruptedException ex) {
        }
    }

    private void createCorrectedOrder(Order order, OrderStatus orderStatus) throws ApiException {
        CreateOrderRequest correctedOrderRequest = correctedOrderBuilder.buildCorrectedOrderRequest(order, orderStatus);
        if (LOGGER.isInfoEnabled()) {
            // JSON.getGson()でnullが返るため.toJson()を.getOrderNumber()に変更
            // LOGGER.info("Corrected Order: {}", order.toJson());
            LOGGER.info("Corrected Order: {}", order.getOrderNumber());
        }
        CreateOrderResponse response = executeOrderCreation(correctedOrderRequest);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{} order {} has been created", orderStatus, response.getOrderNumber());
        }
    }

    private CreateOrderResponse executeOrderCreation(CreateOrderRequest correctedOrderRequest)
            throws ApiException {
        System.out.println(correctedOrderRequest);
        return getZuoraClient().ordersApi().createOrderApi(correctedOrderRequest)
                .execute();
    }

    private void deleteOrder(Order order) throws ApiException {
        getZuoraClient().ordersApi().deleteOrderApi(order.getOrderNumber()).execute();
    }

    private void updateOrderCustomFields(Order order, Map<String, Object> fields) throws ApiException {
        UpdateOrderCustomFieldsRequest request = new UpdateOrderCustomFieldsRequest().customFields(fields);
        getZuoraClient().ordersApi().updateOrderCustomFieldsApi(order.getOrderNumber(), request).execute();
    }

    private void applyPublicSubscriptionId(Order order) {
        List<OrderSubscriptions> subscriptions = order.getSubscriptions();
        if (subscriptions == null) {
            throw new IllegalStateException("Order has no subscriptions");
        }
        for (OrderSubscriptions subscription : subscriptions) {
            String subscriptionNumber = subscription.getSubscriptionNumber();
            List<OrderAction> orderActions = subscription.getOrderActions();
            if (orderActions == null) {
                continue;
            }
            for (OrderAction orderAction : orderActions) {
                OrderActionType orderActionType = orderAction.getType();
                if (orderActionType == OrderActionType.CREATESUBSCRIPTION) {
                    updateSubscriptionWithPublicSubscriptionId(subscriptionNumber);
                }
            }
        }
    }

    private void updateSubscriptionWithPublicSubscriptionId(String subscriptionNumber) {
        // DynamoDBへの登録
        RetryPolicy<Object> retryPolicy = RetryPolicy.builder()
                .handle(ConditionalCheckFailedException.class)
                .withMaxRetries(MAX_RETRY_COUNT)
                .withDelay(Duration.ofSeconds(1))
                .onRetry(e -> LOGGER.info("Retrying due to subscriptionNumber: {}", subscriptionNumber,
                        e.getLastException()))
                .build();
        String publicSubscriptionId = Failsafe.with(retryPolicy).get(() -> {
            String id = subscriptionIdGenerator.generate();
            dynamoDbRepository.putSubscriptionRecord(id, subscriptionNumber);
            return id;
        });
        // サブスクリプションへPublicSubscriptionIDの登録
        Map<String, Object> customFields = new HashMap<>();
        customFields.put("PublicSubscriptionID__c", publicSubscriptionId);
        UpdateSubscriptionCustomFieldsRequest request = new UpdateSubscriptionCustomFieldsRequest()
                .customFields(customFields);
        try {
            getZuoraClient().ordersApi().updateSubscriptionCustomFieldsApi(subscriptionNumber, request)
                    .execute();
        } catch (ApiException e) {
            LOGGER.error("Failed to update subscription custom fields for subscription {}", subscriptionNumber, e);
            throw new RuntimeException(
                    "Failed to update subscription custom fields for subscription " + subscriptionNumber, e);
        }
    }

    private ArrangementResponse sendCreateOrderRequestToAbis(Order order, String oldOrderNumber, List<String> messages)
            throws ApiException {
        // TODO DynamoDBへの重複チェックと登録(タイミングはここでよい？)
        try {
            ArrangementResponse abisResponse = Failsafe.with(ABIS_RETRY_POLICY)
                    .get(() -> requestToAbisCreateOrder(order, oldOrderNumber, messages));
            if (abisResponse == null || !"accepted".equals(abisResponse.status())) {
                LOGGER.error("ABIS API returned unexpected response: {}", abisResponse);
                // TODO DynamoDBへ登録したレコードの削除
                // TODO throw an exception
                throw new RuntimeException("ABIS API returned unexpected response: " + abisResponse);
            }
            return abisResponse;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to send arrangement request after retries", e);
            // TODO DynamoDBへ登録したレコードの削除
            // TODO throw an exception
            throw new RuntimeException("Failed to send arrangement request after retries", e);
        }
    }

    private ArrangementResponse requestToAbisCreateOrder(Order order, String oldOrderNumber, List<String> messages)
            throws IOException, InterruptedException, ApiException {
        ArrangementOrderRequest request = new ArrangementOrderRequest(
                order.getOrderNumber(),
                oldOrderNumber,
                getUser(order).getWorkEmail(),
                messages);
        ArrangementResponse response = abisApiClient.createArrangementOrder(request);
        return response;
    }

    private ContactResponse getUser(Order order) throws ApiException {
        return getZuoraClient().contactsApi().getContactApi(order.getCreatedBy()).execute();
    }

    private void sendNotificationToAbisValidation(Order order, List<String> messages)
            throws IOException, InterruptedException, ApiException {
        for (OrderSubscriptions subscription : order.getSubscriptions()) {
            ValidationNotificationRequest request = new ValidationNotificationRequest(
                    subscription.getSubscriptionNumber(),
                    order.getOrderNumber(),
                    getUser(order).getWorkEmail(),
                    messages);
            abisApiClient.sendValidationNotification(request);
        }
    }



    private Map<String, List<OrderAction>> filterRemoveActionsBySubscription(Order order) {
        List<OrderSubscriptions> subscriptions = order.getSubscriptions();
        if (subscriptions == null) {
            throw new IllegalStateException("Order has no subscriptions");
        }

        Map<String, List<OrderAction>> result = new HashMap<>();
        for (OrderSubscriptions subscription : subscriptions) {
            List<OrderAction> orderActions = subscription.getOrderActions();
            if (orderActions == null) {
                continue;
            }
            List<OrderAction> removeActions = orderActions.stream()
                    .filter(action -> action.getType() == OrderActionType.REMOVEPRODUCT)
                    .collect(Collectors.toList());
            if (!removeActions.isEmpty()) {
                result.put(subscription.getSubscriptionNumber(), removeActions);
            }
        }
        return result;
    }

    private boolean shouldConvertToCancelSubscriptionOrder(Map<String, List<OrderAction>> filterRemoveActionsBySubscription) throws ApiException {
        for (Map.Entry<String, List<OrderAction>> entry : filterRemoveActionsBySubscription.entrySet()) {
            String subscriptionNumber = entry.getKey();
            List<OrderAction> removeActions = entry.getValue();

            for (OrderAction removeAction : removeActions) {
                List<GetSubscriptionRatePlanChargesWithAllSegments> previewRatePlanCharges = getPreviewRatePlanCharges(
                        subscriptionNumber, removeAction);
                boolean allAllowed = previewRatePlanCharges.stream()
                        .allMatch(charge -> isFlatFeeChargeWithZeroPrice(charge) || isOneTimeCharge(charge));
                if (!allAllowed) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * サブスクリプションごとに最初のRemoveProductアクションを使ってCancelSubscriptionオーダーを作成し、リストで返す。
     * RemoveProductアクションがないサブスクリプションはスキップされる。
     */
    private List<Order> createCancelSubscriptionOrders(Order order, Map<String, List<OrderAction>> filterRemoveActionsBySubscription) throws ApiException {
        List<Order> cancelOrders = new ArrayList<>();
        for (Map.Entry<String, List<OrderAction>> entry : filterRemoveActionsBySubscription.entrySet()) {
            String subscriptionNumber = entry.getKey();
            List<OrderAction> removeActions = entry.getValue();
            if (!removeActions.isEmpty()) {
                // サブスクリプションごとに最初のRemoveProductアクションのみを使う
                cancelOrders.add(buildCancelSubscriptionOrder(order, subscriptionNumber, removeActions.get(0)));
            }
        }
        return cancelOrders;
    }

    /**
     * （非推奨）最初のサブスクリプション・RemoveProductアクションのみでCancelSubscriptionオーダーを1件返す。
     * 複数サブスクリプション対応はcreateCancelSubscriptionOrdersを使用。
     */
    @Deprecated
    private Order createCancelSubscriptionOrder(Order order, Map<String, List<OrderAction>> filterRemoveActionsBySubscription) throws ApiException {
        for (Map.Entry<String, List<OrderAction>> entry : filterRemoveActionsBySubscription.entrySet()) {
            String subscriptionNumber = entry.getKey();
            List<OrderAction> removeActions = entry.getValue();
            for (OrderAction removeAction : removeActions) {
                return buildCancelSubscriptionOrder(order, subscriptionNumber, removeAction);
            }
        }
        // 該当がなければnullを返す
        return null;
    }

    private Order buildCancelSubscriptionOrder(Order originalOrder, String subscriptionNumber,
            OrderAction orderAction) {
        return new Order()
                .orderDate(originalOrder.getOrderDate())
                .existingAccountNumber(originalOrder.getExistingAccountNumber())
                .subscriptions(List.of(
                        new OrderSubscriptions()
                                .orderActions(List.of(createCancelOrderAction(orderAction)))
                                .subscriptionNumber(subscriptionNumber)));
    }

    private OrderAction createCancelOrderAction(OrderAction sourceOrderAction) {
        LocalDate cancellationDate = getContractEffectiveDate(sourceOrderAction.getTriggerDates());
        return new OrderAction()
                .triggerDates(sourceOrderAction.getTriggerDates())
                .type(OrderActionType.CANCELSUBSCRIPTION)
                .cancelSubscription(new OrderActionCancelSubscription()
                        .cancellationPolicy(SubscriptionCancellationPolicy.SPECIFICDATE)
                        .cancellationEffectiveDate(cancellationDate));
    }

    private LocalDate getContractEffectiveDate(List<TriggerDate> triggerDates) {
        if (triggerDates == null) {
            return null;
        }
        for (TriggerDate triggerDate : triggerDates) {
            TriggerDateName triggerDateName = triggerDate.getName();
            if (triggerDateName == null) {
                continue;
            }
            if (triggerDateName.equals(TriggerDateName.CONTRACTEFFECTIVE)) {
                return triggerDate.getTriggerDate();
            }
        }
        return null;
    }

    private List<GetSubscriptionRatePlanChargesWithAllSegments> getPreviewRatePlanCharges(
            OrderSubscriptions subscription,
            OrderAction orderAction)
            throws ApiException {
        GetSubscriptionResponse targetSubscription = subscriptionProvider.get(subscription.getSubscriptionNumber());
        if (targetSubscription == null) {
            return Collections.emptyList();
        }
        List<SubscriptionRatePlan> ratePlans = targetSubscription.getRatePlans();
        if (ratePlans == null) {
            return Collections.emptyList();
        }
        OrderActionRemoveProduct removeProduct = orderAction.getRemoveProduct();
        if (removeProduct == null) {
            return Collections.emptyList();
        }

        // Active状態のサブスクリプションのratePlanId一覧
        List<String> activeRatePlanIds = ratePlans.stream()
                .map(SubscriptionRatePlan::getId)
                .collect(Collectors.toList());
        // 削除対象のratePlanIdをまとめて取得（pending状態のremove product対象のもの）
        String removeRatePlanId = removeProduct.getRatePlanId();
        List<String> removeRatePlanIds = removeRatePlanId != null ? Collections.singletonList(removeRatePlanId)
                : Collections.emptyList();
        // アクティブなratePlanIdから削除対象を除外
        List<String> remainingRatePlanIds = activeRatePlanIds.stream()
                .filter(id -> !removeRatePlanIds.contains(id))
                .collect(Collectors.toList());
        // 残りのratePlanIdに該当するratePlanの
        // 全てのratePlanChargesをまとめてリスト化
        List<GetSubscriptionRatePlanChargesWithAllSegments> previewRatePlanCharges = ratePlans
                .stream()
                .filter(ratePlan -> remainingRatePlanIds.contains(ratePlan.getId()))
                .flatMap(ratePlan -> {
                    List<GetSubscriptionRatePlanChargesWithAllSegments> charges = ratePlan.getRatePlanCharges();
                    return charges == null ? Stream.empty() : charges.stream();
                })
                .collect(Collectors.toList());
        return previewRatePlanCharges;
    }

    private boolean isFlatFeeChargeWithZeroPrice(GetSubscriptionRatePlanChargesWithAllSegments ratePlanCharge) {
        if (ratePlanCharge == null) {
            return false;
        }
        BigDecimal price = ratePlanCharge.getPrice();
        return ratePlanCharge.getModel() == ChargeModel.FLATFEE
                && price != null && price.compareTo(BigDecimal.ZERO) == 0;
    }

    private boolean isOneTimeCharge(GetSubscriptionRatePlanChargesWithAllSegments ratePlanCharge) {
        if (ratePlanCharge == null) {
            return false;
        }
        return ratePlanCharge.getType() == ChargeType.ONETIME;
    }
}