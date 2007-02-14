/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server.evaluate;

import com.l7tech.common.logic.*;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author alex
 */
public final class EvaluatorFactory {
    private static final Map<Class<? extends Predicate>, Class<? extends Evaluator>> evaluatorMap = new HashMap<Class<? extends Predicate>, Class<? extends Evaluator>>();
    static {
        // No evaluator for BinaryPredicates; they're handled specially in the ServerAssertion due to the need to 
        // interpolate variables into the RHS expression.
        evaluatorMap.put(CardinalityPredicate.class, CardinalityEvaluator.class);
        evaluatorMap.put(EmptyPredicate.class, EmptyEvaluator.class);
        evaluatorMap.put(RegexPredicate.class, RegexEvaluator.class);
        evaluatorMap.put(StringLengthPredicate.class, StringLengthEvaluator.class);
        evaluatorMap.put(NumericRangePredicate.class, NumericRangeEvaluator.class);
    }

    public static <PT extends Predicate> Evaluator<PT> makeEvaluator(PT predicate) {
        Class<? extends Evaluator> evalClass = evaluatorMap.get(predicate.getClass());
        if (evalClass == null) throw new IllegalArgumentException("No Evaluator class registered for " + predicate.getClass().getName());
        try {
            // TODO caching
            Constructor<? extends Evaluator> ctor = evalClass.getConstructor(predicate.getClass());
            //noinspection unchecked
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
