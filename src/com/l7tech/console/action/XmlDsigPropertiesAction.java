package com.l7tech.console.action;

import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.panels.Utilities;
import com.l7tech.console.panels.XmlDsigAssertionDialog;
import com.l7tech.console.tree.policy.XmlDsigAssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.WindowManager;
import com.l7tech.policy.assertion.xmlsec.XmlDsigAssertion;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>XmlDsigPropertiesAction</code> edits the digital
 * signature assertion properties.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class XmlDsigPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(XmlDsigPropertiesAction.class.getName());

    public XmlDsigPropertiesAction(XmlDsigAssertionTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Xml Dsig properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/edit XML digital signature";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(
          new Runnable() {
            public void run() {
                JFrame f = Registry.getDefault().getWindowManager().getMainWindow();
                XmlDsigAssertionDialog d =
                  new XmlDsigAssertionDialog(f, (XmlDsigAssertion)node.asAssertion());
                d.addPolicyListener(listener);
                d.pack();
                Utilities.centerOnScreen(d);
                d.show();
            }
        });
    }

    private final PolicyListener listener = new PolicyListenerAdapter() {
        public void assertionsChanged(PolicyEvent e) {
            JTree tree =
                  (JTree)WindowManager.getInstance().getPolicyTree();
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    model.nodeChanged(node);
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
        }

    };
}
