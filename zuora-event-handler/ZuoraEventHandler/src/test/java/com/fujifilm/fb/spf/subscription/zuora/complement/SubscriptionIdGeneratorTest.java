package com.fujifilm.fb.spf.subscription.zuora.complement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SubscriptionIdGeneratorTest {
    @Test
    void generate_returns12DigitsWithHyphens() {
        String id = SubscriptionIdGenerator.generate();
        // 形式: nnnn-nnnn-nnnn
        assertNotNull(id);
        assertEquals(14, id.length());
        assertTrue(id.matches("\\d{4}-\\d{4}-\\d{4}"));
    }

    @Test
    void generate_isRandom() {
        String id1 = SubscriptionIdGenerator.generate();
        String id2 = SubscriptionIdGenerator.generate();
        assertNotEquals(id1, id2, "連続で同じIDが生成されるべきではない");
    }
}
