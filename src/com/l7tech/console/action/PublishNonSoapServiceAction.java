package com.l7tech.console.action;

import com.l7tech.console.panels.PublishNonSoapServiceWizard;
import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.common.gui.util.Utilities;

/**
 * SSM action to publish a non-soap xml service.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 14, 2004<br/>
 * $Id$<br/>
 */
public class PublishNonSoapServiceAction extends SecureAction {
    public String getName() {
        return "Publish XML application";
    }

    public String getDescription() {
        return "Publish a non-soap XML application";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/services16.png";
    }

    protected void performAction() {
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        PublishNonSoapServiceWizard wiz = PublishNonSoapServiceWizard.getInstance(mw);
        wiz.pack();
        wiz.setSize(800, 480);
        Utilities.centerOnScreen(wiz);
        wiz.setModal(true);
        wiz.setVisible(true);
    }
}
