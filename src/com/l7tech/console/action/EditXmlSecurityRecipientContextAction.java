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
import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.util.logging.Level;

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
        return "WSS Security Recipient";
    }

    public String getDescription() {
        return getName();
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        XmlSecurityRecipientContextEditor dlg = new XmlSecurityRecipientContextEditor(mw, assertion);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
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
}
