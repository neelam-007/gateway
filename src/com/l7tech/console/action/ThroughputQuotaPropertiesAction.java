/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.ThroughputQuotaTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.ThroughputQuotaForm;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action to edit the properties of an ThroughputQuota assertion
 *
 * @author flascelles@layer7-tech.com
 */
public class ThroughputQuotaPropertiesAction extends SecureAction {

    public ThroughputQuotaPropertiesAction(ThroughputQuotaTreeNode subject) {
        this.subject = subject;
    }

    public String getName() {
        return "Throughput Quota Properties";
    }

    public String getDescription() {
        return "View / Edit properties of a Throughput Quota Assertion";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        ThroughputQuotaForm dlg = new ThroughputQuotaForm(TopComponents.getInstance().getMainWindow(),
                                                          subject.getAssertion(), null);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
        if (dlg.wasOKed()) {
            JTree tree = TopComponents.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged(subject);
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }
    }

    private final Logger log = Logger.getLogger(getClass().getName());
    private ThroughputQuotaTreeNode subject;
}
