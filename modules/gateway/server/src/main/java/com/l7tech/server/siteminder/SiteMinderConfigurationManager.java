package com.l7tech.server.siteminder;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderLowLevelAgent;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntityManager;
import org.springframework.context.ApplicationListener;

/**
 * Created with IntelliJ IDEA.
 * An interface of managing SiteMinder configuration Entity
 * User: nilic
 * Date: 7/22/13
 * Time: 10:53 AM
 * To change this template use File | Settings | File Templates.
 */
public interface SiteMinderConfigurationManager extends GoidEntityManager<SiteMinderConfiguration, EntityHeader>, ApplicationListener {

    /**
     * Retrieve a SiteMinder configuration entity from the database by using a configuration name.
     * @param configurationName: the name of a SiteMinder configuration
     * @return a SiteMinder configuration entity with the name
     * @throws FindException: thrown when errors finding the SiteMinder configuration entity.
     */
    SiteMinderConfiguration getSiteMinderConfiguration(String configurationName) throws FindException;

    /**
     * Retrieve SiteMinderLowLevelAgent by using a configuration name.
     *
     * @param name The name of a SiteMinder configuration
     * @return SiteMinderLowLevelAgent
     * @throws FindException thrown when errors finding the SiteMinder configuration entity
     * @throws SiteMinderApiClassException Thrown when failed to initialize SiteMinderLowLevelAgent.
     */
    SiteMinderLowLevelAgent getSiteMinderLowLevelAgent(String name) throws FindException, SiteMinderApiClassException;

    /**
     * Validate the SiteMinderConfiguration with the provided configuration detail.
     *
     * @param config The SiteMinder Configuration
     * @throws SiteMinderApiClassException Thrown when the configuration is invalid
     */
    void validateSiteMinderConfiguration(SiteMinderConfiguration config) throws SiteMinderApiClassException;

}
