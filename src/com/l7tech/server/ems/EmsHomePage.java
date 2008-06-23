package com.l7tech.server.ems;

import com.l7tech.common.LicenseManager;
import org.restlet.Context;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * Handler for requests to the Enterprise Manager Server root URI.
 */
public class EmsHomePage extends Resource {
    public EmsHomePage(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    public void handleGet() {
        Reference req = getRequest().getResourceRef();
        final Reference target = new Reference(req);

        LicenseManager licenseManager = (LicenseManager) Ems.getAppContext(this).getBean("licenseManager", LicenseManager.class);
        if (licenseManager.isFeatureEnabled("set:core")) {
            // Already licensed -- redirect to login page
            target.setPath("/ems/Login.html");
        } else {
            // Not yet licensed -- redirect to setup page
            target.setPath("/ems/Setup.html");
        }
        getResponse().redirectTemporary(target);
    }
}