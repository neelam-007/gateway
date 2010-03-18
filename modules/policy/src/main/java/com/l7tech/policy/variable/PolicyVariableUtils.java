/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.util.ExceptionUtils;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public final class PolicyVariableUtils {
    private static final Logger logger = Logger.getLogger(PolicyVariableUtils.class.getName());

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
        return getVariablesSetByPredecessors(assertion, CurrentAssertionTranslator.get(), true);
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
        return getVariablesSetByPredecessors(assertion, CurrentAssertionTranslator.get(), false);
    }

    /**
     * Get the variables (that may be) set before this assertions runs. Optionally include variables set by the
     * supplied assertion.
     * <p/>
     * The returned Map keys are in the correct case, and the Map is case insensitive.
     *
     * @param assertion           Assertion to collect predecessor variables for
     * @param assertionTranslator AssertionTranslator used to dereference include assertions to obtain the included assertions
     * @param includeSelf         boolean if true, then the variables set by the current assertion will be included in the returned
     *                            variables
     * @return a map of all variables found in the policy up to and including the current assertion (if includedSelf is true)
     *         Never null.
     */
    public static Map<String, VariableMetadata> getVariablesSetByPredecessors(
            final Assertion assertion,
            final AssertionTranslator assertionTranslator,
            final boolean includeSelf)
    {

        Map<String, VariableMetadata> vars = new TreeMap<String, VariableMetadata>(String.CASE_INSENSITIVE_ORDER);

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
                        while (newPreorderIterator.hasNext()) {
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
     * logs it, and returns an empty array.
     *
     * @param sv the SetsVariables to query.  Required.
     * @return the variables set, or an empty array if VariableNameSyntaxExeption was thrown.
     */
    public static VariableMetadata[] getVariablesSetNoThrow(SetsVariables sv) {
        try {
            return sv.getVariablesSet();
        } catch (VariableNameSyntaxException vnse) {
            logger.log(Level.WARNING, "Unable to get variables set on " + sv.getClass().getName() + ": " + ExceptionUtils.getMessage(vnse));
            return new VariableMetadata[0];
        }
    }


    private PolicyVariableUtils() { }
}
