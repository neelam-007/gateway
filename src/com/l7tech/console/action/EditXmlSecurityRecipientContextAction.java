/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Jan 18, 2005<br/>
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.policy.XpathBasedAssertionTreeNode;
import com.l7tech.console.panels.XmlSecurityRecipientContextEditor;
import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertionBase;
import com.l7tech.common.gui.util.Utilities;

/**
 * Action that lets the manager admin change the {@link com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext}
 * for an assertion of type {@link com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertionBase}.
 *
 * @author flascelles@layer7-tech.com
 */
public class EditXmlSecurityRecipientContextAction extends NodeAction {
    private XmlSecurityAssertionBase assertion;

    public EditXmlSecurityRecipientContextAction(XpathBasedAssertionTreeNode node) {
        super(node);
        assertion = (XmlSecurityAssertionBase)node.asAssertion();
    }

    public String getName() {
        return "View/Edit XML Security Recipient";
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
            // todo, update the tree, make saveable
        }
    }
}
