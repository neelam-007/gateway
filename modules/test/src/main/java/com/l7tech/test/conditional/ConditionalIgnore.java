package com.l7tech.test.conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark unit-tests/methods as ignored, when the specified condition
 * (i.e. implementation of {@link IgnoreCondition}) is satisfied.<br/>
 * In other words, ignore this unit-test if the specified condition is satisfied.
 *
 * This annotation can be put on either the test class or the test method.
 * If it is on both only the conditional ignore on the test method is used
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ConditionalIgnore {
    /**
     * Specified condition class, must implement {@link IgnoreCondition} interface.
     */
    Class<? extends IgnoreCondition> condition();
}
