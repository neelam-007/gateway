package com.l7tech.server.siteminder;

import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.server.HibernateGoidEntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created with IntelliJ IDEA.
 * User: nilic
 * Date: 7/22/13
 * Time: 10:58 AM
 * To change this template use File | Settings | File Templates.
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SiteMinderConfigurationManagerImpl
    extends HibernateGoidEntityManager<SiteMinderConfiguration, EntityHeader>
    implements SiteMinderConfigurationManager {

    @Override
    public Class<? extends GoidEntity> getImpClass() {
        return SiteMinderConfiguration.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NAME;
    }

    public SiteMinderConfiguration getSiteMinderConfiguration(String configurationName) throws FindException {
        return findByUniqueName(configurationName);
    }
}
