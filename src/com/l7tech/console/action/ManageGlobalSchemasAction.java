/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 3, 2005<br/>
 */
package com.l7tech.console.action;

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
        return "com/l7tech/console/resources/cert16.gif";
    }

    protected void performAction() {
        // todo, the real thing
        /*try {
            Collection blah = Registry.getDefault().getSchemaAdmin().findAllSchemas();
            for (Iterator iterator = blah.iterator(); iterator.hasNext();) {
                SchemaEntry o = (SchemaEntry)iterator.next();
                System.out.println("Entry: " + o);
            }
        } catch (Exception e) {
            System.out.println("blah");
        }*/
    }
}
