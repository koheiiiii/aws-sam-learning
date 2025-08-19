package com.fujifilm.fb.spf.subscription.zuora.complement;

import java.util.ArrayList;
import java.util.List;

/**
 * バリデーションコンテキストクラス.
 * バリデーション過程での違反情報を蓄積する.
 */
public class ValidationContext {

  private final List<ValidationViolation> violations;

  /**
   * コンストラクタ.
   */
  public ValidationContext() {
    this.violations = new ArrayList<>();
  }

  /**
   * バリデーション違反を追加する.
   * 
   * @param validatorName バリデータ名
   * @param violationCode 違反コード
   * @param message 違反メッセージ
   */
  public void addViolation(String validatorName, String violationCode, String message) {
    violations.add(new ValidationViolation(validatorName, violationCode, message));
  }

  /**
   * 違反があるかどうかを返す.
   * 
   * @return 違反がある場合true
   */
  public boolean hasViolations() {
    return !violations.isEmpty();
  }

  /**
   * 違反リストを取得する.
   * 
   * @return 違反リスト
   */
  public List<ValidationViolation> getViolations() {
    return new ArrayList<>(violations);
  }

  /**
   * 違反数を取得する.
   * 
   * @return 違反数
   */
  public int getViolationCount() {
    return violations.size();
  }

  /**
   * バリデーション違反情報を表すクラス.
   */
  public static class ValidationViolation {
    private final String validatorName;
    private final String violationCode;
    private final String message;

    /**
     * コンストラクタ.
     * 
     * @param validatorName バリデータ名
     * @param violationCode 違反コード
     * @param message 違反メッセージ
     */
    public ValidationViolation(String validatorName, String violationCode, String message) {
      this.validatorName = validatorName;
      this.violationCode = violationCode;
      this.message = message;
    }

    public String getValidatorName() {
      return validatorName;
    }

    public String getViolationCode() {
      return violationCode;
    }

    public String getMessage() {
      return message;
    }

    @Override
    public String toString() {
      return String.format("[%s] %s: %s", validatorName, violationCode, message);
    }
  }
}
