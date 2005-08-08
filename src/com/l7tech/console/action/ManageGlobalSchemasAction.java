/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 3, 2005<br/>
 */
package com.l7tech.console.action;

import com.l7tech.console.panels.GlobalSchemaDialog;
import com.l7tech.console.util.TopComponents;

/**
 * Action to manage global schemas
 *
 * @author flascelles@layer7-tech.com
 */
public class ManageGlobalSchemasAction extends SecureAction {
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
        GlobalSchemaDialog dlg = new GlobalSchemaDialog(TopComponents.getInstance().getMainWindow());
        dlg.pack();
        dlg.show();
    }
}
