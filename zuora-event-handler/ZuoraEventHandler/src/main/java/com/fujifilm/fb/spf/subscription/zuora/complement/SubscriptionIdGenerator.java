package com.fujifilm.fb.spf.subscription.zuora.complement;

import java.security.SecureRandom;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * サブスクリプションIDを生成するクラスです。
 * <p>
 * 12桁のランダムな数字を4桁ごとにハイフンで区切った
 * 「nnnn-nnnn-nnnn」形式のIDを生成します。
 * </p>
 * <pre>
 * 例: 1234-5678-9012
 * </pre>
 * このクラスはスレッドセーフであり、シングルトンとして使用されます。
 */
@Singleton
class SubscriptionIdGenerator {
    private static final int DIGIT_LENGTH = 12;
    private static final int BLOCK_SIZE = 4;
    
    private final SecureRandom random;

    @Inject
    SubscriptionIdGenerator() {
        this.random = new SecureRandom();
    }

    /**
     * 12桁のランダムな数字を4桁ごとにハイフンで区切ったIDを生成します。
     *
     * @return nnnn-nnnn-nnnn 形式のランダムID
     */
    public String generate() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DIGIT_LENGTH; i++) {
            if (i > 0 && i % BLOCK_SIZE == 0) {
                sb.append("-");
            }
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
