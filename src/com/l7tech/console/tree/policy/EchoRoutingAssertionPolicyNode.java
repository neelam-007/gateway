/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.*;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EchoRoutingAssertion;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.EchoRoutingAssertionDialog;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.common.gui.util.Utilities;

/**
 * Policy node for the EchoRoutingAssertion
 *
 * @author alex
 * @version $Revision$
 */
public class EchoRoutingAssertionPolicyNode extends LeafAssertionTreeNode {

    //- PUBLIC

    /**
     *
     */
    public EchoRoutingAssertionPolicyNode(Assertion assertion ) {
        super( assertion );
        if (!(assertion instanceof EchoRoutingAssertion)) throw new IllegalArgumentException("assertion must be an " + EchoRoutingAssertion.class.getName());
    }

    /**
     *
     */
    public String getName() {
        return "Echo request to response";
    }

    /**
     * Gets the default action for this node.
     *
     * @return The action for editing this nodes assertion properties.
     */
    public Action getPreferredAction() {
        return new SecureAction(null, EchoRoutingAssertion.class) {
            public String getName() {
                return "Routing Properties";
            }

            public String getDescription() {
                return "View and edit routing properties";
            }

            protected String iconResource() {
                return "com/l7tech/console/resources/Properties16.gif";
            }

            protected void performAction() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JFrame f = TopComponents.getInstance().getMainWindow();
                        EchoRoutingAssertionDialog d =
                          new EchoRoutingAssertionDialog(f, (EchoRoutingAssertion)asAssertion());
                        d.addPolicyListener(listener);
                        d.pack();
                        Utilities.centerOnScreen(d);
                        d.setVisible(true);
                    }
                });
            }
        };
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        Action a = getPreferredAction();
        list.add(a);
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     *
     */
    public boolean canDelete() {
        return true;
    }

    //- PROTECTED

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    //- PRIVATE

    private final PolicyListener listener = new PolicyListenerAdapter() {
        public void assertionsChanged(PolicyEvent e) {
            JTree tree = TopComponents.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged(EchoRoutingAssertionPolicyNode.this);
            }
        }
    };
}
