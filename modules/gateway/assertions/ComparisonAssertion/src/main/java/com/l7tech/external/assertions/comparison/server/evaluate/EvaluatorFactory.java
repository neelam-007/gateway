package com.l7tech.external.assertions.comparison.server.evaluate;

import com.l7tech.external.assertions.comparison.*;
import com.l7tech.util.CollectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * @author alex
 */
public final class EvaluatorFactory {
    private static final Map<Class<? extends Predicate>, Class<? extends Evaluator>> evaluatorMap = CollectionUtils.<Class<? extends Predicate>, Class<? extends Evaluator>>mapBuilder()
        // No evaluator for BinaryPredicates; they're handled specially in the ServerAssertion due to the need to
        // interpolate variables into the RHS expression.
        .put(CardinalityPredicate.class, CardinalityEvaluator.class)
        .put(EmptyPredicate.class, EmptyEvaluator.class)
        .put(RegexPredicate.class, RegexEvaluator.class)
        .put(StringLengthPredicate.class, StringLengthEvaluator.class)
        .put(NumericRangePredicate.class, NumericRangeEvaluator.class)
        .unmodifiableMap();

    public static <PT extends Predicate> Evaluator<PT> makeEvaluator( final PT predicate ) {
        @SuppressWarnings({ "unchecked" })
        final Class<? extends Evaluator<PT>> evalClass = (Class<? extends Evaluator<PT>>) evaluatorMap.get(predicate.getClass());
        if (evalClass == null) throw new IllegalArgumentException("No Evaluator class registered for " + predicate.getClass().getName());
        try {
            // TODO caching?
            Constructor<? extends Evaluator<PT>> ctor = evalClass.getConstructor(predicate.getClass());
            return ctor.newInstance(predicate);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No suitable constructor found in " + evalClass.getName());
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("No suitable constructor found in " + evalClass.getName());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("No suitable constructor found in " + evalClass.getName());
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("No suitable constructor found in " + evalClass.getName());
        }
    }
}
