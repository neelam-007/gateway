package com.l7tech.external.assertions.comparison.server;

import com.l7tech.external.assertions.comparison.MultivaluedComparison;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.external.assertions.comparison.BinaryPredicate;
import com.l7tech.external.assertions.comparison.ComparisonAssertion;
import com.l7tech.external.assertions.comparison.DataTypePredicate;
import com.l7tech.external.assertions.comparison.Predicate;
import com.l7tech.external.assertions.comparison.server.convert.ConversionException;
import com.l7tech.external.assertions.comparison.server.convert.ValueConverter;
import com.l7tech.external.assertions.comparison.server.evaluate.Evaluator;
import com.l7tech.policy.variable.DataType;
import com.l7tech.util.ComparisonOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Holds the state kept while evaluating a single request through the {@link ServerComparisonAssertion}.
 *
 * @author alex
*/
abstract class State<T> {
    protected final Map<Predicate, Evaluator> evaluators;
    protected final Audit auditor;
    protected final Map<String, Object> vars;
    @NotNull
    protected T value;

    /**
     * The {@link DataType} of any {@link DataTypePredicate} that has been observed so far in the evaluation.
     * Note that this basically requires that any {@link DataTypePredicate} must come first in the
     * {@link ComparisonAssertion#getPredicates()} array. 
     */
    protected DataType type;

    /**
     * True iff all predicates processed thus far have evaluated to true.
     */
    protected boolean assertionResult = true;

    public boolean getAssertionResult() {
        return assertionResult;
    }

    protected State( final Map<Predicate, Evaluator> evaluators,
                     final Map<String, Object> vars,
                     final Audit auditor ) {
        this.evaluators = evaluators;
        this.vars = vars;
        this.auditor = auditor;
    }

    @NotNull
    final T getValue() {
        return value;
    }

    protected abstract void evaluate(Predicate pred);

    protected static State make( final Map<Predicate, Evaluator> evaluators,
                                 final MultivaluedComparison multivaluedComparison,
                                 @NotNull final Object left,
                                 final Map<String, Object> vars,
                                 final Audit auditor ) {
        final Class leftClass = left.getClass();
        if (leftClass.isArray()) {
            return new MVState(evaluators, multivaluedComparison, (Object[])left, vars, auditor);
        } else if (Collection.class.isAssignableFrom(leftClass)) {
            final Collection<?> leftCollection = (Collection<?>) left;
            return new MVState(evaluators, multivaluedComparison, leftCollection.toArray( new Object[leftCollection.size()] ), vars, auditor);
        } else {
            return new SVState(evaluators, left, vars, auditor);
        }
    }

    @Nullable
    protected Object convertValue(@NotNull Object value, DataType type) {
        return convertValue(value, type, null);
    }

    /**
     * @param value the value to be converted; must not be null.
     * @param type the DataType value should be converted into. The actual type may be any valid value from the type's
     * valueClasses property.
     * @param requiredClass when value represents a 'right' value e.g. when the type of 'left' is known, then
     * requiredClass should not be null and represents the type required of 'right', which is stored in value. This
     * is an issue as the DataType declares more than one valueClass, where the types defined are not logically the same
     * e.g. Boolean defines both the boxed and primitive types, which will always be comparable at runtime, whereas
     * Binary and Date define unrelated value classes.
     *
     * @return the converted object or null if an error occurs
     */
    @Nullable
    protected Object convertValue(@NotNull Object value, DataType type, @Nullable Comparable requiredClass) {
        for (Class clazz : type.getValueClasses()) {
            if (clazz.isAssignableFrom(value.getClass())) {
                if (clazz.isAssignableFrom(value.getClass())
                        && (requiredClass == null || requiredClass.getClass().isAssignableFrom(value.getClass()))) {
                    // no conversion required
                    return value;
                }
            }
        }

        auditor.logAndAudit(AssertionMessages.COMPARISON_CONVERTING, value.getClass().getName(), type.getShortName());
        ValueConverter conv = ValueConverter.Factory.getConverter(type, requiredClass);
        try {
            return conv.convert(value);
        } catch (ConversionException e) {
            auditor.logAndAudit(AssertionMessages.COMPARISON_CANT_CONVERT, value.getClass().getSimpleName(), type.getName());
            return null;
        }
    }

    /**
     * @param value the value to be converted; must not be null.
     * @param typeExample an example object with the desired result type.
     * @return the converted object or null if an error occurs
     */
    protected Object convertValue( final Object value,
                                   final Comparable typeExample ) {
        if (value == null) throw new NullPointerException();

        final ValueConverter converter = ValueConverter.Factory.getConverterOrHelperConverter(typeExample, null);
        if ( converter != null ) {
            auditor.logAndAudit(AssertionMessages.COMPARISON_CONVERTING, value.getClass().getName(), converter.getDataType().getShortName());

            try {
                return converter.convert(value);
            } catch (ConversionException e) {
                auditor.logAndAudit(AssertionMessages.COMPARISON_CANT_CONVERT, value.getClass().getSimpleName(), converter.getDataType().getName());
                return null;
            }
        } else {
            return value; // conversion not possible or not necessary
        }
    }

    protected boolean evalBinary(final Object left, final BinaryPredicate bpred, final Map<String, Object> vars) {
        Comparable cleft;
        if (left instanceof Comparable) {
            cleft = (Comparable)left;
        } else {
            auditor.logAndAudit(AssertionMessages.COMPARISON_NOT_COMPARABLE, left.getClass().getSimpleName(), bpred.toString());
            cleft = left.toString();
        }

        Comparable cright;
        if (bpred.getOperator().isUnary()) {
            cright = null;
        } else {
            Object right = ServerComparisonAssertion.getValue(bpred.getRightValue(), vars, auditor);
            if (right != null) {
                DataType type = this.type;
                if ( type == null ) {
                    right = convertValue(right, cleft);
                } else {
                    right = convertValue(right, type, cleft);
                }
            }
            if (right instanceof Comparable || right == null) {
                cright = (Comparable) right;
            } else {
                auditor.logAndAudit(AssertionMessages.COMPARISON_NOT_COMPARABLE, left.getClass().getSimpleName(), bpred.toString());
                cright = right.toString();
            }
        }
        try {
            return bpred.getOperator().compare(cleft, cright, !bpred.isCaseSensitive());
        } catch (ComparisonOperator.NotComparableException e) {
            // coding error
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, e.getMessage());
            return false;
        }
    }

}
