package com.l7tech.console.action;

import com.l7tech.console.tree.policy.SchemaValidationTreeNode;

import java.util.logging.Logger;

/**
 * Action for viewing or editing the properties of a Schema Validation Assertion node.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 6, 2004<br/>
 * $Id$<br/>
 *
 */
public class SchemaValidationPropertiesAction extends BaseAction {

    public SchemaValidationPropertiesAction(SchemaValidationTreeNode node) {
        this.node = node;
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
        /*
        todo, plug in dialog here baby
        Frame f = Registry.getDefault().getComponentRegistry().getMainWindow();
        SamlPropertiesDialog pw = new SamlPropertiesDialog(f, assertion);
        pw.pack();
        Utilities.centerOnScreen(pw);
        pw.show();
        assertionChanged();

        public void assertionChanged() {
            JTree tree =
              (JTree)ComponentRegistry.getInstance().getPolicyTree();
            if (tree != null) {
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.nodeChanged(assertion);
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }
        */
    }

    private final Logger log = Logger.getLogger(getClass().getName());
    private SchemaValidationTreeNode node;
}
