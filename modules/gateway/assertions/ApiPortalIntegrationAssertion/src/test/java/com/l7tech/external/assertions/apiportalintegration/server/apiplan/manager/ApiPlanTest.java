package com.l7tech.external.assertions.apiportalintegration.server.apiplan.manager;

import com.l7tech.external.assertions.apiportalintegration.server.apiplan.ApiPlan;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class ApiPlanTest {
    private ApiPlan plan1;
    private ApiPlan plan2;

    @Before
    public void setup(){
        plan1 = new ApiPlan();
        plan2 = new ApiPlan();
    }

    @Test
    public void equal(){
        plan1.setName("name");
        plan2.setName("name");
        plan1.setDescription("desc");
        plan2.setDescription("desc");
        plan1.setPolicyXml("policy");
        plan2.setPolicyXml("policy");
        plan1.setDefaultPlan(true);
        plan2.setDefaultPlan(true);

        // these fields shouldn't matter
        plan1.setOid(1234L);
        plan2.setOid(5678L);
        plan1.setLastUpdate(new Date());
        plan2.setLastUpdate(new Date());
        plan1.setVersion(1);
        plan2.setVersion(2);
        plan1.setEnabled(true);
        plan2.setEnabled(false);

        assertTrue(plan1.equals(plan2));
        assertTrue(plan2.equals(plan1));
    }

    @Test
    public void equalSameObject(){
        assertTrue(plan1.equals(plan1));
    }

    @Test
    public void notEqualDifferentName(){
        plan1.setName("name1");
        plan2.setName("name2");

        assertFalse(plan1.equals(plan2));
        assertFalse(plan2.equals(plan1));
    }

    @Test
    public void notEqualDifferentDescription(){
        plan1.setDescription("desc1");
        plan2.setDescription("desc2");

        assertFalse(plan1.equals(plan2));
        assertFalse(plan2.equals(plan1));
    }

    @Test
    public void notEqualDifferentPolicy(){
        plan1.setPolicyXml("policy1");
        plan2.setPolicyXml("policy2");

        assertFalse(plan1.equals(plan2));
        assertFalse(plan2.equals(plan1));
    }

    @Test
    public void notEqualDifferentDefaultPlan(){
        plan1.setDefaultPlan(true);
        plan2.setDefaultPlan(false);

        assertFalse(plan1.equals(plan2));
        assertFalse(plan2.equals(plan1));
    }

    @Test
    public void notEqualRHSNull(){
        plan2 = null;
        assertFalse(plan1.equals(plan2));
    }

    @Test
    public void hashcode(){
        plan1.setName("name");
        plan2.setName("name");
        plan1.setDescription("desc");
        plan2.setDescription("desc");
        plan1.setPolicyXml("policy");
        plan2.setPolicyXml("policy");
        plan1.setDefaultPlan(true);
        plan2.setDefaultPlan(true);

        // these fields shouldn't matter
        plan1.setOid(1234L);
        plan2.setOid(5678L);
        plan1.setLastUpdate(new Date());
        plan2.setLastUpdate(new Date());
        plan1.setVersion(1);
        plan2.setVersion(2);
        plan1.setEnabled(true);
        plan2.setEnabled(false);

        assertTrue(plan1.hashCode() == plan2.hashCode());
    }

    @Test
    public void hashcodeDifferentName(){
        plan1.setName("name1");
        plan2.setName("name2");

        assertFalse(plan1.hashCode() == plan2.hashCode());
    }

    @Test
    public void hashcodeDifferentDescription(){
        plan1.setDescription("desc1");
        plan2.setDescription("desc2");

        assertFalse(plan1.hashCode() == plan2.hashCode());
    }

    @Test
    public void hashcodeDifferentPolicy(){
        plan1.setPolicyXml("policy1");
        plan2.setPolicyXml("policy2");

        assertFalse(plan1.hashCode() == plan2.hashCode());
    }

    @Test
    public void hashcodeDifferentDefaultPlan(){
        plan1.setDefaultPlan(true);
        plan2.setDefaultPlan(false);

        assertFalse(plan1.hashCode() == plan2.hashCode());
    }
}
