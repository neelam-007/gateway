package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Support for implemenations of UsesVariables and SetsVariables.
 */
public class VariableUseSupport {

    /**
     * Create a VariablesUsed with the given initial expressions.
     *
     * <p>This is for supporting direct implemenation of the UsesVariables
     * interface.</p>
     *
     * @param expressions The initial expressions
     * @return The new VariablesUsed
     * @see UsesVariables
     */
    public static VariablesUsed expressions( final String... expressions ) {
        return new VariablesUsed().withExpressions( expressions );
    }

    /**
     * Create a VariablesUsed with the given initial variables.
     *
     * <p>This is for supporting direct implemenation of the UsesVariables
     * interface.</p>
     *
     * @param variables The initial variables
     * @return The new VariablesUsed
     * @see UsesVariables
     */
    public static VariablesUsed variables( final String... variables ) {
        return new VariablesUsed().withVariables( variables );
    }

    /**
     * Create a VariablesSet with the given initial variable metadata.
     *
     * <p>This is for supporting direct implemenation of the SetsVariables
     * interface.</p>
     *
     * @param variables The initial variable metadata
     * @return The new VariablesSet
     * @see SetsVariables
     */
    public static VariablesSet variables( final VariableMetadata... variables ) {
        return new VariablesSet().withVariables( variables );
    }

    /**
     * Aggregates variables used.
     */
    public static final class VariablesUsed extends VariablesUsedSupport<VariablesUsed> {
        protected VariablesUsed(){
        }

        @Override
        protected VariablesUsed get() {
            return this;
        }
    }

    /**
     * Aggregates variables set.
     */
    public static final class VariablesSet extends VariablesSetSupport<VariablesSet> {
        protected VariablesSet() {
        }

        @Override
        protected VariablesSet get() {
            return this;
        }
    }

    /**
     * Support class for aggregation of variables used.
     *
     * <p>This is for use when variable usage is across a class hierarchy. To
     * use the superclass that implements UsesVariables should have a final
     * implementation of <code>getVariablesUsed</code> and a protected method
     * that returns a subclass of VariablesUsedSupport with a private
     * constructor.</p>
     *
     * <p>This ensures subclasses call the superclass method to aggregate
     * variable usage.</p>
     *
     * @param <VUS> The VariablesUsedSupport concrete subclass
     */
    public abstract static class VariablesUsedSupport<VUS extends VariablesUsedSupport<VUS>> {
        private final Set<String> variablesUsed = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        protected VariablesUsedSupport() {
        }

        protected VariablesUsedSupport( final String[] initialVariables ) {
            addVariables( initialVariables );
        }

        /**
         * Add expressions for variables used.
         *
         * <p>Variables will be extracted from the given expressions, which may
         * be null.</p>
         *
         * @param expressions The expressions to add (may be null)
         */
        public final void addExpressions( final String... expressions ) {
            if ( expressions != null ) addExpressions( Arrays.asList( expressions ) );
        }

        /**
         * Add expressions for variables used.
         *
         * <p>Variables will be extracted from the given expressions, which may
         * be null.</p>
         *
         * @param expressions The expressions to add (may be null)
         */
        public final void addExpressions( final Iterable<String> expressions ) {
            if ( expressions != null ) {
                for ( final String expression : expressions ) {
                    addVariables( Syntax.getReferencedNames( expression ) );
                }
            }
        }

        /**
         * Add variables for variables used.
         *
         * <p>Any given non-null variables will be added.</p>
         *
         * @param variables The variables to add (may be null)
         */
        public final void addVariables( final String... variables ) {
            if (variables != null) addVariables( Arrays.asList( variables ) );
        }

        /**
         * Add variables for variables used.
         *
         * <p>Any given non-null variables will be added.</p>
         *
         * @param variables The variables to add (may be null)
         */
        public final void addVariables( final Iterable<String> variables ) {
            if ( variables != null ) {
                for ( final String variable : variables ) {
                    if ( variable != null && !variable.isEmpty()) variablesUsed.add( variable );
                }
            }
        }

        /**
         * Add variables to the variables used.
         *
         * @param other The other VariablesUsedSupport implementation.
         */
        public final void add( final VariablesUsedSupport<?> other ) {
            if ( other != null ) {
                variablesUsed.addAll( other.variablesUsed );
            }
        }

        /**
         * Add expressions for variables used.
         *
         * <p>Variables will be extracted from the given expressions, which may
         * be null.</p>
         *
         * @param expressions The expressions to add (may be null)
         * @return This
         */
        public final VUS withExpressions( final String... expressions ) {
            addExpressions( expressions );
            return get();
        }

        /**
         * Add expressions for variables used.
         *
         * <p>Variables will be extracted from the given expressions, which may
         * be null.</p>
         *
         * @param expressions The expressions to add (may be null)
         * @return This
         */
        public final VUS withExpressions( final Iterable<String> expressions ) {
            addExpressions( expressions );
            return get();
        }

        /**
         * Add variables for variables used.
         *
         * <p>Any given non-null variables will be added.</p>
         *
         * @param variables The variables to add (may be null)
         * @return This
         */
        public final VUS withVariables( final String... variables ) {
            addVariables( variables );
            return get();
        }

        /**
         * Add variables for variables used.
         *
         * <p>Any given non-null variables will be added.</p>
         *
         * @param variables The variables to add (may be null)
         * @return This
         */
        public final VUS withVariables( final Iterable<String> variables ) {
            addVariables( variables );
            return get();
        }

        /**
         * Add variables from the given VariablesUsedSupport.
         *
         * @param other The other VariablesUsedSupport implementation
         * @return This
         */
        public final VUS with( final VariablesUsedSupport<?> other ) {
            add( other );
            return get();
        }

        /**
         * Convert the used variables to an array.
         *
         * @return The array of variables used
         */
        public final String[] asArray() {
            return variablesUsed.toArray( new String[variablesUsed.size()] );
        }

        protected abstract VUS get();
    }

    /**
     * Support class for aggregation of variables set.
     *
     * <p>This is for use when variables are set across a class hierarchy. To
     * use the superclass that implements SetsVariables should have a final
     * implementation of <code>getVariablesSet</code> and a protected method
     * that returns a subclass of VariablesUsedSupport with a private
     * constructor.</p>
     *
     * <p>This ensures subclasses call the superclass method to aggregate
     * variables set.</p>
     *
     * @param <VSS> The VariablesSetSupport concrete subclass
     */
    public static abstract class VariablesSetSupport<VSS extends VariablesSetSupport<VSS>> {
        private final List<VariableMetadata> variablesSet = new ArrayList<VariableMetadata>();

        protected VariablesSetSupport() {
        }

        protected VariablesSetSupport( final VariableMetadata[] initialVariables ) {
            addVariables( initialVariables );
        }

        /**
         * Add variable metadata for variables set.
         *
         * <p>Any given non-null variable metadata will be added.</p>
         *
         * @param variables The variable metadata to add (may be null)
         */
        public final void addVariables( final VariableMetadata... variables ) {
            if ( variables != null ) {
                for ( final VariableMetadata variableMetadata : variables ) {
                    if ( variableMetadata != null ) variablesSet.add( variableMetadata );
                }
            }
        }

        /**
         * Add variable metadata from the given VariablesSetSupport.
         *
         * @param other The other VariablesSetSupport implementation
         */
        public final void add( final VariablesSetSupport<?> other ) {
            if ( other != null ) {
                variablesSet.addAll( other.variablesSet );
            }
        }

        /**
         * Add variable metadata for variables set.
         *
         * <p>Any given non-null variable metadata will be added.</p>
         *
         * @param variables The variable metadata to add (may be null)
         * @return This
         */
        public final VSS withVariables( final VariableMetadata... variables ) {
            addVariables( variables );
            return get();
        }

        /**
         * Add variable metadata from the given VariablesSetSupport.
         *
         * @param other The other VariablesSetSupport implementation
         * @return This
         */
        public final VSS with( final VariablesSetSupport<?> other ) {
            add( other );
            return get();
        }

        /**
         * Convert the used variables to an array.
         *
         * @return The array of variable metadata
         */
        public final VariableMetadata[] asArray() {
            return variablesSet.toArray( new VariableMetadata[variablesSet.size()] );
        }

        protected abstract VSS get();
    }
}
