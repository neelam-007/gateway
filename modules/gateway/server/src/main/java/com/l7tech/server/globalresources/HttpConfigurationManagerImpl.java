package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.HibernateEntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP configuration manager implementation.
 */
@Transactional(propagation=Propagation.SUPPORTS, rollbackFor=Throwable.class)
public class HttpConfigurationManagerImpl extends HibernateEntityManager<HttpConfiguration, EntityHeader> implements HttpConfigurationManager {

    //- PUBLIC

    @Override
    public Class<HttpConfiguration> getImpClass() {
        return HttpConfiguration.class;
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints( final HttpConfiguration entity ) {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("host", entity.getHost());
        map.put("port", entity.getPort());
        map.put("protocol", entity.getProtocol()==null ? NULL : entity.getProtocol());
        map.put("path", entity.getPath()==null ? NULL : entity.getPath());
        return Arrays.asList(map);
    }

}
