package com.l7tech.server.util;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.UsesEntities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test the EntityUseUtils methods and annotations.
 */
public class EntityUseUtilsTest {
    @Test
    public void testEntityUsageOverride() {
        assertEquals("Default entity type name, no annotation", "Listen Port", EntityUseUtils.getTypeName( new UsesEntities() {
            @Override
            public EntityHeader[] getEntitiesUsed() {
                return new EntityHeader[0];
            }
            @Override
            public void replaceEntity( final EntityHeader oldEntityHeader, final EntityHeader newEntityHeader ) {
            }
        }, EntityType.SSG_CONNECTOR ));
    }
}
