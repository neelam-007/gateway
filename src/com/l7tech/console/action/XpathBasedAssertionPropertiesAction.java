package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog;
import com.l7tech.console.panels.XPathExpressionPanel;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.XpathBasedAssertion;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;

/**
 * Action for editing XML security assertion properties
 */
public abstract class XpathBasedAssertionPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(XpathBasedAssertionPropertiesAction.class.getName());

    public static XpathBasedAssertionPropertiesAction actionForNode(XpathBasedAssertionTreeNode node) {
        if (node instanceof RequestWssConfidentialityTreeNode) {
            RequestWssConfidentialityTreeNode n = (RequestWssConfidentialityTreeNode)node;
            return new RequestConfidentialityPropertiesAction(n);
        } else if (node instanceof RequestWssIntegrityTreeNode) {
            RequestWssIntegrityTreeNode n = (RequestWssIntegrityTreeNode)node;
            return new RequestIntegrityPropertiesAction(n);
        } else if (node instanceof ResponseWssConfidentialityTreeNode) {
            ResponseWssConfidentialityTreeNode n = (ResponseWssConfidentialityTreeNode)node;
            return new ResponseConfidentialityPropertiesAction(n);
        } else if (node instanceof ResponseWssIntegrityTreeNode) {
            ResponseWssIntegrityTreeNode n = (ResponseWssIntegrityTreeNode)node;
            return new ResponseIntegrityPropertiesAction(n);
        } else if (node instanceof RequestXpathPolicyTreeNode) {
            RequestXpathPolicyTreeNode n = (RequestXpathPolicyTreeNode)node;
            return new RequestXpathPropertiesAction(n);
        } else if (node instanceof ResponseXpathPolicyTreeNode) {
            ResponseXpathPolicyTreeNode n = (ResponseXpathPolicyTreeNode)node;
            return new ResponseXpathPropertiesAction(n);
        } else if (node instanceof RequestAcceleratedXpathPolicyTreeNode) {
            RequestAcceleratedXpathPolicyTreeNode n = (RequestAcceleratedXpathPolicyTreeNode)node;
            return new RequestAcceleratedXpathPropertiesAction(n);
        } else if (node instanceof ResponseAcceleratedXpathPolicyTreeNode) {
            ResponseAcceleratedXpathPolicyTreeNode n = (ResponseAcceleratedXpathPolicyTreeNode)node;
            return new ResponseAcceleratedXpathPropertiesAction(n);
        }
        throw new RuntimeException("Type not supported " + node.getClass().getName());
    }

    protected XpathBasedAssertionPropertiesAction(XpathBasedAssertionTreeNode node) {
        super(node);
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return getName();
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
                if (n instanceof RequestXpathPolicyTreeNode || n instanceof ResponseXpathPolicyTreeNode) {
                    XpathBasedAssertion xmlSecAssertion = (XpathBasedAssertion)node.asAssertion();
                    String title = null;
                    if (this instanceof RequestXpathPropertiesAction) {
                        title = "Evaluate Request XPath Properties";
                    } else if (this instanceof ResponseXpathPropertiesAction) {
                        title = "Evaluate Response XPath Properties";
                    } else {
                        title = "piglet"; // can't happen
                    }
                    String initialValue = xmlSecAssertion.getXpathExpression().getExpression();
                    XPathExpressionPanel panel = new XPathExpressionPanel(null, title, initialValue, xmlSecAssertion.getXpathExpression().getNamespaces());
                    panel.pack();
                    Utilities.centerOnScreen(panel);
                    panel.show();
                    /*String question = "Please provide xpath value.";

                    String res = (String)JOptionPane.showInputDialog(null, question, title,
                                                                     JOptionPane.QUESTION_MESSAGE,
                                                                     null, null, initialValue);*/
                    if (!panel.wasCanceled()) {
                        xmlSecAssertion.setXpathExpression(new XpathExpression(panel.newXpathValue(),
                                                                               panel.newXpathNamespaceMap()));
                        okListener.actionPerformed(null);
                    }
                } else {
                    JOptionPane.showMessageDialog(mw, "Cannot edit this assertion because it is not configurable " +
                                                      "on non-soap services.");
                }
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
