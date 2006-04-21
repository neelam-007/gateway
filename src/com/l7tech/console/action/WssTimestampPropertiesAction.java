package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.WssTimestampDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.WssTimestampPolicyNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.WssTimestamp;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Edits the {@link com.l7tech.policy.assertion.xmlsec.WssTimestamp} properties.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WssTimestampPropertiesAction extends NodeAction {

    //- PUBLIC

    public WssTimestampPropertiesAction(WssTimestampPolicyNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Timestamp Assertion Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/Edit Timestamp Assertion Properties";
    }

    //- PROTECTED

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/About16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        WssTimestamp ass = (WssTimestamp)node.asAssertion();
        JFrame f = TopComponents.getInstance().getMainWindow();
        WssTimestampDialog dlg = new WssTimestampDialog(f, true, ass);
        Utilities.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);

        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode)node);
        } else {
            WssTimestampPropertiesAction.log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }

    //- PRIVATE

    private static final Logger log = Logger.getLogger(WssTimestampPropertiesAction.class.getName());
}
