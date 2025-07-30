package com.fujifilm.fb.spf.subscription.zuora.complement;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ZuoraEventHandlerTest {

    @Test
    public void successfulResponse() {
        ZuoraEventHandler handler = new ZuoraEventHandler();
        APIGatewayProxyResponseEvent result = handler.handleRequest(null, null);

        assertEquals(200, result.getStatusCode().intValue());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().contains("\"message\""));
        assertTrue(result.getBody().contains("hello Lambda"));
    }
}
