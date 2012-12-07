package com.l7tech.console.tree.policy;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.util.ExceptionUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Policy node for encapsulated assertion.
 */
public class EncapsulatedAssertionPolicyNode extends DefaultAssertionPolicyNode<EncapsulatedAssertion> {
    private static final Logger logger = Logger.getLogger(EncapsulatedAssertionPolicyNode.class.getName());

    public EncapsulatedAssertionPolicyNode(EncapsulatedAssertion assertion) {
        super(loadConfig(assertion));
    }

    private static EncapsulatedAssertion loadConfig(EncapsulatedAssertion assertion) {
        if (assertion.config() == null) {
            String oid = assertion.getEncapsulatedAssertionConfigId();
            try {
                EncapsulatedAssertionConfig config = Registry.getDefault().getEncapsulatedAssertionAdmin().findByPrimaryKey(Long.parseLong(oid));
                assertion.config(config);
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to find encapsulated assertion config: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Invalid encapsulated assertion config ID: " + oid, ExceptionUtils.getDebugException(e));
            }
        }
        return assertion;
    }
}
