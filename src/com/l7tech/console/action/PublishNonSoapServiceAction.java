package com.l7tech.console.action;

/**
 * [class_desc]
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
        // todo
    }
}
