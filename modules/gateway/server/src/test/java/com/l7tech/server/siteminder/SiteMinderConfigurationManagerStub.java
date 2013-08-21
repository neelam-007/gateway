package com.l7tech.server.siteminder;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderLowLevelAgent;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerStub;
import org.springframework.context.ApplicationEvent;

public class SiteMinderConfigurationManagerStub extends EntityManagerStub<SiteMinderConfiguration, EntityHeader> implements SiteMinderConfigurationManager {

    @Override
    public SiteMinderConfiguration getSiteMinderConfiguration(String configurationName) throws FindException {
        return null;
    }

    @Override
    public SiteMinderLowLevelAgent getSiteMinderLowLevelAgent(Goid goid) throws FindException, SiteMinderApiClassException {
        return null;
    }

    @Override
    public void validateSiteMinderConfiguration(SiteMinderConfiguration config) throws SiteMinderApiClassException {
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
    }
}
