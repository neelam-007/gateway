package com.l7tech.gateway.common.siteminder;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Created with IntelliJ IDEA.
 * Admin interface for managing SiteMinder Configuration Entities
 * User: nilic
 * Date: 7/22/13
 * Time: 10:16 AM
 * To change this template use File | Settings | File Templates.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types = EntityType.SITEMINDER_CONFIGURATION)
@Administrative
public interface SiteMinderAdmin extends AsyncAdminMethods {

    /**
     *  Retrieve a SiteMinder configuration entity from the database by using a configuration name.
     * @param configurationName: the name of a SiteMinder configuration
     * @return a SiteMinder configuration entity with the name, configurationName
     * @throws com.l7tech.objectmodel.FindException : thrown when errors finding the SiteMinder configuration entity.
     */
    @Transactional(readOnly = true)
    @Secured(types = EntityType.SITEMINDER_CONFIGURATION, stereotype = MethodStereotype.FIND_ENTITY)
    SiteMinderConfiguration getSiteMinderConfiguration(String configurationName) throws FindException;

    /**
     *  Retrieve a SiteMinder configuration entity from the database by using a configuration name.
     * @param id: the primary key of a SiteMinder configuration
     * @return a SiteMinder configuration entity with the name, configurationName
     * @throws com.l7tech.objectmodel.FindException : thrown when errors finding the SiteMinder configuration entity.
     */
    @Transactional(readOnly = true)
    @Secured(types = EntityType.SITEMINDER_CONFIGURATION, stereotype = MethodStereotype.FIND_ENTITY)
    SiteMinderConfiguration getSiteMinderConfiguration(Goid id) throws FindException;


    /**
     * Retrieve all SiteMinder configuration entities from the database.
     * @return a list of SiteMinder configuration entities
     * @throws FindException: thrown when errors finding the SiteMinder configuration entities.
     */
    @Transactional(readOnly = true)
    @Secured (types = EntityType.SITEMINDER_CONFIGURATION, stereotype = MethodStereotype.FIND_ENTITIES)
    List<SiteMinderConfiguration> getAllSiteMinderConfigurations() throws FindException;

    /**
     * Get the names of all SiteMinder configuration entities.
     * @return a list of the names of all SiteMinder configuration entities.
     * @throws FindException: thrown when errors finding the SiteMinder configuration entities.
     */
    @Transactional(readOnly = true)
    @Secured (types = EntityType.SITEMINDER_CONFIGURATION, stereotype = MethodStereotype.FIND_ENTITIES)
    List<String>  getAllSiteMinderConfigurationNames() throws FindException;

    /**
     * Save a SiteMinder configuration entity into the database.
     * @param siteMinderConfiguration: the SiteMinder configuration entity to be saved
     * @return  a long, the saved entity object id.
     * @throws com.l7tech.objectmodel.UpdateException : thrown when errors saving the SiteMinder configuration entity.
     */
    @Secured (types = EntityType.SITEMINDER_CONFIGURATION, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    Goid saveSiteMinderConfiguration(SiteMinderConfiguration siteMinderConfiguration) throws UpdateException;

    /**
     * Delete a SiteMinder configuration entity from the database.
     * @param siteMinderConfiguration: the SiteMinder configuration entity to be deleted.
     * @throws com.l7tech.objectmodel.DeleteException : thrown when errors deleting the SiteMinder configuration entity.
     */
    @Secured (types = EntityType.SITEMINDER_CONFIGURATION, stereotype = MethodStereotype.DELETE_ENTITY)
    void deleteSiteMinderConfiguration(SiteMinderConfiguration siteMinderConfiguration) throws DeleteException;

    /**
     * Register and retrieve siteminder host configuration.
     * @param address: Policy Server Address
     * @param username: Username to login to PolicyServer
     * @param password: Password to login to PolicyServer
     * @param hostname: Registered hostname
     * @param hostconfig: Host's configuration
     * @param fipsMode: FIPS mode
     * @return
     */
    @Transactional(readOnly = true)
    @Secured (types = EntityType.SITEMINDER_CONFIGURATION, stereotype = MethodStereotype.FIND_ENTITIES)
    AsyncAdminMethods.JobId<SiteMinderHost> registerSiteMinderConfiguration(String address,
                                                                            String username,
                                                                            Goid password,
                                                                            String hostname,
                                                                            String hostconfig,
                                                                            Integer fipsMode);

    /**
     * Test the SiteMinder Configuration
     * @param siteMinderConfiguration The SiteMinder configuration to be tested.
     * @return empty string if the testing is successful.  Otherwise, return an error message with testing failure detail.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.SITEMINDER_CONFIGURATION, stereotype=MethodStereotype.TEST_CONFIGURATION)
    AsyncAdminMethods.JobId<String> testSiteMinderConfiguration(SiteMinderConfiguration siteMinderConfiguration);
}
