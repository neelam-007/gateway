package com.l7tech.server.siteminder;

import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntityManager;

/**
 * Created with IntelliJ IDEA.
 * An interface of managing SiteMinder configuration Entity
 * User: nilic
 * Date: 7/22/13
 * Time: 10:53 AM
 * To change this template use File | Settings | File Templates.
 */
public interface SiteMinderConfigurationManager extends GoidEntityManager<SiteMinderConfiguration, EntityHeader>{

    /**
     * Retrieve a SiteMinder configuration entity from the database by using a configuration name.
     * @param configurationName: the name of a SiteMinder configuration
     * @return a SiteMinder configuration entity with the name
     * @throws FindException: thrown when errors finding the SiteMinder configuration entity.
     */
    SiteMinderConfiguration getSiteMinderConfiguration(String configurationName) throws FindException;

}
