package com.l7tech.server.siteminder;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.util.SiteMinderUtil;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gateway.common.siteminder.SiteMinderFipsModeOption;
import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import com.l7tech.objectmodel.*;
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
 * The implementation of the interface SiteMinderAdmin to manage SiteMinder Configuration Entities,
 * 
 * @author nilic
 * Date: 7/22/13
 * Time: 11:07 AM
 */
public class SiteMinderAdminImpl extends AsyncAdminMethodsImpl implements SiteMinderAdmin {

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

        List<SiteMinderConfiguration> configurations = new ArrayList<>();
        configurations.addAll(siteMinderConfigurationManager.findAll());
        return configurations;
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
     * @param siteMinderConfiguration SiteMinderConfiguration
     */
    @Override
    public AsyncAdminMethods.JobId<SiteMinderHost> registerSiteMinderConfiguration(final SiteMinderConfiguration siteMinderConfiguration) {
        final FutureTask<SiteMinderHost> registerTask =
                new FutureTask<>(find(false).wrapCallable(new Callable<SiteMinderHost>() {
            @Override
            public SiteMinderHost call() throws Exception {
                SiteMinderHost siteMinderHost;

                try {
                    siteMinderHost =  registerSiteMinderHost(siteMinderConfiguration.getAddress(),
                            siteMinderConfiguration.getUserName(),
                            siteMinderConfiguration.getPasswordGoid(),
                            siteMinderConfiguration.getHostname(),
                            siteMinderConfiguration.getHostConfiguration(),
                            SiteMinderFipsModeOption.getByCode(siteMinderConfiguration.getFipsmode()));
                } catch (IOException e) {
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
        final FutureTask<String> registerTask = new FutureTask<>(find(false).wrapCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try{
                    siteMinderConfigurationManager.validateSiteMinderConfiguration(siteMinderConfiguration);
                } catch (SiteMinderApiClassException e) {
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
     */
    public SiteMinderHost registerSiteMinderHost(String address,
                                                 String username,
                                                 Goid passwordGoid,
                                                 String hostname,
                                                 String hostconfig,
                                                 SiteMinderFipsModeOption fipsMode)
            throws IOException, ParseException, FindException {
        String password;

        try {
            SecurePassword securePassword = securePasswordManager.findByPrimaryKey(passwordGoid);

            if (securePassword == null) throw new FindException();

            password = new String(securePasswordManager.decryptPassword(securePassword.getEncodedPassword()));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find password. " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
            throw e;
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Parsing error during password decryption. " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
            throw e;
        }

        return SiteMinderUtil.regHost(address, username, password, hostname, hostconfig, fipsMode);
    }
}
