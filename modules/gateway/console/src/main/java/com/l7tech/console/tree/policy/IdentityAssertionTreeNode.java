package com.l7tech.console.tree.policy;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.PolicyValidatorResult;

import javax.swing.tree.TreeNode;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An assertion node in an assertion tree that refers to a user or group.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Oct 7, 2003<br/>
 * Time: 2:49:00 PM<br/>
 */
public abstract class IdentityAssertionTreeNode<AT extends IdentityAssertion> extends LeafAssertionTreeNode<AT> {

    public IdentityAssertionTreeNode(AT idass) {
        super(idass);
        assertion = idass;
    }

    protected String idProviderName() {
        if (provName == null) {
            long providerid = assertion.getIdentityProviderOid();
            IdentityProviderConfig cfg;
            try {
                if (providerid == IdentityProviderConfig.DEFAULT_OID)
                    provName = NA;
                else {
                    cfg = getIdentityAdmin().findIdentityProviderConfigByID(providerid);
                    if (cfg == null)
                        provName = NA;
                    else
                        provName = cfg.getName();
                }
            } catch (FindException e) {
                provName = NA;
                log.log(Level.SEVERE, "could not find provider associated with this assertion", e);
            } catch (Exception e) {
                provName = NA;
                log.log(Level.SEVERE, "could not loookup the IdentityAdmin", e);
            }
        }
        return provName;
    }

    private IdentityAdmin getIdentityAdmin() throws RuntimeException {
        return Registry.getDefault().getIdentityAdmin();
    }

    /**
     * Set the validator messages into this node. Overriden so the messages can
     * be shown in the identity view. Messages Are
     *
     * @param messages the validator messages
     */
    @Override
    public void setValidatorMessages(Collection<PolicyValidatorResult.Message> messages) {
        if (!PolicyTree.isIdentityView(this)) {
            super.setValidatorMessages(messages);
            return;
        }

        setValidatorMessages(messages, null);
        setValidatorMessages(messages, Boolean.TRUE);
        setValidatorMessages(messages, Boolean.FALSE);
    }

    private boolean isDisiplayable(AssertionTreeNode p) {
        if (!PolicyTree.isIdentityView(this)) {
            return true;
        }
        if (p instanceof IdentityPolicyTreeNode) return true;
        TreeNode[] path = p.getPath();
        if (path.length < 2) return true;
        IdentityPolicyTreeNode in = (IdentityPolicyTreeNode)path[1];

        if (p instanceof OneOrMoreAssertionTreeNode) {
            List<Assertion> pathAssertions = new ArrayList<Assertion>();
            CompositeAssertion ca = (CompositeAssertion)p.asAssertion();
            //noinspection ForLoopReplaceableByForEach
            for (Iterator iterator = ca.getChildren().iterator(); iterator.hasNext();) {
                Assertion assertion = (Assertion)iterator.next();
                if (in.pathContains(assertion)) {
                    pathAssertions.add(assertion);
                }
            }
            return pathAssertions.size() > 1;
        }
        return false;
    }

    /**
     * Merge passed messages and the receiver messages into the receiver messages
     *
     * @param receiver the receiver node
     * @param messages the messages
     */
    private void mergeMessages(AssertionTreeNode receiver, Collection<PolicyValidatorResult.Message> messages, Boolean direction) {
        Set<PolicyValidatorResult.Message> newMessages = new HashSet<PolicyValidatorResult.Message>();
        newMessages.addAll(messages);
        newMessages.addAll(receiver.getAllValidatorMessages());
        if ( receiver instanceof IdentityAssertionTreeNode ) {
            ((IdentityAssertionTreeNode)receiver).setValidatorMessages(newMessages, direction);
        } else {
            receiver.setValidatorMessages(newMessages);
        }
    }

    private void setValidatorMessages(Collection<PolicyValidatorResult.Message> messages, Boolean direction) {
        if ( direction == null ) {
            AssertionTreeNode parent = (AssertionTreeNode)getParent();
            if (parent != null) {
                AssertionTreeNode p = parent;
                while (p != null) {
                    if (isDisiplayable(p)) {
                        mergeMessages(p, messages, null);
                        break;
                    }
                    p = (AssertionTreeNode)p.getParent();
                }
            }
        } else if ( direction ) {
            AssertionTreeNode previous = (AssertionTreeNode)getPreviousSibling();
            if (previous != null) {
                mergeMessages(previous, messages, Boolean.TRUE);
            }
        } else {
            AssertionTreeNode next = (AssertionTreeNode)getNextSibling();
            if (next != null) {
                mergeMessages(next, messages, Boolean.FALSE);
            }
        }
    }

    private static final Logger log = Logger.getLogger(IdentityAssertionTreeNode.class.getName());
    protected AT assertion;
    protected String provName = null;
    public static final String NA = "provider not available";
}
