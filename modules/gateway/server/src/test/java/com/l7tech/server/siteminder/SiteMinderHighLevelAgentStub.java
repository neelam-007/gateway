package com.l7tech.server.siteminder;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderContext;
import com.ca.siteminder.SiteMinderCredentials;
import com.ca.siteminder.SiteMinderHighLevelAgent;

public class SiteMinderHighLevelAgentStub extends SiteMinderHighLevelAgent {
    @Override
    public boolean checkProtected(String userIp, String smAgentName, String resource, String action, SiteMinderContext context) throws SiteMinderApiClassException {
        return true;
    }

    @Override
    public int processAuthorizationRequest(String userIp, String ssoCookie, SiteMinderContext context) throws SiteMinderApiClassException {
        return 1;
    }

    @Override
    public int processAuthenticationRequest(SiteMinderCredentials credentials, String userIp, String ssoCookie, SiteMinderContext context) throws SiteMinderApiClassException {
        return 1;
    }
}
