/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.security.rbac.PermissionDeniedException;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.objectmodel.FindException;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Policy node for {@link Include} assertions.  Note that this class deliberately does not extend
 * {@link LeafAssertionTreeNode}.
 */
public class IncludeAssertionPolicyNode extends AssertionTreeNode<Include> {
    private static final Logger logger = Logger.getLogger(IncludeAssertionPolicyNode.class.getName());

    private Policy policy;
    private boolean permissionDenied;

    public IncludeAssertionPolicyNode(Include assertion) {
        super(assertion);
        setAllowsChildren(true);
    }

    @Override
    protected void loadChildren() {
        Policy policy = getPolicy();
        if (policy == null) return;
        try {
            Assertion ass = policy.getAssertion();
            if (!(ass instanceof CompositeAssertion))
                throw new RuntimeException(MessageFormat.format("Top-level assertion in included policy #{0} ({1}) is not a CompositeAssertion", policy.getOid(), policy.getName()));
            LoadChildrenStrategy strat = LoadChildrenStrategy.newStrategy(this);
            strat.loadChildren(this, (CompositeAssertion)ass);
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format("Couldn't parse included policy #{0} ({1}): {2}", policy.getOid(), policy.getName(), ExceptionUtils.getMessage(e)), e);
        }
    }

    @Override
    public String getName() {
        Policy policy = getPolicy();
        StringBuilder sb = new StringBuilder("Include: ");
        if (policy == null) {
            if ( permissionDenied ) {
                sb.append("Permission Denied");
            } else {
                sb.append("Deleted");
            }
            sb.append(" Policy #");
            sb.append(assertion.getPolicyOid());
            String name = assertion.getPolicyName();
            if (name != null) sb.append(" (").append(name).append(")");
        } else {
            sb.append(policy.getName());
        }
        return sb.toString();
    }

    private Policy getPolicy() {
        if (policy == null) {
            permissionDenied = false;
            try {
                policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(assertion.getPolicyOid());
            } catch ( PermissionDeniedException pde ) {
                logger.log(Level.WARNING, "Couldn't load included policy [permission denied]");                
                permissionDenied = true;
            } catch ( FindException e ) {
                logger.log(Level.WARNING, "Couldn't load included policy", e);
            }
        }
        return policy;
    }

    @Override
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/folder.gif";
    }

    @Override
    public boolean accept(AbstractTreeNode node) {
        // Can't drag into an Include (yet?)
        return false;
    }
}
