package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.XpathBasedAssertion;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.rmi.RemoteException;
import java.io.IOException;

import org.jaxen.JaxenException;
import org.xml.sax.SAXException;

/**
 * Action for editing XML security assertion properties
 */
public class XpathBasedAssertionPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(XpathBasedAssertionPropertiesAction.class.getName());
    private XpathEvaluator testEvaluator;

    public XpathBasedAssertionPropertiesAction(XpathBasedAssertionTreeNode node) {
        super(node);
        try {
            testEvaluator = XpathEvaluator.newEvaluator(XmlUtil.stringToDocument("<blah xmlns=\"http://bzzt.com\"/>"),
                                                        new HashMap());
        } catch (IOException e) {
            logger.log(Level.WARNING, "cannot create evaluator", e); // cannot happen
        } catch (SAXException e) {
            logger.log(Level.WARNING, "cannot create evaluator", e); // cannot happen
        }
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
                if (n instanceof RequestXpathPolicyTreeNode || n instanceof ResponseXpathPolicyTreeNode) {
                    XpathBasedAssertion xmlSecAssertion = (XpathBasedAssertion)node.asAssertion();
                    String res = JOptionPane.showInputDialog("Please provide xpath value.",
                                                             xmlSecAssertion.getXpathExpression().getExpression());
                    if (isValidValue(res)) {
                        xmlSecAssertion.setXpathExpression(new XpathExpression(res));
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

    private boolean isValidValue(String xpath) {
        if (xpath == null) return false;
        xpath = xpath.trim();
        if (xpath.length() < 1) return false;
         try {
            testEvaluator.evaluate(xpath);
        } catch (JaxenException e) {
            log.fine(e.getMessage());
            return false;
        } catch (NullPointerException e) {
            log.fine(e.getMessage());
            return false;
        }
        return true;
    }
}
