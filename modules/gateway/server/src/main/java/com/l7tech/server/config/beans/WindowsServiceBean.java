package com.l7tech.server.config.beans;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 25, 2005
 * Time: 10:50:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class WindowsServiceBean extends BaseConfigurationBean {
    boolean doService;

    private final static String NAME = "Windows Service Configuration";
    private final static String DESCRIPTION = "Configures the SSG to start as a service";
    private static final String DO_SERVICE_INFO = "Configure the SSG to start as a service";
    private static final String DONT_DO_SERVICE_INFO = "Will not configure the SSG to start as a service";


    public WindowsServiceBean() {
        super(NAME, DESCRIPTION);
    }

    public void reset() {

    }

    protected void populateExplanations() {
        explanations.add(getName() + " - " + getDescription());
        if (isDoService()) {
           explanations.add(insertTab + DO_SERVICE_INFO);
        } else {
           explanations.add(insertTab + DONT_DO_SERVICE_INFO);
        }
    }

    public boolean isDoService() {
        return doService;
    }

    public void setDoService(boolean doService) {
        this.doService = doService;
    }
}
