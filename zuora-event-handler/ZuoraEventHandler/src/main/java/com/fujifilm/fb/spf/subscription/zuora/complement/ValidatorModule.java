package com.fujifilm.fb.spf.subscription.zuora.complement;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;

/**
 * バリデータ関連のDaggerモジュール.
 */
@Module
public class ValidatorModule {

  /**
   * OrderValidatorをプロバイド.
   */
  @Provides
  @Singleton
  public OrderValidator provideOrderValidator() {
    return new OrderValidator();
  }
}
