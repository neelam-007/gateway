package com.l7tech.external.assertions.validatenonsoapsaml.console;

import com.l7tech.console.action.NodeActionWithMetaSupport;
import com.l7tech.console.panels.saml.RequireWssSamlPropertiesPanel;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.validatenonsoapsaml.ValidateNonSoapSamlTokenAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xmlsec.RequireSaml;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

public class EditValidateNonSoapSamlTokenAssertionAction extends NodeActionWithMetaSupport {

    public EditValidateNonSoapSamlTokenAssertionAction(AssertionTreeNode node) {
        super(node, ValidateNonSoapSamlTokenAssertion.class, node.asAssertion());
        if (!(node.asAssertion() instanceof RequireSaml)) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected void performAction() {
        final ValidateNonSoapSamlTokenAssertion requestSaml = (ValidateNonSoapSamlTokenAssertion)node.asAssertion();
        final Frame mw = TopComponents.getInstance().getTopParent();
        final RequireWssSamlPropertiesPanel<ValidateNonSoapSamlTokenAssertion> dlg =
          new RequireWssSamlPropertiesPanel<ValidateNonSoapSamlTokenAssertion>(requestSaml, mw, true, !node.canEdit(), true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.hasAssertionChanged()) {
                    final ValidateNonSoapSamlTokenAssertion updatedAssertion = new ValidateNonSoapSamlTokenAssertion(requestSaml);

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
