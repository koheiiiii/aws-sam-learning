package com.fujifilm.fb.spf.subscription.zuora.complement;

import dagger.Component;
import jakarta.inject.Singleton;

/**
 * アプリケーション全体の依存性注入を管理するDaggerコンポーネントです。
 * <p>
 * このコンポーネントは以下の役割を担います：
 * - ZuoraEventHandlerAppへの依存性注入
 * - シングルトンインスタンスの管理
 * - AWS ClientやRepositoryの提供
 * </p>
 */
@Singleton
@Component(modules = {AwsClientModule.class, ValidatorModule.class})
public interface ApplicationComponent {
    
    /**
     * ZuoraEventHandlerAppに依存性を注入します。
     * 
     * @param app 依存性注入対象のZuoraEventHandlerApp
     */
    void inject(ZuoraEventHandlerApp app);
}
