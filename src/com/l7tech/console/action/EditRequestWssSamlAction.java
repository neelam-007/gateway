/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Jan 18, 2005<br/>
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.saml.RequestWssSamlPropertiesPanel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;

import javax.swing.*;
import java.util.logging.Level;

/**
 * Action that invokes the editor for the {@link com.l7tech.policy.assertion.xmlsec.RequestWssSaml}
 * subclasses.
 *
 * @author emil
 */
public class EditRequestWssSamlAction extends NodeAction {
    private RequestWssSaml requestWssSaml;

    public EditRequestWssSamlAction(AssertionTreeNode node) {
        super(node);
        if (!(node.asAssertion() instanceof RequestWssSaml)) {
            throw new IllegalArgumentException();
        }
        requestWssSaml = (RequestWssSaml)node.asAssertion();
    }

    public String getName() {
        return "View/Edit SAML Assertion";
    }

    public String getDescription() {
        return getName();
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        boolean assertionChanged = false;
        assertionChanged = editSamlAssertion(requestWssSaml, mw);

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

    private boolean editSamlAssertion(RequestWssSaml samlAuthenticationStatement, MainWindow mw) {
        RequestWssSamlPropertiesPanel dlg =
          new RequestWssSamlPropertiesPanel(samlAuthenticationStatement, mw, true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
        return dlg.hasAssertionChanged();
    }

}
