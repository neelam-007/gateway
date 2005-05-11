package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.beaneditor.BeanAdapter;
import com.l7tech.console.beaneditor.BeanEditor;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.CustomAssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.*;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * The <code>CustomAssertionPropertiesAction</code> edits the SSL assertion
 * properties.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class CustomAssertionPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(CustomAssertionPropertiesAction.class.getName());
    private CustomAssertionsRegistrar registrar;
    private CustomAssertionUI customAssertionUI;
    private boolean registrarCalled;

    public CustomAssertionPropertiesAction(CustomAssertionTreeNode node) {
        super(node);
        registrar = Registry.getDefault().getCustomAssertionsRegistrar();

    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Custom Assertion Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/Edit Custom Properties";
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
        AssertionEditor editor = getCustomEditor();
        if (editor != null) {
            editor.addEditListener(new EditListener() {
                public void onEditAccepted(Object source, Object bean) {
                    assertionChanged();
                }

                public void onEditCancelled(Object source, Object bean) {}
            });
            if (editor instanceof Window) {
                Utilities.centerOnScreen((Window)editor);
            }
                editor.edit();
        } else {
            performGenericEditorAction();
        }
    }

    private void performGenericEditorAction() {
        CustomAssertionHolder cah = (CustomAssertionHolder)node.asAssertion();
        final JDialog dialog = new JDialog(TopComponents.getInstance().getMainWindow(), true);
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

    private AssertionEditor getCustomEditor() {
        CustomAssertionHolder cah = (CustomAssertionHolder)node.asAssertion();
        if (customAssertionUI != null) {
            return customAssertionUI.getEditor(cah.getCustomAssertion());
        }
        if (registrarCalled) {
            return null;
        }
        try {
            customAssertionUI = registrar.getUI(cah.getCustomAssertion().getClass());
            registrarCalled = true;
            return getCustomEditor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void assertionChanged() {
        JTree tree = (JTree)TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode)node);
        } else {
            log.log(Level.WARNING, "Unable to get the palette tree.");
        }
    }
}
