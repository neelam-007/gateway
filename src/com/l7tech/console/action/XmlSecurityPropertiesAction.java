package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.XmlSecurityPropertiesDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.XmlSecurityTreeNode;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for editing XML security assertion properties
 */
public class XmlSecurityPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(XmlSecurityPropertiesAction.class.getName());

    public XmlSecurityPropertiesAction(XmlSecurityTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "XML security properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/edit XML security properties";
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
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        XmlSecurityTreeNode n = (XmlSecurityTreeNode)node;
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        XmlSecurityPropertiesDialog dialog = new XmlSecurityPropertiesDialog(mw, false, n, okListener);
        dialog.pack();
        dialog.setSize(900, 650); //todo: consider some dynamic sizing - em
        Utilities.centerOnScreen(dialog);
        dialog.show();
    }


    public ActionListener okListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged((AssertionTreeNode)node);
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            });
        }
    };
}
