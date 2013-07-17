package com.l7tech.external.assertions.apiportalintegration.server.accountplan;


import com.l7tech.objectmodel.Goid;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountPlanTest {
    private AccountPlan plan1;
    private AccountPlan plan2;

    @Before
    public void setup(){
        plan1 = new AccountPlan();
        plan2 = new AccountPlan();
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
        plan1.setQuota(100);
        plan2.setQuota(100);
        plan1.setThroughputQuotaEnabled(true);
        plan2.setThroughputQuotaEnabled(true);
        plan1.setTimeUnit(1);
        plan2.setTimeUnit(1);
        plan1.setCounterStrategy(1);
        plan2.setCounterStrategy(1);
        plan1.setIds(new ArrayList<String>());
        plan2.setIds(new ArrayList<String>());

        // these fields shouldn't matter
        plan1.setGoid(new Goid(0,1234L));
        plan2.setGoid(new Goid(0,5678L));
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
    public void notEqualDifferentThroughputQuotaEnabled() {
        plan1.setThroughputQuotaEnabled(true);
        plan2.setThroughputQuotaEnabled(false);

        assertFalse(plan1.equals(plan2));
        assertFalse(plan2.equals(plan1));
    }

    @Test
    public void notEqualDifferentTimeUnit() {
        plan1.setTimeUnit(1);
        plan2.setTimeUnit(2);

        assertFalse(plan1.equals(plan2));
        assertFalse(plan2.equals(plan1));
    }

    @Test
    public void notEqualDifferentCounterStrategy() {
        plan1.setCounterStrategy(1);
        plan2.setCounterStrategy(2);

        assertFalse(plan1.equals(plan2));
        assertFalse(plan2.equals(plan1));
    }

    @Test
    public void equalOrganizationIds() {
        //test both empty
        assertTrue(plan1.equals(plan2));
        assertTrue(plan2.equals(plan1));

        //test empty
        List<String> list1 = new ArrayList<String>();
        List<String> list2 = new ArrayList<String>();

        plan1.setIds(list1);
        plan2.setIds(list2);

        assertTrue(plan1.equals(plan2));
        assertTrue(plan2.equals(plan1));

        //test elements same order
        list1.add("e1");
        list1.add("e2");
        list2.add("e1");
        list2.add("e2");

        plan1.setIds(list1);
        plan2.setIds(list2);

        assertTrue(plan1.equals(plan2));
        assertTrue(plan2.equals(plan1));

        //test different order of ele
        list1 = new ArrayList<String>();
        list1.add("e1");
        list1.add("e3");
        list1.add("e2");

        list2 = new ArrayList<String>();
        list2.add("e3");
        list2.add("e2");
        list2.add("e1");

        plan1.setIds(list1);
        plan2.setIds(list2);

        assertTrue(plan1.equals(plan2));
        assertTrue(plan2.equals(plan1));
    }

    @Test
    public void notEqualDifferentOrganizationIds() {
        //test one null
        List<String> list1 = new ArrayList<String>();

        plan1.setIds(list1);
        plan2.setIds(null);

        assertFalse(plan1.equals(plan2));
        assertFalse(plan2.equals(plan1));

        List<String> list2 = new ArrayList<String>();
        list2.add("ele1");
        plan1.setIds(list1);
        plan2.setIds(list2);

        assertFalse(plan1.equals(plan2));
        assertFalse(plan2.equals(plan1));

        list1.add("ele1");
        list1.add("ele2");
        plan1.setIds(list1);
        plan2.setIds(list2);

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
        plan1.setGoid(new Goid(0,1234L));
        plan2.setGoid(new Goid(0,5678L));
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
