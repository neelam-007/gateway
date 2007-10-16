package com.l7tech.console.tree.policy;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.panels.IdentityAssertionVariablesDialog;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
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
public abstract class IdentityAssertionTreeNode extends LeafAssertionTreeNode<IdentityAssertion> {
    public IdentityAssertionTreeNode(IdentityAssertion idass) {
        super(idass);
        assertion = idass;
    }

    protected String idProviderName() {
        if (provName == null) {
            long providerid = assertion.getIdentityProviderOid();
            IdentityProviderConfig cfg = null;
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
    public void setValidatorMessages(Collection messages) {
        if (!PolicyTree.isIdentityView(this)) {
            super.setValidatorMessages(messages);
            return;
        }
        // identity view
        AssertionTreeNode previous = (AssertionTreeNode)getPreviousSibling();
        AssertionTreeNode next = (AssertionTreeNode)getNextSibling();
        AssertionTreeNode parent = (AssertionTreeNode)getParent();
        if (previous != null) {
            mergeMessages(previous, messages);
        } else if (next != null) {
            mergeMessages(next, messages);
        } else if (parent != null) {
            AssertionTreeNode p = parent;
            while (p != null) {
                if (isDisiplayable(p)) {
                    mergeMessages(p, messages);
                    break;
                }
                p = (AssertionTreeNode)p.getParent();
            }
        }
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
            List pathAssertions = new ArrayList();
            CompositeAssertion ca = (CompositeAssertion)p.asAssertion();
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
    private void mergeMessages(AssertionTreeNode receiver, Collection messages) {
        Set newMessages = new HashSet();
        newMessages.addAll(messages);
        newMessages.addAll(receiver.getAllValidatorMessages());
        receiver.setValidatorMessages(newMessages);
    }

    @Override
    public Action[] getActions() {
        Action[] supers = super.getActions();
        Action[] my = new Action[supers.length+1];
        System.arraycopy(supers, 0, my, 0, supers.length);
        my[my.length-1] = new IdentityAssertionAttributesAction(this);
        return my;
    }

    static final Logger log = Logger.getLogger(IdentityAssertionTreeNode.class.getName());
    private IdentityAssertion assertion;
    private String provName = null;
    public static final String NA = "provider not available";

    private static class IdentityAssertionAttributesAction extends SecureAction {
        private LeafAssertionTreeNode node;

        private IdentityAssertionAttributesAction(LeafAssertionTreeNode node) {
            super(null, SpecificUser.class);
            this.node = node;
}

        public String getName() {
            return "Attributes";
        }

        protected String iconResource() {
            return "com/l7tech/console/resources/user16.png";
        }

        protected void performAction() {
            final Assertion ass = node.asAssertion();
            if (!(ass instanceof IdentityAssertion)) throw new RuntimeException();
            IdentityAssertionVariablesDialog dlg = new IdentityAssertionVariablesDialog(TopComponents.getInstance().getTopParent(), (IdentityAssertion) ass);
            dlg.pack();
            Utilities.centerOnScreen(dlg);
            DialogDisplayer.display(dlg);
            if (dlg.isOk()) {
                JTree tree = TopComponents.getInstance().getPolicyTree();
                if (tree != null) {
                    PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                    model.assertionTreeNodeChanged(node);
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }
        }
    }
}
