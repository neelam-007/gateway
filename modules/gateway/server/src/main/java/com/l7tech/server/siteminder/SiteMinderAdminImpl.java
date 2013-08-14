package com.l7tech.server.siteminder;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.util.SiteMinderUtil;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

import static com.l7tech.server.event.AdminInfo.find;

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

    @Inject
    SecurePasswordManager securePasswordManager;

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

    @Override
    public SiteMinderConfiguration getSiteMinderConfiguration(Goid id) throws FindException {
        return siteMinderConfigurationManager.findByPrimaryKey(id);
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
            if (config.isEnabled()) {
                names.add(config.getName());
            }
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


    /**
     * Register SiteMinder configuration
     * @param address: Policy Server Address
     * @param username: Username to login to PolicyServer
     * @param password: Password to login to PolicyServer
     * @param hostname: Registered hostname
     * @param hostconfig: Host's configuration
     * @param fipsMode: FIPS mode
     * @return
     */
    @Override
    public AsyncAdminMethods.JobId<SiteMinderHost> registerSiteMinderConfiguration(final String address,
                                                                                   final String username,
                                                                                   final Goid password,
                                                                                   final String hostname,
                                                                                   final String hostconfig,
                                                                                   final Integer fipsMode){

        final FutureTask<SiteMinderHost> registerTask = new FutureTask<SiteMinderHost>(find(false).wrapCallable(new Callable<SiteMinderHost>() {
            @Override
            public SiteMinderHost call() throws Exception {
                SiteMinderHost siteMinderHost = null;

                try{
                    siteMinderHost =  registerSiteMinderHost(address, username, password, hostname, hostconfig, fipsMode);
                } catch (IOException e){
                    final String msg = "Unable to register SiteMinder configuration. Check connection with policy server";
                    logger.log(Level.WARNING, msg + " " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    throw e;
                }
                return siteMinderHost;
            }
        }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                registerTask.run();
            }
        }, 0L);

        return registerJob(registerTask, SiteMinderHost.class);
    }

    @Override
    public JobId<String> testSiteMinderConfiguration(final SiteMinderConfiguration siteMinderConfiguration) {
        final FutureTask<String> registerTask = new FutureTask<String>(find(false).wrapCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {

                try{
                    siteMinderConfigurationManager.validateSiteMinderConfiguration(siteMinderConfiguration);
                } catch (SiteMinderApiClassException e){
                    return ExceptionUtils.getMessage(e);
                }
                return "";
            }
        }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                registerTask.run();
            }
        }, 0L);

        return registerJob(registerTask, String.class);
    }

    /**
     * Register SiteMinder Configuration
     * @param address: Policy Server Address
     * @param username: Username to login to PolicyServer
     * @param passwordGoid: Password to login to PolicyServer
     * @param hostname: Registered hostname
     * @param hostconfig: Host's configuration
     * @param fipsMode: FIPS mode
     * @return
     */
    public SiteMinderHost registerSiteMinderHost(String address,
                                                 String username,
                                                 Goid passwordGoid,
                                                 String hostname,
                                                 String hostconfig,
                                                 Integer fipsMode) throws IOException, ParseException, FindException {

        String password = "";
        try{
            SecurePassword securePassword = securePasswordManager.findByPrimaryKey(passwordGoid);
            password = new String(securePasswordManager.decryptPassword(securePassword.getEncodedPassword()));
        } catch (FindException e){
            final String msg = "Unable to find password oid entity.";
            logger.log(Level.WARNING, msg + " " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw e;
        } catch (ParseException e) {
            final String msg = "Parse exception during decrypting password.";
            logger.log(Level.WARNING, msg + " " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw e;
        }
        return SiteMinderUtil.regHost(address, username, password, hostname, hostconfig, fipsMode);
    }
}
