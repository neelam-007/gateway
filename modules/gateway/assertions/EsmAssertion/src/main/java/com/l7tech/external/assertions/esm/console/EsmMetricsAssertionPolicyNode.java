package com.l7tech.external.assertions.esm.console;

import com.l7tech.console.tree.policy.DefaultAssertionPolicyNode;
import com.l7tech.external.assertions.esm.EsmMetricsAssertion;

public class EsmMetricsAssertionPolicyNode extends DefaultAssertionPolicyNode<EsmMetricsAssertion> {
    public EsmMetricsAssertionPolicyNode(EsmMetricsAssertion assertion) {
        super(assertion);
    }

    @Override
    public boolean canDelete() {
        return true;
    }
}
