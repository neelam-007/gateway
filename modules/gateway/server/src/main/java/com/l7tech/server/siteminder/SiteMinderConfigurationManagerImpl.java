package com.l7tech.server.siteminder;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderLowLevelAgent;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created with IntelliJ IDEA.
 * User: nilic
 * Date: 7/22/13
 * Time: 10:58 AM
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SiteMinderConfigurationManagerImpl
    extends HibernateEntityManager<SiteMinderConfiguration, EntityHeader>
    implements SiteMinderConfigurationManager {

    protected final ConcurrentMap<Goid, SiteMinderLowLevelAgent> cache = new ConcurrentHashMap<Goid, SiteMinderLowLevelAgent>();

    @Override
    public Class<? extends PersistentEntity> getImpClass() {
        return SiteMinderConfiguration.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NAME;
    }

    @Override
    public SiteMinderConfiguration getSiteMinderConfiguration(String configurationName) throws FindException {
        return findByUniqueName(configurationName);
    }

    @Override
    public SiteMinderLowLevelAgent getSiteMinderLowLevelAgent(Goid goid) throws FindException, SiteMinderApiClassException {
        SiteMinderLowLevelAgent agent = cache.get(goid);
        if (agent == null) {
            SiteMinderConfiguration c = findByPrimaryKey(goid);
            if (c != null) {
                agent = new SiteMinderLowLevelAgent(new SiteMinderAgentConfig(c));
                cache.put(goid, agent);
            }
        }
        return agent;
    }

    @Override
    public void validateSiteMinderConfiguration(SiteMinderConfiguration config) throws SiteMinderApiClassException {
        new SiteMinderLowLevelAgent(new SiteMinderAgentConfig(config));
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // if a SiteMinderConfiguration has been modified, remove it from the cache
        if (event instanceof EntityInvalidationEvent) {
            final EntityInvalidationEvent entityInvalidationEvent = (EntityInvalidationEvent) event;
            if (SiteMinderConfiguration.class.equals(entityInvalidationEvent.getEntityClass())) {
                final Goid[] ids = entityInvalidationEvent.getEntityIds();
                for (final Goid id : ids) {
                    cache.remove(id);
                }
            }
        }
    }
}
