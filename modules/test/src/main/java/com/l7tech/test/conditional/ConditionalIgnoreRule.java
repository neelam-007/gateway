package com.l7tech.test.conditional;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A JUnit Rule enabling {@link ConditionalIgnore} annotation.<br/>
 * This rule must be declared when using the {@link ConditionalIgnore} annotation.
 */
public class ConditionalIgnoreRule implements TestRule {

    /**
     * Species the {@link ConditionalIgnore} statement.<br/>
     * This statement will be returned when the specified ignore condition (i.e. {@link IgnoreCondition}) is satisfied.
     */
    private static class ConditionalIgnoreStatement extends Statement {
        /**
         * When evaluated, simply assume <code>false</code> so that the unit-test/method will be ignored.
         */
        @Override
        public void evaluate() {
            Assume.assumeTrue(false);
        }
    }

    @Override
    public Statement apply(final Statement statement, final Description description) {
        Statement result = statement;
        // does this method or class have ConditionalIgnore annotation. If both do prefer the method annotation
        final ConditionalIgnore methodAnnotation = description.getAnnotation(ConditionalIgnore.class);
        final ConditionalIgnore classAnnotation = description.getTestClass().getAnnotation(ConditionalIgnore.class);
        final ConditionalIgnore annotation = methodAnnotation != null ? methodAnnotation : classAnnotation;
        if (annotation != null) {
            // if it does, extract the specified condition i.e. IgnoreCondition instance.
            final IgnoreCondition condition = newCondition(annotation);
            if (condition.isSatisfied()) {
                result = new ConditionalIgnoreStatement();
            }
        }
        return result;
    }

    /**
     * Creates a new instance from the specified <tt>annotation</tt>
     * {@link com.l7tech.test.conditional.ConditionalIgnore#condition() condition}.
     *
     * @param annotation    Specified {@link ConditionalIgnore} annotation.
     * @return a instance of the specified {@link ConditionalIgnore} annotation
     *         {@link com.l7tech.test.conditional.ConditionalIgnore#condition() condition}.
     *         Never <code>null</code>.
     * @throws ConditionalIgnoreException if we fail to create a new instance from the specified annotation
     *                                    {@link com.l7tech.test.conditional.ConditionalIgnore#condition() condition}.
     */
    private IgnoreCondition newCondition(final ConditionalIgnore annotation) {
        try {
            return annotation.condition().newInstance();
        } catch (RuntimeException e) {
            throw new ConditionalIgnoreException(e);
        } catch (InstantiationException e) {
            throw new ConditionalIgnoreException("Failed to instantiate ConditionalIgnore annotation", e);
        } catch (IllegalAccessException e) {
            throw new ConditionalIgnoreException("ConditionalIgnore annotation constructor is not accessible", e);
        }
    }
}
