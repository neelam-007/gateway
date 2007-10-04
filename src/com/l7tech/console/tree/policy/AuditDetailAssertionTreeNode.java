package com.l7tech.console.tree.policy;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.panels.AuditDetailAssertionPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AuditAssertion;
import com.l7tech.policy.assertion.AuditDetailAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an AuditDetailAssertion in the policy tree.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 19, 2006<br/>
 */
public class AuditDetailAssertionTreeNode extends LeafAssertionTreeNode {
    protected static final Logger logger = Logger.getLogger(AuditDetailAssertionTreeNode.class.getName());

    public AuditDetailAssertionTreeNode(Assertion assertion) {
        super(assertion);
        if (!(assertion instanceof AuditDetailAssertion))
            throw new IllegalArgumentException("Argument is not an AuditDetailAssertion");
    }

    public String getName() {
        return "Audit detail: \"" + getAssertion().getDetail() + "\"";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    public AuditDetailAssertion getAssertion() {
        return (AuditDetailAssertion)getUserObject();
    }

    public boolean canDelete() {
        return true;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List<Action> list = new ArrayList<Action>();
        list.add(getPreferredAction());
        list.addAll(Arrays.asList(super.getActions()));
        return list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new SecureAction(null, AuditAssertion.class) {
            public String getName() {
                return "Audit Detail Properties";
            }

            public String getDescription() {
                return "Change the properties of the audit detail assertion.";
            }

            protected String iconResource() {
                return "com/l7tech/console/resources/Properties16.gif";
            }

            protected void performAction() {
                AuditDetailAssertionPropertiesDialog aad =
                        new AuditDetailAssertionPropertiesDialog(TopComponents.getInstance().getTopParent(),
                                                                 getAssertion());
                aad.pack();
                Utilities.centerOnScreen(aad);
                Utilities.setEscKeyStrokeDisposes(aad);
                aad.setVisible(true);
                if (aad.isModified()) {
                    assertionChanged();
                }
            }

            public void assertionChanged() {
                JTree tree = TopComponents.getInstance().getPolicyTree();
                if (tree != null) {
                    PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                    model.assertionTreeNodeChanged(AuditDetailAssertionTreeNode.this);
                } else {
                    AuditDetailAssertionTreeNode.logger.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }
        };
    }
}
