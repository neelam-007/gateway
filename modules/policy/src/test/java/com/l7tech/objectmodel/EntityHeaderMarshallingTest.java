/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import com.l7tech.policy.PolicyHeader;
import com.l7tech.objectmodel.migration.EntityHeaderWithDependencies;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.xml.bind.JAXB;
import java.util.Collections;
import java.util.logging.Logger;

/** @author alex */
public class EntityHeaderMarshallingTest extends TestCase {
    private static final Logger log = Logger.getLogger(EntityHeaderMarshallingTest.class.getName());

    public EntityHeaderMarshallingTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(EntityHeaderMarshallingTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSomething() throws Exception {
        JAXB.marshal(new EntityHeaderWithDependencies(new PolicyHeader(-1,true,"a policy", "desc", "abc-123", -1L, false), Collections.singleton(new EntityHeaderRef(EntityType.FOLDER, "-1234"))), System.out);
    }

    public void testIdentityHeader() throws Exception {
        JAXB.marshal(new IdentityHeader(1, "2", EntityType.USER, "username", "some user"), System.out);
    }
}