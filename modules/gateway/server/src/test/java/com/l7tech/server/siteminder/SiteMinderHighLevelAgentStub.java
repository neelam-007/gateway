package com.l7tech.server.siteminder;

import com.ca.siteminder.*;
import com.l7tech.util.MockConfig;

import java.util.HashMap;

public class SiteMinderHighLevelAgentStub extends SiteMinderHighLevelAgent {

    public SiteMinderHighLevelAgentStub() {
        super(new MockConfig(new HashMap<String, String>()), new SiteMinderAgentContextCacheManagerImpl());
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
