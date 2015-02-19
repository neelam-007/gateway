package com.l7tech.console.policy;

import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.IncludeAssertionDereferenceTranslator;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionTranslator;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 */
public class SsmPolicyVariableUtils {
    private static final Logger logger = Logger.getLogger(PolicyVariableUtils.class.getName());

    /**
     * Get the variables (that may be) set before this assertions runs including those the supplied assertion sets.
     *
     * <p>The returned Map keys are in the correct case, and the Map is case
     * insensitive.</p>
     *
     * <p>Will use the managerPolicyCache to translate Include assertions.</p>
     *
     * @param assertion The assertion to process.
     * @return The Map of names to VariableMetadata, may be empty but never null.
     * @see com.l7tech.policy.variable.VariableMetadata
     */
    public static Map<String, VariableMetadata> getVariablesSetByPredecessorsAndSelf( final Assertion assertion ) {
        return PolicyVariableUtils.getVariablesSetByPredecessors(assertion, getSsmAssertionTranslator(), getCurrentInterfaceDesc(), true);
    }

    /**
     * Get the variables (that may be) set before this assertions runs.
     *
     * <p>The returned Map keys are in the correct case, and the Map is case
     * insensitive.</p>
     *
     * <p>Will use the managerPolicyCache to translate Include assertions.</p>
     *
     * @param assertion The assertion to process.
     * @return The Map of names to VariableMetadata, may be empty but never null.
     * @see VariableMetadata
     */
    public static Map<String, VariableMetadata> getVariablesSetByPredecessors( final Assertion assertion ) {
        return PolicyVariableUtils.getVariablesSetByPredecessors(assertion, getSsmAssertionTranslator(), getCurrentInterfaceDesc(), false);
    }

    private static EncapsulatedAssertionConfig getCurrentInterfaceDesc() {
        EncapsulatedAssertionConfig ret = null;

        PolicyEditorPanel pep = TopComponents.getInstance().getPolicyEditorPanel();
        if ( null != pep ) {
            EntityWithPolicyNode node = pep.getPolicyNode();
            if ( null != node ) {
                ret = node.getInterfaceDescription();
            }
        }

        return ret;
    }

    /**
     * Get the variables that are known to be used by successor assertions.
     * <p/>
     * Will use the managerPolicyCache to translate Include assertions.
     *
     * @param assertion The assertion to process.
     * @return The Set of variables names (case insensitive), may be empty but never null.
     */
    public static Set<String> getVariablesUsedBySuccessors( Assertion assertion ) {
        return PolicyVariableUtils.getVariablesUsedBySuccessors(assertion, getSsmAssertionTranslator());
    }

    public static AssertionTranslator getSsmAssertionTranslator() {
        return new IncludeAssertionDereferenceTranslator(Registry.getDefault().getPolicyFinder());
    }

    private SsmPolicyVariableUtils() { }
}
