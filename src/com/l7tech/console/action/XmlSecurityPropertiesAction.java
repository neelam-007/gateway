package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.XmlSecurityPropertiesDialog;
import com.l7tech.console.tree.XmlRequestSecurityNode;
import com.l7tech.console.tree.policy.XmlSecurityTreeNode;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertion;
import com.l7tech.policy.assertion.xmlsec.ElementSecurity;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
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
        final MainWindow mw = Registry.getDefault().getComponentRegistry().getMainWindow();
        XmlSecurityPropertiesDialog dialog = new XmlSecurityPropertiesDialog(mw, true, n);
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        dialog.show();
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performActionOld() {
        XmlSecurityTreeNode n = (XmlSecurityTreeNode)node;
        XmlSecurityAssertion ass = (XmlSecurityAssertion)node.asAssertion();

        String signOnly = "Sign only";
        String encrypt = "Sign and encrypt";
        ElementSecurity[] ese = ass.getElements();
        if (ese == null || ese.length == 0) {
            throw new IllegalArgumentException("empty elements");
        }
        ElementSecurity es = ese[0];
        String s = (String)JOptionPane.showInputDialog(Registry.getDefault().
          getComponentRegistry().getMainWindow(),
            "Please select the " + n.getBaseName() + " options:\n",
            n.getBaseName() + " assertion properties",
            JOptionPane.PLAIN_MESSAGE,
            new ImageIcon(new XmlRequestSecurityNode().getIcon()),
            new Object[]{signOnly, encrypt},
            es.isEncryption() ? encrypt : signOnly);

        if ((s != null) && (s.length() > 0)) {
            es.setEncryption(!s.equalsIgnoreCase(signOnly));
            assertionChanged();
        }
    }

    public void assertionChanged() {
        JTree tree = ComponentRegistry.getInstance().getPolicyTree();
        if (tree != null) {
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            model.nodeChanged(node);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
