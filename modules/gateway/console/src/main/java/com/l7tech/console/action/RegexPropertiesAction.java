package com.l7tech.console.action;

import com.l7tech.console.beaneditor.BeanAdapter;
import com.l7tech.console.panels.RegexDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.PolicyUtil;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.objectmodel.GuidBasedEntityManager;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>RegexPropertiesAction</code> edits the Regex assertion
 * properties.
 *
 * @author Emil Marceta
 */
public class RegexPropertiesAction extends NodeActionWithMetaSupport {
    static final Logger log = Logger.getLogger(RegexPropertiesAction.class.getName());

    public RegexPropertiesAction(AssertionTreeNode node) {
        super(node, Regex.class, node.asAssertion());
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    @Override
    protected void performAction() {
        Regex ra = (Regex)node.asAssertion();
        Frame f = TopComponents.getInstance().getTopParent();
        GuidBasedEntityManager<Policy> finder = Registry.getDefault().getPolicyFinder();
        RegexDialog rd = new RegexDialog(f, ra, PolicyUtil.isAssertionPostRouting(ra, finder), !node.canEdit());
        rd.setModal(true);
        rd.getBeanEditSupport().addBeanListener(new BeanAdapter() {
            @Override
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
