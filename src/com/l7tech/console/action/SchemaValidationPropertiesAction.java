package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.panels.SchemaValidationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.SchemaValidationTreeNode;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.util.Registry;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for viewing or editing the properties of a Schema Validation Assertion node.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 6, 2004<br/>
 * $Id$<br/>
 */
public class SchemaValidationPropertiesAction extends BaseAction {

    public SchemaValidationPropertiesAction(SchemaValidationTreeNode node, PublishedService service) {
        this.node = node;
        this.service = service;
    }

    public String getName() {
        return "Schema validation properties";
    }

    public String getDescription() {
        return "View/Edit properties of the schema validation assertion.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    public void performAction() {
        Frame f = Registry.getDefault().getComponentRegistry().getMainWindow();
        SchemaValidationPropertiesDialog dlg = new SchemaValidationPropertiesDialog(f, node, service);
        dlg.addPolicyListener(listener);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
    }

    private final PolicyListener listener = new PolicyListenerAdapter() {
        public void assertionsChanged(PolicyEvent e) {
            JTree tree = ComponentRegistry.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged(node);
                log.finest("model invalidated");
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }
    };

    private final Logger log = Logger.getLogger(getClass().getName());
    private SchemaValidationTreeNode node;
    private PublishedService service;
}
