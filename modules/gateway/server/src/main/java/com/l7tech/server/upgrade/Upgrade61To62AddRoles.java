package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.log.SinkManager;
import com.l7tech.server.security.rbac.RoleManager;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.EntityType.LOG_SINK;

/**
 * A database upgrade task that adds roles to the database.
 */
public class Upgrade61To62AddRoles implements UpgradeTask {
    private static final Logger logger = Logger.getLogger(Upgrade61To62AddRoles.class.getName());
    private ApplicationContext applicationContext;

    /**
     * Get a bean safely.
     *
     * @param name the bean to get.  Must not be null.
     * @return the requested bean.  Never null.
     * @throws com.l7tech.server.upgrade.FatalUpgradeException  if there is no application context or the requested bean was not found
     */
    private <BT> BT getBean( final String name, final Class<BT> beanClass ) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        return applicationContext.getBean(name, beanClass);
    }

    @Override
    public void upgrade(ApplicationContext applicationContext) throws FatalUpgradeException, NonfatalUpgradeException {
        this.applicationContext = applicationContext;

        final RoleManager roleManager = getBean("roleManager", RoleManager.class);
        final SinkManager sinkManager = getBean("sinkManager", SinkManager.class);
        try {
            addRolesForSinkConfigurations(sinkManager, roleManager);
        } catch (FindException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        } catch (SaveException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        }

    }

    private void addRolesForSinkConfigurations( final SinkManager sinkManager,
                                                final RoleManager roleManager )
            throws FindException, SaveException
    {
        // Find all sinks, if any of them doesn't have a role, try to create it
        final Collection<SinkConfiguration> sinks = sinkManager.findAll();
        for ( final SinkConfiguration sink : sinks ) {
            final Collection<Role> roles = roleManager.findEntitySpecificRoles(LOG_SINK, sink.getOid());
            if ( roles == null || roles.isEmpty() ) {
                logger.info("Auto-creating missing Role for log sink " + sink.getName() + " (#" + sink.getOid() + ")");
                sinkManager.createRoles(sink);
            }
        }
    }

}
