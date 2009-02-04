package com.l7tech.server.migration;

import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.migration.*;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.gateway.common.security.keystore.SsgKeyHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Method;

/**
 * Extracts and applies SsgKeyEntry dependencies using an Assertion's PrivateKeyable interface.
 *
 * Only keys from non-default keystores are exposed as dependencies and mappable.
 *
 * @author jbufu
 */
public class SsgKeyResolver extends AbstractPropertyResolver {

    private static final Logger logger = Logger.getLogger(SsgKeyResolver.class.getName());

    SsgKeyStoreManager keyManager;

    public SsgKeyResolver(PropertyResolverFactory factory, SsgKeyStoreManager keyManager) {
        super(factory);
        this.keyManager = keyManager;
    }

    @Override
    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(ExternalEntityHeader source, Object entity, Method property, String propertyName) throws PropertyResolverException {

        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{propertyName, source});

        if (! (entity instanceof PrivateKeyable) )
            throw new IllegalArgumentException("Cannot handle entity: " + entity);

        Map<ExternalEntityHeader, Set<MigrationDependency>> result = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();

        final MigrationMappingType type = MigrationUtils.getMappingType(property);
        final boolean exported = MigrationUtils.isExported(property);
        PrivateKeyable keyable = (PrivateKeyable) entity;
        if (! keyable.isUsesDefaultKeyStore()) {
            SsgKeyEntry key = null;
            try {
                key = keyManager.lookupKeyByKeyAlias(keyable.getKeyAlias(), keyable.getNonDefaultKeystoreId());
                ExternalEntityHeader dependency = EntityHeaderUtils.toExternal(new SsgKeyHeader(key));
                result.put(dependency, Collections.singleton(new MigrationDependency(source, dependency, propertyName, type, exported)));
            } catch (Exception e) {
                throw new PropertyResolverException("Error retrieving SSG key: " + (key == null ? null : key.getId()) );
            }
        }

        return result;
    }

    public void applyMapping(Object sourceEntity, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader) throws PropertyResolverException {
        logger.log(Level.FINEST, "Applying mapping for {0} : {1}.", new Object[]{sourceEntity, propName});
        
        if ( ! (sourceEntity instanceof PrivateKeyable) || ! (targetValue instanceof SsgKeyEntry) )
            throw new PropertyResolverException("Cannot apply dependency value for: " + sourceEntity + " : " + targetValue);

        PrivateKeyable keyable = (PrivateKeyable) sourceEntity;
        SsgKeyEntry key = (SsgKeyEntry) targetValue;

        keyable.setNonDefaultKeystoreId(key.getKeystoreId());
        keyable.setKeyAlias(key.getAlias());
        keyable.setUsesDefaultKeyStore(false); // keys from non-default keystore aren't mappable in the first place
    }
}
