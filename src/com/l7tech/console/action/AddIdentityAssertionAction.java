package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.FindIdentitiesDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNodeFactory;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;


/**
 * The <code>AddIdentityAssertionAction</code> action assigns
 * the current assertion  to the target policy.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AddIdentityAssertionAction extends BaseAction {
    private AssertionTreeNode node;

    public AddIdentityAssertionAction(AssertionTreeNode n) {
        node = n;
         if (!(node.getUserObject() instanceof CompositeAssertion)) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Add user or group";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Add user or group to the policy";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/user16.png";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(
          new Runnable() {
            public void run() {
                JFrame f = TopComponents.getInstance().getMainWindow();
                FindIdentitiesDialog fd = new FindIdentitiesDialog(f, true);
                fd.pack();
                Utilities.centerOnScreen(fd);
                Principal[] principals = fd.showDialog();
                java.util.List identityAssertions = new ArrayList();

                for (int i = 0; principals !=null && i < principals.length; i++) {
                    Principal principal = principals[i];
                      if (principal instanceof User) {
                        User u = (User)principal;
                        identityAssertions.add(new SpecificUser(u.getProviderId(), u.getLogin()));
                    } else if (principal instanceof Group) {
                        Group g = (Group)principal;
                        MemberOfGroup ma = new MemberOfGroup(g.getProviderId(), g.getName(), g.getUniqueIdentifier());
                        identityAssertions.add(ma);
                    }
                }

                JTree tree = (JTree)TopComponents.getInstance().getComponent(PolicyTree.NAME);
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    for (Iterator idit = identityAssertions.iterator(); idit.hasNext();) {
                        Assertion ass = (Assertion)idit.next();
                        AssertionTreeNode an = AssertionTreeNodeFactory.asTreeNode(ass);
                        model.insertNodeInto(an, node, node.getChildCount());
                    }
                } else {
                    log.log(Level.WARNING, "Unable to reach the policy tree.");
                }
            }
        });
    }
}
