package com.l7tech.external.assertions.validatenonsoapsaml.console;

import com.l7tech.console.action.NodeActionWithMetaSupport;
import com.l7tech.console.panels.saml.RequireWssSamlPropertiesPanel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.validatenonsoapsaml.ValidateNonSoapSamlAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xmlsec.RequireSaml;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

public class EditValidateNonSoapSamlAssertionAction extends NodeActionWithMetaSupport {

    public EditValidateNonSoapSamlAssertionAction(AssertionTreeNode node) {
        super(node, ValidateNonSoapSamlAssertion.class, node.asAssertion());
        if (!(node.asAssertion() instanceof RequireSaml)) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected void performAction() {
        final ValidateNonSoapSamlAssertion requestSaml = (ValidateNonSoapSamlAssertion)node.asAssertion();
        final Frame mw = TopComponents.getInstance().getTopParent();
        final RequireWssSamlPropertiesPanel<ValidateNonSoapSamlAssertion> dlg =
          new RequireWssSamlPropertiesPanel<ValidateNonSoapSamlAssertion>(requestSaml, mw, true, !node.canEdit(), true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.hasAssertionChanged()) {
                    final ValidateNonSoapSamlAssertion updatedAssertion = new ValidateNonSoapSamlAssertion(requestSaml);

                    node.setUserObject(updatedAssertion);
                    if (requestSaml.getParent() != null) {
                        requestSaml.getParent().replaceChild(requestSaml, updatedAssertion);
                    }

                    final JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        final PolicyTreeModel model = (PolicyTreeModel) tree.getModel();
                        model.assertionTreeNodeChanged((AssertionTreeNode) node);
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            }
        });
    }

}
