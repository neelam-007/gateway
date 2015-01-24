/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public final class PolicyVariableUtils {
    private static final Logger logger = Logger.getLogger(PolicyVariableUtils.class.getName());

    /**
     * Get the variables that may be set by this assertion itself, including any of its children (if composite)
     * or include target (if Include, and a CurrentAssertionTranslator is available).
     * <p/>
     * This includes only descendants and does not include predecessor or successor assertions.
     *
     * @param assertion the assertion to examine, which might notably be one or more of SetsVariables, CompositeAssertion, or Include.  May be null.
     * @return The Map of names to VariableMetadata, may be empty but never null.
     */
    public static Map<String, VariableMetadata> getVariablesSetByDescendantsAndSelf( final Assertion assertion ) {
        return getVariablesSetByDescendantsAndSelf(assertion, CurrentAssertionTranslator.get());
    }

    /**
     * Get the variables that may be set by this assertion itself, including any of its children (if composite)
     * or include target (if Include, and a non-null assertionTranslator is provided).
     *
     * @param assertion the assertion to examine, which might notably be one or more of SetsVariable, CompositeAssertion, or Include.  May be null.
     * @param assertionTranslator the assertion translator to use to process Include assertions within this assertion.  May be null.
     * @return the variable metadata for this assertion and all of its descendants (if any).
     */
    public static Map<String, VariableMetadata> getVariablesSetByDescendantsAndSelf( final Assertion assertion, AssertionTranslator assertionTranslator ) {
        final Map<String, VariableMetadata> vars = new TreeMap<String, VariableMetadata>(String.CASE_INSENSITIVE_ORDER);
        collectRecursive(assertion, assertionTranslator, new Functions.UnaryVoid<Assertion>() {
            @Override
            public void call(Assertion assertion) {
                if (assertion instanceof SetsVariables) {
                    final VariableMetadata[] metadataSet = ((SetsVariables) assertion).getVariablesSet();
                    for (VariableMetadata metadata : metadataSet) {
                        vars.put(metadata.getName(), metadata);
                    }
                }
            }
        });
        return vars;
    }

    /**
     * Get the variables that may be used by this assertion itself, including any of its children (if composite)
     * or include target (if Include, and a CurrentAssertionTranslator is available).
     *
     * @param assertion the assertion to examine, which might notably be one or more of SetsVariable, CompositeAssertion, or Include.  May be null.
     * @return the variables used by this assertion and all of its descendants (if any).
     */
    public static String[] getVariablesUsedByDescendantsAndSelf( final Assertion assertion ) {
        return getVariablesUsedByDescendantsAndSelf(assertion, CurrentAssertionTranslator.get());
    }

    /**
     * Get the variables that may be used by this assertion itself, including any of its children (if composite)
     * or include target (if Include, and a non-null assertionTranslator is provided).
     *
     * @param assertion the assertion to examine, which might notably be one or more of SetsVariable, CompositeAssertion, or Include.  May be null.
     * @param assertionTranslator the assertion translator to use to process Include assertions within this assertion.  May be null.
     * @return the variables used by this assertion and all of its descendants (if any).
     */
    public static String[] getVariablesUsedByDescendantsAndSelf( final Assertion assertion, AssertionTranslator assertionTranslator ) {
        final Set<String> vars = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        collectRecursive(assertion, assertionTranslator, new Functions.UnaryVoid<Assertion>() {
            @Override
            public void call(Assertion assertion) {
                if (assertion instanceof UsesVariables) {
                    vars.addAll(Arrays.asList(((UsesVariables) assertion).getVariablesUsed()));
                }
            }
        });
        return vars.toArray(new String[vars.size()]);
    }

    private static void collectRecursive(Assertion assertion, AssertionTranslator assertionTranslator, Functions.UnaryVoid<Assertion> collector) {
        if (assertion == null || !assertion.isEnabled())
            return;

        collector.call(assertion);

        if (assertion instanceof CompositeAssertion) {
            CompositeAssertion compositeAssertion = (CompositeAssertion) assertion;
            for (Assertion kid : compositeAssertion.getChildren()) {
                collectRecursive(kid, assertionTranslator, collector);
            }
        }

        if (assertionTranslator != null && assertion instanceof Include) {
            try {
                Assertion translated = assertionTranslator.translate(assertion);
                if (translated != assertion)
                    collectRecursive(translated, assertionTranslator, collector);
            } catch (PolicyAssertionException e) {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "Error translating assertion: " + ExceptionUtils.getMessage(e), e);
            } finally {
                assertionTranslator.translationFinished(assertion);
            }
        }
    }

    /**
     * Get the variables (that may be) set before this assertions runs including those the supplied assertion sets.
     *
     * <p>The returned Map keys are in the correct case, and the Map is case
     * insensitive.</p>
     *
     * @param assertion The assertion to process.
     * @return The Map of names to VariableMetadata, may be empty but never null.
     * @see VariableMetadata
     */
    public static Map<String, VariableMetadata> getVariablesSetByPredecessorsAndSelf( final Assertion assertion ) {
        return getVariablesSetByPredecessors(assertion, CurrentAssertionTranslator.get(), CurrentInterfaceDescription.get(), true);
    }

    /**
     * Get the variables (that may be) set before this assertions runs.
     *
     * <p>The returned Map keys are in the correct case, and the Map is case
     * insensitive.</p>
     *
     * @param assertion The assertion to process.
     * @return The Map of names to VariableMetadata, may be empty but never null.
     * @see VariableMetadata
     */
    public static Map<String, VariableMetadata> getVariablesSetByPredecessors( final Assertion assertion ) {
        return getVariablesSetByPredecessors(assertion, CurrentAssertionTranslator.get(), CurrentInterfaceDescription.get(), false);
    }

    /**
     * Get the variables (that may be) set before this assertions runs. Optionally include variables set by the
     * supplied assertion.
     * <p/>
     * The returned Map keys are in the correct case, and the Map is case insensitive.
     *
     * @param assertion           Assertion to collect predecessor variables for
     * @param assertionTranslator AssertionTranslator used to dereference include assertions to obtain the included assertions
     * @param interfaceDescription interface description for current policy, or null
     * @param includeSelf         boolean if true, then the variables set by the current assertion will be included in the returned
     *                            variables
     * @return a map of all variables found in the policy up to and including the current assertion (if includedSelf is true)
     *         Never null.
     */
    @NotNull
    public static Map<String, VariableMetadata> getVariablesSetByPredecessors(
            final @Nullable Assertion assertion,
            final @Nullable AssertionTranslator assertionTranslator,
            final @Nullable EncapsulatedAssertionConfig interfaceDescription,
            final boolean includeSelf)
    {

        Map<String, VariableMetadata> vars = new TreeMap<String, VariableMetadata>(String.CASE_INSENSITIVE_ORDER);

        if ( null != interfaceDescription ) {
            for ( EncapsulatedAssertionArgumentDescriptor input : interfaceDescription.getArgumentDescriptors() ) {
                String name = input.getArgumentName().toLowerCase();
                VariableMetadata vm = new VariableMetadata( name, false, true, name, true, input.dataType() );
                vars.put( name, vm );
            }
        }

        if (assertion != null) {
            Assertion ancestor = assertion.getPath()[0];//get the root assertion, all paths have the same root

            for (Iterator i = ancestor.preorderIterator(); i.hasNext();) {
                Assertion ass = (Assertion) i.next();

                if (ass == assertion && !includeSelf)
                    break; // Can't use our own variables or those of any subsequent assertion

                if (ass.isEnabled() && ass instanceof SetsVariables) {
                    collectVariables((SetsVariables) ass, vars);
                } else if (ass.isEnabled() && ass instanceof Include && assertionTranslator != null) {
                    try {
                        Assertion translated = assertionTranslator.translate(ass);
                        NewPreorderIterator newPreorderIterator = new NewPreorderIterator(translated, assertionTranslator);
                        if (ass != translated) while (newPreorderIterator.hasNext()) {
                            Assertion includedAssertion = newPreorderIterator.next();
                            if(includedAssertion.isEnabled() && includedAssertion instanceof SetsVariables){
                                collectVariables((SetsVariables)includedAssertion, vars);
                            }
                        }
                    } catch (PolicyAssertionException e) {
                        if (logger.isLoggable(Level.FINE))
                            logger.log(Level.FINE, "Error translating assertion: " + ExceptionUtils.getMessage(e), e);
                    } finally {
                        assertionTranslator.translationFinished(ass);
                    }
                }

                if (ass == assertion) break; // Can't use variables of any subsequent assertion
            }
        }
       return vars;
    }

    private static void collectVariables(SetsVariables setsVariables, Map<String, VariableMetadata> vars){
        for (VariableMetadata meta : getVariablesSetNoThrow(setsVariables)) {
            String name = meta.getName();
            if (vars.containsKey(name)) {
                vars.remove(name);  // So that case change in name will cause map key to be updated as well.
            }
            vars.put(name, meta);
        }
    }

    /**
     * Find variables that are used by the specified policy without previously being set by the policy.
     *
     * @param root root assertion.  If null, this method returns an empty array.
     * @param assertionTranslator  Assertion translator for processing Include assertions.  Optional.
     * @return a list of all variables that are used by this policy without being earlier set by it.
     */
    public static String[] getVariablesUsedByPolicyButNotPreviouslySet(@Nullable Assertion root, @Nullable AssertionTranslator assertionTranslator) {
        final Map<String, VariableMetadata> varsSet = new TreeMap<String, VariableMetadata>(String.CASE_INSENSITIVE_ORDER);
        return getVariablesUsedButNotSet(varsSet, root, assertionTranslator);
    }

    private static String[] getVariablesUsedButNotSet(Map<String, VariableMetadata> varsSet, @Nullable Assertion root, @Nullable AssertionTranslator assertionTranslator) {
        if (root == null)
            return new String[0];

        Set<String> ret = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        for (Iterator<Assertion> i = root.preorderIterator(); i.hasNext();) {
            Assertion ass = i.next();

            if (!ass.isEnabled())
                continue;

            // If the assertion both uses and sets the same variable, we will assume that it uses the variable
            // before it sets it.
            if (ass instanceof UsesVariables) {
                String[] varsUsed = getVariablesUsedNoThrow((UsesVariables) ass);
                for (String varName : varsUsed) {
                    if (!isVariableSet(varName, varsSet))
                        ret.add(varName);
                }
            }

            if (ass instanceof SetsVariables) {
                collectVariables((SetsVariables)ass, varsSet);
            }

            if (ass instanceof Include && assertionTranslator != null) {
                try {
                    Assertion translated = assertionTranslator.translate(ass);
                    if (ass != translated) {
                        ret.addAll(Arrays.asList(getVariablesUsedButNotSet(varsSet, translated, assertionTranslator)));
                    }
                } catch (PolicyAssertionException e) {
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Error translating assertion: " + ExceptionUtils.getMessage(e), e);
                } finally {
                    assertionTranslator.translationFinished(ass);
                }
            }
        }

        return ret.toArray(new String[ret.size()]);
    }

    private static boolean isVariableSet(String varName, Map<String, VariableMetadata> varsSet) {
        // TODO check for prefix match with VariableMetadata where prefixed=true
        return varsSet.containsKey(varName);
    }


    /**
     * Find variables that are set by the specified policy without subsequently being used by the policy.
     *
     * @param root root assertion.  Required.
     * @param assertionTranslator Assertion translator for processing Include assertions.  Optional.
     * @return metadata for all variables that are set by this policy without subsequently being used by it.
     */
    public static Map<String, VariableMetadata> getVariablesSetByPolicyButNotSubsequentlyUsed(Assertion root, AssertionTranslator assertionTranslator) {
        final Map<String, VariableMetadata> varsSet = new TreeMap<String, VariableMetadata>(String.CASE_INSENSITIVE_ORDER);

        if (root == null)
            return varsSet;

        for (Iterator<Assertion> i = root.preorderIterator(); i.hasNext();) {
            Assertion ass = i.next();

            if (!ass.isEnabled())
                continue;

            if (ass instanceof UsesVariables) {
                String[] varsUsed = getVariablesUsedNoThrow((UsesVariables) ass);
                for (String varName : varsUsed) {
                    // Omit variables earlier set that are now being used
                    varsSet.remove(varName);
                }
            }

            if (ass instanceof SetsVariables) {
                collectVariables((SetsVariables)ass, varsSet);
            }

            if (ass instanceof Include && assertionTranslator != null) {
                try {
                    Assertion translated = assertionTranslator.translate(ass);
                    if (ass != translated) {
                        varsSet.putAll(getVariablesSetByPolicyButNotSubsequentlyUsed(translated, assertionTranslator));
                    }
                } catch (PolicyAssertionException e) {
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Error translating assertion: " + ExceptionUtils.getMessage(e), e);
                } finally {
                    assertionTranslator.translationFinished(ass);
                }
            }
        }

        return varsSet;
    }


    /**
     * Get the variables that are known to be used by successor assertions.
     *
     * @param assertion The assertion to process.
     * @return The Set of variables names (case insensitive), may be empty but never null.
     */
    public static Set<String> getVariablesUsedBySuccessors( Assertion assertion ) {
        return getVariablesUsedBySuccessors(assertion, CurrentAssertionTranslator.get());
    }

    /**
     * Get the variables that are known to be used by successor assertions, using the specified AssertionTranslator to resolve Include assertions.
     *
     * @param assertion The assertion to process.
     * @param assertionTranslator an AssertionTranslator to use, or null to avoid descending into Include assertions.
     * @return The Set of variables names (case insensitive), may be empty but never null.
     */
    public static Set<String> getVariablesUsedBySuccessors( Assertion assertion, AssertionTranslator assertionTranslator ) {
        Set<String> vars = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        if ( assertion != null ) {
            CompositeAssertion parent = assertion.getParent();
            while (parent != null) {
                boolean seenMe = false;
                for (Object o : parent.getChildren()) {
                    Assertion kid = (Assertion) o;
                    if (kid == assertion) {
                        seenMe = true;
                    } else if (seenMe) {
                        simpleRecursiveGather(kid, vars, assertionTranslator);
                    }
                }
                assertion = assertion.getParent();
                parent = assertion.getParent();
            }
        }
        return vars;
    }

    private static void simpleRecursiveGather(Assertion kid, Set<String> vars, AssertionTranslator assertionTranslator) {
        if (!kid.isEnabled())
            return;

        if (kid instanceof CompositeAssertion) {
            CompositeAssertion compositeAssertion = (CompositeAssertion) kid;
            for (Assertion newKid : compositeAssertion.getChildren()) {
                simpleRecursiveGather(newKid, vars, assertionTranslator);
            }
        }

        if (kid instanceof Include && assertionTranslator != null) {
            try {
                Assertion translated = assertionTranslator.translate(kid);
                NewPreorderIterator iterator = new NewPreorderIterator(translated, assertionTranslator);
                while (iterator.hasNext()) {
                    Assertion includedAssertion = iterator.next();
                    simpleRecursiveGather(includedAssertion, vars, assertionTranslator);
                }
            } catch (PolicyAssertionException e) {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "Error translating assertion: " + ExceptionUtils.getMessage(e), e);
            } finally {
                assertionTranslator.translationFinished(kid);
            }
        }

        if (kid instanceof UsesVariables) {
            UsesVariables usesVariables = (UsesVariables) kid;
            vars.addAll(Arrays.asList(usesVariables.getVariablesUsed()));
        }
    }

    /**
     * Wraps SetsVariables.getVariablesSet() with a version that catches VariableNameSyntaxException,
     * logs, and returns an empty array.
     *
     * @param sv the SetsVariables to query.  Required.
     * @return the variables set, or an empty array if an exception was thrown (never null)
     */
    public static VariableMetadata[] getVariablesSetNoThrow( final SetsVariables sv ) {
        try {
            final VariableMetadata[] set = sv.getVariablesSet();
            if ( set != null ) {
                return set;
            }
        } catch ( final VariableNameSyntaxException e ) {
            logger.log(Level.WARNING, "Unable to get variables set on " + sv.getClass().getName() + ": " + ExceptionUtils.getMessage(e));
        }

        return new VariableMetadata[0];
    }

    /**
     * Wraps UsesVariables.getVariablesUsed() with a version that catches VariableNameSyntaxException,
     * logs and returns an empty array.
     *
     * @param uv the UsesVariables to query. Required.
     * @return The variables used, or an empty array if an exception was thrown (never null)
     */
    public static String[] getVariablesUsedNoThrow( final UsesVariables uv ) {
        try {
            final String[] used = uv.getVariablesUsed();
            if ( used != null ) {
                return used;
            }
        } catch ( final VariableNameSyntaxException e ) {
            logger.log(Level.WARNING, "Unable to get variables used on " + uv.getClass().getName() + ": " + ExceptionUtils.getMessage(e));
        }

        return new String[0];
    }

    /**
     * Check to see if the assertion used any of the provided variables
     *
     * @param assertion The assertion to verify
     * @param variableNames A list of provided variables to verify
     * @return True when the assertion used the provided variable, otherwise False
     */
    public static boolean usesAnyVariable(final Assertion assertion, String... variableNames) {
        if (assertion != null && assertion instanceof UsesVariables && variableNames != null) {
            String[] used = getVariablesUsedNoThrow((UsesVariables) assertion);
            for (String s : used) {
                for(String variableName : variableNames) {
                    if (s.equals(variableName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private PolicyVariableUtils() { }
}
