package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.identity.finder.FindIdentitiesDialog;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNodeFactory;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.Group;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.awt.*;


/**
 * The <code>AddIdentityAssertionAction</code> action assigns
 * the current assertion  to the target policy.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AddIdentityAssertionAction extends PolicyUpdatingAssertionAction {
    private int inserPosition = 0;

    public AddIdentityAssertionAction(AssertionTreeNode n) {
        this(n, 0);
    }

    public AddIdentityAssertionAction(AssertionTreeNode node, int inserPosition) {
        super(node, LIC_AUTH_ASSERTIONS, new SpecificUser()/*this or group not important*/);
        if (!(node.getUserObject() instanceof CompositeAssertion)) {
            throw new IllegalArgumentException();
        }
        this.inserPosition = inserPosition;
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    @Override
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Frame f = TopComponents.getInstance().getTopParent();
                Options options = new Options();
                options.setDisposeOnSelect(true);
                options.setDisableOpenProperties(true);
                FindIdentitiesDialog fd = new FindIdentitiesDialog(f, true, options);
                fd.pack();
                Utilities.centerOnScreen(fd);
                FindIdentitiesDialog.FindResult result = fd.showDialog();
                long providerId = result.providerConfigOid;
                EntityHeader[] headers = result.entityHeaders;
                java.util.List identityAssertions = new ArrayList();
                IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();

                try {
                    for (int i = 0; headers != null && i < headers.length; i++) {
                        EntityHeader header = headers[i];
                        if (header.getType() == EntityType.USER) {
                            User u = admin.findUserByID(providerId, header.getStrId());
                            identityAssertions.add(new SpecificUser(u.getProviderId(), u.getLogin(), u.getId(), u.getName()));
                        } else if (header.getType() == EntityType.GROUP) {
                            Group g = admin.findGroupByID(providerId, header.getStrId());
                            MemberOfGroup ma = new MemberOfGroup(g.getProviderId(), g.getName(), g.getId());
                            identityAssertions.add(ma);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Couldn't retrieve user or group", e);
                }

                JTree tree = (JTree)TopComponents.getInstance().getComponent(PolicyTree.NAME);
                int pos = inserPosition;
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    for (Iterator idit = identityAssertions.iterator(); idit.hasNext();) {
                        Assertion ass = (Assertion)idit.next();
                        AssertionTreeNode an = AssertionTreeNodeFactory.asTreeNode(ass);
                        model.insertNodeInto(an, assertionTreeNode, pos++);
                    }
                } else {
                    log.log(Level.WARNING, "Unable to reach the policy tree.");
                }
            }
        });
    }
}
