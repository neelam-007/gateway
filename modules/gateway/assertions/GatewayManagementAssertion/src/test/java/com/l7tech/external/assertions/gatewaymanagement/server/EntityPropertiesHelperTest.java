package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.junit.Test;
import static org.junit.Assert.*;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 */
public class EntityPropertiesHelperTest {

    @Test
    public void testBeanProperties() throws Exception {
        final EntityPropertiesHelper helper = new EntityPropertiesHelper();

        for ( final EntityType baseType : EntityType.values() ) {
            for ( final Class<? extends Entity> entityClass : explode(baseType) ) {
                if ( entityClass != null && !helper.getPropertiesMap( entityClass ).isEmpty() ) {
                    System.out.println("Testing properties for : " + entityClass.getName());
                    final Collection<String> ignoredProperties = helper.getIgnoredProperties(entityClass);
                    final Collection<String> writeOnlyProperties = helper.getWriteOnlyProperties(entityClass);
                    final Collection<String> defaultProperties = helper.getPropertyDefaultsMap(entityClass).keySet();
                    final Map<String,String> propertyMapping = helper.getPropertiesMap(entityClass);

                    assertTrue( "Invalid write only property for " + entityClass.getName(), propertyMapping.keySet().containsAll( writeOnlyProperties ));
                    assertTrue( "Invalid default property for " + entityClass.getName(), propertyMapping.keySet().containsAll( defaultProperties ));

                    final Set<PropertyDescriptor> properties = BeanUtils.omitProperties(
                            BeanUtils.getProperties( entityClass ),
                            ignoredProperties.toArray(new String[ignoredProperties.size()]));

                    for ( PropertyDescriptor prop : properties ) {
                        if ( !propertyMapping.containsKey(prop.getName()) ) {
                            dumpProperties( properties );
                            fail( "Unknown entity property '"+prop.getName()+"' for entity '" + entityClass.getName() + "'."  );
                        }
                        final Class<?> returnType = prop.getReadMethod().getReturnType();
                        if ( Boolean.TYPE.isAssignableFrom( returnType ) ||
                             Boolean.class.isAssignableFrom( returnType ) ||
                             Integer.TYPE.isAssignableFrom( returnType ) ||
                             Integer.class.isAssignableFrom( returnType ) ||
                             Long.TYPE.isAssignableFrom( returnType ) ||
                             Long.class.isAssignableFrom( returnType ) ||
                             String.class.isAssignableFrom( returnType ) ||
                             Date.class.isAssignableFrom( returnType ) ||
                             Enum.class.isAssignableFrom( returnType ) ) {
                            // OK
                        } else {
                            fail("Unsupported property type '" + returnType +"' for entity '" + entityClass.getName() + "', property '"+prop.getName()+"'."  );
                        }
                    }

                }
            }
        }
    }

    private Collection<Class<? extends Entity>> explode( final EntityType type ) {
        if ( type == EntityType.ID_PROVIDER_CONFIG ) {
            return CollectionUtils.<Class<? extends Entity>>list(
                    FederatedIdentityProviderConfig.class,
                    IdentityProviderConfig.class,
                    LdapIdentityProviderConfig.class
            );
        } else {
            return Collections.<Class<? extends Entity>>singleton( type.getEntityClass() );
        }
    }

    private void dumpProperties( final Set<PropertyDescriptor> properties ) {
        System.out.println( "Properties are:" );
        final Set<String> names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        names.addAll( Functions.map( properties, new Functions.Unary<String,PropertyDescriptor>(){
            @Override
            public String call( final PropertyDescriptor propertyDescriptor ) {
                return propertyDescriptor.getName();
            }
        } ) );
        for ( final String name : names ) {
            System.out.println( name );
        }
    }
}
