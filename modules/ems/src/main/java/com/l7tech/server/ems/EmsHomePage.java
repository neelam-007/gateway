package com.l7tech.server.ems;

import com.l7tech.server.GatewayLicenseManager;
import org.restlet.data.Reference;
import org.restlet.resource.Resource;

/**
 * Handler for requests to the Enterprise Manager Server root URI.
 */
public class EmsHomePage extends Resource {
    private GatewayLicenseManager licenseManager;

    public EmsHomePage(GatewayLicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    @Override
    public void handleGet() {
        Reference req = getRequest().getResourceRef();
        final Reference target = new Reference(req);

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