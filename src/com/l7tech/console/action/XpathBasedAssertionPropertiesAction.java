package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.XpathBasedAssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;

/**
 * Action for editing XML security assertion properties
 */
public class XpathBasedAssertionPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(XpathBasedAssertionPropertiesAction.class.getName());

    public XpathBasedAssertionPropertiesAction(XpathBasedAssertionTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Edit XPath Expression";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Edit XPath Expression";
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
    protected void performAction() {
        XpathBasedAssertionTreeNode n = (XpathBasedAssertionTreeNode)node;
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        try {
            if (n.getService().isSoap()) {
                XpathBasedAssertionPropertiesDialog dialog = new XpathBasedAssertionPropertiesDialog(mw, false, n, okListener);
                dialog.pack();
                dialog.setSize(900, 650); //todo: consider some dynamic sizing - em
                Utilities.centerOnScreen(dialog);
                dialog.show();
            } else {
                JOptionPane.showMessageDialog(mw, "Cannot edit this assertion because it is not configurable " +
                                                  "on non-soap services.");
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "cannot get associated service", e);
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "cannot get associated service", e);
        }
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
