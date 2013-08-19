package com.l7tech.console.action;

import com.l7tech.console.panels.HomePagePanel;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.TopComponents;

/**
 * The <code>HomeAction</code> displays the dds the new user.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HomeAction extends SecureAction {
    private WorkSpacePanel wpanel;

    public HomeAction() {
        super(null);
        wpanel = TopComponents.getInstance().getCurrentWorkspace();
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Home";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Go to the Policy Manager home page";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/server16.gif";
    }

    @Override
    protected boolean isPrerequisiteFeatureSetLicensed(ConsoleLicenseManager lm) {
        // this action does not require service:Admin to be licensed
        return true;
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        try {
            wpanel.setComponent(new HomePagePanel());
        } catch (ActionVetoException e) {
            log.fine("workspace change vetoed");
        }
    }

}
