package com.l7tech.console.action;

import com.l7tech.service.PublishedService;

import java.util.logging.Logger;


/**
 * The SSM action type that imports a policy from a file.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 21, 2004<br/>
 */
public class ImportPolicyFromFileAction extends BaseAction {
    static final Logger log = Logger.getLogger(ImportPolicyFromFileAction.class.getName());
    protected PublishedService pubService;

    public ImportPolicyFromFileAction() {
    }

    public ImportPolicyFromFileAction(PublishedService svc) {
        if (svc == null) {
            throw new IllegalArgumentException();
        }
        this.pubService = svc;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Import Policy";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Import a policy from a file along with external references.";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/saveTemplate.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        if (pubService == null) {
            throw new IllegalStateException("no service specified");
        }
        // todo
    }
}
