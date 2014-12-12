package com.l7tech.external.assertions.retrieveservicewsdl.console;

import com.l7tech.console.action.AbstractPublishServiceAction;
import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.util.TopComponents;

/**
 * Launches Publish WSDL Query Handler wizard.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class PublishWsdlQueryHandlerAction extends AbstractPublishServiceAction {

    private static final String TITLE = "Publish WSDL Query Handler";
    private static final String DESCRIPTION = "Publish a WSDL Query Handler service";
    private static final String ICON = "com/l7tech/console/resources/services16.png";

    @Override
    protected AbstractPublishServiceWizard createWizard() {
        return PublishWsdlQueryHandlerWizard.getInstance(TopComponents.getInstance().getTopParent());
    }

    @Override
    public String getName() {
        return TITLE;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    protected String iconResource() {
        return ICON;
    }
}
