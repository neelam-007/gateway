package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.beaneditor.BeanAdapter;
import com.l7tech.console.beaneditor.BeanEditor;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.CustomAssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.CustomAssertion;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>CustomAssertionPropertiesAction</code> edits the SSL assertion
 * properties.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class CustomAssertionPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(CustomAssertionPropertiesAction.class.getName());

    public CustomAssertionPropertiesAction(CustomAssertionTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Custome assertion properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/edit custom properties";
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
    public void performAction() {
        CustomAssertionHolder cah = (CustomAssertionHolder)node.asAssertion();
        final JDialog dialog = new JDialog(Registry.getDefault().getComponentRegistry().getMainWindow(), true);
        final CustomAssertion ca = cah.getCustomAssertion();
        BeanEditor.Options options = new BeanEditor.Options();
        options.setDescription(ca.getName());
        options.setExcludeProperties(new String[]{"name"});
        BeanEditor be = new BeanEditor(dialog, ca, Object.class, options);
        be.addBeanListener(new BeanAdapter() {
            public void onEditAccepted(Object source, Object bean) {
                dialog.dispose();
                assertionChanged();
            }
        });
        dialog.setTitle(ca.getName());
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        dialog.show();
    }

    public void assertionChanged() {
        JTree tree = (JTree)ComponentRegistry.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode)node);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
