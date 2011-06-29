package com.l7tech.external.assertions.comparison.server;

import com.l7tech.external.assertions.comparison.BinaryPredicate;
import com.l7tech.external.assertions.comparison.DataTypePredicate;
import com.l7tech.external.assertions.comparison.MultivaluedComparison;
import com.l7tech.external.assertions.comparison.Predicate;
import com.l7tech.external.assertions.comparison.server.evaluate.Evaluator;
import com.l7tech.external.assertions.comparison.server.evaluate.MultiValuedEvaluator;
import com.l7tech.external.assertions.comparison.server.evaluate.SingleValuedEvaluator;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.util.Option;
import com.l7tech.util.Pair;

import java.util.Map;
import java.util.Arrays;

/**
 * Manages the progress of a <b>multivalued value</b> through {@link ServerComparisonAssertion}.
 *
 * @author alex
*/
class MVState extends State<Object[]> {
    private final MultivaluedComparison multivaluedComparison;

    MVState( final Map<Predicate, Evaluator> evaluators,
             final MultivaluedComparison multivaluedComparison,
             final Object[] values,
             final Map<String, Object> vars,
             final Audit auditor) {
        super(evaluators, vars, auditor);
        this.multivaluedComparison = multivaluedComparison;
        this.value = values;
    }

    @Override
    protected void evaluate( final Predicate pred ) {
        final Evaluator eval = evaluators.get(pred);
        boolean predResult;
        if (pred instanceof DataTypePredicate) {
            evaluateDataType( (DataTypePredicate) pred );
            return;
        } else if (eval instanceof MultiValuedEvaluator) {
            predResult = ((MultiValuedEvaluator) eval).evaluate(value);
        } else if (eval instanceof SingleValuedEvaluator || pred instanceof BinaryPredicate) {
            Option<Boolean> result = evaluateValues( pred, eval );
            if ( !result.isSome() ) {
                assertionResult = false;
                return;
            }
            predResult = result.some();
        } else {
            throw new IllegalStateException("Unable to evaluate predicate " + pred + " against values " + Arrays.toString(value));
        }

        if (pred.isNegated()) predResult = !predResult;
        assertionResult &= predResult;
    }

    private void evaluateDataType( final DataTypePredicate pred ) {
        if (this.type != null) throw new IllegalStateException("DataType already set");
        this.type = pred.getType();

        final Option<Pair<Boolean,Object[]>> inputs = getEvaluationInput();
        if ( !inputs.isSome() ) {
            assertionResult = false;
            return;
        }

        final boolean atLeastOne = inputs.some().left;
        final Object[] sourceValues = inputs.some().right;

        // Try to convert all the values; fail without mutating if any one cannot be converted
        boolean sawSuccess = false;
        final Object[] newvals = new Object[sourceValues.length];
        for ( int i = 0; i < sourceValues.length; i++ ) {
            final Object value = sourceValues[i];
            final Object newval = value == null ? null : convertValue(value, type);
            if ( newval == null ) {
                if ( !atLeastOne ) {
                    // Unable to convert this value to the desired type
                    assertionResult = false;
                    return;
                }
            } else {
                sawSuccess = true;
            }
            newvals[i] = newval;
        }

        if ( atLeastOne && !sawSuccess ) {
            assertionResult = false;
            return;
        }

        // mutate so subsequent predicates see converted value
        final Object[] updatedValues = new Object[value.length];
        if ( multivaluedComparison == MultivaluedComparison.LAST ) {
            System.arraycopy( value, 0, updatedValues, 0, updatedValues.length-1 );
            updatedValues[updatedValues.length-1] = newvals[0];
        } else {
            for ( int i = 0; i < value.length; i++ ) {
                if ( i < newvals.length && newvals[i] != null ) {
                    updatedValues[i] = newvals[i];
                } else {
                    updatedValues[i] = value[i];
                }
            }
        }
        value = updatedValues;
    }

    /**
     * Evaluates to true or false, none on evaluation failure
     */
    private Option<Boolean> evaluateValues( final Predicate pred, final Evaluator eval ) {
        final boolean predResult;
        final Option<Pair<Boolean,Object[]>> inputs = getEvaluationInput();
        if ( !inputs.isSome() ) {
            return Option.none();
        }

        final boolean atLeastOne = inputs.some().left;
        final Object[] sourceValues = inputs.some().right;

        // Left is multivalued, must split into single values for this predicate
        boolean tempResult = !atLeastOne;
        for ( final Object value : sourceValues ) {
            boolean evalResult;
            if (pred instanceof BinaryPredicate ) {
                evalResult = evalBinary(value, (BinaryPredicate)pred, vars);
            } else if (eval instanceof SingleValuedEvaluator ) {
                evalResult = ((SingleValuedEvaluator) eval).evaluate(value);
            } else {
                return Option.none();
            }

            tempResult = atLeastOne ?
                    tempResult || evalResult :
                    evalResult;

            if ( !atLeastOne && !tempResult ) break;
        }

        predResult = tempResult;
        return Option.some(predResult);
    }

    private Option<Pair<Boolean,Object[]>> getEvaluationInput() {
        boolean atLeastOne = false;
        final Object[] sourceValues;
        switch ( multivaluedComparison ) {
            case ALL:
                sourceValues = value;
                break;
            case ANY:
                atLeastOne = true;
                sourceValues = value;
                break;
            case FIRST:
                sourceValues = Arrays.copyOf(value, 1);
                break;
            case LAST:
                sourceValues = Arrays.copyOfRange(value, value.length-1, value.length);
                break;
            default:
                return Option.none();
        }

        return Option.some( new Pair<Boolean,Object[]>( atLeastOne, sourceValues ) );
    }
}
