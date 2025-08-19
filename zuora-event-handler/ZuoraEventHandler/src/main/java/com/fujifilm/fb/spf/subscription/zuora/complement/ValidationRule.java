package com.fujifilm.fb.spf.subscription.zuora.complement;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 個別のバリデーションルールを表すインターフェース.
 */
public interface ValidationRule {
    
    /**
     * バリデーションを実行する.
     * 
     * @param orderData 検証対象のオーダーデータ
     * @param context バリデーションコンテキスト
     */
    void validate(JsonNode orderData, ValidationContext context);
    
    /**
     * このルールの名前を取得する.
     * 
     * @return ルール名
     */
    String getRuleName();
}
