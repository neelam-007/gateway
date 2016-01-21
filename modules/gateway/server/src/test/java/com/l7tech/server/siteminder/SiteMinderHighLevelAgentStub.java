package com.l7tech.server.siteminder;

import com.ca.siteminder.*;

public class SiteMinderHighLevelAgentStub extends SiteMinderHighLevelAgent {

    public SiteMinderHighLevelAgentStub() {
        super(new SiteMinderAgentContextCacheManager());
    }

    @Override
    public boolean checkProtected(String userIp, String smAgentName, String server, String resource, String action, SiteMinderContext context) throws SiteMinderApiClassException {
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
