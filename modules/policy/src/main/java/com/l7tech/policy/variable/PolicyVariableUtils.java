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
        try {
            return getVariablesSetByPredecessors(assertion, null, true);
        } catch (PolicyAssertionException e) {
            //can't happen
            throw new IllegalStateException("PolicyAssertionException should not be thrown");
        }
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
        try {
            return getVariablesSetByPredecessors(assertion, null, false);
        } catch (PolicyAssertionException e) {
            //can't happen
            throw new IllegalStateException("PolicyAssertionException should not be thrown");
        }
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
     * @throws PolicyAssertionException thrown if an include assertion cannot be dereferenced. Will never be thrown when
     *                                  assertionTranslator is null
     */
    public static Map<String, VariableMetadata> getVariablesSetByPredecessors(
            final Assertion assertion,
            final AssertionTranslator assertionTranslator,
            final boolean includeSelf) throws PolicyAssertionException {

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
                    Assertion translated = assertionTranslator.translate(ass);
                    assertionTranslator.translationFinished(ass);
                    NewPreorderIterator newPreorderIterator = new NewPreorderIterator(translated, assertionTranslator);

                    while (newPreorderIterator.hasNext()) {
                        Assertion includedAssertion = newPreorderIterator.next();
                        if(includedAssertion.isEnabled() && includedAssertion instanceof SetsVariables){
                            collectVariables((SetsVariables)includedAssertion, vars);    
                        }
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
                        simpleRecursiveGather(kid, vars);
                    }
                }
                assertion = assertion.getParent();
                parent = assertion.getParent();
            }
        }
        return vars;
    }

    private static void simpleRecursiveGather(Assertion kid, Set<String> vars) {
        if (!kid.isEnabled())
            return;
        if (kid instanceof CompositeAssertion) {
            CompositeAssertion compositeAssertion = (CompositeAssertion) kid;
            for (Iterator i = compositeAssertion.getChildren().iterator(); i.hasNext();) {
                Assertion newKid = (Assertion) i.next();
                simpleRecursiveGather(newKid, vars);
            }
        } else if (kid instanceof UsesVariables) {
            UsesVariables usesVariables = (UsesVariables) kid;
            vars.addAll(Arrays.asList(usesVariables.getVariablesUsed()));
        }
    }

    /**
     * Wraps SetsVariables.getVariablesSet() with a version that can optionally catch the VariableNameSyntaxException
     * and replace it with an empty VariableMetadata array.
     *
     * @param sv the SetsVariables to query.  Required.
     * @param ignoreNameSyntaxExceptions if false, this method will behave excactly the same as SetsVariables.getVariablesSet().
     *                                   If true, this method will catch any VariableNameSyntaxException and translate it into an empty metadata array.
     * @return the variables set by this object.  May be empty, but should not normally be null.
     * @throws VariableNameSyntaxException if ignoreNameSyntaxExceptions was false and VariableNameSyntaxException was thrown.
     */
    public static VariableMetadata[] getVariablesSet(SetsVariables sv, boolean ignoreNameSyntaxExceptions) throws VariableNameSyntaxException {
        return ignoreNameSyntaxExceptions ? getVariablesSetNoThrow(sv) : sv.getVariablesSet();
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
