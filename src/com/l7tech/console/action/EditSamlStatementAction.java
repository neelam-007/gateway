/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Jan 18, 2005<br/>
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.saml.AuthenticationStatementPropertiesPanel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlStatementAssertion;

import javax.swing.*;
import java.util.logging.Level;

/**
 * Action that invokes the editor for the {@link com.l7tech.policy.assertion.xmlsec.SamlStatementAssertion}
 * subclasses.
 *
 * @author emil
 */
public class EditSamlStatementAction extends NodeAction {
    private SamlStatementAssertion assertion;

    public EditSamlStatementAction(AssertionTreeNode node) {
        super(node);
        if (!(node.asAssertion() instanceof SamlStatementAssertion)) {
            throw new IllegalArgumentException();
        }
        assertion = (SamlStatementAssertion)node.asAssertion();
    }

    public String getName() {
        return "View/Edit SAML Assertiont";
    }

    public String getDescription() {
        return getName();
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    protected void performAction() {
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        boolean assertionChanged = false;
        if (assertion instanceof SamlAuthenticationStatement) {
            assertionChanged = editSamlAssertion((SamlAuthenticationStatement)assertion, mw);
        } else {
            throw new IllegalArgumentException("Don't know how to edit the "+assertion.getClass());
        }

        if (assertionChanged) {
            JTree tree = TopComponents.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged((AssertionTreeNode)node);
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }
    }

    private boolean editSamlAssertion(SamlAuthenticationStatement samlAuthenticationStatement, MainWindow mw) {
        AuthenticationStatementPropertiesPanel dlg =
          new AuthenticationStatementPropertiesPanel(samlAuthenticationStatement, mw, true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
        return dlg.hasAssertionChanged();
    }
}
