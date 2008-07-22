package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.SchemaValidationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.SchemaValidationTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.gateway.common.service.PublishedService;

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
public class SchemaValidationPropertiesAction extends SecureAction {

    public SchemaValidationPropertiesAction(SchemaValidationTreeNode node, PublishedService service) {
        super(null, SchemaValidation.class);
        this.node = node;
        this.service = service;
    }

    public String getName() {
        return "Schema Validation Properties";
    }

    public String getDescription() {
        return "View and edit XML schema validation properties";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        final SchemaValidationPropertiesDialog dlg = new SchemaValidationPropertiesDialog(f, node, service);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (!dlg.isChangesCommitted())
                    return;

                JTree tree = TopComponents.getInstance().getPolicyTree();
                if (tree != null) {
                    PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                    model.assertionTreeNodeChanged(node);
                    log.finest("model invalidated");
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }
        });
    }

    private final Logger log = Logger.getLogger(getClass().getName());
    private SchemaValidationTreeNode node;
    private PublishedService service;
}
