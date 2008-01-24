/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.common.policy.Policy;
import com.l7tech.objectmodel.Entity;
import com.l7tech.service.PublishedService;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test for EntityType
 */
public class EntityTypeTest extends TestCase {

    /**
     *
     */
    public EntityTypeTest(String name) throws Exception {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the EntityTypeTest <code>TestCase</code>
     */
    public static Test suite() {
        try {
            return new TestSuite(EntityTypeTest.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // test general case
    public void testEntity() {
        EntityType type = EntityType.findTypeByEntity( Entity.class );
        assertEquals("Any entity", EntityType.ANY, type);
    }

    // test policy since we use this
    public void testPolicyEntity() {
        EntityType type = EntityType.findTypeByEntity( Policy.class );
        assertEquals("Policy entity", EntityType.POLICY, type);
    }

    // test service since we use this
    public void testServiceEntity() {
        EntityType type = EntityType.findTypeByEntity( PublishedService.class );
        assertEquals("Service entity", EntityType.SERVICE, type);
    }

    // test all types 
    public void testAllEntities() {

        for ( EntityType type : EntityType.values() ) {
            Class<? extends Entity> clazz = type.getEntityClass();

            EntityType foundType = EntityType.findTypeByEntity( clazz );
            assertEquals(type, foundType);
        }
    }
}