/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Jan 18, 2005<br/>
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.Functions;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.saml.RequestWssSamlPropertiesPanel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml2;

import javax.swing.*;
import java.util.logging.Level;
import java.awt.*;

/**
 * Action that invokes the editor for the {@link com.l7tech.policy.assertion.xmlsec.RequestWssSaml}
 * subclasses.
 *
 * @author emil
 */
public class EditRequestWssSamlAction extends NodeAction {
    private RequestWssSaml requestWssSaml;

    public EditRequestWssSamlAction(AssertionTreeNode node) {
        super(node, RequestWssSaml.class);
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
        final Frame mw = TopComponents.getInstance().getTopParent();
        editSamlAssertion(requestWssSaml, mw, new Functions.UnaryVoid<Boolean>() {
            public void call(Boolean assertionChanged) {
                if (assertionChanged) {
                    RequestWssSaml updatedAssertion;
                    if (requestWssSaml.getVersion()==null ||
                        requestWssSaml.getVersion().intValue()==1) {
                        updatedAssertion = new RequestWssSaml(requestWssSaml);
                    }
                    else {
                        updatedAssertion = new RequestWssSaml2(requestWssSaml);
                    }
                    node.setUserObject(updatedAssertion);
                    if (requestWssSaml.getParent() != null) {
                        requestWssSaml.getParent().replaceChild(requestWssSaml, updatedAssertion);
                    }

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

    private void editSamlAssertion(RequestWssSaml samlAuthenticationStatement,
                                   Frame parent,
                                   final Functions.UnaryVoid<Boolean> callback)
    {
        final RequestWssSamlPropertiesPanel dlg =
          new RequestWssSamlPropertiesPanel(samlAuthenticationStatement, parent, true, !node.canEdit());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                callback.call(dlg.hasAssertionChanged());
            }
        });
    }

}
