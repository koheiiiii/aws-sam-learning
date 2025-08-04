package com.fujifilm.fb.spf.subscription.zuora.complement;

import java.security.SecureRandom;

/**
 * サブスクリプションIDを生成するユーティリティクラスです。
 * <p>
 * 12桁のランダムな数字を4桁ごとにハイフンで区切った
 * 「nnnn-nnnn-nnnn」形式のIDを生成します。
 * </p>
 * <pre>
 * 例: 1234-5678-9012
 * </pre>
 * インスタンス化はできません。
 */
class SubscriptionIdGenerator {
    private static final int DIGIT_LENGTH = 12;
    private static final int BLOCK_SIZE = 4;

    private SubscriptionIdGenerator() {
        // プライベートコンストラクタでインスタンス化を防ぐ
    }

    /**
     * 12桁のランダムな数字を4桁ごとにハイフンで区切ったIDを生成します。
     *
     * @return nnnn-nnnn-nnnn 形式のランダムID
     */
    public static String generate(){
        SecureRandom random = new SecureRandom();
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
