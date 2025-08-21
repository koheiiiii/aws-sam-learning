package com.fujifilm.fb.spf.subscription.zuora.complement;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/**
 * SSL証明書チェック無効化 (sam local invoke用)
 * 本番Lambda環境では不要だが、Docker環境での証明書エラー回避
 */
public class SslConfig {
    
    /**
     * 開発・ローカル環境でSSL証明書チェックを無効化
     * javax.net.ssl.SSLHandshakeException: PKIX path building failed エラーを解消
     */
    public static void disableSslVerification() {
        try {
            // 【1】全ての証明書を信頼するTrustManager作成
            // → 「この証明書、信じていいの？」というチェックを無効化
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    // 【1-A】サーバーが信頼できる認証局のリストを返す → 空で「全部OK」
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    // 【1-B】クライアント証明書チェック → 何もしない「全部OK」  
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    // 【1-C】サーバー証明書チェック → 何もしない「全部OK」
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            
            // 【2】SSLContextに「証明書チェック無効化」を設定
            // → JavaのHTTPS通信全体に影響
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            // 【3】HostnameVerifierも無効化
            // → 「証明書のホスト名、URLと一致する？」チェックを無効化
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            
            // 【4】OkHttp3用のSSL設定を追加（Zuora SDK対応）
            configureOkHttpSsl(trustAllCerts);
            
        } catch (Exception e) {
            System.err.println("SSL無効化設定エラー: " + e.getMessage());
        }
    }

    /**
     * OkHttp3クライアント用のSSL証明書チェック無効化
     * Zuora SDKが内部で使用するOkHttp3クライアントに適用
     */
    private static void configureOkHttpSsl(TrustManager[] trustAllCerts) {
        try {
            // SSLContextを作成
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // JVMレベルでSSL証明書検証を無効化
            // これによりOkHttp3も含めた全てのHTTPS通信に影響
            System.setProperty("com.sun.net.ssl.checkRevocation", "false");
            System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
            
            // SSL証明書の検証を完全に無効化
            System.setProperty("trust_all_cert", "true");
            
            // OkHttp3向け：デフォルトSSLContextを設定
            SSLContext.setDefault(sslContext);
            
        } catch (Exception e) {
            System.err.println("OkHttp SSL設定エラー: " + e.getMessage());
        }
    }
}