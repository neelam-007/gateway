package com.l7tech.server.ems;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Entity;

import java.util.Set;
import java.util.HashSet;

import org.junit.Test;
import org.junit.Assert;

/**
 * 
 */
public class EntityTypeTest {

    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
    private static final Set<EntityType> IGNORE_ENTITY_TYPES = new HashSet<EntityType>(  ) {{
      add(EntityType.VALUE_REFERENCE);  
    }};

    @Test
    public void testEntityTypesDeclarations() throws Exception {
        EntityType typeAny = EntityType.findTypeByEntity( Entity.class );
        Assert.assertEquals("Any entity", EntityType.ANY, typeAny);

        for ( EntityType type : EntityType.values() ) {
            if ( IGNORE_ENTITY_TYPES.contains( type ) ) continue;

            Class<? extends Entity> clazz = type.getEntityClass();
            Assert.assertNotNull( "EntityType class must not be null.", clazz );

            EntityType foundType = EntityType.findTypeByEntity( clazz );
            Assert.assertEquals(type, foundType);
        }
    }
}
