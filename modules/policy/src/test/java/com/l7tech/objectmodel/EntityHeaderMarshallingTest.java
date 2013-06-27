/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import com.l7tech.policy.PolicyHeader;
import com.l7tech.objectmodel.migration.EntityHeaderWithDependencies;
import com.l7tech.policy.PolicyType;
import org.junit.Test;

import javax.xml.bind.JAXB;
import java.util.Collections;
import java.util.Random;

/** @author alex */
public class EntityHeaderMarshallingTest {

    @Test
    public void testSomething() throws Exception {
        JAXB.marshal(new EntityHeaderWithDependencies(new PolicyHeader(-1,true, PolicyType.INCLUDE_FRAGMENT,"a policy", "desc", "abc-123", -1L, null, 2, 2, false, null), Collections.singleton(new EntityHeaderRef(EntityType.FOLDER, "-1234"))), System.out);
    }

    @Test
    public void testIdentityHeader() throws Exception {
        JAXB.marshal(new IdentityHeader(1, "2", EntityType.USER, "username", "some user", null, null), System.out);
    }

    @Test
    public void testGoidEntityHeader() throws Exception {
        Random random = new Random();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        Goid goid = new Goid(bytes);

        JAXB.marshal(new EntityHeader(goid, EntityType.ANY, "myEntity", "myDescription", 1), System.out);
    }
}