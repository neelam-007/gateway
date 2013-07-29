package com.l7tech.server.siteminder;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderLowLevelAgent;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.GoidEntityManagerStub;
import org.springframework.context.ApplicationEvent;

public class SiteMinderConfigurationManagerStub extends GoidEntityManagerStub<SiteMinderConfiguration, EntityHeader> implements SiteMinderConfigurationManager {

    @Override
    public SiteMinderConfiguration getSiteMinderConfiguration(String configurationName) throws FindException {
        return null;
    }

    @Override
    public SiteMinderLowLevelAgent getSiteMinderLowLevelAgent(String name) throws FindException, SiteMinderApiClassException {
        return null;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
    }
}
