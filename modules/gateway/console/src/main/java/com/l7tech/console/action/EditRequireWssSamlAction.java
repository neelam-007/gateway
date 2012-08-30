/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Jan 18, 2005<br/>
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.saml.RequireWssSamlPropertiesPanel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml2;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action that invokes the editor for the {@link com.l7tech.policy.assertion.xmlsec.RequireWssSaml}
 * subclasses.
 *
 * @author emil
 */
public class EditRequireWssSamlAction extends NodeActionWithMetaSupport {

    public EditRequireWssSamlAction(AssertionTreeNode node) {
        super(node, RequireWssSaml.class, node.asAssertion());
        if (!(node.asAssertion() instanceof RequireWssSaml)) {
            throw new IllegalArgumentException();
        }
    }

    protected void performAction() {
        final RequireWssSaml requestWssSaml = (RequireWssSaml)node.asAssertion();
        final Frame mw = TopComponents.getInstance().getTopParent();
        final RequireWssSamlPropertiesPanel<RequireWssSaml> dlg =
          new RequireWssSamlPropertiesPanel<RequireWssSaml>(requestWssSaml, mw, true, !node.canEdit(), false);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.hasAssertionChanged()) {
                    final RequireWssSaml updatedAssertion;
                    if (requestWssSaml.getVersion()==null ||
                        requestWssSaml.getVersion().intValue()==1) {
                        updatedAssertion = new RequireWssSaml(requestWssSaml);
                    }
                    else {
                        updatedAssertion = new RequireWssSaml2(requestWssSaml);
                    }
                    node.setUserObject(updatedAssertion);
                    if (requestWssSaml.getParent() != null) {
                        requestWssSaml.getParent().replaceChild(requestWssSaml, updatedAssertion);
                    }

                    final JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        final PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged((AssertionTreeNode)node);
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            }
        });
    }
}
