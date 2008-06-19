package com.l7tech.external.assertions.esm.console;

import com.l7tech.external.assertions.esm.EsmMetricsAssertion;
import com.l7tech.external.assertions.esm.EsmSubscriptionAssertion;
import com.l7tech.console.tree.policy.DefaultAssertionPolicyNode;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.common.policy.Policy;

import javax.swing.*;
import java.util.logging.Logger;
import java.text.MessageFormat;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: May 23, 2008
 * Time: 9:22:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class EsmSubscriptionAssertionPolicyNode extends DefaultAssertionPolicyNode<EsmSubscriptionAssertion> {
    private static final Logger logger = Logger.getLogger(EsmSubscriptionAssertionPolicyNode.class.getName());
    public EsmSubscriptionAssertionPolicyNode(EsmSubscriptionAssertion assertion) {
        super(assertion);
    }

    public String getName() {
        String policyGuid = assertion.getNotificationPolicyGuid();
        String polName = "no notification policy selected";
        if (StringUtils.isNotEmpty(policyGuid)) {
            try {
                Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(policyGuid);
                if ( policy != null) {
                    polName = "using notification policy '" + policy.getName() + "'";
                } else if ( assertion.retrieveFragmentPolicy() != null ) {
                    polName = "using notification policy '" + assertion.retrieveFragmentPolicy().getName() + "'";
                } else {
                    assertion.setNotificationPolicyGuid(null);
                }
            } catch (FindException e) {
                logger.warning("Could not find a policy with guid '" + policyGuid + "'");
            }
        }
        return MessageFormat.format("ESM Subscription Assertion ({0})",polName);
    }

    public boolean canDelete() {
        return true;
    }

    public Action[] getActions() {
        return super.getActions();
    }
}
