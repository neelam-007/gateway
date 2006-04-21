package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.MappingAssertionDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.MappingAssertionPolicyNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.util.logging.Level;

/**
 * Display properties dialog for AuditAssertion.
 */
public class MappingAssertionPropertiesAction extends SecureAction {
    private MappingAssertionPolicyNode subject;

    public MappingAssertionPropertiesAction(MappingAssertionPolicyNode subject) {
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
        MappingAssertionDialog aad = new MappingAssertionDialog(TopComponents.getInstance().getMainWindow(), subject.getAssertion(), true);
        aad.pack();
        Utilities.centerOnScreen(aad);
        Utilities.setEscKeyStrokeDisposes(aad);
        aad.setVisible(true);
        if (aad.isModified()) {
            subject.setUserObject(aad.getAssertion());
            assertionChanged();
        }
    }

    public void assertionChanged() {
        JTree tree = (JTree)TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode)subject);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }

}
