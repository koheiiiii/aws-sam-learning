package com.fujifilm.fb.spf.subscription.zuora.complement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SubscriptionIdGeneratorTest {
    
    private SubscriptionIdGenerator generator;
    
    @BeforeEach
    void setUp() {
        // テスト用にインスタンスを直接作成
        generator = new SubscriptionIdGenerator();
    }
    
    @Test
    void generate_returns12DigitsWithHyphens() {
        String id = generator.generate();
        // 形式: nnnn-nnnn-nnnn
        assertNotNull(id);
        assertEquals(14, id.length());
        assertTrue(id.matches("\\d{4}-\\d{4}-\\d{4}"));
    }

    @Test
    void generate_isRandom() {
        String id1 = generator.generate();
        String id2 = generator.generate();
        assertNotEquals(id1, id2, "連続で同じIDが生成されるべきではない");
    }
    
    @Test
    void generate_sameInstanceProducesDifferentIds() {
        // 同じインスタンスから異なるIDが生成されることを確認
        String id1 = generator.generate();
        String id2 = generator.generate();
        String id3 = generator.generate();
        
        assertNotEquals(id1, id2);
        assertNotEquals(id2, id3);
        assertNotEquals(id1, id3);
    }
}
