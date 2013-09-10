package com.l7tech.external.assertions.esm.console;

import com.l7tech.console.tree.policy.DefaultAssertionPolicyNode;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.esm.EsmSubscriptionAssertion;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author megery
 */
public class EsmSubscriptionAssertionPolicyNode extends DefaultAssertionPolicyNode<EsmSubscriptionAssertion> {
    private static final Logger logger = Logger.getLogger(EsmSubscriptionAssertionPolicyNode.class.getName());
    public EsmSubscriptionAssertionPolicyNode(EsmSubscriptionAssertion assertion) {
        super(assertion);
    }

    /**
     * Note: this getName is used only in the policy window. It overrides the default getName in
     * DefaultAssertionPolicyNode, which delegates to getName(boolean). The validator name of this assertion will come
     * from calling getName(false) which is implemented by super
     * @return the name to display in the policy window
     */
    @Override
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
            } catch (final PermissionDeniedException e) {
                logger.log(Level.WARNING, "User does not have permission to read policy", ExceptionUtils.getDebugException(e));
                polName = "Permission Denied Policy #" + policyGuid;
            }

        }
        String assertionName = assertion.meta().get(AssertionMetadata.SHORT_NAME).toString();
        return addCommentToDisplayText(assertion, MessageFormat.format(assertionName + " ({0})", polName));
    }

    @Override
    public boolean canDelete() {
        return true;
    }
}
