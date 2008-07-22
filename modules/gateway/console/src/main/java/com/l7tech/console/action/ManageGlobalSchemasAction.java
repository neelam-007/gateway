/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 3, 2005<br/>
 */
package com.l7tech.console.action;

import com.l7tech.console.panels.GlobalSchemaDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.policy.assertion.xml.SchemaValidation;

/**
 * Action to manage global schemas
 *
 * @author flascelles@layer7-tech.com
 */
public class ManageGlobalSchemasAction extends SecureAction {
    public ManageGlobalSchemasAction() {
        super(new AttemptedAnyOperation(EntityType.SCHEMA_ENTRY), SchemaValidation.class);
    }

    public String getName() {
        return "Manage Global XML Schemas";
    }

    public String getDescription() {
        return "View/Edit XML schemas that are used by other schemas.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/xmlObject16.gif";
    }

    protected void performAction() {
        GlobalSchemaDialog dlg = new GlobalSchemaDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }
}
