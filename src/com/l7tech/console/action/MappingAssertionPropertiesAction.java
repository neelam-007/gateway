package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.panels.MappingAssertionDialog;
import com.l7tech.console.tree.policy.MappingAssertionPolicyNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.identity.MappingAssertion;

import javax.swing.*;
import java.util.logging.Level;

/**
 * Display properties dialog for AuditAssertion.
 */
public class MappingAssertionPropertiesAction extends SecureAction {
    private MappingAssertionPolicyNode subject;

    public MappingAssertionPropertiesAction(MappingAssertionPolicyNode subject) {
        super(null, MappingAssertion.class);
        this.subject = subject;
    }
    public String getName() {
        return "Identity Mapping Assertion Properties";
    }

    public String getDescription() {
        return "Change the properties of the identity mapping assertion.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        final MappingAssertionDialog aad = new MappingAssertionDialog(TopComponents.getInstance().getTopParent(), subject.getAssertion(), true);
        aad.pack();
        Utilities.centerOnScreen(aad);
        Utilities.setEscKeyStrokeDisposes(aad);
        DialogDisplayer.display(aad, new Runnable() {
            public void run() {
                if (aad.isModified()) {
                    subject.setUserObject(aad.getAssertion());
                    assertionChanged();
                }
            }
        });
    }

    public void assertionChanged() {
        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged(subject);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }

}
