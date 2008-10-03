/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.util.*;

/**
 * @author alex
 */
public final class PolicyVariableUtils {
    public static Map<String, VariableMetadata> getVariablesSetByPredecessorsAndSelf(Assertion assertion) {
        Assertion ancestor = assertion.getPath()[0];
        Map<String, VariableMetadata> vars = new TreeMap<String, VariableMetadata>(String.CASE_INSENSITIVE_ORDER);
        for (Iterator i = ancestor.preorderIterator(); i.hasNext(); ) {
            Assertion ass = (Assertion) i.next();
            if (ass instanceof SetsVariables) {
                SetsVariables sv = (SetsVariables)ass;
                for (VariableMetadata meta : sv.getVariablesSet()) {
                    String name = meta.getName();
                    if (vars.containsKey(name)) {
                        vars.remove(name);  // So that case change in name will cause map key to be updated as well.
                    }
                    vars.put(name, meta);
                }
            }
            if (ass == assertion) break; // Can't use variables of any subsequent assertion
        }
        return vars;
    }

    public static Map<String, VariableMetadata> getVariablesSetByPredecessors(Assertion assertion) {
        Assertion ancestor = assertion.getPath()[0];
        Map<String, VariableMetadata> vars = new TreeMap<String, VariableMetadata>(String.CASE_INSENSITIVE_ORDER);
        for (Iterator i = ancestor.preorderIterator(); i.hasNext(); ) {
            Assertion ass = (Assertion) i.next();
            if (ass == assertion) break; // Can't use our own variables or those of any subsequent assertion
            if (ass.isEnabled() && ass instanceof SetsVariables) {
                SetsVariables sv = (SetsVariables)ass;
                for (VariableMetadata meta : sv.getVariablesSet()) {
                    String name = meta.getName();
                    if (vars.containsKey(name)) {
                        vars.remove(name);  // So that case change in name will cause map key to be updated as well.
                    }
                    vars.put(name, meta);
                }
            }
        }
        return vars;
    }

    public static Set<String> getVariablesUsedBySuccessors(Assertion assertion) {
        Set<String> vars = new HashSet<String>();

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
        return vars;
    }

    public static void simpleRecursiveGather(Assertion kid, Set<String> vars) {
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

    private PolicyVariableUtils() { }
}
