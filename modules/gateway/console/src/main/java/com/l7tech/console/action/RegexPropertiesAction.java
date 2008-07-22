package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.beaneditor.BeanAdapter;
import com.l7tech.console.panels.RegexDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.RegexPolicyNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Regex;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * The <code>RegexPropertiesAction</code> edits the Regex assertion
 * properties.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RegexPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(RegexPropertiesAction.class.getName());

    public RegexPropertiesAction(RegexPolicyNode node) {
        super(node, Regex.class);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Regular Expression Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/Edit Regular Expression";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        Regex ra = (Regex)node.asAssertion();
        Frame f = TopComponents.getInstance().getTopParent();
        RegexDialog rd = new RegexDialog(f, ra, !node.canEdit());
        rd.setModal(true);
        rd.getBeanEditSupport().addBeanListener(new BeanAdapter() {
            public void onEditAccepted(Object source, Object bean) {
                assertionChanged();
            }
        });

        Utilities.setEscKeyStrokeDisposes(rd);
        rd.pack();
        Utilities.centerOnScreen(rd);
        DialogDisplayer.display(rd);
    }

    public void assertionChanged() {
        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode)node);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
