package com.l7tech.server.siteminder;

import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * The implementation of the interface SiteMinderAdmin to manage SiteMinder Configuration Entities,
 * User: nilic
 * Date: 7/22/13
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class SiteMinderAdminImpl  extends AsyncAdminMethodsImpl implements SiteMinderAdmin {

    private SiteMinderConfigurationManager siteMinderConfigurationManager;

    public SiteMinderAdminImpl (SiteMinderConfigurationManager siteMinderConfigurationManager) {
        this.siteMinderConfigurationManager = siteMinderConfigurationManager;
    }

    /**
     * Retrieve a SiteMinder configuration entity from the database by using a configuration name.
     * @param configurationName: the name of a SiteMinder configuration
     * @return a SiteMinder Configuration entity with the name, "configurationName".
     * @throws FindException: thrown when errors finding the SiteMinder configuration entity.
     */
    public SiteMinderConfiguration getSiteMinderConfiguration(String configurationName) throws FindException {
        return siteMinderConfigurationManager.getSiteMinderConfiguration(configurationName);
    }

    /**
     * Retrieve all SiteMinder configuration entities from the database.
     * @return  a list of SiteMinder configuration entities
     * @throws FindException: thrown when errors finding the SiteMinder configuration entity.
     */
    public List<SiteMinderConfiguration> getAllSiteMinderConfigurations() throws FindException {

        List<SiteMinderConfiguration> configurations = new ArrayList<SiteMinderConfiguration>();
        configurations.addAll(siteMinderConfigurationManager.findAll());
        return configurations;
    }

    /**
     * Get the names of all SiteMinder configuration entities.
     * @return a list of the names of all SiteMinder configuration entities.
     * @throws FindException: thrown when errors finding the SiteMinder configuration entity.
     */
    public List<String> getAllSiteMinderConfigurationNames() throws FindException {

        List<SiteMinderConfiguration> configList = getAllSiteMinderConfigurations();
        List<String> names = new ArrayList<String>(configList.size());
        for (SiteMinderConfiguration config : configList){
            //TODO: Put the agentName
            names.add(config.getName());
        }

        return names;
    }

    /**
     * Save a SiteMinder configuration entity into the database.
     * @param configuration: the SiteMinder configuration entity to be saved.
     * @return a Goid, the saved entity object id.
     * @throws UpdateException: thrown when errors saving the SiteMinder configuration entity.
     */
    public Goid saveSiteMinderConfiguration(SiteMinderConfiguration configuration) throws UpdateException {
        siteMinderConfigurationManager.update(configuration);
        return configuration.getGoid();
    }

    /**
     * Delete a SiteMinder configuration entity from the database.
     * @param configuration: the SiteMinder configuration entity to be deleted.
     * @throws DeleteException: thrown when errors deleting the SiteMinder configuration entity.
     */
    public void deleteSiteMinderConfiguration(SiteMinderConfiguration configuration) throws DeleteException {
        siteMinderConfigurationManager.delete(configuration);
    }
}
