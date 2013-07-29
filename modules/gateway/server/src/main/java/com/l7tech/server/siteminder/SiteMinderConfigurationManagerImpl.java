package com.l7tech.server.siteminder;

import com.ca.siteminder.SiteMinderApiClassException;
import com.ca.siteminder.SiteMinderLowLevelAgent;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.server.HibernateGoidEntityManager;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
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
    extends HibernateGoidEntityManager<SiteMinderConfiguration, EntityHeader>
    implements SiteMinderConfigurationManager {

    protected final ConcurrentMap<String, SiteMinderLowLevelAgent> cache = new ConcurrentHashMap<String, SiteMinderLowLevelAgent>();

    @Override
    public Class<? extends GoidEntity> getImpClass() {
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
    public SiteMinderLowLevelAgent getSiteMinderLowLevelAgent(String name) throws FindException, SiteMinderApiClassException {
        SiteMinderLowLevelAgent agent = cache.get(name);
        if (agent == null) {
            agent = new SiteMinderLowLevelAgent(new SiteMinderAgentConfig(getSiteMinderConfiguration(name)));
            cache.put(name, agent);
        }
        return agent;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // if a SiteMinderConfiguration has been modified, remove it from the cache
        if (event instanceof GoidEntityInvalidationEvent) {
            final GoidEntityInvalidationEvent entityInvalidationEvent = (GoidEntityInvalidationEvent) event;
            if (SiteMinderConfiguration.class.equals(entityInvalidationEvent.getEntityClass())) {
                final Goid[] ids = entityInvalidationEvent.getEntityIds();
                for (final Goid id : ids) {
                    try {
                        SiteMinderConfiguration c = findByPrimaryKey(id);
                        //TODO may need to do some cleanup
                        cache.remove(c.getName());
                    } catch (FindException e) {
                    }
                }
            }
        }
    }
}
