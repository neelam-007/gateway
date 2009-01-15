package com.l7tech.server.migration;

import com.l7tech.objectmodel.migration.PropertyResolverException;
import com.l7tech.objectmodel.migration.MigrationUtils;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.EntityHeaderUtils;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Method;

/**
 * @author jbufu
 */
public class AssertionPropertyResolver extends DefaultEntityPropertyResolver {

    private static final Logger logger = Logger.getLogger(AssertionPropertyResolver.class.getName());

    public AssertionPropertyResolver(PropertyResolverFactory factory) {
        super(factory);
    }

    public void applyMapping(Object assertion, String propName, EntityHeader targetHeader, Object targetValue, EntityHeader originalHeader) throws PropertyResolverException {

        logger.log(Level.FINEST, "Applying mapping for assertion {0} : {1}.", new Object[]{assertion, propName});
        try {
            Method setter;
            if ("EntitiesUsed".equals(propName)) {
                setter = MigrationUtils.setterForPropertyName(assertion, "replaceEntity", EntityHeader.class, EntityHeader.class);
                setter.invoke(assertion, originalHeader, EntityHeaderUtils.fromEntity( (Entity)targetValue));
            } else {
                setter = MigrationUtils.setterForPropertyName(assertion, propName, targetValue.getClass());
                setter.invoke(assertion, targetValue);
            }
        } catch (Exception e) {
            throw new PropertyResolverException("Error applying mapping for " + propName, e);
        }
    }
}
