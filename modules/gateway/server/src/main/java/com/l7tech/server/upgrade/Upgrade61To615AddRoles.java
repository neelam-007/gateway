package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.log.SinkManagerImpl;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.EntityType.LOG_SINK;

/**
 * A database upgrade task that adds roles to the database.
 */
public class Upgrade61To615AddRoles implements UpgradeTask {
    private static final Logger logger = Logger.getLogger(Upgrade61To615AddRoles.class.getName());
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
        final SessionFactory sessionFactory = getBean("sessionFactory", SessionFactory.class);
        try {
            addRolesForSinkConfigurations(sessionFactory, roleManager);
        } catch (FindException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        } catch (SaveException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        }
    }

    private void addRolesForSinkConfigurations( final SessionFactory sessionFactory,
                                                final RoleManager roleManager )
            throws FindException, SaveException
    {
        // Find all sinks, if any of them doesn't have a role, try to create it
        final Collection<SinkConfiguration> sinks =
            new HibernateTemplate(sessionFactory).execute( new ReadOnlyHibernateCallback<Collection<SinkConfiguration>>(){
                @SuppressWarnings("unchecked")
                @Override
                public Collection<SinkConfiguration> doInHibernateReadOnly( final Session session ) throws HibernateException, SQLException {
                    return (Collection<SinkConfiguration>)session.createCriteria( SinkConfiguration.class ).list();
                }
            } );
        for ( final SinkConfiguration sink : sinks ) {
            final Collection<Role> roles = roleManager.findEntitySpecificRoles(LOG_SINK, sink.getGoid());
            if ( roles == null || roles.isEmpty() ) {
                logger.info("Auto-creating missing Role for log sink " + sink.getName() + " (#" + sink.getGoid() + ")");
                for ( final Role role : SinkManagerImpl.createRolesForSink( sink ) ) {
                    roleManager.save( role );
                }
            }
        }
    }

}
