/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Jan 18, 2005<br/>
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.panels.XmlSecurityRecipientContextEditor;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.util.logging.Level;
import java.awt.*;

/**
 * Action that lets the manager admin change the {@link com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext}
 * for an assertion of type {@link com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable}.
 *
 * @author flascelles@layer7-tech.com
 */
public class EditXmlSecurityRecipientContextAction extends NodeAction {
    private SecurityHeaderAddressable assertion;

    public EditXmlSecurityRecipientContextAction(AssertionTreeNode node) {
        super(node);
        assertion = (SecurityHeaderAddressable)node.asAssertion();
    }

    public String getName() {
        return "WSS Recipient";
    }

    public String getDescription() {
        return getName();
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final XmlSecurityRecipientContextEditor dlg = new XmlSecurityRecipientContextEditor(mw, assertion);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.hasAssertionChanged()) {
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged((AssertionTreeNode)node);
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            }
        });
    }
}
