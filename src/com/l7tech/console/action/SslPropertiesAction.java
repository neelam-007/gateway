package com.l7tech.console.action;

import com.l7tech.console.tree.SslTransportNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.SslAssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.SslAssertion;

import javax.swing.*;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>SslPropertiesAction</code> edits the SSL assertion
 * properties.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SslPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(SslPropertiesAction.class.getName());

    public SslPropertiesAction(SslAssertionTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "SSL Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/Edit SSL Properties";
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
        String[] options = new String[SslAssertion.options().size()];
        int index = 0;
        for (Iterator iterator = SslAssertion.options().iterator(); iterator.hasNext();) {
            SslAssertion.Option option = (SslAssertion.Option)iterator.next();
            options[index++] = option.getName();
        }
        SslAssertionTreeNode sn = (SslAssertionTreeNode)node;
        SslAssertion sslAssertion = (SslAssertion)node.asAssertion();

        String s =
          (String)JOptionPane.showInputDialog(TopComponents.getInstance().getMainWindow(),
            "Select an SSL option:\n",
            "SSL Properties",
            JOptionPane.PLAIN_MESSAGE,
            new ImageIcon(new SslTransportNode().getIcon()),
            options,
            sslAssertion.getOption().getName());
        if ((s != null) && (s.length() > 0)) {
            for (Iterator iterator = SslAssertion.options().iterator(); iterator.hasNext();) {
                SslAssertion.Option option = (SslAssertion.Option)iterator.next();
                if (option.getName().equals(s)) {
                    sslAssertion.setOption(option);
                    assertionChanged();
                    break;
                }
            }
        }
    }

    public void assertionChanged() {
        JTree tree = (JTree)TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode)node);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
