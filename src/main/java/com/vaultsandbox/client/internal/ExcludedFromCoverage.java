package com.vaultsandbox.client.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated element should be excluded from code coverage analysis.
 *
 * <p>This annotation should only be used for code that is intentionally not covered by tests, such
 * as:
 *
 * <ul>
 *   <li>Destructive operations only used for test cleanup
 *   <li>Code paths that cannot be exercised in unit tests (e.g., emergency fallbacks)
 *   <li>Generated or boilerplate code
 * </ul>
 *
 * <p>Most coverage tools (JaCoCo, Cobertura) recognize annotations with "Generated" in the name,
 * but this custom annotation provides clearer semantics for intentional exclusions.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface ExcludedFromCoverage {
  /**
   * The reason for excluding this code from coverage analysis.
   *
   * @return a description of why this code is excluded
   */
  String value() default "";
}
